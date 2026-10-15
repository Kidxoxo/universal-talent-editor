package com.kidxoxo.talenteditor.client;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.kidxoxo.talenteditor.TalentEditorMod;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.Map;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;

public final class TalentDraft {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final String ATTRIBUTE_TYPE = TalentEditorMod.MOD_ID + ":attribute_modifier";
    private static final String DYNAMIC_ATTRIBUTE_TYPE = TalentEditorMod.MOD_ID + ":dynamic_attribute_modifier";
    private static final String CREATIVE_FLIGHT_TYPE = TalentEditorMod.MOD_ID + ":creative_flight";
    private static final String ADD_EFFECT_TYPE = TalentEditorMod.MOD_ID + ":add_effect";
    private static final String REMOVE_EFFECT_TYPE = TalentEditorMod.MOD_ID + ":remove_effect";
    private static final String MODIFY_DAMAGE_TYPE = TalentEditorMod.MOD_ID + ":modify_damage";
    private static final String CANCEL_DAMAGE_TYPE = TalentEditorMod.MOD_ID + ":cancel_damage";
    private static final String MODIFY_ENTITY_ATTRIBUTE_TYPE = TalentEditorMod.MOD_ID + ":modify_entity_attribute";
    private static final String DAMAGE_ENTITY_TYPE = TalentEditorMod.MOD_ID + ":damage_entity";
    private static final String HEAL_ENTITY_TYPE = TalentEditorMod.MOD_ID + ":heal_entity";
    private static final String EFFECT_IMMUNITY_TYPE = TalentEditorMod.MOD_ID + ":effect_immunity";
    private static final String DRAW_REFRESH_BONUS_TYPE = TalentEditorMod.MOD_ID + ":draw_refresh_bonus";
    private static final String QUEUE_DRAW_CONSTRAINT_TYPE = TalentEditorMod.MOD_ID + ":queue_draw_constraint";
    private static final String PLAY_SOUND_TYPE = TalentEditorMod.MOD_ID + ":play_sound";
    private static final String SHOW_MESSAGE_TYPE = TalentEditorMod.MOD_ID + ":show_message";
    private static final Set<String> EDITABLE_SOUND_KEYS = Set.of(
            "type", "sound", "source", "volume", "pitch"
    );
    private static final Set<String> EDITABLE_MESSAGE_KEYS = Set.of(
            "type", "mode", "text", "subtitle", "fadeInTicks", "stayTicks", "fadeOutTicks"
    );
    private static final Set<String> EDITABLE_TRIGGER_EVENTS = Set.of(
            "damage_dealt_pre", "damage_taken_pre", "damage_dealt", "damage_taken", "interact_entity",
            "kill", "death", "item_use_finish", "interact_block", "player_tick", "player_second",
            "player_move_distance", "level_up", "draw_selected", "draw_refreshed", "counter_changed",
            "fatal_damage", "nearby_entity_death", "item_submitted"
    );
    private static final Set<String> EDITABLE_TRIGGER_KEYS = Set.of(
            "id", "event", "priority", "chance", "chanceAttribute", "chanceCounter", "chanceCategory", "chancePerPoint",
            "distanceInterval", "cooldownTicks", "activationLimit", "condition", "actions",
            "actionFailurePolicy", "failureActions"
    );
    private static final Set<String> EDITABLE_ADD_EFFECT_KEYS = Set.of(
            "type", "effect", "durationTicks", "amplifier", "target", "ambient", "showParticles", "showIcon",
            "enabledWhen", "disabledWhen"
    );
    private static final Set<String> EDITABLE_REMOVE_EFFECT_KEYS = Set.of("type", "effect", "target");
    private static final Set<String> EDITABLE_MODIFY_DAMAGE_KEYS = Set.of(
            "type", "operation", "amount", "minimum", "maximum"
    );
    private static final Set<String> EDITABLE_CANCEL_DAMAGE_KEYS = Set.of("type");
    private static final Set<String> EDITABLE_MODIFY_ENTITY_ATTRIBUTE_KEYS = Set.of(
            "type", "attribute", "operation", "amount", "minimum", "maximum", "target", "radius"
    );
    private static final Set<String> EDITABLE_ENTITY_HEALTH_KEYS = Set.of(
            "type", "target", "amount", "radius"
    );
    private static final Set<String> EDITABLE_DRAW_CONSTRAINT_KEYS = Set.of(
            "type", "talents", "categories", "rarities", "tags", "pools", "guaranteed",
            "queueMode", "successCounter", "successIncrement", "milestoneEvery", "milestone"
    );
    private static final Set<String> EDITABLE_DRAW_MILESTONE_KEYS = Set.of(
            "talents", "categories", "rarities", "tags", "guaranteed"
    );
    private static final Set<String> EDITABLE_DYNAMIC_ATTRIBUTE_KEYS = Set.of(
            "type", "attribute", "source", "counter", "base", "scale", "minimum", "maximum",
            "operation", "modifierKey", "showInDescription", "enabledWhen", "disabledWhen"
    );
    private static final Set<String> ADVANCED_ACTION_TYPES = Set.of(
            "modify_state", "modify_damage_expression", "affect_health_expression",
            "modify_attribute_base_expression", "timed_attribute_modifier", "grant_talent",
            "revoke_talent", "give_item", "give_draw_tokens", "modify_counter", "take_item",
            "hud_resource", "player_render", "open_item_submission", "spawn_particles",
            "presentation_transform"
    );
    private static final Set<String> ACTIVE_ADVANCED_ACTION_TYPES = Set.of(
            "hud_resource", "player_render"
    );
    private static final Set<String> VISUAL_ROOT_KEYS = Set.of(
            "schema", "enabled", "text", "rarity", "categories", "icon", "offer", "capacityCost",
            "acquireCondition", "synthesis", "activeEffects", "triggers", "lifecycle",
            "onAcquire", "onSynthesize", "onRemove"
    );
    private static final Set<String> VISUAL_TEXT_KEYS = Set.of("name", "description", "showGeneratedEffects");
    private static final Set<String> VISUAL_OFFER_KEYS = Set.of("enabled", "weight", "pools");
    private static final Set<String> VISUAL_SYNTHESIS_KEYS = Set.of(
            "enabled", "consumeTalents", "consumeCounters", "consumeItems"
    );
    private static final Set<String> VISUAL_LIFECYCLE_KEYS = Set.of(
            "durationTicks", "removeAfterDraws", "retainEffects", "retainCategoryContribution", "retainTriggers"
    );

    String snapshot() { return root.toString(); }
    private final JsonObject root;
    private final ArrayList<JsonObject> effects;

    private TalentDraft(JsonObject root) {
        this.root = root.deepCopy();
        this.effects = new ArrayList<>();
        JsonElement source = this.root.get("activeEffects");
        if (source != null && source.isJsonArray()) {
            for (JsonElement element : source.getAsJsonArray()) {
                if (element.isJsonObject()) effects.add(element.getAsJsonObject().deepCopy());
            }
        }
    }

    public static TalentDraft create() {
        JsonObject root = new JsonObject();
        root.addProperty("schema", 2);
        JsonObject text = new JsonObject();
        text.addProperty("name", "");
        text.add("description", new JsonArray());
        text.addProperty("showGeneratedEffects", true);
        root.add("text", text);
        root.addProperty("rarity", TalentEditorMod.MOD_ID + ":common");
        JsonObject offer = new JsonObject();
        offer.addProperty("enabled", true);
        offer.addProperty("weight", 1.0D);
        JsonArray pools = new JsonArray();
        pools.add(TalentEditorMod.MOD_ID + ":default");
        offer.add("pools", pools);
        root.add("offer", offer);
        root.add("activeEffects", new JsonArray());
        return new TalentDraft(root);
    }

    public static TalentDraft parse(String json) {
        JsonElement parsed = JsonParser.parseString(json);
        if (!parsed.isJsonObject()) throw new IllegalArgumentException("Talent root must be an object");
        return new TalentDraft(parsed.getAsJsonObject());
    }

    public TalentDraft copy() { return new TalentDraft(root); }

    public LifecycleDraft lifecycle() {
        JsonObject lifecycle = object(root, "lifecycle");
        if (lifecycle == null) return new LifecycleDraft(0, 0, false, false, false);
        return new LifecycleDraft(
                integer(lifecycle, "durationTicks", 0),
                integer(lifecycle, "removeAfterDraws", 0),
                booleanValue(lifecycle, "retainEffects", false),
                booleanValue(lifecycle, "retainCategoryContribution", false),
                booleanValue(lifecycle, "retainTriggers", false)
        );
    }

    public void updateLifecycle(LifecycleDraft draft) {
        if (draft.durationTicks() == 0 && draft.removeAfterDraws() == 0
                && !draft.retainEffects() && !draft.retainCategoryContribution()
                && !draft.retainTriggers()) {
            root.remove("lifecycle");
            return;
        }
        JsonObject lifecycle = new JsonObject();
        lifecycle.addProperty("durationTicks", draft.durationTicks());
        lifecycle.addProperty("removeAfterDraws", draft.removeAfterDraws());
        lifecycle.addProperty("retainEffects", draft.retainEffects());
        lifecycle.addProperty("retainCategoryContribution", draft.retainCategoryContribution());
        lifecycle.addProperty("retainTriggers", draft.retainTriggers());
        root.add("lifecycle", lifecycle);
    }

    public void replaceAllFrom(TalentDraft source) {
        root.entrySet().clear();
        source.root.entrySet().forEach(entry -> root.add(entry.getKey(), entry.getValue().deepCopy()));
        effects.clear();
        JsonElement active = root.get("activeEffects");
        if (active != null && active.isJsonArray()) {
            active.getAsJsonArray().forEach(element -> {
                if (element.isJsonObject()) effects.add(element.getAsJsonObject().deepCopy());
            });
        }
    }

    public String name() {
        JsonObject text = object(root, "text");
        if (text == null) return "";
        return LocalizedTextDraft.parse(text.get("name")).fallbackText();
    }

    public TextStylesDraft textStyles() {
        JsonObject text = object(root, "text");
        if (text == null) return new TextStylesDraft(LocalizedTextDraft.literal(""), List.of());
        LocalizedTextDraft name = LocalizedTextDraft.parse(text.get("name"));
        ArrayList<LocalizedTextDraft> descriptions = new ArrayList<>();
        JsonElement raw = text.get("description");
        if (raw != null && raw.isJsonArray()) {
            for (JsonElement element : raw.getAsJsonArray()) descriptions.add(LocalizedTextDraft.parse(element));
        } else if (raw != null) {
            descriptions.add(LocalizedTextDraft.parse(raw));
        }
        return new TextStylesDraft(name, descriptions);
    }

    public void prepareNameLiteral(String displayName) {
        JsonObject text = object(root, "text");
        if (text == null) {
            text = new JsonObject();
            root.add("text", text);
        }
        LocalizedTextDraft current = LocalizedTextDraft.parse(text.get("name"));
        if (displayName.equals(current.fallbackText())) return;
        if (text.get("name") != null && text.get("name").isJsonObject()) {
            text.getAsJsonObject("name").addProperty("literal", displayName);
        } else {
            text.addProperty("name", displayName);
        }
    }

    public void updateTextStyles(LocalizedTextDraft name, List<LocalizedTextDraft> descriptions) {
        JsonObject text = object(root, "text");
        if (text == null) {
            text = new JsonObject();
            root.add("text", text);
        }
        text.add("name", name.toJson());
        JsonArray values = new JsonArray();
        descriptions.forEach(description -> values.add(description.toJson()));
        text.add("description", values);
    }

    public MetadataDraft metadata() {
        ArrayList<ResourceLocation> categories = new ArrayList<>();
        JsonElement rawCategories = root.get("categories");
        if (rawCategories != null && rawCategories.isJsonArray()) {
            for (JsonElement element : rawCategories.getAsJsonArray()) {
                try {
                    ResourceLocation id = element.isJsonPrimitive()
                            ? ResourceLocation.tryParse(element.getAsString()) : null;
                    if (id != null) categories.add(id);
                } catch (RuntimeException ignored) {
                }
            }
        }
        ResourceLocation rarity = ResourceLocation.tryParse(string(root, "rarity"));
        if (rarity == null) rarity = new ResourceLocation(TalentEditorMod.MOD_ID, "common");
        String rawIcon = string(root, "icon");
        ResourceLocation icon = rawIcon.isBlank() ? null : ResourceLocation.tryParse(rawIcon);
        JsonObject text = object(root, "text");
        return new MetadataDraft(
                booleanValue(root, "enabled", true),
                rarity,
                text == null || booleanValue(text, "showGeneratedEffects", true),
                categories,
                Optional.ofNullable(icon),
                integer(root, "capacityCost", 1)
        );
    }

