package com.kidxoxo.talenteditor.client;

import com.kidxoxo.talenteditor.network.EditorBootstrap;
import com.kidxoxo.talenteditor.network.EditorManagementResultPacket;
import com.kidxoxo.talenteditor.network.EditorNetwork;
import com.kidxoxo.talenteditor.network.EditorSettingsPacket;
import com.kidxoxo.talenteditor.settings.EditorSettingsService;
import com.kidxoxo.talenteditor.settings.EditorRuntimeSettings;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.server.packs.repository.Pack;
import net.minecraft.server.packs.repository.PackRepository;

import java.util.Optional;

public final class EditorSettingsScreen extends Screen {
    private final EditorBootstrap bootstrap;
    private boolean automaticSynthesis;
    private EditorRuntimeSettings.SynthesisSoundAudience soundAudience;
    private EditorRuntimeSettings.SynthesisMessageAudience messageAudience;
    private EditorRuntimeSettings.SoundOverlapPolicy overlapPolicy;
    private boolean globalTemplateAvailable;
    private Button synthesisButton;
    private Button audienceButton;
    private Button messageAudienceButton;
    private Button overlapButton;
    private EditBox importField;
    private Component status = Component.empty();
    private int statusColor = EditorTheme.MUTED;

    public EditorSettingsScreen(EditorBootstrap bootstrap) {
        super(Component.translatable("screen.talent_editor.settings.title"));
        this.bootstrap = bootstrap;
        EditorSettingsPacket settings = ClientEditorState.editorSettings();
        automaticSynthesis = settings.automaticSynthesis();
        soundAudience = settings.synthesisSoundAudience();
        messageAudience = settings.synthesisMessageAudience();
        overlapPolicy = settings.soundOverlapPolicy();
        globalTemplateAvailable = settings.globalTemplateAvailable();
    }

