package com.kidxoxo.talenteditor.compile;

import com.google.gson.*;
import com.kidxoxo.talenteditor.domain.*;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class UiDocumentCodecTest {
    static JsonObject minimal(){return JsonParser.parseString("""
            {"elements":[{"id":"slot","type":"slot","index":0}]}
            """).getAsJsonObject();}
    @Test void roundTripRetainsOpacityTexturesAndButtonActions(){
        JsonObject json=minimal();json.getAsJsonArray("elements").add(JsonParser.parseString("""
                {"id":"draw","type":"button","action":"draw","target":"pack:special",
                 "opacity":0.25,"texture":"pack:textures/ui/normal.png","hoverTexture":"pack:textures/ui/hover.png",
                 "anchor":"bottom_right","x":-8,"y":-8,"textX":12,"textY":6,"color":-2147483648}
                """));
        UiDocument ui=UiDocumentCodec.read(json.toString(),false);
        assertEquals(ui,UiDocumentCodec.read(UiDocumentCodec.toJson(ui).toString(),false));
        assertEquals(0.25,ui.elements().get(1).opacity());
        assertEquals(new UiDocument.Rect(372,238,100,24),ui.elements().get(1).bounds(480,270));
    }
    @Test void rejectsUnknownFieldsAndUnsafeOrWrongScreenActions(){
        for(String action:List.of("run_command","open_url","confirm")){
            var root=minimal();var e=JsonParser.parseString("{\"id\":\"b\",\"type\":\"button\"}").getAsJsonObject();e.addProperty("action",action);root.getAsJsonArray("elements").add(e);
            assertThrows(IllegalArgumentException.class,()->UiDocumentCodec.read(root.toString(),false));
        }
        var root=minimal();root.addProperty("typo",1);assertThrows(IllegalArgumentException.class,()->UiDocumentCodec.read(root.toString(),false));
    }
    @Test void rejectsDuplicateIdsIndexesGapsAndOutOfRangeGeometry(){
        for(String field:List.of("opacity","width","index")){
            var root=minimal();root.getAsJsonArray("elements").get(0).getAsJsonObject().addProperty(field,99999);
            assertThrows(IllegalArgumentException.class,()->UiDocumentCodec.read(root.toString(),false));
        }
        var duplicate=minimal();duplicate.getAsJsonArray("elements").add(duplicate.getAsJsonArray("elements").get(0).deepCopy());
        assertThrows(IllegalArgumentException.class,()->UiDocumentCodec.read(duplicate.toString(),false));
        var gap=minimal();gap.getAsJsonArray("elements").get(0).getAsJsonObject().addProperty("index",1);
        assertThrows(IllegalArgumentException.class,()->UiDocumentCodec.read(gap.toString(),false));
    }
    @Test void rejectsMalformedTypesAndExternalImagePaths(){
        for(String text:List.of("null","[]","{\"elements\":null}","{\"elements\":[3]}"))
            assertThrows(IllegalArgumentException.class,()->UiDocumentCodec.read(text,false));
        for(String path:List.of("https://example.com/x.png","C:/secret.png","pack:sounds/test.ogg")){
            var root=minimal();root.getAsJsonArray("elements").get(0).getAsJsonObject().addProperty("texture",path);
            assertThrows(IllegalArgumentException.class,()->UiDocumentCodec.read(root.toString(),false));
        }
    }
    @Test void rejectsExcessiveElementCountAndText(){
        var root=minimal();var entries=root.getAsJsonArray("elements");
        for(int i=0;i<UiDocumentCodec.MAX_ELEMENTS;i++){var e=new JsonObject();e.addProperty("id","p"+i);e.addProperty("type","panel");entries.add(e);}
        assertThrows(IllegalArgumentException.class,()->UiDocumentCodec.read(root.toString(),false));
        var longText=minimal();longText.getAsJsonArray("elements").get(0).getAsJsonObject().addProperty("text","x".repeat(2049));
        String json=longText.toString();assertThrows(IllegalArgumentException.class,()->UiDocumentCodec.read(json,false));
    }
    @Test void drawRequiresFiveUniqueOfferModules(){
        assertThrows(IllegalArgumentException.class,()->UiDocumentCodec.read(minimal().toString(),true));
        var root=new JsonObject();var entries=new JsonArray();root.add("elements",entries);
        for(int i=0;i<5;i++){var e=new JsonObject();e.addProperty("id","offer"+i);e.addProperty("type","offer");e.addProperty("index",i);entries.add(e);}
        assertEquals(5,UiDocumentCodec.read(root.toString(),true).elements().size());
        entries.remove(4);assertThrows(IllegalArgumentException.class,()->UiDocumentCodec.read(root.toString(),true));
    }
    @Test void compilerKeepsLegacyLayoutsAndRejectsInvisibleConfiguredSlots(){
        var id=new ResourceLocation("test:layout");
        var legacy=JsonParser.parseString("{\"schema\":2}").getAsJsonObject();
        var compiler=TalentCompiler.withBuiltins();var environment=new CompilationEnvironment(Set.of("minecraft","forge","talent_editor"),Set.of());
        var old=compiler.compile(RawDefinitionSet.empty().withLayout(id,legacy,"test"),environment);
        assertFalse(old.report().hasErrors(),old.report().issues().toString());
        assertTrue(old.candidate().orElseThrow().layouts().get(id).bookUi().isEmpty());
        legacy.add("bookUi",minimal());
        legacy.add("pages",JsonParser.parseString("""
                [{"id":"page","slots":[{"id":"a","x":0,"y":0,"width":60,"height":30,"iconSize":16},
                {"id":"b","x":70,"y":0,"width":60,"height":30,"iconSize":16}]}]
                """));
        var invalid=compiler.compile(RawDefinitionSet.empty().withLayout(id,legacy,"test"),environment);
        assertTrue(invalid.report().issues().stream().anyMatch(i->i.code().equals("UI_PAGE_CAPACITY")));
    }
    @Test void viewportHitTestingUsesSameScaleAndLetterboxAsDrawing(){
        var ui=UiDocumentCodec.read(minimal().toString(),false);
        var view=UiDocument.Viewport.fit(ui,10,20,960,600);
        assertEquals(2,view.scale());assertEquals(10,view.x());assertEquals(50,view.y());
        assertEquals(100,view.localX(210));assertEquals(100,view.localY(250));
    }
    @Test void rejectsSmallPackJsonThatExpandsBeyondTheWireLimit(){
        var root=minimal();var entries=root.getAsJsonArray("elements");
        for(int i=0;i<319;i++){
            var e=new JsonObject();e.addProperty("id","t"+i);e.addProperty("type","text");e.addProperty("text","x".repeat(500));entries.add(e);
        }
        assertTrue(root.toString().length()<UiDocumentCodec.MAX_JSON);
        assertThrows(IllegalArgumentException.class,()->UiDocumentCodec.read(root.toString(),false));
    }
    @Test void compilerRejectsMissingButtonPoolAndIndexesExistingTarget(){
        var layout=JsonParser.parseString("{\"schema\":2}").getAsJsonObject();var ui=minimal();layout.add("bookUi",ui);
        ui.getAsJsonArray("elements").add(JsonParser.parseString("{\"id\":\"draw\",\"type\":\"button\",\"action\":\"draw\",\"target\":\"test:pool\"}"));
        var id=new ResourceLocation("test:layout");var pool=new ResourceLocation("test:pool");
        var raw=RawDefinitionSet.empty().withLayout(id,layout,"test");
        var environment=new CompilationEnvironment(Set.of("minecraft","forge","talent_editor"),Set.of());
        var compiler=TalentCompiler.withBuiltins();
        assertTrue(compiler.compile(raw,environment).report().hasErrors());
        raw=raw.withPool(pool,JsonParser.parseString("{\"schema\":2,\"name\":{\"literal\":\"Pool\"}}").getAsJsonObject(),"test");
        var valid=compiler.compile(raw,environment);
        assertFalse(valid.report().hasErrors(),valid.report().issues().toString());
        assertTrue(valid.candidate().orElseThrow().referencesTo(DefinitionKind.POOL,pool).stream().anyMatch(r->r.usage().equals("ui_draw_pool")&&r.sourceId().equals(id)));
    }
    @Test void countsUtf8BytesForLongLocalizedText(){
        var root=minimal();var entries=root.getAsJsonArray("elements");
        for(int i=0;i<100;i++){
            var e=new JsonObject();e.addProperty("id","t"+i);e.addProperty("type","text");e.addProperty("text","天".repeat(1000));entries.add(e);
        }
        assertTrue(root.toString().length()<UiDocumentCodec.MAX_JSON);
        assertThrows(IllegalArgumentException.class,()->UiDocumentCodec.read(root.toString(),false));
    }
}
