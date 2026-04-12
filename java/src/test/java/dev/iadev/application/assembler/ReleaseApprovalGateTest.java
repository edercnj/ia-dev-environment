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
 * Tests for x-release skill EPIC-0035 story-0035-0004:
 * Approval Gate with persistent skill halt.
 *
 * <p>Validates that Step 8 APPROVAL-GATE is inserted after
 * Step 7 (OPEN-RELEASE-PR) and before Step 9 (Tag Creation,
 * renumbered from Step 8). Covers default halt behavior,
 * interactive mode with 3 AskUserQuestion options,
 * state transitions, error codes, and the reference
 * document {@code approval-gate-workflow.md}.</p>
 */
@DisplayName("x-release Skill — story-0035-0004"
        + " APPROVAL-GATE")
class ReleaseApprovalGateTest {

    @Nested
    @DisplayName("Workflow Box — Step 8 APPROVAL-GATE"
            + " inserted")
    class WorkflowBox {

        @Test
        @DisplayName("workflow box lists APPROVAL-GATE"
                + " phase")
        void workflowBox_hasApprovalGate(
                @TempDir Path tempDir)
                throws IOException {
            String content =
                    generateClaudeContent(tempDir);
            assertThat(content)
                    .contains("APPROVAL-GATE");
        }

        @Test
        @DisplayName("APPROVAL-GATE appears after"
                + " OPEN-RELEASE-PR in workflow box")
        void workflowBox_approvalAfterOpenPr(
                @TempDir Path tempDir)
                throws IOException {
            String content =
                    generateClaudeContent(tempDir);
            // Search within the workflow box section
            // (starts at "0. RESUME-DETECT")
            int boxStart = content.indexOf(
                    "0. RESUME-DETECT");
            assertThat(boxStart).isPositive();
            int openPr = content.indexOf(
                    "OPEN-RELEASE-PR", boxStart);
            int gate = content.indexOf(
                    "APPROVAL-GATE", openPr);
            assertThat(openPr).isPositive();
            assertThat(gate).isPositive();
            assertThat(gate).isGreaterThan(openPr);
        }

        @Test
        @DisplayName("workflow box step numbering includes"
                + " 8 for APPROVAL-GATE")
        void workflowBox_stepEightIsApprovalGate(
                @TempDir Path tempDir)
                throws IOException {
            String content =
                    generateClaudeContent(tempDir);
            assertThat(content)
                    .contains("8. APPROVAL-GATE");
        }

        @Test
        @DisplayName("TAG is renumbered to Step 9"
                + " in workflow box")
        void workflowBox_tagIsStepNine(
                @TempDir Path tempDir)
                throws IOException {
            String content =
                    generateClaudeContent(tempDir);
            assertThat(content)
                    .contains("9. TAG");
        }

        @Test
        @DisplayName("BACK-MERGE-DEVELOP is Step 10"
                + " in workflow box")
        void workflowBox_backMergeDevelopIsStepTen(
                @TempDir Path tempDir)
                throws IOException {
            String content =
                    generateClaudeContent(tempDir);
            assertThat(content)
                    .contains("10. BACK-MERGE-DEVELOP");
        }

        @Test
        @DisplayName("PUBLISH is renumbered to"
                + " Step 11 in workflow box")
        void workflowBox_publishIsStepEleven(
                @TempDir Path tempDir)
                throws IOException {
            String content =
                    generateClaudeContent(tempDir);
            assertThat(content)
                    .contains("11. PUBLISH");
        }

        @Test
        @DisplayName("CLEANUP is renumbered to"
                + " Step 12 in workflow box")
        void workflowBox_cleanupIsStepTwelve(
                @TempDir Path tempDir)
                throws IOException {
            String content =
                    generateClaudeContent(tempDir);
            assertThat(content)
                    .contains("12. CLEANUP");
        }
    }

    @Nested
    @DisplayName("Step 8 Section — Default Halt Behavior")
    class DefaultHaltBehavior {

        @Test
        @DisplayName("Step 8 heading is Approval Gate")
        void stepEight_headingApprovalGate(
                @TempDir Path tempDir)
                throws IOException {
            String content =
                    generateClaudeContent(tempDir);
            assertThat(content).contains(
                    "### Step 8 \u2014 Approval Gate");
        }

