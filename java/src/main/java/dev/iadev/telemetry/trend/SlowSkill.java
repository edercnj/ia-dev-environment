package dev.iadev.telemetry.trend;

import java.util.Objects;

/**
 * A single entry in the top-10 slowest-skills ranking produced by
 * {@link SlowestSkillsAggregator}.
 *
 * <p>{@code avgP95Ms} is the arithmetic mean of per-epic P95 samples for this
 * skill across the analyzed window — it is NOT itself a percentile. The
 * "avg-of-P95s" framing lets the ranking reflect sustained tail latency rather
 * than a single peak.</p>
 *
 * @param skill        the skill name (kebab-case)
 * @param avgP95Ms     the mean of per-epic P95 durations, in milliseconds
 * @param invocations  the total count of {@code skill.end} events contributing
 *                     to the P95 samples across all analyzed epics
 */
public record SlowSkill(
        String skill,
        long avgP95Ms,
        long invocations) {

    /**
     * Canonical constructor with fail-fast validation.
     *
     * @throws IllegalArgumentException when {@code skill} is blank or any
     *                                  numeric field is negative
     */
    public SlowSkill {
        Objects.requireNonNull(skill, "skill is required");
        if (skill.isBlank()) {
            throw new IllegalArgumentException(
                    "skill must not be blank");
        }
        if (avgP95Ms < 0) {
            throw new IllegalArgumentException(
                    "avgP95Ms must be >= 0");
        }
        if (invocations < 0) {
            throw new IllegalArgumentException(
                    "invocations must be >= 0");
        }
    }
}
