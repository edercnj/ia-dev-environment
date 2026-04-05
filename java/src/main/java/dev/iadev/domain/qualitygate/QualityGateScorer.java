package dev.iadev.domain.qualitygate;

import java.util.ArrayList;
import java.util.List;

/**
 * Computes individual criterion scores for the quality gate.
 *
 * <p>Each method evaluates one dimension and returns a score
 * between 0 and its maximum points.</p>
 */
final class QualityGateScorer {

    private QualityGateScorer() {
    }

    static List<ScenarioScore> scoreScenarios(
            List<GherkinScenario> scenarios) {
        var scores = new ArrayList<ScenarioScore>();
        for (var scenario : scenarios) {
            scores.add(ScenarioScorer.score(scenario));
        }
        return List.copyOf(scores);
    }

    static int scoreDataContract(
            List<DataContractField> fields) {
        if (fields.isEmpty()) {
            return 0;
        }
        boolean hasMandatory = fields.stream()
                .anyMatch(DataContractField::mandatory);
        return hasMandatory
                ? QualityGateEngine.DATA_CONTRACT_POINTS : 0;
    }

    static int scoreTypeExplicitness(
            List<DataContractField> fields) {
        if (fields.isEmpty()) {
            return 0;
        }
        boolean allExplicit = fields.stream()
                .noneMatch(f -> StoryMarkdownParser
                        .isGenericType(f.type()));
        return allExplicit
                ? QualityGateEngine.TYPE_EXPLICITNESS_POINTS
                : 0;
    }

    static int scoreScenarioCount(
            List<GherkinScenario> scenarios) {
        return scenarios.size()
                >= QualityGateEngine.MIN_SCENARIOS
                ? QualityGateEngine.SCENARIO_COUNT_POINTS : 0;
    }

    static int scoreVagueness(
            List<GherkinScenario> scenarios) {
        for (var scenario : scenarios) {
            var terms = VaguenessDetector
                    .checkScenario(scenario);
            if (!terms.isEmpty()) {
                return 0;
            }
        }
        return QualityGateEngine.VAGUENESS_POINTS;
    }

    static int scoreDependencies(
            List<String> dependencies,
            List<String> epicIndex) {
        if (dependencies.isEmpty()) {
            return QualityGateEngine.DEPENDENCY_POINTS;
        }
        for (var dep : dependencies) {
            if (!epicIndex.contains(dep)) {
                return 0;
            }
        }
        return QualityGateEngine.DEPENDENCY_POINTS;
    }

    static int computeRawScore(
            List<ScenarioScore> scenarioScores,
            int dataContractScore,
            int typeScore,
            int countScore,
            int vaguenessScore,
            int depScore) {
        int scenarioRaw = scenarioScores.stream()
                .mapToInt(ScenarioScore::total).sum();
        return scenarioRaw + dataContractScore
                + typeScore + countScore
                + vaguenessScore + depScore;
    }

    static int computeMaxRaw(int scenarioCount) {
        int scenarioMax = scenarioCount
                * ScenarioScore.MAX_STEP_SCORE * 3;
        return scenarioMax
                + QualityGateEngine.FIXED_CRITERIA_MAX;
    }

    static int normalize(int rawScore, int maxRaw) {
        if (maxRaw <= 0) {
            return 0;
        }
        int normalized = Math.round(
                (float) rawScore / maxRaw * 100);
        return Math.min(normalized, 100);
    }
}
