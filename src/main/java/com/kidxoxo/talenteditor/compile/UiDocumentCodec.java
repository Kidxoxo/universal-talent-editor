package com.kidxoxo.talenteditor.compile;

import com.google.gson.*;
import com.kidxoxo.talenteditor.domain.UiDocument;
import com.kidxoxo.talenteditor.validation.ValidationReport;
import java.util.*;

/** One bounded, strict decoder is used by the compiler, network and editor. */
public final class UiDocumentCodec {
    public static final int MAX_ELEMENTS = 320;
    public static final int MAX_JSON = 262_144;
    public static final int MAX_WIRE_BYTES = 262_144;
    private static final Gson GSON = new Gson();
    public static final List<String> ANCHORS = List.of("top_left", "top_center", "top_right", "middle_left",
            "middle_center", "middle_right", "bottom_left", "bottom_center", "bottom_right");
    public static final List<String> BOOK_ACTIONS = List.of("close", "draw", "mode", "owned", "synthesis", "counters", "synthesize", "previous", "next");
    public static final List<String> DRAW_ACTIONS = List.of("close", "confirm", "refresh", "effects");
    public static final List<String> BOOK_BINDINGS = List.of("", "title", "summary", "page", "page_background");
    public static final List<String> DRAW_BINDINGS = List.of("", "title", "summary", "mechanics", "status");
    private static final Set<String> FIELDS = Set.of("id", "type", "anchor", "x", "y", "width", "height", "opacity", "color", "hoverColor", "textColor",
            "texture", "hoverTexture", "text", "binding", "action", "target", "index", "textScale", "iconSize", "iconX", "iconY", "textX", "textY", "rarityY", "bodyY", "align", "buttonStyle", "label", "locked");
    private UiDocumentCodec() {}
    public static JsonObject toJson(UiDocument doc) { return GSON.toJsonTree(doc).getAsJsonObject(); }
    public static UiDocument read(String json, boolean draw) {
        if (json.length() > MAX_JSON) throw new IllegalArgumentException("UI document is too large");
        JsonElement root = JsonParser.parseString(json);
        if (!root.isJsonObject()) throw new IllegalArgumentException("UI document must be an object");
        ValidationReport.Builder report = ValidationReport.builder();
        UiDocument result = compile(root.getAsJsonObject(), draw, "ui", "$", report);
        var validation = report.build();
        if (validation.hasErrors()) throw new IllegalArgumentException(validation.issues().get(0).path() + ": " + validation.issues().get(0).message());
        return result;
    }
    public static UiDocument compile(JsonObject json, boolean draw, String source, String path, ValidationReport.Builder report) {
        JsonAccess.knownKeys(json, Set.of("width", "height", "backdrop", "elements"), source, path, report);
        int width = JsonAccess.integer(json, "width", 480, 240, 4096, source, path, report);
        int height = JsonAccess.integer(json, "height", 270, 135, 4096, source, path, report);
        int backdrop = JsonAccess.integer(json, "backdrop", 0xA0101420, Integer.MIN_VALUE, Integer.MAX_VALUE, source, path, report);
        JsonArray array = JsonAccess.array(json, "elements", true, source, path, report);
        if (array.size() > MAX_ELEMENTS) report.error("UI_LIMIT", source, path, "At most " + MAX_ELEMENTS + " UI elements are allowed");
        List<UiDocument.Element> elements = new ArrayList<>();
        Set<String> ids = new HashSet<>();
        Set<Integer> indices = new HashSet<>();
        for (int i = 0; i < Math.min(array.size(), MAX_ELEMENTS); i++) {
            String p = path + ".elements[" + i + "]";
            if (!array.get(i).isJsonObject()) { report.error("EXPECTED_OBJECT", source, p, "Expected an element object"); continue; }
            JsonObject e = array.get(i).getAsJsonObject();
            JsonAccess.knownKeys(e, FIELDS, source, p, report);
            String id = str(e, "id", "element_" + i, 64, source, p, report);
            if (!id.matches("[a-z0-9_.-]{1,64}") || !ids.add(id)) report.error("UI_ID", source, p + ".id", "Invalid or duplicate element ID");
            String type = choice(e, "type", "panel", draw ? List.of("panel", "image", "text", "button", "offer")
                    : List.of("panel", "image", "text", "button", "slot", "details"), source, p, report);
            String anchor = choice(e, "anchor", "top_left", ANCHORS, source, p, report);
            String action = choice(e, "action", "close", draw ? DRAW_ACTIONS : BOOK_ACTIONS, source, p, report);
            String binding = choice(e, "binding", "", draw ? DRAW_BINDINGS : BOOK_BINDINGS, source, p, report);
            String texture = texture(e, "texture", source, p, report), hover = texture(e, "hoverTexture", source, p, report);
            String target = str(e, "target", "", 256, source, p, report);
            if (!target.isBlank()) JsonAccess.parseId(target, true, source, p + ".target", report);
            int index = JsonAccess.integer(e, "index", 0, 0, draw ? 4 : 255, source, p, report);
            if ((type.equals("slot") || type.equals("offer")) && !indices.add(index)) report.error("UI_INDEX", source, p, "Duplicate slot/offer index");
            elements.add(new UiDocument.Element(id, type, anchor,
                    number(e,"x",0,-4096,4096,source,p,report), number(e,"y",0,-4096,4096,source,p,report),
                    number(e,"width",100,4,4096,source,p,report), number(e,"height",24,4,4096,source,p,report),
                    JsonAccess.decimal(e,"opacity",1,0,1,source,p,report),
                    number(e,"color",0xB0223044,Integer.MIN_VALUE,Integer.MAX_VALUE,source,p,report),
                    number(e,"hoverColor",0xD04A7098,Integer.MIN_VALUE,Integer.MAX_VALUE,source,p,report),
                    number(e,"textColor",0xFFFFFFFF,Integer.MIN_VALUE,Integer.MAX_VALUE,source,p,report),
                    texture,hover,str(e,"text","",2048,source,p,report),binding,action,target,index,
                    JsonAccess.decimal(e,"textScale",1,0.5,3,source,p,report),
                    number(e,"iconSize",16,0,128,source,p,report),
                    number(e,"iconX",4,-4096,4096,source,p,report), number(e,"iconY",4,-4096,4096,source,p,report),
                    number(e,"textX",4,-4096,4096,source,p,report), number(e,"textY",4,-4096,4096,source,p,report),
                    number(e,"rarityY",18,-4096,4096,source,p,report), number(e,"bodyY",34,0,4096,source,p,report),
                    choice(e,"align","left",List.of("left","center","right"),source,p,report),
                    choice(e,"buttonStyle","custom",List.of("custom","vanilla"),source,p,report),
                    str(e,"label","",128,source,p,report),JsonAccess.bool(e,"locked",false,source,p,report)));
        }
        int required = draw ? 5 : 1;
        if (indices.size() < required) report.error("UI_SLOTS",source,path,"Provide " + (draw ? "all five offer modules" : "at least one talent slot"));
        for (int i = 0; i < indices.size(); i++) if (!indices.contains(i)) report.error("UI_INDEX",source,path,"Slot indices must be contiguous starting at zero");
        UiDocument document = new UiDocument(width, height, backdrop, elements);
        // Pack files may omit defaults; the wire representation includes every field.
        String wireJson = toJson(document).toString();
        if (json.toString().length() > MAX_JSON || wireJson.length() > MAX_JSON
                || wireJson.getBytes(java.nio.charset.StandardCharsets.UTF_8).length > MAX_WIRE_BYTES)
            report.error("UI_LIMIT",source,path,"UI JSON exceeds the network limit after applying defaults");
        return document;
    }
    private static int number(JsonObject e,String key,int fallback,int min,int max,String s,String p,ValidationReport.Builder r) {
        return JsonAccess.integer(e,key,fallback,min,max,s,p,r);
    }
    private static String str(JsonObject e,String key,String fallback,int max,String s,String p,ValidationReport.Builder r) {
        String result=JsonAccess.string(e,key,fallback,false,s,p,r);
        if(result.length()>max) { r.error("UI_TEXT_LIMIT",s,p+"."+key,"Text exceeds " + max + " characters"); return fallback; }
        return result;
    }
    private static String choice(JsonObject e,String key,String fallback,List<String> choices,String s,String p,ValidationReport.Builder r) {
        String value=str(e,key,fallback,64,s,p,r);
        if(!choices.contains(value)) { r.error("UI_ENUM",s,p+"."+key,"Expected one of " + choices); return fallback; }
        return value;
    }
    private static String texture(JsonObject e,String key,String s,String p,ValidationReport.Builder r) {
        String value=str(e,key,"",512,s,p,r);
        if(!value.isBlank()) {
            var id=JsonAccess.parseId(value,true,s,p+"."+key,r);
            if(id!=null && (!id.getPath().startsWith("textures/") || !id.getPath().endsWith(".png"))) r.error("UI_TEXTURE",s,p+"."+key,"Use namespace:textures/...png");
        }
        return value;
    }
}
