package com.kidxoxo.talenteditor.client;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class EditorTextEscapesTest {
    @Test
    void multilineEditorEscapesNewlinesAndLiteralBackslashes() {
        String value = "first line\npath \\ value\nlast";
        String encoded = EditorTextEscapes.encodeMultiline(value);

        assertEquals("first line\\npath \\\\ value\\nlast", encoded);
        assertEquals(value, EditorTextEscapes.decodeMultiline(encoded));
    }

    @Test
    void unknownEscapeSequencesRemainLiteral() {
        assertEquals("a\\tb", EditorTextEscapes.decodeMultiline("a\\tb"));
    }
}
