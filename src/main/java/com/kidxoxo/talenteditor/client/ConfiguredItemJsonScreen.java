package com.kidxoxo.talenteditor.client;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.function.Consumer;

/** Escape hatch for trigger features that do not yet have individual visual controls. */
public final class ConfiguredItemJsonScreen extends Screen {
    private final Screen parent;
    private final Consumer<JsonObject> receiver;
    private final String original;
    private EditBox jsonField;
    private String status = "";

    ConfiguredItemJsonScreen(Screen parent, JsonObject value, Consumer<JsonObject> receiver) {
        super(Component.translatable("screen.talent_editor.configured_item.triggers.raw_title"));
        this.parent = parent;
        this.receiver = receiver;
        this.original = value.toString();
    }

    @Override protected void init() {
        int w = Math.min(760, width - 32), x = (width - w) / 2;
        jsonField = new EditBox(font, x, 66, w, 20, Component.empty());
        jsonField.setMaxLength(32767);
        jsonField.setValue(original);
        addRenderableWidget(jsonField);
        int half = (w - 6) / 2;
        addRenderableWidget(Button.builder(Component.translatable("gui.cancel"), b -> close())
                .bounds(x, height - 28, half, 20).build());
        addRenderableWidget(Button.builder(Component.translatable("gui.done"), b -> apply())
                .bounds(x + half + 6, height - 28, w - half - 6, 20).build());
    }

    private void apply() {
        try {
            JsonElement parsed = JsonParser.parseString(jsonField.getValue());
            if (!parsed.isJsonObject()) throw new IllegalArgumentException("Expected a JSON object");
            receiver.accept(parsed.getAsJsonObject().deepCopy());
            close();
        } catch (RuntimeException exception) {
            status = exception.getMessage() == null ? "Invalid JSON" : exception.getMessage();
        }
    }

    @Override public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics);
        int w = Math.min(760, width - 32), x = (width - w) / 2;
        EditorTheme.panel(graphics, x - 7, 27, w + 14, 91);
        super.render(graphics, mouseX, mouseY, partialTick);
        graphics.drawCenteredString(font, title, width / 2, 10, EditorTheme.TEXT);
        graphics.drawString(font, Component.translatable(
                "screen.talent_editor.configured_item.triggers.raw_help"), x, 44, EditorTheme.MUTED, false);
        if (!status.isBlank()) graphics.drawString(font, font.plainSubstrByWidth(status, w), x,
                height - 47, 0xFFFF7777, false);
    }

    private void close() { if (minecraft != null) minecraft.setScreen(parent); }
    @Override public void onClose() { close(); }
}
