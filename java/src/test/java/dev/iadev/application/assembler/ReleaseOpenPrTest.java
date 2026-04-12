package dev.iadev.application.assembler;

import dev.iadev.testutil.TestConfigBuilder;

import dev.iadev.template.TemplateEngine;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for x-release skill EPIC-0035 story-0035-0003:
 * Substitute direct merge-to-main by PR via gh CLI.
 *
 * <p>Validates that the legacy Step 7 MERGE-MAIN is
 * removed and the new Phase OPEN-RELEASE-PR is
 * documented with {@code gh pr create --base main --head
 * release/X.Y.Z}, CHANGELOG extraction, PR body template,
 * state-file persistence of {@code prNumber}/{@code
 * prUrl}/{@code prTitle}/{@code changelogEntry}, and the
 * new {@code --skip-review} flag. Also enforces RULE-001
 * (no {@code git merge main} in the PR-flow step) and
 * RULE-002 (behavior preservation for existing flags).
 */
@DisplayName("x-release Skill — story-0035-0003 OPEN-RELEASE-PR")
class ReleaseOpenPrTest {

    @Nested
    @DisplayName("Frontmatter / Parameters — --skip-review")
    class SkipReviewFlag {

        @Test
        @DisplayName("argument-hint declares --skip-review")
        void argumentHint_hasSkipReviewFlag(
                @TempDir Path tempDir)
                throws IOException {
            String content = generateClaudeContent(tempDir);
            int frontmatterEnd = content.indexOf("---\n",
                    content.indexOf("---\n") + 4);
            assertThat(frontmatterEnd).isPositive();
            String frontmatter = content.substring(0,
                    frontmatterEnd);
            assertThat(frontmatter)
                    .contains("argument-hint:")
                    .contains("--skip-review");
        }

        @Test
        @DisplayName("Parameters table documents"
                + " --skip-review")
        void parametersTable_hasSkipReview(
                @TempDir Path tempDir)
                throws IOException {
            String content = generateClaudeContent(tempDir);
            assertThat(content)
                    .contains("| `--skip-review`");
        }

        @Test
        @DisplayName("description mentions skip-review"
                + " or review integration")
        void description_mentionsReviewIntegration(
                @TempDir Path tempDir)
                throws IOException {
            String content = generateClaudeContent(tempDir);
            assertThat(content).contains("x-review-pr");
        }
    }

    @Nested
    @DisplayName("Workflow Box — Step 7 replaced")
    class WorkflowBox {

        @Test
        @DisplayName("workflow box lists OPEN-RELEASE-PR"
                + " phase")
        void workflowBox_hasOpenReleasePr(
                @TempDir Path tempDir)
                throws IOException {
            String content = generateClaudeContent(tempDir);
            assertThat(content).contains("OPEN-RELEASE-PR");
        }

        @Test
        @DisplayName("main workflow box no longer lists"
                + " legacy MERGE-MAIN label")
        void workflowBox_dropsLegacyMergeMainLabel(
                @TempDir Path tempDir)
                throws IOException {
            String content = generateClaudeContent(tempDir);
            int workflowStart = content.indexOf(
                    "0. RESUME-DETECT");
            int workflowEnd = content.indexOf(
                    "> **Note:**", workflowStart);
            assertThat(workflowStart).isPositive();
            assertThat(workflowEnd).isPositive();
            String workflowBox = content.substring(
                    workflowStart, workflowEnd);
            assertThat(workflowBox).doesNotContain(
                    "MERGE-MAIN");
            assertThat(workflowBox).contains(
                    "OPEN-RELEASE-PR");
        }
    }

    @Nested
    @DisplayName("Step 7 body — OPEN-RELEASE-PR")
    class StepSevenBody {

        @Test
        @DisplayName("Step 7 section renamed to"
                + " Open Release PR")
        void stepSeven_renamed(
                @TempDir Path tempDir)
                throws IOException {
            String content = generateClaudeContent(tempDir);
            assertThat(content).contains(
                    "### Step 7 \u2014 Open Release PR");
        }

        @Test
        @DisplayName("Step 7 pushes the release branch"
                + " before opening the PR")
        void stepSeven_pushesReleaseBranch(
                @TempDir Path tempDir)
                throws IOException {
            String content = generateClaudeContent(tempDir);
            assertThat(content).contains(
                    "git push -u origin \"release/"
                            + "${VERSION}\"");
        }

