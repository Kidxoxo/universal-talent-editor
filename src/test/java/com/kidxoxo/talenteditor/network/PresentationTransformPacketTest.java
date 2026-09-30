package com.kidxoxo.talenteditor.network;

import io.netty.buffer.Unpooled;
import net.minecraft.network.FriendlyByteBuf;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PresentationTransformPacketTest {
    @Test void roundTripsVisualFrameAndClearSignal() {
        UUID player = UUID.randomUUID();
        for (PresentationTransformPacket sent : new PresentationTransformPacket[] {
                new PresentationTransformPacket(player, 0.35f, 1.5f, 0, 0.8f, 0, false),
                new PresentationTransformPacket(player, 1, 1, 0, 0, 0, true)
        }) {
            FriendlyByteBuf buffer = new FriendlyByteBuf(Unpooled.buffer());
            try {
                PresentationTransformPacket.encode(sent, buffer);
                assertEquals(sent, PresentationTransformPacket.decode(buffer));
            } finally { buffer.release(); }
        }
    }
}
