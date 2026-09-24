package com.kidxoxo.talenteditor.network;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/** Compact relationship data used by the visual talent-tree editor. */
record TalentRelationSummary(
        List<ResourceLocation> acquireTalents,
        boolean acquireEditable,
        String acquireMode,
        int acquireOtherConditions
) {
    private static final Set<String> SIMPLE_TYPES = Set.of(
            "has_talent", "counter_at_least", "counter_category_total_at_least",
            "category_count_at_least", "has_item"
    );

    TalentRelationSummary {
        acquireTalents = List.copyOf(acquireTalents);
        acquireMode = acquireMode == null || acquireMode.isBlank() ? "all" : acquireMode;
        acquireOtherConditions = Math.max(0, acquireOtherConditions);
    }

    static TalentRelationSummary from(JsonObject talent) {
        JsonElement raw = talent == null ? null : talent.get("acquireCondition");
        if (raw == null) return new TalentRelationSummary(List.of(), true, "all", 0);
        LinkedHashSet<ResourceLocation> relations = new LinkedHashSet<>();
        int[] other = {0};
        collect(raw, false, relations, other);
        return new TalentRelationSummary(
                new ArrayList<>(relations), editable(raw), mode(raw), other[0]
        );
    }

    private static boolean editable(JsonElement raw) {
        if (!raw.isJsonObject()) return false;
        JsonObject condition = raw.getAsJsonObject();
        String type = localType(condition);
        if (type.equals("all") || type.equals("any")) {
            if (!condition.keySet().equals(Set.of("type", "children"))) return false;
            JsonElement children = condition.get("children");
            if (children == null || !children.isJsonArray()) return false;
            for (JsonElement child : children.getAsJsonArray()) {
                if (!simpleLeaf(child)) return false;
            }
            return true;
        }
        return simpleLeaf(raw);
    }

    private static boolean simpleLeaf(JsonElement raw) {
        if (!raw.isJsonObject()) return false;
        JsonObject condition = raw.getAsJsonObject();
        String type = localType(condition);
        if (!SIMPLE_TYPES.contains(type)) return false;
        return switch (type) {
            case "has_talent" -> condition.keySet().equals(Set.of("type", "talent"));
            case "counter_at_least" -> condition.keySet().equals(Set.of("type", "counter", "value"));
            case "counter_category_total_at_least" ->
                    condition.keySet().equals(Set.of("type", "category", "value"));
            case "category_count_at_least" ->
                    condition.keySet().equals(Set.of("type", "category", "count"));
            case "has_item" -> condition.keySet().equals(Set.of("type", "item", "count"));
            default -> false;
        };
    }

    private static String mode(JsonElement raw) {
        if (!raw.isJsonObject()) return "advanced";
        String type = localType(raw.getAsJsonObject());
        return type.equals("all") || type.equals("any") ? type
                : SIMPLE_TYPES.contains(type) ? "all" : "advanced";
    }

    private static void collect(
            JsonElement raw,
            boolean negated,
            LinkedHashSet<ResourceLocation> relations,
            int[] other
    ) {
        if (raw == null || !raw.isJsonObject()) {
            other[0]++;
            return;
        }
        JsonObject condition = raw.getAsJsonObject();
        String type = localType(condition);
        if (type.equals("all") || type.equals("any")) {
            JsonElement children = condition.get("children");
            if (children == null || !children.isJsonArray()) {
                other[0]++;
                return;
            }
            for (JsonElement child : children.getAsJsonArray()) collect(child, negated, relations, other);
            return;
        }
        if (type.equals("not")) {
            JsonElement child = condition.get("child");
            if (child == null) other[0]++;
            else collect(child, !negated, relations, other);
            return;
        }
        if (type.equals("has_talent") && !negated) {
            ResourceLocation id = resource(condition.get("talent"));
            if (id != null) relations.add(id); else other[0]++;
            return;
        }
        other[0]++;
    }

    private static ResourceLocation resource(JsonElement raw) {
        try {
            return raw != null && raw.isJsonPrimitive()
                    ? ResourceLocation.tryParse(raw.getAsString()) : null;
        } catch (RuntimeException ignored) {
            return null;
        }
    }

    private static String localType(JsonObject object) {
        try {
            String value = object.has("type") ? object.get("type").getAsString() : "";
            int separator = value.indexOf(':');
            return separator < 0 ? value : value.substring(separator + 1);
        } catch (RuntimeException ignored) {
            return "";
        }
    }
}
