package com.kidxoxo.talenteditor.client;

import com.google.gson.JsonObject;
import com.kidxoxo.talenteditor.domain.NumericExpression;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import java.util.List;

/** Visual editor for the two independent conditions on an individual action. */
public final class TalentActionGateScreen extends Screen {
    private static final List<String> OPERATORS = List.of("lt", "lte", "eq", "neq", "gte", "gt");
    private final Screen parent;
    private final TalentEditScreen owner;
    private final TalentDraft.AdvancedActionLocation location;
    private final int triggerIndex;
    private final int actionIndex;
    private final String[] keys = {"enabledWhen", "disabledWhen"};
    private int selected;
    private int mode; // none, has_talent, expression_compare, unsupported
    private int operator;
    private EditBox talentId;
    private EditBox left;
    private EditBox right;
    private Button sideButton;
    private Button modeButton;
    private Button operatorButton;
    private String status = "";

    TalentActionGateScreen(Screen parent, TalentEditScreen owner,
                           TalentDraft.AdvancedActionLocation location, int triggerIndex, int actionIndex) {
        super(Component.translatable("screen.talent_editor.action_gate.title"));
        this.parent = parent;
        this.owner = owner;
        this.location = location;
        this.triggerIndex = triggerIndex;
        this.actionIndex = actionIndex;
    }

    @Override protected void init() {
        int w = Math.min(620, width - 32);
        int x = (width - w) / 2;
        sideButton = addRenderableWidget(Button.builder(Component.empty(), button -> {
            selected = 1 - selected;
            load();
        }).bounds(x, 43, (w - 6) / 2, 20).build());
        modeButton = addRenderableWidget(Button.builder(Component.empty(), button -> {
            mode = mode >= 3 ? 0 : (mode + 1) % 3;
            updateControls();
        }).bounds(x + (w - 6) / 2 + 6, 43, w - (w - 6) / 2 - 6, 20).build());
        talentId = field(x, 87, w - 96, 256);
        addRenderableWidget(Button.builder(Component.translatable("screen.talent_editor.action_gate.choose"),
                button -> chooseTalent()).bounds(x + w - 90, 87, 90, 20).build());
        left = field(x, 87, w, 512);
        operatorButton = addRenderableWidget(Button.builder(Component.empty(), button -> {
            operator = (operator + 1) % OPERATORS.size();
            updateControls();
        }).bounds(x, 117, w, 20).build());
        right = field(x, 147, w, 512);
        int third = (w - 12) / 3;
        addRenderableWidget(Button.builder(Component.translatable("gui.cancel"), button -> close())
                .bounds(x, height - 28, third, 20).build());
        addRenderableWidget(Button.builder(Component.translatable("screen.talent_editor.action_gate.clear"),
                button -> clear()).bounds(x + third + 6, height - 28, third, 20).build());
        addRenderableWidget(Button.builder(Component.translatable("gui.done"), button -> save())
                .bounds(x + (third + 6) * 2, height - 28, w - (third + 6) * 2, 20).build());
        load();
    }

    private EditBox field(int x, int y, int w, int max) {
        EditBox box = new EditBox(font, x, y, w, 20, Component.empty());
        box.setMaxLength(max);
        addRenderableWidget(box);
        return box;
    }

    private void load() {
        JsonObject action = owner.draft().advancedAction(location, triggerIndex, actionIndex);
        JsonObject condition = action.has(keys[selected]) && action.get(keys[selected]).isJsonObject()
                ? action.getAsJsonObject(keys[selected]) : null;
        String type = condition == null ? "" : string(condition, "type");
        if (type.startsWith("talent_editor:")) type = type.substring("talent_editor:".length());
        mode = condition == null ? 0 : type.equals("has_talent") ? 1 : type.equals("expression_compare") ? 2 : 3;
        talentId.setValue(condition == null ? "" : string(condition, "talent"));
        left.setValue(condition == null ? "0" : defaulted(string(condition, "left"), "0"));
        right.setValue(condition == null ? "0" : defaulted(string(condition, "right"), "0"));
        operator = Math.max(0, OPERATORS.indexOf(condition == null ? "gte" : string(condition, "operator")));
        status = mode == 3 ? Component.translatable("screen.talent_editor.action_gate.unsupported").getString() : "";
        updateControls();
    }