        @Test
        @DisplayName("Step 8 is inserted between Step 7"
                + " (Open Release PR) and Step 9"
                + " (Tag Creation)")
        void stepEight_betweenSevenAndNine(
                @TempDir Path tempDir)
                throws IOException {
            String content =
                    generateClaudeContent(tempDir);
            int stepSeven = content.indexOf(
                    "### Step 7 \u2014 Open Release PR");
            int stepEight = content.indexOf(
                    "### Step 8 \u2014 Approval Gate");
            int stepNine = content.indexOf(
                    "### Step 9 \u2014 Tag Creation");
            assertThat(stepSeven).isPositive();
            assertThat(stepEight).isPositive();
            assertThat(stepNine).isPositive();
            assertThat(stepEight)
                    .isGreaterThan(stepSeven);
            assertThat(stepNine)
                    .isGreaterThan(stepEight);
        }

        @Test
        @DisplayName("default behavior persists"
                + " APPROVAL_PENDING phase")
        void stepEight_persistsApprovalPending(
                @TempDir Path tempDir)
                throws IOException {
            String content =
                    generateClaudeContent(tempDir);
            int stepEight = content.indexOf(
                    "### Step 8 \u2014 Approval Gate");
            int stepNine = content.indexOf(
                    "### Step 9 \u2014 Tag Creation");
            String stepBody = content.substring(
                    stepEight, stepNine);
            assertThat(stepBody)
                    .contains("APPROVAL_PENDING");
        }

        @Test
        @DisplayName("default behavior adds"
                + " APPROVAL_GATE_REACHED to"
                + " phasesCompleted")
        void stepEight_addsPhaseCompleted(
                @TempDir Path tempDir)
                throws IOException {
            String content =
                    generateClaudeContent(tempDir);
            int stepEight = content.indexOf(
                    "### Step 8 \u2014 Approval Gate");
            int stepNine = content.indexOf(
                    "### Step 9 \u2014 Tag Creation");
            String stepBody = content.substring(
                    stepEight, stepNine);
            assertThat(stepBody)
                    .contains("APPROVAL_GATE_REACHED");
        }

        @Test
        @DisplayName("default behavior prints human-"
                + "readable instructions with PR URL")
        void stepEight_printsInstructions(
                @TempDir Path tempDir)
                throws IOException {
            String content =
                    generateClaudeContent(tempDir);
            int stepEight = content.indexOf(
                    "### Step 8 \u2014 Approval Gate");
            int stepNine = content.indexOf(
                    "### Step 9 \u2014 Tag Creation");
            String stepBody = content.substring(
                    stepEight, stepNine);
            assertThat(stepBody)
                    .contains("APPROVAL GATE")
                    .contains("PR_URL")
                    .contains("PR_NUMBER");
        }

        @Test
        @DisplayName("default behavior exits with"
                + " exit 0")
        void stepEight_exitsZero(
                @TempDir Path tempDir)
                throws IOException {
            String content =
                    generateClaudeContent(tempDir);
            int stepEight = content.indexOf(
                    "### Step 8 \u2014 Approval Gate");
            int stepNine = content.indexOf(
                    "### Step 9 \u2014 Tag Creation");
            String stepBody = content.substring(
                    stepEight, stepNine);
            assertThat(stepBody)
                    .contains("exit 0");
        }

        @Test
        @DisplayName("default behavior documents"
                + " --continue-after-merge resume hint")
        void stepEight_resumeHint(
                @TempDir Path tempDir)
                throws IOException {
            String content =
                    generateClaudeContent(tempDir);
            int stepEight = content.indexOf(
                    "### Step 8 \u2014 Approval Gate");
            int stepNine = content.indexOf(
                    "### Step 9 \u2014 Tag Creation");
            String stepBody = content.substring(
                    stepEight, stepNine);
            assertThat(stepBody)
                    .contains("--continue-after-merge");
        }
    }

    @Nested
    @DisplayName("Step 8 — Interactive Mode")
    class InteractiveMode {

        @Test
        @DisplayName("interactive mode uses"
                + " AskUserQuestion with 3 options")
        void stepEight_askUserQuestion(
                @TempDir Path tempDir)
                throws IOException {
            String content =
                    generateClaudeContent(tempDir);
            int stepEight = content.indexOf(
                    "### Step 8 \u2014 Approval Gate");
            int stepNine = content.indexOf(
                    "### Step 9 \u2014 Tag Creation");
            String stepBody = content.substring(
                    stepEight, stepNine);
            assertThat(stepBody)
                    .contains("AskUserQuestion");
        }

        @Test
        @DisplayName("option 1: continue to tag"
                + " (verifies via gh pr view)")
        void stepEight_optionOneContinue(
                @TempDir Path tempDir)
                throws IOException {
            String content =
                    generateClaudeContent(tempDir);
            int stepEight = content.indexOf(
                    "### Step 8 \u2014 Approval Gate");
            int stepNine = content.indexOf(
                    "### Step 9 \u2014 Tag Creation");
            String stepBody = content.substring(
                    stepEight, stepNine);
            assertThat(stepBody)
                    .contains("gh pr view")
                    .contains("MERGED");
        }

