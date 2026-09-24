package com.kidxoxo.talenteditor.client;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.kidxoxo.talenteditor.TalentEditorMod;
import com.kidxoxo.talenteditor.domain.NumericExpression;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import java.util.Arrays;
import java.util.List;

/** Form editor for every generic action introduced by the 1.1 runtime. */
public final class TalentAdvancedActionScreen extends Screen {
    private final Screen returnScreen;
    private final TalentEditScreen owner;
    private final TalentDraft.AdvancedActionLocation location;
    private final int triggerRawIndex;
    private final int actionIndex;
    private final List<ActionKind> allowedKinds;
    private JsonObject draft;
    private ActionKind kind;
    private SelectorDraft selector;
    private String operation;
    private boolean firstToggle;
    private boolean secondToggle;
    private boolean thirdToggle;

    private Button typeButton;
    private EditBox resourceField;
    private Button chooseResourceButton;
    private EditBox expressionField;
    private EditBox valueAField;
    private EditBox valueBField;
    private Button operationButton;
    private Button firstToggleButton;
    private Button secondToggleButton;
    private Button thirdToggleButton;
    private Button selectorButton;
    private String status = "";

    TalentAdvancedActionScreen(
            Screen returnScreen,
            TalentEditScreen owner,
            TalentDraft.AdvancedActionLocation location,
            int triggerRawIndex,
            int actionIndex
    ) {
        super(Component.translatable("screen.talent_editor.advanced_action.title"));
        this.returnScreen = returnScreen;
        this.owner = owner;
        this.location = location;
        this.triggerRawIndex = triggerRawIndex;
        this.actionIndex = actionIndex;
        this.allowedKinds = Arrays.stream(ActionKind.values())
                .filter(value -> value.allowedAt(location))
                .toList();
        this.draft = actionIndex < 0
                ? defaults(allowedKinds.get(0))
                : owner.draft().advancedAction(location, triggerRawIndex, actionIndex);
        this.kind = ActionKind.from(draft);
        if (kind == null || !allowedKinds.contains(kind)) kind = allowedKinds.get(0);
        loadState();
    }

    @Override
    protected void init() {
        int formWidth = Math.min(700, width - 32);
        int x = (width - formWidth) / 2;
        int gap = 6;
        int half = (formWidth - gap) / 2;
        typeButton = addRenderableWidget(Button.builder(Component.empty(), button -> cycleType())
                .bounds(x, 29, formWidth, 20).build());
        resourceField = field(x, 61, formWidth - 92, 256);
        chooseResourceButton = addRenderableWidget(Button.builder(Component.translatable(
                        "screen.talent_editor.advanced_action.choose"), button -> chooseResource())
                .bounds(x + formWidth - 86, 61, 86, 20).build());
        expressionField = field(x, 91, formWidth, 512);
        valueAField = field(x, 121, half, NumericExpression.MAX_LENGTH);
        valueBField = field(x + half + gap, 121, formWidth - half - gap, NumericExpression.MAX_LENGTH);
        operationButton = addRenderableWidget(Button.builder(Component.empty(), button -> cycleOperation())
                .bounds(x, 211, half, 20).build());
        firstToggleButton = addRenderableWidget(Button.builder(Component.empty(), button -> {
            firstToggle = !firstToggle;
            updateWidgets();
        }).bounds(x + half + gap, 211, formWidth - half - gap, 20).build());
        secondToggleButton = addRenderableWidget(Button.builder(Component.empty(), button -> {
            secondToggle = !secondToggle;
            updateWidgets();
        }).bounds(x, 241, half, 20).build());
        thirdToggleButton = addRenderableWidget(Button.builder(Component.empty(), button -> {
            thirdToggle = !thirdToggle;
            updateWidgets();
        }).bounds(x + half + gap, 241, formWidth - half - gap, 20).build());
        selectorButton = addRenderableWidget(Button.builder(Component.empty(), button -> openSelector())
                .bounds(x + half + gap, 241, formWidth - half - gap, 20).build());
        addRenderableWidget(Button.builder(Component.translatable("gui.cancel"), button -> returnParent())
                .bounds(x, height - 28, 80, 20).build());
        addRenderableWidget(Button.builder(Component.translatable("gui.done"), button -> apply())
                .bounds(x + 86, height - 28, formWidth - 86, 20).build());
        loadFields();
    }

