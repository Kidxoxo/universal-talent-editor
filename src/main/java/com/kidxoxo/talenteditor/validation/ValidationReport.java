package com.kidxoxo.talenteditor.validation;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public final class ValidationReport {
    private static final Comparator<ValidationIssue> ORDER = Comparator
            .comparing(ValidationIssue::source)
            .thenComparing(ValidationIssue::path)
            .thenComparing(ValidationIssue::severity)
            .thenComparing(ValidationIssue::code);

    private final List<ValidationIssue> issues;

    private ValidationReport(List<ValidationIssue> issues) {
        this.issues = issues.stream().sorted(ORDER).toList();
    }

    public static ValidationReport empty() {
        return new ValidationReport(List.of());
    }

    public static Builder builder() {
        return new Builder();
    }

    public List<ValidationIssue> issues() {
        return issues;
    }

    public boolean hasErrors() {
        return issues.stream().anyMatch(issue -> issue.severity() == ValidationSeverity.ERROR);
    }

    public long errorCount() {
        return count(ValidationSeverity.ERROR);
    }

    public long warningCount() {
        return count(ValidationSeverity.WARNING);
    }

    private long count(ValidationSeverity severity) {
        return issues.stream().filter(issue -> issue.severity() == severity).count();
    }

    public static final class Builder {
        private final ArrayList<ValidationIssue> issues = new ArrayList<>();

        public Builder error(String code, String source, String path, String message) {
            issues.add(new ValidationIssue(ValidationSeverity.ERROR, code, source, path, message));
            return this;
        }

        public Builder warning(String code, String source, String path, String message) {
            issues.add(new ValidationIssue(ValidationSeverity.WARNING, code, source, path, message));
            return this;
        }

        public Builder info(String code, String source, String path, String message) {
            issues.add(new ValidationIssue(ValidationSeverity.INFO, code, source, path, message));
            return this;
        }

        public Builder add(ValidationIssue issue) {
            issues.add(issue);
            return this;
        }

        public Builder addAll(ValidationReport report) {
            issues.addAll(report.issues());
            return this;
        }

        public ValidationReport build() {
            return new ValidationReport(List.copyOf(issues));
        }
    }
}

