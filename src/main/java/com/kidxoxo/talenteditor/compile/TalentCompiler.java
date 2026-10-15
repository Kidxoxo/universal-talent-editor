package com.kidxoxo.talenteditor.compile;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import com.kidxoxo.talenteditor.TalentEditorMod;
import com.kidxoxo.talenteditor.domain.AttributeModifierAction;
import com.kidxoxo.talenteditor.domain.CancelDamageAction;
import com.kidxoxo.talenteditor.domain.CompiledAction;
import com.kidxoxo.talenteditor.domain.ConditionalAction;
import com.kidxoxo.talenteditor.domain.CompiledCondition;
import com.kidxoxo.talenteditor.domain.CompiledTrigger;
import com.kidxoxo.talenteditor.domain.ConfiguredItemDefinition;
import com.kidxoxo.talenteditor.domain.ConfiguredItemAttributeModifier;
import com.kidxoxo.talenteditor.domain.ConfiguredItemLootRule;
import com.kidxoxo.talenteditor.domain.ConfiguredItemLootEntityTarget;
import com.kidxoxo.talenteditor.domain.ConfiguredItemLootTableMatch;
import com.kidxoxo.talenteditor.domain.ConfiguredItemSlot;
import com.kidxoxo.talenteditor.domain.ConfiguredItemUseDefinition;
import com.kidxoxo.talenteditor.domain.ActionTarget;
import com.kidxoxo.talenteditor.domain.ActionFailurePolicy;
import com.kidxoxo.talenteditor.domain.AddEffectAction;
import com.kidxoxo.talenteditor.domain.RemoveEffectAction;
import com.kidxoxo.talenteditor.domain.AttributeOperation;
import com.kidxoxo.talenteditor.domain.CounterAmountMode;
import com.kidxoxo.talenteditor.domain.CounterEventType;
import com.kidxoxo.talenteditor.domain.CounterRounding;
import com.kidxoxo.talenteditor.domain.CounterRuleDefinition;
import com.kidxoxo.talenteditor.domain.CounterVisibility;
import com.kidxoxo.talenteditor.domain.DuplicatePolicy;
import com.kidxoxo.talenteditor.domain.DrawRefreshBonusAction;
import com.kidxoxo.talenteditor.domain.DynamicAttributeModifierAction;
import com.kidxoxo.talenteditor.domain.CreativeFlightAction;
import com.kidxoxo.talenteditor.domain.LocalizedText;
import com.kidxoxo.talenteditor.domain.ModifyDamageAction;
import com.kidxoxo.talenteditor.domain.ModifyDamageExpressionAction;
import com.kidxoxo.talenteditor.domain.GrantTalentAction;
import com.kidxoxo.talenteditor.domain.RevokeTalentAction;
import com.kidxoxo.talenteditor.domain.ModifyCounterAction;
import com.kidxoxo.talenteditor.domain.OfferSettings;
import com.kidxoxo.talenteditor.domain.RarityDefinition;
import com.kidxoxo.talenteditor.domain.QueueDrawConstraintAction;
import com.kidxoxo.talenteditor.domain.TalentDefinition;
import com.kidxoxo.talenteditor.domain.TalentLifecycleDefinition;
import com.kidxoxo.talenteditor.domain.TalentBookLayoutDefinition;
import com.kidxoxo.talenteditor.domain.TalentPoolDefinition;
import com.kidxoxo.talenteditor.domain.SynthesisDefinition;
import com.kidxoxo.talenteditor.runtime.RuntimeSnapshot;
import com.kidxoxo.talenteditor.validation.ValidationIssue;
import com.kidxoxo.talenteditor.validation.ValidationReport;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public final class TalentCompiler {
    public static final int SCHEMA_VERSION = 2;
    private static final int MAX_SYNTHESIS_COSTS = 256;

    private static final Set<String> RARITY_FIELDS = Set.of(
            "schema", "enabled", "name", "color", "order", "offerable", "baseWeight"
    );
    private static final Set<String> TALENT_FIELDS = Set.of(
            "schema", "enabled", "text", "rarity", "categories", "icon", "offer", "capacityCost",
            "tags", "requiredMods", "acquireCondition", "synthesis", "activeEffects", "triggers",
            "lifecycle", "onAcquire", "onSynthesize", "onRemove"
    );
    private static final Set<String> SYNTHESIS_FIELDS = Set.of(
            "enabled", "consumeTalents", "consumeCounters", "consumeItems"
    );
    private static final Set<String> LIFECYCLE_FIELDS = Set.of(
            "durationTicks", "removeAfterDraws", "retainEffects", "retainCategoryContribution",
            "retainTriggers"
    );
    private static final Set<String> POOL_FIELDS = Set.of(
            "schema", "enabled", "name", "choices", "refreshes", "maxOwned", "iconSize",
            "excludeOwned", "rarityWeights", "luckAttribute", "luckFactors", "pityRarity", "pityAfter",
            "duplicatePolicy", "duplicateCounter", "duplicateCounterAmount"
    );
    private static final Set<String> COUNTER_FIELDS = Set.of(
            "schema", "enabled", "name", "event", "increment", "amountMode", "amountScale", "rounding",
            "minimumIncrement", "maximum", "cooldownTicks",
            "intervalTicks", "resetOnLogin", "resetOnDeath", "resetIntervalTicks",
            "categories", "visibility", "displayCategory", "requiredMods", "requiredTalents", "entityTypes", "entityTags",
            "items", "itemTags", "damageTags"
    );
    private static final Set<String> LAYOUT_FIELDS = Set.of(
            "schema", "enabled", "priority", "background", "panelPercent", "columns", "rows",
            "slotWidth", "slotHeight", "iconSize", "showGeneratedEffects", "slots", "pages", "bookUi", "drawUi"
    );
    private static final Set<String> LAYOUT_SLOT_FIELDS = Set.of(
            "id", "x", "y", "width", "height", "iconSize", "talent"
    );
    private static final Set<String> LAYOUT_PAGE_FIELDS = Set.of("id", "background", "slots");
    private static final int MAX_LAYOUT_SLOTS = 256;
    private static final int MAX_LAYOUT_PAGES = 64;
    private static final int MAX_LAYOUT_TOTAL_PAGE_SLOTS = 4_096;
    private static final int MAX_TRIGGERS = 256;
    private static final int MAX_ACTIONS_PER_LIST = 256;
    private static final Set<ResourceLocation> DAMAGE_TRIGGER_EVENTS = Set.of(
            new ResourceLocation(TalentEditorMod.MOD_ID, "damage_dealt_pre"),
            new ResourceLocation(TalentEditorMod.MOD_ID, "damage_taken_pre"),
            new ResourceLocation(TalentEditorMod.MOD_ID, "damage_dealt"),
            new ResourceLocation(TalentEditorMod.MOD_ID, "damage_taken"),
            new ResourceLocation(TalentEditorMod.MOD_ID, "fatal_damage")
    );
    private static final Set<String> TEXT_FIELDS = Set.of("name", "description", "showGeneratedEffects");
    private static final Set<String> OFFER_FIELDS = Set.of("enabled", "weight", "pools");
    private static final Set<String> TRIGGER_FIELDS = Set.of(
            "id", "event", "priority", "chance", "chanceAttribute", "chanceCounter", "chanceCategory", "chancePerPoint",
            "distanceInterval", "cooldownTicks", "activationLimit", "condition", "actions",
            "actionFailurePolicy", "failureActions"
    );
    private static final Set<String> CONFIGURED_ITEM_FIELDS = Set.of(
            "schema", "enabled", "name", "description", "customModelData", "glint",
            "attributeModifiers", "use", "triggers", "loot", "requiredMods", "curiosEquippable",
            "mainhandOnly", "curiosSlots"
    );
    private static final Set<String> CONFIGURED_ITEM_ATTRIBUTE_FIELDS = Set.of(
            "attribute", "amount", "operation", "slot", "slots", "modifierKey"
    );
    private static final Set<String> CONFIGURED_ITEM_USE_FIELDS = Set.of(
            "enabled", "cooldownTicks", "consumeCount", "actions"
    );
    private static final Set<String> CONFIGURED_ITEM_LOOT_FIELDS = Set.of(
            "table", "tableMatch", "chance", "chancePerLuck", "minCount", "maxCount",
            "dimensions", "entityTarget", "entityTypes", "entityTags"
    );
    private static final int MAX_CONFIGURED_ITEM_LOOT_RULES = 256;
    private static final int MAX_CONFIGURED_ITEM_PATTERN_LOOT_RULES = 4_096;
    private static final int MAX_CONFIGURED_ITEM_ATTRIBUTE_MODIFIERS = 256;
    private static final String MOD_ID_PATTERN = "[a-z][a-z0-9_-]{1,63}";
    private static final String LOCAL_KEY_PATTERN = "[a-z0-9_.-]{1,96}";

    private final ActionTypeRegistry actionTypes;

    public TalentCompiler(ActionTypeRegistry actionTypes) {
        this.actionTypes = actionTypes;
    }

    public static TalentCompiler withBuiltins() {
        return new TalentCompiler(BuiltinActionTypes.create());
    }

    public CompilationResult compile(RawDefinitionSet raw, CompilationEnvironment environment) {
        ValidationReport.Builder report = ValidationReport.builder();
        for (ValidationIssue issue : raw.loadingIssues()) report.add(issue);
        CompilationEnvironment actionEnvironment = withAdditionalItems(
                environment, raw.configuredItems().keySet()
        );

        LinkedHashMap<ResourceLocation, RarityDefinition> rarities = new LinkedHashMap<>();
        raw.rarities().entrySet().stream()
                .sorted(Map.Entry.comparingByKey(Comparator.comparing(ResourceLocation::toString)))
                .forEach(entry -> rarities.put(entry.getKey(), compileRarity(entry.getKey(), entry.getValue(), report)));

        LinkedHashMap<ResourceLocation, TalentPoolDefinition> pools = new LinkedHashMap<>();
        raw.pools().entrySet().stream()
                .sorted(Map.Entry.comparingByKey(Comparator.comparing(ResourceLocation::toString)))
                .forEach(entry -> pools.put(
                        entry.getKey(), compilePool(entry.getKey(), entry.getValue(), environment, rarities, report)
                ));

        LinkedHashMap<ResourceLocation, TalentDefinition> talents = new LinkedHashMap<>();
        raw.talents().entrySet().stream()
                .sorted(Map.Entry.comparingByKey(Comparator.comparing(ResourceLocation::toString)))
                .forEach(entry -> talents.put(
                        entry.getKey(),
                        compileTalent(
                                entry.getKey(), entry.getValue(), actionEnvironment, raw.talents().keySet(),
                                raw.configuredItems().keySet(), raw.counters().keySet(),
                                rarities, pools, report
                        )
                ));

        LinkedHashMap<ResourceLocation, CounterRuleDefinition> counters = new LinkedHashMap<>();
        raw.counters().entrySet().stream()
                .sorted(Map.Entry.comparingByKey(Comparator.comparing(ResourceLocation::toString)))
                .forEach(entry -> counters.put(
                        entry.getKey(), compileCounter(
                                entry.getKey(), entry.getValue(), environment, raw.talents().keySet(),
                                raw.configuredItems().keySet(), report
                        )
                ));

        LinkedHashMap<ResourceLocation, TalentBookLayoutDefinition> layouts = new LinkedHashMap<>();
        raw.layouts().entrySet().stream()
                .sorted(Map.Entry.comparingByKey(Comparator.comparing(ResourceLocation::toString)))
                .forEach(entry -> layouts.put(
                        entry.getKey(), compileLayout(
                                entry.getKey(), entry.getValue(), raw.talents().keySet(), pools.keySet(), report
                        )
                ));

        LinkedHashMap<ResourceLocation, ConfiguredItemDefinition> configuredItems = new LinkedHashMap<>();
        raw.configuredItems().entrySet().stream()
                .sorted(Map.Entry.comparingByKey(Comparator.comparing(ResourceLocation::toString)))
                .forEach(entry -> configuredItems.put(
                        entry.getKey(), compileConfiguredItem(
                                entry.getKey(), entry.getValue(), environment, actionEnvironment,
                                raw.talents().keySet(), raw.configuredItems().keySet(), raw.counters().keySet(), report
                        )
                ));

        long patternLootRules = configuredItems.values().stream()
                .flatMap(item -> item.loot().stream())
                .filter(rule -> rule.tableMatch() != ConfiguredItemLootTableMatch.EXACT)
                .count();
        if (patternLootRules > MAX_CONFIGURED_ITEM_PATTERN_LOOT_RULES) {
            report.error("CONFIGURED_ITEM_LOOT_PATTERN_LIMIT", "configured item collection", "$.loot",
                    "All configured items together can define at most "
                            + MAX_CONFIGURED_ITEM_PATTERN_LOOT_RULES + " prefix or regex loot rules");
        }

        ValidationReport finished = report.build();
        if (finished.hasErrors()) return CompilationResult.rejected(finished);

        RuntimeSnapshot candidate = RuntimeSnapshot.candidate(
                CanonicalJsonHasher.hash(raw),
                rarities,
                pools,
                talents,
                counters,
                layouts,
                configuredItems
        );
        return new CompilationResult(Optional.of(candidate), finished);
    }

    private static CompilationEnvironment withAdditionalItems(
            CompilationEnvironment environment, Set<ResourceLocation> additional
    ) {
        LinkedHashSet<ResourceLocation> items = new LinkedHashSet<>(environment.registeredItems());
        items.addAll(additional);
        return new CompilationEnvironment(
                environment.loadedMods(), environment.registeredAttributes(), items,
                environment.registeredEntityTypes(), environment.registeredMobEffects(),
                environment.registeredBlocks(), environment.registeredSoundEvents(),
                environment.registeredParticles()
        );
    }

    private ConfiguredItemDefinition compileConfiguredItem(
            ResourceLocation id,
            JsonObject json,
            CompilationEnvironment environment,
            CompilationEnvironment actionEnvironment,
            Set<ResourceLocation> knownTalents,
            Set<ResourceLocation> knownConfiguredItems,
            Set<ResourceLocation> knownCounters,
            ValidationReport.Builder report
    ) {
        String source = "configured item " + id;
        boolean systemItem = id.equals(new ResourceLocation("talent_editor", "talent_book"))
                || id.equals(new ResourceLocation("talent_editor", "talent_point"));
        JsonAccess.knownKeys(json, CONFIGURED_ITEM_FIELDS, source, "$", report);
        requireSchema(json, source, report);
        boolean enabled = JsonAccess.bool(json, "enabled", true, source, "$", report);
        if (!json.has("name")) {
            report.error("REQUIRED_FIELD", source, "$.name", "Configured item name is required");
        }
        LocalizedText name = JsonAccess.localizedText(json.get("name"), source, "$.name", report);
        List<LocalizedText> description = JsonAccess.localizedTextList(
                json, "description", source, "$", report
        );
        int customModelData = JsonAccess.integer(
                json, "customModelData", 0, 0, 16_777_215, source, "$", report
        );
        boolean glint = JsonAccess.bool(json, "glint", false, source, "$", report);
        boolean curiosEquippable = JsonAccess.bool(json, "curiosEquippable", true, source, "$", report);
        boolean mainhandOnly = JsonAccess.bool(json, "mainhandOnly", false, source, "$", report);
        List<String> curiosSlots = JsonAccess.stringList(json, "curiosSlots", source, "$", report);
        if (curiosSlots.size() > 128 || curiosSlots.stream().anyMatch(slot ->
                !slot.matches("[a-z0-9_.-]{1,64}") || !slot.equals(slot.strip()))
                || new HashSet<>(curiosSlots).size() != curiosSlots.size()) {
            report.error("INVALID_CURIOS_SLOTS", source, "$.curiosSlots",
                    "Curios slots must be distinct lowercase slot identifiers (maximum 128)");
        }
        List<String> requiredMods = normalizeRequiredMods(
                JsonAccess.stringList(json, "requiredMods", source, "$", report), source, report
        );
        List<String> missingMods = requiredMods.stream().filter(mod -> !environment.isModLoaded(mod)).toList();
        if (!missingMods.isEmpty()) {
            report.warning("REQUIRED_MODS_MISSING", source, "$.requiredMods",
                    "Configured item is retained but unavailable; missing mods: "
                            + String.join(", ", missingMods));
        }
        List<ConfiguredItemAttributeModifier> attributeModifiers = compileConfiguredItemAttributes(
                json, environment, missingMods, source, report
        );
        ConfiguredItemUseDefinition use = compileConfiguredItemUse(
                id, json, actionEnvironment, knownTalents, knownConfiguredItems, missingMods, source, report
        );
        if (systemItem && use.enabled()) {
            report.warning("SYSTEM_ITEM_USE_IGNORED", source, "$.use",
                    "The talent book and talent point keep their fixed built-in right-click behavior");
            use = ConfiguredItemUseDefinition.DISABLED;
        }
        List<CompiledTrigger> triggers = compileTriggers(
                id, json.get("triggers"), actionEnvironment, knownTalents, knownConfiguredItems, knownCounters,
                missingMods, source, report
        );
        List<ConfiguredItemLootRule> loot = compileConfiguredItemLoot(
                json, environment, missingMods, source, report
        );
        if (environment.hasItem(id) && !systemItem) {
            report.error("CONFIGURED_ITEM_ID_COLLISION", source, "$",
                    "Configured item id collides with a registered item: " + id);
        }
        return new ConfiguredItemDefinition(
                id, SCHEMA_VERSION, enabled, name, description, customModelData, glint,
                attributeModifiers, use, triggers, loot,
                requiredMods, missingMods.isEmpty(), missingMods, curiosEquippable, mainhandOnly, curiosSlots
        );
    }

    private List<ConfiguredItemAttributeModifier> compileConfiguredItemAttributes(
            JsonObject item,
            CompilationEnvironment environment,
            List<String> missingMods,
            String source,
            ValidationReport.Builder report
    ) {
        if (!item.has("attributeModifiers")) return List.of();
        JsonElement element = item.get("attributeModifiers");
        if (!element.isJsonArray()) {
            report.error("EXPECTED_ARRAY", source, "$.attributeModifiers", "Expected an attribute modifier array");
            return List.of();
        }
        JsonArray array = element.getAsJsonArray();
        if (array.size() > MAX_CONFIGURED_ITEM_ATTRIBUTE_MODIFIERS) {
            report.error("CONFIGURED_ITEM_ATTRIBUTE_LIMIT", source, "$.attributeModifiers",
                    "A configured item can define at most " + MAX_CONFIGURED_ITEM_ATTRIBUTE_MODIFIERS
                            + " attribute modifiers");
        }
        ArrayList<ConfiguredItemAttributeModifier> result = new ArrayList<>();
        HashSet<String> modifierKeys = new HashSet<>();
        for (int index = 0; index < Math.min(array.size(), MAX_CONFIGURED_ITEM_ATTRIBUTE_MODIFIERS); index++) {
            String path = "$.attributeModifiers[" + index + "]";
            JsonElement raw = array.get(index);
            if (!raw.isJsonObject()) {
                report.error("EXPECTED_OBJECT", source, path, "Expected an attribute modifier object");
                continue;
            }
            JsonObject modifier = raw.getAsJsonObject();
            JsonAccess.knownKeys(modifier, CONFIGURED_ITEM_ATTRIBUTE_FIELDS, source, path, report);
            ResourceLocation attribute = JsonAccess.requiredId(
                    modifier, "attribute", "minecraft:generic.max_health", true, source, path, report
            );
            double amount = JsonAccess.decimal(
                    modifier, "amount", 0.0D, -1.0E12D, 1.0E12D, source, path, report
            );
            String rawOperation = JsonAccess.string(
                    modifier, "operation", "addition", false, source, path, report
            );
            AttributeOperation operation;
            try {
                operation = AttributeOperation.parse(rawOperation);
            } catch (IllegalArgumentException ex) {
                report.error("INVALID_ATTRIBUTE_OPERATION", source, JsonAccess.child(path, "operation"),
                        "Expected addition, multiply_base, or multiply_total");
                operation = AttributeOperation.ADDITION;
            }
            ArrayList<ConfiguredItemSlot> slots = new ArrayList<>();
            if (modifier.has("slots")) {
                if (modifier.has("slot")) {
                    report.error("CONFLICTING_EQUIPMENT_SLOTS", source, path,
                            "Use slots or legacy slot, not both");
                }
                JsonElement rawSlots = modifier.get("slots");
                if (!rawSlots.isJsonArray() || rawSlots.getAsJsonArray().isEmpty()
                        || rawSlots.getAsJsonArray().size() > ConfiguredItemSlot.values().length) {
                    report.error("INVALID_EQUIPMENT_SLOTS", source, JsonAccess.child(path, "slots"),
                            "Expected 1-7 equipment slots");
                } else {
                    JsonArray slotArray = rawSlots.getAsJsonArray();
                    for (int slotIndex = 0; slotIndex < slotArray.size(); slotIndex++) {
                        String slotPath = JsonAccess.child(path, "slots") + "[" + slotIndex + "]";
                        try {
                            JsonElement slotElement = slotArray.get(slotIndex);
                            if (!slotElement.isJsonPrimitive() || !slotElement.getAsJsonPrimitive().isString()) {
                                throw new IllegalArgumentException();
                            }
                            ConfiguredItemSlot parsed = ConfiguredItemSlot.parse(slotElement.getAsString());
                            if (slots.contains(parsed)) {
                                report.error("DUPLICATE_EQUIPMENT_SLOT", source, slotPath,
                                        "Duplicate equipment slot");
                            } else {
                                slots.add(parsed);
                            }
                        } catch (IllegalArgumentException ex) {
                            report.error("INVALID_EQUIPMENT_SLOT", source, slotPath,
                                    "Expected mainhand, offhand, head, chest, legs, feet, or curios");
                        }
                    }
                }
            } else {
                String rawSlot = JsonAccess.string(modifier, "slot", "mainhand", false, source, path, report);
                try {
                    slots.add(ConfiguredItemSlot.parse(rawSlot));
                } catch (IllegalArgumentException ex) {
                    report.error("INVALID_EQUIPMENT_SLOT", source, JsonAccess.child(path, "slot"),
                            "Expected mainhand, offhand, head, chest, legs, feet, or curios");
                }
            }
            if (slots.isEmpty()) slots.add(ConfiguredItemSlot.MAINHAND);
            String modifierKey = JsonAccess.string(
                    modifier, "modifierKey", "modifier_" + index, false, source, path, report
            );
            if (!modifierKey.matches("[a-z0-9_.-]{1,160}")) {
                report.error("INVALID_MODIFIER_KEY", source, JsonAccess.child(path, "modifierKey"),
                        "modifierKey must use 1-160 lowercase letters, digits, dot, underscore, or dash");
            } else if (!modifierKeys.add(modifierKey)) {
                report.error("DUPLICATE_MODIFIER_KEY", source, JsonAccess.child(path, "modifierKey"),
                        "Duplicate configured-item modifier key '" + modifierKey + "'");
            }
            if (!environment.hasAttribute(attribute)) {
                if (missingMods.isEmpty()) {
                    report.error("UNKNOWN_ATTRIBUTE", source, JsonAccess.child(path, "attribute"),
                            "Attribute is not registered in the current server environment: " + attribute);
                } else {
                    report.warning("ATTRIBUTE_UNAVAILABLE_WITH_MISSING_MOD", source,
                            JsonAccess.child(path, "attribute"),
                            "Attribute is unavailable while required mods are missing: " + attribute);
                }
            }
            if (amount == 0.0D) {
                report.warning("ZERO_ATTRIBUTE_MODIFIER", source, JsonAccess.child(path, "amount"),
                        "This attribute modifier has no effect");
            }
            result.add(new ConfiguredItemAttributeModifier(attribute, amount, operation, slots, modifierKey));
        }
        return List.copyOf(result);
    }

    private ConfiguredItemUseDefinition compileConfiguredItemUse(
            ResourceLocation itemId,
            JsonObject item,
            CompilationEnvironment environment,
            Set<ResourceLocation> knownTalents,
            Set<ResourceLocation> knownConfiguredItems,
            List<String> missingMods,
            String source,
            ValidationReport.Builder report
    ) {
        if (!item.has("use")) return ConfiguredItemUseDefinition.DISABLED;
        JsonObject use = JsonAccess.object(item, "use", false, source, "$", report);
        JsonAccess.knownKeys(use, CONFIGURED_ITEM_USE_FIELDS, source, "$.use", report);
        boolean enabled = JsonAccess.bool(use, "enabled", true, source, "$.use", report);
        int cooldownTicks = JsonAccess.integer(
                use, "cooldownTicks", 0, 0, 1_200_000, source, "$.use", report
        );
        int consumeCount = JsonAccess.integer(
                use, "consumeCount", 0, 0, 64, source, "$.use", report
        );
        List<CompiledAction> actions = compileActions(
                itemId, use.get("actions"), "$.use.actions", ActionPlacement.ITEM_USE,
                environment, knownTalents, knownConfiguredItems, missingMods, source, report, enabled
        );
        for (int index = 0; index < actions.size(); index++) {
            CompiledAction action = actions.get(index);
            CompiledAction payload = ConditionalAction.unwrap(action);
            ActionTarget target = payload instanceof AddEffectAction add ? add.target()
                    : payload instanceof RemoveEffectAction remove ? remove.target() : ActionTarget.SELF;
            if (target != ActionTarget.SELF) {
                report.error("ITEM_USE_TARGET_UNSUPPORTED", source, "$.use.actions[" + index + "].target",
                        "Configured item use actions can only target self");
            }
        }
        return new ConfiguredItemUseDefinition(enabled, cooldownTicks, consumeCount, actions);
    }

    private List<ConfiguredItemLootRule> compileConfiguredItemLoot(
            JsonObject item,
            CompilationEnvironment environment,
            List<String> missingMods,
            String source,
            ValidationReport.Builder report
    ) {
        if (!item.has("loot")) return List.of();
        JsonElement element = item.get("loot");
        if (!element.isJsonArray()) {
            report.error("EXPECTED_ARRAY", source, "$.loot", "Expected an array of loot rules");
            return List.of();
        }
        JsonArray array = element.getAsJsonArray();
        if (array.size() > MAX_CONFIGURED_ITEM_LOOT_RULES) {
            report.error("CONFIGURED_ITEM_LOOT_LIMIT", source, "$.loot",
                    "A configured item can define at most " + MAX_CONFIGURED_ITEM_LOOT_RULES + " loot rules");
        }
        ArrayList<ConfiguredItemLootRule> result = new ArrayList<>();
        HashSet<String> scopes = new HashSet<>();
        for (int index = 0; index < Math.min(array.size(), MAX_CONFIGURED_ITEM_LOOT_RULES); index++) {
            String path = "$.loot[" + index + "]";
            JsonElement raw = array.get(index);
            if (!raw.isJsonObject()) {
                report.error("EXPECTED_OBJECT", source, path, "Expected a loot rule object");
                continue;
            }
            JsonObject rule = raw.getAsJsonObject();
            JsonAccess.knownKeys(rule, CONFIGURED_ITEM_LOOT_FIELDS, source, path, report);
            String tableExpression = JsonAccess.string(
                    rule, "table", "minecraft:empty", true, source, path, report
            ).strip();
            String rawTableMatch = JsonAccess.string(
                    rule, "tableMatch", "exact", false, source, path, report
            );
            ConfiguredItemLootTableMatch tableMatch;
            boolean validTableMatch = true;
            try {
                tableMatch = ConfiguredItemLootTableMatch.parse(rawTableMatch);
            } catch (IllegalArgumentException exception) {
                report.error("INVALID_LOOT_TABLE_MATCH", source, JsonAccess.child(path, "tableMatch"),
                        "Expected exact, prefix or regex");
                tableMatch = ConfiguredItemLootTableMatch.EXACT;
                tableExpression = "minecraft:empty";
                validTableMatch = false;
            }
            if (validTableMatch) {
                try {
                    ConfiguredItemLootRule.validateTableExpression(tableMatch, tableExpression);
                } catch (IllegalArgumentException exception) {
                    report.error("INVALID_LOOT_TABLE_EXPRESSION", source, JsonAccess.child(path, "table"),
                            exception.getMessage());
                    tableMatch = ConfiguredItemLootTableMatch.EXACT;
                    tableExpression = "minecraft:empty";
                }
            }
            double chance = JsonAccess.decimal(rule, "chance", 1.0D, 0.0D, 1.0D, source, path, report);
            double chancePerLuck = JsonAccess.decimal(
                    rule, "chancePerLuck", 0.0D, -1.0D, 1.0D, source, path, report
            );
            int minimum = JsonAccess.integer(rule, "minCount", 1, 1, 64, source, path, report);
            int maximum = JsonAccess.integer(rule, "maxCount", minimum, 1, 64, source, path, report);
            List<ResourceLocation> dimensions = JsonAccess.idList(
                    rule, "dimensions", true, source, path, report
            );
            String rawEntityTarget = JsonAccess.string(
                    rule, "entityTarget", "this_entity", false, source, path, report
            );
            ConfiguredItemLootEntityTarget entityTarget;
            try {
                entityTarget = ConfiguredItemLootEntityTarget.parse(rawEntityTarget);
            } catch (IllegalArgumentException exception) {
                report.error("INVALID_LOOT_ENTITY_TARGET", source, JsonAccess.child(path, "entityTarget"),
                        "Expected this_entity or killer_entity");
                entityTarget = ConfiguredItemLootEntityTarget.THIS_ENTITY;
            }
            List<ResourceLocation> entityTypes = JsonAccess.idList(
                    rule, "entityTypes", true, source, path, report
            );
            for (int typeIndex = 0; typeIndex < entityTypes.size(); typeIndex++) {
                ResourceLocation entityType = entityTypes.get(typeIndex);
                if (!environment.hasEntityType(entityType)) {
                    if (missingMods.isEmpty()) {
                        report.error("UNKNOWN_ENTITY_TYPE", source,
                                path + ".entityTypes[" + typeIndex + "]",
                                "Unknown entity type '" + entityType + "'");
                    } else {
                        report.warning("ENTITY_TYPE_UNAVAILABLE_WITH_MISSING_MOD", source,
                                path + ".entityTypes[" + typeIndex + "]",
                                "Entity type is unavailable while required mods are missing: " + entityType);
                    }
                }
            }
            List<ResourceLocation> entityTags = JsonAccess.idList(
                    rule, "entityTags", true, source, path, report
            );
            String scope = lootScope(tableMatch, tableExpression, dimensions,
                    entityTarget, entityTypes, entityTags);
            if (!scopes.add(scope)) {
                report.error("DUPLICATE_LOOT_TABLE", source, JsonAccess.child(path, "table"),
                        "Duplicate loot rule scope for " + tableMatch.serializedName()
                                + " table expression '" + tableExpression + "'");
            }
            if (maximum < minimum) {
                report.error("LOOT_COUNT_RANGE", source, JsonAccess.child(path, "maxCount"),
                        "maxCount must be greater than or equal to minCount");
                maximum = minimum;
            }
            if (chance == 0.0D && chancePerLuck == 0.0D) {
                report.warning("ZERO_LOOT_CHANCE", source, JsonAccess.child(path, "chance"),
                        "This loot rule can never add the configured item");
            }
            result.add(new ConfiguredItemLootRule(
                    tableMatch, tableExpression, chance, chancePerLuck, minimum, maximum,
                    dimensions, entityTarget, entityTypes, entityTags
            ));
        }
        return List.copyOf(result);
    }

    private static String lootScope(
            ConfiguredItemLootTableMatch tableMatch,
            String tableExpression,
            List<ResourceLocation> dimensions,
            ConfiguredItemLootEntityTarget entityTarget,
            List<ResourceLocation> entityTypes,
            List<ResourceLocation> entityTags
    ) {
        String target = entityTypes.isEmpty() && entityTags.isEmpty()
                ? "no_entity_filter" : entityTarget.serializedName();
        return tableMatch.serializedName() + ":" + tableExpression + "|"
                + sortedIds(dimensions) + "|" + target
                + "|" + sortedIds(entityTypes) + "|" + sortedIds(entityTags);
    }

    private static String sortedIds(List<ResourceLocation> values) {
        return values.stream().map(ResourceLocation::toString).sorted().reduce(
                (left, right) -> left + "," + right
        ).orElse("");
    }

    private TalentBookLayoutDefinition compileLayout(
            ResourceLocation id,
            JsonObject json,
            Set<ResourceLocation> knownTalents,
            Set<ResourceLocation> knownPools,
            ValidationReport.Builder report
    ) {
        String source = "layout " + id;
        JsonAccess.knownKeys(json, LAYOUT_FIELDS, source, "$", report);
        requireSchema(json, source, report);
        boolean enabled = JsonAccess.bool(json, "enabled", true, source, "$", report);
        int priority = JsonAccess.integer(json, "priority", 0, -100_000, 100_000, source, "$", report);
        ResourceLocation background = JsonAccess.optionalId(json, "background", true, source, "$", report);
        int panelPercent = JsonAccess.integer(json, "panelPercent", 60, 25, 70, source, "$", report);
        int columns = JsonAccess.integer(json, "columns", 2, 1, 8, source, "$", report);
        int rows = JsonAccess.integer(json, "rows", 3, 1, 10, source, "$", report);
        int slotWidth = JsonAccess.integer(json, "slotWidth", 120, 24, 128, source, "$", report);
        int slotHeight = JsonAccess.integer(json, "slotHeight", 56, 22, 96, source, "$", report);
        int iconSize = JsonAccess.integer(json, "iconSize", 48, 8, 64, source, "$", report);
        boolean showGeneratedEffects = JsonAccess.bool(
                json, "showGeneratedEffects", true, source, "$", report
        );
        List<TalentBookLayoutDefinition.SlotDefinition> slots = compileLayoutSlots(
                json, source, knownTalents, report
        );
        List<TalentBookLayoutDefinition.PageDefinition> pages = compileLayoutPages(
                json, source, knownTalents, report
        );
        HashSet<ResourceLocation> fixedTalents = new HashSet<>();
        for (int pageIndex = 0; pageIndex < pages.size(); pageIndex++) {
            TalentBookLayoutDefinition.PageDefinition page = pages.get(pageIndex);
            for (int slotIndex = 0; slotIndex < page.slots().size(); slotIndex++) {
                Optional<ResourceLocation> fixedTalent = page.slots().get(slotIndex).talent();
                if (fixedTalent.isPresent() && !fixedTalents.add(fixedTalent.orElseThrow())) {
                    report.error("DUPLICATE_LAYOUT_FIXED_TALENT", source,
                            "$.pages[" + pageIndex + "].slots[" + slotIndex + "].talent",
                            "Talent '" + fixedTalent.orElseThrow() + "' is already fixed to another slot");
                }
            }
        }
        int totalSlots = slots.size() + pages.stream().mapToInt(page -> page.slots().size()).sum();
        if (totalSlots > MAX_LAYOUT_TOTAL_PAGE_SLOTS) {
            report.error("LAYOUT_TOTAL_SLOT_LIMIT", source, "$.pages",
                    "A layout can define at most " + MAX_LAYOUT_TOTAL_PAGE_SLOTS
                            + " slots across its root template and configured pages");
        }
        if (iconSize > slotHeight - 4) {
            report.error("LAYOUT_ICON_TOO_TALL", source, "$.iconSize",
                    "Icon size must leave at least four pixels of vertical slot padding");
        }
        var bookUi = json.has("bookUi") ? Optional.of(UiDocumentCodec.compile(
                JsonAccess.object(json, "bookUi", false, source, "$", report), false, source, "$.bookUi", report))
                : Optional.<com.kidxoxo.talenteditor.domain.UiDocument>empty();
        var drawUi = json.has("drawUi") ? Optional.of(UiDocumentCodec.compile(
                JsonAccess.object(json, "drawUi", false, source, "$", report), true, source, "$.drawUi", report))
                : Optional.<com.kidxoxo.talenteditor.domain.UiDocument>empty();
        bookUi.ifPresent(ui -> {
            for (int i = 0; i < ui.elements().size(); i++) {
                var element = ui.elements().get(i);
                if (element.type().equals("button") && element.action().equals("draw") && !element.target().isBlank()) {
                    var pool = ResourceLocation.tryParse(element.target());
                    if (pool != null && !knownPools.contains(pool)) report.error("UNKNOWN_UI_POOL", source,
                            "$.bookUi.elements[" + i + "].target", "Unknown draw pool: " + element.target());
                }
            }
            if (pages.stream().anyMatch(p -> p.slots().size() > ui.slotCount()))
                report.error("UI_PAGE_CAPACITY", source, "$.bookUi", "UI must provide at least as many slots as each configured page");
        });
        return new TalentBookLayoutDefinition(
                id, SCHEMA_VERSION, enabled, priority, Optional.ofNullable(background), panelPercent,
                columns, rows, slotWidth, slotHeight, iconSize, showGeneratedEffects, slots, pages, bookUi, drawUi
        );
    }

    private List<TalentBookLayoutDefinition.SlotDefinition> compileLayoutSlots(
            JsonObject json,
            String source,
            Set<ResourceLocation> knownTalents,
            ValidationReport.Builder report
    ) {
        if (!json.has("slots")) return List.of();
        JsonArray values = JsonAccess.array(json, "slots", false, source, "$", report);
        if (values.isEmpty()) {
            report.error("LAYOUT_SLOTS_EMPTY", source, "$.slots",
                    "Remove slots to use the regular grid, or define at least one free slot");
            return List.of();
        }
        return compileLayoutSlotValues(values, source, "$.slots", knownTalents, false, report);
    }

    private List<TalentBookLayoutDefinition.PageDefinition> compileLayoutPages(
            JsonObject json,
            String source,
            Set<ResourceLocation> knownTalents,
            ValidationReport.Builder report
    ) {
        if (!json.has("pages")) return List.of();
        JsonArray values = JsonAccess.array(json, "pages", false, source, "$", report);
        if (values.size() > MAX_LAYOUT_PAGES) {
            report.error("LAYOUT_PAGE_LIMIT", source, "$.pages",
                    "A layout can define at most " + MAX_LAYOUT_PAGES + " configured pages");
        }
        ArrayList<TalentBookLayoutDefinition.PageDefinition> result = new ArrayList<>();
        HashSet<String> ids = new HashSet<>();
        int limit = Math.min(values.size(), MAX_LAYOUT_PAGES);
        for (int index = 0; index < limit; index++) {
            String path = "$.pages[" + index + "]";
            JsonElement element = values.get(index);
            if (!element.isJsonObject()) {
                report.error("EXPECTED_OBJECT", source, path, "Expected a layout page object");
                continue;
            }
            JsonObject page = element.getAsJsonObject();
            JsonAccess.knownKeys(page, LAYOUT_PAGE_FIELDS, source, path, report);
            String pageId = JsonAccess.string(page, "id", "page_" + index, true, source, path, report);
            if (!pageId.matches("[a-z0-9_.-]{1,64}")) {
                report.error("INVALID_LAYOUT_PAGE_ID", source, path + ".id",
                        "Page id must match [a-z0-9_.-]{1,64}");
            } else if (!ids.add(pageId)) {
                report.error("DUPLICATE_LAYOUT_PAGE_ID", source, path + ".id",
                        "Duplicate layout page id '" + pageId + "'");
            }
            ResourceLocation background = JsonAccess.optionalId(
                    page, "background", true, source, path, report
            );
            List<TalentBookLayoutDefinition.SlotDefinition> pageSlots;
            if (!page.has("slots")) {
                report.error("REQUIRED_FIELD", source, path + ".slots", "Page slots are required");
                pageSlots = List.of();
            } else {
                JsonArray slotValues = JsonAccess.array(page, "slots", false, source, path, report);
                if (slotValues.isEmpty()) {
                    report.error("LAYOUT_PAGE_SLOTS_EMPTY", source, path + ".slots",
                            "A configured page must contain at least one slot");
                    pageSlots = List.of();
                } else {
                    pageSlots = compileLayoutSlotValues(
                            slotValues, source, path + ".slots", knownTalents, true, report
                    );
                }
            }
            result.add(new TalentBookLayoutDefinition.PageDefinition(
                    pageId, Optional.ofNullable(background), pageSlots
            ));
        }
        return List.copyOf(result);
    }

    private List<TalentBookLayoutDefinition.SlotDefinition> compileLayoutSlotValues(
            JsonArray values,
            String source,
            String arrayPath,
            Set<ResourceLocation> knownTalents,
            boolean allowFixedTalent,
            ValidationReport.Builder report
    ) {
        if (values.size() > MAX_LAYOUT_SLOTS) {
            report.error("LAYOUT_SLOT_LIMIT", source, arrayPath,
                    "A layout can define at most " + MAX_LAYOUT_SLOTS + " free slots");
        }
        ArrayList<TalentBookLayoutDefinition.SlotDefinition> result = new ArrayList<>();
        HashSet<String> ids = new HashSet<>();
        int limit = Math.min(values.size(), MAX_LAYOUT_SLOTS);
        for (int index = 0; index < limit; index++) {
            String path = arrayPath + "[" + index + "]";
            JsonElement element = values.get(index);
            if (!element.isJsonObject()) {
                report.error("EXPECTED_OBJECT", source, path, "Expected a layout slot object");
                continue;
            }
            JsonObject slot = element.getAsJsonObject();
            JsonAccess.knownKeys(slot, LAYOUT_SLOT_FIELDS, source, path, report);
            String slotId = JsonAccess.string(slot, "id", "slot_" + index, true, source, path, report);
            if (!slotId.matches("[a-z0-9_.-]{1,64}")) {
                report.error("INVALID_LAYOUT_SLOT_ID", source, path + ".id",
                        "Slot id must match [a-z0-9_.-]{1,64}");
            } else if (!ids.add(slotId)) {
                report.error("DUPLICATE_LAYOUT_SLOT_ID", source, path + ".id",
                        "Duplicate layout slot id '" + slotId + "'");
            }
            int x = JsonAccess.integer(slot, "x", 0, 0, 2_048, source, path, report);
            int y = JsonAccess.integer(slot, "y", 0, 0, 2_048, source, path, report);
            int width = JsonAccess.integer(slot, "width", 58, 24, 256, source, path, report);
            int height = JsonAccess.integer(slot, "height", 30, 22, 128, source, path, report);
            int slotIconSize = JsonAccess.integer(slot, "iconSize", 16, 8, 64, source, path, report);
            ResourceLocation fixedTalent = JsonAccess.optionalId(
                    slot, "talent", true, source, path, report
            );
            if (fixedTalent != null && !allowFixedTalent) {
                report.error("LAYOUT_ROOT_FIXED_TALENT", source, path + ".talent",
                        "Fixed talents are only supported in configured page slots");
            } else if (fixedTalent != null && !knownTalents.contains(fixedTalent)) {
                report.error("UNKNOWN_TALENT", source, path + ".talent",
                        "Unknown talent '" + fixedTalent + "'");
            }
            if (slotIconSize > height - 4) {
                report.error("LAYOUT_ICON_TOO_TALL", source, path + ".iconSize",
                        "Icon size must leave at least four pixels of vertical slot padding");
            }
            if (x + width > 2_048 || y + height > 2_048) {
                report.error("LAYOUT_SLOT_BOUNDS", source, path,
                        "Slot rectangle must remain inside the 2048 by 2048 logical canvas");
            }
            result.add(new TalentBookLayoutDefinition.SlotDefinition(
                    slotId, x, y, width, height, slotIconSize, Optional.ofNullable(fixedTalent)
            ));
        }
        for (int first = 0; first < result.size(); first++) {
            for (int second = first + 1; second < result.size(); second++) {
                if (overlaps(result.get(first), result.get(second))) {
                    report.error("LAYOUT_SLOT_OVERLAP", source, arrayPath + "[" + second + "]",
                            "Slot overlaps '" + result.get(first).id() + "'");
                }
            }
        }
        return List.copyOf(result);
    }

    private static boolean overlaps(
            TalentBookLayoutDefinition.SlotDefinition first,
            TalentBookLayoutDefinition.SlotDefinition second
    ) {
        return first.x() < second.x() + second.width()
                && first.x() + first.width() > second.x()
                && first.y() < second.y() + second.height()
                && first.y() + first.height() > second.y();
    }

    private CounterRuleDefinition compileCounter(
            ResourceLocation id,
            JsonObject json,
            CompilationEnvironment environment,
            Set<ResourceLocation> knownTalents,
            Set<ResourceLocation> knownConfiguredItems,
            ValidationReport.Builder report
    ) {
        String source = "counter " + id;
        JsonAccess.knownKeys(json, COUNTER_FIELDS, source, "$", report);
        requireSchema(json, source, report);
        boolean enabled = JsonAccess.bool(json, "enabled", true, source, "$", report);
        if (!json.has("name")) report.error("REQUIRED_FIELD", source, "$.name", "Counter name is required");
        LocalizedText name = JsonAccess.localizedText(json.get("name"), source, "$.name", report);
        String rawEvent = JsonAccess.string(json, "event", "", true, source, "$", report);
        CounterEventType event = CounterEventType.parse(rawEvent).orElseGet(() -> {
            report.error("UNKNOWN_COUNTER_EVENT", source, "$.event",
                    "Expected manual, kill, damage_dealt, damage_taken, item_use, entity_interact, "
                            + "move_distance, or play_time");
            return CounterEventType.KILL;
        });
        long increment = JsonAccess.longInteger(
                json, "increment", 1L, 1L, 1_000_000_000_000L, source, "$", report
        );
        String rawAmountMode = JsonAccess.string(json, "amountMode", "fixed", false, source, "$", report);
        CounterAmountMode amountMode = CounterAmountMode.parse(rawAmountMode).orElseGet(() -> {
            report.error("UNKNOWN_COUNTER_AMOUNT_MODE", source, "$.amountMode",
                    "Expected fixed or event_amount");
            return CounterAmountMode.FIXED;
        });
        double amountScale = JsonAccess.decimal(
                json, "amountScale", 1.0D, 0.0D, 1_000_000_000.0D, source, "$", report
        );
        String rawRounding = JsonAccess.string(json, "rounding", "round", false, source, "$", report);
        CounterRounding rounding = CounterRounding.parse(rawRounding).orElseGet(() -> {
            report.error("UNKNOWN_COUNTER_ROUNDING", source, "$.rounding", "Expected floor, round, or ceil");
            return CounterRounding.ROUND;
        });
        long minimumIncrement = JsonAccess.longInteger(
                json, "minimumIncrement", 0L, 0L, 1_000_000_000_000L, source, "$", report
        );
        if (amountMode == CounterAmountMode.EVENT_AMOUNT
                && event != CounterEventType.DAMAGE_DEALT && event != CounterEventType.DAMAGE_TAKEN
                && event != CounterEventType.MOVE_DISTANCE) {
            report.error("COUNTER_EVENT_AMOUNT_UNSUPPORTED", source, "$.amountMode",
                    "event_amount is only available for damage_dealt, damage_taken, and move_distance counters");
        }
        long maximum = JsonAccess.longInteger(
                json, "maximum", 1_000_000_000_000L, 1L, 1_000_000_000_000L, source, "$", report
        );
        int cooldownTicks = JsonAccess.integer(
                json, "cooldownTicks", 0, 0, 1_200_000, source, "$", report
        );
        int intervalTicks = JsonAccess.integer(
                json, "intervalTicks", 20, 1, 1_200_000, source, "$", report
        );
        boolean resetOnLogin = JsonAccess.bool(json, "resetOnLogin", false, source, "$", report);
        boolean resetOnDeath = JsonAccess.bool(json, "resetOnDeath", false, source, "$", report);
        int resetIntervalTicks = JsonAccess.integer(
                json, "resetIntervalTicks", 0, 0, Integer.MAX_VALUE, source, "$", report
        );
        List<ResourceLocation> categories = JsonAccess.idList(json, "categories", true, source, "$", report);
        String rawVisibility = JsonAccess.string(json, "visibility", "full", false, source, "$", report);
        CounterVisibility visibility = CounterVisibility.parse(rawVisibility).orElseGet(() -> {
            report.error("UNKNOWN_COUNTER_VISIBILITY", source, "$.visibility",
                    "Expected full, condition_only, or hidden");
            return CounterVisibility.FULL;
        });
        java.util.Optional<ResourceLocation> displayCategory = java.util.Optional.empty();
        if (json.has("displayCategory")) {
            ResourceLocation parsed = JsonAccess.optionalId(
                    json, "displayCategory", true, source, "$", report
            );
            displayCategory = java.util.Optional.ofNullable(parsed);
        }
        if (amountMode == CounterAmountMode.FIXED && increment > maximum) {
            report.warning("COUNTER_INCREMENT_ABOVE_MAXIMUM", source, "$.increment",
                    "The first matching event will clamp the counter to its maximum");
        }

        List<String> requiredMods = normalizeRequiredMods(
                JsonAccess.stringList(json, "requiredMods", source, "$", report), source, report
        );
        List<String> missingMods = requiredMods.stream().filter(mod -> !environment.isModLoaded(mod)).toList();
        if (!missingMods.isEmpty()) {
            report.warning("REQUIRED_MODS_MISSING", source, "$.requiredMods",
                    "Counter is retained but inactive; missing mods: " + String.join(", ", missingMods));
        }

        List<ResourceLocation> requiredTalents = JsonAccess.idList(
                json, "requiredTalents", true, source, "$", report
        );
        for (int index = 0; index < requiredTalents.size(); index++) {
            ResourceLocation talent = requiredTalents.get(index);
            if (!knownTalents.contains(talent)) {
                report.error("UNKNOWN_TALENT", source, "$.requiredTalents[" + index + "]",
                        "Unknown talent '" + talent + "'");
            }
        }
        List<ResourceLocation> entityTypes = JsonAccess.idList(json, "entityTypes", true, source, "$", report);
        for (int index = 0; index < entityTypes.size(); index++) {
            ResourceLocation entityType = entityTypes.get(index);
            if (!environment.hasEntityType(entityType)) {
                if (missingMods.isEmpty()) {
                    report.error("UNKNOWN_ENTITY_TYPE", source, "$.entityTypes[" + index + "]",
                            "Unknown entity type '" + entityType + "'");
                } else {
                    report.warning("ENTITY_TYPE_UNAVAILABLE_WITH_MISSING_MOD", source,
                            "$.entityTypes[" + index + "]",
                            "Entity type is unavailable while required mods are missing: " + entityType);
                }
            }
        }
        List<ResourceLocation> entityTags = JsonAccess.idList(json, "entityTags", true, source, "$", report);
        List<ResourceLocation> items = JsonAccess.idList(json, "items", true, source, "$", report);
        for (int index = 0; index < items.size(); index++) {
            ResourceLocation item = items.get(index);
            if (!environment.hasItem(item) && !knownConfiguredItems.contains(item)) {
                if (missingMods.isEmpty()) {
                    report.error("UNKNOWN_ITEM", source, "$.items[" + index + "]", "Unknown item '" + item + "'");
                } else {
                    report.warning("ITEM_UNAVAILABLE_WITH_MISSING_MOD", source, "$.items[" + index + "]",
                            "Item is unavailable while required mods are missing: " + item);
                }
            }
        }
        List<ResourceLocation> itemTags = JsonAccess.idList(json, "itemTags", true, source, "$", report);
        List<ResourceLocation> damageTags = JsonAccess.idList(json, "damageTags", true, source, "$", report);
        validateCounterFilters(
                event, entityTypes, entityTags, items, itemTags, damageTags, source, report
        );
        return new CounterRuleDefinition(
                id, SCHEMA_VERSION, enabled, name, event, increment, amountMode, amountScale, rounding,
                minimumIncrement, maximum, cooldownTicks, intervalTicks,
                resetOnLogin, resetOnDeath, resetIntervalTicks,
                categories, visibility, displayCategory,
                requiredMods, missingMods.isEmpty(), requiredTalents, entityTypes, entityTags, items, itemTags,
                damageTags
        );
    }

    private static void validateCounterFilters(
            CounterEventType event,
            List<ResourceLocation> entityTypes,
            List<ResourceLocation> entityTags,
            List<ResourceLocation> items,
            List<ResourceLocation> itemTags,
            List<ResourceLocation> damageTags,
            String source,
            ValidationReport.Builder report
    ) {
        boolean entityContext = event == CounterEventType.KILL || event == CounterEventType.DAMAGE_DEALT
                || event == CounterEventType.DAMAGE_TAKEN || event == CounterEventType.ENTITY_INTERACT;
        boolean itemContext = event == CounterEventType.KILL || event == CounterEventType.DAMAGE_DEALT
                || event == CounterEventType.DAMAGE_TAKEN || event == CounterEventType.ITEM_USE
                || event == CounterEventType.ENTITY_INTERACT;
        boolean damageContext = event == CounterEventType.DAMAGE_DEALT || event == CounterEventType.DAMAGE_TAKEN;
        if (!entityContext && (!entityTypes.isEmpty() || !entityTags.isEmpty())) {
            report.error("COUNTER_FILTER_NOT_AVAILABLE", source, "$.entityTypes",
                    "This event has no related entity to filter");
        }
        if (!itemContext && (!items.isEmpty() || !itemTags.isEmpty())) {
            report.error("COUNTER_FILTER_NOT_AVAILABLE", source, "$.items",
                    "This event has no related item to filter");
        }
        if (!damageContext && !damageTags.isEmpty()) {
            report.error("COUNTER_FILTER_NOT_AVAILABLE", source, "$.damageTags",
                    "Damage tags can only be used with damage_dealt or damage_taken");
        }
    }

    private RarityDefinition compileRarity(ResourceLocation id, JsonObject json, ValidationReport.Builder report) {
        String source = "rarity " + id;
        JsonAccess.knownKeys(json, RARITY_FIELDS, source, "$", report);
        requireSchema(json, source, report);
        boolean enabled = JsonAccess.bool(json, "enabled", true, source, "$", report);
        if (!json.has("name")) {
            report.error("REQUIRED_FIELD", source, "$.name", "Rarity name is required");
        }
        LocalizedText name = JsonAccess.localizedText(json.get("name"), source, "$.name", report);
        if (!json.has("color")) {
            report.error("REQUIRED_FIELD", source, "$.color", "Rarity color is required");
        }
        int color = json.has("color")
                ? JsonAccess.color(json.get("color"), source, "$.color", report)
                : 0xFFFFFFFF;
        int order = JsonAccess.integer(json, "order", 0, -100_000, 100_000, source, "$", report);
        boolean offerable = JsonAccess.bool(json, "offerable", true, source, "$", report);
        double baseWeight = JsonAccess.decimal(
                json, "baseWeight", 1.0D, 0.0D, 1.0E9D, source, "$", report
        );
        if (offerable && baseWeight == 0.0D) {
            report.warning("ZERO_RARITY_WEIGHT", source, "$.baseWeight",
                    "Offerable rarity has zero base weight");
        }
        return new RarityDefinition(id, SCHEMA_VERSION, enabled, name, color, order, offerable, baseWeight);
    }

    private TalentPoolDefinition compilePool(
            ResourceLocation id,
            JsonObject json,
            CompilationEnvironment environment,
            Map<ResourceLocation, RarityDefinition> rarities,
            ValidationReport.Builder report
    ) {
        String source = "pool " + id;
        JsonAccess.knownKeys(json, POOL_FIELDS, source, "$", report);
        requireSchema(json, source, report);
        boolean enabled = JsonAccess.bool(json, "enabled", true, source, "$", report);
        if (!json.has("name")) report.error("REQUIRED_FIELD", source, "$.name", "Pool name is required");
        LocalizedText name = JsonAccess.localizedText(json.get("name"), source, "$.name", report);
        int choices = JsonAccess.integer(json, "choices", 3, 1, 5, source, "$", report);
        int refreshes = JsonAccess.integer(json, "refreshes", 1, 0, 100, source, "$", report);
        int maxOwned = JsonAccess.integer(json, "maxOwned", 24, 1, 10_000, source, "$", report);
        int iconSize = JsonAccess.integer(json, "iconSize", 64, 8, 64, source, "$", report);
        boolean legacyExcludeOwned = JsonAccess.bool(json, "excludeOwned", true, source, "$", report);

        LinkedHashMap<ResourceLocation, Double> rarityWeights = new LinkedHashMap<>();
        JsonObject weights = JsonAccess.object(json, "rarityWeights", false, source, "$", report);
        for (Map.Entry<String, JsonElement> entry : weights.entrySet()) {
            String path = "$.rarityWeights." + entry.getKey();
            ResourceLocation rarityId = JsonAccess.parseId(entry.getKey(), true, source, path, report);
            double weight = parsePoolWeight(entry.getValue(), source, path, report);
            if (rarityId != null) {
                if (!rarities.containsKey(rarityId)) {
                    report.error("UNKNOWN_RARITY", source, path, "Unknown rarity '" + rarityId + "'");
                }
                rarityWeights.put(rarityId, weight);
            }
        }

        boolean hasPositiveRarity = rarities.values().stream()
                .filter(RarityDefinition::enabled)
                .filter(RarityDefinition::offerable)
                .anyMatch(rarity -> rarityWeights.getOrDefault(rarity.id(), rarity.baseWeight()) > 0.0D);
        if (enabled && !rarities.isEmpty() && !hasPositiveRarity) {
            report.warning("POOL_HAS_NO_WEIGHTED_RARITY", source, "$.rarityWeights",
                    "Enabled pool has no enabled, offerable rarity with positive weight");
        }

        ResourceLocation luckAttributeId = JsonAccess.optionalId(
                json, "luckAttribute", true, source, "$", report
        );
        if (luckAttributeId != null && !environment.hasAttribute(luckAttributeId)) {
            report.error("UNKNOWN_ATTRIBUTE", source, "$.luckAttribute",
                    "Unknown luck attribute '" + luckAttributeId + "'");
        }
        LinkedHashMap<ResourceLocation, Double> luckFactors = compileRarityDecimalMap(
                json, "luckFactors", -1_000.0D, 1_000.0D, rarities, source, report
        );
        if (luckAttributeId == null && !luckFactors.isEmpty()) {
            report.error("LUCK_ATTRIBUTE_REQUIRED", source, "$.luckFactors",
                    "luckFactors requires luckAttribute");
        }

        ResourceLocation pityRarityId = JsonAccess.optionalId(json, "pityRarity", true, source, "$", report);
        int pityAfter = JsonAccess.integer(json, "pityAfter", 0, 0, 1_000_000, source, "$", report);
        if (pityRarityId != null && !rarities.containsKey(pityRarityId)) {
            report.error("UNKNOWN_RARITY", source, "$.pityRarity", "Unknown rarity '" + pityRarityId + "'");
        }
        if (pityAfter > 0 && pityRarityId == null) {
            report.error("PITY_RARITY_REQUIRED", source, "$.pityRarity",
                    "pityRarity is required when pityAfter is greater than zero");
        } else if (pityAfter == 0 && pityRarityId != null) {
            report.warning("PITY_DISABLED", source, "$.pityAfter",
                    "pityRarity is configured but pityAfter is zero");
        }

        String fallbackPolicy = legacyExcludeOwned ? "exclude" : "refund_token";
        String rawPolicy = JsonAccess.string(
                json, "duplicatePolicy", fallbackPolicy, false, source, "$", report
        );
        DuplicatePolicy duplicatePolicy = DuplicatePolicy.parse(rawPolicy).orElseGet(() -> {
            report.error("UNKNOWN_DUPLICATE_POLICY", source, "$.duplicatePolicy",
                    "Expected exclude, refund_token, or add_counter");
            return DuplicatePolicy.EXCLUDE;
        });
        if (json.has("excludeOwned") && json.has("duplicatePolicy")
                && legacyExcludeOwned != (duplicatePolicy == DuplicatePolicy.EXCLUDE)) {
            report.error("DUPLICATE_POLICY_CONFLICT", source, "$.excludeOwned",
                    "excludeOwned conflicts with duplicatePolicy");
        }
        ResourceLocation duplicateCounterId = JsonAccess.optionalId(
                json, "duplicateCounter", true, source, "$", report
        );
        long duplicateCounterAmount = JsonAccess.longInteger(
                json, "duplicateCounterAmount", 1L, 1L, 1_000_000_000_000L, source, "$", report
        );
        if (duplicatePolicy == DuplicatePolicy.ADD_COUNTER && duplicateCounterId == null) {
            report.error("DUPLICATE_COUNTER_REQUIRED", source, "$.duplicateCounter",
                    "duplicateCounter is required for add_counter");
        } else if (duplicatePolicy != DuplicatePolicy.ADD_COUNTER && duplicateCounterId != null) {
            report.warning("UNUSED_DUPLICATE_COUNTER", source, "$.duplicateCounter",
                    "duplicateCounter is only used by add_counter");
        }
        return new TalentPoolDefinition(
                id, SCHEMA_VERSION, enabled, name, choices, refreshes, maxOwned, iconSize,
                rarityWeights, Optional.ofNullable(luckAttributeId), luckFactors,
                Optional.ofNullable(pityRarityId), pityAfter, duplicatePolicy,
                Optional.ofNullable(duplicateCounterId), duplicateCounterAmount
        );
    }

    private static LinkedHashMap<ResourceLocation, Double> compileRarityDecimalMap(
            JsonObject json,
            String field,
            double minimum,
            double maximum,
            Map<ResourceLocation, RarityDefinition> rarities,
            String source,
            ValidationReport.Builder report
    ) {
        LinkedHashMap<ResourceLocation, Double> values = new LinkedHashMap<>();
        JsonObject object = JsonAccess.object(json, field, false, source, "$", report);
        for (Map.Entry<String, JsonElement> entry : object.entrySet()) {
            String path = "$." + field + "." + entry.getKey();
            ResourceLocation rarity = JsonAccess.parseId(entry.getKey(), true, source, path, report);
            double value = parseBoundedDecimal(entry.getValue(), minimum, maximum, source, path, report);
            if (rarity != null) {
                if (!rarities.containsKey(rarity)) {
                    report.error("UNKNOWN_RARITY", source, path, "Unknown rarity '" + rarity + "'");
                }
                values.put(rarity, value);
            }
        }
        return values;
    }

    private static double parseBoundedDecimal(
            JsonElement element,
            double minimum,
            double maximum,
            String source,
            String path,
            ValidationReport.Builder report
    ) {
        if (!(element instanceof JsonPrimitive primitive) || !primitive.isNumber()) {
            report.error("EXPECTED_NUMBER", source, path, "Expected a number");
            return 0.0D;
        }
        try {
            double value = primitive.getAsDouble();
            if (!Double.isFinite(value) || value < minimum || value > maximum) {
                report.error("NUMBER_RANGE", source, path,
                        "Expected a finite number in range " + minimum + ".." + maximum);
                return 0.0D;
            }
            return value;
        } catch (RuntimeException ex) {
            report.error("EXPECTED_NUMBER", source, path, "Expected a number");
            return 0.0D;
        }
    }

    private static double parsePoolWeight(
            JsonElement value,
            String source,
            String path,
            ValidationReport.Builder report
    ) {
        if (!(value instanceof JsonPrimitive primitive) || !primitive.isNumber()) {
            report.error("EXPECTED_NUMBER", source, path, "Expected a rarity weight number");
            return 0.0D;
        }
        try {
            double weight = primitive.getAsDouble();
            if (!Double.isFinite(weight) || weight < 0.0D || weight > 1.0E9D) {
                report.error("NUMBER_RANGE", source, path, "Expected a finite number in range 0..1.0E9");
                return 0.0D;
            }
            return weight;
        } catch (RuntimeException ex) {
            report.error("EXPECTED_NUMBER", source, path, "Expected a rarity weight number");
            return 0.0D;
        }
    }

    private TalentDefinition compileTalent(
            ResourceLocation id,
            JsonObject json,
            CompilationEnvironment environment,
            Set<ResourceLocation> knownTalents,
            Set<ResourceLocation> knownConfiguredItems,
            Set<ResourceLocation> knownCounters,
            Map<ResourceLocation, RarityDefinition> rarities,
            Map<ResourceLocation, TalentPoolDefinition> pools,
            ValidationReport.Builder report
    ) {
        String source = "talent " + id;
        JsonAccess.knownKeys(json, TALENT_FIELDS, source, "$", report);
        requireSchema(json, source, report);
        boolean enabled = JsonAccess.bool(json, "enabled", true, source, "$", report);

        JsonObject text = JsonAccess.object(json, "text", true, source, "$", report);
        JsonAccess.knownKeys(text, TEXT_FIELDS, source, "$.text", report);
        if (!text.has("name")) {
            report.error("REQUIRED_FIELD", source, "$.text.name", "Talent name is required");
        }
        LocalizedText name = JsonAccess.localizedText(text.get("name"), source, "$.text.name", report);
        List<LocalizedText> description = JsonAccess.localizedTextList(
                text, "description", source, "$.text", report
        );
        boolean showGeneratedEffects = JsonAccess.bool(
                text, "showGeneratedEffects", true, source, "$.text", report
        );

        ResourceLocation rarity = JsonAccess.requiredId(
                json, "rarity", TalentEditorMod.MOD_ID + ":common", true, source, "$", report
        );
        RarityDefinition rarityDefinition = rarities.get(rarity);
        if (rarityDefinition == null) {
            report.error("UNKNOWN_RARITY", source, "$.rarity", "Unknown rarity '" + rarity + "'");
        }

        List<ResourceLocation> categories = JsonAccess.idList(
                json, "categories", true, source, "$", report
        );
        ResourceLocation iconId = JsonAccess.optionalId(json, "icon", true, source, "$", report);
        Optional<ResourceLocation> icon = Optional.ofNullable(iconId);
        OfferSettings offer = compileOffer(json, source, report);
        int capacityCost = JsonAccess.integer(
                json, "capacityCost", 1, 0, 10_000, source, "$", report
        );
        List<ResourceLocation> tags = JsonAccess.idList(json, "tags", true, source, "$", report);

        List<String> requiredMods = normalizeRequiredMods(
                JsonAccess.stringList(json, "requiredMods", source, "$", report),
                source,
                report
        );
        List<String> missingMods = requiredMods.stream().filter(mod -> !environment.isModLoaded(mod)).toList();
        if (!missingMods.isEmpty()) {
            report.warning("REQUIRED_MODS_MISSING", source, "$.requiredMods",
                    "Talent is retained but inactive; missing mods: " + String.join(", ", missingMods));
        }
        boolean available = missingMods.isEmpty();

        CompiledCondition acquireCondition = json.has("acquireCondition")
                ? new ConditionCompiler(
                        source, report, environment, knownTalents, knownConfiguredItems, missingMods, false
                ).compile(
                        json.get("acquireCondition"), "$.acquireCondition"
                )
                : new CompiledCondition.Always(true);
        SynthesisDefinition synthesis = compileSynthesis(
                id, json, environment, knownTalents, knownConfiguredItems, missingMods, source, report
        );
        TalentLifecycleDefinition lifecycle = compileLifecycle(json, source, report);

        List<CompiledAction> activeEffects = compileActions(
                id, json.get("activeEffects"), "$.activeEffects", ActionPlacement.ACTIVE,
                environment, knownTalents, knownConfiguredItems, missingMods, source, report, false
        );
        validateModifierKeys(activeEffects, source, report);
        validateActiveEffectReferences(activeEffects, pools, knownCounters, source, report);
        List<CompiledTrigger> triggers = compileTriggers(
                id, json.get("triggers"), environment, knownTalents, knownConfiguredItems,
                knownCounters, missingMods, source, report
        );
        validateTriggerActionReferences(
                triggers, knownTalents, knownCounters, rarities, pools, source, report
        );
        List<CompiledAction> onAcquire = compileActions(
                id, json.get("onAcquire"), "$.onAcquire", ActionPlacement.ON_ACQUIRE,
                environment, knownTalents, knownConfiguredItems, missingMods, source, report, false
        );
        List<CompiledAction> onSynthesize = compileActions(
                id, json.get("onSynthesize"), "$.onSynthesize", ActionPlacement.ON_SYNTHESIZE,
                environment, knownTalents, knownConfiguredItems, missingMods, source, report, false
        );
        List<CompiledAction> onRemove = compileActions(
                id, json.get("onRemove"), "$.onRemove", ActionPlacement.ON_REMOVE,
                environment, knownTalents, knownConfiguredItems, missingMods, source, report, false
        );
        validateActionReferences(onAcquire, "$.onAcquire", knownTalents, knownCounters,
                rarities, pools, source, report);
        validateActionReferences(onSynthesize, "$.onSynthesize", knownTalents, knownCounters,
                rarities, pools, source, report);
        validateActionReferences(onRemove, "$.onRemove", knownTalents, knownCounters,
                rarities, pools, source, report);

        if (offer.enabled()) {
            for (int index = 0; index < offer.pools().size(); index++) {
                ResourceLocation poolId = offer.pools().get(index);
                if (!pools.containsKey(poolId)) {
                    report.error("UNKNOWN_POOL", source, "$.offer.pools[" + index + "]",
                            "Unknown talent pool '" + poolId + "'");
                }
            }
            if (offer.pools().isEmpty() && pools.values().stream().noneMatch(TalentPoolDefinition::enabled)) {
                report.warning("NO_ENABLED_POOL", source, "$.offer.pools",
                        "Offer-enabled talent has no enabled pool to enter");
            }
            if (offer.weight() == 0.0D) {
                report.warning("ZERO_TALENT_WEIGHT", source, "$.offer.weight",
                        "Offer-enabled talent has zero weight");
            }
            if (rarityDefinition != null && !rarityDefinition.offerable()) {
                report.warning("RARITY_NOT_OFFERABLE", source, "$.rarity",
                        "Talent offer is enabled but its rarity is not offerable");
            }
        }

        return new TalentDefinition(
                id, SCHEMA_VERSION, enabled, name, description, showGeneratedEffects, rarity,
                categories, icon, offer, capacityCost, tags, requiredMods, available, missingMods,
                acquireCondition, synthesis, lifecycle, activeEffects, triggers, onAcquire, onSynthesize, onRemove
        );
    }

    private static TalentLifecycleDefinition compileLifecycle(
            JsonObject talent, String source, ValidationReport.Builder report
    ) {
        if (!talent.has("lifecycle")) return TalentLifecycleDefinition.permanent();
        JsonObject lifecycle = JsonAccess.object(talent, "lifecycle", false, source, "$", report);
        JsonAccess.knownKeys(lifecycle, LIFECYCLE_FIELDS, source, "$.lifecycle", report);
        int durationTicks = JsonAccess.integer(
                lifecycle, "durationTicks", 0, 0, 172_800_000, source, "$.lifecycle", report
        );
        int removeAfterDraws = JsonAccess.integer(
                lifecycle, "removeAfterDraws", 0, 0, 1_000_000, source, "$.lifecycle", report
        );
        boolean retainEffects = JsonAccess.bool(
                lifecycle, "retainEffects", false, source, "$.lifecycle", report
        );
        boolean retainCategory = JsonAccess.bool(
                lifecycle, "retainCategoryContribution", false, source, "$.lifecycle", report
        );
        boolean retainTriggers = JsonAccess.bool(
                lifecycle, "retainTriggers", false, source, "$.lifecycle", report
        );
        if (durationTicks == 0 && removeAfterDraws == 0
                && (retainEffects || retainCategory || retainTriggers)) {
            report.warning("LIFECYCLE_RETENTION_UNUSED", source, "$.lifecycle",
                    "Retention flags have no effect without durationTicks or removeAfterDraws");
        }
        return new TalentLifecycleDefinition(
                durationTicks, removeAfterDraws, retainEffects, retainCategory, retainTriggers
        );
    }

    private SynthesisDefinition compileSynthesis(
            ResourceLocation talentId,
            JsonObject talent,
            CompilationEnvironment environment,
            Set<ResourceLocation> knownTalents,
            Set<ResourceLocation> knownConfiguredItems,
            List<String> missingMods,
            String source,
            ValidationReport.Builder report
    ) {
        if (!talent.has("synthesis")) return SynthesisDefinition.disabled();
        JsonObject synthesis = JsonAccess.object(talent, "synthesis", false, source, "$", report);
        JsonAccess.knownKeys(synthesis, SYNTHESIS_FIELDS, source, "$.synthesis", report);
        boolean enabled = JsonAccess.bool(synthesis, "enabled", false, source, "$.synthesis", report);
        List<ResourceLocation> consumeTalents = JsonAccess.idList(
                synthesis, "consumeTalents", true, source, "$.synthesis", report
        );
        for (int index = 0; index < consumeTalents.size(); index++) {
            ResourceLocation consumed = consumeTalents.get(index);
            String path = "$.synthesis.consumeTalents[" + index + "]";
            if (consumed.equals(talentId)) {
                report.error("SYNTHESIS_CONSUMES_RESULT", source, path,
                        "A synthesis cannot consume its own result talent");
            } else if (!knownTalents.contains(consumed)) {
                report.error("UNKNOWN_TALENT", source, path, "Unknown talent '" + consumed + "'");
            }
        }
        Map<ResourceLocation, Long> counters = compileLongCosts(
                synthesis, "consumeCounters", 1_000_000_000_000L, source, report
        );
        Map<ResourceLocation, Long> rawItems = compileLongCosts(
                synthesis, "consumeItems", 1_000_000L, source, report
        );
        LinkedHashMap<ResourceLocation, Integer> items = new LinkedHashMap<>();
        rawItems.forEach((id, count) -> {
            if (!environment.hasItem(id) && !knownConfiguredItems.contains(id)) {
                if (missingMods.isEmpty()) {
                    report.error("UNKNOWN_ITEM", source, "$.synthesis.consumeItems." + id,
                            "Unknown item '" + id + "'");
                } else {
                    report.warning("ITEM_UNAVAILABLE_WITH_MISSING_MOD", source,
                            "$.synthesis.consumeItems." + id,
                            "Item is unavailable while required mods are missing: " + id);
                }
            }
            items.put(id, count.intValue());
        });
        int totalCosts = consumeTalents.size() + counters.size() + items.size();
        if (totalCosts > MAX_SYNTHESIS_COSTS) {
            report.error("SYNTHESIS_COST_LIMIT", source, "$.synthesis",
                    "Synthesis has " + totalCosts + " costs; maximum is " + MAX_SYNTHESIS_COSTS);
        }
        return new SynthesisDefinition(enabled, consumeTalents, counters, items);
    }

    private static Map<ResourceLocation, Long> compileLongCosts(
            JsonObject parent,
            String field,
            long maximum,
            String source,
            ValidationReport.Builder report
    ) {
        JsonObject values = JsonAccess.object(parent, field, false, source, "$.synthesis", report);
        LinkedHashMap<ResourceLocation, Long> result = new LinkedHashMap<>();
        for (Map.Entry<String, JsonElement> entry : values.entrySet()) {
            String path = "$.synthesis." + field + "." + entry.getKey();
            ResourceLocation id = JsonAccess.parseId(entry.getKey(), true, source, path, report);
            long count = 0L;
            if (!(entry.getValue() instanceof JsonPrimitive primitive) || !primitive.isNumber()) {
                report.error("EXPECTED_INTEGER", source, path, "Expected a positive integer cost");
            } else {
                try {
                    String raw = primitive.getAsString();
                    count = primitive.getAsLong();
                    if (!raw.matches("[0-9]+") || count < 1L || count > maximum) {
                        report.error("INTEGER_RANGE", source, path,
                                "Expected an integer in range 1.." + maximum);
                        count = 0L;
                    }
                } catch (RuntimeException ex) {
                    report.error("EXPECTED_INTEGER", source, path, "Expected a positive integer cost");
                    count = 0L;
                }
            }
            if (id != null && count > 0L) result.put(id, count);
        }
        return Map.copyOf(result);
    }

    private OfferSettings compileOffer(JsonObject talent, String source, ValidationReport.Builder report) {
        if (!talent.has("offer")) return OfferSettings.disabled();
        JsonObject offer = JsonAccess.object(talent, "offer", false, source, "$", report);
        JsonAccess.knownKeys(offer, OFFER_FIELDS, source, "$.offer", report);
        boolean enabled = JsonAccess.bool(offer, "enabled", false, source, "$.offer", report);
        double weight = JsonAccess.decimal(
                offer, "weight", 1.0D, 0.0D, 1.0E9D, source, "$.offer", report
        );
        List<ResourceLocation> pools = JsonAccess.idList(
                offer, "pools", true, source, "$.offer", report
        );
        return new OfferSettings(enabled, weight, pools);
    }

    private List<CompiledTrigger> compileTriggers(
            ResourceLocation talentId,
            JsonElement element,
            CompilationEnvironment environment,
            Set<ResourceLocation> knownTalents,
            Set<ResourceLocation> knownConfiguredItems,
            Set<ResourceLocation> knownCounters,
            List<String> missingMods,
            String source,
            ValidationReport.Builder report
    ) {
        if (element == null) return List.of();
        if (!element.isJsonArray()) {
            report.error("EXPECTED_ARRAY", source, "$.triggers", "Expected an array");
            return List.of();
        }
        JsonArray array = element.getAsJsonArray();
        if (array.size() > MAX_TRIGGERS) {
            report.error("TRIGGER_LIMIT", source, "$.triggers",
                    "A talent can define at most " + MAX_TRIGGERS + " triggers");
        }
        ArrayList<CompiledTrigger> result = new ArrayList<>();
        HashSet<String> ids = new HashSet<>();
        for (int index = 0; index < Math.min(array.size(), MAX_TRIGGERS); index++) {
            String path = "$.triggers[" + index + "]";
            JsonElement item = array.get(index);
            if (!item.isJsonObject()) {
                report.error("EXPECTED_OBJECT", source, path, "Expected a trigger object");
                continue;
            }
            JsonObject trigger = item.getAsJsonObject();
            JsonAccess.knownKeys(trigger, TRIGGER_FIELDS, source, path, report);
            String triggerId = JsonAccess.string(
                    trigger, "id", "trigger_" + index, false, source, path, report
            ).toLowerCase(Locale.ROOT);
            if (!triggerId.matches(LOCAL_KEY_PATTERN)) {
                report.error("INVALID_TRIGGER_ID", source, JsonAccess.child(path, "id"),
                        "Trigger id must use lowercase letters, digits, dot, underscore, or dash");
            }
            if (!ids.add(triggerId)) {
                report.error("DUPLICATE_TRIGGER_ID", source, JsonAccess.child(path, "id"),
                        "Duplicate trigger id '" + triggerId + "'");
            }

            String rawEvent = JsonAccess.string(
                    trigger, "event", "", true, source, path, report
            );
            if (!rawEvent.contains(":")) rawEvent = TalentEditorMod.MOD_ID + ":" + rawEvent;
            ResourceLocation event = JsonAccess.parseId(
                    rawEvent, true, source, JsonAccess.child(path, "event"), report
            );
            if (event == null) event = new ResourceLocation(TalentEditorMod.MOD_ID, "invalid");
            int priority = JsonAccess.integer(
                    trigger, "priority", 0, -1_000, 1_000, source, path, report
            );
            double chance = JsonAccess.decimal(
                    trigger, "chance", 1.0D, 0.0D, 1.0D, source, path, report
            );
            Optional<ResourceLocation> chanceAttribute = Optional.empty();
            if (trigger.has("chanceAttribute")) {
                ResourceLocation parsedAttribute = JsonAccess.requiredId(
                        trigger, "chanceAttribute", "minecraft:generic.luck", true,
                        source, path, report
                );
                chanceAttribute = Optional.of(parsedAttribute);
                if (!environment.hasAttribute(parsedAttribute)) {
                    if (missingMods.isEmpty()) {
                        report.error("UNKNOWN_ATTRIBUTE", source, JsonAccess.child(path, "chanceAttribute"),
                                "Chance attribute is not registered in the current server environment: "
                                        + parsedAttribute);
                    } else {
                        report.warning("ATTRIBUTE_UNAVAILABLE_WITH_MISSING_MOD", source,
                                JsonAccess.child(path, "chanceAttribute"),
                                "Chance attribute is unavailable while required mods are missing: "
                                        + parsedAttribute);
                    }
                }
            }
            Optional<ResourceLocation> chanceCounter = Optional.empty();
            if (trigger.has("chanceCounter")) {
                ResourceLocation parsedCounter = JsonAccess.requiredId(
                        trigger, "chanceCounter", TalentEditorMod.MOD_ID + ":counter", true,
                        source, path, report
                );
                chanceCounter = Optional.of(parsedCounter);
                if (!knownCounters.contains(parsedCounter)) {
                    report.error("UNKNOWN_COUNTER", source, JsonAccess.child(path, "chanceCounter"),
                            "Unknown counter '" + parsedCounter + "'");
                }
            }
            Optional<ResourceLocation> chanceCategory = Optional.empty();
            if (trigger.has("chanceCategory")) {
                ResourceLocation parsedCategory = JsonAccess.requiredId(
                        trigger, "chanceCategory", TalentEditorMod.MOD_ID + ":category", true,
                        source, path, report
                );
                chanceCategory = Optional.of(parsedCategory);
            }
            if ((chanceAttribute.isPresent() ? 1 : 0) + (chanceCounter.isPresent() ? 1 : 0)
                    + (chanceCategory.isPresent() ? 1 : 0) > 1) {
                report.error("MULTIPLE_CHANCE_SOURCES", source, path,
                        "chanceAttribute, chanceCounter, and chanceCategory cannot be used together");
                chanceCounter = Optional.empty();
                chanceCategory = Optional.empty();
            }
            double chancePerPoint = JsonAccess.decimal(
                    trigger, "chancePerPoint", 0.0D, -1.0D, 1.0D, source, path, report
            );
            if (chanceAttribute.isEmpty() && chanceCounter.isEmpty() && chanceCategory.isEmpty()
                    && trigger.has("chancePerPoint") && chancePerPoint != 0.0D) {
                report.error("CHANCE_ATTRIBUTE_REQUIRED", source, JsonAccess.child(path, "chancePerPoint"),
                        "chancePerPoint requires chanceAttribute, chanceCounter, or chanceCategory");
                chancePerPoint = 0.0D;
            }
            double distanceInterval = JsonAccess.decimal(
                    trigger, "distanceInterval", 1.0D, 0.01D, 1_000_000.0D,
                    source, path, report
            );
            ResourceLocation movementEvent = new ResourceLocation(TalentEditorMod.MOD_ID, "player_move_distance");
            if (trigger.has("distanceInterval") && !event.equals(movementEvent)) {
                report.error("DISTANCE_INTERVAL_EVENT_REQUIRED", source,
                        JsonAccess.child(path, "distanceInterval"),
                        "distanceInterval is only available for player_move_distance triggers");
            }
            int cooldownTicks = JsonAccess.integer(
                    trigger, "cooldownTicks", 0, 0, 1_728_000, source, path, report
            );
            int activationLimit = JsonAccess.integer(
                    trigger, "activationLimit", 0, 0, 1_000_000, source, path, report
            );
            CompiledCondition condition = trigger.has("condition")
                    ? new ConditionCompiler(
                            source, report, environment, knownTalents, knownConfiguredItems, missingMods, true
                    ).compile(
                            trigger.get("condition"), JsonAccess.child(path, "condition")
                    )
                    : new CompiledCondition.Always(true);
            List<CompiledAction> actions = compileActions(
                    talentId, trigger.get("actions"), JsonAccess.child(path, "actions"),
                    ActionPlacement.TRIGGER, environment, knownTalents, knownConfiguredItems,
                    missingMods, source, report, true
            );
            String rawFailurePolicy = JsonAccess.string(
                    trigger, "actionFailurePolicy", "continue", false, source, path, report
            );
            ActionFailurePolicy failurePolicy = ActionFailurePolicy.parse(rawFailurePolicy).orElseGet(() -> {
                report.error("INVALID_ACTION_FAILURE_POLICY", source,
                        JsonAccess.child(path, "actionFailurePolicy"), "Expected continue or stop");
                return ActionFailurePolicy.CONTINUE;
            });
            List<CompiledAction> failureActions = trigger.has("failureActions")
                    ? compileActions(
                            talentId, trigger.get("failureActions"), JsonAccess.child(path, "failureActions"),
                            ActionPlacement.TRIGGER, environment, knownTalents, knownConfiguredItems,
                            missingMods, source, report, false
                    )
                    : List.of();
            if (!DAMAGE_TRIGGER_EVENTS.contains(event)
                    && java.util.stream.Stream.concat(actions.stream(), failureActions.stream())
                    .map(ConditionalAction::unwrap)
                    .anyMatch(action -> action instanceof ModifyDamageAction
                            || action instanceof ModifyDamageExpressionAction
                            || action instanceof CancelDamageAction)) {
                report.error("DAMAGE_ACTION_EVENT_UNSUPPORTED", source,
                        path,
                        "modify_damage and cancel_damage require a damage event");
            }
            result.add(new CompiledTrigger(
                    triggerId, event, priority, chance, chanceAttribute, chanceCounter, chanceCategory, chancePerPoint,
                    distanceInterval, cooldownTicks, activationLimit, condition, actions,
                    failurePolicy, failureActions
            ));
        }
        return List.copyOf(result);
    }

    private List<CompiledAction> compileActions(
            ResourceLocation talentId,
            JsonElement element,
            String path,
            ActionPlacement placement,
            CompilationEnvironment environment,
            Set<ResourceLocation> knownTalents,
            Set<ResourceLocation> knownConfiguredItems,
            List<String> missingMods,
            String source,
            ValidationReport.Builder report,
            boolean required
    ) {
        if (element == null) {
            if (required) report.error("REQUIRED_FIELD", source, path, "Action array is required");
            return List.of();
        }
        if (!element.isJsonArray()) {
            report.error("EXPECTED_ARRAY", source, path, "Expected an action array");
            return List.of();
        }
        JsonArray array = element.getAsJsonArray();
        if (array.size() > MAX_ACTIONS_PER_LIST) {
            report.error("ACTION_LIMIT", source, path,
                    "An action list can contain at most " + MAX_ACTIONS_PER_LIST + " entries");
        }
        if (required && array.isEmpty()) {
            report.error("EMPTY_ACTION_LIST", source, path, "At least one action is required");
        }
        ArrayList<CompiledAction> result = new ArrayList<>();
        for (int index = 0; index < Math.min(array.size(), MAX_ACTIONS_PER_LIST); index++) {
            String itemPath = path + "[" + index + "]";
            JsonElement item = array.get(index);
            if (!item.isJsonObject()) {
                report.error("EXPECTED_OBJECT", source, itemPath, "Expected an action object");
                continue;
            }
            JsonObject action = item.getAsJsonObject();
            boolean hasEnable = action.has("enabledWhen");
            boolean hasDisable = action.has("disabledWhen");
            CompiledCondition enabledWhen = hasEnable ? new ConditionCompiler(
                    source, report, environment, knownTalents, knownConfiguredItems, missingMods,
                    true
            ).compile(action.get("enabledWhen"), JsonAccess.child(itemPath, "enabledWhen")) : null;
            CompiledCondition disabledWhen = hasDisable ? new ConditionCompiler(
                    source, report, environment, knownTalents, knownConfiguredItems, missingMods,
                    true
            ).compile(action.get("disabledWhen"), JsonAccess.child(itemPath, "disabledWhen")) : null;
            if (hasEnable || hasDisable) {
                action = action.deepCopy();
                action.remove("enabledWhen");
                action.remove("disabledWhen");
            }
            String rawType = JsonAccess.string(action, "type", "", true, source, itemPath, report);
            if (!rawType.contains(":")) rawType = TalentEditorMod.MOD_ID + ":" + rawType;
            ResourceLocation type = JsonAccess.parseId(
                    rawType, true, source, JsonAccess.child(itemPath, "type"), report
            );
            ActionDecoder decoder = type == null ? null : actionTypes.get(type);
            if (decoder == null) {
                report.error("UNKNOWN_ACTION_TYPE", source, JsonAccess.child(itemPath, "type"),
                        "Unknown action type '" + rawType + "'");
                continue;
            }
            if (!decoder.supportedPlacements().contains(placement)) {
                report.error("ACTION_PLACEMENT_UNSUPPORTED", source, itemPath,
                        "Action '" + type + "' cannot be used in " + placement.name().toLowerCase(Locale.ROOT));
                continue;
            }
            ActionDecodeContext context = new ActionDecodeContext(
                    talentId, source, itemPath, placement, environment, missingMods, report
            );
            CompiledCondition requiredCondition = enabledWhen;
            CompiledCondition forbiddenCondition = disabledWhen;
            decoder.decode(context, action).ifPresent(decoded -> result.add(
                    hasEnable || hasDisable ? new ConditionalAction(decoded,
                            java.util.Optional.ofNullable(requiredCondition),
                            java.util.Optional.ofNullable(forbiddenCondition)) : decoded
            ));
        }
        return List.copyOf(result);
    }

    private static List<String> normalizeRequiredMods(
            List<String> values,
            String source,
            ValidationReport.Builder report
    ) {
        ArrayList<String> result = new ArrayList<>();
        HashSet<String> seen = new HashSet<>();
        for (int index = 0; index < values.size(); index++) {
            String value = values.get(index).toLowerCase(Locale.ROOT);
            if (!value.matches(MOD_ID_PATTERN)) {
                report.error("INVALID_MOD_ID", source, "$.requiredMods[" + index + "]",
                        "Invalid mod id '" + value + "'");
            } else if (!seen.add(value)) {
                report.error("DUPLICATE_MOD_ID", source, "$.requiredMods[" + index + "]",
                        "Duplicate mod id '" + value + "'");
            } else {
                result.add(value);
            }
        }
        return List.copyOf(result);
    }

    private static void validateModifierKeys(
            List<CompiledAction> actions,
            String source,
            ValidationReport.Builder report
    ) {
        HashSet<String> keys = new HashSet<>();
        for (int index = 0; index < actions.size(); index++) {
            CompiledAction action = ConditionalAction.unwrap(actions.get(index));
            String modifierKey = action instanceof AttributeModifierAction attribute
                    ? attribute.modifierKey()
                    : action instanceof DynamicAttributeModifierAction dynamic ? dynamic.modifierKey() : null;
            if (modifierKey != null && !keys.add(modifierKey)) {
                report.error("DUPLICATE_MODIFIER_KEY", source, "$.activeEffects[" + index + "].modifierKey",
                        "Duplicate modifier key '" + modifierKey + "'");
            }
        }
        long flightActions = actions.stream().map(ConditionalAction::unwrap)
                .filter(CreativeFlightAction.class::isInstance).count();
        if (flightActions > 1L) {
            report.error("DUPLICATE_CREATIVE_FLIGHT", source, "$.activeEffects",
                    "A talent can contain at most one creative_flight action");
        }
    }

    private static void validateActiveEffectReferences(
            List<CompiledAction> actions,
            Map<ResourceLocation, TalentPoolDefinition> pools,
            Set<ResourceLocation> counters,
            String source,
            ValidationReport.Builder report
    ) {
        for (int actionIndex = 0; actionIndex < actions.size(); actionIndex++) {
            CompiledAction action = ConditionalAction.unwrap(actions.get(actionIndex));
            if (action instanceof DrawRefreshBonusAction bonus) {
                for (int poolIndex = 0; poolIndex < bonus.pools().size(); poolIndex++) {
                    ResourceLocation pool = bonus.pools().get(poolIndex);
                    if (!pools.containsKey(pool)) {
                        report.error("UNKNOWN_POOL", source,
                                "$.activeEffects[" + actionIndex + "].pools[" + poolIndex + "]",
                                "Unknown talent pool '" + pool + "'");
                    }
                }
            }
            if (action instanceof DynamicAttributeModifierAction dynamic
                    && dynamic.counter().isPresent() && !counters.contains(dynamic.counter().orElseThrow())) {
                report.error("UNKNOWN_COUNTER", source,
                        "$.activeEffects[" + actionIndex + "].counter",
                        "Unknown counter '" + dynamic.counter().orElseThrow() + "'");
            }
        }
    }

    private static void validateTriggerActionReferences(
            List<CompiledTrigger> triggers,
            Set<ResourceLocation> talents,
            Set<ResourceLocation> counters,
            Map<ResourceLocation, RarityDefinition> rarities,
            Map<ResourceLocation, TalentPoolDefinition> pools,
            String source,
            ValidationReport.Builder report
    ) {
        for (int triggerIndex = 0; triggerIndex < triggers.size(); triggerIndex++) {
            validateActionReferences(triggers.get(triggerIndex).actions(),
                    "$.triggers[" + triggerIndex + "].actions", talents, counters,
                    rarities, pools, source, report);
            validateActionReferences(triggers.get(triggerIndex).failureActions(),
                    "$.triggers[" + triggerIndex + "].failureActions", talents, counters,
                    rarities, pools, source, report);
        }
    }

    private static void validateActionReferences(
            List<CompiledAction> actions,
            String arrayPath,
            Set<ResourceLocation> talents,
            Set<ResourceLocation> counters,
            Map<ResourceLocation, RarityDefinition> rarities,
            Map<ResourceLocation, TalentPoolDefinition> pools,
            String source,
            ValidationReport.Builder report
    ) {
        for (int actionIndex = 0; actionIndex < actions.size(); actionIndex++) {
            String path = arrayPath + "[" + actionIndex + "]";
            CompiledAction action = ConditionalAction.unwrap(actions.get(actionIndex));
            if (action instanceof GrantTalentAction grant && !talents.contains(grant.talent())) {
                report.error("UNKNOWN_TALENT", source, path + ".talent",
                        "Unknown talent '" + grant.talent() + "'");
            }
            if (action instanceof RevokeTalentAction revoke && !talents.contains(revoke.talent())) {
                report.error("UNKNOWN_TALENT", source, path + ".talent",
                        "Unknown talent '" + revoke.talent() + "'");
            }
            if (action instanceof ModifyCounterAction modifyCounter
                    && !counters.contains(modifyCounter.counter())) {
                report.error("UNKNOWN_COUNTER", source, path + ".counter",
                        "Unknown counter '" + modifyCounter.counter() + "'");
            }
            if (!(action instanceof QueueDrawConstraintAction constraint)) continue;
            validateDrawConstraintSpec(
                    constraint.spec(), talents, rarities, pools, source, path, report
            );
            constraint.successCounter().ifPresent(counter -> {
                if (!counters.contains(counter)) {
                    report.error("UNKNOWN_COUNTER", source, path + ".successCounter",
                            "Unknown counter '" + counter + "'");
                }
            });
            constraint.milestone().ifPresent(spec -> validateDrawConstraintSpec(
                    spec, talents, rarities, pools, source, path + ".milestone", report
            ));
        }
    }

    private static void validateDrawConstraintSpec(
            com.kidxoxo.talenteditor.domain.DrawConstraintSpec constraint,
            Set<ResourceLocation> talents,
            Map<ResourceLocation, RarityDefinition> rarities,
            Map<ResourceLocation, TalentPoolDefinition> pools,
            String source,
            String path,
            ValidationReport.Builder report
    ) {
        validateKnownIds(constraint.talents(), talents, "UNKNOWN_TALENT",
                source, path + ".talents", "talent", report);
        validateKnownIds(constraint.rarities(), rarities.keySet(), "UNKNOWN_RARITY",
                source, path + ".rarities", "rarity", report);
        validateKnownIds(constraint.pools(), pools.keySet(), "UNKNOWN_POOL",
                source, path + ".pools", "talent pool", report);
    }

    private static void validateKnownIds(
            List<ResourceLocation> values,
            Set<ResourceLocation> known,
            String code,
            String source,
            String path,
            String kind,
            ValidationReport.Builder report
    ) {
        for (int index = 0; index < values.size(); index++) {
            ResourceLocation value = values.get(index);
            if (!known.contains(value)) {
                report.error(code, source, path + "[" + index + "]",
                        "Unknown " + kind + " '" + value + "'");
            }
        }
    }

    private static void requireSchema(JsonObject json, String source, ValidationReport.Builder report) {
        if (!json.has("schema")) {
            report.error("REQUIRED_FIELD", source, "$.schema", "schema is required");
            return;
        }
        int schema = JsonAccess.integer(
                json, "schema", SCHEMA_VERSION, Integer.MIN_VALUE, Integer.MAX_VALUE, source, "$", report
        );
        if (schema != SCHEMA_VERSION) {
            report.error("UNSUPPORTED_SCHEMA", source, "$.schema",
                    "Expected schema " + SCHEMA_VERSION + " but found " + schema);
        }
    }
}
