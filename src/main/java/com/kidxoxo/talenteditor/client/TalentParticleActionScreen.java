package com.kidxoxo.talenteditor.client;

import com.google.gson.JsonObject;
import com.kidxoxo.talenteditor.TalentEditorMod;
import com.kidxoxo.talenteditor.domain.ParticleTint;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.List;

/** Visual editor for bounded server particle animations. */
final class TalentParticleActionScreen extends EditorScreen {
    private final Screen parent;
    private final TalentEditScreen owner;
    private final TalentDraft.AdvancedActionLocation location;
    private final int triggerIndex;
    private final int actionIndex;
    private JsonObject draft;
    private final EditorOptionPopup options = new EditorOptionPopup();
    private EditBox particle, count, radius, speed, duration, interval, distance, cooldown, previewDistance;
    private EditBox atTick;
    private String anchor, motion, status = "";

    TalentParticleActionScreen(Screen parent, TalentEditScreen owner,
            TalentDraft.AdvancedActionLocation location, int triggerIndex, int actionIndex) {
        super(Component.translatable("screen.talent_editor.particles.title"));
        this.parent = parent;
        this.owner = owner;
        this.location = location;
        this.triggerIndex = triggerIndex;
        this.actionIndex = actionIndex;
        draft = actionIndex < 0 ? new JsonObject() : owner.draft().advancedAction(location, triggerIndex, actionIndex);
        anchor = value("anchor", "body");
        motion = value("motion", "burst");
    }

    @Override protected void init() {
        int w = Math.min(690, width - 32), x = (width - w) / 2, half = (w - 8) / 2;
        boolean presentation = presentationLocation();
        int third = (w - 16) / 3;
        boolean compact = height < 310;
        int quarter = (w - 24) / 4;
        int particleY = compact ? 43 : 52;
        particle = field(x, particleY, w - 112, value("particle", "minecraft:end_rod"));
        addRenderableWidget(EditorWidgets.button(Component.translatable("screen.talent_editor.particles.search"),
                b -> openPicker()).bounds(x + w - 106, particleY, 106, 20).build());
        if (compact) {
            count = field(x, 78, quarter, value("count", "8"));
            radius = field(x + quarter + 8, 78, quarter, value("radius", "0.7"));
            speed = field(x + (quarter + 8) * 2, 78, quarter, value("speed", "0.03"));
            duration = field(x + (quarter + 8) * 3, 78, w - (quarter + 8) * 3,
                    value("durationTicks", "1"));
            interval = field(x, 113, quarter, value("intervalTicks", "4"));
            distance = field(x + quarter + 8, 113, quarter, value("viewDistance", "24"));
            cooldown = field(x + (quarter + 8) * 2, 113, quarter, value("cooldownTicks", "10"));
            previewDistance = field(x + (quarter + 8) * 3, 113, w - (quarter + 8) * 3,
                    value("previewCameraDistance", "4"));
            if (presentation) {
                atTick = field(x, 148, half, value("atTick", "0"));
            }
        } else {
            count = field(x, 92, half, value("count", "8"));
            radius = field(x + half + 8, 92, half, value("radius", "0.7"));
            speed = field(x, 132, presentation ? third : half, value("speed", "0.03"));
            duration = field(x + (presentation ? third : half) + 8, 132,
                    presentation ? third : half, value("durationTicks", "1"));
            if (presentation) atTick = field(x + (third + 8) * 2, 132,
                    w - (third + 8) * 2, value("atTick", "0"));
            interval = field(x, 172, third, value("intervalTicks", "4"));
            distance = field(x + third + 8, 172, third, value("viewDistance", "24"));
            cooldown = field(x, 212, half, value("cooldownTicks", "10"));
            previewDistance = field(x + half + 8, 212, half, value("previewCameraDistance", "4"));
        }
        addRenderableWidget(EditorWidgets.button(Component.empty(), b -> {
            List<String> choices = List.of("feet", "body", "head");
            options.open(Component.translatable("screen.talent_editor.option.choose"),
                    choices.stream().map(value -> choice("anchor", value)).toList(),
                    Math.max(0, choices.indexOf(anchor)), index -> {
                        anchor = choices.get(index);
                        b.setMessage(choice("anchor", anchor));
                    }, height);
        }).bounds(compact ? (presentation ? x + half + 8 : x) : x + (third + 8) * 2,
                compact ? 148 : 172,
                compact ? (presentation ? half : w) : w - (third + 8) * 2, 20).build())
                .setMessage(choice("anchor", anchor));
        addRenderableWidget(EditorWidgets.button(Component.empty(), b -> {
            List<String> choices = List.of("burst", "inward", "outward", "spiral", "ring", "fountain");
            options.open(Component.translatable("screen.talent_editor.option.choose"),
                    choices.stream().map(value -> choice("motion", value)).toList(),
                    Math.max(0, choices.indexOf(motion)), index -> {
                        motion = choices.get(index);
                        b.setMessage(choice("motion", motion));
                    }, height);
        }).bounds(x, compact ? 183 : 252, half, 20).build())
                .setMessage(choice("motion", motion));
        addRenderableWidget(EditorWidgets.button(Component.translatable("screen.talent_editor.particles.preview"),
                b -> preview()).bounds(x + half + 8, compact ? 183 : 252, half, 20).build());
        addRenderableWidget(EditorWidgets.button(Component.translatable("gui.cancel"), b -> close())
                .bounds(x, height - 28, 90, 20).build());
        addRenderableWidget(EditorWidgets.button(Component.translatable("screen.talent_editor.particles.color.open"),
                b -> openColor()).bounds(x + 98, height - 28, 110, 20).build());
        addRenderableWidget(EditorWidgets.button(Component.translatable("gui.done"), b -> save())
                .bounds(x + 216, height - 28, w - 216, 20).build());
    }

