package com.kidxoxo.talenteditor.runtime;

import com.kidxoxo.talenteditor.domain.ActionFailurePolicy;
import com.kidxoxo.talenteditor.domain.CompiledAction;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Predicate;

/** Pure action-chain control flow; side effects remain in the supplied executor. */
public final class ActionChainEngine {
    private ActionChainEngine() {
    }

    public static Result execute(
            List<CompiledAction> primary,
            List<CompiledAction> failure,
            ActionFailurePolicy policy,
            Predicate<CompiledAction> executor,
            boolean retainTrace
    ) {
        Objects.requireNonNull(primary, "primary");
        Objects.requireNonNull(failure, "failure");
        Objects.requireNonNull(policy, "policy");
        Objects.requireNonNull(executor, "executor");
        ArrayList<TriggerTraceEntry.ActionResult> results = retainTrace ? new ArrayList<>() : null;
        int applied = 0;
        int attempted = 0;
        boolean primaryFailed = false;
        for (int index = 0; index < primary.size(); index++) {
            CompiledAction action = primary.get(index);
            boolean actionApplied = executor.test(action);
            attempted++;
            retain(results, TriggerTraceEntry.Branch.PRIMARY, index, action, actionApplied);
            if (actionApplied) applied++;
            else {
                primaryFailed = true;
                if (policy == ActionFailurePolicy.STOP) break;
            }
        }
        if (primaryFailed) {
            for (int index = 0; index < failure.size(); index++) {
                CompiledAction action = failure.get(index);
                boolean actionApplied = executor.test(action);
                attempted++;
                retain(results, TriggerTraceEntry.Branch.FAILURE, index, action, actionApplied);
                if (actionApplied) applied++;
                else if (policy == ActionFailurePolicy.STOP) break;
            }
        }
        return new Result(applied, attempted, primaryFailed,
                results == null ? List.of() : List.copyOf(results));
    }

    private static void retain(
            List<TriggerTraceEntry.ActionResult> results,
            TriggerTraceEntry.Branch branch,
            int index,
            CompiledAction action,
            boolean applied
    ) {
        if (results == null || results.size() >= TriggerTraceEntry.MAX_ACTION_RESULTS) return;
        results.add(new TriggerTraceEntry.ActionResult(branch, index, action.type(), applied));
    }

    public record Result(
            int appliedActions,
            int attemptedActions,
            boolean primaryFailed,
            List<TriggerTraceEntry.ActionResult> actionResults
    ) {
        public Result {
            actionResults = List.copyOf(actionResults);
        }

        public boolean activated() {
            return appliedActions > 0;
        }
    }
}
