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
import java.util.Optional;
import java.util.Set;

public final class ConfiguredItemDraft {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    String snapshot() { return root.toString(); }
    private final JsonObject root;

    private ConfiguredItemDraft(JsonObject root) {
        this.root = root.deepCopy();
    }

    public static ConfiguredItemDraft create() {
        JsonObject root = new JsonObject();
        root.addProperty("schema", 2);
        root.addProperty("enabled", true);
        root.addProperty("name", "");
        root.add("description", new JsonArray());
        root.addProperty("customModelData", 0);
        root.addProperty("glint", false);
        root.addProperty("curiosEquippable", true);
        root.add("curiosSlots", new JsonArray());
        root.addProperty("mainhandOnly", false);
        root.add("attributeModifiers", new JsonArray());
        JsonObject use = new JsonObject();
        use.addProperty("enabled", false);
        use.addProperty("cooldownTicks", 0);
        use.addProperty("consumeCount", 0);
        use.add("actions", new JsonArray());
        root.add("use", use);
        root.add("triggers", new JsonArray());
        root.add("loot", new JsonArray());
        root.add("requiredMods", new JsonArray());
        return new ConfiguredItemDraft(root);
    }

    public static ConfiguredItemDraft parse(String json) {
        JsonElement parsed = JsonParser.parseString(json);
        if (!parsed.isJsonObject()) throw new IllegalArgumentException("Configured item root must be an object");
        return new ConfiguredItemDraft(parsed.getAsJsonObject());
    }

    public String name() {
        return LocalizedTextDraft.parse(root.get("name")).fallbackText();
    }

    public String descriptionEditorText() {
        return descriptionElements().stream()
                .map(LocalizedTextDraft::parse)
                .map(LocalizedTextDraft::fallbackText)
                .map(value -> value.replace("\\", "\\\\").replace("\n", "\\n"))
                .reduce((left, right) -> left + "\\n" + right)
                .orElse("");
    }

    public boolean enabled() {
        return booleanValue("enabled", true);
    }

    public int customModelData() {
        try {
            return root.has("customModelData") ? root.get("customModelData").getAsInt() : 0;
        } catch (RuntimeException ignored) {
            return 0;
        }
    }

    public boolean glint() {
        return booleanValue("glint", false);
    }

    public boolean curiosEquippable() {
        return booleanValue("curiosEquippable", true);
    }

    public boolean mainhandOnly() {
        return booleanValue("mainhandOnly", false);
    }

    /** Empty means any Curios slot, for compatibility with existing definitions. */
    public List<String> curiosSlots() {
        JsonElement raw = root.get("curiosSlots");
        if (raw == null || !raw.isJsonArray()) return List.of();
        ArrayList<String> slots = new ArrayList<>();
        for (JsonElement element : raw.getAsJsonArray()) {
            if (element.isJsonPrimitive() && element.getAsJsonPrimitive().isString()) {
                slots.add(element.getAsString());
            }
        }
        return List.copyOf(slots);
    }

    public String requiredMods() {
        JsonElement element = root.get("requiredMods");
        if (element == null || !element.isJsonArray()) return "";
        ArrayList<String> values = new ArrayList<>();
        for (JsonElement child : element.getAsJsonArray()) {
            if (child.isJsonPrimitive() && child.getAsJsonPrimitive().isString()) values.add(child.getAsString());
        }
        return String.join(", ", values);
    }

    public List<LootRuleDraft> lootRules() {
        JsonElement element = root.get("loot");
        if (element == null || !element.isJsonArray()) return List.of();
        ArrayList<LootRuleDraft> result = new ArrayList<>();
        for (JsonElement child : element.getAsJsonArray()) {
            if (child.isJsonObject()) result.add(LootRuleDraft.parse(child.getAsJsonObject()));
        }
        return List.copyOf(result);
    }

