package dev.iadev.domain.taskfile;

import java.util.Objects;

/**
 * A single validation finding on a task file.
 *
 * <p>{@code ruleId} matches one of the canonical schema rule IDs documented in
 * {@code plans/epic-0038/schemas/task-schema.md} §4 (e.g. {@code TF-SCHEMA-001}).</p>
 *
 * @param ruleId   canonical rule identifier (non-blank)
 * @param severity violation severity
 * @param message  human-readable description (non-blank)
 */
public record ValidationViolation(String ruleId, Severity severity, String message) {

    public ValidationViolation {
        Objects.requireNonNull(ruleId, "ruleId");
        Objects.requireNonNull(severity, "severity");
        Objects.requireNonNull(message, "message");
        if (ruleId.isBlank()) {
            throw new IllegalArgumentException("ruleId must not be blank");
        }
        if (message.isBlank()) {
            throw new IllegalArgumentException("message must not be blank");
        }
    }
}
