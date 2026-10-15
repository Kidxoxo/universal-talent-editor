package com.kidxoxo.talenteditor.client;

import com.google.gson.JsonObject;
import com.kidxoxo.talenteditor.network.ParticleWavePacket;
import com.kidxoxo.talenteditor.domain.ParticleTint;
import com.kidxoxo.talenteditor.domain.PresentationChatTiming;
import net.minecraft.util.FormattedCharSequence;
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
final class TalentCinematicPreviewScreen extends EditorScreen {
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
                defaultAcquireMessage ? 200 : 0);
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
        addRenderableWidget(EditorWidgets.button(Component.translatable("screen.talent_editor.particles.preview_replay"),
                button -> restart()).bounds(x, y, quarter, 20).build());
        pauseButton = addRenderableWidget(EditorWidgets.button(Component.empty(), button -> {
            paused = !paused; updateButtons();
        }).bounds(x + quarter + 4, y, quarter, 20).build());
        cameraButton = addRenderableWidget(EditorWidgets.button(Component.empty(), button -> {
            if (minecraft == null) return;
            minecraft.options.setCameraType(minecraft.options.getCameraType() == CameraType.THIRD_PERSON_BACK
                    ? CameraType.THIRD_PERSON_FRONT : CameraType.THIRD_PERSON_BACK);
            updateButtons();
        }).bounds(x + (quarter + 4) * 2, y, quarter, 20).build());
        addRenderableWidget(EditorWidgets.button(Component.translatable("gui.back"), button -> close())
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
        else if (age < 16_000 && playing.stream().anyMatch(minecraft.getSoundManager()::isActive)) age++;
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
        EditorTheme.title(graphics,font,title,width);
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
            else if (age - cue.atTick < chatDuration(cue.json)) chats.add(cue);
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
                List<FormattedCharSequence> lines = new ArrayList<>(font.split(text, Math.max(1, width - 40)));
                if (title.json.has("subtitle")) {
                    lines.add(FormattedCharSequence.EMPTY);
                    lines.addAll(font.split(LocalizedTextDraft.parse(title.json.get("subtitle")).component(),
                            Math.max(1, width - 40)));
                }
                int lineHeight = font.lineHeight + 3;
                int maxLines = Math.max(1, (height - 120) / lineHeight);
                if (lines.size() > maxLines) lines = lines.subList(0, maxLines);
                int top = height / 2 - lines.size() * lineHeight / 2;
                for (int i = 0; i < lines.size(); i++) graphics.drawCenteredString(font, lines.get(i),
                        width / 2, top + i * lineHeight, alpha << 24 | 0xFFFFFF);
            }
        }
        if (title == null && defaultSynthesisTitle && age < 100) {
            double fade = age < 10 ? age / 10.0 : age > 80 ? (100 - age) / 20.0 : 1;
            int color = Math.max(4, Math.min(255, (int) (fade * 255))) << 24 | 0xFFFFFF;
            graphics.drawCenteredString(font, Component.translatable("message.talent_editor.synthesis_complete"),
                    width / 2, height / 2 - 12, color);
            graphics.drawCenteredString(font, talentName, width / 2, height / 2 + 9, color);
        }
        if (actionBar != null && age - actionBar.atTick < 60) {
            var lines = font.split(LocalizedTextDraft.parse(actionBar.json.get("text")).component(),
                    Math.max(1, width - 40));
            int shown = Math.min(3, lines.size());
            for (int i = 0; i < shown; i++) graphics.drawCenteredString(font, lines.get(i), width / 2,
                    height - 62 - (shown - i - 1) * (font.lineHeight + 2), 0xFFFFFFFF);
        }
        List<ChatLine> lines = new ArrayList<>();
        int chatWidth = Math.max(1, Math.min(420, width - 32));
        if (defaultAcquireMessage && age < 200) addChatLines(lines,
                Component.translatable("message.talent_editor.talent_acquired", talentName),
                chatWidth, PresentationChatTiming.opacity(age, 200));
        for (Cue chat : chats) addChatLines(lines,
                LocalizedTextDraft.parse(chat.json.get("text")).component(), chatWidth,
                PresentationChatTiming.opacity(age - chat.atTick, chatDuration(chat.json)));
        int lineHeight = font.lineHeight + 3;
        int maxLines = Math.min(10, Math.max(1, (height - 150) / lineHeight));
        int shown = Math.min(maxLines, lines.size()), start = lines.size() - shown;
        for (int i = 0; i < shown; i++) {
            ChatLine line = lines.get(start + i);
            int alpha = (int) (line.opacity * 255);
            if (alpha < 4) continue;
            int y = height - 96 - (shown - i - 1) * lineHeight;
            graphics.fill(8, y - 2, 18 + font.width(line.text), y + font.lineHeight + 2,
                    (int) (line.opacity * 144) << 24);
            graphics.drawString(font, line.text, 13, y, alpha << 24 | 0xFFFFFF, false);
        }
    }

    private void addChatLines(List<ChatLine> output, Component text, int width, double opacity) {
        for (FormattedCharSequence line : font.split(text, width)) output.add(new ChatLine(line, opacity));
    }

    private static int chatDuration(JsonObject json) {
        return PresentationChatTiming.visibleTicks((int) number(json, "stayTicks", 200));
    }

    private record ChatLine(FormattedCharSequence text, double opacity) {}

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
                        + number(json, "stayTicks", 70) + number(json, "fadeOutTicks", 20))
                        : string(json, "mode", "chat").equals("action_bar") ? 60 : chatDuration(json);
                default -> 20;
            };
        }
    }
}
