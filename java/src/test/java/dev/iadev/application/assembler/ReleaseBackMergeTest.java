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
 * Tests for x-release skill EPIC-0035 story-0035-0006:
 * Replace direct back-merge with PR-flow and conflict
 * detection.
 *
 * <p>Validates that the old Step 10 (direct git merge to
 * develop) is replaced by Phase BACK-MERGE-DEVELOP with
 * dry-run merge, clean flow (SNAPSHOT advance + PR), and
 * conflict flow (abort merge + PR with conflict body).
 * Also validates the backmerge-strategies reference
 * document.</p>
 */
@DisplayName("x-release Skill — story-0035-0006"
        + " BACK-MERGE-DEVELOP")
class ReleaseBackMergeTest {

    @Nested
    @DisplayName("Degenerate — wrong phase guard")
    class WrongPhaseGuard {

        @Test
        @DisplayName("phase verification requires TAGGED"
                + " before BACK-MERGE-DEVELOP")
        void backMerge_requiresTaggedPhase(
                @TempDir Path tempDir)
                throws IOException {
            String content =
                    generateClaudeContent(tempDir);
            assertThat(content)
                    .contains("BACKMERGE_WRONG_PHASE")
                    .contains("expected TAGGED");
        }
    }

    @Nested
    @DisplayName("Happy path — clean merge Java project")
    class CleanMergeJava {

        @Test
        @DisplayName("creates chore/backmerge-v branch"
                + " from origin/develop")
        void backMerge_cleanJava_createsBranch(
                @TempDir Path tempDir)
                throws IOException {
            String content =
                    generateClaudeContent(tempDir);
            assertThat(content)
                    .contains("chore/backmerge-v");
        }

        @Test
        @DisplayName("dry-run merge detects conflicts"
                + " via --no-commit --no-ff")
        void backMerge_cleanJava_dryRunMerge(
                @TempDir Path tempDir)
                throws IOException {
            String content =
                    generateClaudeContent(tempDir);
            assertThat(content)
                    .contains("git merge --no-commit"
                            + " --no-ff origin/main");
        }

        @Test
        @DisplayName("Java SNAPSHOT advance computed"
                + " for next minor")
        void backMerge_cleanJava_snapshotAdvance(
                @TempDir Path tempDir)
                throws IOException {
            String content =
                    generateClaudeContent(tempDir);
            assertThat(content)
                    .contains("NEXT_SNAPSHOT")
                    .contains("SNAPSHOT");
        }

        @Test
        @DisplayName("pom.xml updated with sed for"
                + " SNAPSHOT version")
        void backMerge_cleanJava_pomXmlUpdated(
                @TempDir Path tempDir)
                throws IOException {
            String content =
                    generateClaudeContent(tempDir);
            // Phase should reference pom.xml update
            // within the BACK-MERGE-DEVELOP section
            int backMergeStart = content.indexOf(
                    "BACK-MERGE-DEVELOP");
            assertThat(backMergeStart).isPositive();
            String afterBackMerge = content.substring(
                    backMergeStart);
            assertThat(afterBackMerge)
                    .contains("pom.xml")
                    .contains("sed");
        }

        @Test
        @DisplayName("commit message for SNAPSHOT advance"
                + " follows convention")
        void backMerge_cleanJava_commitMessage(
                @TempDir Path tempDir)
                throws IOException {
            String content =
                    generateClaudeContent(tempDir);
            assertThat(content)
                    .contains("chore: advance develop to"
                            + " ${NEXT_SNAPSHOT}");
        }

        @Test
        @DisplayName("gh pr create --base develop invoked"
                + " for clean merge")
        void backMerge_cleanJava_prCreated(
                @TempDir Path tempDir)
                throws IOException {
            String content =
                    generateClaudeContent(tempDir);
            assertThat(content)
                    .contains("gh pr create")
                    .contains("--base develop")
                    .contains("--head \"$BACKMERGE_BRANCH\"");
        }

