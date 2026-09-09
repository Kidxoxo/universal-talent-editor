package com.kidxoxo.talenteditor.network;

import com.kidxoxo.talenteditor.settings.EditorRuntimeSettings;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundSource;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** Plays one non-positional synthesis sound under the client-side replacement/queue policy. */
public record SynthesisSoundPacket(
        ResourceLocation sound,
        SoundSource source,
        float volume,
        float pitch,
        EditorRuntimeSettings.SoundOverlapPolicy overlapPolicy
) {
    static void encode(SynthesisSoundPacket packet, FriendlyByteBuf buffer) {
        buffer.writeResourceLocation(packet.sound());
        buffer.writeEnum(packet.source());
        buffer.writeFloat(packet.volume());
        buffer.writeFloat(packet.pitch());
        buffer.writeEnum(packet.overlapPolicy());
    }

    static SynthesisSoundPacket decode(FriendlyByteBuf buffer) {
        return new SynthesisSoundPacket(
                buffer.readResourceLocation(), buffer.readEnum(SoundSource.class),
                buffer.readFloat(), buffer.readFloat(),
                buffer.readEnum(EditorRuntimeSettings.SoundOverlapPolicy.class)
        );
    }

    static void handle(SynthesisSoundPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () ->
                com.kidxoxo.talenteditor.client.SynthesisSoundPlayback.accept(packet)
        );
        contextSupplier.get().setPacketHandled(true);
    }
}
