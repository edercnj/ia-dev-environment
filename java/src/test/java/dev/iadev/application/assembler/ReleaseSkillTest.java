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
 * Tests for x-release skill with Git Flow release branch
 * workflow.
 *
 * <p>Validates that the x-release skill template is
 * generated correctly with release branch workflow,
 * hotfix support, SNAPSHOT handling, dual merge pattern,
 * and dry-run mode.</p>
 */
@DisplayName("x-release Skill")
class ReleaseSkillTest {

    @Nested
    @DisplayName("Claude SKILL.md -- Frontmatter")
    class ClaudeFrontmatter {

        @Test
        @DisplayName("x-release SKILL.md exists after"
                + " assembly")
        void assemble_release_skillMdExists(
                @TempDir Path tempDir)
                throws IOException {
            Path outputDir = generateOutput(tempDir);
            Path skillMd = outputDir.resolve(
                    "skills/x-release/SKILL.md");
            assertThat(skillMd).exists();
        }

        @Test
        @DisplayName("frontmatter contains name:"
                + " x-release")
        void assemble_release_hasName(
                @TempDir Path tempDir)
                throws IOException {
            String content =
                    generateClaudeContent(tempDir);
            assertThat(content)
                    .contains("name: x-release");
        }

        @Test
        @DisplayName("frontmatter contains"
                + " user-invocable: true")
        void assemble_release_hasUserInvocable(
                @TempDir Path tempDir)
                throws IOException {
            String content =
                    generateClaudeContent(tempDir);
            assertThat(content).satisfiesAnyOf(
                    c -> assertThat(c).contains(
                            "user-invocable: true"),
                    c -> assertThat(c).contains(
                            "user-invocable: \"true\""));
        }

        @Test
        @DisplayName("frontmatter contains argument-hint"
                + " with version options and hotfix")
        void assemble_release_hasArgumentHint(
                @TempDir Path tempDir)
                throws IOException {
            String content =
                    generateClaudeContent(tempDir);
            assertThat(content)
                    .contains("argument-hint:")
                    .contains("major")
                    .contains("minor")
                    .contains("patch")
                    .contains("--dry-run")
                    .contains("--hotfix");
        }

        @Test
        @DisplayName("frontmatter contains allowed-tools")
        void assemble_release_hasAllowedTools(
                @TempDir Path tempDir)
                throws IOException {
            String content =
                    generateClaudeContent(tempDir);
            assertThat(content)
                    .contains("allowed-tools:");
        }

        @Test
        @DisplayName("allowed-tools includes Read, Write,"
                + " Edit, Bash, Glob, Grep, Agent")
        void assemble_release_hasExpectedTools(
                @TempDir Path tempDir)
                throws IOException {
            String content =
                    generateClaudeContent(tempDir);
            assertThat(content)
                    .contains("Read")
                    .contains("Write")
                    .contains("Edit")
                    .contains("Bash")
                    .contains("Glob")
                    .contains("Grep")
                    .contains("Agent");
        }

        @Test
        @DisplayName("frontmatter contains description"
                + " with release branch keywords")
        void assemble_release_hasDescription(
                @TempDir Path tempDir)
                throws IOException {
            String content =
                    generateClaudeContent(tempDir);
            assertThat(content)
                    .contains("description:")
                    .contains("release")
                    .contains("release branch");
        }
    }

    @Nested
    @DisplayName("Claude SKILL.md -- Release Branch"
            + " Workflow")
    class ReleaseBranchWorkflow {

        @Test
        @DisplayName("contains 11-step release branch"
                + " workflow")
        void assemble_release_hasElevenWorkflowSteps(
                @TempDir Path tempDir)
                throws IOException {
            String content =
                    generateClaudeContent(tempDir);
            assertThat(content)
                    .contains("DETERMINE")
                    .contains("VALIDATE")
                    .contains("BRANCH")
                    .contains("UPDATE")
                    .contains("CHANGELOG")
                    .contains("COMMIT")
                    .contains("MERGE-MAIN")
                    .contains("TAG")
                    .contains("MERGE-BACK")
                    .contains("PUBLISH")
                    .contains("CLEANUP")
                    .contains("DRY-RUN");
        }