    private EditBox field(int x, int y, int width, String initial) {
        EditBox box = new EditBox(font, x, y, width, 20, Component.empty());
        box.setMaxLength(160);
        box.setValue(initial);
        box.setResponder(s -> status = "");
        addRenderableWidget(box);
        return box;
    }

    private Component choice(String field, String value) {
        return Component.translatable("screen.talent_editor.particles." + field + "." + value);
    }

    private void openPicker() {
        try { draft = serialize(false); } catch (RuntimeException ignored) {}
        if (minecraft != null) minecraft.setScreen(new ParticleTypePickerScreen(
                this, ResourceLocation.tryParse(particle.getValue().strip()), id -> {
            draft.addProperty("particle", id.toString());
            particle.setValue(id.toString());
        }));
    }

    private void save() {
        try {
            owner.draft().putAdvancedAction(location, triggerIndex, actionIndex, serialize(true));
            ClientEditorState.clearValidation();
            close();
        } catch (RuntimeException e) {
            status = e.getMessage() == null ? "Invalid particle settings" : e.getMessage();
        }
    }

    private void openColor() {
        try { draft = serialize(false); } catch (RuntimeException ignored) {}
        if (minecraft != null) minecraft.setScreen(new TalentParticleColorScreen(this, draft,
                colored -> draft = colored));
    }

    private JsonObject serialize(boolean strict) {
        JsonObject result = new JsonObject();
        result.addProperty("type", TalentEditorMod.MOD_ID + ":spawn_particles");
        String id = particle.getValue().strip();
        ResourceLocation parsed = ResourceLocation.tryParse(id);
        if (strict && (!id.contains(":") || parsed == null || !(ForgeRegistries.PARTICLE_TYPES.getValue(parsed)
                instanceof SimpleParticleType))) throw new IllegalArgumentException(Component.translatable(
                "screen.talent_editor.particles.invalid_particle").getString());
        result.addProperty("particle", id);
        result.addProperty("anchor", anchor);
        result.addProperty("motion", motion);
        int countValue = integer(count, 1, 32);
        double radiusValue = decimal(radius, 0, 4), speedValue = decimal(speed, 0, 0.3);
        int durationValue = integer(duration, 1, 60), intervalValue = integer(interval, 1, 20);
        double distanceValue = decimal(distance, 4, 64);
        double previewDistanceValue = decimal(previewDistance, 2, 16);
        int cooldownValue = integer(cooldown, 0, 1200);
        if (strict && countValue * (1 + (durationValue - 1) / intervalValue) > 256)
            throw new IllegalArgumentException(Component.translatable("screen.talent_editor.particles.budget").getString());
        result.addProperty("count", countValue);
        result.addProperty("radius", radiusValue);
        result.addProperty("speed", speedValue);
        result.addProperty("durationTicks", durationValue);
        result.addProperty("intervalTicks", intervalValue);
        result.addProperty("viewDistance", distanceValue);
        result.addProperty("previewCameraDistance", previewDistanceValue);
        if (presentationLocation()) {
            result.addProperty("atTick", integer(atTick, 0, 2400));
        }
        for (String key : List.of("alphaFrom", "alphaTo")) {
            if (!draft.has(key)) continue;
            double alpha = draft.get(key).getAsDouble();
            if (strict && (!Double.isFinite(alpha) || alpha < 0 || alpha > 1))
                throw new IllegalArgumentException("0..1");
            result.addProperty(key, alpha);
        }
        for (String key : List.of("colorFrom", "colorTo")) {
            if (!draft.has(key)) continue;
            String raw = draft.get(key).getAsString().strip();
            if (strict) ParticleTint.parse(raw);
            if (!raw.isEmpty()) result.addProperty(key, raw);
        }
        if (draft.has("hueShiftDegrees")) {
            double hue = draft.get("hueShiftDegrees").getAsDouble();
            if (strict && (!Double.isFinite(hue) || Math.abs(hue) > 180))
                throw new IllegalArgumentException("-180..180");
            result.addProperty("hueShiftDegrees", hue);
        }
        for (String key : List.of("sizeFrom", "sizeTo")) {
            if (!draft.has(key)) continue;
            double size = draft.get(key).getAsDouble();
            if (strict && (!Double.isFinite(size) || size < 0.1 || size > 4))
                throw new IllegalArgumentException("0.1..4");
            result.addProperty(key, size);
        }
        result.addProperty("cooldownTicks", cooldownValue);
        if (draft.has("enabledWhen")) result.add("enabledWhen", draft.get("enabledWhen").deepCopy());
        if (draft.has("disabledWhen")) result.add("disabledWhen", draft.get("disabledWhen").deepCopy());
        return result;
    }

