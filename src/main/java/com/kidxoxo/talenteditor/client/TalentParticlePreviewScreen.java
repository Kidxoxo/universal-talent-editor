package com.kidxoxo.talenteditor.client;

import com.google.gson.JsonObject;
import com.kidxoxo.talenteditor.network.ParticleWavePacket;
import com.kidxoxo.talenteditor.domain.ParticleTint;
import net.minecraft.client.CameraType;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.List;
import java.util.Set;

/** A client-only, live-world scene for checking an unsaved particle action. */
final class TalentParticlePreviewScreen extends EditorScreen {
    private static final int REPLAY_GAP_TICKS = 20;

    private final Screen parent;
    private final List<ParticleLayer> layers;
    private final ParticlePreviewTimeline timeline;
    private final double cameraDistance;
    private final RandomSource random = RandomSource.create();

    private CameraType originalCamera;
    private boolean cameraCaptured;
    private boolean paused;
    private int age;
    private int replayGap;
    private Button pauseButton;
    private Button cameraButton;

    TalentParticlePreviewScreen(Screen parent, JsonObject action) {
        this(parent, List.of(action));
    }

    TalentParticlePreviewScreen(Screen parent, List<JsonObject> actions) {
        super(Component.translatable("screen.talent_editor.particles.preview_title"));
        this.parent = parent;
        layers = actions.stream().map(ParticleLayer::parse).toList();
        cameraDistance = layers.stream().mapToDouble(ParticleLayer::previewCameraDistance).max().orElse(4);
        timeline = new ParticlePreviewTimeline(layers.stream().map(layer ->
                new ParticlePreviewTimeline.Layer(layer.count, layer.durationTicks, layer.intervalTicks)
        ).toList());
    }

    @Override
    protected void init() {
        int barWidth = Math.min(620, width - 24);
        int buttonWidth = (barWidth - 12) / 4;
        int x = (width - barWidth) / 2;
        int y = height - 31;
        addRenderableWidget(EditorWidgets.button(Component.translatable("screen.talent_editor.particles.preview_replay"),
                button -> restart()).bounds(x, y, buttonWidth, 20).build());
        pauseButton = addRenderableWidget(EditorWidgets.button(Component.empty(), button -> {
            paused = !paused;
            updateButtons();
        }).bounds(x + buttonWidth + 4, y, buttonWidth, 20).build());
        cameraButton = addRenderableWidget(EditorWidgets.button(Component.empty(), button -> {
            if (minecraft == null) return;
            CameraType current = minecraft.options.getCameraType();
            minecraft.options.setCameraType(current == CameraType.THIRD_PERSON_BACK
                    ? CameraType.THIRD_PERSON_FRONT : CameraType.THIRD_PERSON_BACK);
            updateButtons();
        }).bounds(x + (buttonWidth + 4) * 2, y, buttonWidth, 20).build());
        addRenderableWidget(EditorWidgets.button(Component.translatable("gui.back"), button -> returnParent())
                .bounds(x + (buttonWidth + 4) * 3, y,
                        barWidth - (buttonWidth + 4) * 3, 20).build());

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
        age = 0;
        replayGap = 0;
        paused = false;
        emitAt(0);
        age = 1;
        updateButtons();
    }

    @Override
    public void tick() {
        if (paused || minecraft == null || minecraft.player == null || minecraft.level == null) return;
        if (age < timeline.durationTicks()) {
            emitAt(age);
            age++;
        } else if (++replayGap >= REPLAY_GAP_TICKS) {
            restart();
        }
    }

    private void emitAt(int tick) {
        if (minecraft == null || minecraft.player == null || minecraft.level == null) return;
        for (int index = 0; index < layers.size(); index++) {
            if (timeline.emitsAt(index, tick)) emitWave(layers.get(index), tick);
        }
    }

