package com.kidxoxo.talenteditor.network;

import com.kidxoxo.talenteditor.TalentEditorMod;
import com.kidxoxo.talenteditor.domain.DefinitionKind;
import com.kidxoxo.talenteditor.editor.DefinitionDeletionResult;
import com.kidxoxo.talenteditor.editor.DefinitionDeletionStatus;
import com.kidxoxo.talenteditor.forge.ForgeCompilationEnvironment;
import com.kidxoxo.talenteditor.runtime.TalentAttributeRuntime;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.nio.file.Path;
import java.util.List;
import java.util.function.Supplier;

public record DeleteDefinitionPacket(long baseRevision, DefinitionKind kind, ResourceLocation id) {
    static void encode(DeleteDefinitionPacket packet, FriendlyByteBuf buffer) {
        buffer.writeVarLong(packet.baseRevision());
        buffer.writeEnum(packet.kind());
        buffer.writeResourceLocation(packet.id());
    }

    static DeleteDefinitionPacket decode(FriendlyByteBuf buffer) {
        return new DeleteDefinitionPacket(
                buffer.readVarLong(), buffer.readEnum(DefinitionKind.class), buffer.readResourceLocation()
        );
    }

    static void handle(DeleteDefinitionPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        ServerPlayer player = context.getSender();
        if (player == null) {
            context.setPacketHandled(true);
            return;
        }
        if (!player.hasPermissions(2)) {
            EditorNetwork.sendDeletionResult(player, new DefinitionDeletionResultPacket(
                    DefinitionDeletionStatus.PERMISSION_DENIED, packet.kind(), packet.id(),
                    TalentEditorMod.SNAPSHOTS.active().revision(), List.of()
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

        Path datapacksRoot = TalentEditorMod.EDITOR_PACK.datapacksRoot(server);
        DefinitionDeletionResult result = TalentEditorMod.DEFINITIONS.deleteDefinition(
                packet.baseRevision(), packet.kind(), packet.id(),
                ForgeCompilationEnvironment.capture(), datapacksRoot
        );
        if (!result.deleted()) {
            EditorNetwork.sendDeletionResult(player, DefinitionDeletionResultPacket.from(result));
            context.setPacketHandled(true);
            return;
        }

        try {
            List<String> selectedPacks = List.copyOf(server.getPackRepository().getSelectedIds());
            server.reloadResources(selectedPacks).whenComplete((unused, error) -> server.execute(() -> {
                if (error != null) {
                    boolean restored = TalentEditorMod.DEFINITIONS.rollbackDeletion(
                            result, ForgeCompilationEnvironment.capture()
                    );
                    TalentEditorMod.LOGGER.error(
                            "Failed to reload data packs after deleting {} {}; rollback restored={}",
                            packet.kind().serializedName(), packet.id(), restored, error
                    );
                    TalentAttributeRuntime.refreshAll(server);
                    EditorNetwork.syncAllPlayerStates(server);
                    EditorNetwork.sendBootstrap(player, false);
                    EditorNetwork.sendDeletionResult(player, new DefinitionDeletionResultPacket(
                            DefinitionDeletionStatus.RELOAD_FAILED, packet.kind(), packet.id(),
                            TalentEditorMod.SNAPSHOTS.active().revision(), List.of()
                    ));
                    return;
                }
                TalentAttributeRuntime.refreshAll(server);
                EditorNetwork.syncAllPlayerStates(server);
                EditorNetwork.sendBootstrap(player, false);
                EditorNetwork.sendDeletionResult(player, new DefinitionDeletionResultPacket(
                        DefinitionDeletionStatus.DELETED, packet.kind(), packet.id(),
                        TalentEditorMod.SNAPSHOTS.active().revision(), List.of()
                ));
            }));
        } catch (Exception ex) {
            boolean restored = TalentEditorMod.DEFINITIONS.rollbackDeletion(
                    result, ForgeCompilationEnvironment.capture()
            );
            TalentEditorMod.LOGGER.error(
                    "Failed to start data pack reload after deleting {} {}; rollback restored={}",
                    packet.kind().serializedName(), packet.id(), restored, ex
            );
            TalentAttributeRuntime.refreshAll(server);
            EditorNetwork.syncAllPlayerStates(server);
            EditorNetwork.sendBootstrap(player, false);
            EditorNetwork.sendDeletionResult(player, new DefinitionDeletionResultPacket(
                    DefinitionDeletionStatus.RELOAD_FAILED, packet.kind(), packet.id(),
                    TalentEditorMod.SNAPSHOTS.active().revision(), List.of()
            ));
        }
        context.setPacketHandled(true);
    }
}
