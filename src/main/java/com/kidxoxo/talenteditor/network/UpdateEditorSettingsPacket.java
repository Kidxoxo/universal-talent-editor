package com.kidxoxo.talenteditor.network;

import com.kidxoxo.talenteditor.TalentEditorMod;
import com.kidxoxo.talenteditor.settings.EditorRuntimeSettings;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public record UpdateEditorSettingsPacket(
        boolean automaticSynthesis,
        EditorRuntimeSettings.SynthesisSoundAudience synthesisSoundAudience,
        EditorRuntimeSettings.SynthesisMessageAudience synthesisMessageAudience,
        EditorRuntimeSettings.SoundOverlapPolicy soundOverlapPolicy
) {
    static void encode(UpdateEditorSettingsPacket packet, FriendlyByteBuf buffer) {
        buffer.writeBoolean(packet.automaticSynthesis());
        buffer.writeEnum(packet.synthesisSoundAudience());
        buffer.writeEnum(packet.synthesisMessageAudience());
        buffer.writeEnum(packet.soundOverlapPolicy());
    }

    static UpdateEditorSettingsPacket decode(FriendlyByteBuf buffer) {
        return new UpdateEditorSettingsPacket(
                buffer.readBoolean(),
                buffer.readEnum(EditorRuntimeSettings.SynthesisSoundAudience.class),
                buffer.readEnum(EditorRuntimeSettings.SynthesisMessageAudience.class),
                buffer.readEnum(EditorRuntimeSettings.SoundOverlapPolicy.class)
        );
    }

    static void handle(UpdateEditorSettingsPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        ServerPlayer player = context.getSender();
        if (player != null) {
            if (!player.hasPermissions(2)) {
                player.sendSystemMessage(Component.translatable("message.talent_editor.permission_denied"));
                EditorNetwork.sendManagementResult(player, new com.kidxoxo.talenteditor.settings.EditorSettingsService.ManagementResult(
                        com.kidxoxo.talenteditor.settings.EditorSettingsService.ManagementAction.UPDATE_SETTINGS, false,
                        Component.translatable("message.talent_editor.permission_denied").getString()));
            } else {
                try {
                    TalentEditorMod.SETTINGS.update(
                            player.server, new EditorRuntimeSettings(
                                    packet.automaticSynthesis(), packet.synthesisSoundAudience(),
                                    packet.synthesisMessageAudience(), packet.soundOverlapPolicy()
                            )
                    );
                    EditorNetwork.syncSettings(player.server);
                } catch (Exception exception) {
                    TalentEditorMod.LOGGER.error("Could not save talent editor settings", exception);
                    EditorNetwork.sendManagementResult(player,
                            new com.kidxoxo.talenteditor.settings.EditorSettingsService.ManagementResult(
                                    com.kidxoxo.talenteditor.settings.EditorSettingsService.ManagementAction.UPDATE_SETTINGS,
                                    false, exception.getMessage()
                            ));
                }
            }
        }
        context.setPacketHandled(true);
    }
}
