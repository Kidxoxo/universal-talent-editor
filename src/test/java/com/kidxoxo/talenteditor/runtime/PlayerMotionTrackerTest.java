package com.kidxoxo.talenteditor.runtime;

import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PlayerMotionTrackerTest {
    @Test
    void accumulatesStationaryTicksAndResetsForMovementOrDimensionChange() {
        ResourceLocation overworld = new ResourceLocation("minecraft:overworld");
        ResourceLocation nether = new ResourceLocation("minecraft:the_nether");
        PlayerMotionTracker.Sample first = new PlayerMotionTracker.Sample(
                overworld, 0.0D, 64.0D, 0.0D, 0L, 0.0D
        );
        PlayerMotionTracker.Sample still = PlayerMotionTracker.next(first,
                new PlayerMotionTracker.Sample(overworld, 0.0D, 64.0D, 0.0D, 0L, 0.0D));
        assertEquals(1L, still.stationaryTicks());

        PlayerMotionTracker.Sample moved = PlayerMotionTracker.next(still,
                new PlayerMotionTracker.Sample(overworld, 1.0D, 64.0D, 0.0D, 0L, 0.0D));
        assertEquals(0L, moved.stationaryTicks());
        assertEquals(1.0D, moved.lastMovement(), 1.0E-9D);

        PlayerMotionTracker.Sample changedDimension = PlayerMotionTracker.next(moved,
                new PlayerMotionTracker.Sample(nether, 1.0D, 64.0D, 0.0D, 0L, 0.0D));
        assertEquals(0L, changedDimension.stationaryTicks());
    }
}