    public void updateMetadata(
            boolean enabled,
            ResourceLocation rarity,
            boolean showGeneratedEffects,
            List<ResourceLocation> categories,
            Optional<ResourceLocation> icon,
            int capacityCost
    ) {
        root.addProperty("enabled", enabled);
        root.addProperty("rarity", rarity.toString());
        JsonObject text = object(root, "text");
        if (text == null) {
            text = new JsonObject();
            root.add("text", text);
        }
        text.addProperty("showGeneratedEffects", showGeneratedEffects);
        JsonArray serializedCategories = new JsonArray();
        categories.forEach(category -> serializedCategories.add(category.toString()));
        root.add("categories", serializedCategories);
        if (icon.isPresent()) {
            root.addProperty("icon", icon.orElseThrow().toString());
        } else {
            root.remove("icon");
        }
        root.addProperty("capacityCost", capacityCost);
    }

    public static List<String> parseDescriptionEditorText(String value) {
        if (value.isBlank()) return List.of();
        ArrayList<String> result = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean escaped = false;
        for (int index = 0; index < value.length(); index++) {
            char character = value.charAt(index);
            if (escaped) {
                if (character == 'n') {
                    result.add(current.toString());
                    current.setLength(0);
                } else if (character == '\\') {
                    current.append('\\');
                } else {
                    current.append('\\').append(character);
                }
                escaped = false;
            } else if (character == '\\') {
                escaped = true;
            } else {
                current.append(character);
            }
        }
        if (escaped) current.append('\\');
        result.add(current.toString());
        return List.copyOf(result);
    }

    public List<EffectRow> effectRows() {
        ArrayList<EffectRow> rows = new ArrayList<>();
        for (int index = 0; index < effects.size(); index++) {
            JsonObject effect = effects.get(index);
            if (isAttributeEffect(effect)) {
                ResourceLocation attribute = ResourceLocation.tryParse(string(effect, "attribute"));
                if (attribute != null) {
                    rows.add(new EffectRow(
                            index,
                            true,
                            false,
                            false,
                            false,
                            false,
                            false,
                            attribute.toString(),
                            decimal(effect, "amount", 0.0D),
                            operation(effect),
                            ""
                    ));
                    continue;
                }
            }
            if (isEffectImmunity(effect)) {
                ResourceLocation immuneEffect = ResourceLocation.tryParse(string(effect, "effect"));
                if (immuneEffect != null) {
                    rows.add(new EffectRow(index, false, true, false, false, false, false,
                            immuneEffect.toString(), 0.0D, "", ""));
                    continue;
                }
            }
            if (isMaintainedEffect(effect)) {
                ResourceLocation maintainedEffect = ResourceLocation.tryParse(string(effect, "effect"));
                if (maintainedEffect != null) {
                    rows.add(new EffectRow(index, false, false, true, false, false, false,
                            maintainedEffect.toString(), integer(effect, "amplifier", 0), "", ""));
                    continue;
                }
            }
            if (isDrawRefreshBonus(effect)) {
                rows.add(new EffectRow(index, false, false, false, true, false, false,
                        DRAW_REFRESH_BONUS_TYPE, integer(effect, "amount", 1), "", ""));
                continue;
            }
            if (isDynamicAttribute(effect)) {
                ResourceLocation attribute = ResourceLocation.tryParse(string(effect, "attribute"));
                if (attribute != null) {
                    rows.add(new EffectRow(index, false, false, false, false, true, false,
                            attribute.toString(), decimal(effect, "base", 0.0D), operation(effect),
                            string(effect, "source")));
                    continue;
                }
            }
            if (isCreativeFlight(effect)) {
                rows.add(new EffectRow(index, false, false, false, false, false, true,
                        CREATIVE_FLIGHT_TYPE, 0.0D, "", ""));
                continue;
            }
            rows.add(new EffectRow(index, false, false, false, false, false, false,
                    type(effect), 0.0D, "", ""));
        }
        return List.copyOf(rows);
    }

    public List<EffectRow> allEffectRows() {
        ArrayList<EffectRow> rows = new ArrayList<>(effectRows());
        for (TriggerRow trigger : triggerRows()) {
            rows.add(new EffectRow(
                    eventSelectionKey(trigger.rawIndex()),
                    false, false, false, false, false, false,
                    trigger.id(), 0.0D, "", trigger.event()
            ));
        }
        return List.copyOf(rows);
    }

    public List<EffectRow> basicEffectRows() {
        return effectRows().stream().filter(row -> row.editableAttribute()
                || row.editableImmunity() || row.editableMaintainedEffect()).toList();
    }

    public List<EffectRow> specialEffectRows() {
        return effectRows().stream().filter(row -> row.editableDynamicAttribute()
                || row.editableDrawRefreshBonus() || row.editableCreativeFlight()).toList();
    }

    public boolean isEditableEventEffect(EffectRow row) {
        if (!isEventEffect(row)) return false;
        int rawIndex = eventRawIndex(row);
        return triggerRows().stream()
                .anyMatch(trigger -> trigger.rawIndex() == rawIndex && trigger.editable());
    }

    public static boolean isEventEffect(EffectRow row) {
        return row != null && row.rawIndex() <= -2;
    }

    public static int eventRawIndex(EffectRow row) {
        if (!isEventEffect(row)) throw new IllegalArgumentException("Not an event effect row");
        return -row.rawIndex() - 2;
    }

    public static int eventSelectionKey(int rawIndex) {
        if (rawIndex < 0) throw new IllegalArgumentException("Negative trigger index");
        return -rawIndex - 2;
    }

    public int addAttribute(ResourceLocation attribute) {
        JsonObject effect = new JsonObject();
        effect.addProperty("type", ATTRIBUTE_TYPE);
        effect.addProperty("attribute", attribute.toString());
        effect.addProperty("amount", 1.0D);
        effect.addProperty("operation", "addition");
        effects.add(effect);
        return effects.size() - 1;
    }

    public int addEffectImmunity(ResourceLocation effect) {
        JsonObject action = new JsonObject();
        action.addProperty("type", EFFECT_IMMUNITY_TYPE);
        action.addProperty("effect", effect.toString());
        effects.add(action);
        return effects.size() - 1;
    }

    public int addMaintainedEffect(ResourceLocation effect) {
        JsonObject action = new JsonObject();
        action.addProperty("type", ADD_EFFECT_TYPE);
        action.addProperty("effect", effect.toString());
        action.addProperty("durationTicks", 60);
        action.addProperty("amplifier", 0);
        action.addProperty("target", "self");
        action.addProperty("ambient", true);
        action.addProperty("showParticles", false);
        action.addProperty("showIcon", true);
        effects.add(action);
        return effects.size() - 1;
    }

    public int addDrawRefreshBonus() {
        JsonObject action = new JsonObject();
        action.addProperty("type", DRAW_REFRESH_BONUS_TYPE);
        action.addProperty("amount", 1);
        effects.add(action);
        return effects.size() - 1;
    }

    public int addDynamicAttribute(ResourceLocation attribute) {
        JsonObject action = new JsonObject();
        action.addProperty("type", DYNAMIC_ATTRIBUTE_TYPE);
        action.addProperty("attribute", attribute.toString());
        action.addProperty("source", "health_percent");
        action.addProperty("base", 0.0D);
        action.addProperty("scale", 1.0D);
        action.addProperty("minimum", -1.0E12D);
        action.addProperty("maximum", 1.0E12D);
        action.addProperty("operation", "addition");
        action.addProperty("modifierKey", nextModifierKey());
        action.addProperty("showInDescription", true);
        effects.add(action);
        return effects.size() - 1;
    }

    public DynamicAttributeDraft dynamicAttribute(int rawIndex) {
        JsonObject action = effect(rawIndex);
        if (!isDynamicAttribute(action)) throw new IllegalArgumentException("Effect is not a dynamic attribute");
        ResourceLocation attribute = ResourceLocation.tryParse(string(action, "attribute"));
        ResourceLocation counter = ResourceLocation.tryParse(string(action, "counter"));
        if (attribute == null) throw new IllegalArgumentException("Dynamic attribute id is invalid");
        return new DynamicAttributeDraft(
                attribute, string(action, "source"), Optional.ofNullable(counter),
                decimal(action, "base", 0.0D), decimal(action, "scale", 1.0D),
                decimal(action, "minimum", -1.0E12D), decimal(action, "maximum", 1.0E12D),
                operation(action), string(action, "modifierKey"),
                booleanValue(action, "showInDescription", true)
        );
    }

    public void updateDynamicAttribute(int rawIndex, DynamicAttributeDraft value) {
        JsonObject action = effect(rawIndex);
        if (!isDynamicAttribute(action)) throw new IllegalArgumentException("Effect is not a dynamic attribute");
        action.addProperty("attribute", value.attribute().toString());
        action.addProperty("source", value.source());
        if (value.source().equals("counter")) {
            action.addProperty("counter", value.counter().orElseThrow().toString());
        } else {
            action.remove("counter");
        }
        action.addProperty("base", value.base());
        action.addProperty("scale", value.scale());
        action.addProperty("minimum", value.minimum());
        action.addProperty("maximum", value.maximum());
        action.addProperty("operation", value.operation());
        action.addProperty("modifierKey", value.modifierKey());
        action.addProperty("showInDescription", value.showInDescription());
    }

    public int addCreativeFlight() {
        for (int index = 0; index < effects.size(); index++) {
            if (isCreativeFlight(effects.get(index))) return index;
        }
        JsonObject action = new JsonObject();
        action.addProperty("type", CREATIVE_FLIGHT_TYPE);
        effects.add(action);
        return effects.size() - 1;
    }

    public boolean hasCreativeFlight() {
        return effects.stream().anyMatch(TalentDraft::isCreativeFlight);
    }

    public void removeCreativeFlight() {
        for (int index = effects.size() - 1; index >= 0; index--) {
            if (isCreativeFlight(effects.get(index))) effects.remove(index);
        }
    }

    public OfferDraft offer() {
        JsonObject offer = object(root, "offer");
        if (offer == null) return new OfferDraft(false, 1.0D, List.of());
        boolean enabled = booleanValue(offer, "enabled", false);
        double weight = decimal(offer, "weight", 1.0D);
        ArrayList<ResourceLocation> pools = new ArrayList<>();
        JsonElement rawPools = offer.get("pools");
        if (rawPools != null && rawPools.isJsonArray()) {
            for (JsonElement element : rawPools.getAsJsonArray()) {
                try {
                    ResourceLocation id = element.isJsonPrimitive()
                            ? ResourceLocation.tryParse(element.getAsString()) : null;
                    if (id != null) pools.add(id);
                } catch (RuntimeException ignored) {
                }
            }
        }
        return new OfferDraft(enabled, weight, pools);
    }

    public boolean hasOfferDefinition() {
        return root.has("offer") && root.get("offer").isJsonObject();
    }