        @Test
        @DisplayName("Step 7 invokes gh pr create with"
                + " --base main --head release/X.Y.Z")
        void stepSeven_invokesGhPrCreate(
                @TempDir Path tempDir)
                throws IOException {
            String content = generateClaudeContent(tempDir);
            assertThat(content)
                    .contains("gh pr create")
                    .contains("--base main")
                    .contains("--head \"release/"
                            + "${VERSION}\"");
        }

        @Test
        @DisplayName("Step 7 extracts CHANGELOG entry"
                + " via awk")
        void stepSeven_extractsChangelogViaAwk(
                @TempDir Path tempDir)
                throws IOException {
            String content = generateClaudeContent(tempDir);
            assertThat(content)
                    .contains("CHANGELOG_ENTRY")
                    .contains("awk")
                    .contains("CHANGELOG.md");
        }

        @Test
        @DisplayName("Step 7 documents PR body template"
                + " with Release heading and resume hint")
        void stepSeven_prBodyTemplate(
                @TempDir Path tempDir)
                throws IOException {
            String content = generateClaudeContent(tempDir);
            assertThat(content)
                    .contains("## Release v")
                    .contains("--continue-after-merge");
        }

        @Test
        @DisplayName("Step 7 persists prNumber, prUrl,"
                + " prTitle and changelogEntry to state")
        void stepSeven_persistsStateFields(
                @TempDir Path tempDir)
                throws IOException {
            String content = generateClaudeContent(tempDir);
            assertThat(content)
                    .contains("prNumber")
                    .contains("prUrl")
                    .contains("prTitle")
                    .contains("changelogEntry");
        }

        @Test
        @DisplayName("Step 7 transitions state phase"
                + " to PR_OPENED")
        void stepSeven_transitionsToPrOpened(
                @TempDir Path tempDir)
                throws IOException {
            String content = generateClaudeContent(tempDir);
            // PR_OPENED must appear in the Step 7 body,
            // not just in the state-file-schema reference.
            int stepSeven = content.indexOf(
                    "### Step 7 \u2014 Open Release PR");
            int stepEight = content.indexOf(
                    "### Step 8 \u2014 Tag Creation");
            assertThat(stepSeven).isPositive();
            assertThat(stepEight).isPositive();
            String stepSevenBody = content.substring(
                    stepSeven, stepEight);
            assertThat(stepSevenBody)
                    .contains("PR_OPENED")
                    .contains("OPEN_RELEASE_PR");
        }

        @Test
        @DisplayName("Step 7 honours --no-publish by"
                + " skipping gh pr create")
        void stepSeven_honoursNoPublishDryRun(
                @TempDir Path tempDir)
                throws IOException {
            String content = generateClaudeContent(tempDir);
            int stepSeven = content.indexOf(
                    "### Step 7 \u2014 Open Release PR");
            int stepEight = content.indexOf(
                    "### Step 8 \u2014 Tag Creation");
            String stepSevenBody = content.substring(
                    stepSeven, stepEight);
            assertThat(stepSevenBody)
                    .contains("--no-publish");
        }

        @Test
        @DisplayName("Step 7 invokes x-review-pr when"
                + " --skip-review is absent")
        void stepSeven_invokesReviewPrFireAndForget(
                @TempDir Path tempDir)
                throws IOException {
            String content = generateClaudeContent(tempDir);
            int stepSeven = content.indexOf(
                    "### Step 7 \u2014 Open Release PR");
            int stepEight = content.indexOf(
                    "### Step 8 \u2014 Tag Creation");
            String stepSevenBody = content.substring(
                    stepSeven, stepEight);
            assertThat(stepSevenBody)
                    .contains("x-review-pr")
                    .contains("--skip-review");
        }

        @Test
        @DisplayName("Step 7 body contains NO 'git merge"
                + " main' command (RULE-001)")
        void stepSeven_noGitMergeMain(
                @TempDir Path tempDir)
                throws IOException {
            String content = generateClaudeContent(tempDir);
            int stepSeven = content.indexOf(
                    "### Step 7 \u2014 Open Release PR");
            int stepEight = content.indexOf(
                    "### Step 8 \u2014 Tag Creation");
            String stepSevenBody = content.substring(
                    stepSeven, stepEight);
            assertThat(stepSevenBody)
                    .doesNotContain("git merge \"release/"
                            + "${VERSION}\" --no-ff");
            assertThat(stepSevenBody)
                    .doesNotContain("git checkout main");
        }
    }

    @Nested
    @DisplayName("Error Codes — story-0035-0003 catalog")
    class ErrorCodes {

