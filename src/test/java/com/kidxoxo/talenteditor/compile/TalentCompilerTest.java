package com.kidxoxo.talenteditor.compile;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.kidxoxo.talenteditor.runtime.ActivationOutcome;
import com.kidxoxo.talenteditor.runtime.SnapshotRepository;
import com.kidxoxo.talenteditor.domain.ActionFailurePolicy;
import com.kidxoxo.talenteditor.domain.ConditionalAction;
import com.kidxoxo.talenteditor.domain.CompiledCondition;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

class TalentCompilerTest {
    private static final ResourceLocation COMMON = id("talent_editor:common");
    private static final ResourceLocation VITALITY = id("example:vitality");
    private static final ResourceLocation MAX_HEALTH = id("minecraft:generic.max_health");
    private static final ResourceLocation ATTACK_DAMAGE = id("minecraft:generic.attack_damage");

    private final TalentCompiler compiler = TalentCompiler.withBuiltins();
    private final CompilationEnvironment environment = new CompilationEnvironment(
            Set.of("minecraft", "forge", "talent_editor"),
            Set.of(MAX_HEALTH, ATTACK_DAMAGE)
    );

    @Test
    void compilesConditionsOnIndividualActiveAndTriggerActions() {
        JsonObject talent = validTalent();
        talent.getAsJsonArray("activeEffects").get(0).getAsJsonObject().add("disabledWhen", parse("""
                {"type":"has_talent","talent":"example:other"}
                """));
        talent.add("triggers", JsonParser.parseString("""
                [{"id":"conditional_damage","event":"damage_dealt","actions":[
                  {"type":"modify_damage_expression","operation":"multiply","expression":"1.2",
                   "enabledWhen":{"type":"expression_compare","left":"health_percent",
                                  "operator":"gt","right":"0.5"}}
                ]}]
                """));
        CompilationResult result = compiler.compile(new RawDefinitionSet(
                Map.of(COMMON, rarity()), Map.of(VITALITY, talent, id("example:other"), validTalent()),
                Map.of(), Map.of(), List.of()
        ), environment);
        assertFalse(result.report().hasErrors(), result.report().issues().toString());
        var compiled = result.candidate().orElseThrow().talents().get(VITALITY);
        var active = assertInstanceOf(ConditionalAction.class, compiled.activeEffects().get(0));
        assertInstanceOf(CompiledCondition.HasTalent.class, active.disabledWhen().orElseThrow());
        assertInstanceOf(com.kidxoxo.talenteditor.domain.AttributeModifierAction.class, active.delegate());
        var triggered = assertInstanceOf(ConditionalAction.class, compiled.triggers().get(0).actions().get(0));
        assertInstanceOf(CompiledCondition.ExpressionCompare.class, triggered.enabledWhen().orElseThrow());
    }

    @Test
    void legacyTalentWithoutOfferRemainsNonDrawable() {
        CompilationResult result = compiler.compile(new RawDefinitionSet(
                Map.of(COMMON, rarity()), Map.of(VITALITY, validTalent()), Map.of(), Map.of(), List.of()
        ), environment);

        assertFalse(result.report().hasErrors(), result.report().issues().toString());
        assertFalse(result.candidate().orElseThrow().talents().get(VITALITY).offer().enabled());
    }

    @Test
    void compilesHudRenderingSubmissionAndOptionalCuriosInterfaces() {
        JsonObject talent = validTalent();
        talent.add("requiredMods", JsonParser.parseString("[\"curios\"]"));
        talent.add("acquireCondition", JsonParser.parseString("""
                {"type":"curios_equipped","item":"minecraft:diamond"}
                """));
        talent.getAsJsonArray("activeEffects").add(JsonParser.parseString("""
                {"type":"hud_resource","state":"example:energy","maximum":"100+level",
                 "color":5614335,"order":2,"showWhenFull":false}
                """));
        talent.getAsJsonArray("activeEffects").add(JsonParser.parseString("""
                {"type":"player_render","alpha":"1-counter(\\\"example:visibility\\\")/100",
                 "scale":1.2,"hideNameplate":true}
                """));
        talent.add("triggers", JsonParser.parseString("""
                [{"id":"submit","event":"interact_block","actions":[
                  {"type":"open_item_submission","item":"minecraft:diamond","count":"2",
                   "timeoutTicks":200,"consume":true}
                ]}]
                """));
        CompilationEnvironment optionalEnvironment = new CompilationEnvironment(
                Set.of("minecraft", "forge", "talent_editor", "curios"),
                environment.registeredAttributes(),
                Set.of(id("minecraft:diamond"))
        );

        CompilationResult result = compiler.compile(new RawDefinitionSet(
                Map.of(COMMON, rarity()), Map.of(VITALITY, talent), Map.of(), Map.of(), List.of()
        ), optionalEnvironment);

        assertFalse(result.report().hasErrors(), result.report().issues().toString());
        var compiled = result.candidate().orElseThrow().talents().get(VITALITY);
        assertInstanceOf(com.kidxoxo.talenteditor.domain.CompiledCondition.CuriosEquipped.class,
                compiled.acquireCondition());
        assertInstanceOf(com.kidxoxo.talenteditor.domain.HudResourceAction.class,
                compiled.activeEffects().get(1));
        assertInstanceOf(com.kidxoxo.talenteditor.domain.PlayerRenderAction.class,
                compiled.activeEffects().get(2));
        var render = (com.kidxoxo.talenteditor.domain.PlayerRenderAction) compiled.activeEffects().get(2);
        assertEquals("1-counter(\"example:visibility\")/100", render.alpha().source());
        assertEquals("1.2", render.scale().source());
        assertInstanceOf(com.kidxoxo.talenteditor.domain.OpenItemSubmissionAction.class,
                compiled.triggers().get(0).actions().get(0));
    }

    @Test
    void compilesLifecycleExpressionsStateTransitionsAndTimedModifiers() {
        JsonObject talent = validTalent();
        talent.add("lifecycle", JsonParser.parseString("""
                {"durationTicks":1200,"removeAfterDraws":2,
                 "retainEffects":true,"retainCategoryContribution":true,"retainTriggers":true}
                """));
        talent.add("triggers", JsonParser.parseString("""
                [{
                  "id":"formula_guard","event":"fatal_damage",
                  "condition":{"type":"expression_compare","left":"health_percent",
                               "operator":"lte","right":"0.5"},
                  "actions":[
                    {"type":"modify_state","state":"example:growth","operation":"add",
                     "expression":"max(1, counter(\\\"example:kills\\\"))","target":"self"},
                    {"type":"modify_damage_expression","operation":"multiply",
                     "expression":"clamp(1-state(\\\"example:growth\\\")*0.01,0,1)"},
                    {"type":"affect_health_expression","operation":"heal",
                     "expression":"max_health*0.25","invertNegative":true,"target":"self"},
                    {"type":"modify_attribute_base_expression",
                     "attribute":"minecraft:generic.max_health","operation":"add",
                     "expression":"1","target":"self"},
                    {"type":"timed_attribute_modifier",
                     "attribute":"minecraft:generic.attack_damage","expression":"2+level*0.1",
                     "operation":"addition","modifierKey":"formula_boost","durationTicks":100,
                     "target":"self"},
                    {"type":"give_draw_tokens","amount":"1"},
                    {"type":"grant_talent","talent":"example:vitality","onlyIfNeverAcquired":false},
                    {"type":"revoke_talent","talent":"example:vitality","retainEffects":true,
                     "retainTriggers":true},
                    {"type":"modify_counter","counter":"example:kills","operation":"add",
                     "expression":"1","minimum":0,"maximum":1000}
                  ]
                }]
                """));
        JsonObject counter = parse("""
                {"schema":2,"name":"Kills","event":"manual","visibility":"condition_only"}
                """);

        CompilationResult result = compiler.compile(new RawDefinitionSet(
                Map.of(COMMON, rarity()), Map.of(VITALITY, talent), Map.of(),
                Map.of(id("example:kills"), counter), List.of()
        ), environment);

        assertFalse(result.report().hasErrors(), result.report().issues().toString());
        var compiled = result.candidate().orElseThrow().talents().get(VITALITY);
        assertEquals(1200, compiled.lifecycle().durationTicks());
        assertEquals(2, compiled.lifecycle().removeAfterDraws());
        assertTrue(compiled.lifecycle().retainEffects());
        assertTrue(compiled.lifecycle().retainCategoryContribution());
        assertTrue(compiled.lifecycle().retainTriggers());
        assertInstanceOf(com.kidxoxo.talenteditor.domain.CompiledCondition.ExpressionCompare.class,
                compiled.triggers().get(0).condition());
        assertEquals(9, compiled.triggers().get(0).actions().size());
        assertInstanceOf(com.kidxoxo.talenteditor.domain.ModifyNumericStateAction.class,
                compiled.triggers().get(0).actions().get(0));
        assertInstanceOf(com.kidxoxo.talenteditor.domain.TimedAttributeModifierAction.class,
                compiled.triggers().get(0).actions().get(4));
        var snapshot = result.candidate().orElseThrow();
        assertTrue(snapshot.referencesTo(
                com.kidxoxo.talenteditor.domain.DefinitionKind.TALENT, VITALITY
        ).stream().anyMatch(reference -> reference.usage().equals("grant_talent")));
        assertTrue(snapshot.referencesTo(
                com.kidxoxo.talenteditor.domain.DefinitionKind.TALENT, VITALITY
        ).stream().anyMatch(reference -> reference.usage().equals("revoke_talent")));
        assertTrue(snapshot.referencesTo(
                com.kidxoxo.talenteditor.domain.DefinitionKind.COUNTER, id("example:kills")
        ).stream().anyMatch(reference -> reference.usage().equals("modify_counter")));
        assertTrue(snapshot.counterBookIds().contains(id("example:kills")));
    }

    @Test
    void compilesSelectedTalentAndEventEntityEffectConditions() {
        JsonObject talent = validTalent();
        talent.add("triggers", JsonParser.parseString("""
                [
                  {"id":"selected_luck","event":"draw_selected",
                   "condition":{"type":"event_talent_category","category":"example:luck"},
                   "actions":[{"type":"show_message","text":{"literal":"Luck"}}]},
                  {"id":"selected_exact","event":"draw_selected",
                   "condition":{"type":"event_talent","talent":"example:vitality"},
                   "actions":[{"type":"show_message","text":{"literal":"Exact"}}]},
                  {"id":"slowed_target","event":"interact_entity",
                   "condition":{"type":"event_entity_effect","effect":"minecraft:slowness"},
                   "actions":[{"type":"show_message","text":{"literal":"Slowed"}}]}
                ]
                """));
        CompilationEnvironment eventEnvironment = new CompilationEnvironment(
                environment.loadedMods(), environment.registeredAttributes(), Set.of(), Set.of(),
                Set.of(id("minecraft:slowness"))
        );

        CompilationResult result = compiler.compile(new RawDefinitionSet(
                Map.of(COMMON, rarity()), Map.of(VITALITY, talent), Map.of(), Map.of(), List.of()
        ), eventEnvironment);

        assertFalse(result.report().hasErrors(), result.report().issues().toString());
        var triggers = result.candidate().orElseThrow().talents().get(VITALITY).triggers();
        assertInstanceOf(com.kidxoxo.talenteditor.domain.CompiledCondition.EventTalentCategory.class,
                triggers.get(0).condition());
        assertInstanceOf(com.kidxoxo.talenteditor.domain.CompiledCondition.EventTalent.class,
                triggers.get(1).condition());
        assertInstanceOf(com.kidxoxo.talenteditor.domain.CompiledCondition.EventEntityEffect.class,
                triggers.get(2).condition());
    }

