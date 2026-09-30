package com.kidxoxo.talenteditor.client;

import com.google.gson.JsonObject;
import com.kidxoxo.talenteditor.network.EditorBootstrap;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;

/** Ordered normal or recovery actions for one configured-item event. */
public final class ConfiguredItemTriggerActionsScreen extends Screen {
    private final ConfiguredItemTriggerScreen parent;
    private final EditorBootstrap bootstrap;
    private final boolean failure;
    private final ArrayList<JsonObject> actions = new ArrayList<>();
    private int selected;
    private int scroll;
    private Button editButton;
    private Button removeButton;
    private Button upButton;
    private Button downButton;
    private String status = "";

    ConfiguredItemTriggerActionsScreen(ConfiguredItemTriggerScreen parent, EditorBootstrap bootstrap,
                                       List<JsonObject> source, boolean failure) {
        super(Component.translatable(failure
                ? "screen.talent_editor.configured_item.triggers.failure_title"
                : "screen.talent_editor.configured_item.triggers.actions_title"));
        this.parent = parent;
        this.bootstrap = bootstrap;
        this.failure = failure;
        source.forEach(action -> actions.add(action.deepCopy()));
        selected = actions.isEmpty() ? -1 : 0;
    }

    @Override protected void init() {
        int w = Math.min(660, width - 32), x = (width - w) / 2;
        int quarter = (w - 18) / 4;
        addRenderableWidget(Button.builder(Component.translatable(
                        "screen.talent_editor.configured_item.triggers.add_action"), b -> addAction())
                .bounds(x, height - 56, quarter, 20).build());
        editButton = addRenderableWidget(Button.builder(Component.translatable(
                        "screen.talent_editor.configured_item.triggers.edit_action"), b -> editAction())
                .bounds(x + quarter + 6, height - 56, quarter, 20).build());
        removeButton = addRenderableWidget(Button.builder(Component.translatable(
                        "screen.talent_editor.configured_item.triggers.remove_action"), b -> removeAction())
                .bounds(x + (quarter + 6) * 2, height - 56, quarter, 20).build());
        int movesWidth = w - (quarter + 6) * 3;
        upButton = addRenderableWidget(Button.builder(Component.literal("↑"), b -> move(-1))
                .bounds(x + (quarter + 6) * 3, height - 56, (movesWidth - 4) / 2, 20).build());
        downButton = addRenderableWidget(Button.builder(Component.literal("↓"), b -> move(1))
                .bounds(x + (quarter + 6) * 3 + (movesWidth - 4) / 2 + 4, height - 56,
                        movesWidth - (movesWidth - 4) / 2 - 4, 20).build());
        int half = (w - 6) / 2;
        addRenderableWidget(Button.builder(Component.translatable("gui.cancel"), b -> close())
                .bounds(x, height - 28, half, 20).build());
        addRenderableWidget(Button.builder(Component.translatable(
                        "screen.talent_editor.configured_item.triggers.apply"), b -> apply())
                .bounds(x + half + 6, height - 28, w - half - 6, 20).build());
        sync();
    }

    private void sync() {
        if (editButton == null) return;
        editButton.active = selected >= 0;
        removeButton.active = selected >= 0;
        upButton.active = selected > 0;
        downButton.active = selected >= 0 && selected + 1 < actions.size();
    }

    private void addAction() {
        if (actions.size() >= 256) return;
        JsonObject action = new JsonObject();
        action.addProperty("type", "timed_attribute_modifier");
        action.addProperty("attribute", "minecraft:generic.attack_damage");
        action.addProperty("expression", "0");
        action.addProperty("operation", "addition");
        action.addProperty("modifierKey", "item_modifier_" + (actions.size() + 1));
        action.addProperty("durationTicks", 40);
        action.addProperty("target", "self");
        actions.add(action);
        selected = actions.size() - 1;
        sync();
        editAction();
    }

    private void editAction() {
        if (selected < 0 || minecraft == null) return;
        minecraft.setScreen(new ConfiguredItemTriggerActionScreen(this, bootstrap, actions.get(selected)));
    }

    void replaceSelected(JsonObject action) {
        if (selected < 0) return;
        actions.set(selected, action.deepCopy());
        status = "";
    }

    private void removeAction() {
        if (selected < 0) return;
        actions.remove(selected);
        selected = actions.isEmpty() ? -1 : Math.min(selected, actions.size() - 1);
        sync();
    }

    private void move(int offset) {
        int target = selected + offset;
        if (selected < 0 || target < 0 || target >= actions.size()) return;
        JsonObject action = actions.remove(selected);
        actions.add(target, action);
        selected = target;
        sync();
    }

    private void apply() {
        if (!failure && actions.isEmpty()) {
            status = Component.translatable("screen.talent_editor.configured_item.triggers.action_required").getString();
            return;
        }
        parent.replaceActions(List.copyOf(actions), failure);
        close();
    }

    @Override public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics);
        int w = Math.min(660, width - 32), x = (width - w) / 2;
        EditorTheme.panel(graphics, x - 7, 27, w + 14, Math.max(1, height - 91));
        super.render(graphics, mouseX, mouseY, partialTick);
        graphics.drawCenteredString(font, title, width / 2, 10, EditorTheme.TEXT);
        graphics.drawString(font, Component.translatable("screen.talent_editor.configured_item.triggers.action_list"),
                x + 3, 35, EditorTheme.MUTED, false);
        int visible = Math.max(1, (height - 111) / 20);
        scroll = Math.max(0, Math.min(scroll, Math.max(0, actions.size() - visible)));
        for (int index = scroll; index < Math.min(actions.size(), scroll + visible); index++) {
            int y = 57 + (index - scroll) * 20;
            if (selected == index || mouseX >= x && mouseX < x + w && mouseY >= y - 3 && mouseY < y + 15)
                graphics.fill(x, y - 3, x + w, y + 15,
                        selected == index ? 0x7050A0E8 : 0x404A90E2);
            String kind = ConfiguredItemTriggerScreen.local(ConfiguredItemTriggerScreen.string(
                    actions.get(index), "type", "?"));
            graphics.drawString(font, font.plainSubstrByWidth((index + 1) + ". " + kind, w - 8),
                    x + 4, y, EditorTheme.TEXT, false);
        }
        if (!status.isBlank()) graphics.drawString(font, font.plainSubstrByWidth(status, w), x,
                height - 77, 0xFFFF7777, false);
    }

    @Override public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0) {
            int w = Math.min(660, width - 32), x = (width - w) / 2;
            if (mouseX >= x && mouseX < x + w && mouseY >= 54 && mouseY < height - 64) {
                int index = scroll + (int) ((mouseY - 54) / 20);
                if (index < actions.size()) { selected = index; sync(); }
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        scroll = Math.max(0, Math.min(scroll - (int) Math.signum(delta) * 3,
                Math.max(0, actions.size() - Math.max(1, (height - 111) / 20))));
        return true;
    }

    private void close() { if (minecraft != null) minecraft.setScreen(parent); }
    @Override public void onClose() { close(); }
}
