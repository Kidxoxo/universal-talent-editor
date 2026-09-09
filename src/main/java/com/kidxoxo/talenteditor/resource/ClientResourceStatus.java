package com.kidxoxo.talenteditor.resource;

import java.util.Locale;
import java.util.Objects;

/** One client observation. Hash is optional because a sound event may map to multiple files. */
public record ClientResourceStatus(
        ClientResourceRequirement requirement,
        boolean available,
        String sha256
) {
    public ClientResourceStatus {
        requirement = Objects.requireNonNull(requirement, "requirement");
        sha256 = sha256 == null ? "" : sha256.toUpperCase(Locale.ROOT);
        if (!sha256.isEmpty() && !sha256.matches("[0-9A-F]{64}")) {
            throw new IllegalArgumentException("Invalid SHA-256");
        }
        if (!available && !sha256.isEmpty()) {
            throw new IllegalArgumentException("Missing resources cannot have a SHA-256");
        }
    }
}