    public List<JsonObject> triggers() {
        JsonElement element = root.get("triggers");
        if (element == null || !element.isJsonArray()) return List.of();
        ArrayList<JsonObject> result = new ArrayList<>();
        for (JsonElement child : element.getAsJsonArray()) {
            if (child.isJsonObject()) result.add(child.getAsJsonObject().deepCopy());
        }
        return List.copyOf(result);
    }

    public void replaceTriggers(List<JsonObject> triggers) {
        JsonArray array = new JsonArray();
        triggers.forEach(trigger -> array.add(trigger.deepCopy()));
        root.add("triggers", array);
    }

    public List<AttributeModifierDraft> attributeModifiers() {
        JsonElement element = root.get("attributeModifiers");
        if (element == null || !element.isJsonArray()) return List.of();
        ArrayList<AttributeModifierDraft> result = new ArrayList<>();
        for (JsonElement child : element.getAsJsonArray()) {
            if (child.isJsonObject()) result.add(AttributeModifierDraft.parse(child.getAsJsonObject()));
        }
        return List.copyOf(result);
    }

    public void replaceAttributeModifiers(List<AttributeModifierDraft> modifiers) {
        JsonArray array = new JsonArray();
        modifiers.forEach(modifier -> array.add(modifier.toJsonObject()));
        root.add("attributeModifiers", array);
    }

    public UseDraft use() {
        JsonElement raw = root.get("use");
        if (raw == null) return UseDraft.defaults();
        if (!raw.isJsonObject()) return UseDraft.unsupported();
        JsonObject use = raw.getAsJsonObject();
        if (!Set.of("enabled", "cooldownTicks", "consumeCount", "actions").containsAll(use.keySet())) {
            return UseDraft.unsupported();
        }
        JsonElement rawActions = use.get("actions");
        if (rawActions != null && !rawActions.isJsonArray()) return UseDraft.unsupported();
        ArrayList<UseActionDraft> actions = new ArrayList<>();
        if (rawActions != null) {
            for (JsonElement element : rawActions.getAsJsonArray()) {
                if (!element.isJsonObject()) return UseDraft.unsupported();
                UseActionDraft action = UseActionDraft.parse(element.getAsJsonObject());
                if (action == null) return UseDraft.unsupported();
                actions.add(action);
            }
        }
        return new UseDraft(
                true, bool(use, "enabled", true), integer(use, "cooldownTicks", 0),
                integer(use, "consumeCount", 0), List.copyOf(actions)
        );
    }

    public void replaceUse(UseDraft draft) {
        if (!draft.editable()) throw new IllegalArgumentException("Configured item use definition is read-only");
        JsonObject use = new JsonObject();
        use.addProperty("enabled", draft.enabled());
        use.addProperty("cooldownTicks", draft.cooldownTicks());
        use.addProperty("consumeCount", draft.consumeCount());
        JsonArray actions = new JsonArray();
        draft.actions().forEach(action -> actions.add(action.toJsonObject()));
        use.add("actions", actions);
        root.add("use", use);
    }

    public void replaceLootRules(List<LootRuleDraft> rules) {
        JsonArray array = new JsonArray();
        rules.forEach(rule -> array.add(rule.toJsonObject()));
        root.add("loot", array);
    }

    public String toJson(
            String displayName,
            String descriptionText,
            boolean enabled,
            int customModelData,
            boolean glint,
            boolean curiosEquippable,
            boolean mainhandOnly,
            List<String> curiosSlots,
            List<String> requiredMods
    ) {
        root.addProperty("schema", 2);
        root.add("name", updateLocalized(root.get("name"), name(), displayName));
        List<JsonElement> previous = descriptionElements();
        List<String> lines = decodeDescription(descriptionText);
        JsonArray description = new JsonArray();
        for (int index = 0; index < lines.size(); index++) {
            JsonElement old = index < previous.size() ? previous.get(index) : null;
            String oldText = old == null ? "" : LocalizedTextDraft.parse(old).fallbackText();
            description.add(updateLocalized(old, oldText, lines.get(index)));
        }
        root.add("description", description);
        root.addProperty("enabled", enabled);
        root.addProperty("customModelData", customModelData);
        root.addProperty("glint", glint);
        root.addProperty("curiosEquippable", curiosEquippable);
        root.addProperty("mainhandOnly", mainhandOnly);
        JsonArray slots = new JsonArray();
        curiosSlots.forEach(slots::add);
        root.add("curiosSlots", slots);
        JsonArray mods = new JsonArray();
        requiredMods.forEach(mods::add);
        root.add("requiredMods", mods);
        return GSON.toJson(root);
    }

