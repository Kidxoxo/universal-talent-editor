package com.kidxoxo.talenteditor.network;

import com.kidxoxo.talenteditor.editor.DefinitionUndoResult;
import com.kidxoxo.talenteditor.editor.DefinitionUndoStatus;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.UUID;
import java.util.function.Supplier;

public record DefinitionUndoResultPacket(
        DefinitionUndoStatus status,
        UUID transactionId,
        long activeRevision
) {
    static DefinitionUndoResultPacket from(DefinitionUndoResult result) {
        return new DefinitionUndoResultPacket(
                result.status(), result.transactionId(), result.activeRevision()
        );
    }

    static void encode(DefinitionUndoResultPacket packet, FriendlyByteBuf buffer) {
        buffer.writeEnum(packet.status());
        buffer.writeUUID(packet.transactionId());
        buffer.writeVarLong(packet.activeRevision());
    }

    static DefinitionUndoResultPacket decode(FriendlyByteBuf buffer) {
        return new DefinitionUndoResultPacket(
                buffer.readEnum(DefinitionUndoStatus.class),
                buffer.readUUID(),
                buffer.readVarLong()
        );
    }

    static void handle(DefinitionUndoResultPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () ->
                com.kidxoxo.talenteditor.client.ClientPacketHandlers.acceptUndoResult(packet)
        );
        contextSupplier.get().setPacketHandled(true);
    }
}
