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
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Smoke test proving story-0040-0007's DoD: "5 skills with markers in each
 * phase numbered; teste IT conta subagent.* events em cada skill".
 *
 * <p>Scans all 5 planning skills instrumented in this story (x-epic-
 * orchestrate, x-story-plan, x-arch-plan, x-test-plan, x-epic-map) and
 * asserts their phase-marker contract at a glance:
 *
 * <ul>
 *   <li>Each SKILL.md file exists.</li>
 *   <li>Each SKILL.md emits at least 3 balanced phase marker pairs
 *       (x-epic-orchestrate has 3; the other four have 4).</li>
 *   <li>Each SKILL.md passes {@link TelemetryMarkerLint} (no dangling,
 *       unclosed, or duplicate markers).</li>
 *   <li>The 2 skills that dispatch parallel subagents
 *       (x-story-plan, x-epic-orchestrate) emit subagent markers.</li>
 *   <li>The 3 degenerate skills emit ZERO subagent markers.</li>
 * </ul>
 *
 * <p>This test also acts as the TASK-0040-0007-005 acceptance test — if
 * any planning skill loses its instrumentation in a future change, this
 * single test fails and pinpoints which skill regressed.
 */
class PlanningSmokeIT {

    private static final Path SKILLS_ROOT = Paths.get(
            "src/main/resources/targets/claude/skills/core");

    // Skill → expected phase-pair count.
    private static final Map<String, PhaseSpec> EXPECTED = Map.of(
            "x-epic-orchestrate",
                    new PhaseSpec(
                            SKILLS_ROOT.resolve(
                                    "plan/x-epic-orchestrate/SKILL.md"),
                            3, true),
            "x-story-plan",
                    new PhaseSpec(
                            SKILLS_ROOT.resolve(
                                    "plan/x-story-plan/SKILL.md"),
                            4, true),
            "x-arch-plan",
                    new PhaseSpec(
                            SKILLS_ROOT.resolve(
                                    "plan/x-arch-plan/SKILL.md"),
                            4, false),
            "x-test-plan",
                    new PhaseSpec(
                            SKILLS_ROOT.resolve(
                                    "test/x-test-plan/SKILL.md"),
                            4, false),
            "x-epic-map",
                    new PhaseSpec(
                            SKILLS_ROOT.resolve(
                                    "plan/x-epic-map/SKILL.md"),
                            4, false));

    @Test
    @DisplayName("allFivePlanningSkills_emitExpectedPhaseMarkerContract")
    void allFivePlanningSkills_emitExpectedPhaseMarkerContract()
            throws IOException {
        for (Map.Entry<String, PhaseSpec> entry : EXPECTED.entrySet()) {
            String skill = entry.getKey();
            PhaseSpec spec = entry.getValue();

            assertThat(spec.file())
                    .as(skill + " SKILL.md must exist at " + spec.file())
                    .exists();

            String body = Files.readString(
                    spec.file(), StandardCharsets.UTF_8);

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

            long subagentStarts = body.lines()
                    .filter(line -> line.contains(
                            "telemetry-phase.sh subagent-start " + skill))
                    .count();
            long subagentEnds = body.lines()
                    .filter(line -> line.contains(
                            "telemetry-phase.sh subagent-end " + skill))
                    .count();

            if (spec.hasSubagents()) {
                assertThat(subagentStarts)
                        .as(skill + " dispatches parallel subagents and "
                                + "must emit >= 1 subagent-start marker")
                        .isGreaterThanOrEqualTo(1);
                assertThat(subagentEnds)
                        .as(skill + " must emit matching subagent-end "
                                + "markers (start count == end count)")
                        .isEqualTo(subagentStarts);
            } else {
                assertThat(subagentStarts + subagentEnds)
                        .as(skill + " is degenerate — must emit ZERO "
                                + "subagent markers")
                        .isZero();
            }

            List<Finding> findings = TelemetryMarkerLint.lint(spec.file());
            assertThat(findings)
                    .as(skill + " SKILL.md must be marker-balanced "
                            + "(no dangling / unclosed / duplicate)")
                    .isEmpty();
        }
    }

    private record PhaseSpec(Path file, int phasePairs,
            boolean hasSubagents) {
    }
}
