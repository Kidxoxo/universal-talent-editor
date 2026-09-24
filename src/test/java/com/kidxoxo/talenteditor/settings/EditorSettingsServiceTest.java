package com.kidxoxo.talenteditor.settings;

import com.kidxoxo.talenteditor.editor.EditorPackStorage;
import com.kidxoxo.talenteditor.compile.RawDefinitionSet;
import com.google.gson.JsonParser;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.nio.charset.StandardCharsets;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EditorSettingsServiceTest {
    @TempDir
    Path temporaryDirectory;

    @Test
    void configuredItemsCarryTheirCuriosTagInTheModResources() throws Exception {
        try (InputStream input = getClass().getResourceAsStream("/data/curios/tags/items/curio.json")) {
            assertNotNull(input);
            var tag = JsonParser.parseString(new String(input.readAllBytes(), StandardCharsets.UTF_8))
                    .getAsJsonObject();
            assertFalse(tag.get("replace").getAsBoolean());
            assertEquals("talent_editor:configured_item",
                    tag.getAsJsonArray("values").get(0).getAsString());
        }
    }

    @Test
    void settingsDefaultToAutomaticAndRoundTripManualMode() throws Exception {
        Path pack = temporaryDirectory.resolve(EditorPackStorage.PACK_FOLDER);
        Files.createDirectories(pack);

        EditorRuntimeSettings defaults = EditorSettingsService.readSettings(pack);
        assertTrue(defaults.automaticSynthesis());
        assertEquals(EditorRuntimeSettings.SynthesisSoundAudience.PERSONAL,
                defaults.synthesisSoundAudience());
        assertEquals(EditorRuntimeSettings.SynthesisMessageAudience.PERSONAL,
                defaults.synthesisMessageAudience());
        assertEquals(EditorRuntimeSettings.SoundOverlapPolicy.IMMEDIATE, defaults.soundOverlapPolicy());
        EditorSettingsService.writeSettings(pack, new EditorRuntimeSettings(
                false,
                EditorRuntimeSettings.SynthesisSoundAudience.GLOBAL,
                EditorRuntimeSettings.SynthesisMessageAudience.GLOBAL,
                EditorRuntimeSettings.SoundOverlapPolicy.QUEUE
        ));

        EditorRuntimeSettings loaded = EditorSettingsService.readSettings(pack);
        assertFalse(loaded.automaticSynthesis());
        assertEquals(EditorRuntimeSettings.SynthesisSoundAudience.GLOBAL, loaded.synthesisSoundAudience());
        assertEquals(EditorRuntimeSettings.SynthesisMessageAudience.GLOBAL, loaded.synthesisMessageAudience());
        assertEquals(EditorRuntimeSettings.SoundOverlapPolicy.QUEUE, loaded.soundOverlapPolicy());
    }

    @Test
    void globalTemplateOnlySeedsWorldWithoutAnExistingEditorPack() throws Exception {
        Path template = temporaryDirectory.resolve("template").resolve(EditorPackStorage.PACK_FOLDER);
        Files.createDirectories(template.resolve("data/example/talent_editor/talents"));
        Files.writeString(template.resolve("pack.mcmeta"), "{}");
        Files.writeString(template.resolve("data/example/talent_editor/talents/a.json"), "{}");
        Path worldPack = temporaryDirectory.resolve("world/datapacks")
                .resolve(EditorPackStorage.PACK_FOLDER);

        assertTrue(EditorSettingsService.installTemplateIfWorldHasNoEditorPack(template, worldPack));
        assertTrue(Files.isRegularFile(worldPack.resolve(
                "data/example/talent_editor/talents/a.json"
        )));
        Files.writeString(worldPack.resolve("marker.txt"), "world-owned");
        assertFalse(EditorSettingsService.installTemplateIfWorldHasNoEditorPack(template, worldPack));
        assertEquals("world-owned", Files.readString(worldPack.resolve("marker.txt")));
    }

    @Test
    void resolvedPackContainsActiveDefinitionsFromAnySource() throws Exception {
        Path target = temporaryDirectory.resolve("resolved");
        ResourceLocation rarity = new ResourceLocation("external", "nested/rare");
        RawDefinitionSet definitions = new RawDefinitionSet(
                Map.of(rarity, JsonParser.parseString("{\"schema\":2,\"name\":\"Rare\"}")
                        .getAsJsonObject()),
                Map.of(), List.of()
        );

        EditorSettingsService.writeResolvedPack(target, definitions, new EditorRuntimeSettings(false));

        assertTrue(Files.isRegularFile(target.resolve(
                "data/external/talent_editor/rarities/nested/rare.json"
        )));
        assertFalse(EditorSettingsService.readSettings(target).automaticSynthesis());
    }

    @Test
    void exportedArchiveExtractionIsScopedAndRejectsTraversal() throws Exception {
        Path valid = temporaryDirectory.resolve("valid.zip");
        try (ZipOutputStream zip = new ZipOutputStream(Files.newOutputStream(valid), StandardCharsets.UTF_8)) {
            put(zip, "datapacks/talent_editor_generated/pack.mcmeta",
                    "{\"pack\":{\"pack_format\":15,\"description\":\"test\"}}");
            put(zip, "datapacks/talent_editor_generated/talent_editor.settings.json",
                    "{\"schema\":1,\"automaticSynthesis\":true}");
            put(zip, "datapacks/talent_editor_generated/data/example/talent_editor/talents/a.json",
                    "{\"schema\":2,\"name\":\"A\",\"rarity\":\"talent_editor:common\"}");
        }
        Path staging = temporaryDirectory.resolve("staging");
        Files.createDirectories(staging);
        EditorSettingsService.extractImportArchive(valid, staging);
        assertTrue(Files.isRegularFile(staging.resolve(
                "datapacks/talent_editor_generated/data/example/talent_editor/talents/a.json"
        )));
        assertTrue(EditorSettingsService.readImportedDefinitions(
                staging.resolve("datapacks/talent_editor_generated")
        ).talents().containsKey(new ResourceLocation("example", "a")));

        Path traversal = temporaryDirectory.resolve("traversal.zip");
        try (ZipOutputStream zip = new ZipOutputStream(Files.newOutputStream(traversal), StandardCharsets.UTF_8)) {
            put(zip, "datapacks/talent_editor_generated/../escaped.json", "{}");
        }
        Path rejected = temporaryDirectory.resolve("rejected");
        Files.createDirectories(rejected);
        assertThrows(java.io.IOException.class,
                () -> EditorSettingsService.extractImportArchive(traversal, rejected));
        assertFalse(Files.exists(temporaryDirectory.resolve("escaped.json")));
    }

    private static void put(ZipOutputStream zip, String name, String text) throws Exception {
        zip.putNextEntry(new ZipEntry(name));
        zip.write(text.getBytes(StandardCharsets.UTF_8));
        zip.closeEntry();
    }
}
