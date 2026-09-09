package com.kidxoxo.talenteditor.network;

import com.kidxoxo.talenteditor.domain.DefinitionKind;
import com.kidxoxo.talenteditor.editor.EditorTransactionOperation;
import com.kidxoxo.talenteditor.editor.EditorTransactionState;
import com.kidxoxo.talenteditor.editor.EditorTransactionSummary;
import io.netty.buffer.Unpooled;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TransactionHistoryPacketTest {
    @Test
    void roundTripsBoundedTransactionSummaries() {
        TransactionHistoryPacket packet = new TransactionHistoryPacket(
                27L,
                List.of(new EditorTransactionSummary(
                        UUID.randomUUID(), EditorTransactionOperation.UPDATE,
                        DefinitionKind.TALENT, new ResourceLocation("example", "vitality"),
                        1_800_000_000_000L, EditorTransactionState.COMMITTED, true
                ))
        );
        FriendlyByteBuf buffer = new FriendlyByteBuf(Unpooled.buffer());
        try {
            TransactionHistoryPacket.encode(packet, buffer);
            assertEquals(packet, TransactionHistoryPacket.decode(buffer));
        } finally {
            buffer.release();
        }
    }
}
