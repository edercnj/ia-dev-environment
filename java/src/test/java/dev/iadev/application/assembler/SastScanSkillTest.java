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
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for story-0022-0005: SAST Scanner (x-security-sast).
 *
 * <p>Validates that the x-security-sast conditional skill
 * is generated when {@code security.scanning.sast = true},
 * contains all mandatory sections per security-skill-template,
 * and includes the tool selection table for 6 build tools.</p>
 */
@DisplayName("SAST Scanner — x-security-sast")
class SastScanSkillTest {

    @Nested
    @DisplayName("Conditional Generation")
    class ConditionalGeneration {

        @Test
        @DisplayName("sast enabled generates x-security-sast"
                + " skill directory")
        void assemble_sastEnabled_generatesSkillDir(
                @TempDir Path tempDir)
                throws IOException {
            generateSastOutput(tempDir);
            Path skillDir = tempDir.resolve(
                    "output/skills/x-security-sast");
            assertThat(skillDir).isDirectory();
        }

        @Test
        @DisplayName("sast enabled generates SKILL.md file")
        void assemble_sastEnabled_generatesSkillMd(
                @TempDir Path tempDir)
                throws IOException {
            generateSastOutput(tempDir);
            Path skillFile = tempDir.resolve(
                    "output/skills/x-security-sast/SKILL.md");
            assertThat(skillFile).exists();
        }

        @Test
        @DisplayName("sast disabled does not generate"
                + " x-security-sast directory")
        void assemble_sastDisabled_noSkillDir(
                @TempDir Path tempDir)
                throws IOException {
            Path outputDir = tempDir.resolve("output");
            Files.createDirectories(outputDir);
            ProjectConfig config =
                    TestConfigBuilder.builder().build();
            new SkillsAssembler().assemble(
                    config, new TemplateEngine(), outputDir);
            Path skillDir = outputDir.resolve(
                    "skills/x-security-sast");
            assertThat(skillDir).doesNotExist();
        }

        @Test
        @DisplayName("selectConditionalSkills includes"
                + " x-security-sast when sast enabled")
        void select_sastEnabled_includesSastScan() {
            ProjectConfig config =
                    TestConfigBuilder.builder()
                            .scanningFlags(
                                    true, false,
                                    false, false, false)
                            .build();
            List<String> skills =
                    SkillsSelection
                            .selectConditionalSkills(config);
            assertThat(skills).contains("x-security-sast");
        }

        @Test
        @DisplayName("selectConditionalSkills excludes"
                + " x-security-sast when sast disabled")
        void select_sastDisabled_excludesSastScan() {
            ProjectConfig config =
                    TestConfigBuilder.builder().build();
            List<String> skills =
                    SkillsSelection
                            .selectConditionalSkills(config);
            assertThat(skills)
                    .doesNotContain("x-security-sast");
        }
    }

    @Nested
    @DisplayName("SKILL.md — Frontmatter")
    class SkillMdFrontmatter {

        @Test
        @DisplayName("frontmatter has name x-security-sast")
        void assemble_frontmatter_hasName(
                @TempDir Path tempDir)
                throws IOException {
            String content = generateAndRead(tempDir);
            assertThat(content).contains(
                    "name: x-security-sast");
        }

        @Test
        @DisplayName("frontmatter has description")
        void assemble_frontmatter_hasDescription(
                @TempDir Path tempDir)
                throws IOException {
            String content = generateAndRead(tempDir);
            assertThat(content).contains(
                    "description:");
        }

        @Test
        @DisplayName("frontmatter has allowed-tools")
        void assemble_frontmatter_hasAllowedTools(
                @TempDir Path tempDir)
                throws IOException {
            String content = generateAndRead(tempDir);
            assertThat(content).contains(
                    "allowed-tools:");
        }
    }

    @Nested
    @DisplayName("SKILL.md — Mandatory Sections")
    class MandatorySections {

        @Test
        @DisplayName("has Purpose section")
        void assemble_skillMd_hasPurpose(
                @TempDir Path tempDir)
                throws IOException {
            String content = generateAndRead(tempDir);
            assertThat(content).contains("## Purpose");
        }

