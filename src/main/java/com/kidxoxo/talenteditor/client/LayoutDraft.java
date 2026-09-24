package com.kidxoxo.talenteditor.client;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonArray;
import com.google.gson.JsonParser;

import java.util.ArrayList;
import java.util.List;

final class LayoutDraft {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private final JsonObject root;

    private LayoutDraft(JsonObject root) {
        this.root = root;
    }

    static LayoutDraft create() {
        JsonObject root = new JsonObject();
        root.addProperty("schema", 2);
        return new LayoutDraft(root);
    }

    static LayoutDraft parse(String json) {
        JsonElement parsed = JsonParser.parseString(json);
        if (!parsed.isJsonObject()) throw new IllegalArgumentException("Layout definition must be an object");
        return new LayoutDraft(parsed.getAsJsonObject().deepCopy());
    }

    boolean enabled() { return bool("enabled", true); }
    int priority() { return integer("priority", 0); }
    String background() { return string("background", ""); }
    int panelPercent() { return integer("panelPercent", 60); }
    int columns() { return integer("columns", 2); }
    int rows() { return integer("rows", 3); }
    int slotWidth() { return integer("slotWidth", 120); }
    int slotHeight() { return integer("slotHeight", 56); }
    int iconSize() { return integer("iconSize", 48); }
    boolean showGeneratedEffects() { return bool("showGeneratedEffects", true); }

    List<SlotDraft> slots() {
        if (!root.has("slots") || !root.get("slots").isJsonArray()) return List.of();
        return parseSlots(root.getAsJsonArray("slots"));
    }

    List<PageDraft> pages() {
        if (!root.has("pages") || !root.get("pages").isJsonArray()) return List.of();
        ArrayList<PageDraft> result = new ArrayList<>();
        JsonArray pages = root.getAsJsonArray("pages");
        for (int index = 0; index < pages.size(); index++) {
            if (!pages.get(index).isJsonObject()) continue;
            JsonObject page = pages.get(index).getAsJsonObject();
            try {
                String id = page.has("id") ? page.get("id").getAsString() : "page_" + index;
                String background = page.has("background") ? page.get("background").getAsString() : "";
                List<SlotDraft> pageSlots = page.has("slots") && page.get("slots").isJsonArray()
                        ? parseSlots(page.getAsJsonArray("slots")) : List.of();
                result.add(new PageDraft(id, background, pageSlots));
            } catch (RuntimeException ignored) {
                // The authoritative server compiler will report malformed page fields on validation.
            }
        }
        return List.copyOf(result);
    }

    private static List<SlotDraft> parseSlots(JsonArray slots) {
        ArrayList<SlotDraft> result = new ArrayList<>();
        for (int index = 0; index < slots.size(); index++) {
            if (!slots.get(index).isJsonObject()) continue;
            JsonObject slot = slots.get(index).getAsJsonObject();
            try {
                result.add(new SlotDraft(
                        slot.has("id") ? slot.get("id").getAsString() : "slot_" + index,
                        slot.has("x") ? slot.get("x").getAsInt() : 0,
                        slot.has("y") ? slot.get("y").getAsInt() : 0,
                        slot.has("width") ? slot.get("width").getAsInt() : 58,
                        slot.has("height") ? slot.get("height").getAsInt() : 30,
                        slot.has("iconSize") ? slot.get("iconSize").getAsInt() : 16,
                        slot.has("talent") ? slot.get("talent").getAsString() : ""
                ));
            } catch (RuntimeException ignored) {
                // The server compiler is authoritative. A malformed slot remains visible through validation.
            }
        }
        return List.copyOf(result);
    }

