package com.kidxoxo.talenteditor.runtime;

import com.kidxoxo.talenteditor.domain.AttributeModifierAction;
import com.kidxoxo.talenteditor.domain.AddEffectAction;
import com.kidxoxo.talenteditor.domain.CompiledAction;
import com.kidxoxo.talenteditor.domain.CompiledTrigger;
import com.kidxoxo.talenteditor.domain.CompiledCondition;
import com.kidxoxo.talenteditor.domain.ConfiguredItemDefinition;
import com.kidxoxo.talenteditor.domain.CounterEventType;
import com.kidxoxo.talenteditor.domain.CounterRuleDefinition;
import com.kidxoxo.talenteditor.domain.CounterVisibility;
import com.kidxoxo.talenteditor.domain.EffectImmunityAction;
import com.kidxoxo.talenteditor.domain.DynamicAttributeModifierAction;
import com.kidxoxo.talenteditor.domain.DefinitionKind;
import com.kidxoxo.talenteditor.domain.DefinitionReference;
import com.kidxoxo.talenteditor.domain.DefinitionTarget;
import com.kidxoxo.talenteditor.domain.RarityDefinition;
import com.kidxoxo.talenteditor.domain.TalentDefinition;
import com.kidxoxo.talenteditor.domain.TalentBookLayoutDefinition;
import com.kidxoxo.talenteditor.domain.TalentPoolDefinition;
import com.kidxoxo.talenteditor.loot.ConfiguredItemLootBinding;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.LinkedHashSet;
import java.util.Set;