        @Test
        @DisplayName("state advances to BACKMERGE_OPENED"
                + " on clean merge")
        void backMerge_cleanJava_stateOpened(
                @TempDir Path tempDir)
                throws IOException {
            String content =
                    generateClaudeContent(tempDir);
            assertThat(content)
                    .contains("BACKMERGE_OPENED");
        }
    }

    @Nested
    @DisplayName("Error path — conflict detection")
    class ConflictDetection {

        @Test
        @DisplayName("exit code 1 triggers conflict flow")
        void backMerge_conflict_exitCodeOne(
                @TempDir Path tempDir)
                throws IOException {
            String content =
                    generateClaudeContent(tempDir);
            assertThat(content)
                    .contains("MERGE_EXIT");
        }

        @Test
        @DisplayName("conflict flow captures conflicting"
                + " file list")
        void backMerge_conflict_capturesFileList(
                @TempDir Path tempDir)
                throws IOException {
            String content =
                    generateClaudeContent(tempDir);
            assertThat(content)
                    .contains("CONFLICT_LIST")
                    .contains("git diff --name-only"
                            + " --diff-filter=U");
        }

        @Test
        @DisplayName("conflict flow aborts merge and"
                + " pushes main to backmerge branch")
        void backMerge_conflict_abortAndPush(
                @TempDir Path tempDir)
                throws IOException {
            String content =
                    generateClaudeContent(tempDir);
            assertThat(content)
                    .contains("git merge --abort");
        }

        @Test
        @DisplayName("PR body lists conflicting files")
        void backMerge_conflict_prBodyListsFiles(
                @TempDir Path tempDir)
                throws IOException {
            String content =
                    generateClaudeContent(tempDir);
            assertThat(content)
                    .contains("CONFLICTS DETECTED")
                    .contains("${CONFLICT_LIST}");
        }

        @Test
        @DisplayName("state advances to"
                + " BACKMERGE_CONFLICT with"
                + " conflictFiles")
        void backMerge_conflict_stateConflict(
                @TempDir Path tempDir)
                throws IOException {
            String content =
                    generateClaudeContent(tempDir);
            assertThat(content)
                    .contains("BACKMERGE_CONFLICT")
                    .contains("conflictFiles");
        }
    }

    @Nested
    @DisplayName("Happy path — hotfix (no SNAPSHOT)")
    class HotfixNoSnapshot {

        @Test
        @DisplayName("hotfix mode skips SNAPSHOT advance")
        void backMerge_hotfix_noSnapshotAdvance(
                @TempDir Path tempDir)
                throws IOException {
            String content =
                    generateClaudeContent(tempDir);
            // Verify the HOTFIX condition check exists
            assertThat(content)
                    .contains("HOTFIX")
                    .contains("SNAPSHOT");
            // The skill should check for hotfix flag
            // before doing SNAPSHOT advance
            int backMergeStart = content.indexOf(
                    "BACK-MERGE-DEVELOP");
            assertThat(backMergeStart).isPositive();
            String phase = content.substring(
                    backMergeStart);
            assertThat(phase)
                    .contains("HOTFIX");
        }
    }

    @Nested
    @DisplayName("Happy path — non-Java project")
    class NonJavaProject {

        @Test
        @DisplayName("pom.xml detection gates SNAPSHOT"
                + " advance")
        void backMerge_nonJava_noPomXmlNoSnapshot(
                @TempDir Path tempDir)
                throws IOException {
            String content =
                    generateClaudeContent(tempDir);
            // Verify the pom.xml conditional exists
            int backMergeStart = content.indexOf(
                    "BACK-MERGE-DEVELOP");
            assertThat(backMergeStart).isPositive();
            String phase = content.substring(
                    backMergeStart);
            assertThat(phase)
                    .contains("pom.xml");
        }
    }

