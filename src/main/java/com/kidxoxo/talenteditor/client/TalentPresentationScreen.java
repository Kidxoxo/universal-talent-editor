package com.kidxoxo.talenteditor.client;

import com.kidxoxo.talenteditor.network.EditorBootstrap;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import java.util.List;
import java.util.Optional;

public final class TalentPresentationScreen extends Screen {
    private static final List<String> SOURCES = List.of(
            "master", "music", "records", "weather", "blocks",
            "hostile", "neutral", "players", "ambient", "voice"
    );
    private static final List<String> MODES = List.of("chat", "action_bar", "title");

    private final TalentEditScreen parent;
    private final EditorBootstrap bootstrap;
    private final TalentDraft.PresentationDraft[] phases = new TalentDraft.PresentationDraft[2];
    private int phaseIndex;
    private int sectionIndex;
    private int partIndex;
    private int sourceIndex;
    private int modeIndex;
    private boolean soundEnabled;
    private boolean messageEnabled;
    private boolean subtitleEnabled;
    private boolean bold;
    private boolean italic;
    private LocalizedTextDraft mainText;
    private LocalizedTextDraft subtitleText;
    private EditBox soundField;
    private EditBox volumeField;
    private EditBox pitchField;
    private EditBox literalField;
    private EditBox translationField;
    private EditBox colorField;
    private EditBox fadeInField;
    private EditBox stayField;
    private EditBox fadeOutField;
    private Button phaseButton;
    private Button soundSectionButton;
    private Button textSectionButton;
    private Button timingSectionButton;
    private Button soundToggle;
    private Button sourceButton;
    private Button chooseSoundButton;
    private Button messageToggle;
    private Button modeButton;
    private Button partButton;
    private Button subtitleToggle;
    private Button boldButton;
    private Button italicButton;
    private Button previewButton;
    private String status = "";

    public TalentPresentationScreen(TalentEditScreen parent, EditorBootstrap bootstrap) {
        super(Component.translatable("screen.talent_editor.presentation.title"));
        this.parent = parent;
        this.bootstrap = bootstrap;
        phases[0] = parent.presentation(TalentDraft.PresentationPhase.ACQUIRE);
        phases[1] = parent.presentation(TalentDraft.PresentationPhase.SYNTHESIZE);
    }

