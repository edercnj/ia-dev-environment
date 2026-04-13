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
 * Tests for story-0035-0007: Hotfix workflow updated
 * to use PR-flow instead of direct merge.
 *
 * <p>Validates that the Hotfix Release section in the
 * x-release SKILL.md uses PR-flow (gh pr create) for
 * merges to main and develop, detects active release
 * branches, and includes HOTFIX_INVALID_BUMP error.</p>
 */
@DisplayName("x-release Hotfix Workflow (story-0035-0007)")
class ReleaseHotfixWorkflowTest {

    @Nested
    @DisplayName("Hotfix — PR-Flow Instead of Merge")
    class PrFlowInsteadOfMerge {

        @Test
        @DisplayName("hotfix uses gh pr create for"
                + " main PR")
        void hotfix_usesGhPrCreateForMain(
                @TempDir Path tempDir)
                throws IOException {
            String content =
                    generateClaudeContent(tempDir);
            String hotfixSection =
                    extractHotfixSection(content);
            assertThat(hotfixSection)
                    .contains("gh pr create")
                    .contains("--base main");
        }

        @Test
        @DisplayName("hotfix uses PR-flow for develop"
                + " back-merge")
        void hotfix_usesPrFlowForDevelop(
                @TempDir Path tempDir)
                throws IOException {
            String content =
                    generateClaudeContent(tempDir);
            String hotfixSection =
                    extractHotfixSection(content);
            assertThat(hotfixSection)
                    .contains("--base develop");
        }

        @Test
        @DisplayName("hotfix does NOT contain direct"
                + " git merge to main")
        void hotfix_noDirectMergeToMain(
                @TempDir Path tempDir)
                throws IOException {
            String content =
                    generateClaudeContent(tempDir);
            String hotfixSection =
                    extractHotfixSection(content);
            assertThat(hotfixSection)
                    .doesNotContain(
                            "git checkout main\n"
                            + "git merge");
        }
    }

    @Nested
    @DisplayName("Hotfix — Phase Differences Matrix")
    class PhaseDifferencesMatrix {

        @Test
        @DisplayName("hotfix documents Standard vs"
                + " Hotfix phase differences")
        void hotfix_documentsPhaseMatrix(
                @TempDir Path tempDir)
                throws IOException {
            String content =
                    generateClaudeContent(tempDir);
            String hotfixSection =
                    extractHotfixSection(content);
            assertThat(hotfixSection)
                    .contains("Standard")
                    .contains("Hotfix");
        }

        @Test
        @DisplayName("hotfix Phase 1 forces patch"
                + " bump")
        void hotfix_phase1ForcesPatchBump(
                @TempDir Path tempDir)
                throws IOException {
            String content =
                    generateClaudeContent(tempDir);
            String hotfixSection =
                    extractHotfixSection(content);
            assertThat(hotfixSection)
                    .contains("patch")
                    .contains("HOTFIX_INVALID_BUMP");
        }

        @Test
        @DisplayName("hotfix Phase 10 skips SNAPSHOT"
                + " advance")
        void hotfix_phase10SkipsSnapshot(
                @TempDir Path tempDir)
                throws IOException {
            String content =
                    generateClaudeContent(tempDir);
            String hotfixSection =
                    extractHotfixSection(content);
            assertThat(hotfixSection)
                    .containsIgnoringCase(
                            "SNAPSHOT advance")
                    .containsIgnoringCase("skip");
        }
    }

    @Nested
    @DisplayName("Hotfix — Active Release Detection")
    class ActiveReleaseDetection {

        @Test
        @DisplayName("hotfix detects active release/*"
                + " branch")
        void hotfix_detectsActiveReleaseBranch(
                @TempDir Path tempDir)
                throws IOException {
            String content =
                    generateClaudeContent(tempDir);
            String hotfixSection =
                    extractHotfixSection(content);
            assertThat(hotfixSection)
                    .contains("release/")
                    .contains("git branch");
        }

        @Test
        @DisplayName("hotfix creates additional PR to"
                + " active release branch")
        void hotfix_createsAdditionalPrToRelease(
                @TempDir Path tempDir)
                throws IOException {
            String content =
                    generateClaudeContent(tempDir);
            String hotfixSection =
                    extractHotfixSection(content);
            assertThat(hotfixSection).satisfiesAnyOf(
                    c -> assertThat(c).contains(
                            "--base \"$RELEASE_BRANCH\""),
                    c -> assertThat(c).contains(
                            "--base $RELEASE_BRANCH"));
        }
    }

    @Nested
    @DisplayName("Hotfix — HOTFIX_INVALID_BUMP Error")
    class HotfixInvalidBumpError {

        @Test
        @DisplayName("HOTFIX_INVALID_BUMP error code"
                + " is documented")
        void hotfix_invalidBumpErrorDocumented(
                @TempDir Path tempDir)
                throws IOException {
            String content =
                    generateClaudeContent(tempDir);
            assertThat(content)
                    .contains("HOTFIX_INVALID_BUMP");
        }

        @Test
        @DisplayName("HOTFIX_INVALID_BUMP message"
                + " mentions patch only")
        void hotfix_invalidBumpMessageMentionsPatch(
                @TempDir Path tempDir)
                throws IOException {
            String content =
                    generateClaudeContent(tempDir);
            assertThat(content).contains(
                    "Hotfix mode only allows patch"
                            + " bump");
        }
    }

    @Nested
    @DisplayName("Hotfix — Preserves Existing Behavior")
    class PreservesExisting {

        @Test
        @DisplayName("hotfix still enforces PATCH"
                + " only")
        void hotfix_stillEnforcesPatchOnly(
                @TempDir Path tempDir)
                throws IOException {
            String content =
                    generateClaudeContent(tempDir);
            assertThat(content)
                    .contains("PATCH only");
        }

        @Test
        @DisplayName("hotfix still starts from main")
        void hotfix_stillStartsFromMain(
                @TempDir Path tempDir)
                throws IOException {
            String content =
                    generateClaudeContent(tempDir);
            String hotfixSection =
                    extractHotfixSection(content);
            assertThat(hotfixSection)
                    .contains("main");
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

    private String extractHotfixSection(String content) {
        int start = content.indexOf(
                "## Hotfix Release");
        if (start == -1) {
            return "";
        }
        int end = content.indexOf("\n## ", start + 1);
        if (end == -1) {
            end = content.length();
        }
        return content.substring(start, end);
    }

}
