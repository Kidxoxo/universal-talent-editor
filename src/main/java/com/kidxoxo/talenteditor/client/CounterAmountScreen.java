package com.kidxoxo.talenteditor.client;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.List;

public final class CounterAmountScreen extends Screen {
    private static final List<String> MODES = List.of("fixed", "event_amount");
    private static final List<String> ROUNDINGS = List.of("floor", "round", "ceil");
    private final CounterEditScreen parent;
    private String mode;
    private String scale;
    private String rounding;
    private String minimum;
    private Button modeButton;
    private Button roundingButton;
    private String status = "";
    private final EditorOptionPopup options = new EditorOptionPopup();

    public CounterAmountScreen(CounterEditScreen parent) {
        super(Component.translatable("screen.talent_editor.counter.amount.title"));
        this.parent = parent;
        CounterDraft.AmountSettings value = parent.amountSettings();
        this.mode = MODES.contains(value.mode()) ? value.mode() : "fixed";
        this.scale = Double.toString(value.scale());
        this.rounding = ROUNDINGS.contains(value.rounding()) ? value.rounding() : "round";
        this.minimum = Long.toString(value.minimumIncrement());
    }

    @Override
    protected void init() {
        int formWidth = Math.min(620, width - 32);
        int x = (width - formWidth) / 2;
        modeButton = addRenderableWidget(Button.builder(modeLabel(), button -> cycleMode())
                .bounds(x, 52, formWidth, 20).build());
        addField(x, 91, formWidth, scale, 32, value -> scale = value);
        roundingButton = addRenderableWidget(Button.builder(roundingLabel(), button -> chooseRounding())
                .bounds(x, 130, formWidth, 20).build());
        addField(x, 169, formWidth, minimum, 18, value -> minimum = value);
        addRenderableWidget(Button.builder(Component.translatable("gui.cancel"), button -> returnParent())
                .bounds(x, height - 28, (formWidth - 6) / 2, 20).build());
        addRenderableWidget(Button.builder(Component.translatable("gui.done"), button -> apply())
                .bounds(x + (formWidth - 6) / 2 + 6, height - 28, formWidth - (formWidth - 6) / 2 - 6, 20)
                .build());
    }

    private void addField(int x, int y, int width, String value, int maxLength,
                          java.util.function.Consumer<String> responder) {
        EditBox field = new EditBox(font, x, y, width, 20, Component.empty());
        field.setMaxLength(maxLength);
        field.setValue(value);
        field.setResponder(next -> {
            responder.accept(next);
            status = "";
        });
        addRenderableWidget(field);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics);
        super.render(graphics, mouseX, mouseY, partialTick);
        int formWidth = Math.min(620, width - 32);
        int x = (width - formWidth) / 2;
        graphics.drawCenteredString(font, title, width / 2, 14, 0xFFFFFF);
        label(graphics, "screen.talent_editor.counter.amount_scale", x, 80);
        label(graphics, "screen.talent_editor.counter.minimum_increment", x, 158);
        graphics.drawString(font, Component.translatable("screen.talent_editor.counter.amount_help"),
                x, 203, 0xA0A0A0, false);
        if (!status.isBlank()) graphics.drawString(font, status, x, height - 43, 0xFF7777, false);
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

    private void label(GuiGraphics graphics, String key, int x, int y) {
        graphics.drawString(font, Component.translatable(key), x, y, 0xA0A0A0, false);
    }

    private void cycleMode() {
        mode = MODES.get((MODES.indexOf(mode) + 1) % MODES.size());
        modeButton.setMessage(modeLabel());
    }

    private void chooseRounding() {
        options.open(Component.translatable("screen.talent_editor.option.choose"),
                ROUNDINGS.stream().map(value -> Component.translatable(
                        "screen.talent_editor.counter.rounding." + value)).toList(),
                Math.max(0, ROUNDINGS.indexOf(rounding)), index -> {
                    rounding = ROUNDINGS.get(index);
                    roundingButton.setMessage(roundingLabel());
                }, height);
    }

    private Component modeLabel() {
        return Component.translatable("screen.talent_editor.counter.amount_mode",
                Component.translatable("screen.talent_editor.counter.amount_mode." + mode));
    }

    private Component roundingLabel() {
        return Component.translatable("screen.talent_editor.counter.rounding",
                Component.translatable("screen.talent_editor.counter.rounding." + rounding));
    }

    private void apply() {
        double parsedScale;
        long parsedMinimum;
        try {
            parsedScale = Double.parseDouble(scale.strip());
            parsedMinimum = Long.parseLong(minimum.strip());
        } catch (NumberFormatException ex) {
            invalid();
            return;
        }
        if (!Double.isFinite(parsedScale) || parsedScale < 0.0D || parsedScale > 1_000_000_000.0D
                || parsedMinimum < 0L || parsedMinimum > 1_000_000_000_000L) {
            invalid();
            return;
        }
        parent.updateAmountSettings(new CounterDraft.AmountSettings(mode, parsedScale, rounding, parsedMinimum));
        returnParent();
    }

    private void invalid() {
        status = Component.translatable("screen.talent_editor.counter.invalid_amount").getString();
    }

    private void returnParent() {
        if (minecraft != null) minecraft.setScreen(parent);
    }

    @Override
    public void onClose() {
        returnParent();
    }
}
