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
 * Acceptance test proving that {@code x-task-plan/SKILL.md} is
 * correctly instrumented with telemetry phase markers (story-0040-0006,
 * TASK-0040-0006-005).
 *
 * <p>Contract:
 * <ul>
 *   <li>Exactly 3 {@code phase.start} markers (Phase 1 Context, Phase 2
 *       Breakdown, Phase 3 Validation).</li>
 *   <li>Exactly 3 matching {@code phase.end} markers.</li>
 *   <li>Zero {@link TelemetryMarkerLint} violations
 *       (duplicates / danglers / unclosed).</li>
 * </ul>
 */
class XTaskPlanMarkersIT {

    private static final Path SKILL_FILE = Paths.get(
            "src/main/resources/targets/claude/skills/core/plan/"
                    + "x-task-plan/SKILL.md");

    @Test
    @DisplayName("skillFile_containsExactlyThreePhaseMarkerPairs")
    void skillFile_containsExactlyThreePhaseMarkerPairs()
            throws IOException {
        assertThat(SKILL_FILE).exists();
        String body = Files.readString(SKILL_FILE, StandardCharsets.UTF_8);

        long startCount = body.lines()
                .filter(line -> line.contains(
                        "telemetry-phase.sh start x-task-plan"))
                .count();
        long endCount = body.lines()
                .filter(line -> line.contains(
                        "telemetry-phase.sh end x-task-plan"))
                .count();

        assertThat(startCount)
                .as("x-task-plan must emit 3 phase.start markers")
                .isEqualTo(3);
        assertThat(endCount)
                .as("x-task-plan must emit 3 phase.end markers")
                .isEqualTo(3);
    }

    @Test
    @DisplayName("skillFile_passesTelemetryMarkerLint")
    void skillFile_passesTelemetryMarkerLint() {
        List<Finding> findings = TelemetryMarkerLint.lint(SKILL_FILE);
        assertThat(findings)
                .as("x-task-plan SKILL.md must be marker-balanced")
                .isEmpty();
    }
}