        @Test
        @DisplayName("VALIDATE requires develop or"
                + " release/* branch")
        void assemble_release_validateRequiresDevelop(
                @TempDir Path tempDir)
                throws IOException {
            String content =
                    generateClaudeContent(tempDir);
            assertThat(content)
                    .contains("develop")
                    .contains("release/");
            assertThat(content)
                    .contains("not on develop/release");
        }

        @Test
        @DisplayName("BRANCH creates release/X.Y.Z from"
                + " develop inside a worktree")
        void assemble_release_branchFromDevelop(
                @TempDir Path tempDir)
                throws IOException {
            // story-0037-0008: Phase BRANCH is now
            // worktree-first. The branch name is stored in
            // BRANCH_NAME and the checkout is performed by
            // x-git-worktree create (Rule 13 Pattern 1).
            String content =
                    generateClaudeContent(tempDir);
            assertThat(content)
                    .contains(
                            "BRANCH_NAME=\"release/${VERSION}\"")
                    .contains("BASE_BRANCH=\"develop\"")
                    .contains("WT_ID=\"release-${VERSION}\"")
                    .contains("Worktree-Aware");
        }

        @Test
        @DisplayName("OPEN-RELEASE-PR opens PR to main"
                + " via gh pr create (PR-flow)")
        void assemble_release_openReleasePrToMain(
                @TempDir Path tempDir)
                throws IOException {
            String content =
                    generateClaudeContent(tempDir);
            assertThat(content)
                    .contains("gh pr create")
                    .contains("--base main")
                    .contains("OPEN-RELEASE-PR");
        }

        @Test
        @DisplayName("TAG is created on main after merge")
        void assemble_release_tagOnMainAfterMerge(
                @TempDir Path tempDir)
                throws IOException {
            String content =
                    generateClaudeContent(tempDir);
            assertThat(content)
                    .contains("git tag -a");
        }

        @Test
        @DisplayName("BACK-MERGE-DEVELOP opens PR to"
                + " develop via gh pr create (PR-flow)")
        void assemble_release_backMergeToDevelopPr(
                @TempDir Path tempDir)
                throws IOException {
            String content =
                    generateClaudeContent(tempDir);
            assertThat(content)
                    .contains("gh pr create")
                    .contains("--base develop")
                    .contains("BACK-MERGE-DEVELOP");
        }

        @Test
        @DisplayName("PUBLISH pushes the release tag"
                + " (main/develop go via PR flow)")
        void assemble_release_publishPushesTag(
                @TempDir Path tempDir)
                throws IOException {
            // story-0035-0003: Step 10 PUBLISH was
            // narrowed to tag-only. `main` and `develop`
            // are now updated exclusively through the
            // release PR (Step 7) and the back-merge PR
            // (Step 9, story-0035-0006).
            String content =
                    generateClaudeContent(tempDir);
            int stepPublish = content.indexOf(
                    "### Step 11 \u2014 Publish");
            int stepCleanup = content.indexOf(
                    "### Step 12 \u2014 Cleanup");
            assertThat(stepPublish)
                    .as("Step 11 Publish header not found")
                    .isNotNegative();
            assertThat(stepCleanup)
                    .as("Step 12 Cleanup header not found")
                    .isGreaterThan(stepPublish);
            String publishBody = content.substring(
                    stepPublish, stepCleanup);
            assertThat(publishBody).contains(
                    "git push origin \"v${VERSION}\"");
        }

        @Test
        @DisplayName("CLEANUP deletes release branch")
        void assemble_release_cleanupDeletesBranch(
                @TempDir Path tempDir)
                throws IOException {
            String content =
                    generateClaudeContent(tempDir);
            assertThat(content).contains(
                    "git branch -d \"release/${VERSION}\"");
        }

        @Test
        @DisplayName("references x-release-changelog for"
                + " changelog generation")
        void assemble_release_refsChangelog(
                @TempDir Path tempDir)
                throws IOException {
            String content =
                    generateClaudeContent(tempDir);
            assertThat(content)
                    .contains("x-release-changelog");
        }

        @Test
        @DisplayName("references x-git-push for"
                + " commit and tag patterns")
        void assemble_release_refsGitPush(
                @TempDir Path tempDir)
                throws IOException {
            String content =
                    generateClaudeContent(tempDir);
            assertThat(content)
                    .contains("x-git-push");
        }
    }

