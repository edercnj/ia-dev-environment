package dev.iadev.domain.qualitygate;

import java.util.ArrayList;
import java.util.List;

/**
 * Quality Gate scoring engine for story specifications.
 *
 * <p>Evaluates story markdown content against quality criteria
 * and produces a normalized score (0-100). Stories below the
 * threshold are rejected with actionable feedback.</p>
 *
 * @see QualityGateScorer
 * @see QualityGateResult
 */
public final class QualityGateEngine {

    static final int DATA_CONTRACT_POINTS = 10;
    static final int TYPE_EXPLICITNESS_POINTS = 10;
    static final int SCENARIO_COUNT_POINTS = 10;
    static final int VAGUENESS_POINTS = 15;
    static final int DEPENDENCY_POINTS = 10;
    static final int MIN_SCENARIOS = 4;
    static final int FIXED_CRITERIA_MAX =
            DATA_CONTRACT_POINTS + TYPE_EXPLICITNESS_POINTS
                    + SCENARIO_COUNT_POINTS + VAGUENESS_POINTS
                    + DEPENDENCY_POINTS;

    private QualityGateEngine() {
    }

    /**
     * Evaluates a story markdown for quality.
     *
     * @param storyContent     full markdown content
     * @param epicIndexEntries IDs of stories in the epic index
     * @param threshold        minimum score for approval
     * @return quality gate result with scores and action items
     */
    public static QualityGateResult evaluate(
            String storyContent,
            List<String> epicIndexEntries,
            int threshold) {
        var scenarios = StoryMarkdownParser
                .extractScenarios(storyContent);

        if (scenarios.isEmpty()) {
            return buildEmptyResult(threshold);
        }

        var parsed = parseContent(
                storyContent, epicIndexEntries);
        var scores = computeScores(parsed, scenarios);

        return buildResult(scores, parsed, threshold);
    }

    private static ParsedContent parseContent(
            String storyContent,
            List<String> epicIndexEntries) {
        return new ParsedContent(
                StoryMarkdownParser
                        .extractDataContract(storyContent),
                StoryMarkdownParser
                        .extractDependencies(storyContent),
                epicIndexEntries);
    }

    private static ComputedScores computeScores(
            ParsedContent parsed,
            List<GherkinScenario> scenarios) {
        var ss = QualityGateScorer.scoreScenarios(scenarios);
        int dc = QualityGateScorer
                .scoreDataContract(parsed.fields());
        int tp = QualityGateScorer
                .scoreTypeExplicitness(parsed.fields());
        int ct = QualityGateScorer
                .scoreScenarioCount(scenarios);
        int vg = QualityGateScorer
                .scoreVagueness(scenarios);
        int dp = QualityGateScorer.scoreDependencies(
                parsed.dependencies(), parsed.epicIndex());
        int raw = QualityGateScorer.computeRawScore(
                ss, dc, tp, ct, vg, dp);
        int maxRaw = QualityGateScorer
                .computeMaxRaw(scenarios.size());
        return new ComputedScores(
                ss, dc, tp, ct, vg, dp,
                QualityGateScorer.normalize(raw, maxRaw));
    }

    private static QualityGateResult buildResult(
            ComputedScores scores,
            ParsedContent parsed,
            int threshold) {
        return new QualityGateResult(
                scores.normalized(), threshold,
                scores.normalized() >= threshold,
                scores.scenarioScores(),
                scores.dataContractScore(),
                scores.typeScore(),
                scores.countScore(),
                scores.vaguenessScore(),
                scores.depScore(),
                buildActionItems(scores, parsed));
    }

    private static QualityGateResult buildEmptyResult(
            int threshold) {
        return new QualityGateResult(
                0, threshold, false,
                List.of(), 0, 0, 0, 0, 0,
                List.of("No Gherkin scenarios found"
                        + " — minimum 4 required"));
    }

    private static List<String> buildActionItems(
            ComputedScores scores,
            ParsedContent parsed) {
        var items = new ArrayList<String>();
        for (var s : scores.scenarioScores()) {
            items.addAll(s.issues());
        }
        addContractItems(items, parsed.fields());
        addCountItem(items, scores);
        addVaguenessItem(items, scores);
        addDependencyItems(
                items, parsed.dependencies(),
                parsed.epicIndex());
        return List.copyOf(items);
    }

    private static void addContractItems(
            List<String> items,
            List<DataContractField> fields) {
        if (fields.isEmpty()) {
            return;
        }
        boolean hasMandatory = fields.stream()
                .anyMatch(DataContractField::mandatory);
        if (!hasMandatory) {
            items.add("Data contract: add at least "
                    + "1 mandatory (M) field");
        }
        var generic = fields.stream()
                .filter(f -> StoryMarkdownParser
                        .isGenericType(f.type()))
                .toList();
        if (!generic.isEmpty()) {
            items.add("Data contract: %d field(s) "
                    .formatted(generic.size())
                    + "without explicit type");
        }
    }

    private static void addCountItem(
            List<String> items, ComputedScores scores) {
        if (scores.countScore() == 0
                && !scores.scenarioScores().isEmpty()) {
            items.add("Only %d scenario(s) found"
                    .formatted(scores.scenarioScores().size())
                    + " — minimum 4 required for "
                    + "TPP coverage");
        }
    }

    private static void addVaguenessItem(
            List<String> items, ComputedScores scores) {
        if (scores.vaguenessScore() == 0) {
            items.add("Vague language detected"
                    + " in scenarios");
        }
    }

    private static void addDependencyItems(
            List<String> items,
            List<String> dependencies,
            List<String> epicIndex) {
        for (var dep : dependencies) {
            if (!epicIndex.contains(dep)) {
                items.add("Dependency '%s' ".formatted(dep)
                        + "not found in epic index");
            }
        }
    }
}
