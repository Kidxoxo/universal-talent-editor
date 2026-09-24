package com.kidxoxo.talenteditor.domain;

import java.util.Locale;

public enum ComparisonOperator {
    LT, LTE, EQ, NEQ, GTE, GT;

    public static ComparisonOperator parse(String value) {
        return valueOf(value.toUpperCase(Locale.ROOT));
    }

    public boolean test(double left, double right) {
        return switch (this) {
            case LT -> left < right;
            case LTE -> left <= right;
            case EQ -> Math.abs(left - right) < 1.0E-9D;
            case NEQ -> Math.abs(left - right) >= 1.0E-9D;
            case GTE -> left >= right;
            case GT -> left > right;
        };
    }
}
