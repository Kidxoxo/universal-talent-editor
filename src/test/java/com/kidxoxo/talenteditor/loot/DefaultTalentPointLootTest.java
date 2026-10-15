package com.kidxoxo.talenteditor.loot;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.kidxoxo.talenteditor.compile.CompilationEnvironment;
import com.kidxoxo.talenteditor.compile.RawDefinitionSet;
import com.kidxoxo.talenteditor.compile.TalentCompiler;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.levelgen.LegacyRandomSource;
import org.junit.jupiter.api.Test;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class DefaultTalentPointLootTest {
    private static final ResourceLocation POINT = new ResourceLocation("talent_editor:talent_point");

    private JsonObject definition() throws Exception {
        try (var stream = getClass().getResourceAsStream(
                "/data/talent_editor/talent_editor/items/talent_point.json")) {
            assertNotNull(stream);
            return JsonParser.parseReader(new InputStreamReader(stream, StandardCharsets.UTF_8)).getAsJsonObject();
        }
    }

    private List<ConfiguredItemLootBinding> bindings(JsonObject json) {
        var raw = RawDefinitionSet.empty().withConfiguredItem(POINT, json, "builtin talent point");
        var result = TalentCompiler.withBuiltins().compile(raw,
                new CompilationEnvironment(Set.of("minecraft", "forge", "talent_editor"), Set.of()));
        assertFalse(result.report().hasErrors(), result.report().issues().toString());
        assertTrue(result.candidate().orElseThrow().configuredItemLootByTable().isEmpty());
        return result.candidate().orElseThrow().configuredItemLootPatterns();
    }

    @Test void defaultMatchesVanillaAndModChestTablesOnly() throws Exception {
        var rules = bindings(definition());
        assertEquals(1, rules.size());
        var rule = rules.get(0).rule();
        for (String table : List.of("minecraft:chests/simple_dungeon", "minecraft:chests/village/village_weaponsmith",
                "cataclysm:chests/sunken_city_treasure", "lightmanscurrency:chest/village/bank")) {
            assertTrue(rule.matchesTable(new ResourceLocation(table)), table);
        }
        for (String table : List.of("minecraft:blocks/chest", "minecraft:entities/zombie",
                "minecraft:gameplay/fishing", "example:gameplay/chests/reward")) {
            assertFalse(rule.matchesTable(new ResourceLocation(table)), table);
        }
        assertEquals(0.05, rule.effectiveChance(0));
        assertEquals(0.05, rule.effectiveChance(100));
        assertEquals(0.05, rule.effectiveChance(-100));
    }

    @Test void fivePercentBoundaryYieldsOnePointAtMost() throws Exception {
        var rules = bindings(definition());
        var success = ConfiguredItemLootSelector.roll(rules, new LegacyRandomSource(1) {
            @Override public double nextDouble() { return 0.049999; }
        });
        assertEquals(List.of(new ConfiguredItemLootSelector.Roll(POINT, 1)), success);
        assertTrue(ConfiguredItemLootSelector.roll(rules, new LegacyRandomSource(1) {
            @Override public double nextDouble() { return 0.05; }
        }).isEmpty());
    }

    @Test void editorOverrideCanDisableDefaultWithoutFallbackDrops() throws Exception {
        var json = definition();
        json.add("loot", new com.google.gson.JsonArray());
        assertTrue(bindings(json).isEmpty());
    }
}
