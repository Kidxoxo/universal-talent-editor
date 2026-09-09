package com.kidxoxo.talenteditor.client;

import net.minecraft.client.gui.GuiGraphics;

final class EditorTheme {
    static final int TEXT = 0xFFE8EEF8;
    static final int MUTED = 0xFF93A4BC;
    static final int ACCENT = 0xFF55C8FF;

    private EditorTheme() {
    }

    static void panel(GuiGraphics graphics, int x, int y, int width, int height) {
        if (width <= 0 || height <= 0) return;
        graphics.fill(x + 2, y + 2, x + width + 2, y + height + 2, 0x50000000);
        graphics.fill(x, y, x + width, y + height, 0xB0121926);
        graphics.fill(x, y, x + 2, y + height, ACCENT);
        graphics.fill(x + 2, y, x + width, y + 1, 0x604A6B8E);
    }

    static void selectedTab(GuiGraphics graphics, int x, int y, int width) {
        graphics.fill(x + 2, y + 18, x + width - 2, y + 20, ACCENT);
    }
}
