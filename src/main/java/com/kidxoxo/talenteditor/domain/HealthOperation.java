package com.kidxoxo.talenteditor.domain;

import java.util.Locale;

public enum HealthOperation {
    DAMAGE, HEAL;

    public static HealthOperation parse(String value) {
        return valueOf(value.toUpperCase(Locale.ROOT));
    }
}
