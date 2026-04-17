package dev.iadev.skills;

import static org.assertj.core.api.Assertions.assertThat;

import dev.iadev.ci.TelemetryMarkerLint;
import dev.iadev.ci.TelemetryMarkerLint.Finding;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Smoke test proving story-0040-0008 DoD: "5 skills com markers em cada
 * fase" — scans all five creation skills instrumented across TASK-0040-
 * 0008-002..004 and asserts the phase + MCP contract at a glance.
 *
 * <p>Coverage:
 * <ul>
 *   <li>{@code x-epic-create} — 4 phase pairs, 0 MCP calls (Jira handled
 *       by dedicated jira skill).</li>
 *   <li>{@code x-story-create} — 3 phase pairs, 0 MCP calls (delegates
 *       to jira skills).</li>
 *   <li>{@code x-epic-decompose} — 7 phase pairs, 0 MCP calls (itself
 *       delegates to create skills and jira skills for MCP).</li>
 *   <li>{@code x-jira-create-epic} — 3 phase pairs, 1 MCP pair
 *       ({@code createJiraIssue}).</li>
 *   <li>{@code x-jira-create-stories} — 3 phase pairs, 2 MCP pairs
 *       ({@code createJiraIssue}, {@code createIssueLink}).</li>
 * </ul>
 *
 * <p>Also validates the aggregation-ready property declared in
 * story-0040-0008 §3.5: the telemetry report can aggregate
 * {@code tool.call} events by {@code tool} to show "tempo Jira vs.
 * tempo local". This is proven structurally by checking that every
 * mcp-start marker has a matching mcp-end marker (one-to-one pairing)
 * — which is the precondition for the NDJSON to contain N
 * {@code tool.call} events whose durationMs sum is finite and
 * consistent with wall-clock time (§7 Scenario 4).
 */
class CreationSkillsSmokeIT {

    private static final Path SKILLS_ROOT = Paths.get(
            "src/main/resources/targets/claude/skills/core");

    private static final Map<String, SkillSpec> EXPECTED =
            new LinkedHashMap<>();
    static {
        EXPECTED.put("x-epic-create",
                new SkillSpec(
                        SKILLS_ROOT.resolve(
                                "plan/x-epic-create/SKILL.md"),
                        4,
                        Map.of()));
        EXPECTED.put("x-story-create",
                new SkillSpec(
                        SKILLS_ROOT.resolve(
                                "plan/x-story-create/SKILL.md"),
                        3,
                        Map.of()));
        EXPECTED.put("x-epic-decompose",
                new SkillSpec(
                        SKILLS_ROOT.resolve(
                                "plan/x-epic-decompose/SKILL.md"),
                        7,
                        Map.of()));
        EXPECTED.put("x-jira-create-epic",
                new SkillSpec(
                        SKILLS_ROOT.resolve(
                                "jira/x-jira-create-epic/SKILL.md"),
                        3,
                        Map.of("createJiraIssue", 1)));
        EXPECTED.put("x-jira-create-stories",
                new SkillSpec(
                        SKILLS_ROOT.resolve(
                                "jira/x-jira-create-stories/SKILL.md"),
                        3,
                        Map.of(
                                "createJiraIssue", 1,
                                "createIssueLink", 1)));
    }

