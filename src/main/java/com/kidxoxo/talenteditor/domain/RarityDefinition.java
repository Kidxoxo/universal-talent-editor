package com.kidxoxo.talenteditor.domain;

import net.minecraft.resources.ResourceLocation;

import java.util.Objects;

public record RarityDefinition(
        ResourceLocation id,
        int schema,
        boolean enabled,
        LocalizedText name,
        int color,
        int order,
        boolean offerable,
        double baseWeight
) {
    public RarityDefinition {
        id = Objects.requireNonNull(id, "id");
        name = Objects.requireNonNull(name, "name");
    }
}

