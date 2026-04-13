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
 * Tests for story-0035-0007: Dry-Run output reflects
 * ALL 12 new phases (0-11) with halt markers for
 * APPROVAL-GATE and BACKMERGE-CONFLICT.
 *
 * <p>Validates that the dry-run section in the x-release
 * SKILL.md shows the complete phase sequence, halt
 * markers, state file summary, and hotfix variant.</p>
 */
@DisplayName("x-release Dry-Run Output (story-0035-0007)")
class ReleaseDryRunTest {

    @Nested
    @DisplayName("Dry-Run — Phase Listing")
    class PhaseListing {

        @Test
        @DisplayName("dry-run shows all 12 phases"
                + " numbered 0-11")
        void dryRun_showsAllTwelvePhases(
                @TempDir Path tempDir)
                throws IOException {
            String content =
                    generateClaudeContent(tempDir);
            assertThat(content)
                    .contains("0. RESUME_DETECTION")
                    .contains("1. DETERMINE")
                    .contains("2. VALIDATE_DEEP")
                    .contains("3. BRANCH")
                    .contains("4. UPDATE")
                    .contains("5. CHANGELOG")
                    .contains("6. COMMIT")
                    .contains("7. OPEN_RELEASE_PR")
                    .contains("8. APPROVAL_GATE")
                    .contains("9. RESUME_AND_TAG")
                    .contains("10. BACK_MERGE_DEVELOP")
                    .contains("11. PUBLISH")
                    .contains("12. CLEANUP");
        }

        @Test
        @DisplayName("dry-run shows halt marker at"
                + " Phase 8 APPROVAL_GATE")
        void dryRun_showsHaltMarkerAtPhase8(
                @TempDir Path tempDir)
                throws IOException {
            String content =
                    generateClaudeContent(tempDir);
            assertThat(content)
                    .contains("SKILL WILL HALT HERE");
        }

        @Test
        @DisplayName("dry-run shows HUMAN MUST MERGE"
                + " PR label")
        void dryRun_showsHumanMustMergeLabel(
                @TempDir Path tempDir)
                throws IOException {
            String content =
                    generateClaudeContent(tempDir);
            assertThat(content).contains(
                    "HUMAN MUST MERGE PR IN GITHUB");
        }

        @Test
        @DisplayName("dry-run shows NO CHANGES MADE"
                + " footer")
        void dryRun_showsNoChangesMadeFooter(
                @TempDir Path tempDir)
                throws IOException {
            String content =
                    generateClaudeContent(tempDir);
            assertThat(content)
                    .contains("=== NO CHANGES MADE ===");
        }

        @Test
        @DisplayName("dry-run shows RELEASE PLAN"
                + " DRY-RUN header")
        void dryRun_showsReleasePlanHeader(
                @TempDir Path tempDir)
                throws IOException {
            String content =
                    generateClaudeContent(tempDir);
            assertThat(content).contains(
                    "=== RELEASE PLAN (DRY-RUN) ===");
        }
    }

    @Nested
    @DisplayName("Dry-Run — State File and Mode")
    class StateFileAndMode {

        @Test
        @DisplayName("dry-run shows State file path")
        void dryRun_showsStateFilePath(
                @TempDir Path tempDir)
                throws IOException {
            String content =
                    generateClaudeContent(tempDir);
            assertThat(content)
                    .contains("State file:");
        }

        @Test
        @DisplayName("dry-run shows Mode field")
        void dryRun_showsModeField(
                @TempDir Path tempDir)
                throws IOException {
            String content =
                    generateClaudeContent(tempDir);
            assertThat(content)
                    .contains("Mode:");
        }

        @Test
        @DisplayName("dry-run shows estimated duration"
                + " per segment")
        void dryRun_showsEstimatedDuration(
                @TempDir Path tempDir)
                throws IOException {
            String content =
                    generateClaudeContent(tempDir);
            assertThat(content)
                    .contains("Estimated duration:");
        }

        @Test
        @DisplayName("dry-run shows --continue-after-merge"
                + " instruction in resume phase")
        void dryRun_showsContinueAfterMerge(
                @TempDir Path tempDir)
                throws IOException {
            String content =
                    generateClaudeContent(tempDir);
            assertThat(content)
                    .contains("--continue-after-merge");
        }
    }

    @Nested
    @DisplayName("Dry-Run — Hotfix Variant")
    class HotfixVariant {

        @Test
        @DisplayName("dry-run documents hotfix mode"
                + " differences")
        void dryRun_documentsHotfixMode(
                @TempDir Path tempDir)
                throws IOException {
            String content =
                    generateClaudeContent(tempDir);
            assertThat(content)
                    .contains("hotfix mode")
                    .contains("Source branch:")
                    .contains("main");
        }

        @Test
        @DisplayName("dry-run hotfix mode shows"
                + " patch forced")
        void dryRun_hotfixShowsPatchForced(
                @TempDir Path tempDir)
                throws IOException {
            String content =
                    generateClaudeContent(tempDir);
            assertThat(content)
                    .contains("patch (forced)");
        }
    }

    @Nested
    @DisplayName("Dry-Run — Visual Separators")
    class VisualSeparators {

        @Test
        @DisplayName("dry-run has visual separator"
                + " lines around Phase 8")
        void dryRun_hasSeparatorsAroundPhase8(
                @TempDir Path tempDir)
                throws IOException {
            String content =
                    generateClaudeContent(tempDir);
            assertThat(content)
                    .contains("----------");
        }

        @Test
        @DisplayName("dry-run shows Flags active field")
        void dryRun_showsFlagsActiveField(
                @TempDir Path tempDir)
                throws IOException {
            String content =
                    generateClaudeContent(tempDir);
            assertThat(content)
                    .contains("Flags active:");
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
