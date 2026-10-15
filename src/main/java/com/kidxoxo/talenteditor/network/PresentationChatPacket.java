package com.kidxoxo.talenteditor.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;
import java.util.Objects;
import java.util.function.Supplier;

/** One system-chat entry with a longer hold; no repeated messages or global chat changes. */
public record PresentationChatPacket(Component text, int visibleTicks) {
    public PresentationChatPacket {
        Objects.requireNonNull(text);
        if (visibleTicks < 200 || visibleTicks > 12_000) throw new IllegalArgumentException("chat duration");
    }
    static void encode(PresentationChatPacket packet, FriendlyByteBuf buffer) {
        buffer.writeComponent(packet.text());
        buffer.writeVarInt(packet.visibleTicks());
    }
    static PresentationChatPacket decode(FriendlyByteBuf buffer) {
        return new PresentationChatPacket(buffer.readComponent(), buffer.readVarInt());
    }
    static void handle(PresentationChatPacket packet, Supplier<NetworkEvent.Context> context) {
        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () ->
                com.kidxoxo.talenteditor.client.PresentationChatPlayback.accept(packet));
        context.get().setPacketHandled(true);
    }
}
