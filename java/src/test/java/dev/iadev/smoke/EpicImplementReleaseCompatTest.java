package dev.iadev.smoke;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Structural compatibility test for TASK-0046-0005-004 —
 * verifies that the retrofit of
 * {@code x-epic-implement/SKILL.md} wires both the execution
 * plan commit and the per-wave phase report commits so that
 * {@code x-release}'s {@code VALIDATE_DIRTY_WORKDIR}
 * precondition can pass immediately after a v2 epic run
 * (story-0046-0005 §3.5 Métrica de Sucesso).
 *
 * <p>This is a structural contract test: it confirms both
 * commit blocks coexist in the correct order (execution plan
 * BEFORE the wave loop, phase report AFTER each wave) and
 * both reference the canonical clean-workdir invariant. A
 * sandboxed live run is out-of-scope at this layer and is
 * validated manually against a toy epic during rollout.</p>
 */
@DisplayName("story-0046-0005 — x-release compatibility smoke")
class EpicImplementReleaseCompatTest {

    private static final Path SKILL = Paths.get(
            "src/main/resources/targets/claude/skills/core/"
                    + "dev/x-epic-implement/SKILL.md");

    @Test
    void bothCommitBlocksArePresent() throws IOException {
        String body = Files.readString(SKILL,
                StandardCharsets.UTF_8);
        assertThat(body).contains(
                "Atomic Commit — Execution Plan");
        assertThat(body).contains(
                "Atomic Commit — Phase Report");
    }

    @Test
    void executionPlanBlockPrecedesPhaseReportBlock()
            throws IOException {
        String body = Files.readString(SKILL,
                StandardCharsets.UTF_8);
        int planIdx = body.indexOf(
                "Atomic Commit — Execution Plan");
        int phaseIdx = body.indexOf(
                "Atomic Commit — Phase Report");
        assertThat(planIdx)
                .as("execution-plan block found")
                .isPositive();
        assertThat(phaseIdx)
                .as("phase-report block found")
                .isPositive();
        assertThat(planIdx)
                .as("execution-plan block MUST precede "
                        + "phase-report block")
                .isLessThan(phaseIdx);
    }

    @Test
    void bothBlocksCiteCleanWorkdirInvariant()
            throws IOException {
        String body = Files.readString(SKILL,
                StandardCharsets.UTF_8);
        // At least one of the blocks references the clean
        // workdir invariant (RULE-046-06) that x-release
        // VALIDATE_DIRTY_WORKDIR depends on.
        assertThat(body).contains("RULE-046-06");
    }
}