    public String toJson(String displayName, String descriptionText, boolean enabled,
                         int customModelData, boolean glint, boolean curiosEquippable,
                         boolean mainhandOnly, List<String> requiredMods) {
        return toJson(displayName, descriptionText, enabled, customModelData, glint,
                curiosEquippable, mainhandOnly, curiosSlots(), requiredMods);
    }

    public String toJson(String displayName, String descriptionText, boolean enabled,
                         int customModelData, boolean glint, List<String> requiredMods) {
        return toJson(displayName, descriptionText, enabled, customModelData, glint,
                curiosEquippable(), mainhandOnly(), curiosSlots(), requiredMods);
    }

    static List<String> decodeDescription(String value) {
        if (value == null || value.isBlank()) return List.of();
        ArrayList<String> result = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean escaped = false;
        for (int index = 0; index < value.length(); index++) {
            char character = value.charAt(index);
            if (escaped) {
                if (character == 'n') {
                    String line = current.toString().strip();
                    if (!line.isBlank()) result.add(line);
                    current.setLength(0);
                } else {
                    current.append(character);
                }
                escaped = false;
            } else if (character == '\\') {
                escaped = true;
            } else {
                current.append(character);
            }
        }
        if (escaped) current.append('\\');
        String line = current.toString().strip();
        if (!line.isBlank()) result.add(line);
        return List.copyOf(result);
    }

    private List<JsonElement> descriptionElements() {
        JsonElement element = root.get("description");
        if (element == null) return List.of();
        if (!element.isJsonArray()) return List.of(element.deepCopy());
        ArrayList<JsonElement> values = new ArrayList<>();
        element.getAsJsonArray().forEach(child -> values.add(child.deepCopy()));
        return List.copyOf(values);
    }

    private static JsonElement updateLocalized(JsonElement previous, String previousText, String nextText) {
        if (previous != null && previousText.equals(nextText)) return previous.deepCopy();
        if (previous != null && previous.isJsonObject()) {
            JsonObject object = previous.getAsJsonObject().deepCopy();
            object.addProperty("literal", nextText);
            return object;
        }
        return new com.google.gson.JsonPrimitive(nextText);
    }

    private boolean booleanValue(String key, boolean fallback) {
        try {
            return root.has(key) ? root.get(key).getAsBoolean() : fallback;
        } catch (RuntimeException ignored) {
            return fallback;
        }
    }

    private static boolean bool(JsonObject object, String key, boolean fallback) {
        try {
            return object.has(key) ? object.get(key).getAsBoolean() : fallback;
        } catch (RuntimeException ignored) {
            return fallback;
        }
    }

    private static int integer(JsonObject object, String key, int fallback) {
        try {
            return object.has(key) ? object.get(key).getAsInt() : fallback;
        } catch (RuntimeException ignored) {
            return fallback;
        }
    }

    private static double decimal(JsonObject object, String key, double fallback) {
        try {
            return object.has(key) ? object.get(key).getAsDouble() : fallback;
        } catch (RuntimeException ignored) {
            return fallback;
        }
    }

    private static String string(JsonObject object, String key, String fallback) {
        try {
            return object.has(key) ? object.get(key).getAsString() : fallback;
        } catch (RuntimeException ignored) {
            return fallback;
        }
    }

