package com.kidxoxo.talenteditor.client;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ParticlePreviewTimelineTest {
    @Test
    void threeAnimationsStartTogetherThenKeepIndependentIntervalsAndDurations() {
        ParticlePreviewTimeline timeline = new ParticlePreviewTimeline(List.of(
                new ParticlePreviewTimeline.Layer(8, 1, 1),
                new ParticlePreviewTimeline.Layer(5, 6, 2),
                new ParticlePreviewTimeline.Layer(3, 7, 3)
        ));

        assertEquals(7, timeline.durationTicks());
        assertEquals(16, timeline.firstWaveCount());
        for (int layer = 0; layer < timeline.size(); layer++) assertTrue(timeline.emitsAt(layer, 0));
        assertFalse(timeline.emitsAt(0, 1));
        assertTrue(timeline.emitsAt(1, 2));
        assertFalse(timeline.emitsAt(2, 2));
        assertTrue(timeline.emitsAt(2, 3));
        assertFalse(timeline.emitsAt(1, 6));
        assertTrue(timeline.emitsAt(2, 6));
        assertFalse(timeline.emitsAt(2, 7));
    }

    @Test
    void rejectsGroupsThatExceedRuntimeConcurrencyOrParticleBudgets() {
        ParticlePreviewTimeline.Layer layer = new ParticlePreviewTimeline.Layer(32, 60, 20);
        assertThrows(IllegalArgumentException.class, () -> new ParticlePreviewTimeline(List.of(
                layer, layer, layer, layer, layer
        )));
        assertThrows(IllegalArgumentException.class, () -> new ParticlePreviewTimeline(List.of(
                layer, layer, layer, layer, layer, layer, layer, layer, layer
        )));
        assertThrows(IllegalArgumentException.class, () ->
                new ParticlePreviewTimeline.Layer(32, 60, 1));
    }
}
