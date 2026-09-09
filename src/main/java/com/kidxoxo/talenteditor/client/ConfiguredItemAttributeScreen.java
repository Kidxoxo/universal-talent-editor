package com.kidxoxo.talenteditor.client;

import com.kidxoxo.talenteditor.network.EditorBootstrap;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

public final class ConfiguredItemAttributeScreen extends Screen {
    private static final int MAX_MODIFIERS = 256;
    private static final List<String> OPERATIONS = List.of("addition", "multiply_base", "multiply_total");
    private static final List<String> SLOTS = List.of("mainhand", "offhand", "head", "chest", "legs", "feet");

    private final ConfiguredItemEditScreen parent;
    private final EditorBootstrap bootstrap;
    private final ArrayList<ConfiguredItemDraft.AttributeModifierDraft> modifiers;
    private int selected;
    private int operationIndex;
    private int slotIndex;
    private EditBox attributeField;
    private EditBox amountField;
    private EditBox keyField;
    private Button operationButton;
    private Button slotButton;
    private Button previousButton;
    private Button nextButton;
    private Button deleteButton;
    private Button chooseButton;
    private String status = "";

    public ConfiguredItemAttributeScreen(
            ConfiguredItemEditScreen parent,
            EditorBootstrap bootstrap,
            List<ConfiguredItemDraft.AttributeModifierDraft> modifiers
    ) {
        super(Component.translatable("screen.talent_editor.configured_item.attributes.title"));
        this.parent = parent;
        this.bootstrap = bootstrap;
        this.modifiers = new ArrayList<>(modifiers);
        this.selected = modifiers.isEmpty() ? -1 : 0;
    }

    @Override
    protected void init() {
        int formWidth = Math.min(620, width - 32);
        int x = (width - formWidth) / 2;
        int gap = 6;
        attributeField = field(x, 43, formWidth - 92, 256);
        chooseButton = addRenderableWidget(Button.builder(Component.translatable(
                        "screen.talent_editor.configured_item.attributes.choose"), button -> chooseAttribute())
                .bounds(x + formWidth - 86, 43, 86, 20).build());
        int third = (formWidth - gap * 2) / 3;
        amountField = field(x, 84, third, 32);
        operationButton = addRenderableWidget(Button.builder(Component.empty(), button -> cycleOperation())
                .bounds(x + third + gap, 84, third, 20).build());
        slotButton = addRenderableWidget(Button.builder(Component.empty(), button -> cycleSlot())
                .bounds(x + (third + gap) * 2, 84, formWidth - (third + gap) * 2, 20).build());
        keyField = field(x, 125, formWidth, 160);

        int actionWidth = (formWidth - gap * 3) / 4;
        previousButton = addRenderableWidget(Button.builder(Component.translatable(
                        "screen.talent_editor.configured_item.attributes.previous"), button -> switchModifier(-1))
                .bounds(x, 157, actionWidth, 20).build());
        nextButton = addRenderableWidget(Button.builder(Component.translatable(
                        "screen.talent_editor.configured_item.attributes.next"), button -> switchModifier(1))
                .bounds(x + actionWidth + gap, 157, actionWidth, 20).build());
        addRenderableWidget(Button.builder(Component.translatable(
                        "screen.talent_editor.configured_item.attributes.add"), button -> addModifier())
                .bounds(x + (actionWidth + gap) * 2, 157, actionWidth, 20).build());
        deleteButton = addRenderableWidget(Button.builder(Component.translatable(
                        "screen.talent_editor.configured_item.attributes.delete"), button -> deleteModifier())
                .bounds(x + (actionWidth + gap) * 3, 157,
                        formWidth - (actionWidth + gap) * 3, 20).build());

        int half = (formWidth - gap) / 2;
        addRenderableWidget(Button.builder(Component.translatable(
                        "screen.talent_editor.configured_item.attributes.apply"), button -> apply())
                .bounds(x, height - 28, half, 20).build());
        addRenderableWidget(Button.builder(Component.translatable("gui.cancel"), button -> returnParent())
                .bounds(x + half + gap, height - 28, formWidth - half - gap, 20).build());
        loadSelected();
    }

