package com.kidxoxo.talenteditor.domain;

import java.util.Locale;
import java.util.Optional;

public enum CounterAmountMode {
    FIXED,
    EVENT_AMOUNT;

    public String serializedName() {
        return name().toLowerCase(Locale.ROOT);
    }

    public static Optional<CounterAmountMode> parse(String value) {
        if (value == null) return Optional.empty();
        try {
            return Optional.of(valueOf(value.strip().toUpperCase(Locale.ROOT)));
        } catch (IllegalArgumentException ignored) {
            return Optional.empty();
        }
    }
}
