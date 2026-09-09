package com.kidxoxo.talenteditor.domain;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

public record LocalizedText(
        String literal,
        String translation,
        Integer color,
        boolean bold,
        boolean italic
) {
    public LocalizedText {
        literal = literal == null ? "" : literal;
        translation = translation == null ? "" : translation;
    }

    public String fallbackText() {
        return literal.isBlank() ? translation : literal;
    }

    public MutableComponent component() {
        MutableComponent result = literal.isBlank() ? Component.translatable(translation) : Component.literal(literal);
        return result.withStyle(style -> {
            var updated = style.withBold(bold).withItalic(italic);
            return color == null ? updated : updated.withColor(color & 0xFFFFFF);
        });
    }
}
