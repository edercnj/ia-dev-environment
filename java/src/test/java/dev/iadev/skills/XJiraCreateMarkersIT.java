package dev.iadev.skills;

import static org.assertj.core.api.Assertions.assertThat;

import dev.iadev.ci.TelemetryMarkerLint;
import dev.iadev.ci.TelemetryMarkerLint.Finding;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Acceptance test proving that the two Jira creation skills are
 * instrumented with phase markers AND mcp-start / mcp-end markers
 * wrapping every MCP Atlassian call per story-0040-0008 §3.2 and
 * TASK-004.
 *
 * <p>Contract:
 * <ul>
 *   <li>{@code x-jira-create-epic}: 3 phase pairs (Read-Markdown,
 *       MCP-Call, Sync-Back) and 1 mcp-start / mcp-end pair around
 *       {@code createJiraIssue}.</li>
 *   <li>{@code x-jira-create-stories}: 3 phase pairs (Read-Markdowns,
 *       MCP-Loop, Dependency-Links) and 2 mcp-start / mcp-end pairs
 *       (one per MCP call site: {@code createJiraIssue} per story,
 *       {@code createIssueLink} per dependency).</li>
 *   <li>Both files pass {@link TelemetryMarkerLint} — mcp-* markers
 *       do not interfere with the phase linter because the regex
 *       {@code (start|end)} does not match {@code mcp-start} /
 *       {@code mcp-end} (there is no whitespace between "mcp-" and
 *       "start").</li>
 * </ul>
 */
class XJiraCreateMarkersIT {

    private static final Path EPIC_SKILL = Paths.get(
            "src/main/resources/targets/claude/skills/core/jira/"
                    + "x-jira-create-epic/SKILL.md");

    private static final Path STORIES_SKILL = Paths.get(
            "src/main/resources/targets/claude/skills/core/jira/"
                    + "x-jira-create-stories/SKILL.md");

    @Test
    @DisplayName("xJiraCreateEpic_containsThreePhaseMarkerPairs")
    void xJiraCreateEpic_containsThreePhaseMarkerPairs()
            throws IOException {
        assertThat(EPIC_SKILL).exists();
        String body = Files.readString(EPIC_SKILL, StandardCharsets.UTF_8);

        long startCount = countLinesContaining(body,
                "telemetry-phase.sh start x-jira-create-epic");
        long endCount = countLinesContaining(body,
                "telemetry-phase.sh end x-jira-create-epic");

        assertThat(startCount).isEqualTo(3);
        assertThat(endCount).isEqualTo(3);
    }

    @Test
    @DisplayName("xJiraCreateEpic_wrapsMcpCallWithMarker")
    void xJiraCreateEpic_wrapsMcpCallWithMarker() throws IOException {
        String body = Files.readString(EPIC_SKILL, StandardCharsets.UTF_8);

        long mcpStart = countLinesContaining(body,
                "telemetry-phase.sh mcp-start x-jira-create-epic "
                        + "createJiraIssue");
        long mcpEnd = countLinesContaining(body,
                "telemetry-phase.sh mcp-end x-jira-create-epic "
                        + "createJiraIssue");

        assertThat(mcpStart)
                .as("x-jira-create-epic must emit 1 mcp-start for "
                        + "createJiraIssue")
                .isEqualTo(1);
        assertThat(mcpEnd)
                .as("x-jira-create-epic must emit 1 mcp-end for "
                        + "createJiraIssue")
                .isEqualTo(1);
    }

    @Test
    @DisplayName("xJiraCreateStories_containsThreePhaseMarkerPairs")
    void xJiraCreateStories_containsThreePhaseMarkerPairs()
            throws IOException {
        assertThat(STORIES_SKILL).exists();
        String body = Files.readString(
                STORIES_SKILL, StandardCharsets.UTF_8);

        long startCount = countLinesContaining(body,
                "telemetry-phase.sh start x-jira-create-stories");
        long endCount = countLinesContaining(body,
                "telemetry-phase.sh end x-jira-create-stories");

        assertThat(startCount).isEqualTo(3);
        assertThat(endCount).isEqualTo(3);
    }

    @Test
    @DisplayName("xJiraCreateStories_wrapsTwoMcpCallSites")
    void xJiraCreateStories_wrapsTwoMcpCallSites() throws IOException {
        String body = Files.readString(
                STORIES_SKILL, StandardCharsets.UTF_8);

        long createIssueStart = countLinesContaining(body,
                "telemetry-phase.sh mcp-start x-jira-create-stories "
                        + "createJiraIssue");
        long createIssueEnd = countLinesContaining(body,
                "telemetry-phase.sh mcp-end x-jira-create-stories "
                        + "createJiraIssue");
        long linkStart = countLinesContaining(body,
                "telemetry-phase.sh mcp-start x-jira-create-stories "
                        + "createIssueLink");
        long linkEnd = countLinesContaining(body,
                "telemetry-phase.sh mcp-end x-jira-create-stories "
                        + "createIssueLink");

        assertThat(createIssueStart)
                .as("x-jira-create-stories must emit 1 mcp-start for "
                        + "createJiraIssue (loop call site)")
                .isEqualTo(1);
        assertThat(createIssueEnd)
                .as("x-jira-create-stories must emit 1 mcp-end for "
                        + "createJiraIssue")
                .isEqualTo(1);
        assertThat(linkStart)
                .as("x-jira-create-stories must emit 1 mcp-start for "
                        + "createIssueLink (dependency call site)")
                .isEqualTo(1);
        assertThat(linkEnd)
                .as("x-jira-create-stories must emit 1 mcp-end for "
                        + "createIssueLink")
                .isEqualTo(1);
    }

    @Test
    @DisplayName("bothSkills_passTelemetryMarkerLint")
    void bothSkills_passTelemetryMarkerLint() {
        List<Finding> epicFindings = TelemetryMarkerLint.lint(EPIC_SKILL);
        assertThat(epicFindings)
                .as("x-jira-create-epic must be marker-balanced")
                .isEmpty();

        List<Finding> storiesFindings =
                TelemetryMarkerLint.lint(STORIES_SKILL);
        assertThat(storiesFindings)
                .as("x-jira-create-stories must be marker-balanced")
                .isEmpty();
    }

    private long countLinesContaining(String body, String needle) {
        return body.lines()
                .filter(line -> line.contains(needle))
                .count();
    }
}
