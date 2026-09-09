package com.kidxoxo.talenteditor.client;

import org.junit.jupiter.api.Test;

import static com.kidxoxo.talenteditor.client.TalentBookScreen.BookMode.COUNTERS;
import static com.kidxoxo.talenteditor.client.TalentBookScreen.BookMode.OWNED;
import static com.kidxoxo.talenteditor.client.TalentBookScreen.BookMode.SYNTHESIS;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TalentBookScreenTest {
    @Test
    void automaticModeHidesManualSynthesisAndSkipsItsPage() {
        assertFalse(TalentBookScreen.synthesisControlsVisible(true));
        assertEquals(COUNTERS, TalentBookScreen.nextMode(OWNED, true));
        assertEquals(OWNED, TalentBookScreen.nextMode(COUNTERS, true));
        assertEquals(COUNTERS, TalentBookScreen.nextMode(SYNTHESIS, true));
    }

    @Test
    void manualModeKeepsTheThreePageNavigationCycle() {
        assertTrue(TalentBookScreen.synthesisControlsVisible(false));
        assertEquals(SYNTHESIS, TalentBookScreen.nextMode(OWNED, false));
        assertEquals(COUNTERS, TalentBookScreen.nextMode(SYNTHESIS, false));
        assertEquals(OWNED, TalentBookScreen.nextMode(COUNTERS, false));
    }
}