    private EditBox field(int x, int y, int width, int maxLength) {
        EditBox field = new EditBox(font, x, y, width, 20, Component.empty());
        field.setMaxLength(maxLength);
        field.setResponder(ignored -> status = "");
        addRenderableWidget(field);
        return field;
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics);
        super.render(graphics, mouseX, mouseY, partialTick);
        int formWidth = Math.min(620, width - 32);
        int x = (width - formWidth) / 2;
        int gap = 6;
        int third = (formWidth - gap * 2) / 3;
        graphics.drawCenteredString(font, title, width / 2, 10, 0xFFFFFF);
        graphics.drawString(font, Component.translatable(
                "screen.talent_editor.configured_item.attributes.current",
                selected < 0 ? 0 : selected + 1, modifiers.size()
        ), x, 25, 0xA0A0A0, false);
        label(graphics, "attribute", x, 32);
        label(graphics, "amount", x, 73);
        label(graphics, "operation", x + third + gap, 73);
        label(graphics, "slot", x + (third + gap) * 2, 73);
        label(graphics, "key", x, 114);
        graphics.drawString(font, font.plainSubstrByWidth(Component.translatable(
                "screen.talent_editor.configured_item.attributes.help").getString(), formWidth),
                x, 188, 0x808080, false);
        if (!status.isBlank()) {
            graphics.drawString(font, font.plainSubstrByWidth(status, formWidth), x, 207, 0xFF7777, false);
        }
    }

    private void label(GuiGraphics graphics, String suffix, int x, int y) {
        graphics.drawString(font, Component.translatable(
                "screen.talent_editor.configured_item.attributes." + suffix), x, y, 0xA0A0A0, false);
    }

    private void cycleOperation() {
        if (selected < 0) return;
        operationIndex = (operationIndex + 1) % OPERATIONS.size();
        updateButtons();
    }

    private void cycleSlot() {
        if (selected < 0) return;
        slotIndex = (slotIndex + 1) % SLOTS.size();
        updateButtons();
    }

    private void chooseAttribute() {
        if (minecraft == null || selected < 0 || !saveSelected()) return;
        minecraft.setScreen(new ResourceReferencePickerScreen(
                this, ResourceReferencePickerScreen.Source.ATTRIBUTE, bootstrap, this::replaceAttribute
        ));
    }

    private void replaceAttribute(ResourceLocation attribute) {
        attributeField.setValue(attribute.toString());
    }

    private void switchModifier(int direction) {
        if (!saveSelected()) return;
        selected = Math.max(0, Math.min(selected + direction, modifiers.size() - 1));
        loadSelected();
    }

    private void addModifier() {
        if (!saveSelected()) return;
        if (modifiers.size() >= MAX_MODIFIERS) {
            status = Component.translatable("screen.talent_editor.configured_item.attributes.limit").getString();
            return;
        }
        String key = uniqueKey();
        modifiers.add(ConfiguredItemDraft.AttributeModifierDraft.create(key));
        selected = modifiers.size() - 1;
        loadSelected();
    }

    private String uniqueKey() {
        HashSet<String> keys = new HashSet<>();
        modifiers.forEach(modifier -> keys.add(modifier.modifierKey()));
        int suffix = modifiers.size() + 1;
        while (keys.contains("modifier_" + suffix)) suffix++;
        return "modifier_" + suffix;
    }

    private void deleteModifier() {
        if (selected < 0) return;
        modifiers.remove(selected);
        selected = modifiers.isEmpty() ? -1 : Math.min(selected, modifiers.size() - 1);
        loadSelected();
    }

    private boolean saveSelected() {
        if (selected < 0) return true;
        String rawAttribute = attributeField.getValue().strip();
        ResourceLocation attribute = ResourceLocation.tryParse(rawAttribute);
        double amount;
        try {
            amount = Double.parseDouble(amountField.getValue().strip());
        } catch (NumberFormatException ignored) {
            return invalid("screen.talent_editor.configured_item.attributes.invalid_amount");
        }
        String key = keyField.getValue().strip();
        if (attribute == null || !rawAttribute.contains(":")) {
            return invalid("screen.talent_editor.configured_item.attributes.invalid_attribute");
        }
        if (!Double.isFinite(amount) || amount < -1.0E12D || amount > 1.0E12D) {
            return invalid("screen.talent_editor.configured_item.attributes.invalid_amount");
        }
        if (!key.matches("[a-z0-9_.-]{1,160}")) {
            return invalid("screen.talent_editor.configured_item.attributes.invalid_key");
        }
        for (int index = 0; index < modifiers.size(); index++) {
            if (index != selected && modifiers.get(index).modifierKey().equals(key)) {
                return invalid("screen.talent_editor.configured_item.attributes.duplicate_key");
            }
        }
        modifiers.set(selected, modifiers.get(selected).withValues(
                attribute, amount, OPERATIONS.get(operationIndex), SLOTS.get(slotIndex), key
        ));
        return true;
    }

    private boolean invalid(String key) {
        status = Component.translatable(key).getString();
        return false;
    }

    private void loadSelected() {
        boolean active = selected >= 0;
        attributeField.setEditable(active);
        amountField.setEditable(active);
        keyField.setEditable(active);
        operationButton.active = active;
        slotButton.active = active;
        chooseButton.active = active;
        deleteButton.active = active;
        previousButton.active = active && selected > 0;
        nextButton.active = active && selected + 1 < modifiers.size();
        if (!active) {
            attributeField.setValue("");
            amountField.setValue("");
            keyField.setValue("");
            operationIndex = 0;
            slotIndex = 0;
            updateButtons();
            return;
        }
        ConfiguredItemDraft.AttributeModifierDraft modifier = modifiers.get(selected);
        attributeField.setValue(modifier.attribute());
        amountField.setValue(formatDecimal(modifier.amount()));
        keyField.setValue(modifier.modifierKey());
        operationIndex = Math.max(0, OPERATIONS.indexOf(modifier.operation()));
        slotIndex = Math.max(0, SLOTS.indexOf(modifier.slot()));
        updateButtons();
        status = "";
    }

    private void updateButtons() {
        operationButton.setMessage(Component.translatable(
                "screen.talent_editor.configured_item.attributes.operation." + OPERATIONS.get(operationIndex)
        ));
        slotButton.setMessage(Component.translatable(
                "screen.talent_editor.configured_item.attributes.slot." + SLOTS.get(slotIndex)
        ));
    }

    private void apply() {
        if (!saveSelected()) return;
        parent.updateAttributeModifiers(List.copyOf(modifiers));
        returnParent();
    }

    private static String formatDecimal(double value) {
        if (!Double.isFinite(value)) return Double.toString(value);
        return BigDecimal.valueOf(value).stripTrailingZeros().toPlainString();
    }

    private void returnParent() {
        if (minecraft != null) minecraft.setScreen(parent);
    }

    @Override
    public void onClose() {
        returnParent();
    }
}