    @Override
    protected void init() {
        int formWidth = Math.min(680, width - 32);
        int x = (width - formWidth) / 2;
        int gap = 6;
        int half = (formWidth - gap) / 2;
        int third = (formWidth - gap * 2) / 3;

        phaseButton = addRenderableWidget(Button.builder(Component.empty(), button -> cyclePhase())
                .bounds(x, 29, formWidth, 20).build());
        soundSectionButton = addRenderableWidget(Button.builder(Component.translatable(
                        "screen.talent_editor.presentation.section.sound"), button -> switchSection(0))
                .bounds(x, 55, third, 20).build());
        textSectionButton = addRenderableWidget(Button.builder(Component.translatable(
                        "screen.talent_editor.presentation.section.text"), button -> switchSection(1))
                .bounds(x + third + gap, 55, third, 20).build());
        timingSectionButton = addRenderableWidget(Button.builder(Component.translatable(
                        "screen.talent_editor.presentation.section.timing"), button -> switchSection(2))
                .bounds(x + (third + gap) * 2, 55, formWidth - (third + gap) * 2, 20).build());
        soundToggle = addRenderableWidget(Button.builder(Component.empty(), button -> {
            soundEnabled = !soundEnabled;
            updateWidgets();
        }).bounds(x, 84, half, 20).build());
        sourceButton = addRenderableWidget(Button.builder(Component.empty(), button -> {
            sourceIndex = (sourceIndex + 1) % SOURCES.size();
            updateWidgets();
        }).bounds(x + half + gap, 84, formWidth - half - gap, 20).build());
        soundField = field(x, 116, formWidth - 92, 256);
        chooseSoundButton = addRenderableWidget(Button.builder(
                        Component.translatable("screen.talent_editor.presentation.choose_sound"),
                        button -> chooseSound())
                .bounds(x + formWidth - 86, 116, 86, 20).build());
        volumeField = field(x, 148, half, 16);
        pitchField = field(x + half + gap, 148, formWidth - half - gap, 16);

        messageToggle = addRenderableWidget(Button.builder(Component.empty(), button -> {
            if (messageEnabled && !flushPart()) return;
            messageEnabled = !messageEnabled;
            updateWidgets();
        }).bounds(x, 84, half, 20).build());
        modeButton = addRenderableWidget(Button.builder(Component.empty(), button -> cycleMode())
                .bounds(x + half + gap, 84, formWidth - half - gap, 20).build());
        partButton = addRenderableWidget(Button.builder(Component.empty(), button -> cyclePart())
                .bounds(x, 110, half, 20).build());
        subtitleToggle = addRenderableWidget(Button.builder(Component.empty(), button -> {
            if (subtitleEnabled && partIndex == 1 && !flushPart()) return;
            subtitleEnabled = !subtitleEnabled;
            updateWidgets();
        }).bounds(x + half + gap, 110, formWidth - half - gap, 20).build());

        literalField = field(x, 136, formWidth, 1_024);
        literalField.setHint(Component.translatable("screen.talent_editor.multiline_hint"));
        translationField = field(x, 162, formWidth, 1_024);
        translationField.setHint(Component.translatable("screen.talent_editor.presentation.translation"));
        colorField = field(x, 188, third, 9);
        colorField.setHint(Component.translatable("screen.talent_editor.text.color_hint"));
        boldButton = addRenderableWidget(Button.builder(Component.empty(), button -> {
            bold = !bold;
            updateWidgets();
        }).bounds(x + third + gap, 188, third, 20).build());
        italicButton = addRenderableWidget(Button.builder(Component.empty(), button -> {
            italic = !italic;
            updateWidgets();
        }).bounds(x + (third + gap) * 2, 188, formWidth - (third + gap) * 2, 20).build());

        fadeInField = field(x, 92, third, 8);
        stayField = field(x + third + gap, 92, third, 8);
        fadeOutField = field(x + (third + gap) * 2, 92, formWidth - (third + gap) * 2, 8);

        int bottom = height - 28;
        int bottomWidth = (formWidth - gap * 2) / 3;
        addRenderableWidget(Button.builder(Component.translatable("gui.back"), button -> returnWithoutApplying())
                .bounds(x, bottom, bottomWidth, 20).build());
        previewButton = addRenderableWidget(Button.builder(
                        Component.translatable("screen.talent_editor.presentation.preview"), button -> preview())
                .bounds(x + bottomWidth + gap, bottom, bottomWidth, 20).build());
        addRenderableWidget(Button.builder(Component.translatable("screen.talent_editor.presentation.apply"),
                        button -> apply())
                .bounds(x + (bottomWidth + gap) * 2, bottom,
                        formWidth - (bottomWidth + gap) * 2, 20).build());
        loadPhase();
    }

