package com.kidxoxo.talenteditor.client;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public final class TalentTextStyleScreen extends Screen {
    private static final int ROW_HEIGHT = 18;
    private static final int MAX_DESCRIPTIONS = 64;

    private final TalentEditScreen parent;
    private final ArrayList<LocalizedTextDraft> entries = new ArrayList<>();
    private int selectedIndex;
    private int scroll;
    private EditBox literalField;
    private EditBox translationField;
    private EditBox colorField;
    private Button boldButton;
    private Button italicButton;
    private Button removeButton;
    private boolean bold;
    private boolean italic;
    private String status = "";

    public TalentTextStyleScreen(TalentEditScreen parent) {
        super(Component.translatable("screen.talent_editor.text.title"));
        this.parent = parent;
        TalentDraft.TextStylesDraft source = parent.textStyles();
        entries.add(source.name());
        entries.addAll(source.descriptions());
    }

    @Override
    protected void init() {
        int divider = divider();
        int x = divider + 12;
        int formWidth = Math.max(80, width - x - 14);
        literalField = field(x, 48, formWidth, 1_024);
        translationField = field(x, 81, formWidth, 1_024);
        colorField = field(x, 114, formWidth, 9);
        colorField.setHint(Component.translatable("screen.talent_editor.text.color_hint"));

        int gap = 6;
        int toggleWidth = (formWidth - gap) / 2;
        boldButton = addRenderableWidget(Button.builder(boldLabel(), button -> {
            bold = !bold;
            button.setMessage(boldLabel());
        }).bounds(x, 142, toggleWidth, 20).build());
        italicButton = addRenderableWidget(Button.builder(italicLabel(), button -> {
            italic = !italic;
            button.setMessage(italicLabel());
        }).bounds(x + toggleWidth + gap, 142, formWidth - toggleWidth - gap, 20).build());

        int listButtonWidth = Math.max(44, (divider - 28) / 2);
        addRenderableWidget(Button.builder(Component.translatable("screen.talent_editor.text.add_description"),
                        button -> addDescription())
                .bounds(12, height - 55, listButtonWidth, 20).build());
        removeButton = addRenderableWidget(Button.builder(
                        Component.translatable("screen.talent_editor.text.remove_description"),
                        button -> removeDescription())
                .bounds(18 + listButtonWidth, height - 55, divider - listButtonWidth - 30, 20).build());

        int bottomWidth = Math.max(64, (formWidth - gap) / 2);
        addRenderableWidget(Button.builder(Component.translatable("gui.back"), button -> returnToParent())
                .bounds(x, height - 28, bottomWidth, 20).build());
        addRenderableWidget(Button.builder(Component.translatable("screen.talent_editor.text.apply"),
                        button -> apply())
                .bounds(x + bottomWidth + gap, height - 28,
                        formWidth - bottomWidth - gap, 20).build());
        loadSelection();
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
        super.render(graphics, mouseX, mouseY, partialTick);
        int divider = divider();
        int x = divider + 12;
        int formWidth = Math.max(80, width - x - 14);
        graphics.fill(divider, 32, divider + 1, height - 36, 0x60808080);
        graphics.drawCenteredString(font, title, width / 2, 10, 0xFFFFFF);
        graphics.drawString(font, Component.translatable("screen.talent_editor.text.parts"),
                12, 34, 0xA0A0A0, false);
        renderParts(graphics, mouseX, mouseY, divider);
        label(graphics, "screen.talent_editor.text.literal", x, 37);
        label(graphics, "screen.talent_editor.text.translation", x, 70);
        label(graphics, "screen.talent_editor.text.color", x, 103);
        renderPreview(graphics, x, formWidth);
        if (!status.isBlank()) {
            graphics.drawString(font, font.plainSubstrByWidth(status, formWidth),
                    x, height - 47, 0xFF7777, false);
        }
    }

    private void renderParts(GuiGraphics graphics, int mouseX, int mouseY, int divider) {
        int top = 51;
        int rows = Math.max(1, (height - top - 65) / ROW_HEIGHT);
        scroll = Math.max(0, Math.min(scroll, Math.max(0, entries.size() - rows)));
        int end = Math.min(entries.size(), scroll + rows);
        for (int index = scroll; index < end; index++) {
            int y = top + (index - scroll) * ROW_HEIGHT;
            boolean selected = selectedIndex == index;
            boolean hovered = mouseX >= 9 && mouseX < divider - 7 && mouseY >= y - 3 && mouseY < y + 14;
            if (selected || hovered) {
                graphics.fill(9, y - 3, divider - 7, y + 14,
                        selected ? 0x7050A0E8 : 0x404A90E2);
            }
            String type = index == 0
                    ? Component.translatable("screen.talent_editor.text.name_part").getString()
                    : Component.translatable("screen.talent_editor.text.description_part", index).getString();
            String label = type + " · " + entries.get(index).fallbackText();
            graphics.drawString(font, font.plainSubstrByWidth(label, divider - 26),
                    13, y, 0xE0E0E0, false);
        }
    }

    private void renderPreview(GuiGraphics graphics, int x, int formWidth) {
        Integer parsedColor = RarityDraft.tryParseColor(colorField.getValue());
        int swatch = parsedColor == null ? 0xFF777777 : 0xFF000000 | (parsedColor & 0xFFFFFF);
        graphics.fill(x + Math.min(70, formWidth - 14), 102,
                x + Math.min(81, formWidth - 3), 111, swatch);
        String literal = literalField.getValue();
        String translation = translationField.getValue().strip();
        MutableComponent preview = !literal.isBlank()
                ? Component.literal(literal)
                : translation.isBlank()
                ? Component.translatable("screen.talent_editor.text.preview_empty")
                : Component.translatable(translation);
        int textColor = parsedColor == null ? 0xFFD0D0D0 : 0xFF000000 | (parsedColor & 0xFFFFFF);
        preview.withStyle(style -> style.withColor(textColor & 0xFFFFFF).withBold(bold).withItalic(italic));
        graphics.drawString(font, Component.translatable("screen.talent_editor.text.preview"),
                x, 170, 0xA0A0A0, false);
        graphics.drawString(font, preview, x, 184, textColor, false);
    }

    private void label(GuiGraphics graphics, String key, int x, int y) {
        graphics.drawString(font, Component.translatable(key), x, y, 0xA0A0A0, false);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0 && mouseX >= 9 && mouseX < divider() - 7 && mouseY >= 51 && mouseY < height - 65) {
            int index = scroll + (int) ((mouseY - 51) / ROW_HEIGHT);
            if (index >= 0 && index < entries.size() && index != selectedIndex && flushSelected()) {
                selectedIndex = index;
                loadSelection();
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        if (mouseX < divider()) {
            int rows = Math.max(1, (height - 51 - 65) / ROW_HEIGHT);
            scroll = Math.max(0, Math.min(
                    scroll - (int) Math.signum(delta) * 3,
                    Math.max(0, entries.size() - rows)
            ));
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, delta);
    }

    private void addDescription() {
        if (!flushSelected()) return;
        if (entries.size() - 1 >= MAX_DESCRIPTIONS) {
            status = Component.translatable("screen.talent_editor.text.too_many_descriptions").getString();
            return;
        }
        entries.add(LocalizedTextDraft.literal(""));
        selectedIndex = entries.size() - 1;
        loadSelection();
    }

    private void removeDescription() {
        if (selectedIndex <= 0 || selectedIndex >= entries.size()) return;
        entries.remove(selectedIndex);
        selectedIndex = Math.min(selectedIndex, entries.size() - 1);
        loadSelection();
    }

    private boolean flushSelected() {
        if (selectedIndex < 0 || selectedIndex >= entries.size()) return false;
        String literal = literalField.getValue();
        String translation = translationField.getValue().strip();
        if (literal.isBlank() && translation.isBlank()) {
            status = Component.translatable("screen.talent_editor.text.value_required").getString();
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
        entries.set(selectedIndex, new LocalizedTextDraft(
                literal, translation, color, bold, italic
        ));
        status = "";
        return true;
    }

    private void loadSelection() {
        if (entries.isEmpty()) entries.add(LocalizedTextDraft.literal(""));
        selectedIndex = Math.max(0, Math.min(selectedIndex, entries.size() - 1));
        LocalizedTextDraft selected = entries.get(selectedIndex);
        literalField.setValue(selected.literal());
        translationField.setValue(selected.translation());
        colorField.setValue(selected.color().map(RarityDraft::formatColor).orElse(""));
        bold = selected.bold();
        italic = selected.italic();
        boldButton.setMessage(boldLabel());
        italicButton.setMessage(italicLabel());
        removeButton.active = selectedIndex > 0;
        status = "";
    }

    private Component boldLabel() {
        return Component.translatable(bold
                ? "screen.talent_editor.text.bold_yes"
                : "screen.talent_editor.text.bold_no");
    }

    private Component italicLabel() {
        return Component.translatable(italic
                ? "screen.talent_editor.text.italic_yes"
                : "screen.talent_editor.text.italic_no");
    }

    private void apply() {
        if (!flushSelected()) return;
        if (entries.stream().anyMatch(entry -> entry.literal().isBlank() && entry.translation().isBlank())) {
            status = Component.translatable("screen.talent_editor.text.value_required").getString();
            return;
        }
        parent.updateTextStyles(entries.get(0), List.copyOf(entries.subList(1, entries.size())));
        returnToParent();
    }

    private void returnToParent() {
        if (minecraft != null) minecraft.setScreen(parent);
    }

    @Override
    public void onClose() {
        returnToParent();
    }

    private int divider() {
        return Math.max(130, width * 34 / 100);
    }
}
