package dev.iadev.application.assembler;

import dev.iadev.domain.model.ProjectConfig;
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
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for story-0022-0006: Secret Scanner (x-security-secrets).
 *
 * <p>Validates the conditional SKILL.md template is generated
 * when {@code security.scanning.secretScan = true}, contains
 * all required sections per security-skill-template, and
 * covers all 8 secret categories.</p>
 */
@DisplayName("Secret Scanner (x-security-secrets)")
class SecretScanSkillTest {

    @Nested
    @DisplayName("Conditional Generation")
    class ConditionalGeneration {

        @Test
        @DisplayName("secretScan enabled generates"
                + " x-security-secrets SKILL.md")
        void assemble_secretScanEnabled_generatesSkill(
                @TempDir Path tempDir)
                throws IOException {
            Path outputDir = tempDir.resolve("output");
            Files.createDirectories(outputDir);
            ProjectConfig config =
                    TestConfigBuilder.builder()
                            .scanningFlags(
                                    false, false,
                                    true, false, false)
                            .build();
            new SkillsAssembler().assemble(
                    config,
                    new TemplateEngine(), outputDir);

            Path skill = outputDir.resolve(
                    "skills/x-security-secrets/SKILL.md");
            assertThat(skill).exists();
        }

        @Test
        @DisplayName("secretScan disabled does not generate"
                + " x-security-secrets SKILL.md")
        void assemble_secretScanDisabled_noSkill(
                @TempDir Path tempDir)
                throws IOException {
            Path outputDir = tempDir.resolve("output");
            Files.createDirectories(outputDir);
            ProjectConfig config =
                    TestConfigBuilder.builder()
                            .scanningFlags(
                                    false, false,
                                    false, false, false)
                            .build();
            new SkillsAssembler().assemble(
                    config,
                    new TemplateEngine(), outputDir);

            Path skill = outputDir.resolve(
                    "skills/x-security-secrets/SKILL.md");
            assertThat(skill).doesNotExist();
        }

        @Test
        @DisplayName("selectConditionalSkills includes"
                + " x-security-secrets when flag enabled")
        void select_secretScanEnabled_includesSkill() {
            ProjectConfig config =
                    TestConfigBuilder.builder()
                            .scanningFlags(
                                    false, false,
                                    true, false, false)
                            .build();

            List<String> skills =
                    SkillsSelection
                            .selectConditionalSkills(config);

            assertThat(skills)
                    .contains("x-security-secrets");
        }

        @Test
        @DisplayName("selectConditionalSkills excludes"
                + " x-security-secrets when flag disabled")
        void select_secretScanDisabled_excludesSkill() {
            ProjectConfig config =
                    TestConfigBuilder.builder()
                            .scanningFlags(
                                    false, false,
                                    false, false, false)
                            .build();

            List<String> skills =
                    SkillsSelection
                            .selectConditionalSkills(config);

            assertThat(skills)
                    .doesNotContain("x-security-secrets");
        }
    }

    @Nested
    @DisplayName("Frontmatter")
    class Frontmatter {

        @Test
        @DisplayName("contains name x-security-secrets")
        void content_hasName(
                @TempDir Path tempDir)
                throws IOException {
            String content = generateAndRead(tempDir);
            assertThat(content)
                    .contains("name: x-security-secrets");
        }

        @Test
        @DisplayName("contains description")
        void content_hasDescription(
                @TempDir Path tempDir)
                throws IOException {
            String content = generateAndRead(tempDir);
            assertThat(content)
                    .contains("description:");
        }

        @Test
        @DisplayName("contains argument-hint")
        void content_hasArgumentHint(
                @TempDir Path tempDir)
                throws IOException {
            String content = generateAndRead(tempDir);
            assertThat(content)
                    .contains("argument-hint:");
        }

        @Test
        @DisplayName("contains allowed-tools")
        void content_hasAllowedTools(
                @TempDir Path tempDir)
                throws IOException {
            String content = generateAndRead(tempDir);
            assertThat(content)
                    .contains("allowed-tools:");
        }
    }

    @Nested
    @DisplayName("Required Sections")
    class RequiredSections {

        @Test
        @DisplayName("contains Purpose section")
        void content_hasPurpose(
                @TempDir Path tempDir)
                throws IOException {
            String content = generateAndRead(tempDir);
            assertThat(content)
                    .contains("## Purpose");
        }

