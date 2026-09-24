package com.kidxoxo.talenteditor.runtime;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/** Tracks recent server-side player motion for safe expression variables. */
public final class PlayerMotionTracker {
    private static final double STATIONARY_EPSILON_SQUARED = 1.0E-6D;
    private static final double TELEPORT_DISTANCE_SQUARED = 4_096.0D;
    private static final Map<UUID, Sample> SAMPLES = new ConcurrentHashMap<>();

    private PlayerMotionTracker() {
    }

    public static void tick(ServerPlayer player) {
        ResourceLocation dimension = player.level().dimension().location();
        Sample current = new Sample(dimension, player.getX(), player.getY(), player.getZ(), 0L, 0.0D);
        SAMPLES.compute(player.getUUID(), (ignored, previous) -> next(previous, current));
    }

    public static long stationaryTicks(ServerPlayer player) {
        Sample sample = SAMPLES.get(player.getUUID());
        return sample == null ? 0L : sample.stationaryTicks();
    }

    public static double lastMovement(ServerPlayer player) {
        Sample sample = SAMPLES.get(player.getUUID());
        return sample == null ? 0.0D : sample.lastMovement();
    }

    public static void remove(ServerPlayer player) {
        SAMPLES.remove(player.getUUID());
    }

    static Sample next(Sample previous, Sample current) {
        if (previous == null || !previous.dimension().equals(current.dimension())) return current;
        double dx = current.x() - previous.x();
        double dy = current.y() - previous.y();
        double dz = current.z() - previous.z();
        double distanceSquared = dx * dx + dy * dy + dz * dz;
        if (!Double.isFinite(distanceSquared) || distanceSquared > TELEPORT_DISTANCE_SQUARED) return current;
        double distance = Math.sqrt(distanceSquared);
        long stationary = distanceSquared <= STATIONARY_EPSILON_SQUARED
                ? (previous.stationaryTicks() == Long.MAX_VALUE ? Long.MAX_VALUE : previous.stationaryTicks() + 1L)
                : 0L;
        return new Sample(current.dimension(), current.x(), current.y(), current.z(), stationary, distance);
    }

    record Sample(
            ResourceLocation dimension,
            double x,
            double y,
            double z,
            long stationaryTicks,
            double lastMovement
    ) {
    }
}
