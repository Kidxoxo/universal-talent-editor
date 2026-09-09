package com.kidxoxo.talenteditor.network;

import com.kidxoxo.talenteditor.draw.DrawResult;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.Optional;
import java.util.function.Supplier;

public record DrawResultPacket(DrawResult.Status status, Optional<ResourceLocation> acquired) {
    public DrawResultPacket {
        acquired = acquired == null ? Optional.empty() : acquired;
    }

    static void encode(DrawResultPacket packet, FriendlyByteBuf buffer) {
        buffer.writeEnum(packet.status());
        buffer.writeBoolean(packet.acquired().isPresent());
        packet.acquired().ifPresent(buffer::writeResourceLocation);
    }

    static DrawResultPacket decode(FriendlyByteBuf buffer) {
        DrawResult.Status status = buffer.readEnum(DrawResult.Status.class);
        Optional<ResourceLocation> acquired = buffer.readBoolean()
                ? Optional.of(buffer.readResourceLocation()) : Optional.empty();
        return new DrawResultPacket(status, acquired);
    }

    static void handle(DrawResultPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () ->
                com.kidxoxo.talenteditor.client.ClientPacketHandlers.acceptDrawResult(packet)
        );
        contextSupplier.get().setPacketHandled(true);
    }
}