    public static final class LootRuleDraft {
        private final JsonObject root;

        private LootRuleDraft(JsonObject root) {
            this.root = root.deepCopy();
        }

        public static LootRuleDraft create(String table) {
            JsonObject root = new JsonObject();
            root.addProperty("table", table);
            root.addProperty("chance", 0.05D);
            root.addProperty("minCount", 1);
            root.addProperty("maxCount", 1);
            return new LootRuleDraft(root);
        }

        private static LootRuleDraft parse(JsonObject root) {
            return new LootRuleDraft(root);
        }

        public String table() {
            try {
                return root.has("table") ? root.get("table").getAsString() : "";
            } catch (RuntimeException ignored) {
                return "";
            }
        }

        public String tableMatch() {
            return string("tableMatch", "exact");
        }

        public double chance() {
            try {
                return root.has("chance") ? root.get("chance").getAsDouble() : 1.0D;
            } catch (RuntimeException ignored) {
                return 1.0D;
            }
        }

        public double chancePerLuck() {
            try {
                return root.has("chancePerLuck") ? root.get("chancePerLuck").getAsDouble() : 0.0D;
            } catch (RuntimeException ignored) {
                return 0.0D;
            }
        }

        public int minCount() {
            return integer("minCount", 1);
        }

        public int maxCount() {
            return integer("maxCount", minCount());
        }

        public List<ResourceLocation> dimensions() {
            return idList("dimensions");
        }

        public String entityTarget() {
            return string("entityTarget", "this_entity");
        }

        public List<ResourceLocation> entityTypes() {
            return idList("entityTypes");
        }

        public List<ResourceLocation> entityTags() {
            return idList("entityTags");
        }

        public LootRuleDraft withValues(ResourceLocation table, double chance, int minimum, int maximum) {
            return withValues("exact", table.toString(), chance, minimum, maximum);
        }

        public LootRuleDraft withValues(
                String tableMatch,
                String tableExpression,
                double chance,
                int minimum,
                int maximum
        ) {
            JsonObject copy = root.deepCopy();
            copy.addProperty("tableMatch", tableMatch);
            copy.addProperty("table", tableExpression);
            copy.addProperty("chance", chance);
            copy.addProperty("minCount", minimum);
            copy.addProperty("maxCount", maximum);
            return new LootRuleDraft(copy);
        }

        public LootRuleDraft withConditions(
                double chancePerLuck,
                List<ResourceLocation> dimensions,
                String entityTarget,
                List<ResourceLocation> entityTypes,
                List<ResourceLocation> entityTags
        ) {
            JsonObject copy = root.deepCopy();
            copy.addProperty("chancePerLuck", chancePerLuck);
            copy.add("dimensions", idArray(dimensions));
            copy.addProperty("entityTarget", entityTarget);
            copy.add("entityTypes", idArray(entityTypes));
            copy.add("entityTags", idArray(entityTags));
            return new LootRuleDraft(copy);
        }

        boolean sameScope(LootRuleDraft other) {
            boolean noEntityFilter = entityTypes().isEmpty() && entityTags().isEmpty();
            boolean otherNoEntityFilter = other.entityTypes().isEmpty() && other.entityTags().isEmpty();
            return tableMatch().equals(other.tableMatch())
                    && table().equals(other.table())
                    && Set.copyOf(dimensions()).equals(Set.copyOf(other.dimensions()))
                    && (noEntityFilter && otherNoEntityFilter || entityTarget().equals(other.entityTarget()))
                    && Set.copyOf(entityTypes()).equals(Set.copyOf(other.entityTypes()))
                    && Set.copyOf(entityTags()).equals(Set.copyOf(other.entityTags()));
        }

        JsonObject toJsonObject() {
            return root.deepCopy();
        }

