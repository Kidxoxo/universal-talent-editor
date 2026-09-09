package com.kidxoxo.talenteditor.editor;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.kidxoxo.talenteditor.compile.CompilationEnvironment;
import com.kidxoxo.talenteditor.compile.CompilationResult;
import com.kidxoxo.talenteditor.compile.RawDefinitionSet;
import com.kidxoxo.talenteditor.compile.TalentCompiler;
import com.kidxoxo.talenteditor.domain.DefinitionKind;
import com.kidxoxo.talenteditor.domain.DefinitionReference;
import com.kidxoxo.talenteditor.runtime.ActivationOutcome;
import com.kidxoxo.talenteditor.runtime.SnapshotRepository;
import com.kidxoxo.talenteditor.validation.ValidationReport;
import net.minecraft.resources.ResourceLocation;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

public final class DefinitionService {
    public static final int MAX_JSON_CHARACTERS = 131_072;

    private final TalentCompiler compiler;
    private final SnapshotRepository snapshots;
    private final EditorPackStorage storage;
    private final AtomicReference<RawDefinitionSet> activeRaw = new AtomicReference<>(RawDefinitionSet.empty());

    public DefinitionService(TalentCompiler compiler, SnapshotRepository snapshots, EditorPackStorage storage) {
        this.compiler = compiler;
        this.snapshots = snapshots;
        this.storage = storage;
    }

    public synchronized ActivationOutcome acceptReload(
            RawDefinitionSet definitions,
            CompilationEnvironment environment
    ) {
        CompilationResult result = compiler.compile(definitions, environment);
        ActivationOutcome outcome = snapshots.apply(result);
        if (outcome.accepted()) activeRaw.set(definitions);
        return outcome;
    }

    public synchronized EditorSubmissionResult submitTalent(
            long baseRevision,
            ResourceLocation id,
            String jsonText,
            boolean commit,
            CompilationEnvironment environment,
            Path datapacksRoot
    ) {
        if (commit && baseRevision != snapshots.active().revision()) {
            ValidationReport report = ValidationReport.builder()
                    .error(
                            "REVISION_CONFLICT",
                            "talent " + id,
                            "$",
                            "Editor was based on revision " + baseRevision +
                                    " but active revision is " + snapshots.active().revision()
                    )
                    .build();
            return rejected(true, report);
        }
        if (jsonText.length() > MAX_JSON_CHARACTERS) {
            ValidationReport report = ValidationReport.builder()
                    .error(
                            "JSON_SIZE_LIMIT",
                            "talent " + id,
                            "$",
                            "Definition exceeds " + MAX_JSON_CHARACTERS + " characters"
                    )
                    .build();
            return rejected(false, report);
        }

        JsonObject definition;
        try {
            JsonElement parsed = JsonParser.parseString(jsonText);
            if (!parsed.isJsonObject()) {
                ValidationReport report = ValidationReport.builder()
                        .error("EXPECTED_ROOT_OBJECT", "talent " + id, "$", "Definition root must be an object")
                        .build();
                return rejected(false, report);
            }
            definition = parsed.getAsJsonObject();
        } catch (RuntimeException ex) {
            ValidationReport report = ValidationReport.builder()
                    .error(
                            "JSON_PARSE_FAILED",
                            "talent " + id,
                            "$",
                            ex.getMessage() == null ? "Invalid JSON" : ex.getMessage()
                    )
                    .build();
            return rejected(false, report);
        }

        RawDefinitionSet candidateDefinitions = activeRaw.get()
                .withTalent(id, definition, EditorPackStorage.PACK_FOLDER);
        CompilationResult compilation = compiler.compile(candidateDefinitions, environment);
        if (compilation.report().hasErrors() || compilation.candidate().isEmpty()) {
            return rejected(false, compilation.report());
        }
        if (!commit) {
            return new EditorSubmissionResult(
                    true,
                    false,
                    false,
                    snapshots.active().revision(),
                    snapshots.active().contentHash(),
                    compilation.report()
            );
        }

        try {
            storage.writeTalent(datapacksRoot, id, definition);
        } catch (Exception ex) {
            ValidationReport.Builder report = ValidationReport.builder().addAll(compilation.report());
            report.error(
                    "PERSISTENCE_FAILED",
                    "talent " + id,
                    "$",
                    ex.getMessage() == null ? ex.getClass().getSimpleName() : ex.getMessage()
            );
            return rejected(false, report.build());
        }

        ActivationOutcome outcome = snapshots.apply(compilation);
        if (outcome.accepted()) activeRaw.set(candidateDefinitions);
        return new EditorSubmissionResult(
                outcome.accepted(),
                outcome.accepted(),
                false,
                outcome.activeRevision(),
                outcome.activeHash(),
                compilation.report()
        );
    }

