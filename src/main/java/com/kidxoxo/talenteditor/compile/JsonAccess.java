package com.kidxoxo.talenteditor.compile;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import com.kidxoxo.talenteditor.domain.LocalizedText;
import com.kidxoxo.talenteditor.validation.ValidationReport;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

final class JsonAccess {
    private JsonAccess() {
    }

    static void knownKeys(JsonObject object, Set<String> allowed, String source, String path,
                          ValidationReport.Builder report) {
        for (String key : object.keySet()) {
            if (!allowed.contains(key)) {
                report.error("UNKNOWN_FIELD", source, child(path, key), "Unknown field '" + key + "'");
            }
        }
    }

    static boolean bool(JsonObject object, String key, boolean fallback, String source, String path,
                        ValidationReport.Builder report) {
        JsonElement value = object.get(key);
        if (value == null) return fallback;
        if (!(value instanceof JsonPrimitive primitive) || !primitive.isBoolean()) {
            report.error("EXPECTED_BOOLEAN", source, child(path, key), "Expected a boolean");
            return fallback;
        }
        return primitive.getAsBoolean();
    }

    static String string(JsonObject object, String key, String fallback, boolean required, String source,
                         String path, ValidationReport.Builder report) {
        JsonElement value = object.get(key);
        if (value == null) {
            if (required) report.error("REQUIRED_FIELD", source, child(path, key), "Required field is missing");
            return fallback;
        }
        if (!(value instanceof JsonPrimitive primitive) || !primitive.isString()) {
            report.error("EXPECTED_STRING", source, child(path, key), "Expected a string");
            return fallback;
        }
        String result = primitive.getAsString();
        if (required && result.isBlank()) {
            report.error("BLANK_STRING", source, child(path, key), "Value must not be blank");
            return fallback;
        }
        return result;
    }

    static int integer(JsonObject object, String key, int fallback, int min, int max, String source, String path,
                       ValidationReport.Builder report) {
        JsonElement value = object.get(key);
        if (value == null) return fallback;
        if (!(value instanceof JsonPrimitive primitive) || !primitive.isNumber()) {
            report.error("EXPECTED_INTEGER", source, child(path, key), "Expected an integer");
            return fallback;
        }
        try {
            double raw = primitive.getAsDouble();
            if (!Double.isFinite(raw) || raw != Math.rint(raw) || raw < min || raw > max) {
                report.error("INTEGER_RANGE", source, child(path, key),
                        "Expected an integer in range " + min + ".." + max);
                return fallback;
            }
            return (int) raw;
        } catch (RuntimeException ex) {
            report.error("EXPECTED_INTEGER", source, child(path, key), "Expected an integer");
            return fallback;
        }
    }

    static long longInteger(JsonObject object, String key, long fallback, long min, long max, String source,
                            String path, ValidationReport.Builder report) {
        JsonElement value = object.get(key);
        if (value == null) return fallback;
        if (!(value instanceof JsonPrimitive primitive) || !primitive.isNumber()) {
            report.error("EXPECTED_INTEGER", source, child(path, key), "Expected an integer");
            return fallback;
        }
        try {
            long result = primitive.getAsLong();
            if (!primitive.getAsString().matches("-?[0-9]+") || result < min || result > max) {
                report.error("INTEGER_RANGE", source, child(path, key),
                        "Expected an integer in range " + min + ".." + max);
                return fallback;
            }
            return result;
        } catch (RuntimeException ex) {
            report.error("EXPECTED_INTEGER", source, child(path, key), "Expected an integer");
            return fallback;
        }
    }