    private void emitWave(ParticleLayer layer, int tick) {
        double x = minecraft.player.getX();
        double y = minecraft.player.getY() + switch (layer.anchor) {
            case "feet" -> 0.15;
            case "head" -> minecraft.player.getBbHeight() * 0.9;
            default -> minecraft.player.getBbHeight() * 0.52;
        };
        double z = minecraft.player.getZ();
        if (layer.alphaFrom != 1 || layer.alphaTo != 1 || layer.colorFrom >= 0 || layer.colorTo >= 0
                || layer.hueShiftDegrees != 0 || layer.sizeFrom != 1 || layer.sizeTo != 1
                || Set.of("spiral", "ring", "fountain").contains(layer.motion)) {
            float progress = layer.durationTicks <= 1 ? 0 : (float) tick / (layer.durationTicks - 1);
            float alpha = (float) (layer.alphaFrom + (layer.alphaTo - layer.alphaFrom) * progress);
            int color = ParticleTint.at(layer.colorFrom, layer.colorTo, progress);
            float size = (float) (layer.sizeFrom + (layer.sizeTo - layer.sizeFrom) * progress);
            ParticleWavePlayback.accept(new ParticleWavePacket(layer.particleId, layer.motion,
                    x, y, z, layer.count, layer.radius, layer.speed, alpha, tick, random.nextLong(),
                    color, layer.hueShiftDegrees, size));
            return;
        }
        for (int i = 0; i < layer.count; i++) {
            if (layer.motion.equals("burst")) {
                minecraft.level.addParticle(layer.particle,
                        x + random.nextGaussian() * layer.radius,
                        y + random.nextGaussian() * layer.radius * 0.7,
                        z + random.nextGaussian() * layer.radius,
                        random.nextGaussian() * layer.speed,
                        random.nextGaussian() * layer.speed,
                        random.nextGaussian() * layer.speed);
                continue;
            }
            double angle = random.nextDouble() * Math.PI * 2;
            double height = (random.nextDouble() - 0.5) * layer.radius;
            double distance = layer.radius * (0.5 + random.nextDouble() * 0.5);
            double dx = Math.cos(angle), dz = Math.sin(angle);
            boolean inward = layer.motion.equals("inward");
            double velocity = layer.speed * (inward ? -1 : 1);
            minecraft.level.addParticle(layer.particle,
                    x + (inward ? dx * distance : dx * 0.15), y + height,
                    z + (inward ? dz * distance : dz * 0.15),
                    dx * velocity, height / Math.max(layer.radius, 0.1) * velocity, dz * velocity);
        }
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        // The live world remains visible behind this small control overlay.
        graphics.fill(0, 0, width, 58, 0x980B1721);
        graphics.fill(0, height - 42, width, height, 0x980B1721);
        super.render(graphics, mouseX, mouseY, partialTick);
        EditorTheme.title(graphics,font,title,width);
        String details = layers.size() == 1
                ? Component.translatable("screen.talent_editor.particles.preview_details",
                        layers.get(0).particleId, layers.get(0).count,
                        Component.translatable("screen.talent_editor.particles.motion."
                                + layers.get(0).motion)).getString()
                : Component.translatable("screen.talent_editor.particles.preview_group_details",
                        layers.size(), timeline.firstWaveCount(), timeline.durationTicks()).getString();
        graphics.drawCenteredString(font, font.plainSubstrByWidth(details, width - 24),
                width / 2, 22, EditorTheme.MUTED);
        String hint = Component.translatable("screen.talent_editor.particles.preview_hint").getString();
        graphics.drawCenteredString(font, font.plainSubstrByWidth(hint, width - 24),
                width / 2, 34, EditorTheme.MUTED);
        int progressWidth = Math.min(360, width - 40);
        int progressX = (width - progressWidth) / 2;
        graphics.fill(progressX, 50, progressX + progressWidth, 53, 0xFF364652);
        graphics.fill(progressX, 50,
                progressX + progressWidth * Math.min(age, timeline.durationTicks()) / timeline.durationTicks(),
                53, EditorTheme.ACCENT);
    }

