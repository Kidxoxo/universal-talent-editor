package com.kidxoxo.talenteditor.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;

/** Uses the vanilla builder and event lifecycle, changing only the button's drawing. */
final class EditorWidgets {
    private EditorWidgets() {}

    static Button.Builder button(Component message, Button.OnPress action) {
        return button(message, EditorTheme.tone(message.getContents() instanceof TranslatableContents text ? text.getKey() : ""), action);
    }

    static Button.Builder button(Component message, EditorTheme.Tone tone, Button.OnPress action) {
        return new Button.Builder(message, button -> {
            if (Minecraft.getInstance().screen instanceof EditorDefinitionScreen screen && screen.submissionPending()) return;
            if (Minecraft.getInstance().screen instanceof EditorSettingsScreen screen && screen.busy()) return;
            action.onPress(button);
        }) {
            @Override public Button build() { return super.build(builder -> new ThemedButton(builder, tone)); }
        };
    }

    private static final class ThemedButton extends Button {
        private final EditorTheme.Tone tone;
        ThemedButton(Button.Builder builder, EditorTheme.Tone tone) { super(builder); this.tone=tone; }

        @Override protected void renderWidget(GuiGraphics g, int mouseX, int mouseY, float tick) {
            boolean highlight = isHoveredOrFocused();
            int fill = switch (tone) {
                case PRIMARY -> highlight ? 0xFF387D8B : 0xFF285C6D;
                case DANGER -> highlight ? 0xFF80434B : 0xFF543440;
                case NORMAL -> highlight ? EditorTheme.HOVER : EditorTheme.SURFACE;
            };
            if (!active) fill = 0xFF142233;
            int edge = active && highlight ? EditorTheme.ACCENT : EditorTheme.BORDER;
            g.fill(getX(), getY(), getX() + width, getY() + height, alpha(fill));
            EditorTheme.outline(g, getX(), getY(), width, height, alpha(edge));
            if (isFocused()) EditorTheme.outline(g, getX() + 2, getY() + 2, width - 4, height - 4, alpha(EditorTheme.ACCENT));
            renderScrollingString(g, Minecraft.getInstance().font, 5,
                    alpha(active ? EditorTheme.TEXT : 0xFF708397));
        }

        private int alpha(int color) {
            return (color & 0xFFFFFF) | (Math.round(255 * alpha) << 24);
        }
    }
}
