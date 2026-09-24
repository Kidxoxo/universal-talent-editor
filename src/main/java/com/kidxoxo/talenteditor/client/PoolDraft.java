package com.kidxoxo.talenteditor.client;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.resources.ResourceLocation;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

public final class PoolDraft {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private final JsonObject root;

    private PoolDraft(JsonObject root) {
        this.root = root.deepCopy();
    }

    public static PoolDraft create() {
        JsonObject root = new JsonObject();
        root.addProperty("schema", 2);
        root.addProperty("enabled", true);
        root.addProperty("name", "");
        root.addProperty("choices", 3);
        root.addProperty("refreshes", 1);
        root.addProperty("maxOwned", 24);
        root.addProperty("iconSize", 64);
        root.addProperty("excludeOwned", true);
        root.addProperty("duplicatePolicy", "exclude");
        root.addProperty("duplicateCounterAmount", 1L);
        root.addProperty("pityAfter", 0);
        root.add("rarityWeights", new JsonObject());
        root.add("luckFactors", new JsonObject());
        return new PoolDraft(root);
    }

    public static PoolDraft parse(String json) {
        JsonElement parsed = JsonParser.parseString(json);
        if (!parsed.isJsonObject()) throw new IllegalArgumentException("Pool root must be an object");
        return new PoolDraft(parsed.getAsJsonObject());
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

    public int choices() {
        return integer(root, "choices", 3);
    }

    public int refreshes() {
        return integer(root, "refreshes", 1);
    }

    public int maxOwned() {
        return integer(root, "maxOwned", 24);
    }

    public int iconSize() {
        return integer(root, "iconSize", 64);
    }

    public boolean excludeOwned() {
        return duplicatePolicy().equals("exclude");
    }

    public String duplicatePolicy() {
        String value = string(root, "duplicatePolicy");
        return value.isBlank() ? (booleanValue(root, "excludeOwned", true) ? "exclude" : "refund_token") : value;
    }

    public Map<ResourceLocation, Double> rarityWeights() {
        return decimalMap("rarityWeights");
    }

    public Advanced advanced() {
        return new Advanced(
                optionalId("luckAttribute"), decimalMap("luckFactors"), optionalId("pityRarity"),
                integer(root, "pityAfter", 0), duplicatePolicy(), optionalId("duplicateCounter"),
                longInteger(root, "duplicateCounterAmount", 1L)
        );
    }

    private Map<ResourceLocation, Double> decimalMap(String field) {
        LinkedHashMap<ResourceLocation, Double> result = new LinkedHashMap<>();
        JsonElement raw = root.get(field);
        if (raw == null || !raw.isJsonObject()) return result;
        raw.getAsJsonObject().entrySet().forEach(entry -> {
            ResourceLocation id = ResourceLocation.tryParse(entry.getKey());
            try {
                if (id != null) result.put(id, entry.getValue().getAsDouble());
            } catch (RuntimeException ignored) {
            }
        });
        return result;
    }

    public String toJson(
            String displayName,
            boolean enabled,
            int choices,
            int refreshes,
            int maxOwned,
            int iconSize,
            Map<ResourceLocation, Double> rarityWeights,
            Advanced advanced
    ) {
        root.addProperty("schema", 2);
        JsonElement existingName = root.get("name");
        if (existingName != null && existingName.isJsonObject()) {
            existingName.getAsJsonObject().addProperty("literal", displayName);
        } else {
            root.addProperty("name", displayName);
        }
        root.addProperty("enabled", enabled);
        root.addProperty("choices", choices);
        root.addProperty("refreshes", refreshes);
        root.addProperty("maxOwned", maxOwned);
        root.addProperty("iconSize", iconSize);
        root.addProperty("excludeOwned", advanced.duplicatePolicy().equals("exclude"));
        root.addProperty("duplicatePolicy", advanced.duplicatePolicy());
        JsonObject weights = new JsonObject();
        rarityWeights.forEach((id, weight) -> weights.addProperty(id.toString(), weight));
        root.add("rarityWeights", weights);
        putOptionalId("luckAttribute", advanced.luckAttribute());
        JsonObject factors = new JsonObject();
        advanced.luckFactors().forEach((id, factor) -> factors.addProperty(id.toString(), factor));
        root.add("luckFactors", factors);
        putOptionalId("pityRarity", advanced.pityRarity());
        root.addProperty("pityAfter", advanced.pityAfter());
        putOptionalId("duplicateCounter", advanced.duplicateCounter());
        root.addProperty("duplicateCounterAmount", advanced.duplicateCounterAmount());
        return GSON.toJson(root);
    }

    public String toJson(
            String displayName,
            boolean enabled,
            int choices,
            int refreshes,
            int maxOwned,
            Map<ResourceLocation, Double> rarityWeights,
            Advanced advanced
    ) {
        return toJson(displayName, enabled, choices, refreshes, maxOwned, iconSize(), rarityWeights, advanced);
    }

    public String toJson(
            String displayName,
            boolean enabled,
            int choices,
            int refreshes,
            int maxOwned,
            boolean excludeOwned,
            Map<ResourceLocation, Double> rarityWeights
    ) {
        Advanced current = advanced();
        String policy = excludeOwned ? "exclude" : "refund_token";
        return toJson(displayName, enabled, choices, refreshes, maxOwned, iconSize(), rarityWeights,
                new Advanced(current.luckAttribute(), current.luckFactors(), current.pityRarity(),
                        current.pityAfter(), policy, current.duplicateCounter(), current.duplicateCounterAmount()));
    }

    public void updateAdvanced(Advanced advanced) {
        putOptionalId("luckAttribute", advanced.luckAttribute());
        JsonObject factors = new JsonObject();
        advanced.luckFactors().forEach((id, factor) -> factors.addProperty(id.toString(), factor));
        root.add("luckFactors", factors);
        putOptionalId("pityRarity", advanced.pityRarity());
        root.addProperty("pityAfter", advanced.pityAfter());
        root.addProperty("duplicatePolicy", advanced.duplicatePolicy());
        root.addProperty("excludeOwned", advanced.duplicatePolicy().equals("exclude"));
        putOptionalId("duplicateCounter", advanced.duplicateCounter());
        root.addProperty("duplicateCounterAmount", advanced.duplicateCounterAmount());
    }

    private Optional<ResourceLocation> optionalId(String field) {
        String value = string(root, field);
        return Optional.ofNullable(value.isBlank() ? null : ResourceLocation.tryParse(value));
    }

    private void putOptionalId(String field, Optional<ResourceLocation> value) {
        if (value.isPresent()) root.addProperty(field, value.orElseThrow().toString());
        else root.remove(field);
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

    private static long longInteger(JsonObject object, String key, long fallback) {
        JsonElement element = object.get(key);
        try {
            return element == null ? fallback : element.getAsLong();
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

    public record Advanced(
            Optional<ResourceLocation> luckAttribute,
            Map<ResourceLocation, Double> luckFactors,
            Optional<ResourceLocation> pityRarity,
            int pityAfter,
            String duplicatePolicy,
            Optional<ResourceLocation> duplicateCounter,
            long duplicateCounterAmount
    ) {
        public Advanced {
            luckAttribute = luckAttribute == null ? Optional.empty() : luckAttribute;
            luckFactors = Map.copyOf(new LinkedHashMap<>(luckFactors));
            pityRarity = pityRarity == null ? Optional.empty() : pityRarity;
            duplicateCounter = duplicateCounter == null ? Optional.empty() : duplicateCounter;
        }
    }
}
