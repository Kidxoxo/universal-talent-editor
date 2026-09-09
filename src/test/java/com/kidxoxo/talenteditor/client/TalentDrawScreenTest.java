package com.kidxoxo.talenteditor.client;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TalentDrawScreenTest {
    @Test
    void keepsConfiguredIconSizeWhenTheCardHasRoom() {
        assertEquals(48, TalentDrawScreen.fittedIconSize(48, 120, 140));
    }

    @Test
    void shrinksIconsToFitNarrowOrShortCards() {
        assertEquals(32, TalentDrawScreen.fittedIconSize(64, 40, 140));
        assertEquals(19, TalentDrawScreen.fittedIconSize(64, 120, 80));
    }
}
