package com.kidxoxo.talenteditor.draw;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.kidxoxo.talenteditor.compile.CompilationEnvironment;
import com.kidxoxo.talenteditor.compile.CompilationResult;
import com.kidxoxo.talenteditor.compile.RawDefinitionSet;
import com.kidxoxo.talenteditor.compile.TalentCompiler;
import com.kidxoxo.talenteditor.domain.TalentPoolDefinition;
import com.kidxoxo.talenteditor.domain.CompiledCondition;
import com.kidxoxo.talenteditor.domain.QueueDrawConstraintAction;
import com.kidxoxo.talenteditor.domain.DrawConstraintQueueMode;
import com.kidxoxo.talenteditor.runtime.RuntimeSnapshot;
import com.kidxoxo.talenteditor.runtime.ConditionEvaluator;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TalentDrawServiceTest {
    private static final ResourceLocation COMMON = id("example:common");
    private static final ResourceLocation RARE = id("example:rare");
    private static final ResourceLocation POOL = id("example:main");

    @Test
    void attributeComparisonIsAcceptedInAcquisitionCondition() {
        RawDefinitionSet base = definitions(3);
        LinkedHashMap<ResourceLocation, JsonObject> talents = new LinkedHashMap<>(base.talents());
        ResourceLocation bladeId = id("example:common_0");
        JsonObject blade = talents.get(bladeId).deepCopy();
        blade.add("acquireCondition", JsonParser.parseString("""
                {"type":"attribute_compare","attribute":"minecraft:generic.attack_speed",
                 "operator":"gte","value":8}
                """));
        talents.put(bladeId, blade);
        CompilationResult result = TalentCompiler.withBuiltins().compile(
                new RawDefinitionSet(base.rarities(), talents, base.pools(), List.of()),
                new CompilationEnvironment(Set.of("minecraft", "forge", "talent_editor"),
                        Set.of(id("minecraft:generic.attack_speed"))));
        assertFalse(result.report().hasErrors(), () -> result.report().issues().toString());
        assertTrue(result.candidate().orElseThrow().talents().get(bladeId).acquireCondition()
                instanceof CompiledCondition.AttributeCompare);
    }

    @Test
    void poolCompilesWithConfigurableChoicesRefreshesLimitAndRarityWeights() {
        CompilationResult result = TalentCompiler.withBuiltins().compile(
                definitions(5), new CompilationEnvironment(Set.of("minecraft", "forge", "talent_editor"), Set.of())
        );

        assertFalse(result.report().hasErrors());
        TalentPoolDefinition pool = result.candidate().orElseThrow().pools().get(POOL);
        assertEquals(5, pool.choices());
        assertEquals(2, pool.refreshes());
        assertEquals(24, pool.maxOwned());
        assertEquals(28, pool.iconSize());
        assertEquals(1.0D, pool.rarityWeights().get(COMMON));
        assertEquals(1.0D, pool.rarityWeights().get(RARE));
    }

    @Test
    void poolIconSizeDefaultsForOldDefinitionsAndRejectsOutOfRangeValues() {
        RawDefinitionSet base = definitions(3);
        JsonObject legacyPool = base.pools().get(POOL).deepCopy();
        legacyPool.remove("iconSize");
        CompilationResult legacy = TalentCompiler.withBuiltins().compile(
                new RawDefinitionSet(base.rarities(), base.talents(), Map.of(POOL, legacyPool), List.of()),
                new CompilationEnvironment(Set.of("minecraft", "forge", "talent_editor"), Set.of())
        );
        assertFalse(legacy.report().hasErrors());
        assertEquals(64, legacy.candidate().orElseThrow().pools().get(POOL).iconSize());

        JsonObject invalidPool = legacyPool.deepCopy();
        invalidPool.addProperty("iconSize", 65);
        CompilationResult invalid = TalentCompiler.withBuiltins().compile(
                new RawDefinitionSet(base.rarities(), base.talents(), Map.of(POOL, invalidPool), List.of()),
                new CompilationEnvironment(Set.of("minecraft", "forge", "talent_editor"), Set.of())
        );
        assertTrue(invalid.report().hasErrors());
    }

    @Test
    void offersAreUniqueAndExcludeOwnedTalents() {
        RuntimeSnapshot snapshot = snapshot(5);
        TalentPoolDefinition pool = snapshot.pools().get(POOL);
        ResourceLocation owned = id("example:common_0");
        List<ResourceLocation> offers = TalentDrawService.generateOffers(
                snapshot, pool, Set.of(owned), new Random(7L)
        );

        assertEquals(5, offers.size());
        assertEquals(offers.size(), offers.stream().distinct().count());
        assertFalse(offers.contains(owned));
    }

    @Test
    void talentsInAcquisitionHistoryCannotBeOfferedAfterTheyAreRevoked() {
        RuntimeSnapshot snapshot = snapshot(5);
        TalentPoolDefinition pool = snapshot.pools().get(POOL);
        ResourceLocation revoked = id("example:common_0");

        List<ResourceLocation> offers = TalentDrawService.generateOffers(
                snapshot, pool, Set.of(), Set.of(revoked), ConditionEvaluator.Context.empty(Set.of()),
                0.0D, false, new Random(7L)
        );

        assertEquals(5, offers.size());
        assertFalse(offers.contains(revoked));
    }

    @Test
    void rarityChanceDoesNotScaleWithNumberOfTalentsInThatRarity() {
        RuntimeSnapshot snapshot = snapshot(1);
        TalentPoolDefinition pool = snapshot.pools().get(POOL);
        Random random = new Random(42L);
        int rareHits = 0;
        int trials = 10_000;
        for (int index = 0; index < trials; index++) {
            ResourceLocation selected = TalentDrawService.generateOffers(snapshot, pool, Set.of(), random).get(0);
            if (snapshot.talents().get(selected).rarity().equals(RARE)) rareHits++;
        }
        double ratio = rareHits / (double) trials;
        assertTrue(ratio > 0.47D && ratio < 0.53D, "Expected roughly 50%, got " + ratio);
    }

    @Test
    void talentCannotReferenceUnknownPool() {
        RawDefinitionSet definitions = definitions(3);
        LinkedHashMap<ResourceLocation, JsonObject> talents = new LinkedHashMap<>(definitions.talents());
        ResourceLocation firstId = talents.keySet().iterator().next();
        JsonObject changed = talents.get(firstId).deepCopy();
        changed.getAsJsonObject("offer").getAsJsonArray("pools").set(0,
                JsonParser.parseString("\"example:missing\""));
        talents.put(firstId, changed);
        CompilationResult result = TalentCompiler.withBuiltins().compile(
                new RawDefinitionSet(definitions.rarities(), talents, definitions.pools(), List.of()),
                new CompilationEnvironment(Set.of("minecraft", "forge", "talent_editor"), Set.of())
        );

        assertTrue(result.report().hasErrors());
        assertTrue(result.report().issues().stream().anyMatch(issue -> issue.code().equals("UNKNOWN_POOL")));
    }

    @Test
    void acquisitionCounterConditionFiltersDrawCandidates() {
        RawDefinitionSet base = definitions(5);
        LinkedHashMap<ResourceLocation, JsonObject> talents = new LinkedHashMap<>();
        base.talents().forEach((id, value) -> {
            JsonObject changed = value.deepCopy();
            changed.add("acquireCondition", JsonParser.parseString("""
                    {"type":"counter_at_least","counter":"example:kills","value":10}
                    """));
            talents.put(id, changed);
        });
        RuntimeSnapshot snapshot = TalentCompiler.withBuiltins().compile(
                new RawDefinitionSet(base.rarities(), talents, base.pools(), List.of()),
                new CompilationEnvironment(Set.of("minecraft", "forge", "talent_editor"), Set.of())
        ).candidate().orElseThrow();
        TalentPoolDefinition pool = snapshot.pools().get(POOL);

        assertTrue(TalentDrawService.generateOffers(
                snapshot, pool, Set.of(), ConditionEvaluator.Context.empty(Set.of()), new Random(1L)
        ).isEmpty());
        assertEquals(5, TalentDrawService.generateOffers(
                snapshot, pool, Set.of(),
                new ConditionEvaluator.Context(Set.of(), Map.of(id("example:kills"), 10L), Map.of()),
                new Random(1L)
        ).size());
    }

    @Test
    void luckFactorsChangeNormalizedRarityProbability() {
        RawDefinitionSet base = definitions(1);
        JsonObject poolJson = base.pools().get(POOL).deepCopy();
        poolJson.addProperty("luckAttribute", "minecraft:generic.luck");
        poolJson.add("luckFactors", parse("""
                {"example:common": -0.05, "example:rare": 0.10}
                """));
        RuntimeSnapshot snapshot = TalentCompiler.withBuiltins().compile(
                new RawDefinitionSet(base.rarities(), base.talents(), Map.of(POOL, poolJson), List.of()),
                new CompilationEnvironment(
                        Set.of("minecraft", "forge", "talent_editor"),
                        Set.of(id("minecraft:generic.luck"))
                )
        ).candidate().orElseThrow();
        TalentPoolDefinition pool = snapshot.pools().get(POOL);

        assertEquals(0.5D, TalentDrawService.rarityProbabilities(snapshot, pool, 0.0D).get(RARE), 0.00001D);
        assertEquals(0.8D, TalentDrawService.rarityProbabilities(snapshot, pool, 10.0D).get(RARE), 0.00001D);
    }

    @Test
    void activePityGuaranteesFirstEligibleOfferAtThresholdRarity() {
        RawDefinitionSet base = definitions(3);
        JsonObject poolJson = base.pools().get(POOL).deepCopy();
        poolJson.addProperty("pityRarity", RARE.toString());
        poolJson.addProperty("pityAfter", 3);
        RuntimeSnapshot snapshot = TalentCompiler.withBuiltins().compile(
                new RawDefinitionSet(base.rarities(), base.talents(), Map.of(POOL, poolJson), List.of()),
                new CompilationEnvironment(Set.of("minecraft", "forge", "talent_editor"), Set.of())
        ).candidate().orElseThrow();

        List<ResourceLocation> offers = TalentDrawService.generateOffers(
                snapshot, snapshot.pools().get(POOL), Set.of(), ConditionEvaluator.Context.empty(Set.of()),
                0.0D, true, new Random(9L)
        );

        assertEquals(RARE, snapshot.talents().get(offers.get(0)).rarity());
        assertEquals(3, offers.size());
    }

    @Test
    void compilerAcceptsDuplicateCounterPolicyAndRejectsMissingRewardCounter() {
        RawDefinitionSet base = definitions(1);
        JsonObject validPool = base.pools().get(POOL).deepCopy();
        validPool.addProperty("excludeOwned", false);
        validPool.addProperty("duplicatePolicy", "add_counter");
        validPool.addProperty("duplicateCounter", "example:shards");
        validPool.addProperty("duplicateCounterAmount", 4);
        CompilationResult valid = TalentCompiler.withBuiltins().compile(
                new RawDefinitionSet(base.rarities(), base.talents(), Map.of(POOL, validPool), List.of()),
                new CompilationEnvironment(Set.of("minecraft", "forge", "talent_editor"), Set.of())
        );
        assertFalse(valid.report().hasErrors());
        assertEquals(com.kidxoxo.talenteditor.domain.DuplicatePolicy.ADD_COUNTER,
                valid.candidate().orElseThrow().pools().get(POOL).duplicatePolicy());

        JsonObject brokenPool = validPool.deepCopy();
        brokenPool.remove("duplicateCounter");
        CompilationResult broken = TalentCompiler.withBuiltins().compile(
                new RawDefinitionSet(base.rarities(), base.talents(), Map.of(POOL, brokenPool), List.of()),
                new CompilationEnvironment(Set.of("minecraft", "forge", "talent_editor"), Set.of())
        );
        assertTrue(broken.report().issues().stream()
                .anyMatch(issue -> issue.code().equals("DUPLICATE_COUNTER_REQUIRED")));
    }

    @Test
    void ownedActiveTalentAddsFreeRefreshesAndValidatesPoolReferences() {
        RawDefinitionSet base = definitions(3);
        LinkedHashMap<ResourceLocation, JsonObject> talents = new LinkedHashMap<>(base.talents());
        ResourceLocation bonusTalent = id("example:common_0");
        JsonObject changed = talents.get(bonusTalent).deepCopy();
        changed.add("activeEffects", JsonParser.parseString("""
                [{"type":"draw_refresh_bonus","amount":3,"pools":["example:main"]}]
                """));
        talents.put(bonusTalent, changed);
        CompilationResult valid = TalentCompiler.withBuiltins().compile(
                new RawDefinitionSet(base.rarities(), talents, base.pools(), List.of()),
                new CompilationEnvironment(Set.of("minecraft", "forge", "talent_editor"), Set.of())
        );

        assertFalse(valid.report().hasErrors());
        assertEquals(3, TalentDrawService.refreshBonus(
                valid.candidate().orElseThrow(), Set.of(bonusTalent), POOL
        ));
        assertEquals(0, TalentDrawService.refreshBonus(
                valid.candidate().orElseThrow(), Set.of(), POOL
        ));

        changed = changed.deepCopy();
        changed.getAsJsonArray("activeEffects").get(0).getAsJsonObject()
                .getAsJsonArray("pools").set(0, JsonParser.parseString("\"example:missing\""));
        talents.put(bonusTalent, changed);
        CompilationResult invalid = TalentCompiler.withBuiltins().compile(
                new RawDefinitionSet(base.rarities(), talents, base.pools(), List.of()),
                new CompilationEnvironment(Set.of("minecraft", "forge", "talent_editor"), Set.of())
        );
        assertTrue(invalid.report().issues().stream().anyMatch(issue -> issue.code().equals("UNKNOWN_POOL")));
    }

    @Test
    void queuedCategoryConstraintGuaranteesMatchingOffersThenFillsNormally() {
        RawDefinitionSet base = definitions(3);
        LinkedHashMap<ResourceLocation, JsonObject> talents = new LinkedHashMap<>(base.talents());
        for (String path : List.of("common_0", "common_1")) {
            ResourceLocation talentId = id("example:" + path);
            JsonObject changed = talents.get(talentId).deepCopy();
            changed.add("categories", JsonParser.parseString("[\"example:light\"]"));
            talents.put(talentId, changed);
        }
        RuntimeSnapshot snapshot = TalentCompiler.withBuiltins().compile(
                new RawDefinitionSet(base.rarities(), talents, base.pools(), List.of()),
                new CompilationEnvironment(Set.of("minecraft", "forge", "talent_editor"), Set.of())
        ).candidate().orElseThrow();
        QueueDrawConstraintAction constraint = new QueueDrawConstraintAction(
                List.of(), List.of(id("example:light")), List.of(), List.of(), List.of(), 2
        );

        List<ResourceLocation> offers = TalentDrawService.generateOffersWithConstraint(
                snapshot, snapshot.pools().get(POOL), Set.of(), ConditionEvaluator.Context.empty(Set.of()),
                0.0D, false, constraint, new Random(31L)
        );

        assertEquals(3, offers.size());
        assertEquals(2, offers.stream().filter(value -> value.equals(id("example:common_0"))
                || value.equals(id("example:common_1"))).count());
        assertEquals(offers.size(), offers.stream().distinct().count());
    }

    @Test
    void threeQueuedDarkSearchesFillThreeDistinctOfferSlots() {
        RawDefinitionSet base = definitions(3);
        LinkedHashMap<ResourceLocation, JsonObject> talents = new LinkedHashMap<>(base.talents());
        for (String path : List.of("common_0", "common_1", "common_2")) {
            ResourceLocation talentId = id("example:" + path);
            JsonObject changed = talents.get(talentId).deepCopy();
            changed.add("categories", JsonParser.parseString("[\"example:dark\"]"));
            talents.put(talentId, changed);
        }
        RuntimeSnapshot snapshot = TalentCompiler.withBuiltins().compile(
                new RawDefinitionSet(base.rarities(), talents, base.pools(), List.of()),
                new CompilationEnvironment(Set.of("minecraft", "forge", "talent_editor"), Set.of())
        ).candidate().orElseThrow();
        QueueDrawConstraintAction dark = new QueueDrawConstraintAction(
                List.of(), List.of(id("example:dark")), List.of(), List.of(), List.of(POOL), 1);
        List<ResourceLocation> offers = TalentDrawService.generateOffersWithConstraints(
                snapshot, snapshot.pools().get(POOL), Set.of(), Set.of(),
                ConditionEvaluator.Context.empty(Set.of()), 0, false,
                List.of(dark, dark, dark), new Random(37L));
        assertEquals(3, offers.size());
        assertEquals(3, offers.stream().distinct().count());
        assertTrue(offers.stream().allMatch(id -> id.getPath().startsWith("common_")));
    }

    @Test
    void queuedConstraintFallsBackToOrdinaryOffersWhenNoMatchingTalentRemains() {
        RuntimeSnapshot snapshot = snapshot(3);
        QueueDrawConstraintAction constraint = new QueueDrawConstraintAction(
                List.of(), List.of(id("example:missing_category")), List.of(), List.of(), List.of(), 3
        );

        List<ResourceLocation> offers = TalentDrawService.generateOffersWithConstraint(
                snapshot, snapshot.pools().get(POOL), Set.of(), Set.of(),
                ConditionEvaluator.Context.empty(Set.of()), 0.0D, false, constraint, new Random(31L)
        );

        assertEquals(3, offers.size());
        assertEquals(offers.size(), offers.stream().distinct().count());
    }

    @Test
    void queuedSearchCompilerValidatesTalentRarityAndPoolReferences() {
        RawDefinitionSet base = definitions(3);
        LinkedHashMap<ResourceLocation, JsonObject> talents = new LinkedHashMap<>(base.talents());
        ResourceLocation source = id("example:common_0");
        JsonObject changed = talents.get(source).deepCopy();
        changed.add("triggers", JsonParser.parseString("""
                [{"id":"search","event":"player_second","actions":[{
                  "type":"queue_draw_constraint",
                  "talents":["example:missing_talent"],
                  "rarities":["example:missing_rarity"],
                  "pools":["example:missing_pool"],
                  "guaranteed":1
                }]}]
                """));
        talents.put(source, changed);

        CompilationResult result = TalentCompiler.withBuiltins().compile(
                new RawDefinitionSet(base.rarities(), talents, base.pools(), List.of()),
                new CompilationEnvironment(Set.of("minecraft", "forge", "talent_editor"), Set.of())
        );

        assertTrue(result.report().issues().stream().anyMatch(issue -> issue.code().equals("UNKNOWN_TALENT")));
        assertTrue(result.report().issues().stream().anyMatch(issue -> issue.code().equals("UNKNOWN_RARITY")));
        assertTrue(result.report().issues().stream().anyMatch(issue -> issue.code().equals("UNKNOWN_POOL")));

        JsonObject misplaced = talents.get(id("example:common_1")).deepCopy();
        misplaced.add("activeEffects", JsonParser.parseString("""
                [{"type":"queue_draw_constraint","categories":["example:light"]}]
                """));
        talents.put(id("example:common_1"), misplaced);
        CompilationResult wrongPlacement = TalentCompiler.withBuiltins().compile(
                new RawDefinitionSet(base.rarities(), talents, base.pools(), List.of()),
                new CompilationEnvironment(Set.of("minecraft", "forge", "talent_editor"), Set.of())
        );
        assertTrue(wrongPlacement.report().issues().stream()
                .anyMatch(issue -> issue.code().equals("ACTION_PLACEMENT_UNSUPPORTED")));
    }

    @Test
    void compilesQueuePolicySuccessCounterAndMilestoneSelector() {
        RawDefinitionSet base = definitions(3);
        LinkedHashMap<ResourceLocation, JsonObject> talents = new LinkedHashMap<>(base.talents());
        ResourceLocation source = id("example:common_0");
        JsonObject changed = talents.get(source).deepCopy();
        changed.add("triggers", JsonParser.parseString("""
                [{"id":"hope","event":"player_move_distance","chance":0.01,
                  "chanceCategory":"example:light","chancePerPoint":0.005,
                  "distanceInterval":200,"actions":[{
                  "type":"queue_draw_constraint","categories":["example:light"],
                  "pools":["example:main"],"guaranteed":1,"queueMode":"replace",
                  "successCounter":"example:hope_successes","successIncrement":1,
                  "milestoneEvery":9,"milestone":{
                    "categories":["example:light"],"tags":["example:ur_material"]
                  }
                }]}]
                """));
        talents.put(source, changed);
        RawDefinitionSet input = new RawDefinitionSet(
                base.rarities(), talents, base.pools(),
                Map.of(id("example:hope_successes"), parse("""
                        {"schema":2,"name":"Hope successes","event":"kill"}
                        """)), List.of()
        );

        CompilationResult result = TalentCompiler.withBuiltins().compile(
                input, new CompilationEnvironment(Set.of("minecraft", "forge", "talent_editor"), Set.of())
        );

        assertFalse(result.report().hasErrors(), () -> result.report().issues().toString());
        QueueDrawConstraintAction action = (QueueDrawConstraintAction) result.candidate().orElseThrow()
                .talents().get(source).triggers().get(0).actions().get(0);
        assertEquals(DrawConstraintQueueMode.REPLACE, action.queueMode());
        assertEquals(id("example:hope_successes"), action.successCounter().orElseThrow());
        assertEquals(9, action.milestoneEvery());
        assertEquals(List.of(id("example:ur_material")), action.milestone().orElseThrow().tags());
        assertEquals(List.of(POOL), action.milestone().orElseThrow().pools(),
                "omitted milestone pools inherit the primary pool scope");
        var trigger = result.candidate().orElseThrow().talents().get(source).triggers().get(0);
        assertEquals(id("example:light"), trigger.chanceCategory().orElseThrow());
        assertEquals(0.005D, trigger.chancePerPoint());
        assertEquals(200.0D, trigger.distanceInterval());

        changed = talents.get(source).deepCopy();
        changed.add("triggers", JsonParser.parseString("""
                [{"id":"bad","event":"player_second","actions":[{
                  "type":"queue_draw_constraint","categories":["example:light"],
                  "successCounter":"example:missing","milestoneEvery":2,
                  "milestone":{"talents":["example:missing_talent"]}
                }]}]
                """));
        talents.put(source, changed);
        CompilationResult invalid = TalentCompiler.withBuiltins().compile(
                new RawDefinitionSet(base.rarities(), talents, base.pools(), Map.of(), List.of()),
                new CompilationEnvironment(Set.of("minecraft", "forge", "talent_editor"), Set.of())
        );
        assertTrue(invalid.report().issues().stream().anyMatch(issue -> issue.code().equals("UNKNOWN_COUNTER")));
        assertTrue(invalid.report().issues().stream().anyMatch(issue -> issue.code().equals("UNKNOWN_TALENT")));
    }

    private static RuntimeSnapshot snapshot(int choices) {
        return TalentCompiler.withBuiltins().compile(
                definitions(choices),
                new CompilationEnvironment(Set.of("minecraft", "forge", "talent_editor"), Set.of())
        ).candidate().orElseThrow().withRevision(3);
    }

    private static RawDefinitionSet definitions(int choices) {
        Map<ResourceLocation, JsonObject> rarities = Map.of(
                COMMON, rarity("Common", "#A0A0A0"),
                RARE, rarity("Rare", "#55FFFF")
        );
        JsonObject pool = parse("""
                {
                  "schema": 2,
                  "name": "Main",
                  "choices": %d,
                  "refreshes": 2,
                  "maxOwned": 24,
                  "iconSize": 28,
                  "excludeOwned": true,
                  "rarityWeights": {
                    "example:common": 1.0,
                    "example:rare": 1.0
                  }
                }
                """.formatted(choices));
        LinkedHashMap<ResourceLocation, JsonObject> talents = new LinkedHashMap<>();
        for (int index = 0; index < 10; index++) {
            talents.put(id("example:common_" + index), talent("Common " + index, COMMON));
        }
        talents.put(id("example:rare_0"), talent("Rare", RARE));
        return new RawDefinitionSet(rarities, talents, Map.of(POOL, pool), List.of());
    }

    private static JsonObject rarity(String name, String color) {
        return parse("""
                {"schema":2,"name":"%s","color":"%s","baseWeight":1.0}
                """.formatted(name, color));
    }

    private static JsonObject talent(String name, ResourceLocation rarity) {
        return parse("""
                {
                  "schema": 2,
                  "text": {"name": "%s"},
                  "rarity": "%s",
                  "offer": {"enabled": true, "weight": 1.0, "pools": ["example:main"]}
                }
                """.formatted(name, rarity));
    }

    private static JsonObject parse(String value) {
        return JsonParser.parseString(value).getAsJsonObject();
    }

    private static ResourceLocation id(String value) {
        return new ResourceLocation(value);
    }
}
