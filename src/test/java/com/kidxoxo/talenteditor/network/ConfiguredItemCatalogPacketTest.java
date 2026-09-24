package com.kidxoxo.talenteditor.network;

import io.netty.buffer.Unpooled;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ConfiguredItemCatalogPacketTest {
    @Test
    void preservesConfiguredItemNamesAndLore() {
        ConfiguredItemCatalogPacket packet = new ConfiguredItemCatalogPacket(7, List.of(
                new ConfiguredItemCatalogPacket.Entry(
                        new ResourceLocation("example", "mask"), "石鬼面",
                        List.of("佩戴后获得特殊力量。", "可装备在饰品栏。"), 1003, true
                )
        ));
        FriendlyByteBuf buffer = new FriendlyByteBuf(Unpooled.buffer());
        ConfiguredItemCatalogPacket.encode(packet, buffer);
        assertEquals(packet, ConfiguredItemCatalogPacket.decode(buffer));
    }
}
