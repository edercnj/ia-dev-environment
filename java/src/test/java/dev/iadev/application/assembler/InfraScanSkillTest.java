package dev.iadev.application.assembler;

import dev.iadev.testutil.TestConfigBuilder;

import dev.iadev.domain.model.ProjectConfig;
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
 * Tests for story-0022-0008: Infrastructure Security
 * Scanner (x-infra-scan).
 *
 * <p>Validates that the x-infra-scan SKILL.md is generated
 * when infraScan is enabled, follows the security skill
 * template, and contains all required sections.</p>
 */
@DisplayName("x-infra-scan Skill")
class InfraScanSkillTest {

    @Nested
    @DisplayName("Conditional Generation")
    class ConditionalGeneration {

        @Test
        @DisplayName("infraScan enabled generates"
                + " x-infra-scan SKILL.md")
        void assemble_infraScanEnabled_generatesSkill(
                @TempDir Path tempDir)
                throws IOException {
            Path outputDir = assembleWithInfraScan(
                    tempDir, true);

            assertThat(outputDir.resolve(
                    "skills/x-infra-scan/SKILL.md"))
                    .exists();
        }

        @Test
        @DisplayName("infraScan disabled excludes"
                + " x-infra-scan")
        void assemble_infraScanDisabled_excludesSkill(
                @TempDir Path tempDir)
                throws IOException {
            Path outputDir = assembleWithInfraScan(
                    tempDir, false);

            assertThat(outputDir.resolve(
                    "skills/x-infra-scan"))
                    .doesNotExist();
        }

        @Test
        @DisplayName("infraScan SKILL.md is non-empty")
        void assemble_infraScanEnabled_nonEmpty(
                @TempDir Path tempDir)
                throws IOException {
            Path outputDir = assembleWithInfraScan(
                    tempDir, true);
            String content = readSkillContent(outputDir);

            assertThat(content).isNotEmpty();
        }
    }

    @Nested
    @DisplayName("Frontmatter")
    class Frontmatter {

        @Test
        @DisplayName("contains name: x-infra-scan")
        void assemble_skillMd_hasName(
                @TempDir Path tempDir)
                throws IOException {
            Path outputDir = assembleWithInfraScan(
                    tempDir, true);
            String content = readSkillContent(outputDir);

            assertThat(content)
                    .contains("name: x-infra-scan");
        }

        @Test
        @DisplayName("contains description with IaC"
                + " scanning")
        void assemble_skillMd_hasDescription(
                @TempDir Path tempDir)
                throws IOException {
            Path outputDir = assembleWithInfraScan(
                    tempDir, true);
            String content = readSkillContent(outputDir);

            assertThat(content)
                    .contains("Infrastructure Security"
                            + " Scanner");
        }

        @Test
        @DisplayName("contains argument-hint with scope")
        void assemble_skillMd_hasArgumentHint(
                @TempDir Path tempDir)
                throws IOException {
            Path outputDir = assembleWithInfraScan(
                    tempDir, true);
            String content = readSkillContent(outputDir);

            assertThat(content)
                    .contains("argument-hint:");
        }

        @Test
        @DisplayName("contains allowed-tools")
        void assemble_skillMd_hasAllowedTools(
                @TempDir Path tempDir)
                throws IOException {
            Path outputDir = assembleWithInfraScan(
                    tempDir, true);
            String content = readSkillContent(outputDir);

            assertThat(content)
                    .contains("allowed-tools:");
        }
    }

    @Nested
    @DisplayName("Tool Selection Table")
    class ToolSelectionTable {

        @Test
        @DisplayName("contains Kubernetes tool selection")
        void assemble_skillMd_hasK8sToolSelection(
                @TempDir Path tempDir)
                throws IOException {
            Path outputDir = assembleWithInfraScan(
                    tempDir, true);
            String content = readSkillContent(outputDir);

            assertThat(content)
                    .contains("Kubernetes")
                    .contains("kube-bench")
                    .contains("checkov");
        }

        @Test
        @DisplayName("contains Terraform tool selection")
        void assemble_skillMd_hasTerraformToolSelection(
                @TempDir Path tempDir)
                throws IOException {
            Path outputDir = assembleWithInfraScan(
                    tempDir, true);
            String content = readSkillContent(outputDir);

            assertThat(content)
                    .contains("Terraform")
                    .contains("tfsec");
        }

        @Test
        @DisplayName("contains Helm tool selection")
        void assemble_skillMd_hasHelmToolSelection(
                @TempDir Path tempDir)
                throws IOException {
            Path outputDir = assembleWithInfraScan(
                    tempDir, true);
            String content = readSkillContent(outputDir);

            assertThat(content)
                    .contains("Helm")
                    .contains("kubescape");
        }

