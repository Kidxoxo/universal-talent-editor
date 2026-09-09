package com.kidxoxo.talenteditor.runtime;

import com.kidxoxo.talenteditor.domain.CompiledCondition;
import com.kidxoxo.talenteditor.domain.DefinitionKind;
import com.kidxoxo.talenteditor.domain.DefinitionReference;
import com.kidxoxo.talenteditor.domain.DefinitionTarget;
import com.kidxoxo.talenteditor.domain.CounterRuleDefinition;
import com.kidxoxo.talenteditor.domain.DrawRefreshBonusAction;
import com.kidxoxo.talenteditor.domain.DynamicAttributeModifierAction;
import com.kidxoxo.talenteditor.domain.RarityDefinition;
import com.kidxoxo.talenteditor.domain.QueueDrawConstraintAction;
import com.kidxoxo.talenteditor.domain.TalentDefinition;
import com.kidxoxo.talenteditor.domain.TalentBookLayoutDefinition;
import com.kidxoxo.talenteditor.domain.TalentPoolDefinition;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

final class DefinitionReferenceIndex {
    private DefinitionReferenceIndex() {
    }

    static Map<DefinitionTarget, List<DefinitionReference>> build(
            Map<ResourceLocation, RarityDefinition> rarities,
            Map<ResourceLocation, TalentPoolDefinition> pools,
            Map<ResourceLocation, TalentDefinition> talents,
            Map<ResourceLocation, CounterRuleDefinition> counters,
            Map<ResourceLocation, TalentBookLayoutDefinition> layouts,
            Set<ResourceLocation> configuredItems
    ) {
        LinkedHashMap<DefinitionTarget, List<DefinitionReference>> references = new LinkedHashMap<>();
        talents.values().stream().sorted(byId(TalentDefinition::id))
                .forEach(talent -> indexTalent(references, talent, configuredItems));
        pools.values().stream().sorted(byId(TalentPoolDefinition::id))
                .forEach(pool -> indexPool(references, pool));
        counters.values().stream().sorted(byId(CounterRuleDefinition::id))
                .forEach(counter -> indexCounter(references, counter, configuredItems));
        layouts.values().stream().sorted(byId(TalentBookLayoutDefinition::id))
                .forEach(layout -> indexLayout(references, layout));

        LinkedHashMap<DefinitionTarget, List<DefinitionReference>> immutable = new LinkedHashMap<>();
        references.entrySet().stream()
                .sorted(Comparator.<Map.Entry<DefinitionTarget, List<DefinitionReference>>>comparingInt(
                                entry -> entry.getKey().kind().ordinal())
                        .thenComparing(entry -> entry.getKey().id().toString()))
                .forEach(entry -> immutable.put(entry.getKey(), entry.getValue().stream()
                        .sorted(Comparator.comparing((DefinitionReference value) -> value.sourceKind().ordinal())
                                .thenComparing(value -> value.sourceId().toString())
                                .thenComparing(DefinitionReference::path))
                        .toList()));
        return Collections.unmodifiableMap(immutable);
    }

