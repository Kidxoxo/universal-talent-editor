package com.kidxoxo.talenteditor.client;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.List;

public final class TalentRequirementsScreen extends Screen {
    private static final int ROW_HEIGHT = 18;
    private static final int MAX_ROWS = 256;
    private static final List<String> OPERATORS = List.of("lt", "lte", "eq", "neq", "gte", "gt");

    private final TalentEditScreen parent;
    private final Screen returnScreen;
    private final ArrayList<TalentDraft.RequirementRow> rows = new ArrayList<>();
    private TalentDraft.RequirementMode mode;
    private final boolean editable;
    private int selected = -1;
    private int scroll;
    private Button modeButton;
    private Button typeButton;
    private Button addButton;
    private Button removeButton;
    private Button applyButton;
    private Button chooseButton;
    private Button operatorButton;
    private EditBox targetField;
    private EditBox amountField;
    private String status = "";

    public TalentRequirementsScreen(TalentEditScreen parent) {
        this(parent, parent);
    }

    TalentRequirementsScreen(TalentEditScreen parent, Screen returnScreen) {
        super(Component.translatable("screen.talent_editor.requirements.title"));
        this.parent = parent;
        this.returnScreen = returnScreen;
        TalentDraft.RequirementsDraft draft = parent.requirements();
        this.mode = draft.mode();
        this.editable = draft.editable();
        this.rows.addAll(draft.rows());
    }

