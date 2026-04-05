package dev.iadev.domain.qualitygate;

/**
 * Formats a {@link QualityGateResult} into a human-readable
 * quality gate report string.
 */
public final class ReportFormatter {

    private static final String HEADER_FORMAT =
            "=== Spec Quality Gate — %s ===";
    private static final String SCORE_FORMAT =
            "Score: %d/100 (threshold: %d) — %s";
    private static final String PASSED_LABEL = "PASSED";
    private static final String REJECTED_LABEL = "REJECTED";

    private ReportFormatter() {
    }

    /**
     * Formats the quality gate result as a text report.
     *
     * @param storyId the story identifier
     * @param result  the evaluation result
     * @return formatted report string
     */
    public static String format(
            String storyId, QualityGateResult result) {
        var sb = new StringBuilder();
        appendHeader(sb, storyId, result);
        appendBreakdown(sb, result);
        appendActionItems(sb, result);
        return sb.toString();
    }

    private static void appendHeader(
            StringBuilder sb,
            String storyId,
            QualityGateResult result) {
        sb.append(HEADER_FORMAT.formatted(storyId));
        sb.append('\n');
        sb.append(SCORE_FORMAT.formatted(
                result.score(),
                result.threshold(),
                result.passed()
                        ? PASSED_LABEL : REJECTED_LABEL));
        sb.append('\n');
    }

    private static void appendBreakdown(
            StringBuilder sb, QualityGateResult result) {
        sb.append('\n');
        sb.append("Breakdown:\n");
        appendScenarios(sb, result);
        appendFixedScore(sb, "Data Contract",
                result.dataContractScore(), 10);
        appendFixedScore(sb, "Types",
                result.typeExplicitnessScore(), 10);
        appendFixedScore(sb, "Scenario Count",
                result.scenarioCountScore(), 10);
        appendFixedScore(sb, "Vagueness",
                result.vaguenessScore(), 15);
        appendFixedScore(sb, "Dependencies",
                result.dependencyScore(), 10);
    }

    private static void appendScenarios(
            StringBuilder sb, QualityGateResult result) {
        var scenarios = result.scenarioScores();
        int rawTotal = scenarios.stream()
                .mapToInt(ScenarioScore::total).sum();
        int maxRaw = scenarios.size()
                * (ScenarioScore.MAX_STEP_SCORE * 3);
        sb.append("  Scenarios (%d): %d/%d\n".formatted(
                scenarios.size(), rawTotal, maxRaw));
        for (var s : scenarios) {
            sb.append("    %s: ".formatted(s.scenarioId()));
            sb.append(stepLabel("Given", s.givenScore()));
            sb.append(" | ");
            sb.append(stepLabel("When", s.whenScore()));
            sb.append(" | ");
            sb.append(stepLabel("Then", s.thenScore()));
            if (!s.issues().isEmpty()) {
                sb.append(" — ");
                sb.append(s.issues().getFirst());
            }
            sb.append('\n');
        }
    }

    private static String stepLabel(
            String name, int score) {
        var icon = score > 0 ? "\u2705" : "\u274C";
        return "%s %s (%d)".formatted(name, icon, score);
    }

    private static void appendFixedScore(
            StringBuilder sb,
            String label,
            int score,
            int max) {
        sb.append("  %s: %d/%d\n".formatted(
                label, score, max));
    }

    private static void appendActionItems(
            StringBuilder sb, QualityGateResult result) {
        if (result.actionItems().isEmpty()) {
            return;
        }
        sb.append('\n');
        sb.append("Action Items:\n");
        int index = 1;
        for (var item : result.actionItems()) {
            sb.append("  %d. %s\n".formatted(index, item));
            index++;
        }
    }
}