    private void updateControls() {
        sideButton.setMessage(Component.translatable(selected == 0
                ? "screen.talent_editor.action_gate.enabled" : "screen.talent_editor.action_gate.disabled"));
        modeButton.setMessage(Component.translatable("screen.talent_editor.action_gate.mode." + mode));
        talentId.visible = mode == 1;
        left.visible = mode == 2;
        right.visible = mode == 2;
        operatorButton.visible = mode == 2;
        operatorButton.setMessage(Component.translatable(
                "screen.talent_editor.expression_condition.operator_help." + OPERATORS.get(operator)));
    }

    private void chooseTalent() {
        if (mode != 1 || minecraft == null) return;
        minecraft.setScreen(new ResourceReferencePickerScreen(this,
                ResourceReferencePickerScreen.Source.TALENT, owner.bootstrap(),
                id -> talentId.setValue(id.toString())));
    }

    private void clear() {
        JsonObject action = owner.draft().advancedAction(location, triggerIndex, actionIndex);
        action.remove(keys[selected]);
        owner.draft().putAdvancedAction(location, triggerIndex, actionIndex, action);
        ClientEditorState.clearValidation();
        load();
    }

    private void save() {
        if (mode == 3) {
            status = Component.translatable("screen.talent_editor.action_gate.unsupported").getString();
            return;
        }
        JsonObject action = owner.draft().advancedAction(location, triggerIndex, actionIndex);
        if (mode == 0) action.remove(keys[selected]);
        else {
            JsonObject condition = new JsonObject();
            if (mode == 1) {
                ResourceLocation id = ResourceLocation.tryParse(talentId.getValue().strip());
                if (id == null) { status = "Invalid talent ID"; return; }
                condition.addProperty("type", "talent_editor:has_talent");
                condition.addProperty("talent", id.toString());
            } else {
                String leftValue = left.getValue().strip();
                String rightValue = right.getValue().strip();
                try {
                    NumericExpression.compile(leftValue);
                    NumericExpression.compile(rightValue);
                } catch (IllegalArgumentException error) {
                    status = error.getMessage() == null ? "Invalid expression" : error.getMessage();
                    return;
                }
                condition.addProperty("type", "talent_editor:expression_compare");
                condition.addProperty("left", leftValue);
                condition.addProperty("operator", OPERATORS.get(operator));
                condition.addProperty("right", rightValue);
            }
            action.add(keys[selected], condition);
        }
        owner.draft().putAdvancedAction(location, triggerIndex, actionIndex, action);
        ClientEditorState.clearValidation();
        close();
    }

    @Override public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics);
        int w = Math.min(620, width - 32);
        int x = (width - w) / 2;
        EditorTheme.panel(graphics, x - 7, 27, w + 14, 158);
        super.render(graphics, mouseX, mouseY, partialTick);
        graphics.drawCenteredString(font, title, width / 2, 10, EditorTheme.TEXT);
        graphics.drawString(font, Component.translatable("screen.talent_editor.action_gate.help"), x, 69,
                EditorTheme.MUTED, false);
        if (!status.isBlank()) graphics.drawString(font, font.plainSubstrByWidth(status, w), x,
                height - 47, 0xFFFF7777, false);
    }

    private static String string(JsonObject object, String key) {
        return object.has(key) && object.get(key).isJsonPrimitive() ? object.get(key).getAsString() : "";
    }
    private static String defaulted(String value, String fallback) { return value.isBlank() ? fallback : value; }
    private void close() { if (minecraft != null) minecraft.setScreen(parent); }
    @Override public void onClose() { close(); }
}
