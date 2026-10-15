package com.kidxoxo.talenteditor.client;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.kidxoxo.talenteditor.network.EditorBootstrap;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;

/** Event rules owned by a configured item. Unknown fields are retained on every edit. */
public final class ConfiguredItemTriggerScreen extends EditorScreen {
    private static final int ROW_HEIGHT = 20;
    private static final List<String> EVENTS = List.of(
            "damage_dealt_pre", "damage_taken_pre", "damage_dealt", "damage_taken", "interact_entity",
            "kill", "death", "item_use_finish", "interact_block", "player_tick", "player_second",
            "player_move_distance", "level_up", "draw_selected", "draw_refreshed", "counter_changed",
            "fatal_damage", "nearby_entity_death", "item_submitted"
    );
    private final ConfiguredItemEditScreen parent;
    private final EditorBootstrap bootstrap;
    private final ArrayList<JsonObject> triggers = new ArrayList<>();
    private final EditorOptionPopup options = new EditorOptionPopup();
    private int selected;
    private int scroll;
    private String event = "player_second";
    private String failurePolicy = "continue";
    private EditBox idField;
    private EditBox priorityField;
    private EditBox chanceField;
    private EditBox cooldownField;
    private EditBox limitField;
    private Button eventButton;
    private Button policyButton;
    private Button actionsButton;
    private Button failureButton;
    private Button rawButton;
    private Button removeButton;
    private String status = "";

    ConfiguredItemTriggerScreen(ConfiguredItemEditScreen parent, EditorBootstrap bootstrap,
                                List<JsonObject> source) {
        super(Component.translatable("screen.talent_editor.configured_item.triggers.title"));
        this.parent = parent;
        this.bootstrap = bootstrap;
        source.forEach(trigger -> triggers.add(trigger.deepCopy()));
        selected = triggers.isEmpty() ? -1 : 0;
    }

    @Override protected void init() {
        int w = Math.min(760, width - 32), x = (width - w) / 2;
        int leftWidth = Math.min(220, Math.max(120, w * 2 / 5));
        int rx = x + leftWidth + 12, rw = w - leftWidth - 12;
        int quarter = (rw - 18) / 4;
        eventButton = addRenderableWidget(EditorWidgets.button(Component.empty(), b -> chooseEvent())
                .bounds(rx, 35, rw, 20).build());
        idField = field(rx, 70, rw, 64);
        priorityField = field(rx, 105, quarter, 8);
        chanceField = field(rx + quarter + 6, 105, quarter, 16);
        cooldownField = field(rx + (quarter + 6) * 2, 105, quarter, 10);
        limitField = field(rx + (quarter + 6) * 3, 105, rw - (quarter + 6) * 3, 10);
        actionsButton = addRenderableWidget(EditorWidgets.button(Component.empty(), b -> openActions(false))
                .bounds(rx, 137, rw, 20).build());
        failureButton = addRenderableWidget(EditorWidgets.button(Component.empty(), b -> openActions(true))
                .bounds(rx, 164, rw, 20).build());
        int half = (rw - 6) / 2;
        policyButton = addRenderableWidget(EditorWidgets.button(Component.empty(), b -> choosePolicy())
                .bounds(rx, 191, half, 20).build());
        rawButton = addRenderableWidget(EditorWidgets.button(Component.translatable(
                        "screen.talent_editor.configured_item.triggers.raw"), b -> openRaw())
                .bounds(rx + half + 6, 191, rw - half - 6, 20).build());
        int leftHalf = (leftWidth - 6) / 2;
        addRenderableWidget(EditorWidgets.button(Component.translatable(
                        "screen.talent_editor.configured_item.triggers.add"), b -> addTrigger())
                .bounds(x, height - 56, leftHalf, 20).build());
        removeButton = addRenderableWidget(EditorWidgets.button(Component.translatable(
                        "screen.talent_editor.configured_item.triggers.remove"), b -> removeTrigger())
                .bounds(x + leftHalf + 6, height - 56, leftWidth - leftHalf - 6, 20).build());
        int bottomHalf = (w - 6) / 2;
        addRenderableWidget(EditorWidgets.button(Component.translatable("gui.cancel"), b -> close())
                .bounds(x, height - 28, bottomHalf, 20).build());
        addRenderableWidget(EditorWidgets.button(Component.translatable(
                        "screen.talent_editor.configured_item.triggers.apply"), b -> apply())
                .bounds(x + bottomHalf + 6, height - 28, w - bottomHalf - 6, 20).build());
        loadSelected();
    }

