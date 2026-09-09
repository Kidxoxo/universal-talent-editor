package com.kidxoxo.talenteditor.compile;

import com.google.gson.JsonObject;
import com.kidxoxo.talenteditor.domain.DefinitionKind;
import com.kidxoxo.talenteditor.validation.ValidationIssue;
import net.minecraft.resources.ResourceLocation;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public record RawDefinitionSet(
        Map<ResourceLocation, JsonObject> rarities,
        Map<ResourceLocation, JsonObject> talents,
        Map<ResourceLocation, JsonObject> pools,
        Map<ResourceLocation, JsonObject> counters,
        Map<ResourceLocation, JsonObject> layouts,
        Map<ResourceLocation, JsonObject> configuredItems,
        Map<ResourceLocation, String> raritySources,
        Map<ResourceLocation, String> talentSources,
        Map<ResourceLocation, String> poolSources,
        Map<ResourceLocation, String> counterSources,
        Map<ResourceLocation, String> layoutSources,
        Map<ResourceLocation, String> configuredItemSources,
        List<ValidationIssue> loadingIssues
) {
    public RawDefinitionSet {
        rarities = immutableJsonCopy(rarities);
        talents = immutableJsonCopy(talents);
        pools = immutableJsonCopy(pools);
        counters = immutableJsonCopy(counters);
        layouts = immutableJsonCopy(layouts);
        configuredItems = immutableJsonCopy(configuredItems);
        raritySources = immutableSourceCopy(raritySources, rarities);
        talentSources = immutableSourceCopy(talentSources, talents);
        poolSources = immutableSourceCopy(poolSources, pools);
        counterSources = immutableSourceCopy(counterSources, counters);
        layoutSources = immutableSourceCopy(layoutSources, layouts);
        configuredItemSources = immutableSourceCopy(configuredItemSources, configuredItems);
        loadingIssues = List.copyOf(loadingIssues);
    }

    public RawDefinitionSet(
            Map<ResourceLocation, JsonObject> rarities,
            Map<ResourceLocation, JsonObject> talents,
            Map<ResourceLocation, JsonObject> pools,
            Map<ResourceLocation, JsonObject> counters,
            Map<ResourceLocation, JsonObject> layouts,
            Map<ResourceLocation, String> raritySources,
            Map<ResourceLocation, String> talentSources,
            Map<ResourceLocation, String> poolSources,
            Map<ResourceLocation, String> counterSources,
            Map<ResourceLocation, String> layoutSources,
            List<ValidationIssue> loadingIssues
    ) {
        this(rarities, talents, pools, counters, layouts, Map.of(), raritySources, talentSources,
                poolSources, counterSources, layoutSources, Map.of(), loadingIssues);
    }

    public RawDefinitionSet(
            Map<ResourceLocation, JsonObject> rarities,
            Map<ResourceLocation, JsonObject> talents,
            Map<ResourceLocation, JsonObject> pools,
            Map<ResourceLocation, JsonObject> counters,
            Map<ResourceLocation, String> raritySources,
            Map<ResourceLocation, String> talentSources,
            Map<ResourceLocation, String> poolSources,
            Map<ResourceLocation, String> counterSources,
            List<ValidationIssue> loadingIssues
    ) {
        this(rarities, talents, pools, counters, Map.of(), Map.of(), raritySources, talentSources, poolSources,
                counterSources, Map.of(), Map.of(), loadingIssues);
    }

    public RawDefinitionSet(
            Map<ResourceLocation, JsonObject> rarities,
            Map<ResourceLocation, JsonObject> talents,
            Map<ResourceLocation, JsonObject> pools,
            Map<ResourceLocation, JsonObject> counters,
            List<ValidationIssue> loadingIssues
    ) {
        this(rarities, talents, pools, counters, Map.of(), Map.of(), Map.of(), Map.of(), Map.of(), Map.of(),
                Map.of(), Map.of(),
                loadingIssues);
    }

    public RawDefinitionSet(
            Map<ResourceLocation, JsonObject> rarities,
            Map<ResourceLocation, JsonObject> talents,
            Map<ResourceLocation, JsonObject> pools,
            List<ValidationIssue> loadingIssues
    ) {
        this(rarities, talents, pools, Map.of(), loadingIssues);
    }

    public RawDefinitionSet(
            Map<ResourceLocation, JsonObject> rarities,
            Map<ResourceLocation, JsonObject> talents,
            List<ValidationIssue> loadingIssues
    ) {
        this(rarities, talents, Map.of(), Map.of(), loadingIssues);
    }

    public static RawDefinitionSet empty() {
        return new RawDefinitionSet(Map.of(), Map.of(), Map.of(), Map.of(), List.of());
    }

    public RawDefinitionSet withTalent(ResourceLocation id, JsonObject definition, String source) {
        LinkedHashMap<ResourceLocation, JsonObject> nextDefinitions = new LinkedHashMap<>(talents);
        nextDefinitions.put(id, definition);
        LinkedHashMap<ResourceLocation, String> nextSources = new LinkedHashMap<>(talentSources);
        nextSources.put(id, source);
        return new RawDefinitionSet(
                rarities, nextDefinitions, pools, counters, layouts, configuredItems,
                raritySources, nextSources, poolSources, counterSources, layoutSources, configuredItemSources,
                loadingIssues
        );
    }

    public RawDefinitionSet withRarity(ResourceLocation id, JsonObject definition, String source) {
        LinkedHashMap<ResourceLocation, JsonObject> nextDefinitions = new LinkedHashMap<>(rarities);
        nextDefinitions.put(id, definition);
        LinkedHashMap<ResourceLocation, String> nextSources = new LinkedHashMap<>(raritySources);
        nextSources.put(id, source);
        return new RawDefinitionSet(
                nextDefinitions, talents, pools, counters, layouts, configuredItems,
                nextSources, talentSources, poolSources, counterSources, layoutSources, configuredItemSources,
                loadingIssues
        );
    }

    public RawDefinitionSet withPool(ResourceLocation id, JsonObject definition, String source) {
        LinkedHashMap<ResourceLocation, JsonObject> nextDefinitions = new LinkedHashMap<>(pools);
        nextDefinitions.put(id, definition);
        LinkedHashMap<ResourceLocation, String> nextSources = new LinkedHashMap<>(poolSources);
        nextSources.put(id, source);
        return new RawDefinitionSet(
                rarities, talents, nextDefinitions, counters, layouts, configuredItems,
                raritySources, talentSources, nextSources, counterSources, layoutSources, configuredItemSources,
                loadingIssues
        );
    }

    public RawDefinitionSet withCounter(ResourceLocation id, JsonObject definition, String source) {
        LinkedHashMap<ResourceLocation, JsonObject> nextDefinitions = new LinkedHashMap<>(counters);
        nextDefinitions.put(id, definition);
        LinkedHashMap<ResourceLocation, String> nextSources = new LinkedHashMap<>(counterSources);
        nextSources.put(id, source);
        return new RawDefinitionSet(
                rarities, talents, pools, nextDefinitions, layouts, configuredItems,
                raritySources, talentSources, poolSources, nextSources, layoutSources, configuredItemSources,
                loadingIssues
        );
    }

    public RawDefinitionSet withLayout(ResourceLocation id, JsonObject definition, String source) {
        LinkedHashMap<ResourceLocation, JsonObject> nextDefinitions = new LinkedHashMap<>(layouts);
        nextDefinitions.put(id, definition);
        LinkedHashMap<ResourceLocation, String> nextSources = new LinkedHashMap<>(layoutSources);
        nextSources.put(id, source);
        return new RawDefinitionSet(
                rarities, talents, pools, counters, nextDefinitions, configuredItems,
                raritySources, talentSources, poolSources, counterSources, nextSources, configuredItemSources,
                loadingIssues
        );
    }

    public RawDefinitionSet withConfiguredItem(ResourceLocation id, JsonObject definition, String source) {
        LinkedHashMap<ResourceLocation, JsonObject> nextDefinitions = new LinkedHashMap<>(configuredItems);
        nextDefinitions.put(id, definition);
        LinkedHashMap<ResourceLocation, String> nextSources = new LinkedHashMap<>(configuredItemSources);
        nextSources.put(id, source);
        return new RawDefinitionSet(
                rarities, talents, pools, counters, layouts, nextDefinitions,
                raritySources, talentSources, poolSources, counterSources, layoutSources, nextSources,
                loadingIssues
        );
    }

    public boolean contains(DefinitionKind kind, ResourceLocation id) {
        return definitions(kind).containsKey(id);
    }

    public String source(DefinitionKind kind, ResourceLocation id) {
        return sources(kind).getOrDefault(id, "unknown");
    }

    public RawDefinitionSet without(DefinitionKind kind, ResourceLocation id) {
        LinkedHashMap<ResourceLocation, JsonObject> nextRarities = new LinkedHashMap<>(rarities);
        LinkedHashMap<ResourceLocation, JsonObject> nextTalents = new LinkedHashMap<>(talents);
        LinkedHashMap<ResourceLocation, JsonObject> nextPools = new LinkedHashMap<>(pools);
        LinkedHashMap<ResourceLocation, JsonObject> nextCounters = new LinkedHashMap<>(counters);
        LinkedHashMap<ResourceLocation, JsonObject> nextLayouts = new LinkedHashMap<>(layouts);
        LinkedHashMap<ResourceLocation, JsonObject> nextConfiguredItems = new LinkedHashMap<>(configuredItems);
        LinkedHashMap<ResourceLocation, String> nextRaritySources = new LinkedHashMap<>(raritySources);
        LinkedHashMap<ResourceLocation, String> nextTalentSources = new LinkedHashMap<>(talentSources);
        LinkedHashMap<ResourceLocation, String> nextPoolSources = new LinkedHashMap<>(poolSources);
        LinkedHashMap<ResourceLocation, String> nextCounterSources = new LinkedHashMap<>(counterSources);
        LinkedHashMap<ResourceLocation, String> nextLayoutSources = new LinkedHashMap<>(layoutSources);
        LinkedHashMap<ResourceLocation, String> nextConfiguredItemSources = new LinkedHashMap<>(configuredItemSources);
        switch (kind) {
            case RARITY -> {
                nextRarities.remove(id);
                nextRaritySources.remove(id);
            }
            case TALENT -> {
                nextTalents.remove(id);
                nextTalentSources.remove(id);
            }
            case POOL -> {
                nextPools.remove(id);
                nextPoolSources.remove(id);
            }
            case COUNTER -> {
                nextCounters.remove(id);
                nextCounterSources.remove(id);
            }
            case LAYOUT -> {
                nextLayouts.remove(id);
                nextLayoutSources.remove(id);
            }
            case ITEM -> {
                nextConfiguredItems.remove(id);
                nextConfiguredItemSources.remove(id);
            }
            case TALENT_CATEGORY, COUNTER_CATEGORY -> throw new IllegalArgumentException(
                    "Category indexes are not file-backed definitions"
            );
        }
        return new RawDefinitionSet(
                nextRarities, nextTalents, nextPools, nextCounters, nextLayouts, nextConfiguredItems,
                nextRaritySources, nextTalentSources, nextPoolSources, nextCounterSources, nextLayoutSources,
                nextConfiguredItemSources, loadingIssues
        );
    }

    private Map<ResourceLocation, JsonObject> definitions(DefinitionKind kind) {
        return switch (kind) {
            case RARITY -> rarities;
            case TALENT -> talents;
            case POOL -> pools;
            case COUNTER -> counters;
            case LAYOUT -> layouts;
            case ITEM -> configuredItems;
            case TALENT_CATEGORY, COUNTER_CATEGORY -> Map.of();
        };
    }

    private Map<ResourceLocation, String> sources(DefinitionKind kind) {
        return switch (kind) {
            case RARITY -> raritySources;
            case TALENT -> talentSources;
            case POOL -> poolSources;
            case COUNTER -> counterSources;
            case LAYOUT -> layoutSources;
            case ITEM -> configuredItemSources;
            case TALENT_CATEGORY, COUNTER_CATEGORY -> Map.of();
        };
    }

    private static Map<ResourceLocation, JsonObject> immutableJsonCopy(Map<ResourceLocation, JsonObject> source) {
        LinkedHashMap<ResourceLocation, JsonObject> copy = new LinkedHashMap<>();
        source.forEach((id, json) -> copy.put(id, json.deepCopy()));
        return Map.copyOf(copy);
    }

    private static Map<ResourceLocation, String> immutableSourceCopy(
            Map<ResourceLocation, String> source,
            Map<ResourceLocation, JsonObject> definitions
    ) {
        LinkedHashMap<ResourceLocation, String> copy = new LinkedHashMap<>();
        source.forEach((id, value) -> {
            if (definitions.containsKey(id) && value != null && !value.isBlank()) copy.put(id, value);
        });
        return Map.copyOf(copy);
    }
}
