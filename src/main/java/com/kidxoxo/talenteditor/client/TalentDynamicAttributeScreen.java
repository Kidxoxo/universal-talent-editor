package com.kidxoxo.talenteditor.client;

import com.kidxoxo.talenteditor.network.EditorBootstrap;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import java.util.List;
import java.util.Locale;
import java.util.Optional;

public final class TalentDynamicAttributeScreen extends Screen {
    private static final List<String> SOURCES = List.of("counter", "health_percent", "hunger_percent");
    private static final List<String> OPERATIONS = List.of("addition", "multiply_base", "multiply_total");
    private final TalentEditScreen owner;
    private final Screen parent;
    private final EditorBootstrap bootstrap;
    private final int rawIndex;
    private final boolean createdForThisScreen;
    private ResourceLocation attribute;
    private ResourceLocation counter;
    private int sourceIndex;
    private int operationIndex;
    private boolean showInDescription;
    private String baseValue;
    private String scaleValue;
    private String minimumValue;
    private String maximumValue;
    private String keyValue;
    private EditBox attributeField;
    private EditBox counterField;
    private EditBox baseField;
    private EditBox scaleField;
    private EditBox minimumField;
    private EditBox maximumField;
    private EditBox keyField;
    private Button sourceButton;
    private Button operationButton;
    private Button showButton;
    private Button chooseCounterButton;
    private final EditorOptionPopup options = new EditorOptionPopup();
    private String status = "";
    private boolean applied;

    TalentDynamicAttributeScreen(
            TalentEditScreen owner,
            Screen parent,
            EditorBootstrap bootstrap,
            int rawIndex,
            boolean createdForThisScreen
    ) {
        super(Component.translatable("screen.talent_editor.dynamic.title"));
        this.owner = owner;
        this.parent = parent;
        this.bootstrap = bootstrap;
        this.rawIndex = rawIndex;
        this.createdForThisScreen = createdForThisScreen;
        TalentDraft.DynamicAttributeDraft value = owner.draft().dynamicAttribute(rawIndex);
        attribute = value.attribute();
        counter = value.counter().orElse(null);
        sourceIndex = Math.max(0, SOURCES.indexOf(value.source()));
        operationIndex = Math.max(0, OPERATIONS.indexOf(value.operation()));
        showInDescription = value.showInDescription();
        baseValue = compact(value.base());
        scaleValue = compact(value.scale());
        minimumValue = compact(value.minimum());
        maximumValue = compact(value.maximum());
        keyValue = value.modifierKey();
    }

    @Override
    protected void init() {
        TalentDraft.DynamicAttributeDraft value = owner.draft().dynamicAttribute(rawIndex);
        int formWidth = Math.min(700, width - 32);
        int x = (width - formWidth) / 2;
        int gap = 6;
        int chooseWidth = Math.min(92, Math.max(62, formWidth / 6));

        attributeField = field(x, 43, formWidth - chooseWidth - gap, 256, attribute.toString());
        attributeField.setEditable(false);
        addRenderableWidget(Button.builder(Component.translatable("screen.talent_editor.dynamic.choose_attribute"),
                        button -> chooseAttribute())
                .bounds(x + formWidth - chooseWidth, 43, chooseWidth, 20).build());

        int sourceWidth = Math.min(160, Math.max(100, formWidth / 4));
        sourceButton = addRenderableWidget(Button.builder(sourceLabel(), button -> chooseSource())
                .bounds(x, 75, sourceWidth, 20).build());
        int counterWidth = Math.max(30, formWidth - sourceWidth - chooseWidth - gap * 2);
        counterField = field(x + sourceWidth + gap, 75, counterWidth, 256,
                counter == null ? "" : counter.toString());
        counterField.setEditable(false);
        chooseCounterButton = addRenderableWidget(Button.builder(
                        Component.translatable("screen.talent_editor.dynamic.choose_counter"),
                        button -> chooseCounter())
                .bounds(x + sourceWidth + gap + counterWidth + gap, 75, chooseWidth, 20).build());

        int half = (formWidth - gap) / 2;
        baseField = field(x, 107, half, 48, baseValue);
        scaleField = field(x + half + gap, 107, formWidth - half - gap, 48, scaleValue);
        minimumField = field(x, 139, half, 48, minimumValue);
        maximumField = field(x + half + gap, 139, formWidth - half - gap, 48, maximumValue);

        int optionWidth = Math.max(88, (formWidth - gap * 2) / 3);
        operationButton = addRenderableWidget(Button.builder(operationLabel(), button -> chooseOperation())
                .bounds(x, 171, optionWidth, 20).build());
        keyField = field(x + optionWidth + gap, 171, optionWidth, 160, keyValue);
        showButton = addRenderableWidget(Button.builder(showLabel(), button -> toggleShow())
                .bounds(x + (optionWidth + gap) * 2, 171,
                        formWidth - (optionWidth + gap) * 2, 20).build());

        int bottom = height - 28;
        addRenderableWidget(Button.builder(Component.translatable("gui.cancel"), button -> cancel())
                .bounds(x, bottom, half, 20).build());
        addRenderableWidget(Button.builder(Component.translatable("screen.talent_editor.dynamic.apply"),
                        button -> apply())
                .bounds(x + half + gap, bottom, formWidth - half - gap, 20).build());
        syncSourceWidgets();
    }

