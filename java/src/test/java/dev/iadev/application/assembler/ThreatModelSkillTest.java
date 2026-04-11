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
 * Tests for story-0013-0022: x-threat-model skill for
 * STRIDE threat modeling.
 *
 * <p>Validates that the x-threat-model skill template is
 * generated correctly with proper frontmatter, STRIDE
 * categories, severity classification, and fallback
 * instructions.</p>
 */
@DisplayName("x-threat-model Skill")
class ThreatModelSkillTest {

    @Nested
    @DisplayName("Claude SKILL.md — Frontmatter")
    class ClaudeFrontmatter {

        @Test
        @DisplayName("x-threat-model SKILL.md exists after"
                + " assembly")
        void assemble_threatModel_skillMdExists(
                @TempDir Path tempDir)
                throws IOException {
            Path outputDir = generateOutput(tempDir);
            Path skillMd = outputDir.resolve(
                    "skills/x-threat-model/SKILL.md");
            assertThat(skillMd).exists();
        }

        @Test
        @DisplayName("frontmatter contains name:"
                + " x-threat-model")
        void assemble_threatModel_hasName(
                @TempDir Path tempDir)
                throws IOException {
            String content =
                    generateClaudeContent(tempDir);
            assertThat(content)
                    .contains("name: x-threat-model");
        }

        @Test
        @DisplayName("frontmatter contains"
                + " user-invocable: true")
        void assemble_threatModel_hasUserInvocable(
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
        @DisplayName("frontmatter contains argument-hint")
        void assemble_threatModel_hasArgumentHint(
                @TempDir Path tempDir)
                throws IOException {
            String content =
                    generateClaudeContent(tempDir);
            assertThat(content)
                    .contains("argument-hint:");
        }

        @Test
        @DisplayName("frontmatter contains allowed-tools")
        void assemble_threatModel_hasAllowedTools(
                @TempDir Path tempDir)
                throws IOException {
            String content =
                    generateClaudeContent(tempDir);
            assertThat(content)
                    .contains("allowed-tools:");
        }

        @Test
        @DisplayName("allowed-tools includes Read, Write,"
                + " Glob, Grep, Agent")
        void assemble_threatModel_hasExpectedTools(
                @TempDir Path tempDir)
                throws IOException {
            String content =
                    generateClaudeContent(tempDir);
            assertThat(content)
                    .contains("Read")
                    .contains("Write")
                    .contains("Glob")
                    .contains("Grep")
                    .contains("Agent");
        }

        @Test
        @DisplayName("frontmatter contains description"
                + " with STRIDE")
        void assemble_threatModel_hasDescription(
                @TempDir Path tempDir)
                throws IOException {
            String content =
                    generateClaudeContent(tempDir);
            assertThat(content)
                    .contains("description:")
                    .contains("STRIDE");
        }
    }

    @Nested
    @DisplayName("Claude SKILL.md — STRIDE Categories")
    class StrideCategories {

        @Test
        @DisplayName("contains Spoofing category")
        void assemble_threatModel_hasSpoofing(
                @TempDir Path tempDir)
                throws IOException {
            String content =
                    generateClaudeContent(tempDir);
            assertThat(content)
                    .contains("Spoofing");
        }

        @Test
        @DisplayName("contains Tampering category")
        void assemble_threatModel_hasTampering(
                @TempDir Path tempDir)
                throws IOException {
            String content =
                    generateClaudeContent(tempDir);
            assertThat(content)
                    .contains("Tampering");
        }

        @Test
        @DisplayName("contains Repudiation category")
        void assemble_threatModel_hasRepudiation(
                @TempDir Path tempDir)
                throws IOException {
            String content =
                    generateClaudeContent(tempDir);
            assertThat(content)
                    .contains("Repudiation");
        }

        @Test
        @DisplayName("contains Information Disclosure"
                + " category")
        void assemble_threatModel_hasInfoDisclosure(
                @TempDir Path tempDir)
                throws IOException {
            String content =
                    generateClaudeContent(tempDir);
            assertThat(content)
                    .contains("Information Disclosure");
        }

        @Test
        @DisplayName("contains Denial of Service category")
        void assemble_threatModel_hasDenialOfService(
                @TempDir Path tempDir)
                throws IOException {
            String content =
                    generateClaudeContent(tempDir);
            assertThat(content)
                    .contains("Denial of Service");
        }