    @Override
    protected void init() {
        int formWidth = Math.min(650, width - 32);
        int x = (width - formWidth) / 2;
        int gap = 12;
        int leftWidth = Math.max(120, (formWidth - gap) * 52 / 100);
        int rightX = x + leftWidth + gap;
        int rightWidth = formWidth - leftWidth - gap;

        modeButton = addRenderableWidget(Button.builder(modeLabel(), button -> cycleMode())
                .bounds(x, 38, leftWidth, 20).build());
        typeButton = addRenderableWidget(Button.builder(typeLabel(), button -> cycleType())
                .bounds(rightX, 38, rightWidth, 20).build());

        int chooseWidth = Math.min(82, Math.max(58, rightWidth / 3));
        targetField = new EditBox(font, rightX, 81, rightWidth - chooseWidth - 6, 20,
                Component.translatable("screen.talent_editor.requirements.target"));
        targetField.setMaxLength(256);
        addRenderableWidget(targetField);
        chooseButton = addRenderableWidget(Button.builder(
                        Component.translatable("screen.talent_editor.requirements.choose"), button -> openPicker())
                .bounds(rightX + rightWidth - chooseWidth, 81, chooseWidth, 20).build());

        amountField = new EditBox(font, rightX, 124, rightWidth - 96, 20,
                Component.translatable("screen.talent_editor.requirements.amount"));
        amountField.setMaxLength(16);
        addRenderableWidget(amountField);
        operatorButton = addRenderableWidget(Button.builder(Component.empty(), button -> cycleOperator())
                .bounds(rightX + rightWidth - 90, 124, 90, 20).build());

        int actionY = height - 52;
        addButton = addRenderableWidget(Button.builder(
                        Component.translatable("screen.talent_editor.requirements.add"), button -> addRow())
                .bounds(x, actionY, (leftWidth - 6) / 2, 20).build());
        removeButton = addRenderableWidget(Button.builder(
                        Component.translatable("screen.talent_editor.requirements.remove"), button -> removeRow())
                .bounds(x + (leftWidth + 6) / 2, actionY, (leftWidth - 6) / 2, 20).build());

        int backWidth = 76;
        addRenderableWidget(Button.builder(Component.translatable("gui.cancel"), button -> onClose())
                .bounds(x, height - 28, backWidth, 20).build());
        applyButton = addRenderableWidget(Button.builder(
                        Component.translatable("screen.talent_editor.requirements.apply"), button -> apply())
                .bounds(x + backWidth + 6, height - 28, formWidth - backWidth - 6, 20).build());

        if (!editable) {
            status = Component.translatable("screen.talent_editor.requirements.advanced_read_only").getString();
        }
        if (!rows.isEmpty()) selected = Math.min(Math.max(0, selected), rows.size() - 1);
        syncWidgets();
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics);
        super.render(graphics, mouseX, mouseY, partialTick);
        int formWidth = Math.min(650, width - 32);
        int x = (width - formWidth) / 2;
        int gap = 12;
        int leftWidth = Math.max(120, (formWidth - gap) * 52 / 100);
        int rightX = x + leftWidth + gap;
        int rightWidth = formWidth - leftWidth - gap;
        graphics.drawCenteredString(font, title, width / 2, 12, 0xFFFFFF);
        graphics.drawString(font, Component.translatable("screen.talent_editor.requirements.list", rows.size()),
                x, 65, 0xA0A0A0, false);
        graphics.drawString(font, targetLabel(), rightX, 70, 0xA0A0A0, false);
        TalentDraft.RequirementRow current = selectedRow();
        if (current != null && current.type() != TalentDraft.RequirementType.TALENT) {
            graphics.drawString(font, Component.translatable("screen.talent_editor.requirements.amount"),
                    rightX, 113, 0xA0A0A0, false);
        }
        TalentDraft.RequirementRow diagnosticRow = selectedRow();
        if (diagnosticRow != null) {
            ReferenceDiagnostics.Summary references = ReferenceDiagnostics.summarize(
                    parent.bootstrap(), referenceSource(diagnosticRow),
                    targetField == null ? diagnosticRow.target().toString() : targetField.getValue(), false
            );
            graphics.drawString(font, font.plainSubstrByWidth(
                    ReferenceDiagnostics.label(references).getString(), rightWidth
            ), rightX, 153, ReferenceDiagnostics.color(references), false);
        }
        graphics.drawWordWrap(font, Component.translatable("screen.talent_editor.requirements.help"),
                rightX, 165, rightWidth, 0x808080);
        renderRows(graphics, mouseX, mouseY, x, leftWidth);
        if (!status.isBlank()) {
            graphics.drawString(font, font.plainSubstrByWidth(status, rightWidth), rightX,
                    height - 47, editable ? 0xFF7777 : 0xFFAA55, false);
        }
    }

    private void renderRows(GuiGraphics graphics, int mouseX, int mouseY, int x, int width) {
        int top = 80;
        int bottom = height - 62;
        int visible = Math.max(1, (bottom - top) / ROW_HEIGHT);
        scroll = Math.max(0, Math.min(scroll, Math.max(0, rows.size() - visible)));
        int end = Math.min(rows.size(), scroll + visible);
        for (int index = scroll; index < end; index++) {
            TalentDraft.RequirementRow row = rows.get(index);
            int y = top + (index - scroll) * ROW_HEIGHT;
            boolean hovered = mouseX >= x && mouseX < x + width && mouseY >= y - 3 && mouseY < y + 14;
            if (index == selected || hovered) {
                graphics.fill(x, y - 3, x + width, y + 14,
                        index == selected ? 0x7050A0E8 : 0x404A90E2);
            }
            String suffix = row.type() == TalentDraft.RequirementType.TALENT ? ""
                    : row.type() == TalentDraft.RequirementType.ATTRIBUTE
                    ? " " + row.operator() + " " + row.attributeValue() : " >= " + row.amount();
            String label = Component.translatable(typeKey(row.type())).getString()
                    + " · " + row.target() + suffix;
            graphics.drawString(font, font.plainSubstrByWidth(label, width - 8), x + 4, y,
                    editable ? 0xE8E8E8 : 0x909090, false);
        }
        if (rows.isEmpty()) {
            graphics.drawCenteredString(font, Component.translatable("screen.talent_editor.requirements.empty"),
                    x + width / 2, top + 8, 0x808080);
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0) {
            int formWidth = Math.min(650, width - 32);
            int x = (width - formWidth) / 2;
            int leftWidth = Math.max(120, (formWidth - 12) * 52 / 100);
            int top = 80;
            int bottom = height - 62;
            if (mouseX >= x && mouseX < x + leftWidth && mouseY >= top && mouseY < bottom) {
                int index = scroll + (int) ((mouseY - top) / ROW_HEIGHT);
                if (index >= 0 && index < rows.size()) {
                    select(index);
                    return true;
                }
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        int visible = Math.max(1, (height - 62 - 80) / ROW_HEIGHT);
        scroll = Math.max(0, Math.min(
                scroll - (int) Math.signum(delta) * 3,
                Math.max(0, rows.size() - visible)
        ));
        return true;
    }

    private void addRow() {
        if (!editable || rows.size() >= MAX_ROWS || !flushSelected()) return;
        rows.add(new TalentDraft.RequirementRow(
                TalentDraft.RequirementType.TALENT, new ResourceLocation("custom", "required_talent"), 1L
        ));
        selected = rows.size() - 1;
        status = "";
        syncWidgets();
    }

    private void removeRow() {
        if (!editable || selectedRow() == null) return;
        rows.remove(selected);
        selected = Math.min(selected, rows.size() - 1);
        status = "";
        syncWidgets();
    }

    private void select(int index) {
        if (index == selected || !flushSelected()) return;
        selected = index;
        status = "";
        syncWidgets();
    }

    private void cycleMode() {
        if (!editable) return;
        mode = mode == TalentDraft.RequirementMode.ALL
                ? TalentDraft.RequirementMode.ANY : TalentDraft.RequirementMode.ALL;
        modeButton.setMessage(modeLabel());
    }

    private void cycleType() {
        if (!editable || selectedRow() == null || !flushSelected()) return;
        TalentDraft.RequirementRow old = rows.get(selected);
        TalentDraft.RequirementType[] values = TalentDraft.RequirementType.values();
        TalentDraft.RequirementType next = values[(old.type().ordinal() + 1) % values.length];
        long amount = next == TalentDraft.RequirementType.TALENT ? 1L : Math.max(1L, old.amount());
        ResourceLocation target = next == TalentDraft.RequirementType.ATTRIBUTE
                ? new ResourceLocation("minecraft", "generic.attack_speed") : old.target();
        rows.set(selected, new TalentDraft.RequirementRow(next, target, amount));
        status = "";
        syncWidgets();
    }

    private void cycleOperator() {
        TalentDraft.RequirementRow row = selectedRow();
        if (!editable || row == null || row.type() != TalentDraft.RequirementType.ATTRIBUTE
                || !flushSelected()) return;
        row = selectedRow();
        int index = OPERATORS.indexOf(row.operator());
        rows.set(selected, new TalentDraft.RequirementRow(row.type(), row.target(), row.amount(),
                OPERATORS.get((index + 1) % OPERATORS.size()), row.attributeValue()));
        syncWidgets();
    }

    private boolean flushSelected() {
        TalentDraft.RequirementRow old = selectedRow();
        if (old == null || !editable) return true;
        ResourceLocation target = ResourceLocation.tryParse(targetField.getValue().strip());
        if (target == null) {
            status = Component.translatable("screen.talent_editor.requirements.invalid_target").getString();
            return false;
        }
        long amount = 1L;
        double attributeValue = old.attributeValue();
        if (old.type() == TalentDraft.RequirementType.ATTRIBUTE) {
            try {
                attributeValue = Double.parseDouble(amountField.getValue().strip());
                if (!Double.isFinite(attributeValue) || attributeValue < -1.0E9D
                        || attributeValue > 1.0E9D) throw new NumberFormatException();
            } catch (NumberFormatException ignored) {
                status = Component.translatable("screen.talent_editor.requirements.invalid_amount").getString();
                return false;
            }
        } else if (old.type() != TalentDraft.RequirementType.TALENT) {
            try {
                amount = Long.parseLong(amountField.getValue().strip());
                boolean boundedCount = old.type() == TalentDraft.RequirementType.ITEM
                        || old.type() == TalentDraft.RequirementType.CATEGORY;
                long max = boundedCount ? 1_000_000L : 1_000_000_000_000L;
                long min = old.type() == TalentDraft.RequirementType.ITEM ? 1L : 0L;
                if (amount < min || amount > max) throw new NumberFormatException();
            } catch (NumberFormatException ignored) {
                status = Component.translatable("screen.talent_editor.requirements.invalid_amount").getString();
                return false;
            }
        }
        rows.set(selected, new TalentDraft.RequirementRow(old.type(), target, amount,
                old.operator(), attributeValue));
        return true;
    }

    private void apply() {
        if (!editable || !flushSelected()) return;
        parent.updateRequirements(mode, List.copyOf(rows));
        if (minecraft != null) minecraft.setScreen(returnScreen);
    }

    private void syncWidgets() {
        TalentDraft.RequirementRow row = selectedRow();
        boolean selectedEditable = editable && row != null;
        modeButton.active = editable;
        typeButton.active = selectedEditable;
        targetField.active = selectedEditable;
        chooseButton.active = selectedEditable;
        amountField.active = selectedEditable && row.type() != TalentDraft.RequirementType.TALENT;
        amountField.visible = row != null && row.type() != TalentDraft.RequirementType.TALENT;
        operatorButton.visible = row != null && row.type() == TalentDraft.RequirementType.ATTRIBUTE;
        operatorButton.active = selectedEditable;
        addButton.active = editable && rows.size() < MAX_ROWS;
        removeButton.active = selectedEditable;
        applyButton.active = editable;
        typeButton.setMessage(typeLabel());
        modeButton.setMessage(modeLabel());
        if (row == null) {
            targetField.setValue("");
            amountField.setValue("");
        } else {
            targetField.setValue(row.target().toString());
            amountField.setValue(row.type() == TalentDraft.RequirementType.ATTRIBUTE
                    ? Double.toString(row.attributeValue()) : Long.toString(row.amount()));
            operatorButton.setMessage(Component.translatable(
                    "screen.talent_editor.expression_condition.operator_help." + row.operator()));
        }
    }

    private TalentDraft.RequirementRow selectedRow() {
        return selected >= 0 && selected < rows.size() ? rows.get(selected) : null;
    }

    private void openPicker() {
        TalentDraft.RequirementRow row = selectedRow();
        if (!editable || row == null || !flushSelected() || minecraft == null) return;
        ResourceReferencePickerScreen.Source source = referenceSource(row);
        minecraft.setScreen(new ResourceReferencePickerScreen(
                this, source, parent.bootstrap(), this::chooseTarget
        ));
    }

    private static ResourceReferencePickerScreen.Source referenceSource(TalentDraft.RequirementRow row) {
        return switch (row.type()) {
            case TALENT -> ResourceReferencePickerScreen.Source.TALENT;
            case COUNTER -> ResourceReferencePickerScreen.Source.COUNTER;
            case COUNTER_CATEGORY -> ResourceReferencePickerScreen.Source.COUNTER_CATEGORY;
            case CATEGORY -> ResourceReferencePickerScreen.Source.CATEGORY;
            case ITEM -> ResourceReferencePickerScreen.Source.ITEM;
            case ATTRIBUTE -> ResourceReferencePickerScreen.Source.ATTRIBUTE;
        };
    }

    void chooseTarget(ResourceLocation target) {
        TalentDraft.RequirementRow row = selectedRow();
        if (row == null) return;
        rows.set(selected, new TalentDraft.RequirementRow(row.type(), target, row.amount(),
                row.operator(), row.attributeValue()));
        status = "";
    }

    private Component modeLabel() {
        return Component.translatable("screen.talent_editor.requirements.mode." + mode.serializedName());
    }

    private Component typeLabel() {
        TalentDraft.RequirementRow row = selectedRow();
        return row == null
                ? Component.translatable("screen.talent_editor.requirements.no_selection")
                : Component.translatable(typeKey(row.type()));
    }

    private Component targetLabel() {
        TalentDraft.RequirementRow row = selectedRow();
        String suffix = row == null ? "none" : row.type().serializedName();
        return Component.translatable("screen.talent_editor.requirements.target." + suffix);
    }

    private static String typeKey(TalentDraft.RequirementType type) {
        return "screen.talent_editor.requirements.type." + type.serializedName();
    }

    @Override
    public void onClose() {
        if (minecraft != null) minecraft.setScreen(returnScreen);
    }
}
