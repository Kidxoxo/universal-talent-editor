package com.kidxoxo.talenteditor.client;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

import java.util.Optional;

public record LocalizedTextDraft(
        String literal,
        String translation,
        Optional<Integer> color,
        boolean bold,
        boolean italic
) {
    public LocalizedTextDraft {
        literal = literal == null ? "" : literal;
        translation = translation == null ? "" : translation;
        color = color == null ? Optional.empty() : color;
    }

    public static LocalizedTextDraft literal(String value) {
        return new LocalizedTextDraft(value, "", Optional.empty(), false, false);
    }

    public static LocalizedTextDraft parse(JsonElement element) {
        if (element == null) return literal("");
        try {
            if (element.isJsonPrimitive() && element.getAsJsonPrimitive().isString()) {
                return literal(element.getAsString());
            }
            if (!element.isJsonObject()) return literal("");
            JsonObject object = element.getAsJsonObject();
            String literal = string(object, "literal");
            String translation = string(object, "translation");
            Optional<Integer> color = Optional.empty();
            JsonElement rawColor = object.get("color");
            if (rawColor != null) {
                if (rawColor.isJsonPrimitive() && rawColor.getAsJsonPrimitive().isNumber()) {
                    color = Optional.of(rawColor.getAsInt());
                } else {
                    color = Optional.ofNullable(RarityDraft.tryParseColor(rawColor.getAsString()));
                }
            }
            return new LocalizedTextDraft(
                    literal, translation, color,
                    booleanValue(object, "bold", false), booleanValue(object, "italic", false)
            );
        } catch (RuntimeException ignored) {
            return literal("");
        }
    }

    public String fallbackText() {
        return literal.isBlank() ? translation : literal;
    }

    public MutableComponent component() {
        MutableComponent result = literal.isBlank()
                ? Component.translatable(translation)
                : Component.literal(literal);
        return result.withStyle(style -> {
            var updated = style.withBold(bold).withItalic(italic);
            return color.isEmpty() ? updated : updated.withColor(color.orElseThrow() & 0xFFFFFF);
        });
    }

    public JsonObject toJson() {
        JsonObject result = new JsonObject();
        if (!literal.isBlank()) result.addProperty("literal", literal);
        if (!translation.isBlank()) result.addProperty("translation", translation);
        color.ifPresent(value -> result.addProperty("color", RarityDraft.formatColor(value)));
        if (bold) result.addProperty("bold", true);
        if (italic) result.addProperty("italic", true);
        return result;
    }

    private static String string(JsonObject object, String key) {
        JsonElement element = object.get(key);
        try {
            return element != null && element.isJsonPrimitive() ? element.getAsString() : "";
        } catch (RuntimeException ignored) {
            return "";
        }
    }

    private static boolean booleanValue(JsonObject object, String key, boolean fallback) {
        JsonElement element = object.get(key);
        try {
            return element == null ? fallback : element.getAsBoolean();
        } catch (RuntimeException ignored) {
            return fallback;
        }
    }
}
