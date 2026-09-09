package com.kidxoxo.talenteditor.domain;

import net.minecraft.resources.ResourceLocation;

import java.util.List;

/** Immutable selector used by both the normal and milestone result of a queued draw action. */
public record DrawConstraintSpec(
        List<ResourceLocation> talents,
        List<ResourceLocation> categories,
        List<ResourceLocation> rarities,
        List<ResourceLocation> tags,
        List<ResourceLocation> pools,
        int guaranteed
) {
    public DrawConstraintSpec {
        talents = List.copyOf(talents);
        categories = List.copyOf(categories);
        rarities = List.copyOf(rarities);
        tags = List.copyOf(tags);
        pools = List.copyOf(pools);
        if (talents.isEmpty() && categories.isEmpty() && rarities.isEmpty() && tags.isEmpty()) {
            throw new IllegalArgumentException("At least one draw selector is required");
        }
        if (guaranteed < 1 || guaranteed > 5) {
            throw new IllegalArgumentException("guaranteed must be in range 1..5");
        }
    }

    public boolean appliesTo(ResourceLocation pool) {
        return pools.isEmpty() || pools.contains(pool);
    }

    public boolean matches(TalentDefinition talent) {
        return (talents.isEmpty() || talents.contains(talent.id()))
                && (categories.isEmpty() || talent.categories().stream().anyMatch(categories::contains))
                && (rarities.isEmpty() || rarities.contains(talent.rarity()))
                && (tags.isEmpty() || talent.tags().stream().anyMatch(tags::contains));
    }

    /** Empty pool scopes mean all pools, so they overlap every other scope. */
    public boolean overlapsPoolScope(DrawConstraintSpec other) {
        if (pools.isEmpty() || other.pools.isEmpty()) return true;
        return pools.stream().anyMatch(other.pools::contains);
    }
}
