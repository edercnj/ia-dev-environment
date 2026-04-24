package dev.iadev.targets.claude.skills;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * TDD tests for Phase 3 (Sort + File-Overlap Precheck) of
 * x-pr-merge-train/SKILL.md (story-0042-0002, TASK-0042-0002-001).
 *
 * <p>Reads the golden SKILL.md from the golden output directory
 * and asserts that Phase 3 content and the NEUTERED_PARALLEL
 * telemetry code are present.</p>
 */
@DisplayName("MergeTrainSkill — Phase 3 Sort + File-Overlap Precheck")
class MergeTrainSkillPhase3Test {

    private static final String GOLDEN_FULL_PROTOCOL_RELATIVE_PATH =
            "src/test/resources/golden/java-spring-hexagonal"
                    + "/.claude/skills/x-pr-merge-train/references/full-protocol.md";

    @Test
    @DisplayName("phase3_section_present_in_skillmd: "
            + "golden SKILL.md contains Phase 3 header and NEUTERED_PARALLEL code")
    void phase3_section_present_in_skillmd() throws IOException {
        // user.dir is the java/ module directory during mvn test
        Path javaModuleDir = Path.of(System.getProperty("user.dir"));
        Path goldenFile = javaModuleDir.resolve(GOLDEN_FULL_PROTOCOL_RELATIVE_PATH);

        assertThat(goldenFile)
                .as("Golden SKILL.md must exist at " + goldenFile)
                .exists();

        String content = Files.readString(goldenFile);

        assertThat(content)
                .as("Golden SKILL.md must contain a 'Phase 3' header")
                .contains("Phase 3");

        assertThat(content)
                .as("Golden SKILL.md must contain 'NEUTERED_PARALLEL' "
                        + "telemetry code for file-overlap precheck")
                .contains("NEUTERED_PARALLEL");
    }
}