    private EditBox field(int x, int y, int width, int maxLength, String value) {
        EditBox field = new EditBox(font, x, y, width, 20, Component.empty());
        field.setMaxLength(maxLength);
        field.setValue(value);
        addRenderableWidget(field);
        return field;
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics);
        int formWidth = Math.min(700, width - 32);
        int x = (width - formWidth) / 2;
        EditorTheme.panel(graphics, x - 7, 27, formWidth + 14, Math.max(1, height - 63));
        super.render(graphics, mouseX, mouseY, partialTick);
        graphics.drawCenteredString(font, title, width / 2, 9, EditorTheme.TEXT);
        label(graphics, x, 32, "attribute");
        label(graphics, x, 65, "source");
        label(graphics, x, 97, "formula");
        label(graphics, x, 129, "bounds");
        label(graphics, x, 161, "options");
        if (height >= 255) {
            graphics.drawString(font, Component.translatable("screen.talent_editor.dynamic.formula_help"),
                    x, 199, EditorTheme.MUTED, false);
        }
        if (!status.isBlank()) {
            graphics.drawString(font, font.plainSubstrByWidth(status, formWidth), x, height - 43,
                    0xFFFF7777, false);
        }
        options.render(graphics, font, width, height);
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

    private void label(GuiGraphics graphics, int x, int y, String suffix) {
        graphics.drawString(font, Component.translatable("screen.talent_editor.dynamic." + suffix),
                x, y, EditorTheme.MUTED, false);
    }

    private void chooseAttribute() {
        if (minecraft == null) return;
        captureForm();
        minecraft.setScreen(new ResourceReferencePickerScreen(
                this, ResourceReferencePickerScreen.Source.ATTRIBUTE, bootstrap, this::setAttribute
        ));
    }

    private void setAttribute(ResourceLocation value) {
        attribute = value;
        attributeField.setValue(value.toString());
    }

    private void chooseCounter() {
        if (minecraft == null || sourceIndex != 0) return;
        captureForm();
        minecraft.setScreen(new ResourceReferencePickerScreen(
                this, ResourceReferencePickerScreen.Source.COUNTER, bootstrap, this::setCounter
        ));
    }

    private void setCounter(ResourceLocation value) {
        counter = value;
        counterField.setValue(value.toString());
    }

    private void chooseSource() {
        options.open(Component.translatable("screen.talent_editor.option.choose"),
                SOURCES.stream().map(value -> Component.translatable(
                        "screen.talent_editor.dynamic.source." + value)).toList(),
                sourceIndex, index -> {
                    sourceIndex = index;
                    sourceButton.setMessage(sourceLabel());
                    syncSourceWidgets();
                }, height);
    }

