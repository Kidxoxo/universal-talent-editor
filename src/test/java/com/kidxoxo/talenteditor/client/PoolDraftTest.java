package com.kidxoxo.talenteditor.client;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class PoolDraftTest {
    @Test
    void editsVisualFieldsAndPreservesFutureFieldsAndLocalizedStyle() {
        PoolDraft draft = PoolDraft.parse("""
                {
                  "schema": 2,
                  "enabled": false,
                  "name": {"literal": "Old Pool", "color": "#55AAFF"},
                  "choices": 2,
                  "refreshes": 3,
                  "maxOwned": 20,
                  "iconSize": 24,
                  "excludeOwned": true,
                  "rarityWeights": {"talent_editor:common": 1},
                  "futureLuckFormula": {"attribute": "minecraft:generic.luck"}
                }
                """);

        assertFalse(draft.enabled());
        assertEquals(2, draft.choices());
        assertEquals(24, draft.iconSize());
        LinkedHashMap<ResourceLocation, Double> weights = new LinkedHashMap<>();
        weights.put(new ResourceLocation("talent_editor", "common"), 4.0D);
        JsonObject output = JsonParser.parseString(draft.toJson(
                "Updated Pool", true, 5, 1, 40, 48, weights, draft.advanced()
        )).getAsJsonObject();

        assertEquals("Updated Pool", output.getAsJsonObject("name").get("literal").getAsString());
        assertEquals("#55AAFF", output.getAsJsonObject("name").get("color").getAsString());
        assertEquals(5, output.get("choices").getAsInt());
        assertEquals(48, output.get("iconSize").getAsInt());
        assertEquals(4.0D, output.getAsJsonObject("rarityWeights")
                .get("talent_editor:common").getAsDouble());
        assertEquals("minecraft:generic.luck", output.getAsJsonObject("futureLuckFormula")
                .get("attribute").getAsString());
    }

    @Test
    void roundTripsLuckPityAndDuplicateSettings() {
        PoolDraft draft = PoolDraft.create();
        ResourceLocation luck = new ResourceLocation("minecraft", "generic.luck");
        ResourceLocation rare = new ResourceLocation("example", "rare");
        ResourceLocation shards = new ResourceLocation("example", "shards");
        draft.updateAdvanced(new PoolDraft.Advanced(
                Optional.of(luck), Map.of(rare, 0.05D), Optional.of(rare), 10,
                "add_counter", Optional.of(shards), 3L
        ));
        JsonObject output = JsonParser.parseString(draft.toJson(
                "Lucky", true, 3, 1, 24, Map.of(rare, 5.0D), draft.advanced()
        )).getAsJsonObject();

        assertEquals(luck.toString(), output.get("luckAttribute").getAsString());
        assertEquals(0.05D, output.getAsJsonObject("luckFactors").get(rare.toString()).getAsDouble());
        assertEquals(10, output.get("pityAfter").getAsInt());
        assertEquals("add_counter", output.get("duplicatePolicy").getAsString());
        assertEquals(shards.toString(), output.get("duplicateCounter").getAsString());
        assertFalse(output.get("excludeOwned").getAsBoolean());
    }
}