    @Nested
    @DisplayName("Claude SKILL.md -- Hotfix Release")
    class HotfixRelease {

        @Test
        @DisplayName("hotfix section documents branching"
                + " from main")
        void assemble_release_hotfixFromMain(
                @TempDir Path tempDir)
                throws IOException {
            String content =
                    generateClaudeContent(tempDir);
            assertThat(content)
                    .contains("Hotfix Release")
                    .contains("hotfix")
                    .contains("main");
        }

        @Test
        @DisplayName("hotfix enforces PATCH version"
                + " bump only")
        void assemble_release_hotfixPatchOnly(
                @TempDir Path tempDir)
                throws IOException {
            String content =
                    generateClaudeContent(tempDir);
            assertThat(content)
                    .contains("PATCH only");
        }

        @Test
        @DisplayName("hotfix merges to main and develop")
        void assemble_release_hotfixDualMerge(
                @TempDir Path tempDir)
                throws IOException {
            String content =
                    generateClaudeContent(tempDir);
            assertThat(content)
                    .contains("hotfix")
                    .contains("main")
                    .contains("develop");
        }

        @Test
        @DisplayName("hotfix merges into active release"
                + " branch if exists")
        void assemble_release_hotfixMergeReleaseBranch(
                @TempDir Path tempDir)
                throws IOException {
            String content =
                    generateClaudeContent(tempDir);
            assertThat(content)
                    .contains("active release/")
                    .contains("additional PR");
        }
    }

    @Nested
    @DisplayName("Claude SKILL.md -- SNAPSHOT Handling")
    class SnapshotHandling {

        @Test
        @DisplayName("documents SNAPSHOT stripping on"
                + " release branch")
        void assemble_release_snapshotStrip(
                @TempDir Path tempDir)
                throws IOException {
            String content =
                    generateClaudeContent(tempDir);
            assertThat(content)
                    .contains("SNAPSHOT")
                    .contains("strip");
        }

        @Test
        @DisplayName("documents SNAPSHOT advance on"
                + " develop after release")
        void assemble_release_snapshotAdvance(
                @TempDir Path tempDir)
                throws IOException {
            String content =
                    generateClaudeContent(tempDir);
            assertThat(content)
                    .contains("SNAPSHOT")
                    .contains("advance");
        }
    }

    @Nested
    @DisplayName("Claude SKILL.md -- Version Detection")
    class VersionDetection {

        @Test
        @DisplayName("contains Conventional Commits"
                + " auto-detection logic")
        void assemble_release_hasAutoDetection(
                @TempDir Path tempDir)
                throws IOException {
            String content =
                    generateClaudeContent(tempDir);
            assertThat(content)
                    .contains("Conventional Commits");
        }

        @Test
        @DisplayName("contains BREAKING CHANGE detection"
                + " for major bumps")
        void assemble_release_hasBreakingChangeDetection(
                @TempDir Path tempDir)
                throws IOException {
            String content =
                    generateClaudeContent(tempDir);
            assertThat(content)
                    .contains("BREAKING CHANGE");
        }

        @Test
        @DisplayName("contains feat detection for"
                + " minor bumps")
        void assemble_release_hasFeatDetection(
                @TempDir Path tempDir)
                throws IOException {
            String content =
                    generateClaudeContent(tempDir);
            assertThat(content)
                    .contains("feat");
        }

        @Test
        @DisplayName("contains SemVer reference")
        void assemble_release_hasSemVerReference(
                @TempDir Path tempDir)
                throws IOException {
            String content =
                    generateClaudeContent(tempDir);
            assertThat(content)
                    .contains("Semantic Versioning");
        }
    }

    @Nested
    @DisplayName("Claude SKILL.md -- Version File Patterns")
    class VersionFilePatterns {

        @Test
        @DisplayName("contains pom.xml pattern for Maven")
        void assemble_release_hasPomXml(
                @TempDir Path tempDir)
                throws IOException {
            String content =
                    generateClaudeContent(tempDir);
            assertThat(content)
                    .contains("pom.xml");
        }

        @Test
        @DisplayName("contains package.json pattern"
                + " for npm")
        void assemble_release_hasPackageJson(
                @TempDir Path tempDir)
                throws IOException {
            String content =
                    generateClaudeContent(tempDir);
            assertThat(content)
                    .contains("package.json");
        }