    @Test
    void compilesDynamicAttributesAndCreativeFlightWithCounterReferences() {
        ResourceLocation kills = id("example:kills");
        JsonObject talent = validTalent();
        talent.getAsJsonArray("activeEffects").add(JsonParser.parseString("""
                {"type":"dynamic_attribute_modifier",
                 "attribute":"minecraft:generic.attack_damage",
                 "source":"counter","counter":"example:kills",
                 "base":1,"scale":0.25,"minimum":0,"maximum":20,
                 "operation":"addition","modifierKey":"kills_damage"}
                """));
        talent.getAsJsonArray("activeEffects").add(JsonParser.parseString("""
                {"type":"creative_flight"}
                """));
        JsonObject counter = parse("""
                {"schema":2,"name":"Kills","event":"manual","visibility":"condition_only"}
                """);

        CompilationResult result = compiler.compile(new RawDefinitionSet(
                Map.of(COMMON, rarity()), Map.of(VITALITY, talent), Map.of(), Map.of(kills, counter), List.of()
        ), environment);

        assertFalse(result.report().hasErrors());
        var snapshot = result.candidate().orElseThrow();
        assertInstanceOf(com.kidxoxo.talenteditor.domain.DynamicAttributeModifierAction.class,
                snapshot.talents().get(VITALITY).activeEffects().get(1));
        assertInstanceOf(com.kidxoxo.talenteditor.domain.CreativeFlightAction.class,
                snapshot.talents().get(VITALITY).activeEffects().get(2));
        assertEquals("dynamic_attribute_counter", snapshot.referencesTo(
                com.kidxoxo.talenteditor.domain.DefinitionKind.COUNTER, kills
        ).get(0).usage());
        assertTrue(snapshot.counterBookIds().contains(kills));
    }

    @Test
    void rejectsInvalidDynamicAttributeReferencesAndRecursion() {
        JsonObject talent = validTalent();
        talent.getAsJsonArray("activeEffects").add(JsonParser.parseString("""
                {"type":"dynamic_attribute_modifier",
                 "attribute":"minecraft:generic.attack_damage",
                 "source":"counter","counter":"example:missing",
                 "modifierKey":"missing_counter"}
                """));
        talent.getAsJsonArray("activeEffects").add(JsonParser.parseString("""
                {"type":"dynamic_attribute_modifier",
                 "attribute":"minecraft:generic.max_health",
                 "source":"health_percent","modifierKey":"recursive_health"}
                """));

        CompilationResult result = compiler.compile(
                new RawDefinitionSet(Map.of(COMMON, rarity()), Map.of(VITALITY, talent), List.of()), environment
        );

        assertTrue(result.report().issues().stream().anyMatch(issue -> issue.code().equals("UNKNOWN_COUNTER")));
        assertTrue(result.report().issues().stream()
                .anyMatch(issue -> issue.code().equals("DYNAMIC_ATTRIBUTE_SELF_REFERENCE")));
    }

    @Test
    void rejectsModifierKeysSharedByConstantAndDynamicAttributes() {
        JsonObject talent = validTalent();
        talent.getAsJsonArray("activeEffects").get(0).getAsJsonObject().addProperty("modifierKey", "shared");
        talent.getAsJsonArray("activeEffects").add(JsonParser.parseString("""
                {"type":"dynamic_attribute_modifier",
                 "attribute":"minecraft:generic.attack_damage",
                 "source":"hunger_percent","modifierKey":"shared"}
                """));

        CompilationResult result = compiler.compile(
                new RawDefinitionSet(Map.of(COMMON, rarity()), Map.of(VITALITY, talent), List.of()), environment
        );

        assertTrue(result.report().issues().stream()
                .anyMatch(issue -> issue.code().equals("DUPLICATE_MODIFIER_KEY")));
    }

    @Test
    void compilesFailurePolicyAndFallbackActions() {
        JsonObject talent = validTalent();
        talent.add("triggers", JsonParser.parseString("""
                [{
                  "id":"guard_fallback","event":"damage_taken_pre",
                  "actionFailurePolicy":"stop",
                  "actions":[{"type":"modify_damage","operation":"multiply","amount":0.5}],
                  "failureActions":[{"type":"cancel_damage"}]
                }]
                """));

        CompilationResult result = compiler.compile(
                new RawDefinitionSet(Map.of(COMMON, rarity()), Map.of(VITALITY, talent), List.of()),
                environment
        );

        assertFalse(result.report().hasErrors());
        var trigger = result.candidate().orElseThrow().talents().get(VITALITY).triggers().get(0);
        assertEquals(ActionFailurePolicy.STOP, trigger.actionFailurePolicy());
        assertEquals(1, trigger.failureActions().size());
        assertTrue(trigger.failureActions().get(0)
                instanceof com.kidxoxo.talenteditor.domain.CancelDamageAction);
    }

    @Test
    void rejectsUnknownFailurePolicyAndFallbackDamageOutsideDamageEvents() {
        JsonObject talent = validTalent();
        talent.add("triggers", JsonParser.parseString("""
                [{
                  "id":"bad_fallback","event":"player_second",
                  "actionFailurePolicy":"rollback",
                  "actions":[{"type":"queue_draw_constraint","categories":["example:light"]}],
                  "failureActions":[{"type":"cancel_damage"}]
                }]
                """));

        CompilationResult result = compiler.compile(
                new RawDefinitionSet(Map.of(COMMON, rarity()), Map.of(VITALITY, talent), List.of()),
                environment
        );

        assertTrue(result.report().issues().stream()
                .anyMatch(issue -> issue.code().equals("INVALID_ACTION_FAILURE_POLICY")));
        assertTrue(result.report().issues().stream()
                .anyMatch(issue -> issue.code().equals("DAMAGE_ACTION_EVENT_UNSUPPORTED")));
    }

    @Test
    void compilesEveryAttributeEffectInsideOneTalent() {
        JsonObject talent = validTalent();
        talent.getAsJsonArray("activeEffects").add(parse("""
                {
                  "type": "attribute_modifier",
                  "attribute": "minecraft:generic.attack_damage",
                  "amount": 0.25,
                  "operation": "multiply_total"
                }
                """));
        CompilationResult result = compiler.compile(
                new RawDefinitionSet(Map.of(COMMON, rarity()), Map.of(VITALITY, talent), List.of()),
                environment
        );

        assertFalse(result.report().hasErrors());
        assertEquals(1, result.candidate().orElseThrow().attributeBindings().get(MAX_HEALTH).size());
        assertEquals(1, result.candidate().orElseThrow().attributeBindings().get(ATTACK_DAMAGE).size());
        assertEquals(2, result.candidate().orElseThrow().talents().get(VITALITY).activeEffects().size());
    }

    @Test
    void compilesStandaloneTalentAndBuildsAttributeIndex() {
        CompilationResult result = compiler.compile(validDefinitions(), environment);

        assertFalse(result.report().hasErrors());
        assertTrue(result.candidate().isPresent());
        assertEquals(1, result.candidate().orElseThrow().talents().size());
        assertEquals(1, result.candidate().orElseThrow().attributeBindings().get(MAX_HEALTH).size());
        assertTrue(result.candidate().orElseThrow().talents().get(VITALITY).available());
    }

    @Test
    void keepsDefinitionInactiveWhenItsDeclaredModIsMissing() {
        JsonObject talent = parse("""
                {
                  "schema": 2,
                  "text": {"name": "Optional power"},
                  "rarity": "talent_editor:common",
                  "requiredMods": ["optional_power"],
                  "activeEffects": [
                    {
                      "type": "attribute_modifier",
                      "attribute": "optional_power:power",
                      "amount": 2,
                      "operation": "addition"
                    }
                  ]
                }
                """);
        RawDefinitionSet definitions = new RawDefinitionSet(
                Map.of(COMMON, rarity()),
                Map.of(id("example:optional_power"), talent),
                List.of()
        );

        CompilationResult result = compiler.compile(definitions, environment);

        assertFalse(result.report().hasErrors());
        assertEquals(2, result.report().warningCount());
        assertFalse(result.candidate().orElseThrow().talents().get(id("example:optional_power")).available());
        assertTrue(result.candidate().orElseThrow().attributeBindings().isEmpty());
    }

    @Test
    void rejectsUnknownFieldsAndKeepsPreviousSnapshot() {
        SnapshotRepository repository = new SnapshotRepository();
        CompilationResult valid = compiler.compile(validDefinitions(), environment);
        ActivationOutcome first = repository.apply(valid);
        long acceptedRevision = first.activeRevision();
        String acceptedHash = first.activeHash();

        JsonObject broken = validTalent();
        broken.addProperty("capcityCost", 99);
        CompilationResult invalid = compiler.compile(
                new RawDefinitionSet(Map.of(COMMON, rarity()), Map.of(VITALITY, broken), List.of()),
                environment
        );
        ActivationOutcome rejected = repository.apply(invalid);

        assertTrue(first.accepted());
        assertTrue(first.changed());
        assertFalse(rejected.accepted());
        assertEquals(acceptedRevision, repository.active().revision());
        assertEquals(acceptedHash, repository.active().contentHash());
        assertTrue(invalid.report().issues().stream().anyMatch(issue -> issue.code().equals("UNKNOWN_FIELD")));
    }

    @Test
    void canonicalHashDoesNotDependOnObjectFieldOrder() {
        JsonObject original = validTalent();
        JsonObject reordered = parse("""
                {
                  "activeEffects": [
                    {
                      "operation": "addition",
                      "amount": 4,
                      "attribute": "minecraft:generic.max_health",
                      "type": "attribute_modifier"
                    }
                  ],
                  "rarity": "talent_editor:common",
                  "text": {
                    "description": ["Adds health."],
                    "name": "Vitality"
                  },
                  "schema": 2
                }
                """);
        RawDefinitionSet first = new RawDefinitionSet(
                Map.of(COMMON, rarity()), Map.of(VITALITY, original), List.of()
        );
        RawDefinitionSet second = new RawDefinitionSet(
                Map.of(COMMON, rarity()), Map.of(VITALITY, reordered), List.of()
        );

        assertEquals(CanonicalJsonHasher.hash(first), CanonicalJsonHasher.hash(second));

        RawDefinitionSet differentSources = new RawDefinitionSet(
                first.rarities(), first.talents(), first.pools(), first.counters(),
                Map.of(COMMON, "builtin/talent_editor"), Map.of(VITALITY, "file/custom_pack"),
                Map.of(), Map.of(), List.of()
        );
        assertEquals(CanonicalJsonHasher.hash(first), CanonicalJsonHasher.hash(differentSources));

        reordered.getAsJsonArray("activeEffects").get(0).getAsJsonObject().addProperty("amount", 5);
        RawDefinitionSet changed = new RawDefinitionSet(
                Map.of(COMMON, rarity()), Map.of(VITALITY, reordered), List.of()
        );
        assertNotEquals(CanonicalJsonHasher.hash(first), CanonicalJsonHasher.hash(changed));
    }

