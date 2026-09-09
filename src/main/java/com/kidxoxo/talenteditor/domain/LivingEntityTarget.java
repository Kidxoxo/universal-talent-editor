package com.kidxoxo.talenteditor.domain;

import java.util.Locale;

/** Living-entity selectors available to trigger actions. */
public enum LivingEntityTarget {
    SELF,
    EVENT_ENTITY,
    DAMAGE_SOURCE,
    NEARBY;

    public static LivingEntityTarget parse(String value) {
        try {
            return valueOf(value.toUpperCase(Locale.ROOT));
        } catch (RuntimeException exception) {
            throw new IllegalArgumentException("Unknown living entity target: " + value, exception);
        }
    }

    public String serializedName() {
        return name().toLowerCase(Locale.ROOT);
    }
}
