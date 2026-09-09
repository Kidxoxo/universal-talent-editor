package com.kidxoxo.talenteditor.compile;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.resources.ResourceLocation;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Comparator;
import java.util.Map;

final class CanonicalJsonHasher {
    private static final Gson GSON = new Gson();

    private CanonicalJsonHasher() {
    }

    static String hash(RawDefinitionSet definitions) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            updateMap(digest, "rarity", definitions.rarities());
            updateMap(digest, "pool", definitions.pools());
            updateMap(digest, "counter", definitions.counters());
            updateMap(digest, "layout", definitions.layouts());
            updateMap(digest, "item", definitions.configuredItems());
            updateMap(digest, "talent", definitions.talents());
            return toHex(digest.digest());
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 is unavailable", ex);
        }
    }

    private static void updateMap(MessageDigest digest, String type, Map<ResourceLocation, JsonObject> values) {
        values.entrySet().stream()
                .sorted(Map.Entry.comparingByKey(Comparator.comparing(ResourceLocation::toString)))
                .forEach(entry -> {
                    update(digest, type);
                    update(digest, entry.getKey().toString());
                    StringBuilder canonical = new StringBuilder();
                    append(entry.getValue(), canonical);
                    update(digest, canonical.toString());
                });
    }

    private static void append(JsonElement element, StringBuilder output) {
        if (element.isJsonObject()) {
            output.append('{');
            element.getAsJsonObject().entrySet().stream()
                    .sorted(Map.Entry.comparingByKey())
                    .forEach(entry -> {
                        output.append(GSON.toJson(entry.getKey())).append(':');
                        append(entry.getValue(), output);
                        output.append(',');
                    });
            output.append('}');
        } else if (element.isJsonArray()) {
            output.append('[');
            element.getAsJsonArray().forEach(child -> {
                append(child, output);
                output.append(',');
            });
            output.append(']');
        } else {
            output.append(GSON.toJson(element));
        }
    }

    private static void update(MessageDigest digest, String value) {
        byte[] bytes = value.getBytes(StandardCharsets.UTF_8);
        digest.update((byte) (bytes.length >>> 24));
        digest.update((byte) (bytes.length >>> 16));
        digest.update((byte) (bytes.length >>> 8));
        digest.update((byte) bytes.length);
        digest.update(bytes);
    }

    private static String toHex(byte[] bytes) {
        StringBuilder result = new StringBuilder(bytes.length * 2);
        for (byte value : bytes) result.append(String.format("%02x", value));
        return result.toString();
    }
}