    public EditorOverview editorOverview() {
        LinkedHashMap<EditorSection, EditorSectionState> states = new LinkedHashMap<>();
        TextStylesDraft text = textStyles();
        boolean styledName = !text.name().translation().isBlank() || text.name().color().isPresent()
                || text.name().bold() || text.name().italic();
        states.put(EditorSection.TEXT, new EditorSectionState(
                styledName || !text.descriptions().isEmpty(), text.descriptions().size(), false
        ));

        MetadataDraft metadata = metadata();
        int metadataChanges = 0;
        if (!metadata.enabled()) metadataChanges++;
        if (!metadata.rarity().equals(new ResourceLocation(TalentEditorMod.MOD_ID, "common"))) metadataChanges++;
        if (!metadata.showGeneratedEffects()) metadataChanges++;
        if (!metadata.categories().isEmpty()) metadataChanges++;
        if (metadata.icon().isPresent()) metadataChanges++;
        if (metadata.capacityCost() != 1) metadataChanges++;
        states.put(EditorSection.METADATA, new EditorSectionState(metadataChanges > 0, metadataChanges, false));

        int presentationCount = presentationActionCount("onAcquire") + presentationActionCount("onSynthesize");
        states.put(EditorSection.PRESENTATION, new EditorSectionState(
                presentationCount > 0, presentationCount, false
        ));

        OfferDraft offer = offer();
        List<ResourceLocation> defaultPools = List.of(new ResourceLocation(TalentEditorMod.MOD_ID, "default"));
        boolean defaultOffer = hasOfferDefinition() && offer.enabled()
                && Double.compare(offer.weight(), 1.0D) == 0 && offer.pools().equals(defaultPools);
        states.put(EditorSection.OFFER, new EditorSectionState(!defaultOffer, offer.pools().size(), false));

        RequirementsDraft requirements = requirements();
        states.put(EditorSection.REQUIREMENTS, new EditorSectionState(
                root.has("acquireCondition"), requirements.rows().size(),
                root.has("acquireCondition") && !requirements.editable()
        ));

        SynthesisDraft synthesis = synthesis();
        int synthesisCosts = synthesis.consumeTalents().size()
                + synthesis.consumeCounters().size() + synthesis.consumeItems().size();
        states.put(EditorSection.SYNTHESIS, new EditorSectionState(
                synthesis.enabled() || synthesisCosts > 0, synthesisCosts, false
        ));

        LifecycleDraft lifecycle = lifecycle();
        boolean lifecycleConfigured = lifecycle.durationTicks() > 0 || lifecycle.removeAfterDraws() > 0
                || lifecycle.retainEffects() || lifecycle.retainCategoryContribution() || lifecycle.retainTriggers();
        states.put(EditorSection.LIFECYCLE, new EditorSectionState(lifecycleConfigured,
                (lifecycle.durationTicks() > 0 ? 1 : 0) + (lifecycle.removeAfterDraws() > 0 ? 1 : 0), false));

        List<EffectRow> basic = basicEffectRows();
        List<EffectRow> special = specialEffectRows();
        List<AdvancedActionRow> activeInterfaces = advancedActionRows(AdvancedActionLocation.ACTIVE_EFFECTS, -1);
        states.put(EditorSection.BASIC_EFFECTS, new EditorSectionState(!basic.isEmpty(), basic.size(), false));
        states.put(EditorSection.SPECIAL_EFFECTS, new EditorSectionState(!special.isEmpty(), special.size(), false));
        states.put(EditorSection.ACTIVE_INTERFACES, new EditorSectionState(
                !activeInterfaces.isEmpty(), activeInterfaces.size(),
                activeInterfaces.stream().anyMatch(row -> !row.editable())
        ));

        List<TriggerRow> triggers = triggerRows();
        states.put(EditorSection.TRIGGERS, new EditorSectionState(
                !triggers.isEmpty(), triggers.size(), triggers.stream().anyMatch(row -> !row.editable())
        ));
        putActionState(states, EditorSection.ON_ACQUIRE_ACTIONS, AdvancedActionLocation.ON_ACQUIRE);
        putActionState(states, EditorSection.ON_SYNTHESIZE_ACTIONS, AdvancedActionLocation.ON_SYNTHESIZE);
        putActionState(states, EditorSection.ON_REMOVE_ACTIONS, AdvancedActionLocation.ON_REMOVE);

        boolean advancedJson = hasAdvancedJsonOnlyContent();
        states.put(EditorSection.RAW_JSON, new EditorSectionState(advancedJson, advancedJson ? 1 : 0, advancedJson));
        return new EditorOverview(states);
    }

    private void putActionState(
            Map<EditorSection, EditorSectionState> states,
            EditorSection section,
            AdvancedActionLocation location
    ) {
        List<AdvancedActionRow> rows = advancedActionRows(location, -1);
        long presentationRows = rows.stream().filter(row -> isPresentationAction(row.type())).count();
        int behaviorCount = (int) Math.max(0L, rows.size() - presentationRows);
        states.put(section, new EditorSectionState(
                behaviorCount > 0, behaviorCount,
                rows.stream().anyMatch(row -> !row.editable() && !isPresentationAction(row.type()))
        ));
    }

    private int presentationActionCount(String field) {
        JsonElement raw = root.get(field);
        if (raw == null || !raw.isJsonArray()) return 0;
        int count = 0;
        for (JsonElement element : raw.getAsJsonArray()) {
            if (!element.isJsonObject()) continue;
            String actionType = normalizeLocalId(type(element.getAsJsonObject()));
            if (isPresentationAction(actionType)) count++;
        }
        return count;
    }

    private static boolean isPresentationAction(String type) {
        return type.equals("play_sound") || type.equals("show_message") || type.equals("spawn_particles")
                || type.equals("presentation_transform");
    }

    public List<AdvancedActionRow> presentationCues(PresentationPhase phase) {
        AdvancedActionLocation location = phase == PresentationPhase.ACQUIRE
                ? AdvancedActionLocation.ON_ACQUIRE : AdvancedActionLocation.ON_SYNTHESIZE;
        return advancedActionRows(location, -1).stream()
                .filter(row -> isPresentationAction(row.type())).toList();
    }

    public void movePresentationCue(PresentationPhase phase, int rawIndex, int direction) {
        List<AdvancedActionRow> cues = presentationCues(phase);
        int selected = -1;
        for (int index = 0; index < cues.size(); index++) {
            if (cues.get(index).rawIndex() == rawIndex) { selected = index; break; }
        }
        if (selected < 0 || selected + direction < 0 || selected + direction >= cues.size()) return;
        int other = cues.get(selected + direction).rawIndex();
        swapAdvancedActions(phase == PresentationPhase.ACQUIRE
                ? AdvancedActionLocation.ON_ACQUIRE : AdvancedActionLocation.ON_SYNTHESIZE,
                -1, rawIndex, other);
    }

    public void swapAdvancedActions(AdvancedActionLocation location, int triggerRawIndex,
                                    int firstIndex, int secondIndex) {
        JsonArray actions = advancedActionArray(location, triggerRawIndex, false);
        if (firstIndex < 0 || secondIndex < 0 || firstIndex >= actions.size() || secondIndex >= actions.size()) return;
        JsonElement swap = actions.get(firstIndex);
        actions.set(firstIndex, actions.get(secondIndex));
        actions.set(secondIndex, swap);
    }

    private boolean hasAdvancedJsonOnlyContent() {
        if (!VISUAL_ROOT_KEYS.containsAll(root.keySet())) return true;
        if (hasUnknownKeys(object(root, "text"), VISUAL_TEXT_KEYS)
                || hasUnknownKeys(object(root, "offer"), VISUAL_OFFER_KEYS)
                || hasUnknownKeys(object(root, "synthesis"), VISUAL_SYNTHESIS_KEYS)
                || hasUnknownKeys(object(root, "lifecycle"), VISUAL_LIFECYCLE_KEYS)) return true;
        if (root.has("acquireCondition") && !requirements().editable()) return true;
        for (EffectRow row : effectRows()) {
            if (!row.editableAttribute() && !row.editableImmunity() && !row.editableMaintainedEffect()
                    && !row.editableDrawRefreshBonus() && !row.editableDynamicAttribute()
                    && !row.editableCreativeFlight()) {
                String actionType = normalizeLocalId(row.idOrType());
                if (!ACTIVE_ADVANCED_ACTION_TYPES.contains(actionType)) return true;
            }
        }
        JsonElement rawTriggers = root.get("triggers");
        if (rawTriggers != null) {
            if (!rawTriggers.isJsonArray()) return true;
            for (JsonElement trigger : rawTriggers.getAsJsonArray()) {
                if (!trigger.isJsonObject() || !isVisualTrigger(trigger.getAsJsonObject())) return true;
            }
        }
        for (AdvancedActionLocation location : List.of(
                AdvancedActionLocation.ON_ACQUIRE,
                AdvancedActionLocation.ON_SYNTHESIZE,
                AdvancedActionLocation.ON_REMOVE
        )) {
            for (AdvancedActionRow row : advancedActionRows(location, -1)) {
                if (!row.editable() && !row.type().equals("play_sound") && !row.type().equals("show_message")) {
                    return true;
                }
            }
        }
        return false;
    }

    private boolean isVisualTrigger(JsonObject trigger) {
        if (!EDITABLE_TRIGGER_KEYS.containsAll(trigger.keySet()) || string(trigger, "id").isBlank()
                || !EDITABLE_TRIGGER_EVENTS.contains(normalizeLocalId(string(trigger, "event")))) return false;
        JsonElement conditionValue = trigger.get("condition");
        if (conditionValue != null) {
            if (!conditionValue.isJsonObject()) return false;
            JsonObject condition = conditionValue.getAsJsonObject();
            String conditionType = normalizeLocalId(string(condition, "type"));
            if (conditionType.equals("expression_compare")) {
                if (!Set.of("type", "left", "operator", "right").containsAll(condition.keySet())) return false;
            } else {
                TriggerFilter filter = TriggerFilter.fromSerialized(conditionType);
                Set<String> expectedKeys = filter.requiresValue()
                        ? Set.of("type", filter.field()) : Set.of("type");
                if (!condition.keySet().equals(expectedKeys)) return false;
            }
        }
        JsonElement actions = trigger.get("actions");
        if (actions == null || !actions.isJsonArray() || actions.getAsJsonArray().isEmpty()
                || !visualActionArray(actions.getAsJsonArray())) return false;
        JsonElement failureActions = trigger.get("failureActions");
        return failureActions == null || failureActions.isJsonArray()
                && visualActionArray(failureActions.getAsJsonArray());
    }

    private boolean visualActionArray(JsonArray actions) {
        for (JsonElement actionValue : actions) {
            if (!actionValue.isJsonObject()) return false;
            JsonObject action = actionValue.getAsJsonObject();
            String actionType = normalizeLocalId(type(action));
            if (parseEditableTriggerAction(action) == null && !ADVANCED_ACTION_TYPES.contains(actionType)) {
                return false;
            }
        }
        return true;
    }

    private static boolean hasUnknownKeys(JsonObject value, Set<String> known) {
        return value != null && !known.containsAll(value.keySet());
    }

    public void updateOffer(boolean enabled, double weight, List<ResourceLocation> pools) {
        JsonObject offer = object(root, "offer");
        if (offer == null) {
            offer = new JsonObject();
            root.add("offer", offer);
        }
        offer.addProperty("enabled", enabled);
        offer.addProperty("weight", weight);
        JsonArray serializedPools = new JsonArray();
        pools.forEach(pool -> serializedPools.add(pool.toString()));
        offer.add("pools", serializedPools);
    }

    public RequirementsDraft requirements() {
        JsonElement raw = root.get("acquireCondition");
        if (raw == null) return new RequirementsDraft(true, RequirementMode.ALL, List.of());
        if (!raw.isJsonObject()) return new RequirementsDraft(false, RequirementMode.ALL, List.of());
        JsonObject condition = raw.getAsJsonObject();
        RequirementMode mode = RequirementMode.fromType(string(condition, "type"));
        if (mode == null || !condition.keySet().equals(Set.of("type", "children"))) {
            RequirementRow single = parseRequirement(condition);
            return single == null
                    ? new RequirementsDraft(false, RequirementMode.ALL, List.of())
                    : new RequirementsDraft(true, RequirementMode.ALL, List.of(single));
        }
        JsonElement rawChildren = condition.get("children");
        if (rawChildren == null || !rawChildren.isJsonArray()) {
            return new RequirementsDraft(false, mode, List.of());
        }
        ArrayList<RequirementRow> rows = new ArrayList<>();
        for (JsonElement child : rawChildren.getAsJsonArray()) {
            if (!child.isJsonObject()) return new RequirementsDraft(false, mode, List.of());
            RequirementRow row = parseRequirement(child.getAsJsonObject());
            if (row == null) return new RequirementsDraft(false, mode, List.of());
            rows.add(row);
        }
        return new RequirementsDraft(true, mode, rows);
    }

    public void updateRequirements(RequirementMode mode, List<RequirementRow> rows) {
        if (rows.isEmpty()) {
            root.remove("acquireCondition");
            return;
        }
        JsonObject group = new JsonObject();
        group.addProperty("type", mode.serializedName());
        JsonArray children = new JsonArray();
        for (RequirementRow row : rows) {
            JsonObject child = new JsonObject();
            child.addProperty("type", row.type().serializedName());
            child.addProperty(row.type().targetKey(), row.target().toString());
            if (row.type() == RequirementType.COUNTER
                    || row.type() == RequirementType.COUNTER_CATEGORY) {
                child.addProperty("value", row.amount());
            }
            if (row.type() == RequirementType.ITEM || row.type() == RequirementType.CATEGORY) {
                child.addProperty("count", row.amount());
            }
            if (row.type() == RequirementType.ATTRIBUTE) {
                child.addProperty("operator", row.operator());
                child.addProperty("value", row.attributeValue());
            }
            children.add(child);
        }
        group.add("children", children);
        root.add("acquireCondition", group);
    }

