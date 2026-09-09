package com.kidxoxo.talenteditor.network;

import com.kidxoxo.talenteditor.TalentEditorMod;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public record RequestPoolPacket(ResourceLocation id) {
    static void encode(RequestPoolPacket packet, FriendlyByteBuf buffer) {
        buffer.writeResourceLocation(packet.id());
    }

    static RequestPoolPacket decode(FriendlyByteBuf buffer) {
        return new RequestPoolPacket(buffer.readResourceLocation());
    }

    static void handle(RequestPoolPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        ServerPlayer player = context.getSender();
        if (player != null) {
            if (!player.hasPermissions(2)) {
                player.sendSystemMessage(Component.translatable("message.talent_editor.permission_denied"));
            } else {
                TalentEditorMod.DEFINITIONS.findPool(packet.id()).ifPresentOrElse(
                        definition -> EditorNetwork.sendPoolDefinition(player, packet.id(), definition),
                        () -> player.sendSystemMessage(Component.translatable(
                                "message.talent_editor.pool_not_found", packet.id().toString()
                        ))
                );
            }
        }
        context.setPacketHandled(true);
    }
}