        @Test
        @DisplayName("contains Tool Selection section")
        void content_hasToolSelection(
                @TempDir Path tempDir)
                throws IOException {
            String content = generateAndRead(tempDir);
            assertThat(content)
                    .contains("## Tool Selection");
        }

        @Test
        @DisplayName("contains Parameters section")
        void content_hasParameters(
                @TempDir Path tempDir)
                throws IOException {
            String content = generateAndRead(tempDir);
            assertThat(content)
                    .contains("## Parameters");
        }

        @Test
        @DisplayName("contains Output Format section")
        void content_hasOutputFormat(
                @TempDir Path tempDir)
                throws IOException {
            String content = generateAndRead(tempDir);
            assertThat(content)
                    .contains("Generate Reports");
        }

        @Test
        @DisplayName("contains Error Handling section")
        void content_hasErrorHandling(
                @TempDir Path tempDir)
                throws IOException {
            String content = generateAndRead(tempDir);
            assertThat(content)
                    .contains("## Error Handling");
        }

        @Test
        @DisplayName("contains CI Integration section")
        void content_hasCiIntegration(
                @TempDir Path tempDir)
                throws IOException {
            String content = generateAndRead(tempDir);
            assertThat(content)
                    .contains("## CI Integration");
        }

        @Test
        @DisplayName("contains Idempotency section")
        void content_hasIdempotency(
                @TempDir Path tempDir)
                throws IOException {
            String content = generateAndRead(tempDir);
            assertThat(content)
                    .contains("## Idempotency");
        }
    }

    @Nested
    @DisplayName("Tool Selection")
    class ToolSelectionContent {

        @Test
        @DisplayName("gitleaks is preferred tool")
        void content_hasGitleaksPreferred(
                @TempDir Path tempDir)
                throws IOException {
            String content = generateAndRead(tempDir);
            assertThat(content)
                    .contains("gitleaks");
        }

        @Test
        @DisplayName("trufflehog is fallback tool")
        void content_hasTrufflehogFallback(
                @TempDir Path tempDir)
                throws IOException {
            String content = generateAndRead(tempDir);
            assertThat(content)
                    .contains("trufflehog");
        }

        @Test
        @DisplayName("detect-secrets listed for Python")
        void content_hasDetectSecrets(
                @TempDir Path tempDir)
                throws IOException {
            String content = generateAndRead(tempDir);
            assertThat(content)
                    .contains("detect-secrets");
        }
    }

    @Nested
    @DisplayName("Secret Categories (8 required)")
    class SecretCategories {

        @Test
        @DisplayName("SECRET-001: AWS Credentials")
        void content_hasAwsCategory(
                @TempDir Path tempDir)
                throws IOException {
            String content = generateAndRead(tempDir);
            assertThat(content)
                    .contains("SECRET-001")
                    .contains("AWS");
        }

        @Test
        @DisplayName("SECRET-002: GCP Credentials")
        void content_hasGcpCategory(
                @TempDir Path tempDir)
                throws IOException {
            String content = generateAndRead(tempDir);
            assertThat(content)
                    .contains("SECRET-002")
                    .contains("GCP");
        }

        @Test
        @DisplayName("SECRET-003: Azure Credentials")
        void content_hasAzureCategory(
                @TempDir Path tempDir)
                throws IOException {
            String content = generateAndRead(tempDir);
            assertThat(content)
                    .contains("SECRET-003")
                    .contains("Azure");
        }

        @Test
        @DisplayName("SECRET-004: API Tokens")
        void content_hasApiTokenCategory(
                @TempDir Path tempDir)
                throws IOException {
            String content = generateAndRead(tempDir);
            assertThat(content)
                    .contains("SECRET-004")
                    .contains("API Token");
        }

        @Test
        @DisplayName("SECRET-005: Private Keys")
        void content_hasPrivateKeyCategory(
                @TempDir Path tempDir)
                throws IOException {
            String content = generateAndRead(tempDir);
            assertThat(content)
                    .contains("SECRET-005")
                    .contains("Private Key");
        }