    public SynthesisDraft synthesis() {
        JsonObject synthesis = object(root, "synthesis");
        if (synthesis == null) return new SynthesisDraft(false, List.of(), Map.of(), Map.of());
        ArrayList<ResourceLocation> talents = new ArrayList<>();
        JsonElement rawTalents = synthesis.get("consumeTalents");
        if (rawTalents != null && rawTalents.isJsonArray()) {
            for (JsonElement element : rawTalents.getAsJsonArray()) {
                try {
                    ResourceLocation id = element.isJsonPrimitive()
                            ? ResourceLocation.tryParse(element.getAsString()) : null;
                    if (id != null) talents.add(id);
                } catch (RuntimeException ignored) {
                }
            }
        }
        return new SynthesisDraft(
                booleanValue(synthesis, "enabled", false),
                talents,
                resourceLongMap(synthesis, "consumeCounters"),
                resourceIntMap(synthesis, "consumeItems")
        );
    }

    public void updateSynthesis(
            boolean enabled,
            List<ResourceLocation> talents,
            Map<ResourceLocation, Long> counters,
            Map<ResourceLocation, Integer> items
    ) {
        JsonObject synthesis = object(root, "synthesis");
        if (synthesis == null) {
            synthesis = new JsonObject();
            root.add("synthesis", synthesis);
        }
        synthesis.addProperty("enabled", enabled);
        JsonArray serializedTalents = new JsonArray();
        talents.forEach(id -> serializedTalents.add(id.toString()));
        synthesis.add("consumeTalents", serializedTalents);
        JsonObject serializedCounters = new JsonObject();
        counters.forEach((id, count) -> serializedCounters.addProperty(id.toString(), count));
        synthesis.add("consumeCounters", serializedCounters);
        JsonObject serializedItems = new JsonObject();
        items.forEach((id, count) -> serializedItems.addProperty(id.toString(), count));
        synthesis.add("consumeItems", serializedItems);
    }

    public PresentationDraft presentation(PresentationPhase phase) {
        JsonElement raw = root.get(phase.field());
        if (raw == null) return PresentationDraft.defaults();
        if (!raw.isJsonArray() || raw.getAsJsonArray().size() > 256) return PresentationDraft.unsupported();
        boolean soundEnabled = false;
        ResourceLocation sound = new ResourceLocation("minecraft", "entity.player.levelup");
        String soundSource = "players";
        double volume = 1.0D;
        double pitch = 1.0D;
        boolean messageEnabled = false;
        String mode = "chat";
        LocalizedTextDraft text = LocalizedTextDraft.literal("");
        boolean subtitleEnabled = false;
        LocalizedTextDraft subtitle = LocalizedTextDraft.literal("");
        int fadeInTicks = 10;
        int stayTicks = 70;
        int fadeOutTicks = 20;
        for (JsonElement element : raw.getAsJsonArray()) {
            if (!element.isJsonObject()) return PresentationDraft.unsupported();
            JsonObject action = element.getAsJsonObject();
            String type = normalizeLocalId(string(action, "type"));
            if (type.equals(normalizeLocalId(PLAY_SOUND_TYPE))) {
                if (soundEnabled || !EDITABLE_SOUND_KEYS.containsAll(action.keySet())) {
                    return PresentationDraft.unsupported();
                }
                sound = ResourceLocation.tryParse(string(action, "sound"));
                if (sound == null) return PresentationDraft.unsupported();
                soundEnabled = true;
                soundSource = string(action, "source");
                if (soundSource.isBlank()) soundSource = "players";
                volume = decimal(action, "volume", 1.0D);
                pitch = decimal(action, "pitch", 1.0D);
            } else if (type.equals(normalizeLocalId(SHOW_MESSAGE_TYPE))) {
                if (messageEnabled || !EDITABLE_MESSAGE_KEYS.containsAll(action.keySet())
                        || !action.has("text")) return PresentationDraft.unsupported();
                messageEnabled = true;
                mode = string(action, "mode");
                if (mode.isBlank()) mode = "chat";
                text = LocalizedTextDraft.parse(action.get("text"));
                subtitleEnabled = action.has("subtitle");
                if (subtitleEnabled) subtitle = LocalizedTextDraft.parse(action.get("subtitle"));
                fadeInTicks = integer(action, "fadeInTicks", 10);
                stayTicks = integer(action, "stayTicks", 70);
                fadeOutTicks = integer(action, "fadeOutTicks", 20);
            }
        }
        return new PresentationDraft(
                true, soundEnabled, sound, soundSource, volume, pitch,
                messageEnabled, mode, text, subtitleEnabled, subtitle,
                fadeInTicks, stayTicks, fadeOutTicks
        );
    }

    public void updatePresentation(PresentationPhase phase, PresentationDraft draft) {
        if (!draft.editable()) throw new IllegalArgumentException("Presentation action list is not editable");
        JsonArray actions = new JsonArray();
        JsonElement current = root.get(phase.field());
        if (current != null && current.isJsonArray()) {
            for (JsonElement element : current.getAsJsonArray()) {
                if (!element.isJsonObject()) continue;
                String type = normalizeLocalId(string(element.getAsJsonObject(), "type"));
                if (!type.equals(normalizeLocalId(PLAY_SOUND_TYPE))
                        && !type.equals(normalizeLocalId(SHOW_MESSAGE_TYPE))) {
                    actions.add(element.deepCopy());
                }
            }
        }
        if (draft.soundEnabled()) {
            JsonObject sound = new JsonObject();
            sound.addProperty("type", PLAY_SOUND_TYPE);
            sound.addProperty("sound", draft.sound().toString());
            sound.addProperty("source", draft.soundSource());
            sound.addProperty("volume", draft.volume());
            sound.addProperty("pitch", draft.pitch());
            actions.add(sound);
        }
        if (draft.messageEnabled()) {
            JsonObject message = new JsonObject();
            message.addProperty("type", SHOW_MESSAGE_TYPE);
            message.addProperty("mode", draft.mode());
            message.add("text", draft.text().toJson());
            if (draft.subtitleEnabled()) message.add("subtitle", draft.subtitle().toJson());
            message.addProperty("fadeInTicks", draft.fadeInTicks());
            message.addProperty("stayTicks", draft.stayTicks());
            message.addProperty("fadeOutTicks", draft.fadeOutTicks());
            actions.add(message);
        }
        if (actions.isEmpty()) root.remove(phase.field());
        else root.add(phase.field(), actions);
    }

    public List<TriggerRow> triggerRows() {
        JsonElement source = root.get("triggers");
        if (source == null || !source.isJsonArray()) return List.of();
        ArrayList<TriggerRow> rows = new ArrayList<>();
        JsonArray triggers = source.getAsJsonArray();
        for (int index = 0; index < triggers.size(); index++) {
            JsonElement value = triggers.get(index);
            if (!value.isJsonObject()) {
                rows.add(TriggerRow.unsupported(index, "?", "?"));
                continue;
            }
            JsonObject trigger = value.getAsJsonObject();
            String id = string(trigger, "id");
            String event = normalizeLocalId(string(trigger, "event"));
            TriggerRow editable = parseEditableTrigger(index, trigger, id, event);
            rows.add(editable == null ? TriggerRow.unsupported(index, id, event) : editable);
        }
        return List.copyOf(rows);
    }

    public List<AdvancedTriggerRow> advancedTriggerRows() {
        JsonElement source = root.get("triggers");
        if (source == null || !source.isJsonArray()) return List.of();
        ArrayList<AdvancedTriggerRow> rows = new ArrayList<>();
        JsonArray triggers = source.getAsJsonArray();
        for (int index = 0; index < triggers.size(); index++) {
            JsonElement value = triggers.get(index);
            if (!value.isJsonObject()) {
                rows.add(new AdvancedTriggerRow(index, "?", "?", false, false, 0, 0));
                continue;
            }
            JsonObject trigger = value.getAsJsonObject();
            JsonObject condition = object(trigger, "condition");
            boolean expression = condition != null
                    && normalizeLocalId(string(condition, "type")).equals("expression_compare");
            rows.add(new AdvancedTriggerRow(
                    index,
                    string(trigger, "id").isBlank() ? "?" : string(trigger, "id"),
                    normalizeLocalId(string(trigger, "event")),
                    true,
                    expression,
                    actionArray(trigger, "actions").size(),
                    actionArray(trigger, "failureActions").size()
            ));
        }
        return List.copyOf(rows);
    }

    public ExpressionConditionDraft triggerExpressionCondition(int rawIndex) {
        JsonObject trigger = triggerObject(rawIndex);
        JsonObject condition = object(trigger, "condition");
        if (condition == null) return new ExpressionConditionDraft(false, false, "0", "gte", "0");
        if (!normalizeLocalId(string(condition, "type")).equals("expression_compare")) {
            return new ExpressionConditionDraft(false, true, "0", "gte", "0");
        }
        String operator = string(condition, "operator");
        if (operator.isBlank()) operator = "gte";
        return new ExpressionConditionDraft(
                true, true, stringOr(condition, "left", "0"), operator, stringOr(condition, "right", "0")
        );
    }

    public void updateTriggerExpressionCondition(
            int rawIndex, String left, String operator, String right
    ) {
        JsonObject condition = new JsonObject();
        condition.addProperty("type", TalentEditorMod.MOD_ID + ":expression_compare");
        condition.addProperty("left", left);
        condition.addProperty("operator", operator);
        condition.addProperty("right", right);
        triggerObject(rawIndex).add("condition", condition);
    }

    public void clearTriggerCondition(int rawIndex) {
        triggerObject(rawIndex).remove("condition");
    }

    public List<AdvancedActionRow> advancedActionRows(
            AdvancedActionLocation location, int triggerRawIndex
    ) {
        ArrayList<AdvancedActionRow> rows = new ArrayList<>();
        if (location == AdvancedActionLocation.ACTIVE_EFFECTS) {
            for (int index = 0; index < effects.size(); index++) {
                String actionType = normalizeLocalId(type(effects.get(index)));
                if (ACTIVE_ADVANCED_ACTION_TYPES.contains(actionType)) {
                    rows.add(new AdvancedActionRow(index, actionType, true));
                }
            }
            return List.copyOf(rows);
        }
        JsonArray actions = advancedActionArray(location, triggerRawIndex, false);
        for (int index = 0; index < actions.size(); index++) {
            JsonElement value = actions.get(index);
            String actionType = value.isJsonObject() ? normalizeLocalId(type(value.getAsJsonObject())) : "?";
            rows.add(new AdvancedActionRow(
                    index, actionType.isBlank() ? "?" : actionType,
                    value.isJsonObject() && ADVANCED_ACTION_TYPES.contains(actionType)
            ));
        }
        return List.copyOf(rows);
    }

    public List<ResourceLocation> numericStateIds() {
        LinkedHashSet<ResourceLocation> result = new LinkedHashSet<>();
        collectNumericStateIds(root, result);
        return result.stream().sorted(java.util.Comparator.comparing(ResourceLocation::toString)).toList();
    }

    private static void collectNumericStateIds(JsonElement element, Set<ResourceLocation> result) {
        if (element == null) return;
        if (element.isJsonArray()) {
            element.getAsJsonArray().forEach(child -> collectNumericStateIds(child, result));
            return;
        }
        if (!element.isJsonObject()) return;
        JsonObject object = element.getAsJsonObject();
        String actionType = normalizeLocalId(string(object, "type"));
        if (actionType.equals("modify_state") || actionType.equals("hud_resource")) {
            ResourceLocation state = ResourceLocation.tryParse(string(object, "state"));
            if (state != null) result.add(state);
        }
        object.entrySet().forEach(entry -> collectNumericStateIds(entry.getValue(), result));
    }

    public JsonObject advancedAction(
            AdvancedActionLocation location, int triggerRawIndex, int actionIndex
    ) {
        if (location == AdvancedActionLocation.ACTIVE_EFFECTS) {
            if (actionIndex < 0 || actionIndex >= effects.size()) {
                throw new IllegalArgumentException("Invalid active effect index " + actionIndex);
            }
            return effects.get(actionIndex).deepCopy();
        }
        JsonArray actions = advancedActionArray(location, triggerRawIndex, false);
        if (actionIndex < 0 || actionIndex >= actions.size() || !actions.get(actionIndex).isJsonObject()) {
            throw new IllegalArgumentException("Invalid advanced action index " + actionIndex);
        }
        return actions.get(actionIndex).getAsJsonObject().deepCopy();
    }

