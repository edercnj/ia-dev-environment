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
 * Tests for story-0039-0014: Hotfix parity section in the
 * x-release SKILL.md documents the new error codes and
 * parametric behaviours introduced by
 * {@code ReleaseContext.forHotfix()}.
 */
@DisplayName("x-release Hotfix Parity (story-0039-0014)")
class ReleaseHotfixParityTest {

    @Nested
    @DisplayName("Error Catalog — new codes")
    class ErrorCatalogNewCodes {

        @Test
        @DisplayName("HOTFIX_INVALID_COMMITS documented")
        void hotfixInvalidCommits_documented(
                @TempDir Path tempDir)
                throws IOException {
            String content =
                    generateClaudeContent(tempDir);

            assertThat(content)
                    .contains("HOTFIX_INVALID_COMMITS");
        }

        @Test
        @DisplayName("HOTFIX_VERSION_NOT_PATCH "
                + "documented")
        void hotfixVersionNotPatch_documented(
                @TempDir Path tempDir)
                throws IOException {
            String content =
                    generateClaudeContent(tempDir);

            assertThat(content)
                    .contains("HOTFIX_VERSION_NOT_PATCH");
        }
    }

    @Nested
    @DisplayName("Hotfix Flow — parity documentation")
    class HotfixFlowParity {

        @Test
        @DisplayName("Hotfix Flow section exists")
        void hotfixFlowSection_exists(
                @TempDir Path tempDir)
                throws IOException {
            String content =
                    generateClaudeContent(tempDir);

            assertThat(content)
                    .contains("Hotfix Flow (Parity");
        }

        @Test
        @DisplayName("documents separate state file path")
        void hotfixFlow_stateFilePath(
                @TempDir Path tempDir)
                throws IOException {
            String content =
                    generateClaudeContent(tempDir);

            assertThat(content).contains(
                    "release-state-hotfix-");
        }

        @Test
        @DisplayName("documents modo HOTFIX banner")
        void hotfixFlow_banner(
                @TempDir Path tempDir)
                throws IOException {
            String content =
                    generateClaudeContent(tempDir);

            assertThat(content)
                    .contains("modo HOTFIX")
                    .contains("base=main")
                    .contains("bump=PATCH");
        }

        @Test
        @DisplayName("documents telemetry releaseType "
                + "derivation")
        void hotfixFlow_releaseType(
                @TempDir Path tempDir)
                throws IOException {
            String content =
                    generateClaudeContent(tempDir);

            assertThat(content)
                    .contains("releaseType")
                    .contains("hotfix");
        }

        @Test
        @DisplayName("documents summary diagram variant")
        void hotfixFlow_summaryDiagram(
                @TempDir Path tempDir)
                throws IOException {
            String content =
                    generateClaudeContent(tempDir);

            assertThat(content)
                    .contains("hotfix/X.Y.Z");
        }

        @Test
        @DisplayName("documents security checklist")
        void hotfixFlow_security(
                @TempDir Path tempDir)
                throws IOException {
            String content =
                    generateClaudeContent(tempDir);

            assertThat(content)
                    .contains("OWASP A03")
                    .contains("CWE-22");
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
        Path skillMd = outputDir.resolve("skills/x-release/SKILL.md");
        Path fullProtocol = outputDir.resolve(
                "skills/x-release/references/full-protocol.md");
        String content = Files.readString(skillMd, StandardCharsets.UTF_8);
        if (Files.exists(fullProtocol)) {
            content += "\n" + Files.readString(fullProtocol, StandardCharsets.UTF_8);
        }
        return content;
    }
}
