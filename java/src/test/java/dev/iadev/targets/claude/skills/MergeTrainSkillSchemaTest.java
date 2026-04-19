package dev.iadev.targets.claude.skills;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * TDD tests for the state.json schema documentation, atomic-write pattern,
 * and --resume entry logic in x-pr-merge-train/SKILL.md (story-0042-0003,
 * TASK-0042-0003-003).
 *
 * <p>Reads the golden SKILL.md from the golden output directory
 * and asserts that the state.json complete schema, atomic-write (.tmp + rename)
 * pattern, and --resume entry logic are documented.</p>
 */
@DisplayName("MergeTrainSkill — state.json Schema, Atomic Write, and Resume Logic")
class MergeTrainSkillSchemaTest {

    private static final String GOLDEN_RELATIVE_PATH =
            "src/test/resources/golden/java-spring-hexagonal"
                    + "/.claude/skills/x-pr-merge-train/SKILL.md";

    @Test
    @DisplayName("state_schema_section_present_in_golden_skillmd: "
            + "golden SKILL.md contains complete state.json schema with all required fields "
            + "and atomic-write pattern and STATE_CONFLICT resume logic")
    void state_schema_section_present_in_golden_skillmd() throws IOException {
        Path javaModuleDir = Path.of(System.getProperty("user.dir"));
        Path goldenFile = javaModuleDir.resolve(GOLDEN_RELATIVE_PATH);

        assertThat(goldenFile)
                .as("Golden SKILL.md must exist at " + goldenFile)
                .exists();

        String content = Files.readString(goldenFile);

        assertThat(content)
                .as("Golden SKILL.md must contain 'schemaVersion' field in state.json schema")
                .contains("schemaVersion");

        assertThat(content)
                .as("Golden SKILL.md must contain 'lastPhaseCompletedAt' field in state.json schema")
                .contains("lastPhaseCompletedAt");

        assertThat(content)
                .as("Golden SKILL.md must contain STATE_CONFLICT error code in resume logic")
                .contains("STATE_CONFLICT");

        assertThat(content)
                .as("Golden SKILL.md must document atomic-write (.tmp + rename) pattern")
                .contains("state.json.tmp");

        assertThat(content)
                .as("Golden SKILL.md must document --resume entry logic section")
                .contains("Resume Entry Logic");
    }
}
