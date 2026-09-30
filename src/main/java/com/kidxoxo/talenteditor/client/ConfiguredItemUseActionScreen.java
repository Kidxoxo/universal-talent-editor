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

public final class ConfiguredItemUseActionScreen extends Screen {
    private static final List<String> SOUND_SOURCES = List.of(
            "master", "music", "records", "weather", "blocks",
            "hostile", "neutral", "players", "ambient", "voice"
    );
    private static final List<String> MESSAGE_MODES = List.of("chat", "action_bar", "title");

    private final ConfiguredItemUseScreen parent;
    private final EditorBootstrap bootstrap;
    private ConfiguredItemDraft.UseActionDraft draft;
    private int soundSourceIndex;
    private int messageModeIndex;
    private int partIndex;
    private boolean ambient;
    private boolean particles;
    private boolean icon;
    private boolean subtitleEnabled;
    private boolean bold;
    private boolean italic;
    private LocalizedTextDraft mainText;
    private LocalizedTextDraft subtitleText;
    private Button typeButton;
    private EditBox resourceField;
    private Button chooseResourceButton;
    private EditBox durationField;
    private EditBox amplifierField;
    private Button ambientButton;
    private Button particlesButton;
    private Button iconButton;
    private Button soundSourceButton;
    private EditBox volumeField;
    private EditBox pitchField;
    private Button messageModeButton;
    private Button partButton;
    private Button subtitleButton;
    private EditBox literalField;
    private EditBox translationField;
    private EditBox colorField;
    private Button boldButton;
    private Button italicButton;
    private EditBox fadeInField;
    private EditBox stayField;
    private EditBox fadeOutField;
    private final EditorOptionPopup options = new EditorOptionPopup();
    private String status = "";

    ConfiguredItemUseActionScreen(
            ConfiguredItemUseScreen parent,
            EditorBootstrap bootstrap,
            ConfiguredItemDraft.UseActionDraft draft
    ) {
        super(Component.translatable("screen.talent_editor.configured_item.use.action.title"));
        this.parent = parent;
        this.bootstrap = bootstrap;
        this.draft = draft;
        this.mainText = LocalizedTextDraft.literal("");
        this.subtitleText = LocalizedTextDraft.literal("");
    }

    @Override
    protected void init() {
        int formWidth = Math.min(680, width - 32);
        int x = (width - formWidth) / 2;
        int gap = 6;
        int half = (formWidth - gap) / 2;
        int third = (formWidth - gap * 2) / 3;
        typeButton = addRenderableWidget(Button.builder(Component.empty(), button -> chooseType())
                .bounds(x, 32, formWidth, 20).build());
        resourceField = field(x, 58, formWidth - 92, 256);
        chooseResourceButton = addRenderableWidget(Button.builder(Component.translatable(
                        "screen.talent_editor.configured_item.use.action.choose"), button -> chooseResource())
                .bounds(x + formWidth - 86, 58, 86, 20).build());
        durationField = field(x, 84, half, 8);
        amplifierField = field(x + half + gap, 84, formWidth - half - gap, 3);
        ambientButton = addRenderableWidget(Button.builder(Component.empty(), button -> {
            ambient = !ambient;
            updateWidgets();
        }).bounds(x, 110, third, 20).build());
        particlesButton = addRenderableWidget(Button.builder(Component.empty(), button -> {
            particles = !particles;
            updateWidgets();
        }).bounds(x + third + gap, 110, third, 20).build());
        iconButton = addRenderableWidget(Button.builder(Component.empty(), button -> {
            icon = !icon;
            updateWidgets();
        }).bounds(x + (third + gap) * 2, 110, formWidth - (third + gap) * 2, 20).build());
        soundSourceButton = addRenderableWidget(Button.builder(Component.empty(), button -> chooseSoundSource())
                .bounds(x, 84, formWidth, 20).build());
        volumeField = field(x, 110, half, 16);
        pitchField = field(x + half + gap, 110, formWidth - half - gap, 16);

        messageModeButton = addRenderableWidget(Button.builder(Component.empty(), button -> chooseMessageMode())
                .bounds(x, 58, formWidth, 20).build());
        partButton = addRenderableWidget(Button.builder(Component.empty(), button -> cyclePart())
                .bounds(x, 84, half, 20).build());
        subtitleButton = addRenderableWidget(Button.builder(Component.empty(), button -> toggleSubtitle())
                .bounds(x + half + gap, 84, formWidth - half - gap, 20).build());
        literalField = field(x, 110, formWidth, 1_024);
        literalField.setHint(Component.translatable("screen.talent_editor.multiline_hint"));
        translationField = field(x, 136, formWidth, 1_024);
        colorField = field(x, 162, third, 9);
        colorField.setHint(Component.translatable("screen.talent_editor.text.color_hint"));
        boldButton = addRenderableWidget(Button.builder(Component.empty(), button -> {
            bold = !bold;
            updateWidgets();
        }).bounds(x + third + gap, 162, third, 20).build());
        italicButton = addRenderableWidget(Button.builder(Component.empty(), button -> {
            italic = !italic;
            updateWidgets();
        }).bounds(x + (third + gap) * 2, 162, formWidth - (third + gap) * 2, 20).build());
        fadeInField = field(x, 188, third, 8);
        stayField = field(x + third + gap, 188, third, 8);
        fadeOutField = field(x + (third + gap) * 2, 188,
                formWidth - (third + gap) * 2, 8);

        addRenderableWidget(Button.builder(Component.translatable("gui.cancel"), button -> returnParent())
                .bounds(x, height - 28, half, 20).build());
        addRenderableWidget(Button.builder(Component.translatable(
                        "screen.talent_editor.configured_item.use.action.apply"), button -> apply())
                .bounds(x + half + gap, height - 28, formWidth - half - gap, 20).build());
        loadDraft();
    }