    @Override public boolean isPauseScreen() { return false; }

    @Override
    public void removed() {
        ParticlePreviewCameraDistance.deactivate();
        if (cameraCaptured && minecraft != null) minecraft.options.setCameraType(originalCamera);
        cameraCaptured = false;
    }

    private void returnParent() {
        if (minecraft != null) minecraft.setScreen(parent);
    }

    @Override public void onClose() { returnParent(); }

    private record ParticleLayer(ResourceLocation particleId, SimpleParticleType particle,
                                 String anchor, String motion, int count, double radius,
                                 double speed, int durationTicks, int intervalTicks,
                                 double previewCameraDistance, double alphaFrom, double alphaTo,
                                 int colorFrom, int colorTo, float hueShiftDegrees,
                                 double sizeFrom, double sizeTo) {
        static ParticleLayer parse(JsonObject action) {
            ResourceLocation id = ResourceLocation.tryParse(string(action, "particle", "minecraft:end_rod"));
            if (id == null || !(ForgeRegistries.PARTICLE_TYPES.getValue(id) instanceof SimpleParticleType particle)) {
                throw new IllegalArgumentException("Unregistered simple particle type");
            }
            String anchor = string(action, "anchor", "body");
            String motion = string(action, "motion", "burst");
            if (!Set.of("feet", "body", "head").contains(anchor)
                    || !Set.of("burst", "inward", "outward", "spiral", "ring", "fountain").contains(motion)) {
                throw new IllegalArgumentException("Invalid particle anchor or motion");
            }
            int count = integer(action, "count", 8);
            int duration = integer(action, "durationTicks", 1);
            int interval = integer(action, "intervalTicks", 4);
            new ParticlePreviewTimeline.Layer(count, duration, interval);
            double radius = decimal(action, "radius", 0.7);
            double speed = decimal(action, "speed", 0.03);
            double previewCameraDistance = decimal(action, "previewCameraDistance", 4);
            double alphaFrom = decimal(action, "alphaFrom", 1);
            double alphaTo = decimal(action, "alphaTo", 1);
            int colorFrom = ParticleTint.parse(string(action, "colorFrom", ""));
            int colorTo = ParticleTint.parse(string(action, "colorTo", ""));
            double hueShift = decimal(action, "hueShiftDegrees", 0);
            double sizeFrom = decimal(action, "sizeFrom", 1);
            double sizeTo = decimal(action, "sizeTo", 1);
            if (!Double.isFinite(radius) || radius < 0 || radius > 4
                    || !Double.isFinite(speed) || speed < 0 || speed > 0.3
                    || !Double.isFinite(previewCameraDistance)
                    || previewCameraDistance < 2 || previewCameraDistance > 16
                    || !Double.isFinite(alphaFrom) || alphaFrom < 0 || alphaFrom > 1
                    || !Double.isFinite(alphaTo) || alphaTo < 0 || alphaTo > 1
                    || !Double.isFinite(hueShift) || Math.abs(hueShift) > 180
                    || !Double.isFinite(sizeFrom) || sizeFrom < 0.1 || sizeFrom > 4
                    || !Double.isFinite(sizeTo) || sizeTo < 0.1 || sizeTo > 4) {
                throw new IllegalArgumentException("Invalid particle radius, speed or preview camera distance");
            }
            return new ParticleLayer(id, particle, anchor, motion, count, radius, speed, duration, interval,
                    previewCameraDistance, alphaFrom, alphaTo, colorFrom, colorTo, (float) hueShift,
                    sizeFrom, sizeTo);
        }

        private static String string(JsonObject action, String key, String fallback) {
            return action.has(key) ? action.get(key).getAsString() : fallback;
        }

        private static int integer(JsonObject action, String key, int fallback) {
            return action.has(key) ? action.get(key).getAsInt() : fallback;
        }

        private static double decimal(JsonObject action, String key, double fallback) {
            return action.has(key) ? action.get(key).getAsDouble() : fallback;
        }
    }
}
