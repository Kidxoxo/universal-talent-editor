package com.kidxoxo.talenteditor.editor;

import com.google.gson.JsonObject;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EditorPackStorageTest {
    @TempDir
    Path temporaryDirectory;

    @Test
    void writesStandardWorldDataPackAndBacksUpReplacement() throws Exception {
        EditorPackStorage storage = new EditorPackStorage();
        ResourceLocation id = new ResourceLocation("example", "starter/vitality");
        JsonObject first = new JsonObject();
        first.addProperty("schema", 2);

        Path target = storage.writeTalent(temporaryDirectory, id, first);
        assertTrue(Files.exists(temporaryDirectory.resolve("talent_editor_generated/pack.mcmeta")));
        assertEquals(
                temporaryDirectory.resolve(
                        "talent_editor_generated/data/example/talent_editor/talents/starter/vitality.json"
                ).toAbsolutePath().normalize(),
                target
        );
        assertTrue(Files.readString(target).contains("\"schema\": 2"));

        JsonObject second = new JsonObject();
        second.addProperty("schema", 3);
        storage.writeTalent(temporaryDirectory, id, second);

        Path backupDirectory = temporaryDirectory.resolve(
                "talent_editor_generated/.talent_editor_backups/example/talents/starter/vitality"
        );
        try (var files = Files.list(backupDirectory)) {
            assertEquals(1L, files.count());
        }
        assertTrue(Files.readString(target).contains("\"schema\": 3"));
    }

    @Test
    void writesPoolInItsOwnDataDirectory() throws Exception {
        EditorPackStorage storage = new EditorPackStorage();
        ResourceLocation id = new ResourceLocation("example", "main");
        JsonObject pool = new JsonObject();
        pool.addProperty("schema", 2);

        Path target = storage.writePool(temporaryDirectory, id, pool);

        assertEquals(
                temporaryDirectory.resolve(
                        "talent_editor_generated/data/example/talent_editor/pools/main.json"
                ).toAbsolutePath().normalize(),
                target
        );
        assertTrue(Files.readString(target).contains("\"schema\": 2"));
    }

    @Test
    void writesRarityInItsOwnDataDirectory() throws Exception {
        EditorPackStorage storage = new EditorPackStorage();
        ResourceLocation id = new ResourceLocation("example", "epic");
        JsonObject rarity = new JsonObject();
        rarity.addProperty("schema", 2);

        Path target = storage.writeRarity(temporaryDirectory, id, rarity);

        assertEquals(
                temporaryDirectory.resolve(
                        "talent_editor_generated/data/example/talent_editor/rarities/epic.json"
                ).toAbsolutePath().normalize(),
                target
        );
        assertTrue(Files.readString(target).contains("\"schema\": 2"));
    }

    @Test
    void recordsCreateAndUpdateThenSupportsHashGuardedMultiLevelUndo() throws Exception {
        EditorPackStorage storage = new EditorPackStorage();
        ResourceLocation id = new ResourceLocation("example", "history");
        JsonObject first = new JsonObject();
        first.addProperty("schema", 2);
        first.addProperty("value", "first");
        Path target = storage.writeTalent(temporaryDirectory, id, first);
        JsonObject second = new JsonObject();
        second.addProperty("schema", 2);
        second.addProperty("value", "second");
        storage.writeTalent(temporaryDirectory, id, second);

        var history = storage.history(temporaryDirectory);
        assertEquals(2, history.size());
        assertEquals(EditorTransactionOperation.UPDATE, history.get(0).operation());
        assertTrue(history.get(0).canUndo());
        assertEquals(EditorTransactionOperation.CREATE, history.get(1).operation());
        assertFalse(history.get(1).canUndo());

        EditorPackStorage.UndoPlan plan = storage.prepareUndo(
                temporaryDirectory, history.get(0).transactionId()
        ).plan().orElseThrow();
        EditorPackStorage.UndoFileChange change = storage.applyUndo(plan);
        assertTrue(Files.readString(target).contains("first"));
        assertTrue(storage.rollbackUndo(change));
        assertTrue(Files.readString(target).contains("second"));

        change = storage.applyUndo(plan);
        storage.completeUndo(plan);
        assertTrue(Files.readString(target).contains("first"));
        history = storage.history(temporaryDirectory);
        assertEquals(EditorTransactionState.UNDONE, history.get(0).state());
        assertTrue(history.get(1).canUndo());
    }

    @Test
    void refusesUndoWhenTheGeneratedFileWasChangedOutsideTheJournal() throws Exception {
        EditorPackStorage storage = new EditorPackStorage();
        ResourceLocation id = new ResourceLocation("example", "changed");
        JsonObject definition = new JsonObject();
        definition.addProperty("schema", 2);
        Path target = storage.writePool(temporaryDirectory, id, definition);
        UUID transactionId = storage.history(temporaryDirectory).get(0).transactionId();

        Files.writeString(target, "{\"schema\":3}\n");

        assertFalse(storage.history(temporaryDirectory).get(0).canUndo());
        assertEquals(
                DefinitionUndoStatus.STALE_TARGET,
                storage.prepareUndo(temporaryDirectory, transactionId).status()
        );
    }

    @Test
    void keepsTransactionHistoryBounded() throws Exception {
        EditorPackStorage storage = new EditorPackStorage();
        for (int index = 0; index < EditorPackStorage.MAX_HISTORY_ENTRIES + 5; index++) {
            JsonObject definition = new JsonObject();
            definition.addProperty("schema", 2);
            definition.addProperty("index", index);
            storage.writePool(
                    temporaryDirectory,
                    new ResourceLocation("example", "pool_" + index),
                    definition
            );
        }

        assertEquals(EditorPackStorage.MAX_HISTORY_ENTRIES, storage.history(temporaryDirectory).size());
    }
}
