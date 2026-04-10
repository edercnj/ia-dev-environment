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
 * Tests for story-0022-0003: Security Skill Template
 * and CI Integration Pattern.
 *
 * <p>Validates that the security-skill-template.md
 * reference file exists, contains all mandatory sections,
 * and the security KP SKILL.md references it.</p>
 */
@DisplayName("Security Skill Template + CI Integration")
class SecuritySkillTemplateTest {

    @Nested
    @DisplayName("Template File Existence")
    class TemplateFileExistence {

        @Test
        @DisplayName("security-skill-template.md exists"
                + " in output")
        void assemble_skillTemplate_fileExists(
                @TempDir Path tempDir)
                throws IOException {
            generateSecurityOutput(tempDir);
            Path templateFile = tempDir.resolve(
                    "output/skills/security/references/"
                            + "security-skill-template.md");
            assertThat(templateFile).exists();
        }

        @Test
        @DisplayName("security-skill-template.md is"
                + " non-empty")
        void assemble_skillTemplate_nonEmpty(
                @TempDir Path tempDir)
                throws IOException {
            generateSecurityOutput(tempDir);
            String content = readTemplateContent(tempDir);
            assertThat(content).isNotEmpty();
        }
    }

    @Nested
    @DisplayName("Mandatory Sections — Structure")
    class MandatorySections {

        @Test
        @DisplayName("template contains Tool Selection"
                + " section")
        void assemble_skillTemplate_hasToolSelection(
                @TempDir Path tempDir)
                throws IOException {
            generateSecurityOutput(tempDir);
            String content = readTemplateContent(tempDir);
            assertThat(content)
                    .contains("## Tool Selection");
        }

        @Test
        @DisplayName("template contains Parameters"
                + " section")
        void assemble_skillTemplate_hasParameters(
                @TempDir Path tempDir)
                throws IOException {
            generateSecurityOutput(tempDir);
            String content = readTemplateContent(tempDir);
            assertThat(content)
                    .contains("## Parameters");
        }

        @Test
        @DisplayName("template contains Output Format"
                + " section")
        void assemble_skillTemplate_hasOutputFormat(
                @TempDir Path tempDir)
                throws IOException {
            generateSecurityOutput(tempDir);
            String content = readTemplateContent(tempDir);
            assertThat(content)
                    .contains("## Output Format");
        }

        @Test
        @DisplayName("template contains Error Handling"
                + " section")
        void assemble_skillTemplate_hasErrorHandling(
                @TempDir Path tempDir)
                throws IOException {
            generateSecurityOutput(tempDir);
            String content = readTemplateContent(tempDir);
            assertThat(content)
                    .contains("## Error Handling");
        }

        @Test
        @DisplayName("template contains CI Integration"
                + " section")
        void assemble_skillTemplate_hasCiIntegration(
                @TempDir Path tempDir)
                throws IOException {
            generateSecurityOutput(tempDir);
            String content = readTemplateContent(tempDir);
            assertThat(content)
                    .contains("## CI Integration");
        }

        @Test
        @DisplayName("template contains Idempotency"
                + " section")
        void assemble_skillTemplate_hasIdempotency(
                @TempDir Path tempDir)
                throws IOException {
            generateSecurityOutput(tempDir);
            String content = readTemplateContent(tempDir);
            assertThat(content)
                    .contains("## Idempotency");
        }
    }

    @Nested
    @DisplayName("Tool Selection Table Format")
    class ToolSelectionTable {

        @Test
        @DisplayName("tool-selection table has Build Tool"
                + " column")
        void assemble_skillTemplate_hasBuildToolColumn(
                @TempDir Path tempDir)
                throws IOException {
            generateSecurityOutput(tempDir);
            String content = readTemplateContent(tempDir);
            assertThat(content)
                    .contains("Build Tool");
        }

        @Test
        @DisplayName("tool-selection table has Language"
                + " column")
        void assemble_skillTemplate_hasLanguageColumn(
                @TempDir Path tempDir)
                throws IOException {
            generateSecurityOutput(tempDir);
            String content = readTemplateContent(tempDir);
            assertThat(content)
                    .contains("Language");
        }