        @Test
        @DisplayName("contains Docker Compose tool"
                + " selection")
        void assemble_skillMd_hasComposeToolSelection(
                @TempDir Path tempDir)
                throws IOException {
            Path outputDir = assembleWithInfraScan(
                    tempDir, true);
            String content = readSkillContent(outputDir);

            assertThat(content)
                    .contains("Docker Compose");
        }

        @Test
        @DisplayName("all 4 IaC types have install"
                + " command")
        void assemble_skillMd_hasInstallCommands(
                @TempDir Path tempDir)
                throws IOException {
            Path outputDir = assembleWithInfraScan(
                    tempDir, true);
            String content = readSkillContent(outputDir);

            assertThat(content)
                    .contains("pip install checkov");
        }
    }

    @Nested
    @DisplayName("Auto-Detection Rules")
    class AutoDetectionRules {

        @Test
        @DisplayName("contains Kubernetes detection"
                + " criteria")
        void assemble_skillMd_hasK8sDetection(
                @TempDir Path tempDir)
                throws IOException {
            Path outputDir = assembleWithInfraScan(
                    tempDir, true);
            String content = readSkillContent(outputDir);

            assertThat(content)
                    .contains("apiVersion")
                    .contains("kind");
        }

        @Test
        @DisplayName("contains Terraform detection"
                + " criteria")
        void assemble_skillMd_hasTerraformDetection(
                @TempDir Path tempDir)
                throws IOException {
            Path outputDir = assembleWithInfraScan(
                    tempDir, true);
            String content = readSkillContent(outputDir);

            assertThat(content).contains(".tf");
        }

        @Test
        @DisplayName("contains Helm detection criteria")
        void assemble_skillMd_hasHelmDetection(
                @TempDir Path tempDir)
                throws IOException {
            Path outputDir = assembleWithInfraScan(
                    tempDir, true);
            String content = readSkillContent(outputDir);

            assertThat(content)
                    .contains("Chart.yaml");
        }

        @Test
        @DisplayName("contains Compose detection"
                + " criteria")
        void assemble_skillMd_hasComposeDetection(
                @TempDir Path tempDir)
                throws IOException {
            Path outputDir = assembleWithInfraScan(
                    tempDir, true);
            String content = readSkillContent(outputDir);

            assertThat(content)
                    .contains("docker-compose.yml")
                    .contains("compose.yml");
        }
    }

    @Nested
    @DisplayName("Kubernetes Checks")
    class KubernetesChecks {

        @Test
        @DisplayName("contains security context check")
        void assemble_skillMd_hasSecurityContextCheck(
                @TempDir Path tempDir)
                throws IOException {
            Path outputDir = assembleWithInfraScan(
                    tempDir, true);
            String content = readSkillContent(outputDir);

            assertThat(content)
                    .contains("missing-security-context")
                    .contains("CIS-5.2.6");
        }

        @Test
        @DisplayName("contains network policy check")
        void assemble_skillMd_hasNetworkPolicyCheck(
                @TempDir Path tempDir)
                throws IOException {
            Path outputDir = assembleWithInfraScan(
                    tempDir, true);
            String content = readSkillContent(outputDir);

            assertThat(content)
                    .contains("missing-network-policy");
        }

        @Test
        @DisplayName("contains RBAC wildcard check")
        void assemble_skillMd_hasRbacCheck(
                @TempDir Path tempDir)
                throws IOException {
            Path outputDir = assembleWithInfraScan(
                    tempDir, true);
            String content = readSkillContent(outputDir);

            assertThat(content)
                    .contains("rbac-wildcard")
                    .contains("CRITICAL");
        }

        @Test
        @DisplayName("contains plaintext secret check")
        void assemble_skillMd_hasPlaintextSecretCheck(
                @TempDir Path tempDir)
                throws IOException {
            Path outputDir = assembleWithInfraScan(
                    tempDir, true);
            String content = readSkillContent(outputDir);

            assertThat(content)
                    .contains("plaintext-secret");
        }

        @Test
        @DisplayName("contains resource limits check")
        void assemble_skillMd_hasResourceLimitsCheck(
                @TempDir Path tempDir)
                throws IOException {
            Path outputDir = assembleWithInfraScan(
                    tempDir, true);
            String content = readSkillContent(outputDir);

            assertThat(content)
                    .contains("missing-resource-limits");
        }

        @Test
        @DisplayName("contains Pod Security Standards"
                + " check")
        void assemble_skillMd_hasPssCheck(
                @TempDir Path tempDir)
                throws IOException {
            Path outputDir = assembleWithInfraScan(
                    tempDir, true);
            String content = readSkillContent(outputDir);

            assertThat(content)
                    .contains("pss-violation");
        }
    }