    private EditBox field(int x, int y, int width, int maxLength) {
        EditBox value = new EditBox(font, x, y, width, 20, Component.empty());
        value.setMaxLength(maxLength);
        value.setResponder(ignored -> status = "");
        addRenderableWidget(value);
        return value;
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics);
        int formWidth = Math.min(680, width - 32);
        int x = (width - formWidth) / 2;
        EditorTheme.panel(graphics, x - 7, 24, formWidth + 14, Math.max(1, height - 60));
        super.render(graphics, mouseX, mouseY, partialTick);
        graphics.drawCenteredString(font, title, width / 2, 10, 0xFFFFFF);
        int gap = 6;
        int third = (formWidth - gap * 2) / 3;
        int selectedX = x + sectionIndex * (third + gap);
        int selectedWidth = sectionIndex == 2 ? formWidth - (third + gap) * 2 : third;
        EditorTheme.selectedTab(graphics, selectedX, 55, selectedWidth);
        if (sectionIndex == 0) {
            label(graphics, x, 105, "sound");
            renderSoundDiagnostic(graphics, x, 105, formWidth);
            label(graphics, x, 137, "volume_pitch");
        } else if (sectionIndex == 2) {
            label(graphics, x, 81, "title_times");
            graphics.drawString(font, font.plainSubstrByWidth(Component.translatable(
                    "screen.talent_editor.presentation.timing_help").getString(), formWidth),
                    x, 118, EditorTheme.MUTED, false);
        }
        if (!phases[phaseIndex].editable()) {
            graphics.drawCenteredString(font,
                    Component.translatable("screen.talent_editor.presentation.advanced_read_only"),
                    width / 2, 19, 0xFFFFAA55);
        } else if (!status.isBlank()) {
            graphics.drawString(font, font.plainSubstrByWidth(status, formWidth),
                    x, 19, 0xFFFF7777, false);
        }
    }

    private void label(GuiGraphics graphics, int x, int y, String suffix) {
        graphics.drawString(font, Component.translatable("screen.talent_editor.presentation." + suffix),
                x, y, 0xFFA0A0A0, false);
    }

    private void renderSoundDiagnostic(GuiGraphics graphics, int x, int y, int formWidth) {
        if (!soundEnabled || minecraft == null) return;
        Component label = Component.translatable("screen.talent_editor.presentation.sound");
        int diagnosticX = x + font.width(label) + 8;
        int availableWidth = Math.max(1, x + formWidth - diagnosticX);
        ClientResourceDiagnostics.Result result = ClientResourceDiagnostics.inspect(
                soundField.getValue(), id -> minecraft.getSoundManager().getSoundEvent(id) != null
        );
        Component message;
        int color;
        if (result.state() == ClientResourceDiagnostics.State.AVAILABLE) {
            message = Component.translatable("screen.talent_editor.resource.available_short");
            color = 0xFF77DD88;
        } else if (result.state() == ClientResourceDiagnostics.State.MISSING) {
            message = Component.translatable("screen.talent_editor.resource.sound_missing_short");
            color = 0xFFFFAA55;
        } else if (result.state() == ClientResourceDiagnostics.State.INVALID) {
            message = Component.translatable("screen.talent_editor.resource.invalid_short");
            color = 0xFFFF7777;
        } else {
            return;
        }
        graphics.drawString(font, font.plainSubstrByWidth(message.getString(), availableWidth),
                diagnosticX, y, color, false);
    }

    private void cyclePhase() {
        if (!storePhase()) return;
        phaseIndex = (phaseIndex + 1) % phases.length;
        partIndex = 0;
        loadPhase();
    }

    private void switchSection(int next) {
        if (sectionIndex == 1 && !flushPart()) return;
        sectionIndex = Math.max(0, Math.min(2, next));
        updateWidgets();
    }

    private void cyclePart() {
        if (!flushPart()) return;
        partIndex = (partIndex + 1) % 2;
        loadPart();
        updateWidgets();
    }

    private void cycleMode() {
        if (!flushPart()) return;
        modeIndex = (modeIndex + 1) % MODES.size();
        if (!MODES.get(modeIndex).equals("title") && partIndex == 1) partIndex = 0;
        loadPart();
        updateWidgets();
    }

    private void loadPhase() {
        TalentDraft.PresentationDraft value = phases[phaseIndex];
        soundEnabled = value.soundEnabled();
        soundField.setValue(value.sound().toString());
        sourceIndex = Math.max(0, SOURCES.indexOf(value.soundSource()));
        volumeField.setValue(Double.toString(value.volume()));
        pitchField.setValue(Double.toString(value.pitch()));
        messageEnabled = value.messageEnabled();
        modeIndex = Math.max(0, MODES.indexOf(value.mode()));
        mainText = value.text();
        subtitleEnabled = value.subtitleEnabled();
        subtitleText = value.subtitle();
        fadeInField.setValue(Integer.toString(value.fadeInTicks()));
        stayField.setValue(Integer.toString(value.stayTicks()));
        fadeOutField.setValue(Integer.toString(value.fadeOutTicks()));
        loadPart();
        updateWidgets();
        status = "";
    }

    private void loadPart() {
        LocalizedTextDraft value = partIndex == 0 ? mainText : subtitleText;
        literalField.setValue(EditorTextEscapes.encodeMultiline(value.literal()));
        translationField.setValue(value.translation());
        colorField.setValue(value.color().map(RarityDraft::formatColor).orElse(""));
        bold = value.bold();
        italic = value.italic();
    }

    private boolean flushPart() {
        if (!phases[phaseIndex].editable() || !messageEnabled
                || partIndex == 1 && !subtitleEnabled) return true;
        String literal = EditorTextEscapes.decodeMultiline(literalField.getValue());
        String translation = translationField.getValue().strip();
        if (literal.isBlank() && translation.isBlank()) {
            status = Component.translatable("screen.talent_editor.presentation.text_required").getString();
            return false;
        }
        Optional<Integer> color = Optional.empty();
        String rawColor = colorField.getValue().strip();
        if (!rawColor.isBlank()) {
            Integer parsed = RarityDraft.tryParseColor(rawColor);
            if (parsed == null) {
                status = Component.translatable("screen.talent_editor.text.invalid_color").getString();
                return false;
            }
            color = Optional.of(parsed);
        }
        LocalizedTextDraft updated = new LocalizedTextDraft(literal, translation, color, bold, italic);
        if (partIndex == 0) mainText = updated;
        else subtitleText = updated;
        return true;
    }

    private boolean storePhase() {
        TalentDraft.PresentationDraft current = phases[phaseIndex];
        if (!current.editable()) return true;
        if (!flushPart()) return false;
        ResourceLocation sound = ResourceLocation.tryParse(soundField.getValue().strip());
        double volume;
        double pitch;
        int fadeIn;
        int stay;
        int fadeOut;
        try {
            volume = Double.parseDouble(volumeField.getValue().strip());
            pitch = Double.parseDouble(pitchField.getValue().strip());
            fadeIn = Integer.parseInt(fadeInField.getValue().strip());
            stay = Integer.parseInt(stayField.getValue().strip());
            fadeOut = Integer.parseInt(fadeOutField.getValue().strip());
        } catch (NumberFormatException exception) {
            status = Component.translatable("screen.talent_editor.presentation.invalid_number").getString();
            return false;
        }
        if (soundEnabled && sound == null || !Double.isFinite(volume) || volume < 0.0D || volume > 16.0D
                || !Double.isFinite(pitch) || pitch < 0.01D || pitch > 4.0D
                || fadeIn < 0 || fadeIn > 1_200 || stay < 0 || stay > 12_000
                || fadeOut < 0 || fadeOut > 1_200) {
            status = Component.translatable("screen.talent_editor.presentation.invalid_value").getString();
            return false;
        }
        String mode = MODES.get(modeIndex);
        if (messageEnabled && mainText.fallbackText().isBlank()) {
            status = Component.translatable("screen.talent_editor.presentation.text_required").getString();
            return false;
        }
        if (subtitleEnabled && !mode.equals("title")) {
            status = Component.translatable("screen.talent_editor.presentation.subtitle_requires_title").getString();
            return false;
        }
        if (subtitleEnabled && subtitleText.fallbackText().isBlank()) {
            status = Component.translatable("screen.talent_editor.presentation.text_required").getString();
            return false;
        }
        phases[phaseIndex] = new TalentDraft.PresentationDraft(
                true, soundEnabled,
                sound == null ? new ResourceLocation("minecraft", "entity.player.levelup") : sound,
                SOURCES.get(sourceIndex), volume, pitch, messageEnabled, mode, mainText,
                subtitleEnabled, subtitleText, fadeIn, stay, fadeOut
        );
        status = "";
        return true;
    }

    private void updateWidgets() {
        if (phaseButton == null) return;
        boolean editable = phases[phaseIndex].editable();
        String phase = phaseIndex == 0 ? "acquire" : "synthesize";
        phaseButton.setMessage(Component.translatable("screen.talent_editor.presentation.phase." + phase));
        soundToggle.setMessage(Component.translatable("screen.talent_editor.presentation.sound_enabled",
                Component.translatable("options." + (soundEnabled ? "on" : "off"))));
        sourceButton.setMessage(Component.translatable("screen.talent_editor.presentation.sound_source",
                SOURCES.get(sourceIndex)));
        messageToggle.setMessage(Component.translatable("screen.talent_editor.presentation.message_enabled",
                Component.translatable("options." + (messageEnabled ? "on" : "off"))));
        modeButton.setMessage(Component.translatable("screen.talent_editor.presentation.mode." + MODES.get(modeIndex)));
        partButton.setMessage(Component.translatable("screen.talent_editor.presentation.part."
                + (partIndex == 0 ? "main" : "subtitle")));
        subtitleToggle.setMessage(Component.translatable("screen.talent_editor.presentation.subtitle_enabled",
                Component.translatable("options." + (subtitleEnabled ? "on" : "off"))));
        boldButton.setMessage(Component.translatable("screen.talent_editor.presentation.bold",
                Component.translatable("options." + (bold ? "on" : "off"))));
        italicButton.setMessage(Component.translatable("screen.talent_editor.presentation.italic",
                Component.translatable("options." + (italic ? "on" : "off"))));

        boolean soundPage = sectionIndex == 0;
        boolean textPage = sectionIndex == 1;
        boolean timingPage = sectionIndex == 2;
        soundToggle.visible = soundPage;
        sourceButton.visible = soundPage;
        chooseSoundButton.visible = soundPage;
        soundField.visible = soundPage;
        volumeField.visible = soundPage;
        pitchField.visible = soundPage;
        boolean soundActive = editable && soundEnabled;
        soundToggle.active = editable;
        sourceButton.active = soundActive;
        chooseSoundButton.active = soundActive;
        soundField.setEditable(soundActive);
        volumeField.setEditable(soundActive);
        pitchField.setEditable(soundActive);
        messageToggle.visible = textPage;
        modeButton.visible = textPage;
        partButton.visible = textPage;
        subtitleToggle.visible = textPage;
        literalField.visible = textPage;
        translationField.visible = textPage;
        colorField.visible = textPage;
        boldButton.visible = textPage;
        italicButton.visible = textPage;
        boolean messageActive = editable && messageEnabled;
        messageToggle.active = editable;
        modeButton.active = messageActive;
        boolean title = MODES.get(modeIndex).equals("title");
        partButton.active = messageActive && title;
        subtitleToggle.active = messageActive && title;
        boolean textActive = messageActive && (partIndex == 0 || subtitleEnabled && title);
        literalField.setEditable(textActive);
        translationField.setEditable(textActive);
        colorField.setEditable(textActive);
        boldButton.active = textActive;
        italicButton.active = textActive;
        previewButton.active = editable;
        fadeInField.visible = timingPage;
        stayField.visible = timingPage;
        fadeOutField.visible = timingPage;
        fadeInField.setEditable(messageActive && title);
        stayField.setEditable(messageActive && title);
        fadeOutField.setEditable(messageActive && title);
    }

    private void chooseSound() {
        if (minecraft == null || !storePhase()) return;
        minecraft.setScreen(new ResourceReferencePickerScreen(
                this, ResourceReferencePickerScreen.Source.SOUND_EVENT, bootstrap, this::replaceSound
        ));
    }

    private void replaceSound(ResourceLocation sound) {
        TalentDraft.PresentationDraft value = phases[phaseIndex];
        phases[phaseIndex] = new TalentDraft.PresentationDraft(
                value.editable(), value.soundEnabled(), sound, value.soundSource(), value.volume(), value.pitch(),
                value.messageEnabled(), value.mode(), value.text(), value.subtitleEnabled(), value.subtitle(),
                value.fadeInTicks(), value.stayTicks(), value.fadeOutTicks()
        );
    }

    private void apply() {
        if (!storePhase()) return;
        parent.updatePresentations(phases[0], phases[1]);
        if (minecraft != null) minecraft.setScreen(parent);
    }

    private void preview() {
        if (!storePhase() || minecraft == null || !phases[phaseIndex].editable()) return;
        minecraft.setScreen(new TalentPresentationPreviewScreen(this, phases[phaseIndex]));
    }

    private void returnWithoutApplying() {
        if (minecraft != null) minecraft.setScreen(parent);
    }

    @Override
    public void onClose() {
        returnWithoutApplying();
    }
}
