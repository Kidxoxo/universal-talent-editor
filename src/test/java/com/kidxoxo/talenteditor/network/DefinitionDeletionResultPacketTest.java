package com.kidxoxo.talenteditor.network;

import com.kidxoxo.talenteditor.domain.DefinitionKind;
import com.kidxoxo.talenteditor.domain.DefinitionReference;
import com.kidxoxo.talenteditor.domain.DefinitionTarget;
import com.kidxoxo.talenteditor.editor.DefinitionDeletionStatus;
import io.netty.buffer.Unpooled;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class DefinitionDeletionResultPacketTest {
    @Test
    void roundTripsBlockingReferences() {
        ResourceLocation target = new ResourceLocation("example", "vitality");
        DefinitionDeletionResultPacket packet = new DefinitionDeletionResultPacket(
                DefinitionDeletionStatus.BLOCKED_REFERENCES,
                DefinitionKind.TALENT,
                target,
                42L,
                List.of(new DefinitionReference(
                        new DefinitionTarget(DefinitionKind.TALENT, target),
                        DefinitionKind.TALENT,
                        new ResourceLocation("example", "dependent"),
                        "condition_talent",
                        "$.acquireCondition.talent"
                ))
        );
        FriendlyByteBuf buffer = new FriendlyByteBuf(Unpooled.buffer());
        try {
            DefinitionDeletionResultPacket.encode(packet, buffer);
            assertEquals(packet, DefinitionDeletionResultPacket.decode(buffer));
        } finally {
            buffer.release();
        }
    }
}
