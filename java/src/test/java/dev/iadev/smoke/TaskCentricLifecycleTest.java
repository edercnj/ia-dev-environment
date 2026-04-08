package dev.iadev.smoke;

import dev.iadev.application.assembler.AssemblerPipeline;
import dev.iadev.application.assembler.PipelineOptions;
import dev.iadev.config.ConfigProfiles;
import dev.iadev.domain.model.PipelineResult;
import dev.iadev.domain.model.ProjectConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Validates the task-centric lifecycle rewrite
 * (story-0029-0015).
 *
 * <p>Acceptance criteria:
 * <ul>
 *   <li>Phase 2 is Task Execution Loop</li>
 *   <li>Phase 3 is Story-Level Verification</li>
 *   <li>--auto-approve-pr flag documented</li>
 *   <li>Resume per task with execution-state.json</li>
 *   <li>Delegation to x-tdd, x-pr-create, x-commit</li>
 *   <li>Backward compatibility for stories without
 *       formal tasks</li>
 *   <li>RULE-001 (1 task = 1 branch = 1 PR)</li>
 * </ul>
 */
@DisplayName("Task-Centric Lifecycle (story-0029-0015)")
class TaskCentricLifecycleTest {

    private static final String PROFILE = "java-spring";

    @TempDir
    Path tempDir;

    private Path outputDir;
    private String lifecycleContent;

    @BeforeEach
    void setUp() throws IOException {
        outputDir = tempDir.resolve(PROFILE);
        SmokeTestValidators
                .createDirectoryQuietly(outputDir);
        ProjectConfig config =
                ConfigProfiles.getStack(PROFILE);
        AssemblerPipeline pipeline =
                new AssemblerPipeline(
                        AssemblerPipeline.buildAssemblers());
        PipelineOptions options = new PipelineOptions(
                false, true, false, null);
        PipelineResult result =
                pipeline.runPipeline(
                        config, outputDir, options);
        assertThat(result.success())
                .as("Pipeline must succeed")
                .isTrue();
        lifecycleContent = readSkill("x-dev-lifecycle");
    }

    @Nested
    @DisplayName("Phase 2 -- Task Execution Loop")
    class Phase2TaskExecutionLoop {

        @Test
        @DisplayName("contains Task Execution Loop"
                + " section header")
        void phase2_containsTaskExecutionLoop() {
            assertThat(lifecycleContent)
                    .contains("Task Execution Loop");
        }

        @Test
        @DisplayName("Phase 2 references x-tdd skill")
        void phase2_referencesXTdd() {
            assertThat(lifecycleContent)
                    .contains("/x-tdd");
        }

        @Test
        @DisplayName("Phase 2 references x-pr-create"
                + " skill")
        void phase2_referencesXPrCreate() {
            assertThat(lifecycleContent)
                    .contains("/x-pr-create");
        }

        @Test
        @DisplayName("Phase 2 references x-commit skill")
        void phase2_referencesXCommit() {
            assertThat(lifecycleContent)
                    .contains("x-commit");
        }

        @Test
        @DisplayName("RULE-001 documented: 1 task = 1"
                + " branch = 1 PR")
        void phase2_ruleOneDocumented() {
            assertThat(lifecycleContent)
                    .contains("1 branch = 1 PR");
        }

        @Test
        @DisplayName("approval gate with"
                + " APPROVE/REJECT/PAUSE")
        void phase2_approvalGateOptions() {
            assertThat(lifecycleContent)
                    .contains("APPROVE")
                    .contains("REJECT")
                    .contains("PAUSE");
        }

        @Test
        @DisplayName("task branch naming pattern"
                + " documented")
        void phase2_taskBranchNaming() {
            assertThat(lifecycleContent)
                    .contains(
                            "feat/task-XXXX-YYYY-NNN-desc");
        }
    }

    @Nested
    @DisplayName("Phase 3 -- Story-Level Verification")
    class Phase3StoryVerification {

        @Test
        @DisplayName("contains Story-Level Verification"
                + " section")
        void phase3_containsStoryVerification() {
            assertThat(lifecycleContent)
                    .contains("Story-Level Verification");
        }

        @Test
        @DisplayName("Phase 3 includes coverage"
                + " consolidation")
        void phase3_coverageConsolidation() {
            assertThat(lifecycleContent)
                    .contains("Coverage Consolidation");
        }

        @Test
        @DisplayName("Phase 3 includes cross-file"
                + " consistency")
        void phase3_crossFileConsistency() {
            assertThat(lifecycleContent)
                    .contains(
                            "Cross-File Consistency Check");
        }

        @Test
        @DisplayName("Phase 3 includes review"
                + " invocation")
        void phase3_reviewInvocation() {
            assertThat(lifecycleContent)
                    .contains("/x-review");
        }

        @Test
        @DisplayName("Phase 3 includes Tech Lead"
                + " review")
        void phase3_techLeadReview() {
            assertThat(lifecycleContent)
                    .contains("x-review-pr");
        }
    }

    @Nested
    @DisplayName("--auto-approve-pr flag")
    class AutoApproveFlag {

        @Test
        @DisplayName("CLI argument documented")
        void autoApprove_cliArgumentDocumented() {
            assertThat(lifecycleContent)
                    .contains("--auto-approve-pr");
        }

