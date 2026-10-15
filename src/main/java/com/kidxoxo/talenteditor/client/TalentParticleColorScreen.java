package com.kidxoxo.talenteditor.client;

import com.google.gson.JsonObject;
import com.kidxoxo.talenteditor.domain.ParticleTint;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.function.Consumer;

/** Optional RGB, hue, opacity, and relative size adjustment for simple registered particles. */
final class TalentParticleColorScreen extends EditorScreen {
    private final Screen parent;
    private final JsonObject draft;
    private final Consumer<JsonObject> apply;
    private EditBox from, to, hue, sizeFrom, sizeTo, alphaFrom, alphaTo;
    private String status = "";

    TalentParticleColorScreen(Screen parent, JsonObject draft, Consumer<JsonObject> apply) {
        super(Component.translatable("screen.talent_editor.particles.color.title"));
        this.parent = parent;
        this.draft = draft.deepCopy();
        this.apply = apply;
    }

    @Override protected void init() {
        int w = Math.min(500, width - 32), x = (width - w) / 2;
        int half = (w - 8) / 2, right = x + half + 8;
        from = field(x, 49, half, value("colorFrom", ""));
        to = field(x, 83, half, value("colorTo", ""));
        hue = field(x, 117, half, value("hueShiftDegrees", "0"));
        sizeFrom = field(right, 49, half, value("sizeFrom", "1"));
        sizeTo = field(right, 83, half, value("sizeTo", "1"));
        alphaFrom = field(right, 117, half, value("alphaFrom", "1"));
        alphaTo = field(right, 151, half, value("alphaTo", "1"));
        addRenderableWidget(EditorWidgets.button(Component.translatable("gui.cancel"), button -> close())
                .bounds(x, height - 28, 90, 20).build());
        addRenderableWidget(EditorWidgets.button(Component.translatable("gui.done"), button -> save())
                .bounds(x + 98, height - 28, w - 98, 20).build());
    }

    private EditBox field(int x, int y, int width, String initial) {
        EditBox box = new EditBox(font, x, y, width, 20, Component.empty());
        box.setMaxLength(16);
        box.setValue(initial);
        box.setResponder(value -> status = "");
        addRenderableWidget(box);
        return box;
    }

    private void save() {
        try {
            String start = from.getValue().strip(), end = to.getValue().strip();
            ParticleTint.parse(start);
            ParticleTint.parse(end);
            double degrees = Double.parseDouble(hue.getValue().strip());
            if (!Double.isFinite(degrees) || Math.abs(degrees) > 180)
                throw new IllegalArgumentException("-180..180");
            double startSize = parseSize(sizeFrom), endSize = parseSize(sizeTo);
            double startAlpha = parseAlpha(alphaFrom), endAlpha = parseAlpha(alphaTo);
            if (start.isEmpty()) draft.remove("colorFrom"); else draft.addProperty("colorFrom", start);
            if (end.isEmpty()) draft.remove("colorTo"); else draft.addProperty("colorTo", end);
            if (degrees == 0) draft.remove("hueShiftDegrees");
            else draft.addProperty("hueShiftDegrees", degrees);
            if (startSize == 1) draft.remove("sizeFrom");
            else draft.addProperty("sizeFrom", startSize);
            if (endSize == 1) draft.remove("sizeTo");
            else draft.addProperty("sizeTo", endSize);
            if (startAlpha == 1) draft.remove("alphaFrom");
            else draft.addProperty("alphaFrom", startAlpha);
            if (endAlpha == 1) draft.remove("alphaTo");
            else draft.addProperty("alphaTo", endAlpha);
            apply.accept(draft);
            close();
        } catch (RuntimeException exception) { status = exception.getMessage(); }
    }

    private static double parseSize(EditBox field) {
        double value = Double.parseDouble(field.getValue().strip());
        if (!Double.isFinite(value) || value < 0.1 || value > 4)
            throw new IllegalArgumentException("0.1..4");
        return value;
    }

    private static double parseAlpha(EditBox field) {
        double value = Double.parseDouble(field.getValue().strip());
        if (!Double.isFinite(value) || value < 0 || value > 1)
            throw new IllegalArgumentException("0..1");
        return value;
    }

    @Override public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics);
        int w = Math.min(500, width - 32), x = (width - w) / 2;
        int half = (w - 8) / 2, right = x + half + 8;
        EditorTheme.panel(graphics, x - 7, 25, w + 14, Math.min(205, height - 58));
        super.render(graphics, mouseX, mouseY, partialTick);
        EditorTheme.title(graphics,font,title,width);
        label(graphics, "from", x, 37);
        label(graphics, "to", x, 71);
        label(graphics, "hue", x, 105);
        label(graphics, "size_from", right, 37);
        label(graphics, "size_to", right, 71);
        label(graphics, "alpha_from", right, 105);
        label(graphics, "alpha_to", right, 139);
        int start = safeColor(from), end = safeColor(to);
        if (start >= 0) graphics.fill(x, 155, x + half / 2 - 3, 167, 0xFF000000 | start);
        if (end >= 0) graphics.fill(x + half / 2 + 3, 155, x + half, 167, 0xFF000000 | end);
        graphics.drawString(font, Component.translatable("screen.talent_editor.particles.color.hint"),
                x, 183, EditorTheme.MUTED, false);
        if (!status.isBlank()) graphics.drawString(font, status, x, height - 42, EditorTheme.ERROR, false);
    }

    private int safeColor(EditBox box) {
        try { return ParticleTint.parse(box.getValue().strip()); }
        catch (RuntimeException ignored) { return -1; }
    }

    private void label(GuiGraphics graphics, String key, int x, int y) {
        graphics.drawString(font, Component.translatable("screen.talent_editor.particles.color." + key),
                x, y, EditorTheme.MUTED, false);
    }

    private String value(String key, String fallback) {
        try { return draft.has(key) ? draft.get(key).getAsString() : fallback; }
        catch (RuntimeException ignored) { return fallback; }
    }

    private void close() { if (minecraft != null) minecraft.setScreen(parent); }
    @Override public void onClose() { close(); }
}
