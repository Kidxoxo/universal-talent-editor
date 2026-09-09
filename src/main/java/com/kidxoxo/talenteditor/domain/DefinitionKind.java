package com.kidxoxo.talenteditor.domain;

public enum DefinitionKind {
    TALENT("talent"),
    RARITY("rarity"),
    POOL("pool"),
    COUNTER("counter"),
    LAYOUT("layout"),
    ITEM("item"),
    TALENT_CATEGORY("talent_category"),
    COUNTER_CATEGORY("counter_category");

    private final String serializedName;

    DefinitionKind(String serializedName) {
        this.serializedName = serializedName;
    }

    public String serializedName() {
        return serializedName;
    }
}
