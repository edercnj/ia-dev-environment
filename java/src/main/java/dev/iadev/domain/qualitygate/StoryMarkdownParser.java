package dev.iadev.domain.qualitygate;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Parses story markdown content to extract Gherkin scenarios,
 * data contract fields, and dependency references.
 *
 * <p>This parser handles the standard story markdown format
 * used by x-story-create.</p>
 */
public final class StoryMarkdownParser {

    private static final Pattern SCENARIO_ID_PATTERN =
            Pattern.compile("^@(GK-\\d+)");
    private static final Pattern STEP_GIVEN =
            Pattern.compile(
                    "^\\s*(DADO|Given|GIVEN)\\b",
                    Pattern.CASE_INSENSITIVE);
    private static final Pattern STEP_WHEN =
            Pattern.compile(
                    "^\\s*(QUANDO|When|WHEN)\\b",
                    Pattern.CASE_INSENSITIVE);
    private static final Pattern STEP_THEN =
            Pattern.compile(
                    "^\\s*(ENTAO|ENTÃO|Then|THEN)\\b",
                    Pattern.CASE_INSENSITIVE);
    private static final Pattern STEP_AND =
            Pattern.compile(
                    "^\\s*(E\\b|And\\b|AND\\b)",
                    Pattern.CASE_INSENSITIVE);

    private static final Set<String> GENERIC_TYPES = Set.of(
            "data", "object", "any", "dynamic", "var");
    private static final Set<String> EMPTY_MARKERS =
            Set.of("-", "--", "\u2014", "");
    private static final int MIN_TABLE_CELLS = 3;
    private static final String GHERKIN_SECTION =
            "criterios de aceite";
    private static final String DATA_CONTRACT_SECTION =
            "contratos de dados";
    private static final String DEPENDENCY_SECTION =
            "dependencias";

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
        var scenarios = new ArrayList<GherkinScenario>();
        int i = 0;

        i = skipToSection(lines, i, GHERKIN_SECTION);
        if (i >= lines.length) {
            return List.of();
        }

        while (i < lines.length) {
            if (isNextSection(lines[i])) {
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
        if (content == null || content.isBlank()) {
            return List.of();
        }
        var lines = content.split("\n");
        int i = skipToSection(
                lines, 0, DATA_CONTRACT_SECTION);
        if (i >= lines.length) {
            return List.of();
        }

        var fields = new ArrayList<DataContractField>();
        boolean inTable = false;
        boolean headerSkipped = false;

        while (i < lines.length) {
            var line = lines[i].trim();
            if (isNextSection(line)
                    && !sectionMatches(
                    line, DATA_CONTRACT_SECTION)) {
                break;
            }
            if (line.startsWith("|")) {
                if (!inTable) {
                    inTable = true;
                    i++;
                    continue;
                }
                if (!headerSkipped
                        && line.contains("---")) {
                    headerSkipped = true;
                    i++;
                    continue;
                }
                parseContractRow(line).ifPresent(fields::add);
            } else if (inTable && headerSkipped
                    && !line.isBlank()) {
                inTable = false;
                headerSkipped = false;
            }
            i++;
        }
        return List.copyOf(fields);
    }

    /**
     * Extracts blocked-by dependency IDs from story markdown.
     *
     * @param content the full story markdown
     * @return list of dependency IDs (empty if none found)
     */
    public static List<String> extractDependencies(
            String content) {
        if (content == null || content.isBlank()) {
            return List.of();
        }
        var lines = content.split("\n");
        int i = skipToSection(
                lines, 0, DEPENDENCY_SECTION);
        if (i >= lines.length) {
            return List.of();
        }

        while (i < lines.length) {
            var line = lines[i].trim();
            if (line.startsWith("|")
                    && !line.contains("---")
                    && !line.toLowerCase(Locale.ROOT)
                    .contains("blocked")) {
                var cells = splitTableRow(line);
                if (!cells.isEmpty()) {
                    var firstCell = cells.getFirst().trim();
                    if (!EMPTY_MARKERS.contains(firstCell)
                            && !firstCell.isBlank()) {
                        return parseDependencyIds(firstCell);
                    }
                }
            }
            i++;
        }
        return List.of();
    }

    private static int skipToSection(
            String[] lines, int start, String sectionName) {
        for (int i = start; i < lines.length; i++) {
            if (sectionMatches(lines[i], sectionName)) {
                return i + 1;
            }
        }
        return lines.length;
    }

    private static boolean sectionMatches(
            String line, String sectionName) {
        var trimmed = line.trim().toLowerCase(Locale.ROOT);
        return trimmed.startsWith("#")
                && trimmed.contains(sectionName);
    }

    private static boolean isNextSection(String line) {
        return line.trim().startsWith("#");
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
                || isNextSection(raw);
    }

    private static boolean isSkippableLine(String line) {
        return line.isBlank()
                || line.startsWith("Cenario:")
                || line.startsWith("Scenario:");
    }

    private static final class StepCollector {
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
                addToCurrentStep(
                        current, line, given, when, then);
            }
        }

        GherkinScenario build(String id) {
            return new GherkinScenario(
                    id, given, when, then);
        }
    }

    private static void addToCurrentStep(
            StepType currentStep,
            String line,
            List<String> givenLines,
            List<String> whenLines,
            List<String> thenLines) {
        switch (currentStep) {
            case GIVEN -> givenLines.add(line);
            case WHEN -> whenLines.add(line);
            case THEN -> thenLines.add(line);
        }
    }

    private static java.util.Optional<DataContractField>
    parseContractRow(String line) {
        var cells = splitTableRow(line);
        if (cells.size() < MIN_TABLE_CELLS) {
            return java.util.Optional.empty();
        }
        var name = stripMarkdown(cells.get(0).trim());
        var rawType = cells.get(1).trim();
        var type = stripMarkdown(rawType);
        var mandatoryCell = cells.get(2).trim();
        boolean mandatory = "M".equalsIgnoreCase(mandatoryCell);

        if (name.isBlank() || EMPTY_MARKERS.contains(name)) {
            return java.util.Optional.empty();
        }

        return java.util.Optional.of(
                new DataContractField(name, type, mandatory));
    }

    private static String stripMarkdown(String text) {
        return text.replaceAll("[`*_]", "")
                .replaceAll("<[^>]+>", "")
                .replaceAll("&lt;", "<")
                .replaceAll("&gt;", ">")
                .trim();
    }

    /**
     * Checks if a field type is generic or missing.
     *
     * @param type the declared type
     * @return true if the type is considered generic
     */
    public static boolean isGenericType(String type) {
        if (type == null || type.isBlank()) {
            return true;
        }
        return GENERIC_TYPES.contains(
                type.toLowerCase(Locale.ROOT).trim());
    }

    private static List<String> splitTableRow(String line) {
        var parts = line.split("\\|");
        var cells = new ArrayList<String>();
        for (int i = 1; i < parts.length; i++) {
            var trimmed = parts[i].trim();
            if (!trimmed.isEmpty() || i < parts.length - 1) {
                cells.add(trimmed);
            }
        }
        return cells;
    }

    private static List<String> parseDependencyIds(
            String cell) {
        var ids = new ArrayList<String>();
        for (var id : cell.split(",")) {
            var stripped = id.trim();
            if (!stripped.isEmpty()
                    && !EMPTY_MARKERS.contains(stripped)) {
                ids.add(stripped);
            }
        }
        return List.copyOf(ids);
    }

    private record ParsedScenarioResult(
            GherkinScenario scenario,
            int nextIndex) {
    }
}