    public void putAdvancedAction(
            AdvancedActionLocation location, int triggerRawIndex, int actionIndex, JsonObject action
    ) {
        if (location == AdvancedActionLocation.ACTIVE_EFFECTS) {
            if (actionIndex == effects.size() || actionIndex == -1) effects.add(action.deepCopy());
            else if (actionIndex >= 0 && actionIndex < effects.size()) effects.set(actionIndex, action.deepCopy());
            else throw new IllegalArgumentException("Invalid active effect index " + actionIndex);
            return;
        }
        JsonArray actions = advancedActionArray(location, triggerRawIndex, true);
        if (actionIndex == actions.size() || actionIndex == -1) actions.add(action.deepCopy());
        else if (actionIndex >= 0 && actionIndex < actions.size()) actions.set(actionIndex, action.deepCopy());
        else throw new IllegalArgumentException("Invalid advanced action index " + actionIndex);
    }

    public void removeAdvancedAction(
            AdvancedActionLocation location, int triggerRawIndex, int actionIndex
    ) {
        if (location == AdvancedActionLocation.ACTIVE_EFFECTS) {
            if (actionIndex < 0 || actionIndex >= effects.size()) throw new IllegalArgumentException(
                    "Invalid active effect index " + actionIndex);
            effects.remove(actionIndex);
            return;
        }
        JsonArray actions = advancedActionArray(location, triggerRawIndex, false);
        if (actionIndex < 0 || actionIndex >= actions.size()) {
            throw new IllegalArgumentException("Invalid advanced action index " + actionIndex);
        }
        if (location == AdvancedActionLocation.TRIGGER_ACTIONS && actions.size() <= 1) {
            throw new IllegalStateException("A trigger requires at least one primary action");
        }
        actions.remove(actionIndex);
        cleanupEmptyActionArray(location, triggerRawIndex, actions);
    }

    public void moveAdvancedAction(
            AdvancedActionLocation location, int triggerRawIndex, int actionIndex, int offset
    ) {
        if (location == AdvancedActionLocation.ACTIVE_EFFECTS) {
            List<AdvancedActionRow> rows = advancedActionRows(location, triggerRawIndex);
            int visibleIndex = -1;
            for (int index = 0; index < rows.size(); index++) {
                if (rows.get(index).rawIndex() == actionIndex) {
                    visibleIndex = index;
                    break;
                }
            }
            int destinationIndex = visibleIndex + offset;
            if (visibleIndex < 0 || destinationIndex < 0 || destinationIndex >= rows.size()) return;
            int destination = rows.get(destinationIndex).rawIndex();
            JsonObject moving = effects.get(actionIndex);
            effects.set(actionIndex, effects.get(destination));
            effects.set(destination, moving);
            return;
        }
        JsonArray actions = advancedActionArray(location, triggerRawIndex, false);
        int destination = actionIndex + offset;
        if (actionIndex < 0 || actionIndex >= actions.size()
                || destination < 0 || destination >= actions.size()) return;
        JsonElement moving = actions.get(actionIndex);
        JsonElement displaced = actions.get(destination);
        actions.set(destination, moving);
        actions.set(actionIndex, displaced);
    }

    private JsonArray advancedActionArray(
            AdvancedActionLocation location, int triggerRawIndex, boolean create
    ) {
        JsonObject owner;
        String field;
        if (location.triggerScoped()) {
            owner = triggerObject(triggerRawIndex);
            field = location == AdvancedActionLocation.TRIGGER_ACTIONS ? "actions" : "failureActions";
        } else {
            owner = root;
            field = location.rootField();
        }
        JsonElement current = owner.get(field);
        if (current != null && current.isJsonArray()) return current.getAsJsonArray();
        if (!create) return new JsonArray();
        JsonArray created = new JsonArray();
        owner.add(field, created);
        return created;
    }

    private void cleanupEmptyActionArray(
            AdvancedActionLocation location, int triggerRawIndex, JsonArray actions
    ) {
        if (!actions.isEmpty() || location == AdvancedActionLocation.TRIGGER_ACTIONS) return;
        if (location.triggerScoped()) triggerObject(triggerRawIndex).remove("failureActions");
        else root.remove(location.rootField());
    }

    private JsonObject triggerObject(int rawIndex) {
        JsonElement source = root.get("triggers");
        if (source == null || !source.isJsonArray() || rawIndex < 0
                || rawIndex >= source.getAsJsonArray().size()
                || !source.getAsJsonArray().get(rawIndex).isJsonObject()) {
            throw new IllegalArgumentException("Invalid trigger index " + rawIndex);
        }
        return source.getAsJsonArray().get(rawIndex).getAsJsonObject();
    }

    private static JsonArray actionArray(JsonObject owner, String field) {
        JsonElement current = owner.get(field);
        return current != null && current.isJsonArray() ? current.getAsJsonArray() : new JsonArray();
    }

    private static String stringOr(JsonObject object, String key, String fallback) {
        String value = string(object, key);
        return value.isBlank() ? fallback : value;
    }

    public int addEffectTrigger() {
        JsonArray triggers = triggerArray();
        int index = triggers.size();
        updateEffectTrigger(index, new TriggerDraft(
                "effect_" + (index + 1), "damage_dealt", 0, 1.0D, 0,
                TriggerAction.ADD_EFFECT,
                new ResourceLocation("minecraft", "speed"), 100, 0,
                "event_entity", false, true, true, TriggerFilter.NONE, null,
                "add", 0.0D, 0.0D, 1.0E9D
        ));
        return index;
    }

    public void updateEffectTrigger(int rawIndex, TriggerDraft draft) {
        JsonObject trigger = new JsonObject();
        trigger.addProperty("id", draft.id());
        trigger.addProperty("event", draft.event());
        trigger.addProperty("priority", draft.priority());
        trigger.addProperty("chance", draft.chance());
        if (draft.chanceAttribute() != null) {
            trigger.addProperty("chanceAttribute", draft.chanceAttribute().toString());
        }
        if (draft.chanceCounter() != null) {
            trigger.addProperty("chanceCounter", draft.chanceCounter().toString());
        }
        if (draft.chanceCategory() != null) {
            trigger.addProperty("chanceCategory", draft.chanceCategory().toString());
        }
        if (draft.chanceAttribute() != null || draft.chanceCounter() != null
                || draft.chanceCategory() != null || draft.chancePerPoint() != 0.0D) {
            trigger.addProperty("chancePerPoint", draft.chancePerPoint());
        }
        if (draft.event().equals("player_move_distance") || draft.distanceInterval() != 1.0D) {
            trigger.addProperty("distanceInterval", draft.distanceInterval());
        }
        trigger.addProperty("cooldownTicks", draft.cooldownTicks());
        trigger.addProperty("activationLimit", draft.activationLimit());
        trigger.addProperty("actionFailurePolicy", draft.actionFailurePolicy());
        if (draft.filter() != TriggerFilter.NONE
                && (!draft.filter().requiresValue() || draft.filterValue() != null)) {
            JsonObject condition = new JsonObject();
            condition.addProperty("type", draft.filter().serializedName());
            if (draft.filter().requiresValue()) {
                condition.addProperty(draft.filter().field(), draft.filterValue().toString());
            }
            trigger.add("condition", condition);
        }
        JsonArray actions = new JsonArray();
        for (TriggerActionDraft draftAction : draft.actions()) actions.add(serializeTriggerAction(draftAction));
        trigger.add("actions", actions);
        if (!draft.failureActions().isEmpty()) {
            JsonArray failureActions = new JsonArray();
            for (TriggerActionDraft draftAction : draft.failureActions()) {
                failureActions.add(serializeTriggerAction(draftAction));
            }
            trigger.add("failureActions", failureActions);
        }
        JsonArray triggers = triggerArray();
        if (rawIndex == triggers.size()) triggers.add(trigger);
        else if (rawIndex >= 0 && rawIndex < triggers.size()) triggers.set(rawIndex, trigger);
        else throw new IllegalArgumentException("Invalid trigger index " + rawIndex);
    }

    public void removeEffectTrigger(int rawIndex) {
        TriggerRow row = triggerRows().stream().filter(value -> value.rawIndex() == rawIndex).findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Invalid trigger index " + rawIndex));
        if (!row.editable()) throw new IllegalArgumentException("Trigger is not editable by the effect form");
        root.getAsJsonArray("triggers").remove(rawIndex);
    }

    private static JsonObject serializeTriggerAction(TriggerActionDraft draftAction) {
        JsonObject action = new JsonObject();
        action.addProperty("type", draftAction.action().serializedName());
        if (draftAction.action() == TriggerAction.QUEUE_DRAW_CONSTRAINT) {
            JsonArray targets = new JsonArray();
            targets.add(draftAction.searchValue().toString());
            action.add(draftAction.searchTarget().field(), targets);
            if (draftAction.searchPool() != null) {
                JsonArray pools = new JsonArray();
                pools.add(draftAction.searchPool().toString());
                action.add("pools", pools);
            }
            action.addProperty("guaranteed", draftAction.searchGuaranteed());
            action.addProperty("queueMode", draftAction.searchQueueMode());
            if (draftAction.searchSuccessCounter() != null) {
                action.addProperty("successCounter", draftAction.searchSuccessCounter().toString());
                action.addProperty("successIncrement", draftAction.searchSuccessIncrement());
                if (draftAction.searchMilestoneEvery() > 0
                        && draftAction.searchMilestoneValue() != null) {
                    action.addProperty("milestoneEvery", draftAction.searchMilestoneEvery());
                    JsonObject milestone = new JsonObject();
                    JsonArray milestoneTargets = new JsonArray();
                    milestoneTargets.add(draftAction.searchMilestoneValue().toString());
                    milestone.add(draftAction.searchMilestoneTarget().field(), milestoneTargets);
                    milestone.addProperty("guaranteed", draftAction.searchMilestoneGuaranteed());
                    action.add("milestone", milestone);
                }
            }
        } else if (draftAction.action() == TriggerAction.MODIFY_DAMAGE) {
            action.addProperty("operation", draftAction.damageOperation());
            action.addProperty("amount", draftAction.damageAmount());
            action.addProperty("minimum", draftAction.damageMinimum());
            action.addProperty("maximum", draftAction.damageMaximum());
        } else if (draftAction.action() == TriggerAction.MODIFY_ENTITY_ATTRIBUTE) {
            action.addProperty("attribute", draftAction.effect().toString());
            action.addProperty("target", draftAction.target());
            action.addProperty("operation", draftAction.damageOperation());
            action.addProperty("amount", draftAction.damageAmount());
            action.addProperty("minimum", draftAction.damageMinimum());
            action.addProperty("maximum", draftAction.damageMaximum());
            action.addProperty("radius", draftAction.entityRadius());
        } else if (draftAction.action() == TriggerAction.DAMAGE_ENTITY
                || draftAction.action() == TriggerAction.HEAL_ENTITY) {
            action.addProperty("target", draftAction.target());
            action.addProperty("amount", draftAction.damageAmount());
            action.addProperty("radius", draftAction.entityRadius());
        } else if (draftAction.action() != TriggerAction.CANCEL_DAMAGE) {
            action.addProperty("effect", draftAction.effect().toString());
            action.addProperty("target", draftAction.target());
            if (draftAction.action() == TriggerAction.ADD_EFFECT) {
                action.addProperty("durationTicks", draftAction.durationTicks());
                action.addProperty("amplifier", draftAction.amplifier());
                action.addProperty("ambient", draftAction.ambient());
                action.addProperty("showParticles", draftAction.showParticles());
                action.addProperty("showIcon", draftAction.showIcon());
            }
        }
        return action;
    }