        @Test
        @DisplayName("tool-selection table has Preferred"
                + " Tool column")
        void assemble_skillTemplate_hasPreferredToolColumn(
                @TempDir Path tempDir)
                throws IOException {
            generateSecurityOutput(tempDir);
            String content = readTemplateContent(tempDir);
            assertThat(content)
                    .contains("Preferred Tool");
        }

        @Test
        @DisplayName("tool-selection table has Fallback"
                + " Tool column")
        void assemble_skillTemplate_hasFallbackToolColumn(
                @TempDir Path tempDir)
                throws IOException {
            generateSecurityOutput(tempDir);
            String content = readTemplateContent(tempDir);
            assertThat(content)
                    .contains("Fallback Tool");
        }

        @Test
        @DisplayName("tool-selection table has Install"
                + " Command column")
        void assemble_skillTemplate_hasInstallCmdColumn(
                @TempDir Path tempDir)
                throws IOException {
            generateSecurityOutput(tempDir);
            String content = readTemplateContent(tempDir);
            assertThat(content)
                    .contains("Install Command");
        }

        @Test
        @DisplayName("tool-selection table includes"
                + " Semgrep as universal fallback")
        void assemble_skillTemplate_hasSemgrepFallback(
                @TempDir Path tempDir)
                throws IOException {
            generateSecurityOutput(tempDir);
            String content = readTemplateContent(tempDir);
            assertThat(content)
                    .contains("Semgrep");
        }
    }

    @Nested
    @DisplayName("CI Integration Snippets")
    class CiIntegrationSnippets {

        @Test
        @DisplayName("template has GitHub Actions snippet")
        void assemble_skillTemplate_hasGitHubActions(
                @TempDir Path tempDir)
                throws IOException {
            generateSecurityOutput(tempDir);
            String content = readTemplateContent(tempDir);
            assertThat(content)
                    .contains("### GitHub Actions");
        }

        @Test
        @DisplayName("GitHub Actions snippet references"
                + " SARIF upload")
        void assemble_skillTemplate_ghHasSarifUpload(
                @TempDir Path tempDir)
                throws IOException {
            generateSecurityOutput(tempDir);
            String content = readTemplateContent(tempDir);
            assertThat(content)
                    .contains("upload-sarif");
        }

        @Test
        @DisplayName("template has GitLab CI snippet")
        void assemble_skillTemplate_hasGitLabCi(
                @TempDir Path tempDir)
                throws IOException {
            generateSecurityOutput(tempDir);
            String content = readTemplateContent(tempDir);
            assertThat(content)
                    .contains("### GitLab CI");
        }

        @Test
        @DisplayName("GitLab CI snippet references"
                + " artifacts section")
        void assemble_skillTemplate_gitlabHasArtifacts(
                @TempDir Path tempDir)
                throws IOException {
            generateSecurityOutput(tempDir);
            String content = readTemplateContent(tempDir);
            assertThat(content)
                    .contains("artifacts:")
                    .contains("expire_in:");
        }

        @Test
        @DisplayName("template has Azure DevOps snippet")
        void assemble_skillTemplate_hasAzureDevOps(
                @TempDir Path tempDir)
                throws IOException {
            generateSecurityOutput(tempDir);
            String content = readTemplateContent(tempDir);
            assertThat(content)
                    .contains("### Azure DevOps");
        }

        @Test
        @DisplayName("Azure DevOps snippet references"
                + " PublishBuildArtifacts task")
        void assemble_skillTemplate_azureHasPublishTask(
                @TempDir Path tempDir)
                throws IOException {
            generateSecurityOutput(tempDir);
            String content = readTemplateContent(tempDir);
            assertThat(content)
                    .contains("PublishBuildArtifacts");
        }

        @Test
        @DisplayName("all CI snippets use results/security/"
                + " output path")
        void assemble_skillTemplate_ciUsesResultsDir(
                @TempDir Path tempDir)
                throws IOException {
            generateSecurityOutput(tempDir);
            String content = readTemplateContent(tempDir);
            assertThat(content)
                    .contains("results/security/");
        }
    }

    @Nested
    @DisplayName("Error Handling Conventions")
    class ErrorHandlingConventions {