    private void syncSourceWidgets() {
        boolean usesCounter = sourceIndex == 0;
        counterField.active = usesCounter;
        chooseCounterButton.active = usesCounter;
    }

    private void chooseOperation() {
        options.open(Component.translatable("screen.talent_editor.option.choose"),
                OPERATIONS.stream().map(value -> Component.translatable(
                        "screen.talent_editor.operation." + value)).toList(),
                operationIndex, index -> {
                    operationIndex = index;
                    operationButton.setMessage(operationLabel());
                }, height);
    }

    private void toggleShow() {
        showInDescription = !showInDescription;
        showButton.setMessage(showLabel());
    }

    private void apply() {
        captureForm();
        ResourceLocation parsedAttribute = ResourceLocation.tryParse(attributeField.getValue().strip());
        ResourceLocation parsedCounter = ResourceLocation.tryParse(counterField.getValue().strip());
        double base;
        double scale;
        double minimum;
        double maximum;
        try {
            base = bounded(baseValue);
            scale = bounded(scaleValue);
            minimum = bounded(minimumValue);
            maximum = bounded(maximumValue);
        } catch (NumberFormatException exception) {
            status = Component.translatable("screen.talent_editor.dynamic.invalid_number").getString();
            return;
        }
        String source = SOURCES.get(sourceIndex);
        String key = keyValue.strip();
        if (parsedAttribute == null || source.equals("counter") && parsedCounter == null
                || minimum > maximum || !key.matches("[a-z0-9_.-]{1,160}")) {
            status = Component.translatable("screen.talent_editor.dynamic.invalid_value").getString();
            return;
        }
        if (source.equals("health_percent")
                && parsedAttribute.equals(new ResourceLocation("minecraft", "generic.max_health"))) {
            status = Component.translatable("screen.talent_editor.dynamic.self_reference").getString();
            return;
        }
        owner.draft().updateDynamicAttribute(rawIndex, new TalentDraft.DynamicAttributeDraft(
                parsedAttribute, source,
                source.equals("counter") ? Optional.of(parsedCounter) : Optional.empty(),
                base, scale, minimum, maximum, OPERATIONS.get(operationIndex), key, showInDescription
        ));
        applied = true;
        owner.markComplexUpdated("screen.talent_editor.dynamic.updated");
        if (minecraft != null) minecraft.setScreen(parent);
    }

    private static double bounded(String value) {
        double parsed = Double.parseDouble(value.strip());
        if (!Double.isFinite(parsed) || parsed < -1.0E12D || parsed > 1.0E12D) {
            throw new NumberFormatException();
        }
        return parsed;
    }

    private void captureForm() {
        if (baseField != null) baseValue = baseField.getValue();
        if (scaleField != null) scaleValue = scaleField.getValue();
        if (minimumField != null) minimumValue = minimumField.getValue();
        if (maximumField != null) maximumValue = maximumField.getValue();
        if (keyField != null) keyValue = keyField.getValue();
    }

    private Component sourceLabel() {
        return Component.translatable("screen.talent_editor.dynamic.source." + SOURCES.get(sourceIndex));
    }

    private Component operationLabel() {
        return Component.translatable("screen.talent_editor.operation." + OPERATIONS.get(operationIndex));
    }

    private Component showLabel() {
        return Component.translatable("screen.talent_editor.dynamic.show",
                Component.translatable(showInDescription ? "options.on" : "options.off"));
    }

    private void cancel() {
        if (createdForThisScreen && !applied) owner.draft().removeEditableActiveEffect(rawIndex);
        if (minecraft != null) minecraft.setScreen(parent);
    }

    @Override
    public void onClose() {
        cancel();
    }

    private static String compact(double value) {
        if (value == Math.rint(value)) return Long.toString((long) value);
        return String.format(Locale.ROOT, "%.8f", value).replaceFirst("0+$", "").replaceFirst("\\.$", "");
    }
}
