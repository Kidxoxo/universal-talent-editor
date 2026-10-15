package com.kidxoxo.talenteditor.client;

import com.google.gson.JsonObject;
import com.kidxoxo.talenteditor.domain.NumericExpression;
import com.kidxoxo.talenteditor.network.EditorBootstrap;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import java.util.List;

/** Visual parameters for the three expression actions used by configured-item event rules. */
public final class ConfiguredItemTriggerActionScreen extends EditorScreen {
    private static final List<String> TYPES = List.of(
            "timed_attribute_modifier", "modify_damage_expression", "affect_health_expression");
    private final ConfiguredItemTriggerActionsScreen parent;
    private final EditorBootstrap bootstrap;
    private JsonObject action;
    private String kind;
    private String operation;
    private EditBox attributeField;
    private EditBox expressionField;
    private EditBox valueAField;
    private EditBox valueBField;
    private Button attributeButton;
    private Button typeButton;
    private Button operationButton;
    private Button rawButton;
    private final EditorOptionPopup options = new EditorOptionPopup();
    private String status = "";

    ConfiguredItemTriggerActionScreen(ConfiguredItemTriggerActionsScreen parent,
                                      EditorBootstrap bootstrap, JsonObject action) {
        super(Component.translatable("screen.talent_editor.configured_item.triggers.action_title"));
        this.parent = parent;
        this.bootstrap = bootstrap;
        this.action = action.deepCopy();
        kind = ConfiguredItemTriggerScreen.local(ConfiguredItemTriggerScreen.string(action, "type", ""));
        operation = ConfiguredItemTriggerScreen.string(action, "operation", defaultOperation());
    }

    @Override protected void init() {
        int w = Math.min(680, width - 32), x = (width - w) / 2, half = (w - 6) / 2;
        typeButton = addRenderableWidget(EditorWidgets.button(Component.empty(), b -> chooseType())
                .bounds(x, 32, w, 20).build());
        attributeField = field(x, 70, w - 92, 256);
        attributeButton = addRenderableWidget(EditorWidgets.button(Component.translatable(
                        "screen.talent_editor.configured_item.triggers.choose_attribute"), b -> chooseAttribute())
                .bounds(x + w - 86, 70, 86, 20).build());
        expressionField = field(x, 108, w, NumericExpression.MAX_LENGTH);
        operationButton = addRenderableWidget(EditorWidgets.button(Component.empty(), b -> chooseOperation())
                .bounds(x, 146, w, 20).build());
        valueAField = field(x, 184, half, 160);
        valueBField = field(x + half + 6, 184, w - half - 6, 32);
        rawButton = addRenderableWidget(EditorWidgets.button(Component.translatable(
                        "screen.talent_editor.configured_item.triggers.raw"), b -> openRaw())
                .bounds(x, 216, w, 20).build());
        rawButton.visible = height >= 270;
        addRenderableWidget(EditorWidgets.button(Component.translatable("gui.cancel"), b -> close())
                .bounds(x, height - 28, half, 20).build());
        addRenderableWidget(EditorWidgets.button(Component.translatable(
                        "screen.talent_editor.configured_item.triggers.apply"), b -> apply())
                .bounds(x + half + 6, height - 28, w - half - 6, 20).build());
        load();
    }

    private EditBox field(int x, int y, int w, int max) {
        EditBox box = new EditBox(font, x, y, w, 20, Component.empty());
        box.setMaxLength(max);
        addRenderableWidget(box);
        return box;
    }

    private void load() {
        attributeField.setValue(ConfiguredItemTriggerScreen.string(action, "attribute",
                "minecraft:generic.attack_damage"));
        expressionField.setValue(ConfiguredItemTriggerScreen.string(action, "expression", "0"));
        valueAField.setValue(ConfiguredItemTriggerScreen.string(action,
                kind.equals("timed_attribute_modifier") ? "modifierKey"
                        : kind.equals("modify_damage_expression") ? "minimum" : "maximum",
                kind.equals("timed_attribute_modifier") ? "item_modifier" : "0"));
        valueBField.setValue(ConfiguredItemTriggerScreen.string(action,
                kind.equals("timed_attribute_modifier") ? "durationTicks" : "maximum",
                kind.equals("timed_attribute_modifier") ? "40" : "1000000000"));
        operation = ConfiguredItemTriggerScreen.string(action, "operation", defaultOperation());
        updateControls();
    }

    private String defaultOperation() {
        return kind.equals("timed_attribute_modifier") ? "addition"
                : kind.equals("affect_health_expression") ? "heal" : "add";
    }

    private List<String> operations() {
        return switch (kind) {
            case "timed_attribute_modifier" -> List.of("addition", "multiply_base", "multiply_total");
            case "modify_damage_expression" -> List.of("add", "multiply");
            case "affect_health_expression" -> List.of("damage", "heal");
            default -> List.of();
        };
    }

    private void updateControls() {
        boolean known = TYPES.contains(kind);
        boolean attribute = kind.equals("timed_attribute_modifier");
        typeButton.setMessage(known ? Component.translatable(
                "screen.talent_editor.advanced_action.type." + kind) : Component.literal(kind));
        attributeField.visible = attribute;
        attributeButton.visible = attribute;
        expressionField.visible = known;
        operationButton.visible = known;
        valueAField.visible = known;
        valueBField.visible = known && !kind.equals("affect_health_expression");
        operationButton.setMessage(operationLabel(operation));
    }

    private Component operationLabel(String value) {
        return switch (value) {
            case "addition", "multiply_base", "multiply_total" -> Component.translatable(
                    "screen.talent_editor.operation." + value);
            default -> Component.translatable("screen.talent_editor.advanced_action.operation_value." + value);
        };
    }