        @Test
        @DisplayName("error handling covers tool-not-found"
                + " scenario")
        void assemble_skillTemplate_hasToolNotFound(
                @TempDir Path tempDir)
                throws IOException {
            generateSecurityOutput(tempDir);
            String content = readTemplateContent(tempDir);
            assertThat(content)
                    .contains("### Tool Not Found");
        }

        @Test
        @DisplayName("tool-not-found generates INFO level"
                + " finding")
        void assemble_skillTemplate_toolNotFoundInfoLevel(
                @TempDir Path tempDir)
                throws IOException {
            generateSecurityOutput(tempDir);
            String content = readTemplateContent(tempDir);
            assertThat(content)
                    .contains("`none` (INFO)");
        }

        @Test
        @DisplayName("error handling covers scan timeout"
                + " scenario")
        void assemble_skillTemplate_hasScanTimeout(
                @TempDir Path tempDir)
                throws IOException {
            generateSecurityOutput(tempDir);
            String content = readTemplateContent(tempDir);
            assertThat(content)
                    .contains("### Scan Timeout");
        }

        @Test
        @DisplayName("error handling covers tool crash"
                + " scenario")
        void assemble_skillTemplate_hasToolCrash(
                @TempDir Path tempDir)
                throws IOException {
            generateSecurityOutput(tempDir);
            String content = readTemplateContent(tempDir);
            assertThat(content)
                    .contains("### Tool Crash");
        }

        @Test
        @DisplayName("error handling covers zero findings"
                + " scenario")
        void assemble_skillTemplate_hasZeroFindings(
                @TempDir Path tempDir)
                throws IOException {
            generateSecurityOutput(tempDir);
            String content = readTemplateContent(tempDir);
            assertThat(content)
                    .contains("### Zero Findings");
        }

        @Test
        @DisplayName("zero findings sets score to 100")
        void assemble_skillTemplate_zeroFindingsScore100(
                @TempDir Path tempDir)
                throws IOException {
            generateSecurityOutput(tempDir);
            String content = readTemplateContent(tempDir);
            assertThat(content)
                    .contains("Set score to 100");
        }
    }

    @Nested
    @DisplayName("SARIF and Scoring Integration")
    class SarifAndScoring {

        @Test
        @DisplayName("template references SARIF 2.1.0"
                + " format")
        void assemble_skillTemplate_referencesSarif(
                @TempDir Path tempDir)
                throws IOException {
            generateSecurityOutput(tempDir);
            String content = readTemplateContent(tempDir);
            assertThat(content)
                    .contains("SARIF 2.1.0");
        }

        @Test
        @DisplayName("template references sarif-template.md")
        void assemble_skillTemplate_refsSarifTemplate(
                @TempDir Path tempDir)
                throws IOException {
            generateSecurityOutput(tempDir);
            String content = readTemplateContent(tempDir);
            assertThat(content)
                    .contains("sarif-template.md");
        }

        @Test
        @DisplayName("template references"
                + " security-scoring.md")
        void assemble_skillTemplate_refsScoring(
                @TempDir Path tempDir)
                throws IOException {
            generateSecurityOutput(tempDir);
            String content = readTemplateContent(tempDir);
            assertThat(content)
                    .contains("security-scoring.md");
        }

        @Test
        @DisplayName("severity mapping includes CRITICAL"
                + " through INFO")
        void assemble_skillTemplate_hasSeverityMapping(
                @TempDir Path tempDir)
                throws IOException {
            generateSecurityOutput(tempDir);
            String content = readTemplateContent(tempDir);
            assertThat(content)
                    .contains("CRITICAL")
                    .contains("HIGH")
                    .contains("MEDIUM")
                    .contains("LOW")
                    .contains("INFO");
        }

        @Test
        @DisplayName("template includes grade thresholds"
                + " A through F")
        void assemble_skillTemplate_hasGradeThresholds(
                @TempDir Path tempDir)
                throws IOException {
            generateSecurityOutput(tempDir);
            String content = readTemplateContent(tempDir);
            assertThat(content)
                    .contains("| A |")
                    .contains("| B |")
                    .contains("| C |")
                    .contains("| D |")
                    .contains("| F |");
        }
    }