    @Test
    void compilesBoundedCompositeConditionTreeBeforeActionValidation() {
        JsonObject talent = validTalent();
        talent.add("triggers", JsonParser.parseString("""
                [
                  {
                    "id": "example",
                    "event": "attack_pre",
                    "condition": {
                      "type": "all",
                      "children": [
                        {"type": "always", "value": true},
                        {"type": "not", "child": {"type": "always", "value": false}}
                      ]
                    },
                    "actions": [
                      {
                        "type": "attribute_modifier",
                        "attribute": "minecraft:generic.max_health",
                        "amount": 1
                      }
                    ]
                  }
                ]
                """));

        CompilationResult result = compiler.compile(
                new RawDefinitionSet(Map.of(COMMON, rarity()), Map.of(VITALITY, talent), List.of()),
                environment
        );

        assertTrue(result.report().hasErrors());
        assertTrue(result.report().issues().stream()
                .anyMatch(issue -> issue.code().equals("ACTION_PLACEMENT_UNSUPPORTED")));
        assertFalse(result.report().issues().stream()
                .anyMatch(issue -> issue.code().startsWith("UNKNOWN_CONDITION")));
    }

    @Test
    void compilesAcquisitionConditionsAgainstKnownTalentsCountersAndItems() {
        ResourceLocation prerequisite = id("example:prerequisite");
        ResourceLocation diamond = id("minecraft:diamond");
        JsonObject talent = validTalent();
        talent.add("acquireCondition", JsonParser.parseString("""
                {
                  "type": "all",
                  "children": [
                    {"type": "has_talent", "talent": "example:prerequisite"},
                    {"type": "counter_at_least", "counter": "example:kills", "value": 10},
                    {"type": "category_count_at_least", "category": "example:combat", "count": 2},
                    {"type": "has_item", "item": "minecraft:diamond", "count": 3}
                  ]
                }
                """));
        CompilationEnvironment withItems = new CompilationEnvironment(
                environment.loadedMods(), environment.registeredAttributes(), Set.of(diamond)
        );
        CompilationResult result = compiler.compile(
                new RawDefinitionSet(
                        Map.of(COMMON, rarity()),
                        Map.of(VITALITY, talent, prerequisite, validTalent()),
                        List.of()
                ),
                withItems
        );

        assertFalse(result.report().hasErrors());
        assertEquals(4, assertInstanceOf(
                com.kidxoxo.talenteditor.domain.CompiledCondition.All.class,
                result.candidate().orElseThrow().talents().get(VITALITY).acquireCondition()
        ).children().size());
    }

    @Test
    void rejectsUnknownTalentAndItemInAcquisitionConditions() {
        JsonObject talent = validTalent();
        talent.add("acquireCondition", JsonParser.parseString("""
                {
                  "type": "all",
                  "children": [
                    {"type": "has_talent", "talent": "missing:talent"},
                    {"type": "has_item", "item": "missing:item", "count": 1}
                  ]
                }
                """));
        CompilationResult result = compiler.compile(
                new RawDefinitionSet(Map.of(COMMON, rarity()), Map.of(VITALITY, talent), List.of()),
                environment
        );

        assertTrue(result.report().issues().stream().anyMatch(issue -> issue.code().equals("UNKNOWN_TALENT")));
        assertTrue(result.report().issues().stream().anyMatch(issue -> issue.code().equals("UNKNOWN_ITEM")));
    }

    @Test
    void rejectsOutOfRangeCategoryCountRequirement() {
        JsonObject talent = validTalent();
        talent.add("acquireCondition", JsonParser.parseString("""
                {
                  "type": "category_count_at_least",
                  "category": "example:combat",
                  "count": 1000001
                }
                """));
        CompilationResult result = compiler.compile(
                new RawDefinitionSet(Map.of(COMMON, rarity()), Map.of(VITALITY, talent), List.of()), environment
        );

        assertTrue(result.report().issues().stream().anyMatch(issue -> issue.code().equals("INTEGER_RANGE")));
    }

    @Test
    void compilesTalentCentricSynthesisCosts() {
        ResourceLocation base = id("example:base");
        ResourceLocation diamond = id("minecraft:diamond");
        JsonObject resultTalent = validTalent();
        resultTalent.add("synthesis", JsonParser.parseString("""
                {
                  "enabled": true,
                  "consumeTalents": ["example:base"],
                  "consumeCounters": {"example:kills": 10},
                  "consumeItems": {"minecraft:diamond": 3}
                }
                """));
        CompilationResult result = compiler.compile(
                new RawDefinitionSet(
                        Map.of(COMMON, rarity()), Map.of(VITALITY, resultTalent, base, validTalent()), List.of()
                ),
                new CompilationEnvironment(
                        environment.loadedMods(), environment.registeredAttributes(), Set.of(diamond)
                )
        );

        assertFalse(result.report().hasErrors());
        var synthesis = result.candidate().orElseThrow().talents().get(VITALITY).synthesis();
        assertTrue(synthesis.enabled());
        assertEquals(List.of(base), synthesis.consumeTalents());
        assertEquals(10L, synthesis.consumeCounters().get(id("example:kills")));
        assertEquals(3, synthesis.consumeItems().get(diamond));
        var snapshot = result.candidate().orElseThrow();
        assertTrue(snapshot.referencesTo(com.kidxoxo.talenteditor.domain.DefinitionKind.TALENT, base).stream()
                .anyMatch(reference -> reference.usage().equals("synthesis_talent")
                        && reference.path().equals("$.synthesis.consumeTalents[0]")));
        assertTrue(snapshot.referencesTo(
                        com.kidxoxo.talenteditor.domain.DefinitionKind.COUNTER, id("example:kills")
                ).stream().anyMatch(reference -> reference.usage().equals("synthesis_counter")));
        assertEquals(2, snapshot.referencesTo(
                com.kidxoxo.talenteditor.domain.DefinitionKind.RARITY, COMMON
        ).size());
    }

    @Test
    void rejectsSynthesisThatConsumesItsResultOrUnknownResources() {
        JsonObject talent = validTalent();
        talent.add("synthesis", JsonParser.parseString("""
                {
                  "enabled": true,
                  "consumeTalents": ["example:vitality", "missing:talent"],
                  "consumeItems": {"missing:item": 1}
                }
                """));
        CompilationResult result = compiler.compile(
                new RawDefinitionSet(Map.of(COMMON, rarity()), Map.of(VITALITY, talent), List.of()),
                environment
        );

        assertTrue(result.report().issues().stream()
                .anyMatch(issue -> issue.code().equals("SYNTHESIS_CONSUMES_RESULT")));
        assertTrue(result.report().issues().stream().anyMatch(issue -> issue.code().equals("UNKNOWN_TALENT")));
        assertTrue(result.report().issues().stream().anyMatch(issue -> issue.code().equals("UNKNOWN_ITEM")));
    }

    @Test
    void compilesIndexedAutomaticCounterRules() {
        ResourceLocation counterId = id("example:zombie_kills");
        JsonObject counter = parse("""
                {
                  "schema": 2,
                  "name": "Zombie kills",
                  "event": "kill",
                  "increment": 2,
                  "maximum": 50,
                  "cooldownTicks": 4,
                  "resetOnLogin": true,
                  "resetOnDeath": true,
                  "resetIntervalTicks": 1200,
                  "categories": ["example:combat_events"],
                  "visibility": "condition_only",
                  "requiredTalents": ["example:vitality"],
                  "entityTypes": ["minecraft:zombie"],
                  "itemTags": ["minecraft:swords"]
                }
                """);
        JsonObject talent = validTalent();
        talent.add("acquireCondition", parse("""
                {"type":"counter_category_total_at_least","category":"example:combat_events","value":2}
                """));
        ResourceLocation hiddenCounterId = id("example:hidden_kills");
        JsonObject hiddenCounter = counter.deepCopy();
        hiddenCounter.addProperty("name", "Hidden kills");
        hiddenCounter.addProperty("visibility", "hidden");
        CompilationResult result = compiler.compile(
                new RawDefinitionSet(
                        Map.of(COMMON, rarity()), Map.of(VITALITY, talent), Map.of(),
                        Map.of(counterId, counter, hiddenCounterId, hiddenCounter), List.of()
                ),
                new CompilationEnvironment(
                        environment.loadedMods(), environment.registeredAttributes(),
                        Set.of(), Set.of(id("minecraft:zombie"))
                )
        );

        assertFalse(result.report().hasErrors());
        var compiled = result.candidate().orElseThrow().counters().get(counterId);
        assertEquals(2L, compiled.increment());
        assertEquals(50L, compiled.maximum());
        assertTrue(compiled.resetOnLogin());
        assertTrue(compiled.resetOnDeath());
        assertEquals(1200, compiled.resetIntervalTicks());
        assertTrue(compiled.resetsOn(com.kidxoxo.talenteditor.domain.CounterResetTrigger.LOGIN));
        assertTrue(compiled.resetsOn(com.kidxoxo.talenteditor.domain.CounterResetTrigger.DEATH));
        assertFalse(compiled.periodicResetDue(1199L));
        assertTrue(compiled.periodicResetDue(1200L));
        assertEquals(List.of(id("example:combat_events")), compiled.categories());
        assertEquals(com.kidxoxo.talenteditor.domain.CounterVisibility.CONDITION_ONLY, compiled.visibility());
        assertTrue(result.candidate().orElseThrow().countersByEvent()
                .get(com.kidxoxo.talenteditor.domain.CounterEventType.KILL).contains(compiled));
        assertTrue(result.candidate().orElseThrow().countersByCategory()
                .get(id("example:combat_events")).contains(compiled));
        assertTrue(result.candidate().orElseThrow().counterBookIds().contains(counterId));
        assertFalse(result.candidate().orElseThrow().counterBookIds().contains(hiddenCounterId));
        assertEquals(2, result.candidate().orElseThrow().referencesTo(
                com.kidxoxo.talenteditor.domain.DefinitionKind.TALENT, VITALITY
        ).stream().filter(reference -> reference.usage().equals("counter_required_talent")).count());
        assertEquals(3, result.candidate().orElseThrow().referencesTo(
                com.kidxoxo.talenteditor.domain.DefinitionKind.COUNTER_CATEGORY,
                id("example:combat_events")
        ).size());
    }

    @Test
    void rejectsUnknownCounterEventEntityAndIncompatibleFilter() {
        ResourceLocation counterId = id("example:bad_counter");
        JsonObject counter = parse("""
                {
                  "schema": 2,
                  "name": "Bad",
                  "event": "jump",
                  "visibility": "sometimes",
                  "entityTypes": ["missing:thing"],
                  "damageTags": ["minecraft:is_fire"]
                }
                """);
        CompilationResult result = compiler.compile(
                new RawDefinitionSet(
                        Map.of(COMMON, rarity()), Map.of(VITALITY, validTalent()), Map.of(),
                        Map.of(counterId, counter), List.of()
                ), environment
        );

        assertTrue(result.report().issues().stream().anyMatch(issue -> issue.code().equals("UNKNOWN_COUNTER_EVENT")));
        assertTrue(result.report().issues().stream()
                .anyMatch(issue -> issue.code().equals("UNKNOWN_COUNTER_VISIBILITY")));
        assertTrue(result.report().issues().stream().anyMatch(issue -> issue.code().equals("UNKNOWN_ENTITY_TYPE")));
        assertTrue(result.report().issues().stream().anyMatch(issue -> issue.code().equals("COUNTER_FILTER_NOT_AVAILABLE")));
    }

