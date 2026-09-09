package com.kidxoxo.talenteditor.client;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.util.Locale;

public final class RarityDraft {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private final JsonObject root;

    private RarityDraft(JsonObject root) {
        this.root = root.deepCopy();
    }

    public static RarityDraft create() {
        JsonObject root = new JsonObject();
        root.addProperty("schema", 2);
        root.addProperty("enabled", true);
        root.addProperty("name", "");
        root.addProperty("color", "#A0A0A0");
        root.addProperty("order", 0);
        root.addProperty("offerable", true);
        root.addProperty("baseWeight", 1.0D);
        return new RarityDraft(root);
    }

    public static RarityDraft parse(String json) {
        JsonElement parsed = JsonParser.parseString(json);
        if (!parsed.isJsonObject()) throw new IllegalArgumentException("Rarity root must be an object");
        return new RarityDraft(parsed.getAsJsonObject());
    }

    public String name() {
        JsonElement name = root.get("name");
        if (name == null) return "";
        if (name.isJsonPrimitive() && name.getAsJsonPrimitive().isString()) return name.getAsString();
        if (!name.isJsonObject()) return "";
        JsonObject localized = name.getAsJsonObject();
        String literal = string(localized, "literal");
        return literal.isBlank() ? string(localized, "translation") : literal;
    }

    public boolean enabled() {
        return booleanValue(root, "enabled", true);
    }

    public String color() {
        JsonElement color = root.get("color");
        if (color == null) return "#A0A0A0";
        try {
            if (color.isJsonPrimitive() && color.getAsJsonPrimitive().isNumber()) {
                return formatColor(color.getAsInt());
            }
            return color.getAsString();
        } catch (RuntimeException ignored) {
            return "#A0A0A0";
        }
    }

    public int order() {
        return integer(root, "order", 0);
    }

    public boolean offerable() {
        return booleanValue(root, "offerable", true);
    }

    public double baseWeight() {
        return decimal(root, "baseWeight", 1.0D);
    }

    public String toJson(
            String displayName,
            boolean enabled,
            String color,
            int order,
            boolean offerable,
            double baseWeight
    ) {
        root.addProperty("schema", 2);
        JsonElement existingName = root.get("name");
        if (existingName != null && existingName.isJsonObject()) {
            existingName.getAsJsonObject().addProperty("literal", displayName);
        } else {
            root.addProperty("name", displayName);
        }
        root.addProperty("enabled", enabled);
        root.addProperty("color", normalizeColor(color));
        root.addProperty("order", order);
        root.addProperty("offerable", offerable);
        root.addProperty("baseWeight", baseWeight);
        return GSON.toJson(root);
    }

    public static Integer tryParseColor(String value) {
        String raw = value.strip().toUpperCase(Locale.ROOT);
        if (raw.startsWith("#")) raw = raw.substring(1);
        if (raw.matches("[0-9A-F]{6}")) raw = "FF" + raw;
        if (!raw.matches("[0-9A-F]{8}")) return null;
        try {
            return (int) Long.parseLong(raw, 16);
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    public static String formatColor(int color) {
        if ((color >>> 24) == 0xFF) return String.format(Locale.ROOT, "#%06X", color & 0xFFFFFF);
        return String.format(Locale.ROOT, "#%08X", color);
    }

    private static String normalizeColor(String value) {
        Integer color = tryParseColor(value);
        return color == null ? value.strip() : formatColor(color);
    }

    private static String string(JsonObject object, String key) {
        JsonElement element = object.get(key);
        try {
            return element != null && element.isJsonPrimitive() ? element.getAsString() : "";
        } catch (RuntimeException ignored) {
            return "";
        }
    }

    private static int integer(JsonObject object, String key, int fallback) {
        JsonElement element = object.get(key);
        try {
            return element == null ? fallback : element.getAsInt();
        } catch (RuntimeException ignored) {
            return fallback;
        }
    }

    private static double decimal(JsonObject object, String key, double fallback) {
        JsonElement element = object.get(key);
        try {
            return element == null ? fallback : element.getAsDouble();
        } catch (RuntimeException ignored) {
            return fallback;
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