    private static void indexTalent(
            Map<DefinitionTarget, List<DefinitionReference>> references,
            TalentDefinition talent,
            Set<ResourceLocation> configuredItems
    ) {
        add(references, DefinitionKind.RARITY, talent.rarity(), DefinitionKind.TALENT, talent.id(),
                "talent_rarity", "$.rarity");
        for (int index = 0; index < talent.categories().size(); index++) {
            add(references, DefinitionKind.TALENT_CATEGORY, talent.categories().get(index),
                    DefinitionKind.TALENT, talent.id(), "talent_category_member", "$.categories[" + index + "]");
        }
        for (int index = 0; index < talent.offer().pools().size(); index++) {
            add(references, DefinitionKind.POOL, talent.offer().pools().get(index),
                    DefinitionKind.TALENT, talent.id(), "offer_pool", "$.offer.pools[" + index + "]");
        }
        for (int actionIndex = 0; actionIndex < talent.activeEffects().size(); actionIndex++) {
            var action = talent.activeEffects().get(actionIndex);
            if (action instanceof DrawRefreshBonusAction bonus) {
                for (int poolIndex = 0; poolIndex < bonus.pools().size(); poolIndex++) {
                    add(references, DefinitionKind.POOL, bonus.pools().get(poolIndex),
                            DefinitionKind.TALENT, talent.id(), "refresh_bonus_pool",
                            "$.activeEffects[" + actionIndex + "].pools[" + poolIndex + "]");
                }
            }
            if (action instanceof DynamicAttributeModifierAction dynamic) {
                if (dynamic.counter().isPresent()) {
                    add(references, DefinitionKind.COUNTER, dynamic.counter().orElseThrow(),
                            DefinitionKind.TALENT, talent.id(), "dynamic_attribute_counter",
                            "$.activeEffects[" + actionIndex + "].counter");
                }
            }
        }
        indexCondition(references, talent.id(), talent.acquireCondition(), "$.acquireCondition", configuredItems);
        for (int index = 0; index < talent.synthesis().consumeTalents().size(); index++) {
            add(references, DefinitionKind.TALENT, talent.synthesis().consumeTalents().get(index),
                    DefinitionKind.TALENT, talent.id(), "synthesis_talent",
                    "$.synthesis.consumeTalents[" + index + "]");
        }
        talent.synthesis().consumeCounters().keySet().forEach(counter -> add(
                references, DefinitionKind.COUNTER, counter, DefinitionKind.TALENT, talent.id(),
                "synthesis_counter", "$.synthesis.consumeCounters[\"" + counter + "\"]"
        ));
        talent.synthesis().consumeItems().keySet().stream()
                .filter(configuredItems::contains)
                .forEach(item -> add(
                        references, DefinitionKind.ITEM, item, DefinitionKind.TALENT, talent.id(),
                        "synthesis_item", "$.synthesis.consumeItems[\"" + item + "\"]"
                ));
        for (int index = 0; index < talent.triggers().size(); index++) {
            var trigger = talent.triggers().get(index);
            if (trigger.chanceCounter().isPresent()) {
                add(references, DefinitionKind.COUNTER, trigger.chanceCounter().orElseThrow(),
                        DefinitionKind.TALENT, talent.id(), "trigger_chance_counter",
                        "$.triggers[" + index + "].chanceCounter");
            }
            if (trigger.chanceCategory().isPresent()) {
                add(references, DefinitionKind.TALENT_CATEGORY, trigger.chanceCategory().orElseThrow(),
                        DefinitionKind.TALENT, talent.id(), "trigger_chance_category",
                        "$.triggers[" + index + "].chanceCategory");
            }
            indexCondition(references, talent.id(), trigger.condition(),
                    "$.triggers[" + index + "].condition", configuredItems);
            for (int actionIndex = 0; actionIndex < trigger.actions().size(); actionIndex++) {
                if (trigger.actions().get(actionIndex)
                        instanceof QueueDrawConstraintAction constraint) {
                    indexDrawConstraint(references, talent.id(), constraint,
                            "$.triggers[" + index + "].actions[" + actionIndex + "]");
                }
            }
            for (int actionIndex = 0; actionIndex < trigger.failureActions().size(); actionIndex++) {
                if (trigger.failureActions().get(actionIndex)
                        instanceof QueueDrawConstraintAction constraint) {
                    indexDrawConstraint(references, talent.id(), constraint,
                            "$.triggers[" + index + "].failureActions[" + actionIndex + "]");
                }
            }
        }
    }

