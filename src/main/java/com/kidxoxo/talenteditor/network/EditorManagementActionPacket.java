package com.kidxoxo.talenteditor.network;

import com.kidxoxo.talenteditor.TalentEditorMod;
import com.kidxoxo.talenteditor.settings.EditorSettingsService;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.io.IOException;
import java.nio.file.Path;
import java.util.concurrent.CompletionException;
import java.util.function.Supplier;

public record EditorManagementActionPacket(EditorSettingsService.ManagementAction action, String input) {
    private static final int MAX_INPUT = 4_096;

    public EditorManagementActionPacket {
        input = input == null ? "" : input;
        if (input.length() > MAX_INPUT) input = input.substring(0, MAX_INPUT);
    }

    public EditorManagementActionPacket(EditorSettingsService.ManagementAction action) {
        this(action, "");
    }

    static void encode(EditorManagementActionPacket packet, FriendlyByteBuf buffer) {
        buffer.writeEnum(packet.action());
        buffer.writeUtf(packet.input(), MAX_INPUT);
    }

    static EditorManagementActionPacket decode(FriendlyByteBuf buffer) {
        return new EditorManagementActionPacket(
                buffer.readEnum(EditorSettingsService.ManagementAction.class), buffer.readUtf(MAX_INPUT)
        );
    }

    static void handle(EditorManagementActionPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        ServerPlayer player = context.getSender();
        if (player != null) {
            if (!player.hasPermissions(2)) {
                player.sendSystemMessage(Component.translatable("message.talent_editor.permission_denied"));
            } else {
                if (packet.action() == EditorSettingsService.ManagementAction.IMPORT) {
                    beginImport(player, packet.input());
                    context.setPacketHandled(true);
                    return;
                }
                try {
                    Path result = switch (packet.action()) {
                        case EXPORT -> TalentEditorMod.SETTINGS.exportCurrent(player.server);
                        case INSTALL_GLOBAL -> TalentEditorMod.SETTINGS.installGlobalTemplate(player.server);
                        case UPDATE_SETTINGS, IMPORT -> throw new IllegalArgumentException("Invalid management action");
                    };
                    EditorNetwork.sendManagementResult(player, new EditorSettingsService.ManagementResult(
                            packet.action(), true, result.toString()
                    ));
                    EditorNetwork.sendSettings(player);
                } catch (Exception exception) {
                    TalentEditorMod.LOGGER.error("Talent editor management action failed: {}", packet.action(), exception);
                    EditorNetwork.sendManagementResult(player, new EditorSettingsService.ManagementResult(
                            packet.action(), false,
                            exception.getMessage() == null ? exception.getClass().getSimpleName() : exception.getMessage()
                    ));
                }
            }
        }
        context.setPacketHandled(true);
    }

    private static void beginImport(ServerPlayer player, String input) {
        EditorSettingsService.ImportInstallation installation = null;
        try {
            installation = TalentEditorMod.SETTINGS.beginImport(player.server, input);
            EditorSettingsService.ImportInstallation installed = installation;
            TalentEditorMod.EDITOR_PACK.selectAndReload(player.server).whenComplete((unused, error) ->
                    player.server.execute(() -> finishImport(player, installed, error)));
        } catch (Exception exception) {
            if (installation != null) {
                try {
                    TalentEditorMod.SETTINGS.rollbackImport(installation);
                } catch (Exception rollbackError) {
                    exception.addSuppressed(rollbackError);
                }
            }
            TalentEditorMod.LOGGER.error("Talent editor import failed before reload", exception);
            sendFailure(player, exception);
            EditorNetwork.syncSettings(player.server);
        }
    }

    private static void finishImport(
            ServerPlayer player,
            EditorSettingsService.ImportInstallation installation,
            Throwable reloadError
    ) {
        if (reloadError != null || TalentEditorMod.SNAPSHOTS.lastReport().hasErrors()) {
            Throwable failure = reloadError == null
                    ? new IllegalStateException("Imported definitions were rejected during data-pack reload")
                    : unwrap(reloadError);
            try {
                TalentEditorMod.SETTINGS.rollbackImport(installation);
                TalentEditorMod.EDITOR_PACK.selectAndReload(player.server).whenComplete((unused, restoreError) ->
                        player.server.execute(() -> {
                            if (restoreError != null) failure.addSuppressed(unwrap(restoreError));
                            else {
                                com.kidxoxo.talenteditor.runtime.TalentAttributeRuntime.refreshAll(player.server);
                                EditorNetwork.syncAllPlayerStates(player.server);
                                EditorNetwork.sendBootstrap(player, false);
                            }
                            TalentEditorMod.LOGGER.error("Talent editor import was rolled back", failure);
                            sendFailure(player, failure);
                            EditorNetwork.syncSettings(player.server);
                        }));
            } catch (Exception rollbackError) {
                failure.addSuppressed(rollbackError);
                TalentEditorMod.LOGGER.error("Talent editor import rollback failed", failure);
                sendFailure(player, failure);
                EditorNetwork.syncSettings(player.server);
            }
            return;
        }

        String detail;
        try {
            detail = TalentEditorMod.SETTINGS.completeImport(installation);
        } catch (IOException cleanupError) {
            TalentEditorMod.LOGGER.warn("Imported settings, but could not remove staging files", cleanupError);
            detail = installation.archive() + " (installed; staging cleanup needs attention)";
        }
        com.kidxoxo.talenteditor.runtime.TalentAttributeRuntime.refreshAll(player.server);
        EditorNetwork.syncAllPlayerStates(player.server);
        EditorNetwork.sendBootstrap(player, false);
        EditorNetwork.syncSettings(player.server);
        EditorNetwork.sendManagementResult(player, new EditorSettingsService.ManagementResult(
                EditorSettingsService.ManagementAction.IMPORT, true, detail
        ));
    }

    private static void sendFailure(ServerPlayer player, Throwable throwable) {
        String detail = throwable.getMessage() == null
                ? throwable.getClass().getSimpleName() : throwable.getMessage();
        EditorNetwork.sendManagementResult(player, new EditorSettingsService.ManagementResult(
                EditorSettingsService.ManagementAction.IMPORT, false, detail
        ));
    }

    private static Throwable unwrap(Throwable throwable) {
        if (throwable instanceof CompletionException && throwable.getCause() != null) {
            return throwable.getCause();
        }
        return throwable;
    }
}
