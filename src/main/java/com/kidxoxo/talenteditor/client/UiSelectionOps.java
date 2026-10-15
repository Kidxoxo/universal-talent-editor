package com.kidxoxo.talenteditor.client;

import com.google.gson.*;
import com.kidxoxo.talenteditor.compile.UiDocumentCodec;
import com.kidxoxo.talenteditor.domain.UiDocument;
import java.util.*;

/** All operations use visible bounds, then convert back to each element's original anchor. */
final class UiSelectionOps {
    enum Align { LEFT, CENTER, RIGHT, TOP, MIDDLE, BOTTOM, HORIZONTAL, VERTICAL }
    static UiDocument move(UiDocument doc,Set<String> ids,int dx,int dy,boolean draw){
        var chosen=doc.elements().stream().filter(e->ids.contains(e.id())&&!e.locked()).toList();
        if(chosen.isEmpty())return doc;
        int x=Math.max(chosen.stream().mapToInt(e->-4096-e.x()).max().orElse(0),Math.min(dx,chosen.stream().mapToInt(e->4096-e.x()).min().orElse(0)));
        int y=Math.max(chosen.stream().mapToInt(e->-4096-e.y()).max().orElse(0),Math.min(dy,chosen.stream().mapToInt(e->4096-e.y()).min().orElse(0)));
        JsonObject root=UiDocumentCodec.toJson(doc);for(int i=0;i<doc.elements().size();i++){var e=doc.elements().get(i);if(!ids.contains(e.id())||e.locked())continue;var v=root.getAsJsonArray("elements").get(i).getAsJsonObject();v.addProperty("x",e.x()+x);v.addProperty("y",e.y()+y);}
        return UiDocumentCodec.read(root.toString(),draw);
    }
    static UiDocument align(UiDocument doc,Set<String> ids,Align mode,boolean draw){
        var chosen=new ArrayList<>(doc.elements().stream().filter(e->ids.contains(e.id())&&!e.locked()).toList());if(chosen.isEmpty())return doc;
        boolean distribute=mode==Align.HORIZONTAL||mode==Align.VERTICAL;boolean vertical=mode==Align.VERTICAL;
        if(distribute&&chosen.size()<3)return doc;
        int left=chosen.size()==1?0:chosen.stream().mapToInt(e->e.bounds(doc.width(),doc.height()).x()).min().orElse(0);
        int right=chosen.size()==1?doc.width():chosen.stream().mapToInt(e->e.bounds(doc.width(),doc.height()).right()).max().orElse(0);
        int top=chosen.size()==1?0:chosen.stream().mapToInt(e->e.bounds(doc.width(),doc.height()).y()).min().orElse(0);
        int bottom=chosen.size()==1?doc.height():chosen.stream().mapToInt(e->e.bounds(doc.width(),doc.height()).bottom()).max().orElse(0);
        if(distribute)chosen.sort(Comparator.comparingInt(e->vertical?e.bounds(doc.width(),doc.height()).y():e.bounds(doc.width(),doc.height()).x()));
        double cursor=vertical?top:left;
        double gap=distribute?((vertical?bottom-top:right-left)-chosen.stream().mapToInt(e->vertical?e.height():e.width()).sum())/(double)(chosen.size()-1):0;
        JsonObject root=UiDocumentCodec.toJson(doc);
        for(var e:chosen){var r=e.bounds(doc.width(),doc.height());int x=r.x(),y=r.y();
            switch(mode){case LEFT->x=left;case CENTER->x=(left+right-e.width())/2;case RIGHT->x=right-e.width();case TOP->y=top;case MIDDLE->y=(top+bottom-e.height())/2;case BOTTOM->y=bottom-e.height();case HORIZONTAL->{x=(int)Math.round(cursor);cursor+=e.width()+gap;}case VERTICAL->{y=(int)Math.round(cursor);cursor+=e.height()+gap;}}
            var v=root.getAsJsonArray("elements").get(doc.elements().indexOf(e)).getAsJsonObject();v.addProperty("x",e.x()+x-r.x());v.addProperty("y",e.y()+y-r.y());
        }
        return UiDocumentCodec.read(root.toString(),draw);
    }
    static final Set<String> STYLE=Set.of("opacity","color","hoverColor","textColor","texture","hoverTexture","textScale","iconSize","iconX","iconY","textX","textY","rarityY","bodyY","align","buttonStyle");
    static UiDocument pasteStyle(UiDocument doc,Set<String> ids,JsonObject style,boolean draw){
        JsonObject root=UiDocumentCodec.toJson(doc);for(int i=0;i<doc.elements().size();i++){var e=doc.elements().get(i);if(!ids.contains(e.id())||e.locked())continue;var object=root.getAsJsonArray("elements").get(i).getAsJsonObject();for(String key:STYLE)if(style.has(key))object.add(key,style.get(key).deepCopy());}
        return UiDocumentCodec.read(root.toString(),draw);
    }
}