    String toJson(
            boolean enabled,
            int priority,
            String background,
            int panelPercent,
            int columns,
            int rows,
            int slotWidth,
            int slotHeight,
            int iconSize,
            boolean showGeneratedEffects,
            List<SlotDraft> slots,
            List<PageDraft> pages
    ) {
        JsonObject next = root.deepCopy();
        next.addProperty("schema", 2);
        next.addProperty("enabled", enabled);
        next.addProperty("priority", priority);
        if (background.isBlank()) next.remove("background");
        else next.addProperty("background", background);
        next.addProperty("panelPercent", panelPercent);
        next.addProperty("columns", columns);
        next.addProperty("rows", rows);
        next.addProperty("slotWidth", slotWidth);
        next.addProperty("slotHeight", slotHeight);
        next.addProperty("iconSize", iconSize);
        next.addProperty("showGeneratedEffects", showGeneratedEffects);
        if (slots.isEmpty()) {
            next.remove("slots");
        } else {
            JsonArray array = new JsonArray();
            for (SlotDraft slot : slots) {
                JsonObject value = new JsonObject();
                value.addProperty("id", slot.id());
                value.addProperty("x", slot.x());
                value.addProperty("y", slot.y());
                value.addProperty("width", slot.width());
                value.addProperty("height", slot.height());
                value.addProperty("iconSize", slot.iconSize());
                if (!slot.talent().isBlank()) value.addProperty("talent", slot.talent());
                array.add(value);
            }
            next.add("slots", array);
        }
        if (pages.isEmpty()) {
            next.remove("pages");
        } else {
            JsonArray array = new JsonArray();
            for (PageDraft page : pages) {
                JsonObject value = new JsonObject();
                value.addProperty("id", page.id());
                if (!page.background().isBlank()) value.addProperty("background", page.background());
                JsonArray pageSlots = new JsonArray();
                for (SlotDraft slot : page.slots()) {
                    JsonObject slotValue = new JsonObject();
                    slotValue.addProperty("id", slot.id());
                    slotValue.addProperty("x", slot.x());
                    slotValue.addProperty("y", slot.y());
                    slotValue.addProperty("width", slot.width());
                    slotValue.addProperty("height", slot.height());
                    slotValue.addProperty("iconSize", slot.iconSize());
                    if (!slot.talent().isBlank()) slotValue.addProperty("talent", slot.talent());
                    pageSlots.add(slotValue);
                }
                value.add("slots", pageSlots);
                array.add(value);
            }
            next.add("pages", array);
        }
        return GSON.toJson(next);
    }

    private boolean bool(String key, boolean fallback) {
        try { return root.has(key) ? root.get(key).getAsBoolean() : fallback; }
        catch (RuntimeException ignored) { return fallback; }
    }

    private int integer(String key, int fallback) {
        try { return root.has(key) ? root.get(key).getAsInt() : fallback; }
        catch (RuntimeException ignored) { return fallback; }
    }

    private String string(String key, String fallback) {
        try { return root.has(key) ? root.get(key).getAsString() : fallback; }
        catch (RuntimeException ignored) { return fallback; }
    }

    record SlotDraft(String id, int x, int y, int width, int height, int iconSize, String talent) {
        SlotDraft {
            talent = talent == null ? "" : talent;
        }

        SlotDraft(String id, int x, int y, int width, int height, int iconSize) {
            this(id, x, y, width, height, iconSize, "");
        }

        SlotDraft withGeometry(int x, int y, int width, int height, int iconSize) {
            return new SlotDraft(id, x, y, width, height, iconSize, talent);
        }

        SlotDraft withTalent(String talent) {
            return new SlotDraft(id, x, y, width, height, iconSize, talent);
        }

        com.kidxoxo.talenteditor.domain.TalentBookLayoutDefinition.SlotDefinition definition() {
            return new com.kidxoxo.talenteditor.domain.TalentBookLayoutDefinition.SlotDefinition(
                    id, x, y, width, height, iconSize, talent.isBlank() ? java.util.Optional.empty()
                            : java.util.Optional.of(new net.minecraft.resources.ResourceLocation(talent)));
        }
    }

    record PageDraft(String id, String background, List<SlotDraft> slots) {
        PageDraft {
            background = background == null ? "" : background;
            slots = slots == null ? List.of() : List.copyOf(slots);
        }
    }
}
