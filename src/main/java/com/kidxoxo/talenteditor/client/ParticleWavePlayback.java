package com.kidxoxo.talenteditor.client;

import com.kidxoxo.talenteditor.mixin.client.ParticleAlphaAccessor;
import com.kidxoxo.talenteditor.mixin.client.ParticleColorAccessor;
import com.kidxoxo.talenteditor.domain.ParticleTint;
import com.kidxoxo.talenteditor.network.ParticleWavePacket;
import net.minecraft.client.Minecraft;
import net.minecraft.client.particle.Particle;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.util.RandomSource;
import net.minecraftforge.registries.ForgeRegistries;

/** Client-side wave rendering supports opacity without changing the event trigger particle format. */
public final class ParticleWavePlayback {
    private ParticleWavePlayback() {}

    public static void accept(ParticleWavePacket packet) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null || packet.count() < 1 || packet.count() > 128
                || !Double.isFinite(packet.radius()) || packet.radius() < 0 || packet.radius() > 4
                || !Double.isFinite(packet.speed()) || packet.speed() < 0 || packet.speed() > 0.3
                || !Float.isFinite(packet.alpha()) || packet.alpha() < 0 || packet.alpha() > 1
                || !Float.isFinite(packet.hueShiftDegrees()) || Math.abs(packet.hueShiftDegrees()) > 180
                || !Float.isFinite(packet.size()) || packet.size() < 0.1f || packet.size() > 4f
                || packet.color() < -1 || packet.color() > 0xFFFFFF
                || !(ForgeRegistries.PARTICLE_TYPES.getValue(packet.particle()) instanceof SimpleParticleType type)) {
            return;
        }
        RandomSource random = RandomSource.create(packet.seed());
        for (int i = 0; i < packet.count(); i++) {
            double x = packet.x(), y = packet.y(), z = packet.z();
            double vx, vy, vz;
            if (packet.motion().equals("burst")) {
                x += random.nextGaussian() * packet.radius();
                y += random.nextGaussian() * packet.radius() * 0.7;
                z += random.nextGaussian() * packet.radius();
                vx = random.nextGaussian() * packet.speed();
                vy = random.nextGaussian() * packet.speed();
                vz = random.nextGaussian() * packet.speed();
            } else if (packet.motion().equals("spiral")) {
                double angle = packet.age() * 0.35 + i * Math.PI * 2 / packet.count();
                double radius = Math.max(0.15, packet.radius());
                double dx = Math.cos(angle), dz = Math.sin(angle);
                x += dx * radius;
                y += (i % 4) * radius * 0.12;
                z += dz * radius;
                vx = -dz * packet.speed();
                vy = packet.speed() * 0.5;
                vz = dx * packet.speed();
            } else if (packet.motion().equals("ring")) {
                double angle = i * Math.PI * 2 / packet.count() + packet.age() * 0.15;
                double dx = Math.cos(angle), dz = Math.sin(angle);
                x += dx * packet.radius();
                z += dz * packet.radius();
                vy = packet.speed() * 0.2;
                vx = dx * packet.speed();
                vz = dz * packet.speed();
            } else if (packet.motion().equals("fountain")) {
                double angle = random.nextDouble() * Math.PI * 2;
                double spread = random.nextDouble() * packet.radius() * 0.3;
                double dx = Math.cos(angle), dz = Math.sin(angle);
                x += dx * spread;
                z += dz * spread;
                vx = dx * packet.speed() * 0.5;
                vy = packet.speed() * (1.5 + random.nextDouble());
                vz = dz * packet.speed() * 0.5;
            } else if (packet.motion().equals("inward") || packet.motion().equals("outward")) {
                double angle = random.nextDouble() * Math.PI * 2;
                double height = (random.nextDouble() - 0.5) * packet.radius();
                double distance = packet.radius() * (0.5 + random.nextDouble() * 0.5);
                double dx = Math.cos(angle), dz = Math.sin(angle);
                boolean inward = packet.motion().equals("inward");
                double velocity = packet.speed() * (inward ? -1 : 1);
                x += inward ? dx * distance : dx * 0.15;
                y += height;
                z += inward ? dz * distance : dz * 0.15;
                vx = dx * velocity;
                vy = height / Math.max(packet.radius(), 0.1) * velocity;
                vz = dz * velocity;
            } else return;
            Particle particle = minecraft.particleEngine.createParticle(type, x, y, z, vx, vy, vz);
            if (particle != null) {
                ((ParticleAlphaAccessor) particle).talentEditor$setAlpha(packet.alpha());
                if (packet.size() != 1f) particle.scale(packet.size());
                if (packet.color() >= 0 || packet.hueShiftDegrees() != 0) {
                    int color = packet.color();
                    if (color < 0) {
                        ParticleColorAccessor base = (ParticleColorAccessor) particle;
                        color = Math.round(Math.max(0, Math.min(1, base.talentEditor$getRed())) * 255) << 16
                                | Math.round(Math.max(0, Math.min(1, base.talentEditor$getGreen())) * 255) << 8
                                | Math.round(Math.max(0, Math.min(1, base.talentEditor$getBlue())) * 255);
                    }
                    color = ParticleTint.rotateHue(color, packet.hueShiftDegrees());
                    particle.setColor(((color >> 16) & 255) / 255f,
                            ((color >> 8) & 255) / 255f, (color & 255) / 255f);
                }
            }
        }
    }
}
