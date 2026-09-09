package com.kidxoxo.talenteditor.runtime;

import com.kidxoxo.talenteditor.domain.AddEffectAction;
import com.kidxoxo.talenteditor.domain.ActionTarget;
import com.kidxoxo.talenteditor.domain.CompiledCondition;
import com.kidxoxo.talenteditor.domain.CompiledTrigger;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TriggerTraceBufferTest {
    @Test
    void staysDisabledByDefaultAndRetainsNewestEntriesWithinCapacity() {
        TriggerTraceBuffer trace = new TriggerTraceBuffer(2);
        TriggerBinding binding = binding();
        trace.record(1L, "Alex", id("talent_editor:damage_taken"), binding,
                TriggerTraceEntry.Outcome.CONDITION_MISS, 0.5D, 0);
        assertTrue(trace.recent(10).isEmpty());

        trace.setEnabled(true);
        trace.record(2L, "Alex", id("talent_editor:damage_taken"), binding,
                TriggerTraceEntry.Outcome.CHANCE_MISS, 0.5D, 0);
        trace.record(3L, "Alex", id("talent_editor:damage_taken"), binding,
                TriggerTraceEntry.Outcome.NO_ACTION_APPLIED, 0.5D, 0);
        trace.record(4L, "Alex", id("talent_editor:damage_taken"), binding,
                TriggerTraceEntry.Outcome.ACTIVATED, 0.5D, 1, 2, List.of(
                        new TriggerTraceEntry.ActionResult(
                                TriggerTraceEntry.Branch.PRIMARY, 0,
                                id("talent_editor:remove_effect"), false),
                        new TriggerTraceEntry.ActionResult(
                                TriggerTraceEntry.Branch.FAILURE, 0,
                                id("talent_editor:add_effect"), true)
                ));

        assertEquals(List.of(4L, 3L), trace.recent(10).stream()
                .map(TriggerTraceEntry::playerTick).toList());
        assertEquals(1L, trace.snapshot().overwritten());
        assertEquals(2, trace.snapshot().size());
        assertTrue(trace.snapshot().enabled());
        TriggerTraceEntry newest = trace.recent(1).get(0);
        assertEquals(2, newest.attemptedActions());
        assertEquals(TriggerTraceEntry.Branch.FAILURE, newest.actionResults().get(1).branch());
        assertTrue(newest.actionResults().get(1).applied());

        trace.setEnabled(false);
        trace.clear();
        assertFalse(trace.snapshot().enabled());
        assertEquals(0, trace.snapshot().size());
        assertEquals(0L, trace.snapshot().overwritten());
    }

    private static TriggerBinding binding() {
        return new TriggerBinding(id("example:guard"), new CompiledTrigger(
                "once", id("talent_editor:damage_taken"), 0,
                new CompiledCondition.Always(true), List.of(new AddEffectAction(
                        id("minecraft:resistance"), 20, 0, ActionTarget.SELF, false, true, true
                ))
        ));
    }

    private static ResourceLocation id(String value) {
        return new ResourceLocation(value);
    }
}
