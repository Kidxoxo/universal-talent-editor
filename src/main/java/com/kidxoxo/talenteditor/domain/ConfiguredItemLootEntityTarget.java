package com.kidxoxo.talenteditor.domain;

import java.util.Locale;

public enum ConfiguredItemLootEntityTarget {
    THIS_ENTITY,
    KILLER_ENTITY;

    public String serializedName() {
        return name().toLowerCase(Locale.ROOT);
    }

    public static ConfiguredItemLootEntityTarget parse(String value) {
        return valueOf(value.strip().toUpperCase(Locale.ROOT));
    }
}
