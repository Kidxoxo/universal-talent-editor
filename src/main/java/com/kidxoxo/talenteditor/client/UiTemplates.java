package com.kidxoxo.talenteditor.client;

import com.google.gson.*;
import com.kidxoxo.talenteditor.compile.UiDocumentCodec;
import com.kidxoxo.talenteditor.domain.*;
import java.util.*;

/** Converts the existing book geometry into editable modules at a 480 x 270 reference size. */
final class UiTemplates {
    private UiTemplates() {}
    static UiDocument create(boolean draw, TalentBookLayoutDefinition layout) { return create(draw,layout,false); }
    static UiDocument create(boolean draw, TalentBookLayoutDefinition layout, boolean automaticSynthesis) {
        JsonObject root=new JsonObject();root.addProperty("width",480);root.addProperty("height",270);
        root.addProperty("backdrop",0xA0101010);
        JsonArray a=new JsonArray();root.add("elements",a);
        JsonObject bg=element("background","image",0,0,480,270);
        bg.addProperty("texture",layout.background().map(Object::toString).orElse(""));bg.addProperty("color",0);
        if(!draw)bg.addProperty("binding","page_background");a.add(bg);
        text(a,"title",0,10,480,12,"title",true);
        text(a,"summary",0,25,480,12,"summary",true);
        if(draw) {
            text(a,"mechanics",12,38,456,12,"mechanics",true);
            text(a,"status",12,50,456,12,"status",true);
            for(int i=0;i<5;i++) {
                JsonObject e=element("offer_"+i,"offer",12+i*92,63,85,168);
                e.addProperty("index",i);e.addProperty("iconSize",32);e.addProperty("iconX",26);e.addProperty("iconY",10);
                e.addProperty("textY",47);e.addProperty("rarityY",61);e.addProperty("bodyY",79);
                e.addProperty("color",0x30202020);e.addProperty("hoverColor",0x8050A0E8);a.add(e);
            }
            bottomButtons(a,List.of("refresh","confirm","close"));
        } else {
            int divider=Math.max(145,480*layout.panelPercent()/100);
            JsonObject line=element("divider","panel",divider,42,4,192);line.addProperty("width",4);line.addProperty("color",0x60808080);a.add(line);
            // Preserve the first configured page, then the old free-slot template, then the regular grid.
            List<TalentBookLayoutDefinition.SlotDefinition> slots=layout.pages().isEmpty()?layout.slots():layout.pages().get(0).slots();
            int count=Math.max(layout.pageCapacity(),layout.pages().stream().mapToInt(page->page.slots().size()).max().orElse(0));
            int columns=layout.columns(),rows=(count+columns-1)/columns;
            double gridScale=Math.min(1,Math.min((divider-16.0)/(columns*layout.slotWidth()),182.0/(rows*layout.slotHeight())));
            double freeScale=slots.isEmpty()?1:Math.min(1,Math.min((divider-16.0)/slots.stream().mapToInt(s->s.x()+s.width()).max().orElse(1),182.0/slots.stream().mapToInt(s->s.y()+s.height()).max().orElse(1)));
            for(int i=0;i<count;i++) {
                int x,y,w,h,icon;
                if(i<slots.size()) {
                    var s=slots.get(i);x=9+(int)Math.round(s.x()*freeScale);y=49+(int)Math.round(s.y()*freeScale);
                    w=Math.max(4,(int)Math.round(s.width()*freeScale));h=Math.max(4,(int)Math.round(s.height()*freeScale));icon=Math.max(0,(int)Math.round(s.iconSize()*freeScale));
                } else {
                    x=9+(int)Math.round((i%columns)*layout.slotWidth()*gridScale);y=49+(int)Math.round((i/columns)*layout.slotHeight()*gridScale);
                    w=Math.max(4,(int)Math.round(layout.slotWidth()*gridScale)-3);h=Math.max(4,(int)Math.round(layout.slotHeight()*gridScale)-3);icon=Math.max(0,(int)Math.round(layout.iconSize()*gridScale));
                }
                JsonObject e=element("slot_"+i,"slot",x,y,w,h);e.addProperty("index",i);
                e.addProperty("color",0);e.addProperty("hoverColor",0x704A90E2);
                e.addProperty("iconSize",Math.min(128,icon));e.addProperty("iconX",7);e.addProperty("iconY",Math.max(2,(h-icon)/2));
                e.addProperty("textX",11+icon);e.addProperty("textY",3);e.addProperty("rarityY",14);a.add(e);
            }
            JsonObject detail=element("details","details",divider+13,50,480-divider-27,184);
            detail.addProperty("color",0);detail.addProperty("hoverColor",0);detail.addProperty("iconSize",16);detail.addProperty("iconX",0);detail.addProperty("iconY",0);
            detail.addProperty("textX",22);detail.addProperty("textY",0);detail.addProperty("rarityY",14);detail.addProperty("bodyY",34);a.add(detail);
            text(a,"page",58,26,120,12,"page",false);
            button(a,"previous",8,20,20,"previous");button(a,"next",32,20,20,"next");
            bottomButtons(a,automaticSynthesis?List.of("mode","draw","close"):List.of("mode","synthesize","draw","close"));
        }
        return UiDocumentCodec.read(root.toString(),draw);
    }
    static JsonObject element(String id,String type,int x,int y,int w,int h) {
        JsonObject e=new JsonObject();e.addProperty("id",id);e.addProperty("type",type);e.addProperty("x",x);e.addProperty("y",y);e.addProperty("width",w);e.addProperty("height",h);return e;
    }
    private static void text(JsonArray a,String id,int x,int y,int w,int h,String binding,boolean centered) {
        JsonObject e=element(id,"text",x,y,w,h);e.addProperty("binding",binding);e.addProperty("color",0);e.addProperty("textX",0);e.addProperty("textY",0);e.addProperty("align",centered?"center":"left");a.add(e);
    }
    private static void bottomButtons(JsonArray a,List<String> actions) {
        int available=464-6*(actions.size()-1),w=available/actions.size(),extra=available%actions.size(),x=8;
        for(int i=0;i<actions.size();i++){int allocated=w+(i<extra?1:0);button(a,actions.get(i),x,242,allocated,actions.get(i));x+=allocated+6;}
    }
    private static void button(JsonArray a,String id,int x,int y,int w,String action) {
        JsonObject e=element(id,"button",x,y,w,20);e.addProperty("action",action);e.addProperty("buttonStyle","vanilla");e.addProperty("align","center");e.addProperty("textX",0);e.addProperty("textY",6);a.add(e);
    }
}
