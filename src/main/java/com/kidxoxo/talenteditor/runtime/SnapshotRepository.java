package com.kidxoxo.talenteditor.runtime;

import com.kidxoxo.talenteditor.compile.CompilationResult;
import com.kidxoxo.talenteditor.validation.ValidationReport;

import java.util.concurrent.atomic.AtomicReference;

public final class SnapshotRepository {
    private final AtomicReference<RuntimeSnapshot> active = new AtomicReference<>(RuntimeSnapshot.empty());
    private final AtomicReference<ValidationReport> lastReport = new AtomicReference<>(ValidationReport.empty());

    public RuntimeSnapshot active() {
        return active.get();
    }

    public ValidationReport lastReport() {
        return lastReport.get();
    }

    public synchronized ActivationOutcome apply(CompilationResult result) {
        lastReport.set(result.report());
        RuntimeSnapshot current = active.get();
        if (result.report().hasErrors() || result.candidate().isEmpty()) {
            return new ActivationOutcome(false, false, current.revision(), current.contentHash());
        }

        RuntimeSnapshot candidate = result.candidate().orElseThrow();
        if (candidate.contentHash().equals(current.contentHash())) {
            return new ActivationOutcome(true, false, current.revision(), current.contentHash());
        }

        RuntimeSnapshot next = candidate.withRevision(current.revision() + 1L);
        active.set(next);
        return new ActivationOutcome(true, true, next.revision(), next.contentHash());
    }
}