    private TriggerRow parseEditableTrigger(int index, JsonObject trigger, String id, String event) {
        if (!EDITABLE_TRIGGER_KEYS.containsAll(trigger.keySet()) || id.isBlank()
                || !EDITABLE_TRIGGER_EVENTS.contains(event)) return null;
        TriggerFilter filter = TriggerFilter.NONE;
        ResourceLocation filterValue = null;
        if (trigger.has("condition")) {
            if (!trigger.get("condition").isJsonObject()) return null;
            JsonObject condition = trigger.getAsJsonObject("condition");
            filter = TriggerFilter.fromSerialized(normalizeLocalId(string(condition, "type")));
            if (filter == null) return null;
            Set<String> expectedKeys = filter.requiresValue()
                    ? Set.of("type", filter.field()) : Set.of("type");
            if (!condition.keySet().equals(expectedKeys)) return null;
            if (filter.requiresValue()) {
                filterValue = ResourceLocation.tryParse(string(condition, filter.field()));
                if (filterValue == null) return null;
            }
        }
        JsonElement actionsValue = trigger.get("actions");
        if (actionsValue == null || !actionsValue.isJsonArray() || actionsValue.getAsJsonArray().isEmpty()
                || actionsValue.getAsJsonArray().size() > 256) return null;
        ArrayList<TriggerActionDraft> actions = new ArrayList<>();
        for (JsonElement actionElement : actionsValue.getAsJsonArray()) {
            if (!actionElement.isJsonObject()) return null;
            TriggerActionDraft parsed = parseEditableTriggerAction(actionElement.getAsJsonObject());
            if (parsed == null) return null;
            actions.add(parsed);
        }
        String actionFailurePolicy = string(trigger, "actionFailurePolicy");
        if (actionFailurePolicy.isBlank()) actionFailurePolicy = "continue";
        if (!Set.of("continue", "stop").contains(actionFailurePolicy)) return null;
        ArrayList<TriggerActionDraft> failureActions = new ArrayList<>();
        if (trigger.has("failureActions")) {
            if (!trigger.get("failureActions").isJsonArray()
                    || trigger.getAsJsonArray("failureActions").size() > 256) return null;
            for (JsonElement actionElement : trigger.getAsJsonArray("failureActions")) {
                if (!actionElement.isJsonObject()) return null;
                TriggerActionDraft parsed = parseEditableTriggerAction(actionElement.getAsJsonObject());
                if (parsed == null) return null;
                failureActions.add(parsed);
            }
        }
        ResourceLocation chanceAttribute = trigger.has("chanceAttribute")
                ? ResourceLocation.tryParse(string(trigger, "chanceAttribute")) : null;
        if (trigger.has("chanceAttribute") && chanceAttribute == null) return null;
        ResourceLocation chanceCounter = trigger.has("chanceCounter")
                ? ResourceLocation.tryParse(string(trigger, "chanceCounter")) : null;
        ResourceLocation chanceCategory = trigger.has("chanceCategory")
                ? ResourceLocation.tryParse(string(trigger, "chanceCategory")) : null;
        if (trigger.has("chanceCounter") && chanceCounter == null
                || trigger.has("chanceCategory") && chanceCategory == null
                || (chanceAttribute != null ? 1 : 0) + (chanceCounter != null ? 1 : 0)
                + (chanceCategory != null ? 1 : 0) > 1) {
            return null;
        }
        return new TriggerRow(index, true, new TriggerDraft(
                id, event, integer(trigger, "priority", 0), decimal(trigger, "chance", 1.0D),
                integer(trigger, "cooldownTicks", 0), integer(trigger, "activationLimit", 0), chanceAttribute,
                chanceCounter, chanceCategory, decimal(trigger, "chancePerPoint", 0.0D),
                decimal(trigger, "distanceInterval", 1.0D), filter, filterValue, actions,
                actionFailurePolicy, failureActions
        ), id, event);
    }

    private TriggerActionDraft parseEditableTriggerAction(JsonObject action) {
        TriggerAction triggerAction = TriggerAction.fromSerialized(normalizeLocalId(string(action, "type")));
        if (triggerAction == null) return null;
        Set<String> allowedActionKeys = switch (triggerAction) {
            case ADD_EFFECT -> EDITABLE_ADD_EFFECT_KEYS;
            case REMOVE_EFFECT -> EDITABLE_REMOVE_EFFECT_KEYS;
            case MODIFY_DAMAGE -> EDITABLE_MODIFY_DAMAGE_KEYS;
            case CANCEL_DAMAGE -> EDITABLE_CANCEL_DAMAGE_KEYS;
            case QUEUE_DRAW_CONSTRAINT -> EDITABLE_DRAW_CONSTRAINT_KEYS;
            case MODIFY_ENTITY_ATTRIBUTE -> EDITABLE_MODIFY_ENTITY_ATTRIBUTE_KEYS;
            case DAMAGE_ENTITY, HEAL_ENTITY -> EDITABLE_ENTITY_HEALTH_KEYS;
        };
        if (!allowedActionKeys.containsAll(action.keySet())) return null;
        if (triggerAction == TriggerAction.QUEUE_DRAW_CONSTRAINT) {
            DrawSearchTarget searchTarget = null;
            ResourceLocation searchValue = null;
            for (DrawSearchTarget candidate : DrawSearchTarget.values()) {
                List<ResourceLocation> values = resourceList(action, candidate.field());
                if (!values.isEmpty()) {
                    if (searchTarget != null || values.size() != 1) return null;
                    searchTarget = candidate;
                    searchValue = values.get(0);
                }
            }
            List<ResourceLocation> pools = resourceList(action, "pools");
            if (searchTarget == null || searchValue == null || pools.size() > 1) return null;
            String queueMode = string(action, "queueMode");
            if (queueMode.isBlank()) queueMode = "append";
            if (!Set.of("append", "dedupe", "replace").contains(queueMode)) return null;
            ResourceLocation successCounter = action.has("successCounter")
                    ? ResourceLocation.tryParse(string(action, "successCounter")) : null;
            if (action.has("successCounter") && successCounter == null) return null;
            long successIncrement = longInteger(action, "successIncrement", successCounter == null ? 0L : 1L);
            int milestoneEvery = integer(action, "milestoneEvery", 0);
            DrawSearchTarget milestoneTarget = DrawSearchTarget.TAG;
            ResourceLocation milestoneValue = null;
            int milestoneGuaranteed = integer(action, "guaranteed", 1);
            if (action.has("milestone")) {
                if (successCounter == null || milestoneEvery < 1 || !action.get("milestone").isJsonObject()) {
                    return null;
                }
                JsonObject milestone = action.getAsJsonObject("milestone");
                if (!EDITABLE_DRAW_MILESTONE_KEYS.containsAll(milestone.keySet())) return null;
                DrawSearchTarget found = null;
                for (DrawSearchTarget candidate : DrawSearchTarget.values()) {
                    List<ResourceLocation> values = resourceList(milestone, candidate.field());
                    if (!values.isEmpty()) {
                        if (found != null || values.size() != 1) return null;
                        found = candidate;
                        milestoneValue = values.get(0);
                    }
                }
                if (found == null || milestoneValue == null) return null;
                milestoneTarget = found;
                milestoneGuaranteed = integer(milestone, "guaranteed", integer(action, "guaranteed", 1));
            } else if (milestoneEvery != 0) {
                return null;
            }
            if (successCounter == null && (action.has("successIncrement") || action.has("milestone"))) return null;
            return new TriggerActionDraft(
                    triggerAction, new ResourceLocation("minecraft", "speed"), 100, 0,
                    "self", false, true, true, "add", 0.0D, 0.0D, 1.0E9D,
                    searchTarget, searchValue, pools.isEmpty() ? null : pools.get(0),
                    integer(action, "guaranteed", 1), queueMode, successCounter, successIncrement,
                    milestoneEvery, milestoneTarget, milestoneValue, milestoneGuaranteed
            );
        }
        if (triggerAction == TriggerAction.MODIFY_ENTITY_ATTRIBUTE) {
            ResourceLocation attribute = ResourceLocation.tryParse(string(action, "attribute"));
            String operation = string(action, "operation");
            String target = string(action, "target");
            if (attribute == null || !(operation.equals("add") || operation.equals("multiply"))
                    || !isExtendedTarget(target)) return null;
            return new TriggerActionDraft(
                    triggerAction, attribute, 100, 0,
                    target, false, true, true, operation, decimal(action, "amount", 0.0D),
                    decimal(action, "minimum", -1.0E9D), decimal(action, "maximum", 1.0E9D),
                    decimal(action, "radius", 4.0D)
            );
        }
        if (triggerAction == TriggerAction.DAMAGE_ENTITY || triggerAction == TriggerAction.HEAL_ENTITY) {
            String target = string(action, "target");
            if (!isExtendedTarget(target)) return null;
            return new TriggerActionDraft(
                    triggerAction, new ResourceLocation("minecraft", "speed"), 100, 0,
                    target, false, true, true, "add", decimal(action, "amount", 2.0D),
                    0.0D, 1.0E9D, decimal(action, "radius", 4.0D)
            );
        }
        ResourceLocation effect = new ResourceLocation("minecraft", "speed");
        String target = "self";
        String damageOperation = string(action, "operation");
        if (triggerAction == TriggerAction.MODIFY_DAMAGE) {
            if (!(damageOperation.equals("add") || damageOperation.equals("multiply"))) return null;
        } else if (triggerAction != TriggerAction.CANCEL_DAMAGE) {
            effect = ResourceLocation.tryParse(string(action, "effect"));
            target = string(action, "target");
            if (effect == null || !(target.equals("self") || target.equals("event_entity"))) return null;
        }
        return new TriggerActionDraft(triggerAction, effect,
                integer(action, "durationTicks", 100), integer(action, "amplifier", 0), target,
                booleanValue(action, "ambient", false), booleanValue(action, "showParticles", true),
                booleanValue(action, "showIcon", true),
                damageOperation.isBlank() ? "add" : damageOperation,
                decimal(action, "amount", 0.0D), decimal(action, "minimum", 0.0D),
                decimal(action, "maximum", 1.0E9D)
        );
    }

    private static boolean isExtendedTarget(String target) {
        return Set.of("self", "event_entity", "damage_source", "nearby").contains(target);
    }

    private JsonArray triggerArray() {
        JsonElement current = root.get("triggers");
        if (current != null && current.isJsonArray()) return current.getAsJsonArray();
        JsonArray created = new JsonArray();
        root.add("triggers", created);
        return created;
    }

    public void replaceTriggersFrom(TalentDraft source) {
        JsonElement triggers = source.root.get("triggers");
        if (triggers == null) root.remove("triggers");
        else root.add("triggers", triggers.deepCopy());
    }

    private static String normalizeLocalId(String raw) {
        String prefix = TalentEditorMod.MOD_ID + ":";
        return raw.startsWith(prefix) ? raw.substring(prefix.length()) : raw;
    }

    public void updateAttribute(int rawIndex, double amount, String operation) {
        JsonObject effect = effect(rawIndex);
        if (!isAttributeEffect(effect)) throw new IllegalArgumentException("Effect is not an attribute modifier");
        effect.addProperty("amount", amount);
        effect.addProperty("operation", operation);
    }

    public void updateMaintainedEffectAmplifier(int rawIndex, int amplifier) {
        JsonObject effect = effect(rawIndex);
        if (!isMaintainedEffect(effect)) throw new IllegalArgumentException("Effect is not maintained");
        effect.addProperty("amplifier", amplifier);
    }

    public void updateDrawRefreshBonus(int rawIndex, int amount) {
        JsonObject effect = effect(rawIndex);
        if (!isDrawRefreshBonus(effect)) throw new IllegalArgumentException("Effect is not a refresh bonus");
        effect.addProperty("amount", amount);
    }

    public void removeAttribute(int rawIndex) {
        JsonObject effect = effect(rawIndex);
        if (!isAttributeEffect(effect)) throw new IllegalArgumentException("Effect is not an attribute modifier");
        effects.remove(rawIndex);
    }

    public void removeEditableActiveEffect(int rawIndex) {
        JsonObject effect = effect(rawIndex);
        if (!isAttributeEffect(effect) && !isEffectImmunity(effect) && !isMaintainedEffect(effect)
                && !isDrawRefreshBonus(effect) && !isDynamicAttribute(effect) && !isCreativeFlight(effect)) {
            throw new IllegalArgumentException("Effect is not editable");
        }
        effects.remove(rawIndex);
    }

    public String toJson(String displayName) {
        prepareNameLiteral(displayName);
        JsonArray serializedEffects = new JsonArray();
        effects.forEach(effect -> serializedEffects.add(effect.deepCopy()));
        root.add("activeEffects", serializedEffects);
        return GSON.toJson(root);
    }

    private List<String> descriptionLines() {
        JsonObject text = object(root, "text");
        if (text == null) return List.of();
        JsonElement raw = text.get("description");
        if (raw == null) return List.of();
        ArrayList<String> result = new ArrayList<>();
        if (raw.isJsonArray()) {
            for (JsonElement element : raw.getAsJsonArray()) result.add(localizedFallback(element));
        } else {
            result.add(localizedFallback(raw));
        }
        return List.copyOf(result);
    }

