package dev.iadev.release.validate;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifies the x-release SKILL.md source contains the markers
 * required by story-0039-0004 (parallelization of VALIDATE-DEEP).
 *
 * <p>This is a content-level regression test: every preserved
 * {@code VALIDATE_*} error code must still be present
 * (RULE-005), and the parallel dispatch block must be in place.
 * Running against the source file (not the generated copy)
 * guarantees the generator output inherits these markers.</p>
 */
final class ValidateDeepSkillContentTest {

    private static final Path SKILL_MD = Paths.get(
            "src/main/resources/targets/claude/skills/core/ops/x-release/SKILL.md");

    @Test
    void skillFile_exists() {
        assertThat(SKILL_MD).exists();
    }

    @Test
    void skillFile_declaresMaxParallelArgument() throws IOException {
        String content = Files.readString(SKILL_MD);

        assertThat(content).contains("--max-parallel");
        assertThat(content).contains("1..16");
    }

    @Test
    void skillFile_containsParallelDispatchBlock() throws IOException {
        String content = Files.readString(SKILL_MD);

        // Marker block specific to story-0039-0004
        assertThat(content).contains(
                "Parallel Dispatch of Checks 5-10 (story-0039-0004)");
        assertThat(content).contains("run_check");
        assertThat(content).contains("wait \"$pid\"");
    }

    @Test
    void skillFile_preservesAllValidateCodes() throws IOException {
        String content = Files.readString(SKILL_MD);

        // RULE-005: every VALIDATE_* code from sequential flow preserved
        assertThat(content).contains("VALIDATE_BUILD_FAILED");
        assertThat(content).contains("VALIDATE_COVERAGE_LINE");
        assertThat(content).contains("VALIDATE_COVERAGE_BRANCH");
        assertThat(content).contains("VALIDATE_GOLDEN_DRIFT");
        assertThat(content).contains("VALIDATE_HARDCODED_VERSION");
        assertThat(content).contains("VALIDATE_VERSION_MISMATCH");
        assertThat(content).contains("VALIDATE_GENERATION_DRIFT");
        assertThat(content).contains("VALIDATE_DIRTY_WORKDIR");
        assertThat(content).contains("VALIDATE_WRONG_BRANCH");
        assertThat(content).contains("VALIDATE_EMPTY_UNRELEASED");
    }

    @Test
    void skillFile_documentsParallelizationStrategy() throws IOException {
        String content = Files.readString(SKILL_MD);

        assertThat(content).contains("Parallelization strategy");
        assertThat(content).contains("min(CPU_COUNT, 4)");
        assertThat(content).contains("RULE-005");
    }

    @Test
    void skillFile_sortsFailuresAlphabetically() throws IOException {
        String content = Files.readString(SKILL_MD);

        // Alphabetic sort ensures deterministic abort code
        assertThat(content).contains("LC_ALL=C sort");
    }
}