    @Nested
    @DisplayName("Idempotency Rules")
    class IdempotencyRules {

        @Test
        @DisplayName("template specifies dated filename"
                + " convention")
        void assemble_skillTemplate_hasDatedFilename(
                @TempDir Path tempDir)
                throws IOException {
            generateSecurityOutput(tempDir);
            String content = readTemplateContent(tempDir);
            assertThat(content)
                    .contains("YYYYMMDD")
                    .contains("HHMMSS");
        }

        @Test
        @DisplayName("template specifies results/security/"
                + " output directory")
        void assemble_skillTemplate_hasOutputDirConvention(
                @TempDir Path tempDir)
                throws IOException {
            generateSecurityOutput(tempDir);
            String content = readTemplateContent(tempDir);
            assertThat(content).contains(
                    "results/security/");
        }

        @Test
        @DisplayName("template requires no overwrite of"
                + " previous results")
        void assemble_skillTemplate_noOverwriteRule(
                @TempDir Path tempDir)
                throws IOException {
            generateSecurityOutput(tempDir);
            String content = readTemplateContent(tempDir);
            assertThat(content).contains(
                    "never overwrite previous results");
        }
    }

    @Nested
    @DisplayName("SKILL.md Reference Integration")
    class SkillMdReference {

        @Test
        @DisplayName("SKILL.md references"
                + " security-skill-template.md")
        void assemble_securityKp_refsSkillTemplate(
                @TempDir Path tempDir)
                throws IOException {
            String content = generateSecurityKpContent(
                    tempDir);
            assertThat(content).contains(
                    "security-skill-template.md");
        }

        @Test
        @DisplayName("SKILL.md preserves existing"
                + " reference entries")
        void assemble_securityKp_preservesExistingRefs(
                @TempDir Path tempDir)
                throws IOException {
            String content = generateSecurityKpContent(
                    tempDir);
            assertThat(content)
                    .contains("security-principles.md")
                    .contains("application-security.md")
                    .contains("cryptography.md")
                    .contains("sbom-generation-guide.md")
                    .contains(
                            "supply-chain-hardening.md");
        }
    }

    @Nested
    @DisplayName("Example Skill Validation")
    class ExampleSkillValidation {

        @Test
        @DisplayName("template includes a complete SAST"
                + " example skill")
        void assemble_skillTemplate_hasSastExample(
                @TempDir Path tempDir)
                throws IOException {
            generateSecurityOutput(tempDir);
            String content = readTemplateContent(tempDir);
            assertThat(content)
                    .contains("x-security-sast")
                    .contains("SpotBugs")
                    .contains("Bandit")
                    .contains("gosec");
        }
    }

    @Nested
    @DisplayName("Template Compliance Checklist")
    class TemplateComplianceChecklist {

        @Test
        @DisplayName("template includes compliance"
                + " checklist")
        void assemble_skillTemplate_hasChecklist(
                @TempDir Path tempDir)
                throws IOException {
            generateSecurityOutput(tempDir);
            String content = readTemplateContent(tempDir);
            assertThat(content).contains(
                    "## Template Compliance Checklist");
        }
    }

    private String readTemplateContent(Path tempDir)
            throws IOException {
        return Files.readString(
                tempDir.resolve(
                        "output/skills/security/"
                                + "references/"
                                + "security-skill-template"
                                + ".md"),
                StandardCharsets.UTF_8);
    }

    private String generateSecurityKpContent(Path tempDir)
            throws IOException {
        Path outputDir = tempDir.resolve("output");
        Files.createDirectories(outputDir);
        SkillsAssembler assembler =
                new SkillsAssembler();
        assembler.assemble(
                TestConfigBuilder.minimal(),
                new TemplateEngine(), outputDir);
        return Files.readString(
                outputDir.resolve(
                        "skills/security/SKILL.md"),
                StandardCharsets.UTF_8);
    }

    private void generateSecurityOutput(Path tempDir)
            throws IOException {
        Path outputDir = tempDir.resolve("output");
        Files.createDirectories(outputDir);
        SkillsAssembler assembler =
                new SkillsAssembler();
        assembler.assemble(
                TestConfigBuilder.minimal(),
                new TemplateEngine(), outputDir);
    }
}
