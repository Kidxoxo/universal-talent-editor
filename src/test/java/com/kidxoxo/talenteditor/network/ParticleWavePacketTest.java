package com.kidxoxo.talenteditor.network;

import io.netty.buffer.Unpooled;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ParticleWavePacketTest {
    @Test void roundTripsTranslucentSpiralWave() {
        ParticleWavePacket sent = new ParticleWavePacket(new ResourceLocation("minecraft:end_rod"),
                "spiral", 12.5, 73.25, -4, 24, 1.5, 0.04, 0.6f, 32, 123456L,
                0xCC44EE, 30, 2.25f);
        FriendlyByteBuf buffer = new FriendlyByteBuf(Unpooled.buffer());
        try {
            ParticleWavePacket.encode(sent, buffer);
            assertEquals(sent, ParticleWavePacket.decode(buffer));
        } finally {
            buffer.release();
        }
    }
}
