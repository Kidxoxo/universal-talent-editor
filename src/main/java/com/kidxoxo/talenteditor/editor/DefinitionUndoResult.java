package com.kidxoxo.talenteditor.editor;

import com.kidxoxo.talenteditor.compile.RawDefinitionSet;

import java.util.Optional;
import java.util.UUID;

public record DefinitionUndoResult(
        DefinitionUndoStatus status,
        UUID transactionId,
        long activeRevision,
        Optional<EditorPackStorage.UndoPlan> plan,
        Optional<EditorPackStorage.UndoFileChange> fileChange,
        Optional<RawDefinitionSet> rollbackDefinitions
) {
    public DefinitionUndoResult {
        plan = plan == null ? Optional.empty() : plan;
        fileChange = fileChange == null ? Optional.empty() : fileChange;
        rollbackDefinitions = rollbackDefinitions == null ? Optional.empty() : rollbackDefinitions;
    }

    public boolean undone() {
        return status == DefinitionUndoStatus.UNDONE;
    }

    public static DefinitionUndoResult rejected(
            DefinitionUndoStatus status,
            UUID transactionId,
            long activeRevision
    ) {
        return new DefinitionUndoResult(
                status, transactionId, activeRevision,
                Optional.empty(), Optional.empty(), Optional.empty()
        );
    }
}
