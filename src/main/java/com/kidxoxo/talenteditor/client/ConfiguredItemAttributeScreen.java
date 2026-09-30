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

/** List of item attributes with an editor for the selected row. */
public final class ConfiguredItemAttributeScreen extends Screen {
    private static final int MAX_MODIFIERS = 256;
    private static final int ROW_HEIGHT = 27;
    private static final List<String> OPERATIONS = List.of("addition", "multiply_base", "multiply_total");
    private final ConfiguredItemEditScreen parent;
    private final EditorBootstrap bootstrap;
    private final ArrayList<ConfiguredItemDraft.AttributeModifierDraft> modifiers;
    private final EditorOptionPopup options = new EditorOptionPopup();
    private final ArrayList<String> selectedSlots = new ArrayList<>();
    private int selected;
    private int scroll;
    private int operationIndex;
    private EditBox attributeField;
    private EditBox amountField;
    private EditBox keyField;
    private Button operationButton;
    private Button slotsButton;
    private Button deleteButton;
    private Button chooseButton;
    private String status = "";

    public ConfiguredItemAttributeScreen(ConfiguredItemEditScreen parent, EditorBootstrap bootstrap,
                                         List<ConfiguredItemDraft.AttributeModifierDraft> modifiers) {
        super(Component.translatable("screen.talent_editor.configured_item.attributes.title"));
        this.parent = parent;
        this.bootstrap = bootstrap;
        this.modifiers = new ArrayList<>(modifiers);
        selected = modifiers.isEmpty() ? -1 : 0;
    }

    private int contentWidth() { return Math.min(900, width - 32); }
    private int leftX() { return (width - contentWidth()) / 2; }
    private int listWidth() { return Math.max(180, contentWidth() * 38 / 100); }
    private int formX() { return leftX() + listWidth() + 14; }
    private int formWidth() { return contentWidth() - listWidth() - 14; }
    private int listBottom() { return height - 68; }
    private int visibleRows() { return Math.max(1, (listBottom() - 52) / ROW_HEIGHT); }

    @Override protected void init() {
        int x = formX(), w = formWidth();
        attributeField = field(x + 8, 53, w - 116, 256);
        chooseButton = addRenderableWidget(Button.builder(Component.translatable(
                "screen.talent_editor.configured_item.attributes.choose"), b -> chooseAttribute())
                .bounds(x + w - 100, 53, 92, 20).build());
        amountField = field(x + 8, 91, Math.max(70, (w - 24) / 2), 32);
        operationButton = addRenderableWidget(Button.builder(Component.empty(), b -> chooseOperation())
                .bounds(x + 16 + (w - 24) / 2, 91, w - 24 - (w - 24) / 2, 20).build());
        slotsButton = addRenderableWidget(Button.builder(Component.empty(), b -> chooseSlots())
                .bounds(x + 8, 129, w - 16, 20).build());
        keyField = field(x + 8, 167, w - 16, 160);
        int lx = leftX(), lw = listWidth();
        addRenderableWidget(Button.builder(Component.translatable(
                "screen.talent_editor.configured_item.attributes.add"), b -> addModifier())
                .bounds(lx + 8, height - 57, (lw - 22) / 2, 20).build());
        deleteButton = addRenderableWidget(Button.builder(Component.translatable(
                "screen.talent_editor.configured_item.attributes.delete"), b -> deleteModifier())
                .bounds(lx + 14 + (lw - 22) / 2, height - 57, (lw - 22) / 2, 20).build());
        int half = (contentWidth() - 6) / 2;
        addRenderableWidget(Button.builder(Component.translatable(
                "screen.talent_editor.configured_item.attributes.apply"), b -> apply())
                .bounds(lx, height - 28, half, 20).build());
        addRenderableWidget(Button.builder(Component.translatable("gui.cancel"), b -> returnParent())
                .bounds(lx + half + 6, height - 28, contentWidth() - half - 6, 20).build());
        loadSelected();
    }

    private EditBox field(int x, int y, int width, int maxLength) {
        EditBox field = new EditBox(font, x, y, width, 20, Component.empty());
        field.setMaxLength(maxLength);
        field.setResponder(ignored -> status = "");
        addRenderableWidget(field);
        return field;
    }

