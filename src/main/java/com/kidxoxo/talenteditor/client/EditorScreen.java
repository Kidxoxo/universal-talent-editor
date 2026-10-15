package com.kidxoxo.talenteditor.client;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.MultiLineEditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/** Shared editor chrome. Gameplay screens and authored UI documents keep their own appearance. */
abstract class EditorScreen extends Screen {
    protected EditorScreen(Component title) { super(title); }

    @Override public void resize(net.minecraft.client.Minecraft minecraft, int width, int height) {
        // Rebuilding controls must not erase text that has not yet been applied to a draft.
        var values = captureInputs();
        super.resize(minecraft, width, height);
        restoreInputs(values);
    }

    final java.util.Map<String,java.util.List<String>> captureInputs() {
        var values = new java.util.HashMap<String,java.util.List<String>>();
        for (var child : children()) {
            if(child instanceof EditBox f)values.computeIfAbsent(f.getMessage().getString(),k->new java.util.ArrayList<>()).add(f.getValue());
            else if(child instanceof MultiLineEditBox f)values.computeIfAbsent(f.getMessage().getString(),k->new java.util.ArrayList<>()).add(f.getValue());
        }
        return values;
    }

    final void restoreInputs(java.util.Map<String,java.util.List<String>> snapshot) {
        var values = new java.util.HashMap<String,java.util.ArrayDeque<String>>();
        snapshot.forEach((key,value)->values.put(key,new java.util.ArrayDeque<>(value)));
        for (var child : children()) {
            if(child instanceof EditBox f){var saved=values.get(f.getMessage().getString());if(saved!=null&&!saved.isEmpty())f.setValue(saved.removeFirst());}
            else if(child instanceof MultiLineEditBox f){var saved=values.get(f.getMessage().getString());if(saved!=null&&!saved.isEmpty())f.setValue(saved.removeFirst());}
        }
    }

    @Override public void renderBackground(GuiGraphics graphics) {
        EditorTheme.background(graphics, width, height);
    }

    @Override public void render(GuiGraphics g, int mouseX, int mouseY, float tick) {
        super.render(g, mouseX, mouseY, tick);
        // Keep Minecraft's text editing, caret, selection, narration and hit boxes intact.
        for (var renderable : renderables) {
            if (renderable instanceof EditBox field && field.visible) {
                EditorTheme.outline(g, field.getX() - 1, field.getY() - 1,
                        field.getWidth() + 2, field.getHeight() + 2,
                        field.isFocused() ? EditorTheme.ACCENT : EditorTheme.BORDER);
            } else if (renderable instanceof MultiLineEditBox field && field.visible) {
                EditorTheme.outline(g, field.getX() - 1, field.getY() - 1,
                        field.getWidth() + 2, field.getHeight() + 2,
                        field.isFocused() ? EditorTheme.ACCENT : EditorTheme.BORDER);
            }
        }
    }
}
