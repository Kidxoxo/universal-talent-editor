package com.kidxoxo.talenteditor.client;

import com.google.gson.JsonObject;
import com.kidxoxo.talenteditor.TalentEditorMod;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import java.util.List;
import java.util.Optional;

/** Edits one independently timed sound, chat/action-bar message, or title cue. */
final class TalentPresentationCueScreen extends EditorScreen {
    private static final List<String> SOURCES = List.of("master", "music", "records", "weather", "blocks",
            "hostile", "neutral", "players", "ambient", "voice");
    private static final List<String> MODES = List.of("chat", "action_bar", "title");
    private final Screen parent;
    private final TalentEditScreen owner;
    private final TalentDraft.AdvancedActionLocation location;
    private final int actionIndex;
    private final String type;
    private JsonObject draft;
    private final EditorOptionPopup options = new EditorOptionPopup();
    private EditBox atTick, sound, volume, pitch, literal, translation, subtitle, color, fadeIn, stay, fadeOut;
    private Button modeButton, sourceButton, boldButton, italicButton, textTab, timingTab;
    private int sourceIndex, modeIndex, section;
    private boolean bold, italic;
    private String status = "";

    TalentPresentationCueScreen(Screen parent, TalentEditScreen owner,
                                TalentDraft.AdvancedActionLocation location, int actionIndex, String type) {
        super(Component.translatable("screen.talent_editor.presentation.cue." + type));
        this.parent = parent;
        this.owner = owner;
        this.location = location;
        this.actionIndex = actionIndex;
        this.type = type;
        draft = actionIndex < 0 ? new JsonObject() : owner.draft().advancedAction(location, -1, actionIndex);
        sourceIndex = Math.max(0, SOURCES.indexOf(value("source", "players")));
        modeIndex = Math.max(0, MODES.indexOf(value("mode", "chat")));
        LocalizedTextDraft text = LocalizedTextDraft.parse(draft.get("text"));
        bold = text.bold();
        italic = text.italic();
    }

    @Override protected void init() {
        int w = Math.min(680, width - 32), x = (width - w) / 2, half = (w - 8) / 2;
        atTick = field(x, 52, half, value("atTick", "0"), 8);
        if (type.equals("play_sound")) {
            sound = field(x, 108, w - 96, value("sound", "minecraft:entity.player.levelup"), 256);
            addRenderableWidget(EditorWidgets.button(Component.translatable("screen.talent_editor.presentation.choose_sound"),
                    button -> chooseSound()).bounds(x + w - 90, 108, 90, 20).build());
            sourceButton = addRenderableWidget(EditorWidgets.button(Component.empty(), button -> {
                options.open(Component.translatable("screen.talent_editor.option.choose"),
                        SOURCES.stream().map(Component::literal).toList(), sourceIndex, index -> {
                            sourceIndex = index;
                            updateButtons();
                        }, height);
            }).bounds(x, 140, w, 20).build());
            volume = field(x, 172, half, value("volume", "1"), 16);
            pitch = field(x + half + 8, 172, half, value("pitch", "1"), 16);
        } else {
            textTab = addRenderableWidget(EditorWidgets.button(Component.translatable(
                    "screen.talent_editor.presentation.section.text"), button -> switchSection(0))
                    .bounds(x, 72, half, 20).build());
            timingTab = addRenderableWidget(EditorWidgets.button(Component.translatable(
                    "screen.talent_editor.presentation.section.timing"), button -> switchSection(1))
                    .bounds(x + half + 8, 72, half, 20).build());
            modeButton = addRenderableWidget(EditorWidgets.button(Component.empty(), button -> {
                options.open(Component.translatable("screen.talent_editor.option.choose"),
                        MODES.stream().map(value -> Component.translatable(
                                "screen.talent_editor.presentation.mode." + value)).toList(),
                        modeIndex, index -> {
                            modeIndex = index;
                            updateButtons();
                        }, height);
            }).bounds(x, 96, w, 20).build());
            LocalizedTextDraft text = LocalizedTextDraft.parse(draft.get("text"));
            literal = field(x, 120, w, EditorTextEscapes.encodeMultiline(text.literal()), 1024);
            literal.setHint(Component.translatable("screen.talent_editor.presentation.main_text"));
            translation = field(x, 144, w, text.translation(), 1024);
            translation.setHint(Component.translatable("screen.talent_editor.presentation.translation"));
            LocalizedTextDraft subtitleValue = LocalizedTextDraft.parse(draft.get("subtitle"));
            subtitle = field(x, 168, w, subtitleValue.translation().isBlank()
                    ? EditorTextEscapes.encodeMultiline(subtitleValue.literal())
                    : "@" + subtitleValue.translation(), 1024);
            subtitle.setHint(Component.translatable("screen.talent_editor.presentation.subtitle_text"));
            color = field(x, 192, (w - 16) / 3, text.color().map(RarityDraft::formatColor).orElse(""), 9);
            color.setHint(Component.translatable("screen.talent_editor.text.color_hint"));
            int third = (w - 16) / 3;
            boldButton = addRenderableWidget(EditorWidgets.button(Component.empty(), button -> {
                bold = !bold; updateButtons();
            }).bounds(x + third + 8, 192, third, 20).build());
            italicButton = addRenderableWidget(EditorWidgets.button(Component.empty(), button -> {
                italic = !italic; updateButtons();
            }).bounds(x + (third + 8) * 2, 192, w - (third + 8) * 2, 20).build());
            fadeIn = field(x, 108, (w - 16) / 3, value("fadeInTicks", "10"), 8);
            stay = field(x + (w - 16) / 3 + 8, 108, (w - 16) / 3, value("stayTicks", "70"), 8);
            fadeOut = field(x + ((w - 16) / 3 + 8) * 2, 108,
                    w - ((w - 16) / 3 + 8) * 2, value("fadeOutTicks", "20"), 8);
        }
        addRenderableWidget(EditorWidgets.button(Component.translatable("gui.cancel"), button -> close())
                .bounds(x, height - 28, half, 20).build());
        addRenderableWidget(EditorWidgets.button(Component.translatable("gui.done"), button -> save())
                .bounds(x + half + 8, height - 28, half, 20).build());
        updateButtons();
    }

