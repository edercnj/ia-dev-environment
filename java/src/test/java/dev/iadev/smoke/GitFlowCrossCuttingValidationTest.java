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
 * Cross-cutting validation for Git Flow behavior across
 * all generated skills.
 *
 * <p>Story-0027-0010: validates that all skills affected
 * by the Git Flow migration correctly reference
 * {@code develop} as base branch, document hotfix
 * workflows, and include Git Flow-specific CI triggers.
 * Runs a full pipeline for a representative profile
 * and validates each skill's content.</p>
 *
 * @see SmokeTestBase
 */
@DisplayName("Git Flow Cross-Cutting Validation")
class GitFlowCrossCuttingValidationTest {

    private static final String PROFILE = "java-spring";

    @TempDir
    Path tempDir;

    private Path outputDir;

    @BeforeEach
    void setUp() {
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
    }

    @Nested
    @DisplayName("x-git-push — develop references")
    class GitPushDevelop {

        @Test
        @DisplayName("branch strategy shows develop"
                + " as integration branch")
        void gitPush_branchStrategy_showsDevelop()
                throws IOException {
            String content = readSkill(
                    "x-git-push");
            assertThat(content).contains(
                    "develop (integration,"
                            + " always green)");
        }

        @Test
        @DisplayName("PR creation targets develop")
        void gitPush_prCreation_targetsDevelop()
                throws IOException {
            String content = readSkill(
                    "x-git-push");
            assertThat(content)
                    .contains("--base develop");
        }

        @Test
        @DisplayName("hotfix section exists")
        void gitPush_hotfix_sectionExists()
                throws IOException {
            String content = readSkill(
                    "x-git-push");
            assertThat(content)
                    .contains(
                            "### Step 4 — Hotfix Workflow");
        }
    }

    @Nested
    @DisplayName("x-dev-lifecycle — develop references")
    class LifecycleDevelop {

        @Test
        @DisplayName("Phase 0 creates branch"
                + " (no checkout main)")
        void lifecycle_phase0_noBranchFromMain()
                throws IOException {
            String content = readSkill(
                    "x-dev-lifecycle");
            String phase0 = extractSection(
                    content,
                    "## Phase 0",
                    "## Phase 0.5");
            assertThat(phase0)
                    .doesNotContain("checkout main");
        }

        @Test
        @DisplayName("Phase 6 PR targets develop")
        void lifecycle_phase6_prTargetsDevelop()
                throws IOException {
            String content = readSkill(
                    "x-dev-lifecycle");
            assertThat(content).contains(
                    "gh pr create --base develop");
        }

        @Test
        @DisplayName("review reads commits from develop")
        void lifecycle_review_readsDevelopCommits()
                throws IOException {
            String content = readSkill(
                    "x-dev-lifecycle");
            assertThat(content).contains(
                    "git log develop..HEAD");
        }
    }

    @Nested
    @DisplayName("x-dev-epic-implement"
            + " — develop references")
    class EpicImplementDevelop {

        @Test
        @DisplayName("default merge mode is no-merge")
        void epicImplement_defaultMerge_isNoMerge()
                throws IOException {
            String content = readSkill(
                    "x-dev-epic-implement");
            assertThat(content).contains(
                    "mergeMode = \"no-merge\"");
        }

        @Test
        @DisplayName("baseBranch defaults to develop")
        void epicImplement_baseBranch_defaultsDevelop()
                throws IOException {
            String content = readSkill(
                    "x-dev-epic-implement");
            assertThat(content).contains(
                    "`baseBranch`: `\"develop\"`");
        }

        @Test
        @DisplayName("auto-rebase references develop")
        void epicImplement_autoRebase_refsDevelop()
                throws IOException {
            String content = readSkill(
                    "x-dev-epic-implement");
            assertThat(content)
                    .contains("auto-rebase");
        }

        @Test
        @DisplayName("orchestrator stays on develop")
        void epicImplement_orchestrator_staysDevelop()
                throws IOException {
            String content = readSkill(
                    "x-dev-epic-implement");
            assertThat(content).contains(
                    "orchestrator stays on"
                            + " `develop`");
        }
    }

    @Nested
    @DisplayName("x-release — release branch workflow")
    class ReleaseBranchWorkflow {

        @Test
        @DisplayName("contains 11 workflow steps")
        void release_workflow_hasElevenSteps()
                throws IOException {
            String content = readSkill("x-release");
            assertThat(content)
                    .contains("DETERMINE")
                    .contains("VALIDATE")
                    .contains("BRANCH")
                    .contains("MERGE-MAIN")
                    .contains("TAG")
                    .contains("MERGE-BACK")
                    .contains("CLEANUP");
        }

        @Test
        @DisplayName("hotfix branches from main")
        void release_hotfix_branchesFromMain()
                throws IOException {
            String content = readSkill("x-release");
            assertThat(content)
                    .contains("Hotfix Release");
        }
    }

    @Nested
    @DisplayName("x-ci-cd-generate"
            + " — multi-branch triggers")
    class CiCdTriggers {

