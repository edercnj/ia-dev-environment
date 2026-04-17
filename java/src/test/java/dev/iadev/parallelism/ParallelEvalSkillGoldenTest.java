package dev.iadev.parallelism;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Story-0041-0004 validation: the {@code /x-parallel-eval} SKILL.md exists
 * at the source-of-truth path under
 * {@code targets/claude/skills/core/plan/x-parallel-eval/} and matches its
 * checked-in golden copy byte-for-byte.
 */
@DisplayName("x-parallel-eval SKILL.md golden")
class ParallelEvalSkillGoldenTest {

    private static final Path SKILL_SOURCE = Path.of(
            "src", "main", "resources", "targets", "claude",
            "skills", "core", "plan", "x-parallel-eval",
            "SKILL.md");

    private static final Path SKILL_GOLDEN = Path.of(
            "src", "test", "resources", "golden",
            "x-parallel-eval", "SKILL.md");

    @Test
    void skillMd_existsAtSourceOfTruthPath() {
        assertThat(SKILL_SOURCE)
                .exists()
                .isRegularFile();
    }

    @Test
    void skillMd_matchesGolden_byteForByte()
            throws IOException {
        String source = Files.readString(SKILL_SOURCE);
        String golden = Files.readString(SKILL_GOLDEN);
        assertThat(source).isEqualTo(golden);
    }

    @Test
    void skillMd_declaresUserInvocableAndArgumentHint()
            throws IOException {
        String body = Files.readString(SKILL_SOURCE);
        assertThat(body).contains("user-invocable: true");
        assertThat(body).contains("argument-hint:");
        assertThat(body).contains(
                "parallelism-heuristics");
    }
}
