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
 * Acceptance test proving that {@code x-epic-implement/SKILL.md} is
 * correctly instrumented with telemetry phase markers (story-0040-0006,
 * TASK-0040-0006-002).
 *
 * <p>Expected: 4 {@code phase.start} + 4 {@code phase.end} pairs, zero
 * lint violations.
 */
class XEpicImplementMarkersIT {

    private static final Path SKILL_FILE = Paths.get(
            "src/main/resources/targets/claude/skills/core/dev/"
                    + "x-epic-implement/SKILL.md");

    @Test
    @DisplayName("skillFile_containsExactlyFourPhaseMarkerPairs")
    void skillFile_containsExactlyFourPhaseMarkerPairs()
            throws IOException {
        assertThat(SKILL_FILE).exists();
        String body = Files.readString(SKILL_FILE, StandardCharsets.UTF_8);

        long startCount = body.lines()
                .filter(line -> line.contains(
                        "telemetry-phase.sh start x-epic-implement"))
                .count();
        long endCount = body.lines()
                .filter(line -> line.contains(
                        "telemetry-phase.sh end x-epic-implement"))
                .count();

        assertThat(startCount)
                .as("x-epic-implement must emit 4 phase.start markers")
                .isEqualTo(4);
        assertThat(endCount)
                .as("x-epic-implement must emit 4 phase.end markers")
                .isEqualTo(4);
    }

    @Test
    @DisplayName("skillFile_passesTelemetryMarkerLint")
    void skillFile_passesTelemetryMarkerLint() {
        assertThat(SKILL_FILE).exists();
        List<Finding> findings = TelemetryMarkerLint.lint(SKILL_FILE);
        assertThat(findings)
                .as("x-epic-implement SKILL.md must be marker-balanced")
                .isEmpty();
    }
}
