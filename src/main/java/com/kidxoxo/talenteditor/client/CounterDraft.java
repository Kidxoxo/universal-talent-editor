package com.kidxoxo.talenteditor.client;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.List;

public final class CounterDraft {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    String snapshot() { return root.toString(); }
    private final JsonObject root;

    private CounterDraft(JsonObject root) {
        this.root = root.deepCopy();
    }

    public static CounterDraft create() {
        JsonObject root = new JsonObject();
        root.addProperty("schema", 2);
        root.addProperty("enabled", true);
        root.addProperty("name", "");
        root.addProperty("event", "kill");
        root.addProperty("increment", 1L);
        root.addProperty("amountMode", "fixed");
        root.addProperty("amountScale", 1.0D);
        root.addProperty("rounding", "round");
        root.addProperty("minimumIncrement", 0L);
        root.addProperty("maximum", 1_000_000_000_000L);
        root.addProperty("cooldownTicks", 0);
        root.addProperty("intervalTicks", 20);
        root.addProperty("resetOnLogin", false);
        root.addProperty("resetOnDeath", false);
        root.addProperty("resetIntervalTicks", 0);
        root.addProperty("visibility", "full");
        for (String field : List.of(
                "categories", "requiredMods", "requiredTalents", "entityTypes", "entityTags", "items",
                "itemTags", "damageTags"
        )) root.add(field, new JsonArray());
        return new CounterDraft(root);
    }

    public static CounterDraft parse(String json) {
        JsonElement parsed = JsonParser.parseString(json);
        if (!parsed.isJsonObject()) throw new IllegalArgumentException("Counter root must be an object");
        return new CounterDraft(parsed.getAsJsonObject());
    }

    public String name() {
        JsonElement value = root.get("name");
        if (value == null) return "";
        if (value.isJsonPrimitive() && value.getAsJsonPrimitive().isString()) return value.getAsString();
        if (!value.isJsonObject()) return "";
        String literal = string(value.getAsJsonObject(), "literal");
        return literal.isBlank() ? string(value.getAsJsonObject(), "translation") : literal;
    }

    public boolean enabled() {
        return bool("enabled", true);
    }

    public String event() {
        return string(root, "event", "kill");
    }

    public long increment() {
        return number("increment", 1L);
    }

    public AmountSettings amountSettings() {
        return new AmountSettings(
                string(root, "amountMode", "fixed"), decimal("amountScale", 1.0D),
                string(root, "rounding", "round"), number("minimumIncrement", 0L)
        );
    }

    public void updateAmountSettings(AmountSettings value) {
        root.addProperty("amountMode", value.mode());
        root.addProperty("amountScale", value.scale());
        root.addProperty("rounding", value.rounding());
        root.addProperty("minimumIncrement", value.minimumIncrement());
    }

    public long maximum() {
        return number("maximum", 1_000_000_000_000L);
    }

    public int cooldownTicks() {
        return (int) number("cooldownTicks", 0L);
    }

    public int intervalTicks() {
        return (int) number("intervalTicks", 20L);
    }

    public ResetSettings resetSettings() {
        return new ResetSettings(
                bool("resetOnLogin", false), bool("resetOnDeath", false),
                (int) number("resetIntervalTicks", 0L)
        );
    }

    public MetadataSettings metadataSettings() {
        String rawDisplayCategory = string(root, "displayCategory", "").strip();
        ResourceLocation displayCategory = rawDisplayCategory.isEmpty()
                ? null : ResourceLocation.tryParse(rawDisplayCategory);
        return new MetadataSettings(ids("categories"), string(root, "visibility", "full"), displayCategory);
    }

    public List<String> requiredMods() {
        return strings("requiredMods");
    }

    public List<ResourceLocation> requiredTalents() {
        return ids("requiredTalents");
    }

    public Filters filters() {
        return new Filters(ids("entityTypes"), ids("entityTags"), ids("items"), ids("itemTags"), ids("damageTags"));
    }

