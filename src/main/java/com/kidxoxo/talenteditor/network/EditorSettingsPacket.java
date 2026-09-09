package com.kidxoxo.talenteditor.network;

import com.kidxoxo.talenteditor.settings.EditorRuntimeSettings;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public record EditorSettingsPacket(
        boolean automaticSynthesis,
        EditorRuntimeSettings.SynthesisSoundAudience synthesisSoundAudience,
        EditorRuntimeSettings.SynthesisMessageAudience synthesisMessageAudience,
        EditorRuntimeSettings.SoundOverlapPolicy soundOverlapPolicy,
        boolean globalTemplateAvailable
) {
    public EditorRuntimeSettings settings() {
        return new EditorRuntimeSettings(
                automaticSynthesis, synthesisSoundAudience, synthesisMessageAudience, soundOverlapPolicy
        );
    }

    static void encode(EditorSettingsPacket packet, FriendlyByteBuf buffer) {
        buffer.writeBoolean(packet.automaticSynthesis());
        buffer.writeEnum(packet.synthesisSoundAudience());
        buffer.writeEnum(packet.synthesisMessageAudience());
        buffer.writeEnum(packet.soundOverlapPolicy());
        buffer.writeBoolean(packet.globalTemplateAvailable());
    }

    static EditorSettingsPacket decode(FriendlyByteBuf buffer) {
        return new EditorSettingsPacket(
                buffer.readBoolean(),
                buffer.readEnum(EditorRuntimeSettings.SynthesisSoundAudience.class),
                buffer.readEnum(EditorRuntimeSettings.SynthesisMessageAudience.class),
                buffer.readEnum(EditorRuntimeSettings.SoundOverlapPolicy.class),
                buffer.readBoolean()
        );
    }

    static void handle(EditorSettingsPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () ->
                com.kidxoxo.talenteditor.client.ClientPacketHandlers.acceptEditorSettings(packet)
        );
        contextSupplier.get().setPacketHandled(true);
    }
}
