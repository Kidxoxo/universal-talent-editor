package com.kidxoxo.talenteditor.loot;

import com.kidxoxo.talenteditor.domain.ConfiguredItemLootRule;
import com.kidxoxo.talenteditor.domain.ConfiguredItemLootTableMatch;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ConfiguredItemLootSelectorTest {
    @Test
    void rollsRulesIndependentlyAndHonorsZeroOneAndCountBounds() {
        ResourceLocation table = id("minecraft:chests/simple_dungeon");
        List<ConfiguredItemLootBinding> bindings = List.of(
                new ConfiguredItemLootBinding(id("example:never"),
                        new ConfiguredItemLootRule(table, 0.0D, 1, 1)),
                new ConfiguredItemLootBinding(id("example:fixed"),
                        new ConfiguredItemLootRule(table, 1.0D, 3, 3)),
                new ConfiguredItemLootBinding(id("example:ranged"),
                        new ConfiguredItemLootRule(table, 1.0D, 2, 5))
        );

        for (int seed = 0; seed < 100; seed++) {
            List<ConfiguredItemLootSelector.Roll> rolls = ConfiguredItemLootSelector.roll(
                    bindings, RandomSource.create(seed)
            );
            assertEquals(2, rolls.size());
            assertEquals(id("example:fixed"), rolls.get(0).itemId());
            assertEquals(3, rolls.get(0).count());
            assertEquals(id("example:ranged"), rolls.get(1).itemId());
            assertTrue(rolls.get(1).count() >= 2 && rolls.get(1).count() <= 5);
        }
    }

    @Test
    void filtersDimensionAndEntityContextAndAdjustsChanceByLuck() {
        ResourceLocation table = id("minecraft:entities/skeleton");
        var rule = new ConfiguredItemLootRule(
                table, 0.2D, 0.1D, 1, 1,
                List.of(id("minecraft:the_nether")),
                com.kidxoxo.talenteditor.domain.ConfiguredItemLootEntityTarget.THIS_ENTITY,
                List.of(id("minecraft:zombie")), List.of(id("minecraft:undead"))
        );
        var binding = new ConfiguredItemLootBinding(id("example:relic"), rule);
        var undeadSkeleton = new ConfiguredItemLootContext.EntityView(
                id("minecraft:skeleton"), Set.of(id("minecraft:undead"))
        );
        var nether = new ConfiguredItemLootContext(
                Optional.of(id("minecraft:the_nether")), Optional.of(undeadSkeleton),
                Optional.empty(), 10.0D
        );
        var overworld = new ConfiguredItemLootContext(
                Optional.of(id("minecraft:overworld")), Optional.of(undeadSkeleton),
                Optional.empty(), 10.0D
        );

        assertTrue(ConfiguredItemLootSelector.matches(binding, nether));
        assertTrue(ConfiguredItemLootSelector.roll(
                List.of(binding), nether, RandomSource.create(1L)
        ).stream().anyMatch(roll -> roll.itemId().equals(id("example:relic"))));
        assertTrue(ConfiguredItemLootSelector.roll(
                List.of(binding), overworld, RandomSource.create(1L)
        ).isEmpty());
        assertEquals(0.3D, rule.effectiveChance(1.0D), 1.0E-12D);
        assertEquals(1.0D, rule.effectiveChance(100.0D));
        assertEquals(0.0D, rule.effectiveChance(-100.0D));
    }

    @Test
    void supportsExplicitKillerEntityContext() {
        var rule = new ConfiguredItemLootRule(
                id("minecraft:entities/zombie"), 1.0D, 0.0D, 1, 1, List.of(),
                com.kidxoxo.talenteditor.domain.ConfiguredItemLootEntityTarget.KILLER_ENTITY,
                List.of(id("minecraft:player")), List.of()
        );
        var binding = new ConfiguredItemLootBinding(id("example:hunter_token"), rule);
        var context = new ConfiguredItemLootContext(
                Optional.of(id("minecraft:overworld")), Optional.empty(),
                Optional.of(new ConfiguredItemLootContext.EntityView(
                        id("minecraft:player"), Set.of()
                )), 0.0D
        );

        assertTrue(ConfiguredItemLootSelector.matches(binding, context));
    }

    @Test
    void matchesExactPrefixAndFullRegexTableExpressions() {
        var exact = rule(ConfiguredItemLootTableMatch.EXACT, "minecraft:chests/simple_dungeon");
        var prefix = rule(ConfiguredItemLootTableMatch.PREFIX, "minecraft:chests/");
        var regex = rule(ConfiguredItemLootTableMatch.REGEX,
                "(?:minecraft|example):entities/(?:zombie|skeleton)");

        assertTrue(exact.matchesTable(id("minecraft:chests/simple_dungeon")));
        assertFalse(exact.matchesTable(id("minecraft:chests/stronghold_library")));
        assertTrue(prefix.matchesTable(id("minecraft:chests/stronghold_library")));
        assertFalse(prefix.matchesTable(id("minecraft:entities/zombie")));
        assertTrue(regex.matchesTable(id("example:entities/skeleton")));
        assertFalse(regex.matchesTable(id("example:entities/creeper")));
    }

    @Test
    void rejectsInvalidOrOversizedTableExpressionsBeforeRuntime() {
        assertThrows(IllegalArgumentException.class,
                () -> rule(ConfiguredItemLootTableMatch.PREFIX, "Minecraft:chests/"));
        assertThrows(IllegalArgumentException.class,
                () -> rule(ConfiguredItemLootTableMatch.REGEX, "(["));
        assertThrows(IllegalArgumentException.class,
                () -> rule(ConfiguredItemLootTableMatch.REGEX, "example:entities/(a+)+"));
        assertThrows(IllegalArgumentException.class,
                () -> rule(ConfiguredItemLootTableMatch.REGEX, "(example):entities/\\1"));
        assertThrows(IllegalArgumentException.class,
                () -> rule(ConfiguredItemLootTableMatch.REGEX, "x".repeat(257)));
    }

    private static ConfiguredItemLootRule rule(
            ConfiguredItemLootTableMatch tableMatch,
            String expression
    ) {
        return new ConfiguredItemLootRule(
                tableMatch, expression, 1.0D, 0.0D, 1, 1, List.of(),
                com.kidxoxo.talenteditor.domain.ConfiguredItemLootEntityTarget.THIS_ENTITY,
                List.of(), List.of()
        );
    }

    private static ResourceLocation id(String value) {
        return new ResourceLocation(value);
    }
}
