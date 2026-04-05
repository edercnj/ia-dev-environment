package dev.iadev.domain.qualitygate;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Parses story markdown content to extract Gherkin scenarios,
 * data contract fields, and dependency references.
 *
 * <p>Gherkin scenario parsing is handled inline.
 * Data contracts and dependencies delegate to
 * {@link MarkdownTableParser}.</p>
 */
public final class StoryMarkdownParser {

    private static final Pattern SCENARIO_ID_PATTERN =
            Pattern.compile("^@(GK-\\d+)");
    static final Pattern STEP_GIVEN =
            Pattern.compile(
                    "^\\s*(DADO|Given|GIVEN)\\b",
                    Pattern.CASE_INSENSITIVE);
    static final Pattern STEP_WHEN =
            Pattern.compile(
                    "^\\s*(QUANDO|When|WHEN)\\b",
                    Pattern.CASE_INSENSITIVE);
    static final Pattern STEP_THEN =
            Pattern.compile(
                    "^\\s*(ENTAO|ENTÃO|Then|THEN)\\b",
                    Pattern.CASE_INSENSITIVE);
    static final Pattern STEP_AND =
            Pattern.compile(
                    "^\\s*(E\\b|And\\b|AND\\b)",
                    Pattern.CASE_INSENSITIVE);

    private static final String GHERKIN_SECTION =
            "criterios de aceite";

    private StoryMarkdownParser() {
    }

    /**
     * Extracts Gherkin scenarios from story markdown.
     *
     * @param content the full story markdown
     * @return list of parsed scenarios (empty if none found)
     */
    public static List<GherkinScenario> extractScenarios(
            String content) {
        if (content == null || content.isBlank()) {
            return List.of();
        }
        var lines = content.split("\n");
        int i = SectionNavigator.skipToSection(
                lines, 0, GHERKIN_SECTION);
        if (i >= lines.length) {
            return List.of();
        }

        var scenarios = new ArrayList<GherkinScenario>();
        while (i < lines.length) {
            if (SectionNavigator.isNextSection(lines[i])) {
                break;
            }
            var idMatch = SCENARIO_ID_PATTERN.matcher(
                    lines[i].trim());
            if (idMatch.find()) {
                var parsed = parseScenario(
                        lines, i, idMatch.group(0));
                scenarios.add(parsed.scenario());
                i = parsed.nextIndex();
            } else {
                i++;
            }
        }
        return List.copyOf(scenarios);
    }

    /**
     * Extracts data contract fields from story markdown.
     *
     * @param content the full story markdown
     * @return list of parsed fields (empty if none found)
     */
    public static List<DataContractField> extractDataContract(
            String content) {
        return MarkdownTableParser
                .extractDataContract(content);
    }

    /**
     * Extracts blocked-by dependency IDs from story markdown.
     *
     * @param content the full story markdown
     * @return list of dependency IDs (empty if none found)
     */
    public static List<String> extractDependencies(
            String content) {
        return MarkdownTableParser
                .extractDependencies(content);
    }

    /**
     * Checks if a field type is generic or missing.
     *
     * @param type the declared type
     * @return true if the type is considered generic
     */
    public static boolean isGenericType(String type) {
        return MarkdownTableParser.isGenericType(type);
    }

    private static ParsedScenarioResult parseScenario(
            String[] lines, int startIndex,
            String scenarioId) {
        var collector = new StepCollector();
        int i = startIndex + 1;

        while (i < lines.length) {
            var line = lines[i].trim();
            if (isScenarioBoundary(line, lines[i])) {
                break;
            }
            if (!isSkippableLine(line)) {
                collector.addLine(line);
            }
            i++;
        }

        return new ParsedScenarioResult(
                collector.build(scenarioId), i);
    }

    private static boolean isScenarioBoundary(
            String trimmed, String raw) {
        return trimmed.startsWith("@GK-")
                || SectionNavigator.isNextSection(raw);
    }

    private static boolean isSkippableLine(String line) {
        return line.isBlank()
                || line.startsWith("Cenario:")
                || line.startsWith("Scenario:");
    }

    static final class StepCollector {
        private final List<String> given = new ArrayList<>();
        private final List<String> when = new ArrayList<>();
        private final List<String> then = new ArrayList<>();
        private StepType current;

        void addLine(String line) {
            if (STEP_GIVEN.matcher(line).find()) {
                current = StepType.GIVEN;
                given.add(line);
            } else if (STEP_WHEN.matcher(line).find()) {
                current = StepType.WHEN;
                when.add(line);
            } else if (STEP_THEN.matcher(line).find()) {
                current = StepType.THEN;
                then.add(line);
            } else if (STEP_AND.matcher(line).find()
                    && current != null) {
                addAndStep(line);
            }
        }

        private void addAndStep(String line) {
            switch (current) {
                case GIVEN -> given.add(line);
                case WHEN -> when.add(line);
                case THEN -> then.add(line);
            }
        }

        GherkinScenario build(String id) {
            return new GherkinScenario(
                    id, given, when, then);
        }
    }

    private record ParsedScenarioResult(
            GherkinScenario scenario,
            int nextIndex) {
    }
}