    private static String descriptionEditorText(List<String> descriptions) {
        return descriptions.stream()
                .map(value -> value.replace("\\", "\\\\").replace("\n", "\\n"))
                .reduce((left, right) -> left + "\\n" + right)
                .orElse("");
    }

    private static String localizedFallback(JsonElement element) {
        return LocalizedTextDraft.parse(element).fallbackText();
    }

    private JsonObject effect(int rawIndex) {
        if (rawIndex < 0 || rawIndex >= effects.size()) {
            throw new IllegalArgumentException("Invalid effect index " + rawIndex);
        }
        return effects.get(rawIndex);
    }

    private static boolean isAttributeEffect(JsonObject effect) {
        String type = type(effect);
        return type.equals("attribute_modifier") || type.equals(ATTRIBUTE_TYPE);
    }

    private static boolean isEffectImmunity(JsonObject effect) {
        String type = type(effect);
        return type.equals("effect_immunity") || type.equals(EFFECT_IMMUNITY_TYPE);
    }

    private static boolean isMaintainedEffect(JsonObject effect) {
        String value = type(effect);
        return (value.equals("add_effect") || value.equals(ADD_EFFECT_TYPE))
                && string(effect, "target").equals("self")
                && EDITABLE_ADD_EFFECT_KEYS.containsAll(effect.keySet());
    }

    private static boolean isDrawRefreshBonus(JsonObject effect) {
        String value = type(effect);
        return (value.equals("draw_refresh_bonus") || value.equals(DRAW_REFRESH_BONUS_TYPE))
                && Set.of("type", "amount", "pools").containsAll(effect.keySet());
    }

    private static boolean isDynamicAttribute(JsonObject effect) {
        String value = type(effect);
        String source = string(effect, "source");
        boolean sourceValid = Set.of("counter", "health_percent", "hunger_percent").contains(source);
        boolean counterValid = source.equals("counter") == !string(effect, "counter").isBlank();
        return (value.equals("dynamic_attribute_modifier") || value.equals(DYNAMIC_ATTRIBUTE_TYPE))
                && EDITABLE_DYNAMIC_ATTRIBUTE_KEYS.containsAll(effect.keySet()) && sourceValid && counterValid;
    }

    private static boolean isCreativeFlight(JsonObject effect) {
        String value = type(effect);
        return (value.equals("creative_flight") || value.equals(CREATIVE_FLIGHT_TYPE))
                && Set.of("type", "enabledWhen", "disabledWhen").containsAll(effect.keySet());
    }

    private String nextModifierKey() {
        Set<String> used = new java.util.HashSet<>();
        effects.forEach(effect -> {
            String key = string(effect, "modifierKey");
            if (!key.isBlank()) used.add(key);
        });
        for (int index = 1; index <= 10_000; index++) {
            String candidate = "dynamic_" + index;
            if (!used.contains(candidate)) return candidate;
        }
        throw new IllegalStateException("Too many dynamic attribute keys");
    }

    private static String type(JsonObject effect) {
        return string(effect, "type");
    }

    private static String operation(JsonObject effect) {
        String value = string(effect, "operation");
        return value.isBlank() ? "addition" : value;
    }

    private static JsonObject object(JsonObject parent, String key) {
        JsonElement element = parent.get(key);
        return element != null && element.isJsonObject() ? element.getAsJsonObject() : null;
    }