    public synchronized EditorSubmissionResult submitPool(
            long baseRevision,
            ResourceLocation id,
            String jsonText,
            boolean commit,
            CompilationEnvironment environment,
            Path datapacksRoot
    ) {
        if (commit && baseRevision != snapshots.active().revision()) {
            ValidationReport report = ValidationReport.builder()
                    .error(
                            "REVISION_CONFLICT",
                            "pool " + id,
                            "$",
                            "Editor was based on revision " + baseRevision +
                                    " but active revision is " + snapshots.active().revision()
                    )
                    .build();
            return rejected(true, report);
        }
        if (jsonText.length() > MAX_JSON_CHARACTERS) {
            ValidationReport report = ValidationReport.builder()
                    .error(
                            "JSON_SIZE_LIMIT",
                            "pool " + id,
                            "$",
                            "Definition exceeds " + MAX_JSON_CHARACTERS + " characters"
                    )
                    .build();
            return rejected(false, report);
        }

        JsonObject definition;
        try {
            JsonElement parsed = JsonParser.parseString(jsonText);
            if (!parsed.isJsonObject()) {
                ValidationReport report = ValidationReport.builder()
                        .error("EXPECTED_ROOT_OBJECT", "pool " + id, "$", "Definition root must be an object")
                        .build();
                return rejected(false, report);
            }
            definition = parsed.getAsJsonObject();
        } catch (RuntimeException ex) {
            ValidationReport report = ValidationReport.builder()
                    .error(
                            "JSON_PARSE_FAILED",
                            "pool " + id,
                            "$",
                            ex.getMessage() == null ? "Invalid JSON" : ex.getMessage()
                    )
                    .build();
            return rejected(false, report);
        }

        RawDefinitionSet candidateDefinitions = activeRaw.get()
                .withPool(id, definition, EditorPackStorage.PACK_FOLDER);
        CompilationResult compilation = compiler.compile(candidateDefinitions, environment);
        if (compilation.report().hasErrors() || compilation.candidate().isEmpty()) {
            return rejected(false, compilation.report());
        }
        if (!commit) {
            return new EditorSubmissionResult(
                    true,
                    false,
                    false,
                    snapshots.active().revision(),
                    snapshots.active().contentHash(),
                    compilation.report()
            );
        }

        try {
            storage.writePool(datapacksRoot, id, definition);
        } catch (Exception ex) {
            ValidationReport.Builder report = ValidationReport.builder().addAll(compilation.report());
            report.error(
                    "PERSISTENCE_FAILED",
                    "pool " + id,
                    "$",
                    ex.getMessage() == null ? ex.getClass().getSimpleName() : ex.getMessage()
            );
            return rejected(false, report.build());
        }

        ActivationOutcome outcome = snapshots.apply(compilation);
        if (outcome.accepted()) activeRaw.set(candidateDefinitions);
        return new EditorSubmissionResult(
                outcome.accepted(),
                outcome.accepted(),
                false,
                outcome.activeRevision(),
                outcome.activeHash(),
                compilation.report()
        );
    }

