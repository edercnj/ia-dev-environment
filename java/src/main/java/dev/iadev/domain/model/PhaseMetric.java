package dev.iadev.domain.model;

import java.time.Instant;
import java.util.Objects;

/**
 * Immutable value object representing a single phase
 * execution telemetry record, written as one line in
 * {@code plans/release-metrics.jsonl} (story-0039-0012 §3.1).
 *
 * <p>Fields match the schema table in §5.1 of the story:
 * {@code releaseVersion}, {@code releaseType},
 * {@code phase}, {@code startedAt}, {@code endedAt},
 * {@code durationSec}, {@code outcome}. The record is
 * a pure domain value object (zero framework imports on
 * type usage — serialization is performed by the adapter
 * layer via Jackson).
 */
public record PhaseMetric(
        String releaseVersion,
        ReleaseType releaseType,
        String phase,
        Instant startedAt,
        Instant endedAt,
        long durationSec,
        PhaseOutcome outcome) {

    /**
     * Canonical constructor with strict validation.
     *
     * @throws IllegalArgumentException if any mandatory
     *         field is null/blank or if
     *         {@code durationSec < 0}
     */
    public PhaseMetric {
        requireNonBlank(releaseVersion, "releaseVersion");
        Objects.requireNonNull(releaseType, "releaseType");
        requireNonBlank(phase, "phase");
        Objects.requireNonNull(startedAt, "startedAt");
        Objects.requireNonNull(endedAt, "endedAt");
        Objects.requireNonNull(outcome, "outcome");
        if (durationSec < 0) {
            throw new IllegalArgumentException(
                    "durationSec must be >= 0, was "
                            + durationSec);
        }
    }

    private static void requireNonBlank(
            String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(
                    field + " must not be null or blank");
        }
    }
}
