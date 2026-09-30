package com.kidxoxo.talenteditor.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.UUID;
import java.util.function.Supplier;

/** Current presentation pose, sent to the owner and tracking players while a cue runs. */
public record PresentationTransformPacket(UUID player, float alpha, float scale,
                                          float offsetX, float offsetY, float offsetZ, boolean clear) {
    static void encode(PresentationTransformPacket packet, FriendlyByteBuf buffer) {
        buffer.writeUUID(packet.player);
        buffer.writeFloat(packet.alpha);
        buffer.writeFloat(packet.scale);
        buffer.writeFloat(packet.offsetX);
        buffer.writeFloat(packet.offsetY);
        buffer.writeFloat(packet.offsetZ);
        buffer.writeBoolean(packet.clear);
    }

    static PresentationTransformPacket decode(FriendlyByteBuf buffer) {
        return new PresentationTransformPacket(buffer.readUUID(), buffer.readFloat(), buffer.readFloat(),
                buffer.readFloat(), buffer.readFloat(), buffer.readFloat(), buffer.readBoolean());
    }

    static void handle(PresentationTransformPacket packet, Supplier<NetworkEvent.Context> context) {
        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () ->
                com.kidxoxo.talenteditor.client.ClientVisualState.acceptPresentation(packet));
        context.get().setPacketHandled(true);
    }
}
