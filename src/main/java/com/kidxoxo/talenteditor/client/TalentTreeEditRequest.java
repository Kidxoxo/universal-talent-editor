package com.kidxoxo.talenteditor.client;

import net.minecraft.resources.ResourceLocation;

import java.util.Objects;
import java.util.Optional;

record TalentTreeEditRequest(
        ResourceLocation target,
        Page page,
        Mutation mutation,
        Optional<ResourceLocation> relatedTalent
) {
    TalentTreeEditRequest {
        target = Objects.requireNonNull(target, "target");
        page = Objects.requireNonNull(page, "page");
        mutation = Objects.requireNonNull(mutation, "mutation");
        relatedTalent = relatedTalent == null ? Optional.empty() : relatedTalent;
    }

    static TalentTreeEditRequest open(ResourceLocation target, Page page) {
        return new TalentTreeEditRequest(target, page, Mutation.NONE, Optional.empty());
    }

    static TalentTreeEditRequest mutate(
            ResourceLocation target, Page page, Mutation mutation, ResourceLocation relatedTalent
    ) {
        return new TalentTreeEditRequest(target, page, mutation, Optional.of(relatedTalent));
    }

    enum Page {
        TALENT,
        ACQUISITION,
        SYNTHESIS
    }

    enum Mutation {
        NONE,
        ADD,
        REMOVE
    }
}