    private EditBox field(int x, int y, int w, int max) {
        EditBox box = new EditBox(font, x, y, w, 20, Component.empty());
        box.setMaxLength(max);
        addRenderableWidget(box);
        return box;
    }

    private void loadSelected() {
        boolean active = selected >= 0 && selected < triggers.size();
        JsonObject trigger = active ? triggers.get(selected) : null;
        idField.setValue(active ? string(trigger, "id", "") : "");
        event = active ? local(string(trigger, "event", "player_second")) : "player_second";
        priorityField.setValue(active ? string(trigger, "priority", "0") : "0");
        chanceField.setValue(active ? string(trigger, "chance", "1.0") : "1.0");
        cooldownField.setValue(active ? string(trigger, "cooldownTicks", "0") : "0");
        limitField.setValue(active ? string(trigger, "activationLimit", "0") : "0");
        failurePolicy = active ? string(trigger, "actionFailurePolicy", "continue") : "continue";
        for (EditBox field : List.of(idField, priorityField, chanceField, cooldownField, limitField)) field.active = active;
        eventButton.active = active;
        actionsButton.active = active;
        failureButton.active = active;
        policyButton.active = active;
        rawButton.active = active;
        removeButton.active = active;
        eventButton.setMessage(eventLabel());
        policyButton.setMessage(Component.translatable(
                "screen.talent_editor.configured_item.triggers.policy." + failurePolicy));
        int normal = active ? actionCount(trigger, "actions") : 0;
        int failure = active ? actionCount(trigger, "failureActions") : 0;
        actionsButton.setMessage(Component.translatable(
                "screen.talent_editor.configured_item.triggers.actions", normal));
        failureButton.setMessage(Component.translatable(
                "screen.talent_editor.configured_item.triggers.failure", failure));
    }

    private Component eventLabel() {
        return Component.translatable("screen.talent_editor.trigger.event." + event);
    }

    private boolean storeSelected() {
        if (selected < 0 || selected >= triggers.size()) return true;
        String id = idField.getValue().strip();
        if (!id.matches("[a-z0-9_.-]{1,64}")) return error("id");
        try {
            int priority = Integer.parseInt(priorityField.getValue().strip());
            double chance = Double.parseDouble(chanceField.getValue().strip());
            int cooldown = Integer.parseInt(cooldownField.getValue().strip());
            int limit = Integer.parseInt(limitField.getValue().strip());
            if (priority < -1000 || priority > 1000 || !Double.isFinite(chance)
                    || chance < 0 || chance > 1 || cooldown < 0 || cooldown > 1_728_000
                    || limit < 0 || limit > 1_000_000) return error("number");
            JsonObject copy = triggers.get(selected).deepCopy();
            copy.addProperty("id", id);
            copy.addProperty("event", event);
            copy.addProperty("priority", priority);
            copy.addProperty("chance", chance);
            copy.addProperty("cooldownTicks", cooldown);
            copy.addProperty("activationLimit", limit);
            copy.addProperty("actionFailurePolicy", failurePolicy);
            triggers.set(selected, copy);
            status = "";
            return true;
        } catch (NumberFormatException exception) {
            return error("number");
        }
    }

    private boolean error(String suffix) {
        status = Component.translatable("screen.talent_editor.configured_item.triggers.invalid." + suffix).getString();
        return false;
    }

