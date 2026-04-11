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
 * Tests for story-0027-0007: x-pr-fix-epic-comments
 * correction PRs must target develop instead of main.
 *
 * <p>Validates that the generated SKILL.md uses
 * {@code --base develop} for PR creation and
 * {@code git checkout develop} for branch setup,
 * with baseBranch resolution from execution-state.</p>
 */
@DisplayName("x-pr-fix-epic-comments — Develop Base")
class FixEpicPrCommentsDevelopTest {

    @Nested
    @DisplayName("Claude SKILL.md — PR Base Branch")
    class PrBaseBranch {

        @Test
        @DisplayName("PR creation uses --base develop")
        void assemble_fixEpicPr_usesBaseDevelop(
                @TempDir Path tempDir)
                throws IOException {
            String content =
                    generateClaudeContent(tempDir);
            assertThat(content)
                    .contains("--base develop");
        }

        @Test
        @DisplayName("no --base main in correction flow")
        void assemble_fixEpicPr_noBaseMain(
                @TempDir Path tempDir)
                throws IOException {
            String content =
                    generateClaudeContent(tempDir);
            assertThat(content)
                    .doesNotContain("--base main");
        }

        @Test
        @DisplayName("PR output shows Base: develop")
        void assemble_fixEpicPr_prOutputShowsDevelop(
                @TempDir Path tempDir)
                throws IOException {
            String content =
                    generateClaudeContent(tempDir);
            assertThat(content)
                    .contains("Base: develop");
        }
    }

    @Nested
    @DisplayName("Claude SKILL.md — Branch Setup")
    class BranchSetup {

        @Test
        @DisplayName("branch creation uses"
                + " git checkout develop")
        void assemble_fixEpicPr_checkoutDevelop(
                @TempDir Path tempDir)
                throws IOException {
            String content =
                    generateClaudeContent(tempDir);
            assertThat(content)
                    .contains("git checkout develop");
        }

        @Test
        @DisplayName("no git checkout main"
                + " in correction flow")
        void assemble_fixEpicPr_noCheckoutMain(
                @TempDir Path tempDir)
                throws IOException {
            String content =
                    generateClaudeContent(tempDir);
            assertThat(content)
                    .doesNotContain(
                            "git checkout main");
        }

        @Test
        @DisplayName("branch creation from develop"
                + " is documented")
        void assemble_fixEpicPr_branchFromDevelop(
                @TempDir Path tempDir)
                throws IOException {
            String content =
                    generateClaudeContent(tempDir);
            assertThat(content)
                    .contains("from `develop`");
        }
    }

    @Nested
    @DisplayName("Claude SKILL.md — baseBranch Resolution")
    class BaseBranchResolution {

        @Test
        @DisplayName("documents baseBranch from"
                + " execution-state.json")
        void assemble_fixEpicPr_baseBranchFromState(
                @TempDir Path tempDir)
                throws IOException {
            String content =
                    generateClaudeContent(tempDir);
            assertThat(content)
                    .contains("baseBranch")
                    .contains("execution-state");
        }

        @Test
        @DisplayName("documents develop as default"
                + " fallback for baseBranch")
        void assemble_fixEpicPr_developFallback(
                @TempDir Path tempDir)
                throws IOException {
            String content =
                    generateClaudeContent(tempDir);
            assertThat(content)
                    .contains("develop");
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
                        "skills/x-pr-fix-epic-comments"
                                + "/SKILL.md"),
                StandardCharsets.UTF_8);
    }

}