    @Test
    void compilesEventAmountCountersAndActionManagedCounters() {
        ResourceLocation counterId = id("example:damage_total");
        JsonObject counter = parse("""
                {
                  "schema": 2,
                  "name": "Damage total",
                  "event": "damage_dealt",
                  "amountMode": "event_amount",
                  "amountScale": 1.5,
                  "rounding": "ceil",
                  "minimumIncrement": 2
                }
                """);
        CompilationResult valid = compiler.compile(
                new RawDefinitionSet(Map.of(COMMON, rarity()), Map.of(VITALITY, validTalent()), Map.of(),
                        Map.of(counterId, counter), List.of()), environment
        );

        assertFalse(valid.report().hasErrors());
        var compiled = valid.candidate().orElseThrow().counters().get(counterId);
        assertEquals(com.kidxoxo.talenteditor.domain.CounterAmountMode.EVENT_AMOUNT, compiled.amountMode());
        assertEquals(1.5D, compiled.amountScale());
        assertEquals(com.kidxoxo.talenteditor.domain.CounterRounding.CEIL, compiled.rounding());
        assertEquals(2L, compiled.minimumIncrement());

        counter.addProperty("event", "move_distance");
        CompilationResult movement = compiler.compile(
                new RawDefinitionSet(Map.of(COMMON, rarity()), Map.of(VITALITY, validTalent()), Map.of(),
                        Map.of(counterId, counter), List.of()), environment
        );
        assertFalse(movement.report().hasErrors());
        assertEquals(com.kidxoxo.talenteditor.domain.CounterEventType.MOVE_DISTANCE,
                movement.candidate().orElseThrow().counters().get(counterId).event());

        counter.addProperty("event", "kill");
        CompilationResult invalid = compiler.compile(
                new RawDefinitionSet(Map.of(COMMON, rarity()), Map.of(VITALITY, validTalent()), Map.of(),
                        Map.of(counterId, counter), List.of()), environment
        );
        assertTrue(invalid.report().issues().stream()
                .anyMatch(issue -> issue.code().equals("COUNTER_EVENT_AMOUNT_UNSUPPORTED")));

        JsonObject manual = parse("""
                {"schema":2,"name":"Hope successes","event":"manual","visibility":"condition_only"}
                """);
        CompilationResult actionManaged = compiler.compile(
                new RawDefinitionSet(Map.of(COMMON, rarity()), Map.of(VITALITY, validTalent()), Map.of(),
                        Map.of(id("example:hope_successes"), manual), List.of()), environment
        );
        assertFalse(actionManaged.report().hasErrors());
        assertEquals(com.kidxoxo.talenteditor.domain.CounterEventType.MANUAL,
                actionManaged.candidate().orElseThrow().counters().get(id("example:hope_successes")).event());
    }

    @Test
    void indexesNestedConditionsAndPoolReferencesWithJsonPaths() {
        ResourceLocation base = id("example:base");
        ResourceLocation poolId = id("example:main");
        ResourceLocation counterId = id("example:kills");
        JsonObject talent = validTalent();
        talent.add("offer", parse("""
                {"enabled":true,"weight":1,"pools":["example:main"]}
                """));
        talent.add("acquireCondition", parse("""
                {
                  "type":"all",
                  "children":[
                    {"type":"has_talent","talent":"example:base"},
                    {"type":"not","child":{"type":"counter_at_least","counter":"example:kills","value":10}}
                  ]
                }
                """));
        JsonObject pool = parse("""
                {
                  "schema":2,
                  "name":"Main",
                  "choices":3,
                  "refreshes":1,
                  "maxOwned":24,
                  "rarityWeights":{"talent_editor:common":1},
                  "luckAttribute":"minecraft:generic.max_health",
                  "luckFactors":{"talent_editor:common":0.1},
                  "pityRarity":"talent_editor:common",
                  "pityAfter":5,
                  "duplicatePolicy":"add_counter",
                  "duplicateCounter":"example:kills",
                  "duplicateCounterAmount":1
                }
                """);
        CompilationResult result = compiler.compile(new RawDefinitionSet(
                Map.of(COMMON, rarity()), Map.of(VITALITY, talent, base, validTalent()),
                Map.of(poolId, pool), Map.of(), List.of()
        ), environment);

        assertFalse(result.report().hasErrors());
        var snapshot = result.candidate().orElseThrow();
        assertEquals(1, snapshot.referencesTo(com.kidxoxo.talenteditor.domain.DefinitionKind.POOL, poolId).size());
        assertTrue(snapshot.referencesTo(com.kidxoxo.talenteditor.domain.DefinitionKind.TALENT, base).stream()
                .anyMatch(reference -> reference.path().equals("$.acquireCondition.children[0].talent")));
        assertTrue(snapshot.referencesTo(com.kidxoxo.talenteditor.domain.DefinitionKind.COUNTER, counterId).stream()
                .anyMatch(reference -> reference.path().equals(
                        "$.acquireCondition.children[1].child.counter"
                )));
        assertEquals(5, snapshot.referencesTo(
                com.kidxoxo.talenteditor.domain.DefinitionKind.RARITY, COMMON
        ).size());
    }

    private static RawDefinitionSet validDefinitions() {
        LinkedHashMap<ResourceLocation, JsonObject> rarities = new LinkedHashMap<>();
        rarities.put(COMMON, rarity());
        LinkedHashMap<ResourceLocation, JsonObject> talents = new LinkedHashMap<>();
        talents.put(VITALITY, validTalent());
        return new RawDefinitionSet(rarities, talents, List.of());
    }

    @Test
    void compilesLayoutsAndSelectsHighestPriorityEnabledLayout() {
        ResourceLocation low = id("example:compact");
        ResourceLocation high = id("example:illustrated");
        RawDefinitionSet definitions = new RawDefinitionSet(
                Map.of(COMMON, rarity()), Map.of(VITALITY, validTalent()), Map.of(), Map.of(),
                Map.of(
                        low, parse("""
                                {"schema":2,"priority":2,"columns":1,"rows":3,"slotWidth":48,
                                 "slotHeight":28,"iconSize":16,"showGeneratedEffects":true}
                                """),
                        high, parse("""
                                {"schema":2,"priority":9,"background":"example:textures/gui/book.png",
                                 "panelPercent":50,"columns":3,"rows":5,"slotWidth":40,
                                 "slotHeight":32,"iconSize":20,"showGeneratedEffects":false}
                                """)
                ), Map.of(), Map.of(), Map.of(), Map.of(), Map.of(), List.of()
        );

        CompilationResult result = compiler.compile(definitions, environment);

        assertFalse(result.report().hasErrors());
        var snapshot = result.candidate().orElseThrow();
        assertEquals(2, snapshot.layouts().size());
        assertEquals(high, snapshot.bookLayout().id());
        assertEquals(15, snapshot.bookLayout().pageCapacity());
        assertFalse(snapshot.bookLayout().showGeneratedEffects());
    }

    @Test
    void rejectsUnknownOrImpossibleLayoutFields() {
        ResourceLocation layout = id("example:broken");
        RawDefinitionSet definitions = new RawDefinitionSet(
                Map.of(COMMON, rarity()), Map.of(VITALITY, validTalent()), Map.of(), Map.of(),
                Map.of(layout, parse("""
                        {"schema":2,"columns":2,"rows":4,"slotWidth":48,"slotHeight":22,
                         "iconSize":24,"mystery":true}
                        """)), Map.of(), Map.of(), Map.of(), Map.of(), Map.of(), List.of()
        );

        CompilationResult result = compiler.compile(definitions, environment);

        assertTrue(result.report().hasErrors());
        assertTrue(result.report().issues().stream().anyMatch(issue -> issue.code().equals("UNKNOWN_FIELD")));
        assertTrue(result.report().issues().stream().anyMatch(issue -> issue.code().equals("LAYOUT_ICON_TOO_TALL")));
    }

    @Test
    void compilesFreeLayoutSlotsAndUsesThemAsPageCapacity() {
        ResourceLocation layout = id("example:freeform");
        RawDefinitionSet definitions = new RawDefinitionSet(
                Map.of(COMMON, rarity()), Map.of(VITALITY, validTalent()), Map.of(), Map.of(),
                Map.of(layout, parse("""
                        {"schema":2,"columns":8,"rows":10,"slots":[
                          {"id":"featured","x":0,"y":0,"width":80,"height":40,"iconSize":24},
                          {"id":"compact","x":86,"y":8,"width":48,"height":26,"iconSize":16}
                        ]}
                        """)), Map.of(), Map.of(), Map.of(), Map.of(), Map.of(), List.of()
        );

        CompilationResult result = compiler.compile(definitions, environment);

        assertFalse(result.report().hasErrors());
        var compiled = result.candidate().orElseThrow().layouts().get(layout);
        assertEquals(2, compiled.pageCapacity());
        assertEquals("featured", compiled.slots().get(0).id());
        assertEquals(80, compiled.slots().get(0).width());
    }

    @Test
    void rejectsDuplicateOverlappingAndOutOfBoundsFreeSlots() {
        ResourceLocation layout = id("example:broken_slots");
        RawDefinitionSet definitions = new RawDefinitionSet(
                Map.of(COMMON, rarity()), Map.of(VITALITY, validTalent()), Map.of(), Map.of(),
                Map.of(layout, parse("""
                        {"schema":2,"slots":[
                          {"id":"same","x":0,"y":0,"width":60,"height":30,"iconSize":16},
                          {"id":"same","x":40,"y":10,"width":60,"height":30,"iconSize":16},
                          {"id":"edge","x":2040,"y":0,"width":24,"height":22,"iconSize":8}
                        ]}
                        """)), Map.of(), Map.of(), Map.of(), Map.of(), Map.of(), List.of()
        );

        CompilationResult result = compiler.compile(definitions, environment);

        assertTrue(result.report().hasErrors());
        assertTrue(result.report().issues().stream()
                .anyMatch(issue -> issue.code().equals("DUPLICATE_LAYOUT_SLOT_ID")));
        assertTrue(result.report().issues().stream()
                .anyMatch(issue -> issue.code().equals("LAYOUT_SLOT_OVERLAP")));
        assertTrue(result.report().issues().stream()
                .anyMatch(issue -> issue.code().equals("LAYOUT_SLOT_BOUNDS")));
    }

    @Test
    void compilesConfiguredPagesWithIndependentSlotsAndBackgrounds() {
        ResourceLocation layout = id("example:paged");
        RawDefinitionSet definitions = new RawDefinitionSet(
                Map.of(COMMON, rarity()), Map.of(VITALITY, validTalent()), Map.of(), Map.of(),
                Map.of(layout, parse("""
                        {"schema":2,"background":"example:textures/gui/root.png","slots":[
                          {"id":"fallback","x":0,"y":0,"width":58,"height":30,"iconSize":16}
                        ],"pages":[
                          {"id":"opening","background":"example:textures/gui/opening.png","slots":[
                            {"id":"hero","x":4,"y":2,"width":96,"height":48,"iconSize":28},
                            {"id":"side","x":104,"y":10,"width":52,"height":28,"iconSize":16}
                          ]},
                          {"id":"second","slots":[
                            {"id":"center","x":30,"y":12,"width":70,"height":34,"iconSize":20}
                          ]}
                        ]}
                        """)), Map.of(), Map.of(), Map.of(), Map.of(), Map.of(), List.of()
        );

        CompilationResult result = compiler.compile(definitions, environment);

        assertFalse(result.report().hasErrors());
        var compiled = result.candidate().orElseThrow().layouts().get(layout);
        assertEquals(2, compiled.pages().size());
        assertEquals(2, compiled.slotsForPage(0).size());
        assertEquals(1, compiled.slotsForPage(1).size());
        assertEquals("fallback", compiled.slotsForPage(2).get(0).id());
        assertEquals(id("example:textures/gui/opening.png"), compiled.backgroundForPage(0).orElseThrow());
        assertEquals(id("example:textures/gui/root.png"), compiled.backgroundForPage(1).orElseThrow());
    }

