package dev.iadev.targets.claude.skills;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * TDD tests for Phase 4 (Base PR Merge) of
 * x-pr-merge-train/SKILL.md (story-0042-0002, TASK-0042-0002-002).
 *
 * <p>Reads the golden SKILL.md from the golden output directory
 * and asserts that Phase 4 content and the relevant error codes
 * are present.</p>
 */
@DisplayName("MergeTrainSkill — Phase 4 Base PR Merge")
class MergeTrainSkillPhase4Test {

    private static final String GOLDEN_RELATIVE_PATH =
            "src/test/resources/golden/java-spring-hexagonal"
                    + "/.claude/skills/x-pr-merge-train/references/full-protocol.md";

    @Test
    @DisplayName("phase4_section_present_in_skillmd: "
            + "golden SKILL.md contains Phase 4 header and both error codes")
    void phase4_section_present_in_skillmd() throws IOException {
        // user.dir is the java/ module directory during mvn test
        Path javaModuleDir = Path.of(System.getProperty("user.dir"));
        Path goldenFile = javaModuleDir.resolve(GOLDEN_RELATIVE_PATH);

        assertThat(goldenFile)
                .as("Golden SKILL.md must exist at " + goldenFile)
                .exists();

        String content = Files.readString(goldenFile);

        assertThat(content)
                .as("Golden SKILL.md must contain a 'Phase 4' header")
                .contains("Phase 4");

        assertThat(content)
                .as("Golden SKILL.md must contain 'MERGE_POLL_TIMEOUT' "
                        + "error code for Phase 4 polling timeout")
                .contains("MERGE_POLL_TIMEOUT");

        assertThat(content)
                .as("Golden SKILL.md must contain "
                        + "'MERGE_REJECTED_BY_PROTECTION' "
                        + "error code for branch protection rejection")
                .contains("MERGE_REJECTED_BY_PROTECTION");
    }
}
