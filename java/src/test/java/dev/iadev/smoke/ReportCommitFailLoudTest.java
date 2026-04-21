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
 * Structural fail-loud test for TASK-0046-0005-005 — verifies
 * both retrofit blocks declare the
 * {@code REPORT_COMMIT_FAILED} exit code (21) and forbid
 * falling back to {@code --no-verify}, satisfying
 * RULE-046-08 (fail loud on status update failure) and
 * story-0046-0005 §5.2.
 */
@DisplayName("story-0046-0005 — fail-loud invariant smoke")
class ReportCommitFailLoudTest {

    private static final Path SKILL = Paths.get(
            "src/main/resources/targets/claude/skills/core/"
                    + "dev/x-epic-implement/SKILL.md");

    @Test
    void executionPlanBlockForbidsNoVerifyFallback()
            throws IOException {
        String body = Files.readString(SKILL,
                StandardCharsets.UTF_8);
        int anchor = body.indexOf(
                "Atomic Commit — Execution Plan");
        assertThat(anchor).isPositive();
        String block = body.substring(anchor,
                Math.min(anchor + 3000, body.length()));
        assertThat(block)
                .as("execution-plan block MUST NOT allow "
                        + "--no-verify bypass")
                .contains("Do NOT fall back to `--no-verify`");
    }

    @Test
    void phaseReportBlockForbidsNoVerifyFallback()
            throws IOException {
        String body = Files.readString(SKILL,
                StandardCharsets.UTF_8);
        int anchor = body.indexOf(
                "Atomic Commit — Phase Report");
        assertThat(anchor).isPositive();
        String block = body.substring(anchor,
                Math.min(anchor + 3500, body.length()));
        assertThat(block)
                .as("phase-report block MUST NOT allow "
                        + "--no-verify bypass")
                .contains("Do NOT fall back to `--no-verify`");
    }

    @Test
    void bothBlocksDeclareExitCode21() throws IOException {
        String body = Files.readString(SKILL,
                StandardCharsets.UTF_8);
        // Exit code 21 = REPORT_COMMIT_FAILED
        int execIdx = body.indexOf(
                "Atomic Commit — Execution Plan");
        int phaseIdx = body.indexOf(
                "Atomic Commit — Phase Report");
        String execBlock = body.substring(execIdx,
                Math.min(execIdx + 3000, body.length()));
        String phaseBlock = body.substring(phaseIdx,
                Math.min(phaseIdx + 3500, body.length()));
        assertThat(execBlock).contains("(21)");
        assertThat(phaseBlock).contains("(21)");
    }
}
