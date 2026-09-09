package com.kidxoxo.talenteditor.domain;

import java.util.Locale;

public enum AttributeOperation {
    ADDITION,
    MULTIPLY_BASE,
    MULTIPLY_TOTAL;

    public static AttributeOperation parse(String value) {
        return valueOf(value.toUpperCase(Locale.ROOT));
    }
}

