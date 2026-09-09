package com.kidxoxo.talenteditor.client;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public final class CounterResetScreen extends Screen {
    private final CounterEditScreen parent;
    private boolean onLogin;
    private boolean onDeath;
    private String intervalValue;
    private Button loginButton;
    private Button deathButton;
    private EditBox intervalField;
    private String status = "";

    public CounterResetScreen(CounterEditScreen parent) {
        super(Component.translatable("screen.talent_editor.counter.reset.title"));
        this.parent = parent;
        CounterDraft.ResetSettings settings = parent.resetSettings();
        this.onLogin = settings.onLogin();
        this.onDeath = settings.onDeath();
        this.intervalValue = Integer.toString(settings.intervalTicks());
    }

    @Override
    protected void init() {
        int formWidth = Math.min(540, width - 32);
        int x = (width - formWidth) / 2;
        int half = (formWidth - 6) / 2;
        loginButton = addRenderableWidget(Button.builder(loginLabel(), button -> {
            onLogin = !onLogin;
            loginButton.setMessage(loginLabel());
        }).bounds(x, 49, half, 20).build());
        deathButton = addRenderableWidget(Button.builder(deathLabel(), button -> {
            onDeath = !onDeath;
            deathButton.setMessage(deathLabel());
        }).bounds(x + half + 6, 49, formWidth - half - 6, 20).build());
        intervalField = new EditBox(font, x, 92, formWidth, 20,
                Component.translatable("screen.talent_editor.counter.reset.interval"));
        intervalField.setMaxLength(10);
        intervalField.setValue(intervalValue);
        intervalField.setResponder(value -> {
            intervalValue = value;
            status = "";
        });
        addRenderableWidget(intervalField);
        addRenderableWidget(Button.builder(Component.translatable("gui.cancel"), button -> returnParent())
                .bounds(x, height - 28, 80, 20).build());
        addRenderableWidget(Button.builder(Component.translatable("gui.done"), button -> save())
                .bounds(x + 86, height - 28, formWidth - 86, 20).build());
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics);
        super.render(graphics, mouseX, mouseY, partialTick);
        int formWidth = Math.min(540, width - 32);
        int x = (width - formWidth) / 2;
        graphics.drawCenteredString(font, title, width / 2, 15, 0xFFFFFF);
        graphics.drawString(font, Component.translatable("screen.talent_editor.counter.reset.triggers"),
                x, 38, 0xA0A0A0, false);
        graphics.drawString(font, Component.translatable("screen.talent_editor.counter.reset.interval"),
                x, 81, 0xA0A0A0, false);
        graphics.drawWordWrap(font, Component.translatable("screen.talent_editor.counter.reset.help"),
                x, 123, formWidth, 0x808080);
        if (!status.isBlank()) {
            graphics.drawString(font, font.plainSubstrByWidth(status, formWidth),
                    x, height - 45, 0xFF7777, false);
        }
    }

    private void save() {
        int interval;
        try {
            interval = Integer.parseInt(intervalField.getValue().strip());
            if (interval < 0) throw new NumberFormatException();
        } catch (NumberFormatException ignored) {
            status = Component.translatable("screen.talent_editor.counter.reset.invalid_interval").getString();
            return;
        }
        parent.updateResetSettings(new CounterDraft.ResetSettings(onLogin, onDeath, interval));
        returnParent();
    }

    private Component loginLabel() {
        return Component.translatable(onLogin
                ? "screen.talent_editor.counter.reset.login_yes"
                : "screen.talent_editor.counter.reset.login_no");
    }

    private Component deathLabel() {
        return Component.translatable(onDeath
                ? "screen.talent_editor.counter.reset.death_yes"
                : "screen.talent_editor.counter.reset.death_no");
    }

    private void returnParent() {
        if (minecraft != null) minecraft.setScreen(parent);
    }

    @Override
    public void onClose() {
        returnParent();
    }
}
