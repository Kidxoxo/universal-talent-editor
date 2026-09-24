package com.kidxoxo.talenteditor.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.UUID;
import java.util.function.Supplier;

public record ItemSubmissionOpenPacket(UUID sessionId, ResourceLocation item, int count, boolean consume, int timeoutTicks) {
    public ItemSubmissionOpenPacket {
        if (count < 1 || count > 4096 || timeoutTicks < 20 || timeoutTicks > 72_000) {
            throw new IllegalArgumentException("Invalid item submission session");
        }
    }
    static void encode(ItemSubmissionOpenPacket value, FriendlyByteBuf buffer) {
        buffer.writeUUID(value.sessionId); buffer.writeResourceLocation(value.item);
        buffer.writeVarInt(value.count); buffer.writeBoolean(value.consume); buffer.writeVarInt(value.timeoutTicks);
    }
    static ItemSubmissionOpenPacket decode(FriendlyByteBuf buffer) {
        return new ItemSubmissionOpenPacket(buffer.readUUID(), buffer.readResourceLocation(),
                buffer.readVarInt(), buffer.readBoolean(), buffer.readVarInt());
    }
    static void handle(ItemSubmissionOpenPacket packet, Supplier<NetworkEvent.Context> context) {
        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () ->
                com.kidxoxo.talenteditor.client.ClientPacketHandlers.acceptItemSubmissionOpen(packet));
        context.get().setPacketHandled(true);
    }
}
