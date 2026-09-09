package com.kidxoxo.talenteditor.resource;

import net.minecraft.resources.ResourceLocation;

import java.util.Objects;

/** A client asset referenced by the currently effective server definitions. */
public record ClientResourceRequirement(Kind kind, ResourceLocation id) {
    public ClientResourceRequirement {
        kind = Objects.requireNonNull(kind, "kind");
        id = Objects.requireNonNull(id, "id");
    }

    public enum Kind {
        TEXTURE,
        SOUND
    }
}
