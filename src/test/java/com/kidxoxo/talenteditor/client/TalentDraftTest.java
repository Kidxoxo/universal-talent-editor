package com.kidxoxo.talenteditor.client;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TalentDraftTest {
    @Test
    void attributeRequirementIsVisuallyEditableAndRoundTrips() {
        TalentDraft draft = TalentDraft.parse("""
                {"schema":2,"text":{"name":"Blade"},"rarity":"talent_editor:common",
                 "acquireCondition":{"type":"all","children":[
                   {"type":"has_talent","talent":"example:sword"},
                   {"type":"attribute_compare","attribute":"minecraft:generic.attack_speed",
                    "operator":"gte","value":8}]}}
                """);
        TalentDraft.RequirementsDraft requirements = draft.requirements();
        assertTrue(requirements.editable());
        assertEquals(TalentDraft.RequirementType.ATTRIBUTE, requirements.rows().get(1).type());
        assertEquals("gte", requirements.rows().get(1).operator());
        assertEquals(8, requirements.rows().get(1).attributeValue());
        draft.updateRequirements(requirements.mode(), requirements.rows());
        TalentDraft reopened = TalentDraft.parse(draft.toJson("Blade"));
        assertEquals(requirements.rows(), reopened.requirements().rows());
    }

    @Test
    void individualEffectGatesRemainVisibleAndSurviveDraftRoundTrip() {
        String source = """
                {"schema":2,"text":{"name":"Gated"},"rarity":"talent_editor:common",
                 "activeEffects":[
                   {"type":"attribute_modifier","attribute":"minecraft:generic.armor",
                    "amount":5,"operation":"addition",
                    "disabledWhen":{"type":"has_talent","talent":"example:override"}},
                   {"type":"add_effect","effect":"minecraft:speed","target":"self",
                    "durationTicks":100,"amplifier":0,
                    "enabledWhen":{"type":"has_talent","talent":"example:required"}}],
                 "triggers":[{"id":"hit","event":"damage_dealt","actions":[
                   {"type":"modify_damage_expression","operation":"add","expression":"40",
                    "disabledWhen":{"type":"has_talent","talent":"example:override"}}]}]}
                """;
        TalentDraft draft = TalentDraft.parse(source);
        assertEquals(2, draft.basicEffectRows().size());
        assertFalse(draft.editorOverview().section(TalentDraft.EditorSection.RAW_JSON).configured());
        assertTrue(draft.advancedAction(TalentDraft.AdvancedActionLocation.ACTIVE_EFFECTS, -1, 0)
                .has("disabledWhen"));
        assertTrue(draft.advancedAction(TalentDraft.AdvancedActionLocation.TRIGGER_ACTIONS, 0, 0)
                .has("disabledWhen"));
        TalentDraft reopened = TalentDraft.parse(draft.toJson("Gated"));
        assertEquals(2, reopened.basicEffectRows().size());
        assertTrue(reopened.advancedAction(TalentDraft.AdvancedActionLocation.ACTIVE_EFFECTS, -1, 1)
                .has("enabledWhen"));
        assertTrue(reopened.advancedAction(TalentDraft.AdvancedActionLocation.TRIGGER_ACTIONS, 0, 0)
                .has("disabledWhen"));
    }
    @Test
    void newTalentUsesDrawableDefaultOfferWhileLegacyOmissionStaysDisabled() {
        TalentDraft created = TalentDraft.create();
        assertTrue(created.hasOfferDefinition());
        assertTrue(created.offer().enabled());
        assertEquals(1.0D, created.offer().weight());
        assertEquals(java.util.List.of(new ResourceLocation("talent_editor", "default")),
                created.offer().pools());
        assertFalse(created.editorOverview().section(TalentDraft.EditorSection.OFFER).configured());

        TalentDraft legacy = TalentDraft.parse("""
                {"schema":2,"text":{"name":"Legacy"},
                 "rarity":"talent_editor:common","activeEffects":[]}
                """);
        assertFalse(legacy.hasOfferDefinition());
        assertFalse(legacy.offer().enabled());
        assertTrue(legacy.editorOverview().section(TalentDraft.EditorSection.OFFER).configured());
        assertFalse(JsonParser.parseString(legacy.toJson("Legacy")).getAsJsonObject().has("offer"));
    }

    @Test
    void editorOverviewTracksVisualSectionsAndAdvancedJsonFallback() {
        TalentDraft draft = TalentDraft.create();
        TalentDraft.EditorOverview defaults = draft.editorOverview();
        assertFalse(defaults.section(TalentDraft.EditorSection.TEXT).configured());
        assertFalse(defaults.section(TalentDraft.EditorSection.METADATA).configured());
        assertFalse(defaults.section(TalentDraft.EditorSection.BASIC_EFFECTS).configured());
        assertFalse(defaults.section(TalentDraft.EditorSection.RAW_JSON).configured());

        draft.addAttribute(new ResourceLocation("minecraft", "generic.armor"));
        draft.updateLifecycle(new TalentDraft.LifecycleDraft(200, 0, false, false, false));
        TalentDraft.EditorOverview changed = draft.editorOverview();
        assertEquals(1, changed.section(TalentDraft.EditorSection.BASIC_EFFECTS).count());
        assertTrue(changed.section(TalentDraft.EditorSection.BASIC_EFFECTS).configured());
        assertTrue(changed.section(TalentDraft.EditorSection.LIFECYCLE).configured());

        TalentDraft advanced = TalentDraft.parse("""
                {"schema":2,"text":{"name":"Advanced"},"rarity":"talent_editor:common",
                 "activeEffects":[{"type":"future:unknown_effect","value":7}]}
                """);
        assertTrue(advanced.editorOverview().section(TalentDraft.EditorSection.RAW_JSON).configured());
        assertTrue(advanced.editorOverview().section(TalentDraft.EditorSection.RAW_JSON).advancedOnly());
    }

    @Test
    void metadataUpdatesDoNotRewriteStyledDescriptionsAndEffectGroupsPreserveUnknownRows() {
        TalentDraft draft = TalentDraft.parse("""
                {
                  "schema":2,
                  "text":{"name":"Grouped","description":[{"literal":"Keep","color":"#55AAFF"}]},
                  "rarity":"talent_editor:common",
                  "activeEffects":[
                    {"type":"attribute_modifier","attribute":"minecraft:generic.armor","amount":2},
                    {"type":"dynamic_attribute_modifier","attribute":"minecraft:generic.attack_damage",
                     "source":"health_percent","base":0,"scale":1},
                    {"type":"future:unknown_effect","value":9}
                  ]
                }
                """);
        draft.updateMetadata(false, new ResourceLocation("talent_editor", "common"), false,
                java.util.List.of(new ResourceLocation("example", "combat")),
                java.util.Optional.empty(), 2);
        assertEquals(1, draft.basicEffectRows().size());
        assertEquals(1, draft.specialEffectRows().size());
        JsonObject output = JsonParser.parseString(draft.toJson("Grouped")).getAsJsonObject();
        assertEquals("#55AAFF", output.getAsJsonObject("text").getAsJsonArray("description")
                .get(0).getAsJsonObject().get("color").getAsString());
        assertEquals(9, output.getAsJsonArray("activeEffects").get(2)
                .getAsJsonObject().get("value").getAsInt());
    }

    @Test
    void activeAdvancedEditorListsOnlyHudAndRenderRowsWithoutDroppingOrdinaryEffects() {
        TalentDraft draft = TalentDraft.parse("""
                {
                  "schema":2,
                  "text":{"name":"Active interfaces"},
                  "rarity":"talent_editor:common",
                  "activeEffects":[
                    {"type":"attribute_modifier","attribute":"minecraft:generic.armor","amount":2},
                    {"type":"hud_resource","state":"example:energy","maximum":"100"},
                    {"type":"future:custom_action","customValue":7},
                    {"type":"player_render","alpha":0.7,"scale":1.1}
                  ]
                }
                """);

        var rows = draft.advancedActionRows(TalentDraft.AdvancedActionLocation.ACTIVE_EFFECTS, -1);
        assertEquals(2, rows.size());
        assertEquals(1, rows.get(0).rawIndex());
        assertEquals("hud_resource", rows.get(0).type());
        assertEquals(3, rows.get(1).rawIndex());
        assertEquals(java.util.List.of(new ResourceLocation("example", "energy")), draft.numericStateIds());

        draft.moveAdvancedAction(TalentDraft.AdvancedActionLocation.ACTIVE_EFFECTS, -1, 3, -1);
        JsonArray effects = JsonParser.parseString(draft.toJson("Active interfaces"))
                .getAsJsonObject().getAsJsonArray("activeEffects");
        assertEquals("attribute_modifier", effects.get(0).getAsJsonObject().get("type").getAsString());
        assertEquals("player_render", effects.get(1).getAsJsonObject().get("type").getAsString());
        assertEquals(7, effects.get(2).getAsJsonObject().get("customValue").getAsInt());
        assertEquals("hud_resource", effects.get(3).getAsJsonObject().get("type").getAsString());
    }

    @Test
    void visualAdvancedEditorsMutateConditionsAndOrderedActionChainsWithoutDroppingLegacyActions() {
        TalentDraft draft = TalentDraft.parse("""
                {
                  "schema":2,
                  "text":{"name":"Visual Advanced"},
                  "rarity":"talent_editor:common",
                  "triggers":[{
                    "id":"guard","event":"fatal_damage",
                    "condition":{"type":"event_entity_tag","tag":"minecraft:undead"},
                    "actions":[{"type":"cancel_damage"}]
                  }]
                }
                """);

        TalentDraft.ExpressionConditionDraft original = draft.triggerExpressionCondition(0);
        assertFalse(original.expression());
        assertTrue(original.hadCondition());
        draft.updateTriggerExpressionCondition(0, "health_percent", "lte", "0.25");
        assertTrue(draft.triggerExpressionCondition(0).expression());

        JsonObject state = JsonParser.parseString("""
                {"type":"talent_editor:modify_state","state":"example:guard",
                 "operation":"add","expression":"1","minimum":0,"maximum":10,
                 "target":"self","radius":4}
                """).getAsJsonObject();
        draft.putAdvancedAction(TalentDraft.AdvancedActionLocation.TRIGGER_ACTIONS, 0, -1, state);
        assertEquals(java.util.List.of(new ResourceLocation("example", "guard")), draft.numericStateIds());
        assertEquals(2, draft.advancedActionRows(
                TalentDraft.AdvancedActionLocation.TRIGGER_ACTIONS, 0).size());
        assertFalse(draft.advancedActionRows(
                TalentDraft.AdvancedActionLocation.TRIGGER_ACTIONS, 0).get(0).editable());
        assertTrue(draft.advancedActionRows(
                TalentDraft.AdvancedActionLocation.TRIGGER_ACTIONS, 0).get(1).editable());
        assertFalse(draft.editorOverview().section(TalentDraft.EditorSection.RAW_JSON).configured());
        assertTrue(draft.editorOverview().section(TalentDraft.EditorSection.TRIGGERS).advancedOnly());

        draft.moveAdvancedAction(TalentDraft.AdvancedActionLocation.TRIGGER_ACTIONS, 0, 1, -1);
        JsonObject reward = JsonParser.parseString("""
                {"type":"talent_editor:give_item","item":"minecraft:diamond",
                 "count":"2","dropOverflow":true}
                """).getAsJsonObject();
        draft.putAdvancedAction(TalentDraft.AdvancedActionLocation.ON_ACQUIRE, -1, -1, reward);

        JsonObject output = JsonParser.parseString(draft.toJson("Visual Advanced")).getAsJsonObject();
        JsonObject condition = output.getAsJsonArray("triggers").get(0).getAsJsonObject()
                .getAsJsonObject("condition");
        assertEquals("talent_editor:expression_compare", condition.get("type").getAsString());
        assertEquals("health_percent", condition.get("left").getAsString());
        assertEquals("talent_editor:modify_state", output.getAsJsonArray("triggers").get(0)
                .getAsJsonObject().getAsJsonArray("actions").get(0).getAsJsonObject()
                .get("type").getAsString());
        assertEquals("cancel_damage", output.getAsJsonArray("triggers").get(0)
                .getAsJsonObject().getAsJsonArray("actions").get(1).getAsJsonObject()
                .get("type").getAsString());
        assertEquals("minecraft:diamond", output.getAsJsonArray("onAcquire").get(0)
                .getAsJsonObject().get("item").getAsString());
    }

    @Test
    void exposesEventTriggersAsEditableUnifiedEffectRows() {
        TalentDraft draft = TalentDraft.create();
        draft.addAttribute(new ResourceLocation("minecraft", "generic.attack_damage"));
        int triggerRawIndex = draft.addEffectTrigger();

        assertEquals(1, draft.effectRows().size());
        assertEquals(2, draft.allEffectRows().size());
        TalentDraft.EffectRow event = draft.allEffectRows().get(1);
        assertTrue(TalentDraft.isEventEffect(event));
        assertTrue(draft.isEditableEventEffect(event));
        assertEquals(triggerRawIndex, TalentDraft.eventRawIndex(event));
        assertEquals(TalentDraft.eventSelectionKey(triggerRawIndex), event.rawIndex());
        assertEquals("effect_1", event.idOrType());
        assertEquals("damage_dealt", event.dynamicSource());

        draft.removeEffectTrigger(triggerRawIndex);
        assertEquals(1, draft.allEffectRows().size());
    }

    @Test
    void editsMultipleAttributesAndPreservesUnrepresentedFieldsAndActions() {
        TalentDraft draft = TalentDraft.parse("""
                {
                  "schema": 2,
                  "text": {
                    "name": {"literal": "Old", "color": "#55FF55"},
                    "description": [{"literal": "Kept description", "color": "#AAAAAA"}]
                  },
                  "rarity": "talent_editor:common",
                  "categories": ["example:combat"],
                  "activeEffects": [
                    {
                      "type": "attribute_modifier",
                      "attribute": "minecraft:generic.max_health",
                      "amount": 4,
                      "operation": "addition",
                      "showInDescription": false
                    },
                    {
                      "type": "talent_editor:attribute_modifier",
                      "attribute": "minecraft:generic.attack_damage",
                      "amount": 0.2,
                      "operation": "multiply_total"
                    },
                    {"type": "future:custom_action", "customValue": 7}
                  ]
                }
                """);

        assertEquals(3, draft.effectRows().size());
        assertTrue(draft.effectRows().get(0).editableAttribute());
        assertTrue(draft.effectRows().get(1).editableAttribute());
        assertFalse(draft.effectRows().get(2).editableAttribute());

        draft.updateAttribute(1, 0.35D, "multiply_base");
        draft.removeAttribute(0);
        draft.addAttribute(new ResourceLocation("minecraft", "generic.armor"));
        draft.updateOffer(true, 2.5D, java.util.List.of(new ResourceLocation("example", "main")));
        TalentDraft.MetadataDraft metadata = draft.metadata();
        draft.updateMetadata(
                false,
                new ResourceLocation("talent_editor", "common"),
                false,
                java.util.List.of(
                        new ResourceLocation("example", "combat"),
                        new ResourceLocation("example", "survival")
                ),
                java.util.Optional.of(new ResourceLocation("example", "textures/talent/vitality.png")),
                2
        );
        TalentDraft.TextStylesDraft textStyles = draft.textStyles();
        draft.updateTextStyles(
                new LocalizedTextDraft(
                        "Updated", "talent.example.updated", java.util.Optional.of(0xFF55FF55), true, false
                ),
                textStyles.descriptions()
        );
        JsonObject output = JsonParser.parseString(draft.toJson("Updated")).getAsJsonObject();

        assertEquals("Updated", output.getAsJsonObject("text").getAsJsonObject("name").get("literal").getAsString());
        assertEquals("#55FF55", output.getAsJsonObject("text").getAsJsonObject("name").get("color").getAsString());
        assertEquals("talent.example.updated",
                output.getAsJsonObject("text").getAsJsonObject("name").get("translation").getAsString());
        assertTrue(output.getAsJsonObject("text").getAsJsonObject("name").get("bold").getAsBoolean());
        assertFalse(output.get("enabled").getAsBoolean());
        assertEquals("example:survival", output.getAsJsonArray("categories").get(1).getAsString());
        assertEquals("#AAAAAA", output.getAsJsonObject("text").getAsJsonArray("description")
                .get(0).getAsJsonObject().get("color").getAsString());
        assertFalse(output.getAsJsonObject("text").get("showGeneratedEffects").getAsBoolean());
        assertEquals("example:textures/talent/vitality.png", output.get("icon").getAsString());
        assertEquals(2, output.get("capacityCost").getAsInt());
        JsonArray effects = output.getAsJsonArray("activeEffects");
        assertEquals(3, effects.size());
        assertEquals(0.35D, effects.get(0).getAsJsonObject().get("amount").getAsDouble());
        assertEquals("multiply_base", effects.get(0).getAsJsonObject().get("operation").getAsString());
        assertEquals(7, effects.get(1).getAsJsonObject().get("customValue").getAsInt());
        assertEquals("minecraft:generic.armor",
                effects.get(2).getAsJsonObject().get("attribute").getAsString());
        assertTrue(output.getAsJsonObject("offer").get("enabled").getAsBoolean());
        assertEquals(2.5D, output.getAsJsonObject("offer").get("weight").getAsDouble());
        assertEquals("example:main",
                output.getAsJsonObject("offer").getAsJsonArray("pools").get(0).getAsString());
    }

    @Test
    void descriptionEditorEscapingRoundTripsLineBreaksAndLiteralBackslashes() {
        assertEquals(
                java.util.List.of("First", "literal\\n", "Third"),
                TalentDraft.parseDescriptionEditorText("First\\nliteral\\\\n\\nThird")
        );
    }

    @Test
    void editsFlatRequirementsAndPreservesAdvancedTreesAsReadOnly() {
        TalentDraft draft = TalentDraft.parse("""
                {
                  "schema": 2,
                  "text": {"name": "Upgrade"},
                  "rarity": "talent_editor:common",
                  "acquireCondition": {
                    "type": "all",
                    "children": [
                      {"type": "has_talent", "talent": "example:starter"},
                      {"type": "counter_at_least", "counter": "example:kills", "value": 10},
                      {"type": "category_count_at_least", "category": "example:combat", "count": 2},
                      {"type": "has_item", "item": "minecraft:diamond", "count": 3}
                    ]
                  }
                }
                """);
        TalentDraft.RequirementsDraft requirements = draft.requirements();
        assertTrue(requirements.editable());
        assertEquals(4, requirements.rows().size());
        assertEquals(TalentDraft.RequirementType.CATEGORY, requirements.rows().get(2).type());
        draft.updateRequirements(TalentDraft.RequirementMode.ANY, requirements.rows().subList(1, 4));
        JsonObject output = JsonParser.parseString(draft.toJson("Upgrade")).getAsJsonObject();
        assertEquals("any", output.getAsJsonObject("acquireCondition").get("type").getAsString());
        assertEquals(3, output.getAsJsonObject("acquireCondition").getAsJsonArray("children").size());

        TalentDraft advanced = TalentDraft.parse("""
                {
                  "schema": 2,
                  "text": {"name": "Advanced"},
                  "rarity": "talent_editor:common",
                  "acquireCondition": {
                    "type": "not",
                    "child": {"type": "has_talent", "talent": "example:blocked"}
                  }
                }
                """);
        assertFalse(advanced.requirements().editable());
        JsonObject preserved = JsonParser.parseString(advanced.toJson("Advanced")).getAsJsonObject();
        assertEquals("not", preserved.getAsJsonObject("acquireCondition").get("type").getAsString());
    }

    @Test
    void editsTalentCentricSynthesisSettings() {
        TalentDraft draft = TalentDraft.create();
        draft.updateSynthesis(
                true,
                java.util.List.of(new ResourceLocation("example", "base")),
                java.util.Map.of(new ResourceLocation("example", "kills"), 10L),
                java.util.Map.of(new ResourceLocation("minecraft", "diamond"), 3)
        );
        TalentDraft.SynthesisDraft synthesis = draft.synthesis();
        assertTrue(synthesis.enabled());
        assertEquals(10L, synthesis.consumeCounters().get(new ResourceLocation("example", "kills")));
        JsonObject output = JsonParser.parseString(draft.toJson("Advanced")).getAsJsonObject();
        JsonObject serialized = output.getAsJsonObject("synthesis");
        assertEquals("example:base", serialized.getAsJsonArray("consumeTalents").get(0).getAsString());
        assertEquals(3, serialized.getAsJsonObject("consumeItems").get("minecraft:diamond").getAsInt());
    }

    @Test
    void referencePickerAppendHelpersPreserveExistingValuesAndAvoidDuplicates() {
        ResourceLocation talent = new ResourceLocation("example", "starter");
        ResourceLocation counter = new ResourceLocation("example", "kills");
        ResourceLocation category = new ResourceLocation("example", "combat");

        assertEquals("example:starter", TalentSynthesisScreen.appendUnique("", talent, false));
        assertEquals("example:kills=1", TalentSynthesisScreen.appendUnique("", counter, true));
        assertEquals("example:kills=7", TalentSynthesisScreen.appendUnique("example:kills=7", counter, true));
        assertEquals("example:magic, example:combat",
                TalentMetadataScreen.appendUniqueId("example:magic", category));
        assertEquals("example:combat", TalentMetadataScreen.appendUniqueId("example:combat", category));
    }

    @Test
    void editsSimpleEffectTriggersWhilePreservingAdvancedJsonTriggers() {
        TalentDraft draft = TalentDraft.parse("""
                {
                  "schema":2,
                  "text":{"name":"Reactive"},
                  "rarity":"talent_editor:common",
                  "triggers":[
                    {
                      "id":"slow_undead","event":"damage_dealt","priority":5,
                      "chance":0.5,"cooldownTicks":30,
                      "condition":{"type":"event_entity_tag","tag":"minecraft:undead"},
                      "actions":[{"type":"add_effect","effect":"minecraft:slowness",
                        "durationTicks":60,"amplifier":0,"target":"event_entity"}]
                    },
                    {
                      "id":"advanced","event":"custom:event",
                      "condition":{"type":"not","child":{"type":"always","value":false}},
                      "actions":[{"type":"custom:action","value":7}]
                    }
                  ]
                }
                """);
        assertEquals(2, draft.triggerRows().size());
        assertTrue(draft.triggerRows().get(0).editable());
        assertFalse(draft.triggerRows().get(1).editable());
        assertEquals(TalentDraft.TriggerFilter.ENTITY_TAG, draft.triggerRows().get(0).draft().filter());
        assertEquals(0.5D, draft.triggerRows().get(0).draft().chance());
        assertEquals(30, draft.triggerRows().get(0).draft().cooldownTicks());

        TalentDraft.TriggerDraft changed = new TalentDraft.TriggerDraft(
                "poison_fire", "damage_taken", 30, 0.25D, 100,
                TalentDraft.TriggerAction.REMOVE_EFFECT,
                new ResourceLocation("minecraft", "poison"),
                120, 1, "self", true, false, true,
                TalentDraft.TriggerFilter.DAMAGE_TAG, new ResourceLocation("minecraft", "is_fire"),
                "add", 0.0D, 0.0D, 1.0E9D
        );
        draft.updateEffectTrigger(0, changed);
        int added = draft.addEffectTrigger();
        assertEquals(2, added);
        draft.removeEffectTrigger(added);
        JsonObject output = JsonParser.parseString(draft.toJson("Reactive")).getAsJsonObject();
        JsonObject first = output.getAsJsonArray("triggers").get(0).getAsJsonObject();
        assertEquals("damage_taken", first.get("event").getAsString());
        assertEquals(0.25D, first.get("chance").getAsDouble());
        assertEquals(100, first.get("cooldownTicks").getAsInt());
        assertEquals("talent_editor:remove_effect", first.getAsJsonArray("actions").get(0)
                .getAsJsonObject().get("type").getAsString());
        assertEquals("minecraft:is_fire", first.getAsJsonObject("condition").get("tag").getAsString());
        assertEquals("minecraft:poison", first.getAsJsonArray("actions").get(0)
                .getAsJsonObject().get("effect").getAsString());
        assertEquals("custom:action", output.getAsJsonArray("triggers").get(1).getAsJsonObject()
                .getAsJsonArray("actions").get(0).getAsJsonObject().get("type").getAsString());

        TalentDraft target = TalentDraft.create();
        target.replaceTriggersFrom(draft);
        assertEquals(2, target.triggerRows().size());
    }

    @Test
    void editsEffectImmunityAsATalentLevelActiveEffect() {
        TalentDraft draft = TalentDraft.create();
        int index = draft.addEffectImmunity(new ResourceLocation("minecraft", "poison"));
        assertEquals(1, draft.effectRows().size());
        assertTrue(draft.effectRows().get(0).editableImmunity());
        assertFalse(draft.effectRows().get(0).editableAttribute());

        JsonObject output = JsonParser.parseString(draft.toJson("Immune")).getAsJsonObject();
        JsonObject immunity = output.getAsJsonArray("activeEffects").get(0).getAsJsonObject();
        assertEquals("talent_editor:effect_immunity", immunity.get("type").getAsString());
        assertEquals("minecraft:poison", immunity.get("effect").getAsString());

        draft.removeEditableActiveEffect(index);
        assertTrue(draft.effectRows().isEmpty());
    }

    @Test
    void editsMaintainedSelfEffectAtTalentLevel() {
        TalentDraft draft = TalentDraft.create();
        int index = draft.addMaintainedEffect(new ResourceLocation("minecraft", "speed"));

        assertTrue(draft.effectRows().get(0).editableMaintainedEffect());
        assertEquals(0.0D, draft.effectRows().get(0).amount());
        draft.updateMaintainedEffectAmplifier(index, 2);

        JsonObject action = JsonParser.parseString(draft.toJson("Swift")).getAsJsonObject()
                .getAsJsonArray("activeEffects").get(0).getAsJsonObject();
        assertEquals("talent_editor:add_effect", action.get("type").getAsString());
        assertEquals("self", action.get("target").getAsString());
        assertEquals(60, action.get("durationTicks").getAsInt());
        assertEquals(2, action.get("amplifier").getAsInt());
        assertFalse(action.get("showParticles").getAsBoolean());

        draft.removeEditableActiveEffect(index);
        assertTrue(draft.effectRows().isEmpty());
    }

    @Test
    void editsFreeDrawRefreshesAtTalentLevel() {
        TalentDraft draft = TalentDraft.create();
        int index = draft.addDrawRefreshBonus();

        assertTrue(draft.effectRows().get(0).editableDrawRefreshBonus());
        assertEquals(1.0D, draft.effectRows().get(0).amount());
        draft.updateDrawRefreshBonus(index, 4);

        JsonObject action = JsonParser.parseString(draft.toJson("Second Chance")).getAsJsonObject()
                .getAsJsonArray("activeEffects").get(0).getAsJsonObject();
        assertEquals("talent_editor:draw_refresh_bonus", action.get("type").getAsString());
        assertEquals(4, action.get("amount").getAsInt());

        draft.removeEditableActiveEffect(index);
        assertTrue(draft.effectRows().isEmpty());
    }

    @Test
    void editsDynamicAttributesAndCreativeFlightAtTalentLevel() {
        TalentDraft draft = TalentDraft.create();
        int dynamic = draft.addDynamicAttribute(new ResourceLocation("minecraft", "generic.attack_damage"));
        TalentDraft.DynamicAttributeDraft initial = draft.dynamicAttribute(dynamic);
        draft.updateDynamicAttribute(dynamic, new TalentDraft.DynamicAttributeDraft(
                initial.attribute(), "counter", java.util.Optional.of(new ResourceLocation("example", "kills")),
                1.0D, 0.25D, 0.0D, 20.0D, "multiply_base", "kills_damage", false
        ));
        int flight = draft.addCreativeFlight();

        assertTrue(draft.effectRows().get(0).editableDynamicAttribute());
        assertTrue(draft.effectRows().get(1).editableCreativeFlight());
        assertTrue(draft.hasCreativeFlight());
        JsonObject output = JsonParser.parseString(draft.toJson("Scaling Power")).getAsJsonObject();
        JsonObject action = output.getAsJsonArray("activeEffects").get(0).getAsJsonObject();
        assertEquals("talent_editor:dynamic_attribute_modifier", action.get("type").getAsString());
        assertEquals("example:kills", action.get("counter").getAsString());
        assertEquals(0.25D, action.get("scale").getAsDouble());
        assertFalse(action.get("showInDescription").getAsBoolean());

        draft.removeEditableActiveEffect(flight);
        assertFalse(draft.hasCreativeFlight());
    }

    @Test
    void editsPreMitigationDamageModificationTrigger() {
        TalentDraft draft = TalentDraft.parse("""
                {
                  "schema":2,"text":{"name":"Power"},"rarity":"talent_editor:common",
                  "triggers":[{
                    "id":"heavy_hit","event":"damage_dealt_pre","priority":40,
                    "condition":{"type":"damage_type_tag","tag":"minecraft:is_projectile"},
                    "actions":[{"type":"modify_damage","operation":"multiply","amount":1.5,
                      "minimum":0,"maximum":100}]
                  }]
                }
                """);

        TalentDraft.TriggerDraft value = draft.triggerRows().get(0).draft();
        assertTrue(draft.triggerRows().get(0).editable());
        assertEquals(TalentDraft.TriggerAction.MODIFY_DAMAGE, value.action());
        assertEquals("multiply", value.damageOperation());
        assertEquals(1.5D, value.damageAmount());
        assertEquals(100.0D, value.damageMaximum());

        draft.updateEffectTrigger(0, value);
        JsonObject output = JsonParser.parseString(draft.toJson("Power")).getAsJsonObject();
        JsonObject action = output.getAsJsonArray("triggers").get(0).getAsJsonObject()
                .getAsJsonArray("actions").get(0).getAsJsonObject();
        assertEquals("talent_editor:modify_damage", action.get("type").getAsString());
        assertEquals("multiply", action.get("operation").getAsString());
        assertFalse(action.has("effect"));
        assertFalse(action.has("target"));
    }

    @Test
    void roundTripsExtendedEntityActionsAndNearbyRadius() {
        TalentDraft draft = TalentDraft.parse("""
                {
                  "schema":2,"text":{"name":"Aura"},"rarity":"talent_editor:common",
                  "triggers":[{
                    "id":"aura","event":"damage_taken",
                    "actions":[
                      {"type":"modify_entity_attribute","attribute":"minecraft:generic.attack_damage",
                       "operation":"add","amount":2,"minimum":0,"maximum":50,"target":"self"},
                      {"type":"damage_entity","amount":4,"target":"damage_source"},
                      {"type":"heal_entity","amount":1.5,"target":"nearby","radius":12}
                    ]
                  }]
                }
                """);

        var actions = draft.triggerRows().get(0).draft().actions();
        assertEquals(TalentDraft.TriggerAction.MODIFY_ENTITY_ATTRIBUTE, actions.get(0).action());
        assertEquals(new ResourceLocation("minecraft", "generic.attack_damage"), actions.get(0).effect());
        assertEquals(TalentDraft.TriggerAction.DAMAGE_ENTITY, actions.get(1).action());
        assertEquals("damage_source", actions.get(1).target());
        assertEquals(TalentDraft.TriggerAction.HEAL_ENTITY, actions.get(2).action());
        assertEquals(12.0D, actions.get(2).entityRadius());

        draft.updateEffectTrigger(0, draft.triggerRows().get(0).draft());
        JsonObject serialized = JsonParser.parseString(draft.toJson("Aura")).getAsJsonObject();
        var outputActions = serialized.getAsJsonArray("triggers").get(0).getAsJsonObject()
                .getAsJsonArray("actions");
        assertEquals("talent_editor:modify_entity_attribute",
                outputActions.get(0).getAsJsonObject().get("type").getAsString());
        assertEquals("minecraft:generic.attack_damage",
                outputActions.get(0).getAsJsonObject().get("attribute").getAsString());
        assertEquals("damage_source",
                outputActions.get(1).getAsJsonObject().get("target").getAsString());
        assertEquals(12.0D, outputActions.get(2).getAsJsonObject().get("radius").getAsDouble());
    }

    @Test
    void editsAndReordersMultipleTriggerActionsWithAttributeChance() {
        TalentDraft draft = TalentDraft.parse("""
                {
                  "schema":2,"text":{"name":"Guard"},"rarity":"talent_editor:common",
                  "triggers":[{
                    "id":"lucky_guard","event":"damage_taken_pre","chance":0.1,"activationLimit":1,
                    "chanceAttribute":"minecraft:generic.luck","chancePerPoint":0.02,
                    "actions":[
                      {"type":"add_effect","effect":"minecraft:resistance",
                       "durationTicks":40,"target":"self"},
                      {"type":"modify_damage","operation":"multiply","amount":0.5},
                      {"type":"cancel_damage"}
                    ]
                  }]
                }
                """);

        TalentDraft.TriggerDraft value = draft.triggerRows().get(0).draft();
        assertTrue(draft.triggerRows().get(0).editable());
        assertEquals(new ResourceLocation("minecraft", "generic.luck"), value.chanceAttribute());
        assertEquals(0.02D, value.chancePerPoint());
        assertEquals(1, value.activationLimit());
        assertEquals(3, value.actions().size());
        assertEquals(TalentDraft.TriggerAction.CANCEL_DAMAGE, value.actions().get(2).action());

        java.util.ArrayList<TalentDraft.TriggerActionDraft> reversed = new java.util.ArrayList<>(value.actions());
        java.util.Collections.reverse(reversed);
        draft.updateEffectTrigger(0, new TalentDraft.TriggerDraft(
                value.id(), value.event(), value.priority(), value.chance(), value.cooldownTicks(),
                value.activationLimit(),
                value.chanceAttribute(), value.chancePerPoint(), value.filter(), value.filterValue(), reversed
        ));
        JsonObject output = JsonParser.parseString(draft.toJson("Guard")).getAsJsonObject();
        JsonObject trigger = output.getAsJsonArray("triggers").get(0).getAsJsonObject();
        assertEquals("minecraft:generic.luck", trigger.get("chanceAttribute").getAsString());
        assertEquals(1, trigger.get("activationLimit").getAsInt());
        assertEquals("talent_editor:cancel_damage", trigger.getAsJsonArray("actions")
                .get(0).getAsJsonObject().get("type").getAsString());
        assertEquals("talent_editor:add_effect", trigger.getAsJsonArray("actions")
                .get(2).getAsJsonObject().get("type").getAsString());
    }

    @Test
    void editsFailurePolicyAndFallbackActionBranch() {
        TalentDraft draft = TalentDraft.parse("""
                {
                  "schema":2,"text":{"name":"Fallback"},"rarity":"talent_editor:common",
                  "triggers":[{
                    "id":"cleanse_or_guard","event":"damage_taken_pre",
                    "actionFailurePolicy":"stop","activationLimit":1,
                    "actions":[{"type":"remove_effect","effect":"minecraft:poison","target":"self"}],
                    "failureActions":[{"type":"add_effect","effect":"minecraft:resistance",
                      "durationTicks":40,"amplifier":0,"target":"self"}]
                  }]
                }
                """);

        TalentDraft.TriggerDraft trigger = draft.triggerRows().get(0).draft();
        assertTrue(draft.triggerRows().get(0).editable());
        assertEquals("stop", trigger.actionFailurePolicy());
        assertEquals(1, trigger.failureActions().size());
        assertEquals(TalentDraft.TriggerAction.ADD_EFFECT, trigger.failureActions().get(0).action());

        draft.updateEffectTrigger(0, trigger);
        JsonObject serialized = JsonParser.parseString(draft.toJson("Fallback")).getAsJsonObject()
                .getAsJsonArray("triggers").get(0).getAsJsonObject();
        assertEquals("stop", serialized.get("actionFailurePolicy").getAsString());
        assertEquals("talent_editor:add_effect", serialized.getAsJsonArray("failureActions")
                .get(0).getAsJsonObject().get("type").getAsString());
    }

    @Test
    void editsQueuedCategorySearchAsATriggerAction() {
        TalentDraft draft = TalentDraft.parse("""
                {
                  "schema":2,"text":{"name":"Hope"},"rarity":"talent_editor:common",
                  "triggers":[{
                    "id":"find_light","event":"player_second","activationLimit":1,
                    "actions":[{
                      "type":"queue_draw_constraint","categories":["example:light"],
                      "pools":["example:main"],"guaranteed":2
                    }]
                  }]
                }
                """);

        TalentDraft.TriggerActionDraft action = draft.triggerRows().get(0).draft().actions().get(0);
        assertTrue(draft.triggerRows().get(0).editable());
        assertEquals(TalentDraft.TriggerAction.QUEUE_DRAW_CONSTRAINT, action.action());
        assertEquals(TalentDraft.DrawSearchTarget.CATEGORY, action.searchTarget());
        assertEquals(new ResourceLocation("example", "light"), action.searchValue());
        assertEquals(new ResourceLocation("example", "main"), action.searchPool());
        assertEquals(2, action.searchGuaranteed());

        JsonObject output = JsonParser.parseString(draft.toJson("Hope")).getAsJsonObject();
        JsonObject serialized = output.getAsJsonArray("triggers").get(0).getAsJsonObject()
                .getAsJsonArray("actions").get(0).getAsJsonObject();
        assertEquals("example:light", serialized.getAsJsonArray("categories").get(0).getAsString());
        assertEquals("example:main", serialized.getAsJsonArray("pools").get(0).getAsString());
        assertEquals(2, serialized.get("guaranteed").getAsInt());
        assertFalse(serialized.has("effect"));
    }

    @Test
    void editsQueuedSearchSuccessMilestoneAndQueuePolicy() {
        TalentDraft draft = TalentDraft.parse("""
                {
                  "schema":2,"text":{"name":"Hope"},"rarity":"talent_editor:common",
                  "triggers":[{
                    "id":"find_light","event":"player_move_distance","chance":0.01,
                    "chanceCategory":"example:light","chancePerPoint":0.005,"distanceInterval":200,
                    "actions":[{
                      "type":"queue_draw_constraint","categories":["example:light"],
                      "guaranteed":1,"queueMode":"replace",
                      "successCounter":"example:hope_successes","successIncrement":1,
                      "milestoneEvery":9,"milestone":{
                        "tags":["example:ur_material"],"guaranteed":1
                      }
                    }]
                  }]
                }
                """);

        TalentDraft.TriggerActionDraft action = draft.triggerRows().get(0).draft().actions().get(0);
        assertTrue(draft.triggerRows().get(0).editable());
        assertEquals(new ResourceLocation("example", "light"),
                draft.triggerRows().get(0).draft().chanceCategory());
        assertEquals(200.0D, draft.triggerRows().get(0).draft().distanceInterval());
        assertEquals("replace", action.searchQueueMode());
        assertEquals(new ResourceLocation("example", "hope_successes"), action.searchSuccessCounter());
        assertEquals(1L, action.searchSuccessIncrement());
        assertEquals(9, action.searchMilestoneEvery());
        assertEquals(TalentDraft.DrawSearchTarget.TAG, action.searchMilestoneTarget());
        assertEquals(new ResourceLocation("example", "ur_material"), action.searchMilestoneValue());

        JsonObject output = JsonParser.parseString(draft.toJson("Hope")).getAsJsonObject();
        JsonObject serialized = output.getAsJsonArray("triggers").get(0).getAsJsonObject()
                .getAsJsonArray("actions").get(0).getAsJsonObject();
        assertEquals("replace", serialized.get("queueMode").getAsString());
        assertEquals("example:hope_successes", serialized.get("successCounter").getAsString());
        assertEquals(9, serialized.get("milestoneEvery").getAsInt());
        assertEquals("example:ur_material", serialized.getAsJsonObject("milestone")
                .getAsJsonArray("tags").get(0).getAsString());
        JsonObject trigger = output.getAsJsonArray("triggers").get(0).getAsJsonObject();
        assertEquals("example:light", trigger.get("chanceCategory").getAsString());
        assertEquals(200.0D, trigger.get("distanceInterval").getAsDouble());
    }

    @Test
    void editsItemAndBlockFilteredGeneralEvents() {
        TalentDraft draft = TalentDraft.parse("""
                {
                  "schema":2,"text":{"name":"Explorer"},"rarity":"talent_editor:common",
                  "triggers":[
                    {"id":"eat_apple","event":"item_use_finish",
                     "condition":{"type":"event_item","item":"minecraft:apple"},
                     "actions":[{"type":"add_effect","effect":"minecraft:speed",
                       "durationTicks":20,"target":"self"}]},
                    {"id":"touch_logs","event":"interact_block",
                     "condition":{"type":"event_block_tag","tag":"minecraft:logs"},
                     "actions":[{"type":"remove_effect","effect":"minecraft:poison","target":"self"}]},
                    {"id":"heartbeat","event":"player_second",
                     "actions":[{"type":"add_effect","effect":"minecraft:regeneration",
                       "durationTicks":25,"target":"self"}]}
                  ]
                }
                """);

        assertEquals(3, draft.triggerRows().size());
        assertTrue(draft.triggerRows().stream().allMatch(TalentDraft.TriggerRow::editable));
        assertEquals(TalentDraft.TriggerFilter.ITEM, draft.triggerRows().get(0).draft().filter());
        assertEquals(TalentDraft.TriggerFilter.BLOCK_TAG, draft.triggerRows().get(1).draft().filter());
        assertEquals("player_second", draft.triggerRows().get(2).draft().event());

        JsonObject output = JsonParser.parseString(draft.toJson("Explorer")).getAsJsonObject();
        assertEquals("minecraft:apple", output.getAsJsonArray("triggers").get(0).getAsJsonObject()
                .getAsJsonObject("condition").get("item").getAsString());
        assertEquals("minecraft:logs", output.getAsJsonArray("triggers").get(1).getAsJsonObject()
                .getAsJsonObject("condition").get("tag").getAsString());
    }

    @Test
    void editsSimpleAcquisitionPresentationAndPreservesAdvancedPhase() {
        TalentDraft draft = TalentDraft.parse("""
                {
                  "schema":2,"text":{"name":"Ceremony"},"rarity":"talent_editor:common",
                  "onAcquire":[
                    {"type":"play_sound","sound":"minecraft:entity.player.levelup",
                     "source":"players","volume":1.25,"pitch":0.9},
                    {"type":"show_message","mode":"title",
                     "text":{"literal":"Awakened","color":"#55FFFF","bold":true},
                     "subtitle":{"literal":"A new path","italic":true},
                     "fadeInTicks":5,"stayTicks":50,"fadeOutTicks":10}
                  ],
                  "onSynthesize":[{"type":"future:custom_presentation","value":7}]
                }
                """);

        TalentDraft.PresentationDraft acquire = draft.presentation(TalentDraft.PresentationPhase.ACQUIRE);
        assertTrue(acquire.editable());
        assertTrue(acquire.soundEnabled());
        assertTrue(acquire.messageEnabled());
        assertTrue(acquire.subtitleEnabled());
        assertEquals("Awakened", acquire.text().literal());
        assertTrue(draft.presentation(TalentDraft.PresentationPhase.SYNTHESIZE).editable());

        TalentDraft.PresentationDraft changed = new TalentDraft.PresentationDraft(
                true, true, new ResourceLocation("minecraft", "ui.toast.challenge_complete"),
                "master", 2.0D, 1.1D, true, "action_bar",
                new LocalizedTextDraft("Complete", "", java.util.Optional.of(0xFFFFAA00), false, true),
                false, LocalizedTextDraft.literal(""), 10, 70, 20
        );
        draft.updatePresentation(TalentDraft.PresentationPhase.ACQUIRE, changed);
        draft.updatePresentation(TalentDraft.PresentationPhase.SYNTHESIZE, changed);
        JsonObject output = JsonParser.parseString(draft.toJson("Ceremony")).getAsJsonObject();
        assertEquals("minecraft:ui.toast.challenge_complete", output.getAsJsonArray("onAcquire")
                .get(0).getAsJsonObject().get("sound").getAsString());
        assertEquals("action_bar", output.getAsJsonArray("onAcquire")
                .get(1).getAsJsonObject().get("mode").getAsString());
        assertEquals("future:custom_presentation", output.getAsJsonArray("onSynthesize")
                .get(0).getAsJsonObject().get("type").getAsString());
        assertEquals("minecraft:ui.toast.challenge_complete", output.getAsJsonArray("onSynthesize")
                .get(1).getAsJsonObject().get("sound").getAsString());
    }

    @Test
    void presentationTimelineMovesCuesWithoutChangingBehaviorActions() {
        TalentDraft draft = TalentDraft.parse("""
                {"schema":2,"text":{"name":"Ceremony"},"rarity":"talent_editor:common",
                 "onAcquire":[
                   {"type":"show_message","mode":"chat","text":{"literal":"First"},"atTick":20},
                   {"type":"give_draw_tokens","amount":1},
                   {"type":"spawn_particles","particle":"minecraft:end_rod","atTick":40},
                   {"type":"play_sound","sound":"minecraft:entity.player.levelup","atTick":60}
                 ]}
                """);
        var cues = draft.presentationCues(TalentDraft.PresentationPhase.ACQUIRE);
        assertEquals(3, cues.size());
        var presentation = draft.editorOverview().section(TalentDraft.EditorSection.PRESENTATION);
        assertEquals(3, presentation.count());
        assertFalse(presentation.advancedOnly());
        draft.movePresentationCue(TalentDraft.PresentationPhase.ACQUIRE, cues.get(2).rawIndex(), -1);
        JsonObject output = JsonParser.parseString(draft.toJson("Ceremony")).getAsJsonObject();
        assertEquals("give_draw_tokens", output.getAsJsonArray("onAcquire").get(1)
                .getAsJsonObject().get("type").getAsString());
        assertEquals("play_sound", output.getAsJsonArray("onAcquire").get(2)
                .getAsJsonObject().get("type").getAsString());
        assertEquals("spawn_particles", output.getAsJsonArray("onAcquire").get(3)
                .getAsJsonObject().get("type").getAsString());
    }
}
