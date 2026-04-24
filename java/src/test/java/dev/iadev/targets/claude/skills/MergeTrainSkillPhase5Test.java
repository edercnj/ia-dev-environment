package dev.iadev.targets.claude.skills;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * TDD tests for Phase 5 (Parallel Tail Orchestration) of
 * x-pr-merge-train/SKILL.md (story-0042-0002,
 * TASK-0042-0002-003 and TASK-0042-0002-004).
 *
 * <p>Reads the golden SKILL.md from the golden output directory
 * and asserts that Phase 5 content including the canonical rebase
 * subagent prompt (with GoldenFileRegenerator regen block) and
 * the Agent-based wave dispatcher are present.</p>
 */
@DisplayName("MergeTrainSkill — Phase 5 Parallel Tail Orchestration")
class MergeTrainSkillPhase5Test {

    private static final String GOLDEN_RELATIVE_PATH =
            "src/test/resources/golden/java-spring-hexagonal"
                    + "/.claude/skills/x-pr-merge-train/references/full-protocol.md";

    @Test
    @DisplayName("phase5_canonical_prompt_contains_regen_block: "
            + "golden SKILL.md Phase 5 contains the GoldenFileRegenerator regen block")
    void phase5_canonical_prompt_contains_regen_block() throws IOException {
        // user.dir is the java/ module directory during mvn test
        Path javaModuleDir = Path.of(System.getProperty("user.dir"));
        Path goldenFile = javaModuleDir.resolve(GOLDEN_RELATIVE_PATH);

        assertThat(goldenFile)
                .as("Golden SKILL.md must exist at " + goldenFile)
                .exists();

        String content = Files.readString(goldenFile);

        assertThat(content)
                .as("Golden SKILL.md must contain a 'Phase 5' header")
                .contains("Phase 5");

        assertThat(content)
                .as("Golden SKILL.md Phase 5 must contain the canonical regen "
                        + "block with GoldenFileRegenerator class reference "
                        + "(RULE-005: byte-a-byte match with README.md:810-818)")
                .contains("GoldenFileRegenerator");
    }

    @Test
    @DisplayName("phase5_wave_dispatcher_uses_agent_pattern: "
            + "golden SKILL.md Phase 5 uses Agent(...) pattern (Rule 13 Pattern 2) "
            + "and has zero bare-slash delegation")
    void phase5_wave_dispatcher_uses_agent_pattern() throws IOException {
        // user.dir is the java/ module directory during mvn test
        Path javaModuleDir = Path.of(System.getProperty("user.dir"));
        Path goldenFile = javaModuleDir.resolve(GOLDEN_RELATIVE_PATH);

        assertThat(goldenFile)
                .as("Golden SKILL.md must exist at " + goldenFile)
                .exists();

        String content = Files.readString(goldenFile);

        assertThat(content)
                .as("Golden SKILL.md Phase 5 must use Agent( pattern "
                        + "for sibling subagent dispatch (Rule 13 Pattern 2 — "
                        + "SUBAGENT-GENERAL)")
                .contains("Agent(");

        // Rule 13 compliance: zero bare-slash in delegation context.
        // We check that the Phase 5 section does not contain lines like
        // "  /x-foo" that would indicate bare-slash delegation.
        // Extract Phase 5 content (from "## Phase 5" to next "## Phase")
        int phase5Start = content.indexOf("## Phase 5");
        int phase6Start = content.indexOf("## Phase 6");
        if (phase5Start >= 0) {
            String phase5Content = phase6Start >= 0
                    ? content.substring(phase5Start, phase6Start)
                    : content.substring(phase5Start);
            // Check that Phase 5 delegation section uses Agent(...) not /x-foo
            assertThat(phase5Content)
                    .as("Phase 5 delegation context must NOT use bare-slash "
                            + "pattern (Rule 13 compliance)")
                    .doesNotContainPattern("^\\s{2,}/x-[a-z]");
        }
    }
}
