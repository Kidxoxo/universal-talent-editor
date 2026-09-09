package com.kidxoxo.talenteditor.network;

import com.kidxoxo.talenteditor.resource.ClientResourceRequirement;
import com.kidxoxo.talenteditor.resource.ClientResourceStatus;
import io.netty.buffer.Unpooled;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ClientResourcePacketTest {
    private static final String HASH = "0".repeat(64);

    @Test
    void roundTripsRequirementsAndStatuses() {
        ClientResourceRequirement texture = new ClientResourceRequirement(
                ClientResourceRequirement.Kind.TEXTURE,
                new ResourceLocation("example", "textures/a.png")
        );
        ClientResourceRequirement sound = new ClientResourceRequirement(
                ClientResourceRequirement.Kind.SOUND,
                new ResourceLocation("example", "talent.acquire")
        );
        ClientResourceRequirementsPacket requirements = new ClientResourceRequirementsPacket(
                9L, HASH, true, List.of(texture, sound)
        );
        FriendlyByteBuf requirementBuffer = new FriendlyByteBuf(Unpooled.buffer());
        try {
            ClientResourceRequirementsPacket.encode(requirements, requirementBuffer);
            assertEquals(requirements, ClientResourceRequirementsPacket.decode(requirementBuffer));
        } finally {
            requirementBuffer.release();
        }

        ClientResourceStatusPacket statuses = new ClientResourceStatusPacket(
                9L, HASH, List.of(
                new ClientResourceStatus(texture, true, "A".repeat(64)),
                new ClientResourceStatus(sound, false, "")
        ));
        FriendlyByteBuf statusBuffer = new FriendlyByteBuf(Unpooled.buffer());
        try {
            ClientResourceStatusPacket.encode(statuses, statusBuffer);
            assertEquals(statuses, ClientResourceStatusPacket.decode(statusBuffer));
        } finally {
            statusBuffer.release();
        }
        assertEquals(1, statuses.availableCount());
        assertEquals(1, statuses.missingCount());
    }

    @Test
    void rejectsInvalidHashes() {
        assertThrows(IllegalArgumentException.class, () -> new ClientResourceRequirementsPacket(
                1L, "short", false, List.of()
        ));
    }
}
