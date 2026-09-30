package com.kidxoxo.talenteditor.client;

import com.google.gson.JsonObject;
import com.kidxoxo.talenteditor.domain.PresentationTransformAction;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/** Edits a bounded model and position cue in the acquisition/synthesis timeline. */
final class TalentPresentationTransformScreen extends Screen {
    private final Screen parent;
    private final TalentEditScreen owner;
    private final TalentDraft.AdvancedActionLocation location;
    private final int actionIndex;
    private JsonObject draft;
    private EditBox atTick, duration, alphaFrom, alphaTo, scaleFrom, scaleTo, x, y, z;
    private String positionMode;
    private Button positionModeButton;
    private String status = "";

    TalentPresentationTransformScreen(Screen parent, TalentEditScreen owner,
            TalentDraft.AdvancedActionLocation location, int actionIndex) {
        super(Component.translatable("screen.talent_editor.presentation.transform.title"));
        this.parent = parent;
        this.owner = owner;
        this.location = location;
        this.actionIndex = actionIndex;
        draft = actionIndex < 0 ? new JsonObject() : owner.draft().advancedAction(location, -1, actionIndex);
        positionMode = value("positionMode", "return");
        if (!positionMode.equals("hold")) positionMode = "return";
    }

    @Override protected void init() {
        int w = Math.min(600, width - 32), left = (width - w) / 2;
        int half = (w - 8) / 2, third = (w - 16) / 3;
        boolean compact = height < 310;
        int first = compact ? 44 : 60, step = compact ? 34 : 40;
        atTick = field(left, first, half, value("atTick", "0"));
        duration = field(left + half + 8, first, half, value("durationTicks", "40"));
        alphaFrom = field(left, first + step, half, value("alphaFrom", "1"));
        alphaTo = field(left + half + 8, first + step, half, value("alphaTo", "1"));
        scaleFrom = field(left, first + step * 2, half, value("scaleFrom", "1"));
        scaleTo = field(left + half + 8, first + step * 2, half, value("scaleTo", "1"));
        x = field(left, first + step * 3, third, value("offsetX", "0"));
        y = field(left + third + 8, first + step * 3, third, value("offsetY", "0"));
        z = field(left + (third + 8) * 2, first + step * 3,
                w - (third + 8) * 2, value("offsetZ", "0"));
        positionModeButton = addRenderableWidget(Button.builder(Component.empty(), button -> {
            positionMode = positionMode.equals("return") ? "hold" : "return";
            updatePositionMode();
        }).bounds(left, first + step * 3 + 27, w, 20).build());
        updatePositionMode();
        int bottom = height - 28;
        addRenderableWidget(Button.builder(Component.translatable("gui.cancel"), button -> close())
                .bounds(left, bottom, 90, 20).build());
        addRenderableWidget(Button.builder(Component.translatable("screen.talent_editor.presentation.preview_full"),
                button -> preview()).bounds(left + 98, bottom, half - 98, 20).build());
        addRenderableWidget(Button.builder(Component.translatable("gui.done"), button -> save())
                .bounds(left + half + 8, bottom, half, 20).build());
    }

    private void updatePositionMode() {
        if (positionModeButton != null) positionModeButton.setMessage(Component.translatable(
                "screen.talent_editor.presentation.transform.position_mode." + positionMode));
    }

    private EditBox field(int x, int y, int w, String initial) {
        EditBox box = new EditBox(font, x, y, w, 20, Component.empty());
        box.setMaxLength(20);
        box.setValue(initial);
        box.setResponder(value -> status = "");
        addRenderableWidget(box);
        return box;
    }