        @Test
        @DisplayName("documents PR_NO_CHANGELOG_ENTRY"
                + " error code")
        void errorCatalog_hasNoChangelogEntry(
                @TempDir Path tempDir)
                throws IOException {
            String content = generateClaudeContent(tempDir);
            assertThat(content).contains(
                    "PR_NO_CHANGELOG_ENTRY");
        }

        @Test
        @DisplayName("documents PR_CREATE_FAILED"
                + " error code")
        void errorCatalog_hasCreateFailed(
                @TempDir Path tempDir)
                throws IOException {
            String content = generateClaudeContent(tempDir);
            assertThat(content).contains("PR_CREATE_FAILED");
        }

        @Test
        @DisplayName("documents PR_PUSH_REJECTED"
                + " error code")
        void errorCatalog_hasPushRejected(
                @TempDir Path tempDir)
                throws IOException {
            String content = generateClaudeContent(tempDir);
            assertThat(content).contains(
                    "PR_PUSH_REJECTED");
        }
    }

    @Nested
    @DisplayName("Step 10 PUBLISH — tag-only push")
    class PublishStep {

        @Test
        @DisplayName("Step 10 no longer pushes develop"
                + " directly (moved to PR flow)")
        void stepTen_doesNotPushDevelopDirectly(
                @TempDir Path tempDir)
                throws IOException {
            String content = generateClaudeContent(tempDir);
            int stepTen = content.indexOf(
                    "### Step 10 \u2014 Publish");
            int stepEleven = content.indexOf(
                    "### Step 11 \u2014 Cleanup");
            assertThat(stepTen).isPositive();
            assertThat(stepEleven).isPositive();
            String stepTenBody = content.substring(
                    stepTen, stepEleven);
            assertThat(stepTenBody)
                    .doesNotContain("git push origin main")
                    .doesNotContain(
                            "git push origin develop");
        }

        @Test
        @DisplayName("Step 10 still pushes the release"
                + " tag to remote")
        void stepTen_pushesTag(
                @TempDir Path tempDir)
                throws IOException {
            String content = generateClaudeContent(tempDir);
            int stepTen = content.indexOf(
                    "### Step 10 \u2014 Publish");
            int stepEleven = content.indexOf(
                    "### Step 11 \u2014 Cleanup");
            String stepTenBody = content.substring(
                    stepTen, stepEleven);
            assertThat(stepTenBody).contains(
                    "git push origin \"v${VERSION}\"");
        }
    }

    @Nested
    @DisplayName("Behavior Preservation (RULE-002)")
    class BehaviorPreservation {

        @Test
        @DisplayName("existing flags still documented"
                + " in argument-hint")
        void preservesExistingFlags(
                @TempDir Path tempDir)
                throws IOException {
            String content = generateClaudeContent(tempDir);
            assertThat(content)
                    .contains("--dry-run")
                    .contains("--skip-tests")
                    .contains("--no-publish")
                    .contains("--hotfix")
                    .contains("--continue-after-merge")
                    .contains("--interactive")
                    .contains("--signed-tag")
                    .contains("--state-file");
        }

        @Test
        @DisplayName("Step 0, 1, 3, 4, 5, 6, 11 remain"
                + " present")
        void preservesCoreSteps(
                @TempDir Path tempDir)
                throws IOException {
            String content = generateClaudeContent(tempDir);
            assertThat(content)
                    .contains("### Step 0 \u2014 Resume"
                            + " Detection")
                    .contains("### Step 1 \u2014 Determine"
                            + " Version")
                    .contains("### Step 3 \u2014 Branch"
                            + " Creation")
                    .contains("### Step 4 \u2014 Update"
                            + " Version Files")
                    .contains("### Step 5 \u2014 Changelog"
                            + " Generation")
                    .contains("### Step 6 \u2014 Commit"
                            + " Release")
                    .contains("### Step 11 \u2014 Cleanup");
        }
    }

    private Path generateOutput(Path tempDir)
            throws IOException {
        Path outputDir = tempDir.resolve("output");
        Files.createDirectories(outputDir);
        SkillsAssembler assembler = new SkillsAssembler();
        assembler.assemble(
                TestConfigBuilder.minimal(),
                new TemplateEngine(), outputDir);
        return outputDir;
    }

    private String generateClaudeContent(Path tempDir)
            throws IOException {
        Path outputDir = generateOutput(tempDir);
        return Files.readString(
                outputDir.resolve(
                        "skills/x-release/SKILL.md"),
                StandardCharsets.UTF_8);
    }
}