public record RuntimeSnapshot(
        long revision,
        String contentHash,
        Map<ResourceLocation, RarityDefinition> rarities,
        Map<ResourceLocation, TalentPoolDefinition> pools,
        Map<ResourceLocation, TalentDefinition> talents,
        Map<ResourceLocation, CounterRuleDefinition> counters,
        Map<ResourceLocation, TalentBookLayoutDefinition> layouts,
        Map<ResourceLocation, ConfiguredItemDefinition> configuredItems,
        TalentBookLayoutDefinition bookLayout,
        Map<ResourceLocation, List<ActiveAttributeBinding>> attributeBindings,
        Map<ResourceLocation, List<ActiveEffectImmunityBinding>> effectImmunityBindings,
        Map<ResourceLocation, List<ActiveStatusEffectBinding>> activeStatusEffectBindings,
        Map<ResourceLocation, List<TriggerBinding>> triggersByEvent,
        Map<CounterEventType, List<CounterRuleDefinition>> countersByEvent,
        Map<ResourceLocation, List<CounterRuleDefinition>> countersByCategory,
        Set<ResourceLocation> counterBookIds,
        Map<ResourceLocation, List<ConfiguredItemLootBinding>> configuredItemLootByTable,
        List<ConfiguredItemLootBinding> configuredItemLootPatterns,
        Map<DefinitionTarget, List<DefinitionReference>> reverseReferences
) {
    public RuntimeSnapshot {
        rarities = Collections.unmodifiableMap(new LinkedHashMap<>(rarities));
        pools = Collections.unmodifiableMap(new LinkedHashMap<>(pools));
        talents = Collections.unmodifiableMap(new LinkedHashMap<>(talents));
        counters = Collections.unmodifiableMap(new LinkedHashMap<>(counters));
        layouts = Collections.unmodifiableMap(new LinkedHashMap<>(layouts));
        configuredItems = Collections.unmodifiableMap(new LinkedHashMap<>(configuredItems));
        attributeBindings = immutableListMap(attributeBindings);
        effectImmunityBindings = immutableListMap(effectImmunityBindings);
        activeStatusEffectBindings = immutableListMap(activeStatusEffectBindings);
        triggersByEvent = immutableListMap(triggersByEvent);
        countersByEvent = immutableListMap(countersByEvent);
        countersByCategory = immutableListMap(countersByCategory);
        counterBookIds = Collections.unmodifiableSet(new LinkedHashSet<>(counterBookIds));
        configuredItemLootByTable = immutableListMap(configuredItemLootByTable);
        configuredItemLootPatterns = List.copyOf(configuredItemLootPatterns);
        reverseReferences = immutableListMap(reverseReferences);
    }

    public static RuntimeSnapshot candidate(
            String contentHash,
            Map<ResourceLocation, RarityDefinition> rarities,
            Map<ResourceLocation, TalentPoolDefinition> pools,
            Map<ResourceLocation, TalentDefinition> talents,
            Map<ResourceLocation, CounterRuleDefinition> counters,
            Map<ResourceLocation, TalentBookLayoutDefinition> layouts,
            Map<ResourceLocation, ConfiguredItemDefinition> configuredItems
    ) {
        LinkedHashMap<ResourceLocation, List<ActiveAttributeBinding>> attributes = new LinkedHashMap<>();
        LinkedHashMap<ResourceLocation, List<ActiveEffectImmunityBinding>> immunities = new LinkedHashMap<>();
        LinkedHashMap<ResourceLocation, List<ActiveStatusEffectBinding>> statusEffects = new LinkedHashMap<>();
        LinkedHashMap<ResourceLocation, List<TriggerBinding>> triggers = new LinkedHashMap<>();
        LinkedHashMap<CounterEventType, List<CounterRuleDefinition>> counterIndex = new LinkedHashMap<>();
        LinkedHashMap<ResourceLocation, List<CounterRuleDefinition>> counterCategoryIndex = new LinkedHashMap<>();
        LinkedHashMap<ResourceLocation, List<ConfiguredItemLootBinding>> configuredItemLoot = new LinkedHashMap<>();
        ArrayList<ConfiguredItemLootBinding> configuredItemLootPatterns = new ArrayList<>();

        talents.values().stream()
                .filter(TalentDefinition::enabled)
                .filter(TalentDefinition::available)
                .forEach(talent -> {
                    for (CompiledAction action : talent.activeEffects()) {
                        if (action instanceof AttributeModifierAction attributeAction) {
                            attributes.computeIfAbsent(attributeAction.attribute(), ignored -> new ArrayList<>())
                                    .add(new ActiveAttributeBinding(talent.id(), attributeAction));
                        } else if (action instanceof EffectImmunityAction immunityAction) {
                            immunities.computeIfAbsent(immunityAction.effect(), ignored -> new ArrayList<>())
                                    .add(new ActiveEffectImmunityBinding(talent.id(), immunityAction));
                        } else if (action instanceof AddEffectAction effectAction) {
                            statusEffects.computeIfAbsent(effectAction.effect(), ignored -> new ArrayList<>())
                                    .add(new ActiveStatusEffectBinding(talent.id(), effectAction));
                        }
                    }
                    for (CompiledTrigger trigger : talent.triggers()) {
                        triggers.computeIfAbsent(trigger.event(), ignored -> new ArrayList<>())
                                .add(new TriggerBinding(talent.id(), trigger));
                    }
                });

        counters.values().stream()
                .filter(CounterRuleDefinition::enabled)
                .filter(CounterRuleDefinition::available)
                .forEach(counter -> {
                    counterIndex.computeIfAbsent(counter.event(), ignored -> new ArrayList<>()).add(counter);
                    counter.categories().forEach(category -> counterCategoryIndex
                            .computeIfAbsent(category, ignored -> new ArrayList<>()).add(counter));
                });

        configuredItems.values().stream()
                .filter(ConfiguredItemDefinition::enabled)
                .filter(ConfiguredItemDefinition::available)
                .forEach(item -> item.loot().forEach(rule -> {
                    ConfiguredItemLootBinding binding = new ConfiguredItemLootBinding(item.id(), rule);
                    rule.exactTable().ifPresentOrElse(
                            table -> configuredItemLoot.computeIfAbsent(table, ignored -> new ArrayList<>())
                                    .add(binding),
                            () -> configuredItemLootPatterns.add(binding)
                    );
                }));

        LinkedHashSet<ResourceLocation> referencedCounters = new LinkedHashSet<>();
        LinkedHashSet<ResourceLocation> referencedCategories = new LinkedHashSet<>();
        talents.values().stream().filter(TalentDefinition::enabled).filter(TalentDefinition::available)
                .forEach(talent -> {
                    collectCounterReferences(talent.acquireCondition(), referencedCounters, referencedCategories);
                    if (talent.synthesis().enabled()) {
                        referencedCounters.addAll(talent.synthesis().consumeCounters().keySet());
                    }
                    talent.activeEffects().stream()
                            .filter(DynamicAttributeModifierAction.class::isInstance)
                            .map(DynamicAttributeModifierAction.class::cast)
                            .forEach(action -> action.counter().ifPresent(referencedCounters::add));
                    talent.triggers().forEach(trigger -> collectCounterReferences(
                            trigger.condition(), referencedCounters, referencedCategories
                    ));
                });
        pools.values().stream().filter(TalentPoolDefinition::enabled)
                .forEach(pool -> pool.duplicateCounter().ifPresent(referencedCounters::add));
        LinkedHashSet<ResourceLocation> counterBookIds = new LinkedHashSet<>();
        counters.values().forEach(counter -> {
            boolean categoryReferenced = counter.categories().stream().anyMatch(referencedCategories::contains);
            if (counter.visibility() == CounterVisibility.FULL
                    || counter.visibility() == CounterVisibility.CONDITION_ONLY
                    && (referencedCounters.contains(counter.id()) || categoryReferenced)) {
                counterBookIds.add(counter.id());
            }
        });

        TalentBookLayoutDefinition activeLayout = layouts.values().stream()
                .filter(TalentBookLayoutDefinition::enabled)
                .sorted(Comparator.comparingInt(TalentBookLayoutDefinition::priority).reversed()
                        .thenComparing(layout -> layout.id().toString()))
                .findFirst()
                .orElse(TalentBookLayoutDefinition.DEFAULT);

        return new RuntimeSnapshot(
                0L, contentHash, rarities, pools, talents, counters, layouts, configuredItems, activeLayout,
                attributes, immunities, statusEffects, triggers, counterIndex, counterCategoryIndex, counterBookIds,
                configuredItemLoot, configuredItemLootPatterns,
                DefinitionReferenceIndex.build(
                        rarities, pools, talents, counters, layouts, configuredItems.keySet()
                )
        );
    }

    public static RuntimeSnapshot candidate(
            String contentHash,
            Map<ResourceLocation, RarityDefinition> rarities,
            Map<ResourceLocation, TalentPoolDefinition> pools,
            Map<ResourceLocation, TalentDefinition> talents,
            Map<ResourceLocation, CounterRuleDefinition> counters,
            Map<ResourceLocation, TalentBookLayoutDefinition> layouts
    ) {
        return candidate(contentHash, rarities, pools, talents, counters, layouts, Map.of());
    }

    public static RuntimeSnapshot candidate(
            String contentHash,
            Map<ResourceLocation, RarityDefinition> rarities,
            Map<ResourceLocation, TalentPoolDefinition> pools,
            Map<ResourceLocation, TalentDefinition> talents,
            Map<ResourceLocation, CounterRuleDefinition> counters
    ) {
        return candidate(contentHash, rarities, pools, talents, counters, Map.of());
    }

    public static RuntimeSnapshot candidate(
            String contentHash,
            Map<ResourceLocation, RarityDefinition> rarities,
            Map<ResourceLocation, TalentPoolDefinition> pools,
            Map<ResourceLocation, TalentDefinition> talents
    ) {
        return candidate(contentHash, rarities, pools, talents, Map.of(), Map.of());
    }

    public static RuntimeSnapshot empty() {
        return candidate("0".repeat(64), Map.of(), Map.of(), Map.of(), Map.of(), Map.of());
    }

    public RuntimeSnapshot withRevision(long nextRevision) {
        return new RuntimeSnapshot(
                nextRevision, contentHash, rarities, pools, talents, counters, layouts, configuredItems, bookLayout,
                attributeBindings, effectImmunityBindings, activeStatusEffectBindings, triggersByEvent,
                countersByEvent, countersByCategory, counterBookIds, configuredItemLootByTable,
                configuredItemLootPatterns, reverseReferences
        );
    }

    public List<DefinitionReference> referencesTo(DefinitionKind kind, ResourceLocation id) {
        return reverseReferences.getOrDefault(new DefinitionTarget(kind, id), List.of());
    }

    private static void collectCounterReferences(
            CompiledCondition condition,
            Set<ResourceLocation> counters,
            Set<ResourceLocation> categories
    ) {
        if (condition instanceof CompiledCondition.CounterAtLeast counter) {
            counters.add(counter.counter());
        } else if (condition instanceof CompiledCondition.CounterCategoryTotalAtLeast category) {
            categories.add(category.category());
        } else if (condition instanceof CompiledCondition.All all) {
            all.children().forEach(child -> collectCounterReferences(child, counters, categories));
        } else if (condition instanceof CompiledCondition.Any any) {
            any.children().forEach(child -> collectCounterReferences(child, counters, categories));
        } else if (condition instanceof CompiledCondition.Not not) {
            collectCounterReferences(not.child(), counters, categories);
        }
    }

    private static <K, V> Map<K, List<V>> immutableListMap(Map<K, List<V>> source) {
        LinkedHashMap<K, List<V>> copy = new LinkedHashMap<>();
        source.forEach((key, value) -> copy.put(key, List.copyOf(value)));
        return Collections.unmodifiableMap(copy);
    }
}
