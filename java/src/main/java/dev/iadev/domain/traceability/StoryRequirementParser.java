package dev.iadev.domain.traceability;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Pattern;

/**
 * Parses story markdown files to extract Gherkin scenario
 * requirements and their acceptance test linkages.
 *
 * <p>Extracts {@code @GK-N} identifiers and scenario titles
 * from section 7 (Criterios de Aceite / Acceptance Criteria),
 * and maps them to {@code AT-N} identifiers from section 8
 * (Sub-tarefas).</p>
 */
public final class StoryRequirementParser {

    private static final Pattern GHERKIN_ID_PATTERN =
            Pattern.compile("^@GK-(\\d+)\\s*$");
    private static final Pattern SCENARIO_TITLE_PATTERN =
            Pattern.compile(
                    "^(?:Cenario|Scenario):\\s*(.+)$");
    private static final Pattern SECTION_HEADER_PATTERN =
            Pattern.compile("^##\\s+\\d+");
    private static final Pattern SECTION_7_PATTERN =
            Pattern.compile(
                    "^##\\s+7\\.\\s+");
    private static final Pattern SECTION_8_PATTERN =
            Pattern.compile(
                    "^##\\s+8\\.\\s+");
    private static final Pattern AT_MAPPING_PATTERN =
            Pattern.compile(
                    "\\[AT-(\\d+)]\\s+@GK-(\\d+)");

    private StoryRequirementParser() {
    }

    /**
     * Parses markdown content to extract story requirements.
     *
     * @param markdown the full story markdown content
     * @return immutable list of extracted requirements
     */
    public static List<StoryRequirement> parse(
            String markdown) {
        if (markdown == null || markdown.isBlank()) {
            return List.of();
        }

        var lines = markdown.split("\n");
        var gherkinScenarios = extractGherkinScenarios(lines);
        if (gherkinScenarios.isEmpty()) {
            return List.of();
        }

        var atMappings = extractAtMappings(lines);
        return buildRequirements(gherkinScenarios, atMappings);
    }

    private static List<GherkinScenario> extractGherkinScenarios(
            String[] lines) {
        var scenarios = new ArrayList<GherkinScenario>();
        boolean inSection7 = false;
        String pendingGkId = null;

        for (var line : lines) {
            var trimmed = line.trim();

            if (SECTION_7_PATTERN.matcher(trimmed).find()) {
                inSection7 = true;
                continue;
            }

            if (inSection7
                    && SECTION_HEADER_PATTERN.matcher(trimmed)
                    .find()) {
                inSection7 = false;
                continue;
            }

            if (!inSection7) {
                continue;
            }

            var gkMatcher = GHERKIN_ID_PATTERN.matcher(trimmed);
            if (gkMatcher.matches()) {
                pendingGkId =
                        "@GK-%s".formatted(gkMatcher.group(1));
                continue;
            }

            if (pendingGkId != null) {
                var scenarioMatcher =
                        SCENARIO_TITLE_PATTERN.matcher(trimmed);
                if (scenarioMatcher.matches()) {
                    scenarios.add(new GherkinScenario(
                            pendingGkId,
                            scenarioMatcher.group(1).trim()));
                    pendingGkId = null;
                }
            }
        }

        return scenarios;
    }

    private static Map<String, String> extractAtMappings(
            String[] lines) {
        var mappings = new HashMap<String, String>();
        boolean inSection8 = false;

        for (var line : lines) {
            var trimmed = line.trim();

            if (SECTION_8_PATTERN.matcher(trimmed).find()) {
                inSection8 = true;
                continue;
            }

            if (inSection8
                    && SECTION_HEADER_PATTERN.matcher(trimmed)
                    .find()) {
                break;
            }

            if (!inSection8) {
                continue;
            }

            var atMatcher =
                    AT_MAPPING_PATTERN.matcher(trimmed);
            if (atMatcher.find()) {
                var atId = "AT-%s".formatted(atMatcher.group(1));
                var gkId =
                        "@GK-%s".formatted(atMatcher.group(2));
                mappings.put(gkId, atId);
            }
        }

        return mappings;
    }

    private static List<StoryRequirement> buildRequirements(
            List<GherkinScenario> scenarios,
            Map<String, String> atMappings) {
        var requirements = new ArrayList<StoryRequirement>();

        for (var scenario : scenarios) {
            var atId = Optional.ofNullable(
                    atMappings.get(scenario.gkId()));
            requirements.add(new StoryRequirement(
                    scenario.gkId(),
                    scenario.title(),
                    atId));
        }

        return List.copyOf(requirements);
    }

    private record GherkinScenario(
            String gkId, String title) {
    }
}
