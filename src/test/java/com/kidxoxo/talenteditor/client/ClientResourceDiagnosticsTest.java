package com.kidxoxo.talenteditor.client;

import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ClientResourceDiagnosticsTest {
    @Test
    void distinguishesEmptyInvalidAvailableAndMissingIds() {
        Set<ResourceLocation> available = Set.of(new ResourceLocation("example", "textures/icon.png"));

        ClientResourceDiagnostics.Result empty = ClientResourceDiagnostics.inspect("  ", available::contains);
        ClientResourceDiagnostics.Result invalid = ClientResourceDiagnostics.inspect("Bad ID", available::contains);
        ClientResourceDiagnostics.Result present = ClientResourceDiagnostics.inspect(
                "example:textures/icon.png", available::contains
        );
        ClientResourceDiagnostics.Result missing = ClientResourceDiagnostics.inspect(
                "example:textures/missing.png", available::contains
        );

        assertEquals(ClientResourceDiagnostics.State.EMPTY, empty.state());
        assertNull(empty.id());
        assertEquals(ClientResourceDiagnostics.State.INVALID, invalid.state());
        assertEquals(ClientResourceDiagnostics.State.AVAILABLE, present.state());
        assertTrue(present.present());
        assertEquals(ClientResourceDiagnostics.State.MISSING, missing.state());
    }
}
