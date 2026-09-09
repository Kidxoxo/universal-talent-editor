package com.kidxoxo.talenteditor.loot;

import net.minecraft.resources.ResourceLocation;

import java.util.Optional;
import java.util.Objects;
import java.util.Set;

public record ConfiguredItemLootContext(
        Optional<ResourceLocation> dimension,
        Optional<EntityView> thisEntity,
        Optional<EntityView> killerEntity,
        double luck
) {
    public static final ConfiguredItemLootContext EMPTY =
            new ConfiguredItemLootContext(Optional.empty(), Optional.empty(), Optional.empty(), 0.0D);

    public ConfiguredItemLootContext {
        dimension = dimension == null ? Optional.empty() : dimension;
        thisEntity = thisEntity == null ? Optional.empty() : thisEntity;
        killerEntity = killerEntity == null ? Optional.empty() : killerEntity;
        if (!Double.isFinite(luck)) luck = 0.0D;
    }

    public record EntityView(ResourceLocation type, Set<ResourceLocation> tags) {
        public EntityView {
            type = Objects.requireNonNull(type, "type");
            tags = Set.copyOf(Objects.requireNonNull(tags, "tags"));
        }
    }
}
