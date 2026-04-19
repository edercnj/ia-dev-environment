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
 * Acceptance test proving that {@code x-story-plan/SKILL.md} is correctly
 * instrumented with telemetry phase markers AND 5 subagent markers
 * wrapping the parallel planning agents (Architect, QA, Security, Tech
 * Lead, Product Owner) per story-0040-0007 §3.1.
 *
 * <p>Contract:
 * <ul>
 *   <li>Exactly 5 {@code phase.start} / {@code phase.end} pairs (Context,
 *       Parallel-Planning, Consolidation, DoR-Validation,
 *       Aggregate-Footprint).</li>
 *   <li>Exactly 5 {@code subagent-start} and 5 {@code subagent-end}
 *       markers on the parallel planning phase.</li>
 *   <li>Each of the 5 roles (Architect, QA, Security, TechLead, PO)
 *       appears exactly once as a subagent-start role argument.</li>
 *   <li>Zero {@link TelemetryMarkerLint} violations on phase markers.</li>
 * </ul>
 */
class XStoryPlanMarkersIT {

    private static final Path SKILL_FILE = Paths.get(
            "src/main/resources/targets/claude/skills/core/plan/"
                    + "x-story-plan/SKILL.md");

    @Test
    @DisplayName("skillFile_containsExactlyFivePhaseMarkerPairs")
    void skillFile_containsExactlyFivePhaseMarkerPairs()
            throws IOException {
        assertThat(SKILL_FILE).exists();
        String body = Files.readString(SKILL_FILE, StandardCharsets.UTF_8);

        long startCount = body.lines()
                .filter(line -> line.contains(
                        "telemetry-phase.sh start x-story-plan"))
                .count();
        long endCount = body.lines()
                .filter(line -> line.contains(
                        "telemetry-phase.sh end x-story-plan"))
                .count();

        assertThat(startCount)
                .as("x-story-plan must emit 5 phase.start markers")
                .isEqualTo(5);
        assertThat(endCount)
                .as("x-story-plan must emit 5 phase.end markers")
                .isEqualTo(5);
    }

    @Test
    @DisplayName("skillFile_containsFiveSubagentPairsForParallelPlanning")
    void skillFile_containsFiveSubagentPairsForParallelPlanning()
            throws IOException {
        String body = Files.readString(SKILL_FILE, StandardCharsets.UTF_8);

        long subagentStartCount = body.lines()
                .filter(line -> line.contains(
                        "telemetry-phase.sh subagent-start x-story-plan"))
                .count();
        long subagentEndCount = body.lines()
                .filter(line -> line.contains(
                        "telemetry-phase.sh subagent-end x-story-plan"))
                .count();

        assertThat(subagentStartCount)
                .as("x-story-plan must emit 5 subagent-start markers "
                        + "(Architect/QA/Security/TechLead/PO)")
                .isEqualTo(5);
        assertThat(subagentEndCount)
                .as("x-story-plan must emit 5 matching subagent-end "
                        + "markers")
                .isEqualTo(5);
    }

    @Test
    @DisplayName("skillFile_allFiveSubagentRolesAppearExactlyOnce")
    void skillFile_allFiveSubagentRolesAppearExactlyOnce()
            throws IOException {
        String body = Files.readString(SKILL_FILE, StandardCharsets.UTF_8);
        List<String> roles = List.of(
                "Architect", "QA", "Security", "TechLead", "PO");
        for (String role : roles) {
            String needle = "telemetry-phase.sh subagent-start "
                    + "x-story-plan " + role;
            long count = body.lines()
                    .filter(line -> line.contains(needle))
                    .count();
            assertThat(count)
                    .as("Role '" + role + "' must appear exactly once "
                            + "as a subagent-start role")
                    .isEqualTo(1);
        }
    }

    @Test
    @DisplayName("skillFile_passesTelemetryMarkerLint")
    void skillFile_passesTelemetryMarkerLint() {
        List<Finding> findings = TelemetryMarkerLint.lint(SKILL_FILE);
        assertThat(findings)
                .as("x-story-plan SKILL.md must be marker-balanced")
                .isEmpty();
    }
}