    private JsonObject serialize() {
        JsonObject result = new JsonObject();
        result.addProperty("type", PresentationTransformAction.TYPE.toString());
        result.addProperty("atTick", integer(atTick, 0, 2400));
        result.addProperty("durationTicks", integer(duration, 2, 200));
        result.addProperty("alphaFrom", decimal(alphaFrom, 0, 1));
        result.addProperty("alphaTo", decimal(alphaTo, 0, 1));
        result.addProperty("scaleFrom", decimal(scaleFrom, 0.1, 4));
        result.addProperty("scaleTo", decimal(scaleTo, 0.1, 4));
        result.addProperty("offsetX", decimal(x, -3, 3));
        result.addProperty("offsetY", decimal(y, -3, 3));
        result.addProperty("offsetZ", decimal(z, -3, 3));
        result.addProperty("positionMode", positionMode);
        return result;
    }

    private static int integer(EditBox box, int min, int max) {
        int parsed = Integer.parseInt(box.getValue().strip());
        if (parsed < min || parsed > max) throw new IllegalArgumentException(min + ".." + max);
        return parsed;
    }

    private static double decimal(EditBox box, double min, double max) {
        double parsed = Double.parseDouble(box.getValue().strip());
        if (!Double.isFinite(parsed) || parsed < min || parsed > max)
            throw new IllegalArgumentException(min + ".." + max);
        return parsed;
    }

    private void save() {
        try {
            owner.draft().putAdvancedAction(location, -1, actionIndex, serialize());
            ClientEditorState.clearValidation();
            close();
        } catch (RuntimeException exception) { status = exception.getMessage(); }
    }

    private void preview() {
        if (minecraft == null || minecraft.level == null || minecraft.player == null) return;
        try {
            draft = serialize();
            TalentDraft preview = owner.draft().copy();
            preview.putAdvancedAction(location, -1, actionIndex, draft);
            minecraft.setScreen(new TalentCinematicPreviewScreen(this, preview,
                    location == TalentDraft.AdvancedActionLocation.ON_ACQUIRE
                            ? TalentDraft.PresentationPhase.ACQUIRE : TalentDraft.PresentationPhase.SYNTHESIZE));
        } catch (RuntimeException exception) { status = exception.getMessage(); }
    }

    @Override public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics);
        int w = Math.min(600, width - 32), left = (width - w) / 2;
        int half = (w - 8) / 2, third = (w - 16) / 3;
        boolean compact = height < 310;
        int first = compact ? 44 : 60, step = compact ? 34 : 40;
        EditorTheme.panel(graphics, left - 7, 25, w + 14, Math.min(height - 58, compact ? 205 : 235));
        super.render(graphics, mouseX, mouseY, partialTick);
        graphics.drawCenteredString(font, title, width / 2, 10, EditorTheme.TEXT);
        label(graphics, "at", left, first - 12);
        label(graphics, "duration", left + half + 8, first - 12);
        label(graphics, "alpha_from", left, first + step - 12);
        label(graphics, "alpha_to", left + half + 8, first + step - 12);
        label(graphics, "scale_from", left, first + step * 2 - 12);
        label(graphics, "scale_to", left + half + 8, first + step * 2 - 12);
        label(graphics, "x", left, first + step * 3 - 12);
        label(graphics, "y", left + third + 8, first + step * 3 - 12);
        label(graphics, "z", left + (third + 8) * 2, first + step * 3 - 12);
        graphics.drawString(font, Component.translatable("screen.talent_editor.presentation.transform.hint"),
                left, first + step * 3 + 51, EditorTheme.MUTED, false);
        if (!status.isBlank()) graphics.drawString(font, font.plainSubstrByWidth(status, w),
                left, height - 42, 0xFF7777, false);
    }

    private void label(GuiGraphics graphics, String key, int x, int y) {
        graphics.drawString(font, Component.translatable("screen.talent_editor.presentation.transform." + key),
                x, y, EditorTheme.MUTED, false);
    }

    private String value(String key, String fallback) {
        try { return draft.has(key) ? draft.get(key).getAsString() : fallback; }
        catch (RuntimeException ignored) { return fallback; }
    }

    private void close() { if (minecraft != null) minecraft.setScreen(parent); }
    @Override public void onClose() { close(); }
}