    static double decimal(JsonObject object, String key, double fallback, double min, double max, String source,
                          String path, ValidationReport.Builder report) {
        JsonElement value = object.get(key);
        if (value == null) return fallback;
        if (!(value instanceof JsonPrimitive primitive) || !primitive.isNumber()) {
            report.error("EXPECTED_NUMBER", source, child(path, key), "Expected a number");
            return fallback;
        }
        try {
            double result = primitive.getAsDouble();
            if (!Double.isFinite(result) || result < min || result > max) {
                report.error("NUMBER_RANGE", source, child(path, key),
                        "Expected a finite number in range " + min + ".." + max);
                return fallback;
            }
            return result;
        } catch (RuntimeException ex) {
            report.error("EXPECTED_NUMBER", source, child(path, key), "Expected a number");
            return fallback;
        }
    }

    static JsonObject object(JsonObject parent, String key, boolean required, String source, String path,
                             ValidationReport.Builder report) {
        JsonElement value = parent.get(key);
        if (value == null) {
            if (required) report.error("REQUIRED_FIELD", source, child(path, key), "Required object is missing");
            return new JsonObject();
        }
        if (!value.isJsonObject()) {
            report.error("EXPECTED_OBJECT", source, child(path, key), "Expected an object");
            return new JsonObject();
        }
        return value.getAsJsonObject();
    }

    static JsonArray array(JsonObject parent, String key, boolean required, String source, String path,
                           ValidationReport.Builder report) {
        JsonElement value = parent.get(key);
        if (value == null) {
            if (required) report.error("REQUIRED_FIELD", source, child(path, key), "Required array is missing");
            return new JsonArray();
        }
        if (!value.isJsonArray()) {
            report.error("EXPECTED_ARRAY", source, child(path, key), "Expected an array");
            return new JsonArray();
        }
        return value.getAsJsonArray();
    }

    static ResourceLocation requiredId(JsonObject object, String key, String fallback, boolean explicitNamespace,
                                       String source, String path, ValidationReport.Builder report) {
        String raw = string(object, key, fallback, true, source, path, report);
        ResourceLocation parsed = parseId(raw, explicitNamespace, source, child(path, key), report);
        ResourceLocation fallbackId = ResourceLocation.tryParse(fallback);
        return parsed == null ? fallbackId : parsed;
    }

    static ResourceLocation optionalId(JsonObject object, String key, boolean explicitNamespace, String source,
                                       String path, ValidationReport.Builder report) {
        if (!object.has(key)) return null;
        String raw = string(object, key, "", false, source, path, report);
        return parseId(raw, explicitNamespace, source, child(path, key), report);
    }

    static ResourceLocation parseId(String raw, boolean explicitNamespace, String source, String path,
                                    ValidationReport.Builder report) {
        if (explicitNamespace && !raw.contains(":")) {
            report.error("EXPLICIT_NAMESPACE_REQUIRED", source, path,
                    "Resource identifier must include a namespace");
            return null;
        }
        ResourceLocation id = ResourceLocation.tryParse(raw);
        if (id == null) {
            report.error("INVALID_RESOURCE_ID", source, path, "Invalid resource identifier '" + raw + "'");
        }
        return id;
    }

    static List<ResourceLocation> idList(JsonObject object, String key, boolean explicitNamespace, String source,
                                         String path, ValidationReport.Builder report) {
        JsonArray array = array(object, key, false, source, path, report);
        ArrayList<ResourceLocation> result = new ArrayList<>();
        HashSet<ResourceLocation> seen = new HashSet<>();
        for (int index = 0; index < array.size(); index++) {
            JsonElement element = array.get(index);
            String itemPath = child(path, key) + "[" + index + "]";
            if (!(element instanceof JsonPrimitive primitive) || !primitive.isString()) {
                report.error("EXPECTED_STRING", source, itemPath, "Expected a resource identifier string");
                continue;
            }
            ResourceLocation id = parseId(primitive.getAsString(), explicitNamespace, source, itemPath, report);
            if (id != null && !seen.add(id)) {
                report.error("DUPLICATE_VALUE", source, itemPath, "Duplicate resource identifier '" + id + "'");
            } else if (id != null) {
                result.add(id);
            }
        }
        return List.copyOf(result);
    }