    public synchronized EditorSubmissionResult submitRarity(
            long baseRevision,
            ResourceLocation id,
            String jsonText,
            boolean commit,
            CompilationEnvironment environment,
            Path datapacksRoot
    ) {
        if (commit && baseRevision != snapshots.active().revision()) {
            ValidationReport report = ValidationReport.builder()
                    .error(
                            "REVISION_CONFLICT",
                            "rarity " + id,
                            "$",
                            "Editor was based on revision " + baseRevision +
                                    " but active revision is " + snapshots.active().revision()
                    )
                    .build();
            return rejected(true, report);
        }
        if (jsonText.length() > MAX_JSON_CHARACTERS) {
            ValidationReport report = ValidationReport.builder()
                    .error(
                            "JSON_SIZE_LIMIT",
                            "rarity " + id,
                            "$",
                            "Definition exceeds " + MAX_JSON_CHARACTERS + " characters"
                    )
                    .build();
            return rejected(false, report);
        }

        JsonObject definition;
        try {
            JsonElement parsed = JsonParser.parseString(jsonText);
            if (!parsed.isJsonObject()) {
                ValidationReport report = ValidationReport.builder()
                        .error("EXPECTED_ROOT_OBJECT", "rarity " + id, "$", "Definition root must be an object")
                        .build();
                return rejected(false, report);
            }
            definition = parsed.getAsJsonObject();
        } catch (RuntimeException ex) {
            ValidationReport report = ValidationReport.builder()
                    .error(
                            "JSON_PARSE_FAILED",
                            "rarity " + id,
                            "$",
                            ex.getMessage() == null ? "Invalid JSON" : ex.getMessage()
                    )
                    .build();
            return rejected(false, report);
        }

        RawDefinitionSet candidateDefinitions = activeRaw.get()
                .withRarity(id, definition, EditorPackStorage.PACK_FOLDER);
        CompilationResult compilation = compiler.compile(candidateDefinitions, environment);
        if (compilation.report().hasErrors() || compilation.candidate().isEmpty()) {
            return rejected(false, compilation.report());
        }
        if (!commit) {
            return new EditorSubmissionResult(
                    true,
                    false,
                    false,
                    snapshots.active().revision(),
                    snapshots.active().contentHash(),
                    compilation.report()
            );
        }

        try {
            storage.writeRarity(datapacksRoot, id, definition);
        } catch (Exception ex) {
            ValidationReport.Builder report = ValidationReport.builder().addAll(compilation.report());
            report.error(
                    "PERSISTENCE_FAILED",
                    "rarity " + id,
                    "$",
                    ex.getMessage() == null ? ex.getClass().getSimpleName() : ex.getMessage()
            );
            return rejected(false, report.build());
        }

        ActivationOutcome outcome = snapshots.apply(compilation);
        if (outcome.accepted()) activeRaw.set(candidateDefinitions);
        return new EditorSubmissionResult(
                outcome.accepted(),
                outcome.accepted(),
                false,
                outcome.activeRevision(),
                outcome.activeHash(),
                compilation.report()
        );
    }

    public RawDefinitionSet activeRaw() {
        return activeRaw.get();
    }

