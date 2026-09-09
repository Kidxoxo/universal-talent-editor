package com.kidxoxo.talenteditor.domain;

import java.util.Locale;
import java.util.Optional;

public enum MessageDisplay {
    CHAT,
    ACTION_BAR,
    TITLE;

    public static Optional<MessageDisplay> parse(String value) {
        try {
            return Optional.of(valueOf(value.strip().toUpperCase(Locale.ROOT)));
        } catch (IllegalArgumentException | NullPointerException exception) {
            return Optional.empty();
        }
    }

    public String serializedName() {
        return name().toLowerCase(Locale.ROOT);
    }
}