    private EditBox field(int x, int y, int width, int maxLength) {
        EditBox field = new EditBox(font, x, y, width, 20, Component.empty());
        field.setMaxLength(maxLength);
        field.setResponder(ignored -> status = "");
        addRenderableWidget(field);
        return field;
    }

    private void loadState() {
        operation = string(draft, "operation", kind.defaultOperation());
        firstToggle = switch (kind) {
            case GRANT_TALENT -> bool(draft, "onlyIfNeverAcquired", true);
            case REVOKE_TALENT -> bool(draft, "retainEffects", false);
            case GIVE_ITEM -> bool(draft, "dropOverflow", true);
            case AFFECT_HEALTH_EXPRESSION -> bool(draft, "invertNegative", false);
            case HUD_RESOURCE -> bool(draft, "showWhenFull", true);
            case PLAYER_RENDER -> bool(draft, "hideNameplate", false);
            case OPEN_ITEM_SUBMISSION -> bool(draft, "consume", true);
            default -> false;
        };
        secondToggle = kind == ActionKind.REVOKE_TALENT
                && bool(draft, "retainCategoryContribution", false);
        if (kind == ActionKind.GRANT_TALENT) secondToggle = bool(draft, "synthesized", false);
        thirdToggle = kind == ActionKind.REVOKE_TALENT
                && bool(draft, "retainTriggers", false);
        selector = SelectorDraft.from(draft, location.triggerScoped() ? "event_entity" : "self");
    }

    private void loadFields() {
        resourceField.setValue(string(draft, kind.resourceField(), kind.defaultResource()));
        expressionField.setValue(string(draft, kind.expressionField(), kind.defaultExpression()));
        switch (kind) {
            case MODIFY_STATE -> bounds("-1000000000000", "1000000000000");
            case MODIFY_DAMAGE_EXPRESSION -> bounds("0", "1000000000");
            case AFFECT_HEALTH_EXPRESSION -> {
                valueAField.setValue(string(draft, "maximum", "1000000000"));
                valueBField.setValue("");
            }
            case MODIFY_ATTRIBUTE_BASE_EXPRESSION -> bounds("-1000000000", "1000000000");
            case TIMED_ATTRIBUTE_MODIFIER -> {
                valueAField.setValue(string(draft, "modifierKey", "advanced_modifier"));
                valueBField.setValue(string(draft, "durationTicks", "100"));
            }
            case MODIFY_COUNTER -> bounds("0", "1000000000000");
            case HUD_RESOURCE -> {
                valueAField.setValue(string(draft, "color", "5614335"));
                valueBField.setValue(string(draft, "order", "0"));
            }
            case PLAYER_RENDER -> {
                valueAField.setValue(string(draft, "alpha", "1.0"));
                valueBField.setValue(string(draft, "scale", "1.0"));
            }
            case OPEN_ITEM_SUBMISSION -> {
                valueAField.setValue(string(draft, "timeoutTicks", "600"));
                valueBField.setValue("");
            }
            default -> {
                valueAField.setValue("");
                valueBField.setValue("");
            }
        }
        updateWidgets();
    }

    private void bounds(String minimum, String maximum) {
        valueAField.setValue(string(draft, "minimum", minimum));
        valueBField.setValue(string(draft, "maximum", maximum));
    }

