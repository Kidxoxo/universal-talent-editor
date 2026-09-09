package com.kidxoxo.talenteditor.domain;

import java.util.Locale;
import java.util.Optional;

public enum CounterVisibility {
    FULL,
    CONDITION_ONLY,
    HIDDEN;

    public String serializedName() {
        return name().toLowerCase(Locale.ROOT);
    }

    public static Optional<CounterVisibility> parse(String value) {
        if (value == null) return Optional.empty();
        try {
            return Optional.of(valueOf(value.strip().toUpperCase(Locale.ROOT)));
        } catch (IllegalArgumentException ignored) {
            return Optional.empty();
        }
    }
}
