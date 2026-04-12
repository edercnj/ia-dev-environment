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
 * Tests for Phase VALIDATE-DEEP in x-release skill.
 *
 * <p>Validates that the x-release skill template
 * contains the 8+1 deep validation checks replacing
 * the old Step 2, with proper error codes, skip-tests
 * behavior, and state file advancement.</p>
 *
 * <p>Story: story-0035-0002 (EPIC-0035)</p>
 */
@DisplayName("x-release — Phase VALIDATE-DEEP"
        + " (story-0035-0002)")
class ReleaseValidateDeepTest {

    @Nested
    @DisplayName("Check 1 — Working Directory Clean")
    class Check1WorkingDirClean {

        @Test
        @DisplayName("VALIDATE-DEEP check 1 uses"
                + " git status --porcelain")
        void validateDeep_check1_usesGitStatus(
                @TempDir Path tempDir)
                throws IOException {
            String content =
                    generateClaudeContent(tempDir);
            assertThat(content)
                    .contains("git status --porcelain");
        }

        @Test
        @DisplayName("check 1 aborts with"
                + " VALIDATE_DIRTY_WORKDIR error code")
        void validateDeep_check1_abortsDirtyWorkdir(
                @TempDir Path tempDir)
                throws IOException {
            String content =
                    generateClaudeContent(tempDir);
            assertThat(content)
                    .contains("VALIDATE_DIRTY_WORKDIR");
        }
    }

    @Nested
    @DisplayName("Check 2 — Correct Branch")
    class Check2CorrectBranch {

        @Test
        @DisplayName("VALIDATE-DEEP check 2 validates"
                + " branch is develop or main for hotfix")
        void validateDeep_check2_validatesBranch(
                @TempDir Path tempDir)
                throws IOException {
            String content =
                    generateClaudeContent(tempDir);
            assertThat(content)
                    .contains("VALIDATE_WRONG_BRANCH");
        }

        @Test
        @DisplayName("check 2 uses branch --show-current")
        void validateDeep_check2_usesBranchShow(
                @TempDir Path tempDir)
                throws IOException {
            String content =
                    generateClaudeContent(tempDir);
            assertThat(content)
                    .contains("branch --show-current");
        }
    }

    @Nested
    @DisplayName("Check 3 — CHANGELOG [Unreleased]"
            + " Non-Empty")
    class Check3ChangelogUnreleased {

        @Test
        @DisplayName("VALIDATE-DEEP check 3 parses"
                + " CHANGELOG.md for [Unreleased] section")
        void validateDeep_check3_parsesChangelog(
                @TempDir Path tempDir)
                throws IOException {
            String content =
                    generateClaudeContent(tempDir);
            assertThat(content)
                    .contains("[Unreleased]")
                    .contains("CHANGELOG");
        }

        @Test
        @DisplayName("check 3 aborts with"
                + " VALIDATE_EMPTY_UNRELEASED error code")
        void validateDeep_check3_abortsEmpty(
                @TempDir Path tempDir)
                throws IOException {
            String content =
                    generateClaudeContent(tempDir);
            assertThat(content)
                    .contains(
                            "VALIDATE_EMPTY_UNRELEASED");
        }
    }

    @Nested
    @DisplayName("Check 4 — Build + Tests")
    class Check4BuildTests {

        @Test
        @DisplayName("VALIDATE-DEEP check 4 runs"
                + " build command")
        void validateDeep_check4_runsBuild(
                @TempDir Path tempDir)
                throws IOException {
            String content =
                    generateClaudeContent(tempDir);
            assertThat(content)
                    .contains("VALIDATE_BUILD_FAILED");
        }

        @Test
        @DisplayName("check 4 uses BUILD_COMMAND"
                + " template variable")
        void validateDeep_check4_usesBuildCommand(
                @TempDir Path tempDir)
                throws IOException {
            String content =
                    generateClaudeContent(tempDir);
            assertThat(content)
                    .contains("{{BUILD_COMMAND}}");
        }
    }

    @Nested
    @DisplayName("Check 5 — Coverage Thresholds")
    class Check5CoverageThresholds {

        @Test
        @DisplayName("VALIDATE-DEEP check 5 validates"
                + " line coverage threshold")
        void validateDeep_check5_validatesLineCoverage(
                @TempDir Path tempDir)
                throws IOException {
            String content =
                    generateClaudeContent(tempDir);
            assertThat(content)
                    .contains("VALIDATE_COVERAGE_LINE");
        }

