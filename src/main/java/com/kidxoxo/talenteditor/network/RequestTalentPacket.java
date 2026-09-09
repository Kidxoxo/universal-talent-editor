package com.kidxoxo.talenteditor.network;

import com.kidxoxo.talenteditor.TalentEditorMod;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public record RequestTalentPacket(ResourceLocation id) {
    static void encode(RequestTalentPacket packet, FriendlyByteBuf buffer) {
        buffer.writeResourceLocation(packet.id());
    }

    static RequestTalentPacket decode(FriendlyByteBuf buffer) {
        return new RequestTalentPacket(buffer.readResourceLocation());
    }

    static void handle(RequestTalentPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        ServerPlayer player = context.getSender();
        if (player != null) {
            if (!player.hasPermissions(2)) {
                player.sendSystemMessage(Component.translatable("message.talent_editor.permission_denied"));
            } else {
                TalentEditorMod.DEFINITIONS.findTalent(packet.id()).ifPresentOrElse(
                        definition -> EditorNetwork.sendTalentDefinition(player, packet.id(), definition),
                        () -> player.sendSystemMessage(Component.translatable(
                                "message.talent_editor.talent_not_found", packet.id().toString()
                        ))
                );
            }
        }
        context.setPacketHandled(true);
    }
}
