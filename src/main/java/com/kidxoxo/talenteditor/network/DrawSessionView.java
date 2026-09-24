package com.kidxoxo.talenteditor.network;

import net.minecraft.resources.ResourceLocation;

import java.util.List;
import java.util.UUID;
import java.util.Map;
import java.util.LinkedHashMap;
import java.util.Collections;

public record DrawSessionView(
        UUID sessionId,
        long definitionRevision,
        ResourceLocation poolId,
        String poolName,
        int drawTokens,
        int refreshesRemaining,
        int iconSize,
        double luckValue,
        int pityProgress,
        int pityAfter,
        boolean pityActive,
        Map<ResourceLocation, Double> rarityProbabilities,
        List<PlayerTalentState.TalentView> offers
) {
    public DrawSessionView {
        if (iconSize < 8 || iconSize > 64) throw new IllegalArgumentException("iconSize must be 8-64");
        rarityProbabilities = Collections.unmodifiableMap(new LinkedHashMap<>(rarityProbabilities));
        offers = List.copyOf(offers);
    }

    public DrawSessionView(
            UUID sessionId,
            long definitionRevision,
            ResourceLocation poolId,
            String poolName,
            int drawTokens,
            int refreshesRemaining,
            List<PlayerTalentState.TalentView> offers
    ) {
        this(sessionId, definitionRevision, poolId, poolName, drawTokens, refreshesRemaining,
                64, 0.0D, 0, 0, false, Map.of(), offers);
    }
}
