package com.kidxoxo.talenteditor.client;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

import java.util.List;
import java.util.function.IntConsumer;

/** A compact scrollable choice list that keeps the underlying form and its unsaved fields alive. */
final class EditorOptionPopup {
    private static final int ROW = 22;
    private Component title = Component.empty();
    private List<Component> choices = List.of();
    private IntConsumer select = index -> {};
    private int selected;
    private int scroll;
    private boolean open;

    void open(Component title, List<? extends Component> choices, int selected, IntConsumer select, int height) {
        if (choices.isEmpty()) return;
        this.title = title;
        this.choices = List.copyOf(choices);
        this.selected = Math.max(0, Math.min(choices.size() - 1, selected));
        this.select = select;
        this.open = true;
        int visible = visible(height);
        scroll = Math.max(0, Math.min(choices.size() - visible, this.selected - visible / 2));
    }

    boolean isOpen() { return open; }

    void render(GuiGraphics graphics, Font font, int width, int height) {
        if (!open) return;
        int visible = visible(height), w = Math.min(430, width - 32);
        int x = (width - w) / 2, top = top(height, visible);
        // Submit the form's buffered text before drawing the modal backdrop.
        graphics.flush();
        graphics.pose().pushPose();
        graphics.pose().translate(0, 0, 400);
        graphics.fill(0, 0, width, height, 0x99000000);
        EditorTheme.modalPanel(graphics, x - 7, top - 7, w + 14, 35 + visible * ROW);
        for (int row = 0; row < visible; row++) {
            int index = scroll + row;
            if (index >= choices.size()) break;
            int y = top + 23 + row * ROW;
            if (index == selected) graphics.fill(x, y - 2, x + w, y + 18, 0xFF285078);
        }
        if (choices.size() > visible) {
            int barX = x + w - 4, barTop = top + 23, barHeight = visible * ROW;
            graphics.fill(barX, barTop, barX + 3, barTop + barHeight, 0xFF344454);
            int thumb = Math.max(10, barHeight * visible / choices.size());
            int offset = (barHeight - thumb) * scroll / Math.max(1, choices.size() - visible);
            graphics.fill(barX, barTop + offset, barX + 3, barTop + offset + thumb, EditorTheme.ACCENT);
        }
        graphics.flush();
        graphics.drawCenteredString(font, title, width / 2, top, EditorTheme.TEXT);
        for (int row = 0; row < visible; row++) {
            int index = scroll + row;
            if (index >= choices.size()) break;
            int y = top + 23 + row * ROW;
            graphics.drawString(font, font.plainSubstrByWidth(choices.get(index).getString(), w - 16),
                    x + 8, y + 3, index == selected ? EditorTheme.TEXT : EditorTheme.MUTED, false);
        }
        graphics.flush();
        graphics.pose().popPose();
    }

    boolean mouseClicked(double mouseX, double mouseY, int button, int width, int height) {
        if (!open) return false;
        if (button != 0) { open = false; return true; }
        int visible = visible(height), w = Math.min(430, width - 32);
        int x = (width - w) / 2, top = top(height, visible);
        int row = (int) ((mouseY - top - 23) / ROW);
        if (mouseX >= x && mouseX < x + w && mouseY >= top + 23
                && row >= 0 && row < visible && scroll + row < choices.size()) {
            int chosen = scroll + row;
            open = false;
            select.accept(chosen);
        } else open = false;
        return true;
    }

    boolean mouseScrolled(double delta, int height) {
        if (!open) return false;
        scroll = Math.max(0, Math.min(Math.max(0, choices.size() - visible(height)),
                scroll - (int) Math.signum(delta) * 3));
        return true;
    }

    boolean keyPressed(int keyCode, int height) {
        if (!open) return false;
        if (keyCode == GLFW.GLFW_KEY_ESCAPE) { open = false; return true; }
        if (keyCode == GLFW.GLFW_KEY_UP || keyCode == GLFW.GLFW_KEY_DOWN) {
            selected = Math.max(0, Math.min(choices.size() - 1,
                    selected + (keyCode == GLFW.GLFW_KEY_DOWN ? 1 : -1)));
            scroll = Math.max(0, Math.min(scroll, selected));
            scroll = Math.max(scroll, selected - visible(height) + 1);
            return true;
        }
        if (keyCode == GLFW.GLFW_KEY_ENTER || keyCode == GLFW.GLFW_KEY_KP_ENTER) {
            int chosen = selected;
            open = false;
            select.accept(chosen);
            return true;
        }
        return true;
    }

    private int visible(int height) { return Math.max(1, Math.min(choices.size(), (height - 80) / ROW)); }
    private int top(int height, int visible) { return Math.max(18, (height - (30 + visible * ROW)) / 2); }
}
