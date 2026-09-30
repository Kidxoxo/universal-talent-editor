package com.kidxoxo.talenteditor.domain;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ParticleTintTest {
    @Test void interpolatesOptionalColorsAndRotatesHue() {
        assertEquals(-1, ParticleTint.parse(""));
        assertEquals(0x800080, ParticleTint.at(0xFF0000, 0x0000FF, 0.5));
        assertEquals(0xFF0000, ParticleTint.at(0xFF0000, -1, 0.75));
        assertEquals(0x00FF00, ParticleTint.rotateHue(0xFF0000, 120));
    }

    @Test void rejectsInvalidHexColor() {
        assertThrows(IllegalArgumentException.class, () -> ParticleTint.parse("#GG00FF"));
    }
}
