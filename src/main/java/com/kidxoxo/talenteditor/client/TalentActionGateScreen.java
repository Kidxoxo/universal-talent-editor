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
public final class TalentActionGateScreen extends EditorScreen {
    private static final List<String> OPERATORS = List.of("lt", "lte", "eq", "neq", "gte", "gt");
    private final Screen parent;
    private final TalentEditScreen owner;
    private final TalentDraft.AdvancedActionLocation location;
    private final int triggerIndex;
    private final int actionIndex;
    private final String[] keys = {"enabledWhen", "disabledWhen"};
    private JsonObject working;
    private int selected;
    private int mode; // none, has_talent, expression_compare, unsupported
    private int operator;
    private EditBox talentId;
    private EditBox left;
    private EditBox right;
    private Button modeButton;
    private Button operatorButton;
    private Button chooseTalentButton;
    private final EditorOptionPopup options = new EditorOptionPopup();
    private String status = "";

    TalentActionGateScreen(Screen parent, TalentEditScreen owner,
                           TalentDraft.AdvancedActionLocation location, int triggerIndex, int actionIndex) {
        super(Component.translatable("screen.talent_editor.action_gate.title"));
        this.parent = parent;
        this.owner = owner;
        this.location = location;
        this.triggerIndex = triggerIndex;
        this.actionIndex = actionIndex;
        this.working = owner.draft().advancedAction(location, triggerIndex, actionIndex);
    }

