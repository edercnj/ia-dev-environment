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
 * Tests for story-0016-0004: x-spec-drift skill
 * for standalone spec drift detection.
 *
 * <p>Validates that the x-spec-drift skill template
 * is generated correctly with proper frontmatter, drift
 * check categories, output format, and exit code
 * semantics.</p>
 */
@DisplayName("x-spec-drift Skill")
class SpecDriftCheckSkillTest {

    @Nested
    @DisplayName("Claude SKILL.md — Frontmatter")
    class ClaudeFrontmatter {

        @Test
        @DisplayName("x-spec-drift SKILL.md exists"
                + " after assembly")
        void assemble_specDriftCheck_skillMdExists(
                @TempDir Path tempDir)
                throws IOException {
            Path outputDir = generateOutput(tempDir);
            Path skillMd = outputDir.resolve(
                    "skills/x-spec-drift/SKILL.md");
            assertThat(skillMd).exists();
        }

        @Test
        @DisplayName("frontmatter contains name:"
                + " x-spec-drift")
        void assemble_specDriftCheck_hasName(
                @TempDir Path tempDir)
                throws IOException {
            String content =
                    generateClaudeContent(tempDir);
            assertThat(content)
                    .contains("name: x-spec-drift");
        }

        @Test
        @DisplayName("frontmatter contains"
                + " user-invocable: true")
        void assemble_specDriftCheck_hasUserInvocable(
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
                + " with STORY-ID")
        void assemble_specDriftCheck_hasArgumentHint(
                @TempDir Path tempDir)
                throws IOException {
            String content =
                    generateClaudeContent(tempDir);
            assertThat(content)
                    .contains("argument-hint:")
                    .contains("STORY");
        }

        @Test
        @DisplayName("frontmatter contains allowed-tools")
        void assemble_specDriftCheck_hasAllowedTools(
                @TempDir Path tempDir)
                throws IOException {
            String content =
                    generateClaudeContent(tempDir);
            assertThat(content)
                    .contains("allowed-tools:");
        }

        @Test
        @DisplayName("frontmatter contains description"
                + " with drift")
        void assemble_specDriftCheck_hasDescription(
                @TempDir Path tempDir)
                throws IOException {
            String content =
                    generateClaudeContent(tempDir);
            assertThat(content)
                    .contains("description:")
                    .contains("drift");
        }
    }

    @Nested
    @DisplayName("Claude SKILL.md — Drift Categories")
    class DriftCategories {

        @Test
        @DisplayName("contains Field Missing check")
        void assemble_specDriftCheck_hasFieldMissing(
                @TempDir Path tempDir)
                throws IOException {
            String content =
                    generateClaudeContent(tempDir);
            assertThat(content)
                    .contains("Field Missing");
        }

        @Test
        @DisplayName("contains Field Type Mismatch check")
        void assemble_specDriftCheck_hasFieldTypeMismatch(
                @TempDir Path tempDir)
                throws IOException {
            String content =
                    generateClaudeContent(tempDir);
            assertThat(content)
                    .contains("Field Type Mismatch");
        }

        @Test
        @DisplayName("contains Endpoint Missing check")
        void assemble_specDriftCheck_hasEndpointMissing(
                @TempDir Path tempDir)
                throws IOException {
            String content =
                    generateClaudeContent(tempDir);
            assertThat(content)
                    .contains("Endpoint Missing");
        }

        @Test
        @DisplayName("contains Scenario Uncovered check")
        void assemble_specDriftCheck_hasScenarioUncovered(
                @TempDir Path tempDir)
                throws IOException {
            String content =
                    generateClaudeContent(tempDir);
            assertThat(content)
                    .contains("Scenario Uncovered");
        }

        @Test
        @DisplayName("contains Naming Violation check")
        void assemble_specDriftCheck_hasNamingViolation(
                @TempDir Path tempDir)
                throws IOException {
            String content =
                    generateClaudeContent(tempDir);
            assertThat(content)
                    .contains("Naming Violation");
        }
    }