        @Test
        @DisplayName("SECRET-006: Hardcoded Passwords")
        void content_hasPasswordCategory(
                @TempDir Path tempDir)
                throws IOException {
            String content = generateAndRead(tempDir);
            assertThat(content)
                    .contains("SECRET-006")
                    .contains("Password");
        }

        @Test
        @DisplayName("SECRET-007: JWT Tokens")
        void content_hasJwtCategory(
                @TempDir Path tempDir)
                throws IOException {
            String content = generateAndRead(tempDir);
            assertThat(content)
                    .contains("SECRET-007")
                    .contains("JWT");
        }

        @Test
        @DisplayName("SECRET-008: Database Connection"
                + " Strings")
        void content_hasDatabaseCategory(
                @TempDir Path tempDir)
                throws IOException {
            String content = generateAndRead(tempDir);
            assertThat(content)
                    .contains("SECRET-008")
                    .contains("Database");
        }
    }

    @Nested
    @DisplayName("CLI Parameters")
    class CliParameters {

        @Test
        @DisplayName("--scope parameter documented")
        void content_hasScopeParam(
                @TempDir Path tempDir)
                throws IOException {
            String content = generateAndRead(tempDir);
            assertThat(content).contains("--scope");
        }

        @Test
        @DisplayName("--baseline parameter documented")
        void content_hasBaselineParam(
                @TempDir Path tempDir)
                throws IOException {
            String content = generateAndRead(tempDir);
            assertThat(content).contains("--baseline");
        }

        @Test
        @DisplayName("--since-commit parameter documented")
        void content_hasSinceCommitParam(
                @TempDir Path tempDir)
                throws IOException {
            String content = generateAndRead(tempDir);
            assertThat(content).contains("--since-commit");
        }

        @Test
        @DisplayName("--format parameter documented")
        void content_hasFormatParam(
                @TempDir Path tempDir)
                throws IOException {
            String content = generateAndRead(tempDir);
            assertThat(content).contains("--format");
        }
    }

    @Nested
    @DisplayName("Baseline System")
    class BaselineSystem {

        @Test
        @DisplayName("documents .security-baseline.json")
        void content_hasBaselineFile(
                @TempDir Path tempDir)
                throws IOException {
            String content = generateAndRead(tempDir);
            assertThat(content)
                    .contains(".security-baseline.json");
        }

        @Test
        @DisplayName("documents fingerprint field")
        void content_hasFingerprint(
                @TempDir Path tempDir)
                throws IOException {
            String content = generateAndRead(tempDir);
            assertThat(content)
                    .contains("fingerprint");
        }

        @Test
        @DisplayName("documents reason field")
        void content_hasReason(
                @TempDir Path tempDir)
                throws IOException {
            String content = generateAndRead(tempDir);
            assertThat(content)
                    .contains("reason");
        }

        @Test
        @DisplayName("documents approvedBy field")
        void content_hasApprovedBy(
                @TempDir Path tempDir)
                throws IOException {
            String content = generateAndRead(tempDir);
            assertThat(content)
                    .contains("approvedBy");
        }

        @Test
        @DisplayName("documents approvedDate field")
        void content_hasApprovedDate(
                @TempDir Path tempDir)
                throws IOException {
            String content = generateAndRead(tempDir);
            assertThat(content)
                    .contains("approvedDate");
        }
    }

    @Nested
    @DisplayName("SARIF Output")
    class SarifOutput {

        @Test
        @DisplayName("references SARIF 2.1.0")
        void content_hasSarifVersion(
                @TempDir Path tempDir)
                throws IOException {
            String content = generateAndRead(tempDir);
            assertThat(content).contains("2.1.0");
        }

        @Test
        @DisplayName("contains SARIF output example")
        void content_hasSarifExample(
                @TempDir Path tempDir)
                throws IOException {
            String content = generateAndRead(tempDir);
            assertThat(content)
                    .contains(".sarif.json");
        }

        @Test
        @DisplayName("contains scoring formula")
        void content_hasScoringFormula(
                @TempDir Path tempDir)
                throws IOException {
            String content = generateAndRead(tempDir);
            assertThat(content)
                    .contains("Score and Grade");
        }

        @Test
        @DisplayName("contains grade thresholds")
        void content_hasGradeThresholds(
                @TempDir Path tempDir)
                throws IOException {
            String content = generateAndRead(tempDir);
            assertThat(content)
                    .contains("| A |")
                    .contains("| B |")
                    .contains("| C |")
                    .contains("| D |")
                    .contains("| F |");
        }
    }