        @Test
        @DisplayName("parent branch creation"
                + " documented")
        void autoApprove_parentBranchCreation() {
            assertThat(lifecycleContent).contains(
                    "feat/story-XXXX-YYYY-desc");
        }

        @Test
        @DisplayName("parent branch NEVER auto-merges"
                + " to develop")
        void autoApprove_neverAutoMergeToDevelop() {
            assertThat(lifecycleContent).contains(
                    "NEVER");
        }

        @Test
        @DisplayName("RULE-004 referenced")
        void autoApprove_rule004Referenced() {
            assertThat(lifecycleContent)
                    .contains("RULE-004");
        }
    }

    @Nested
    @DisplayName("Resume per task (RULE-014)")
    class ResumePerTask {

        @Test
        @DisplayName("execution-state.json documented")
        void resume_executionStateDocumented() {
            assertThat(lifecycleContent)
                    .contains("execution-state.json");
        }

        @Test
        @DisplayName("resume reclassification table"
                + " present")
        void resume_reclassificationTable() {
            assertThat(lifecycleContent)
                    .contains("IN_PROGRESS")
                    .contains("PR_CREATED")
                    .contains("PR_APPROVED")
                    .contains("DONE")
                    .contains("FAILED")
                    .contains("BLOCKED");
        }

        @Test
        @DisplayName("gh pr view for status check")
        void resume_ghPrViewCheck() {
            assertThat(lifecycleContent)
                    .contains("gh pr view");
        }
    }

    @Nested
    @DisplayName("Backward compatibility")
    class BackwardCompatibility {

        @Test
        @DisplayName("stories without formal tasks"
                + " treated as single implicit task")
        void backwardCompat_singleImplicitTask() {
            assertThat(lifecycleContent).contains(
                    "single implicit task");
        }

        @Test
        @DisplayName("G1-G7 fallback preserved")
        void backwardCompat_g17FallbackPreserved() {
            assertThat(lifecycleContent)
                    .contains("G1-G7 Fallback");
        }

        @Test
        @DisplayName("PRE_PLANNED mode still"
                + " supported")
        void backwardCompat_prePlannedMode() {
            assertThat(lifecycleContent)
                    .contains("PRE_PLANNED");
        }
    }

    @Nested
    @DisplayName("Skill delegation table")
    class SkillDelegation {

        @Test
        @DisplayName("integration notes list all"
                + " delegated skills")
        void delegation_integrationNotesComplete() {
            assertThat(lifecycleContent)
                    .contains("x-tdd")
                    .contains("x-commit")
                    .contains("x-pr-create")
                    .contains("x-format")
                    .contains("x-lint")
                    .contains("x-plan-task");
        }
    }

    @Nested
    @DisplayName("Multi-profile consistency")
    class MultiProfile {

        @ParameterizedTest(name = "[{0}]")
        @MethodSource(
                "dev.iadev.smoke"
                        + ".TaskCentricLifecycleTest"
                        + "#representativeProfiles")
        @DisplayName("lifecycle contains Task Execution"
                + " Loop")
        void allProfiles_containsTaskExecutionLoop(
                String profile) throws IOException {
            Path out = runProfile(profile);
            String content = Files.readString(
                    out.resolve(
                            ".claude/skills/"
                                    + "x-dev-lifecycle/"
                                    + "SKILL.md"),
                    StandardCharsets.UTF_8);
            assertThat(content)
                    .as("[%s] x-dev-lifecycle must"
                                    + " contain Task"
                                    + " Execution Loop",
                            profile)
                    .contains("Task Execution Loop");
        }

        @ParameterizedTest(name = "[{0}]")
        @MethodSource(
                "dev.iadev.smoke"
                        + ".TaskCentricLifecycleTest"
                        + "#representativeProfiles")
        @DisplayName("lifecycle contains"
                + " --auto-approve-pr")
        void allProfiles_containsAutoApprove(
                String profile) throws IOException {
            Path out = runProfile(profile);
            String content = Files.readString(
                    out.resolve(
                            ".claude/skills/"
                                    + "x-dev-lifecycle/"
                                    + "SKILL.md"),
                    StandardCharsets.UTF_8);
            assertThat(content)
                    .as("[%s] x-dev-lifecycle must"
                                    + " contain"
                                    + " --auto-approve-pr",
                            profile)
                    .contains("--auto-approve-pr");
        }
    }

    static Stream<String> representativeProfiles() {
        return Stream.of(
                "go-gin",
                "java-quarkus",
                "kotlin-ktor",
                "python-fastapi",
                "rust-axum",
                "typescript-nestjs");
    }

    private Path runProfile(String profile)
            throws IOException {
        Path out = tempDir.resolve(
                "multi-" + profile);
        Files.createDirectories(out);
        ProjectConfig config =
                ConfigProfiles.getStack(profile);
        AssemblerPipeline pipeline =
                new AssemblerPipeline(
                        AssemblerPipeline.buildAssemblers());
        PipelineOptions options = new PipelineOptions(
                false, true, false, null);
        PipelineResult result =
                pipeline.runPipeline(
                        config, out, options);
        assertThat(result.success()).isTrue();
        return out;
    }

    private String readSkill(String skillName)
            throws IOException {
        return Files.readString(
                outputDir.resolve(
                        ".claude/skills/" + skillName
                                + "/SKILL.md"),
                StandardCharsets.UTF_8);
    }
}
