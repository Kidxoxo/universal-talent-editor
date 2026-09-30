package com.kidxoxo.talenteditor.client;

import com.google.gson.JsonObject;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;

/** Ordered visual list for trigger and lifecycle extension actions. */
public final class TalentAdvancedActionListScreen extends Screen {
    private static final int ROW_HEIGHT = 19;
    private final Screen returnScreen;
    private final TalentEditScreen owner;
    private final TalentDraft.AdvancedActionLocation location;
    private final int triggerRawIndex;
    private int selected;
    private int scroll;
    private Button editButton;
    private Button removeButton;
    private Button upButton;
    private Button downButton;
    private Button gateButton;
    private Button previewButton;
    private String status = "";

    TalentAdvancedActionListScreen(
            Screen returnScreen,
            TalentEditScreen owner,
            TalentDraft.AdvancedActionLocation location,
            int triggerRawIndex
    ) {
        super(Component.translatable("screen.talent_editor.advanced_actions.title." + key(location)));
        this.returnScreen = returnScreen;
        this.owner = owner;
        this.location = location;
        this.triggerRawIndex = triggerRawIndex;
    }

    @Override
    protected void init() {
        int formWidth = Math.min(680, width - 32);
        int x = (width - formWidth) / 2;
        int gap = 6;
        int third = (formWidth - gap * 2) / 3;
        addRenderableWidget(Button.builder(Component.translatable("screen.talent_editor.advanced_actions.add"),
                        button -> openEditor(-1))
                .bounds(x, height - 55, third, 20).build());
        editButton = addRenderableWidget(Button.builder(Component.translatable(
                        "screen.talent_editor.advanced_actions.edit"), button -> editSelected())
                .bounds(x + third + gap, height - 55, third, 20).build());
        removeButton = addRenderableWidget(Button.builder(Component.translatable(
                        "screen.talent_editor.advanced_actions.remove"), button -> removeSelected())
                .bounds(x + (third + gap) * 2, height - 55,
                        formWidth - (third + gap) * 2, 20).build());
        int bottomColumns = location == TalentDraft.AdvancedActionLocation.ACTIVE_EFFECTS
                || isPresentationLocation() ? 3 : 4;
        int bottomWidth = (formWidth - gap * (bottomColumns - 1)) / bottomColumns;
        upButton = addRenderableWidget(Button.builder(Component.translatable(
                        "screen.talent_editor.advanced_actions.up"), button -> move(-1))
                .bounds(x, height - 28, bottomWidth, 20).build());
        downButton = addRenderableWidget(Button.builder(Component.translatable(
                        "screen.talent_editor.advanced_actions.down"), button -> move(1))
                .bounds(x + bottomWidth + gap, height - 28, bottomWidth, 20).build());
        if (bottomColumns == 4) {
            previewButton = addRenderableWidget(Button.builder(Component.translatable(
                    "screen.talent_editor.particles.preview_group"), button -> previewGroup())
                    .bounds(x + (bottomWidth + gap) * 2, height - 28, bottomWidth, 20).build());
        }
        addRenderableWidget(Button.builder(Component.translatable("gui.back"), button -> returnParent())
                .bounds(x + (bottomWidth + gap) * (bottomColumns - 1), height - 28,
                        formWidth - (bottomWidth + gap) * (bottomColumns - 1), 20).build());
        gateButton = addRenderableWidget(Button.builder(Component.translatable(
                        "screen.talent_editor.action_gate.open"), button -> openGate())
                .bounds(x + formWidth - 100, 8, 100, 20).build());
        if (location != TalentDraft.AdvancedActionLocation.ACTIVE_EFFECTS && !isPresentationLocation()) {
            addRenderableWidget(Button.builder(Component.translatable(
                    "screen.talent_editor.particles.add"), button -> openParticleEditor(-1))
                    .bounds(x, 8, 108, 20).build());
        }
        syncButtons();
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics);
        int formWidth = Math.min(680, width - 32);
        int x = (width - formWidth) / 2;
        int top = 44;
        int bottom = height - 64;
        EditorTheme.panel(graphics, x - 7, 27, formWidth + 14, Math.max(1, bottom - 27));
        super.render(graphics, mouseX, mouseY, partialTick);
        graphics.drawCenteredString(font, title, width / 2, 10, EditorTheme.TEXT);
        graphics.drawCenteredString(font, Component.translatable("screen.talent_editor.advanced_actions.help"),
                width / 2, 28, EditorTheme.MUTED);
        List<TalentDraft.AdvancedActionRow> rows = rows();
        int visible = Math.max(1, (bottom - top) / ROW_HEIGHT);
        scroll = clamp(scroll, rows.size(), visible);
        for (int index = scroll; index < Math.min(rows.size(), scroll + visible); index++) {
            TalentDraft.AdvancedActionRow row = rows.get(index);
            int y = top + (index - scroll) * ROW_HEIGHT;
            boolean hovered = mouseX >= x && mouseX < x + formWidth && mouseY >= y - 2 && mouseY < y + 15;
            if (index == selected || hovered) graphics.fill(x, y - 2, x + formWidth, y + 15,
                    index == selected ? 0x7050A0E8 : 0x404A90E2);
            Component name = isPresentationRow(row)
                    ? Component.translatable("screen.talent_editor.advanced_actions.presentation_managed")
                    : row.editable()
                    ? Component.translatable("screen.talent_editor.advanced_action.type." + row.type())
                    : Component.literal("[JSON] " + row.type());
            var action = owner.draft().advancedAction(location, triggerRawIndex, row.rawIndex());
            String gated = action.has("enabledWhen") || action.has("disabledWhen")
                    ? " " + Component.translatable("screen.talent_editor.action_gate.marked").getString() : "";
            graphics.drawString(font, font.plainSubstrByWidth((index + 1) + ". " + name.getString() + gated,
                            formWidth - 8), x + 4, y,
                    row.editable() ? EditorTheme.TEXT : 0x909090, false);
        }
        if (rows.isEmpty()) graphics.drawCenteredString(font,
                Component.translatable("screen.talent_editor.advanced_actions.empty"),
                width / 2, top + 10, EditorTheme.MUTED);
        if (!status.isBlank()) graphics.drawString(font, font.plainSubstrByWidth(status, formWidth),
                x, height - 73, 0xFF7777, false);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0) {
            int formWidth = Math.min(680, width - 32);
            int x = (width - formWidth) / 2;
            int top = 44;
            int bottom = height - 64;
            if (mouseX >= x && mouseX < x + formWidth && mouseY >= top && mouseY < bottom) {
                int index = scroll + (int) ((mouseY - top) / ROW_HEIGHT);
                if (index >= 0 && index < rows().size()) {
                    selected = index;
                    status = "";
                    syncButtons();
                    return true;
                }
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        int visible = Math.max(1, (height - 64 - 44) / ROW_HEIGHT);
        scroll = clamp(scroll - (int) Math.signum(delta) * 3, rows().size(), visible);
        return true;
    }

    private List<TalentDraft.AdvancedActionRow> rows() {
        List<TalentDraft.AdvancedActionRow> all = owner.draft().advancedActionRows(location, triggerRawIndex);
        return isPresentationLocation() ? all.stream().filter(row -> !isPresentationRow(row)).toList() : all;
    }

    private boolean isPresentationLocation() {
        return location == TalentDraft.AdvancedActionLocation.ON_ACQUIRE
                || location == TalentDraft.AdvancedActionLocation.ON_SYNTHESIZE;
    }

    private TalentDraft.AdvancedActionRow selectedRow() {
        List<TalentDraft.AdvancedActionRow> rows = rows();
        return rows.isEmpty() ? null : rows.get(Math.max(0, Math.min(selected, rows.size() - 1)));
    }

    private void syncButtons() {
        if (editButton == null) return;
        TalentDraft.AdvancedActionRow row = selectedRow();
        editButton.active = row != null && row.editable();
        removeButton.active = row != null && !isPresentationRow(row)
                && !(location == TalentDraft.AdvancedActionLocation.TRIGGER_ACTIONS && rows().size() <= 1);
        upButton.active = row != null && !isPresentationRow(row) && selected > 0;
        downButton.active = row != null && !isPresentationRow(row) && selected + 1 < rows().size();
        gateButton.active = row != null;
        if (previewButton != null) previewButton.active = rows().stream()
                .anyMatch(candidate -> candidate.type().equals("spawn_particles"));
    }

    private void editSelected() {
        TalentDraft.AdvancedActionRow row = selectedRow();
        if (row != null && row.editable()) openEditor(row.rawIndex());
    }

    private void openEditor(int actionIndex) {
        if (minecraft == null) return;
        if (actionIndex >= 0 && owner.draft().advancedAction(location, triggerRawIndex, actionIndex)
                .get("type").getAsString().replaceFirst("^talent_editor:", "").equals("spawn_particles")) {
            openParticleEditor(actionIndex);
        } else minecraft.setScreen(new TalentAdvancedActionScreen(
                this, owner, location, triggerRawIndex, actionIndex));
    }

    private void openParticleEditor(int actionIndex) {
        if (minecraft != null) minecraft.setScreen(new TalentParticleActionScreen(
                this, owner, location, triggerRawIndex, actionIndex));
    }

    private void previewGroup() {
        if (minecraft == null || minecraft.level == null || minecraft.player == null) return;
        List<JsonObject> particles = new ArrayList<>();
        for (TalentDraft.AdvancedActionRow row : rows()) {
            if (row.type().equals("spawn_particles")) {
                particles.add(owner.draft().advancedAction(location, triggerRawIndex, row.rawIndex()));
            }
        }
        if (particles.isEmpty()) return;
        try {
            minecraft.setScreen(new TalentParticlePreviewScreen(this, particles));
        } catch (RuntimeException exception) {
            status = Component.translatable("screen.talent_editor.particles.preview_group_invalid").getString();
        }
    }

    private void openGate() {
        TalentDraft.AdvancedActionRow row = selectedRow();
        if (row != null && minecraft != null) minecraft.setScreen(new TalentActionGateScreen(
                this, owner, location, triggerRawIndex, row.rawIndex()));
    }

    private void removeSelected() {
        TalentDraft.AdvancedActionRow row = selectedRow();
        if (row == null) return;
        try {
            owner.draft().removeAdvancedAction(location, triggerRawIndex, row.rawIndex());
            ClientEditorState.clearValidation();
            selected = Math.max(0, Math.min(selected, rows().size() - 1));
        } catch (IllegalStateException exception) {
            status = Component.translatable("screen.talent_editor.advanced_actions.primary_required").getString();
        }
        syncButtons();
    }

    private void move(int offset) {
        TalentDraft.AdvancedActionRow row = selectedRow();
        if (row == null) return;
        List<TalentDraft.AdvancedActionRow> visible = rows();
        if (isPresentationLocation() && selected + offset >= 0 && selected + offset < visible.size()) {
            owner.draft().swapAdvancedActions(location, triggerRawIndex,
                    row.rawIndex(), visible.get(selected + offset).rawIndex());
        } else owner.draft().moveAdvancedAction(location, triggerRawIndex, row.rawIndex(), offset);
        selected += offset;
        ClientEditorState.clearValidation();
        syncButtons();
    }

    private static String key(TalentDraft.AdvancedActionLocation location) {
        return switch (location) {
            case ACTIVE_EFFECTS -> "active_effects";
            case ON_ACQUIRE -> "on_acquire";
            case ON_SYNTHESIZE -> "on_synthesize";
            case ON_REMOVE -> "on_remove";
            case TRIGGER_ACTIONS -> "trigger";
            case TRIGGER_FAILURE_ACTIONS -> "failure";
        };
    }

    private boolean isPresentationRow(TalentDraft.AdvancedActionRow row) {
        return row != null
                && (location == TalentDraft.AdvancedActionLocation.ON_ACQUIRE
                || location == TalentDraft.AdvancedActionLocation.ON_SYNTHESIZE)
                && (row.type().equals("play_sound") || row.type().equals("show_message")
                || row.type().equals("spawn_particles"));
    }

    private static int clamp(int value, int size, int visible) {
        return Math.max(0, Math.min(value, Math.max(0, size - visible)));
    }

    private void returnParent() {
        if (returnScreen instanceof TalentTriggerScreen triggerScreen) triggerScreen.refreshAdvancedChanges();
        if (minecraft != null) minecraft.setScreen(returnScreen);
    }
    @Override public void onClose() { returnParent(); }
}
