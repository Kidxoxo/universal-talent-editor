package com.kidxoxo.talenteditor.network;

import com.kidxoxo.talenteditor.settings.EditorSettingsService;
import com.kidxoxo.talenteditor.settings.EditorRuntimeSettings;
import io.netty.buffer.Unpooled;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundSource;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class EditorSettingsPacketTest {
    @Test
    void roundTripsRuntimeSettingsAndManagementResult() {
        FriendlyByteBuf settingsBuffer = new FriendlyByteBuf(Unpooled.buffer());
        try {
            EditorSettingsPacket expected = new EditorSettingsPacket(
                    false,
                    EditorRuntimeSettings.SynthesisSoundAudience.GLOBAL,
                    EditorRuntimeSettings.SynthesisMessageAudience.GLOBAL,
                    EditorRuntimeSettings.SoundOverlapPolicy.QUEUE,
                    true
            );
            EditorSettingsPacket.encode(expected, settingsBuffer);
            assertEquals(expected, EditorSettingsPacket.decode(settingsBuffer));
        } finally {
            settingsBuffer.release();
        }

        FriendlyByteBuf updateBuffer = new FriendlyByteBuf(Unpooled.buffer());
        try {
            UpdateEditorSettingsPacket expected = new UpdateEditorSettingsPacket(
                    true,
                    EditorRuntimeSettings.SynthesisSoundAudience.PERSONAL,
                    EditorRuntimeSettings.SynthesisMessageAudience.GLOBAL,
                    EditorRuntimeSettings.SoundOverlapPolicy.IMMEDIATE
            );
            UpdateEditorSettingsPacket.encode(expected, updateBuffer);
            assertEquals(expected, UpdateEditorSettingsPacket.decode(updateBuffer));
        } finally {
            updateBuffer.release();
        }

        FriendlyByteBuf resultBuffer = new FriendlyByteBuf(Unpooled.buffer());
        try {
            EditorManagementResultPacket expected = new EditorManagementResultPacket(
                    EditorSettingsService.ManagementAction.INSTALL_GLOBAL, true, "config/template"
            );
            EditorManagementResultPacket.encode(expected, resultBuffer);
            assertEquals(expected, EditorManagementResultPacket.decode(resultBuffer));
        } finally {
            resultBuffer.release();
        }

        FriendlyByteBuf actionBuffer = new FriendlyByteBuf(Unpooled.buffer());
        try {
            EditorManagementActionPacket expected = new EditorManagementActionPacket(
                    EditorSettingsService.ManagementAction.IMPORT, "talent_editor_exports/example.zip"
            );
            EditorManagementActionPacket.encode(expected, actionBuffer);
            assertEquals(expected, EditorManagementActionPacket.decode(actionBuffer));
        } finally {
            actionBuffer.release();
        }

        FriendlyByteBuf soundBuffer = new FriendlyByteBuf(Unpooled.buffer());
        try {
            SynthesisSoundPacket expected = new SynthesisSoundPacket(
                    new ResourceLocation("example", "synthesis"), SoundSource.PLAYERS,
                    1.25F, 0.9F, EditorRuntimeSettings.SoundOverlapPolicy.IMMEDIATE
            );
            SynthesisSoundPacket.encode(expected, soundBuffer);
            assertEquals(expected, SynthesisSoundPacket.decode(soundBuffer));
        } finally {
            soundBuffer.release();
        }
    }
}
