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
 * Acceptance test proving that {@code x-story-implement/SKILL.md} is
 * correctly instrumented with telemetry phase markers (story-0040-0006,
 * TASK-0040-0006-003).
 *
 * <p>Expected: 5 {@code phase.start} + 5 {@code phase.end} pairs (Plan,
 * Implement, Review, Remediate, PR), zero lint violations.
 */
class XStoryImplementMarkersIT {

    private static final Path SKILL_FILE = Paths.get(
            "src/main/resources/targets/claude/skills/core/dev/"
                    + "x-story-implement/SKILL.md");

    @Test
    @DisplayName("skillFile_containsExactlyFivePhaseMarkerPairs")
    void skillFile_containsExactlyFivePhaseMarkerPairs()
            throws IOException {
        assertThat(SKILL_FILE).exists();
        String body = Files.readString(SKILL_FILE, StandardCharsets.UTF_8);

        long startCount = body.lines()
                .filter(line -> line.contains(
                        "telemetry-phase.sh start x-story-implement"))
                .count();
        long endCount = body.lines()
                .filter(line -> line.contains(
                        "telemetry-phase.sh end x-story-implement"))
                .count();

        assertThat(startCount)
                .as("x-story-implement must emit 5 phase.start markers")
                .isEqualTo(5);
        assertThat(endCount)
                .as("x-story-implement must emit 5 phase.end markers")
                .isEqualTo(5);
    }

    @Test
    @DisplayName("skillFile_passesTelemetryMarkerLint")
    void skillFile_passesTelemetryMarkerLint() {
        List<Finding> findings = TelemetryMarkerLint.lint(SKILL_FILE);
        assertThat(findings)
                .as("x-story-implement SKILL.md must be marker-balanced")
                .isEmpty();
    }
}
