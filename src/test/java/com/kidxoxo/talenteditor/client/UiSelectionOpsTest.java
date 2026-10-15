package com.kidxoxo.talenteditor.client;

import com.google.gson.JsonParser;
import com.kidxoxo.talenteditor.compile.UiDocumentCodec;
import com.kidxoxo.talenteditor.domain.UiDocument;
import org.junit.jupiter.api.Test;
import java.util.Set;
import static org.junit.jupiter.api.Assertions.*;

class UiSelectionOpsTest {
    private UiDocument document(){return UiDocumentCodec.read("""
        {"width":480,"height":270,"elements":[
          {"id":"slot","type":"slot","index":0},
          {"id":"a","type":"button","x":10,"y":20,"width":40,"height":20,"action":"draw","target":"test:pool","label":"Open pool"},
          {"id":"b","type":"panel","x":-20,"y":-30,"width":60,"height":30,"anchor":"bottom_right"},
          {"id":"c","type":"image","x":200,"y":150,"width":80,"height":40},
          {"id":"locked","type":"panel","x":9,"y":9,"locked":true}]}
        """,false);}
    @Test void alignmentPreservesAnchorsAndLockedElements() {
        var original=document();var next=UiSelectionOps.align(original,Set.of("a","b","locked"),UiSelectionOps.Align.LEFT,false);
        assertEquals(10,next.elements().get(2).bounds(480,270).x());assertEquals("bottom_right",next.elements().get(2).anchor());
        assertEquals(original.elements().get(4),next.elements().get(4));
    }
    @Test void distributionAccountsForDifferentWidthsAndKeepsOuterEdges() {
        var result=UiSelectionOps.align(document(),Set.of("a","b","c"),UiSelectionOps.Align.HORIZONTAL,false);
        var a=result.elements().get(1).bounds(480,270);var b=result.elements().get(2).bounds(480,270);var c=result.elements().get(3).bounds(480,270);
        assertEquals(10,a.x());assertEquals(460,b.right());assertEquals(c.x()-a.right(),b.x()-c.right());
    }
    @Test void moveClampsTheWholeGroupAndSkipsLockedElements() {
        var original=document();var next=UiSelectionOps.move(original,Set.of("a","c","locked"),5000,-5000,false);
        assertEquals(4096,next.elements().get(3).x());
        assertEquals(190,next.elements().get(3).x()-next.elements().get(1).x());
        assertEquals(-4096,next.elements().get(1).y());assertEquals(original.elements().get(4),next.elements().get(4));
    }
    @Test void pasteStyleCannotOverwriteBindingsPositionOrLocks() {
        var original=document();var result=UiSelectionOps.pasteStyle(original,Set.of("a","locked"),JsonParser.parseString("""
          {"color":-1,"opacity":0.4,"x":1000,"action":"close","target":"bad:pool","locked":true,"label":"bad"}
          """).getAsJsonObject(),false);
        var element=result.elements().get(1);assertEquals(0.4,element.opacity());assertEquals(10,element.x());
        assertEquals("draw",element.action());assertEquals("test:pool",element.target());assertEquals("Open pool",element.label());assertFalse(element.locked());
        assertEquals(original.elements().get(4),result.elements().get(4));
    }
    @Test void editorMetadataRoundTripsAndOlderElementsRemainUnlocked() {
        var doc=document();assertEquals(doc,UiDocumentCodec.read(UiDocumentCodec.toJson(doc).toString(),false));
        assertFalse(doc.elements().get(0).locked());assertEquals("",doc.elements().get(0).label());
    }
}