        @Test
        @DisplayName("check 5 validates branch coverage"
                + " threshold")
        void validateDeep_check5_validatesBranchCoverage(
                @TempDir Path tempDir)
                throws IOException {
            String content =
                    generateClaudeContent(tempDir);
            assertThat(content)
                    .contains(
                            "VALIDATE_COVERAGE_BRANCH");
        }

        @Test
        @DisplayName("check 5 references coverage"
                + " threshold template variables")
        void validateDeep_check5_refsThresholds(
                @TempDir Path tempDir)
                throws IOException {
            String content =
                    generateClaudeContent(tempDir);
            assertThat(content)
                    .contains(
                            "{{COVERAGE_LINE_THRESHOLD}}")
                    .contains(
                            "{{COVERAGE_BRANCH_THRESHOLD}}");
        }
    }

    @Nested
    @DisplayName("Check 6 — Golden File Consistency")
    class Check6GoldenFiles {

        @Test
        @DisplayName("VALIDATE-DEEP check 6 runs"
                + " golden file tests")
        void validateDeep_check6_runsGoldenTests(
                @TempDir Path tempDir)
                throws IOException {
            String content =
                    generateClaudeContent(tempDir);
            assertThat(content)
                    .contains("VALIDATE_GOLDEN_DRIFT");
        }

        @Test
        @DisplayName("check 6 uses GOLDEN_TEST_COMMAND"
                + " template variable")
        void validateDeep_check6_usesGoldenTestCmd(
                @TempDir Path tempDir)
                throws IOException {
            String content =
                    generateClaudeContent(tempDir);
            assertThat(content)
                    .contains("{{GOLDEN_TEST_COMMAND}}");
        }
    }

    @Nested
    @DisplayName("Check 7 — Hardcoded Version Strings")
    class Check7HardcodedVersion {

        @Test
        @DisplayName("VALIDATE-DEEP check 7 searches"
                + " for hardcoded version strings")
        void validateDeep_check7_searchesHardcoded(
                @TempDir Path tempDir)
                throws IOException {
            String content =
                    generateClaudeContent(tempDir);
            assertThat(content)
                    .contains(
                            "VALIDATE_HARDCODED_VERSION");
        }

        @Test
        @DisplayName("check 7 uses grep to find"
                + " version matches")
        void validateDeep_check7_usesGrep(
                @TempDir Path tempDir)
                throws IOException {
            String content =
                    generateClaudeContent(tempDir);
            assertThat(content)
                    .contains("grep")
                    .contains("CURRENT_VERSION");
        }
    }

    @Nested
    @DisplayName("Check 8 — Cross-File Version"
            + " Consistency")
    class Check8CrossFileConsistency {

        @Test
        @DisplayName("VALIDATE-DEEP check 8 validates"
                + " version consistency")
        void validateDeep_check8_validatesConsistency(
                @TempDir Path tempDir)
                throws IOException {
            String content =
                    generateClaudeContent(tempDir);
            assertThat(content)
                    .contains(
                            "VALIDATE_VERSION_MISMATCH");
        }
    }

    @Nested
    @DisplayName("Check 9 — Generation Dry-Run"
            + " (Conditional)")
    class Check9GenerationDryRun {

        @Test
        @DisplayName("VALIDATE-DEEP check 9 is"
                + " conditional on GENERATION_COMMAND")
        void validateDeep_check9_isConditional(
                @TempDir Path tempDir)
                throws IOException {
            String content =
                    generateClaudeContent(tempDir);
            assertThat(content)
                    .contains(
                            "VALIDATE_GENERATION_DRIFT")
                    .contains(
                            "{{GENERATION_COMMAND}}");
        }
    }

    @Nested
    @DisplayName("Skip-Tests Behavior")
    class SkipTestsBehavior {

        @Test
        @DisplayName("--skip-tests skips only checks"
                + " 4, 5, 6")
        void validateDeep_skipTests_skipsOnlyChecks456(
                @TempDir Path tempDir)
                throws IOException {
            String content =
                    generateClaudeContent(tempDir);
            assertThat(content)
                    .contains("--skip-tests")
                    .contains("checks 4, 5, 6");
        }

        @Test
        @DisplayName("checks 1, 2, 3, 7, 8 are always"
                + " mandatory")
        void validateDeep_skipTests_mandatoryChecks(
                @TempDir Path tempDir)
                throws IOException {
            String content =
                    generateClaudeContent(tempDir);
            assertThat(content)
                    .contains("always-mandatory");
        }
    }