    private int integer(EditBox box, int min, int max) {
        int value = Integer.parseInt(box.getValue().strip());
        if (value < min || value > max) throw new IllegalArgumentException(min + ".." + max);
        return value;
    }

    private double decimal(EditBox box, double min, double max) {
        double value = Double.parseDouble(box.getValue().strip());
        if (!Double.isFinite(value) || value < min || value > max)
            throw new IllegalArgumentException(min + ".." + max);
        return value;
    }

    private void preview() {
        if (minecraft == null || minecraft.level == null || minecraft.player == null) return;
        try {
            // Keep unsaved form values when returning from the preview page.
            draft = serialize(true);
            minecraft.setScreen(new TalentParticlePreviewScreen(this, draft));
        } catch (RuntimeException e) {
            status = e.getMessage() == null ? "Invalid particle settings" : e.getMessage();
        }
    }

    @Override public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics);
        int w = Math.min(690, width - 32), x = (width - w) / 2, half = (w - 8) / 2;
        EditorTheme.panel(graphics, x - 7, 24, w + 14, Math.min(270, height - 54));
        super.render(graphics, mouseX, mouseY, partialTick);
        EditorTheme.title(graphics,font,title,width);
        if (height < 310) {
            int quarter = (w - 24) / 4;
            label(graphics, "particle", x, 31);
            label(graphics, "count_short", x, 66);
            label(graphics, "radius_short", x + quarter + 8, 66);
            label(graphics, "speed_short", x + (quarter + 8) * 2, 66);
            label(graphics, "duration_short", x + (quarter + 8) * 3, 66);
            label(graphics, "interval_short", x, 101);
            label(graphics, "distance_short", x + quarter + 8, 101);
            label(graphics, "cooldown_short", x + (quarter + 8) * 2, 101);
            label(graphics, "preview_distance_short", x + (quarter + 8) * 3, 101);
            if (presentationLocation()) {
                label(graphics, "at_tick_short", x, 136);
            }
            if (!status.isBlank()) graphics.drawString(font, font.plainSubstrByWidth(status, w),
                    x, height - 37, EditorTheme.ERROR, false);
            options.render(graphics, font, width, height);
            return;
        }
        label(graphics, "particle", x, 40); label(graphics, "count", x, 80);
        boolean presentation = presentationLocation();
        int third = (w - 16) / 3;
        label(graphics, "radius", x + half + 8, 80); label(graphics, "speed", x, 120);
        label(graphics, "duration", x + (presentation ? third : half) + 8, 120);
        label(graphics, "interval", x, 160);
        label(graphics, "distance", x + third + 8, 160);
        label(graphics, "cooldown", x, 200);
        label(graphics, "preview_distance", x + half + 8, 200);
        if (presentation) {
            label(graphics, "at_tick", x + (third + 8) * 2, 120);
        }
        if (!status.isBlank()) graphics.drawString(font, font.plainSubstrByWidth(status, w),
                x, height - 42, EditorTheme.ERROR, false);
        options.render(graphics, font, width, height);
    }

    @Override public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (options.mouseClicked(mouseX, mouseY, button, width, height)) return true;
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        if (options.mouseScrolled(delta, height)) return true;
        return super.mouseScrolled(mouseX, mouseY, delta);
    }

    @Override public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (options.keyPressed(keyCode, height)) return true;
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override public boolean charTyped(char codePoint, int modifiers) {
        return options.isOpen() || super.charTyped(codePoint, modifiers);
    }

    private void label(GuiGraphics graphics, String key, int x, int y) {
        graphics.drawString(font, Component.translatable("screen.talent_editor.particles." + key),
                x, y, EditorTheme.MUTED, false);
    }

    private String value(String key, String fallback) {
        try { return draft.has(key) ? draft.get(key).getAsString() : fallback; }
        catch (RuntimeException e) { return fallback; }
    }

    private boolean presentationLocation() {
        return location == TalentDraft.AdvancedActionLocation.ON_ACQUIRE
                || location == TalentDraft.AdvancedActionLocation.ON_SYNTHESIZE;
    }

    private void close() { if (minecraft != null) minecraft.setScreen(parent); }
    @Override public void onClose() { close(); }
}
