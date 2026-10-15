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

/** Manages ordinary attributes, maintained effects, and effect immunities in one list. */
public final class TalentEffectEditorScreen extends EditorScreen {
    private static final List<String> OPERATIONS = List.of("addition", "multiply_base", "multiply_total");
    private static final int ROW_HEIGHT = 20;

    private final TalentEditScreen owner;
    private final EditorBootstrap bootstrap;
    private int selectedRawIndex;
    private int operationIndex;
    private int scroll;
    private EditBox amountField;
    private Button operationButton;
    private Button applyButton;
    private Button removeButton;
    private Button gateButton;
    private final EditorOptionPopup options = new EditorOptionPopup();
    private String status = "";
    private int statusColor = EditorTheme.MUTED;

    TalentEffectEditorScreen(TalentEditScreen owner, EditorBootstrap bootstrap, int selectedRawIndex) {
        super(Component.translatable("screen.talent_editor.effect_editor.manage_title"));
        this.owner = owner;
        this.bootstrap = bootstrap;
        this.selectedRawIndex = selectedRawIndex;
    }

    @Override
    protected void init() {
        int formWidth = Math.min(700, width - 32);
        int x = (width - formWidth) / 2;
        int gap = 6;
        int third = (formWidth - gap * 2) / 3;
        addRenderableWidget(EditorWidgets.button(Component.translatable("screen.talent_editor.add_attribute"),
                        button -> chooseAttribute())
                .bounds(x, 43, third, 20).build());
        addRenderableWidget(EditorWidgets.button(Component.translatable("screen.talent_editor.add_maintained_effect"),
                        button -> chooseMaintainedEffect())
                .bounds(x + third + gap, 43, third, 20).build());
        addRenderableWidget(EditorWidgets.button(Component.translatable("screen.talent_editor.add_immunity"),
                        button -> chooseImmunity())
                .bounds(x + (third + gap) * 2, 43, formWidth - (third + gap) * 2, 20).build());

        int half = (formWidth - gap) / 2;
        int detailY = height - 82;
        amountField = new EditBox(font, x, detailY, half, 20,
                Component.translatable("screen.talent_editor.form.amount"));
        amountField.setMaxLength(48);
        addRenderableWidget(amountField);
        operationButton = addRenderableWidget(EditorWidgets.button(operationLabel(), button -> chooseOperation())
                .bounds(x + half + gap, detailY, formWidth - half - gap, 20).build());

        int bottom = height - 28;
        addRenderableWidget(EditorWidgets.button(Component.translatable("gui.back"), button -> returnOwner())
                .bounds(x, bottom, third, 20).build());
        applyButton = addRenderableWidget(EditorWidgets.button(
                        Component.translatable("screen.talent_editor.effect_editor.apply"), button -> applySelected())
                .bounds(x + third + gap, bottom, third, 20).build());
        removeButton = addRenderableWidget(EditorWidgets.button(
                        Component.translatable("screen.talent_editor.remove_effect"), button -> removeSelected())
                .bounds(x + (third + gap) * 2, bottom, formWidth - (third + gap) * 2, 20).build());
        gateButton = addRenderableWidget(EditorWidgets.button(Component.translatable(
                        "screen.talent_editor.action_gate.open"), button -> editGate())
                .bounds(x + formWidth - 100, 8, 100, 20).build());
        syncSelection();
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics);
        int formWidth = Math.min(700, width - 32);
        int x = (width - formWidth) / 2;
        int listBottom = height - 91;
        EditorTheme.panel(graphics, x - 7, 27, formWidth + 14, 43);
        EditorTheme.panel(graphics, x - 7, 75, formWidth + 14, Math.max(1, listBottom - 75));
        EditorTheme.panel(graphics, x - 7, height - 89, formWidth + 14, 54);
        super.render(graphics, mouseX, mouseY, partialTick);
        EditorTheme.title(graphics,font,title,width);
        graphics.drawCenteredString(font, Component.translatable("screen.talent_editor.effect_editor.manage_help"),
                width / 2, 29, EditorTheme.MUTED);
        renderRows(graphics, mouseX, mouseY, x, formWidth);
        if (!status.isBlank()) {
            graphics.drawString(font, font.plainSubstrByWidth(status, formWidth), x, height - 55,
                    statusColor, false);
        }
        options.render(graphics, font, width, height);
    }

    private void renderRows(GuiGraphics graphics, int mouseX, int mouseY, int x, int formWidth) {
        List<TalentDraft.EffectRow> rows = rows();
        int top = 84;
        int bottom = height - 96;
        int visible = Math.max(1, (bottom - top) / ROW_HEIGHT);
        scroll = Math.max(0, Math.min(scroll, Math.max(0, rows.size() - visible)));
        for (int index = scroll; index < Math.min(rows.size(), scroll + visible); index++) {
            TalentDraft.EffectRow row = rows.get(index);
            int y = top + (index - scroll) * ROW_HEIGHT;
            boolean selected = row.rawIndex() == selectedRawIndex;
            boolean hovered = mouseX >= x && mouseX < x + formWidth && mouseY >= y - 3 && mouseY < y + 15;
            if (selected || hovered) graphics.fill(x, y - 3, x + formWidth, y + 15,
                    selected ? 0x7050A0E8 : 0x404A90E2);
            var action = owner.draft().advancedAction(TalentDraft.AdvancedActionLocation.ACTIVE_EFFECTS,
                    -1, row.rawIndex());
            String gated = action.has("enabledWhen") || action.has("disabledWhen")
                    ? " " + Component.translatable("screen.talent_editor.action_gate.marked").getString() : "";
            graphics.drawString(font, font.plainSubstrByWidth(owner.effectLabel(row) + gated, formWidth - 8),
                    x + 4, y, EditorTheme.TEXT, false);
        }
        if (rows.isEmpty()) graphics.drawCenteredString(font,
                Component.translatable("screen.talent_editor.effect_editor.empty"),
                width / 2, top + 10, EditorTheme.MUTED);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (options.mouseClicked(mouseX, mouseY, button, width, height)) return true;
        if (button == 0) {
            int formWidth = Math.min(700, width - 32);
            int x = (width - formWidth) / 2;
            int top = 84;
            int bottom = height - 96;
            if (mouseX >= x && mouseX < x + formWidth && mouseY >= top && mouseY < bottom) {
                int index = scroll + (int) ((mouseY - top) / ROW_HEIGHT);
                if(index>=scroll+Math.max(1,(height-84-96)/ROW_HEIGHT))return true;
                if (index >= 0 && index < rows().size()) {
                    selectedRawIndex = rows().get(index).rawIndex();
                    status = "";
                    syncSelection();
                    return true;
                }
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        if (options.mouseScrolled(delta, height)) return true;
        int visible = Math.max(1, (height - 96 - 84) / ROW_HEIGHT);
        scroll = Math.max(0, Math.min(scroll - (int) Math.signum(delta) * 3,
                Math.max(0, rows().size() - visible)));
        return true;
    }

    private List<TalentDraft.EffectRow> rows() {
        return owner.draft().basicEffectRows();
    }

    private void chooseAttribute() {
        if (minecraft != null) minecraft.setScreen(new ResourceReferencePickerScreen(
                this, ResourceReferencePickerScreen.Source.ATTRIBUTE, bootstrap, this::addAttribute
        ));
    }

    private void addAttribute(ResourceLocation attribute) {
        selectedRawIndex = owner.addAttributeEffect(attribute);
        updated("screen.talent_editor.attribute_added", attribute);
    }

    private void chooseMaintainedEffect() {
        if (minecraft != null) minecraft.setScreen(new ResourceReferencePickerScreen(
                this, ResourceReferencePickerScreen.Source.MOB_EFFECT, bootstrap, this::addMaintainedEffect
        ));
    }

    private void addMaintainedEffect(ResourceLocation effect) {
        selectedRawIndex = owner.addMaintainedEffect(effect);
        updated("screen.talent_editor.maintained_effect_added", effect);
    }

    private void chooseImmunity() {
        if (minecraft != null) minecraft.setScreen(new ResourceReferencePickerScreen(
                this, ResourceReferencePickerScreen.Source.MOB_EFFECT, bootstrap, this::addImmunity
        ));
    }

    private void addImmunity(ResourceLocation effect) {
        selectedRawIndex = owner.addEffectImmunity(effect);
        updated("screen.talent_editor.immunity_added", effect);
    }

    private void updated(String key, Object argument) {
        status = Component.translatable(key, argument).getString();
        statusColor = 0xFF55FF55;
        syncSelection();
    }

    private void applySelected() {
        TalentDraft.EffectRow selected = selectedRow();
        if (selected == null || selected.editableImmunity()) return;
        double amount;
        try {
            amount = Double.parseDouble(amountField.getValue().strip());
            if (!Double.isFinite(amount) || amount < -1.0E12D || amount > 1.0E12D) {
                throw new NumberFormatException();
            }
        } catch (NumberFormatException ignored) {
            invalid("screen.talent_editor.form.invalid_amount");
            return;
        }
        if (selected.editableMaintainedEffect()) {
            if (amount != Math.rint(amount) || amount < 0.0D || amount > 255.0D) {
                invalid("screen.talent_editor.invalid_effect_level");
                return;
            }
            owner.draft().updateMaintainedEffectAmplifier(selected.rawIndex(), (int) amount);
        } else if (selected.editableAttribute()) {
            owner.draft().updateAttribute(selected.rawIndex(), amount, OPERATIONS.get(operationIndex));
        } else {
            return;
        }
        owner.markEffectUpdated(selected.rawIndex(), "screen.talent_editor.effect_editor.applied");
        status = Component.translatable("screen.talent_editor.effect_editor.applied").getString();
        statusColor = 0xFF55FF55;
        syncSelection();
    }

    private void removeSelected() {
        TalentDraft.EffectRow selected = selectedRow();
        if (selected == null) return;
        owner.draft().removeEditableActiveEffect(selected.rawIndex());
        selectedRawIndex = -1;
        owner.markComplexUpdated("screen.talent_editor.effect_removed");
        status = Component.translatable("screen.talent_editor.effect_removed").getString();
        statusColor = 0xFF55FF55;
        syncSelection();
    }

    private void editGate() {
        TalentDraft.EffectRow row = selectedRow();
        if (row != null && minecraft != null) minecraft.setScreen(new TalentActionGateScreen(
                this, owner, TalentDraft.AdvancedActionLocation.ACTIVE_EFFECTS, -1, row.rawIndex()));
    }

    private void invalid(String key) {
        status = Component.translatable(key).getString();
        statusColor = 0xFFFF7777;
    }

    private void syncSelection() {
        TalentDraft.EffectRow selected = selectedRow();
        boolean amountEditable = selected != null
                && (selected.editableAttribute() || selected.editableMaintainedEffect());
        if (amountField == null) return;
        amountField.visible = amountEditable;
        amountField.active = amountEditable;
        operationButton.visible = selected != null && selected.editableAttribute();
        operationButton.active = operationButton.visible;
        applyButton.active = amountEditable;
        removeButton.active = selected != null;
        gateButton.active = selected != null;
        if (!amountEditable) {
            amountField.setValue("");
            return;
        }
        amountField.setValue(compact(selected.amount()));
        operationIndex = selected.editableAttribute()
                ? Math.max(0, OPERATIONS.indexOf(selected.operation())) : 0;
        operationButton.setMessage(operationLabel());
    }

    private TalentDraft.EffectRow selectedRow() {
        return rows().stream().filter(row -> row.rawIndex() == selectedRawIndex).findFirst().orElse(null);
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

    @Override public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (options.keyPressed(keyCode, height)) return true;
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override public boolean charTyped(char codePoint, int modifiers) {
        return options.isOpen() || super.charTyped(codePoint, modifiers);
    }

    private Component operationLabel() {
        return Component.translatable("screen.talent_editor.operation." + OPERATIONS.get(operationIndex));
    }

    private void returnOwner() {
        if (minecraft != null) minecraft.setScreen(owner);
    }

    @Override
    public void onClose() {
        returnOwner();
    }

    private static String compact(double value) {
        if (value == Math.rint(value)) return Long.toString((long) value);
        return String.format(Locale.ROOT, "%.8f", value).replaceFirst("0+$", "").replaceFirst("\\.$", "");
    }
}
