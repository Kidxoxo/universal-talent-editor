package com.kidxoxo.talenteditor.domain;

import java.util.Locale;
import java.util.Optional;

public enum DuplicatePolicy {
    EXCLUDE,
    REFUND_TOKEN,
    ADD_COUNTER;

    public String serializedName() {
        return name().toLowerCase(Locale.ROOT);
    }

    public static Optional<DuplicatePolicy> parse(String value) {
        if (value == null) return Optional.empty();
        try {
            return Optional.of(valueOf(value.strip().toUpperCase(Locale.ROOT)));
        } catch (IllegalArgumentException ignored) {
            return Optional.empty();
        }
    }
}
