package dev.iadev.targets.claude.skills;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * TDD tests for Integration Notes and Examples sections in
 * x-pr-merge-train/SKILL.md (story-0042-0003, TASK-0042-0003-005).
 *
 * <p>Reads the golden SKILL.md from the golden output directory
 * and asserts that the Integration Notes table and ≥ 4 Examples
 * (including --resume) are present.</p>
 */
@DisplayName("MergeTrainSkill — Integration Notes and Examples")
class MergeTrainSkillExamplesTest {

    private static final String GOLDEN_RELATIVE_PATH =
            "src/test/resources/golden/java-spring-hexagonal"
                    + "/.claude/skills/x-pr-merge-train/references/full-protocol.md";

    @Test
    @DisplayName("integration_notes_and_examples_present_in_golden_skillmd: "
            + "golden SKILL.md contains Integration Notes table and Examples with --resume")
    void integration_notes_and_examples_present_in_golden_skillmd() throws IOException {
        Path javaModuleDir = Path.of(System.getProperty("user.dir"));
        Path goldenFile = javaModuleDir.resolve(GOLDEN_RELATIVE_PATH);

        assertThat(goldenFile)
                .as("Golden SKILL.md must exist at " + goldenFile)
                .exists();

        String content = Files.readString(goldenFile);

        assertThat(content)
                .as("Golden SKILL.md must contain 'Integration Notes' section header")
                .contains("Integration Notes");

        assertThat(content)
                .as("Golden SKILL.md Integration Notes must reference x-pr-fix-epic")
                .contains("x-pr-fix-epic");

        assertThat(content)
                .as("Golden SKILL.md Examples must contain --resume --train-id invocation")
                .contains("--resume --train-id");
    }
}
