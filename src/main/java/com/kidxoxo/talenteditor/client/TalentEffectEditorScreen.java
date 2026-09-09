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

public final class TalentEffectEditorScreen extends Screen {
    private static final List<String> OPERATIONS = List.of("addition", "multiply_base", "multiply_total");

    private final TalentEditScreen owner;
    private final EditorBootstrap bootstrap;
    private int selectedRawIndex;
    private int operationIndex;
    private EditBox amountField;
    private Button operationButton;
    private Button applyButton;
    private String status = "";
    private int statusColor = EditorTheme.MUTED;

    TalentEffectEditorScreen(TalentEditScreen owner, EditorBootstrap bootstrap, int selectedRawIndex) {
        super(Component.translatable(selectedRawIndex < 0
                ? "screen.talent_editor.effect_editor.add_title"
                : "screen.talent_editor.effect_editor.edit_title"));
        this.owner = owner;
        this.bootstrap = bootstrap;
        this.selectedRawIndex = selectedRawIndex;
    }

    @Override
    protected void init() {
        int formWidth = Math.min(700, width - 32);
        int x = (width - formWidth) / 2;
        int gap = 6;
        int quarter = (formWidth - gap * 3) / 4;
        addRenderableWidget(Button.builder(Component.translatable("screen.talent_editor.add_attribute"),
                        button -> chooseAttribute())
                .bounds(x, 43, quarter, 20).build());
        addRenderableWidget(Button.builder(Component.translatable("screen.talent_editor.add_maintained_effect"),
                        button -> chooseMaintainedEffect())
                .bounds(x + quarter + gap, 43, quarter, 20).build());
        addRenderableWidget(Button.builder(Component.translatable("screen.talent_editor.add_immunity"),
                        button -> chooseImmunity())
                .bounds(x + (quarter + gap) * 2, 43, quarter, 20).build());
        addRenderableWidget(Button.builder(Component.translatable("screen.talent_editor.add_complex_effect"),
                        button -> openComplexEffects())
                .bounds(x + (quarter + gap) * 3, 43,
                        formWidth - (quarter + gap) * 3, 20).build());

        int half = (formWidth - gap) / 2;
        amountField = new EditBox(font, x, 126, half, 20,
                Component.translatable("screen.talent_editor.form.amount"));
        amountField.setMaxLength(48);
        addRenderableWidget(amountField);
        operationButton = addRenderableWidget(Button.builder(operationLabel(), button -> cycleOperation())
                .bounds(x + half + gap, 126, formWidth - half - gap, 20).build());

        int bottom = height - 28;
        addRenderableWidget(Button.builder(Component.translatable("gui.back"), button -> returnOwner())
                .bounds(x, bottom, half, 20).build());
        applyButton = addRenderableWidget(Button.builder(
                        Component.translatable("screen.talent_editor.effect_editor.apply"), button -> applySelected())
                .bounds(x + half + gap, bottom, formWidth - half - gap, 20).build());
        syncSelection();
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics);
        int formWidth = Math.min(700, width - 32);
        int x = (width - formWidth) / 2;
        EditorTheme.panel(graphics, x - 7, 27, formWidth + 14, 43);
        EditorTheme.panel(graphics, x - 7, 76, formWidth + 14, Math.max(1, height - 112));
        super.render(graphics, mouseX, mouseY, partialTick);
        graphics.drawCenteredString(font, title, width / 2, 9, EditorTheme.TEXT);
        graphics.drawCenteredString(font, Component.translatable("screen.talent_editor.effect_editor.help"),
                width / 2, 29, EditorTheme.MUTED);
        TalentDraft.EffectRow selected = selectedRow();
        if (selected == null) {
            graphics.drawCenteredString(font,
                    Component.translatable("screen.talent_editor.effect_editor.choose_type"),
                    width / 2, 91, EditorTheme.MUTED);
        } else {
            graphics.drawString(font, font.plainSubstrByWidth(owner.effectLabel(selected), formWidth - 8),
                    x + 4, 88, EditorTheme.TEXT, false);
            graphics.drawString(font, font.plainSubstrByWidth(
                    detailHelp(selected).getString(), formWidth - 8), x + 4, 106, EditorTheme.MUTED, false);
        }
        if (!status.isBlank()) {
            graphics.drawString(font, font.plainSubstrByWidth(status, formWidth), x, height - 43,
                    statusColor, false);
        }
    }

    private Component detailHelp(TalentDraft.EffectRow row) {
        String suffix = row.editableAttribute() ? "attribute"
                : row.editableMaintainedEffect() ? "maintained"
                : row.editableDrawRefreshBonus() ? "refresh"
                : row.editableDynamicAttribute() ? "dynamic"
                : row.editableImmunity() ? "immunity"
                : row.editableCreativeFlight() ? "flight" : "unsupported";
        return Component.translatable("screen.talent_editor.effect_editor.help." + suffix);
    }

    private void chooseAttribute() {
        if (minecraft == null) return;
        minecraft.setScreen(new ResourceReferencePickerScreen(
                this, ResourceReferencePickerScreen.Source.ATTRIBUTE, bootstrap, this::addAttribute
        ));
    }

    private void addAttribute(ResourceLocation attribute) {
        selectedRawIndex = owner.addAttributeEffect(attribute);
        status = Component.translatable("screen.talent_editor.attribute_added", attribute).getString();
        statusColor = 0xFF55FF55;
    }

    private void chooseMaintainedEffect() {
        if (minecraft == null) return;
        minecraft.setScreen(new ResourceReferencePickerScreen(
                this, ResourceReferencePickerScreen.Source.MOB_EFFECT, bootstrap, this::addMaintainedEffect
        ));
    }

    private void addMaintainedEffect(ResourceLocation effect) {
        selectedRawIndex = owner.addMaintainedEffect(effect);
        status = Component.translatable("screen.talent_editor.maintained_effect_added", effect).getString();
        statusColor = 0xFF55FF55;
    }

    private void chooseImmunity() {
        if (minecraft == null) return;
        minecraft.setScreen(new ResourceReferencePickerScreen(
                this, ResourceReferencePickerScreen.Source.MOB_EFFECT, bootstrap, this::addImmunity
        ));
    }

    private void addImmunity(ResourceLocation effect) {
        selectedRawIndex = owner.addEffectImmunity(effect);
        status = Component.translatable("screen.talent_editor.immunity_added", effect).getString();
        statusColor = 0xFF55FF55;
    }

    private void openComplexEffects() {
        if (minecraft == null) return;
        selectedRawIndex = -1;
        minecraft.setScreen(new TalentComplexEffectsScreen(owner, this, bootstrap));
    }

    private void applySelected() {
        TalentDraft.EffectRow selected = selectedRow();
        if (selected == null) return;
        if (selected.editableDynamicAttribute()) {
            if (minecraft != null) minecraft.setScreen(new TalentDynamicAttributeScreen(
                    owner, this, bootstrap, selected.rawIndex(), false
            ));
            return;
        }
        double amount;
        try {
            amount = Double.parseDouble(amountField.getValue().strip());
            if (!Double.isFinite(amount) || amount < -1.0E12D || amount > 1.0E12D) {
                throw new NumberFormatException();
            }
        } catch (NumberFormatException ignored) {
            status = Component.translatable("screen.talent_editor.form.invalid_amount").getString();
            statusColor = 0xFFFF7777;
            return;
        }
        if (selected.editableMaintainedEffect()) {
            if (amount != Math.rint(amount) || amount < 0.0D || amount > 255.0D) {
                status = Component.translatable("screen.talent_editor.invalid_effect_level").getString();
                statusColor = 0xFFFF7777;
                return;
            }
            owner.draft().updateMaintainedEffectAmplifier(selected.rawIndex(), (int) amount);
        } else if (selected.editableDrawRefreshBonus()) {
            if (amount != Math.rint(amount) || amount < 1.0D || amount > 100.0D) {
                status = Component.translatable("screen.talent_editor.invalid_refresh_bonus").getString();
                statusColor = 0xFFFF7777;
                return;
            }
            owner.draft().updateDrawRefreshBonus(selected.rawIndex(), (int) amount);
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

    private void syncSelection() {
        TalentDraft.EffectRow selected = selectedRow();
        boolean amountEditable = selected != null && (selected.editableAttribute()
                || selected.editableMaintainedEffect() || selected.editableDrawRefreshBonus());
        amountField.visible = amountEditable;
        amountField.active = amountEditable;
        operationButton.visible = selected != null && selected.editableAttribute();
        operationButton.active = operationButton.visible;
        applyButton.active = amountEditable || (selected != null && selected.editableDynamicAttribute());
        applyButton.setMessage(Component.translatable(selected != null && selected.editableDynamicAttribute()
                ? "screen.talent_editor.complex.edit_dynamic" : "screen.talent_editor.effect_editor.apply"));
        if (!amountEditable) return;
        amountField.setValue(compact(selected.amount()));
        operationIndex = selected.editableAttribute()
                ? Math.max(0, OPERATIONS.indexOf(selected.operation())) : 0;
        operationButton.setMessage(operationLabel());
    }

    private TalentDraft.EffectRow selectedRow() {
        return owner.draft().effectRows().stream()
                .filter(row -> row.rawIndex() == selectedRawIndex)
                .findFirst().orElse(null);
    }

    private void cycleOperation() {
        operationIndex = (operationIndex + 1) % OPERATIONS.size();
        operationButton.setMessage(operationLabel());
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
