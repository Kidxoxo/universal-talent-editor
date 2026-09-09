package com.kidxoxo.talenteditor.synthesis;

import net.minecraft.resources.ResourceLocation;

import java.util.Optional;

public record SynthesisResult(
        Status status,
        ResourceLocation target,
        Optional<ResourceLocation> detail
) {
    public SynthesisResult {
        detail = detail == null ? Optional.empty() : detail;
    }

    public boolean successful() {
        return status == Status.SUCCESS;
    }

    public static SynthesisResult success(ResourceLocation target) {
        return new SynthesisResult(Status.SUCCESS, target, Optional.empty());
    }

    public static SynthesisResult failed(Status status, ResourceLocation target) {
        return new SynthesisResult(status, target, Optional.empty());
    }

    public static SynthesisResult failed(Status status, ResourceLocation target, ResourceLocation detail) {
        return new SynthesisResult(status, target, Optional.of(detail));
    }

    public enum Status {
        SUCCESS,
        TARGET_NOT_FOUND,
        TARGET_UNAVAILABLE,
        NOT_SYNTHESIZABLE,
        ALREADY_OWNED,
        REQUIREMENTS_NOT_MET,
        MISSING_TALENT,
        MISSING_COUNTER,
        MISSING_ITEM,
        PLAYER_DATA_UNAVAILABLE
    }
}
