package com.kidxoxo.talenteditor.client;

import com.google.gson.JsonObject;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.List;

/** Ordered acquisition/synthesis cues; behavioural actions remain in their own event list. */
final class TalentPresentationTimelineScreen extends Screen {
    private static final int ROW = 22;
    private final Screen parent;
    private final TalentEditScreen owner;
    private final TalentDraft original;
    private TalentDraft.PresentationPhase phase;
    private boolean applied;
    private int selected;
    private int scroll;
    private Button edit, remove, up, down;
    private Button acquirePhaseButton, synthesisPhaseButton;
    private String status = "";

    TalentPresentationTimelineScreen(Screen parent, TalentEditScreen owner, TalentDraft.PresentationPhase phase) {
        super(Component.translatable("screen.talent_editor.presentation.timeline_title"));
        this.parent = parent;
        this.owner = owner;
        this.original = owner.draft().copy();
        this.phase = phase;
    }

    @Override protected void init() {
        int w = Math.min(700, width - 32), x = (width - w) / 2;
        int phaseWidth = (w - 6) / 2;
        acquirePhaseButton = addRenderableWidget(Button.builder(
                Component.translatable("screen.talent_editor.presentation.phase.acquire"),
                button -> selectPhase(TalentDraft.PresentationPhase.ACQUIRE))
                .bounds(x, 27, phaseWidth, 20).build());
        synthesisPhaseButton = addRenderableWidget(Button.builder(
                Component.translatable("screen.talent_editor.presentation.phase.synthesize"),
                button -> selectPhase(TalentDraft.PresentationPhase.SYNTHESIZE))
                .bounds(x + phaseWidth + 6, 27, w - phaseWidth - 6, 20).build());
        int third = (w - 12) / 3;
        int fourth = (w - 18) / 4;
        addRenderableWidget(Button.builder(Component.translatable("screen.talent_editor.presentation.add_text"),
                button -> openCue("show_message", -1)).bounds(x, 54, fourth, 20).build());
        addRenderableWidget(Button.builder(Component.translatable("screen.talent_editor.presentation.add_sound"),
                button -> openCue("play_sound", -1)).bounds(x + fourth + 6, 54, fourth, 20).build());
        addRenderableWidget(Button.builder(Component.translatable("screen.talent_editor.presentation.add_particles"),
                button -> openCue("spawn_particles", -1)).bounds(x + (fourth + 6) * 2, 54,
                fourth, 20).build());
        addRenderableWidget(Button.builder(Component.translatable("screen.talent_editor.presentation.add_transform"),
                button -> openCue("presentation_transform", -1)).bounds(x + (fourth + 6) * 3, 54,
                w - (fourth + 6) * 3, 20).build());
        int bottom = height - 55;
        edit = addRenderableWidget(Button.builder(Component.translatable("screen.talent_editor.advanced_actions.edit"),
                button -> editSelected()).bounds(x, bottom, third, 20).build());
        remove = addRenderableWidget(Button.builder(Component.translatable("screen.talent_editor.advanced_actions.remove"),
                button -> removeSelected()).bounds(x + third + 6, bottom, third, 20).build());
        addRenderableWidget(Button.builder(Component.translatable("screen.talent_editor.presentation.preview_full"),
                button -> preview()).bounds(x + (third + 6) * 2, bottom,
                w - (third + 6) * 2, 20).build());
        int quarter = (w - 18) / 4;
        down = addRenderableWidget(Button.builder(Component.translatable("screen.talent_editor.advanced_actions.down"),
                button -> move(1)).bounds(x + quarter + 6, height - 28, quarter, 20).build());
        up = addRenderableWidget(Button.builder(Component.translatable("screen.talent_editor.advanced_actions.up"),
                button -> move(-1)).bounds(x, height - 28, quarter, 20).build());
        addRenderableWidget(Button.builder(Component.translatable("gui.cancel"), button -> close())
                .bounds(x + (quarter + 6) * 2, height - 28, quarter, 20).build());
        addRenderableWidget(Button.builder(Component.translatable("screen.talent_editor.presentation.apply"),
                button -> apply()).bounds(x + (quarter + 6) * 3, height - 28,
                w - (quarter + 6) * 3, 20).build());
        updateButtons();
    }

    private void selectPhase(TalentDraft.PresentationPhase next) {
        if (phase == next) return;
        phase = next;
        selected = scroll = 0;
        updateButtons();
    }

    private TalentDraft.AdvancedActionLocation location() {
        return phase == TalentDraft.PresentationPhase.ACQUIRE
                ? TalentDraft.AdvancedActionLocation.ON_ACQUIRE
                : TalentDraft.AdvancedActionLocation.ON_SYNTHESIZE;
    }

    private List<TalentDraft.AdvancedActionRow> cues() { return owner.draft().presentationCues(phase); }

    private void updateButtons() {
        if (edit == null) return;
        List<TalentDraft.AdvancedActionRow> rows = cues();
        edit.active = !rows.isEmpty();
        remove.active = !rows.isEmpty();
        up.active = selected > 0 && selected < rows.size();
        down.active = selected >= 0 && selected + 1 < rows.size();
        if (acquirePhaseButton != null) acquirePhaseButton.active = phase != TalentDraft.PresentationPhase.ACQUIRE;
        if (synthesisPhaseButton != null) synthesisPhaseButton.active = phase != TalentDraft.PresentationPhase.SYNTHESIZE;
    }

