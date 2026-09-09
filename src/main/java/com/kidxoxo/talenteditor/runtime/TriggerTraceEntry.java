package com.kidxoxo.talenteditor.runtime;

import net.minecraft.resources.ResourceLocation;

import java.util.Objects;
import java.util.List;

public record TriggerTraceEntry(
        long sequence,
        long playerTick,
        String playerName,
        ResourceLocation event,
        ResourceLocation talent,
        String trigger,
        Outcome outcome,
        double effectiveChance,
        int appliedActions,
        int attemptedActions,
        List<ActionResult> actionResults
) {
    public static final int MAX_ACTION_RESULTS = 64;

    public TriggerTraceEntry {
        playerName = Objects.requireNonNull(playerName, "playerName");
        event = Objects.requireNonNull(event, "event");
        talent = Objects.requireNonNull(talent, "talent");
        trigger = Objects.requireNonNull(trigger, "trigger");
        outcome = Objects.requireNonNull(outcome, "outcome");
        if (appliedActions < 0 || attemptedActions < appliedActions) {
            throw new IllegalArgumentException("Invalid action totals");
        }
        actionResults = List.copyOf(actionResults);
        if (actionResults.size() > MAX_ACTION_RESULTS) {
            throw new IllegalArgumentException("Too many retained action results");
        }
    }

    public record ActionResult(Branch branch, int index, ResourceLocation type, boolean applied) {
        public ActionResult {
            branch = Objects.requireNonNull(branch, "branch");
            if (index < 0) throw new IllegalArgumentException("index must not be negative");
            type = Objects.requireNonNull(type, "type");
        }
    }

    public enum Branch {
        PRIMARY,
        FAILURE
    }

    public enum Outcome {
        CONDITION_MISS,
        ACTIVATION_LIMIT,
        COOLDOWN,
        CHANCE_MISS,
        NO_ACTION_APPLIED,
        ACTIVATED
    }
}
