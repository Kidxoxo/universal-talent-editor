package com.kidxoxo.talenteditor.editor;

import com.kidxoxo.talenteditor.compile.RawDefinitionSet;
import com.kidxoxo.talenteditor.domain.DefinitionKind;
import com.kidxoxo.talenteditor.domain.DefinitionReference;
import net.minecraft.resources.ResourceLocation;

import java.util.List;
import java.util.Optional;

public record DefinitionDeletionResult(
        DefinitionDeletionStatus status,
        DefinitionKind kind,
        ResourceLocation id,
        long activeRevision,
        List<DefinitionReference> blockingReferences,
        Optional<EditorPackStorage.RecoveredDefinition> recovered,
        Optional<RawDefinitionSet> rollbackDefinitions
) {
    public DefinitionDeletionResult {
        blockingReferences = List.copyOf(blockingReferences);
        recovered = recovered == null ? Optional.empty() : recovered;
        rollbackDefinitions = rollbackDefinitions == null ? Optional.empty() : rollbackDefinitions;
    }

    public boolean deleted() {
        return status == DefinitionDeletionStatus.DELETED;
    }

    public static DefinitionDeletionResult rejected(
            DefinitionDeletionStatus status,
            DefinitionKind kind,
            ResourceLocation id,
            long activeRevision,
            List<DefinitionReference> blockingReferences
    ) {
        return new DefinitionDeletionResult(
                status, kind, id, activeRevision, blockingReferences, Optional.empty(), Optional.empty()
        );
    }
}