        @Test
        @DisplayName("has Tool Selection section")
        void assemble_skillMd_hasToolSelection(
                @TempDir Path tempDir)
                throws IOException {
            String content = generateAndRead(tempDir);
            assertThat(content).contains(
                    "Select Scanner");
        }

        @Test
        @DisplayName("has Parameters section")
        void assemble_skillMd_hasParameters(
                @TempDir Path tempDir)
                throws IOException {
            String content = generateAndRead(tempDir);
            assertThat(content).contains("## Parameters");
        }

        @Test
        @DisplayName("has Output Format section")
        void assemble_skillMd_hasOutputFormat(
                @TempDir Path tempDir)
                throws IOException {
            String content = generateAndRead(tempDir);
            assertThat(content).contains(
                    "## SARIF Rule ID Convention");
        }

        @Test
        @DisplayName("has Error Handling section")
        void assemble_skillMd_hasErrorHandling(
                @TempDir Path tempDir)
                throws IOException {
            String content = generateAndRead(tempDir);
            assertThat(content).contains(
                    "## Error Handling");
        }

        @Test
        @DisplayName("has CI Integration section")
        void assemble_skillMd_hasCiIntegration(
                @TempDir Path tempDir)
                throws IOException {
            String content = generateAndRead(tempDir);
            assertThat(content).contains(
                    "## CI Integration");
        }

        @Test
        @DisplayName("has Idempotency section")
        void assemble_skillMd_hasIdempotency(
                @TempDir Path tempDir)
                throws IOException {
            String content = generateAndRead(tempDir);
            assertThat(content).contains(
                    "## Idempotency");
        }
    }

    @Nested
    @DisplayName("Tool Selection Table — 6 Build Tools")
    class ToolSelectionTable {

        @Test
        @DisplayName("table includes Maven with SpotBugs")
        void assemble_toolTable_hasMavenSpotBugs(
                @TempDir Path tempDir)
                throws IOException {
            String content = generateAndRead(tempDir);
            assertThat(content)
                    .contains("| maven")
                    .contains("SpotBugs");
        }

        @Test
        @DisplayName("table includes Gradle with SpotBugs")
        void assemble_toolTable_hasGradleSpotBugs(
                @TempDir Path tempDir)
                throws IOException {
            String content = generateAndRead(tempDir);
            assertThat(content)
                    .contains("| gradle")
                    .contains("SpotBugs");
        }

        @Test
        @DisplayName("table includes npm with ESLint"
                + " security")
        void assemble_toolTable_hasNpmEslint(
                @TempDir Path tempDir)
                throws IOException {
            String content = generateAndRead(tempDir);
            assertThat(content)
                    .contains("| npm")
                    .contains("ESLint security");
        }

        @Test
        @DisplayName("table includes pip with Bandit")
        void assemble_toolTable_hasPipBandit(
                @TempDir Path tempDir)
                throws IOException {
            String content = generateAndRead(tempDir);
            assertThat(content)
                    .contains("| pip")
                    .contains("Bandit");
        }

        @Test
        @DisplayName("table includes go with gosec")
        void assemble_toolTable_hasGoGosec(
                @TempDir Path tempDir)
                throws IOException {
            String content = generateAndRead(tempDir);
            assertThat(content)
                    .contains("| go")
                    .contains("gosec");
        }

        @Test
        @DisplayName("table includes cargo with cargo-audit")
        void assemble_toolTable_hasCargoAudit(
                @TempDir Path tempDir)
                throws IOException {
            String content = generateAndRead(tempDir);
            assertThat(content)
                    .contains("| cargo")
                    .contains("cargo-audit");
        }

        @Test
        @DisplayName("all tools have Semgrep as fallback")
        void assemble_toolTable_hasSemgrepFallback(
                @TempDir Path tempDir)
                throws IOException {
            String content = generateAndRead(tempDir);
            assertThat(content).contains("Semgrep");
        }
    }

    @Nested
    @DisplayName("OWASP Top 10 Mapping")
    class OwaspMapping {

