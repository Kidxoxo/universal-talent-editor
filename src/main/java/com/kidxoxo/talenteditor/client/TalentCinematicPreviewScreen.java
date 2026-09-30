package com.kidxoxo.talenteditor.client;

import com.google.gson.JsonObject;
import com.kidxoxo.talenteditor.network.ParticleWavePacket;
import com.kidxoxo.talenteditor.domain.ParticleTint;
import com.kidxoxo.talenteditor.domain.PresentationTransformAction;
import net.minecraft.client.CameraType;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

/** Live-world, client-only preview of the complete acquisition or synthesis presentation. */
final class TalentCinematicPreviewScreen extends Screen {
    private final Screen parent;
    private final List<Cue> cues;
    private final List<SoundInstance> playing = new ArrayList<>();
    private final RandomSource random = RandomSource.create();
    private final int totalTicks;
    private final double cameraDistance;
    private final boolean defaultAcquireMessage;
    private final boolean defaultSynthesisTitle;
    private final Component talentName;
    private int age;
    private boolean paused;
    private CameraType originalCamera;
    private boolean cameraCaptured;
    private Button pauseButton, cameraButton;

    TalentCinematicPreviewScreen(Screen parent, TalentDraft draft, TalentDraft.PresentationPhase phase) {
        super(Component.translatable("screen.talent_editor.presentation.preview_full_title"));
        this.parent = parent;
        List<Cue> values = new ArrayList<>();
        collect(draft, TalentDraft.PresentationPhase.ACQUIRE, values);
        if (phase == TalentDraft.PresentationPhase.SYNTHESIZE) {
            collect(draft, TalentDraft.PresentationPhase.SYNTHESIZE, values);
        }
        values.sort(Comparator.comparingInt(Cue::atTick));
        cues = List.copyOf(values);
        defaultAcquireMessage = draft.presentationCues(TalentDraft.PresentationPhase.ACQUIRE).stream()
                .noneMatch(row -> row.type().equals("show_message"));
        defaultSynthesisTitle = phase == TalentDraft.PresentationPhase.SYNTHESIZE
                && draft.presentationCues(TalentDraft.PresentationPhase.SYNTHESIZE).stream()
                .noneMatch(row -> row.type().equals("show_message"));
        talentName = Component.literal(draft.name());
        int ending = Math.max(cues.stream().mapToInt(Cue::endTick).max().orElse(0),
                defaultAcquireMessage ? 180 : 0);
        ending = Math.max(ending, defaultSynthesisTitle ? 100 : 0);
        totalTicks = Math.max(60, Math.min(16_000, ending + 30));
        cameraDistance = cues.stream().filter(cue -> cue.type.equals("spawn_particles"))
                .mapToDouble(cue -> number(cue.json, "previewCameraDistance", 4))
                .max().orElse(4);
        if (!Double.isFinite(cameraDistance) || cameraDistance < 2 || cameraDistance > 16) {
            throw new IllegalArgumentException("Invalid preview camera distance");
        }
    }

    private static void collect(TalentDraft draft, TalentDraft.PresentationPhase phase, List<Cue> result) {
        TalentDraft.AdvancedActionLocation location = phase == TalentDraft.PresentationPhase.ACQUIRE
                ? TalentDraft.AdvancedActionLocation.ON_ACQUIRE
                : TalentDraft.AdvancedActionLocation.ON_SYNTHESIZE;
        for (TalentDraft.AdvancedActionRow row : draft.presentationCues(phase)) {
            JsonObject action = draft.advancedAction(location, -1, row.rawIndex());
            int tick = (int) number(action, "atTick", 0);
            if (tick < 0 || tick > 2400) throw new IllegalArgumentException("Invalid cue time");
            result.add(new Cue(row.type(), tick, action));
        }
    }