        private int integer(String key, int fallback) {
            try {
                return root.has(key) ? root.get(key).getAsInt() : fallback;
            } catch (RuntimeException ignored) {
                return fallback;
            }
        }

        private String string(String key, String fallback) {
            try {
                return root.has(key) ? root.get(key).getAsString() : fallback;
            } catch (RuntimeException ignored) {
                return fallback;
            }
        }

        private List<ResourceLocation> idList(String key) {
            JsonElement element = root.get(key);
            if (element == null || !element.isJsonArray()) return List.of();
            ArrayList<ResourceLocation> result = new ArrayList<>();
            for (JsonElement child : element.getAsJsonArray()) {
                try {
                    ResourceLocation id = ResourceLocation.tryParse(child.getAsString());
                    if (id != null) result.add(id);
                } catch (RuntimeException ignored) {
                }
            }
            return List.copyOf(result);
        }

        private static JsonArray idArray(List<ResourceLocation> values) {
            JsonArray array = new JsonArray();
            values.forEach(value -> array.add(value.toString()));
            return array;
        }
    }

    public static final class AttributeModifierDraft {
        private final JsonObject root;

        private AttributeModifierDraft(JsonObject root) {
            this.root = root.deepCopy();
        }

        public static AttributeModifierDraft create(String modifierKey) {
            JsonObject root = new JsonObject();
            root.addProperty("attribute", "minecraft:generic.max_health");
            root.addProperty("amount", 1.0D);
            root.addProperty("operation", "addition");
            JsonArray slots = new JsonArray();
            slots.add("mainhand");
            root.add("slots", slots);
            root.addProperty("modifierKey", modifierKey);
            return new AttributeModifierDraft(root);
        }

        private static AttributeModifierDraft parse(JsonObject root) {
            return new AttributeModifierDraft(root);
        }

        public String attribute() {
            return string("attribute", "");
        }

        public double amount() {
            try {
                return root.has("amount") ? root.get("amount").getAsDouble() : 0.0D;
            } catch (RuntimeException ignored) {
                return 0.0D;
            }
        }

        public String operation() {
            return string("operation", "addition");
        }

        public String slot() {
            return slots().get(0);
        }

        public List<String> slots() {
            if (root.has("slots") && root.get("slots").isJsonArray()) {
                ArrayList<String> values = new ArrayList<>();
                for (JsonElement element : root.getAsJsonArray("slots")) {
                    if (element.isJsonPrimitive() && element.getAsJsonPrimitive().isString()) {
                        values.add(element.getAsString());
                    }
                }
                if (!values.isEmpty()) return List.copyOf(values);
            }
            return List.of(string("slot", "mainhand"));
        }

        public String modifierKey() {
            return string("modifierKey", "");
        }

        public AttributeModifierDraft withValues(
                ResourceLocation attribute,
                double amount,
                String operation,
                List<String> slots,
                String modifierKey
        ) {
            JsonObject copy = root.deepCopy();
            copy.addProperty("attribute", attribute.toString());
            copy.addProperty("amount", amount);
            copy.addProperty("operation", operation);
            JsonArray slotArray = new JsonArray();
            slots.forEach(slotArray::add);
            copy.add("slots", slotArray);
            copy.remove("slot");
            copy.addProperty("modifierKey", modifierKey);
            return new AttributeModifierDraft(copy);
        }

        public AttributeModifierDraft withValues(
                ResourceLocation attribute, double amount, String operation,
                String slot, String modifierKey
        ) {
            return withValues(attribute, amount, operation, List.of(slot), modifierKey);
        }

        JsonObject toJsonObject() {
            return root.deepCopy();
        }

        private String string(String key, String fallback) {
            try {
                return root.has(key) ? root.get(key).getAsString() : fallback;
            } catch (RuntimeException ignored) {
                return fallback;
            }
        }
    }

