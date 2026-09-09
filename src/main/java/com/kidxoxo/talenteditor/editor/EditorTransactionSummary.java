package com.kidxoxo.talenteditor.editor;

import com.kidxoxo.talenteditor.domain.DefinitionKind;
import net.minecraft.resources.ResourceLocation;

import java.util.UUID;

public record EditorTransactionSummary(
        UUID transactionId,
        EditorTransactionOperation operation,
        DefinitionKind kind,
        ResourceLocation definitionId,
        long timestamp,
        EditorTransactionState state,
        boolean canUndo
) {
}
