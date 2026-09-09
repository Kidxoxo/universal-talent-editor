package com.kidxoxo.talenteditor.client;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public final class TalentSettingsMenuScreen extends Screen {
    private final TalentEditScreen parent;

    TalentSettingsMenuScreen(TalentEditScreen parent) {
        super(Component.translatable("screen.talent_editor.settings_menu.title"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        int formWidth = Math.min(520, width - 32);
        int x = (width - formWidth) / 2;
        int gap = 6;
        int half = (formWidth - gap) / 2;
        addEntry(x, 45, half, "text", parent::openTextSettings);
        addEntry(x + half + gap, 45, formWidth - half - gap, "metadata", parent::openMetadataSettings);
        addEntry(x, 75, half, "requirements", parent::openRequirementsSettings);
        addEntry(x + half + gap, 75, formWidth - half - gap, "synthesis", parent::openSynthesisSettings);
        addEntry(x, 105, half, "offer", parent::openOfferSettings);
        addEntry(x + half + gap, 105, formWidth - half - gap, "presentation", parent::openPresentationSettings);
        addRenderableWidget(Button.builder(Component.translatable("gui.back"), button -> returnParent())
                .bounds(x, height - 28, formWidth, 20).build());
    }

    private void addEntry(int x, int y, int width, String suffix, Runnable action) {
        addRenderableWidget(Button.builder(Component.translatable(
                "screen.talent_editor.settings_menu." + suffix), button -> action.run())
                .bounds(x, y, width, 20).build());
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics);
        int formWidth = Math.min(520, width - 32);
        int x = (width - formWidth) / 2;
        EditorTheme.panel(graphics, x - 7, 29, formWidth + 14, Math.min(107, height - 70));
        super.render(graphics, mouseX, mouseY, partialTick);
        graphics.drawCenteredString(font, title, width / 2, 11, EditorTheme.TEXT);
        graphics.drawCenteredString(font, Component.translatable("screen.talent_editor.settings_menu.help"),
                width / 2, 29, EditorTheme.MUTED);
    }

    private void returnParent() {
        if (minecraft != null) minecraft.setScreen(parent);
    }

    @Override
    public void onClose() {
        returnParent();
    }
}
