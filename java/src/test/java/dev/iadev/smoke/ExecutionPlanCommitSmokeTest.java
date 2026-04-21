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
 * Structural smoke test for TASK-0046-0005-002 — verifies that
 * {@code x-epic-implement/SKILL.md} declares an atomic commit
 * of the epic execution plan, V2-gated per Rule 19, as
 * required by story-0046-0005 §3.1.
 *
 * <p>The test is grep-based because the skill contract is
 * expressed in markdown. A live end-to-end test that actually
 * runs {@code /x-epic-implement} against a toy v2 epic is
 * deferred to manual verification — the markdown invariants
 * checked here are sufficient to catch accidental removal of
 * the commit block or drift of the Rule 19 fallback matrix.</p>
 */
@DisplayName("story-0046-0005 — execution-plan commit retrofit smoke")
class ExecutionPlanCommitSmokeTest {

    private static final Path SKILL = Paths.get(
            "src/main/resources/targets/claude/skills/core/"
                    + "dev/x-epic-implement/SKILL.md");

    @Test
    void skillFileExists() {
        assertThat(SKILL).exists().isRegularFile();
    }

    @Test
    void skillDeclaresAtomicCommitBlockForExecutionPlan()
            throws IOException {
        String body = Files.readString(SKILL,
                StandardCharsets.UTF_8);
        assertThat(body).contains(
                "Atomic Commit — Execution Plan (V2-gated,"
                        + " story-0046-0005)");
    }

    @Test
    void executionPlanBlockInvokesXGitCommitViaSkillTool()
            throws IOException {
        String body = Files.readString(SKILL,
                StandardCharsets.UTF_8);
        int anchor = body.indexOf(
                "Atomic Commit — Execution Plan");
        assertThat(anchor).isPositive();
        String block = body.substring(anchor,
                Math.min(anchor + 3000, body.length()));
        assertThat(block).contains(
                "Skill(skill: \"x-git-commit\"");
    }

    @Test
    void executionPlanBlockReferencesReportCommitMessageBuilder()
            throws IOException {
        String body = Files.readString(SKILL,
                StandardCharsets.UTF_8);
        int anchor = body.indexOf(
                "Atomic Commit — Execution Plan");
        String block = body.substring(anchor,
                Math.min(anchor + 3000, body.length()));
        assertThat(block).contains(
                "ReportCommitMessageBuilder.executionPlan");
    }

    @Test
    void executionPlanBlockDeclaresRule19FallbackMatrix()
            throws IOException {
        String body = Files.readString(SKILL,
                StandardCharsets.UTF_8);
        int anchor = body.indexOf(
                "Atomic Commit — Execution Plan");
        String block = body.substring(anchor,
                Math.min(anchor + 3000, body.length()));
        assertThat(block).contains("planningSchemaVersion");
        assertThat(block).contains("V1 no-op");
        assertThat(block).contains("V2 active");
    }

    @Test
    void executionPlanBlockDeclaresFailLoudExitCode()
            throws IOException {
        String body = Files.readString(SKILL,
                StandardCharsets.UTF_8);
        int anchor = body.indexOf(
                "Atomic Commit — Execution Plan");
        String block = body.substring(anchor,
                Math.min(anchor + 3000, body.length()));
        assertThat(block).contains("REPORT_COMMIT_FAILED");
    }
}
