package com.kidxoxo.talenteditor.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** One bounded translucent particle wave, generated locally from a shared seed. */
public record ParticleWavePacket(ResourceLocation particle, String motion, double x, double y, double z,
                                 int count, double radius, double speed, float alpha, int age, long seed,
                                 int color, float hueShiftDegrees, float size) {
    public ParticleWavePacket(ResourceLocation particle, String motion, double x, double y, double z,
                              int count, double radius, double speed, float alpha, int age, long seed) {
        this(particle, motion, x, y, z, count, radius, speed, alpha, age, seed, -1, 0, 1);
    }
    public ParticleWavePacket(ResourceLocation particle, String motion, double x, double y, double z,
                              int count, double radius, double speed, float alpha, int age, long seed,
                              int color, float hueShiftDegrees) {
        this(particle, motion, x, y, z, count, radius, speed, alpha, age, seed, color, hueShiftDegrees, 1);
    }
    static void encode(ParticleWavePacket packet, FriendlyByteBuf buffer) {
        buffer.writeResourceLocation(packet.particle);
        buffer.writeUtf(packet.motion, 16);
        buffer.writeDouble(packet.x);
        buffer.writeDouble(packet.y);
        buffer.writeDouble(packet.z);
        buffer.writeVarInt(packet.count);
        buffer.writeDouble(packet.radius);
        buffer.writeDouble(packet.speed);
        buffer.writeFloat(packet.alpha);
        buffer.writeVarInt(packet.age);
        buffer.writeLong(packet.seed);
        buffer.writeInt(packet.color);
        buffer.writeFloat(packet.hueShiftDegrees);
        buffer.writeFloat(packet.size);
    }

    static ParticleWavePacket decode(FriendlyByteBuf buffer) {
        return new ParticleWavePacket(buffer.readResourceLocation(), buffer.readUtf(16),
                buffer.readDouble(), buffer.readDouble(), buffer.readDouble(), buffer.readVarInt(),
                buffer.readDouble(), buffer.readDouble(), buffer.readFloat(), buffer.readVarInt(), buffer.readLong(),
                buffer.readInt(), buffer.readFloat(), buffer.readFloat());
    }

    static void handle(ParticleWavePacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () ->
                com.kidxoxo.talenteditor.client.ParticleWavePlayback.accept(packet));
        contextSupplier.get().setPacketHandled(true);
    }
}
