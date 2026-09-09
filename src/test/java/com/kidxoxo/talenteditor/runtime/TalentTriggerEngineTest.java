package com.kidxoxo.talenteditor.runtime;

import com.kidxoxo.talenteditor.domain.ActionTarget;
import com.kidxoxo.talenteditor.domain.AddEffectAction;
import com.kidxoxo.talenteditor.domain.CompiledCondition;
import com.kidxoxo.talenteditor.domain.CompiledTrigger;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TalentTriggerEngineTest {
    @Test
    void selectsOwnedMatchingTriggersInDeterministicPriorityOrder() {
        ResourceLocation firstTalent = id("example:first");
        ResourceLocation secondTalent = id("example:second");
        ResourceLocation missingTalent = id("example:missing");
        ResourceLocation undead = id("minecraft:undead");
        AddEffectAction low = action("minecraft:slowness");
        AddEffectAction high = action("minecraft:poison");
        AddEffectAction ignored = action("minecraft:weakness");
        List<TriggerBinding> bindings = List.of(
                binding(firstTalent, "low", 1, new CompiledCondition.Always(true), low),
                binding(secondTalent, "high", 50, new CompiledCondition.EventEntityTag(undead), high),
                binding(missingTalent, "not_owned", 100, new CompiledCondition.Always(true), ignored),
                binding(firstTalent, "wrong_tag", 90,
                        new CompiledCondition.EventEntityTag(id("minecraft:raiders")), ignored)
        );
        ConditionEvaluator.Context context = new ConditionEvaluator.Context(
                Set.of(firstTalent, secondTalent), java.util.Map.of(), java.util.Map.of(),
                java.util.Map.of(), java.util.Map.of(), java.util.Optional.empty(), Set.of(undead), Set.of()
        );

        assertEquals(List.of(high, low), TalentTriggerEngine.select(
                bindings, Set.of(firstTalent, secondTalent), context
        ));
    }

    @Test
    void appliesChanceAndCooldownBoundariesDeterministically() {
        assertFalse(TalentTriggerEngine.chancePasses(0.0D, 0.0D));
        assertTrue(TalentTriggerEngine.chancePasses(1.0D, 0.999999D));
        assertTrue(TalentTriggerEngine.chancePasses(0.25D, 0.249999D));
        assertFalse(TalentTriggerEngine.chancePasses(0.25D, 0.25D));

        assertFalse(TalentTriggerEngine.coolingDown(-1L, 100L, 20));
        assertTrue(TalentTriggerEngine.coolingDown(100L, 119L, 20));
        assertFalse(TalentTriggerEngine.coolingDown(100L, 120L, 20));
        assertFalse(TalentTriggerEngine.coolingDown(100L, 99L, 20));
        assertFalse(TalentTriggerEngine.coolingDown(100L, 101L, 0));

        assertFalse(TalentTriggerEngine.activationLimitReached(999, 0));
        assertFalse(TalentTriggerEngine.activationLimitReached(2, 3));
        assertTrue(TalentTriggerEngine.activationLimitReached(3, 3));
        assertTrue(TalentTriggerEngine.activationLimitReached(4, 3));
    }

    @Test
    void derivesAndClampsChanceFromAnAttributeValue() {
        assertEquals(0.45D, TalentTriggerEngine.effectiveChance(0.25D, 10.0D, 0.02D), 0.000001D);
        assertEquals(1.0D, TalentTriggerEngine.effectiveChance(0.9D, 20.0D, 0.1D));
        assertEquals(0.0D, TalentTriggerEngine.effectiveChance(0.1D, 20.0D, -0.1D));
        assertEquals(1.0D, TalentTriggerEngine.effectiveChance(1.0D, Double.MAX_VALUE, 1.0D));
    }

    private static TriggerBinding binding(
            ResourceLocation talent, String id, int priority,
            CompiledCondition condition, AddEffectAction action
    ) {
        return new TriggerBinding(talent, new CompiledTrigger(
                id, TalentEventTypes.DAMAGE_DEALT, priority, condition, List.of(action)
        ));
    }

    private static AddEffectAction action(String effect) {
        return new AddEffectAction(id(effect), 100, 0, ActionTarget.EVENT_ENTITY, false, true, true);
    }

    private static ResourceLocation id(String value) {
        return new ResourceLocation(value);
    }
}