    @Nested
    @DisplayName("Boundary — unexpected merge exit code")
    class UnexpectedExitCode {

        @Test
        @DisplayName("BACKMERGE_UNEXPECTED for unknown"
                + " exit codes")
        void backMerge_unexpectedExitCode(
                @TempDir Path tempDir)
                throws IOException {
            String content =
                    generateClaudeContent(tempDir);
            assertThat(content)
                    .contains("BACKMERGE_UNEXPECTED");
        }
    }

    @Nested
    @DisplayName("Legacy Step 10 removed")
    class LegacyRemoved {

        @Test
        @DisplayName("old direct git merge to develop"
                + " is removed")
        void backMerge_noDirectMergeToDevelop(
                @TempDir Path tempDir)
                throws IOException {
            String content =
                    generateClaudeContent(tempDir);
            // Old Step 10 had "git checkout develop"
            // followed by "git merge release/..."
            // The new flow should NOT have a direct
            // merge from release branch to develop
            assertThat(content).doesNotContain(
                    "git merge \"release/${VERSION}\""
                            + " --no-ff\n"
                            + "    -m \"release: merge"
                            + " release/${VERSION} back"
                            + " into develop\"");
        }

        @Test
        @DisplayName("workflow box shows BACK-MERGE-DEVELOP"
                + " instead of MERGE-BACK")
        void backMerge_workflowBoxUpdated(
                @TempDir Path tempDir)
                throws IOException {
            String content =
                    generateClaudeContent(tempDir);
            assertThat(content)
                    .contains("BACK-MERGE-DEVELOP");
        }
    }

    @Nested
    @DisplayName("Workflow box numbering")
    class WorkflowBoxNumbering {

        @Test
        @DisplayName("Step 10 is BACK-MERGE-DEVELOP in"
                + " workflow box")
        void backMerge_stepTenIsBackMergeDevelop(
                @TempDir Path tempDir)
                throws IOException {
            String content =
                    generateClaudeContent(tempDir);
            // Find the workflow box
            int boxStart = content.indexOf(
                    "0. RESUME-DETECT");
            assertThat(boxStart).isPositive();
            String workflowBox = content.substring(
                    boxStart,
                    content.indexOf("```",
                            boxStart));
            assertThat(workflowBox)
                    .contains("10. BACK-MERGE-DEVELOP");
        }
    }

    @Nested
    @DisplayName("State file fields — backmergePrUrl"
            + " and backmergePrNumber")
    class StateFileFields {

        @Test
        @DisplayName("clean flow persists backmergePrUrl"
                + " to state file")
        void backMerge_clean_persistsPrUrl(
                @TempDir Path tempDir)
                throws IOException {
            String content =
                    generateClaudeContent(tempDir);
            assertThat(content)
                    .contains("backmergePrUrl");
        }

        @Test
        @DisplayName("clean flow persists"
                + " backmergePrNumber to state file")
        void backMerge_clean_persistsPrNumber(
                @TempDir Path tempDir)
                throws IOException {
            String content =
                    generateClaudeContent(tempDir);
            assertThat(content)
                    .contains("backmergePrNumber");
        }
    }

    @Nested
    @DisplayName("Error catalog — BACKMERGE_* codes")
    class ErrorCatalog {

        @Test
        @DisplayName("error catalog includes"
                + " BACKMERGE_WRONG_PHASE")
        void backMerge_errorCatalog_wrongPhase(
                @TempDir Path tempDir)
                throws IOException {
            String content =
                    generateClaudeContent(tempDir);
            assertThat(content)
                    .contains("BACKMERGE_WRONG_PHASE");
        }

        @Test
        @DisplayName("error catalog includes"
                + " BACKMERGE_UNEXPECTED")
        void backMerge_errorCatalog_unexpected(
                @TempDir Path tempDir)
                throws IOException {
            String content =
                    generateClaudeContent(tempDir);
            assertThat(content)
                    .contains("BACKMERGE_UNEXPECTED");
        }
    }

