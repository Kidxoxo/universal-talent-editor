package com.kidxoxo.talenteditor.client;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ConfiguredItemDraftTest {
    @Test
    void editsEventRulesAndEquipmentScopeWithoutLosingActions() {
        ConfiguredItemDraft draft = ConfiguredItemDraft.parse("""
                {"schema":2,"name":"Sword","triggers":[
                  {"id":"damage","event":"damage_dealt","futureFlag":7,"actions":[
                    {"type":"modify_damage_expression","operation":"multiply","expression":"3"}
                  ]}
                ]}
                """);
        JsonObject trigger = draft.triggers().get(0);
        trigger.addProperty("chance", 0.75);
        draft.replaceTriggers(List.of(trigger));
        JsonObject output = JsonParser.parseString(draft.toJson(
                "Sword", "", true, 42, false, false, true, List.of("charm"), List.of()
        )).getAsJsonObject();
        assertFalse(output.get("curiosEquippable").getAsBoolean());
        assertTrue(output.get("mainhandOnly").getAsBoolean());
        assertEquals("charm", output.getAsJsonArray("curiosSlots").get(0).getAsString());
        JsonObject restored = output.getAsJsonArray("triggers").get(0).getAsJsonObject();
        assertEquals(7, restored.get("futureFlag").getAsInt());
        assertEquals(0.75, restored.get("chance").getAsDouble());
        assertEquals("3", restored.getAsJsonArray("actions").get(0).getAsJsonObject()
                .get("expression").getAsString());
    }

    @Test
    void editsVisualFieldsWhilePreservingLocalizedStylesAndUnknownData() {
        ConfiguredItemDraft draft = ConfiguredItemDraft.parse("""
                {
                  "schema": 2,
                  "enabled": false,
                  "name": {"literal":"Old Shard","color":"#A855F7","bold":true},
                  "description": [
                    {"literal":"Line one","italic":true},
                    "Line two"
                  ],
                  "customModelData": 1001,
                  "glint": true,
                  "loot": [{
                    "table":"minecraft:chests/simple_dungeon",
                    "chance":0.25,
                    "minCount":1,
                    "maxCount":2,
                    "futureCondition":{"difficulty":"hard"}
                  }],
                  "requiredMods": ["minecraft"],
                  "futureUse": {"consume": false}
                }
                """);

        assertFalse(draft.enabled());
        assertTrue(draft.glint());
        assertEquals("Line one\\nLine two", draft.descriptionEditorText());
        JsonObject output = JsonParser.parseString(draft.toJson(
                "New Shard", "Line one\\nChanged\\\\path", true, 2002, false,
                List.of("minecraft", "forge")
        )).getAsJsonObject();

        assertEquals("New Shard", output.getAsJsonObject("name").get("literal").getAsString());
        assertTrue(output.getAsJsonObject("name").get("bold").getAsBoolean());
        assertTrue(output.getAsJsonArray("description").get(0).getAsJsonObject().get("italic").getAsBoolean());
        assertEquals("Changed\\path", output.getAsJsonArray("description").get(1).getAsString());
        assertEquals(2002, output.get("customModelData").getAsInt());
        assertFalse(output.get("glint").getAsBoolean());
        assertEquals("forge", output.getAsJsonArray("requiredMods").get(1).getAsString());
        assertFalse(output.getAsJsonObject("futureUse").get("consume").getAsBoolean());
        assertEquals("hard", output.getAsJsonArray("loot").get(0).getAsJsonObject()
                .getAsJsonObject("futureCondition").get("difficulty").getAsString());
    }

    @Test
    void descriptionEscapeCodecSupportsLinesAndLiteralBackslashes() {
        assertEquals(List.of("one", "two\\path", "three"),
                ConfiguredItemDraft.decodeDescription("one\\ntwo\\\\path\\nthree"));
        assertEquals(List.of(), ConfiguredItemDraft.decodeDescription("  "));
    }

    @Test
    void editsLootRulesWhilePreservingUnknownRuleFields() {
        ConfiguredItemDraft draft = ConfiguredItemDraft.parse("""
                {"schema":2,"name":"Shard","loot":[{
                  "table":"minecraft:chests/simple_dungeon","chance":0.1,
                  "minCount":1,"maxCount":1,"futureCondition":{"moon":"full"}
                }]}
                """);
        var rule = draft.lootRules().get(0).withValues(
                new net.minecraft.resources.ResourceLocation("minecraft", "chests/stronghold_library"),
                0.5D, 2, 4
        );
        draft.replaceLootRules(List.of(rule));

        JsonObject output = JsonParser.parseString(draft.toJson(
                "Shard", "", true, 0, false, List.of()
        )).getAsJsonObject().getAsJsonArray("loot").get(0).getAsJsonObject();
        assertEquals("minecraft:chests/stronghold_library", output.get("table").getAsString());
        assertEquals(0.5D, output.get("chance").getAsDouble());
        assertEquals(2, output.get("minCount").getAsInt());
        assertEquals(4, output.get("maxCount").getAsInt());
        assertEquals("full", output.getAsJsonObject("futureCondition").get("moon").getAsString());
    }

    @Test
    void editsLootTableMatchModeAndExpressionWithoutLosingFutureFields() {
        ConfiguredItemDraft draft = ConfiguredItemDraft.parse("""
                {"schema":2,"name":"Pattern Shard","loot":[{
                  "tableMatch":"prefix","table":"minecraft:chests/","chance":0.1,
                  "minCount":1,"maxCount":1,"futureMatch":{"source":"pack"}
                }]}
                """);
        var original = draft.lootRules().get(0);
        assertEquals("prefix", original.tableMatch());
        var updated = original.withValues(
                "regex", "(?:minecraft|example):chests/rare_.*", 0.25D, 2, 3
        );
        draft.replaceLootRules(List.of(updated));

        JsonObject output = JsonParser.parseString(draft.toJson(
                "Pattern Shard", "", true, 0, false, List.of()
        )).getAsJsonObject().getAsJsonArray("loot").get(0).getAsJsonObject();
        assertEquals("regex", output.get("tableMatch").getAsString());
        assertEquals("(?:minecraft|example):chests/rare_.*", output.get("table").getAsString());
        assertEquals("pack", output.getAsJsonObject("futureMatch").get("source").getAsString());
    }

    @Test
    void editsAdvancedLootConditionsAndKeepsFutureFields() {
        ConfiguredItemDraft draft = ConfiguredItemDraft.parse("""
                {"schema":2,"name":"Hunter Relic","loot":[{
                  "table":"minecraft:entities/zombie","chance":0.2,"chancePerLuck":0.01,
                  "minCount":1,"maxCount":1,"dimensions":["minecraft:overworld"],
                  "entityTarget":"this_entity","entityTypes":["minecraft:zombie"],
                  "entityTags":["minecraft:undead"],"futureCondition":{"moon":"full"}
                }]}
                """);
        var original = draft.lootRules().get(0);
        assertEquals(0.01D, original.chancePerLuck());
        assertEquals(List.of(new net.minecraft.resources.ResourceLocation("minecraft", "overworld")),
                original.dimensions());

        var updated = original.withConditions(
                -0.02D,
                List.of(new net.minecraft.resources.ResourceLocation("minecraft", "the_nether")),
                "killer_entity",
                List.of(new net.minecraft.resources.ResourceLocation("minecraft", "player")),
                List.of()
        );
        draft.replaceLootRules(List.of(updated));
        JsonObject serialized = JsonParser.parseString(draft.toJson(
                "Hunter Relic", "", true, 0, false, List.of()
        )).getAsJsonObject().getAsJsonArray("loot").get(0).getAsJsonObject();

        assertEquals(-0.02D, serialized.get("chancePerLuck").getAsDouble());
        assertEquals("minecraft:the_nether", serialized.getAsJsonArray("dimensions").get(0).getAsString());
        assertEquals("killer_entity", serialized.get("entityTarget").getAsString());
        assertEquals("minecraft:player", serialized.getAsJsonArray("entityTypes").get(0).getAsString());
        assertTrue(serialized.getAsJsonArray("entityTags").isEmpty());
        assertEquals("full", serialized.getAsJsonObject("futureCondition").get("moon").getAsString());
    }

    @Test
    void editsAttributeModifiersWhilePreservingUnknownRuleFieldsAndUseActions() {
        ConfiguredItemDraft draft = ConfiguredItemDraft.parse("""
                {"schema":2,"name":"Relic","attributeModifiers":[{
                  "attribute":"minecraft:generic.max_health","amount":2,
                  "operation":"addition","slot":"offhand","modifierKey":"health",
                  "futurePredicate":{"dimension":"minecraft:the_end"}
                }],"use":{"enabled":true,"actions":[
                  {"type":"show_message","mode":"chat","text":"Used"}
                ]}}
                """);
        var modifier = draft.attributeModifiers().get(0).withValues(
                new net.minecraft.resources.ResourceLocation("minecraft", "generic.attack_damage"),
                0.25D, "multiply_total", List.of("mainhand", "offhand", "curios"), "damage"
        );
        draft.replaceAttributeModifiers(List.of(modifier));

        JsonObject output = JsonParser.parseString(draft.toJson(
                "Relic", "", true, 0, false, List.of()
        )).getAsJsonObject();
        JsonObject serialized = output.getAsJsonArray("attributeModifiers").get(0).getAsJsonObject();
        assertEquals("minecraft:generic.attack_damage", serialized.get("attribute").getAsString());
        assertEquals(0.25D, serialized.get("amount").getAsDouble());
        assertEquals("multiply_total", serialized.get("operation").getAsString());
        assertFalse(serialized.has("slot"));
        assertEquals(List.of("mainhand", "offhand", "curios"),
                serialized.getAsJsonArray("slots").asList().stream().map(element -> element.getAsString()).toList());
        assertEquals("damage", serialized.get("modifierKey").getAsString());
        assertEquals("minecraft:the_end", serialized.getAsJsonObject("futurePredicate")
                .get("dimension").getAsString());
        assertEquals("show_message", output.getAsJsonObject("use").getAsJsonArray("actions")
                .get(0).getAsJsonObject().get("type").getAsString());
    }

    @Test
    void parsesAndRebuildsVisualConfiguredItemUseActions() {
        ConfiguredItemDraft draft = ConfiguredItemDraft.parse("""
                {"schema":2,"name":"Relic","use":{
                  "enabled":true,"cooldownTicks":80,"consumeCount":1,"actions":[
                    {"type":"add_effect","effect":"minecraft:speed","durationTicks":60,
                     "amplifier":2,"target":"self","ambient":true,"showParticles":false,"showIcon":true},
                    {"type":"remove_effect","effect":"minecraft:slowness","target":"self"},
                    {"type":"play_sound","sound":"minecraft:entity.player.levelup",
                     "source":"players","volume":0.5,"pitch":1.2},
                    {"type":"show_message","mode":"title",
                     "text":{"literal":"Ready","color":"#55FFFF","bold":true},
                     "subtitle":{"translation":"message.example.ready"},
                     "fadeInTicks":4,"stayTicks":30,"fadeOutTicks":8}
                  ]
                }}
                """);

        ConfiguredItemDraft.UseDraft use = draft.use();
        assertTrue(use.editable());
        assertTrue(use.enabled());
        assertEquals(80, use.cooldownTicks());
        assertEquals(1, use.consumeCount());
        assertEquals(4, use.actions().size());
        assertEquals(ConfiguredItemDraft.UseActionType.ADD_EFFECT, use.actions().get(0).type());
        assertEquals(60, use.actions().get(0).durationTicks());
        assertTrue(use.actions().get(0).ambient());
        assertFalse(use.actions().get(0).showParticles());
        assertEquals("players", use.actions().get(2).soundSource());
        assertEquals("title", use.actions().get(3).messageMode());
        assertEquals("Ready", use.actions().get(3).text().literal());
        assertTrue(use.actions().get(3).subtitleEnabled());

        List<ConfiguredItemDraft.UseActionDraft> reversed = List.of(
                use.actions().get(3), use.actions().get(2), use.actions().get(1), use.actions().get(0)
        );
        draft.replaceUse(new ConfiguredItemDraft.UseDraft(true, true, 100, 0, reversed));
        JsonObject serialized = JsonParser.parseString(draft.toJson(
                "Relic", "", true, 0, false, List.of()
        )).getAsJsonObject().getAsJsonObject("use");
        assertEquals(100, serialized.get("cooldownTicks").getAsInt());
        assertEquals(0, serialized.get("consumeCount").getAsInt());
        assertEquals("show_message", serialized.getAsJsonArray("actions").get(0)
                .getAsJsonObject().get("type").getAsString());
        assertEquals("add_effect", serialized.getAsJsonArray("actions").get(3)
                .getAsJsonObject().get("type").getAsString());
    }

    @Test
    void unknownConfiguredItemUseJsonIsReadOnlyAndPreserved() {
        ConfiguredItemDraft draft = ConfiguredItemDraft.parse("""
                {"schema":2,"name":"Future Relic","use":{
                  "enabled":true,"futurePolicy":"once_per_day","actions":[
                    {"type":"future_mod:teleport","distance":8}
                  ]
                }}
                """);

        assertFalse(draft.use().editable());
        JsonObject use = JsonParser.parseString(draft.toJson(
                "Future Relic", "", true, 0, false, List.of()
        )).getAsJsonObject().getAsJsonObject("use");
        assertEquals("once_per_day", use.get("futurePolicy").getAsString());
        assertEquals("future_mod:teleport", use.getAsJsonArray("actions").get(0)
                .getAsJsonObject().get("type").getAsString());
    }
}
