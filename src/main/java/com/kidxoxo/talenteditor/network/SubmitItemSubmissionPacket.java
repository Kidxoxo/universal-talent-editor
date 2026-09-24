package com.kidxoxo.talenteditor.network;

import com.kidxoxo.talenteditor.TalentEditorMod;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.UUID;
import java.util.function.Supplier;

public record SubmitItemSubmissionPacket(UUID sessionId, boolean confirm) {
    static void encode(SubmitItemSubmissionPacket value, FriendlyByteBuf buffer) {
        buffer.writeUUID(value.sessionId); buffer.writeBoolean(value.confirm);
    }
    static SubmitItemSubmissionPacket decode(FriendlyByteBuf buffer) {
        return new SubmitItemSubmissionPacket(buffer.readUUID(), buffer.readBoolean());
    }
    static void handle(SubmitItemSubmissionPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        ServerPlayer player = context.getSender();
        if (player != null) context.enqueueWork(() -> {
            if (packet.confirm) TalentEditorMod.SUBMISSIONS.submit(player, packet.sessionId);
            else TalentEditorMod.SUBMISSIONS.cancel(player, packet.sessionId);
        });
        context.setPacketHandled(true);
    }
}
