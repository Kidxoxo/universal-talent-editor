package com.kidxoxo.talenteditor.compile;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.kidxoxo.talenteditor.TalentEditorMod;
import com.kidxoxo.talenteditor.domain.CompiledCondition;
import com.kidxoxo.talenteditor.validation.ValidationReport;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

final class ConditionCompiler {
    private static final int MAX_DEPTH = 16;
    private static final int MAX_NODES = 256;

    private final String source;
    private final ValidationReport.Builder report;
    private final CompilationEnvironment environment;
    private final Set<ResourceLocation> knownTalents;
    private final Set<ResourceLocation> knownConfiguredItems;
    private final List<String> missingMods;
    private final boolean allowEventContext;
    private int nodes;

    ConditionCompiler(
            String source,
            ValidationReport.Builder report,
            CompilationEnvironment environment,
            Set<ResourceLocation> knownTalents,
            Set<ResourceLocation> knownConfiguredItems,
            List<String> missingMods,
            boolean allowEventContext
    ) {
        this.source = source;
        this.report = report;
        this.environment = environment;
        this.knownTalents = knownTalents;
        this.knownConfiguredItems = knownConfiguredItems;
        this.missingMods = missingMods;
        this.allowEventContext = allowEventContext;
    }

    CompiledCondition compile(JsonElement element, String path) {
        return compile(element, path, 0);
    }