    @Override protected void init() {
        int w = Math.min(620, width - 32);
        int x = (width - w) / 2;
        int listWidth = Math.min(190, Math.max(110, w * 38 / 100));
        int rightX = x + listWidth + 12;
        int rightWidth = w - listWidth - 12;
        modeButton = addRenderableWidget(EditorWidgets.button(Component.empty(), button ->
                options.open(Component.translatable("screen.talent_editor.option.choose"),
                        List.of(0, 1, 2).stream().map(index -> Component.translatable(
                                "screen.talent_editor.action_gate.mode." + index)).toList(),
                        Math.min(mode, 2), index -> { mode = index; status = ""; updateControls(); }, height))
                .bounds(rightX, 43, rightWidth, 20).build());
        talentId = field(rightX, 87, rightWidth - 96, 256);
        chooseTalentButton = addRenderableWidget(EditorWidgets.button(Component.translatable(
                        "screen.talent_editor.action_gate.choose"), button -> chooseTalent())
                .bounds(rightX + rightWidth - 90, 87, 90, 20).build());
        left = field(rightX, 87, rightWidth, 512);
        operatorButton = addRenderableWidget(EditorWidgets.button(Component.empty(), button ->
                options.open(Component.translatable("screen.talent_editor.option.choose"),
                        OPERATORS.stream().map(value -> Component.translatable(
                                "screen.talent_editor.expression_condition.operator_help." + value)).toList(),
                        operator, index -> { operator = index; updateControls(); }, height))
                .bounds(rightX, 117, rightWidth, 20).build());
        right = field(rightX, 147, rightWidth, 512);
        int third = (w - 12) / 3;
        addRenderableWidget(EditorWidgets.button(Component.translatable("gui.cancel"), button -> close())
                .bounds(x, height - 28, third, 20).build());
        addRenderableWidget(EditorWidgets.button(Component.translatable("screen.talent_editor.action_gate.clear"),
                button -> clear()).bounds(x + third + 6, height - 28, third, 20).build());
        addRenderableWidget(EditorWidgets.button(Component.translatable("gui.done"), button -> save())
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
        JsonObject condition = working.has(keys[selected]) && working.get(keys[selected]).isJsonObject()
                ? working.getAsJsonObject(keys[selected]) : null;
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
        modeButton.setMessage(Component.translatable("screen.talent_editor.action_gate.mode." + mode));
        talentId.visible = mode == 1;
        chooseTalentButton.visible = mode == 1;
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
                id -> {
                    talentId.setValue(id.toString());
                    storeCurrent();
                }));
    }

    private void clear() {
        working.remove(keys[selected]);
        load();
    }

    private void save() {
        if (!storeCurrent()) return;
        owner.draft().putAdvancedAction(location, triggerIndex, actionIndex, working);
        ClientEditorState.clearValidation();
        close();
    }

    private boolean storeCurrent() {
        if (mode == 3) return true; // Keep unfamiliar JSON conditions unchanged.
        if (mode == 0) working.remove(keys[selected]);
        else {
            JsonObject condition = new JsonObject();
            if (mode == 1) {
                ResourceLocation id = ResourceLocation.tryParse(talentId.getValue().strip());
                if (id == null) { status = "Invalid talent ID"; return false; }
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
                    return false;
                }
                condition.addProperty("type", "talent_editor:expression_compare");
                condition.addProperty("left", leftValue);
                condition.addProperty("operator", OPERATORS.get(operator));
                condition.addProperty("right", rightValue);
            }
            working.add(keys[selected], condition);
        }
        status = "";
        return true;
    }

    private void selectSide(int index) {
        if (index == selected || !storeCurrent()) return;
        selected = index;
        load();
    }

    @Override public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics);
        int w = Math.min(620, width - 32);
        int x = (width - w) / 2;
        int listWidth = Math.min(190, Math.max(110, w * 38 / 100));
        int rightX = x + listWidth + 12;
        EditorTheme.panel(graphics, x - 7, 27, w + 14, height >= 260 ? 178 : 155);
        super.render(graphics, mouseX, mouseY, partialTick);
        EditorTheme.title(graphics,font,title,width);
        graphics.drawString(font, Component.translatable("screen.talent_editor.action_gate.direction_list"),
                x + 3, 46, EditorTheme.MUTED, false);
        for (int index = 0; index < keys.length; index++) {
            int rowY = 69 + index * 34;
            boolean hovered = mouseX >= x && mouseX < x + listWidth && mouseY >= rowY - 4 && mouseY < rowY + 23;
            if (selected == index || hovered) graphics.fill(x, rowY - 4, x + listWidth, rowY + 23,
                    selected == index ? 0x7050A0E8 : 0x404A90E2);
            String label = Component.translatable(index == 0
                    ? "screen.talent_editor.action_gate.enabled" : "screen.talent_editor.action_gate.disabled")
                    .getString();
            graphics.drawString(font, font.plainSubstrByWidth(label, listWidth - 8), x + 4, rowY,
                    EditorTheme.TEXT, false);
            String detail = index == selected
                    ? Component.translatable("screen.talent_editor.action_gate.mode." + mode).getString()
                    : conditionSummary(index);
            graphics.drawString(font, font.plainSubstrByWidth(detail, listWidth - 8), x + 4, rowY + 11,
                    EditorTheme.MUTED, false);
        }
        if (mode == 1) graphics.drawString(font, Component.translatable(
                "screen.talent_editor.action_gate.talent_label"), rightX, 73, EditorTheme.MUTED, false);
        if (mode == 2) {
            graphics.drawString(font, Component.translatable("screen.talent_editor.expression_condition.left"),
                    rightX, 73, EditorTheme.MUTED, false);
            graphics.drawString(font, Component.translatable("screen.talent_editor.expression_condition.right"),
                    rightX, 138, EditorTheme.MUTED, false);
        }
        if (height >= 260) graphics.drawString(font, font.plainSubstrByWidth(Component.translatable(
                "screen.talent_editor.action_gate.help").getString(), w), x, 190, EditorTheme.MUTED, false);
        if (!status.isBlank()) graphics.drawString(font, font.plainSubstrByWidth(status, w), x,
                height - 47, 0xFFFF7777, false);
        options.render(graphics, font, width, height);
    }

    private String conditionSummary(int index) {
        if (!working.has(keys[index]) || !working.get(keys[index]).isJsonObject())
            return Component.translatable("screen.talent_editor.action_gate.mode.0").getString();
        String type = string(working.getAsJsonObject(keys[index]), "type");
        if (type.equals("has_talent") || type.endsWith(":has_talent")) return Component.translatable(
                "screen.talent_editor.action_gate.mode.1").getString();
        if (type.equals("expression_compare") || type.endsWith(":expression_compare")) return Component.translatable(
                "screen.talent_editor.action_gate.mode.2").getString();
        return Component.translatable("screen.talent_editor.action_gate.mode.3").getString();
    }

    @Override public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (options.mouseClicked(mouseX, mouseY, button, width, height)) return true;
        if (button == 0) {
            int w = Math.min(620, width - 32);
            int x = (width - w) / 2;
            int listWidth = Math.min(190, Math.max(110, w * 38 / 100));
            for (int index = 0; index < keys.length; index++) {
                int rowY = 69 + index * 34;
                if (mouseX >= x && mouseX < x + listWidth && mouseY >= rowY - 4 && mouseY < rowY + 23) {
                    selectSide(index);
                    return true;
                }
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }
    @Override public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        if (options.mouseScrolled(delta, height)) return true;
        return super.mouseScrolled(mouseX, mouseY, delta);
    }
    @Override public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (options.keyPressed(keyCode, height)) return true;
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override public boolean charTyped(char codePoint, int modifiers) {
        return options.isOpen() || super.charTyped(codePoint, modifiers);
    }

    private static String string(JsonObject object, String key) {
        return object.has(key) && object.get(key).isJsonPrimitive() ? object.get(key).getAsString() : "";
    }
    private static String defaulted(String value, String fallback) { return value.isBlank() ? fallback : value; }
    private void close() { if (minecraft != null) minecraft.setScreen(parent); }
    @Override public void onClose() { close(); }
}
