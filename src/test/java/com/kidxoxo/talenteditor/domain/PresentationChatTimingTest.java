package com.kidxoxo.talenteditor.domain;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class PresentationChatTimingTest {
    @Test void oldTitleDefaultsDoNotShortenChat() {
        assertEquals(200, PresentationChatTiming.visibleTicks(70));
        assertEquals(0, PresentationChatTiming.timestampOffset(70));
        assertEquals(1, PresentationChatTiming.opacity(180, 70));
        assertEquals(0, PresentationChatTiming.opacity(200, 70));
    }
    @Test void extendedMessagesKeepVanillaFadeAfterTheirOwnHold() {
        assertEquals(580, PresentationChatTiming.timestampOffset(780));
        assertEquals(1, PresentationChatTiming.opacity(760, 780));
        assertEquals(.25, PresentationChatTiming.opacity(770, 780));
        assertEquals(0, PresentationChatTiming.opacity(781, 780));
        assertEquals(1, PresentationChatTiming.opacity(500, 1900));
    }
    @Test void durationIsBounded() {
        assertEquals(200, PresentationChatTiming.visibleTicks(-1));
        assertEquals(12_000, PresentationChatTiming.visibleTicks(Integer.MAX_VALUE));
    }
}
