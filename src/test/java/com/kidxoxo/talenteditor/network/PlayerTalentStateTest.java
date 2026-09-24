package com.kidxoxo.talenteditor.network;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.kidxoxo.talenteditor.compile.CompilationEnvironment;
import com.kidxoxo.talenteditor.compile.RawDefinitionSet;
import com.kidxoxo.talenteditor.compile.TalentCompiler;
import com.kidxoxo.talenteditor.runtime.RuntimeSnapshot;
import com.kidxoxo.talenteditor.domain.TalentBookLayoutDefinition;
import io.netty.buffer.Unpooled;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PlayerTalentStateTest {
    private static final ResourceLocation COMMON = id("talent_editor:common");
    private static final ResourceLocation TALENT = id("example:multi_attribute");
    private static final ResourceLocation HEALTH = id("minecraft:generic.max_health");
    private static final ResourceLocation ARMOR = id("minecraft:generic.armor");

    @Test
    void createsServerAuthoritativeBookViewWithAllVisibleAttributeEffects() {
        RuntimeSnapshot snapshot = snapshot();
        ResourceLocation removed = id("example:removed_definition");
        PlayerTalentState state = PlayerTalentStateFactory.build(snapshot, List.of(TALENT, removed));

        assertEquals(7, state.definitionRevision());
        assertEquals(2, state.talents().size());
        PlayerTalentState.TalentView talent = state.talents().stream()
                .filter(view -> view.id().equals(TALENT))
                .findFirst().orElseThrow();
        assertTrue(talent.defined());
        assertTrue(talent.available());
        assertEquals("Dual Training", talent.name().fallbackText());
        assertEquals(0xFF55AAFF, talent.name().color());
        assertTrue(talent.name().bold());
        assertEquals(id("example:textures/talent/dual_training.png"), talent.icon().orElseThrow());
        assertEquals(3, talent.attributeEffects().size());
        assertEquals(HEALTH, talent.attributeEffects().get(0).attribute());
        assertFalse(talent.attributeEffects().get(1).showInDescription());
        assertEquals("clamp(0 + hunger_percent × 2, 0, 2)", talent.attributeEffects().get(2).formula());
        assertFalse(state.talents().stream()
                .filter(view -> view.id().equals(removed))
                .findFirst().orElseThrow().defined());
    }

    @Test
    void playerTalentStatePacketRoundTripsWithinProtocolBounds() {
        PlayerTalentState original = PlayerTalentStateFactory.build(snapshot(), List.of(TALENT));
        PlayerTalentStatePacket packet = new PlayerTalentStatePacket(
                original,
                new TalentBookLayoutDefinition(
                        id("example:book"), 2, true, 10,
                        java.util.Optional.of(id("example:textures/gui/talent_book.png")),
                        55, 3, 5, 40, 32, 20, false,
                        List.of(
                                new TalentBookLayoutDefinition.SlotDefinition(
                                        "featured", 4, 8, 72, 38, 22
                                ),
                                new TalentBookLayoutDefinition.SlotDefinition(
                                        "secondary", 82, 12, 54, 30, 16
                                )
                        ),
                        List.of(
                                new TalentBookLayoutDefinition.PageDefinition(
                                        "opening",
                                        java.util.Optional.of(id("example:textures/gui/opening.png")),
                                        List.of(new TalentBookLayoutDefinition.SlotDefinition(
                                                "hero", 8, 6, 88, 42, 24, java.util.Optional.of(TALENT)
                                        ))
                                )
                        )
                ),
                true
        );
        FriendlyByteBuf buffer = new FriendlyByteBuf(Unpooled.buffer());
        try {
            PlayerTalentStatePacket.encode(packet, buffer);
            PlayerTalentStatePacket decoded = PlayerTalentStatePacket.decode(buffer);
            assertEquals(packet, decoded);
            assertEquals(0, buffer.readableBytes());
        } finally {
            buffer.release();
        }
    }

    @Test
    void rejectsRootAndDuplicateFixedTalentBindingsFromNetwork() {
        var fixed = new TalentBookLayoutDefinition.SlotDefinition(
                "hero", 0, 0, 58, 30, 16, java.util.Optional.of(TALENT));
        var firstPage = new TalentBookLayoutDefinition.PageDefinition(
                "first", java.util.Optional.empty(), List.of(fixed));
        var secondPage = new TalentBookLayoutDefinition.PageDefinition(
                "second", java.util.Optional.empty(), List.of(fixed));
        List<TalentBookLayoutDefinition> invalidLayouts = List.of(
                new TalentBookLayoutDefinition(id("example:root_binding"), 2, true, 0,
                        java.util.Optional.empty(), 50, 3, 5, 58, 30, 16, true,
                        List.of(fixed), List.of()),
                new TalentBookLayoutDefinition(id("example:duplicate_binding"), 2, true, 0,
                        java.util.Optional.empty(), 50, 3, 5, 58, 30, 16, true,
                        List.of(), List.of(firstPage, secondPage))
        );
        PlayerTalentState state = PlayerTalentStateFactory.build(snapshot(), List.of(TALENT));
        for (var layout : invalidLayouts) {
            FriendlyByteBuf buffer = new FriendlyByteBuf(Unpooled.buffer());
            try {
                PlayerTalentStatePacket.encode(new PlayerTalentStatePacket(state, layout, false), buffer);
                assertThrows(IllegalArgumentException.class, () -> PlayerTalentStatePacket.decode(buffer),
                        layout.id().toString());
            } finally {
                buffer.release();
            }
        }
    }

    @Test
    void counterProgressPacketRoundTripsWithinProtocolBounds() {
        CounterProgressPacket packet = new CounterProgressPacket(List.of(
                new CounterProgressPacket.Entry(id("example:kills"), 42L),
                new CounterProgressPacket.Entry(id("example:damage"), 1234L)
        ));
        FriendlyByteBuf buffer = new FriendlyByteBuf(Unpooled.buffer());
        try {
            CounterProgressPacket.encode(packet, buffer);
            assertEquals(packet, CounterProgressPacket.decode(buffer));
        } finally {
            buffer.release();
        }
    }

    @Test
    void hudResourceStatePacketRoundTripsLightweightUpdates() {
        HudResourceStatePacket packet = new HudResourceStatePacket(List.of(
                new PlayerTalentState.HudResourceView(
                        id("example:energy"), 12.5D, 40.0D, 0x55AAFF, -2, false
                )
        ));
        FriendlyByteBuf buffer = new FriendlyByteBuf(Unpooled.buffer());
        try {
            HudResourceStatePacket.encode(packet, buffer);
            assertEquals(packet, HudResourceStatePacket.decode(buffer));
            assertEquals(0, buffer.readableBytes());
        } finally {
            buffer.release();
        }
    }

    @Test
    void synthesisViewsAndResultsRoundTripWithinProtocolBounds() {
        PlayerTalentState.TalentView target = PlayerTalentStateFactory.view(snapshot(), TALENT);
        PlayerTalentState.SynthesisView synthesis = new PlayerTalentState.SynthesisView(
                target, true, false, List.of(
                        new PlayerTalentState.SynthesisCostView(
                                PlayerTalentState.SynthesisCostType.TALENT,
                                id("example:base"), 1L, 1L
                        ),
                        new PlayerTalentState.SynthesisCostView(
                                PlayerTalentState.SynthesisCostType.COUNTER,
                                id("example:kills"), 10L, 7L
                        ),
                        new PlayerTalentState.SynthesisCostView(
                                PlayerTalentState.SynthesisCostType.ITEM,
                                id("minecraft:diamond"), 3L, 3L
                        )
                )
        );
        PlayerTalentStatePacket packet = new PlayerTalentStatePacket(
                new PlayerTalentState(7L, 2, false, List.of(), false, List.of(synthesis)), true
        );
        FriendlyByteBuf stateBuffer = new FriendlyByteBuf(Unpooled.buffer());
        try {
            PlayerTalentStatePacket.encode(packet, stateBuffer);
            assertEquals(packet, PlayerTalentStatePacket.decode(stateBuffer));
        } finally {
            stateBuffer.release();
        }

        SynthesisResultPacket result = new SynthesisResultPacket(
                com.kidxoxo.talenteditor.synthesis.SynthesisResult.Status.MISSING_COUNTER,
                TALENT, java.util.Optional.of(id("example:kills"))
        );
        FriendlyByteBuf resultBuffer = new FriendlyByteBuf(Unpooled.buffer());
        try {
            SynthesisResultPacket.encode(result, resultBuffer);
            assertEquals(result, SynthesisResultPacket.decode(resultBuffer));
        } finally {
            resultBuffer.release();
        }
    }

    @Test
    void drawSessionPacketRoundTripsWithServerAuthoredCards() {
        RuntimeSnapshot snapshot = snapshot();
        PlayerTalentState.TalentView card = PlayerTalentStateFactory.view(snapshot, TALENT);
        DrawSessionPacket packet = new DrawSessionPacket(new DrawSessionView(
                UUID.fromString("661d8b50-a1e6-45ee-9a28-94ae2cb5ec30"),
                snapshot.revision(), new ResourceLocation("talent_editor", "default"),
                "Default", 3, 1, 40, 6.0D, 4, 5, true,
                Map.of(new ResourceLocation("talent_editor", "common"), 1.0D), List.of(card)
        ));
        FriendlyByteBuf buffer = new FriendlyByteBuf(Unpooled.buffer());
        try {
            DrawSessionPacket.encode(packet, buffer);
            assertEquals(packet, DrawSessionPacket.decode(buffer));
        } finally {
            buffer.release();
        }
    }

    private static RuntimeSnapshot snapshot() {
        JsonObject rarity = parse("""
                {"schema":2,"name":"Common","color":"#A0A0A0"}
                """);
        JsonObject talent = parse("""
                {
                  "schema": 2,
                  "text": {
                    "name": {"literal":"Dual Training","color":"#55AAFF","bold":true},
                    "description": [{"literal":"One talent, two attributes.","italic":true}],
                    "showGeneratedEffects": true
                  },
                  "rarity": "talent_editor:common",
                  "icon": "example:textures/talent/dual_training.png",
                  "activeEffects": [
                    {
                      "type": "attribute_modifier",
                      "attribute": "minecraft:generic.max_health",
                      "amount": 4
                    },
                    {
                      "type": "attribute_modifier",
                      "attribute": "minecraft:generic.armor",
                      "amount": 2,
                      "showInDescription": false
                    },
                    {
                      "type": "dynamic_attribute_modifier",
                      "attribute": "minecraft:generic.armor",
                      "source": "hunger_percent",
                      "base": 0,
                      "scale": 2,
                      "minimum": 0,
                      "maximum": 2,
                      "modifierKey": "fed_armor"
                    }
                  ]
                }
                """);
        return TalentCompiler.withBuiltins().compile(
                new RawDefinitionSet(Map.of(COMMON, rarity), Map.of(TALENT, talent), List.of()),
                new CompilationEnvironment(
                        Set.of("minecraft", "forge", "talent_editor"), Set.of(HEALTH, ARMOR)
                )
        ).candidate().orElseThrow().withRevision(7);
    }

    private static JsonObject parse(String value) {
        return JsonParser.parseString(value).getAsJsonObject();
    }

    private static ResourceLocation id(String value) {
        return new ResourceLocation(value);
    }
}
