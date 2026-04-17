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
 * Acceptance test proving that {@code x-epic-create/SKILL.md} and
 * {@code x-story-create/SKILL.md} are correctly instrumented with
 * telemetry phase markers per story-0040-0008 §3.1 and TASK-002.
 *
 * <p>Contract:
 * <ul>
 *   <li>{@code x-epic-create} has exactly 4 phase.start / phase.end pairs
 *       (Spec-Analysis, Rules-Extraction, Story-Index, Epic-Generation).</li>
 *   <li>{@code x-story-create} has exactly 3 phase.start / phase.end pairs
 *       (Context-Gathering, Generation-Loop, Validation).</li>
 *   <li>Zero {@link TelemetryMarkerLint} violations on either file.</li>
 * </ul>
 */
class CreationSkillsMarkersIT {

    private static final Path EPIC_CREATE_SKILL = Paths.get(
            "src/main/resources/targets/claude/skills/core/plan/"
                    + "x-epic-create/SKILL.md");

    private static final Path STORY_CREATE_SKILL = Paths.get(
            "src/main/resources/targets/claude/skills/core/plan/"
                    + "x-story-create/SKILL.md");

    @Test
    @DisplayName("xEpicCreate_containsExactlyFourPhaseMarkerPairs")
    void xEpicCreate_containsExactlyFourPhaseMarkerPairs()
            throws IOException {
        assertThat(EPIC_CREATE_SKILL).exists();
        String body = Files.readString(
                EPIC_CREATE_SKILL, StandardCharsets.UTF_8);

        long startCount = body.lines()
                .filter(line -> line.contains(
                        "telemetry-phase.sh start x-epic-create"))
                .count();
        long endCount = body.lines()
                .filter(line -> line.contains(
                        "telemetry-phase.sh end x-epic-create"))
                .count();

        assertThat(startCount)
                .as("x-epic-create must emit 4 phase.start markers")
                .isEqualTo(4);
        assertThat(endCount)
                .as("x-epic-create must emit 4 phase.end markers")
                .isEqualTo(4);
    }

    @Test
    @DisplayName("xEpicCreate_phaseNamesMatchStoryContract")
    void xEpicCreate_phaseNamesMatchStoryContract() throws IOException {
        String body = Files.readString(
                EPIC_CREATE_SKILL, StandardCharsets.UTF_8);
        List<String> phases = List.of(
                "Phase-1-Spec-Analysis",
                "Phase-2-Rules-Extraction",
                "Phase-3-Story-Index",
                "Phase-4-Epic-Generation");
        for (String phase : phases) {
            String needle = "telemetry-phase.sh start x-epic-create "
                    + phase;
            long count = body.lines()
                    .filter(line -> line.contains(needle))
                    .count();
            assertThat(count)
                    .as("Phase '" + phase
                            + "' must appear exactly once as "
                            + "phase.start")
                    .isEqualTo(1);
        }
    }

    @Test
    @DisplayName("xStoryCreate_containsExactlyThreePhaseMarkerPairs")
    void xStoryCreate_containsExactlyThreePhaseMarkerPairs()
            throws IOException {
        assertThat(STORY_CREATE_SKILL).exists();
        String body = Files.readString(
                STORY_CREATE_SKILL, StandardCharsets.UTF_8);

        long startCount = body.lines()
                .filter(line -> line.contains(
                        "telemetry-phase.sh start x-story-create"))
                .count();
        long endCount = body.lines()
                .filter(line -> line.contains(
                        "telemetry-phase.sh end x-story-create"))
                .count();

        assertThat(startCount)
                .as("x-story-create must emit 3 phase.start markers")
                .isEqualTo(3);
        assertThat(endCount)
                .as("x-story-create must emit 3 phase.end markers")
                .isEqualTo(3);
    }

    @Test
    @DisplayName("xStoryCreate_phaseNamesMatchStoryContract")
    void xStoryCreate_phaseNamesMatchStoryContract() throws IOException {
        String body = Files.readString(
                STORY_CREATE_SKILL, StandardCharsets.UTF_8);
        List<String> phases = List.of(
                "Phase-1-Context-Gathering",
                "Phase-2-Generation-Loop",
                "Phase-3-Validation");
        for (String phase : phases) {
            String needle = "telemetry-phase.sh start x-story-create "
                    + phase;
            long count = body.lines()
                    .filter(line -> line.contains(needle))
                    .count();
            assertThat(count)
                    .as("Phase '" + phase
                            + "' must appear exactly once as "
                            + "phase.start")
                    .isEqualTo(1);
        }
    }

    @Test
    @DisplayName("bothSkills_passTelemetryMarkerLint")
    void bothSkills_passTelemetryMarkerLint() {
        List<Finding> epicFindings =
                TelemetryMarkerLint.lint(EPIC_CREATE_SKILL);
        assertThat(epicFindings)
                .as("x-epic-create SKILL.md must be marker-balanced")
                .isEmpty();

        List<Finding> storyFindings =
                TelemetryMarkerLint.lint(STORY_CREATE_SKILL);
        assertThat(storyFindings)
                .as("x-story-create SKILL.md must be marker-balanced")
                .isEmpty();
    }
}
