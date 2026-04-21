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
 * Rule 19 backward-compatibility smoke for TASK-0046-0005-005
 * — verifies that the retrofit of
 * {@code x-epic-implement/SKILL.md} is V2-gated, so v1 epics
 * (absent or {@code "1.0"} {@code planningSchemaVersion})
 * retain their legacy behavior: reports remain optional and
 * uncommitted. Enforces story-0046-0005 §3.3 and Rule 19
 * {@code SCHEMA_VERSION_FALLBACK_*}.
 */
@DisplayName("story-0046-0005 — Rule 19 v1-no-op smoke")
class EpicV1NoReportCommitTest {

    private static final Path SKILL = Paths.get(
            "src/main/resources/targets/claude/skills/core/"
                    + "dev/x-epic-implement/SKILL.md");

    @Test
    void executionPlanBlockDeclaresV1Skip() throws IOException {
        String body = Files.readString(SKILL,
                StandardCharsets.UTF_8);
        int anchor = body.indexOf(
                "Atomic Commit — Execution Plan");
        String block = body.substring(anchor,
                Math.min(anchor + 3000, body.length()));
        assertThat(block).contains("V1 no-op");
        assertThat(block).contains(
                "Skip the commit block entirely");
    }

    @Test
    void phaseReportBlockDeclaresV1Skip() throws IOException {
        String body = Files.readString(SKILL,
                StandardCharsets.UTF_8);
        int anchor = body.indexOf(
                "Atomic Commit — Phase Report");
        String block = body.substring(anchor,
                Math.min(anchor + 3500, body.length()));
        assertThat(block).contains("V1 no-op");
        assertThat(block).contains(
                "Skip the commit block entirely");
    }

    @Test
    void bothBlocksListInvalidVersionAsV1NoOp()
            throws IOException {
        String body = Files.readString(SKILL,
                StandardCharsets.UTF_8);
        int execIdx = body.indexOf(
                "Atomic Commit — Execution Plan");
        int phaseIdx = body.indexOf(
                "Atomic Commit — Phase Report");
        String execBlock = body.substring(execIdx,
                Math.min(execIdx + 3000, body.length()));
        String phaseBlock = body.substring(phaseIdx,
                Math.min(phaseIdx + 3500, body.length()));
        // Matches Rule 19 fallback matrix — absent / "1.0" /
        // invalid all collapse to V1 no-op.
        assertThat(execBlock).contains("invalid");
        assertThat(phaseBlock).contains("invalid");
    }

    @Test
    void rule19Referenced() throws IOException {
        String body = Files.readString(SKILL,
                StandardCharsets.UTF_8);
        assertThat(body).contains("Rule 19");
    }
}
