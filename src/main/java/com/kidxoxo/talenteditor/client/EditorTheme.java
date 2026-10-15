package com.kidxoxo.talenteditor.client;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.Font;
import net.minecraft.network.chat.Component;

final class EditorTheme {
    static final int TEXT = 0xFFE6EEF7;
    static final int MUTED = 0xFF9AAFC4;
    static final int ACCENT = 0xFF7DCAD6;
    static final int BORDER = 0xFF324A62;
    static final int SURFACE = 0xFF17293C;
    static final int HOVER = 0xFF28465F;
    static final int SELECTED = 0xFF264F64;
    static final int SUCCESS = 0xFF98D6B0;
    static final int WARNING = 0xFFE8C383;
    static final int ERROR = 0xFFF09B9B;
    static final int DEFINITION_ROW_HEIGHT = 28;
    enum Tone { NORMAL, PRIMARY, DANGER }

    private EditorTheme() {
    }

    static void background(GuiGraphics g, int width, int height) {
        g.fillGradient(0, 0, width, height, 0xFF0D1725, 0xFF15283A);
        g.fill(0, 0, width, 20, 0xFF0F1D2D);
        g.fill(0, 19, width, 20, BORDER);
        g.fill(8, 25, width - 8, height - 36, 0xB0101E2E);
        g.fill(0, height - 34, width, height, 0xFF0F1D2D);
        g.fill(0, height - 35, width, height - 34, BORDER);
    }

    static void title(GuiGraphics g, Font font, Component title, int width) {
        String label = font.plainSubstrByWidth(title.getString(), Math.max(1, width - 36));
        g.drawCenteredString(font, label, width / 2, 7, TEXT);
    }

    static Tone tone(String key) {
        if (key.equals("gui.done") || key.endsWith(".save") || key.endsWith(".apply")
                || key.endsWith(".new_talent") || key.endsWith(".create")) return Tone.PRIMARY;
        if (key.endsWith(".delete") || key.endsWith(".remove") || key.endsWith(".delete.confirm")
                || key.endsWith(".remove_effect") || key.endsWith(".discard")) return Tone.DANGER;
        return Tone.NORMAL;
    }

    static void outline(GuiGraphics g, int x, int y, int width, int height, int color) {
        if (width <= 0 || height <= 0) return;
        g.fill(x, y, x + width, y + 1, color);
        g.fill(x, y + height - 1, x + width, y + height, color);
        g.fill(x, y + 1, x + 1, y + height - 1, color);
        g.fill(x + width - 1, y + 1, x + width, y + height - 1, color);
    }

    static void row(GuiGraphics g, int x, int y, int width, int height, boolean selected, boolean hovered) {
        g.fill(x, y, x + width, y + height, selected ? SELECTED : hovered ? HOVER : SURFACE);
        if (selected) g.fill(x, y, x + 2, y + height, ACCENT);
        g.fill(x, y + height - 1, x + width, y + height, BORDER);
    }

    static void definitionRow(GuiGraphics g, Font font, int x, int y, int width,
                              boolean selected, boolean hovered, String name, String detail, int color) {
        row(g, x, y, width, DEFINITION_ROW_HEIGHT - 2, selected, hovered);
        int available = Math.max(1, width - 16);
        g.drawString(font, font.plainSubstrByWidth(name, available), x + 8, y + 3, color, false);
        g.drawString(font, font.plainSubstrByWidth(detail, available), x + 8, y + 15, MUTED, false);
    }

    static void panel(GuiGraphics graphics, int x, int y, int width, int height) {
        if (width <= 0 || height <= 0) return;
        graphics.fill(x + 2, y + 2, x + width + 2, y + height + 2, 0x30000000);
        graphics.fill(x, y, x + width, y + height, SURFACE);
        outline(graphics, x, y, width, height, BORDER);
    }

    static void modalPanel(GuiGraphics graphics, int x, int y, int width, int height) {
        if (width <= 0 || height <= 0) return;
        graphics.fill(x + 3, y + 3, x + width + 3, y + height + 3, 0xB0000000);
        graphics.fill(x, y, x + width, y + height, 0xFF152638);
        outline(graphics, x, y, width, height, BORDER);
        graphics.fill(x + 1, y, x + width - 1, y + 2, ACCENT);
    }

    static void selectedTab(GuiGraphics graphics, int x, int y, int width) {
        graphics.fill(x + 2, y + 18, x + width - 2, y + 20, ACCENT);
    }
}