    private void openCue(String type, int rawIndex) {
        if (minecraft == null) return;
        if (type.equals("spawn_particles")) {
            minecraft.setScreen(new TalentParticleActionScreen(this, owner, location(), -1, rawIndex));
        } else if (type.equals("presentation_transform")) {
            minecraft.setScreen(new TalentPresentationTransformScreen(this, owner, location(), rawIndex));
        } else minecraft.setScreen(new TalentPresentationCueScreen(this, owner, location(), rawIndex, type));
    }

    private void editSelected() {
        List<TalentDraft.AdvancedActionRow> rows = cues();
        if (rows.isEmpty()) return;
        TalentDraft.AdvancedActionRow row = rows.get(Math.min(selected, rows.size() - 1));
        openCue(row.type(), row.rawIndex());
    }

    private void removeSelected() {
        List<TalentDraft.AdvancedActionRow> rows = cues();
        if (rows.isEmpty()) return;
        owner.draft().removeAdvancedAction(location(), -1,
                rows.get(Math.min(selected, rows.size() - 1)).rawIndex());
        selected = Math.min(selected, Math.max(0, cues().size() - 1));
        ClientEditorState.clearValidation();
        updateButtons();
    }

    private void move(int direction) {
        List<TalentDraft.AdvancedActionRow> rows = cues();
        if (selected < 0 || selected >= rows.size()) return;
        owner.draft().movePresentationCue(phase, rows.get(selected).rawIndex(), direction);
        selected += direction;
        ClientEditorState.clearValidation();
        updateButtons();
    }

    private void preview() {
        if (minecraft == null || minecraft.level == null || minecraft.player == null) return;
        try { minecraft.setScreen(new TalentCinematicPreviewScreen(this, owner.draft(), phase)); }
        catch (RuntimeException exception) { status = exception.getMessage(); }
    }

    @Override public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics);
        int w = Math.min(700, width - 32), x = (width - w) / 2;
        EditorTheme.panel(graphics, x - 7, 24, w + 14, Math.max(1, height - 87));
        super.render(graphics, mouseX, mouseY, partialTick);
        graphics.drawCenteredString(font, title, width / 2, 8, EditorTheme.TEXT);
        int phaseWidth = (w - 6) / 2;
        EditorTheme.selectedTab(graphics, phase == TalentDraft.PresentationPhase.ACQUIRE
                ? x : x + phaseWidth + 6, 27, phase == TalentDraft.PresentationPhase.ACQUIRE
                ? phaseWidth : w - phaseWidth - 6);
        List<TalentDraft.AdvancedActionRow> rows = cues();
        int top = 84, bottom = height - 66, visible = Math.max(1, (bottom - top) / ROW);
        scroll = Math.max(0, Math.min(scroll, Math.max(0, rows.size() - visible)));
        for (int i = scroll; i < Math.min(rows.size(), scroll + visible); i++) {
            TalentDraft.AdvancedActionRow row = rows.get(i);
            JsonObject cue = owner.draft().advancedAction(location(), -1, row.rawIndex());
            int y = top + (i - scroll) * ROW;
            if (i == selected) graphics.fill(x, y - 2, x + w, y + 17, 0x7050A0E8);
            String kind = Component.translatable("screen.talent_editor.presentation.cue." + row.type()).getString();
            int at = cue.has("atTick") ? cue.get("atTick").getAsInt() : 0;
            String detail = switch (row.type()) {
                case "play_sound" -> cue.has("sound") ? cue.get("sound").getAsString() : "";
                case "spawn_particles" -> cue.has("particle") ? cue.get("particle").getAsString() : "";
                case "presentation_transform" -> cue.has("durationTicks")
                        ? cue.get("durationTicks").getAsString() + "t" : "";
                default -> cue.has("text") ? LocalizedTextDraft.parse(cue.get("text")).fallbackText() : "";
            };
            String line = (i + 1) + ".  " + at + "t   " + kind + "   " + detail;
            graphics.drawString(font, font.plainSubstrByWidth(line, w - 12), x + 6, y + 2,
                    EditorTheme.TEXT, false);
        }
        if (rows.isEmpty()) graphics.drawCenteredString(font, Component.translatable(
                "screen.talent_editor.presentation.timeline_empty"), width / 2, top + 12, EditorTheme.MUTED);
        if (status != null && !status.isBlank()) graphics.drawString(font,
                font.plainSubstrByWidth(status, w), x, height - 68, 0xFF7777, false);
    }

    @Override public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0) {
            int w = Math.min(700, width - 32), x = (width - w) / 2;
            int top = 84, bottom = height - 66;
            if (mouseX >= x && mouseX < x + w && mouseY >= top && mouseY < bottom) {
                int index = scroll + (int) ((mouseY - top) / ROW);
                if (index < cues().size()) { selected = index; updateButtons(); return true; }
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        scroll = Math.max(0, scroll - (int) Math.signum(delta) * 3);
        return true;
    }

    private void apply() {
        applied = true;
        owner.presentationApplied();
        close();
    }
    private void close() {
        if (!applied) owner.draft().replaceAllFrom(original);
        if (minecraft != null) minecraft.setScreen(parent);
    }

    @Override public void onClose() { close(); }
}
