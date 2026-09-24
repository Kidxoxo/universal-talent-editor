package com.kidxoxo.talenteditor.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.UUID;
import java.util.function.Supplier;

public record ItemSubmissionResultPacket(UUID sessionId, Status status) {
    public enum Status { ACCEPTED, INSUFFICIENT_ITEMS, EXPIRED, NOT_FOUND }
    static void encode(ItemSubmissionResultPacket value, FriendlyByteBuf buffer) {
        buffer.writeUUID(value.sessionId); buffer.writeEnum(value.status);
    }
    static ItemSubmissionResultPacket decode(FriendlyByteBuf buffer) {
        return new ItemSubmissionResultPacket(buffer.readUUID(), buffer.readEnum(Status.class));
    }
    static void handle(ItemSubmissionResultPacket packet, Supplier<NetworkEvent.Context> context) {
        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () ->
                com.kidxoxo.talenteditor.client.ClientPacketHandlers.acceptItemSubmissionResult(packet));
        context.get().setPacketHandled(true);
    }
}