    private void chooseType() {
        options.open(Component.translatable("screen.talent_editor.option.choose"), TYPES.stream()
                .map(value -> Component.translatable("screen.talent_editor.advanced_action.type." + value)).toList(),
                Math.max(0, TYPES.indexOf(kind)), index -> {
                    kind = TYPES.get(index);
                    action = new JsonObject();
                    action.addProperty("type", kind);
                    action.addProperty("target", "self");
                    operation = defaultOperation();
                    load();
                }, height);
    }

    private void chooseOperation() {
        List<String> values = operations();
        if (values.isEmpty()) return;
        options.open(Component.translatable("screen.talent_editor.option.choose"), values.stream()
                .map(this::operationLabel).toList(), Math.max(0, values.indexOf(operation)), index -> {
                    operation = values.get(index);
                    updateControls();
                }, height);
    }

    private void chooseAttribute() {
        if (minecraft == null) return;
        snapshot();
        minecraft.setScreen(new ResourceReferencePickerScreen(this,
                ResourceReferencePickerScreen.Source.ATTRIBUTE, bootstrap,
                id -> action.addProperty("attribute", id.toString())));
    }

    private void openRaw() {
        if (minecraft == null) return;
        snapshot();
        minecraft.setScreen(new ConfiguredItemJsonScreen(this, action, value -> {
            action = value.deepCopy();
            kind = ConfiguredItemTriggerScreen.local(ConfiguredItemTriggerScreen.string(action, "type", ""));
        }));
    }

    private void snapshot() {
        if (!TYPES.contains(kind)) return;
        action.addProperty("type", kind);
        action.addProperty("expression", expressionField.getValue());
        action.addProperty("operation", operation);
        if (kind.equals("timed_attribute_modifier")) {
            action.addProperty("attribute", attributeField.getValue());
            action.addProperty("modifierKey", valueAField.getValue());
            action.addProperty("durationTicks", valueBField.getValue());
        } else if (kind.equals("modify_damage_expression")) {
            action.addProperty("minimum", valueAField.getValue());
            action.addProperty("maximum", valueBField.getValue());
        } else action.addProperty("maximum", valueAField.getValue());
    }

    private void apply() {
        if (!TYPES.contains(kind)) {
            parent.replaceSelected(action);
            close();
            return;
        }
        try {
            String expression = expressionField.getValue().strip();
            NumericExpression.compile(expression);
            JsonObject next = action.deepCopy();
            next.addProperty("type", kind);
            next.addProperty("expression", expression);
            next.addProperty("operation", operation);
            if (kind.equals("timed_attribute_modifier")) {
                String id = attributeField.getValue().strip();
                if (ResourceLocation.tryParse(id) == null || !id.contains(":")) throw new IllegalArgumentException("attribute");
                String key = valueAField.getValue().strip();
                if (!key.matches("[a-z0-9_.-]{1,160}")) throw new IllegalArgumentException("modifierKey");
                int duration = Integer.parseInt(valueBField.getValue().strip());
                if (duration < 1 || duration > 1_728_000) throw new IllegalArgumentException("durationTicks");
                next.addProperty("attribute", id);
                next.addProperty("modifierKey", key);
                next.addProperty("durationTicks", duration);
                next.addProperty("target", "self");
            } else if (kind.equals("modify_damage_expression")) {
                double minimum = Double.parseDouble(valueAField.getValue().strip());
                double maximum = Double.parseDouble(valueBField.getValue().strip());
                if (!Double.isFinite(minimum) || !Double.isFinite(maximum) || minimum < 0
                        || maximum > 1_000_000_000 || minimum > maximum) throw new IllegalArgumentException("bounds");
                next.addProperty("minimum", minimum);
                next.addProperty("maximum", maximum);
            } else {
                double maximum = Double.parseDouble(valueAField.getValue().strip());
                if (!Double.isFinite(maximum) || maximum < 0 || maximum > 1_000_000_000)
                    throw new IllegalArgumentException("maximum");
                next.addProperty("maximum", maximum);
                next.addProperty("target", "self");
            }
            parent.replaceSelected(next);
            close();
        } catch (RuntimeException exception) {
            status = exception.getMessage() == null ? "Invalid action" : exception.getMessage();
        }
    }

    @Override public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics);
        int w = Math.min(680, width - 32), x = (width - w) / 2, half = (w - 6) / 2;
        EditorTheme.panel(graphics, x - 7, 27, w + 14, 213);
        super.render(graphics, mouseX, mouseY, partialTick);
        EditorTheme.title(graphics,font,title,width);
        if (kind.equals("timed_attribute_modifier")) {
            label(graphics, x, 59, "attribute");
            label(graphics, x, 173, "modifier_key");
            label(graphics, x + half + 6, 173, "duration");
        } else if (kind.equals("modify_damage_expression")) {
            label(graphics, x, 173, "minimum");
            label(graphics, x + half + 6, 173, "maximum");
        } else if (kind.equals("affect_health_expression")) label(graphics, x, 173, "maximum");
        if (TYPES.contains(kind)) {
            label(graphics, x, 97, "expression");
            label(graphics, x, 135, "operation");
        }
        if (!status.isBlank()) graphics.drawString(font, font.plainSubstrByWidth(status, w), x,
                height - 47, 0xFFFF7777, false);
        options.render(graphics, font, width, height);
    }

    private void label(GuiGraphics graphics, int x, int y, String suffix) {
        graphics.drawString(font, Component.translatable("screen.talent_editor.configured_item.triggers." + suffix),
                x, y, EditorTheme.MUTED, false);
    }
    @Override public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (options.mouseClicked(mouseX, mouseY, button, width, height)) return true;
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
    private void close() { if (minecraft != null) minecraft.setScreen(parent); }
    @Override public void onClose() { close(); }
}