    @Nested
    @DisplayName("Terraform Checks")
    class TerraformChecks {

        @Test
        @DisplayName("contains open security group check")
        void assemble_skillMd_hasOpenSgCheck(
                @TempDir Path tempDir)
                throws IOException {
            Path outputDir = assembleWithInfraScan(
                    tempDir, true);
            String content = readSkillContent(outputDir);

            assertThat(content)
                    .contains("open-security-group")
                    .contains("0.0.0.0/0");
        }

        @Test
        @DisplayName("contains IAM wildcard policy check")
        void assemble_skillMd_hasIamWildcardCheck(
                @TempDir Path tempDir)
                throws IOException {
            Path outputDir = assembleWithInfraScan(
                    tempDir, true);
            String content = readSkillContent(outputDir);

            assertThat(content)
                    .contains("iam-wildcard-policy");
        }

        @Test
        @DisplayName("contains encryption check")
        void assemble_skillMd_hasEncryptionCheck(
                @TempDir Path tempDir)
                throws IOException {
            Path outputDir = assembleWithInfraScan(
                    tempDir, true);
            String content = readSkillContent(outputDir);

            assertThat(content)
                    .contains("encryption-disabled");
        }
    }

    @Nested
    @DisplayName("SARIF and Scoring")
    class SarifAndScoring {

        @Test
        @DisplayName("references SARIF 2.1.0 format")
        void assemble_skillMd_referencesSarif(
                @TempDir Path tempDir)
                throws IOException {
            Path outputDir = assembleWithInfraScan(
                    tempDir, true);
            String content = readSkillContent(outputDir);

            assertThat(content)
                    .contains("SARIF 2.1.0");
        }

        @Test
        @DisplayName("references sarif-template.md")
        void assemble_skillMd_refsSarifTemplate(
                @TempDir Path tempDir)
                throws IOException {
            Path outputDir = assembleWithInfraScan(
                    tempDir, true);
            String content = readSkillContent(outputDir);

            assertThat(content)
                    .contains("sarif-template.md");
        }

        @Test
        @DisplayName("references security-scoring.md")
        void assemble_skillMd_refsScoring(
                @TempDir Path tempDir)
                throws IOException {
            Path outputDir = assembleWithInfraScan(
                    tempDir, true);
            String content = readSkillContent(outputDir);

            assertThat(content)
                    .contains("security-scoring.md");
        }

        @Test
        @DisplayName("includes severity mapping with"
                + " score impacts")
        void assemble_skillMd_hasSeverityMapping(
                @TempDir Path tempDir)
                throws IOException {
            Path outputDir = assembleWithInfraScan(
                    tempDir, true);
            String content = readSkillContent(outputDir);

            assertThat(content)
                    .contains("CRITICAL")
                    .contains("HIGH")
                    .contains("MEDIUM")
                    .contains("LOW")
                    .contains("INFO");
        }

        @Test
        @DisplayName("includes grade thresholds A"
                + " through F")
        void assemble_skillMd_hasGradeThresholds(
                @TempDir Path tempDir)
                throws IOException {
            Path outputDir = assembleWithInfraScan(
                    tempDir, true);
            String content = readSkillContent(outputDir);

            assertThat(content)
                    .contains("| A |")
                    .contains("| B |")
                    .contains("| C |")
                    .contains("| D |")
                    .contains("| F |");
        }
    }

    @Nested
    @DisplayName("Error Handling")
    class ErrorHandling {

        @Test
        @DisplayName("covers tool-not-found scenario")
        void assemble_skillMd_hasToolNotFound(
                @TempDir Path tempDir)
                throws IOException {
            Path outputDir = assembleWithInfraScan(
                    tempDir, true);
            String content = readSkillContent(outputDir);

            assertThat(content)
                    .contains("### Tool Not Found");
        }

        @Test
        @DisplayName("covers scan timeout scenario")
        void assemble_skillMd_hasScanTimeout(
                @TempDir Path tempDir)
                throws IOException {
            Path outputDir = assembleWithInfraScan(
                    tempDir, true);
            String content = readSkillContent(outputDir);

            assertThat(content)
                    .contains("### Scan Timeout");
        }

        @Test
        @DisplayName("covers tool crash scenario")
        void assemble_skillMd_hasToolCrash(
                @TempDir Path tempDir)
                throws IOException {
            Path outputDir = assembleWithInfraScan(
                    tempDir, true);
            String content = readSkillContent(outputDir);

            assertThat(content)
                    .contains("### Tool Crash");
        }

