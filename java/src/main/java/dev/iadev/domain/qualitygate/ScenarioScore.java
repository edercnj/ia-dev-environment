package dev.iadev.domain.qualitygate;

import java.util.List;

/**
 * Detailed score for a single Gherkin scenario.
 *
 * @param scenarioId the scenario identifier (e.g., "@GK-1")
 * @param givenScore 0 or 5
 * @param whenScore  0 or 5
 * @param thenScore  0 or 5
 * @param issues     problems detected in this scenario
 */
public record ScenarioScore(
        String scenarioId,
        int givenScore,
        int whenScore,
        int thenScore,
        List<String> issues
) {

    public static final int MAX_STEP_SCORE = 5;

    public ScenarioScore {
        issues = List.copyOf(issues);
    }

    /** Returns the total score for this scenario (0-15). */
    public int total() {
        return givenScore + whenScore + thenScore;
    }
}