    @Override protected void init() {
        int w = Math.min(620, width - 24), x = (width - w) / 2, y = height - 30;
        int quarter = (w - 12) / 4;
        addRenderableWidget(Button.builder(Component.translatable("screen.talent_editor.particles.preview_replay"),
                button -> restart()).bounds(x, y, quarter, 20).build());
        pauseButton = addRenderableWidget(Button.builder(Component.empty(), button -> {
            paused = !paused; updateButtons();
        }).bounds(x + quarter + 4, y, quarter, 20).build());
        cameraButton = addRenderableWidget(Button.builder(Component.empty(), button -> {
            if (minecraft == null) return;
            minecraft.options.setCameraType(minecraft.options.getCameraType() == CameraType.THIRD_PERSON_BACK
                    ? CameraType.THIRD_PERSON_FRONT : CameraType.THIRD_PERSON_BACK);
            updateButtons();
        }).bounds(x + (quarter + 4) * 2, y, quarter, 20).build());
        addRenderableWidget(Button.builder(Component.translatable("gui.back"), button -> close())
                .bounds(x + (quarter + 4) * 3, y, w - (quarter + 4) * 3, 20).build());
        if (!cameraCaptured && minecraft != null) {
            originalCamera = minecraft.options.getCameraType();
            cameraCaptured = true;
            ParticlePreviewCameraDistance.activate(cameraDistance);
            minecraft.options.setCameraType(CameraType.THIRD_PERSON_BACK);
            restart();
        }
        updateButtons();
    }

    private void updateButtons() {
        if (pauseButton != null) pauseButton.setMessage(Component.translatable(paused
                ? "screen.talent_editor.particles.preview_resume"
                : "screen.talent_editor.particles.preview_pause"));
        if (cameraButton != null && minecraft != null) cameraButton.setMessage(Component.translatable(
                minecraft.options.getCameraType() == CameraType.THIRD_PERSON_FRONT
                        ? "screen.talent_editor.particles.preview_camera_front"
                        : "screen.talent_editor.particles.preview_camera_back"));
    }

    private void restart() {
        stopSounds();
        clearTransform();
        age = 0;
        paused = false;
        playAt(0);
        age = 1;
        updateButtons();
    }

    @Override public void tick() {
        if (paused || minecraft == null || minecraft.level == null || minecraft.player == null) return;
        if (age < totalTicks) playAt(age++);
        else restart();
    }

    private void playAt(int tick) {
        int particlesThisTick = 0;
        Cue transform = null;
        for (Cue cue : cues) {
            if (cue.type.equals("presentation_transform") && tick >= cue.atTick
                    && (tick - cue.atTick < (int) number(cue.json, "durationTicks", 40)
                    || string(cue.json, "positionMode", "return").equals("hold"))) transform = cue;
        }
        if (transform == null) clearTransform();
        else showTransform(transform, tick);
        for (Cue cue : cues) {
            if (cue.type.equals("play_sound") && cue.atTick == tick) playSound(cue.json);
            if (cue.type.equals("spawn_particles") && tick >= cue.atTick) {
                int elapsed = tick - cue.atTick;
                int duration = (int) number(cue.json, "durationTicks", 1);
                int interval = Math.max(1, (int) number(cue.json, "intervalTicks", 4));
                int count = (int) number(cue.json, "count", 8);
                if (elapsed < duration && elapsed % interval == 0
                        && particlesThisTick + count <= 128) {
                    emitParticles(cue.json, elapsed, duration);
                    particlesThisTick += count;
                }
            }
        }
    }

    private void showTransform(Cue cue, int tick) {
        if (minecraft == null || minecraft.player == null) return;
        int duration = (int) number(cue.json, "durationTicks", 40);
        boolean active = tick - cue.atTick < duration;
        double progress = Math.min(1, (double) (tick - cue.atTick) / Math.max(1, duration - 1));
        double alpha = active ? number(cue.json, "alphaFrom", 1)
                + (number(cue.json, "alphaTo", 1) - number(cue.json, "alphaFrom", 1)) * progress
                : 1;
        double scale = active ? number(cue.json, "scaleFrom", 1)
                + (number(cue.json, "scaleTo", 1) - number(cue.json, "scaleFrom", 1)) * progress
                : 1;
        double factor = PresentationTransformAction.positionFactor(
                string(cue.json, "positionMode", "return"), progress);
        ClientVisualState.setPresentation(minecraft.player.getUUID(), alpha, scale,
                number(cue.json, "offsetX", 0) * factor,
                number(cue.json, "offsetY", 0) * factor,
                number(cue.json, "offsetZ", 0) * factor);
    }

    private void clearTransform() {
        if (minecraft != null && minecraft.player != null)
            ClientVisualState.clearPresentation(minecraft.player.getUUID());
    }