    private void chooseEvent() {
        if (selected < 0) return;
        options.open(Component.translatable("screen.talent_editor.option.choose"), EVENTS.stream()
                .map(value -> Component.translatable("screen.talent_editor.trigger.event." + value)).toList(),
                Math.max(0, EVENTS.indexOf(event)), index -> {
                    event = EVENTS.get(index);
                    eventButton.setMessage(eventLabel());
                }, height);
    }

    private void choosePolicy() {
        if (selected < 0) return;
        List<String> values = List.of("continue", "stop");
        options.open(Component.translatable("screen.talent_editor.option.choose"), values.stream().map(value ->
                        Component.translatable("screen.talent_editor.configured_item.triggers.policy." + value)).toList(),
                Math.max(0, values.indexOf(failurePolicy)), index -> {
                    failurePolicy = values.get(index);
                    policyButton.setMessage(Component.translatable(
                            "screen.talent_editor.configured_item.triggers.policy." + failurePolicy));
                }, height);
    }

    private void addTrigger() {
        if (triggers.size() >= 256 || !storeSelected()) return;
        JsonObject trigger = new JsonObject();
        int number = triggers.size() + 1;
        while (idExists("item_trigger_" + number)) number++;
        trigger.addProperty("id", "item_trigger_" + number);
        trigger.addProperty("event", "damage_dealt");
        trigger.addProperty("priority", 0);
        trigger.addProperty("chance", 1);
        trigger.addProperty("cooldownTicks", 0);
        trigger.addProperty("activationLimit", 0);
        trigger.addProperty("actionFailurePolicy", "continue");
        JsonObject action = new JsonObject();
        action.addProperty("type", "modify_damage_expression");
        action.addProperty("operation", "add");
        action.addProperty("expression", "0");
        action.addProperty("minimum", 0);
        action.addProperty("maximum", 1_000_000_000);
        JsonArray actions = new JsonArray();
        actions.add(action);
        trigger.add("actions", actions);
        triggers.add(trigger);
        selected = triggers.size() - 1;
        loadSelected();
    }

    private boolean idExists(String id) {
        return triggers.stream().anyMatch(trigger -> id.equals(string(trigger, "id", "")));
    }

    private void removeTrigger() {
        if (selected < 0) return;
        triggers.remove(selected);
        selected = triggers.isEmpty() ? -1 : Math.min(selected, triggers.size() - 1);
        loadSelected();
    }

    private void select(int index) {
        if (index == selected || index < 0 || index >= triggers.size() || !storeSelected()) return;
        selected = index;
        loadSelected();
    }

    private void openActions(boolean failure) {
        if (!storeSelected() || minecraft == null) return;
        minecraft.setScreen(new ConfiguredItemTriggerActionsScreen(this, bootstrap,
                actions(triggers.get(selected), failure ? "failureActions" : "actions"), failure));
    }

    private void openRaw() {
        if (!storeSelected() || minecraft == null) return;
        minecraft.setScreen(new ConfiguredItemJsonScreen(this, triggers.get(selected), this::replaceSelected));
    }

    void replaceSelected(JsonObject trigger) {
        if (selected < 0) return;
        triggers.set(selected, trigger.deepCopy());
        status = "";
    }

    void replaceActions(List<JsonObject> actions, boolean failure) {
        if (selected < 0) return;
        JsonObject trigger = triggers.get(selected).deepCopy();
        JsonArray array = new JsonArray();
        actions.forEach(action -> array.add(action.deepCopy()));
        trigger.add(failure ? "failureActions" : "actions", array);
        triggers.set(selected, trigger);
        status = "";
    }

    private void apply() {
        if (!storeSelected()) return;
        parent.updateTriggers(List.copyOf(triggers));
        close();
    }

