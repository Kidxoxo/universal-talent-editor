package com.kidxoxo.talenteditor.domain;

import net.minecraft.resources.ResourceLocation;

import java.util.Objects;

public record DefinitionTarget(DefinitionKind kind, ResourceLocation id) {
    public DefinitionTarget {
        kind = Objects.requireNonNull(kind, "kind");
        id = Objects.requireNonNull(id, "id");
    }
}