    private static String string(JsonObject object, String key) {
        JsonElement element = object.get(key);
        try {
            return element != null && element.isJsonPrimitive() ? element.getAsString() : "";
        } catch (RuntimeException ignored) {
            return "";
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

    private static int integer(JsonObject object, String key, int fallback) {
        JsonElement element = object.get(key);
        try {
            return element == null ? fallback : element.getAsInt();
        } catch (RuntimeException ignored) {
            return fallback;
        }
    }

    private static long longInteger(JsonObject object, String key, long fallback) {
        try {
            return object.has(key) ? object.get(key).getAsLong() : fallback;
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

    private static RequirementRow parseRequirement(JsonObject object) {
        RequirementType type = RequirementType.fromType(string(object, "type"));
        if (type == null || !object.keySet().equals(type.allowedKeys())) return null;
        ResourceLocation target = ResourceLocation.tryParse(string(object, type.targetKey()));
        if (target == null) return null;
        long amount = switch (type) {
            case TALENT -> 1L;
            case COUNTER, COUNTER_CATEGORY -> longValue(object, "value", -1L);
            case ITEM, CATEGORY -> longValue(object, "count", -1L);
            case ATTRIBUTE -> 1L;
        };
        if (amount < 0L) return null;
        if (type == RequirementType.ATTRIBUTE) {
            String operator = string(object, "operator");
            if (!java.util.Set.of("lt", "lte", "eq", "neq", "gte", "gt").contains(operator)) return null;
            try {
                double value = object.get("value").getAsDouble();
                if (!Double.isFinite(value) || value < -1.0E9D || value > 1.0E9D) return null;
                return new RequirementRow(type, target, amount, operator, value);
            } catch (RuntimeException exception) {
                return null;
            }
        }
        return new RequirementRow(type, target, amount);
    }

    private static long longValue(JsonObject object, String key, long fallback) {
        JsonElement element = object.get(key);
        try {
            if (element == null || !element.isJsonPrimitive()
                    || !element.getAsString().matches("[0-9]+")) return fallback;
            return element.getAsLong();
        } catch (RuntimeException ignored) {
            return fallback;
        }
    }

    private static Map<ResourceLocation, Long> resourceLongMap(JsonObject parent, String key) {
        JsonObject source = object(parent, key);
        if (source == null) return Map.of();
        LinkedHashMap<ResourceLocation, Long> result = new LinkedHashMap<>();
        source.entrySet().forEach(entry -> {
            ResourceLocation id = ResourceLocation.tryParse(entry.getKey());
            try {
                long value = entry.getValue().getAsLong();
                if (id != null && value > 0L) result.put(id, value);
            } catch (RuntimeException ignored) {
            }
        });
        return Map.copyOf(result);
    }

    private static Map<ResourceLocation, Integer> resourceIntMap(JsonObject parent, String key) {
        LinkedHashMap<ResourceLocation, Integer> result = new LinkedHashMap<>();
        resourceLongMap(parent, key).forEach((id, value) -> {
            if (value <= Integer.MAX_VALUE) result.put(id, value.intValue());
        });
        return Map.copyOf(result);
    }

    private static List<ResourceLocation> resourceList(JsonObject parent, String key) {
        JsonElement raw = parent.get(key);
        if (raw == null) return List.of();
        if (!raw.isJsonArray()) return List.of();
        ArrayList<ResourceLocation> result = new ArrayList<>();
        for (JsonElement element : raw.getAsJsonArray()) {
            if (!element.isJsonPrimitive()) return List.of();
            ResourceLocation value = ResourceLocation.tryParse(element.getAsString());
            if (value == null) return List.of();
            result.add(value);
        }
        return List.copyOf(result);
    }

    public record OfferDraft(boolean enabled, double weight, List<ResourceLocation> pools) {
        public OfferDraft {
            pools = List.copyOf(pools);
        }
    }

    public record MetadataDraft(
            boolean enabled,
            ResourceLocation rarity,
            boolean showGeneratedEffects,
            List<ResourceLocation> categories,
            Optional<ResourceLocation> icon,
            int capacityCost
    ) {
        public MetadataDraft {
            categories = List.copyOf(categories);
            icon = icon == null ? Optional.empty() : icon;
        }
    }

    public enum EditorSection {
        TEXT,
        METADATA,
        PRESENTATION,
        OFFER,
        REQUIREMENTS,
        SYNTHESIS,
        LIFECYCLE,
        BASIC_EFFECTS,
        SPECIAL_EFFECTS,
        ACTIVE_INTERFACES,
        TRIGGERS,
        ON_ACQUIRE_ACTIONS,
        ON_SYNTHESIZE_ACTIONS,
        ON_REMOVE_ACTIONS,
        RAW_JSON
    }

    public record EditorSectionState(boolean configured, int count, boolean advancedOnly) {
    }

    public record EditorOverview(Map<EditorSection, EditorSectionState> sections) {
        public EditorOverview {
            sections = Map.copyOf(sections);
        }

        public EditorSectionState section(EditorSection section) {
            return sections.getOrDefault(section, new EditorSectionState(false, 0, false));
        }
    }

    public record TextStylesDraft(
            LocalizedTextDraft name,
            List<LocalizedTextDraft> descriptions
    ) {
        public TextStylesDraft {
            descriptions = List.copyOf(descriptions);
        }
    }

    public record EffectRow(
            int rawIndex,
            boolean editableAttribute,
            boolean editableImmunity,
            boolean editableMaintainedEffect,
            boolean editableDrawRefreshBonus,
            boolean editableDynamicAttribute,
            boolean editableCreativeFlight,
            String idOrType,
            double amount,
            String operation,
            String dynamicSource
    ) {
    }

    public record DynamicAttributeDraft(
            ResourceLocation attribute,
            String source,
            Optional<ResourceLocation> counter,
            double base,
            double scale,
            double minimum,
            double maximum,
            String operation,
            String modifierKey,
            boolean showInDescription
    ) {
        public DynamicAttributeDraft {
            counter = counter == null ? Optional.empty() : counter;
        }
    }

    public record LifecycleDraft(
            int durationTicks,
            int removeAfterDraws,
            boolean retainEffects,
            boolean retainCategoryContribution,
            boolean retainTriggers
    ) {
    }

    public record AdvancedTriggerRow(
            int rawIndex,
            String id,
            String event,
            boolean editable,
            boolean expressionCondition,
            int actionCount,
            int failureActionCount
    ) {
    }

    public record ExpressionConditionDraft(
            boolean expression,
            boolean hadCondition,
            String left,
            String operator,
            String right
    ) {
    }

    public record AdvancedActionRow(int rawIndex, String type, boolean editable) {
    }

    public enum AdvancedActionLocation {
        ACTIVE_EFFECTS("activeEffects", false),
        ON_ACQUIRE("onAcquire", false),
        ON_SYNTHESIZE("onSynthesize", false),
        ON_REMOVE("onRemove", false),
        TRIGGER_ACTIONS("", true),
        TRIGGER_FAILURE_ACTIONS("", true);

        private final String rootField;
        private final boolean triggerScoped;

        AdvancedActionLocation(String rootField, boolean triggerScoped) {
            this.rootField = rootField;
            this.triggerScoped = triggerScoped;
        }

        public String rootField() { return rootField; }
        public boolean triggerScoped() { return triggerScoped; }
    }

    public record TriggerRow(int rawIndex, boolean editable, TriggerDraft draft, String id, String event) {
        static TriggerRow unsupported(int index, String id, String event) {
            return new TriggerRow(index, false, null, id.isBlank() ? "?" : id, event.isBlank() ? "?" : event);
        }
    }

    public record TriggerDraft(
            String id, String event, int priority, double chance, int cooldownTicks,
            int activationLimit,
            ResourceLocation chanceAttribute, ResourceLocation chanceCounter, ResourceLocation chanceCategory,
            double chancePerPoint,
            double distanceInterval,
            TriggerFilter filter, ResourceLocation filterValue,
            List<TriggerActionDraft> actions,
            String actionFailurePolicy,
            List<TriggerActionDraft> failureActions
    ) {
        public TriggerDraft {
            actions = List.copyOf(actions);
            if (actions.isEmpty()) throw new IllegalArgumentException("A trigger requires at least one action");
            actionFailurePolicy = actionFailurePolicy == null ? "continue" : actionFailurePolicy;
            if (!Set.of("continue", "stop").contains(actionFailurePolicy)) {
                throw new IllegalArgumentException("Invalid action failure policy");
            }
            failureActions = List.copyOf(failureActions);
        }

        public TriggerDraft(
                String id, String event, int priority, double chance, int cooldownTicks,
                int activationLimit,
                ResourceLocation chanceAttribute, ResourceLocation chanceCounter, ResourceLocation chanceCategory,
                double chancePerPoint,
                double distanceInterval,
                TriggerFilter filter, ResourceLocation filterValue,
                List<TriggerActionDraft> actions
        ) {
            this(id, event, priority, chance, cooldownTicks, activationLimit, chanceAttribute, chanceCounter,
                    chanceCategory, chancePerPoint, distanceInterval, filter, filterValue, actions,
                    "continue", List.of());
        }

        public TriggerDraft(
                String id, String event, int priority, double chance, int cooldownTicks,
                int activationLimit, ResourceLocation chanceAttribute, double chancePerPoint,
                TriggerFilter filter, ResourceLocation filterValue, List<TriggerActionDraft> actions
        ) {
            this(id, event, priority, chance, cooldownTicks, activationLimit, chanceAttribute, null, null,
                    chancePerPoint, 1.0D, filter, filterValue, actions, "continue", List.of());
        }

        public TriggerDraft(
                String id, String event, int priority, double chance, int cooldownTicks,
                TriggerAction action,
                ResourceLocation effect, int durationTicks, int amplifier,
                String target, boolean ambient, boolean showParticles, boolean showIcon,
                TriggerFilter filter, ResourceLocation filterValue,
                String damageOperation, double damageAmount, double damageMinimum, double damageMaximum
        ) {
            this(id, event, priority, chance, cooldownTicks, 0, null, null, null, 0.0D, 1.0D,
                    filter, filterValue,
                    List.of(new TriggerActionDraft(
                            action, effect, durationTicks, amplifier, target, ambient, showParticles, showIcon,
                            damageOperation, damageAmount, damageMinimum, damageMaximum
                    )), "continue", List.of());
        }

        private TriggerActionDraft firstAction() { return actions.get(0); }
        public TriggerAction action() { return firstAction().action(); }
        public ResourceLocation effect() { return firstAction().effect(); }
        public int durationTicks() { return firstAction().durationTicks(); }
        public int amplifier() { return firstAction().amplifier(); }
        public String target() { return firstAction().target(); }
        public boolean ambient() { return firstAction().ambient(); }
        public boolean showParticles() { return firstAction().showParticles(); }
        public boolean showIcon() { return firstAction().showIcon(); }
        public String damageOperation() { return firstAction().damageOperation(); }
        public double damageAmount() { return firstAction().damageAmount(); }
        public double damageMinimum() { return firstAction().damageMinimum(); }
        public double damageMaximum() { return firstAction().damageMaximum(); }
    }

    public record TriggerActionDraft(
            TriggerAction action,
            ResourceLocation effect, int durationTicks, int amplifier,
            String target, boolean ambient, boolean showParticles, boolean showIcon,
            String damageOperation, double damageAmount, double damageMinimum, double damageMaximum,
            DrawSearchTarget searchTarget, ResourceLocation searchValue,
            ResourceLocation searchPool, int searchGuaranteed,
            String searchQueueMode, ResourceLocation searchSuccessCounter, long searchSuccessIncrement,
            int searchMilestoneEvery, DrawSearchTarget searchMilestoneTarget,
            ResourceLocation searchMilestoneValue, int searchMilestoneGuaranteed,
            double entityRadius
    ) {
        public TriggerActionDraft {
            searchQueueMode = searchQueueMode == null ? "append" : searchQueueMode;
            searchMilestoneTarget = searchMilestoneTarget == null ? DrawSearchTarget.TAG : searchMilestoneTarget;
        }

        public TriggerActionDraft(
                TriggerAction action,
                ResourceLocation effect, int durationTicks, int amplifier,
                String target, boolean ambient, boolean showParticles, boolean showIcon,
                String damageOperation, double damageAmount, double damageMinimum, double damageMaximum,
                DrawSearchTarget searchTarget, ResourceLocation searchValue,
                ResourceLocation searchPool, int searchGuaranteed,
                String searchQueueMode, ResourceLocation searchSuccessCounter, long searchSuccessIncrement,
                int searchMilestoneEvery, DrawSearchTarget searchMilestoneTarget,
                ResourceLocation searchMilestoneValue, int searchMilestoneGuaranteed
        ) {
            this(action, effect, durationTicks, amplifier, target, ambient, showParticles, showIcon,
                    damageOperation, damageAmount, damageMinimum, damageMaximum,
                    searchTarget, searchValue, searchPool, searchGuaranteed, searchQueueMode,
                    searchSuccessCounter, searchSuccessIncrement, searchMilestoneEvery,
                    searchMilestoneTarget, searchMilestoneValue, searchMilestoneGuaranteed, 4.0D);
        }

        public TriggerActionDraft(
                TriggerAction action,
                ResourceLocation effect, int durationTicks, int amplifier,
                String target, boolean ambient, boolean showParticles, boolean showIcon,
                String damageOperation, double damageAmount, double damageMinimum, double damageMaximum
        ) {
            this(action, effect, durationTicks, amplifier, target, ambient, showParticles, showIcon,
                    damageOperation, damageAmount, damageMinimum, damageMaximum,
                    DrawSearchTarget.TALENT, new ResourceLocation("custom", "target_talent"), null, 1,
                    "append", null, 0L, 0, DrawSearchTarget.TAG, null, 1, 4.0D);
        }

        public TriggerActionDraft(
                TriggerAction action,
                ResourceLocation effect, int durationTicks, int amplifier,
                String target, boolean ambient, boolean showParticles, boolean showIcon,
                String damageOperation, double damageAmount, double damageMinimum, double damageMaximum,
                double entityRadius
        ) {
            this(action, effect, durationTicks, amplifier, target, ambient, showParticles, showIcon,
                    damageOperation, damageAmount, damageMinimum, damageMaximum,
                    DrawSearchTarget.TALENT, new ResourceLocation("custom", "target_talent"), null, 1,
                    "append", null, 0L, 0, DrawSearchTarget.TAG, null, 1, entityRadius);
        }

        public static TriggerActionDraft defaults() {
            return new TriggerActionDraft(
                    TriggerAction.ADD_EFFECT, new ResourceLocation("minecraft", "speed"), 100, 0,
                    "event_entity", false, true, true, "add", 0.0D, 0.0D, 1.0E9D
            );
        }
    }

    public record PresentationDraft(
            boolean editable,
            boolean soundEnabled,
            ResourceLocation sound,
            String soundSource,
            double volume,
            double pitch,
            boolean messageEnabled,
            String mode,
            LocalizedTextDraft text,
            boolean subtitleEnabled,
            LocalizedTextDraft subtitle,
            int fadeInTicks,
            int stayTicks,
            int fadeOutTicks
    ) {
        static PresentationDraft defaults() {
            return new PresentationDraft(
                    true, false, new ResourceLocation("minecraft", "entity.player.levelup"),
                    "players", 1.0D, 1.0D, false, "chat", LocalizedTextDraft.literal(""),
                    false, LocalizedTextDraft.literal(""), 10, 70, 20
            );
        }

        static PresentationDraft unsupported() {
            PresentationDraft defaults = defaults();
            return new PresentationDraft(
                    false, defaults.soundEnabled(), defaults.sound(), defaults.soundSource(),
                    defaults.volume(), defaults.pitch(), defaults.messageEnabled(), defaults.mode(),
                    defaults.text(), defaults.subtitleEnabled(), defaults.subtitle(),
                    defaults.fadeInTicks(), defaults.stayTicks(), defaults.fadeOutTicks()
            );
        }
    }

    public enum PresentationPhase {
        ACQUIRE("onAcquire"),
        SYNTHESIZE("onSynthesize");

        private final String field;

        PresentationPhase(String field) {
            this.field = field;
        }

        public String field() {
            return field;
        }
    }

    public enum TriggerAction {
        ADD_EFFECT(ADD_EFFECT_TYPE),
        REMOVE_EFFECT(REMOVE_EFFECT_TYPE),
        MODIFY_DAMAGE(MODIFY_DAMAGE_TYPE),
        CANCEL_DAMAGE(CANCEL_DAMAGE_TYPE),
        MODIFY_ENTITY_ATTRIBUTE(MODIFY_ENTITY_ATTRIBUTE_TYPE),
        DAMAGE_ENTITY(DAMAGE_ENTITY_TYPE),
        HEAL_ENTITY(HEAL_ENTITY_TYPE),
        QUEUE_DRAW_CONSTRAINT(QUEUE_DRAW_CONSTRAINT_TYPE);

        private final String serializedName;

        TriggerAction(String serializedName) {
            this.serializedName = serializedName;
        }

        public String serializedName() { return serializedName; }

        static TriggerAction fromSerialized(String value) {
            for (TriggerAction action : values()) {
                if (normalizeLocalId(action.serializedName).equals(value)) return action;
            }
            return null;
        }
    }

    public enum DrawSearchTarget {
        TALENT("talents"),
        CATEGORY("categories"),
        RARITY("rarities"),
        TAG("tags");

        private final String field;

        DrawSearchTarget(String field) {
            this.field = field;
        }

        public String field() {
            return field;
        }
    }

    public enum TriggerFilter {
        NONE("", ""),
        ENTITY_TYPE("event_entity_type", "entityType"),
        ENTITY_TAG("event_entity_tag", "tag"),
        ENTITY_EFFECT("event_entity_effect", "effect"),
        DAMAGE_TAG("damage_type_tag", "tag"),
        EVENT_TALENT("event_talent", "talent"),
        EVENT_TALENT_CATEGORY("event_talent_category", "category"),
        EVENT_TALENT_RARITY("event_talent_rarity", "rarity"),
        ITEM("event_item", "item"),
        ITEM_TAG("event_item_tag", "tag"),
        ITEM_EDIBLE("event_item_edible", ""),
        BLOCK("event_block", "block"),
        BLOCK_TAG("event_block_tag", "tag"),
        CURIOS_EQUIPPED("curios_equipped", "item");

        private final String serializedName;
        private final String field;

        TriggerFilter(String serializedName, String field) {
            this.serializedName = serializedName;
            this.field = field;
        }

        public String serializedName() { return serializedName; }
        public String field() { return field; }
        public boolean requiresValue() { return !field.isBlank(); }

        static TriggerFilter fromSerialized(String value) {
            for (TriggerFilter filter : values()) {
                if (filter.serializedName.equals(value)) return filter;
            }
            return null;
        }
    }

    public enum RequirementMode {
        ALL("all"),
        ANY("any");

        private final String serializedName;

        RequirementMode(String serializedName) {
            this.serializedName = serializedName;
        }

        public String serializedName() {
            return serializedName;
        }

        private static RequirementMode fromType(String raw) {
            String normalized = raw.startsWith(TalentEditorMod.MOD_ID + ":")
                    ? raw.substring(TalentEditorMod.MOD_ID.length() + 1) : raw;
            for (RequirementMode value : values()) {
                if (value.serializedName.equals(normalized)) return value;
            }
            return null;
        }
    }

    public enum RequirementType {
        TALENT("has_talent", "talent", Set.of("type", "talent")),
        COUNTER("counter_at_least", "counter", Set.of("type", "counter", "value")),
        COUNTER_CATEGORY("counter_category_total_at_least", "category", Set.of("type", "category", "value")),
        CATEGORY("category_count_at_least", "category", Set.of("type", "category", "count")),
        ITEM("has_item", "item", Set.of("type", "item", "count")),
        ATTRIBUTE("attribute_compare", "attribute", Set.of("type", "attribute", "operator", "value"));

        private final String serializedName;
        private final String targetKey;
        private final Set<String> allowedKeys;

        RequirementType(String serializedName, String targetKey, Set<String> allowedKeys) {
            this.serializedName = serializedName;
            this.targetKey = targetKey;
            this.allowedKeys = allowedKeys;
        }

        public String serializedName() {
            return serializedName;
        }

        public String targetKey() {
            return targetKey;
        }

        private Set<String> allowedKeys() {
            return allowedKeys;
        }

        private static RequirementType fromType(String raw) {
            String normalized = raw.startsWith(TalentEditorMod.MOD_ID + ":")
                    ? raw.substring(TalentEditorMod.MOD_ID.length() + 1) : raw;
            for (RequirementType value : values()) {
                if (value.serializedName.equals(normalized)) return value;
            }
            return null;
        }
    }

    public record RequirementRow(RequirementType type, ResourceLocation target, long amount,
                                 String operator, double attributeValue) {
        public RequirementRow(RequirementType type, ResourceLocation target, long amount) {
            this(type, target, amount, "gte", amount);
        }
    }

    public record RequirementsDraft(
            boolean editable,
            RequirementMode mode,
            List<RequirementRow> rows
    ) {
        public RequirementsDraft {
            rows = List.copyOf(rows);
        }
    }

    public record SynthesisDraft(
            boolean enabled,
            List<ResourceLocation> consumeTalents,
            Map<ResourceLocation, Long> consumeCounters,
            Map<ResourceLocation, Integer> consumeItems
    ) {
        public SynthesisDraft {
            consumeTalents = List.copyOf(consumeTalents);
            consumeCounters = Map.copyOf(consumeCounters);
            consumeItems = Map.copyOf(consumeItems);
        }
    }
}
