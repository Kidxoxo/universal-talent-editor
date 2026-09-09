package com.kidxoxo.talenteditor.domain;

import java.util.Locale;
import java.util.Optional;

public enum CounterEventType {
    MANUAL,
    KILL,
    DAMAGE_DEALT,
    DAMAGE_TAKEN,
    ITEM_USE,
    ENTITY_INTERACT,
    MOVE_DISTANCE,
    PLAY_TIME;

    public String serializedName() {
        return name().toLowerCase(Locale.ROOT);
    }

    public static Optional<CounterEventType> parse(String value) {
        if (value == null) return Optional.empty();
        try {
            return Optional.of(valueOf(value.strip().toUpperCase(Locale.ROOT)));
        } catch (IllegalArgumentException ignored) {
            return Optional.empty();
        }
    }
}