    @Nested
    @DisplayName("Reference document —"
            + " backmerge-strategies.md")
    class BackmergeStrategiesDoc {

        @Test
        @DisplayName("backmerge-strategies.md exists after"
                + " assembly")
        void backMergeStrategies_exists(
                @TempDir Path tempDir)
                throws IOException {
            Path outputDir = generateOutput(tempDir);
            Path doc = outputDir.resolve(
                    "skills/x-release/references/"
                            + "backmerge-strategies.md");
            assertThat(doc).exists();
        }

        @Test
        @DisplayName("backmerge-strategies.md documents"
                + " clean merge flow")
        void backMergeStrategies_cleanFlow(
                @TempDir Path tempDir)
                throws IOException {
            Path outputDir = generateOutput(tempDir);
            String content = Files.readString(
                    outputDir.resolve(
                            "skills/x-release/references/"
                                    + "backmerge-strategies"
                                    + ".md"),
                    StandardCharsets.UTF_8);
            assertThat(content)
                    .containsIgnoringCase("clean");
        }

        @Test
        @DisplayName("backmerge-strategies.md documents"
                + " conflict flow")
        void backMergeStrategies_conflictFlow(
                @TempDir Path tempDir)
                throws IOException {
            Path outputDir = generateOutput(tempDir);
            String content = Files.readString(
                    outputDir.resolve(
                            "skills/x-release/references/"
                                    + "backmerge-strategies"
                                    + ".md"),
                    StandardCharsets.UTF_8);
            assertThat(content)
                    .containsIgnoringCase("conflict");
        }

        @Test
        @DisplayName("backmerge-strategies.md documents"
                + " SNAPSHOT advance")
        void backMergeStrategies_snapshotAdvance(
                @TempDir Path tempDir)
                throws IOException {
            Path outputDir = generateOutput(tempDir);
            String content = Files.readString(
                    outputDir.resolve(
                            "skills/x-release/references/"
                                    + "backmerge-strategies"
                                    + ".md"),
                    StandardCharsets.UTF_8);
            assertThat(content)
                    .contains("SNAPSHOT");
        }
    }

    @Nested
    @DisplayName("Behaviour preservation (RULE-002)")
    class BehaviourPreservation {

        @Test
        @DisplayName("existing steps 1-6 preserved")
        void preservesExistingSteps(
                @TempDir Path tempDir)
                throws IOException {
            String content =
                    generateClaudeContent(tempDir);
            assertThat(content)
                    .contains("### Step 1 \u2014 Determine"
                            + " Version")
                    .contains("### Step 2 \u2014 Phase"
                            + " VALIDATE-DEEP")
                    .contains("### Step 3 \u2014 Branch"
                            + " Creation")
                    .contains("### Step 4 \u2014 Update"
                            + " Version Files")
                    .contains("### Step 5 \u2014 Changelog"
                            + " Generation")
                    .contains("### Step 6 \u2014 Commit"
                            + " Release");
        }

        @Test
        @DisplayName("Step 7 OPEN-RELEASE-PR preserved")
        void preservesOpenReleasePr(
                @TempDir Path tempDir)
                throws IOException {
            String content =
                    generateClaudeContent(tempDir);
            assertThat(content)
                    .contains("### Step 7 \u2014 Open"
                            + " Release PR");
        }

        @Test
        @DisplayName("Step 8 APPROVAL-GATE preserved")
        void preservesApprovalGate(
                @TempDir Path tempDir)
                throws IOException {
            String content =
                    generateClaudeContent(tempDir);
            assertThat(content)
                    .contains("### Step 8 \u2014 Approval"
                            + " Gate");
        }

        @Test
        @DisplayName("existing flags still documented")
        void preservesExistingFlags(
                @TempDir Path tempDir)
                throws IOException {
            String content =
                    generateClaudeContent(tempDir);
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
