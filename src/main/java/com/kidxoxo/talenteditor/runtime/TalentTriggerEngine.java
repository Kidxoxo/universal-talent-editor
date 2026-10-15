package com.kidxoxo.talenteditor.runtime;

import com.kidxoxo.talenteditor.domain.CompiledAction;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

public final class TalentTriggerEngine {
    private TalentTriggerEngine() {
    }

    public static List<CompiledAction> select(
            List<TriggerBinding> bindings,
            Set<ResourceLocation> ownedTalents,
            ConditionEvaluator.Context context
    ) {
        ArrayList<CompiledAction> result = new ArrayList<>();
        selectBindings(bindings, ownedTalents, context)
                .forEach(binding -> result.addAll(binding.trigger().actions()));
        return List.copyOf(result);
    }

    public static List<TriggerBinding> selectBindings(
            List<TriggerBinding> bindings,
            Set<ResourceLocation> ownedTalents,
            ConditionEvaluator.Context context
    ) {
        return ownedBindings(bindings, ownedTalents).stream()
                .filter(binding -> ConditionEvaluator.test(binding.trigger().condition(), context))
                .toList();
    }

    public static List<TriggerBinding> ownedBindings(
            List<TriggerBinding> bindings,
            Set<ResourceLocation> ownedTalents
    ) {
        return ownedBindings(bindings, ownedTalents, Set.of());
    }

    public static List<TriggerBinding> ownedBindings(
            List<TriggerBinding> bindings,
            Set<ResourceLocation> ownedTalents,
            Set<ResourceLocation> equippedItems
    ) {
        return bindings.stream()
                .filter(binding -> binding.ownedBy(ownedTalents, equippedItems))
                .sorted(Comparator.comparingInt((TriggerBinding binding) -> binding.trigger().priority()).reversed()
                        .thenComparing(binding -> binding.talentId().toString())
                        .thenComparing(binding -> binding.sourceKind().name())
                        .thenComparing(binding -> binding.trigger().id()))
                .toList();
    }

    public static boolean chancePasses(double chance, double roll) {
        if (chance <= 0.0D) return false;
        if (chance >= 1.0D) return true;
        return roll >= 0.0D && roll < chance;
    }

    public static double effectiveChance(double baseChance, double attributeValue, double chancePerPoint) {
        double calculated = baseChance + attributeValue * chancePerPoint;
        if (!Double.isFinite(calculated)) return calculated > 0.0D ? 1.0D : 0.0D;
        return Math.max(0.0D, Math.min(1.0D, calculated));
    }

    public static boolean coolingDown(long lastActivationTick, long currentTick, int cooldownTicks) {
        if (cooldownTicks <= 0 || lastActivationTick < 0L || currentTick < lastActivationTick) return false;
        return currentTick - lastActivationTick < cooldownTicks;
    }

    public static boolean activationLimitReached(int activationCount, int activationLimit) {
        return activationLimit > 0 && activationCount >= activationLimit;
    }
}