        @Test
        @DisplayName("contains Cargo.toml pattern"
                + " for Rust")
        void assemble_release_hasCargoToml(
                @TempDir Path tempDir)
                throws IOException {
            String content =
                    generateClaudeContent(tempDir);
            assertThat(content)
                    .contains("Cargo.toml");
        }

        @Test
        @DisplayName("contains pyproject.toml pattern"
                + " for Python")
        void assemble_release_hasPyprojectToml(
                @TempDir Path tempDir)
                throws IOException {
            String content =
                    generateClaudeContent(tempDir);
            assertThat(content)
                    .contains("pyproject.toml");
        }

        @Test
        @DisplayName("contains build.gradle pattern"
                + " for Gradle")
        void assemble_release_hasBuildGradle(
                @TempDir Path tempDir)
                throws IOException {
            String content =
                    generateClaudeContent(tempDir);
            assertThat(content)
                    .contains("build.gradle");
        }

        @Test
        @DisplayName("contains go module note for Go")
        void assemble_release_hasGoModule(
                @TempDir Path tempDir)
                throws IOException {
            String content =
                    generateClaudeContent(tempDir);
            assertThat(content)
                    .contains("Go");
        }
    }

    @Nested
    @DisplayName("Claude SKILL.md -- Dry-Run Mode")
    class DryRunMode {

        @Test
        @DisplayName("dry-run shows complete release"
                + " branch plan")
        void assemble_release_dryRunShowsBranchPlan(
                @TempDir Path tempDir)
                throws IOException {
            String content =
                    generateClaudeContent(tempDir);
            assertThat(content)
                    .contains("dry-run")
                    .contains("--dry-run")
                    .contains("BRANCH")
                    .contains("OPEN_RELEASE_PR")
                    .contains("BACK_MERGE_DEVELOP")
                    .contains("CLEANUP");
        }

        @Test
        @DisplayName("dry-run shows SNAPSHOT advance")
        void assemble_release_dryRunShowsSnapshot(
                @TempDir Path tempDir)
                throws IOException {
            String content =
                    generateClaudeContent(tempDir);
            assertThat(content)
                    .contains("SNAPSHOT");
        }
    }

    @Nested
    @DisplayName("Claude SKILL.md -- Pre-conditions")
    class PreConditions {

        @Test
        @DisplayName("validates uncommitted changes")
        void assemble_release_validatesUncommitted(
                @TempDir Path tempDir)
                throws IOException {
            String content =
                    generateClaudeContent(tempDir);
            assertThat(content)
                    .contains("uncommitted");
        }

        @Test
        @DisplayName("validates tests pass")
        void assemble_release_validatesTests(
                @TempDir Path tempDir)
                throws IOException {
            String content =
                    generateClaudeContent(tempDir);
            assertThat(content)
                    .contains("--skip-tests");
        }

        @Test
        @DisplayName("validates current branch is develop")
        void assemble_release_validatesDevelopBranch(
                @TempDir Path tempDir)
                throws IOException {
            String content =
                    generateClaudeContent(tempDir);
            assertThat(content)
                    .contains("develop");
        }
    }

    @Nested
    @DisplayName("Claude SKILL.md -- Integration")
    class Integration {

        @Test
        @DisplayName("references release-management KP")
        void assemble_release_refsReleaseManagementKp(
                @TempDir Path tempDir)
                throws IOException {
            String content =
                    generateClaudeContent(tempDir);
            assertThat(content)
                    .contains("release-management");
        }

        @Test
        @DisplayName("references release checklist"
                + " template")
        void assemble_release_refsReleaseChecklist(
                @TempDir Path tempDir)
                throws IOException {
            String content =
                    generateClaudeContent(tempDir);
            assertThat(content).satisfiesAnyOf(
                    c -> assertThat(c).contains(
                            "release-checklist"),
                    c -> assertThat(c).contains(
                            "RELEASE-CHECKLIST"));
        }

        @Test
        @DisplayName("contains --no-publish flag")
        void assemble_release_hasNoPublishFlag(
                @TempDir Path tempDir)
                throws IOException {
            String content =
                    generateClaudeContent(tempDir);
            assertThat(content)
                    .contains("--no-publish");
        }

