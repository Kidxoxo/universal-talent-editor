package com.kidxoxo.talenteditor.network;

import com.kidxoxo.talenteditor.TalentEditorMod;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public record RequestRarityPacket(ResourceLocation id) {
    static void encode(RequestRarityPacket packet, FriendlyByteBuf buffer) {
        buffer.writeResourceLocation(packet.id());
    }

    static RequestRarityPacket decode(FriendlyByteBuf buffer) {
        return new RequestRarityPacket(buffer.readResourceLocation());
    }

    static void handle(RequestRarityPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        ServerPlayer player = context.getSender();
        if (player != null) {
            if (!player.hasPermissions(2)) {
                player.sendSystemMessage(Component.translatable("message.talent_editor.permission_denied"));
            } else {
                TalentEditorMod.DEFINITIONS.findRarity(packet.id()).ifPresentOrElse(
                        definition -> EditorNetwork.sendRarityDefinition(player, packet.id(), definition),
                        () -> player.sendSystemMessage(Component.translatable(
                                "message.talent_editor.rarity_not_found", packet.id().toString()
                        ))
                );
            }
        }
        context.setPacketHandled(true);
    }
}
