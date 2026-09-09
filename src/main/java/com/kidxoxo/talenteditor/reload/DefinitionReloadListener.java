package com.kidxoxo.talenteditor.reload;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.kidxoxo.talenteditor.TalentEditorMod;
import com.kidxoxo.talenteditor.compile.RawDefinitionSet;
import com.kidxoxo.talenteditor.editor.DefinitionService;
import com.kidxoxo.talenteditor.forge.ForgeCompilationEnvironment;
import com.kidxoxo.talenteditor.runtime.ActivationOutcome;
import com.kidxoxo.talenteditor.validation.ValidationIssue;
import com.kidxoxo.talenteditor.validation.ValidationSeverity;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimplePreparableReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;

import java.io.Reader;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;

public final class DefinitionReloadListener extends SimplePreparableReloadListener<RawDefinitionSet> {
    private static final String ROOT = TalentEditorMod.MOD_ID;
    private final DefinitionService definitions;

    public DefinitionReloadListener(DefinitionService definitions) {
        this.definitions = definitions;
    }

    @Override
    protected RawDefinitionSet prepare(ResourceManager resourceManager, ProfilerFiller profiler) {
        LinkedHashMap<ResourceLocation, com.google.gson.JsonObject> rarities = new LinkedHashMap<>();
        LinkedHashMap<ResourceLocation, com.google.gson.JsonObject> talents = new LinkedHashMap<>();
        LinkedHashMap<ResourceLocation, com.google.gson.JsonObject> pools = new LinkedHashMap<>();
        LinkedHashMap<ResourceLocation, com.google.gson.JsonObject> counters = new LinkedHashMap<>();
        LinkedHashMap<ResourceLocation, com.google.gson.JsonObject> layouts = new LinkedHashMap<>();
        LinkedHashMap<ResourceLocation, com.google.gson.JsonObject> configuredItems = new LinkedHashMap<>();
        LinkedHashMap<ResourceLocation, String> raritySources = new LinkedHashMap<>();
        LinkedHashMap<ResourceLocation, String> talentSources = new LinkedHashMap<>();
        LinkedHashMap<ResourceLocation, String> poolSources = new LinkedHashMap<>();
        LinkedHashMap<ResourceLocation, String> counterSources = new LinkedHashMap<>();
        LinkedHashMap<ResourceLocation, String> layoutSources = new LinkedHashMap<>();
        LinkedHashMap<ResourceLocation, String> configuredItemSources = new LinkedHashMap<>();
        ArrayList<ValidationIssue> issues = new ArrayList<>();
        loadDirectory(resourceManager, ROOT + "/rarities", rarities, raritySources, issues);
        loadDirectory(resourceManager, ROOT + "/talents", talents, talentSources, issues);
        loadDirectory(resourceManager, ROOT + "/pools", pools, poolSources, issues);
        loadDirectory(resourceManager, ROOT + "/counters", counters, counterSources, issues);
        loadDirectory(resourceManager, ROOT + "/layouts", layouts, layoutSources, issues);
        loadDirectory(resourceManager, ROOT + "/items", configuredItems, configuredItemSources, issues);
        return new RawDefinitionSet(
                rarities, talents, pools, counters, layouts, configuredItems,
                raritySources, talentSources, poolSources, counterSources, layoutSources, configuredItemSources,
                issues
        );
    }

    @Override
    protected void apply(RawDefinitionSet definitions, ResourceManager resourceManager, ProfilerFiller profiler) {
        ActivationOutcome outcome = this.definitions.acceptReload(definitions, ForgeCompilationEnvironment.capture());
        TalentEditorMod.SNAPSHOTS.lastReport().issues().forEach(issue -> {
            String message = "{} {} {}: {}";
            if (issue.severity() == ValidationSeverity.ERROR) {
                TalentEditorMod.LOGGER.error(message, issue.code(), issue.source(), issue.path(), issue.message());
            } else if (issue.severity() == ValidationSeverity.WARNING) {
                TalentEditorMod.LOGGER.warn(message, issue.code(), issue.source(), issue.path(), issue.message());
            } else {
                TalentEditorMod.LOGGER.info(message, issue.code(), issue.source(), issue.path(), issue.message());
            }
        });
        if (outcome.accepted()) {
            TalentEditorMod.LOGGER.info(
                    "Talent definitions accepted: revision={}, changed={}, rarities={}, pools={}, talents={}, counters={}, layouts={}, items={}, hash={}",
                    outcome.activeRevision(), outcome.changed(), TalentEditorMod.SNAPSHOTS.active().rarities().size(),
                    TalentEditorMod.SNAPSHOTS.active().pools().size(),
                    TalentEditorMod.SNAPSHOTS.active().talents().size(),
                    TalentEditorMod.SNAPSHOTS.active().counters().size(),
                    TalentEditorMod.SNAPSHOTS.active().layouts().size(),
                    TalentEditorMod.SNAPSHOTS.active().configuredItems().size(), shortHash(outcome.activeHash())
            );
        } else {
            TalentEditorMod.LOGGER.error(
                    "Talent definition reload rejected; keeping revision {} ({} error(s))",
                    outcome.activeRevision(), TalentEditorMod.SNAPSHOTS.lastReport().errorCount()
            );
        }
    }

    private static void loadDirectory(
            ResourceManager resourceManager,
            String directory,
            Map<ResourceLocation, com.google.gson.JsonObject> output,
            Map<ResourceLocation, String> sources,
            ArrayList<ValidationIssue> issues
    ) {
        Map<ResourceLocation, Resource> resources = resourceManager.listResources(
                directory,
                id -> id.getPath().endsWith(".json")
        );
        String prefix = directory + "/";
        resources.forEach((resourceId, resource) -> {
            String path = resourceId.getPath();
            if (!path.startsWith(prefix) || !path.endsWith(".json")) return;
            String relative = path.substring(prefix.length(), path.length() - ".json".length());
            ResourceLocation definitionId = ResourceLocation.tryBuild(resourceId.getNamespace(), relative);
            if (definitionId == null) {
                issues.add(new ValidationIssue(
                        ValidationSeverity.ERROR,
                        "INVALID_DEFINITION_PATH",
                        resourceId.toString(),
                        "$",
                        "File path cannot be converted to a definition id"
                ));
                return;
            }
            try (Reader reader = resource.openAsReader()) {
                JsonElement parsed = JsonParser.parseReader(reader);
                if (!parsed.isJsonObject()) {
                    issues.add(new ValidationIssue(
                            ValidationSeverity.ERROR,
                            "EXPECTED_ROOT_OBJECT",
                            resourceId.toString(),
                            "$",
                            "Definition root must be a JSON object"
                    ));
                    return;
                }
                output.put(definitionId, parsed.getAsJsonObject());
                sources.put(definitionId, resource.sourcePackId());
            } catch (Exception ex) {
                issues.add(new ValidationIssue(
                        ValidationSeverity.ERROR,
                        "JSON_READ_FAILED",
                        resourceId.toString(),
                        "$",
                        ex.getMessage() == null ? ex.getClass().getSimpleName() : ex.getMessage()
                ));
            }
        });
    }

    private static String shortHash(String hash) {
        return hash.length() <= 12 ? hash : hash.substring(0, 12);
    }
}