    @Override public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics);
        EditorTheme.panel(graphics, leftX(), 28, listWidth(), height - 62);
        EditorTheme.panel(graphics, formX(), 28, formWidth(), Math.min(height - 62, 225));
        super.render(graphics, mouseX, mouseY, partialTick);
        graphics.drawCenteredString(font, title, width / 2, 10, EditorTheme.TEXT);
        graphics.drawString(font, Component.translatable(
                "screen.talent_editor.configured_item.attributes.list", modifiers.size()),
                leftX() + 8, 38, EditorTheme.TEXT, false);
        drawList(graphics, mouseX, mouseY);
        label(graphics, "attribute", formX() + 8, 42);
        label(graphics, "amount", formX() + 8, 80);
        label(graphics, "operation", formX() + 16 + (formWidth() - 24) / 2, 80);
        label(graphics, "slot", formX() + 8, 118);
        label(graphics, "key", formX() + 8, 156);
        graphics.drawString(font, font.plainSubstrByWidth(Component.translatable(
                "screen.talent_editor.configured_item.attributes.help").getString(), formWidth() - 16),
                formX() + 8, 197, EditorTheme.MUTED, false);
        if (!status.isBlank()) graphics.drawString(font, font.plainSubstrByWidth(status, formWidth() - 16),
                formX() + 8, 217, 0xFFFF7777, false);
        options.render(graphics, font, width, height);
    }

    private void drawList(GuiGraphics graphics, int mouseX, int mouseY) {
        int x = leftX() + 7, w = listWidth() - 14;
        scroll = Math.max(0, Math.min(scroll, Math.max(0, modifiers.size() - visibleRows())));
        graphics.enableScissor(x, 52, x + w, listBottom());
        for (int i = scroll; i < Math.min(modifiers.size(), scroll + visibleRows()); i++) {
            int y = 52 + (i - scroll) * ROW_HEIGHT;
            boolean hover = mouseX >= x && mouseX < x + w && mouseY >= y && mouseY < y + ROW_HEIGHT;
            graphics.fill(x, y, x + w, y + ROW_HEIGHT - 2,
                    i == selected ? 0xFF28547C : hover ? 0xFF25394F : 0xFF172738);
            var modifier = modifiers.get(i);
            String name = (i + 1) + ". " + modifier.attribute();
            String detail = formatDecimal(modifier.amount()) + "  ·  "
                    + Component.translatable("screen.talent_editor.configured_item.attributes.operation."
                    + modifier.operation()).getString() + "  ·  "
                    + modifier.slots().stream().map(slot -> Component.translatable(
                    "screen.talent_editor.configured_item.attributes.slot." + slot).getString())
                    .reduce((a, b) -> a + ", " + b).orElse("");
            graphics.drawString(font, font.plainSubstrByWidth(name, w - 12), x + 6, y + 3,
                    EditorTheme.TEXT, false);
            graphics.drawString(font, font.plainSubstrByWidth(detail, w - 12), x + 6, y + 15,
                    EditorTheme.MUTED, false);
        }
        graphics.disableScissor();
    }

    private void label(GuiGraphics graphics, String suffix, int x, int y) {
        graphics.drawString(font, Component.translatable(
                "screen.talent_editor.configured_item.attributes." + suffix), x, y, EditorTheme.MUTED, false);
    }

    @Override public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (options.mouseClicked(mouseX, mouseY, button, width, height)) return true;
        int x = leftX() + 7;
        if (button == 0 && mouseX >= x && mouseX < x + listWidth() - 14
                && mouseY >= 52 && mouseY < listBottom()) {
            int index = scroll + (int) ((mouseY - 52) / ROW_HEIGHT);
            if (index < modifiers.size() && index != selected && saveSelected()) {
                selected = index;
                loadSelected();
            }
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        if (options.mouseScrolled(delta, height)) return true;
        if (mouseX >= leftX() && mouseX < leftX() + listWidth()) {
            scroll = Math.max(0, Math.min(scroll - (int) Math.signum(delta) * 3,
                    Math.max(0, modifiers.size() - visibleRows())));
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, delta);
    }

    @Override public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (options.keyPressed(keyCode, height)) return true;
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override public boolean charTyped(char codePoint, int modifiers) {
        return options.isOpen() || super.charTyped(codePoint, modifiers);
    }

    private void chooseOperation() {
        if (selected < 0) return;
        options.open(Component.translatable("screen.talent_editor.option.choose"),
                OPERATIONS.stream().map(value -> Component.translatable(
                        "screen.talent_editor.configured_item.attributes.operation." + value)).toList(),
                operationIndex, index -> { operationIndex = index; updateButtons(); }, height);
    }

    private void chooseSlots() {
        if (minecraft == null || selected < 0 || !saveSelected()) return;
        minecraft.setScreen(new ConfiguredItemAttributeSlotsScreen(this, selectedSlots, this::replaceSlots));
    }

    private void replaceSlots(List<String> slots) {
        selectedSlots.clear();
        selectedSlots.addAll(slots);
        saveSelected();
        updateButtons();
    }

    private void chooseAttribute() {
        if (minecraft == null || selected < 0 || !saveSelected()) return;
        minecraft.setScreen(new ResourceReferencePickerScreen(
                this, ResourceReferencePickerScreen.Source.ATTRIBUTE, bootstrap, this::replaceAttribute));
    }

    private void replaceAttribute(ResourceLocation attribute) {
        if (selected < 0) return;
        var current = modifiers.get(selected);
        modifiers.set(selected, current.withValues(attribute, current.amount(), current.operation(),
                current.slots(), current.modifierKey()));
    }

    private void addModifier() {
        if (!saveSelected()) return;
        if (modifiers.size() >= MAX_MODIFIERS) {
            invalid("screen.talent_editor.configured_item.attributes.limit");
            return;
        }
        modifiers.add(ConfiguredItemDraft.AttributeModifierDraft.create(uniqueKey()));
        selected = modifiers.size() - 1;
        scroll = Math.max(0, selected - visibleRows() + 1);
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
        if (attribute == null || !rawAttribute.contains(":")) {
            return invalid("screen.talent_editor.configured_item.attributes.invalid_attribute");
        }
        double amount;
        try {
            amount = Double.parseDouble(amountField.getValue().strip());
        } catch (NumberFormatException ignored) {
            return invalid("screen.talent_editor.configured_item.attributes.invalid_amount");
        }
        if (!Double.isFinite(amount) || amount < -1.0E12D || amount > 1.0E12D) {
            return invalid("screen.talent_editor.configured_item.attributes.invalid_amount");
        }
        if (selectedSlots.isEmpty()) return invalid("screen.talent_editor.configured_item.attributes.invalid_slots");
        String key = keyField.getValue().strip();
        if (!key.matches("[a-z0-9_.-]{1,160}")) {
            return invalid("screen.talent_editor.configured_item.attributes.invalid_key");
        }
        for (int i = 0; i < modifiers.size(); i++) {
            if (i != selected && modifiers.get(i).modifierKey().equals(key)) {
                return invalid("screen.talent_editor.configured_item.attributes.duplicate_key");
            }
        }
        modifiers.set(selected, modifiers.get(selected).withValues(attribute, amount,
                OPERATIONS.get(operationIndex), List.copyOf(selectedSlots), key));
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
        slotsButton.active = active;
        chooseButton.active = active;
        deleteButton.active = active;
        selectedSlots.clear();
        if (!active) {
            attributeField.setValue("");
            amountField.setValue("");
            keyField.setValue("");
            operationIndex = 0;
        } else {
            var modifier = modifiers.get(selected);
            attributeField.setValue(modifier.attribute());
            amountField.setValue(formatDecimal(modifier.amount()));
            keyField.setValue(modifier.modifierKey());
            operationIndex = Math.max(0, OPERATIONS.indexOf(modifier.operation()));
            selectedSlots.addAll(modifier.slots());
        }
        updateButtons();
        status = "";
    }

    private void updateButtons() {
        operationButton.setMessage(Component.translatable(
                "screen.talent_editor.configured_item.attributes.operation." + OPERATIONS.get(operationIndex)));
        slotsButton.setMessage(Component.translatable(
                "screen.talent_editor.configured_item.attributes.slots_button",
                selectedSlots.stream().map(slot -> Component.translatable(
                        "screen.talent_editor.configured_item.attributes.slot." + slot).getString())
                        .reduce((a, b) -> a + " / " + b).orElse("0")));
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

    @Override public void onClose() { returnParent(); }
}
