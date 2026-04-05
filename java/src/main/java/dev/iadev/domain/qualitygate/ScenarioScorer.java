package dev.iadev.domain.qualitygate;

import java.util.ArrayList;
import java.util.List;

/**
 * Scores a single Gherkin scenario based on step quality.
 *
 * <p>Each step (Given, When, Then) earns 5 points if it
 * contains no vague terms. A step with vague terms scores 0.</p>
 */
public final class ScenarioScorer {

    private ScenarioScorer() {
    }

    /**
     * Scores a single Gherkin scenario.
     *
     * @param scenario the scenario to score
     * @return detailed score with issues
     */
    public static ScenarioScore score(
            GherkinScenario scenario) {
        var issues = new ArrayList<String>();

        int givenScore = scoreStep(
                scenario.givenLines(), StepType.GIVEN,
                scenario.scenarioId(), issues);
        int whenScore = scoreStep(
                scenario.whenLines(), StepType.WHEN,
                scenario.scenarioId(), issues);
        int thenScore = scoreStep(
                scenario.thenLines(), StepType.THEN,
                scenario.scenarioId(), issues);

        return new ScenarioScore(
                scenario.scenarioId(),
                givenScore, whenScore, thenScore,
                List.copyOf(issues));
    }

    private static int scoreStep(
            List<String> lines,
            StepType stepType,
            String scenarioId,
            List<String> issues) {
        for (var line : lines) {
            var terms = VaguenessDetector.check(
                    line, stepType);
            if (!terms.isEmpty()) {
                var firstTerm = terms.getFirst().term();
                issues.add(formatIssue(
                        scenarioId, stepType, firstTerm));
                return 0;
            }
        }
        return ScenarioScore.MAX_STEP_SCORE;
    }

    private static String formatIssue(
            String scenarioId,
            StepType stepType,
            String term) {
        var suggestion = buildSuggestion(stepType);
        return "%s %s: '%s' is %s — %s".formatted(
                scenarioId,
                stepType.name().charAt(0)
                        + stepType.name()
                        .substring(1).toLowerCase(),
                term,
                describeIssue(stepType),
                suggestion);
    }

    private static String describeIssue(StepType stepType) {
        return switch (stepType) {
            case GIVEN -> "vague";
            case WHEN -> "vague";
            case THEN -> "non-verifiable";
        };
    }

    private static String buildSuggestion(StepType stepType) {
        return switch (stepType) {
            case GIVEN ->
                    "specify configuration state "
                            + "with concrete values";
            case WHEN ->
                    "describe single, specific action";
            case THEN ->
                    "specify exact HTTP status, "
                            + "field name, or value";
        };
    }
}