    private void updateWidgets() {
        if (typeButton == null) return;
        typeButton.setMessage(Component.translatable("screen.talent_editor.advanced_action.type",
                Component.translatable("screen.talent_editor.advanced_action.type." + kind.serializedName())));
        boolean resource = kind.hasResource();
        resourceField.visible = resource;
        resourceField.setEditable(resource);
        boolean picker = resource && kind.resourceSource() != null;
        chooseResourceButton.visible = picker;
        resourceField.setWidth(picker ? Math.min(700, width - 32) - 92 : Math.min(700, width - 32));
        boolean expression = kind.hasExpression();
        expressionField.visible = expression;
        expressionField.setEditable(expression);
        boolean values = kind.hasValues();
        valueAField.visible = values;
        valueAField.setEditable(values);
        boolean secondValue = values && kind != ActionKind.AFFECT_HEALTH_EXPRESSION
                && kind != ActionKind.OPEN_ITEM_SUBMISSION;
        valueBField.visible = secondValue;
        valueBField.setEditable(secondValue);
        boolean operationVisible = !kind.operations().isEmpty();
        operationButton.visible = operationVisible;
        operationButton.setMessage(Component.translatable("screen.talent_editor.advanced_action.operation",
                operation));
        firstToggleButton.visible = kind.firstToggleKey() != null;
        if (kind.firstToggleKey() != null) firstToggleButton.setMessage(toggleLabel(
                kind.firstToggleKey(), firstToggle
        ));
        secondToggleButton.visible = kind.secondToggleKey() != null;
        if (kind.secondToggleKey() != null) secondToggleButton.setMessage(toggleLabel(
                kind.secondToggleKey(), secondToggle
        ));
        thirdToggleButton.visible = kind == ActionKind.REVOKE_TALENT;
        if (thirdToggleButton.visible) thirdToggleButton.setMessage(toggleLabel(
                "retain_triggers", thirdToggle
        ));
        selectorButton.visible = kind.selector();
        selectorButton.setMessage(Component.translatable("screen.talent_editor.advanced_action.selector",
                Component.translatable("screen.talent_editor.advanced_selector.target." + selector.target())));
    }

    private Component toggleLabel(String key, boolean value) {
        return Component.translatable("screen.talent_editor.advanced_action." + key,
                Component.translatable(value ? "options.on" : "options.off"));
    }

    private void cycleType() {
        int next = (allowedKinds.indexOf(kind) + 1) % allowedKinds.size();
        kind = allowedKinds.get(next);
        draft = defaults(kind);
        loadState();
        loadFields();
        status = "";
    }

    private void cycleOperation() {
        List<String> operations = kind.operations();
        if (operations.isEmpty()) return;
        int current = Math.max(0, operations.indexOf(operation));
        operation = operations.get((current + 1) % operations.size());
        updateWidgets();
    }

    private void chooseResource() {
        if (minecraft == null || kind.resourceSource() == null) return;
        snapshot();
        minecraft.setScreen(new ResourceReferencePickerScreen(
                this, kind.resourceSource(), owner.bootstrap(),
                kind == ActionKind.MODIFY_STATE || kind == ActionKind.HUD_RESOURCE
                        ? owner.draft().numericStateIds() : List.of(), id -> {
                    draft.addProperty(kind.resourceField(), id.toString());
                    resourceField.setValue(id.toString());
                }
        ));
    }

    private void openSelector() {
        if (minecraft == null || !kind.selector()) return;
        snapshot();
        minecraft.setScreen(new TalentEntitySelectorScreen(this, selector));
    }

    void applySelector(SelectorDraft value) {
        selector = value;
        writeSelector(draft, selector);
    }

    private void snapshot() {
        try {
            draft = serialize(false);
        } catch (RuntimeException ignored) {
        }
    }

    private void apply() {
        try {
            JsonObject serialized = serialize(true);
            owner.draft().putAdvancedAction(location, triggerRawIndex, actionIndex, serialized);
            ClientEditorState.clearValidation();
            returnParent();
        } catch (RuntimeException exception) {
            status = exception.getMessage() == null ? "Invalid value" : exception.getMessage();
        }
    }

