package com.kidxoxo.talenteditor.runtime;

import com.kidxoxo.talenteditor.domain.CompiledCondition;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ConditionEvaluatorTest {
    @Test
    void evaluatesTalentCounterItemAndCompositeConditions() {
        ResourceLocation talent = id("example:starter");
        ResourceLocation counter = id("example:kills");
        ResourceLocation item = id("minecraft:diamond");
        ResourceLocation category = id("example:combat");
        CompiledCondition condition = new CompiledCondition.All(List.of(
                new CompiledCondition.HasTalent(talent),
                new CompiledCondition.CounterAtLeast(counter, 10L),
                new CompiledCondition.CategoryCountAtLeast(category, 2),
                new CompiledCondition.Any(List.of(
                        new CompiledCondition.HasItem(item, 3),
                        new CompiledCondition.Not(new CompiledCondition.Always(true))
                ))
        ));

        assertTrue(ConditionEvaluator.test(condition, new ConditionEvaluator.Context(
                Set.of(talent), Map.of(counter, 10L), Map.of(item, 3), Map.of(category, 2)
        )));
        assertFalse(ConditionEvaluator.test(condition, new ConditionEvaluator.Context(
                Set.of(talent), Map.of(counter, 9L), Map.of(item, 64), Map.of(category, 2)
        )));
        assertFalse(ConditionEvaluator.test(condition, new ConditionEvaluator.Context(
                Set.of(), Map.of(counter, 10L), Map.of(item, 3), Map.of(category, 1)
        )));
    }

    @Test
    void evaluatesCounterCategoryTotalsIndependentlyFromOwnedTalentCategoryCounts() {
        ResourceLocation category = id("example:combat");
        CompiledCondition condition = new CompiledCondition.All(List.of(
                new CompiledCondition.CategoryCountAtLeast(category, 2),
                new CompiledCondition.CounterCategoryTotalAtLeast(category, 15L)
        ));

        assertTrue(ConditionEvaluator.test(condition, new ConditionEvaluator.Context(
                Set.of(), Map.of(), Map.of(), Map.of(category, 2), Map.of(category, 15L)
        )));
        assertFalse(ConditionEvaluator.test(condition, new ConditionEvaluator.Context(
                Set.of(), Map.of(), Map.of(), Map.of(category, 20), Map.of(category, 14L)
        )));
    }

    @Test
    void evaluatesEventEntityAndDamageTagsOnlyFromEventContext() {
        ResourceLocation zombie = id("minecraft:zombie");
        ResourceLocation undead = id("minecraft:undead");
        ResourceLocation fire = id("minecraft:is_fire");
        ConditionEvaluator.Context event = new ConditionEvaluator.Context(
                Set.of(), Map.of(), Map.of(), Map.of(), Map.of(), java.util.Optional.of(zombie),
                Set.of(undead), Set.of(fire)
        );
        assertTrue(ConditionEvaluator.test(new CompiledCondition.EventEntityType(zombie), event));
        assertTrue(ConditionEvaluator.test(new CompiledCondition.EventEntityTag(undead), event));
        assertTrue(ConditionEvaluator.test(new CompiledCondition.DamageTypeTag(fire), event));
        assertFalse(ConditionEvaluator.test(new CompiledCondition.EventEntityTag(id("minecraft:raiders")), event));
        assertFalse(ConditionEvaluator.test(new CompiledCondition.EventEntityType(zombie),
                ConditionEvaluator.Context.empty(Set.of())));
    }

    @Test
    void evaluatesEventItemAndBlockReferencesOnlyFromEventContext() {
        ResourceLocation item = id("minecraft:diamond_pickaxe");
        ResourceLocation itemTag = id("minecraft:pickaxes");
        ResourceLocation block = id("minecraft:stone");
        ResourceLocation blockTag = id("minecraft:mineable/pickaxe");
        ConditionEvaluator.Context event = new ConditionEvaluator.Context(
                Set.of(), Map.of(), Map.of(), Map.of(), Map.of(), java.util.Optional.empty(),
                Set.of(), Set.of(), java.util.Optional.of(item), Set.of(itemTag),
                java.util.Optional.of(block), Set.of(blockTag)
        );

        assertTrue(ConditionEvaluator.test(new CompiledCondition.EventItem(item), event));
        assertTrue(ConditionEvaluator.test(new CompiledCondition.EventItemTag(itemTag), event));
        assertTrue(ConditionEvaluator.test(new CompiledCondition.EventBlock(block), event));
        assertTrue(ConditionEvaluator.test(new CompiledCondition.EventBlockTag(blockTag), event));
        assertFalse(ConditionEvaluator.test(new CompiledCondition.EventItem(id("minecraft:apple")), event));
        assertFalse(ConditionEvaluator.test(new CompiledCondition.EventBlock(block),
                ConditionEvaluator.Context.empty(Set.of())));
    }

    @Test
    void evaluatesSelectedTalentMetadataAndEventEntityEffects() {
        ResourceLocation talent = id("example:selected");
        ResourceLocation category = id("example:luck");
        ResourceLocation rarity = id("example:ur");
        ResourceLocation effect = id("minecraft:slowness");
        ConditionEvaluator.Context event = new ConditionEvaluator.Context(
                Set.of(), Map.of(), Map.of(), Map.of(), Map.of(), java.util.Optional.empty(),
                Set.of(), Set.of(effect), Set.of(), java.util.Optional.of(talent), Set.of(category),
                java.util.Optional.of(rarity), java.util.Optional.empty(), Set.of(),
                java.util.Optional.empty(), Set.of()
        );
        assertTrue(ConditionEvaluator.test(new CompiledCondition.EventTalent(talent), event));
        assertTrue(ConditionEvaluator.test(new CompiledCondition.EventTalentCategory(category), event));
        assertTrue(ConditionEvaluator.test(new CompiledCondition.EventTalentRarity(rarity), event));
        assertTrue(ConditionEvaluator.test(new CompiledCondition.EventEntityEffect(effect), event));
        assertFalse(ConditionEvaluator.test(
                new CompiledCondition.EventTalentCategory(id("example:dark")), event
        ));
    }

    private static ResourceLocation id(String value) {
        return new ResourceLocation(value);
    }
}
