package dev.iadev.telemetry.analyze;

import java.time.Instant;
import java.util.Objects;

/**
 * Single row of the Mermaid Gantt timeline in a telemetry report.
 *
 * <p>Produced by {@link TelemetryAggregator} from pairs of
 * {@code phase.start} / {@code phase.end} events that share the same
 * {@code (sessionId, skill, phase)} triple. A phase without a matching end
 * event is synthesized with {@code durationMs == 0} and {@link #endInstant()}
 * equal to {@link #startInstant()} so the Gantt row collapses to a point.</p>
 *
 * <p>Immutable value object.</p>
 */
public record PhaseTimeline(
        String skill,
        String phase,
        Instant startInstant,
        Instant endInstant,
        long durationMs) {

    /**
     * Canonical constructor applying fail-fast validation.
     *
     * @throws NullPointerException     when skill, phase, startInstant, or
     *                                  endInstant is null
     * @throws IllegalArgumentException when duration is negative
     */
    public PhaseTimeline {
        Objects.requireNonNull(skill, "skill is required");
        Objects.requireNonNull(phase, "phase is required");
        Objects.requireNonNull(startInstant,
                "startInstant is required");
        Objects.requireNonNull(endInstant, "endInstant is required");
        if (durationMs < 0) {
            throw new IllegalArgumentException(
                    "durationMs must be >= 0");
        }
    }
}
