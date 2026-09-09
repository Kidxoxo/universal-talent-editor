package com.kidxoxo.talenteditor.editor;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.kidxoxo.talenteditor.compile.CompilationEnvironment;
import com.kidxoxo.talenteditor.compile.RawDefinitionSet;
import com.kidxoxo.talenteditor.compile.TalentCompiler;
import com.kidxoxo.talenteditor.domain.DefinitionKind;
import com.kidxoxo.talenteditor.runtime.SnapshotRepository;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DefinitionServiceTest {
    private static final ResourceLocation COMMON = new ResourceLocation("talent_editor", "common");
    private static final ResourceLocation HEALTH = new ResourceLocation("minecraft", "generic.max_health");
    private static final ResourceLocation TALENT = new ResourceLocation("example", "vitality");

    @TempDir
    Path temporaryDirectory;

    @Test
    void validatesWithoutWritingThenCommitsAgainstMatchingRevision() {
        Fixture fixture = fixture();
        long baseRevision = fixture.snapshots.active().revision();

        EditorSubmissionResult validation = fixture.service.submitTalent(
                baseRevision, TALENT, validTalent(), false, fixture.environment, temporaryDirectory
        );
        assertTrue(validation.accepted());
        assertFalse(validation.committed());
        assertEquals(baseRevision, fixture.snapshots.active().revision());
        assertFalse(Files.exists(temporaryDirectory.resolve("talent_editor_generated")));

        EditorSubmissionResult committed = fixture.service.submitTalent(
                baseRevision, TALENT, validTalent(), true, fixture.environment, temporaryDirectory
        );
        assertTrue(committed.accepted());
        assertTrue(committed.committed());
        assertEquals(baseRevision + 1, committed.activeRevision());
        assertTrue(Files.exists(temporaryDirectory.resolve(
                "talent_editor_generated/data/example/talent_editor/talents/vitality.json"
        )));
    }

    @Test
    void rejectsStaleRevisionBeforeWriting() {
        Fixture fixture = fixture();

        EditorSubmissionResult result = fixture.service.submitTalent(
                fixture.snapshots.active().revision() - 1,
                TALENT,
                validTalent(),
                true,
                fixture.environment,
                temporaryDirectory
        );

        assertFalse(result.accepted());
        assertTrue(result.conflict());
        assertTrue(result.report().issues().stream().anyMatch(issue -> issue.code().equals("REVISION_CONFLICT")));
        assertFalse(Files.exists(temporaryDirectory.resolve("talent_editor_generated")));
    }

    @Test
    void validatesAndCommitsPoolThroughTheSameCompiledSnapshot() {
        Fixture fixture = fixture();
        long baseRevision = fixture.snapshots.active().revision();
        ResourceLocation poolId = new ResourceLocation("example", "main");
        String pool = """
                {
                  "schema": 2,
                  "name": "Main Pool",
                  "choices": 4,
                  "refreshes": 2,
                  "maxOwned": 30,
                  "excludeOwned": true,
                  "rarityWeights": {"talent_editor:common": 2.5}
                }
                """;

        EditorSubmissionResult validation = fixture.service.submitPool(
                baseRevision, poolId, pool, false, fixture.environment, temporaryDirectory
        );
        assertTrue(validation.accepted());
        assertFalse(validation.committed());

        EditorSubmissionResult committed = fixture.service.submitPool(
                baseRevision, poolId, pool, true, fixture.environment, temporaryDirectory
        );
        assertTrue(committed.committed());
        assertEquals(4, fixture.snapshots.active().pools().get(poolId).choices());
        assertTrue(Files.exists(temporaryDirectory.resolve(
                "talent_editor_generated/data/example/talent_editor/pools/main.json"
        )));
    }

    @Test
    void validatesAndCommitsRarityThroughTheSameCompiledSnapshot() {
        Fixture fixture = fixture();
        long baseRevision = fixture.snapshots.active().revision();
        ResourceLocation rarityId = new ResourceLocation("example", "epic");
        String rarity = """
                {
                  "schema": 2,
                  "name": "Epic",
                  "color": "#AA55FF",
                  "order": 20,
                  "offerable": true,
                  "baseWeight": 5
                }
                """;

        EditorSubmissionResult validation = fixture.service.submitRarity(
                baseRevision, rarityId, rarity, false, fixture.environment, temporaryDirectory
        );
        assertTrue(validation.accepted());
        assertFalse(validation.committed());

        EditorSubmissionResult committed = fixture.service.submitRarity(
                baseRevision, rarityId, rarity, true, fixture.environment, temporaryDirectory
        );
        assertTrue(committed.committed());
        assertEquals(20, fixture.snapshots.active().rarities().get(rarityId).order());
        assertTrue(Files.exists(temporaryDirectory.resolve(
                "talent_editor_generated/data/example/talent_editor/rarities/epic.json"
        )));
    }

    @Test
    void commitsCounterAndPreservesItWhenAnotherDefinitionChanges() {
        Fixture fixture = fixture();
        ResourceLocation counterId = new ResourceLocation("example", "play_minutes");
        String counter = """
                {
                  "schema": 2,
                  "name": "Play minutes",
                  "event": "play_time",
                  "increment": 1,
                  "maximum": 10000,
                  "intervalTicks": 1200
                }
                """;
        EditorSubmissionResult validation = fixture.service.submitCounter(
                fixture.snapshots.active().revision(), counterId, counter, false,
                fixture.environment, temporaryDirectory
        );
        assertTrue(validation.accepted());
        assertFalse(validation.committed());

        EditorSubmissionResult committed = fixture.service.submitCounter(
                fixture.snapshots.active().revision(), counterId, counter, true,
                fixture.environment, temporaryDirectory
        );
        assertTrue(committed.committed());
        assertEquals(1200, fixture.snapshots.active().counters().get(counterId).intervalTicks());
        assertTrue(Files.exists(temporaryDirectory.resolve(
                "talent_editor_generated/data/example/talent_editor/counters/play_minutes.json"
        )));

        EditorSubmissionResult talentCommitted = fixture.service.submitTalent(
                fixture.snapshots.active().revision(), TALENT, validTalent(), true,
                fixture.environment, temporaryDirectory
        );
        assertTrue(talentCommitted.committed());
        assertTrue(fixture.snapshots.active().counters().containsKey(counterId));
    }

    @Test
    void deletesGeneratedUnreferencedDefinitionIntoRecoveryAndCanRollback() {
        Fixture fixture = fixture();
        EditorSubmissionResult committed = fixture.service.submitTalent(
                fixture.snapshots.active().revision(), TALENT, validTalent(), true,
                fixture.environment, temporaryDirectory
        );
        assertTrue(committed.committed());
        Path source = temporaryDirectory.resolve(
                "talent_editor_generated/data/example/talent_editor/talents/vitality.json"
        );

        DefinitionDeletionResult deleted = fixture.service.deleteDefinition(
                fixture.snapshots.active().revision(), DefinitionKind.TALENT, TALENT,
                fixture.environment, temporaryDirectory
        );

        assertEquals(DefinitionDeletionStatus.DELETED, deleted.status());
        assertFalse(Files.exists(source));
        assertTrue(Files.exists(deleted.recovered().orElseThrow().recovered()));
        assertFalse(fixture.snapshots.active().talents().containsKey(TALENT));

        assertTrue(fixture.service.rollbackDeletion(deleted, fixture.environment));
        assertTrue(Files.exists(source));
        assertTrue(fixture.snapshots.active().talents().containsKey(TALENT));
        assertFalse(Files.exists(deleted.recovered().orElseThrow().recovered()));
    }

    @Test
    void deletionRollbackPreservesDefinitionsCommittedAfterTheDeletion() {
        Fixture fixture = fixture();
        assertTrue(fixture.service.submitTalent(
                fixture.snapshots.active().revision(), TALENT, validTalent(), true,
                fixture.environment, temporaryDirectory
        ).committed());
        DefinitionDeletionResult deleted = fixture.service.deleteDefinition(
                fixture.snapshots.active().revision(), DefinitionKind.TALENT, TALENT,
                fixture.environment, temporaryDirectory
        );
        assertEquals(DefinitionDeletionStatus.DELETED, deleted.status());

        ResourceLocation later = new ResourceLocation("example", "later");
        assertTrue(fixture.service.submitTalent(
                fixture.snapshots.active().revision(), later, validTalent(), true,
                fixture.environment, temporaryDirectory
        ).committed());

        assertTrue(fixture.service.rollbackDeletion(deleted, fixture.environment));
        assertTrue(fixture.snapshots.active().talents().containsKey(TALENT));
        assertTrue(fixture.snapshots.active().talents().containsKey(later));
        assertTrue(Files.exists(temporaryDirectory.resolve(
                "talent_editor_generated/data/example/talent_editor/talents/later.json"
        )));
    }

    @Test
    void rejectsDeletionWhenAnotherDefinitionReferencesTheTarget() {
        Fixture fixture = fixture();
        assertTrue(fixture.service.submitTalent(
                fixture.snapshots.active().revision(), TALENT, validTalent(), true,
                fixture.environment, temporaryDirectory
        ).committed());
        ResourceLocation dependentId = new ResourceLocation("example", "dependent");
        JsonObject dependent = JsonParser.parseString(validTalent()).getAsJsonObject();
        dependent.add("acquireCondition", JsonParser.parseString("""
                {"type":"has_talent","talent":"example:vitality"}
                """));
        assertTrue(fixture.service.submitTalent(
                fixture.snapshots.active().revision(), dependentId, dependent.toString(), true,
                fixture.environment, temporaryDirectory
        ).committed());

        DefinitionDeletionResult result = fixture.service.deleteDefinition(
                fixture.snapshots.active().revision(), DefinitionKind.TALENT, TALENT,
                fixture.environment, temporaryDirectory
        );

        assertEquals(DefinitionDeletionStatus.BLOCKED_REFERENCES, result.status());
        assertTrue(result.blockingReferences().stream().anyMatch(reference ->
                reference.sourceId().equals(dependentId) && reference.usage().equals("condition_talent")
        ));
        assertTrue(Files.exists(temporaryDirectory.resolve(
                "talent_editor_generated/data/example/talent_editor/talents/vitality.json"
        )));
        assertTrue(fixture.snapshots.active().talents().containsKey(TALENT));
    }

    @Test
    void rejectsExternalDefinitionsAndStaleDeletionRequests() {
        Fixture fixture = fixture();

        DefinitionDeletionResult external = fixture.service.deleteDefinition(
                fixture.snapshots.active().revision(), DefinitionKind.RARITY, COMMON,
                fixture.environment, temporaryDirectory
        );
        assertEquals(DefinitionDeletionStatus.NOT_GENERATED, external.status());

        DefinitionDeletionResult stale = fixture.service.deleteDefinition(
                fixture.snapshots.active().revision() - 1, DefinitionKind.TALENT, TALENT,
                fixture.environment, temporaryDirectory
        );
        assertEquals(DefinitionDeletionStatus.REVISION_CONFLICT, stale.status());
    }

    @Test
    void undoesARecordedCreationAndMarksItsTransactionConsumed() throws Exception {
        Fixture fixture = fixture();
        assertTrue(fixture.service.submitTalent(
                fixture.snapshots.active().revision(), TALENT, validTalent(), true,
                fixture.environment, temporaryDirectory
        ).committed());
        EditorPackStorage storage = new EditorPackStorage();
        EditorTransactionSummary transaction = storage.history(temporaryDirectory).get(0);

        DefinitionUndoResult result = fixture.service.undoTransaction(
                fixture.snapshots.active().revision(), transaction.transactionId(),
                fixture.environment, temporaryDirectory
        );

        assertEquals(DefinitionUndoStatus.UNDONE, result.status());
        assertFalse(fixture.snapshots.active().talents().containsKey(TALENT));
        assertFalse(Files.exists(temporaryDirectory.resolve(
                "talent_editor_generated/data/example/talent_editor/talents/vitality.json"
        )));
        assertTrue(fixture.service.completeUndo(result));
        assertEquals(
                EditorTransactionState.UNDONE,
                storage.history(temporaryDirectory).get(0).state()
        );
    }

    @Test
    void refusesToUndoCreationWhenAnotherDefinitionReferencesIt() throws Exception {
        Fixture fixture = fixture();
        assertTrue(fixture.service.submitTalent(
                fixture.snapshots.active().revision(), TALENT, validTalent(), true,
                fixture.environment, temporaryDirectory
        ).committed());
        ResourceLocation dependentId = new ResourceLocation("example", "undo_dependent");
        JsonObject dependent = JsonParser.parseString(validTalent()).getAsJsonObject();
        dependent.add("acquireCondition", JsonParser.parseString(
                "{\"type\":\"has_talent\",\"talent\":\"example:vitality\"}"
        ));
        assertTrue(fixture.service.submitTalent(
                fixture.snapshots.active().revision(), dependentId, dependent.toString(), true,
                fixture.environment, temporaryDirectory
        ).committed());
        EditorTransactionSummary creation = new EditorPackStorage().history(temporaryDirectory).stream()
                .filter(value -> value.definitionId().equals(TALENT))
                .findFirst().orElseThrow();

        DefinitionUndoResult result = fixture.service.undoTransaction(
                fixture.snapshots.active().revision(), creation.transactionId(),
                fixture.environment, temporaryDirectory
        );

        assertEquals(DefinitionUndoStatus.BLOCKED_REFERENCES, result.status());
        assertTrue(fixture.snapshots.active().talents().containsKey(TALENT));
    }

    @Test
    void undoingAnUpdateRestoresThePriorDefinition() throws Exception {
        Fixture fixture = fixture();
        assertTrue(fixture.service.submitTalent(
                fixture.snapshots.active().revision(), TALENT, validTalent(), true,
                fixture.environment, temporaryDirectory
        ).committed());
        JsonObject changed = JsonParser.parseString(validTalent()).getAsJsonObject();
        changed.getAsJsonObject("text").addProperty("name", "Changed Vitality");
        assertTrue(fixture.service.submitTalent(
                fixture.snapshots.active().revision(), TALENT, changed.toString(), true,
                fixture.environment, temporaryDirectory
        ).committed());
        EditorTransactionSummary update = new EditorPackStorage().history(temporaryDirectory).get(0);
        assertEquals(EditorTransactionOperation.UPDATE, update.operation());

        DefinitionUndoResult result = fixture.service.undoTransaction(
                fixture.snapshots.active().revision(), update.transactionId(),
                fixture.environment, temporaryDirectory
        );

        assertEquals(DefinitionUndoStatus.UNDONE, result.status());
        assertEquals(
                "Vitality",
                fixture.service.findTalent(TALENT).orElseThrow()
                        .getAsJsonObject("text").get("name").getAsString()
        );
        assertTrue(fixture.service.completeUndo(result));
    }

    @Test
    void undoingADeletionRestoresTheDefinitionFileAndSnapshot() throws Exception {
        Fixture fixture = fixture();
        assertTrue(fixture.service.submitTalent(
                fixture.snapshots.active().revision(), TALENT, validTalent(), true,
                fixture.environment, temporaryDirectory
        ).committed());
        DefinitionDeletionResult deletion = fixture.service.deleteDefinition(
                fixture.snapshots.active().revision(), DefinitionKind.TALENT, TALENT,
                fixture.environment, temporaryDirectory
        );
        assertTrue(deletion.deleted());
        EditorTransactionSummary transaction = new EditorPackStorage().history(temporaryDirectory).get(0);
        assertEquals(EditorTransactionOperation.DELETE, transaction.operation());

        DefinitionUndoResult undo = fixture.service.undoTransaction(
                fixture.snapshots.active().revision(), transaction.transactionId(),
                fixture.environment, temporaryDirectory
        );

        assertEquals(DefinitionUndoStatus.UNDONE, undo.status());
        assertTrue(fixture.snapshots.active().talents().containsKey(TALENT));
        assertTrue(Files.exists(temporaryDirectory.resolve(
                "talent_editor_generated/data/example/talent_editor/talents/vitality.json"
        )));
        assertTrue(fixture.service.completeUndo(undo));
    }

    @Test
    void rollbackAfterUndoRestoresThePreUndoStateAndKeepsTheTransactionAvailable() throws Exception {
        Fixture fixture = fixture();
        assertTrue(fixture.service.submitTalent(
                fixture.snapshots.active().revision(), TALENT, validTalent(), true,
                fixture.environment, temporaryDirectory
        ).committed());
        JsonObject changed = JsonParser.parseString(validTalent()).getAsJsonObject();
        changed.getAsJsonObject("text").addProperty("name", "Changed Vitality");
        assertTrue(fixture.service.submitTalent(
                fixture.snapshots.active().revision(), TALENT, changed.toString(), true,
                fixture.environment, temporaryDirectory
        ).committed());
        EditorPackStorage storage = new EditorPackStorage();
        EditorTransactionSummary update = storage.history(temporaryDirectory).get(0);
        DefinitionUndoResult undo = fixture.service.undoTransaction(
                fixture.snapshots.active().revision(), update.transactionId(),
                fixture.environment, temporaryDirectory
        );
        assertEquals(DefinitionUndoStatus.UNDONE, undo.status());

        assertTrue(fixture.service.rollbackUndo(undo, fixture.environment));
        assertEquals(
                "Changed Vitality",
                fixture.service.findTalent(TALENT).orElseThrow()
                        .getAsJsonObject("text").get("name").getAsString()
        );
        assertTrue(storage.history(temporaryDirectory).get(0).canUndo());
    }

    @Test
    void layoutBindingBlocksTalentDeletionUntilBindingIsCleared() throws Exception {
        Fixture fixture = fixture();
        assertTrue(fixture.service.submitTalent(fixture.snapshots.active().revision(),
                TALENT, validTalent(), true, fixture.environment, temporaryDirectory).committed());
        ResourceLocation layout = new ResourceLocation("example", "fixed_book");
        String bound = """
                {"schema":2,"pages":[{"id":"one","slots":[{"id":"hero","talent":"example:vitality"}]}]}
                """;
        assertTrue(fixture.service.submitLayout(fixture.snapshots.active().revision(),
                layout, bound, true, fixture.environment, temporaryDirectory).committed());
        long revision = fixture.snapshots.active().revision();
        var blocked = fixture.service.deleteDefinition(revision, DefinitionKind.TALENT, TALENT,
                fixture.environment, temporaryDirectory);
        assertFalse(blocked.deleted());
        assertEquals(revision, fixture.snapshots.active().revision());
        assertTrue(Files.exists(temporaryDirectory.resolve(
                "talent_editor_generated/data/example/talent_editor/talents/vitality.json")));
        assertEquals("$.pages[0].slots[0].talent", fixture.snapshots.active()
                .referencesTo(DefinitionKind.TALENT, TALENT).get(0).path());
        assertTrue(fixture.service.submitLayout(revision, layout,
                "{\"schema\":2,\"pages\":[{\"id\":\"one\",\"slots\":[{\"id\":\"hero\"}]}]}",
                true, fixture.environment, temporaryDirectory).committed());
        assertTrue(fixture.service.deleteDefinition(fixture.snapshots.active().revision(),
                DefinitionKind.TALENT, TALENT, fixture.environment, temporaryDirectory).deleted());
    }

    private static Fixture fixture() {
        SnapshotRepository snapshots = new SnapshotRepository();
        DefinitionService service = new DefinitionService(
                TalentCompiler.withBuiltins(),
                snapshots,
                new EditorPackStorage()
        );
        CompilationEnvironment environment = new CompilationEnvironment(
                Set.of("minecraft", "forge", "talent_editor"),
                Set.of(HEALTH)
        );
        JsonObject rarity = JsonParser.parseString("""
                {
                  "schema": 2,
                  "name": "Common",
                  "color": "#A0A0A0"
                }
                """).getAsJsonObject();
        service.acceptReload(
                new RawDefinitionSet(Map.of(COMMON, rarity), Map.of(), List.of()),
                environment
        );
        return new Fixture(service, snapshots, environment);
    }

    @Test
    void layoutSubmissionUsesTheSharedTransactionAndUndoPipeline() throws Exception {
        Fixture fixture = fixture();
        ResourceLocation layout = new ResourceLocation("example", "book");
        String json = """
                {
                  "schema": 2,
                  "enabled": true,
                  "priority": 15,
                  "background": "example:textures/gui/book.png",
                  "panelPercent": 50,
                  "columns": 3,
                  "rows": 4,
                  "slotWidth": 40,
                  "slotHeight": 30,
                  "iconSize": 16,
                  "showGeneratedEffects": false
                }
                """;

        EditorSubmissionResult submission = fixture.service.submitLayout(
                fixture.snapshots.active().revision(), layout, json, true,
                fixture.environment, temporaryDirectory
        );

        assertTrue(submission.committed());
        assertEquals(layout, fixture.snapshots.active().bookLayout().id());
        assertTrue(Files.exists(temporaryDirectory.resolve(
                "talent_editor_generated/data/example/talent_editor/layouts/book.json"
        )));
        EditorTransactionSummary transaction = new EditorPackStorage().history(temporaryDirectory).get(0);
        assertEquals(DefinitionKind.LAYOUT, transaction.kind());
        DefinitionUndoResult undo = fixture.service.undoTransaction(
                fixture.snapshots.active().revision(), transaction.transactionId(),
                fixture.environment, temporaryDirectory
        );
        assertEquals(DefinitionUndoStatus.UNDONE, undo.status());
        assertFalse(fixture.snapshots.active().layouts().containsKey(layout));
        assertEquals(com.kidxoxo.talenteditor.domain.TalentBookLayoutDefinition.DEFAULT_ID,
                fixture.snapshots.active().bookLayout().id());
        assertTrue(fixture.service.completeUndo(undo));
    }

    @Test
    void configuredItemSubmissionUsesTheSharedTransactionDeleteAndUndoPipeline() throws Exception {
        Fixture fixture = fixture();
        ResourceLocation item = new ResourceLocation("example", "talent_shard");
        String json = """
                {
                  "schema": 2,
                  "name": "Talent Shard",
                  "description": ["A configurable material."],
                  "customModelData": 1001,
                  "glint": true
                }
                """;

        EditorSubmissionResult validation = fixture.service.submitConfiguredItem(
                fixture.snapshots.active().revision(), item, json, false,
                fixture.environment, temporaryDirectory
        );
        assertTrue(validation.accepted());
        assertFalse(Files.exists(temporaryDirectory.resolve("talent_editor_generated")));

        EditorSubmissionResult submission = fixture.service.submitConfiguredItem(
                fixture.snapshots.active().revision(), item, json, true,
                fixture.environment, temporaryDirectory
        );
        assertTrue(submission.committed());
        assertEquals(1001, fixture.snapshots.active().configuredItems().get(item).customModelData());
        Path path = temporaryDirectory.resolve(
                "talent_editor_generated/data/example/talent_editor/items/talent_shard.json"
        );
        assertTrue(Files.exists(path));
        assertTrue(fixture.service.findConfiguredItem(item).isPresent());
        EditorTransactionSummary transaction = new EditorPackStorage().history(temporaryDirectory).get(0);
        assertEquals(DefinitionKind.ITEM, transaction.kind());

        DefinitionDeletionResult deletion = fixture.service.deleteDefinition(
                fixture.snapshots.active().revision(), DefinitionKind.ITEM, item,
                fixture.environment, temporaryDirectory
        );
        assertTrue(deletion.deleted());
        assertFalse(fixture.snapshots.active().configuredItems().containsKey(item));

        EditorTransactionSummary deleteTransaction = new EditorPackStorage().history(temporaryDirectory).get(0);
        DefinitionUndoResult undo = fixture.service.undoTransaction(
                fixture.snapshots.active().revision(), deleteTransaction.transactionId(),
                fixture.environment, temporaryDirectory
        );
        assertEquals(DefinitionUndoStatus.UNDONE, undo.status());
        assertTrue(fixture.snapshots.active().configuredItems().containsKey(item));
        assertTrue(fixture.service.completeUndo(undo));
    }

    @Test
    void configuredItemDeletionIsBlockedWhileTalentReferencesIt() throws Exception {
        Fixture fixture = fixture();
        ResourceLocation item = new ResourceLocation("example", "talent_shard");
        assertTrue(fixture.service.submitConfiguredItem(
                fixture.snapshots.active().revision(), item,
                "{\"schema\":2,\"name\":\"Talent Shard\"}", true,
                fixture.environment, temporaryDirectory
        ).committed());
        String talent = """
                {
                  "schema":2,
                  "text":{"name":"Vitality"},
                  "rarity":"talent_editor:common",
                  "acquireCondition":{"type":"has_item","item":"example:talent_shard","count":1},
                  "synthesis":{"enabled":true,"consumeItems":{"example:talent_shard":2}}
                }
                """;
        assertTrue(fixture.service.submitTalent(
                fixture.snapshots.active().revision(), TALENT, talent, true,
                fixture.environment, temporaryDirectory
        ).committed());

        var references = fixture.snapshots.active().referencesTo(DefinitionKind.ITEM, item);
        assertEquals(2, references.size());
        long revision = fixture.snapshots.active().revision();
        DefinitionDeletionResult blocked = fixture.service.deleteDefinition(
                revision, DefinitionKind.ITEM, item, fixture.environment, temporaryDirectory
        );
        assertFalse(blocked.deleted());
        assertEquals(revision, fixture.snapshots.active().revision());
        assertTrue(fixture.snapshots.active().configuredItems().containsKey(item));

        assertTrue(fixture.service.deleteDefinition(
                fixture.snapshots.active().revision(), DefinitionKind.TALENT, TALENT,
                fixture.environment, temporaryDirectory
        ).deleted());
        assertTrue(fixture.service.deleteDefinition(
                fixture.snapshots.active().revision(), DefinitionKind.ITEM, item,
                fixture.environment, temporaryDirectory
        ).deleted());
    }

    private static String validTalent() {
        return """
                {
                  "schema": 2,
                  "text": {"name": "Vitality"},
                  "rarity": "talent_editor:common",
                  "activeEffects": [
                    {
                      "type": "attribute_modifier",
                      "attribute": "minecraft:generic.max_health",
                      "amount": 4
                    }
                  ]
                }
                """;
    }

    private record Fixture(
            DefinitionService service,
            SnapshotRepository snapshots,
            CompilationEnvironment environment
    ) {
    }
}
