package com.kidxoxo.talenteditor.network;

import com.kidxoxo.talenteditor.synthesis.SynthesisResult;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.Optional;
import java.util.function.Supplier;

public record SynthesisResultPacket(
        SynthesisResult.Status status,
        ResourceLocation target,
        Optional<ResourceLocation> detail
) {
    public SynthesisResultPacket {
        detail = detail == null ? Optional.empty() : detail;
    }

    static SynthesisResultPacket from(SynthesisResult result) {
        return new SynthesisResultPacket(result.status(), result.target(), result.detail());
    }

    static void encode(SynthesisResultPacket packet, FriendlyByteBuf buffer) {
        buffer.writeEnum(packet.status());
        buffer.writeResourceLocation(packet.target());
        buffer.writeBoolean(packet.detail().isPresent());
        packet.detail().ifPresent(buffer::writeResourceLocation);
    }

    static SynthesisResultPacket decode(FriendlyByteBuf buffer) {
        SynthesisResult.Status status = buffer.readEnum(SynthesisResult.Status.class);
        ResourceLocation target = buffer.readResourceLocation();
        Optional<ResourceLocation> detail = buffer.readBoolean()
                ? Optional.of(buffer.readResourceLocation()) : Optional.empty();
        return new SynthesisResultPacket(status, target, detail);
    }

    static void handle(SynthesisResultPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () ->
                com.kidxoxo.talenteditor.client.ClientPacketHandlers.acceptSynthesisResult(packet)
        );
        contextSupplier.get().setPacketHandled(true);
    }
}
