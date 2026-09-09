package com.kidxoxo.talenteditor.draw;

import net.minecraft.resources.ResourceLocation;

import java.util.List;
import java.util.UUID;

public record DrawSession(
        UUID id,
        UUID playerId,
        ResourceLocation poolId,
        long definitionRevision,
        List<ResourceLocation> offers,
        int refreshesRemaining
) {
    public DrawSession {
        offers = List.copyOf(offers);
    }
}
