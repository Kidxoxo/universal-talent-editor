package com.kidxoxo.talenteditor.domain;

import java.util.Locale;
import java.util.Optional;

/** Controls whether the primary action chain continues after an action does not apply. */
public enum ActionFailurePolicy {
    CONTINUE,
    STOP;

    public String serializedName() {
        return name().toLowerCase(Locale.ROOT);
    }

    public static Optional<ActionFailurePolicy> parse(String value) {
        if (value == null) return Optional.empty();
        try {
            return Optional.of(valueOf(value.strip().toUpperCase(Locale.ROOT)));
        } catch (IllegalArgumentException ignored) {
            return Optional.empty();
        }
    }
}