    public record UseDraft(
            boolean editable,
            boolean enabled,
            int cooldownTicks,
            int consumeCount,
            List<UseActionDraft> actions
    ) {
        public UseDraft {
            actions = List.copyOf(actions);
        }

        static UseDraft defaults() {
            return new UseDraft(true, false, 0, 0, List.of());
        }

        static UseDraft unsupported() {
            return new UseDraft(false, false, 0, 0, List.of());
        }
    }

    public enum UseActionType {
        ADD_EFFECT("add_effect"),
        REMOVE_EFFECT("remove_effect"),
        PLAY_SOUND("play_sound"),
        SHOW_MESSAGE("show_message");

        private final String serializedName;

        UseActionType(String serializedName) {
            this.serializedName = serializedName;
        }

        public String serializedName() {
            return serializedName;
        }

        static Optional<UseActionType> parse(String raw) {
            String normalized = raw.contains(":") ? raw : "talent_editor:" + raw;
            for (UseActionType value : values()) {
                if (normalized.equals("talent_editor:" + value.serializedName)) return Optional.of(value);
            }
            return Optional.empty();
        }
    }

    public static final class UseActionDraft {
        private static final Set<String> ADD_EFFECT_FIELDS = Set.of(
                "type", "effect", "durationTicks", "amplifier", "target", "ambient", "showParticles", "showIcon"
        );
        private static final Set<String> REMOVE_EFFECT_FIELDS = Set.of("type", "effect", "target");
        private static final Set<String> PLAY_SOUND_FIELDS = Set.of("type", "sound", "source", "volume", "pitch");
        private static final Set<String> SHOW_MESSAGE_FIELDS = Set.of(
                "type", "mode", "text", "subtitle", "fadeInTicks", "stayTicks", "fadeOutTicks"
        );

        private final JsonObject root;
        private final UseActionType type;

        private UseActionDraft(JsonObject root, UseActionType type) {
            this.root = root.deepCopy();
            this.type = type;
        }

        public static UseActionDraft defaults(UseActionType type) {
            return switch (type) {
                case ADD_EFFECT -> effectAction(type, new ResourceLocation("minecraft", "speed"),
                        100, 0, false, true, true);
                case REMOVE_EFFECT -> effectAction(type, new ResourceLocation("minecraft", "speed"),
                        100, 0, false, true, true);
                case PLAY_SOUND -> soundAction(new ResourceLocation("minecraft", "entity.player.levelup"),
                        "players", 1.0D, 1.0D);
                case SHOW_MESSAGE -> messageAction("chat", LocalizedTextDraft.literal("Used"), false,
                        LocalizedTextDraft.literal(""), 10, 70, 20);
            };
        }

        private static UseActionDraft parse(JsonObject action) {
            Optional<UseActionType> parsed = UseActionType.parse(string(action, "type", ""));
            if (parsed.isEmpty()) return null;
            UseActionType type = parsed.orElseThrow();
            Set<String> allowed = switch (type) {
                case ADD_EFFECT -> ADD_EFFECT_FIELDS;
                case REMOVE_EFFECT -> REMOVE_EFFECT_FIELDS;
                case PLAY_SOUND -> PLAY_SOUND_FIELDS;
                case SHOW_MESSAGE -> SHOW_MESSAGE_FIELDS;
            };
            if (!allowed.containsAll(action.keySet())) return null;
            if ((type == UseActionType.ADD_EFFECT || type == UseActionType.REMOVE_EFFECT)
                    && !string(action, "target", "self").equals("self")) return null;
            String idValue = switch (type) {
                case ADD_EFFECT, REMOVE_EFFECT -> string(action, "effect", "");
                case PLAY_SOUND -> string(action, "sound", "");
                case SHOW_MESSAGE -> "talent_editor:message";
            };
            if (ResourceLocation.tryParse(idValue) == null) return null;
            if (type == UseActionType.SHOW_MESSAGE && !action.has("text")) return null;
            return new UseActionDraft(action, type);
        }

        public UseActionType type() {
            return type;
        }