        @Test
        @DisplayName("release commit follows"
                + " Conventional Commits")
        void assemble_release_hasReleaseCommitFormat(
                @TempDir Path tempDir)
                throws IOException {
            String content =
                    generateClaudeContent(tempDir);
            assertThat(content)
                    .contains("release: v");
        }

        @Test
        @DisplayName("annotated tag with v prefix")
        void assemble_release_hasAnnotatedTag(
                @TempDir Path tempDir)
                throws IOException {
            String content =
                    generateClaudeContent(tempDir);
            assertThat(content)
                    .contains("git tag -a");
        }

        @Test
        @DisplayName("references Rule 09 branching model")
        void assemble_release_refsRule09(
                @TempDir Path tempDir)
                throws IOException {
            String content =
                    generateClaudeContent(tempDir);
            assertThat(content)
                    .contains("Rule 09")
                    .contains("Branching Model");
        }
    }

    @Nested
    @DisplayName("Claude SKILL.md -- EPIC-0035"
            + " Foundation (story-0035-0001)")
    class Epic0035Foundation {

        @Test
        @DisplayName("allowed-tools adds Skill and"
                + " AskUserQuestion")
        void assemble_release_allowedToolsAddsSkillAsk(
                @TempDir Path tempDir)
                throws IOException {
            String content =
                    generateClaudeContent(tempDir);
            assertThat(content)
                    .contains("allowed-tools:")
                    .contains("Skill")
                    .contains("AskUserQuestion");
        }

        @Test
        @DisplayName("argument-hint declares the four"
                + " new EPIC-0035 flags")
        void assemble_release_argumentHintHasNewFlags(
                @TempDir Path tempDir)
                throws IOException {
            String content =
                    generateClaudeContent(tempDir);
            assertThat(content)
                    .contains("argument-hint:")
                    .contains("--continue-after-merge")
                    .contains("--interactive")
                    .contains("--signed-tag")
                    .contains("--state-file");
        }

        @Test
        @DisplayName("description mentions approval gate,"
                + " PR-flow and deep validation")
        void assemble_release_descriptionMentionsEpic(
                @TempDir Path tempDir)
                throws IOException {
            String content =
                    generateClaudeContent(tempDir);
            assertThat(content)
                    .contains("approval gate")
                    .contains("PR-flow")
                    .contains("deep validation");
        }

        @Test
        @DisplayName("Parameters table contains all four"
                + " new EPIC-0035 flags")
        void assemble_release_parametersTableHasNewFlags(
                @TempDir Path tempDir)
                throws IOException {
            String content =
                    generateClaudeContent(tempDir);
            assertThat(content)
                    .contains("| `--continue-after-merge`")
                    .contains("| `--interactive`")
                    .contains("| `--signed-tag`")
                    .contains("| `--state-file <path>`");
        }

        @Test
        @DisplayName("workflow box lists numbered"
                + " RESUME-DETECT step")
        void assemble_release_workflowBoxHasStepZero(
                @TempDir Path tempDir)
                throws IOException {
            String content =
                    generateClaudeContent(tempDir);
            assertThat(content)
                    .contains("0. RESUME-DETECT");
        }

        @Test
        @DisplayName("Step 0 section is inserted before"
                + " Step 1 Determine Version")
        void assemble_release_stepZeroBeforeStepOne(
                @TempDir Path tempDir)
                throws IOException {
            String content =
                    generateClaudeContent(tempDir);
            int stepZero = content.indexOf(
                    "### Step 0 \u2014 Resume Detection");
            int stepOne = content.indexOf(
                    "### Step 1 \u2014 Determine Version");
            assertThat(stepZero).isPositive();
            assertThat(stepOne).isPositive();
            assertThat(stepZero).isLessThan(stepOne);
        }

        @Test
        @DisplayName("Step 0 verifies gh, jq and gh auth"
                + " status (RULE-008)")
        void assemble_release_stepZeroChecksDependencies(
                @TempDir Path tempDir)
                throws IOException {
            String content =
                    generateClaudeContent(tempDir);
            assertThat(content)
                    .contains("command -v gh")
                    .contains("command -v jq")
                    .contains("gh auth status");
        }

