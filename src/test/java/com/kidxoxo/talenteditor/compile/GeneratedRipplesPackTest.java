package com.kidxoxo.talenteditor.compile;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Assumptions;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Optional workspace integration check; the distributable source tree need not contain the generated pack. */
class GeneratedRipplesPackTest {
    @Test
    void generatedPackCompilesWithTheCurrentSchema() throws Exception {
        Path dataRoot = Path.of("..", "generated", "talent_editor_generated", "data").toAbsolutePath().normalize();
        Assumptions.assumeTrue(Files.isDirectory(dataRoot));
        RawDefinitionSet definitions = RawDefinitionSet.empty();
        Set<ResourceLocation> attributes = new HashSet<>();
        Set<ResourceLocation> items = new HashSet<>();
        Set<ResourceLocation> entities = new HashSet<>();
        Set<ResourceLocation> effects = new HashSet<>();
        Set<ResourceLocation> blocks = new HashSet<>();
        Set<ResourceLocation> sounds = new HashSet<>();
        Set<String> mods = new HashSet<>(Set.of("minecraft", "forge", "talent_editor", "ripples_talents"));
        try (Stream<Path> files = Files.walk(dataRoot)) {
            for (Path file : files.filter(path -> path.toString().endsWith(".json")).sorted().toList()) {
                Path relative = dataRoot.relativize(file);
                if (relative.getNameCount() < 4 || !relative.getName(1).toString().equals("talent_editor")) continue;
                String namespace = relative.getName(0).toString();
                String kind = relative.getName(2).toString();
                String valuePath = relative.subpath(3, relative.getNameCount()).toString()
                        .replace('\\', '/').replaceFirst("\\.json$", "");
                ResourceLocation id = new ResourceLocation(namespace, valuePath);
                JsonObject json = JsonParser.parseString(Files.readString(file)).getAsJsonObject();
                collectRegistries(json, "", attributes, items, entities, effects, blocks, sounds, mods);
                String source = file.toString();
                definitions = switch (kind) {
                    case "talents" -> definitions.withTalent(id, json, source);
                    case "rarities" -> definitions.withRarity(id, json, source);
                    case "pools" -> definitions.withPool(id, json, source);
                    case "counters" -> definitions.withCounter(id, json, source);
                    case "layouts" -> definitions.withLayout(id, json, source);
                    case "items" -> definitions.withConfiguredItem(id, json, source);
                    default -> definitions;
                };
            }
        }
        // Configured items are logical data-pack IDs, not pre-registered Forge items.
        items.removeAll(definitions.configuredItems().keySet());
        CompilationResult result = TalentCompiler.withBuiltins().compile(definitions,
                new CompilationEnvironment(mods, attributes, items, entities, effects, blocks, sounds));
        assertFalse(result.report().hasErrors(), () -> result.report().issues().toString());
        assertTrue(result.candidate().orElseThrow().talents().size() >= 75);
    }

    private static void collectRegistries(JsonElement element, String key,
                                          Set<ResourceLocation> attributes, Set<ResourceLocation> items,
                                          Set<ResourceLocation> entities, Set<ResourceLocation> effects,
                                          Set<ResourceLocation> blocks, Set<ResourceLocation> sounds,
                                          Set<String> mods) {
        if (element.isJsonObject()) {
            element.getAsJsonObject().entrySet().forEach(entry -> collectRegistries(entry.getValue(),
                    entry.getKey(), attributes, items, entities, effects, blocks, sounds, mods));
        } else if (element.isJsonArray()) {
            element.getAsJsonArray().forEach(child -> collectRegistries(child, key,
                    attributes, items, entities, effects, blocks, sounds, mods));
        } else if (element.isJsonPrimitive() && element.getAsJsonPrimitive().isString()) {
            String raw = element.getAsString();
            if (key.equals("requiredMods")) mods.add(raw);
            ResourceLocation id = ResourceLocation.tryParse(raw);
            if (id == null || !raw.contains(":")) return;
            mods.add(id.getNamespace());
            if (key.toLowerCase(java.util.Locale.ROOT).contains("attribute")) attributes.add(id);
            if (key.equals("item") || key.equals("icon")) items.add(id);
            if (key.equals("entityType")) entities.add(id);
            if (key.equals("effect")) effects.add(id);
            if (key.equals("block")) blocks.add(id);
            if (key.equals("sound")) sounds.add(id);
        }
    }
}
