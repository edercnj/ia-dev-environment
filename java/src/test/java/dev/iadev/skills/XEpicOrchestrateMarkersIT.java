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
 * Acceptance test proving that {@code x-epic-orchestrate/SKILL.md} is
 * correctly instrumented with telemetry phase markers AND subagent
 * markers around its per-story parallel-planning loop (story-0040-0007,
 * TASK-0040-0007-002).
 *
 * <p>Contract (story-0040-0007 §3.1 and §7):
 * <ul>
 *   <li>Exactly 3 {@code phase.start} markers covering Phase 1 (Discovery /
 *       dependency order), Phase 2 (Story Orchestration Loop), Phase 3
 *       (Consolidation / Report).</li>
 *   <li>Exactly 3 matching {@code phase.end} markers.</li>
 *   <li>At least 1 {@code subagent-start} / {@code subagent-end} pair
 *       instrumenting the per-story subagent dispatched inside the Phase 2
 *       loop.</li>
 *   <li>Zero {@link TelemetryMarkerLint} violations on phase markers.</li>
 * </ul>
 */
class XEpicOrchestrateMarkersIT {

    private static final Path SKILL_FILE = Paths.get(
            "src/main/resources/targets/claude/skills/core/plan/"
                    + "x-epic-orchestrate/SKILL.md");

    @Test
    @DisplayName("skillFile_containsExactlyThreePhaseMarkerPairs")
    void skillFile_containsExactlyThreePhaseMarkerPairs()
            throws IOException {
        assertThat(SKILL_FILE).exists();
        String body = Files.readString(SKILL_FILE, StandardCharsets.UTF_8);

        long startCount = body.lines()
                .filter(line -> line.contains(
                        "telemetry-phase.sh start x-epic-orchestrate"))
                .count();
        long endCount = body.lines()
                .filter(line -> line.contains(
                        "telemetry-phase.sh end x-epic-orchestrate"))
                .count();

        assertThat(startCount)
                .as("x-epic-orchestrate must emit 3 phase.start markers")
                .isEqualTo(3);
        assertThat(endCount)
                .as("x-epic-orchestrate must emit 3 phase.end markers")
                .isEqualTo(3);
    }

    @Test
    @DisplayName("skillFile_containsSubagentMarkersForPerStoryLoop")
    void skillFile_containsSubagentMarkersForPerStoryLoop()
            throws IOException {
        String body = Files.readString(SKILL_FILE, StandardCharsets.UTF_8);

        long subagentStartCount = body.lines()
                .filter(line -> line.contains(
                        "telemetry-phase.sh subagent-start "
                                + "x-epic-orchestrate"))
                .count();
        long subagentEndCount = body.lines()
                .filter(line -> line.contains(
                        "telemetry-phase.sh subagent-end "
                                + "x-epic-orchestrate"))
                .count();

        assertThat(subagentStartCount)
                .as("x-epic-orchestrate must emit >= 1 subagent-start "
                        + "marker (per-story loop)")
                .isGreaterThanOrEqualTo(1);
        assertThat(subagentEndCount)
                .as("x-epic-orchestrate must emit a matching "
                        + "subagent-end count")
                .isEqualTo(subagentStartCount);
    }

    @Test
    @DisplayName("skillFile_passesTelemetryMarkerLint")
    void skillFile_passesTelemetryMarkerLint() {
        List<Finding> findings = TelemetryMarkerLint.lint(SKILL_FILE);
        assertThat(findings)
                .as("x-epic-orchestrate SKILL.md must be "
                        + "marker-balanced")
                .isEmpty();
    }
}