    public String toJson(
            String displayName,
            boolean enabled,
            String event,
            long increment,
            long maximum,
            int cooldownTicks,
            int intervalTicks,
            ResetSettings resetSettings,
            MetadataSettings metadataSettings,
            List<String> requiredMods,
            List<ResourceLocation> requiredTalents,
            Filters filters
    ) {
        root.addProperty("schema", 2);
        JsonElement existingName = root.get("name");
        if (existingName != null && existingName.isJsonObject()) {
            existingName.getAsJsonObject().addProperty("literal", displayName);
        } else {
            root.addProperty("name", displayName);
        }
        root.addProperty("enabled", enabled);
        root.addProperty("event", event);
        root.addProperty("increment", increment);
        root.addProperty("maximum", maximum);
        root.addProperty("cooldownTicks", cooldownTicks);
        root.addProperty("intervalTicks", intervalTicks);
        root.addProperty("resetOnLogin", resetSettings.onLogin());
        root.addProperty("resetOnDeath", resetSettings.onDeath());
        root.addProperty("resetIntervalTicks", resetSettings.intervalTicks());
        putIds("categories", metadataSettings.categories());
        root.addProperty("visibility", metadataSettings.visibility());
        if (metadataSettings.displayCategory() == null) root.remove("displayCategory");
        else root.addProperty("displayCategory", metadataSettings.displayCategory().toString());
        putStrings("requiredMods", requiredMods);
        putIds("requiredTalents", requiredTalents);
        putIds("entityTypes", filters.entityTypes());
        putIds("entityTags", filters.entityTags());
        putIds("items", filters.items());
        putIds("itemTags", filters.itemTags());
        putIds("damageTags", filters.damageTags());
        return GSON.toJson(root);
    }

    private void putIds(String field, List<ResourceLocation> values) {
        JsonArray array = new JsonArray();
        values.forEach(value -> array.add(value.toString()));
        root.add(field, array);
    }

    private void putStrings(String field, List<String> values) {
        JsonArray array = new JsonArray();
        values.forEach(array::add);
        root.add(field, array);
    }

    private List<ResourceLocation> ids(String field) {
        ArrayList<ResourceLocation> result = new ArrayList<>();
        for (String value : strings(field)) {
            ResourceLocation id = ResourceLocation.tryParse(value);
            if (id != null) result.add(id);
        }
        return List.copyOf(result);
    }

    private List<String> strings(String field) {
        JsonElement value = root.get(field);
        if (value == null || !value.isJsonArray()) return List.of();
        ArrayList<String> result = new ArrayList<>();
        for (JsonElement element : value.getAsJsonArray()) {
            if (element.isJsonPrimitive() && element.getAsJsonPrimitive().isString()) result.add(element.getAsString());
        }
        return List.copyOf(result);
    }

    private boolean bool(String field, boolean fallback) {
        try {
            return root.has(field) ? root.get(field).getAsBoolean() : fallback;
        } catch (RuntimeException ignored) {
            return fallback;
        }
    }

    private long number(String field, long fallback) {
        try {
            return root.has(field) ? root.get(field).getAsLong() : fallback;
        } catch (RuntimeException ignored) {
            return fallback;
        }
    }

    private double decimal(String field, double fallback) {
        try {
            return root.has(field) ? root.get(field).getAsDouble() : fallback;
        } catch (RuntimeException ignored) {
            return fallback;
        }
    }

    private static String string(JsonObject object, String field) {
        return string(object, field, "");
    }

    private static String string(JsonObject object, String field, String fallback) {
        try {
            return object.has(field) ? object.get(field).getAsString() : fallback;
        } catch (RuntimeException ignored) {
            return fallback;
        }
    }

    public record Filters(
            List<ResourceLocation> entityTypes,
            List<ResourceLocation> entityTags,
            List<ResourceLocation> items,
            List<ResourceLocation> itemTags,
            List<ResourceLocation> damageTags
    ) {
        public Filters {
            entityTypes = List.copyOf(entityTypes);
            entityTags = List.copyOf(entityTags);
            items = List.copyOf(items);
            itemTags = List.copyOf(itemTags);
            damageTags = List.copyOf(damageTags);
        }
    }

    public record AmountSettings(String mode, double scale, String rounding, long minimumIncrement) {
        public AmountSettings {
            mode = mode == null ? "fixed" : mode;
            rounding = rounding == null ? "round" : rounding;
        }
    }

    public record ResetSettings(boolean onLogin, boolean onDeath, int intervalTicks) {
    }

    public record MetadataSettings(
            List<ResourceLocation> categories,
            String visibility,
            ResourceLocation displayCategory
    ) {
        public MetadataSettings(List<ResourceLocation> categories, String visibility) {
            this(categories, visibility, null);
        }

        public MetadataSettings {
            categories = List.copyOf(categories);
            visibility = visibility == null ? "full" : visibility;
        }
    }
}
