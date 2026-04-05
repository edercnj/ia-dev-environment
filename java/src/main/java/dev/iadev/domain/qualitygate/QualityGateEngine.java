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
 * <h3>Scoring Criteria</h3>
 * <ul>
 *   <li>Per-scenario Given/When/Then: 5 pts each (15/scenario)
 *   <li>Data contract mandatory fields: 10 pts
 *   <li>Explicit types in all fields: 10 pts
 *   <li>4+ scenarios (TPP coverage): 10 pts
 *   <li>No vague language globally: 15 pts
 *   <li>Valid dependencies: 10 pts
 * </ul>
 *
 * <p>Score is normalized to 100 maximum.</p>
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
    private static final int MAX_SCORE = 100;

    private QualityGateEngine() {
    }

    /**
     * Evaluates a story markdown for quality.
     *
     * @param storyContent    full markdown content
     * @param epicIndexEntries IDs of stories in the epic index
     * @param threshold       minimum score for approval
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

        var fields = StoryMarkdownParser
                .extractDataContract(storyContent);
        var dependencies = StoryMarkdownParser
                .extractDependencies(storyContent);

        var scenarioScores = scoreScenarios(scenarios);
        int dataContractScore =
                scoreDataContract(fields);
        int typeScore = scoreTypeExplicitness(fields);
        int countScore = scoreScenarioCount(scenarios);
        int vaguenessScore =
                scoreVagueness(scenarios);
        int depScore = scoreDependencies(
                dependencies, epicIndexEntries);

        int rawScore = computeRawScore(
                scenarioScores, dataContractScore,
                typeScore, countScore,
                vaguenessScore, depScore);
        int maxRaw = computeMaxRaw(scenarios.size());
        int normalized = normalize(rawScore, maxRaw);

        var actionItems = buildActionItems(
                scenarioScores, scenarios,
                fields, dependencies,
                epicIndexEntries, countScore,
                vaguenessScore);

        return new QualityGateResult(
                normalized, threshold,
                normalized >= threshold,
                scenarioScores, dataContractScore,
                typeScore, countScore,
                vaguenessScore, depScore,
                actionItems);
    }

    private static QualityGateResult buildEmptyResult(
            int threshold) {
        return new QualityGateResult(
                0, threshold, false,
                List.of(), 0, 0, 0, 0, 0,
                List.of("No Gherkin scenarios found"
                        + " — minimum 4 required"));
    }

    private static List<ScenarioScore> scoreScenarios(
            List<GherkinScenario> scenarios) {
        var scores = new ArrayList<ScenarioScore>();
        for (var scenario : scenarios) {
            scores.add(ScenarioScorer.score(scenario));
        }
        return List.copyOf(scores);
    }

    private static int scoreDataContract(
            List<DataContractField> fields) {
        if (fields.isEmpty()) {
            return 0;
        }
        boolean hasMandatory = fields.stream()
                .anyMatch(DataContractField::mandatory);
        return hasMandatory ? DATA_CONTRACT_POINTS : 0;
    }

    private static int scoreTypeExplicitness(
            List<DataContractField> fields) {
        if (fields.isEmpty()) {
            return 0;
        }
        boolean allExplicit = fields.stream()
                .noneMatch(f -> StoryMarkdownParser
                        .isGenericType(f.type()));
        return allExplicit ? TYPE_EXPLICITNESS_POINTS : 0;
    }

    private static int scoreScenarioCount(
            List<GherkinScenario> scenarios) {
        return scenarios.size() >= MIN_SCENARIOS
                ? SCENARIO_COUNT_POINTS : 0;
    }

    private static int scoreVagueness(
            List<GherkinScenario> scenarios) {
        for (var scenario : scenarios) {
            var terms = VaguenessDetector
                    .checkScenario(scenario);
            if (!terms.isEmpty()) {
                return 0;
            }
        }
        return VAGUENESS_POINTS;
    }

    private static int scoreDependencies(
            List<String> dependencies,
            List<String> epicIndex) {
        if (dependencies.isEmpty()) {
            return DEPENDENCY_POINTS;
        }
        for (var dep : dependencies) {
            if (!epicIndex.contains(dep)) {
                return 0;
            }
        }
        return DEPENDENCY_POINTS;
    }

    private static int computeRawScore(
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
        return scenarioMax + FIXED_CRITERIA_MAX;
    }

    static int normalize(int rawScore, int maxRaw) {
        if (maxRaw <= 0) {
            return 0;
        }
        int normalized = Math.round(
                (float) rawScore / maxRaw * MAX_SCORE);
        return Math.min(normalized, MAX_SCORE);
    }

    private static List<String> buildActionItems(
            List<ScenarioScore> scenarioScores,
            List<GherkinScenario> scenarios,
            List<DataContractField> fields,
            List<String> dependencies,
            List<String> epicIndex,
            int countScore,
            int vaguenessScore) {
        var items = new ArrayList<String>();
        addScenarioActionItems(items, scenarioScores);
        addDataContractActionItems(items, fields);
        addTypeActionItems(items, fields);
        addCountActionItems(items, scenarios, countScore);
        addVaguenessActionItems(
                items, scenarios, vaguenessScore);
        addDependencyActionItems(
                items, dependencies, epicIndex);
        if (scenarios.isEmpty()) {
            items.add("No Gherkin scenarios found"
                    + " — minimum 4 required");
        }
        return List.copyOf(items);
    }

    private static void addScenarioActionItems(
            List<String> items,
            List<ScenarioScore> scores) {
        for (var s : scores) {
            items.addAll(s.issues());
        }
    }

    private static void addDataContractActionItems(
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
    }

    private static void addTypeActionItems(
            List<String> items,
            List<DataContractField> fields) {
        var genericFields = fields.stream()
                .filter(f -> StoryMarkdownParser
                        .isGenericType(f.type()))
                .toList();
        if (!genericFields.isEmpty()) {
            items.add("Data contract: %d field(s) "
                    .formatted(genericFields.size())
                    + "without explicit type");
        }
    }

    private static void addCountActionItems(
            List<String> items,
            List<GherkinScenario> scenarios,
            int countScore) {
        if (countScore == 0 && !scenarios.isEmpty()) {
            items.add("Only %d scenario(s) found"
                    .formatted(scenarios.size())
                    + " — minimum 4 required for "
                    + "TPP coverage");
        }
    }

    private static void addVaguenessActionItems(
            List<String> items,
            List<GherkinScenario> scenarios,
            int vaguenessScore) {
        if (vaguenessScore == 0) {
            int count = 0;
            for (var scenario : scenarios) {
                count += VaguenessDetector
                        .checkScenario(scenario).size();
            }
            if (count > 0) {
                items.add("%d vague term(s) detected"
                        .formatted(count)
                        + " across scenarios");
            }
        }
    }

    private static void addDependencyActionItems(
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
