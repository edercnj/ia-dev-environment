package dev.iadev.domain.qualitygate;

import java.util.List;

/**
 * Result of the quality gate evaluation.
 *
 * @param score                normalized score (0-100)
 * @param threshold            threshold used for pass/fail
 * @param passed               true if score >= threshold
 * @param scenarioScores       detailed per-scenario scores
 * @param dataContractScore    points for data contract (0-10)
 * @param typeExplicitnessScore points for explicit types (0-10)
 * @param scenarioCountScore   points for scenario count (0-10)
 * @param vaguenessScore       points for no vague language (0-15)
 * @param dependencyScore      points for valid dependencies (0-10)
 * @param actionItems          actionable fix suggestions
 */
public record QualityGateResult(
        int score,
        int threshold,
        boolean passed,
        List<ScenarioScore> scenarioScores,
        int dataContractScore,
        int typeExplicitnessScore,
        int scenarioCountScore,
        int vaguenessScore,
        int dependencyScore,
        List<String> actionItems
) {

    public QualityGateResult {
        scenarioScores = List.copyOf(scenarioScores);
        actionItems = List.copyOf(actionItems);
    }
}
