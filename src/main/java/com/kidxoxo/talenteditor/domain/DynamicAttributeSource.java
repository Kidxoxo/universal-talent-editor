package com.kidxoxo.talenteditor.domain;

import java.util.Locale;
import java.util.Optional;

public enum DynamicAttributeSource {
    COUNTER("counter"),
    HEALTH_PERCENT("health_percent"),
    HUNGER_PERCENT("hunger_percent");

    private final String serializedName;

    DynamicAttributeSource(String serializedName) {
        this.serializedName = serializedName;
    }

    public String serializedName() {
        return serializedName;
    }

    public static Optional<DynamicAttributeSource> parse(String value) {
        if (value == null) return Optional.empty();
        String normalized = value.toLowerCase(Locale.ROOT);
        for (DynamicAttributeSource source : values()) {
            if (source.serializedName.equals(normalized)) return Optional.of(source);
        }
        return Optional.empty();
    }
}