    private void playSound(JsonObject cue) {
        if (minecraft == null) return;
        ResourceLocation id = ResourceLocation.tryParse(string(cue, "sound", ""));
        if (id == null || minecraft.getSoundManager().getSoundEvent(id) == null) return;
        SoundSource source;
        try { source = SoundSource.valueOf(string(cue, "source", "players").toUpperCase(Locale.ROOT)); }
        catch (IllegalArgumentException ignored) { source = SoundSource.PLAYERS; }
        SoundInstance sound = new SimpleSoundInstance(id, source,
                (float) number(cue, "volume", 1), (float) number(cue, "pitch", 1), RandomSource.create(),
                false, 0, SoundInstance.Attenuation.NONE, 0, 0, 0, true);
        playing.add(sound);
        minecraft.getSoundManager().play(sound);
    }

    private void emitParticles(JsonObject cue, int elapsed, int duration) {
        if (minecraft == null || minecraft.player == null) return;
        ClientVisualState.PresentationPose pose = ClientVisualState.presentation(minecraft.player.getUUID());
        double offsetX = pose == null ? 0 : pose.x();
        double offsetY = pose == null ? 0 : pose.y();
        double offsetZ = pose == null ? 0 : pose.z();
        ResourceLocation id = ResourceLocation.tryParse(string(cue, "particle", ""));
        if (id == null) return;
        double y = minecraft.player.getY() + offsetY + switch (string(cue, "anchor", "body")) {
            case "feet" -> 0.15;
            case "head" -> minecraft.player.getBbHeight() * 0.9;
            default -> minecraft.player.getBbHeight() * 0.52;
        };
        double progress = duration <= 1 ? 0 : (double) elapsed / (duration - 1);
        float alpha = (float) (number(cue, "alphaFrom", 1)
                + (number(cue, "alphaTo", 1) - number(cue, "alphaFrom", 1)) * progress);
        int color = ParticleTint.at(ParticleTint.parse(string(cue, "colorFrom", "")),
                ParticleTint.parse(string(cue, "colorTo", "")), progress);
        float size = (float) (number(cue, "sizeFrom", 1)
                + (number(cue, "sizeTo", 1) - number(cue, "sizeFrom", 1)) * progress);
        ParticleWavePlayback.accept(new ParticleWavePacket(id, string(cue, "motion", "burst"),
                minecraft.player.getX() + offsetX, y, minecraft.player.getZ() + offsetZ,
                (int) number(cue, "count", 8), number(cue, "radius", 0.7), number(cue, "speed", 0.03),
                alpha, elapsed, random.nextLong(), color, (float) number(cue, "hueShiftDegrees", 0), size));
    }

