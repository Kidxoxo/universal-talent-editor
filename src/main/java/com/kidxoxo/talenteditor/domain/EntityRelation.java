package com.kidxoxo.talenteditor.domain;

import java.util.Locale;

public enum EntityRelation {
    ANY, ALLY, ENEMY;

    public static EntityRelation parse(String value) {
        return valueOf(value.toUpperCase(Locale.ROOT));
    }
}
