package com.kidxoxo.talenteditor.network;

import com.kidxoxo.talenteditor.catalog.AttributeDescriptor;
import com.kidxoxo.talenteditor.domain.DefinitionKind;
import com.kidxoxo.talenteditor.domain.DefinitionReference;
import com.kidxoxo.talenteditor.domain.DefinitionTarget;
import io.netty.buffer.Unpooled;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BootstrapPacketTest {
    @Test
    void roundTripsPoolCatalogAlongsideTalentsAndAttributes() {
        ResourceLocation common = new ResourceLocation("talent_editor", "common");
        EditorBootstrap bootstrap = new EditorBootstrap(
                7L,
                "abc123",
                List.of(new EditorBootstrap.RaritySummary(
                        common, "Common", 0xFFA0A0A0, 0, true, true, 1.0D,
                        "builtin/talent_editor", 2
                )),
                List.of(new EditorBootstrap.TalentSummary(
                        new ResourceLocation("example", "vitality"), "Vitality", common, true, true,
                        List.of(new ResourceLocation("example", "combat")), "file/example_pack", 1,
                        Optional.of(new ResourceLocation("example", "textures/talent/vitality.png")),
                        List.of(new ResourceLocation("example", "root")), true, "all", 2,
                        true, List.of(new ResourceLocation("example", "lesser_vitality")), 1
                )),
                List.of(new EditorBootstrap.PoolSummary(
                        new ResourceLocation("example", "main"), "Main", true,
                        4, 2, 30, true, Map.of(common, 2.5D), "file/example_pack", 1
                )),
                List.of(new EditorBootstrap.CounterSummary(
                        new ResourceLocation("example", "kills"), "Kills", "kill", true, true,
                        List.of(new ResourceLocation("example", "combat_events")), "condition_only",
                        "talent_editor_generated", 1
                )),
                List.of(new EditorBootstrap.LayoutSummary(
                        new ResourceLocation("example", "book"), true, true, 12,
                        Optional.of(new ResourceLocation("example", "textures/gui/book.png")),
                        48, 3, 5, 42, 30, 16, 3, 2, false, "talent_editor_generated", 0
                )),
                List.of(new DefinitionReference(
                        new DefinitionTarget(DefinitionKind.TALENT, new ResourceLocation("example", "vitality")),
                        DefinitionKind.COUNTER, new ResourceLocation("example", "kills"),
                        "counter_required_talent", "$.requiredTalents[0]"
                )),
                false,
                List.of(new ResourceLocation("minecraft", "skeletons")),
                List.of(new ResourceLocation("minecraft", "swords")),
                List.of(new ResourceLocation("minecraft", "is_fire")),
                List.of(new AttributeDescriptor(
                        new ResourceLocation("minecraft", "generic.max_health"),
                        "attribute.name.generic.max_health", 20.0D, 1.0D, 1024.0D, true, true
                )),
                List.of(new EditorBootstrap.ConfiguredItemSummary(
                        new ResourceLocation("example", "talent_shard"), "Talent Shard",
                        true, true, 1001, true, "talent_editor_generated", 0
                )),
                List.of(new ResourceLocation("example", "growth"))
        );
        FriendlyByteBuf buffer = new FriendlyByteBuf(Unpooled.buffer());
        try {
            BootstrapPacket.encode(new BootstrapPacket(bootstrap, true), buffer);
            BootstrapPacket decoded = BootstrapPacket.decode(buffer);

            assertTrue(decoded.openScreen());
            assertEquals(bootstrap, decoded.bootstrap());
        } finally {
            buffer.release();
        }
    }
}
