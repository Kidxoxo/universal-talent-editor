package com.kidxoxo.talenteditor.domain;

import java.util.Locale;

public enum EntityOrder {
    NEAREST, FARTHEST, RANDOM;

    public static EntityOrder parse(String value) {
        return valueOf(value.toUpperCase(Locale.ROOT));
    }
}
