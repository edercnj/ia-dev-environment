package dev.iadev.domain.lifecycle;

import java.nio.file.Path;

/**
 * Immutable record describing a single divergence between
 * {@code execution-state.json} (checkpoint — telemetry) and
 * the canonical {@code **Status:**} field of an Epic / Story /
 * Task markdown artifact (Rule 22 — Markdown is Source of
 * Truth; state.json is telemetry).
 *
 * <p>A divergence is produced by
 * {@code LifecycleReconciler.diff(Path)}; each one carries
 * enough context for the apply phase to locate the file,
 * validate the proposed transition via
 * {@link LifecycleStatus}, and emit an audit-log entry.</p>
 *
 * @param artifactId  human-readable identifier
 *                    (e.g. {@code story-0024-0001},
 *                    {@code epic-0024},
 *                    {@code IMPLEMENTATION-MAP.md:row-5})
 * @param file        absolute path to the markdown artifact
 *                    (the implementation-map artifact always
 *                    points to {@code IMPLEMENTATION-MAP.md};
 *                    per-row updates rewrite the single file
 *                    in-place)
 * @param from        current status in the markdown (null when
 *                    the artifact has no Status line yet)
 * @param to          target status derived from
 *                    {@code execution-state.json}
 */
public record Divergence(
        String artifactId,
        Path file,
        LifecycleStatus from,
        LifecycleStatus to) {

    public Divergence {
        if (artifactId == null || artifactId.isBlank()) {
            throw new IllegalArgumentException(
                    "artifactId must not be blank");
        }
        if (file == null) {
            throw new IllegalArgumentException(
                    "file must not be null");
        }
        if (to == null) {
            throw new IllegalArgumentException(
                    "to status must not be null");
        }
        if (from != null && from == to) {
            throw new IllegalArgumentException(
                    "divergence requires from != to "
                            + "(same status is not a "
                            + "divergence)");
        }
    }
}
