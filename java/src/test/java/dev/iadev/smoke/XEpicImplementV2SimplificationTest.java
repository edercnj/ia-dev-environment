package dev.iadev.smoke;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Story-0038-0007 validation: x-epic-implement documents its v2 scope reduction —
 * task management is fully delegated to x-story-implement; the epic orchestrator
 * handles only story-level concerns (phase order, story PR, epic verification).
 */
class XEpicImplementV2SimplificationTest {

    private static final Path SKILL_MD = Path.of(
            "src", "main", "resources", "targets", "claude", "skills",
            "core", "dev", "x-epic-implement", "SKILL.md");

    @Nested
    class Description {
        @Test
        void mentionsTaskManagementDelegatedAndTasksInvisible() throws IOException {
            String md = Files.readString(SKILL_MD, StandardCharsets.UTF_8);
            assertThat(md).contains("EPIC-0038 simplification")
                    .contains("Tasks are invisible at the epic level");
        }
    }

    @Nested
    class V2Appendix {
        @Test
        void scopeReductionMatrixPresent() throws IOException {
            String md = Files.readString(SKILL_MD, StandardCharsets.UTF_8);
            assertThat(md).contains("Scope Reduction Matrix")
                    .contains("Task-level tracking")
                    .contains("Per-task PR creation")
                    .contains("Coalesced task bundling");
        }

        @Test
        void projectionReadsOnlyStoryLevel() throws IOException {
            String md = Files.readString(SKILL_MD, StandardCharsets.UTF_8);
            assertThat(md).contains("execution-state.json Projection")
                    .contains("READ-ONLY from epic");
        }

        @Test
        void argumentSurfacePreserved() throws IOException {
            String md = Files.readString(SKILL_MD, StandardCharsets.UTF_8);
            assertThat(md).contains("Argument Surface (unchanged)")
                    .contains("--batch-approval")
                    .contains("--task-tracking")
                    .contains("--auto-approve-pr");
        }

        @Test
        void dispatcherInvocationContractDocumented() throws IOException {
            String md = Files.readString(SKILL_MD, StandardCharsets.UTF_8);
            assertThat(md).contains("Dispatcher Invocation Contract")
                    .contains("Skill(skill: \"x-story-implement\"")
                    .contains("waveCount")
                    .contains("taskCount");
        }

        @Test
        void benefitsEnumerated() throws IOException {
            String md = Files.readString(SKILL_MD, StandardCharsets.UTF_8);
            assertThat(md).contains("v2 Benefits")
                    .contains("Reduced maintenance surface")
                    .contains("Cleaner mental model")
                    .contains("Failure attribution");
        }
    }
}