    @Override public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        graphics.fill(0, 0, width, 45, 0x980B1721);
        graphics.fill(0, height - 40, width, height, 0x980B1721);
        super.render(graphics, mouseX, mouseY, partialTick);
        graphics.drawCenteredString(font, title, width / 2, 6, EditorTheme.TEXT);
        graphics.drawCenteredString(font, Component.translatable("screen.talent_editor.presentation.preview_clock",
                Math.min(age, totalTicks), totalTicks, cues.size()), width / 2, 20, EditorTheme.MUTED);
        int progressWidth = Math.min(360, width - 40), progressX = (width - progressWidth) / 2;
        graphics.fill(progressX, 37, progressX + progressWidth, 40, 0xFF364652);
        graphics.fill(progressX, 37, progressX + progressWidth * Math.min(age, totalTicks) / totalTicks,
                40, EditorTheme.ACCENT);
        renderMessages(graphics);
    }

    private void renderMessages(GuiGraphics graphics) {
        Cue title = null, actionBar = null;
        List<Cue> chats = new ArrayList<>();
        for (Cue cue : cues) {
            if (!cue.type.equals("show_message") || cue.atTick >= age) continue;
            String mode = string(cue.json, "mode", "chat");
            if (mode.equals("title")) title = cue;
            else if (mode.equals("action_bar")) actionBar = cue;
            else if (age - cue.atTick < 180) chats.add(cue);
        }
        if (title != null) {
            int elapsed = age - title.atTick;
            int fadeIn = (int) number(title.json, "fadeInTicks", 10);
            int stay = (int) number(title.json, "stayTicks", 70);
            int fadeOut = (int) number(title.json, "fadeOutTicks", 20);
            int total = fadeIn + stay + fadeOut;
            if (elapsed < total) {
                double fade = fadeIn > 0 && elapsed < fadeIn ? (double) elapsed / fadeIn
                        : fadeOut > 0 && elapsed > fadeIn + stay
                        ? (double) (total - elapsed) / fadeOut : 1;
                int alpha = Math.max(4, Math.min(255, (int) (fade * 255)));
                Component text = LocalizedTextDraft.parse(title.json.get("text")).component();
                graphics.drawCenteredString(font, text, width / 2, height / 2 - 12,
                        alpha << 24 | 0xFFFFFF);
                if (title.json.has("subtitle")) graphics.drawCenteredString(font,
                        LocalizedTextDraft.parse(title.json.get("subtitle")).component(),
                        width / 2, height / 2 + 9, alpha << 24 | 0xFFFFFF);
            }
        }
        if (title == null && defaultSynthesisTitle && age < 100) {
            double fade = age < 10 ? age / 10.0 : age > 80 ? (100 - age) / 20.0 : 1;
            int color = Math.max(4, Math.min(255, (int) (fade * 255))) << 24 | 0xFFFFFF;
            graphics.drawCenteredString(font, Component.translatable("message.talent_editor.synthesis_complete"),
                    width / 2, height / 2 - 12, color);
            graphics.drawCenteredString(font, talentName, width / 2, height / 2 + 9, color);
        }
        if (actionBar != null && age - actionBar.atTick < 60) graphics.drawCenteredString(font,
                LocalizedTextDraft.parse(actionBar.json.get("text")).component(),
                width / 2, height - 62, 0xFFFFFFFF);
        int shown = Math.min(3, chats.size()), start = chats.size() - shown;
        for (int i = 0; i < shown; i++) {
            Cue chat = chats.get(start + i);
            Component message = LocalizedTextDraft.parse(chat.json.get("text")).component();
            int y = height - 108 - (shown - i - 1) * 12;
            int textWidth = Math.min(font.width(message) + 10, width - 28);
            graphics.fill(8, y - 2, 8 + textWidth, y + 11, 0x90000000);
            graphics.drawString(font, message, 13, y, 0xFFFFFFFF, false);
        }
        if (defaultAcquireMessage && age < 180) {
            Component message = Component.translatable("message.talent_editor.talent_acquired", talentName);
            int y = height - 120 - shown * 12;
            graphics.fill(8, y - 2, 8 + Math.min(font.width(message) + 10, width - 28), y + 11, 0x90000000);
            graphics.drawString(font, message, 13, y, 0xFFFFFFFF, false);
        }
    }

    private void stopSounds() {
        if (minecraft != null) for (SoundInstance sound : playing) minecraft.getSoundManager().stop(sound);
        playing.clear();
    }

    @Override public void removed() {
        stopSounds();
        clearTransform();
        ParticlePreviewCameraDistance.deactivate();
        if (cameraCaptured && minecraft != null) minecraft.options.setCameraType(originalCamera);
        cameraCaptured = false;
    }

    @Override public boolean isPauseScreen() { return false; }
    private void close() { if (minecraft != null) minecraft.setScreen(parent); }
    @Override public void onClose() { close(); }

    private static String string(JsonObject json, String key, String fallback) {
        try { return json.has(key) ? json.get(key).getAsString() : fallback; }
        catch (RuntimeException ignored) { return fallback; }
    }

    private static double number(JsonObject json, String key, double fallback) {
        try { return json.has(key) ? json.get(key).getAsDouble() : fallback; }
        catch (RuntimeException ignored) { return fallback; }
    }

    private record Cue(String type, int atTick, JsonObject json) {
        int endTick() {
            return atTick + switch (type) {
                case "spawn_particles" -> (int) number(json, "durationTicks", 1);
                case "presentation_transform" -> (int) number(json, "durationTicks", 40);
                case "show_message" -> string(json, "mode", "chat").equals("title")
                        ? (int) (number(json, "fadeInTicks", 10)
                        + number(json, "stayTicks", 70) + number(json, "fadeOutTicks", 20)) : 180;
                default -> 20;
            };
        }
    }
}