    @Test
    void rejectsDuplicatePageIdsAndPagesWithoutSlots() {
        ResourceLocation layout = id("example:broken_pages");
        RawDefinitionSet definitions = new RawDefinitionSet(
                Map.of(COMMON, rarity()), Map.of(VITALITY, validTalent()), Map.of(), Map.of(),
                Map.of(layout, parse("""
                        {"schema":2,"pages":[
                          {"id":"same","slots":[]},
                          {"id":"same"}
                        ]}
                        """)), Map.of(), Map.of(), Map.of(), Map.of(), Map.of(), List.of()
        );

        CompilationResult result = compiler.compile(definitions, environment);

        assertTrue(result.report().hasErrors());
        assertTrue(result.report().issues().stream()
                .anyMatch(issue -> issue.code().equals("DUPLICATE_LAYOUT_PAGE_ID")));
        assertTrue(result.report().issues().stream()
                .anyMatch(issue -> issue.code().equals("LAYOUT_PAGE_SLOTS_EMPTY")));
        assertTrue(result.report().issues().stream()
                .anyMatch(issue -> issue.code().equals("REQUIRED_FIELD")
                        && issue.path().equals("$.pages[1].slots")));
    }

    @Test
    void fixedLayoutTalentCreatesAReverseReferenceEvenOnDisabledLayouts() {
        var layout = parse("""
                {"schema":2,"enabled":false,"pages":[{"id":"one","slots":[
                  {"id":"hero","talent":"example:vitality"}
                ]}]}
                """);
        var result = compileTestLayout(layout);
        assertFalse(result.report().hasErrors());
        var snapshot = result.candidate().orElseThrow();
        var references = snapshot.referencesTo(com.kidxoxo.talenteditor.domain.DefinitionKind.TALENT, VITALITY);
        assertEquals(1, references.size());
        assertEquals(com.kidxoxo.talenteditor.domain.DefinitionKind.LAYOUT, references.get(0).sourceKind());
        assertEquals("$.pages[0].slots[0].talent", references.get(0).path());
        assertEquals(VITALITY, snapshot.layouts().get(id("example:test_layout"))
                .pages().get(0).slots().get(0).talent().orElseThrow());
    }

    @Test
    void rejectsUnknownDuplicateAndRootFixedTalentsAtTheirPaths() {
        var result = compileTestLayout(parse("""
                {"schema":2,"slots":[{"id":"root","talent":"example:vitality"}],"pages":[
                  {"id":"one","slots":[{"id":"hero","talent":"example:vitality"}]},
                  {"id":"two","slots":[{"id":"hero","talent":"example:vitality"}]},
                  {"id":"three","slots":[{"id":"hero","talent":"example:absent"}]}
                ]}
                """));
        assertTrue(result.candidate().isEmpty());
        var codes = result.report().issues().stream()
                .map(issue -> issue.code() + " " + issue.path()).toList();
        assertTrue(codes.contains("LAYOUT_ROOT_FIXED_TALENT $.slots[0].talent"));
        assertTrue(codes.contains("DUPLICATE_LAYOUT_FIXED_TALENT $.pages[1].slots[0].talent"));
        assertTrue(codes.contains("UNKNOWN_TALENT $.pages[2].slots[0].talent"));
        assertTrue(compileTestLayout(parse("""
                {"schema":2,"pages":[{"id":"one","slots":[{"id":"hero","talent":17}]}]}
                """)).report().hasErrors());
    }

    @Test
    void compilesEventFilteredMobEffectTriggersAndRejectsUnknownEffects() {
        JsonObject talent = validTalent();
        talent.add("triggers", JsonParser.parseString("""
                [{
                  "id":"poison_undead",
                  "event":"damage_dealt",
                  "priority":20,
                  "chance":0.35,
                  "cooldownTicks":40,
                  "condition":{"type":"event_entity_type","entityType":"minecraft:zombie"},
                  "actions":[{
                    "type":"add_effect",
                    "effect":"minecraft:poison",
                    "durationTicks":80,
                    "amplifier":1,
                    "target":"event_entity",
                    "ambient":false,
                    "showParticles":true,
                    "showIcon":false
                  }]
                }]
                """).getAsJsonArray());
        CompilationEnvironment triggerEnvironment = new CompilationEnvironment(
                Set.of("minecraft", "forge", "talent_editor"), Set.of(MAX_HEALTH, ATTACK_DAMAGE), Set.of(),
                Set.of(id("minecraft:zombie")), Set.of(id("minecraft:poison"))
        );
        CompilationResult result = compiler.compile(new RawDefinitionSet(
                Map.of(COMMON, rarity()), Map.of(VITALITY, talent), List.of()
        ), triggerEnvironment);

        assertFalse(result.report().hasErrors());
        var binding = result.candidate().orElseThrow().triggersByEvent()
                .get(id("talent_editor:damage_dealt")).get(0);
        assertInstanceOf(com.kidxoxo.talenteditor.domain.CompiledCondition.EventEntityType.class,
                binding.trigger().condition());
        assertEquals(0.35D, binding.trigger().chance());
        assertEquals(40, binding.trigger().cooldownTicks());
        var action = assertInstanceOf(com.kidxoxo.talenteditor.domain.AddEffectAction.class,
                binding.trigger().actions().get(0));
        assertEquals(id("minecraft:poison"), action.effect());
        assertEquals(com.kidxoxo.talenteditor.domain.ActionTarget.EVENT_ENTITY, action.target());
        assertEquals(80, action.durationTicks());

        talent.getAsJsonArray("triggers").get(0).getAsJsonObject().getAsJsonArray("actions")
                .get(0).getAsJsonObject().addProperty("effect", "minecraft:absent");
        CompilationResult invalid = compiler.compile(new RawDefinitionSet(
                Map.of(COMMON, rarity()), Map.of(VITALITY, talent), List.of()
        ), triggerEnvironment);
        assertTrue(invalid.report().issues().stream().anyMatch(issue ->
                issue.code().equals("UNKNOWN_MOB_EFFECT")
                        && issue.path().equals("$.triggers[0].actions[0].effect")));

        JsonObject invalidTrigger = talent.getAsJsonArray("triggers").get(0).getAsJsonObject();
        invalidTrigger.addProperty("chance", 1.1D);
        invalidTrigger.addProperty("cooldownTicks", -1);
        CompilationResult invalidTiming = compiler.compile(new RawDefinitionSet(
                Map.of(COMMON, rarity()), Map.of(VITALITY, talent), List.of()
        ), triggerEnvironment);
        assertTrue(invalidTiming.report().issues().stream().anyMatch(issue ->
                issue.code().equals("NUMBER_RANGE") && issue.path().equals("$.triggers[0].chance")));
        assertTrue(invalidTiming.report().issues().stream().anyMatch(issue ->
                issue.code().equals("INTEGER_RANGE")
                        && issue.path().equals("$.triggers[0].cooldownTicks")));
    }

    @Test
    void compilesEventItemAndBlockConditionsAgainstRegistries() {
        JsonObject talent = validTalent();
        talent.add("triggers", JsonParser.parseString("""
                [{
                  "id":"open_chest_with_key","event":"interact_block",
                  "condition":{"type":"all","children":[
                    {"type":"event_item","item":"minecraft:tripwire_hook"},
                    {"type":"event_item_tag","tag":"example:keys"},
                    {"type":"event_block","block":"minecraft:chest"},
                    {"type":"event_block_tag","tag":"minecraft:guarded_by_piglins"}
                  ]},
                  "actions":[{"type":"add_effect","effect":"minecraft:speed",
                    "durationTicks":20,"target":"self"}]
                }]
                """).getAsJsonArray());
        CompilationEnvironment triggerEnvironment = new CompilationEnvironment(
                Set.of("minecraft", "forge", "talent_editor"), Set.of(MAX_HEALTH, ATTACK_DAMAGE),
                Set.of(id("minecraft:tripwire_hook")), Set.of(), Set.of(id("minecraft:speed")),
                Set.of(id("minecraft:chest"))
        );

        CompilationResult result = compiler.compile(new RawDefinitionSet(
                Map.of(COMMON, rarity()), Map.of(VITALITY, talent), List.of()
        ), triggerEnvironment);

        assertFalse(result.report().hasErrors());
        var all = assertInstanceOf(com.kidxoxo.talenteditor.domain.CompiledCondition.All.class,
                result.candidate().orElseThrow().triggersByEvent()
                        .get(id("talent_editor:interact_block")).get(0).trigger().condition());
        assertInstanceOf(com.kidxoxo.talenteditor.domain.CompiledCondition.EventItem.class,
                all.children().get(0));
        assertInstanceOf(com.kidxoxo.talenteditor.domain.CompiledCondition.EventBlockTag.class,
                all.children().get(3));

        talent.getAsJsonArray("triggers").get(0).getAsJsonObject().getAsJsonObject("condition")
                .getAsJsonArray("children").get(2).getAsJsonObject()
                .addProperty("block", "minecraft:absent");
        CompilationResult invalid = compiler.compile(new RawDefinitionSet(
                Map.of(COMMON, rarity()), Map.of(VITALITY, talent), List.of()
        ), triggerEnvironment);
        assertTrue(invalid.report().issues().stream().anyMatch(issue ->
                issue.code().equals("UNKNOWN_EVENT_BLOCK")));
    }

