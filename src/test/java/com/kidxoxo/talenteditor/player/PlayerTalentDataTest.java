package com.kidxoxo.talenteditor.player;

import com.kidxoxo.talenteditor.domain.QueueDrawConstraintAction;
import com.kidxoxo.talenteditor.domain.DrawConstraintQueueMode;
import com.kidxoxo.talenteditor.domain.DrawConstraintSpec;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PlayerTalentDataTest {
    @Test
    void cloneCopyPreservesOwnershipTokensCountersAndPity() {
        PlayerTalentData original = new PlayerTalentData();
        ResourceLocation talent = new ResourceLocation("example", "bound_talent");
        ResourceLocation counter = new ResourceLocation("example", "kills");
        ResourceLocation pool = new ResourceLocation("example", "main");
        original.grant(talent);
        original.setDrawTokens(9);
        original.setCounter(counter, 14L);
        original.recordDrawPity(pool, false, 10);

        PlayerTalentData clone = new PlayerTalentData();
        clone.copyFrom(original);

        assertEquals(Set.of(talent), clone.owned());
        assertEquals(9, clone.drawTokens());
        assertEquals(14L, clone.counter(counter));
        assertEquals(1, clone.drawPity(pool));
    }

    @Test
    void serializesUniqueOwnedTalentsAndIgnoresInvalidSavedIds() {
        PlayerTalentData original = new PlayerTalentData();
        ResourceLocation first = new ResourceLocation("example", "first");
        ResourceLocation second = new ResourceLocation("example", "second");
        assertTrue(original.grant(first));
        assertFalse(original.grant(first));
        assertTrue(original.grant(second));
        original.setDrawTokens(2);
        ResourceLocation counter = new ResourceLocation("example", "kills");
        assertEquals(3L, original.addCounter(counter, 3L));

        CompoundTag saved = original.serializeNBT();
        saved.getList("talents", 8).add(StringTag.valueOf("INVALID ID"));
        PlayerTalentData restored = new PlayerTalentData();
        restored.deserializeNBT(saved);

        assertEquals(2, restored.owned().size());
        assertTrue(restored.contains(first));
        assertTrue(restored.contains(second));
        assertEquals(2, restored.drawTokens());
        assertEquals(3L, restored.counter(counter));
        assertEquals(1L, restored.addCounter(counter, -2L));
        assertEquals(0L, restored.addCounter(counter, Long.MIN_VALUE));
        assertFalse(restored.counters().containsKey(counter));
        assertEquals(PlayerTalentData.MAX_COUNTER_VALUE,
                restored.addCounter(counter, Long.MAX_VALUE));
        assertTrue(restored.acquireWithDrawToken(new ResourceLocation("example", "drawn")));
        assertEquals(1, restored.drawTokens());
        assertFalse(restored.acquireWithDrawToken(new ResourceLocation("example", "drawn")));
        assertEquals(1, restored.drawTokens());
        assertTrue(restored.revoke(first));
        assertFalse(restored.contains(first));
    }

    @Test
    void synthesisConsumesTalentsAndCountersExactlyOnce() {
        PlayerTalentData data = new PlayerTalentData();
        ResourceLocation base = new ResourceLocation("example", "base");
        ResourceLocation result = new ResourceLocation("example", "advanced");
        ResourceLocation kills = new ResourceLocation("example", "kills");
        data.grant(base);
        data.setCounter(kills, 12L);

        assertFalse(data.applySynthesis(result, Set.of(base), Map.of(kills, 13L)));
        assertTrue(data.contains(base));
        assertFalse(data.contains(result));
        assertEquals(12L, data.counter(kills));

        assertTrue(data.applySynthesis(result, Set.of(base), Map.of(kills, 10L)));
        assertFalse(data.contains(base));
        assertTrue(data.contains(result));
        assertEquals(2L, data.counter(kills));
        assertFalse(data.applySynthesis(result, Set.of(base), Map.of(kills, 1L)));
        assertEquals(2L, data.counter(kills));
    }

    @Test
    void persistsPoolPityAndAppliesDuplicateCounterRewardAtomically() {
        ResourceLocation pool = new ResourceLocation("example", "main");
        ResourceLocation shards = new ResourceLocation("example", "talent_shards");
        PlayerTalentData data = new PlayerTalentData();
        data.setDrawTokens(2);

        assertEquals(1, data.recordDrawPity(pool, false, 3));
        assertEquals(2, data.recordDrawPity(pool, false, 3));
        assertEquals(3, data.recordDrawPity(pool, false, 3));
        assertEquals(3, data.recordDrawPity(pool, false, 3));
        assertTrue(data.exchangeDrawTokenForCounter(shards, 7L));
        assertEquals(1, data.drawTokens());
        assertEquals(7L, data.counter(shards));

        PlayerTalentData restored = new PlayerTalentData();
        restored.deserializeNBT(data.serializeNBT());
        assertEquals(3, restored.drawPity(pool));
        assertEquals(7L, restored.counter(shards));
        assertEquals(0, restored.recordDrawPity(pool, true, 3));

        restored.setCounter(shards, PlayerTalentData.MAX_COUNTER_VALUE);
        int tokensBefore = restored.drawTokens();
        assertFalse(restored.exchangeDrawTokenForCounter(shards, 1L));
        assertEquals(tokensBefore, restored.drawTokens());
    }

    @Test
    void persistsOnlineCounterLifecycleClockAndWrapsWithoutUsingZero() {
        PlayerTalentData data = new PlayerTalentData();
        assertEquals(1L, data.advanceCounterLifecycleTick());
        assertEquals(2L, data.advanceCounterLifecycleTick());

        PlayerTalentData restored = new PlayerTalentData();
        restored.deserializeNBT(data.serializeNBT());
        assertEquals(2L, restored.counterLifecycleTicks());
        assertEquals(3L, restored.advanceCounterLifecycleTick());

        CompoundTag overflow = new CompoundTag();
        overflow.putLong("counterLifecycleTicks", Long.MAX_VALUE);
        restored.deserializeNBT(overflow);
        assertEquals(1L, restored.advanceCounterLifecycleTick());
    }

    @Test
    void persistsBoundedPerTriggerActivationTicks() {
        PlayerTalentData data = new PlayerTalentData();
        String first = "example:first#poison";
        String second = "example:second#shield";
        assertEquals(-1L, data.triggerLastActivation(first));
        assertTrue(data.setTriggerLastActivation(first, 20L));
        assertTrue(data.setTriggerLastActivation(second, 45L));
        assertFalse(data.setTriggerLastActivation("", 10L));
        assertFalse(data.setTriggerLastActivation("example:bad#negative", -1L));

        PlayerTalentData restored = new PlayerTalentData();
        restored.deserializeNBT(data.serializeNBT());
        assertEquals(20L, restored.triggerLastActivation(first));
        assertEquals(45L, restored.triggerLastActivation(second));

        PlayerTalentData copied = new PlayerTalentData();
        copied.copyFrom(restored);
        assertEquals(20L, copied.triggerLastActivation(first));
    }

    @Test
    void persistsLimitedTriggerCountsAndClearsStateWhenTalentIsRevoked() {
        PlayerTalentData data = new PlayerTalentData();
        ResourceLocation talent = new ResourceLocation("example", "guard");
        String trigger = talent + "#once";
        data.grant(talent);
        assertTrue(data.canRecordTriggerActivation(trigger));
        assertTrue(data.incrementTriggerActivation(trigger));
        assertTrue(data.incrementTriggerActivation(trigger));
        assertEquals(2, data.triggerActivationCount(trigger));
        assertTrue(data.setTriggerLastActivation(trigger, 42L));

        PlayerTalentData restored = new PlayerTalentData();
        restored.deserializeNBT(data.serializeNBT());
        assertEquals(2, restored.triggerActivationCount(trigger));
        assertTrue(restored.revoke(talent));
        assertEquals(0, restored.triggerActivationCount(trigger));
        assertEquals(-1L, restored.triggerLastActivation(trigger));

        PlayerTalentData copied = new PlayerTalentData();
        copied.copyFrom(restored);
        assertEquals(0, copied.triggerActivationCount(trigger));
    }

    @Test
    void persistsAndConsumesTheFirstApplicableDrawConstraint() {
        ResourceLocation firstPool = new ResourceLocation("example", "first_pool");
        ResourceLocation secondPool = new ResourceLocation("example", "second_pool");
        QueueDrawConstraintAction first = new QueueDrawConstraintAction(
                List.of(), List.of(new ResourceLocation("example", "light")), List.of(), List.of(),
                List.of(firstPool), 1
        );
        QueueDrawConstraintAction second = new QueueDrawConstraintAction(
                List.of(new ResourceLocation("example", "hope")), List.of(), List.of(), List.of(),
                List.of(secondPool), 2
        );
        PlayerTalentData data = new PlayerTalentData();
        assertTrue(data.queueDrawConstraint(first));
        assertTrue(data.queueDrawConstraint(second));

        PlayerTalentData restored = new PlayerTalentData();
        restored.deserializeNBT(data.serializeNBT());
        assertEquals(second, restored.nextDrawConstraint(secondPool).orElseThrow());
        assertTrue(restored.consumeNextDrawConstraint(secondPool));
        assertTrue(restored.nextDrawConstraint(secondPool).isEmpty());
        assertEquals(first, restored.nextDrawConstraint(firstPool).orElseThrow());

        PlayerTalentData copied = new PlayerTalentData();
        copied.copyFrom(restored);
        assertEquals(List.of(first), copied.drawConstraints());
    }

    @Test
    void countsSuccessfulQueueActionsAndReplacesEveryNinthResultWithTheMilestone() {
        ResourceLocation counter = new ResourceLocation("example", "hope_successes");
        ResourceLocation light = new ResourceLocation("example", "light");
        ResourceLocation material = new ResourceLocation("example", "ur_material");
        PlayerTalentData data = new PlayerTalentData();
        data.setCounter(counter, 8L);
        QueueDrawConstraintAction action = new QueueDrawConstraintAction(
                List.of(), List.of(light), List.of(), List.of(), List.of(), 1,
                DrawConstraintQueueMode.APPEND, Optional.of(counter), 1L, 9,
                Optional.of(new DrawConstraintSpec(
                        List.of(), List.of(light), List.of(), List.of(material), List.of(), 1
                ))
        );

        assertTrue(data.queueDrawConstraint(action));
        assertEquals(9L, data.counter(counter));
        QueueDrawConstraintAction ninth = data.drawConstraints().get(0);
        assertEquals(List.of(light), ninth.categories());
        assertEquals(List.of(material), ninth.tags());
        assertTrue(ninth.successCounter().isEmpty(), "pending entries must not repeat success side effects");

        assertTrue(data.queueDrawConstraint(action));
        assertEquals(10L, data.counter(counter));
        assertTrue(data.drawConstraints().get(1).tags().isEmpty());
    }

    @Test
    void dedupeDoesNotCountAndReplaceOnlyClearsOverlappingPoolScopes() {
        ResourceLocation firstPool = new ResourceLocation("example", "first_pool");
        ResourceLocation secondPool = new ResourceLocation("example", "second_pool");
        ResourceLocation light = new ResourceLocation("example", "light");
        ResourceLocation dark = new ResourceLocation("example", "dark");
        ResourceLocation successes = new ResourceLocation("example", "successes");
        PlayerTalentData data = new PlayerTalentData();
        QueueDrawConstraintAction first = new QueueDrawConstraintAction(
                List.of(), List.of(light), List.of(), List.of(), List.of(firstPool), 1
        );
        QueueDrawConstraintAction second = new QueueDrawConstraintAction(
                List.of(), List.of(dark), List.of(), List.of(), List.of(secondPool), 1
        );
        assertTrue(data.queueDrawConstraint(first));
        assertTrue(data.queueDrawConstraint(second));

        QueueDrawConstraintAction duplicate = new QueueDrawConstraintAction(
                List.of(), List.of(light), List.of(), List.of(), List.of(firstPool), 1,
                DrawConstraintQueueMode.DEDUPE, Optional.of(successes), 1L, 0, Optional.empty()
        );
        assertFalse(data.queueDrawConstraint(duplicate));
        assertEquals(0L, data.counter(successes));

        QueueDrawConstraintAction replacement = new QueueDrawConstraintAction(
                List.of(new ResourceLocation("example", "hope")), List.of(), List.of(), List.of(),
                List.of(firstPool), 2, DrawConstraintQueueMode.REPLACE,
                Optional.of(successes), 1L, 0, Optional.empty()
        );
        assertTrue(data.queueDrawConstraint(replacement));
        assertEquals(1L, data.counter(successes));
        assertEquals(2, data.drawConstraints().size());
        assertEquals(second, data.nextDrawConstraint(secondPool).orElseThrow());
        assertEquals(List.of(new ResourceLocation("example", "hope")),
                data.nextDrawConstraint(firstPool).orElseThrow().talents());
    }

    @Test
    void persistsPerTriggerMovementProgressAndPreservesIntervalRemainder() {
        ResourceLocation talent = new ResourceLocation("example", "hope");
        String trigger = talent + "#move_search";
        PlayerTalentData data = new PlayerTalentData();
        assertTrue(data.grant(talent));
        assertFalse(data.claimTriggerDistanceInterval(trigger, 0.4D, 0.6D));
        assertTrue(data.claimTriggerDistanceInterval(trigger, 0.7D, 0.6D));
        assertEquals(0.5D, data.triggerDistanceProgress(trigger), 1.0E-9D);

        PlayerTalentData restored = new PlayerTalentData();
        restored.deserializeNBT(data.serializeNBT());
        assertEquals(0.5D, restored.triggerDistanceProgress(trigger), 1.0E-9D);
        assertTrue(restored.claimTriggerDistanceInterval(trigger, 0.1D, 0.6D));
        assertEquals(0.0D, restored.triggerDistanceProgress(trigger), 1.0E-9D);

        assertFalse(restored.claimTriggerDistanceInterval(trigger, Double.POSITIVE_INFINITY, 0.6D));
        assertTrue(restored.revoke(talent));
        assertEquals(0.0D, restored.triggerDistanceProgress(trigger), 1.0E-9D);
    }
}
