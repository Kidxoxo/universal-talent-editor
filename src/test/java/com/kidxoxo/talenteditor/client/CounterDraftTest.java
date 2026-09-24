package com.kidxoxo.talenteditor.client;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CounterDraftTest {
    @Test
    void editsCounterWhilePreservingLocalizedNameMetadata() {
        CounterDraft draft = CounterDraft.parse("""
                {
                  "schema": 2,
                  "enabled": true,
                  "name": {"literal":"Kills","translation":"counter.example.kills","color":"#55AAFF"},
                  "event": "kill",
                  "increment": 1,
                  "maximum": 100,
                  "resetOnLogin": true,
                  "resetOnDeath": false,
                  "resetIntervalTicks": 1200,
                  "categories": ["example:combat_events"],
                  "visibility": "condition_only",
                  "entityTypes": ["minecraft:zombie"]
                }
                """);
        draft.updateAmountSettings(new CounterDraft.AmountSettings("event_amount", 1.5D, "ceil", 2L));
        String json = draft.toJson(
                "Undead defeated", false, "damage_dealt", 2L, 200L, 5, 20,
                new CounterDraft.ResetSettings(false, true, 2400),
                new CounterDraft.MetadataSettings(
                        List.of(new ResourceLocation("example", "damage_events")), "hidden",
                        new ResourceLocation("example", "light")
                ),
                List.of("minecraft"), List.of(new ResourceLocation("example", "hunter")),
                new CounterDraft.Filters(
                        List.of(new ResourceLocation("minecraft", "skeleton")), List.of(), List.of(), List.of(),
                        List.of(new ResourceLocation("minecraft", "is_projectile"))
                )
        );
        JsonObject root = JsonParser.parseString(json).getAsJsonObject();

        assertFalse(root.get("enabled").getAsBoolean());
        assertEquals("Undead defeated", root.getAsJsonObject("name").get("literal").getAsString());
        assertEquals("counter.example.kills", root.getAsJsonObject("name").get("translation").getAsString());
        assertEquals("#55AAFF", root.getAsJsonObject("name").get("color").getAsString());
        assertEquals("damage_dealt", root.get("event").getAsString());
        assertEquals("minecraft:skeleton", root.getAsJsonArray("entityTypes").get(0).getAsString());
        assertEquals("minecraft:is_projectile", root.getAsJsonArray("damageTags").get(0).getAsString());
        assertEquals("event_amount", root.get("amountMode").getAsString());
        assertEquals(1.5D, root.get("amountScale").getAsDouble());
        assertEquals("ceil", root.get("rounding").getAsString());
        assertEquals(2L, root.get("minimumIncrement").getAsLong());
        assertFalse(root.get("resetOnLogin").getAsBoolean());
        assertTrue(root.get("resetOnDeath").getAsBoolean());
        assertEquals(2400, root.get("resetIntervalTicks").getAsInt());
        assertEquals("example:damage_events", root.getAsJsonArray("categories").get(0).getAsString());
        assertEquals("hidden", root.get("visibility").getAsString());
        assertEquals("example:light", root.get("displayCategory").getAsString());
        assertEquals(new CounterDraft.ResetSettings(true, false, 1200),
                CounterDraft.parse("""
                        {"resetOnLogin":true,"resetOnDeath":false,"resetIntervalTicks":1200}
                        """).resetSettings());
        assertEquals(new CounterDraft.MetadataSettings(
                        List.of(new ResourceLocation("example", "combat_events")), "condition_only"),
                CounterDraft.parse("""
                        {"categories":["example:combat_events"],"visibility":"condition_only"}
                        """).metadataSettings());
        assertEquals(new CounterDraft.MetadataSettings(
                        List.of(), "full", new ResourceLocation("example", "dark")),
                CounterDraft.parse("""
                        {"displayCategory":"example:dark"}
                        """).metadataSettings());
    }
}
