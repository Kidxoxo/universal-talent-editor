package com.kidxoxo.talenteditor.domain;

import java.util.Locale;

public enum ActionTarget {
    SELF,
    EVENT_ENTITY;

    public static ActionTarget parse(String value) {
        return switch (value.toLowerCase(Locale.ROOT)) {
            case "self" -> SELF;
            case "event_entity" -> EVENT_ENTITY;
            default -> throw new IllegalArgumentException("Unknown action target: " + value);
        };
    }

    public String serializedName() {
        return name().toLowerCase(Locale.ROOT);
    }
}