    private JsonObject serialize(boolean strict) {
        JsonObject result = new JsonObject();
        result.addProperty("type", TalentEditorMod.MOD_ID + ":" + kind.serializedName());
        if (kind.hasResource()) {
            String raw = resourceField.getValue().strip();
            if (strict && (ResourceLocation.tryParse(raw) == null || !raw.contains(":"))) {
                throw new IllegalArgumentException(Component.translatable(
                        "screen.talent_editor.advanced_action.invalid_resource").getString());
            }
            result.addProperty(kind.resourceField(), raw);
        }
        if (kind.hasExpression()) {
            String expression = expressionField.getValue().strip();
            if (strict) NumericExpression.compile(expression);
            result.addProperty(kind.expressionField(), expression);
        }
        if (!kind.operations().isEmpty()) result.addProperty("operation", operation);
        switch (kind) {
            case MODIFY_STATE, MODIFY_DAMAGE_EXPRESSION, MODIFY_ATTRIBUTE_BASE_EXPRESSION, MODIFY_COUNTER -> {
                double minimum = number(valueAField.getValue(), strict);
                double maximum = number(valueBField.getValue(), strict);
                if (strict && minimum > maximum) throw new IllegalArgumentException(Component.translatable(
                        "screen.talent_editor.advanced_action.invalid_bounds").getString());
                if (kind == ActionKind.MODIFY_COUNTER) {
                    result.addProperty("minimum", whole(minimum, strict));
                    result.addProperty("maximum", whole(maximum, strict));
                } else {
                    result.addProperty("minimum", minimum);
                    result.addProperty("maximum", maximum);
                }
            }
            case AFFECT_HEALTH_EXPRESSION -> result.addProperty(
                    "maximum", number(valueAField.getValue(), strict)
            );
            case TIMED_ATTRIBUTE_MODIFIER -> {
                String modifierKey = valueAField.getValue().strip();
                if (strict && !modifierKey.matches("[a-z0-9_.-]{1,160}")) {
                    throw new IllegalArgumentException(Component.translatable(
                            "screen.talent_editor.advanced_action.invalid_modifier_key").getString());
                }
                result.addProperty("modifierKey", modifierKey);
                result.addProperty("durationTicks", whole(number(valueBField.getValue(), strict), strict));
            }
            case HUD_RESOURCE -> {
                long color = whole(number(valueAField.getValue(), strict), strict);
                long order = whole(number(valueBField.getValue(), strict), strict);
                if (strict && (color < 0 || color > 0xFFFFFF || order < -10_000 || order > 10_000)) {
                    throw new IllegalArgumentException("Invalid HUD color or order");
                }
                result.addProperty("color", color);
                result.addProperty("order", order);
            }
            case PLAYER_RENDER -> {
                String alpha = valueAField.getValue().strip();
                String scale = valueBField.getValue().strip();
                if (strict) {
                    NumericExpression.compile(alpha);
                    NumericExpression.compile(scale);
                }
                result.addProperty("alpha", alpha);
                result.addProperty("scale", scale);
            }
            case OPEN_ITEM_SUBMISSION -> {
                long timeout = whole(number(valueAField.getValue(), strict), strict);
                if (strict && (timeout < 20 || timeout > 72_000)) throw new IllegalArgumentException(
                        "Invalid submission timeout");
                result.addProperty("timeoutTicks", timeout);
            }
            default -> {
            }
        }
        if (kind == ActionKind.GRANT_TALENT) {
            result.addProperty("onlyIfNeverAcquired", firstToggle);
            result.addProperty("synthesized", secondToggle);
        }
        if (kind == ActionKind.REVOKE_TALENT) {
            result.addProperty("retainEffects", firstToggle);
            result.addProperty("retainCategoryContribution", secondToggle);
            result.addProperty("retainTriggers", thirdToggle);
        }
        if (kind == ActionKind.GIVE_ITEM) result.addProperty("dropOverflow", firstToggle);
        if (kind == ActionKind.AFFECT_HEALTH_EXPRESSION) {
            result.addProperty("invertNegative", firstToggle);
        }
        if (kind == ActionKind.HUD_RESOURCE) result.addProperty("showWhenFull", firstToggle);
        if (kind == ActionKind.PLAYER_RENDER) result.addProperty("hideNameplate", firstToggle);
        if (kind == ActionKind.OPEN_ITEM_SUBMISSION) result.addProperty("consume", firstToggle);
        if (kind.selector()) writeSelector(result, selector);
        if (draft.has("enabledWhen")) result.add("enabledWhen", draft.get("enabledWhen").deepCopy());
        if (draft.has("disabledWhen")) result.add("disabledWhen", draft.get("disabledWhen").deepCopy());
        return result;
    }

