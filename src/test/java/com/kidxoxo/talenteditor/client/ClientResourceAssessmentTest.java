package com.kidxoxo.talenteditor.client;

import com.kidxoxo.talenteditor.network.ClientResourceRequirementsPacket;
import com.kidxoxo.talenteditor.resource.ClientResourceRequirement;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ClientResourceAssessmentTest {
    @Test
    void keepsRequirementOrderAndTurnsProbeFailuresIntoMissingStatuses() {
        ClientResourceRequirement present = requirement("textures/present.png");
        ClientResourceRequirement failed = requirement("textures/failed.png");
        ClientResourceRequirementsPacket packet = new ClientResourceRequirementsPacket(
                4L, "0".repeat(64), false, List.of(present, failed)
        );

        var status = ClientResourceAssessment.assess(packet, requirement -> {
            if (requirement.equals(failed)) throw new IllegalStateException("test failure");
            return new ClientResourceAssessment.Observation(true, "A".repeat(64));
        });

        assertEquals(4L, status.revision());
        assertEquals(present, status.statuses().get(0).requirement());
        assertTrue(status.statuses().get(0).available());
        assertFalse(status.statuses().get(1).available());
        assertEquals("", status.statuses().get(1).sha256());
    }

    private static ClientResourceRequirement requirement(String path) {
        return new ClientResourceRequirement(
                ClientResourceRequirement.Kind.TEXTURE, new ResourceLocation("example", path)
        );
    }
}
