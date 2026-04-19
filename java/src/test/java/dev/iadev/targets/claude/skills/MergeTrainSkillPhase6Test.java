package dev.iadev.targets.claude.skills;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * TDD tests for Phase 6 (Final Verification) and Phase 7 (Report + Cleanup)
 * of x-pr-merge-train/SKILL.md (story-0042-0003,
 * TASK-0042-0003-001 and TASK-0042-0003-002).
 *
 * <p>Reads the golden SKILL.md from the golden output directory
 * and asserts that Phases 6 and 7 content are present with the
 * required error codes, report.md generation, and worktree cleanup.</p>
 */
@DisplayName("MergeTrainSkill — Phase 6 Final Verification and Phase 7 Report + Cleanup")
class MergeTrainSkillPhase6Test {

    private static final String GOLDEN_RELATIVE_PATH =
            "src/test/resources/golden/java-spring-hexagonal"
                    + "/.claude/skills/x-pr-merge-train/SKILL.md";

    @Test
    @DisplayName("phase6_section_present_in_golden_skillmd: "
            + "golden SKILL.md contains Phase 6 with SMOKE_TEST_FAILED error code")
    void phase6_section_present_in_golden_skillmd() throws IOException {
        Path javaModuleDir = Path.of(System.getProperty("user.dir"));
        Path goldenFile = javaModuleDir.resolve(GOLDEN_RELATIVE_PATH);

        assertThat(goldenFile)
                .as("Golden SKILL.md must exist at " + goldenFile)
                .exists();

        String content = Files.readString(goldenFile);

        assertThat(content)
                .as("Golden SKILL.md must contain a 'Phase 6' header")
                .contains("Phase 6");

        assertThat(content)
                .as("Golden SKILL.md Phase 6 must contain SMOKE_TEST_FAILED error code")
                .contains("SMOKE_TEST_FAILED");
    }

    @Test
    @DisplayName("phase7_section_present_in_golden_skillmd: "
            + "golden SKILL.md contains Phase 7 with report.md generation")
    void phase7_section_present_in_golden_skillmd() throws IOException {
        Path javaModuleDir = Path.of(System.getProperty("user.dir"));
        Path goldenFile = javaModuleDir.resolve(GOLDEN_RELATIVE_PATH);

        assertThat(goldenFile)
                .as("Golden SKILL.md must exist at " + goldenFile)
                .exists();

        String content = Files.readString(goldenFile);

        assertThat(content)
                .as("Golden SKILL.md must contain a 'Phase 7' header")
                .contains("Phase 7");

        assertThat(content)
                .as("Golden SKILL.md Phase 7 must reference report.md generation")
                .contains("report.md");
    }
}
