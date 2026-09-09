package com.kidxoxo.talenteditor.domain;

import net.minecraft.resources.ResourceLocation;

import java.util.List;

public record CounterRuleDefinition(
        ResourceLocation id,
        int schema,
        boolean enabled,
        LocalizedText name,
        CounterEventType event,
        long increment,
        CounterAmountMode amountMode,
        double amountScale,
        CounterRounding rounding,
        long minimumIncrement,
        long maximum,
        int cooldownTicks,
        int intervalTicks,
        boolean resetOnLogin,
        boolean resetOnDeath,
        int resetIntervalTicks,
        List<ResourceLocation> categories,
        CounterVisibility visibility,
        List<String> requiredMods,
        boolean available,
        List<ResourceLocation> requiredTalents,
        List<ResourceLocation> entityTypes,
        List<ResourceLocation> entityTags,
        List<ResourceLocation> items,
        List<ResourceLocation> itemTags,
        List<ResourceLocation> damageTags
) {
    public CounterRuleDefinition {
        categories = List.copyOf(categories);
        requiredMods = List.copyOf(requiredMods);
        requiredTalents = List.copyOf(requiredTalents);
        entityTypes = List.copyOf(entityTypes);
        entityTags = List.copyOf(entityTags);
        items = List.copyOf(items);
        itemTags = List.copyOf(itemTags);
        damageTags = List.copyOf(damageTags);
    }

    public boolean resetsOn(CounterResetTrigger trigger) {
        return switch (trigger) {
            case LOGIN -> resetOnLogin;
            case DEATH -> resetOnDeath;
        };
    }

    public boolean periodicResetDue(long onlineTicks) {
        return resetIntervalTicks > 0 && onlineTicks > 0L && onlineTicks % resetIntervalTicks == 0L;
    }

    public CounterRuleDefinition(
            ResourceLocation id,
            int schema,
            boolean enabled,
            LocalizedText name,
            CounterEventType event,
            long increment,
            CounterAmountMode amountMode,
            double amountScale,
            CounterRounding rounding,
            long minimumIncrement,
            long maximum,
            int cooldownTicks,
            int intervalTicks,
            List<String> requiredMods,
            boolean available,
            List<ResourceLocation> requiredTalents,
            List<ResourceLocation> entityTypes,
            List<ResourceLocation> entityTags,
            List<ResourceLocation> items,
            List<ResourceLocation> itemTags,
            List<ResourceLocation> damageTags
    ) {
        this(id, schema, enabled, name, event, increment, amountMode, amountScale, rounding, minimumIncrement,
                maximum, cooldownTicks, intervalTicks, false, false, 0, List.of(), CounterVisibility.FULL,
                requiredMods, available,
                requiredTalents, entityTypes, entityTags, items, itemTags, damageTags);
    }

    public CounterRuleDefinition(
            ResourceLocation id,
            int schema,
            boolean enabled,
            LocalizedText name,
            CounterEventType event,
            long increment,
            long maximum,
            int cooldownTicks,
            int intervalTicks,
            List<String> requiredMods,
            boolean available,
            List<ResourceLocation> requiredTalents,
            List<ResourceLocation> entityTypes,
            List<ResourceLocation> entityTags,
            List<ResourceLocation> items,
            List<ResourceLocation> itemTags,
            List<ResourceLocation> damageTags
    ) {
        this(id, schema, enabled, name, event, increment, CounterAmountMode.FIXED, 1.0D,
                CounterRounding.ROUND, 0L, maximum, cooldownTicks, intervalTicks, false, false, 0,
                List.of(), CounterVisibility.FULL,
                requiredMods, available,
                requiredTalents, entityTypes, entityTags, items, itemTags, damageTags);
    }
}