    public synchronized EditorSubmissionResult submitCounter(
            long baseRevision,
            ResourceLocation id,
            String jsonText,
            boolean commit,
            CompilationEnvironment environment,
            Path datapacksRoot
    ) {
        String source = "counter " + id;
        if (commit && baseRevision != snapshots.active().revision()) {
            return rejected(true, ValidationReport.builder().error(
                    "REVISION_CONFLICT", source, "$",
                    "Editor was based on revision " + baseRevision
                            + " but active revision is " + snapshots.active().revision()
            ).build());
        }
        if (jsonText.length() > MAX_JSON_CHARACTERS) {
            return rejected(false, ValidationReport.builder().error(
                    "JSON_SIZE_LIMIT", source, "$",
                    "Definition exceeds " + MAX_JSON_CHARACTERS + " characters"
            ).build());
        }

        JsonObject definition;
        try {
            JsonElement parsed = JsonParser.parseString(jsonText);
            if (!parsed.isJsonObject()) {
                return rejected(false, ValidationReport.builder().error(
                        "EXPECTED_ROOT_OBJECT", source, "$", "Definition root must be an object"
                ).build());
            }
            definition = parsed.getAsJsonObject();
        } catch (RuntimeException ex) {
            return rejected(false, ValidationReport.builder().error(
                    "JSON_PARSE_FAILED", source, "$",
                    ex.getMessage() == null ? "Invalid JSON" : ex.getMessage()
            ).build());
        }

        RawDefinitionSet candidateDefinitions = activeRaw.get()
                .withCounter(id, definition, EditorPackStorage.PACK_FOLDER);
        CompilationResult compilation = compiler.compile(candidateDefinitions, environment);
        if (compilation.report().hasErrors() || compilation.candidate().isEmpty()) {
            return rejected(false, compilation.report());
        }
        if (!commit) {
            return new EditorSubmissionResult(
                    true, false, false, snapshots.active().revision(),
                    snapshots.active().contentHash(), compilation.report()
            );
        }

        try {
            storage.writeCounter(datapacksRoot, id, definition);
        } catch (Exception ex) {
            ValidationReport.Builder report = ValidationReport.builder().addAll(compilation.report());
            report.error("PERSISTENCE_FAILED", source, "$",
                    ex.getMessage() == null ? ex.getClass().getSimpleName() : ex.getMessage());
            return rejected(false, report.build());
        }
        ActivationOutcome outcome = snapshots.apply(compilation);
        if (outcome.accepted()) activeRaw.set(candidateDefinitions);
        return new EditorSubmissionResult(
                outcome.accepted(), outcome.accepted(), false,
                outcome.activeRevision(), outcome.activeHash(), compilation.report()
        );
    }

    public synchronized EditorSubmissionResult submitLayout(
            long baseRevision,
            ResourceLocation id,
            String jsonText,
            boolean commit,
            CompilationEnvironment environment,
            Path datapacksRoot
    ) {
        String source = "layout " + id;
        if (commit && baseRevision != snapshots.active().revision()) {
            return rejected(true, ValidationReport.builder().error(
                    "REVISION_CONFLICT", source, "$",
                    "Editor was based on revision " + baseRevision
                            + " but active revision is " + snapshots.active().revision()
            ).build());
        }
        if (jsonText.length() > MAX_JSON_CHARACTERS) {
            return rejected(false, ValidationReport.builder().error(
                    "JSON_SIZE_LIMIT", source, "$",
                    "Definition exceeds " + MAX_JSON_CHARACTERS + " characters"
            ).build());
        }

        JsonObject definition;
        try {
            JsonElement parsed = JsonParser.parseString(jsonText);
            if (!parsed.isJsonObject()) {
                return rejected(false, ValidationReport.builder().error(
                        "EXPECTED_ROOT_OBJECT", source, "$", "Definition root must be an object"
                ).build());
            }
            definition = parsed.getAsJsonObject();
        } catch (RuntimeException ex) {
            return rejected(false, ValidationReport.builder().error(
                    "JSON_PARSE_FAILED", source, "$",
                    ex.getMessage() == null ? "Invalid JSON" : ex.getMessage()
            ).build());
        }

        RawDefinitionSet candidateDefinitions = activeRaw.get()
                .withLayout(id, definition, EditorPackStorage.PACK_FOLDER);
        CompilationResult compilation = compiler.compile(candidateDefinitions, environment);
        if (compilation.report().hasErrors() || compilation.candidate().isEmpty()) {
            return rejected(false, compilation.report());
        }
        if (!commit) {
            return new EditorSubmissionResult(
                    true, false, false, snapshots.active().revision(),
                    snapshots.active().contentHash(), compilation.report()
            );
        }

        try {
            storage.writeLayout(datapacksRoot, id, definition);
        } catch (Exception ex) {
            ValidationReport.Builder report = ValidationReport.builder().addAll(compilation.report());
            report.error("PERSISTENCE_FAILED", source, "$",
                    ex.getMessage() == null ? ex.getClass().getSimpleName() : ex.getMessage());
            return rejected(false, report.build());
        }
        ActivationOutcome outcome = snapshots.apply(compilation);
        if (outcome.accepted()) activeRaw.set(candidateDefinitions);
        return new EditorSubmissionResult(
                outcome.accepted(), outcome.accepted(), false,
                outcome.activeRevision(), outcome.activeHash(), compilation.report()
        );
    }