    private EditBox field(int x, int y, int width, String initial, int maxLength) {
        EditBox box = new EditBox(font, x, y, width, 20, Component.empty());
        box.setMaxLength(maxLength);
        box.setValue(initial);
        box.setResponder(value -> status = "");
        addRenderableWidget(box);
        return box;
    }

    private void updateButtons() {
        if (sourceButton != null) sourceButton.setMessage(Component.translatable(
                "screen.talent_editor.presentation.sound_source", SOURCES.get(sourceIndex)));
        if (modeButton == null) return;
        modeButton.setMessage(Component.translatable("screen.talent_editor.presentation.mode." + MODES.get(modeIndex)));
        boldButton.setMessage(Component.translatable("screen.talent_editor.presentation.bold",
                Component.translatable("options." + (bold ? "on" : "off"))));
        italicButton.setMessage(Component.translatable("screen.talent_editor.presentation.italic",
                Component.translatable("options." + (italic ? "on" : "off"))));
        boolean textPage = section == 0;
        modeButton.visible = textPage;
        literal.visible = textPage;
        translation.visible = textPage;
        subtitle.visible = textPage && MODES.get(modeIndex).equals("title");
        color.visible = textPage;
        boldButton.visible = textPage;
        italicButton.visible = textPage;
        boolean titleMode = MODES.get(modeIndex).equals("title");
        boolean chatMode = MODES.get(modeIndex).equals("chat");
        fadeIn.visible = !textPage && titleMode;
        stay.visible = !textPage && (titleMode || chatMode);
        fadeOut.visible = !textPage && titleMode;
        int w = Math.min(680, width - 32), x = (width - w) / 2;
        stay.setX(chatMode ? x : x + (w - 16) / 3 + 8);
        stay.setWidth(chatMode ? w : (w - 16) / 3);
    }

    private void switchSection(int next) { section = next; updateButtons(); }

    private void chooseSound() {
        if (minecraft == null) return;
        try { draft = serialize(); }
        catch (RuntimeException ignored) { return; }
        minecraft.setScreen(new ResourceReferencePickerScreen(this,
                ResourceReferencePickerScreen.Source.SOUND_EVENT, owner.bootstrap(), id -> {
            draft.addProperty("sound", id.toString());
            sound.setValue(id.toString());
        }));
    }

