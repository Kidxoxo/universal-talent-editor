package com.kidxoxo.talenteditor.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public record OpenTalentBookPacket() {
    static void encode(OpenTalentBookPacket packet, FriendlyByteBuf buffer) {
    }

    static OpenTalentBookPacket decode(FriendlyByteBuf buffer) {
        return new OpenTalentBookPacket();
    }

    static void handle(OpenTalentBookPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        ServerPlayer player = context.getSender();
        if (player != null) {
            EditorNetwork.sendSettings(player);
            EditorNetwork.sendPlayerState(player, true);
        }
        context.setPacketHandled(true);
    }
}
