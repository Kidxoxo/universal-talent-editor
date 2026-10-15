package com.kidxoxo.talenteditor.network;

import io.netty.buffer.Unpooled;
import net.minecraft.ChatFormatting;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class PresentationChatPacketTest {
    @Test void preservesLineBreaksStylesAndPerMessageDuration() {
        var packet = new PresentationChatPacket(Component.literal("First\nSecond\n\nLast").withStyle(ChatFormatting.AQUA), 700);
        var buffer = new FriendlyByteBuf(Unpooled.buffer());
        try {
            PresentationChatPacket.encode(packet, buffer);
            var decoded = PresentationChatPacket.decode(buffer);
            assertEquals(packet, decoded);
            assertEquals("First\nSecond\n\nLast", decoded.text().getString());
        } finally { buffer.release(); }
    }
    @Test void rejectsOutOfRangeWireDurations() {
        for (int ticks : new int[]{-1, 199, 12_001, Integer.MAX_VALUE}) {
            var buffer = new FriendlyByteBuf(Unpooled.buffer());
            try {
                buffer.writeComponent(Component.literal("text")); buffer.writeVarInt(ticks);
                assertThrows(IllegalArgumentException.class, () -> PresentationChatPacket.decode(buffer));
            } finally { buffer.release(); }
        }
    }
}