    @Override
    protected void init() {
        int formWidth = Math.min(620, width - 32);
        int x = (width - formWidth) / 2;
        synthesisButton = addRenderableWidget(Button.builder(synthesisLabel(), button -> {
            automaticSynthesis = !automaticSynthesis;
            button.setMessage(synthesisLabel());
            saveSettings();
        }).bounds(x, 28, formWidth, 20).build());
        int gap = 8;
        int half = (formWidth - gap) / 2;
        audienceButton = addRenderableWidget(Button.builder(audienceLabel(), button -> {
            soundAudience = soundAudience.next();
            button.setMessage(audienceLabel());
            saveSettings();
        }).bounds(x, 52, half, 20).build());
        messageAudienceButton = addRenderableWidget(Button.builder(messageAudienceLabel(), button -> {
            messageAudience = messageAudience.next();
            button.setMessage(messageAudienceLabel());
            saveSettings();
        }).bounds(x + half + gap, 52, formWidth - half - gap, 20).build());
        overlapButton = addRenderableWidget(Button.builder(overlapLabel(), button -> {
            overlapPolicy = overlapPolicy.next();
            button.setMessage(overlapLabel());
            saveSettings();
        }).bounds(x, 76, formWidth, 20).build());
        addRenderableWidget(Button.builder(Component.translatable("screen.talent_editor.settings.export"),
                button -> run(EditorSettingsService.ManagementAction.EXPORT))
                .bounds(x, 104, half, 20).build());
        addRenderableWidget(Button.builder(Component.translatable("screen.talent_editor.settings.install_global"),
                button -> run(EditorSettingsService.ManagementAction.INSTALL_GLOBAL))
                .bounds(x + half + gap, 104, formWidth - half - gap, 20).build());
        int importButtonWidth = Math.min(160, Math.max(90, formWidth / 3));
        importField = new EditBox(
                font, x, 140, formWidth - importButtonWidth - gap, 20,
                Component.translatable("screen.talent_editor.settings.import_path")
        );
        importField.setMaxLength(4_096);
        importField.setHint(Component.translatable("screen.talent_editor.settings.import_hint"));
        addRenderableWidget(importField);
        addRenderableWidget(Button.builder(Component.translatable("screen.talent_editor.settings.import"),
                button -> importSettings())
                .bounds(x + formWidth - importButtonWidth, 140, importButtonWidth, 20).build());
        addRenderableWidget(Button.builder(Component.translatable("gui.back"), button -> returnHome())
                .bounds(width / 2 - 55, height - 28, 110, 20).build());
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics);
        int formWidth = Math.min(620, width - 32);
        int x = (width - formWidth) / 2;
        EditorTheme.panel(graphics, x - 7, 23, formWidth + 14, Math.max(1, height - 59));
        super.render(graphics, mouseX, mouseY, partialTick);
        graphics.drawCenteredString(font, title, width / 2, 10, 0xFFFFFF);
        graphics.drawString(font, Component.translatable("screen.talent_editor.settings.import_path"),
                x, 129, EditorTheme.MUTED, false);
        Component help = Component.translatable(
                        globalTemplateAvailable
                                ? "screen.talent_editor.settings.global_installed"
                                : "screen.talent_editor.settings.global_help");
        graphics.drawString(font, font.plainSubstrByWidth(help.getString(), formWidth),
                x, 166, globalTemplateAvailable ? 0xFF77DD88 : EditorTheme.MUTED, false);
        if (!status.getString().isBlank()) {
            if (height >= 300) graphics.drawWordWrap(font, status, x, 181, formWidth, statusColor);
            else graphics.drawString(font, font.plainSubstrByWidth(status.getString(), formWidth),
                    x, 181, statusColor, false);
        }
    }

    public void acceptSettings(EditorSettingsPacket packet) {
        automaticSynthesis = packet.automaticSynthesis();
        soundAudience = packet.synthesisSoundAudience();
        messageAudience = packet.synthesisMessageAudience();
        overlapPolicy = packet.soundOverlapPolicy();
        globalTemplateAvailable = packet.globalTemplateAvailable();
        if (synthesisButton != null) synthesisButton.setMessage(synthesisLabel());
        if (audienceButton != null) audienceButton.setMessage(audienceLabel());
        if (messageAudienceButton != null) messageAudienceButton.setMessage(messageAudienceLabel());
        if (overlapButton != null) overlapButton.setMessage(overlapLabel());
        status = Component.translatable("screen.talent_editor.settings.saved");
        statusColor = 0xFF77DD88;
    }

    public void acceptManagementResult(EditorManagementResultPacket packet) {
        if (packet.successful() && packet.action() == EditorSettingsService.ManagementAction.EXPORT
                && importField != null) importField.setValue(packet.detail());
        status = Component.translatable(
                packet.successful()
                        ? "screen.talent_editor.settings.result.success"
                        : "screen.talent_editor.settings.result.failure",
                packet.detail()
        );
        statusColor = packet.successful() ? 0xFF77DD88 : 0xFFFF7777;
        if (packet.successful() && packet.action() == EditorSettingsService.ManagementAction.IMPORT) {
            reloadImportedClientPack(packet.detail());
        }
    }

    private void run(EditorSettingsService.ManagementAction action) {
        status = Component.translatable("screen.talent_editor.settings.working");
        statusColor = EditorTheme.ACCENT;
        EditorNetwork.runManagementAction(action);
    }

    private void importSettings() {
        status = Component.translatable("screen.talent_editor.settings.import_working");
        statusColor = EditorTheme.ACCENT;
        EditorNetwork.importSettings(importField == null ? "" : importField.getValue());
    }

    private void saveSettings() {
        status = Component.translatable("screen.talent_editor.settings.saving");
        statusColor = EditorTheme.ACCENT;
        EditorNetwork.updateSettings(new EditorRuntimeSettings(
                automaticSynthesis, soundAudience, messageAudience, overlapPolicy
        ));
    }

    private Component synthesisLabel() {
        return Component.translatable(automaticSynthesis
                ? "screen.talent_editor.settings.synthesis.auto"
                : "screen.talent_editor.settings.synthesis.manual");
    }

    private Component audienceLabel() {
        return Component.translatable(soundAudience == EditorRuntimeSettings.SynthesisSoundAudience.GLOBAL
                ? "screen.talent_editor.settings.sound_audience.global"
                : "screen.talent_editor.settings.sound_audience.personal");
    }

    private Component messageAudienceLabel() {
        return Component.translatable(messageAudience == EditorRuntimeSettings.SynthesisMessageAudience.GLOBAL
                ? "screen.talent_editor.settings.message_audience.global"
                : "screen.talent_editor.settings.message_audience.personal");
    }

    private Component overlapLabel() {
        return Component.translatable(overlapPolicy == EditorRuntimeSettings.SoundOverlapPolicy.QUEUE
                ? "screen.talent_editor.settings.sound_overlap.queue"
                : "screen.talent_editor.settings.sound_overlap.immediate");
    }

    private void reloadImportedClientPack(String detail) {
        if (minecraft == null) return;
        PackRepository repository = minecraft.getResourcePackRepository();
        repository.reload();
        Optional<String> packId = repository.getAvailableIds().stream()
                .filter(id -> id.equals(ManagedClientResourcePack.PREFERRED_PACK_ID))
                .findFirst()
                .or(() -> repository.getAvailableIds().stream()
                        .filter(id -> id.endsWith("/" + ManagedClientResourcePack.DIRECTORY_NAME)).findFirst());
        if (packId.isEmpty() || !repository.getSelectedIds().contains(packId.orElseThrow())
                && !repository.addPack(packId.orElseThrow())) {
            status = Component.translatable("screen.talent_editor.settings.import_no_client_pack", detail);
            statusColor = 0xFFFFAA55;
            return;
        }
        minecraft.options.resourcePacks.clear();
        minecraft.options.incompatibleResourcePacks.clear();
        for (Pack selected : repository.getSelectedPacks()) {
            if (selected.isFixedPosition()) continue;
            minecraft.options.resourcePacks.add(selected.getId());
            if (!selected.getCompatibility().isCompatible()) {
                minecraft.options.incompatibleResourcePacks.add(selected.getId());
            }
        }
        minecraft.options.save();
        status = Component.translatable("screen.talent_editor.settings.import_reloading");
        statusColor = EditorTheme.ACCENT;
        minecraft.reloadResourcePacks().whenComplete((ignored, error) -> minecraft.execute(() -> {
            if (error == null) {
                ClientPacketHandlers.recheckResourceRequirements();
                status = Component.translatable("screen.talent_editor.settings.import_complete", detail);
                statusColor = 0xFF77DD88;
            } else {
                status = Component.translatable("screen.talent_editor.settings.import_reload_failed",
                        error.getMessage() == null ? error.getClass().getSimpleName() : error.getMessage());
                statusColor = 0xFFFFAA55;
            }
        }));
    }

    private void returnHome() {
        if (minecraft == null) return;
        EditorBootstrap latest = ClientEditorState.bootstrap();
        minecraft.setScreen(new EditorHomeScreen(latest == null ? bootstrap : latest));
    }

    @Override
    public void onClose() {
        returnHome();
    }
}