    @Nested
    @DisplayName("State File Advancement")
    class StateFileAdvancement {

        @Test
        @DisplayName("VALIDATE-DEEP advances state file"
                + " to VALIDATED")
        void validateDeep_advancesStateToValidated(
                @TempDir Path tempDir)
                throws IOException {
            String content =
                    generateClaudeContent(tempDir);
            assertThat(content)
                    .contains("phase")
                    .contains("VALIDATED");
        }
    }

    @Nested
    @DisplayName("Error Codes Complete")
    class ErrorCodesComplete {

        @Test
        @DisplayName("all 10 VALIDATE error codes present"
                + " in error handling table")
        void validateDeep_allErrorCodesPresent(
                @TempDir Path tempDir)
                throws IOException {
            String content =
                    generateClaudeContent(tempDir);
            assertThat(content)
                    .contains("VALIDATE_DIRTY_WORKDIR")
                    .contains("VALIDATE_WRONG_BRANCH")
                    .contains("VALIDATE_EMPTY_UNRELEASED")
                    .contains("VALIDATE_BUILD_FAILED")
                    .contains("VALIDATE_COVERAGE_LINE")
                    .contains("VALIDATE_COVERAGE_BRANCH")
                    .contains("VALIDATE_GOLDEN_DRIFT")
                    .contains("VALIDATE_HARDCODED_VERSION")
                    .contains("VALIDATE_VERSION_MISMATCH")
                    .contains(
                            "VALIDATE_GENERATION_DRIFT");
        }
    }

    @Nested
    @DisplayName("Phase VALIDATE-DEEP replaces Step 2")
    class ReplacesStep2 {

        @Test
        @DisplayName("workflow lists VALIDATE-DEEP"
                + " instead of old VALIDATE")
        void validateDeep_replacesStep2InWorkflow(
                @TempDir Path tempDir)
                throws IOException {
            String content =
                    generateClaudeContent(tempDir);
            assertThat(content)
                    .contains("VALIDATE-DEEP");
        }

        @Test
        @DisplayName("Step 2 heading references"
                + " VALIDATE-DEEP")
        void validateDeep_step2HeadingReferences(
                @TempDir Path tempDir)
                throws IOException {
            String content =
                    generateClaudeContent(tempDir);
            assertThat(content)
                    .contains("Phase VALIDATE-DEEP");
        }
    }

    @Nested
    @DisplayName("ContextBuilder Template Variables")
    class ContextBuilderVars {

        @Test
        @DisplayName("ContextBuilder includes"
                + " COVERAGE_LINE_THRESHOLD variable")
        void contextBuilder_hasCoverageLineThreshold() {
            var ctx = dev.iadev.config.ContextBuilder
                    .buildContext(
                            TestConfigBuilder.minimal());
            assertThat(ctx)
                    .containsKey(
                            "COVERAGE_LINE_THRESHOLD");
            assertThat(ctx.get(
                    "COVERAGE_LINE_THRESHOLD"))
                    .isEqualTo(95);
        }

        @Test
        @DisplayName("ContextBuilder includes"
                + " COVERAGE_BRANCH_THRESHOLD variable")
        void contextBuilder_hasCoverageBranchThreshold() {
            var ctx = dev.iadev.config.ContextBuilder
                    .buildContext(
                            TestConfigBuilder.minimal());
            assertThat(ctx)
                    .containsKey(
                            "COVERAGE_BRANCH_THRESHOLD");
            assertThat(ctx.get(
                    "COVERAGE_BRANCH_THRESHOLD"))
                    .isEqualTo(90);
        }

        @Test
        @DisplayName("ContextBuilder includes"
                + " GOLDEN_TEST_COMMAND with empty default")
        void contextBuilder_hasGoldenTestCommand() {
            var ctx = dev.iadev.config.ContextBuilder
                    .buildContext(
                            TestConfigBuilder.minimal());
            assertThat(ctx)
                    .containsKey("GOLDEN_TEST_COMMAND");
            assertThat(ctx.get("GOLDEN_TEST_COMMAND"))
                    .isEqualTo("");
        }

        @Test
        @DisplayName("ContextBuilder includes"
                + " GENERATION_COMMAND with empty default")
        void contextBuilder_hasGenerationCommand() {
            var ctx = dev.iadev.config.ContextBuilder
                    .buildContext(
                            TestConfigBuilder.minimal());
            assertThat(ctx)
                    .containsKey("GENERATION_COMMAND");
            assertThat(ctx.get("GENERATION_COMMAND"))
                    .isEqualTo("");
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