    @Test
    void compilesAcquisitionAndSynthesisPresentationActions() {
        JsonObject talent = validTalent();
        talent.add("onAcquire", JsonParser.parseString("""
                [
                  {"type":"play_sound","sound":"minecraft:entity.player.levelup",
                   "source":"players","volume":1.5,"pitch":0.8},
                  {"type":"show_message","mode":"title",
                   "text":{"literal":"Talent acquired","color":"#55FFFF","bold":true},
                   "subtitle":{"translation":"talent.example.subtitle","italic":true},
                   "fadeInTicks":5,"stayTicks":60,"fadeOutTicks":15}
                ]
                """).getAsJsonArray());
        talent.add("onSynthesize", JsonParser.parseString("""
                [{"type":"show_message","mode":"action_bar","text":"Synthesis complete"}]
                """).getAsJsonArray());
        CompilationEnvironment presentationEnvironment = new CompilationEnvironment(
                Set.of("minecraft", "forge", "talent_editor"), Set.of(MAX_HEALTH, ATTACK_DAMAGE),
                Set.of(), Set.of(), Set.of(), Set.of(),
                Set.of(id("minecraft:entity.player.levelup"))
        );

        CompilationResult result = compiler.compile(new RawDefinitionSet(
                Map.of(COMMON, rarity()), Map.of(VITALITY, talent), List.of()
        ), presentationEnvironment);

        assertFalse(result.report().hasErrors());
        var definition = result.candidate().orElseThrow().talents().get(VITALITY);
        var sound = assertInstanceOf(com.kidxoxo.talenteditor.domain.PlaySoundAction.class,
                definition.onAcquire().get(0));
        assertEquals(id("minecraft:entity.player.levelup"), sound.sound());
        assertEquals(net.minecraft.sounds.SoundSource.PLAYERS, sound.source());
        var title = assertInstanceOf(com.kidxoxo.talenteditor.domain.ShowMessageAction.class,
                definition.onAcquire().get(1));
        assertEquals(com.kidxoxo.talenteditor.domain.MessageDisplay.TITLE, title.mode());
        assertEquals(0xFF55FFFF, title.text().color());
        assertEquals("talent.example.subtitle", title.subtitle().orElseThrow().translation());
        assertEquals(com.kidxoxo.talenteditor.domain.MessageDisplay.ACTION_BAR,
                assertInstanceOf(com.kidxoxo.talenteditor.domain.ShowMessageAction.class,
                        definition.onSynthesize().get(0)).mode());

        talent.getAsJsonArray("onAcquire").get(0).getAsJsonObject()
                .addProperty("sound", "minecraft:absent");
        CompilationResult unregisteredSound = compiler.compile(new RawDefinitionSet(
                Map.of(COMMON, rarity()), Map.of(VITALITY, talent), List.of()
        ), presentationEnvironment);
        assertFalse(unregisteredSound.report().hasErrors());
        assertTrue(unregisteredSound.candidate().isPresent());
        assertTrue(unregisteredSound.report().issues().stream().anyMatch(issue ->
                issue.code().equals("UNREGISTERED_SOUND_EVENT")));

        JsonObject rawSound = talent.getAsJsonArray("onAcquire").get(0).getAsJsonObject();
        rawSound.addProperty("source", "invalid");
        rawSound.addProperty("volume", 17);
        CompilationResult invalidSoundSettings = compiler.compile(new RawDefinitionSet(
                Map.of(COMMON, rarity()), Map.of(VITALITY, talent), List.of()
        ), presentationEnvironment);
        assertTrue(invalidSoundSettings.report().issues().stream().anyMatch(issue ->
                issue.code().equals("INVALID_SOUND_SOURCE")));
        assertTrue(invalidSoundSettings.report().issues().stream().anyMatch(issue ->
                issue.code().equals("NUMBER_RANGE") && issue.path().endsWith(".volume")));
        rawSound.addProperty("source", "players");
        rawSound.addProperty("volume", 1);
        talent.getAsJsonArray("onSynthesize").get(0).getAsJsonObject()
                .add("subtitle", JsonParser.parseString("\"not allowed\""));
        CompilationResult invalid = compiler.compile(new RawDefinitionSet(
                Map.of(COMMON, rarity()), Map.of(VITALITY, talent), List.of()
        ), presentationEnvironment);
        assertTrue(invalid.report().issues().stream().anyMatch(issue ->
                issue.code().equals("SUBTITLE_REQUIRES_TITLE")));
    }

    @Test
    void compilesEffectRemovalTriggersAndIndexesActiveImmunity() {
        JsonObject talent = validTalent();
        talent.getAsJsonArray("activeEffects").add(JsonParser.parseString("""
                {"type":"effect_immunity","effect":"minecraft:poison"}
                """).getAsJsonObject());
        talent.getAsJsonArray("activeEffects").add(JsonParser.parseString("""
                {"type":"add_effect","effect":"minecraft:speed","durationTicks":60,
                 "amplifier":1,"target":"self","ambient":true,"showParticles":false,"showIcon":true}
                """).getAsJsonObject());
        talent.add("triggers", JsonParser.parseString("""
                [{
                  "id":"cleanse_poison","event":"damage_taken",
                  "actions":[{"type":"remove_effect","effect":"minecraft:poison","target":"self"}]
                }]
                """).getAsJsonArray());
        CompilationEnvironment triggerEnvironment = new CompilationEnvironment(
                Set.of("minecraft", "forge", "talent_editor"), Set.of(MAX_HEALTH, ATTACK_DAMAGE), Set.of(),
                Set.of(), Set.of(id("minecraft:poison"), id("minecraft:speed"))
        );

        CompilationResult result = compiler.compile(new RawDefinitionSet(
                Map.of(COMMON, rarity()), Map.of(VITALITY, talent), List.of()
        ), triggerEnvironment);

        assertFalse(result.report().hasErrors());
        var snapshot = result.candidate().orElseThrow();
        assertEquals(VITALITY, snapshot.effectImmunityBindings().get(id("minecraft:poison"))
                .get(0).talentId());
        assertEquals(VITALITY, snapshot.activeStatusEffectBindings().get(id("minecraft:speed"))
                .get(0).talentId());
        assertEquals(1, snapshot.activeStatusEffectBindings().get(id("minecraft:speed"))
                .get(0).action().amplifier());
        var action = snapshot.triggersByEvent().get(id("talent_editor:damage_taken"))
                .get(0).trigger().actions().get(0);
        assertInstanceOf(com.kidxoxo.talenteditor.domain.RemoveEffectAction.class, action);

        var activeEffects = talent.getAsJsonArray("activeEffects");
        activeEffects.get(activeEffects.size() - 1).getAsJsonObject()
                .addProperty("target", "event_entity");
        CompilationResult invalidTarget = compiler.compile(new RawDefinitionSet(
                Map.of(COMMON, rarity()), Map.of(VITALITY, talent), List.of()
        ), triggerEnvironment);
        assertTrue(invalidTarget.report().issues().stream()
                .anyMatch(issue -> issue.code().equals("ACTIVE_EFFECT_TARGET_UNSUPPORTED")));
    }

    @Test
    void compilesBoundedDamageModificationOnlyForDamageEvents() {
        JsonObject talent = validTalent();
        talent.add("triggers", JsonParser.parseString("""
                [{
                  "id":"projectile_power","event":"damage_dealt_pre",
                  "condition":{"type":"damage_type_tag","tag":"minecraft:is_projectile"},
                  "actions":[{"type":"modify_damage","operation":"multiply","amount":1.5,
                    "minimum":2,"maximum":100}]
                }]
                """).getAsJsonArray());

        CompilationResult result = compiler.compile(new RawDefinitionSet(
                Map.of(COMMON, rarity()), Map.of(VITALITY, talent), List.of()
        ), environment);

        assertFalse(result.report().hasErrors());
        var action = assertInstanceOf(com.kidxoxo.talenteditor.domain.ModifyDamageAction.class,
                result.candidate().orElseThrow().triggersByEvent()
                        .get(id("talent_editor:damage_dealt_pre")).get(0).trigger().actions().get(0));
        assertEquals(com.kidxoxo.talenteditor.domain.DamageOperation.MULTIPLY, action.operation());
        assertEquals(1.5D, action.amount());
        assertEquals(2.0D, action.minimum());
        assertEquals(100.0D, action.maximum());

        JsonObject trigger = talent.getAsJsonArray("triggers").get(0).getAsJsonObject();
        trigger.addProperty("event", "interact_entity");
        CompilationResult wrongEvent = compiler.compile(new RawDefinitionSet(
                Map.of(COMMON, rarity()), Map.of(VITALITY, talent), List.of()
        ), environment);
        assertTrue(wrongEvent.report().issues().stream().anyMatch(issue ->
                issue.code().equals("DAMAGE_ACTION_EVENT_UNSUPPORTED")));

        trigger.addProperty("event", "damage_taken");
        JsonObject rawAction = trigger.getAsJsonArray("actions").get(0).getAsJsonObject();
        rawAction.addProperty("minimum", 20);
        rawAction.addProperty("maximum", 10);
        CompilationResult wrongBounds = compiler.compile(new RawDefinitionSet(
                Map.of(COMMON, rarity()), Map.of(VITALITY, talent), List.of()
        ), environment);
        assertTrue(wrongBounds.report().issues().stream().anyMatch(issue ->
                issue.code().equals("DAMAGE_BOUNDS_ORDER")));
    }

    @Test
    void compilesEntityAttributeDamageHealingAndExtendedTargets() {
        JsonObject talent = validTalent();
        talent.add("triggers", JsonParser.parseString("""
                [{
                  "id":"retaliating_aura","event":"damage_taken",
                  "actions":[
                    {"type":"modify_entity_attribute","attribute":"minecraft:generic.attack_damage",
                     "operation":"add","amount":1.5,"minimum":0,"maximum":100,
                     "target":"self","radius":4},
                    {"type":"damage_entity","amount":3,"target":"damage_source","radius":4},
                    {"type":"heal_entity","amount":2,"target":"nearby","radius":8}
                  ]
                }]
                """).getAsJsonArray());

        CompilationResult result = compiler.compile(new RawDefinitionSet(
                Map.of(COMMON, rarity()), Map.of(VITALITY, talent), List.of()
        ), environment);

        assertFalse(result.report().hasErrors());
        var actions = result.candidate().orElseThrow().talents().get(VITALITY)
                .triggers().get(0).actions();
        var attribute = assertInstanceOf(
                com.kidxoxo.talenteditor.domain.ModifyEntityAttributeAction.class, actions.get(0));
        assertEquals(ATTACK_DAMAGE, attribute.attribute());
        assertEquals(com.kidxoxo.talenteditor.domain.LivingEntityTarget.SELF, attribute.target());
        var damage = assertInstanceOf(
                com.kidxoxo.talenteditor.domain.DamageEntityAction.class, actions.get(1));
        assertEquals(com.kidxoxo.talenteditor.domain.LivingEntityTarget.DAMAGE_SOURCE, damage.target());
        var healing = assertInstanceOf(
                com.kidxoxo.talenteditor.domain.HealEntityAction.class, actions.get(2));
        assertEquals(com.kidxoxo.talenteditor.domain.LivingEntityTarget.NEARBY, healing.target());
        assertEquals(8.0D, healing.radius());
    }

    @Test
    void compilesAttributeDrivenChanceAndDamageCancellation() {
        JsonObject talent = validTalent();
        talent.add("triggers", JsonParser.parseString("""
                [{
                  "id":"lucky_guard","event":"damage_taken_pre","chance":0.1,"activationLimit":1,
                  "chanceAttribute":"minecraft:generic.luck","chancePerPoint":0.02,
                  "actions":[
                    {"type":"add_effect","effect":"minecraft:resistance",
                     "durationTicks":40,"target":"self"},
                    {"type":"cancel_damage"}
                  ]
                }]
                """).getAsJsonArray());
        CompilationEnvironment triggerEnvironment = new CompilationEnvironment(
                Set.of("minecraft", "forge", "talent_editor"),
                Set.of(MAX_HEALTH, ATTACK_DAMAGE, id("minecraft:generic.luck")), Set.of(), Set.of(),
                Set.of(id("minecraft:resistance"))
        );

        CompilationResult result = compiler.compile(new RawDefinitionSet(
                Map.of(COMMON, rarity()), Map.of(VITALITY, talent), List.of()
        ), triggerEnvironment);

        assertFalse(result.report().hasErrors());
        var trigger = result.candidate().orElseThrow().triggersByEvent()
                .get(id("talent_editor:damage_taken_pre")).get(0).trigger();
        assertEquals(id("minecraft:generic.luck"), trigger.chanceAttribute().orElseThrow());
        assertEquals(0.02D, trigger.chancePerPoint());
        assertEquals(1, trigger.activationLimit());
        assertEquals(2, trigger.actions().size());
        assertInstanceOf(com.kidxoxo.talenteditor.domain.CancelDamageAction.class, trigger.actions().get(1));

        JsonObject raw = talent.getAsJsonArray("triggers").get(0).getAsJsonObject();
        raw.addProperty("event", "player_second");
        CompilationResult wrongEvent = compiler.compile(new RawDefinitionSet(
                Map.of(COMMON, rarity()), Map.of(VITALITY, talent), List.of()
        ), triggerEnvironment);
        assertTrue(wrongEvent.report().issues().stream().anyMatch(issue ->
                issue.code().equals("DAMAGE_ACTION_EVENT_UNSUPPORTED")));

        raw.addProperty("event", "damage_taken_pre");
        raw.remove("chanceAttribute");
        CompilationResult missingAttribute = compiler.compile(new RawDefinitionSet(
                Map.of(COMMON, rarity()), Map.of(VITALITY, talent), List.of()
        ), triggerEnvironment);
        assertTrue(missingAttribute.report().issues().stream().anyMatch(issue ->
                issue.code().equals("CHANCE_ATTRIBUTE_REQUIRED")));

        raw.addProperty("chanceAttribute", "example:missing_attribute");
        CompilationResult unknownAttribute = compiler.compile(new RawDefinitionSet(
                Map.of(COMMON, rarity()), Map.of(VITALITY, talent), List.of()
        ), triggerEnvironment);
        assertTrue(unknownAttribute.report().issues().stream().anyMatch(issue ->
                issue.code().equals("UNKNOWN_ATTRIBUTE")
                        && issue.path().equals("$.triggers[0].chanceAttribute")));

        raw.addProperty("chanceAttribute", "minecraft:generic.luck");
        raw.addProperty("chancePerPoint", 1.1D);
        CompilationResult invalidFactor = compiler.compile(new RawDefinitionSet(
                Map.of(COMMON, rarity()), Map.of(VITALITY, talent), List.of()
        ), triggerEnvironment);
        assertTrue(invalidFactor.report().issues().stream().anyMatch(issue ->
                issue.code().equals("NUMBER_RANGE")
                        && issue.path().equals("$.triggers[0].chancePerPoint")));

        raw.addProperty("chancePerPoint", 0.02D);
        raw.addProperty("activationLimit", 1_000_001);
        CompilationResult invalidLimit = compiler.compile(new RawDefinitionSet(
                Map.of(COMMON, rarity()), Map.of(VITALITY, talent), List.of()
        ), triggerEnvironment);
        assertTrue(invalidLimit.report().issues().stream().anyMatch(issue ->
                issue.code().equals("INTEGER_RANGE")
                        && issue.path().equals("$.triggers[0].activationLimit")));
    }

