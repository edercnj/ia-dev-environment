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
 * Acceptance test proving that {@code x-epic-decompose/SKILL.md} is
 * correctly instrumented with 7 telemetry phase markers per
 * story-0040-0008 §3.1 and TASK-003.
 *
 * <p>Contract:
 * <ul>
 *   <li>7 phase.start / phase.end pairs: Analysis, Jira-Decision,
 *       Epic, Stories, Map, Jira-Links, Report.</li>
 *   <li>Phase 1.5 and 4.5 use the {@code _5} naming form so the linter
 *       regex ({@code [A-Za-z0-9_-]}) accepts the phase name without
 *       collapsing on the dot character.</li>
 *   <li>Zero {@link TelemetryMarkerLint} violations.</li>
 * </ul>
 */
class XEpicDecomposeMarkersIT {

    private static final Path SKILL_FILE = Paths.get(
            "src/main/resources/targets/claude/skills/core/plan/"
                    + "x-epic-decompose/SKILL.md");

    @Test
    @DisplayName("skillFile_containsExactlySevenPhaseMarkerPairs")
    void skillFile_containsExactlySevenPhaseMarkerPairs()
            throws IOException {
        assertThat(SKILL_FILE).exists();
        String body = Files.readString(
                SKILL_FILE, StandardCharsets.UTF_8);

        long startCount = body.lines()
                .filter(line -> line.contains(
                        "telemetry-phase.sh start x-epic-decompose"))
                .count();
        long endCount = body.lines()
                .filter(line -> line.contains(
                        "telemetry-phase.sh end x-epic-decompose"))
                .count();

        assertThat(startCount)
                .as("x-epic-decompose must emit 7 phase.start markers")
                .isEqualTo(7);
        assertThat(endCount)
                .as("x-epic-decompose must emit 7 phase.end markers")
                .isEqualTo(7);
    }

    @Test
    @DisplayName("skillFile_allSevenPhaseNamesAppearExactlyOnce")
    void skillFile_allSevenPhaseNamesAppearExactlyOnce()
            throws IOException {
        String body = Files.readString(
                SKILL_FILE, StandardCharsets.UTF_8);
        List<String> phases = List.of(
                "Phase-1-Analysis",
                "Phase-1_5-Jira-Decision",
                "Phase-2-Epic",
                "Phase-3-Stories",
                "Phase-4-Map",
                "Phase-4_5-Jira-Links",
                "Phase-5-Report");
        for (String phase : phases) {
            String needle = "telemetry-phase.sh start x-epic-decompose "
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
    @DisplayName("skillFile_passesTelemetryMarkerLint")
    void skillFile_passesTelemetryMarkerLint() {
        List<Finding> findings = TelemetryMarkerLint.lint(SKILL_FILE);
        assertThat(findings)
                .as("x-epic-decompose SKILL.md must be marker-balanced")
                .isEmpty();
    }
}