    private void save() {
        try {
            owner.draft().putAdvancedAction(location, -1, actionIndex, serialize());
            ClientEditorState.clearValidation();
            close();
        } catch (RuntimeException exception) {
            status = Component.translatable("screen.talent_editor.presentation.invalid_value").getString();
        }
    }

    private JsonObject serialize() {
        JsonObject result = draft.deepCopy();
        result.addProperty("type", TalentEditorMod.MOD_ID + ":" + type);
        result.addProperty("atTick", integer(atTick, 0, 2400));
        if (type.equals("play_sound")) {
            ResourceLocation id = ResourceLocation.tryParse(sound.getValue().strip());
            if (id == null || !sound.getValue().contains(":")) throw new IllegalArgumentException("sound");
            result.addProperty("sound", id.toString());
            result.addProperty("source", SOURCES.get(sourceIndex));
            result.addProperty("volume", decimal(volume, 0, 16));
            result.addProperty("pitch", decimal(pitch, 0.01, 4));
        } else {
            LocalizedTextDraft text = new LocalizedTextDraft(EditorTextEscapes.decodeMultiline(literal.getValue()),
                    translation.getValue().strip(), parseColor(), bold, italic);
            if (text.fallbackText().isBlank()) throw new IllegalArgumentException("text");
            String mode = MODES.get(modeIndex);
            result.addProperty("mode", mode);
            result.add("text", text.toJson());
            String sub = subtitle.getValue().strip();
            if (mode.equals("title") && !sub.isBlank()) result.add("subtitle",
                    (sub.startsWith("@") ? new LocalizedTextDraft("", sub.substring(1), Optional.empty(), false, false)
                            : LocalizedTextDraft.literal(EditorTextEscapes.decodeMultiline(sub))).toJson());
            else result.remove("subtitle");
            result.addProperty("fadeInTicks", integer(fadeIn, 0, 1200));
            result.addProperty("stayTicks", integer(stay, 0, 12000));
            result.addProperty("fadeOutTicks", integer(fadeOut, 0, 1200));
        }
        return result;
    }

    private Optional<Integer> parseColor() {
        String raw = color.getValue().strip();
        if (raw.isBlank()) return Optional.empty();
        Integer parsed = RarityDraft.tryParseColor(raw);
        if (parsed == null) throw new IllegalArgumentException("color");
        return Optional.of(parsed);
    }

    private int integer(EditBox box, int min, int max) {
        int value = Integer.parseInt(box.getValue().strip());
        if (value < min || value > max) throw new IllegalArgumentException("range");
        return value;
    }

    private double decimal(EditBox box, double min, double max) {
        double value = Double.parseDouble(box.getValue().strip());
        if (!Double.isFinite(value) || value < min || value > max) throw new IllegalArgumentException("range");
        return value;
    }

    private String value(String key, String fallback) {
        try { return draft.has(key) ? draft.get(key).getAsString() : fallback; }
        catch (RuntimeException ignored) { return fallback; }
    }

    @Override public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics);
        int w = Math.min(680, width - 32), x = (width - w) / 2;
        EditorTheme.panel(graphics, x - 7, 24, w + 14, Math.min(height - 56, 230));
        super.render(graphics, mouseX, mouseY, partialTick);
        EditorTheme.title(graphics,font,title,width);
        label(graphics, x, 40, "cue_at_tick");
        if (type.equals("play_sound")) {
            label(graphics, x, 96, "sound");
            label(graphics, x, 160, "volume_pitch");
        } else if (section == 0) {
            if (height > 255) graphics.drawString(font, Component.translatable(
                    "screen.talent_editor.presentation.cue_text_hint"), x, 218, EditorTheme.MUTED, false);
        } else {
            String mode = MODES.get(modeIndex);
            label(graphics, x, 96, mode.equals("title") ? "title_times"
                    : mode.equals("chat") ? "chat_hold" : "action_bar_hold");
            label(graphics, x, 148, mode.equals("chat") ? "chat_hold_help" : "timing_help");
        }
        if (!status.isBlank()) graphics.drawString(font, font.plainSubstrByWidth(status, w),
                x, 27, 0xFFFF7777, false);
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

    private void label(GuiGraphics graphics, int x, int y, String key) {
        graphics.drawString(font, Component.translatable("screen.talent_editor.presentation." + key),
                x, y, EditorTheme.MUTED, false);
    }

    private void close() { if (minecraft != null) minecraft.setScreen(parent); }
    @Override public void onClose() { close(); }
}
