package com.kidxoxo.talenteditor.network;

import com.kidxoxo.talenteditor.TalentEditorMod;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public record RequestTransactionHistoryPacket() {
    static void encode(RequestTransactionHistoryPacket packet, FriendlyByteBuf buffer) {
    }

    static RequestTransactionHistoryPacket decode(FriendlyByteBuf buffer) {
        return new RequestTransactionHistoryPacket();
    }

    static void handle(RequestTransactionHistoryPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        ServerPlayer player = context.getSender();
        if (player != null) {
            if (!player.hasPermissions(2)) {
                player.sendSystemMessage(Component.translatable("message.talent_editor.permission_denied"));
            } else {
                try {
                    MinecraftServer server = player.getServer();
                    if (server == null) {
                        context.setPacketHandled(true);
                        return;
                    }
                    EditorNetwork.sendTransactionHistory(
                            player,
                            TalentEditorMod.EDITOR_PACK.history(TalentEditorMod.EDITOR_PACK.datapacksRoot(
                                    server
                            ))
                    );
                } catch (Exception ex) {
                    TalentEditorMod.LOGGER.error("Failed to read editor transaction history", ex);
                    player.sendSystemMessage(Component.translatable(
                            "message.talent_editor.history_read_failed"
                    ));
                }
            }
        }
        context.setPacketHandled(true);
    }
}
