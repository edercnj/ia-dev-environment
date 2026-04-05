package dev.iadev.domain.qualitygate;

import java.util.List;

/**
 * A parsed Gherkin scenario extracted from story markdown.
 *
 * @param scenarioId the scenario identifier (e.g., "@GK-1")
 * @param givenLines the Given step lines
 * @param whenLines  the When step lines
 * @param thenLines  the Then step lines
 */
public record GherkinScenario(
        String scenarioId,
        List<String> givenLines,
        List<String> whenLines,
        List<String> thenLines
) {

    public GherkinScenario {
        givenLines = List.copyOf(givenLines);
        whenLines = List.copyOf(whenLines);
        thenLines = List.copyOf(thenLines);
    }
}
