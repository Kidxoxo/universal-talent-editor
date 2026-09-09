package com.kidxoxo.talenteditor.network;

import com.kidxoxo.talenteditor.editor.DefinitionUndoStatus;
import io.netty.buffer.Unpooled;
import net.minecraft.network.FriendlyByteBuf;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

class DefinitionUndoResultPacketTest {
    @Test
    void roundTripsUndoOutcome() {
        DefinitionUndoResultPacket packet = new DefinitionUndoResultPacket(
                DefinitionUndoStatus.STALE_TARGET, UUID.randomUUID(), 91L
        );
        FriendlyByteBuf buffer = new FriendlyByteBuf(Unpooled.buffer());
        try {
            DefinitionUndoResultPacket.encode(packet, buffer);
            assertEquals(packet, DefinitionUndoResultPacket.decode(buffer));
        } finally {
            buffer.release();
        }
    }
}