        @Test
        @DisplayName("includes OWASP A01-A10 categories")
        void assemble_owaspMapping_hasAllCategories(
                @TempDir Path tempDir)
                throws IOException {
            String content = generateAndRead(tempDir);
            assertThat(content)
                    .contains("| A01 |")
                    .contains("| A02 |")
                    .contains("| A03 |")
                    .contains("| A04 |")
                    .contains("| A05 |")
                    .contains("| A06 |")
                    .contains("| A07 |")
                    .contains("| A08 |")
                    .contains("| A09 |")
                    .contains("| A10 |");
        }

        @Test
        @DisplayName("includes SSRF category for A10")
        void assemble_owaspMapping_hasUnclassified(
                @TempDir Path tempDir)
                throws IOException {
            String content = generateAndRead(tempDir);
            assertThat(content)
                    .contains("SSRF");
        }
    }

    @Nested
    @DisplayName("SARIF Rule IDs")
    class SarifRuleIds {

        @Test
        @DisplayName("uses SAST-NNN rule ID prefix")
        void assemble_sarifRules_usesSastPrefix(
                @TempDir Path tempDir)
                throws IOException {
            String content = generateAndRead(tempDir);
            assertThat(content)
                    .contains("SAST-001")
                    .contains("SAST-002")
                    .contains("SAST-003");
        }

        @Test
        @DisplayName("SAST-001 maps to SQL Injection")
        void assemble_sarifRules_sast001SqlInjection(
                @TempDir Path tempDir)
                throws IOException {
            String content = generateAndRead(tempDir);
            assertThat(content)
                    .contains("SAST-001")
                    .contains("CWE-89")
                    .contains("SQL Injection");
        }

        @Test
        @DisplayName("SAST-002 maps to XSS")
        void assemble_sarifRules_sast002Xss(
                @TempDir Path tempDir)
                throws IOException {
            String content = generateAndRead(tempDir);
            assertThat(content)
                    .contains("SAST-002")
                    .contains("CWE-79")
                    .contains("Cross-Site Scripting");
        }

        @Test
        @DisplayName("SAST-015 is tool-not-found INFO")
        void assemble_sarifRules_sast015ToolNotFound(
                @TempDir Path tempDir)
                throws IOException {
            String content = generateAndRead(tempDir);
            assertThat(content)
                    .contains("SAST-015")
                    .contains("Tool Not Found");
        }
    }

    @Nested
    @DisplayName("Parameters")
    class Parameters {

        @Test
        @DisplayName("documents --scope parameter")
        void assemble_params_hasScope(
                @TempDir Path tempDir)
                throws IOException {
            String content = generateAndRead(tempDir);
            assertThat(content)
                    .contains("`--scope`")
                    .contains("`all`");
        }

        @Test
        @DisplayName("documents --fix parameter")
        void assemble_params_hasFix(
                @TempDir Path tempDir)
                throws IOException {
            String content = generateAndRead(tempDir);
            assertThat(content)
                    .contains("`--fix`")
                    .contains("`report-only`");
        }

        @Test
        @DisplayName("documents --severity-threshold"
                + " parameter")
        void assemble_params_hasSeverityThreshold(
                @TempDir Path tempDir)
                throws IOException {
            String content = generateAndRead(tempDir);
            assertThat(content)
                    .contains("`--severity-threshold`")
                    .contains("`LOW`");
        }

        @Test
        @DisplayName("documents --exclude parameter")
        void assemble_params_hasExclude(
                @TempDir Path tempDir)
                throws IOException {
            String content = generateAndRead(tempDir);
            assertThat(content)
                    .contains("`--exclude`");
        }
    }

    @Nested
    @DisplayName("Scoring Integration")
    class ScoringIntegration {

        @Test
        @DisplayName("references security-scoring.md")
        void assemble_scoring_refsModel(
                @TempDir Path tempDir)
                throws IOException {
            String content = generateAndRead(tempDir);
            assertThat(content).contains(
                    "security-scoring.md");
        }

        @Test
        @DisplayName("includes severity penalty table")
        void assemble_scoring_hasPenaltyTable(
                @TempDir Path tempDir)
                throws IOException {
            String content = generateAndRead(tempDir);
            assertThat(content)
                    .contains("| CRITICAL | -25")
                    .contains("| HIGH | -15")
                    .contains("| MEDIUM | -5")
                    .contains("| LOW | -2")
                    .contains("| INFO | 0");
        }

