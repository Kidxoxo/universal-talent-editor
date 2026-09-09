package com.kidxoxo.talenteditor.client;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;

class RarityDraftTest {
    @Test
    void editsVisualFieldsAndPreservesLocalizedStyleAndUnrepresentedFields() {
        RarityDraft draft = RarityDraft.parse("""
                {
                  "schema": 2,
                  "enabled": false,
                  "name": {"literal": "Old Epic", "bold": true},
                  "color": "#AA55FF",
                  "order": 10,
                  "offerable": false,
                  "baseWeight": 2,
                  "futureDisplay": {"glow": true}
                }
                """);

        assertFalse(draft.enabled());
        assertEquals("#AA55FF", draft.color());
        JsonObject output = JsonParser.parseString(draft.toJson(
                "Epic", true, "#55AAFF", 20, true, 5.5D
        )).getAsJsonObject();

        assertEquals("Epic", output.getAsJsonObject("name").get("literal").getAsString());
        assertEquals(true, output.getAsJsonObject("name").get("bold").getAsBoolean());
        assertEquals("#55AAFF", output.get("color").getAsString());
        assertEquals(20, output.get("order").getAsInt());
        assertEquals(5.5D, output.get("baseWeight").getAsDouble());
        assertEquals(true, output.getAsJsonObject("futureDisplay").get("glow").getAsBoolean());
    }

    @Test
    void parsesRgbAndArgbColorsAndRejectsMalformedValues() {
        assertEquals(0xFF12AB34, RarityDraft.tryParseColor("#12ab34"));
        assertEquals(0x8012AB34, RarityDraft.tryParseColor("8012AB34"));
        assertEquals("#12AB34", RarityDraft.formatColor(0xFF12AB34));
        assertEquals("#8012AB34", RarityDraft.formatColor(0x8012AB34));
        assertNull(RarityDraft.tryParseColor("blue"));
    }
}