    static List<String> stringList(JsonObject object, String key, String source, String path,
                                   ValidationReport.Builder report) {
        JsonArray array = array(object, key, false, source, path, report);
        ArrayList<String> result = new ArrayList<>();
        HashSet<String> seen = new HashSet<>();
        for (int index = 0; index < array.size(); index++) {
            JsonElement element = array.get(index);
            String itemPath = child(path, key) + "[" + index + "]";
            if (!(element instanceof JsonPrimitive primitive) || !primitive.isString()) {
                report.error("EXPECTED_STRING", source, itemPath, "Expected a string");
                continue;
            }
            String value = primitive.getAsString();
            if (value.isBlank()) {
                report.error("BLANK_STRING", source, itemPath, "Value must not be blank");
            } else if (!seen.add(value)) {
                report.error("DUPLICATE_VALUE", source, itemPath, "Duplicate value '" + value + "'");
            } else {
                result.add(value);
            }
        }
        return List.copyOf(result);
    }

    static LocalizedText localizedText(JsonElement value, String source, String path,
                                       ValidationReport.Builder report) {
        if (value instanceof JsonPrimitive primitive && primitive.isString()) {
            String literal = primitive.getAsString();
            if (literal.isBlank()) {
                report.error("BLANK_TEXT", source, path, "Text must not be blank");
            }
            return new LocalizedText(literal, "", null, false, false);
        }
        if (value == null || !value.isJsonObject()) {
            report.error("EXPECTED_TEXT", source, path, "Expected a string or localized text object");
            return new LocalizedText("?", "", null, false, false);
        }
        JsonObject object = value.getAsJsonObject();
        knownKeys(object, Set.of("literal", "translation", "color", "bold", "italic"), source, path, report);
        String literal = string(object, "literal", "", false, source, path, report);
        String translation = string(object, "translation", "", false, source, path, report);
        if (literal.isBlank() && translation.isBlank()) {
            report.error("TEXT_VALUE_REQUIRED", source, path, "Either literal or translation is required");
        }
        Integer color = object.has("color") ? color(object.get("color"), source, child(path, "color"), report) : null;
        boolean bold = bool(object, "bold", false, source, path, report);
        boolean italic = bool(object, "italic", false, source, path, report);
        return new LocalizedText(literal, translation, color, bold, italic);
    }

    static List<LocalizedText> localizedTextList(JsonObject object, String key, String source, String path,
                                                 ValidationReport.Builder report) {
        JsonElement value = object.get(key);
        if (value == null) return List.of();
        if (!value.isJsonArray()) {
            return List.of(localizedText(value, source, child(path, key), report));
        }
        JsonArray array = value.getAsJsonArray();
        ArrayList<LocalizedText> result = new ArrayList<>();
        for (int index = 0; index < array.size(); index++) {
            result.add(localizedText(array.get(index), source, child(path, key) + "[" + index + "]", report));
        }
        return List.copyOf(result);
    }

    static int color(JsonElement value, String source, String path, ValidationReport.Builder report) {
        if (value instanceof JsonPrimitive primitive && primitive.isNumber()) {
            try {
                return primitive.getAsInt();
            } catch (RuntimeException ignored) {
                report.error("INVALID_COLOR", source, path, "Invalid numeric color");
                return 0xFFFFFFFF;
            }
        }
        if (value instanceof JsonPrimitive primitive && primitive.isString()) {
            String raw = primitive.getAsString().trim().toUpperCase(Locale.ROOT);
            if (raw.startsWith("#")) raw = raw.substring(1);
            if (raw.matches("[0-9A-F]{6}")) raw = "FF" + raw;
            if (raw.matches("[0-9A-F]{8}")) {
                return (int) Long.parseLong(raw, 16);
            }
        }
        report.error("INVALID_COLOR", source, path, "Expected #RRGGBB, #AARRGGBB, or an integer");
        return 0xFFFFFFFF;
    }

    static String child(String path, String key) {
        return "$".equals(path) ? "$." + key : path + "." + key;
    }
}
