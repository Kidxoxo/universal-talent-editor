package com.kidxoxo.talenteditor.domain;

import java.util.Locale;

public enum StateOperation {
    SET, ADD, MULTIPLY;

    public static StateOperation parse(String value) {
        return valueOf(value.toUpperCase(Locale.ROOT));
    }

    public double apply(double current, double amount) {
        return switch (this) {
            case SET -> amount;
            case ADD -> current + amount;
            case MULTIPLY -> current * amount;
        };
    }
}
