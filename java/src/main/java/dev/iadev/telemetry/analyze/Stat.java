package dev.iadev.telemetry.analyze;

import java.util.List;
import java.util.Objects;

/**
 * Aggregated statistics for a named dimension (skill, phase, or tool) in
 * telemetry analysis.
 *
 * <p>Computed by {@link TelemetryAggregator} by reducing a stream of
 * {@code skill.end} / {@code phase.end} / {@code tool.result} events whose
 * {@code durationMs} field is present. {@link #p50Ms()} and {@link #p95Ms()}
 * are calculated from the sorted duration samples — percentiles are
 * Nearest-Rank (ceil) per ISO 3534-1:2006 §1.7 to keep the arithmetic
 * integer-stable (no floating-point interpolation).</p>
 *
 * <p>Instances are immutable value objects; {@link #epicIds()} is defensively
 * copied in the canonical constructor.</p>
 */
public record Stat(
        String name,
        int invocations,
        long totalMs,
        long avgMs,
        long p50Ms,
        long p95Ms,
        List<String> epicIds) {

    /**
     * Canonical constructor applying fail-fast validation and defensive
     * copying of {@code epicIds}.
     *
     * @throws IllegalArgumentException when name is null/blank or numeric
     *                                  fields are negative
     */
    public Stat {
        Objects.requireNonNull(name, "name is required");
        if (name.isBlank()) {
            throw new IllegalArgumentException(
                    "name must not be blank");
        }
        if (invocations < 0) {
            throw new IllegalArgumentException(
                    "invocations must be >= 0");
        }
        if (totalMs < 0) {
            throw new IllegalArgumentException(
                    "totalMs must be >= 0");
        }
        if (avgMs < 0) {
            throw new IllegalArgumentException(
                    "avgMs must be >= 0");
        }
        if (p50Ms < 0) {
            throw new IllegalArgumentException(
                    "p50Ms must be >= 0");
        }
        if (p95Ms < 0) {
            throw new IllegalArgumentException(
                    "p95Ms must be >= 0");
        }
        epicIds = epicIds == null
                ? List.of()
                : List.copyOf(epicIds);
    }
}