    public synchronized EditorSubmissionResult submitConfiguredItem(
            long baseRevision,
            ResourceLocation id,
            String jsonText,
            boolean commit,
            CompilationEnvironment environment,
            Path datapacksRoot
    ) {
        String source = "configured item " + id;
        if (commit && baseRevision != snapshots.active().revision()) {
            return rejected(true, ValidationReport.builder().error(
                    "REVISION_CONFLICT", source, "$",
                    "Editor was based on revision " + baseRevision
                            + " but active revision is " + snapshots.active().revision()
            ).build());
        }
        if (jsonText.length() > MAX_JSON_CHARACTERS) {
            return rejected(false, ValidationReport.builder().error(
                    "JSON_SIZE_LIMIT", source, "$",
                    "Definition exceeds " + MAX_JSON_CHARACTERS + " characters"
            ).build());
        }

        JsonObject definition;
        try {
            JsonElement parsed = JsonParser.parseString(jsonText);
            if (!parsed.isJsonObject()) {
                return rejected(false, ValidationReport.builder().error(
                        "EXPECTED_ROOT_OBJECT", source, "$", "Definition root must be an object"
                ).build());
            }
            definition = parsed.getAsJsonObject();
        } catch (RuntimeException ex) {
            return rejected(false, ValidationReport.builder().error(
                    "JSON_PARSE_FAILED", source, "$",
                    ex.getMessage() == null ? "Invalid JSON" : ex.getMessage()
            ).build());
        }

        RawDefinitionSet candidateDefinitions = activeRaw.get()
                .withConfiguredItem(id, definition, EditorPackStorage.PACK_FOLDER);
        CompilationResult compilation = compiler.compile(candidateDefinitions, environment);
        if (compilation.report().hasErrors() || compilation.candidate().isEmpty()) {
            return rejected(false, compilation.report());
        }
        if (!commit) {
            return new EditorSubmissionResult(
                    true, false, false, snapshots.active().revision(),
                    snapshots.active().contentHash(), compilation.report()
            );
        }

        try {
            storage.writeConfiguredItem(datapacksRoot, id, definition);
        } catch (Exception ex) {
            ValidationReport.Builder report = ValidationReport.builder().addAll(compilation.report());
            report.error("PERSISTENCE_FAILED", source, "$",
                    ex.getMessage() == null ? ex.getClass().getSimpleName() : ex.getMessage());
            return rejected(false, report.build());
        }
        ActivationOutcome outcome = snapshots.apply(compilation);
        if (outcome.accepted()) activeRaw.set(candidateDefinitions);
        return new EditorSubmissionResult(
                outcome.accepted(), outcome.accepted(), false,
                outcome.activeRevision(), outcome.activeHash(), compilation.report()
        );
    }

    public Optional<JsonObject> findTalent(ResourceLocation id) {
        JsonObject talent = activeRaw.get().talents().get(id);
        return talent == null ? Optional.empty() : Optional.of(talent.deepCopy());
    }

    public Optional<JsonObject> findPool(ResourceLocation id) {
        JsonObject pool = activeRaw.get().pools().get(id);
        return pool == null ? Optional.empty() : Optional.of(pool.deepCopy());
    }