        @Test
        @DisplayName("option 1: aborts with"
                + " APPROVAL_PR_STILL_OPEN if PR"
                + " not merged")
        void stepEight_optionOneAbortIfOpen(
                @TempDir Path tempDir)
                throws IOException {
            String content =
                    generateClaudeContent(tempDir);
            int stepEight = content.indexOf(
                    "### Step 8 \u2014 Approval Gate");
            int stepNine = content.indexOf(
                    "### Step 9 \u2014 Tag Creation");
            String stepBody = content.substring(
                    stepEight, stepNine);
            assertThat(stepBody)
                    .contains("APPROVAL_PR_STILL_OPEN");
        }

        @Test
        @DisplayName("option 2: halt (identical to"
                + " default exit 0 behavior)")
        void stepEight_optionTwoHalt(
                @TempDir Path tempDir)
                throws IOException {
            String content =
                    generateClaudeContent(tempDir);
            int stepEight = content.indexOf(
                    "### Step 8 \u2014 Approval Gate");
            int stepNine = content.indexOf(
                    "### Step 9 \u2014 Tag Creation");
            String stepBody = content.substring(
                    stepEight, stepNine);
            assertThat(stepBody).contains("Halt");
        }

        @Test
        @DisplayName("option 3: cancel with double"
                + " confirmation")
        void stepEight_optionThreeCancel(
                @TempDir Path tempDir)
                throws IOException {
            String content =
                    generateClaudeContent(tempDir);
            int stepEight = content.indexOf(
                    "### Step 8 \u2014 Approval Gate");
            int stepNine = content.indexOf(
                    "### Step 9 \u2014 Tag Creation");
            String stepBody = content.substring(
                    stepEight, stepNine);
            assertThat(stepBody)
                    .contains("Cancel")
                    .contains("confirmation");
        }

        @Test
        @DisplayName("option 3: documents"
                + " APPROVAL_CANCELLED error code"
                + " and exit 2")
        void stepEight_optionThreeExitTwo(
                @TempDir Path tempDir)
                throws IOException {
            String content =
                    generateClaudeContent(tempDir);
            int stepEight = content.indexOf(
                    "### Step 8 \u2014 Approval Gate");
            int stepNine = content.indexOf(
                    "### Step 9 \u2014 Tag Creation");
            String stepBody = content.substring(
                    stepEight, stepNine);
            assertThat(stepBody)
                    .contains("APPROVAL_CANCELLED")
                    .contains("exit 2");
        }
    }

    @Nested
    @DisplayName("Step 8 — State Transitions")
    class StateTransitions {

        @Test
        @DisplayName("state file transitions from"
                + " PR_OPENED to APPROVAL_PENDING")
        void stepEight_prOpenedToApprovalPending(
                @TempDir Path tempDir)
                throws IOException {
            String content =
                    generateClaudeContent(tempDir);
            int stepEight = content.indexOf(
                    "### Step 8 \u2014 Approval Gate");
            int stepNine = content.indexOf(
                    "### Step 9 \u2014 Tag Creation");
            String stepBody = content.substring(
                    stepEight, stepNine);
            assertThat(stepBody)
                    .contains("PR_OPENED")
                    .contains("APPROVAL_PENDING");
        }

        @Test
        @DisplayName("state update uses atomic"
                + " write-to-temp + rename")
        void stepEight_atomicStateWrite(
                @TempDir Path tempDir)
                throws IOException {
            String content =
                    generateClaudeContent(tempDir);
            int stepEight = content.indexOf(
                    "### Step 8 \u2014 Approval Gate");
            int stepNine = content.indexOf(
                    "### Step 9 \u2014 Tag Creation");
            String stepBody = content.substring(
                    stepEight, stepNine);
            assertThat(stepBody)
                    .contains("TMP=")
                    .contains("mv \"$TMP\"");
        }
    }

    @Nested
    @DisplayName("Error Codes — story-0035-0004 catalog")
    class ErrorCodes {

        @Test
        @DisplayName("documents APPROVAL_PR_STILL_OPEN"
                + " error code")
        void errorCatalog_hasPrStillOpen(
                @TempDir Path tempDir)
                throws IOException {
            String content =
                    generateClaudeContent(tempDir);
            assertThat(content)
                    .contains("APPROVAL_PR_STILL_OPEN");
        }

