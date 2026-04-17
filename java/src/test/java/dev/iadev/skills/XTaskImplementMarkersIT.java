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
 * Acceptance test proving that {@code x-task-implement/SKILL.md} is
 * correctly instrumented with per-TDD-cycle telemetry markers
 * (story-0040-0006, TASK-0040-0006-004).
 *
 * <p>The story spec requests "3 marcadores por ciclo": the instrumented
 * template emits one pair per TDD phase (Red / Green / Refactor), i.e.
 * 3 {@code phase.start} + 3 {@code phase.end} occurrences in the SKILL.md
 * template body. At runtime the orchestrator repeats these for every UT-N
 * cycle.
 */
class XTaskImplementMarkersIT {

    private static final Path SKILL_FILE = Paths.get(
            "src/main/resources/targets/claude/skills/core/dev/"
                    + "x-task-implement/SKILL.md");

    @Test
    @DisplayName("skillFile_containsExactlyThreeTddPhaseMarkerPairs")
    void skillFile_containsExactlyThreeTddPhaseMarkerPairs()
            throws IOException {
        assertThat(SKILL_FILE).exists();
        String body = Files.readString(SKILL_FILE, StandardCharsets.UTF_8);

        long startCount = body.lines()
                .filter(line -> line.contains(
                        "telemetry-phase.sh start x-task-implement"))
                .count();
        long endCount = body.lines()
                .filter(line -> line.contains(
                        "telemetry-phase.sh end x-task-implement"))
                .count();

        assertThat(startCount)
                .as("x-task-implement must emit 3 phase.start markers "
                        + "(Red, Green, Refactor)")
                .isEqualTo(3);
        assertThat(endCount)
                .as("x-task-implement must emit 3 phase.end markers")
                .isEqualTo(3);
    }

    @Test
    @DisplayName("skillFile_containsAllThreeTddPhaseNames")
    void skillFile_containsAllThreeTddPhaseNames()
            throws IOException {
        String body = Files.readString(SKILL_FILE, StandardCharsets.UTF_8);
        assertThat(body)
                .as("Red-Phase marker present")
                .contains("telemetry-phase.sh start x-task-implement "
                        + "Red-Phase");
        assertThat(body)
                .as("Green-Phase marker present")
                .contains("telemetry-phase.sh start x-task-implement "
                        + "Green-Phase");
        assertThat(body)
                .as("Refactor-Phase marker present")
                .contains("telemetry-phase.sh start x-task-implement "
                        + "Refactor-Phase");
    }

    @Test
    @DisplayName("skillFile_passesTelemetryMarkerLint")
    void skillFile_passesTelemetryMarkerLint() {
        assertThat(SKILL_FILE).exists();
        List<Finding> findings = TelemetryMarkerLint.lint(SKILL_FILE);
        assertThat(findings)
                .as("x-task-implement SKILL.md must be marker-balanced")
                .isEmpty();
    }
}