        @Test
        @DisplayName("Step 0 declares every resume-"
                + "detection error code")
        void assemble_release_stepZeroErrorCodes(
                @TempDir Path tempDir)
                throws IOException {
            String content =
                    generateClaudeContent(tempDir);
            assertThat(content)
                    .contains("DEP_GH_MISSING")
                    .contains("DEP_JQ_MISSING")
                    .contains("DEP_GH_AUTH")
                    .contains("STATE_INVALID_JSON")
                    .contains("STATE_SCHEMA_VERSION")
                    .contains("RESUME_NO_STATE")
                    .contains("STATE_CONFLICT");
        }

        @Test
        @DisplayName("Step 0 documents --continue-after-"
                + "merge resume branching")
        void assemble_release_stepZeroDocumentsResume(
                @TempDir Path tempDir)
                throws IOException {
            String content =
                    generateClaudeContent(tempDir);
            assertThat(content)
                    .contains("--continue-after-merge")
                    .contains("APPROVAL_PENDING")
                    .contains("RESUME");
        }

        @Test
        @DisplayName("state-file-schema reference file"
                + " is assembled alongside SKILL.md")
        void assemble_release_stateSchemaFileExists(
                @TempDir Path tempDir)
                throws IOException {
            Path outputDir = generateOutput(tempDir);
            Path schema = outputDir.resolve(
                    "skills/x-release/references/"
                            + "state-file-schema.md");
            assertThat(schema).exists();
        }

        @Test
        @DisplayName("state-file-schema declares schema"
                + " version 1 and the 14-phase enum")
        void assemble_release_stateSchemaHasEnum(
                @TempDir Path tempDir)
                throws IOException {
            Path outputDir = generateOutput(tempDir);
            String schema = Files.readString(
                    outputDir.resolve(
                            "skills/x-release/references/"
                                    + "state-file-schema.md"),
                    StandardCharsets.UTF_8);
            assertThat(schema)
                    .contains("schemaVersion")
                    .contains("`INITIALIZED`")
                    .contains("`DETERMINED`")
                    .contains("`VALIDATED`")
                    .contains("`BRANCHED`")
                    .contains("`UPDATED`")
                    .contains("`CHANGELOG_DONE`")
                    .contains("`COMMITTED`")
                    .contains("`PR_OPENED`")
                    .contains("`APPROVAL_PENDING`")
                    .contains("`MERGED`")
                    .contains("`TAGGED`")
                    .contains("`BACKMERGE_OPENED`")
                    .contains("`BACKMERGE_CONFLICT`")
                    .contains("`COMPLETED`");
        }

        @Test
        @DisplayName("state-file-schema documents atomic"
                + " write protocol and error catalog")
        void assemble_release_stateSchemaCatalog(
                @TempDir Path tempDir)
                throws IOException {
            Path outputDir = generateOutput(tempDir);
            String schema = Files.readString(
                    outputDir.resolve(
                            "skills/x-release/references/"
                                    + "state-file-schema.md"),
                    StandardCharsets.UTF_8);
            assertThat(schema)
                    .contains("write-to-temp")
                    .contains("DEP_GH_MISSING")
                    .contains("STATE_INVALID_JSON")
                    .contains("STATE_CONFLICT")
                    .contains("RESUME_NO_STATE");
        }

        @Test
        @DisplayName("RULE-002 preserved: existing flags"
                + " still documented")
        void assemble_release_preservesExistingFlags(
                @TempDir Path tempDir)
                throws IOException {
            String content =
                    generateClaudeContent(tempDir);
            assertThat(content)
                    .contains("--dry-run")
                    .contains("--skip-tests")
                    .contains("--no-publish")
                    .contains("--hotfix");
        }
    }

    @Nested
    @DisplayName("Claude SKILL.md -- CI-Watch"
            + " Phase (story-0045-0005)")
    class CiWatchPhase {

        @Test
        @DisplayName("--ci-watch flag present in"
                + " argument-hint and Parameters table")
        void assemble_release_ciWatchFlagDocumented(
                @TempDir Path tempDir)
                throws IOException {
            String content =
                    generateClaudeContent(tempDir);
            assertThat(content)
                    .contains("--ci-watch")
                    .contains("| `--ci-watch`");
        }