    @Nested
    @DisplayName("Redaction")
    class Redaction {

        @Test
        @DisplayName("documents redactedMatch field")
        void content_hasRedactedMatch(
                @TempDir Path tempDir)
                throws IOException {
            String content = generateAndRead(tempDir);
            assertThat(content)
                    .contains("redactedMatch");
        }

        @Test
        @DisplayName("shows redacted examples")
        void content_hasRedactedExamples(
                @TempDir Path tempDir)
                throws IOException {
            String content = generateAndRead(tempDir);
            assertThat(content)
                    .contains("****");
        }
    }

    @Nested
    @DisplayName("CI Integration Snippets")
    class CiIntegration {

        @Test
        @DisplayName("contains GitHub Actions snippet")
        void content_hasGitHubActions(
                @TempDir Path tempDir)
                throws IOException {
            String content = generateAndRead(tempDir);
            assertThat(content)
                    .contains("### GitHub Actions");
        }

        @Test
        @DisplayName("contains GitLab CI reference"
                + " in secret categories")
        void content_hasGitLabCi(
                @TempDir Path tempDir)
                throws IOException {
            String content = generateAndRead(tempDir);
            assertThat(content)
                    .contains("## CI Integration");
        }

        @Test
        @DisplayName("contains GitHub Actions CI snippet")
        void content_hasAzureDevOps(
                @TempDir Path tempDir)
                throws IOException {
            String content = generateAndRead(tempDir);
            assertThat(content)
                    .contains("### GitHub Actions");
        }
    }

    @Nested
    @DisplayName("Error Handling Conventions")
    class ErrorHandlingConventions {

        @Test
        @DisplayName("documents Tool Not Found handling")
        void content_hasToolNotFound(
                @TempDir Path tempDir)
                throws IOException {
            String content = generateAndRead(tempDir);
            assertThat(content)
                    .contains("Tool not found");
        }

        @Test
        @DisplayName("documents Scan Timeout handling")
        void content_hasScanTimeout(
                @TempDir Path tempDir)
                throws IOException {
            String content = generateAndRead(tempDir);
            assertThat(content)
                    .contains("Scan timeout");
        }

        @Test
        @DisplayName("documents Tool Crash handling")
        void content_hasToolCrash(
                @TempDir Path tempDir)
                throws IOException {
            String content = generateAndRead(tempDir);
            assertThat(content)
                    .contains("Tool crash");
        }

        @Test
        @DisplayName("documents Zero Findings handling")
        void content_hasZeroFindings(
                @TempDir Path tempDir)
                throws IOException {
            String content = generateAndRead(tempDir);
            assertThat(content)
                    .contains("Zero findings");
        }
    }

    @Nested
    @DisplayName("Knowledge Pack References")
    class KnowledgePackReferences {

        @Test
        @DisplayName("references sarif-template.md")
        void content_refsSarifTemplate(
                @TempDir Path tempDir)
                throws IOException {
            String content = generateAndRead(tempDir);
            assertThat(content)
                    .contains("sarif-template.md");
        }

        @Test
        @DisplayName("references security-scoring.md")
        void content_refsScoringModel(
                @TempDir Path tempDir)
                throws IOException {
            String content = generateAndRead(tempDir);
            assertThat(content)
                    .contains("security-scoring.md");
        }

        @Test
        @DisplayName("references security-skill-template.md")
        void content_refsSkillTemplate(
                @TempDir Path tempDir)
                throws IOException {
            String content = generateAndRead(tempDir);
            assertThat(content)
                    .contains("security-skill-template.md");
        }
    }

    private String generateAndRead(Path tempDir)
            throws IOException {
        Path outputDir = tempDir.resolve("output");
        Files.createDirectories(outputDir);
        ProjectConfig config =
                TestConfigBuilder.builder()
                        .scanningFlags(
                                false, false,
                                true, false, false)
                        .build();
        new SkillsAssembler().assemble(
                config,
                new TemplateEngine(), outputDir);
        return Files.readString(
                outputDir.resolve(
                        "skills/x-security-secrets/SKILL.md"),
                StandardCharsets.UTF_8);
    }
}