    private static double number(String value, boolean strict) {
        try {
            double parsed = Double.parseDouble(value.strip());
            if (!Double.isFinite(parsed)) throw new NumberFormatException();
            return parsed;
        } catch (NumberFormatException exception) {
            if (strict) throw new IllegalArgumentException("Invalid number: " + value);
            return 0.0D;
        }
    }

    private static long whole(double value, boolean strict) {
        if (strict && (value != Math.rint(value) || value < Long.MIN_VALUE || value > Long.MAX_VALUE)) {
            throw new IllegalArgumentException("Expected a whole number");
        }
        return (long) value;
    }

    private static void writeSelector(JsonObject result, SelectorDraft selector) {
        result.addProperty("target", selector.target());
        result.addProperty("radius", selector.radius());
        if (selector.target().equals("nearby")) {
            result.addProperty("relation", selector.relation());
            result.addProperty("order", selector.order());
            result.addProperty("limit", selector.limit());
        }
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics);
        int formWidth = Math.min(700, width - 32);
        int x = (width - formWidth) / 2;
        EditorTheme.panel(graphics, x - 7, 24, formWidth + 14, Math.min(284, height - 56));
        super.render(graphics, mouseX, mouseY, partialTick);
        graphics.drawCenteredString(font, title, width / 2, 10, EditorTheme.TEXT);
        if (kind.hasResource()) label(graphics, kind.resourceLabel(), x, 50);
        if (kind.hasExpression()) label(graphics, kind.expressionLabel(), x, 80);
        if (kind.hasValues()) {
            label(graphics, kind.valueALabel(), x, 110);
            if (kind != ActionKind.AFFECT_HEALTH_EXPRESSION
                    && kind != ActionKind.OPEN_ITEM_SUBMISSION) {
                label(graphics, kind.valueBLabel(), x + (formWidth - 6) / 2 + 6, 110);
            }
        }
        if (kind.hasExpression() || kind == ActionKind.PLAYER_RENDER) {
            graphics.drawWordWrap(font, Component.translatable(
                    kind == ActionKind.PLAYER_RENDER
                            ? "screen.talent_editor.advanced_action.render_expression_help"
                            : "screen.talent_editor.advanced_action.expression_help"
            ), x, 270, formWidth, EditorTheme.MUTED);
        }
        if (!status.isBlank()) graphics.drawString(font, font.plainSubstrByWidth(status, formWidth),
                x, height - 43, 0xFF7777, false);
    }

    private void label(GuiGraphics graphics, String key, int x, int y) {
        graphics.drawString(font, Component.translatable(
                "screen.talent_editor.advanced_action.field." + key), x, y, EditorTheme.MUTED, false);
    }

    private JsonObject defaults(ActionKind value) {
        JsonObject result = new JsonObject();
        result.addProperty("type", TalentEditorMod.MOD_ID + ":" + value.serializedName());
        if (value.hasResource()) result.addProperty(value.resourceField(), value.defaultResource());
        if (value.hasExpression()) result.addProperty(value.expressionField(), value.defaultExpression());
        if (!value.operations().isEmpty()) result.addProperty("operation", value.defaultOperation());
        if (value.selector()) writeSelector(result, new SelectorDraft(
                location.triggerScoped() ? "event_entity" : "self", 4.0D, "any", "nearest", 64
        ));
        return result;
    }

    private static String string(JsonObject object, String key, String fallback) {
        if (key == null || key.isBlank()) return "";
        JsonElement value = object.get(key);
        try {
            return value == null ? fallback : value.getAsString();
        } catch (RuntimeException ignored) {
            return fallback;
        }
    }

    private static boolean bool(JsonObject object, String key, boolean fallback) {
        try {
            return object.has(key) ? object.get(key).getAsBoolean() : fallback;
        } catch (RuntimeException ignored) {
            return fallback;
        }
    }

    private void returnParent() { if (minecraft != null) minecraft.setScreen(returnScreen); }
    @Override public void onClose() { returnParent(); }

    record SelectorDraft(String target, double radius, String relation, String order, int limit) {
        static SelectorDraft from(JsonObject object, String defaultTarget) {
            return new SelectorDraft(
                    string(object, "target", defaultTarget),
                    numeric(object, "radius", 4.0D),
                    string(object, "relation", "any"),
                    string(object, "order", "nearest"),
                    integer(object, "limit", 64)
            );
        }

        private static double numeric(JsonObject object, String key, double fallback) {
            try { return object.has(key) ? object.get(key).getAsDouble() : fallback; }
            catch (RuntimeException ignored) { return fallback; }
        }

        private static int integer(JsonObject object, String key, int fallback) {
            try { return object.has(key) ? object.get(key).getAsInt() : fallback; }
            catch (RuntimeException ignored) { return fallback; }
        }
    }

    private enum ActionKind {
        MODIFY_STATE("modify_state", true, true, true, "state", "state", "example:value",
                "expression", "expression", "1", List.of("set", "add", "multiply"), "add",
                null, null, "minimum", "maximum", ResourceReferencePickerScreen.Source.NUMERIC_STATE),
        MODIFY_DAMAGE_EXPRESSION("modify_damage_expression", false, true, false, null, "", "",
                "expression", "expression", "0", List.of("add", "multiply"), "add",
                null, null, "minimum", "maximum", null),
        AFFECT_HEALTH_EXPRESSION("affect_health_expression", false, true, true, null, "", "",
                "expression", "expression", "0", List.of("damage", "heal"), "damage",
                "invert_negative", null, "maximum", "", null),
        MODIFY_ATTRIBUTE_BASE_EXPRESSION("modify_attribute_base_expression", false, true, true,
                "attribute", "attribute", "minecraft:generic.max_health", "expression", "expression", "0",
                List.of("set", "add", "multiply"), "add", null, null,
                "minimum", "maximum", ResourceReferencePickerScreen.Source.ATTRIBUTE),
        TIMED_ATTRIBUTE_MODIFIER("timed_attribute_modifier", false, true, true,
                "attribute", "attribute", "minecraft:generic.movement_speed", "expression", "expression", "0",
                List.of("addition", "multiply_base", "multiply_total"), "addition", null, null,
                "modifier_key", "duration", ResourceReferencePickerScreen.Source.ATTRIBUTE),
        GRANT_TALENT("grant_talent", true, false, false, "talent", "talent", "custom:target",
                null, "", "", List.of(), "", "only_if_never", "synthesized",
                "", "", ResourceReferencePickerScreen.Source.TALENT),
        REVOKE_TALENT("revoke_talent", false, false, false, "talent", "talent", "custom:target",
                null, "", "", List.of(), "", "retain_effects", "retain_category",
                "", "", ResourceReferencePickerScreen.Source.TALENT),
        GIVE_ITEM("give_item", true, true, false, "item", "item", "minecraft:diamond",
                "count", "count", "1", List.of(), "", "drop_overflow", null,
                "", "", ResourceReferencePickerScreen.Source.ITEM),
        GIVE_DRAW_TOKENS("give_draw_tokens", true, true, false, null, "", "",
                "amount", "amount", "1", List.of(), "", null, null,
                "", "", null),
        MODIFY_COUNTER("modify_counter", true, true, false, "counter", "counter", "custom:counter",
                "expression", "expression", "1", List.of("set", "add", "multiply"), "add",
                null, null, "minimum", "maximum", ResourceReferencePickerScreen.Source.COUNTER),
        TAKE_ITEM("take_item", true, true, false, "item", "item", "minecraft:diamond",
                "count", "count", "1", List.of(), "", null, null,
                "", "", ResourceReferencePickerScreen.Source.ITEM),
        HUD_RESOURCE("hud_resource", false, true, false, "state", "state", "talent_editor:resource",
                "maximum", "maximum_expression", "100", List.of(), "", "show_when_full", null,
                "hud_color", "hud_order", ResourceReferencePickerScreen.Source.NUMERIC_STATE),
        PLAYER_RENDER("player_render", false, false, false, null, "", "",
                null, "", "", List.of(), "", "hide_nameplate", null,
                "render_alpha", "render_scale", null),
        OPEN_ITEM_SUBMISSION("open_item_submission", false, true, false, "item", "item", "minecraft:emerald",
                "count", "count", "1", List.of(), "", "consume_submission", null,
                "submission_timeout", "", ResourceReferencePickerScreen.Source.ITEM);

        private final String serializedName;
        private final boolean lifecycleAllowed;
        private final boolean expression;
        private final boolean selector;
        private final String resourceField;
        private final String resourceLabel;
        private final String defaultResource;
        private final String expressionField;
        private final String expressionLabel;
        private final String defaultExpression;
        private final List<String> operations;
        private final String defaultOperation;
        private final String firstToggleKey;
        private final String secondToggleKey;
        private final String valueALabel;
        private final String valueBLabel;
        private final ResourceReferencePickerScreen.Source resourceSource;

        ActionKind(
                String serializedName, boolean lifecycleAllowed, boolean expression, boolean selector,
                String resourceField, String resourceLabel, String defaultResource,
                String expressionField, String expressionLabel, String defaultExpression,
                List<String> operations, String defaultOperation,
                String firstToggleKey, String secondToggleKey,
                String valueALabel, String valueBLabel,
                ResourceReferencePickerScreen.Source resourceSource
        ) {
            this.serializedName = serializedName;
            this.lifecycleAllowed = lifecycleAllowed;
            this.expression = expression;
            this.selector = selector;
            this.resourceField = resourceField;
            this.resourceLabel = resourceLabel;
            this.defaultResource = defaultResource;
            this.expressionField = expressionField;
            this.expressionLabel = expressionLabel;
            this.defaultExpression = defaultExpression;
            this.operations = operations;
            this.defaultOperation = defaultOperation;
            this.firstToggleKey = firstToggleKey;
            this.secondToggleKey = secondToggleKey;
            this.valueALabel = valueALabel;
            this.valueBLabel = valueBLabel;
            this.resourceSource = resourceSource;
        }

        static ActionKind from(JsonObject object) {
            String raw = string(object, "type", "");
            String prefix = TalentEditorMod.MOD_ID + ":";
            if (raw.startsWith(prefix)) raw = raw.substring(prefix.length());
            for (ActionKind value : values()) if (value.serializedName.equals(raw)) return value;
            return null;
        }

        String serializedName() { return serializedName; }
        boolean lifecycleAllowed() { return lifecycleAllowed; }
        boolean allowedAt(TalentDraft.AdvancedActionLocation location) {
            if (location == TalentDraft.AdvancedActionLocation.ACTIVE_EFFECTS) {
                return this == HUD_RESOURCE || this == PLAYER_RENDER;
            }
            if (location.triggerScoped()) return this != HUD_RESOURCE && this != PLAYER_RENDER;
            if (!lifecycleAllowed) return false;
            return true;
        }
        boolean selector() { return selector; }
        boolean hasResource() { return resourceField != null; }
        boolean hasExpression() { return expression; }
        boolean hasValues() { return !valueALabel.isBlank(); }
        String resourceField() { return resourceField; }
        String resourceLabel() { return resourceLabel; }
        String defaultResource() { return defaultResource; }
        String expressionField() { return expressionField; }
        String expressionLabel() { return expressionLabel; }
        String defaultExpression() { return defaultExpression; }
        List<String> operations() { return operations; }
        String defaultOperation() { return defaultOperation; }
        String firstToggleKey() { return firstToggleKey; }
        String secondToggleKey() { return secondToggleKey; }
        String valueALabel() { return valueALabel; }
        String valueBLabel() { return valueBLabel; }
        ResourceReferencePickerScreen.Source resourceSource() { return resourceSource; }
    }
}
