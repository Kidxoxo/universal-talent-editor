package com.kidxoxo.talenteditor.client;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import java.util.List;
import java.util.function.Consumer;

/** Confirmation used only by editor operations; Escape always takes the cancel branch. */
final class EditorConfirmScreen extends EditorScreen {
    private final Consumer<Boolean> answer;
    private final Component message;
    private List<FormattedCharSequence> lines = List.of();
    private int panelX, panelY, panelWidth, panelHeight;

    EditorConfirmScreen(Consumer<Boolean> answer, Component title, Component message) {
        super(title); this.answer = answer; this.message = message;
    }

    @Override protected void init() {
        panelWidth = Math.min(440, width - 32);
        lines = font.split(message, panelWidth - 32);
        panelHeight = Math.max(102, 65 + lines.size() * 12);
        panelX = (width - panelWidth) / 2;
        panelY = Math.max(26, (height - panelHeight) / 2);
        int buttonWidth = (panelWidth - 38) / 2;
        addRenderableWidget(EditorWidgets.button(Component.translatable("gui.cancel"), b -> answer.accept(false))
                .bounds(panelX + 16, panelY + panelHeight - 32, buttonWidth, 20).build());
        addRenderableWidget(EditorWidgets.button(Component.translatable("gui.proceed"), b -> answer.accept(true))
                .bounds(panelX + 22 + buttonWidth, panelY + panelHeight - 32, buttonWidth, 20).build());
    }

    @Override public void render(GuiGraphics g, int mx, int my, float tick) {
        renderBackground(g);
        EditorTheme.title(g, font, title, width);
        EditorTheme.modalPanel(g, panelX, panelY, panelWidth, panelHeight);
        int y = panelY + 18;
        for (var line : lines) { g.drawString(font, line, panelX + 16, y, EditorTheme.TEXT, false); y += 12; }
        super.render(g, mx, my, tick);
    }

    @Override public void onClose() { answer.accept(false); }
}
