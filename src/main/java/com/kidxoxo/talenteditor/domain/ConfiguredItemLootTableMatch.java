package com.kidxoxo.talenteditor.domain;

import java.util.Locale;

public enum ConfiguredItemLootTableMatch {
    EXACT("exact"),
    PREFIX("prefix"),
    REGEX("regex");

    private final String serializedName;

    ConfiguredItemLootTableMatch(String serializedName) {
        this.serializedName = serializedName;
    }

    public String serializedName() {
        return serializedName;
    }

    public static ConfiguredItemLootTableMatch parse(String value) {
        String normalized = value.toLowerCase(Locale.ROOT);
        for (ConfiguredItemLootTableMatch match : values()) {
            if (match.serializedName.equals(normalized)) return match;
        }
        throw new IllegalArgumentException("Unknown loot table match mode: " + value);
    }
}
