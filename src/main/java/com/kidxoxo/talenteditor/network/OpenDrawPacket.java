package com.kidxoxo.talenteditor.network;

import com.kidxoxo.talenteditor.TalentEditorMod;
import com.kidxoxo.talenteditor.draw.DrawResult;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public record OpenDrawPacket(ResourceLocation poolId) {
    static void encode(OpenDrawPacket packet, FriendlyByteBuf buffer) {
        buffer.writeResourceLocation(packet.poolId());
    }

    static OpenDrawPacket decode(FriendlyByteBuf buffer) {
        return new OpenDrawPacket(buffer.readResourceLocation());
    }

    static void handle(OpenDrawPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        ServerPlayer player = context.getSender();
        if (player != null) {
            DrawResult result = TalentEditorMod.DRAWS.open(player, packet.poolId());
            EditorNetwork.sendDrawResult(player, result);
        }
        context.setPacketHandled(true);
    }
}