    private CompiledCondition compile(JsonElement element, String path, int depth) {
        nodes++;
        if (depth > MAX_DEPTH) {
            report.error("CONDITION_DEPTH_LIMIT", source, path, "Condition tree exceeds depth " + MAX_DEPTH);
            return new CompiledCondition.Always(false);
        }
        if (nodes > MAX_NODES) {
            report.error("CONDITION_NODE_LIMIT", source, path, "Condition tree exceeds " + MAX_NODES + " nodes");
            return new CompiledCondition.Always(false);
        }
        if (element == null || !element.isJsonObject()) {
            report.error("EXPECTED_CONDITION", source, path, "Expected a condition object");
            return new CompiledCondition.Always(false);
        }

        JsonObject object = element.getAsJsonObject();
        String rawType = JsonAccess.string(object, "type", "always", true, source, path, report);
        ResourceLocation type = JsonAccess.parseId(
                rawType.contains(":") ? rawType : TalentEditorMod.MOD_ID + ":" + rawType,
                true, source, JsonAccess.child(path, "type"), report
        );
        if (CompiledCondition.Always.TYPE.equals(type)) {
            JsonAccess.knownKeys(object, Set.of("type", "value"), source, path, report);
            return new CompiledCondition.Always(JsonAccess.bool(object, "value", true, source, path, report));
        }
        if (CompiledCondition.All.TYPE.equals(type) || CompiledCondition.Any.TYPE.equals(type)) {
            JsonAccess.knownKeys(object, Set.of("type", "children"), source, path, report);
            JsonArray children = JsonAccess.array(object, "children", true, source, path, report);
            if (children.isEmpty()) {
                report.error("EMPTY_CONDITION_GROUP", source, JsonAccess.child(path, "children"),
                        "Condition group must contain at least one child");
            }
            ArrayList<CompiledCondition> compiled = new ArrayList<>();
            for (int index = 0; index < children.size(); index++) {
                compiled.add(compile(children.get(index), JsonAccess.child(path, "children") + "[" + index + "]",
                        depth + 1));
            }
            return CompiledCondition.All.TYPE.equals(type)
                    ? new CompiledCondition.All(compiled)
                    : new CompiledCondition.Any(compiled);
        }
        if (CompiledCondition.Not.TYPE.equals(type)) {
            JsonAccess.knownKeys(object, Set.of("type", "child"), source, path, report);
            return new CompiledCondition.Not(compile(object.get("child"), JsonAccess.child(path, "child"), depth + 1));
        }
        if (CompiledCondition.HasTalent.TYPE.equals(type)) {
            JsonAccess.knownKeys(object, Set.of("type", "talent"), source, path, report);
            ResourceLocation talent = JsonAccess.requiredId(
                    object, "talent", "minecraft:air", true, source, path, report
            );
            if (!knownTalents.contains(talent)) {
                report.error("UNKNOWN_TALENT", source, JsonAccess.child(path, "talent"),
                        "Unknown talent '" + talent + "'");
            }
            return new CompiledCondition.HasTalent(talent);
        }
        if (CompiledCondition.CounterAtLeast.TYPE.equals(type)) {
            JsonAccess.knownKeys(object, Set.of("type", "counter", "value"), source, path, report);
            ResourceLocation counter = JsonAccess.requiredId(
                    object, "counter", "minecraft:invalid", true, source, path, report
            );
            long value = JsonAccess.longInteger(
                    object, "value", 1L, 0L, 1_000_000_000_000L, source, path, report
            );
            return new CompiledCondition.CounterAtLeast(counter, value);
        }
        if (CompiledCondition.CategoryCountAtLeast.TYPE.equals(type)) {
            JsonAccess.knownKeys(object, Set.of("type", "category", "count"), source, path, report);
            ResourceLocation category = JsonAccess.requiredId(
                    object, "category", "minecraft:invalid", true, source, path, report
            );
            int count = JsonAccess.integer(object, "count", 1, 0, 1_000_000, source, path, report);
            return new CompiledCondition.CategoryCountAtLeast(category, count);
        }
        if (CompiledCondition.CounterCategoryTotalAtLeast.TYPE.equals(type)) {
            JsonAccess.knownKeys(object, Set.of("type", "category", "value"), source, path, report);
            ResourceLocation category = JsonAccess.requiredId(
                    object, "category", "minecraft:invalid", true, source, path, report
            );
            long value = JsonAccess.longInteger(
                    object, "value", 1L, 0L, 1_000_000_000_000L, source, path, report
            );
            return new CompiledCondition.CounterCategoryTotalAtLeast(category, value);
        }
        if (CompiledCondition.HasItem.TYPE.equals(type)) {
            JsonAccess.knownKeys(object, Set.of("type", "item", "count"), source, path, report);
            ResourceLocation item = JsonAccess.requiredId(
                    object, "item", "minecraft:air", true, source, path, report
            );
            int count = JsonAccess.integer(object, "count", 1, 1, 1_000_000, source, path, report);
            if (!environment.hasItem(item) && !knownConfiguredItems.contains(item)) {
                if (missingMods.isEmpty()) {
                    report.error("UNKNOWN_ITEM", source, JsonAccess.child(path, "item"),
                            "Unknown item '" + item + "'");
                } else {
                    report.warning("ITEM_UNAVAILABLE_WITH_MISSING_MOD", source, JsonAccess.child(path, "item"),
                            "Item is unavailable while required mods are missing: " + item);
                }
            }
            return new CompiledCondition.HasItem(item, count);
        }
        if (CompiledCondition.EventEntityType.TYPE.equals(type)) {
            rejectOutsideEvent(path);
            JsonAccess.knownKeys(object, Set.of("type", "entityType"), source, path, report);
            ResourceLocation entityType = JsonAccess.requiredId(
                    object, "entityType", "minecraft:pig", true, source, path, report
            );
            if (!environment.hasEntityType(entityType)) {
                if (missingMods.isEmpty()) {
                    report.error("UNKNOWN_ENTITY_TYPE", source, JsonAccess.child(path, "entityType"),
                            "Unknown entity type '" + entityType + "'");
                } else {
                    report.warning("ENTITY_TYPE_UNAVAILABLE_WITH_MISSING_MOD", source,
                            JsonAccess.child(path, "entityType"),
                            "Entity type is unavailable while required mods are missing: " + entityType);
                }
            }
            return new CompiledCondition.EventEntityType(entityType);
        }
        if (CompiledCondition.EventEntityTag.TYPE.equals(type)) {
            rejectOutsideEvent(path);
            JsonAccess.knownKeys(object, Set.of("type", "tag"), source, path, report);
            return new CompiledCondition.EventEntityTag(JsonAccess.requiredId(
                    object, "tag", "minecraft:undead", true, source, path, report
            ));
        }
        if (CompiledCondition.DamageTypeTag.TYPE.equals(type)) {
            rejectOutsideEvent(path);
            JsonAccess.knownKeys(object, Set.of("type", "tag"), source, path, report);
            return new CompiledCondition.DamageTypeTag(JsonAccess.requiredId(
                    object, "tag", "minecraft:is_fire", true, source, path, report
            ));
        }
        if (CompiledCondition.EventItem.TYPE.equals(type)) {
            rejectOutsideEvent(path);
            JsonAccess.knownKeys(object, Set.of("type", "item"), source, path, report);
            ResourceLocation item = JsonAccess.requiredId(
                    object, "item", "minecraft:air", true, source, path, report
            );
            if (!environment.hasItem(item) && !knownConfiguredItems.contains(item)) {
                if (missingMods.isEmpty()) {
                    report.error("UNKNOWN_EVENT_ITEM", source, JsonAccess.child(path, "item"),
                            "Unknown event item '" + item + "'");
                } else {
                    report.warning("EVENT_ITEM_UNAVAILABLE_WITH_MISSING_MOD", source,
                            JsonAccess.child(path, "item"),
                            "Event item is unavailable while required mods are missing: " + item);
                }
            }
            return new CompiledCondition.EventItem(item);
        }
        if (CompiledCondition.EventItemTag.TYPE.equals(type)) {
            rejectOutsideEvent(path);
            JsonAccess.knownKeys(object, Set.of("type", "tag"), source, path, report);
            return new CompiledCondition.EventItemTag(JsonAccess.requiredId(
                    object, "tag", "minecraft:tools", true, source, path, report
            ));
        }
        if (CompiledCondition.EventBlock.TYPE.equals(type)) {
            rejectOutsideEvent(path);
            JsonAccess.knownKeys(object, Set.of("type", "block"), source, path, report);
            ResourceLocation block = JsonAccess.requiredId(
                    object, "block", "minecraft:stone", true, source, path, report
            );
            if (!environment.hasBlock(block)) {
                if (missingMods.isEmpty()) {
                    report.error("UNKNOWN_EVENT_BLOCK", source, JsonAccess.child(path, "block"),
                            "Unknown event block '" + block + "'");
                } else {
                    report.warning("EVENT_BLOCK_UNAVAILABLE_WITH_MISSING_MOD", source,
                            JsonAccess.child(path, "block"),
                            "Event block is unavailable while required mods are missing: " + block);
                }
            }
            return new CompiledCondition.EventBlock(block);
        }
        if (CompiledCondition.EventBlockTag.TYPE.equals(type)) {
            rejectOutsideEvent(path);
            JsonAccess.knownKeys(object, Set.of("type", "tag"), source, path, report);
            return new CompiledCondition.EventBlockTag(JsonAccess.requiredId(
                    object, "tag", "minecraft:logs", true, source, path, report
            ));
        }

        report.error("UNKNOWN_CONDITION_TYPE", source, JsonAccess.child(path, "type"),
                "Unknown condition type '" + rawType + "'");
        return new CompiledCondition.Always(false);
    }

    private void rejectOutsideEvent(String path) {
        if (!allowEventContext) {
            report.error("EVENT_CONDITION_UNSUPPORTED", source, path,
                    "Event context conditions can only be used inside triggers");
        }
    }
}
