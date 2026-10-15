package com.kidxoxo.talenteditor.client;

import com.kidxoxo.talenteditor.compile.UiDocumentCodec;
import com.kidxoxo.talenteditor.domain.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class UiEditorTest {
    @Test void defaultTemplatesAreValidForBothScreens(){
        var book=UiTemplates.create(false,TalentBookLayoutDefinition.DEFAULT);
        var draw=UiTemplates.create(true,TalentBookLayoutDefinition.DEFAULT);
        assertEquals(6,book.slotCount());
        assertEquals(5,draw.elements().stream().filter(e->e.type().equals("offer")).count());
        assertEquals(book,UiDocumentCodec.read(UiDocumentCodec.toJson(book).toString(),false));
    }
    @Test void undoRedoBranchingDoesNotResurrectDiscardedFuture(){
        var original=UiTemplates.create(false,TalentBookLayoutDefinition.DEFAULT);
        var history=new UiEditHistory(original);
        var a=new UiDocument(640,360,0,original.elements());var b=new UiDocument(800,450,0,original.elements());
        history.commit(a);history.commit(a);history.undo();assertEquals(original,history.value());
        history.redo();assertEquals(a,history.value());history.undo();history.commit(b);history.redo();assertEquals(b,history.value());
        history.undo();assertEquals(original,history.value());
    }
    @Test void legacyDraftRoundTripPreservesNewUi(){
        var doc=UiTemplates.create(false,TalentBookLayoutDefinition.DEFAULT);
        var root=new com.google.gson.JsonObject();root.addProperty("schema",2);root.add("bookUi",UiDocumentCodec.toJson(doc));
        var draft=LayoutDraft.parse(root.toString());assertEquals(doc,draft.ui(false).orElseThrow());
        String json=draft.toJson(true,0,"",60,2,3,120,56,48,true,java.util.List.of(),java.util.List.of());
        assertEquals(doc,LayoutDraft.parse(json).ui(false).orElseThrow());
    }
    @Test void snapHandlesNegativeCoordinates(){assertEquals(-8,UiEditHistory.snap(-9,4));assertEquals(12,UiEditHistory.snap(11,4));assertEquals(11,UiEditHistory.snap(11,1));}
    @Test void bookBaseUsesExistingTwoColumnGeometryAndNativeControls(){
        var ui=UiTemplates.create(false,TalentBookLayoutDefinition.DEFAULT);
        var slots=ui.elements().stream().filter(e->e.type().equals("slot")).toList();
        assertEquals(new UiDocument.Rect(9,49,117,53),slots.get(0).bounds(480,270));
        assertEquals(129,slots.get(1).x());assertEquals(105,slots.get(2).y());assertEquals(48,slots.get(0).iconSize());
        assertTrue(ui.elements().stream().filter(e->e.type().equals("button")).allMatch(e->e.buttonStyle().equals("vanilla")&&e.align().equals("center")));
        assertEquals(301,ui.elements().stream().filter(e->e.type().equals("details")).findFirst().orElseThrow().x());
    }
    @Test void conversionPreservesFirstPageFreeSlotsBackgroundAndEnoughCapacity(){
        var id=new net.minecraft.resources.ResourceLocation("test:layout");
        var bg=new net.minecraft.resources.ResourceLocation("test:textures/book.png");
        var page=new TalentBookLayoutDefinition.PageDefinition("first",java.util.Optional.empty(),java.util.List.of(
                new TalentBookLayoutDefinition.SlotDefinition("special",20,10,90,40,24)));
        var layout=new TalentBookLayoutDefinition(id,2,true,0,java.util.Optional.of(bg),60,2,3,120,56,48,true,java.util.List.of(),java.util.List.of(page));
        var ui=UiTemplates.create(false,layout);
        var slot=ui.elements().stream().filter(e->e.type().equals("slot")).findFirst().orElseThrow();
        assertEquals(new UiDocument.Rect(29,59,90,40),slot.bounds(480,270));assertEquals(24,slot.iconSize());assertEquals(6,ui.slotCount());
        var background=ui.elements().get(0);assertEquals(bg.toString(),background.texture());assertEquals("page_background",background.binding());assertEquals(1,background.opacity());
    }
    @Test void defaultControlsFollowAutomaticSynthesisSetting(){
        var automatic=UiTemplates.create(false,TalentBookLayoutDefinition.DEFAULT,true);
        assertFalse(automatic.elements().stream().anyMatch(e->e.action().equals("synthesize")));
        assertEquals(3,automatic.elements().stream().filter(e->e.type().equals("button")&&e.y()==242).count());
        assertTrue(UiTemplates.create(false,TalentBookLayoutDefinition.DEFAULT,false).elements().stream().anyMatch(e->e.action().equals("synthesize")));
    }
    @Test void oldUiDocumentsKeepTheirCustomStyleWhenNewFieldsAreAbsent(){
        var ui=UiDocumentCodec.read("{\"elements\":[{\"type\":\"slot\"},{\"id\":\"b\",\"type\":\"button\"}]}",false);
        assertEquals("custom",ui.elements().get(1).buttonStyle());assertEquals("left",ui.elements().get(1).align());
        var root=UiDocumentCodec.toJson(ui);root.getAsJsonArray("elements").get(1).getAsJsonObject().addProperty("align","bad");
        assertThrows(IllegalArgumentException.class,()->UiDocumentCodec.read(root.toString(),false));
    }
    @Test void contentPageTemplatesPassTheAuthoritativeCompiler(){
        var draft=LayoutDraft.create();
        var base=java.util.List.of(new LayoutDraft.SlotDraft("old",0,0,120,56,48));
        for(int capacity:java.util.List.of(6,64,256)){
            var slots=LayoutContentSlots.template(base,capacity);
            assertEquals(capacity,slots.size());assertEquals(base.get(0),slots.get(0));
            String json=draft.toJson(true,0,"",60,2,3,120,56,48,true,java.util.List.of(),
                    java.util.List.of(new LayoutDraft.PageDraft("new_page","",slots)));
            var root=com.google.gson.JsonParser.parseString(json).getAsJsonObject();
            if(capacity==6)root.add("bookUi",UiDocumentCodec.toJson(UiTemplates.create(false,TalentBookLayoutDefinition.DEFAULT)));
            var result=com.kidxoxo.talenteditor.compile.TalentCompiler.withBuiltins().compile(
                    com.kidxoxo.talenteditor.compile.RawDefinitionSet.empty().withLayout(new net.minecraft.resources.ResourceLocation("test:layout"),root,"test"),
                    new com.kidxoxo.talenteditor.compile.CompilationEnvironment(java.util.Set.of("minecraft","forge","talent_editor"),java.util.Set.of()));
            assertFalse(result.report().hasErrors(),result.report().issues().toString());
        }
    }
    @Test void appendingContentKeepsExistingGeometryBindingsAndUniqueIds(){
        var existing=new LayoutDraft.SlotDraft("slot_0",0,0,120,56,48,"test:fixed");
        var slots=new java.util.ArrayList<>(java.util.List.of(existing));
        slots.add(LayoutContentSlots.next(slots).orElseThrow());
        assertEquals(existing,slots.get(0));assertEquals("test:fixed",slots.get(0).talent());
        assertNotEquals(existing.id(),slots.get(1).id());
        assertTrue(slots.get(1).x()>=120||slots.get(1).y()>=56);
        assertTrue(slots.get(1).talent().isEmpty());
        assertTrue(LayoutContentSlots.next(LayoutContentSlots.template(java.util.List.of(),256)).isEmpty());
    }
}
