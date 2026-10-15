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

public final class TalentComplexEffectsScreen extends EditorScreen {
    private static final int ROW_HEIGHT = 20;
    private final TalentEditScreen owner;
    private final Screen parent;
    private final EditorBootstrap bootstrap;
    private int selectedRawIndex = -1;
    private int scroll;
    private EditBox refreshAmount;
    private Button applyRefresh;
    private Button editSelected;
    private Button remove;
    private Button flight;
    private String status = "";

    TalentComplexEffectsScreen(TalentEditScreen owner, Screen parent, EditorBootstrap bootstrap) {
        super(Component.translatable("screen.talent_editor.complex.title"));
        this.owner = owner;
        this.parent = parent;
        this.bootstrap = bootstrap;
    }

    @Override
    protected void init() {
        int formWidth = Math.min(700, width - 32);
        int x = (width - formWidth) / 2;
        int gap = 6;
        int topThird = (formWidth - gap * 2) / 3;
        addRenderableWidget(EditorWidgets.button(Component.translatable("screen.talent_editor.complex.add_dynamic"),
                        button -> addDynamic())
                .bounds(x, 42, topThird, 20).build());
        addRenderableWidget(EditorWidgets.button(Component.translatable("screen.talent_editor.add_refresh_bonus"),
                        button -> addRefresh())
                .bounds(x + topThird + gap, 42, topThird, 20).build());
        flight = addRenderableWidget(EditorWidgets.button(flightLabel(), button -> toggleFlight())
                .bounds(x + (topThird + gap) * 2, 42, formWidth - (topThird + gap) * 2, 20).build());

        int detailY = height - 84;
        int third = (formWidth - gap * 2) / 3;
        refreshAmount = new EditBox(font, x, detailY, third, 20,
                Component.translatable("screen.talent_editor.complex.refresh_amount"));
        refreshAmount.setMaxLength(3);
        addRenderableWidget(refreshAmount);
        applyRefresh = addRenderableWidget(EditorWidgets.button(
                        Component.translatable("screen.talent_editor.complex.apply_refresh"),
                        button -> applyRefresh())
                .bounds(x + third + gap, detailY, third, 20).build());
        editSelected = addRenderableWidget(EditorWidgets.button(
                        Component.translatable("screen.talent_editor.complex.edit_dynamic"),
                        button -> editSelected())
                .bounds(x + (third + gap) * 2, detailY,
                        formWidth - (third + gap) * 2, 20).build());

        int bottom = height - 28;
        int half = (formWidth - gap) / 2;
        addRenderableWidget(EditorWidgets.button(Component.translatable("gui.back"), button -> returnOwner())
                .bounds(x, bottom, half, 20).build());
        remove = addRenderableWidget(EditorWidgets.button(Component.translatable("screen.talent_editor.remove_effect"), EditorTheme.Tone.DANGER,
                        button -> removeSelected())
                .bounds(x + half + gap, bottom, formWidth - half - gap, 20).build());
        syncSelection();
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics);
        int formWidth = Math.min(700, width - 32);
        int x = (width - formWidth) / 2;
        EditorTheme.panel(graphics, x - 7, 27, formWidth + 14, 42);
        EditorTheme.panel(graphics, x - 7, 70, formWidth + 14, Math.max(1, height - 167));
        EditorTheme.panel(graphics, x - 7, height - 91, formWidth + 14, 55);
        super.render(graphics, mouseX, mouseY, partialTick);
        EditorTheme.title(graphics,font,title,width);
        graphics.drawCenteredString(font, Component.translatable("screen.talent_editor.complex.help"),
                width / 2, 28, EditorTheme.MUTED);
        renderRows(graphics, mouseX, mouseY, x, formWidth);
        if (!status.isBlank()) {
            graphics.drawString(font, font.plainSubstrByWidth(status, formWidth), x, height - 57,
                    EditorTheme.MUTED, false);
        }
    }

    private void renderRows(GuiGraphics graphics, int mouseX, int mouseY, int x, int formWidth) {
        List<TalentDraft.EffectRow> rows = rows();
        int top = 82;
        int bottom = height - 96;
        int visible = Math.max(1, (bottom - top) / ROW_HEIGHT);
        scroll = Math.max(0, Math.min(scroll, Math.max(0, rows.size() - visible)));
        int end = Math.min(rows.size(), scroll + visible);
        for (int index = scroll; index < end; index++) {
            TalentDraft.EffectRow row = rows.get(index);
            int y = top + (index - scroll) * ROW_HEIGHT;
            boolean selected = row.rawIndex() == selectedRawIndex;
            boolean hovered = mouseX >= x && mouseX < x + formWidth && mouseY >= y - 3 && mouseY < y + 16;
            if (selected || hovered) {
                graphics.fill(x, y - 3, x + formWidth, y + 16,
                        selected ? 0x7050A0E8 : 0x404A90E2);
            }
            String label;
            if (row.editableDynamicAttribute()) {
                TalentDraft.DynamicAttributeDraft value = owner.draft().dynamicAttribute(row.rawIndex());
                label = Component.translatable("screen.talent_editor.complex.dynamic_row",
                        value.attribute().toString(), sourceLabel(value.source()), compact(value.base()),
                        compact(value.scale())).getString();
            } else if (row.editableDrawRefreshBonus()) {
                label = Component.translatable("screen.talent_editor.refresh_bonus", (int) row.amount()).getString();
            } else {
                label = Component.translatable("screen.talent_editor.creative_flight_effect").getString();
            }
            graphics.drawString(font, font.plainSubstrByWidth(label, formWidth - 8), x + 4, y,
                    0xFFE8E8E8, false);
        }
        if (rows.isEmpty()) {
            graphics.drawCenteredString(font, Component.translatable("screen.talent_editor.complex.empty"),
                    width / 2, top + 10, 0xFF808080);
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0) {
            int formWidth = Math.min(700, width - 32);
            int x = (width - formWidth) / 2;
            int top = 82;
            int bottom = height - 96;
            if (mouseX >= x && mouseX < x + formWidth && mouseY >= top && mouseY < bottom) {
                int index = scroll + (int) ((mouseY - top) / ROW_HEIGHT);
                if(index>=scroll+Math.max(1,(height-82-96)/ROW_HEIGHT))return true;
                List<TalentDraft.EffectRow> rows = rows();
                if (index >= 0 && index < rows.size()) {
                    selectedRawIndex = rows.get(index).rawIndex();
                    syncSelection();
                    return true;
                }
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        int visible = Math.max(1, (height - 96 - 82) / ROW_HEIGHT);
        scroll = Math.max(0, Math.min(scroll - (int) Math.signum(delta) * 3,
                Math.max(0, rows().size() - visible)));
        return true;
    }

    private void addDynamic() {
        int rawIndex = owner.draft().addDynamicAttribute(
                new ResourceLocation("minecraft", "generic.attack_damage")
        );
        selectedRawIndex = rawIndex;
        if (minecraft != null) {
            minecraft.setScreen(new TalentDynamicAttributeScreen(owner, this, bootstrap, rawIndex, true));
        }
    }

    private void editSelected() {
        TalentDraft.EffectRow selected = selected();
        if (selected == null || minecraft == null) return;
        if (selected.editableDynamicAttribute()) {
            minecraft.setScreen(new TalentDynamicAttributeScreen(
                    owner, this, bootstrap, selected.rawIndex(), false
            ));
        }
    }

    private void addRefresh() {
        selectedRawIndex = owner.draft().addDrawRefreshBonus();
        owner.markComplexUpdated("screen.talent_editor.refresh_bonus_added");
        status = Component.translatable("screen.talent_editor.refresh_bonus_added").getString();
        syncSelection();
    }

    private void toggleFlight() {
        if (owner.draft().hasCreativeFlight()) {
            owner.draft().removeCreativeFlight();
            selectedRawIndex = -1;
            status = Component.translatable("screen.talent_editor.complex.flight_removed").getString();
        } else {
            selectedRawIndex = owner.draft().addCreativeFlight();
            status = Component.translatable("screen.talent_editor.complex.flight_added").getString();
        }
        owner.markComplexUpdated("screen.talent_editor.complex.updated");
        flight.setMessage(flightLabel());
        syncSelection();
    }

    private void applyRefresh() {
        TalentDraft.EffectRow selected = selected();
        if (selected == null || !selected.editableDrawRefreshBonus()) return;
        try {
            int amount = Integer.parseInt(refreshAmount.getValue().strip());
            if (amount < 1 || amount > 100) throw new NumberFormatException();
            owner.draft().updateDrawRefreshBonus(selected.rawIndex(), amount);
            owner.markComplexUpdated("screen.talent_editor.complex.updated");
            status = Component.translatable("screen.talent_editor.complex.refresh_updated").getString();
            syncSelection();
        } catch (NumberFormatException exception) {
            status = Component.translatable("screen.talent_editor.invalid_refresh_bonus").getString();
        }
    }

    private void removeSelected() {
        TalentDraft.EffectRow selected = selected();
        if (selected == null) return;
        owner.draft().removeEditableActiveEffect(selected.rawIndex());
        selectedRawIndex = -1;
        owner.markComplexUpdated("screen.talent_editor.complex.updated");
        status = Component.translatable("screen.talent_editor.effect_removed").getString();
        if (flight != null) flight.setMessage(flightLabel());
        syncSelection();
    }

    private void syncSelection() {
        TalentDraft.EffectRow selected = selected();
        boolean refresh = selected != null && selected.editableDrawRefreshBonus();
        refreshAmount.active = refresh;
        applyRefresh.active = refresh;
        editSelected.active = selected != null && selected.editableDynamicAttribute();
        editSelected.setMessage(Component.translatable("screen.talent_editor.complex.edit_dynamic"));
        remove.active = selected != null;
        refreshAmount.setValue(refresh ? Integer.toString((int) selected.amount()) : "");
    }

    private TalentDraft.EffectRow selected() {
        return rows().stream().filter(row -> row.rawIndex() == selectedRawIndex).findFirst().orElse(null);
    }

    private List<TalentDraft.EffectRow> rows() {
        return owner.draft().specialEffectRows();
    }

    private Component flightLabel() {
        return Component.translatable("screen.talent_editor.complex.flight",
                Component.translatable(owner.draft().hasCreativeFlight() ? "options.on" : "options.off"));
    }

    private static String sourceLabel(String source) {
        return Component.translatable("screen.talent_editor.dynamic.source." + source).getString();
    }

    private static String compact(double value) {
        if (value == Math.rint(value)) return Long.toString((long) value);
        return String.format(Locale.ROOT, "%.6f", value).replaceFirst("0+$", "").replaceFirst("\\.$", "");
    }

    private void returnOwner() {
        if (minecraft != null) minecraft.setScreen(parent);
    }

    @Override
    public void onClose() {
        returnOwner();
    }
}