    @Test
    void compilesConfiguredCarrierItemAndIncludesItInContentHash() {
        ResourceLocation itemId = id("example:talent_shard");
        JsonObject item = parse("""
                {
                  "schema": 2,
                  "name": {"literal":"Talent Shard","color":"#A855F7","bold":true},
                  "description": [
                    "Used by talent recipes.",
                    {"translation":"item.example.talent_shard.hint","italic":true}
                  ],
                  "customModelData": 1001,
                  "glint": true,
                  "loot": [
                    {"table":"minecraft:chests/simple_dungeon","chance":0.25,
                     "minCount":1,"maxCount":2},
                    {"table":"minecraft:chests/abandoned_mineshaft","chance":1.0,
                     "minCount":3,"maxCount":3}
                  ],
                  "requiredMods": ["minecraft"]
                }
                """);
        RawDefinitionSet definitions = validDefinitions().withConfiguredItem(itemId, item, "test/items");

        CompilationResult result = compiler.compile(definitions, environment);

        assertFalse(result.report().hasErrors());
        var compiled = result.candidate().orElseThrow().configuredItems().get(itemId);
        assertEquals("Talent Shard", compiled.name().fallbackText());
        assertEquals(2, compiled.description().size());
        assertEquals(1001, compiled.customModelData());
        assertTrue(compiled.glint());
        assertEquals(2, compiled.loot().size());
        assertEquals(id("minecraft:chests/simple_dungeon"),
                compiled.loot().get(0).exactTable().orElseThrow());
        assertEquals(0.25D, compiled.loot().get(0).chance());
        assertEquals(2, compiled.loot().get(0).maxCount());
        assertTrue(compiled.available());
        assertEquals(itemId, result.candidate().orElseThrow().configuredItemLootByTable()
                .get(id("minecraft:chests/simple_dungeon")).get(0).itemId());
        assertEquals("test/items", definitions.configuredItemSources().get(itemId));
        assertNotEquals(
                CanonicalJsonHasher.hash(validDefinitions()),
                CanonicalJsonHasher.hash(definitions)
        );
    }

    @Test
    void compilesEquippedConfiguredItemTriggersIntoTheRuntimeEventIndex() {
        ResourceLocation itemId = id("example:equipped_relic");
        JsonObject item = parse("""
                {
                  "schema":2,
                  "name":"Equipped Relic",
                  "triggers":[
                    {"id":"equipped_tick","event":"player_second","actions":[
                      {"type":"timed_attribute_modifier",
                       "attribute":"minecraft:generic.attack_damage","expression":"2",
                       "operation":"addition","modifierKey":"equipped_attack",
                       "durationTicks":40,"target":"self"}
                    ]}
                  ]
                }
                """);

        CompilationResult result = compiler.compile(
                validDefinitions().withConfiguredItem(itemId, item, "test/items"), environment
        );

        assertFalse(result.report().hasErrors(), () -> result.report().issues().toString());
        var compiled = result.candidate().orElseThrow().configuredItems().get(itemId);
        assertEquals(1, compiled.triggers().size());
        assertEquals(itemId, result.candidate().orElseThrow().triggersByEvent()
                .get(id("talent_editor:player_second")).get(0).talentId());
    }

    @Test
    void compilesDerivedCategoryCounterMetadata() {
        ResourceLocation counterId = id("example:light_count");
        CompilationResult result = compiler.compile(
                validDefinitions().withCounter(counterId, parse("""
                        {"schema":2,"name":"Light Count","event":"manual",
                         "displayCategory":"example:light"}
                        """), "test/counters"),
                environment
        );

        assertFalse(result.report().hasErrors(), () -> result.report().issues().toString());
        assertEquals(id("example:light"), result.candidate().orElseThrow().counters()
                .get(counterId).displayCategory().orElseThrow());
        assertTrue(result.candidate().orElseThrow().counterBookIds().contains(counterId));
    }

    @Test
    void acceptsConfiguredItemIdsInConditionsAndSynthesisAndIndexesReferences() {
        ResourceLocation itemId = id("example:talent_shard");
        JsonObject talent = validTalent();
        talent.add("acquireCondition", JsonParser.parseString("""
                {"type":"has_item","item":"example:talent_shard","count":2}
                """));
        talent.add("synthesis", JsonParser.parseString("""
                {"enabled":true,"consumeItems":{"example:talent_shard":3}}
                """));
        talent.add("triggers", JsonParser.parseString("""
                [{
                  "id":"shard_notice",
                  "event":"item_use_complete",
                  "condition":{"type":"event_item","item":"example:talent_shard"},
                  "actions":[{"type":"add_effect","effect":"minecraft:speed",
                    "durationTicks":20,"target":"self"}]
                }]
                """).getAsJsonArray());
        RawDefinitionSet definitions = validDefinitions()
                .withTalent(VITALITY, talent, "test/talents")
                .withCounter(id("example:shard_uses"), parse("""
                        {"schema":2,"name":"Shard Uses","event":"item_use",
                         "items":["example:talent_shard"]}
                        """), "test/counters")
                .withConfiguredItem(itemId, parse("""
                        {"schema":2,"name":"Talent Shard","customModelData":1001}
                        """), "test/items");

        CompilationEnvironment withSpeed = new CompilationEnvironment(
                environment.loadedMods(), environment.registeredAttributes(), Set.of(), Set.of(),
                Set.of(id("minecraft:speed"))
        );
        CompilationResult result = compiler.compile(definitions, withSpeed);

        assertFalse(result.report().hasErrors());
        var references = result.candidate().orElseThrow().referencesTo(
                com.kidxoxo.talenteditor.domain.DefinitionKind.ITEM, itemId
        );
        assertEquals(4, references.size());
        assertEquals(2, references.stream().filter(reference ->
                reference.usage().equals("condition_item")).count());
        assertEquals(1, references.stream().filter(reference ->
                reference.usage().equals("synthesis_item")).count());
        assertEquals(1, references.stream().filter(reference ->
                reference.usage().equals("counter_item_filter")).count());
    }

    @Test
    void rejectsConfiguredItemIdThatCollidesWithRegisteredItem() {
        ResourceLocation diamond = id("minecraft:diamond");
        CompilationEnvironment withDiamond = new CompilationEnvironment(
                environment.loadedMods(), environment.registeredAttributes(), Set.of(diamond)
        );
        CompilationResult result = compiler.compile(
                validDefinitions().withConfiguredItem(diamond, parse("""
                        {"schema":2,"name":"Ambiguous Diamond"}
                        """), "test/items"),
                withDiamond
        );

        assertTrue(result.report().issues().stream().anyMatch(issue ->
                issue.code().equals("CONFIGURED_ITEM_ID_COLLISION")));
    }

    @Test
    void allowsBuiltInTalentBookAndPointDefinitionsToOverlayRegisteredSystemItems() {
        ResourceLocation book = id("talent_editor:talent_book");
        ResourceLocation point = id("talent_editor:talent_point");
        CompilationEnvironment withSystemItems = new CompilationEnvironment(
                environment.loadedMods(), environment.registeredAttributes(), Set.of(book, point)
        );
        RawDefinitionSet definitions = validDefinitions()
                .withConfiguredItem(book, parse("""
                        {"schema":2,"name":"Editable Talent Book",
                         "use":{"enabled":true,"actions":[
                           {"type":"show_message","text":"ignored"}
                         ]}}
                        """), "builtin/items")
                .withConfiguredItem(point, parse("""
                        {"schema":2,"name":"Editable Talent Point"}
                        """), "builtin/items");

        CompilationResult result = compiler.compile(definitions, withSystemItems);

        assertFalse(result.report().hasErrors());
        assertTrue(result.candidate().orElseThrow().configuredItems().containsKey(book));
        assertTrue(result.candidate().orElseThrow().configuredItems().containsKey(point));
        assertFalse(result.candidate().orElseThrow().configuredItems().get(book).use().enabled());
        assertTrue(result.report().issues().stream().anyMatch(issue ->
                issue.code().equals("SYSTEM_ITEM_USE_IGNORED")));
    }

    @Test
    void validatesConfiguredItemFieldsAndMissingMods() {
        ResourceLocation optionalId = id("example:optional_relic");
        JsonObject optional = parse("""
                {
                  "schema": 2,
                  "name": "Optional Relic",
                  "requiredMods": ["optional_relics"]
                }
                """);
        CompilationResult missingMod = compiler.compile(
                validDefinitions().withConfiguredItem(optionalId, optional, "test/optional"), environment
        );

        assertFalse(missingMod.report().hasErrors());
        assertTrue(missingMod.report().issues().stream().anyMatch(issue ->
                issue.code().equals("REQUIRED_MODS_MISSING") && issue.path().equals("$.requiredMods")));
        assertFalse(missingMod.candidate().orElseThrow().configuredItems().get(optionalId).available());

        JsonObject invalid = parse("""
                {
                  "schema": 2,
                  "name": "Broken Relic",
                  "customModelData": 16777216,
                  "modelData": 7
                }
                """);
        CompilationResult rejected = compiler.compile(
                validDefinitions().withConfiguredItem(id("example:broken_relic"), invalid, "test/broken"),
                environment
        );

        assertTrue(rejected.report().hasErrors());
        assertTrue(rejected.report().issues().stream().anyMatch(issue ->
                issue.code().equals("INTEGER_RANGE") && issue.path().equals("$.customModelData")));
        assertTrue(rejected.report().issues().stream().anyMatch(issue ->
                issue.code().equals("UNKNOWN_FIELD") && issue.path().equals("$.modelData")));
    }

