package com.kidxoxo.talenteditor.compile;

import com.kidxoxo.talenteditor.runtime.RuntimeSnapshot;
import com.kidxoxo.talenteditor.validation.ValidationReport;

import java.util.Optional;

public record CompilationResult(
        Optional<RuntimeSnapshot> candidate,
        ValidationReport report
) {
    public CompilationResult {
        candidate = candidate == null ? Optional.empty() : candidate;
    }

    public static CompilationResult rejected(ValidationReport report) {
        return new CompilationResult(Optional.empty(), report);
    }
}