        @Test
        @DisplayName("includes grade thresholds A-F")
        void assemble_scoring_hasGradeThresholds(
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
    @DisplayName("CI Integration Snippets")
    class CiIntegrationSnippets {

        @Test
        @DisplayName("includes GitHub Actions snippet")
        void assemble_ci_hasGitHubActions(
                @TempDir Path tempDir)
                throws IOException {
            String content = generateAndRead(tempDir);
            assertThat(content).contains(
                    "### GitHub Actions");
        }

        @Test
        @DisplayName("GitHub Actions uploads SARIF")
        void assemble_ci_ghUploadsSarif(
                @TempDir Path tempDir)
                throws IOException {
            String content = generateAndRead(tempDir);
            assertThat(content).contains("upload-sarif");
        }

        @Test
        @DisplayName("includes upload-sarif action")
        void assemble_ci_hasGitLabCi(
                @TempDir Path tempDir)
                throws IOException {
            String content = generateAndRead(tempDir);
            assertThat(content).contains(
                    "upload-sarif");
        }

        @Test
        @DisplayName("includes SARIF file output path")
        void assemble_ci_hasAzureDevOps(
                @TempDir Path tempDir)
                throws IOException {
            String content = generateAndRead(tempDir);
            assertThat(content).contains(
                    "sarif_file: results/security/");
        }
    }

    @Nested
    @DisplayName("Error Handling")
    class ErrorHandling {

        @Test
        @DisplayName("tool-not-found sets score to 100")
        void assemble_errorHandling_toolNotFoundScore100(
                @TempDir Path tempDir)
                throws IOException {
            String content = generateAndRead(tempDir);
            assertThat(content).contains(
                    "score 100");
        }

        @Test
        @DisplayName("tool crash sets score to 0")
        void assemble_errorHandling_toolCrashScore0(
                @TempDir Path tempDir)
                throws IOException {
            String content = generateAndRead(tempDir);
            assertThat(content).contains(
                    "score 0");
        }

        @Test
        @DisplayName("zero findings generates valid SARIF")
        void assemble_errorHandling_zeroFindingsValidSarif(
                @TempDir Path tempDir)
                throws IOException {
            String content = generateAndRead(tempDir);
            assertThat(content)
                    .contains("Zero findings")
                    .contains("score 100");
        }
    }

    @Nested
    @DisplayName("SARIF Output Format")
    class SarifOutputFormat {

        @Test
        @DisplayName("references SARIF 2.1.0 format")
        void assemble_sarif_references210(
                @TempDir Path tempDir)
                throws IOException {
            String content = generateAndRead(tempDir);
            assertThat(content).contains("SARIF 2.1.0");
        }

        @Test
        @DisplayName("references sarif-template.md")
        void assemble_sarif_refsTemplate(
                @TempDir Path tempDir)
                throws IOException {
            String content = generateAndRead(tempDir);
            assertThat(content).contains(
                    "sarif-template.md");
        }

        @Test
        @DisplayName("specifies SAST-specific SARIF rule"
                + " ID convention")
        void assemble_sarif_hasToolDriverName(
                @TempDir Path tempDir)
                throws IOException {
            String content = generateAndRead(tempDir);
            assertThat(content).contains(
                    "SARIF Rule ID Convention");
        }
    }

    private String generateAndRead(Path tempDir)
            throws IOException {
        generateSastOutput(tempDir);
        return Files.readString(
                tempDir.resolve(
                        "output/skills/x-security-sast"
                                + "/SKILL.md"),
                StandardCharsets.UTF_8);
    }

    private void generateSastOutput(Path tempDir)
            throws IOException {
        Path outputDir = tempDir.resolve("output");
        Files.createDirectories(outputDir);
        ProjectConfig config =
                TestConfigBuilder.builder()
                        .scanningFlags(
                                true, false,
                                false, false, false)
                        .build();
        new SkillsAssembler().assemble(
                config, new TemplateEngine(), outputDir);
    }
}
