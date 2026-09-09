package com.kidxoxo.talenteditor.network;

import com.kidxoxo.talenteditor.domain.DefinitionKind;
import com.kidxoxo.talenteditor.editor.EditorPackStorage;
import com.kidxoxo.talenteditor.editor.EditorTransactionOperation;
import com.kidxoxo.talenteditor.editor.EditorTransactionState;
import com.kidxoxo.talenteditor.editor.EditorTransactionSummary;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

public record TransactionHistoryPacket(long revision, List<EditorTransactionSummary> transactions) {
    public TransactionHistoryPacket {
        transactions = List.copyOf(transactions);
    }

    static void encode(TransactionHistoryPacket packet, FriendlyByteBuf buffer) {
        if (packet.transactions().size() > EditorPackStorage.MAX_HISTORY_ENTRIES) {
            throw new IllegalArgumentException("Too many editor transactions");
        }
        buffer.writeVarLong(packet.revision());
        buffer.writeVarInt(packet.transactions().size());
        for (EditorTransactionSummary transaction : packet.transactions()) {
            buffer.writeUUID(transaction.transactionId());
            buffer.writeEnum(transaction.operation());
            buffer.writeEnum(transaction.kind());
            buffer.writeResourceLocation(transaction.definitionId());
            buffer.writeVarLong(transaction.timestamp());
            buffer.writeEnum(transaction.state());
            buffer.writeBoolean(transaction.canUndo());
        }
    }

    static TransactionHistoryPacket decode(FriendlyByteBuf buffer) {
        long revision = buffer.readVarLong();
        int count = buffer.readVarInt();
        if (count < 0 || count > EditorPackStorage.MAX_HISTORY_ENTRIES) {
            throw new IllegalArgumentException("Invalid transaction count: " + count);
        }
        ArrayList<EditorTransactionSummary> transactions = new ArrayList<>(count);
        for (int index = 0; index < count; index++) {
            transactions.add(new EditorTransactionSummary(
                    buffer.readUUID(),
                    buffer.readEnum(EditorTransactionOperation.class),
                    buffer.readEnum(DefinitionKind.class),
                    buffer.readResourceLocation(),
                    buffer.readVarLong(),
                    buffer.readEnum(EditorTransactionState.class),
                    buffer.readBoolean()
            ));
        }
        return new TransactionHistoryPacket(revision, transactions);
    }

    static void handle(TransactionHistoryPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () ->
                com.kidxoxo.talenteditor.client.ClientPacketHandlers.acceptTransactionHistory(packet)
        );
        contextSupplier.get().setPacketHandled(true);
    }
}