    public Optional<JsonObject> findRarity(ResourceLocation id) {
        JsonObject rarity = activeRaw.get().rarities().get(id);
        return rarity == null ? Optional.empty() : Optional.of(rarity.deepCopy());
    }

    public Optional<JsonObject> findCounter(ResourceLocation id) {
        JsonObject counter = activeRaw.get().counters().get(id);
        return counter == null ? Optional.empty() : Optional.of(counter.deepCopy());
    }

    public Optional<JsonObject> findLayout(ResourceLocation id) {
        JsonObject layout = activeRaw.get().layouts().get(id);
        return layout == null ? Optional.empty() : Optional.of(layout.deepCopy());
    }

    public Optional<JsonObject> findConfiguredItem(ResourceLocation id) {
        JsonObject item = activeRaw.get().configuredItems().get(id);
        return item == null ? Optional.empty() : Optional.of(item.deepCopy());
    }

    public synchronized DefinitionDeletionResult deleteDefinition(
            long baseRevision,
            DefinitionKind kind,
            ResourceLocation id,
            CompilationEnvironment environment,
            Path datapacksRoot
    ) {
        long activeRevision = snapshots.active().revision();
        if (baseRevision != activeRevision) {
            return DefinitionDeletionResult.rejected(
                    DefinitionDeletionStatus.REVISION_CONFLICT, kind, id, activeRevision, List.of()
            );
        }
        if (kind == DefinitionKind.TALENT_CATEGORY || kind == DefinitionKind.COUNTER_CATEGORY) {
            return DefinitionDeletionResult.rejected(
                    DefinitionDeletionStatus.INVALID_KIND, kind, id, activeRevision, List.of()
            );
        }
        RawDefinitionSet current = activeRaw.get();
        if (!current.contains(kind, id)) {
            return DefinitionDeletionResult.rejected(
                    DefinitionDeletionStatus.NOT_FOUND, kind, id, activeRevision, List.of()
            );
        }
        if (!EditorPackStorage.isGeneratedSource(current.source(kind, id))) {
            return DefinitionDeletionResult.rejected(
                    DefinitionDeletionStatus.NOT_GENERATED, kind, id, activeRevision, List.of()
            );
        }
        List<DefinitionReference> blocking = snapshots.active().referencesTo(kind, id).stream()
                .filter(reference -> reference.sourceKind() != kind || !reference.sourceId().equals(id))
                .toList();
        if (!blocking.isEmpty()) {
            return DefinitionDeletionResult.rejected(
                    DefinitionDeletionStatus.BLOCKED_REFERENCES, kind, id, activeRevision, blocking
            );
        }

        RawDefinitionSet candidateDefinitions = current.without(kind, id);
        CompilationResult compilation = compiler.compile(candidateDefinitions, environment);
        if (compilation.report().hasErrors() || compilation.candidate().isEmpty()) {
            return DefinitionDeletionResult.rejected(
                    DefinitionDeletionStatus.VALIDATION_FAILED, kind, id, activeRevision, List.of()
            );
        }

        EditorPackStorage.RecoveredDefinition recovered;
        try {
            recovered = storage.recoverDefinition(datapacksRoot, kind, id);
        } catch (Exception ex) {
            return DefinitionDeletionResult.rejected(
                    DefinitionDeletionStatus.IO_FAILED, kind, id, activeRevision, List.of()
            );
        }

        ActivationOutcome outcome = snapshots.apply(compilation);
        if (!outcome.accepted()) {
            try {
                storage.restoreRecovered(recovered);
            } catch (Exception ignored) {
            }
            return DefinitionDeletionResult.rejected(
                    DefinitionDeletionStatus.VALIDATION_FAILED, kind, id,
                    snapshots.active().revision(), List.of()
            );
        }
        activeRaw.set(candidateDefinitions);
        return new DefinitionDeletionResult(
                DefinitionDeletionStatus.DELETED, kind, id, outcome.activeRevision(), List.of(),
                Optional.of(recovered), Optional.of(current)
        );
    }