    private static void indexDrawConstraint(
            Map<DefinitionTarget, List<DefinitionReference>> references,
            ResourceLocation sourceId,
            QueueDrawConstraintAction constraint,
            String path
    ) {
        indexDrawConstraintSpec(references, sourceId, constraint.spec(), path);
        constraint.successCounter().ifPresent(counter -> add(
                references, DefinitionKind.COUNTER, counter,
                DefinitionKind.TALENT, sourceId, "draw_constraint_success_counter",
                path + ".successCounter"
        ));
        constraint.milestone().ifPresent(spec ->
                indexDrawConstraintSpec(references, sourceId, spec, path + ".milestone"));
    }

    private static void indexDrawConstraintSpec(
            Map<DefinitionTarget, List<DefinitionReference>> references,
            ResourceLocation sourceId,
            com.kidxoxo.talenteditor.domain.DrawConstraintSpec constraint,
            String path
    ) {
        for (int index = 0; index < constraint.talents().size(); index++) {
            add(references, DefinitionKind.TALENT, constraint.talents().get(index),
                    DefinitionKind.TALENT, sourceId, "draw_constraint_talent",
                    path + ".talents[" + index + "]");
        }
        for (int index = 0; index < constraint.categories().size(); index++) {
            add(references, DefinitionKind.TALENT_CATEGORY, constraint.categories().get(index),
                    DefinitionKind.TALENT, sourceId, "draw_constraint_category",
                    path + ".categories[" + index + "]");
        }
        for (int index = 0; index < constraint.rarities().size(); index++) {
            add(references, DefinitionKind.RARITY, constraint.rarities().get(index),
                    DefinitionKind.TALENT, sourceId, "draw_constraint_rarity",
                    path + ".rarities[" + index + "]");
        }
        for (int index = 0; index < constraint.pools().size(); index++) {
            add(references, DefinitionKind.POOL, constraint.pools().get(index),
                    DefinitionKind.TALENT, sourceId, "draw_constraint_pool",
                    path + ".pools[" + index + "]");
        }
    }

    private static void indexPool(
            Map<DefinitionTarget, List<DefinitionReference>> references,
            TalentPoolDefinition pool
    ) {
        pool.rarityWeights().keySet().forEach(rarity -> add(
                references, DefinitionKind.RARITY, rarity, DefinitionKind.POOL, pool.id(),
                "pool_rarity_weight", "$.rarityWeights[\"" + rarity + "\"]"
        ));
        pool.luckFactors().keySet().forEach(rarity -> add(
                references, DefinitionKind.RARITY, rarity, DefinitionKind.POOL, pool.id(),
                "pool_luck_factor", "$.luckFactors[\"" + rarity + "\"]"
        ));
        pool.pityRarity().ifPresent(rarity -> add(
                references, DefinitionKind.RARITY, rarity, DefinitionKind.POOL, pool.id(),
                "pool_pity_rarity", "$.pityRarity"
        ));
        pool.duplicateCounter().ifPresent(counter -> add(
                references, DefinitionKind.COUNTER, counter, DefinitionKind.POOL, pool.id(),
                "pool_duplicate_counter", "$.duplicateCounter"
        ));
    }

    private static void indexCounter(
            Map<DefinitionTarget, List<DefinitionReference>> references,
            CounterRuleDefinition counter,
            Set<ResourceLocation> configuredItems
    ) {
        for (int index = 0; index < counter.categories().size(); index++) {
            add(references, DefinitionKind.COUNTER_CATEGORY, counter.categories().get(index),
                    DefinitionKind.COUNTER, counter.id(), "counter_category_member",
                    "$.categories[" + index + "]");
        }
        for (int index = 0; index < counter.requiredTalents().size(); index++) {
            add(references, DefinitionKind.TALENT, counter.requiredTalents().get(index),
                    DefinitionKind.COUNTER, counter.id(), "counter_required_talent",
                    "$.requiredTalents[" + index + "]");
        }
        for (int index = 0; index < counter.items().size(); index++) {
            ResourceLocation item = counter.items().get(index);
            if (configuredItems.contains(item)) {
                add(references, DefinitionKind.ITEM, item, DefinitionKind.COUNTER, counter.id(),
                        "counter_item_filter", "$.items[" + index + "]");
            }
        }
    }

