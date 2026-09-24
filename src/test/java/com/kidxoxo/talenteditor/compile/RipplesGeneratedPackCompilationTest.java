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

class RipplesGeneratedPackCompilationTest {
    private static final Pattern RESOURCE_LOCATION = Pattern.compile("[a-z0-9_.-]+:[a-z0-9_./-]+");

    @Test
    void generatedRipplesPackCompilesWithAllReferencedRegistriesPresent() throws IOException {
        Path data = Path.of("..", "generated", "talent_editor_generated", "data")
                .toAbsolutePath().normalize();
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
