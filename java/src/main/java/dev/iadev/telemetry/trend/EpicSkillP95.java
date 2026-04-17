package dev.iadev.telemetry.trend;

import java.util.Objects;

/**
 * One point in the per-skill P95 series: for a given {@code skill} and
 * {@code epicId}, the {@code p95Ms} value is the Nearest-Rank P95 of all
 * {@code skill.end} {@code durationMs} samples observed in that epic's
 * NDJSON log.
 *
 * <p>Produced by {@link TelemetryIndexBuilder} and consumed by
 * {@link RegressionDetector} and {@link SlowestSkillsAggregator}.</p>
 *
 * @param epicId       the epic ID (e.g. {@code EPIC-0040})
 * @param skill        the skill name (kebab-case)
 * @param p95Ms        the epic-scoped P95 in milliseconds
 * @param invocations  the count of samples that produced this P95
 */
public record EpicSkillP95(
        String epicId,
        String skill,
        long p95Ms,
        long invocations) {

    /**
     * Canonical constructor with fail-fast validation.
     *
     * @throws IllegalArgumentException when any string is blank or any
     *                                  numeric field is negative
     */
    public EpicSkillP95 {
        Objects.requireNonNull(epicId, "epicId is required");
        Objects.requireNonNull(skill, "skill is required");
        if (epicId.isBlank()) {
            throw new IllegalArgumentException(
                    "epicId must not be blank");
        }
        if (skill.isBlank()) {
            throw new IllegalArgumentException(
                    "skill must not be blank");
        }
        if (p95Ms < 0) {
            throw new IllegalArgumentException(
                    "p95Ms must be >= 0");
        }
        if (invocations < 0) {
            throw new IllegalArgumentException(
                    "invocations must be >= 0");
        }
    }
}