        @Test
        @DisplayName("CI-WATCH phase inserted between"
                + " OPEN-RELEASE-PR and APPROVAL-GATE"
                + " in workflow box")
        void assemble_release_ciWatchInWorkflowBox(
                @TempDir Path tempDir)
                throws IOException {
            String content =
                    generateClaudeContent(tempDir);
            // Locate the workflow box (```...```) to
            // search within it only
            int boxStart = content.indexOf(
                    "7. OPEN-RELEASE-PR");
            int boxEnd = content.indexOf(
                    "9. TAG", boxStart);
            assertThat(boxStart).isPositive();
            assertThat(boxEnd).isPositive();
            String workflowBox = content.substring(
                    boxStart, boxEnd);
            assertThat(workflowBox)
                    .contains("CI-WATCH")
                    .contains("APPROVAL-GATE");
            int ciWatch = workflowBox.indexOf(
                    "CI-WATCH");
            int approvalGate = workflowBox.indexOf(
                    "APPROVAL-GATE");
            assertThat(ciWatch).isLessThan(
                    approvalGate);
        }

        @Test
        @DisplayName("CI-WATCH phase invokes x-pr-watch-ci"
                + " via Rule 13 INLINE-SKILL")
        void assemble_release_ciWatchUsesInlineSkill(
                @TempDir Path tempDir)
                throws IOException {
            String content =
                    generateClaudeContent(tempDir);
            assertThat(content)
                    .contains(
                            "Skill(skill:"
                                    + " \"x-pr-watch-ci\"");
        }

        @Test
        @DisplayName("exit 20 transitions phase to"
                + " RELEASE_ABORTED (no tag created)")
        void assemble_release_exit20AbortsRelease(
                @TempDir Path tempDir)
                throws IOException {
            String content =
                    generateClaudeContent(tempDir);
            assertThat(content)
                    .contains("RELEASE_ABORTED")
                    .contains("CI_FAILED")
                    .contains("no tag");
        }

        @Test
        @DisplayName("exit 30 (TIMEOUT) also aborts"
                + " release")
        void assemble_release_exit30AbortsRelease(
                @TempDir Path tempDir)
                throws IOException {
            String content =
                    generateClaudeContent(tempDir);
            assertThat(content)
                    .contains("TIMEOUT")
                    .contains("RELEASE_ABORTED");
        }

        @Test
        @DisplayName("ciWatchResult field documented"
                + " in state-file-schema")
        void assemble_release_ciWatchResultInSchema(
                @TempDir Path tempDir)
                throws IOException {
            Path outputDir = generateOutput(tempDir);
            String schema = Files.readString(
                    outputDir.resolve(
                            "skills/x-release/references/"
                                    + "state-file-schema.md"),
                    StandardCharsets.UTF_8);
            assertThat(schema)
                    .contains("ciWatchResult")
                    .contains("CI_WATCH_PENDING")
                    .contains("CI_WATCH_COMPLETE")
                    .contains("RELEASE_ABORTED");
        }

        @Test
        @DisplayName("idempotency: CI_WATCH_COMPLETE"
                + " skips re-invocation")
        void assemble_release_ciWatchIdempotent(
                @TempDir Path tempDir)
                throws IOException {
            String content =
                    generateClaudeContent(tempDir);
            assertThat(content)
                    .contains("CI_WATCH_COMPLETE")
                    .contains("idempotent");
        }

        @Test
        @DisplayName("--ci-watch is opt-in: absent ="
                + " current default behavior preserved")
        void assemble_release_ciWatchOptIn(
                @TempDir Path tempDir)
                throws IOException {
            String content =
                    generateClaudeContent(tempDir);
            assertThat(content)
                    .contains("opt-in")
                    .contains(
                            "--ci-watch not set");
        }

        @Test
        @DisplayName("telemetry markers present for"
                + " CI-WATCH phase")
        void assemble_release_ciWatchTelemetry(
                @TempDir Path tempDir)
                throws IOException {
            String content =
                    generateClaudeContent(tempDir);
            assertThat(content)
                    .contains("telemetry-phase.sh"
                            + " start x-release"
                            + " Phase-CI-Watch")
                    .contains("telemetry-phase.sh"
                            + " end x-release"
                            + " Phase-CI-Watch");
        }
    }

    private Path generateOutput(Path tempDir)
            throws IOException {
        Path outputDir = tempDir.resolve("output");
        Files.createDirectories(outputDir);
        SkillsAssembler assembler =
                new SkillsAssembler();
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
