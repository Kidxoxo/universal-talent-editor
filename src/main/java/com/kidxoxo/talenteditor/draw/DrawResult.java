package com.kidxoxo.talenteditor.draw;

import net.minecraft.resources.ResourceLocation;

import java.util.Optional;

public record DrawResult(
        Status status,
        Optional<DrawSession> session,
        Optional<ResourceLocation> acquired
) {
    public DrawResult {
        session = session == null ? Optional.empty() : session;
        acquired = acquired == null ? Optional.empty() : acquired;
    }

    public boolean success() {
        return status == Status.READY || status == Status.REFRESHED || changesPlayerState()
                || status == Status.DUPLICATE_REFUNDED;
    }

    public boolean changesPlayerState() {
        return status == Status.ACQUIRED || status == Status.DUPLICATE_COUNTER_GRANTED;
    }

    public static DrawResult session(Status status, DrawSession session) {
        return new DrawResult(status, Optional.of(session), Optional.empty());
    }

    public static DrawResult acquired(ResourceLocation talent) {
        return new DrawResult(Status.ACQUIRED, Optional.empty(), Optional.of(talent));
    }

    public static DrawResult resolved(Status status, ResourceLocation talent) {
        return new DrawResult(status, Optional.empty(), Optional.of(talent));
    }

    public static DrawResult failed(Status status) {
        return new DrawResult(status, Optional.empty(), Optional.empty());
    }

    public enum Status {
        READY,
        REFRESHED,
        ACQUIRED,
        DUPLICATE_REFUNDED,
        DUPLICATE_COUNTER_GRANTED,
        DUPLICATE_REWARD_FULL,
        POOL_NOT_FOUND,
        POOL_DISABLED,
        NO_DRAW_TOKEN,
        OWNED_LIMIT_REACHED,
        NO_ELIGIBLE_TALENT,
        SESSION_EXPIRED,
        REFRESH_EXHAUSTED,
        PAID_REFRESH_REQUIRES_TWO_TOKENS,
        OFFER_INVALID
    }
}