    @Override public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics);
        int w = Math.min(760, width - 32), x = (width - w) / 2;
        int leftWidth = Math.min(220, Math.max(120, w * 2 / 5));
        int rx = x + leftWidth + 12, rw = w - leftWidth - 12;
        EditorTheme.panel(graphics, x - 6, 27, leftWidth + 12, Math.max(1, height - 63));
        EditorTheme.panel(graphics, rx - 6, 27, rw + 12, Math.max(1, height - 63));
        super.render(graphics, mouseX, mouseY, partialTick);
        EditorTheme.title(graphics,font,title,width);
        graphics.drawString(font, Component.translatable("screen.talent_editor.configured_item.triggers.list"),
                x + 3, 36, EditorTheme.MUTED, false);
        label(graphics, rx, 59, "id");
        label(graphics, rx, 94, "numbers");
        int visible = Math.max(1, (height - 121) / ROW_HEIGHT);
        scroll = Math.max(0, Math.min(scroll, Math.max(0, triggers.size() - visible)));
        for (int index = scroll; index < Math.min(triggers.size(), scroll + visible); index++) {
            int y = 61 + (index - scroll) * ROW_HEIGHT;
            boolean hover = mouseX >= x && mouseX < x + leftWidth && mouseY >= y - 3 && mouseY < y + 15;
            if (selected == index || hover) graphics.fill(x, y - 3, x + leftWidth, y + 15,
                    selected == index ? 0x7050A0E8 : 0x404A90E2);
            JsonObject trigger = triggers.get(index);
            String text = string(trigger, "id", "?") + " · " + local(string(trigger, "event", "?"));
            graphics.drawString(font, font.plainSubstrByWidth(text, leftWidth - 8), x + 4, y,
                    EditorTheme.TEXT, false);
        }
        if (!status.isBlank()) graphics.drawString(font, font.plainSubstrByWidth(status, w), x,
                height - 76, 0xFFFF7777, false);
        options.render(graphics, font, width, height);
    }

    private void label(GuiGraphics graphics, int x, int y, String suffix) {
        graphics.drawString(font, Component.translatable("screen.talent_editor.configured_item.triggers." + suffix),
                x, y, EditorTheme.MUTED, false);
    }

    @Override public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (options.mouseClicked(mouseX, mouseY, button, width, height)) return true;
        if (button == 0) {
            int w = Math.min(760, width - 32), x = (width - w) / 2;
            int lw = Math.min(220, Math.max(120, w * 2 / 5));
            if (mouseX >= x && mouseX < x + lw && mouseY >= 58 && mouseY < height - 63) {
                select(scroll + (int) ((mouseY - 58) / ROW_HEIGHT));
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }
    @Override public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        if (options.mouseScrolled(delta, height)) return true;
        scroll = Math.max(0, Math.min(scroll - (int) Math.signum(delta) * 3,
                Math.max(0, triggers.size() - Math.max(1, (height - 121) / ROW_HEIGHT))));
        return true;
    }
    @Override public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (options.keyPressed(keyCode, height)) return true;
        return super.keyPressed(keyCode, scanCode, modifiers);
    }
    @Override public boolean charTyped(char codePoint, int modifiers) {
        return options.isOpen() || super.charTyped(codePoint, modifiers);
    }
    private void close() { if (minecraft != null) minecraft.setScreen(parent); }
    @Override public void onClose() { close(); }

    static List<JsonObject> actions(JsonObject trigger, String key) {
        if (!trigger.has(key) || !trigger.get(key).isJsonArray()) return List.of();
        ArrayList<JsonObject> result = new ArrayList<>();
        trigger.getAsJsonArray(key).forEach(value -> {
            if (value.isJsonObject()) result.add(value.getAsJsonObject().deepCopy());
        });
        return List.copyOf(result);
    }
    static int actionCount(JsonObject trigger, String key) { return actions(trigger, key).size(); }
    static String string(JsonObject object, String key, String fallback) {
        try { return object.has(key) ? object.get(key).getAsString() : fallback; }
        catch (RuntimeException ignored) { return fallback; }
    }
    static String local(String id) { return id.startsWith("talent_editor:") ? id.substring(14) : id; }
}