        @Test
        @DisplayName("contains Elevation of Privilege"
                + " category")
        void assemble_threatModel_hasElevationOfPrivilege(
                @TempDir Path tempDir)
                throws IOException {
            String content =
                    generateClaudeContent(tempDir);
            assertThat(content)
                    .contains("Elevation of Privilege");
        }
    }

    @Nested
    @DisplayName("Claude SKILL.md — Workflow")
    class Workflow {

        @Test
        @DisplayName("contains 7-step workflow")
        void assemble_threatModel_hasWorkflowSteps(
                @TempDir Path tempDir)
                throws IOException {
            String content =
                    generateClaudeContent(tempDir);
            assertThat(content)
                    .contains("IDENTIFY")
                    .contains("MAP")
                    .contains("ANALYZE")
                    .contains("CLASSIFY")
                    .contains("MITIGATE")
                    .contains("GENERATE");
        }

        @Test
        @DisplayName("references security KP for"
                + " mitigations")
        void assemble_threatModel_refsSecurityKp(
                @TempDir Path tempDir)
                throws IOException {
            String content =
                    generateClaudeContent(tempDir);
            assertThat(content)
                    .contains("security");
        }

        @Test
        @DisplayName("references _TEMPLATE-THREAT-MODEL.md")
        void assemble_threatModel_refsThreatModelTemplate(
                @TempDir Path tempDir)
                throws IOException {
            String content =
                    generateClaudeContent(tempDir);
            assertThat(content)
                    .contains("threat-model");
        }
    }

    @Nested
    @DisplayName("Claude SKILL.md — Severity Classification")
    class SeverityClassification {

        @Test
        @DisplayName("contains CRITICAL severity level")
        void assemble_threatModel_hasCritical(
                @TempDir Path tempDir)
                throws IOException {
            String content =
                    generateClaudeContent(tempDir);
            assertThat(content)
                    .contains("CRITICAL");
        }

        @Test
        @DisplayName("contains HIGH severity level")
        void assemble_threatModel_hasHigh(
                @TempDir Path tempDir)
                throws IOException {
            String content =
                    generateClaudeContent(tempDir);
            assertThat(content)
                    .contains("HIGH");
        }

        @Test
        @DisplayName("contains MEDIUM severity level")
        void assemble_threatModel_hasMedium(
                @TempDir Path tempDir)
                throws IOException {
            String content =
                    generateClaudeContent(tempDir);
            assertThat(content)
                    .contains("MEDIUM");
        }

        @Test
        @DisplayName("contains LOW severity level")
        void assemble_threatModel_hasLow(
                @TempDir Path tempDir)
                throws IOException {
            String content =
                    generateClaudeContent(tempDir);
            assertThat(content)
                    .contains("LOW");
        }
    }

    @Nested
    @DisplayName("Claude SKILL.md — Fallback Instructions")
    class FallbackInstructions {

        @Test
        @DisplayName("contains fallback when no"
                + " architecture plan exists")
        void assemble_threatModel_hasFallback(
                @TempDir Path tempDir)
                throws IOException {
            String content =
                    generateClaudeContent(tempDir);
            assertThat(content)
                    .contains("codebase");
        }

        @Test
        @DisplayName("contains alternative formats"
                + " PASTA and LINDDUN")
        void assemble_threatModel_hasAlternativeFormats(
                @TempDir Path tempDir)
                throws IOException {
            String content =
                    generateClaudeContent(tempDir);
            assertThat(content)
                    .contains("PASTA")
                    .contains("LINDDUN");
        }
    }

    @Nested
    @DisplayName("Claude SKILL.md — Threat Matrix")
    class ThreatMatrix {

        @Test
        @DisplayName("contains threat matrix structure")
        void assemble_threatModel_hasThreatMatrix(
                @TempDir Path tempDir)
                throws IOException {
            String content =
                    generateClaudeContent(tempDir);
            assertThat(content)
                    .contains("Threat Matrix");
        }
    }

    @Nested
    @DisplayName("SkillGroupRegistry — Review Group")
    class RegistryReviewGroup {

        @Test
        @DisplayName("review group contains"
                + " x-threat-model")
        void register_reviewGroup_containsThreatModel() {
            assertThat(SkillGroupRegistry.SKILL_GROUPS
                    .get("review"))
                    .contains("x-threat-model");
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
                        "skills/x-threat-model/SKILL.md"),
                StandardCharsets.UTF_8);
    }

}
