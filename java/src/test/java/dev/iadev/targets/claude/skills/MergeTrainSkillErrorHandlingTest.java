package dev.iadev.targets.claude.skills;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * TDD tests for the Error Handling table (≥ 11 codes) in
 * x-pr-merge-train/SKILL.md (story-0042-0003, TASK-0042-0003-004).
 *
 * <p>Reads the golden SKILL.md from the golden output directory
 * and asserts that the Error Handling section contains all key error codes
 * including SMOKE_TEST_FAILED, STATE_CONFLICT, and CODE_CONFLICT_NEEDS_HUMAN.</p>
 */
@DisplayName("MergeTrainSkill — Error Handling Table")
class MergeTrainSkillErrorHandlingTest {

    private static final String GOLDEN_RELATIVE_PATH =
            "src/test/resources/golden/java-spring-hexagonal"
                    + "/.claude/skills/x-pr-merge-train/references/full-protocol.md";

    @Test
    @DisplayName("error_handling_table_present_in_golden_skillmd: "
            + "golden SKILL.md contains Error Handling section with all key codes")
    void error_handling_table_present_in_golden_skillmd() throws IOException {
        Path javaModuleDir = Path.of(System.getProperty("user.dir"));
        Path goldenFile = javaModuleDir.resolve(GOLDEN_RELATIVE_PATH);

        assertThat(goldenFile)
                .as("Golden SKILL.md must exist at " + goldenFile)
                .exists();

        String content = Files.readString(goldenFile);

        assertThat(content)
                .as("Golden SKILL.md must contain 'Error Handling' section header")
                .contains("Error Handling");

        assertThat(content)
                .as("Golden SKILL.md Error Handling must contain SMOKE_TEST_FAILED code")
                .contains("SMOKE_TEST_FAILED");

        assertThat(content)
                .as("Golden SKILL.md Error Handling must contain STATE_CONFLICT code")
                .contains("STATE_CONFLICT");

        assertThat(content)
                .as("Golden SKILL.md Error Handling must contain CODE_CONFLICT_NEEDS_HUMAN code")
                .contains("CODE_CONFLICT_NEEDS_HUMAN");
    }
}
