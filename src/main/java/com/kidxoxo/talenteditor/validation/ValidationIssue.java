package com.kidxoxo.talenteditor.validation;

import java.util.Objects;

public record ValidationIssue(
        ValidationSeverity severity,
        String code,
        String source,
        String path,
        String message
) {
    public ValidationIssue {
        severity = Objects.requireNonNull(severity, "severity");
        code = requireText(code, "code");
        source = requireText(source, "source");
        path = path == null || path.isBlank() ? "$" : path;
        message = requireText(message, "message");
    }

    private static String requireText(String value, String name) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(name + " must not be blank");
        }
        return value;
    }
}