    public synchronized boolean rollbackDeletion(
            DefinitionDeletionResult deletion,
            CompilationEnvironment environment
    ) {
        if (!deletion.deleted() || deletion.recovered().isEmpty() || deletion.rollbackDefinitions().isEmpty()) {
            return false;
        }
        try {
            RawDefinitionSet previous = deletion.rollbackDefinitions().orElseThrow();
            RawDefinitionSet rollback = restoreDeletedDefinition(
                    activeRaw.get(), previous, deletion.kind(), deletion.id()
            );
            CompilationResult compilation = compiler.compile(rollback, environment);
            if (compilation.report().hasErrors() || compilation.candidate().isEmpty()) return false;
            storage.restoreRecovered(deletion.recovered().orElseThrow());
            ActivationOutcome outcome = snapshots.apply(compilation);
            if (!outcome.accepted()) return false;
            activeRaw.set(rollback);
            return true;
        } catch (Exception ignored) {
            return false;
        }
    }

    public synchronized DefinitionUndoResult undoTransaction(
            long baseRevision,
            java.util.UUID transactionId,
            CompilationEnvironment environment,
            Path datapacksRoot
    ) {
        long activeRevision = snapshots.active().revision();
        if (baseRevision != activeRevision) {
            return DefinitionUndoResult.rejected(
                    DefinitionUndoStatus.REVISION_CONFLICT, transactionId, activeRevision
            );
        }
        EditorPackStorage.UndoPreparation preparation = storage.prepareUndo(datapacksRoot, transactionId);
        if (preparation.plan().isEmpty()) {
            return DefinitionUndoResult.rejected(preparation.status(), transactionId, activeRevision);
        }
        EditorPackStorage.UndoPlan plan = preparation.plan().orElseThrow();
        RawDefinitionSet current = activeRaw.get();
        if (plan.afterDefinition().isPresent()
                && (!current.contains(plan.kind(), plan.definitionId())
                || !EditorPackStorage.isGeneratedSource(current.source(plan.kind(), plan.definitionId())))) {
            return DefinitionUndoResult.rejected(
                    DefinitionUndoStatus.STALE_TARGET, transactionId, activeRevision
            );
        }
        if (plan.beforeDefinition().isEmpty()) {
            List<DefinitionReference> blocking = snapshots.active()
                    .referencesTo(plan.kind(), plan.definitionId()).stream()
                    .filter(reference -> reference.sourceKind() != plan.kind()
                            || !reference.sourceId().equals(plan.definitionId()))
                    .toList();
            if (!blocking.isEmpty()) {
                return DefinitionUndoResult.rejected(
                        DefinitionUndoStatus.BLOCKED_REFERENCES, transactionId, activeRevision
                );
            }
        }

        RawDefinitionSet candidate = applyTargetState(
                current, plan.kind(), plan.definitionId(),
                plan.beforeDefinition(), EditorPackStorage.PACK_FOLDER
        );
        CompilationResult compilation = compiler.compile(candidate, environment);
        if (compilation.report().hasErrors() || compilation.candidate().isEmpty()) {
            return DefinitionUndoResult.rejected(
                    DefinitionUndoStatus.VALIDATION_FAILED, transactionId, activeRevision
            );
        }

        EditorPackStorage.UndoFileChange change;
        try {
            change = storage.applyUndo(plan);
        } catch (Exception ex) {
            return DefinitionUndoResult.rejected(
                    DefinitionUndoStatus.IO_FAILED, transactionId, activeRevision
            );
        }
        ActivationOutcome outcome = snapshots.apply(compilation);
        if (!outcome.accepted()) {
            storage.rollbackUndo(change);
            return DefinitionUndoResult.rejected(
                    DefinitionUndoStatus.VALIDATION_FAILED, transactionId,
                    snapshots.active().revision()
            );
        }
        activeRaw.set(candidate);
        return new DefinitionUndoResult(
                DefinitionUndoStatus.UNDONE, transactionId, outcome.activeRevision(),
                Optional.of(plan), Optional.of(change), Optional.of(current)
        );
    }

