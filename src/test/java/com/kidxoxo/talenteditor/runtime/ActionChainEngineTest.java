package com.kidxoxo.talenteditor.runtime;

import com.kidxoxo.talenteditor.domain.ActionFailurePolicy;
import com.kidxoxo.talenteditor.domain.CancelDamageAction;
import com.kidxoxo.talenteditor.domain.CompiledAction;
import com.kidxoxo.talenteditor.domain.ModifyDamageAction;
import com.kidxoxo.talenteditor.domain.DamageOperation;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ActionChainEngineTest {
    @Test
    void continuesPrimaryChainAndRunsFallbackAfterAnyFailure() {
        CompiledAction first = damage(1.0D);
        CompiledAction failed = damage(2.0D);
        CompiledAction third = damage(3.0D);
        CompiledAction fallback = new CancelDamageAction();
        Set<CompiledAction> successful = identitySet(first, third, fallback);

        ActionChainEngine.Result result = ActionChainEngine.execute(
                List.of(first, failed, third), List.of(fallback), ActionFailurePolicy.CONTINUE,
                successful::contains, true
        );

        assertTrue(result.primaryFailed());
        assertTrue(result.activated());
        assertEquals(3, result.appliedActions());
        assertEquals(4, result.attemptedActions());
        assertEquals(TriggerTraceEntry.Branch.FAILURE, result.actionResults().get(3).branch());
    }

    @Test
    void stopsAtFirstFailureAndSkipsFallbackWhenPrimaryFullySucceeds() {
        CompiledAction first = damage(1.0D);
        CompiledAction failed = damage(2.0D);
        CompiledAction skipped = damage(3.0D);
        CompiledAction fallback = new CancelDamageAction();
        Set<CompiledAction> successful = identitySet(first, fallback);

        ActionChainEngine.Result stopped = ActionChainEngine.execute(
                List.of(first, failed, skipped), List.of(fallback), ActionFailurePolicy.STOP,
                successful::contains, true
        );
        assertEquals(3, stopped.attemptedActions());
        assertEquals(2, stopped.appliedActions());
        assertEquals(TriggerTraceEntry.Branch.FAILURE, stopped.actionResults().get(2).branch());

        ActionChainEngine.Result noFailure = ActionChainEngine.execute(
                List.of(first), List.of(fallback), ActionFailurePolicy.CONTINUE,
                action -> true, true
        );
        assertFalse(noFailure.primaryFailed());
        assertEquals(1, noFailure.attemptedActions());
    }

    @Test
    void boundsRetainedTraceWithoutChangingAttemptTotals() {
        CompiledAction action = damage(1.0D);
        ActionChainEngine.Result result = ActionChainEngine.execute(
                Collections.nCopies(70, action), List.of(), ActionFailurePolicy.CONTINUE,
                ignored -> true, true
        );
        assertEquals(70, result.attemptedActions());
        assertEquals(TriggerTraceEntry.MAX_ACTION_RESULTS, result.actionResults().size());
    }

    private static ModifyDamageAction damage(double amount) {
        return new ModifyDamageAction(DamageOperation.ADD, amount, 0.0D, 1.0E9D);
    }

    @SafeVarargs
    private static Set<CompiledAction> identitySet(CompiledAction... actions) {
        Set<CompiledAction> result = Collections.newSetFromMap(new IdentityHashMap<>());
        Collections.addAll(result, actions);
        return result;
    }
}
