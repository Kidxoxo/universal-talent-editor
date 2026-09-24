package com.kidxoxo.talenteditor.network;

import com.kidxoxo.talenteditor.TalentEditorMod;
import com.kidxoxo.talenteditor.editor.DefinitionService;
import com.kidxoxo.talenteditor.editor.EditorSubmissionResult;
import com.kidxoxo.talenteditor.forge.ForgeCompilationEnvironment;
import com.kidxoxo.talenteditor.validation.ValidationReport;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.nio.file.Path;
import java.util.function.Supplier;

public record SubmitConfiguredItemPacket(
        long baseRevision,
        ResourceLocation id,
        String json,
        boolean commit
) {
    static void encode(SubmitConfiguredItemPacket packet, FriendlyByteBuf buffer) {
        buffer.writeVarLong(packet.baseRevision());
        buffer.writeResourceLocation(packet.id());
        buffer.writeUtf(packet.json(), DefinitionService.MAX_JSON_CHARACTERS);
        buffer.writeBoolean(packet.commit());
    }

    static SubmitConfiguredItemPacket decode(FriendlyByteBuf buffer) {
        return new SubmitConfiguredItemPacket(
                buffer.readVarLong(), buffer.readResourceLocation(),
                buffer.readUtf(DefinitionService.MAX_JSON_CHARACTERS), buffer.readBoolean()
        );
    }

    static void handle(SubmitConfiguredItemPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        ServerPlayer player = context.getSender();
        if (player == null) {
            context.setPacketHandled(true);
            return;
        }
        if (!player.hasPermissions(2)) {
            ValidationReport report = ValidationReport.builder().error(
                    "PERMISSION_DENIED", "configured item " + packet.id(), "$",
                    "Operator permission level 2 is required"
            ).build();
            EditorNetwork.sendValidation(player, new EditorSubmissionResult(
                    false, false, false, TalentEditorMod.SNAPSHOTS.active().revision(),
                    TalentEditorMod.SNAPSHOTS.active().contentHash(), report
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
        EditorSubmissionResult result = TalentEditorMod.DEFINITIONS.submitConfiguredItem(
                packet.baseRevision(), packet.id(), packet.json(), packet.commit(),
                ForgeCompilationEnvironment.capture(), datapacksRoot
        );
        EditorNetwork.sendValidation(player, result);
        if (result.committed()) {
            EditorNetwork.sendBootstrap(player, false);
            server.getPlayerList().getPlayers().stream().filter(other -> other != player)
                    .forEach(EditorNetwork::sendConfiguredItemCatalog);
            try {
                TalentEditorMod.EDITOR_PACK.ensureSelected(server).whenComplete((unused, error) -> {
                    if (error != null) {
                        TalentEditorMod.LOGGER.error("Failed to enable generated talent data pack", error);
                        server.execute(() -> player.sendSystemMessage(
                                Component.translatable("message.talent_editor.pack_enable_failed")
                        ));
                    }
                });
            } catch (Exception ex) {
                TalentEditorMod.LOGGER.error("Failed to discover generated talent data pack", ex);
                player.sendSystemMessage(Component.translatable("message.talent_editor.pack_enable_failed"));
            }
        }
        context.setPacketHandled(true);
    }
}