    @Test
    @DisplayName(
            "allFiveCreationSkills_emitExpectedPhaseAndMcpMarkerContract")
    void allFiveCreationSkills_emitExpectedPhaseAndMcpMarkerContract()
            throws IOException {
        for (Map.Entry<String, SkillSpec> entry : EXPECTED.entrySet()) {
            String skill = entry.getKey();
            SkillSpec spec = entry.getValue();

            assertThat(spec.file())
                    .as(skill + " SKILL.md must exist at " + spec.file())
                    .exists();

            String body = Files.readString(
                    spec.file(), StandardCharsets.UTF_8);

            // Phase marker counts.
            long startCount = body.lines()
                    .filter(line -> line.contains(
                            "telemetry-phase.sh start " + skill))
                    .count();
            long endCount = body.lines()
                    .filter(line -> line.contains(
                            "telemetry-phase.sh end " + skill))
                    .count();

            assertThat(startCount)
                    .as(skill + " must emit exactly "
                            + spec.phasePairs() + " phase.start markers")
                    .isEqualTo(spec.phasePairs());
            assertThat(endCount)
                    .as(skill + " must emit exactly "
                            + spec.phasePairs() + " phase.end markers")
                    .isEqualTo(spec.phasePairs());

            // MCP marker counts per method.
            for (Map.Entry<String, Integer> mcp
                    : spec.mcpMethods().entrySet()) {
                String method = mcp.getKey();
                int expectedCount = mcp.getValue();

                long mcpStart = body.lines()
                        .filter(line -> line.contains(
                                "telemetry-phase.sh mcp-start "
                                        + skill + " " + method))
                        .count();
                long mcpEnd = body.lines()
                        .filter(line -> line.contains(
                                "telemetry-phase.sh mcp-end "
                                        + skill + " " + method))
                        .count();

                assertThat(mcpStart)
                        .as(skill + " must emit exactly "
                                + expectedCount + " mcp-start marker(s) "
                                + "for " + method)
                        .isEqualTo(expectedCount);
                assertThat(mcpEnd)
                        .as(skill + " must emit exactly "
                                + expectedCount + " mcp-end marker(s) "
                                + "for " + method)
                        .isEqualTo(expectedCount);
            }

            // Non-jira skills must emit ZERO mcp markers.
            if (spec.mcpMethods().isEmpty()) {
                long anyMcpStart = body.lines()
                        .filter(line -> line.contains(
                                "telemetry-phase.sh mcp-start "
                                        + skill + " "))
                        .count();
                long anyMcpEnd = body.lines()
                        .filter(line -> line.contains(
                                "telemetry-phase.sh mcp-end "
                                        + skill + " "))
                        .count();
                assertThat(anyMcpStart + anyMcpEnd)
                        .as(skill + " is non-MCP — must emit ZERO "
                                + "mcp markers")
                        .isZero();
            }

            // Phase-marker balance (no dangling / unclosed / duplicate).
            List<Finding> findings = TelemetryMarkerLint.lint(spec.file());
            assertThat(findings)
                    .as(skill + " SKILL.md must be marker-balanced "
                            + "(no dangling / unclosed / duplicate)")
                    .isEmpty();
        }
    }

    @Test
    @DisplayName(
            "allMcpMarkerPairs_arePairedByToolEnablingDurationAggregation")
    void allMcpMarkerPairs_arePairedByToolEnablingDurationAggregation()
            throws IOException {
        // §3.5 aggregation-ready contract: every mcp-start has exactly one
        // mcp-end for the same (skill, method). This one-to-one pairing is
        // the precondition for computing P50/P95 per tool across a session.
        for (Map.Entry<String, SkillSpec> entry : EXPECTED.entrySet()) {
            String skill = entry.getKey();
            SkillSpec spec = entry.getValue();

            if (spec.mcpMethods().isEmpty()) {
                continue;
            }

            String body = Files.readString(
                    spec.file(), StandardCharsets.UTF_8);

            for (String method : spec.mcpMethods().keySet()) {
                long starts = body.lines()
                        .filter(line -> line.contains(
                                "telemetry-phase.sh mcp-start "
                                        + skill + " " + method))
                        .count();
                long ends = body.lines()
                        .filter(line -> line.contains(
                                "telemetry-phase.sh mcp-end "
                                        + skill + " " + method))
                        .count();
                assertThat(starts)
                        .as(skill + " " + method
                                + ": mcp-start count must equal "
                                + "mcp-end count (one-to-one pairing "
                                + "required for durationMs aggregation)")
                        .isEqualTo(ends);
            }
        }
    }

    private record SkillSpec(
            Path file,
            int phasePairs,
            Map<String, Integer> mcpMethods) {
    }
}
