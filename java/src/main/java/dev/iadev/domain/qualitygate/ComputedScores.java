package dev.iadev.domain.qualitygate;

import java.util.List;

/**
 * Computed scores from quality gate evaluation.
 */
record ComputedScores(
        List<ScenarioScore> scenarioScores,
        int dataContractScore,
        int typeScore,
        int countScore,
        int vaguenessScore,
        int depScore,
        int normalized) {
}