        @Test
        @DisplayName("CI workflow triggers on develop")
        void cicd_triggers_includesDevelop()
                throws IOException {
            String ciYml = readGithubFile(
                    "workflows/ci.yml");
            assertThat(ciYml)
                    .contains("develop");
        }

        @Test
        @DisplayName("CI workflow triggers on"
                + " release/**")
        void cicd_triggers_includesRelease()
                throws IOException {
            String ciYml = readGithubFile(
                    "workflows/ci.yml");
            assertThat(ciYml)
                    .contains("release/**");
        }

        @Test
        @DisplayName("CI workflow triggers on"
                + " hotfix/**")
        void cicd_triggers_includesHotfix()
                throws IOException {
            String ciYml = readGithubFile(
                    "workflows/ci.yml");
            assertThat(ciYml)
                    .contains("hotfix/**");
        }
    }

    @Nested
    @DisplayName("x-fix-epic-pr-comments"
            + " — develop base")
    class FixEpicPrComments {

        @Test
        @DisplayName("PR targets develop")
        void fixPr_prCreation_targetsDevelop()
                throws IOException {
            String content = readSkill(
                    "x-fix-epic-pr-comments");
            assertThat(content)
                    .contains("--base develop");
        }

        @Test
        @DisplayName("branch setup uses develop")
        void fixPr_branchSetup_usesDevelop()
                throws IOException {
            String content = readSkill(
                    "x-fix-epic-pr-comments");
            assertThat(content)
                    .contains("git checkout develop");
        }
    }

    @Nested
    @DisplayName("Rule 09 — branching model")
    class Rule09 {

        @Test
        @DisplayName("Rule 09 file exists")
        void rule09_exists() {
            assertThat(outputDir.resolve(
                    ".claude/rules/"
                            + "09-branching-model.md"))
                    .exists();
        }

        @Test
        @DisplayName("contains 5 branch types")
        void rule09_hasFiveBranchTypes()
                throws IOException {
            String content = readFile(
                    "rules/09-branching-model.md");
            assertThat(content)
                    .contains("main")
                    .contains("develop")
                    .contains("feature/")
                    .contains("release/")
                    .contains("hotfix/");
        }
    }

    @Nested
    @DisplayName("release-management"
            + " — Git Flow recommended")
    class ReleaseManagement {

        @Test
        @DisplayName("GitFlow marked as recommended")
        void relMgmt_gitFlowRecommended()
                throws IOException {
            String content = readSkill(
                    "release-management");
            assertThat(content).contains(
                    "GitFlow (Recommended)");
        }

        @Test
        @DisplayName("alternatives preserved")
        void relMgmt_alternativesPreserved()
                throws IOException {
            String content = readSkill(
                    "release-management");
            assertThat(content)
                    .contains("Trunk-based")
                    .contains("Release branches");
        }
    }

    @Nested
    @DisplayName("multi-profile consistency")
    class MultiProfileConsistency {

        @ParameterizedTest(name = "[{0}]")
        @MethodSource(
                "dev.iadev.smoke"
                        + ".GitFlowCrossCutting"
                        + "ValidationTest"
                        + "#representativeProfiles")
        @DisplayName("x-git-push references develop")
        void allProfiles_gitPush_refsDevelop(
                String profile) throws IOException {
            Path out = runProfile(profile);
            String content = Files.readString(
                    out.resolve(
                            ".claude/skills/"
                                    + "x-git-push/SKILL.md"),
                    StandardCharsets.UTF_8);
            assertThat(content)
                    .as("[%s] x-git-push must reference"
                            + " develop", profile)
                    .contains("--base develop");
        }

        @ParameterizedTest(name = "[{0}]")
        @MethodSource(
                "dev.iadev.smoke"
                        + ".GitFlowCrossCutting"
                        + "ValidationTest"
                        + "#representativeProfiles")
        @DisplayName("Rule 09 exists with 5 branches")
        void allProfiles_rule09_hasFiveBranches(
                String profile) throws IOException {
            Path out = runProfile(profile);
            String content = Files.readString(
                    out.resolve(
                            ".claude/rules/"
                                    + "09-branching-model.md"),
                    StandardCharsets.UTF_8);
            assertThat(content)
                    .as("[%s] Rule 09 must have 5 branch"
                            + " types", profile)
                    .contains("main")
                    .contains("develop")
                    .contains("feature/")
                    .contains("release/")
                    .contains("hotfix/");
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

    private String readFile(String relativePath)
            throws IOException {
        return Files.readString(
                outputDir.resolve(
                        ".claude/" + relativePath),
                StandardCharsets.UTF_8);
    }

    private String readGithubFile(String relativePath)
            throws IOException {
        return Files.readString(
                outputDir.resolve(
                        ".github/" + relativePath),
                StandardCharsets.UTF_8);
    }

    private static String extractSection(
            String content,
            String start, String end) {
        int startIdx = content.indexOf(start);
        if (startIdx < 0) {
            return "";
        }
        if (end == null) {
            return content.substring(startIdx);
        }
        int endIdx = content.indexOf(end, startIdx);
        if (endIdx < 0) {
            return content.substring(startIdx);
        }
        return content.substring(startIdx, endIdx);
    }
}
