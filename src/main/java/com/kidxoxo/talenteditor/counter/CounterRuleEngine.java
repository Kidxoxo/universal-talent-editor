package com.kidxoxo.talenteditor.counter;

import com.kidxoxo.talenteditor.domain.CounterRuleDefinition;
import com.kidxoxo.talenteditor.domain.CounterAmountMode;
import net.minecraft.resources.ResourceLocation;

import java.util.Optional;
import java.util.Set;

public final class CounterRuleEngine {
    private CounterRuleEngine() {
    }

    public static boolean matches(CounterRuleDefinition rule, EventContext context) {
        if (!context.ownedTalents().containsAll(rule.requiredTalents())) return false;
        if (!matchesSelector(rule.entityTypes(), rule.entityTags(), context.entityType(), context.entityTags())) {
            return false;
        }
        if (!matchesSelector(rule.items(), rule.itemTags(), context.item(), context.itemTags())) return false;
        return rule.damageTags().isEmpty() || intersects(rule.damageTags(), context.damageTags());
    }

    public static long nextValue(CounterRuleDefinition rule, long current) {
        return nextValue(rule, current, 0.0D);
    }

    public static long nextValue(CounterRuleDefinition rule, long current, double eventAmount) {
        if (current >= rule.maximum()) return rule.maximum();
        long increment = incrementForEvent(rule, eventAmount);
        if (increment <= 0L) return current;
        if (increment >= rule.maximum() - current) return rule.maximum();
        return current + increment;
    }

    public static long incrementForEvent(CounterRuleDefinition rule, double eventAmount) {
        if (rule.amountMode() == CounterAmountMode.FIXED) return rule.increment();
        if (!Double.isFinite(eventAmount) || eventAmount <= 0.0D) return 0L;
        double scaled = eventAmount * rule.amountScale();
        if (!Double.isFinite(scaled)) return rule.maximum();
        double rounded = switch (rule.rounding()) {
            case FLOOR -> Math.floor(scaled);
            case ROUND -> Math.round(scaled);
            case CEIL -> Math.ceil(scaled);
        };
        long result = rounded >= rule.maximum() ? rule.maximum() : (long) Math.max(0.0D, rounded);
        return Math.max(rule.minimumIncrement(), result);
    }

    private static boolean matchesSelector(
            java.util.List<ResourceLocation> ids,
            java.util.List<ResourceLocation> configuredTags,
            Optional<ResourceLocation> actualId,
            Set<ResourceLocation> actualTags
    ) {
        if (ids.isEmpty() && configuredTags.isEmpty()) return true;
        return actualId.filter(ids::contains).isPresent() || intersects(configuredTags, actualTags);
    }

    private static boolean intersects(java.util.List<ResourceLocation> expected, Set<ResourceLocation> actual) {
        for (ResourceLocation value : expected) {
            if (actual.contains(value)) return true;
        }
        return false;
    }

    public record EventContext(
            Set<ResourceLocation> ownedTalents,
            Optional<ResourceLocation> entityType,
            Set<ResourceLocation> entityTags,
            Optional<ResourceLocation> item,
            Set<ResourceLocation> itemTags,
            Set<ResourceLocation> damageTags
    ) {
        public EventContext {
            ownedTalents = Set.copyOf(ownedTalents);
            entityType = entityType == null ? Optional.empty() : entityType;
            entityTags = Set.copyOf(entityTags);
            item = item == null ? Optional.empty() : item;
            itemTags = Set.copyOf(itemTags);
            damageTags = Set.copyOf(damageTags);
        }
    }
}