    @Nested
    @DisplayName("Claude SKILL.md — Severity Levels")
    class SeverityLevels {

        @Test
        @DisplayName("contains FAIL severity")
        void assemble_specDriftCheck_hasFail(
                @TempDir Path tempDir)
                throws IOException {
            String content =
                    generateClaudeContent(tempDir);
            assertThat(content)
                    .contains("FAIL");
        }

        @Test
        @DisplayName("contains WARN severity")
        void assemble_specDriftCheck_hasWarn(
                @TempDir Path tempDir)
                throws IOException {
            String content =
                    generateClaudeContent(tempDir);
            assertThat(content)
                    .contains("WARN");
        }

        @Test
        @DisplayName("contains PASS status")
        void assemble_specDriftCheck_hasPass(
                @TempDir Path tempDir)
                throws IOException {
            String content =
                    generateClaudeContent(tempDir);
            assertThat(content)
                    .contains("PASS");
        }
    }

    @Nested
    @DisplayName("Claude SKILL.md — Output Format")
    class OutputFormat {

        @Test
        @DisplayName("contains Data Contracts section")
        void assemble_specDriftCheck_hasDataContracts(
                @TempDir Path tempDir)
                throws IOException {
            String content =
                    generateClaudeContent(tempDir);
            assertThat(content)
                    .contains("Data Contracts");
        }

        @Test
        @DisplayName("contains Endpoints section")
        void assemble_specDriftCheck_hasEndpoints(
                @TempDir Path tempDir)
                throws IOException {
            String content =
                    generateClaudeContent(tempDir);
            assertThat(content)
                    .contains("Endpoints");
        }

        @Test
        @DisplayName("contains Gherkin Coverage section")
        void assemble_specDriftCheck_hasGherkinCoverage(
                @TempDir Path tempDir)
                throws IOException {
            String content =
                    generateClaudeContent(tempDir);
            assertThat(content)
                    .contains("Gherkin Coverage");
        }

        @Test
        @DisplayName("contains Summary line with"
                + " DRIFT DETECTED")
        void assemble_specDriftCheck_hasSummary(
                @TempDir Path tempDir)
                throws IOException {
            String content =
                    generateClaudeContent(tempDir);
            assertThat(content)
                    .contains("DRIFT DETECTED");
        }
    }

    @Nested
    @DisplayName("Claude SKILL.md — Workflow Steps")
    class WorkflowSteps {

        @Test
        @DisplayName("contains PARSE step")
        void assemble_specDriftCheck_hasParseStep(
                @TempDir Path tempDir)
                throws IOException {
            String content =
                    generateClaudeContent(tempDir);
            assertThat(content)
                    .contains("PARSE");
        }

        @Test
        @DisplayName("contains SCAN step")
        void assemble_specDriftCheck_hasScanStep(
                @TempDir Path tempDir)
                throws IOException {
            String content =
                    generateClaudeContent(tempDir);
            assertThat(content)
                    .contains("SCAN");
        }

        @Test
        @DisplayName("contains REPORT step")
        void assemble_specDriftCheck_hasReportStep(
                @TempDir Path tempDir)
                throws IOException {
            String content =
                    generateClaudeContent(tempDir);
            assertThat(content)
                    .contains("REPORT");
        }
    }

    @Nested
    @DisplayName("Claude SKILL.md — Exit Code")
    class ExitCode {

        @Test
        @DisplayName("documents exit code semantics")
        void assemble_specDriftCheck_hasExitCode(
                @TempDir Path tempDir)
                throws IOException {
            String content =
                    generateClaudeContent(tempDir);
            assertThat(content)
                    .containsIgnoringCase("exit code");
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
                        "skills/x-spec-drift"
                                + "/SKILL.md"),
                StandardCharsets.UTF_8);
    }

}
