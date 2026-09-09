package com.kidxoxo.talenteditor.client;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LayoutDraftTest {
    @Test
    void parsesAndSerializesEveryEditableLayoutField() {
        LayoutDraft draft = LayoutDraft.parse("""
                {
                  "schema": 2,
                  "enabled": false,
                  "priority": 7,
                  "background": "example:textures/gui/old.png",
                  "panelPercent": 45,
                  "columns": 3,
                  "rows": 5,
                  "slotWidth": 40,
                  "slotHeight": 30,
                  "iconSize": 18,
                  "showGeneratedEffects": false,
                  "slots": [
                    {"id":"featured","x":4,"y":6,"width":72,"height":34,"iconSize":20}
                  ],
                  "pages": [
                    {"id":"opening","background":"example:textures/gui/opening.png","slots":[
                      {"id":"hero","x":8,"y":4,"width":80,"height":40,"iconSize":24}
                    ]}
                  ]
                }
                """);

        assertFalse(draft.enabled());
        assertEquals(7, draft.priority());
        assertEquals(15, draft.columns() * draft.rows());
        assertEquals(1, draft.slots().size());
        assertEquals("featured", draft.slots().get(0).id());
        assertEquals(1, draft.pages().size());
        assertEquals("opening", draft.pages().get(0).id());
        JsonObject changed = JsonParser.parseString(draft.toJson(
                true, 20, "example:textures/gui/new.png", 52,
                4, 6, 36, 28, 16, true,
                java.util.List.of(
                        new LayoutDraft.SlotDraft("left", 0, 0, 48, 28, 16),
                        new LayoutDraft.SlotDraft("right", 52, 5, 64, 34, 20)
                ),
                java.util.List.of(
                        new LayoutDraft.PageDraft("combat", "example:textures/gui/combat.png",
                                java.util.List.of(new LayoutDraft.SlotDraft(
                                        "large", 2, 3, 96, 46, 28
                                )))
                )
        )).getAsJsonObject();
        assertTrue(changed.get("enabled").getAsBoolean());
        assertEquals(20, changed.get("priority").getAsInt());
        assertEquals("example:textures/gui/new.png", changed.get("background").getAsString());
        assertEquals(4, changed.get("columns").getAsInt());
        assertTrue(changed.get("showGeneratedEffects").getAsBoolean());
        assertEquals(2, changed.getAsJsonArray("slots").size());
        assertEquals("right", changed.getAsJsonArray("slots").get(1).getAsJsonObject()
                .get("id").getAsString());
        assertEquals("combat", changed.getAsJsonArray("pages").get(0).getAsJsonObject()
                .get("id").getAsString());
        assertEquals(96, changed.getAsJsonArray("pages").get(0).getAsJsonObject()
                .getAsJsonArray("slots").get(0).getAsJsonObject().get("width").getAsInt());
    }

    @Test
    void omitsSlotsToPreserveLegacyGridMode() {
        LayoutDraft draft = LayoutDraft.create();

        JsonObject output = JsonParser.parseString(draft.toJson(
                true, 0, "", 42, 2, 4, 58, 30, 16, true,
                java.util.List.of(), java.util.List.of()
        )).getAsJsonObject();

        assertFalse(output.has("slots"));
        assertFalse(output.has("pages"));
    }

    @Test
    void bindingSurvivesGeometryAndSerializationAndCanBeCleared() {
        LayoutDraft draft = LayoutDraft.parse("""
                {"schema":2,"pages":[{"id":"one","slots":[
                  {"id":"hero","talent":"example:vitality","x":0,"y":0,"width":80,"height":40,"iconSize":24}
                ]}]}
                """);
        var moved = draft.pages().get(0).slots().get(0).withGeometry(10, 12, 100, 48, 28);
        assertEquals("example:vitality", moved.talent());
        for (var slot : java.util.List.of(moved, moved.withTalent(""))) {
            String json = draft.toJson(true, 0, "", 42, 2, 4, 58, 30, 16, true,
                    java.util.List.of(), java.util.List.of(new LayoutDraft.PageDraft(
                            "one", "", java.util.List.of(slot))));
            var restored = LayoutDraft.parse(json).pages().get(0).slots().get(0);
            assertEquals(slot, restored);
            assertEquals(!slot.talent().isBlank(), restored.definition().talent().isPresent());
            assertEquals(!slot.talent().isBlank(), JsonParser.parseString(json).getAsJsonObject()
                    .getAsJsonArray("pages").get(0).getAsJsonObject()
                    .getAsJsonArray("slots").get(0).getAsJsonObject().has("talent"));
        }
    }
}
