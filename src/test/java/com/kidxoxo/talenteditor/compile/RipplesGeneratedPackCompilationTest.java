package com.kidxoxo.talenteditor.compile;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RipplesGeneratedPackCompilationTest {
    private static final Pattern RESOURCE_LOCATION = Pattern.compile("[a-z0-9_.-]+:[a-z0-9_./-]+");

    @Test
    void generatedRipplesPackCompilesWithAllReferencedRegistriesPresent() throws IOException {
        String releaseData = System.getenv("TALENT_EDITOR_RELEASE_DATA");
        Path data = (releaseData == null || releaseData.isBlank()
                ? Path.of("..", "generated", "talent_editor_generated", "data")
                : Path.of(releaseData)).toAbsolutePath().normalize();
        Map<ResourceLocation, JsonObject> rarities = load(data, "rarities");
        Map<ResourceLocation, JsonObject> talents = load(data, "talents");
        Map<ResourceLocation, JsonObject> pools = load(data, "pools");
        Map<ResourceLocation, JsonObject> counters = load(data, "counters");
        Map<ResourceLocation, JsonObject> layouts = load(data, "layouts");
        Map<ResourceLocation, JsonObject> configuredItems = load(data, "items");

        LinkedHashSet<ResourceLocation> referencedIds = new LinkedHashSet<>();
        LinkedHashSet<String> loadedMods = new LinkedHashSet<>(Set.of("minecraft", "forge", "talent_editor"));
        for (Map<ResourceLocation, JsonObject> definitions :
                List.of(rarities, talents, pools, counters, layouts, configuredItems)) {
            definitions.forEach((id, json) -> {
                loadedMods.add(id.getNamespace());
                collectReferences(json, referencedIds, loadedMods);
            });
        }

        LinkedHashSet<ResourceLocation> registeredItems = new LinkedHashSet<>(referencedIds);
        registeredItems.removeAll(configuredItems.keySet());
        CompilationEnvironment environment = new CompilationEnvironment(
                loadedMods,
                referencedIds,
                registeredItems,
                referencedIds,
                referencedIds,
                referencedIds,
                referencedIds,
                referencedIds
        );
        CompilationResult result = TalentCompiler.withBuiltins().compile(
                new RawDefinitionSet(
                        rarities, talents, pools, counters, layouts, configuredItems,
                        Map.of(), Map.of(), Map.of(), Map.of(), Map.of(), Map.of(), List.of()
                ),
                environment
        );

        assertFalse(result.report().hasErrors(), result.report().issues().toString());
        if (releaseData != null && !releaseData.isBlank()) {
            assertEquals(77, talents.size());
            assertEquals(5, configuredItems.size());
            JsonObject memory = counters.get(new ResourceLocation("ripples_talents", "talent_036_memory"));
            assertEquals(1200, memory.get("intervalTicks").getAsInt());
            JsonObject translucent = talents.get(new ResourceLocation("ripples_talents", "talent_036"));
            JsonObject roll = translucent.getAsJsonArray("triggers").asList().stream()
                    .map(JsonElement::getAsJsonObject)
                    .filter(trigger -> trigger.get("id").getAsString().equals("a_transparency_roll"))
                    .findFirst().orElseThrow();
            assertEquals("0.05", roll.getAsJsonObject("condition").getAsJsonArray("children")
                    .asList().stream().map(JsonElement::getAsJsonObject)
                    .filter(condition -> condition.has("left")
                            && condition.get("left").getAsString().equals("random()"))
                    .findFirst().orElseThrow().get("right").getAsString());
            assertEquals("10", roll.getAsJsonArray("actions").get(0).getAsJsonObject()
                    .get("expression").getAsString());
            JsonObject progression = translucent.getAsJsonArray("triggers").asList().stream()
                    .map(JsonElement::getAsJsonObject)
                    .filter(trigger -> trigger.get("id").getAsString().equals("auto_synthesis_039"))
                    .findFirst().orElseThrow();
            assertTrue(progression.get("condition").toString().contains("transparency"));
            assertEquals("grant_talent", progression.getAsJsonArray("actions").get(0)
                    .getAsJsonObject().get("type").getAsString());
            assertEquals("0", progression.getAsJsonArray("actions").get(2)
                    .getAsJsonObject().get("expression").getAsString());
            JsonObject shinku = talents.get(new ResourceLocation("ripples_talents", "talent_039"));
            int[] ticks = {0, 100, 200, 340, 520};
            for (int index = 0; index < ticks.length; index++) {
                JsonObject cue = shinku.getAsJsonArray("onAcquire").get(index).getAsJsonObject();
                assertEquals("show_message", cue.get("type").getAsString());
                assertEquals(ticks[index], cue.get("atTick").getAsInt());
            }
            JsonObject friend = configuredItems.get(new ResourceLocation("ripples_talents", "gugu_friend"));
            assertEquals(3, friend.getAsJsonArray("attributeModifiers").size());
            friend.getAsJsonArray("attributeModifiers").forEach(modifier ->
                    assertEquals(3, modifier.getAsJsonObject().getAsJsonArray("slots").size()));
        }
    }

    private static Map<ResourceLocation, JsonObject> load(Path data, String kind) throws IOException {
        LinkedHashMap<ResourceLocation, JsonObject> result = new LinkedHashMap<>();
        if (!Files.isDirectory(data)) return result;
        try (var namespaces = Files.list(data)) {
            for (Path namespace : namespaces.filter(Files::isDirectory).toList()) {
                Path directory = namespace.resolve("talent_editor").resolve(kind);
                if (!Files.isDirectory(directory)) continue;
                try (var files = Files.list(directory)) {
                    for (Path file : files.filter(path -> path.getFileName().toString().endsWith(".json")).toList()) {
                        String name = file.getFileName().toString();
                        ResourceLocation id = new ResourceLocation(
                                namespace.getFileName().toString(), name.substring(0, name.length() - 5)
                        );
                        result.put(id, JsonParser.parseString(Files.readString(file)).getAsJsonObject());
                    }
                }
            }
        }
        return result;
    }

    private static void collectReferences(
            JsonElement element,
            Set<ResourceLocation> referencedIds,
            Set<String> loadedMods
    ) {
        if (element == null || element.isJsonNull()) return;
        if (element.isJsonArray()) {
            element.getAsJsonArray().forEach(child -> collectReferences(child, referencedIds, loadedMods));
            return;
        }
        if (element.isJsonObject()) {
            JsonObject object = element.getAsJsonObject();
            if (object.has("requiredMods") && object.get("requiredMods").isJsonArray()) {
                object.getAsJsonArray("requiredMods").forEach(mod -> loadedMods.add(mod.getAsString()));
            }
            object.entrySet().forEach(entry -> collectReferences(entry.getValue(), referencedIds, loadedMods));
            return;
        }
        if (!element.isJsonPrimitive() || !element.getAsJsonPrimitive().isString()) return;
        String value = element.getAsString();
        if (!RESOURCE_LOCATION.matcher(value).matches()) return;
        ResourceLocation id = ResourceLocation.tryParse(value);
        if (id != null) {
            referencedIds.add(id);
            loadedMods.add(id.getNamespace());
        }
    }
}
