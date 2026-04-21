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
 * Structural smoke test for TASK-0046-0005-003 — verifies that
 * {@code x-epic-implement/SKILL.md} declares an atomic commit
 * of per-wave phase reports, V2-gated per Rule 19, as required
 * by story-0046-0005 §3.2 and §3.4.
 */
@DisplayName("story-0046-0005 — phase-report commit retrofit smoke")
class PhaseReportCommitsSmokeTest {

    private static final Path SKILL = Paths.get(
            "src/main/resources/targets/claude/skills/core/"
                    + "dev/x-epic-implement/SKILL.md");

    @Test
    void skillDeclaresAtomicCommitBlockForPhaseReport()
            throws IOException {
        String body = Files.readString(SKILL,
                StandardCharsets.UTF_8);
        assertThat(body).contains(
                "Atomic Commit — Phase Report (V2-gated,"
                        + " story-0046-0005)");
    }

    @Test
    void phaseReportBlockInvokesXGitCommitViaSkillTool()
            throws IOException {
        String body = Files.readString(SKILL,
                StandardCharsets.UTF_8);
        int anchor = body.indexOf(
                "Atomic Commit — Phase Report");
        assertThat(anchor).isPositive();
        String block = body.substring(anchor,
                Math.min(anchor + 3500, body.length()));
        assertThat(block).contains(
                "Skill(skill: \"x-git-commit\"");
    }

    @Test
    void phaseReportBlockReferencesReportCommitMessageBuilder()
            throws IOException {
        String body = Files.readString(SKILL,
                StandardCharsets.UTF_8);
        int anchor = body.indexOf(
                "Atomic Commit — Phase Report");
        String block = body.substring(anchor,
                Math.min(anchor + 3500, body.length()));
        assertThat(block).contains(
                "ReportCommitMessageBuilder.phaseReport");
    }

    @Test
    void phaseReportBlockIsV2GatedWithRule19Matrix()
            throws IOException {
        String body = Files.readString(SKILL,
                StandardCharsets.UTF_8);
        int anchor = body.indexOf(
                "Atomic Commit — Phase Report");
        String block = body.substring(anchor,
                Math.min(anchor + 3500, body.length()));
        assertThat(block).contains("planningSchemaVersion");
        assertThat(block).contains("V1 no-op");
        assertThat(block).contains("V2 active");
    }

    @Test
    void phaseReportBlockDeclaresFailLoudExitCode()
            throws IOException {
        String body = Files.readString(SKILL,
                StandardCharsets.UTF_8);
        int anchor = body.indexOf(
                "Atomic Commit — Phase Report");
        String block = body.substring(anchor,
                Math.min(anchor + 3500, body.length()));
        assertThat(block).contains("REPORT_COMMIT_FAILED");
    }

    @Test
    void phaseReportBlockDocumentsCanonicalV2Ordering()
            throws IOException {
        String body = Files.readString(SKILL,
                StandardCharsets.UTF_8);
        int anchor = body.indexOf(
                "Atomic Commit — Phase Report");
        String block = body.substring(anchor,
                Math.min(anchor + 3500, body.length()));
        assertThat(block).contains("Phase 1.7");
        assertThat(block).contains("Phase 5");
    }
}