        @Test
        @DisplayName("documents APPROVAL_CANCELLED"
                + " error code")
        void errorCatalog_hasCancelled(
                @TempDir Path tempDir)
                throws IOException {
            String content =
                    generateClaudeContent(tempDir);
            assertThat(content)
                    .contains("APPROVAL_CANCELLED");
        }
    }

    @Nested
    @DisplayName("Step Renumbering — post APPROVAL-GATE")
    class StepRenumbering {

        @Test
        @DisplayName("Step 9 is now Tag Creation")
        void stepNine_tagCreation(
                @TempDir Path tempDir)
                throws IOException {
            String content =
                    generateClaudeContent(tempDir);
            assertThat(content).contains(
                    "### Step 9 \u2014 Tag Creation");
        }

        @Test
        @DisplayName("Step 10 is now Back-Merge"
                + " Develop (BACK-MERGE-DEVELOP)")
        void stepTen_backMergeDevelop(
                @TempDir Path tempDir)
                throws IOException {
            String content =
                    generateClaudeContent(tempDir);
            assertThat(content).contains(
                    "### Step 10 \u2014 Back-Merge"
                            + " Develop");
        }

        @Test
        @DisplayName("Step 11 is now Publish")
        void stepEleven_publish(
                @TempDir Path tempDir)
                throws IOException {
            String content =
                    generateClaudeContent(tempDir);
            assertThat(content).contains(
                    "### Step 11 \u2014 Publish");
        }

        @Test
        @DisplayName("Step 12 is now Cleanup")
        void stepTwelve_cleanup(
                @TempDir Path tempDir)
                throws IOException {
            String content =
                    generateClaudeContent(tempDir);
            assertThat(content).contains(
                    "### Step 12 \u2014 Cleanup");
        }
    }

    @Nested
    @DisplayName("Reference — approval-gate-workflow.md")
    class ApprovalGateWorkflowReference {

        @Test
        @DisplayName("approval-gate-workflow.md exists"
                + " after assembly")
        void referenceFile_exists(
                @TempDir Path tempDir)
                throws IOException {
            Path outputDir = generateOutput(tempDir);
            Path ref = outputDir.resolve(
                    "skills/x-release/references/"
                            + "approval-gate-workflow.md");
            assertThat(ref).exists();
        }

        @Test
        @DisplayName("approval-gate-workflow.md documents"
                + " default and interactive workflows")
        void referenceFile_hasBothWorkflows(
                @TempDir Path tempDir)
                throws IOException {
            Path outputDir = generateOutput(tempDir);
            String ref = Files.readString(
                    outputDir.resolve(
                            "skills/x-release/references/"
                                    + "approval-gate-workflow"
                                    + ".md"),
                    StandardCharsets.UTF_8);
            assertThat(ref)
                    .contains("Default")
                    .contains("Interactive")
                    .contains("APPROVAL_PENDING")
                    .contains("--continue-after-merge");
        }

        @Test
        @DisplayName("approval-gate-workflow.md documents"
                + " all 3 interactive options")
        void referenceFile_documentsThreeOptions(
                @TempDir Path tempDir)
                throws IOException {
            Path outputDir = generateOutput(tempDir);
            String ref = Files.readString(
                    outputDir.resolve(
                            "skills/x-release/references/"
                                    + "approval-gate-workflow"
                                    + ".md"),
                    StandardCharsets.UTF_8);
            assertThat(ref)
                    .contains("APPROVAL_PR_STILL_OPEN")
                    .contains("APPROVAL_CANCELLED")
                    .contains("AskUserQuestion");
        }

        @Test
        @DisplayName("approval-gate-workflow.md documents"
                + " state file transitions")
        void referenceFile_documentsStateTransitions(
                @TempDir Path tempDir)
                throws IOException {
            Path outputDir = generateOutput(tempDir);
            String ref = Files.readString(
                    outputDir.resolve(
                            "skills/x-release/references/"
                                    + "approval-gate-workflow"
                                    + ".md"),
                    StandardCharsets.UTF_8);
            assertThat(ref)
                    .contains("PR_OPENED")
                    .contains("APPROVAL_PENDING")
                    .contains("MERGED");
        }
    }

    @Nested
    @DisplayName("Behavior Preservation (RULE-002)")
    class BehaviorPreservation {

        @Test
        @DisplayName("Step 0, 1, 3, 4, 5, 6, 7 remain"
                + " present after renumbering")
        void preservesCoreSteps(
                @TempDir Path tempDir)
                throws IOException {
            String content =
                    generateClaudeContent(tempDir);
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
                    .contains("### Step 7 \u2014 Open"
                            + " Release PR");
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
