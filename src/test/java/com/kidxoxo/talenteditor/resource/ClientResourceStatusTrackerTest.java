package com.kidxoxo.talenteditor.resource;

import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ClientResourceStatusTrackerTest {
    private static final String HASH = "A".repeat(64);

    @Test
    void rejectsStaleResponseAndClassifiesMissingAssets() {
        ClientResourceStatusTracker tracker = new ClientResourceStatusTracker();
        UUID player = UUID.randomUUID();
        ClientResourceRequirement texture = requirement(ClientResourceRequirement.Kind.TEXTURE, "example:textures/a.png");
        ClientResourceRequirement sound = requirement(ClientResourceRequirement.Kind.SOUND, "example:a");
        List<ClientResourceRequirement> expected = List.of(texture, sound);
        tracker.begin(player, "Alex", 7L, HASH, expected, false);

        assertFalse(tracker.accept(player, 6L, HASH, List.of(), expected));
        assertEquals(ClientResourceStatusTracker.State.PENDING, tracker.snapshot().get(0).state());
        assertTrue(tracker.accept(player, 7L, HASH, List.of(
                new ClientResourceStatus(texture, true, "B".repeat(64)),
                new ClientResourceStatus(sound, false, "")
        ), expected));

        ClientResourceStatusTracker.Report report = tracker.snapshot().get(0);
        assertEquals(ClientResourceStatusTracker.State.MISSING, report.state());
        assertEquals(1, report.available());
        assertEquals(1, report.missing());
        assertEquals(1, report.hashed());
        assertEquals(List.of(sound), report.missingEntries());
    }

    @Test
    void doesNotTrustUnexpectedOrIncompleteClientLists() {
        ClientResourceStatusTracker tracker = new ClientResourceStatusTracker();
        UUID player = UUID.randomUUID();
        ClientResourceRequirement expected = requirement(ClientResourceRequirement.Kind.TEXTURE, "example:textures/a.png");
        ClientResourceRequirement unexpected = requirement(ClientResourceRequirement.Kind.SOUND, "example:other");
        tracker.begin(player, "Steve", 1L, HASH, List.of(expected), false);

        assertTrue(tracker.accept(player, 1L, HASH, List.of(
                new ClientResourceStatus(unexpected, true, "")
        ), List.of(expected)));
        ClientResourceStatusTracker.Report report = tracker.snapshot().get(0);
        assertEquals(ClientResourceStatusTracker.State.INCOMPLETE, report.state());
        assertEquals(1, report.unexpected());
        assertEquals(1, report.missing());

        tracker.remove(player);
        assertTrue(tracker.snapshot().isEmpty());
    }

    private static ClientResourceRequirement requirement(ClientResourceRequirement.Kind kind, String id) {
        return new ClientResourceRequirement(kind, new ResourceLocation(id));
    }
}
