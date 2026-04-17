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
 * Acceptance tests proving that {@code x-arch-plan/SKILL.md} and
 * {@code x-test-plan/SKILL.md} are instrumented with 4 telemetry phase
 * marker pairs each (story-0040-0007, TASK-0040-0007-004).
 *
 * <p>The story treats x-arch-plan and x-test-plan as degenerate skills
 * with no parallel agent dispatch — they emit only phase markers, no
 * subagent markers. (Story text uses pre-EPIC-0036 skill names; the
 * renamed forms are authoritative here.)
 */
class PlanningSkillsMarkersIT {

    private static final Path ARCH_PLAN = Paths.get(
            "src/main/resources/targets/claude/skills/core/plan/"
                    + "x-arch-plan/SKILL.md");

    private static final Path TEST_PLAN = Paths.get(
            "src/main/resources/targets/claude/skills/core/test/"
                    + "x-test-plan/SKILL.md");

    @Test
    @DisplayName("archPlanFile_containsExactlyFourPhaseMarkerPairs")
    void archPlanFile_containsExactlyFourPhaseMarkerPairs()
            throws IOException {
        assertPhasePairs(ARCH_PLAN, "x-arch-plan", 4);
    }

    @Test
    @DisplayName("archPlanFile_emitsNoSubagentMarkers")
    void archPlanFile_emitsNoSubagentMarkers() throws IOException {
        String body = Files.readString(ARCH_PLAN, StandardCharsets.UTF_8);
        long subagentCount = body.lines()
                .filter(line -> line.contains(
                        "telemetry-phase.sh subagent-"))
                .count();
        assertThat(subagentCount)
                .as("x-arch-plan has no parallel agent dispatch, must "
                        + "emit 0 subagent markers")
                .isZero();
    }

    @Test
    @DisplayName("archPlanFile_passesTelemetryMarkerLint")
    void archPlanFile_passesTelemetryMarkerLint() {
        List<Finding> findings = TelemetryMarkerLint.lint(ARCH_PLAN);
        assertThat(findings)
                .as("x-arch-plan SKILL.md must be marker-balanced")
                .isEmpty();
    }

    @Test
    @DisplayName("testPlanFile_containsExactlyFourPhaseMarkerPairs")
    void testPlanFile_containsExactlyFourPhaseMarkerPairs()
            throws IOException {
        assertPhasePairs(TEST_PLAN, "x-test-plan", 4);
    }

    @Test
    @DisplayName("testPlanFile_emitsNoSubagentMarkers")
    void testPlanFile_emitsNoSubagentMarkers() throws IOException {
        String body = Files.readString(TEST_PLAN, StandardCharsets.UTF_8);
        long subagentCount = body.lines()
                .filter(line -> line.contains(
                        "telemetry-phase.sh subagent-"))
                .count();
        assertThat(subagentCount)
                .as("x-test-plan has no parallel agent dispatch, must "
                        + "emit 0 subagent markers")
                .isZero();
    }

    @Test
    @DisplayName("testPlanFile_passesTelemetryMarkerLint")
    void testPlanFile_passesTelemetryMarkerLint() {
        List<Finding> findings = TelemetryMarkerLint.lint(TEST_PLAN);
        assertThat(findings)
                .as("x-test-plan SKILL.md must be marker-balanced")
                .isEmpty();
    }

    // ----------------------------------------------------------------
    // Helpers
    // ----------------------------------------------------------------

    private void assertPhasePairs(Path file, String skillName,
            int expectedPairs) throws IOException {
        assertThat(file).exists();
        String body = Files.readString(file, StandardCharsets.UTF_8);
        long startCount = body.lines()
                .filter(line -> line.contains(
                        "telemetry-phase.sh start " + skillName))
                .count();
        long endCount = body.lines()
                .filter(line -> line.contains(
                        "telemetry-phase.sh end " + skillName))
                .count();
        assertThat(startCount)
                .as(skillName + " must emit " + expectedPairs
                        + " phase.start markers")
                .isEqualTo(expectedPairs);
        assertThat(endCount)
                .as(skillName + " must emit " + expectedPairs
                        + " phase.end markers")
                .isEqualTo(expectedPairs);
    }
}
