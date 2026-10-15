package com.kidxoxo.talenteditor.client;

import com.google.gson.Gson;
import com.google.gson.JsonParser;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/** Complete JSON editor retained for forward compatibility and unsupported extension fields. */
public final class TalentRawJsonScreen extends EditorScreen {
    private static final Gson COMPACT = new Gson();
    private final Screen returnScreen;
    private final TalentEditScreen owner;
    private final String initial;
    private EditBox json;
    private String status = "";

    TalentRawJsonScreen(Screen returnScreen, TalentEditScreen owner, String initial) {
        super(Component.translatable("screen.talent_editor.advanced_json.title"));
        this.returnScreen = returnScreen;
        this.owner = owner;
        this.initial = COMPACT.toJson(JsonParser.parseString(initial));
    }

    @Override
    protected void init() {
        int formWidth = Math.min(760, width - 24);
        int x = (width - formWidth) / 2;
        json = new EditBox(font, x, 54, formWidth, 20,
                Component.translatable("screen.talent_editor.advanced_json.field"));
        json.setMaxLength(1_000_000);
        json.setValue(initial);
        addRenderableWidget(json);
        addRenderableWidget(EditorWidgets.button(Component.translatable("gui.cancel"), button -> returnParent())
                .bounds(x, height - 28, 80, 20).build());
        addRenderableWidget(EditorWidgets.button(Component.translatable("gui.done"), button -> apply())
                .bounds(x + 86, height - 28, formWidth - 86, 20).build());
    }

    private void apply() {
        try {
            owner.applyAdvancedJson(json.getValue());
            returnParent();
        } catch (RuntimeException exception) {
            status = exception.getMessage() == null ? exception.getClass().getSimpleName() : exception.getMessage();
        }
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics);
        int formWidth = Math.min(760, width - 24);
        int x = (width - formWidth) / 2;
        EditorTheme.panel(graphics, x - 7, 28, formWidth + 14, 76);
        super.render(graphics, mouseX, mouseY, partialTick);
        EditorTheme.title(graphics,font,title,width);
        graphics.drawWordWrap(font, Component.translatable("screen.talent_editor.advanced_json.help"),
                x, 82, formWidth, EditorTheme.MUTED);
        if (!status.isBlank()) graphics.drawString(font, font.plainSubstrByWidth(status, formWidth),
                x, height - 47, EditorTheme.ERROR, false);
    }

    private void returnParent() { if (minecraft != null) minecraft.setScreen(returnScreen); }
    @Override public void onClose() { returnParent(); }
}
