package com.kidxoxo.talenteditor.editor;

import com.kidxoxo.talenteditor.validation.ValidationReport;

public record EditorSubmissionResult(
        boolean accepted,
        boolean committed,
        boolean conflict,
        long activeRevision,
        String activeHash,
        ValidationReport report
) {
}

