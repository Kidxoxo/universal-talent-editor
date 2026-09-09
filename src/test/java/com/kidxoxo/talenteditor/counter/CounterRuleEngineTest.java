package com.kidxoxo.talenteditor.counter;

import com.kidxoxo.talenteditor.domain.CounterEventType;
import com.kidxoxo.talenteditor.domain.CounterRuleDefinition;
import com.kidxoxo.talenteditor.domain.CounterAmountMode;
import com.kidxoxo.talenteditor.domain.CounterRounding;
import com.kidxoxo.talenteditor.domain.LocalizedText;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CounterRuleEngineTest {
    @Test
    void matchesRequiredTalentsAndOrSelectorsAcrossFilterGroups() {
        CounterRuleDefinition rule = rule();
        CounterRuleEngine.EventContext matching = new CounterRuleEngine.EventContext(
                Set.of(id("example:hunter")), Optional.of(id("minecraft:skeleton")),
                Set.of(id("minecraft:skeletons")), Optional.of(id("minecraft:iron_sword")),
                Set.of(id("minecraft:swords")), Set.of(id("minecraft:is_projectile"))
        );
        CounterRuleEngine.EventContext wrongItem = new CounterRuleEngine.EventContext(
                Set.of(id("example:hunter")), Optional.of(id("minecraft:zombie")), Set.of(),
                Optional.of(id("minecraft:stick")), Set.of(), Set.of(id("minecraft:is_projectile"))
        );
        CounterRuleEngine.EventContext missingTalent = new CounterRuleEngine.EventContext(
                Set.of(), Optional.of(id("minecraft:zombie")), Set.of(),
                Optional.of(id("minecraft:iron_sword")), Set.of(), Set.of(id("minecraft:is_projectile"))
        );

        assertTrue(CounterRuleEngine.matches(rule, matching));
        assertFalse(CounterRuleEngine.matches(rule, wrongItem));
        assertFalse(CounterRuleEngine.matches(rule, missingTalent));
    }

    @Test
    void clampsIncrementWithoutOverflow() {
        CounterRuleDefinition rule = rule();
        assertEquals(7L, CounterRuleEngine.nextValue(rule, 2L));
        assertEquals(10L, CounterRuleEngine.nextValue(rule, 9L));
        assertEqualsAtMaximum(rule);
    }

    @Test
    void derivesDamageIncrementWithScaleRoundingAndMinimum() {
        CounterRuleDefinition floor = amountRule(CounterRounding.FLOOR, 1.5D, 0L);
        CounterRuleDefinition round = amountRule(CounterRounding.ROUND, 1.0D, 3L);
        CounterRuleDefinition ceil = amountRule(CounterRounding.CEIL, 0.25D, 0L);

        assertEquals(3L, CounterRuleEngine.incrementForEvent(floor, 2.6D));
        assertEquals(3L, CounterRuleEngine.incrementForEvent(round, 1.2D));
        assertEquals(1L, CounterRuleEngine.incrementForEvent(ceil, 2.1D));
        assertEquals(0L, CounterRuleEngine.incrementForEvent(ceil, Double.NaN));
        assertEquals(10L, CounterRuleEngine.nextValue(floor, 9L, 20.0D));
    }

    private static void assertEqualsAtMaximum(CounterRuleDefinition rule) {
        assertEquals(10L, CounterRuleEngine.nextValue(rule, 10L));
    }

    private static CounterRuleDefinition rule() {
        return new CounterRuleDefinition(
                id("example:ranged_kills"), 2, true,
                new LocalizedText("Ranged kills", "", null, false, false),
                CounterEventType.DAMAGE_DEALT, 5L, 10L, 0, 20, List.of(), true,
                List.of(id("example:hunter")), List.of(id("minecraft:zombie")),
                List.of(id("minecraft:skeletons")), List.of(id("minecraft:iron_sword")),
                List.of(id("minecraft:swords")), List.of(id("minecraft:is_projectile"))
        );
    }

    private static CounterRuleDefinition amountRule(CounterRounding rounding, double scale, long minimum) {
        return new CounterRuleDefinition(
                id("example:damage"), 2, true,
                new LocalizedText("Damage", "", null, false, false), CounterEventType.DAMAGE_DEALT,
                1L, CounterAmountMode.EVENT_AMOUNT, scale, rounding, minimum, 10L, 0, 20,
                List.of(), true, List.of(), List.of(), List.of(), List.of(), List.of(), List.of()
        );
    }

    private static ResourceLocation id(String value) {
        return new ResourceLocation(value);
    }
}
