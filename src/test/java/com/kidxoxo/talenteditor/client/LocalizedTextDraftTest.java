package com.kidxoxo.talenteditor.client;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LocalizedTextDraftTest {
    @Test
    void parsesAndSerializesLiteralTranslationColorAndFontStyles() {
        LocalizedTextDraft draft = LocalizedTextDraft.parse(JsonParser.parseString("""
                {
                  "literal": "Fallback",
                  "translation": "talent.example.name",
                  "color": "#55AAFF",
                  "bold": true,
                  "italic": true
                }
                """));

        assertEquals("Fallback", draft.fallbackText());
        assertEquals(0xFF55AAFF, draft.color().orElseThrow());
        assertTrue(draft.bold());
        assertTrue(draft.italic());
        JsonObject output = draft.toJson();
        assertEquals("talent.example.name", output.get("translation").getAsString());
        assertEquals("#55AAFF", output.get("color").getAsString());
        assertTrue(output.get("bold").getAsBoolean());
    }
}