    @Test
    void rejectsMalformedDuplicateAndOutOfRangeConfiguredItemLootRules() {
        JsonObject invalid = parse("""
                {
                  "schema":2,
                  "name":"Broken Loot",
                  "loot":[
                    {"table":"minecraft:chests/simple_dungeon","chance":1.1,
                     "minCount":0,"maxCount":65,"future":true},
                    {"table":"minecraft:chests/simple_dungeon","chance":0.5,
                     "minCount":4,"maxCount":2}
                  ]
                }
                """);

        CompilationResult result = compiler.compile(
                validDefinitions().withConfiguredItem(id("example:broken_loot"), invalid, "test/items"),
                environment
        );

        assertTrue(result.report().hasErrors());
        assertTrue(result.report().issues().stream().anyMatch(issue ->
                issue.code().equals("NUMBER_RANGE") && issue.path().equals("$.loot[0].chance")));
        assertTrue(result.report().issues().stream().anyMatch(issue ->
                issue.code().equals("UNKNOWN_FIELD") && issue.path().equals("$.loot[0].future")));
        assertTrue(result.report().issues().stream().anyMatch(issue ->
                issue.code().equals("DUPLICATE_LOOT_TABLE")));
        assertTrue(result.report().issues().stream().anyMatch(issue ->
                issue.code().equals("LOOT_COUNT_RANGE")));
    }

    @Test
    void compilesDimensionEntityAndLuckLootConditions() {
        JsonObject item = parse("""
                {
                  "schema":2,"name":"Conditional Relic","loot":[
                    {"table":"minecraft:entities/zombie","chance":0.1,"chancePerLuck":0.02,
                     "minCount":1,"maxCount":2,"dimensions":["minecraft:overworld"],
                     "entityTarget":"this_entity","entityTypes":["minecraft:zombie"],
                     "entityTags":["minecraft:undead"]},
                    {"table":"minecraft:entities/zombie","chance":0.5,
                     "dimensions":["minecraft:the_nether"]}
                  ]
                }
                """);
        CompilationEnvironment lootEnvironment = new CompilationEnvironment(
                environment.loadedMods(), environment.registeredAttributes(), Set.of(),
                Set.of(id("minecraft:zombie"))
        );

        CompilationResult result = compiler.compile(
                validDefinitions().withConfiguredItem(id("example:conditional_relic"), item, "test/items"),
                lootEnvironment
        );

        assertFalse(result.report().hasErrors());
        var rules = result.candidate().orElseThrow().configuredItems()
                .get(id("example:conditional_relic")).loot();
        assertEquals(2, rules.size());
        assertEquals(0.02D, rules.get(0).chancePerLuck());
        assertEquals(List.of(id("minecraft:overworld")), rules.get(0).dimensions());
        assertEquals(List.of(id("minecraft:zombie")), rules.get(0).entityTypes());
        assertEquals(List.of(id("minecraft:undead")), rules.get(0).entityTags());
        assertEquals("this_entity", rules.get(0).entityTarget().serializedName());
    }

    @Test
    void compilesPrefixAndRegexLootTableMatchersIntoPatternIndex() {
        JsonObject item = parse("""
                {"schema":2,"name":"Pattern Relic","loot":[
                  {"tableMatch":"prefix","table":"minecraft:chests/","chance":0.2},
                  {"tableMatch":"regex","table":"example:(?:chests|entities)/rare_.*","chance":1}
                ]}
                """);

        CompilationResult result = compiler.compile(
                validDefinitions().withConfiguredItem(id("example:pattern_relic"), item, "test/items"),
                environment
        );

        assertFalse(result.report().hasErrors());
        var snapshot = result.candidate().orElseThrow();
        assertEquals(2, snapshot.configuredItemLootPatterns().size());
        assertTrue(snapshot.configuredItemLootPatterns().get(0).rule()
                .matchesTable(id("minecraft:chests/simple_dungeon")));
        assertTrue(snapshot.configuredItemLootPatterns().get(1).rule()
                .matchesTable(id("example:entities/rare_zombie")));
        assertFalse(snapshot.configuredItemLootPatterns().get(1).rule()
                .matchesTable(id("example:entities/common_zombie")));
    }

    @Test
    void rejectsInvalidLootTableMatchModesAndExpressions() {
        JsonObject item = parse("""
                {"schema":2,"name":"Broken Matchers","loot":[
                  {"tableMatch":"glob","table":"minecraft:chests/*"},
                  {"tableMatch":"prefix","table":"Minecraft:chests/"},
                  {"tableMatch":"regex","table":"(["}
                ]}
                """);

        CompilationResult result = compiler.compile(
                validDefinitions().withConfiguredItem(id("example:broken_matchers"), item, "test/items"),
                environment
        );

        assertTrue(result.report().hasErrors());
        assertTrue(result.report().issues().stream().anyMatch(issue ->
                issue.code().equals("INVALID_LOOT_TABLE_MATCH")
                        && issue.path().equals("$.loot[0].tableMatch")));
        assertEquals(2, result.report().issues().stream().filter(issue ->
                issue.code().equals("INVALID_LOOT_TABLE_EXPRESSION")).count());
    }

    @Test
    void rejectsInvalidAdvancedLootConditions() {
        JsonObject item = parse("""
                {"schema":2,"name":"Broken Conditions","loot":[{
                  "table":"minecraft:entities/zombie","chance":0.5,"chancePerLuck":2,
                  "entityTarget":"direct_killer","entityTypes":["example:missing"]
                }]}
                """);

        CompilationResult result = compiler.compile(
                validDefinitions().withConfiguredItem(id("example:broken_conditions"), item, "test/items"),
                environment
        );

        assertTrue(result.report().hasErrors());
        assertTrue(result.report().issues().stream().anyMatch(issue ->
                issue.code().equals("NUMBER_RANGE") && issue.path().equals("$.loot[0].chancePerLuck")));
        assertTrue(result.report().issues().stream().anyMatch(issue ->
                issue.code().equals("INVALID_LOOT_ENTITY_TARGET")));
        assertTrue(result.report().issues().stream().anyMatch(issue ->
                issue.code().equals("UNKNOWN_ENTITY_TYPE") && issue.path().equals("$.loot[0].entityTypes[0]")));
    }

    @Test
    void compilesConfiguredItemAttributesAndSelfUseActions() {
        ResourceLocation itemId = id("example:swift_relic");
        JsonObject item = parse("""
                {
                  "schema":2,
                  "name":"Swift Relic",
                  "attributeModifiers":[
                    {"attribute":"minecraft:generic.attack_damage","amount":2,
                     "operation":"addition","slot":"mainhand","modifierKey":"attack"},
                    {"attribute":"minecraft:generic.max_health","amount":0.1,
                     "operation":"multiply_total","slot":"offhand","modifierKey":"health"}
                  ],
                  "use":{
                    "enabled":true,"cooldownTicks":40,"consumeCount":1,
                    "actions":[
                      {"type":"add_effect","effect":"minecraft:speed","durationTicks":100,
                       "amplifier":1,"target":"self"},
                      {"type":"play_sound","sound":"minecraft:entity.player.levelup"},
                      {"type":"show_message","mode":"action_bar","text":{"literal":"Swift!","color":"#55FFFF"}}
                    ]
                  }
                }
                """);
        CompilationEnvironment itemEnvironment = new CompilationEnvironment(
                environment.loadedMods(), environment.registeredAttributes(), Set.of(), Set.of(),
                Set.of(id("minecraft:speed")), Set.of(), Set.of(id("minecraft:entity.player.levelup"))
        );

        CompilationResult result = compiler.compile(
                validDefinitions().withConfiguredItem(itemId, item, "test/items"), itemEnvironment
        );

        assertFalse(result.report().hasErrors());
        var compiled = result.candidate().orElseThrow().configuredItems().get(itemId);
        assertEquals(2, compiled.attributeModifiers().size());
        assertEquals("mainhand", compiled.attributeModifiers().get(0).slot().serializedName());
        assertEquals("health", compiled.attributeModifiers().get(1).modifierKey());
        assertTrue(compiled.use().enabled());
        assertEquals(40, compiled.use().cooldownTicks());
        assertEquals(1, compiled.use().consumeCount());
        assertEquals(3, compiled.use().actions().size());
    }

    @Test
    void rejectsInvalidConfiguredItemAttributeAndUseBoundaries() {
        JsonObject item = parse("""
                {
                  "schema":2,
                  "name":"Broken Relic",
                  "attributeModifiers":[
                    {"attribute":"minecraft:generic.attack_damage","amount":1,
                     "slot":"mainhand","modifierKey":"same"},
                    {"attribute":"example:missing","amount":1,
                     "slot":"belt","modifierKey":"same"}
                  ],
                  "use":{"actions":[
                    {"type":"add_effect","effect":"minecraft:speed","target":"event_entity"},
                    {"type":"modify_damage","operation":"add","amount":1}
                  ]}
                }
                """);
        CompilationEnvironment itemEnvironment = new CompilationEnvironment(
                environment.loadedMods(), environment.registeredAttributes(), Set.of(), Set.of(),
                Set.of(id("minecraft:speed"))
        );

        CompilationResult result = compiler.compile(
                validDefinitions().withConfiguredItem(id("example:broken_relic"), item, "test/items"),
                itemEnvironment
        );

        assertTrue(result.report().hasErrors());
        assertTrue(result.report().issues().stream().anyMatch(issue ->
                issue.code().equals("DUPLICATE_MODIFIER_KEY")));
        assertTrue(result.report().issues().stream().anyMatch(issue ->
                issue.code().equals("INVALID_EQUIPMENT_SLOT")));
        assertTrue(result.report().issues().stream().anyMatch(issue ->
                issue.code().equals("UNKNOWN_ATTRIBUTE") && issue.path().equals("$.attributeModifiers[1].attribute")));
        assertTrue(result.report().issues().stream().anyMatch(issue ->
                issue.code().equals("ITEM_USE_TARGET_UNSUPPORTED")));
        assertTrue(result.report().issues().stream().anyMatch(issue ->
                issue.code().equals("ACTION_PLACEMENT_UNSUPPORTED") && issue.path().equals("$.use.actions[1]")));
    }

    private CompilationResult compileTestLayout(JsonObject layout) {
        return compiler.compile(new RawDefinitionSet(
                Map.of(COMMON, rarity()), Map.of(VITALITY, validTalent()), Map.of(), Map.of(),
                Map.of(id("example:test_layout"), layout),
                Map.of(), Map.of(), Map.of(), Map.of(), Map.of(), List.of()), environment);
    }

    private static JsonObject rarity() {
        return parse("""
                {
                  "schema": 2,
                  "name": "Common",
                  "color": "#A0A0A0",
                  "order": 0,
                  "offerable": true,
                  "baseWeight": 1.0
                }
                """);
    }

    private static JsonObject validTalent() {
        return parse("""
                {
                  "schema": 2,
                  "text": {
                    "name": "Vitality",
                    "description": ["Adds health."]
                  },
                  "rarity": "talent_editor:common",
                  "activeEffects": [
                    {
                      "type": "attribute_modifier",
                      "attribute": "minecraft:generic.max_health",
                      "amount": 4,
                      "operation": "addition"
                    }
                  ]
                }
                """);
    }

    private static JsonObject parse(String json) {
        return JsonParser.parseString(json).getAsJsonObject();
    }

    private static ResourceLocation id(String value) {
        return new ResourceLocation(value);
    }
}