    private EditBox field(int x, int y, int width, int maxLength) {
        EditBox field = new EditBox(font, x, y, width, 20, Component.empty());
        field.setMaxLength(maxLength);
        field.setResponder(ignored -> status = "");
        addRenderableWidget(field);
        return field;
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics);
        int formWidth = Math.min(680, width - 32);
        int x = (width - formWidth) / 2;
        EditorTheme.panel(graphics, x - 7, 27, formWidth + 14, Math.min(183, height - 64));
        super.render(graphics, mouseX, mouseY, partialTick);
        graphics.drawCenteredString(font, title, width / 2, 10, 0xFFFFFF);
        switch (draft.type()) {
            case ADD_EFFECT -> {
                label(graphics, "effect", x, 48);
                label(graphics, "duration", x, 74);
                label(graphics, "amplifier", x + (formWidth - 6) / 2 + 6, 74);
            }
            case REMOVE_EFFECT -> label(graphics, "effect", x, 48);
            case PLAY_SOUND -> {
                label(graphics, "sound", x, 48);
                label(graphics, "volume", x, 100);
                label(graphics, "pitch", x + (formWidth - 6) / 2 + 6, 100);
            }
            case SHOW_MESSAGE -> {
                label(graphics, "literal", x, 100);
                label(graphics, "translation", x, 126);
                label(graphics, "color_style", x, 152);
                if (MESSAGE_MODES.get(messageModeIndex).equals("title")) {
                    label(graphics, "title_times", x, 178);
                }
            }
        }
        if (!status.isBlank()) {
            graphics.drawString(font, font.plainSubstrByWidth(status, formWidth),
                    x, 20, 0xFF7777, false);
        }
        options.render(graphics, font, width, height);
    }

    @Override public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (options.mouseClicked(mouseX, mouseY, button, width, height)) return true;
        return super.mouseClicked(mouseX, mouseY, button);
    }
    @Override public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        if (options.mouseScrolled(delta, height)) return true;
        return super.mouseScrolled(mouseX, mouseY, delta);
    }
    @Override public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (options.keyPressed(keyCode, height)) return true;
        return super.keyPressed(keyCode, scanCode, modifiers);
    }
    @Override public boolean charTyped(char codePoint, int modifiers) {
        return options.isOpen() || super.charTyped(codePoint, modifiers);
    }

    private void label(GuiGraphics graphics, String suffix, int x, int y) {
        graphics.drawString(font, Component.translatable(
                "screen.talent_editor.configured_item.use.action." + suffix), x, y, 0xA0A0A0, false);
    }

    private void loadDraft() {
        if (draft.type() == ConfiguredItemDraft.UseActionType.ADD_EFFECT
                || draft.type() == ConfiguredItemDraft.UseActionType.REMOVE_EFFECT) {
            resourceField.setValue(draft.effect().toString());
            durationField.setValue(Integer.toString(draft.durationTicks()));
            amplifierField.setValue(Integer.toString(draft.amplifier()));
            ambient = draft.ambient();
            particles = draft.showParticles();
            icon = draft.showIcon();
        } else if (draft.type() == ConfiguredItemDraft.UseActionType.PLAY_SOUND) {
            resourceField.setValue(draft.sound().toString());
            soundSourceIndex = Math.max(0, SOUND_SOURCES.indexOf(draft.soundSource()));
            volumeField.setValue(Double.toString(draft.volume()));
            pitchField.setValue(Double.toString(draft.pitch()));
        } else {
            messageModeIndex = Math.max(0, MESSAGE_MODES.indexOf(draft.messageMode()));
            mainText = draft.text();
            subtitleEnabled = draft.subtitleEnabled();
            subtitleText = draft.subtitle();
            fadeInField.setValue(Integer.toString(draft.fadeInTicks()));
            stayField.setValue(Integer.toString(draft.stayTicks()));
            fadeOutField.setValue(Integer.toString(draft.fadeOutTicks()));
            partIndex = 0;
            loadPart();
        }
        status = "";
        updateWidgets();
    }

    private void loadPart() {
        LocalizedTextDraft text = partIndex == 0 ? mainText : subtitleText;
        literalField.setValue(EditorTextEscapes.encodeMultiline(text.literal()));
        translationField.setValue(text.translation());
        colorField.setValue(text.color().map(RarityDraft::formatColor).orElse(""));
        bold = text.bold();
        italic = text.italic();
    }

    private boolean flushPart() {
        if (draft.type() != ConfiguredItemDraft.UseActionType.SHOW_MESSAGE
                || partIndex == 1 && !subtitleEnabled) return true;
        String literal = EditorTextEscapes.decodeMultiline(literalField.getValue());
        String translation = translationField.getValue().strip();
        if (literal.isBlank() && translation.isBlank()) {
            return invalid("screen.talent_editor.configured_item.use.action.text_required");
        }
        Optional<Integer> color = Optional.empty();
        String rawColor = colorField.getValue().strip();
        if (!rawColor.isBlank()) {
            Integer parsed = RarityDraft.tryParseColor(rawColor);
            if (parsed == null) return invalid("screen.talent_editor.text.invalid_color");
            color = Optional.of(parsed);
        }
        LocalizedTextDraft value = new LocalizedTextDraft(literal, translation, color, bold, italic);
        if (partIndex == 0) mainText = value;
        else subtitleText = value;
        return true;
    }

    private void chooseType() {
        ConfiguredItemDraft.UseActionType[] values = ConfiguredItemDraft.UseActionType.values();
        options.open(Component.translatable("screen.talent_editor.option.choose"),
                java.util.Arrays.stream(values).map(value -> Component.translatable(
                        "screen.talent_editor.configured_item.use.action." + value.serializedName())).toList(),
                draft.type().ordinal(), index -> selectType(values[index]), height);
    }

    private void selectType(ConfiguredItemDraft.UseActionType next) {
        if (next == draft.type()) return;
        draft = ConfiguredItemDraft.UseActionDraft.defaults(next);
        loadDraft();
    }

    private void chooseSoundSource() {
        options.open(Component.translatable("screen.talent_editor.option.choose"),
                SOUND_SOURCES.stream().map(Component::literal).toList(),
                soundSourceIndex, index -> { soundSourceIndex = index; updateWidgets(); }, height);
    }

    private void chooseMessageMode() {
        if (!flushPart()) return;
        options.open(Component.translatable("screen.talent_editor.option.choose"),
                MESSAGE_MODES.stream().map(value -> Component.translatable(
                        "screen.talent_editor.configured_item.use.action.mode." + value)).toList(),
                messageModeIndex, this::selectMessageMode, height);
    }

    private void selectMessageMode(int index) {
        messageModeIndex = index;
        if (!MESSAGE_MODES.get(messageModeIndex).equals("title")) {
            subtitleEnabled = false;
            partIndex = 0;
            loadPart();
        }
        updateWidgets();
    }

    private void cyclePart() {
        if (!flushPart()) return;
        partIndex = (partIndex + 1) % 2;
        loadPart();
        updateWidgets();
    }

    private void toggleSubtitle() {
        if (subtitleEnabled && partIndex == 1 && !flushPart()) return;
        subtitleEnabled = !subtitleEnabled;
        if (!subtitleEnabled && partIndex == 1) {
            partIndex = 0;
            loadPart();
        }
        updateWidgets();
    }

    private void chooseResource() {
        if (minecraft == null) return;
        ResourceReferencePickerScreen.Source source = draft.type() == ConfiguredItemDraft.UseActionType.PLAY_SOUND
                ? ResourceReferencePickerScreen.Source.SOUND_EVENT
                : ResourceReferencePickerScreen.Source.MOB_EFFECT;
        minecraft.setScreen(new ResourceReferencePickerScreen(this, source, bootstrap,
                id -> resourceField.setValue(id.toString())));
    }

    private void updateWidgets() {
        if (typeButton == null) return;
        ConfiguredItemDraft.UseActionType type = draft.type();
        typeButton.setMessage(Component.translatable(
                "screen.talent_editor.configured_item.use.action." + type.serializedName()
        ));
        boolean effect = type == ConfiguredItemDraft.UseActionType.ADD_EFFECT
                || type == ConfiguredItemDraft.UseActionType.REMOVE_EFFECT;
        boolean add = type == ConfiguredItemDraft.UseActionType.ADD_EFFECT;
        boolean sound = type == ConfiguredItemDraft.UseActionType.PLAY_SOUND;
        boolean message = type == ConfiguredItemDraft.UseActionType.SHOW_MESSAGE;
        resourceField.visible = effect || sound;
        resourceField.setEditable(effect || sound);
        chooseResourceButton.visible = effect || sound;
        durationField.visible = add;
        amplifierField.visible = add;
        durationField.setEditable(add);
        amplifierField.setEditable(add);
        ambientButton.visible = add;
        particlesButton.visible = add;
        iconButton.visible = add;
        ambientButton.setMessage(toggleLabel("ambient", ambient));
        particlesButton.setMessage(toggleLabel("particles", particles));
        iconButton.setMessage(toggleLabel("icon", icon));
        soundSourceButton.visible = sound;
        soundSourceButton.setMessage(Component.translatable(
                "screen.talent_editor.configured_item.use.action.source", SOUND_SOURCES.get(soundSourceIndex)
        ));
        volumeField.visible = sound;
        pitchField.visible = sound;
        volumeField.setEditable(sound);
        pitchField.setEditable(sound);
        messageModeButton.visible = message;
        messageModeButton.setMessage(Component.translatable(
                "screen.talent_editor.configured_item.use.action.mode." + MESSAGE_MODES.get(messageModeIndex)
        ));
        boolean title = message && MESSAGE_MODES.get(messageModeIndex).equals("title");
        partButton.visible = message;
        partButton.active = title;
        partButton.setMessage(Component.translatable("screen.talent_editor.configured_item.use.action.part."
                + (partIndex == 0 ? "main" : "subtitle")));
        subtitleButton.visible = message;
        subtitleButton.active = title;
        subtitleButton.setMessage(toggleLabel("subtitle", subtitleEnabled));
        boolean textActive = message && (partIndex == 0 || subtitleEnabled && title);
        literalField.visible = message;
        translationField.visible = message;
        colorField.visible = message;
        boldButton.visible = message;
        italicButton.visible = message;
        literalField.setEditable(textActive);
        translationField.setEditable(textActive);
        colorField.setEditable(textActive);
        boldButton.active = textActive;
        italicButton.active = textActive;
        boldButton.setMessage(toggleLabel("bold", bold));
        italicButton.setMessage(toggleLabel("italic", italic));
        fadeInField.visible = title;
        stayField.visible = title;
        fadeOutField.visible = title;
        fadeInField.setEditable(title);
        stayField.setEditable(title);
        fadeOutField.setEditable(title);
    }

    private Component toggleLabel(String key, boolean value) {
        return Component.translatable("screen.talent_editor.configured_item.use.action." + key,
                Component.translatable("options." + (value ? "on" : "off")));
    }

    private void apply() {
        ResourceLocation resource;
        try {
            switch (draft.type()) {
                case ADD_EFFECT, REMOVE_EFFECT -> {
                    resource = parseResource("effect");
                    if (resource == null) return;
                    int duration = draft.type() == ConfiguredItemDraft.UseActionType.ADD_EFFECT
                            ? Integer.parseInt(durationField.getValue().strip()) : 100;
                    int amplifier = draft.type() == ConfiguredItemDraft.UseActionType.ADD_EFFECT
                            ? Integer.parseInt(amplifierField.getValue().strip()) : 0;
                    if (duration < 1 || duration > 72_000 || amplifier < 0 || amplifier > 255) {
                        invalid("screen.talent_editor.configured_item.use.action.invalid_effect_numbers");
                        return;
                    }
                    draft = ConfiguredItemDraft.UseActionDraft.effectAction(
                            draft.type(), resource, duration, amplifier, ambient, particles, icon
                    );
                }
                case PLAY_SOUND -> {
                    resource = parseResource("sound");
                    if (resource == null) return;
                    double volume = Double.parseDouble(volumeField.getValue().strip());
                    double pitch = Double.parseDouble(pitchField.getValue().strip());
                    if (!Double.isFinite(volume) || volume < 0.0D || volume > 16.0D
                            || !Double.isFinite(pitch) || pitch < 0.01D || pitch > 4.0D) {
                        invalid("screen.talent_editor.configured_item.use.action.invalid_sound_numbers");
                        return;
                    }
                    draft = ConfiguredItemDraft.UseActionDraft.soundAction(
                            resource, SOUND_SOURCES.get(soundSourceIndex), volume, pitch
                    );
                }
                case SHOW_MESSAGE -> {
                    if (!flushPart()) return;
                    int fadeIn = Integer.parseInt(fadeInField.getValue().strip());
                    int stay = Integer.parseInt(stayField.getValue().strip());
                    int fadeOut = Integer.parseInt(fadeOutField.getValue().strip());
                    if (fadeIn < 0 || fadeIn > 1_200 || stay < 0 || stay > 12_000
                            || fadeOut < 0 || fadeOut > 1_200) {
                        invalid("screen.talent_editor.configured_item.use.action.invalid_title_times");
                        return;
                    }
                    draft = ConfiguredItemDraft.UseActionDraft.messageAction(
                            MESSAGE_MODES.get(messageModeIndex), mainText, subtitleEnabled, subtitleText,
                            fadeIn, stay, fadeOut
                    );
                }
            }
        } catch (NumberFormatException ignored) {
            invalid("screen.talent_editor.configured_item.use.action.invalid_numbers");
            return;
        }
        parent.replaceSelected(draft);
        returnParent();
    }

    private ResourceLocation parseResource(String field) {
        String raw = resourceField.getValue().strip();
        ResourceLocation parsed = ResourceLocation.tryParse(raw);
        if (parsed == null || !raw.contains(":")) {
            invalid("screen.talent_editor.configured_item.use.action.invalid_" + field);
            return null;
        }
        return parsed;
    }

    private boolean invalid(String key) {
        status = Component.translatable(key).getString();
        return false;
    }

    private void returnParent() {
        if (minecraft != null) minecraft.setScreen(parent);
    }

    @Override
    public void onClose() {
        returnParent();
    }
}