        public ResourceLocation effect() {
            ResourceLocation parsed = ResourceLocation.tryParse(string(root, "effect", "minecraft:speed"));
            return parsed == null ? new ResourceLocation("minecraft", "speed") : parsed;
        }

        public int durationTicks() {
            return integer(root, "durationTicks", 100);
        }

        public int amplifier() {
            return integer(root, "amplifier", 0);
        }

        public boolean ambient() {
            return bool(root, "ambient", false);
        }

        public boolean showParticles() {
            return bool(root, "showParticles", true);
        }

        public boolean showIcon() {
            return bool(root, "showIcon", true);
        }

        public ResourceLocation sound() {
            ResourceLocation parsed = ResourceLocation.tryParse(
                    string(root, "sound", "minecraft:entity.player.levelup")
            );
            return parsed == null ? new ResourceLocation("minecraft", "entity.player.levelup") : parsed;
        }

        public String soundSource() {
            return string(root, "source", "players");
        }

        public double volume() {
            return decimal(root, "volume", 1.0D);
        }

        public double pitch() {
            return decimal(root, "pitch", 1.0D);
        }

        public String messageMode() {
            return string(root, "mode", "chat");
        }

        public LocalizedTextDraft text() {
            return LocalizedTextDraft.parse(root.get("text"));
        }

        public boolean subtitleEnabled() {
            return root.has("subtitle");
        }

        public LocalizedTextDraft subtitle() {
            return LocalizedTextDraft.parse(root.get("subtitle"));
        }

        public int fadeInTicks() {
            return integer(root, "fadeInTicks", 10);
        }

        public int stayTicks() {
            return integer(root, "stayTicks", 70);
        }

        public int fadeOutTicks() {
            return integer(root, "fadeOutTicks", 20);
        }

        public static UseActionDraft effectAction(
                UseActionType type,
                ResourceLocation effect,
                int durationTicks,
                int amplifier,
                boolean ambient,
                boolean showParticles,
                boolean showIcon
        ) {
            JsonObject action = new JsonObject();
            action.addProperty("type", type.serializedName());
            action.addProperty("effect", effect.toString());
            action.addProperty("target", "self");
            if (type == UseActionType.ADD_EFFECT) {
                action.addProperty("durationTicks", durationTicks);
                action.addProperty("amplifier", amplifier);
                action.addProperty("ambient", ambient);
                action.addProperty("showParticles", showParticles);
                action.addProperty("showIcon", showIcon);
            }
            return new UseActionDraft(action, type);
        }

        public static UseActionDraft soundAction(
                ResourceLocation sound, String source, double volume, double pitch
        ) {
            JsonObject action = new JsonObject();
            action.addProperty("type", UseActionType.PLAY_SOUND.serializedName());
            action.addProperty("sound", sound.toString());
            action.addProperty("source", source);
            action.addProperty("volume", volume);
            action.addProperty("pitch", pitch);
            return new UseActionDraft(action, UseActionType.PLAY_SOUND);
        }

        public static UseActionDraft messageAction(
                String mode,
                LocalizedTextDraft text,
                boolean subtitleEnabled,
                LocalizedTextDraft subtitle,
                int fadeInTicks,
                int stayTicks,
                int fadeOutTicks
        ) {
            JsonObject action = new JsonObject();
            action.addProperty("type", UseActionType.SHOW_MESSAGE.serializedName());
            action.addProperty("mode", mode);
            action.add("text", text.toJson());
            if (subtitleEnabled) action.add("subtitle", subtitle.toJson());
            action.addProperty("fadeInTicks", fadeInTicks);
            action.addProperty("stayTicks", stayTicks);
            action.addProperty("fadeOutTicks", fadeOutTicks);
            return new UseActionDraft(action, UseActionType.SHOW_MESSAGE);
        }

        JsonObject toJsonObject() {
            return root.deepCopy();
        }
    }
}
