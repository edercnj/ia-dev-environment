package dev.iadev.smoke;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Story-0038-0003 validation: the refactored {@code x-task-plan} skill's source-of-truth
 * SKILL.md and its accompanying example plan-file must document the contract introduced by
 * EPIC-0038 (task-file-first invocation) alongside the legacy story-scoped mode.
 *
 * <p>The skill itself is LLM instructions, not executable Java — we cannot "invoke" it
 * from a unit test. Instead we validate its documentation: that the EPIC-0038 contract
 * surface is present, that the example plan file conforms to the schema at
 * {@code plans/epic-0038/schemas/plan-task-schema.md}, and that the referenced task-file
 * fixture exists and is well-formed.</p>
 */
class XTaskPlanSkillDocumentationTest {

    private static final Path SKILL_MD = Path.of(
            "src", "main", "resources", "targets", "claude", "skills",
            "core", "plan", "x-task-plan", "SKILL.md");

    private static final Path TASK_FIXTURE = Path.of(
            "..", "plans", "epic-0038", "examples", "task-TASK-0038-0003-EXAMPLE.md");

    private static final Path PLAN_GOLDEN = Path.of(
            "src", "test", "resources", "smoke", "epic-0038",
            "plan-task-TASK-0038-0003-EXAMPLE.md");

    @Nested
    class SkillDocumentation {

        @Test
        void skillMd_documentsBothInvocationModes() throws IOException {
            String md = Files.readString(SKILL_MD, StandardCharsets.UTF_8);
            assertThat(md).contains("--task-file").contains("--output-dir")
                    .contains("Task-file-first mode")
                    .contains("Story-scoped mode");
        }

        @Test
        void skillMd_mentionsStandaloneOrViaXStoryPlan() throws IOException {
            String md = Files.readString(SKILL_MD, StandardCharsets.UTF_8);
            assertThat(md).contains("standalone OR via x-story-plan");
        }

        @Test
        void skillMd_documentsFourExitCodes() throws IOException {
            String md = Files.readString(SKILL_MD, StandardCharsets.UTF_8);
            assertThat(md)
                    .contains("Plan written to")
                    .contains("Task file invalid")
                    .contains("Output dir not writable")
                    .contains("Testability not declared (RULE-TF-01)");
        }

        @Test
        void skillMd_phase1SplitsByInvocationMode() throws IOException {
            String md = Files.readString(SKILL_MD, StandardCharsets.UTF_8);
            assertThat(md).contains("1A. Task-file-first branch")
                    .contains("1B. Story-scoped branch");
        }
    }

    @Nested
    class FixtureAndGolden {

        @Test
        void taskFixture_hasExpectedHeaderAndSchemaSections() throws IOException {
            String md = Files.readString(TASK_FIXTURE, StandardCharsets.UTF_8);
            assertThat(md)
                    .contains("**ID:** TASK-0038-0003-EXAMPLE")
                    .contains("**Story:** story-0038-0003")
                    .contains("## 1. Objetivo")
                    .contains("### 2.3 Testabilidade")
                    .contains("[x] Independentemente testável");
        }

        @Test
        void planGolden_containsAllFiveRequiredSections() throws IOException {
            String md = Files.readString(PLAN_GOLDEN, StandardCharsets.UTF_8);
            assertThat(md)
                    .contains("# Task Implementation Plan — TASK-0038-0003-EXAMPLE")
                    .contains("## 1. Resumo")
                    .contains("## 2. Red-Green-Refactor Cycles (TPP Order)")
                    .contains("## 3. File Impact Analysis")
                    .contains("## 4. TPP Justification")
                    .contains("## 5. Exit Criteria");
        }

        @Test
        void planGolden_listsCyclesInTppOrderStartingWithDegenerate() throws IOException {
            String md = Files.readString(PLAN_GOLDEN, StandardCharsets.UTF_8);
            int firstCycle = md.indexOf("| 1 |");
            int lastCycle = md.indexOf("| 5 |");
            assertThat(firstCycle).isPositive();
            assertThat(lastCycle).isGreaterThan(firstCycle);
            int degenerateIdx = md.indexOf("Degenerate", firstCycle);
            int edgeIdx = md.indexOf("Edge case", lastCycle);
            assertThat(degenerateIdx).isPositive().isLessThan(edgeIdx);
        }

        @Test
        void planGolden_referencesSourceTaskFile() throws IOException {
            String md = Files.readString(PLAN_GOLDEN, StandardCharsets.UTF_8);
            assertThat(md).contains("task-TASK-0038-0003-EXAMPLE.md");
        }
    }
}
