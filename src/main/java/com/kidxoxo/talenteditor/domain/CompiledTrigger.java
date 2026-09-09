package com.kidxoxo.talenteditor.domain;

import net.minecraft.resources.ResourceLocation;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

public record CompiledTrigger(
        String id,
        ResourceLocation event,
        int priority,
        double chance,
        Optional<ResourceLocation> chanceAttribute,
        Optional<ResourceLocation> chanceCounter,
        Optional<ResourceLocation> chanceCategory,
        double chancePerPoint,
        double distanceInterval,
        int cooldownTicks,
        int activationLimit,
        CompiledCondition condition,
        List<CompiledAction> actions,
        ActionFailurePolicy actionFailurePolicy,
        List<CompiledAction> failureActions
) {
    public CompiledTrigger {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("id must not be blank");
        }
        event = Objects.requireNonNull(event, "event");
        if (!Double.isFinite(chance) || chance < 0.0D || chance > 1.0D) {
            throw new IllegalArgumentException("chance must be finite and in range 0..1");
        }
        chanceAttribute = Objects.requireNonNull(chanceAttribute, "chanceAttribute");
        chanceCounter = Objects.requireNonNull(chanceCounter, "chanceCounter");
        chanceCategory = Objects.requireNonNull(chanceCategory, "chanceCategory");
        if ((chanceAttribute.isPresent() ? 1 : 0) + (chanceCounter.isPresent() ? 1 : 0)
                + (chanceCategory.isPresent() ? 1 : 0) > 1) {
            throw new IllegalArgumentException("chance sources are mutually exclusive");
        }
        if (!Double.isFinite(chancePerPoint) || chancePerPoint < -1.0D || chancePerPoint > 1.0D) {
            throw new IllegalArgumentException("chancePerPoint must be finite and in range -1..1");
        }
        if (chanceAttribute.isEmpty() && chanceCounter.isEmpty() && chanceCategory.isEmpty()
                && chancePerPoint != 0.0D) {
            throw new IllegalArgumentException("chancePerPoint requires a chance source");
        }
        if (!Double.isFinite(distanceInterval) || distanceInterval < 0.01D
                || distanceInterval > 1_000_000.0D) {
            throw new IllegalArgumentException("distanceInterval must be in range 0.01..1000000");
        }
        if (cooldownTicks < 0 || cooldownTicks > 1_728_000) {
            throw new IllegalArgumentException("cooldownTicks must be in range 0..1728000");
        }
        if (activationLimit < 0 || activationLimit > 1_000_000) {
            throw new IllegalArgumentException("activationLimit must be in range 0..1000000");
        }
        condition = Objects.requireNonNull(condition, "condition");
        actions = List.copyOf(actions);
        actionFailurePolicy = Objects.requireNonNull(actionFailurePolicy, "actionFailurePolicy");
        failureActions = List.copyOf(failureActions);
    }

    public CompiledTrigger(
            String id,
            ResourceLocation event,
            int priority,
            double chance,
            int cooldownTicks,
            CompiledCondition condition,
            List<CompiledAction> actions
    ) {
        this(id, event, priority, chance, Optional.empty(), Optional.empty(), Optional.empty(), 0.0D, 1.0D,
                cooldownTicks, 0, condition, actions, ActionFailurePolicy.CONTINUE, List.of());
    }

    public CompiledTrigger(
            String id,
            ResourceLocation event,
            int priority,
            CompiledCondition condition,
            List<CompiledAction> actions
    ) {
        this(id, event, priority, 1.0D, Optional.empty(), Optional.empty(), Optional.empty(), 0.0D, 1.0D,
                0, 0, condition, actions, ActionFailurePolicy.CONTINUE, List.of());
    }

    public CompiledTrigger(
            String id,
            ResourceLocation event,
            int priority,
            double chance,
            Optional<ResourceLocation> chanceAttribute,
            Optional<ResourceLocation> chanceCounter,
            Optional<ResourceLocation> chanceCategory,
            double chancePerPoint,
            double distanceInterval,
            int cooldownTicks,
            int activationLimit,
            CompiledCondition condition,
            List<CompiledAction> actions
    ) {
        this(id, event, priority, chance, chanceAttribute, chanceCounter, chanceCategory, chancePerPoint,
                distanceInterval, cooldownTicks, activationLimit, condition, actions,
                ActionFailurePolicy.CONTINUE, List.of());
    }
}
