package com.kidxoxo.talenteditor.domain;

import java.util.Locale;

public enum DamageOperation {
    ADD,
    MULTIPLY;

    public static DamageOperation parse(String value) {
        return switch (value.toLowerCase(Locale.ROOT)) {
            case "add" -> ADD;
            case "multiply" -> MULTIPLY;
            default -> throw new IllegalArgumentException("Unknown damage operation: " + value);
        };
    }

    public String serializedName() {
        return name().toLowerCase(Locale.ROOT);
    }
}