    private static void indexLayout(
            Map<DefinitionTarget, List<DefinitionReference>> references,
            TalentBookLayoutDefinition layout
    ) {
        for (int pageIndex = 0; pageIndex < layout.pages().size(); pageIndex++) {
            TalentBookLayoutDefinition.PageDefinition page = layout.pages().get(pageIndex);
            for (int slotIndex = 0; slotIndex < page.slots().size(); slotIndex++) {
                java.util.Optional<ResourceLocation> talent = page.slots().get(slotIndex).talent();
                if (talent.isPresent()) {
                    add(references, DefinitionKind.TALENT, talent.orElseThrow(),
                            DefinitionKind.LAYOUT, layout.id(), "layout_fixed_talent",
                            "$.pages[" + pageIndex + "].slots[" + slotIndex + "].talent");
                }
            }
        }
    }

    private static void indexCondition(
            Map<DefinitionTarget, List<DefinitionReference>> references,
            ResourceLocation sourceId,
            CompiledCondition condition,
            String path,
            Set<ResourceLocation> configuredItems
    ) {
        if (condition instanceof CompiledCondition.HasTalent value) {
            add(references, DefinitionKind.TALENT, value.talent(), DefinitionKind.TALENT, sourceId,
                    "condition_talent", path + ".talent");
        } else if (condition instanceof CompiledCondition.CounterAtLeast value) {
            add(references, DefinitionKind.COUNTER, value.counter(), DefinitionKind.TALENT, sourceId,
                    "condition_counter", path + ".counter");
        } else if (condition instanceof CompiledCondition.CategoryCountAtLeast value) {
            add(references, DefinitionKind.TALENT_CATEGORY, value.category(), DefinitionKind.TALENT, sourceId,
                    "condition_talent_category", path + ".category");
        } else if (condition instanceof CompiledCondition.CounterCategoryTotalAtLeast value) {
            add(references, DefinitionKind.COUNTER_CATEGORY, value.category(), DefinitionKind.TALENT, sourceId,
                    "condition_counter_category", path + ".category");
        } else if (condition instanceof CompiledCondition.HasItem value
                && configuredItems.contains(value.item())) {
            add(references, DefinitionKind.ITEM, value.item(), DefinitionKind.TALENT, sourceId,
                    "condition_item", path + ".item");
        } else if (condition instanceof CompiledCondition.EventItem value
                && configuredItems.contains(value.item())) {
            add(references, DefinitionKind.ITEM, value.item(), DefinitionKind.TALENT, sourceId,
                    "condition_item", path + ".item");
        } else if (condition instanceof CompiledCondition.All value) {
            for (int index = 0; index < value.children().size(); index++) {
                indexCondition(references, sourceId, value.children().get(index),
                        path + ".children[" + index + "]", configuredItems);
            }
        } else if (condition instanceof CompiledCondition.Any value) {
            for (int index = 0; index < value.children().size(); index++) {
                indexCondition(references, sourceId, value.children().get(index),
                        path + ".children[" + index + "]", configuredItems);
            }
        } else if (condition instanceof CompiledCondition.Not value) {
            indexCondition(references, sourceId, value.child(), path + ".child", configuredItems);
        }
    }

    private static void add(
            Map<DefinitionTarget, List<DefinitionReference>> references,
            DefinitionKind targetKind,
            ResourceLocation targetId,
            DefinitionKind sourceKind,
            ResourceLocation sourceId,
            String usage,
            String path
    ) {
        DefinitionTarget target = new DefinitionTarget(targetKind, targetId);
        references.computeIfAbsent(target, ignored -> new ArrayList<>())
                .add(new DefinitionReference(target, sourceKind, sourceId, usage, path));
    }

    private static <T> Comparator<T> byId(java.util.function.Function<T, ResourceLocation> id) {
        return Comparator.comparing(value -> id.apply(value).toString());
    }
}
