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

public final class EditorSettingsScreen extends EditorScreen {
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
    private String importValue="";
    private boolean transferTab, settingsPending, managementPending;
    boolean busy(){return settingsPending||managementPending;}
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
        synthesisButton = addRenderableWidget(EditorWidgets.button(synthesisLabel(), button -> {
            automaticSynthesis = !automaticSynthesis;
            button.setMessage(synthesisLabel());
            saveSettings();
        }).bounds(x, 60, formWidth, 20).build());
        int gap = 8;
        int half = (formWidth - gap) / 2;
        audienceButton = addRenderableWidget(EditorWidgets.button(audienceLabel(), button -> {
            soundAudience = soundAudience.next();
            button.setMessage(audienceLabel());
            saveSettings();
        }).bounds(x, 84, half, 20).build());
        messageAudienceButton = addRenderableWidget(EditorWidgets.button(messageAudienceLabel(), button -> {
            messageAudience = messageAudience.next();
            button.setMessage(messageAudienceLabel());
            saveSettings();
        }).bounds(x + half + gap, 84, formWidth - half - gap, 20).build());
        overlapButton = addRenderableWidget(EditorWidgets.button(overlapLabel(), button -> {
            overlapPolicy = overlapPolicy.next();
            button.setMessage(overlapLabel());
            saveSettings();
        }).bounds(x, 108, formWidth, 20).build());
        addRenderableWidget(EditorWidgets.button(Component.translatable("screen.talent_editor.settings.export"),
                button -> run(EditorSettingsService.ManagementAction.EXPORT))
                .bounds(x, 60, half, 20).build());
        addRenderableWidget(EditorWidgets.button(Component.translatable("screen.talent_editor.settings.install_global"),
                button -> minecraft.setScreen(new EditorConfirmScreen(yes->{minecraft.setScreen(this);if(yes)run(EditorSettingsService.ManagementAction.INSTALL_GLOBAL);},EditorDefinitionScreen.tr("global_template"),EditorDefinitionScreen.tr("global_template_hint"))))
                .bounds(x + half + gap, 60, formWidth - half - gap, 20).build());
        int importButtonWidth = Math.min(160, Math.max(90, formWidth / 3));
        importField = new EditBox(
                font, x, 100, formWidth - importButtonWidth - gap, 20,
                Component.translatable("screen.talent_editor.settings.import_path")
        );
        importField.setMaxLength(4_096);
        importField.setHint(Component.translatable("screen.talent_editor.settings.import_hint"));
        importField.setValue(importValue);importField.setResponder(value->importValue=value);
        addRenderableWidget(importField);
        addRenderableWidget(EditorWidgets.button(Component.translatable("screen.talent_editor.settings.import"),
                button -> importSettings())
                .bounds(x + formWidth - importButtonWidth, 100, importButtonWidth, 20).build());
        addRenderableWidget(EditorWidgets.button(Component.translatable("gui.back"), button -> returnHome())
                .bounds(width / 2 - 55, height - 28, 110, 20).build());
        var controls=children().stream().filter(c->c instanceof net.minecraft.client.gui.components.AbstractWidget).map(c->(net.minecraft.client.gui.components.AbstractWidget)c).toList();
        for(int i=0;i<4;i++)controls.get(i).visible=!transferTab;
        for(int i=4;i<8;i++)controls.get(i).visible=transferTab;
        addRenderableWidget(EditorWidgets.button(EditorDefinitionScreen.tr("runtime_settings"),b->{transferTab=false;rebuildWidgets();}).bounds(x,28,half,20).build());
        addRenderableWidget(EditorWidgets.button(EditorDefinitionScreen.tr("pack_tools"),b->{transferTab=true;rebuildWidgets();}).bounds(x+half+gap,28,formWidth-half-gap,20).build());

    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics);
        int formWidth = Math.min(620, width - 32);
        int x = (width - formWidth) / 2;
        EditorTheme.panel(graphics, x - 7, 23, formWidth + 14, Math.max(1, height - 59));
        for (var child : children()) if (child instanceof net.minecraft.client.gui.components.AbstractWidget widget) widget.active = !busy();
        super.render(graphics, mouseX, mouseY, partialTick);
        EditorTheme.title(graphics,font,title,width);
        EditorTheme.selectedTab(graphics,transferTab?x+halfWidth(formWidth)+8:x,28,halfWidth(formWidth));
        if(transferTab){
            graphics.drawString(font,Component.translatable("screen.talent_editor.settings.import_path"),x,89,EditorTheme.MUTED,false);
            graphics.drawWordWrap(font,EditorDefinitionScreen.tr("pack_scope"),x,133,formWidth,EditorTheme.MUTED);
        }else graphics.drawWordWrap(font,EditorDefinitionScreen.tr("immediate_settings"),x,145,formWidth,EditorTheme.MUTED);
        if (!status.getString().isBlank()) {
            if (height >= 300) graphics.drawWordWrap(font, status, x, 181, formWidth, statusColor);
            else graphics.drawString(font, font.plainSubstrByWidth(status.getString(), formWidth),
                    x, 181, statusColor, false);
        }
    }

    private static int halfWidth(int width){return (width-8)/2;}

    public void acceptSettings(EditorSettingsPacket packet) {
        boolean saved = settingsPending;
        settingsPending=false;
        automaticSynthesis = packet.automaticSynthesis();
        soundAudience = packet.synthesisSoundAudience();
        messageAudience = packet.synthesisMessageAudience();
        overlapPolicy = packet.soundOverlapPolicy();
        globalTemplateAvailable = packet.globalTemplateAvailable();
        if (synthesisButton != null) synthesisButton.setMessage(synthesisLabel());
        if (audienceButton != null) audienceButton.setMessage(audienceLabel());
        if (messageAudienceButton != null) messageAudienceButton.setMessage(messageAudienceLabel());
        if (overlapButton != null) overlapButton.setMessage(overlapLabel());
        if (saved) {
            status = Component.translatable("screen.talent_editor.settings.saved");
            statusColor = 0xFF77DD88;
        }
    }

    public void acceptManagementResult(EditorManagementResultPacket packet) {
        managementPending=false;
        if(packet.action()==EditorSettingsService.ManagementAction.UPDATE_SETTINGS)settingsPending=false;
        if(packet.successful()&&packet.action()==EditorSettingsService.ManagementAction.PREVIEW_IMPORT){minecraft.setScreen(new EditorImportPreviewScreen(this,packet.detail()));return;}
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
        if(busy())return;managementPending=true;
        status = Component.translatable("screen.talent_editor.settings.working");
        statusColor = EditorTheme.ACCENT;
        EditorNetwork.runManagementAction(action);
    }

    private void importSettings() {
        if(busy())return;managementPending=true;
        status = Component.translatable("screen.talent_editor.settings.import_working");
        statusColor = EditorTheme.ACCENT;
        EditorNetwork.previewImport(importField == null ? "" : importField.getValue());
    }

    void confirmImport(String frozen){
        if(busy())return;managementPending=true;status=Component.translatable("screen.talent_editor.settings.import_working");EditorNetwork.importSettings(frozen);
    }

    private void saveSettings() {
        if(busy())return;settingsPending=true;
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
        if(busy())return;
        if (minecraft == null) return;
        EditorBootstrap latest = ClientEditorState.bootstrap();
        minecraft.setScreen(new EditorHomeScreen(latest == null ? bootstrap : latest));
    }

    @Override
    public void onClose() {
        returnHome();
    }
}
