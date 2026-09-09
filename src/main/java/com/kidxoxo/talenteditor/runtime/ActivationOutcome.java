package com.kidxoxo.talenteditor.runtime;

public record ActivationOutcome(
        boolean accepted,
        boolean changed,
        long activeRevision,
        String activeHash
) {
}