    public synchronized boolean completeUndo(DefinitionUndoResult result) {
        if (!result.undone() || result.plan().isEmpty()) return false;
        try {
            storage.completeUndo(result.plan().orElseThrow());
            return true;
        } catch (Exception ignored) {
            return false;
        }
    }

    public synchronized boolean rollbackUndo(
            DefinitionUndoResult result,
            CompilationEnvironment environment
    ) {
        if (!result.undone() || result.fileChange().isEmpty() || result.rollbackDefinitions().isEmpty()
                || result.plan().isEmpty()) {
            return false;
        }
        EditorPackStorage.UndoPlan plan = result.plan().orElseThrow();
        RawDefinitionSet desired = result.rollbackDefinitions().orElseThrow();
        RawDefinitionSet rollback = desired.contains(plan.kind(), plan.definitionId())
                ? applyTargetState(
                        activeRaw.get(), plan.kind(), plan.definitionId(),
                        definition(desired, plan.kind(), plan.definitionId()),
                        desired.source(plan.kind(), plan.definitionId())
                )
                : activeRaw.get().without(plan.kind(), plan.definitionId());
        CompilationResult compilation = compiler.compile(rollback, environment);
        if (compilation.report().hasErrors() || compilation.candidate().isEmpty()) return false;
        if (!storage.rollbackUndo(result.fileChange().orElseThrow())) return false;
        ActivationOutcome outcome = snapshots.apply(compilation);
        if (!outcome.accepted()) return false;
        activeRaw.set(rollback);
        return true;
    }

    private static RawDefinitionSet restoreDeletedDefinition(
            RawDefinitionSet current,
            RawDefinitionSet previous,
            DefinitionKind kind,
            ResourceLocation id
    ) {
        return applyTargetState(
                current, kind, id, definition(previous, kind, id), previous.source(kind, id)
        );
    }

    private static Optional<JsonObject> definition(
            RawDefinitionSet definitions,
            DefinitionKind kind,
            ResourceLocation id
    ) {
        return Optional.ofNullable(switch (kind) {
            case TALENT -> definitions.talents().get(id);
            case RARITY -> definitions.rarities().get(id);
            case POOL -> definitions.pools().get(id);
            case COUNTER -> definitions.counters().get(id);
            case LAYOUT -> definitions.layouts().get(id);
            case ITEM -> definitions.configuredItems().get(id);
            case TALENT_CATEGORY, COUNTER_CATEGORY -> null;
        });
    }

    private static RawDefinitionSet applyTargetState(
            RawDefinitionSet current,
            DefinitionKind kind,
            ResourceLocation id,
            Optional<JsonObject> definition,
            String source
    ) {
        if (definition.isEmpty()) return current.without(kind, id);
        JsonObject value = definition.orElseThrow();
        return switch (kind) {
            case TALENT -> current.withTalent(id, value, source);
            case RARITY -> current.withRarity(id, value, source);
            case POOL -> current.withPool(id, value, source);
            case COUNTER -> current.withCounter(id, value, source);
            case LAYOUT -> current.withLayout(id, value, source);
            case ITEM -> current.withConfiguredItem(id, value, source);
            case TALENT_CATEGORY, COUNTER_CATEGORY -> throw new IllegalArgumentException(
                    "Category indexes are not file-backed definitions"
            );
        };
    }

    private EditorSubmissionResult rejected(boolean conflict, ValidationReport report) {
        return new EditorSubmissionResult(
                false,
                false,
                conflict,
                snapshots.active().revision(),
                snapshots.active().contentHash(),
                report
        );
    }
}
