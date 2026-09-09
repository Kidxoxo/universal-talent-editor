package com.kidxoxo.talenteditor.domain;

import net.minecraft.resources.ResourceLocation;

import java.util.Objects;

public record DefinitionReference(
        DefinitionTarget target,
        DefinitionKind sourceKind,
        ResourceLocation sourceId,
        String usage,
        String path
) {
    public DefinitionReference {
        target = Objects.requireNonNull(target, "target");
        sourceKind = Objects.requireNonNull(sourceKind, "sourceKind");
        sourceId = Objects.requireNonNull(sourceId, "sourceId");
        usage = Objects.requireNonNull(usage, "usage");
        path = Objects.requireNonNull(path, "path");
    }
}
