package com.kidxoxo.talenteditor.network;

import com.kidxoxo.talenteditor.TalentEditorMod;
import com.kidxoxo.talenteditor.domain.DefinitionKind;
import com.kidxoxo.talenteditor.editor.EditorSubmissionResult;
import com.kidxoxo.talenteditor.forge.ForgeCompilationEnvironment;
import com.kidxoxo.talenteditor.runtime.TalentAttributeRuntime;
import com.kidxoxo.talenteditor.validation.ValidationReport;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;
import java.util.function.Supplier;

final class DefinitionSubmissionHandler {
    private DefinitionSubmissionHandler() { }

    static void handle(DefinitionKind kind, long revision, ResourceLocation id, String json,
                       boolean commit, long requestId, Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        try {
            ServerPlayer player = context.getSender();
            if (player == null) return;
            if (!player.hasPermissions(2)) {
                var report = ValidationReport.builder().error("PERMISSION_DENIED",
                        (kind == DefinitionKind.ITEM ? "configured item" : kind.serializedName()) + " " + id,
                        "$", "Operator permission level 2 is required").build();
                EditorNetwork.sendValidation(player, requestId, new EditorSubmissionResult(false, false, false,
                        TalentEditorMod.SNAPSHOTS.active().revision(),
                        TalentEditorMod.SNAPSHOTS.active().contentHash(), report));
                player.sendSystemMessage(Component.translatable("message.talent_editor.permission_denied"));
                return;
            }
            MinecraftServer server = player.getServer();
            if (server == null) return;
            var result = TalentEditorMod.DEFINITIONS.submit(kind, revision, id, json, commit,
                    ForgeCompilationEnvironment.capture(), TalentEditorMod.EDITOR_PACK.datapacksRoot(server));
            if (result.committed()) {
                if (kind == DefinitionKind.TALENT || kind == DefinitionKind.RARITY || kind == DefinitionKind.POOL)
                    TalentAttributeRuntime.refreshAll(server);
                if (kind != DefinitionKind.ITEM) EditorNetwork.syncAllPlayerStates(server);
                // Metadata must arrive before the acknowledgement unlocks reference/delete actions.
                EditorNetwork.sendBootstrap(player, false);
                if (kind == DefinitionKind.ITEM) server.getPlayerList().getPlayers().stream()
                        .filter(other -> other != player).forEach(EditorNetwork::sendConfiguredItemCatalog);
            }
            EditorNetwork.sendValidation(player, requestId, result);
            if (result.committed()) enablePack(server, player);
        } finally {
            context.setPacketHandled(true);
        }
    }

    private static void enablePack(MinecraftServer server, ServerPlayer player) {
        try {
            TalentEditorMod.EDITOR_PACK.ensureSelected(server).whenComplete((unused, error) -> {
                if (error != null) {
                    TalentEditorMod.LOGGER.error("Failed to enable generated talent data pack", error);
                    server.execute(() -> player.sendSystemMessage(
                            Component.translatable("message.talent_editor.pack_enable_failed")));
                }
            });
        } catch (Exception ex) {
            TalentEditorMod.LOGGER.error("Failed to discover generated talent data pack", ex);
            player.sendSystemMessage(Component.translatable("message.talent_editor.pack_enable_failed"));
        }
    }
}
