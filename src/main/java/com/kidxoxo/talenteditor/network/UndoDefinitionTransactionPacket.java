package com.kidxoxo.talenteditor.network;

import com.kidxoxo.talenteditor.TalentEditorMod;
import com.kidxoxo.talenteditor.editor.DefinitionUndoResult;
import com.kidxoxo.talenteditor.editor.DefinitionUndoStatus;
import com.kidxoxo.talenteditor.forge.ForgeCompilationEnvironment;
import com.kidxoxo.talenteditor.runtime.TalentAttributeRuntime;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.List;
import java.util.UUID;
import java.util.function.Supplier;

public record UndoDefinitionTransactionPacket(long baseRevision, UUID transactionId) {
    static void encode(UndoDefinitionTransactionPacket packet, FriendlyByteBuf buffer) {
        buffer.writeVarLong(packet.baseRevision());
        buffer.writeUUID(packet.transactionId());
    }

    static UndoDefinitionTransactionPacket decode(FriendlyByteBuf buffer) {
        return new UndoDefinitionTransactionPacket(buffer.readVarLong(), buffer.readUUID());
    }

    static void handle(UndoDefinitionTransactionPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        ServerPlayer player = context.getSender();
        if (player == null) {
            context.setPacketHandled(true);
            return;
        }
        if (!player.hasPermissions(2)) {
            EditorNetwork.sendUndoResult(player, new DefinitionUndoResultPacket(
                    DefinitionUndoStatus.PERMISSION_DENIED, packet.transactionId(),
                    TalentEditorMod.SNAPSHOTS.active().revision()
            ));
            player.sendSystemMessage(Component.translatable("message.talent_editor.permission_denied"));
            context.setPacketHandled(true);
            return;
        }
        MinecraftServer server = player.getServer();
        if (server == null) {
            context.setPacketHandled(true);
            return;
        }
        DefinitionUndoResult result = TalentEditorMod.DEFINITIONS.undoTransaction(
                packet.baseRevision(), packet.transactionId(), ForgeCompilationEnvironment.capture(),
                TalentEditorMod.EDITOR_PACK.datapacksRoot(server)
        );
        if (!result.undone()) {
            EditorNetwork.sendUndoResult(player, DefinitionUndoResultPacket.from(result));
            context.setPacketHandled(true);
            return;
        }

        try {
            List<String> selectedPacks = List.copyOf(server.getPackRepository().getSelectedIds());
            server.reloadResources(selectedPacks).whenComplete((unused, error) -> server.execute(() -> {
                if (error != null) {
                    rollback(player, server, result, error);
                    return;
                }
                if (!TalentEditorMod.DEFINITIONS.completeUndo(result)) {
                    TalentEditorMod.LOGGER.error(
                            "Undo {} succeeded, but its history state could not be persisted",
                            packet.transactionId()
                    );
                }
                TalentAttributeRuntime.refreshAll(server);
                EditorNetwork.syncAllPlayerStates(server);
                EditorNetwork.sendBootstrap(player, false);
                EditorNetwork.sendUndoResult(player, new DefinitionUndoResultPacket(
                        DefinitionUndoStatus.UNDONE, packet.transactionId(),
                        TalentEditorMod.SNAPSHOTS.active().revision()
                ));
            }));
        } catch (Exception ex) {
            rollback(player, server, result, ex);
        }
        context.setPacketHandled(true);
    }

    private static void rollback(
            ServerPlayer player,
            MinecraftServer server,
            DefinitionUndoResult result,
            Throwable error
    ) {
        boolean restored = TalentEditorMod.DEFINITIONS.rollbackUndo(
                result, ForgeCompilationEnvironment.capture()
        );
        TalentEditorMod.LOGGER.error(
                "Failed to reload data packs after undo {}; rollback restored={}",
                result.transactionId(), restored, error
        );
        TalentAttributeRuntime.refreshAll(server);
        EditorNetwork.syncAllPlayerStates(server);
        EditorNetwork.sendBootstrap(player, false);
        EditorNetwork.sendUndoResult(player, new DefinitionUndoResultPacket(
                DefinitionUndoStatus.RELOAD_FAILED, result.transactionId(),
                TalentEditorMod.SNAPSHOTS.active().revision()
        ));
    }
}