        @Test
        @DisplayName("covers zero findings scenario")
        void assemble_skillMd_hasZeroFindings(
                @TempDir Path tempDir)
                throws IOException {
            Path outputDir = assembleWithInfraScan(
                    tempDir, true);
            String content = readSkillContent(outputDir);

            assertThat(content)
                    .contains("### Zero Findings");
        }

        @Test
        @DisplayName("covers no IaC files detected")
        void assemble_skillMd_hasNoIacFilesDetected(
                @TempDir Path tempDir)
                throws IOException {
            Path outputDir = assembleWithInfraScan(
                    tempDir, true);
            String content = readSkillContent(outputDir);

            assertThat(content)
                    .contains("No IaC Files Detected");
        }
    }

    @Nested
    @DisplayName("CI Integration")
    class CiIntegration {

        @Test
        @DisplayName("has GitHub Actions snippet")
        void assemble_skillMd_hasGitHubActions(
                @TempDir Path tempDir)
                throws IOException {
            Path outputDir = assembleWithInfraScan(
                    tempDir, true);
            String content = readSkillContent(outputDir);

            assertThat(content)
                    .contains("### GitHub Actions");
        }

        @Test
        @DisplayName("GitHub Actions references SARIF"
                + " upload")
        void assemble_skillMd_ghHasSarifUpload(
                @TempDir Path tempDir)
                throws IOException {
            Path outputDir = assembleWithInfraScan(
                    tempDir, true);
            String content = readSkillContent(outputDir);

            assertThat(content)
                    .contains("upload-sarif");
        }

        @Test
        @DisplayName("has GitLab CI snippet")
        void assemble_skillMd_hasGitLabCi(
                @TempDir Path tempDir)
                throws IOException {
            Path outputDir = assembleWithInfraScan(
                    tempDir, true);
            String content = readSkillContent(outputDir);

            assertThat(content)
                    .contains("### GitLab CI");
        }

        @Test
        @DisplayName("has Azure DevOps snippet")
        void assemble_skillMd_hasAzureDevOps(
                @TempDir Path tempDir)
                throws IOException {
            Path outputDir = assembleWithInfraScan(
                    tempDir, true);
            String content = readSkillContent(outputDir);

            assertThat(content)
                    .contains("### Azure DevOps");
        }

        @Test
        @DisplayName("CI uses results/security/ output"
                + " path")
        void assemble_skillMd_ciUsesResultsDir(
                @TempDir Path tempDir)
                throws IOException {
            Path outputDir = assembleWithInfraScan(
                    tempDir, true);
            String content = readSkillContent(outputDir);

            assertThat(content)
                    .contains("results/security/");
        }
    }

    @Nested
    @DisplayName("Idempotency")
    class Idempotency {

        @Test
        @DisplayName("specifies dated filename convention")
        void assemble_skillMd_hasDatedFilename(
                @TempDir Path tempDir)
                throws IOException {
            Path outputDir = assembleWithInfraScan(
                    tempDir, true);
            String content = readSkillContent(outputDir);

            assertThat(content)
                    .contains("YYYYMMDD")
                    .contains("HHMMSS");
        }

        @Test
        @DisplayName("specifies results/security/ output"
                + " directory")
        void assemble_skillMd_hasOutputDirConvention(
                @TempDir Path tempDir)
                throws IOException {
            Path outputDir = assembleWithInfraScan(
                    tempDir, true);
            String content = readSkillContent(outputDir);

            assertThat(content)
                    .contains("results/security/");
        }

        @Test
        @DisplayName("requires no overwrite of previous"
                + " results")
        void assemble_skillMd_noOverwriteRule(
                @TempDir Path tempDir)
                throws IOException {
            Path outputDir = assembleWithInfraScan(
                    tempDir, true);
            String content = readSkillContent(outputDir);

            assertThat(content).contains(
                    "never overwrite previous results");
        }
    }

    private Path assembleWithInfraScan(
            Path tempDir, boolean enabled)
            throws IOException {
        Path outputDir = tempDir.resolve("output");
        Files.createDirectories(outputDir);
        SkillsAssembler assembler =
                new SkillsAssembler();
        ProjectConfig config =
                TestConfigBuilder.builder()
                        .clearInterfaces()
                        .addInterface("cli")
                        .smokeTests(false)
                        .performanceTests(false)
                        .scanningFlags(
                                false, false,
                                false, false, enabled)
                        .build();
        assembler.assemble(
                config, new TemplateEngine(),
                outputDir);
        return outputDir;
    }

    private String readSkillContent(Path outputDir)
            throws IOException {
        return Files.readString(
                outputDir.resolve(
                        "skills/x-infra-scan/SKILL.md"),
                StandardCharsets.UTF_8);
    }
}
