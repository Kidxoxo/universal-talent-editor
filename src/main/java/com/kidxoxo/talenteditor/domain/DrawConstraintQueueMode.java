package com.kidxoxo.talenteditor.domain;

import java.util.Locale;
import java.util.Optional;

/** Controls how a newly produced one-shot draw constraint enters the player's pending queue. */
public enum DrawConstraintQueueMode {
    APPEND,
    DEDUPE,
    REPLACE;

    public String serializedName() {
        return name().toLowerCase(Locale.ROOT);
    }

    public static Optional<DrawConstraintQueueMode> parse(String value) {
        if (value == null) return Optional.empty();
        try {
            return Optional.of(valueOf(value.strip().toUpperCase(Locale.ROOT)));
        } catch (IllegalArgumentException ignored) {
            return Optional.empty();
        }
    }
}
