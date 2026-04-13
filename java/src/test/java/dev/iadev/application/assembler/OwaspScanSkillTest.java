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
 * Tests for story-0022-0010: x-owasp-scan skill for
 * OWASP Top 10 verification mapped to ASVS levels.
 *
 * <p>Validates that the x-owasp-scan skill template is
 * generated correctly with proper frontmatter, all 10
 * OWASP categories, ASVS level mapping, delegation of
 * A06, scoring, and SARIF output.</p>
 */
@DisplayName("x-owasp-scan Skill")
class OwaspScanSkillTest {

    @Nested
    @DisplayName("Claude SKILL.md -- Frontmatter")
    class ClaudeFrontmatter {

        @Test
        @DisplayName("SKILL.md exists after assembly")
        void assemble_owaspScan_skillMdExists(
                @TempDir Path tempDir)
                throws IOException {
            Path outputDir = generateOutput(tempDir);
            Path skillMd = outputDir.resolve(
                    "skills/x-owasp-scan/SKILL.md");
            assertThat(skillMd).exists();
        }

        @Test
        @DisplayName("frontmatter contains name:"
                + " x-owasp-scan")
        void assemble_owaspScan_hasName(
                @TempDir Path tempDir)
                throws IOException {
            String content =
                    generateClaudeContent(tempDir);
            assertThat(content)
                    .contains("name: x-owasp-scan");
        }

        @Test
        @DisplayName("frontmatter contains"
                + " user-invocable: true")
        void assemble_owaspScan_hasUserInvocable(
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
        void assemble_owaspScan_hasArgumentHint(
                @TempDir Path tempDir)
                throws IOException {
            String content =
                    generateClaudeContent(tempDir);
            assertThat(content)
                    .contains("argument-hint:");
        }

        @Test
        @DisplayName("frontmatter contains allowed-tools")
        void assemble_owaspScan_hasAllowedTools(
                @TempDir Path tempDir)
                throws IOException {
            String content =
                    generateClaudeContent(tempDir);
            assertThat(content)
                    .contains("allowed-tools:");
        }

        @Test
        @DisplayName("frontmatter contains description"
                + " with OWASP")
        void assemble_owaspScan_hasDescription(
                @TempDir Path tempDir)
                throws IOException {
            String content =
                    generateClaudeContent(tempDir);
            assertThat(content)
                    .contains("description:")
                    .contains("OWASP");
        }
    }

    @Nested
    @DisplayName("Claude SKILL.md -- OWASP Categories")
    class OwaspCategories {

        @Test
        @DisplayName("contains A01 Broken Access Control")
        void assemble_owaspScan_hasA01(
                @TempDir Path tempDir)
                throws IOException {
            String content =
                    generateClaudeContent(tempDir);
            assertThat(content)
                    .contains("A01")
                    .contains("Broken Access Control");
        }

        @Test
        @DisplayName("contains A02 Cryptographic Failures")
        void assemble_owaspScan_hasA02(
                @TempDir Path tempDir)
                throws IOException {
            String content =
                    generateClaudeContent(tempDir);
            assertThat(content)
                    .contains("A02")
                    .contains("Cryptographic Failures");
        }

        @Test
        @DisplayName("contains A03 Injection")
        void assemble_owaspScan_hasA03(
                @TempDir Path tempDir)
                throws IOException {
            String content =
                    generateClaudeContent(tempDir);
            assertThat(content)
                    .contains("A03")
                    .contains("Injection");
        }

        @Test
        @DisplayName("contains A04 Insecure Design")
        void assemble_owaspScan_hasA04(
                @TempDir Path tempDir)
                throws IOException {
            String content =
                    generateClaudeContent(tempDir);
            assertThat(content)
                    .contains("A04")
                    .contains("Insecure Design");
        }

        @Test
        @DisplayName("contains A05 Security"
                + " Misconfiguration")
        void assemble_owaspScan_hasA05(
                @TempDir Path tempDir)
                throws IOException {
            String content =
                    generateClaudeContent(tempDir);
            assertThat(content)
                    .contains("A05")
                    .contains("Security Misconfiguration");
        }

        @Test
        @DisplayName("contains A06 Vulnerable Components")
        void assemble_owaspScan_hasA06(
                @TempDir Path tempDir)
                throws IOException {
            String content =
                    generateClaudeContent(tempDir);
            assertThat(content)
                    .contains("A06")
                    .contains("Vulnerable");
        }

        @Test
        @DisplayName("contains A07 Auth Failures")
        void assemble_owaspScan_hasA07(
                @TempDir Path tempDir)
                throws IOException {
            String content =
                    generateClaudeContent(tempDir);
            assertThat(content)
                    .contains("A07")
                    .contains("Auth");
        }

        @Test
        @DisplayName("contains A08 Software/Data"
                + " Integrity")
        void assemble_owaspScan_hasA08(
                @TempDir Path tempDir)
                throws IOException {
            String content =
                    generateClaudeContent(tempDir);
            assertThat(content)
                    .contains("A08")
                    .contains("Integrity");
        }

        @Test
        @DisplayName("contains A09 Logging Failures")
        void assemble_owaspScan_hasA09(
                @TempDir Path tempDir)
                throws IOException {
            String content =
                    generateClaudeContent(tempDir);
            assertThat(content)
                    .contains("A09")
                    .contains("Logging");
        }

        @Test
        @DisplayName("contains A10 SSRF")
        void assemble_owaspScan_hasA10(
                @TempDir Path tempDir)
                throws IOException {
            String content =
                    generateClaudeContent(tempDir);
            assertThat(content)
                    .contains("A10")
                    .contains("SSRF");
        }
    }

    @Nested
    @DisplayName("Claude SKILL.md -- ASVS Mapping")
    class AsvsMapping {

        @Test
        @DisplayName("maps A01 to ASVS V4")
        void assemble_owaspScan_mapsA01ToV4(
                @TempDir Path tempDir)
                throws IOException {
            String content =
                    generateClaudeContent(tempDir);
            assertThat(content).contains("V4");
        }

        @Test
        @DisplayName("maps A02 to ASVS V6 and V9")
        void assemble_owaspScan_mapsA02ToV6V9(
                @TempDir Path tempDir)
                throws IOException {
            String content =
                    generateClaudeContent(tempDir);
            assertThat(content)
                    .contains("V6")
                    .contains("V9");
        }

        @Test
        @DisplayName("maps A03 to ASVS V5")
        void assemble_owaspScan_mapsA03ToV5(
                @TempDir Path tempDir)
                throws IOException {
            String content =
                    generateClaudeContent(tempDir);
            assertThat(content).contains("V5");
        }

        @Test
        @DisplayName("maps A07 to ASVS V2 and V3")
        void assemble_owaspScan_mapsA07ToV2V3(
                @TempDir Path tempDir)
                throws IOException {
            String content =
                    generateClaudeContent(tempDir);
            assertThat(content)
                    .contains("V2")
                    .contains("V3");
        }

        @Test
        @DisplayName("maps A09 to ASVS V7")
        void assemble_owaspScan_mapsA09ToV7(
                @TempDir Path tempDir)
                throws IOException {
            String content =
                    generateClaudeContent(tempDir);
            assertThat(content).contains("V7");
        }
    }

    @Nested
    @DisplayName("Claude SKILL.md -- ASVS Levels")
    class AsvsLevels {

        @Test
        @DisplayName("contains L1 level definition")
        void assemble_owaspScan_hasL1(
                @TempDir Path tempDir)
                throws IOException {
            String content =
                    generateClaudeContent(tempDir);
            assertThat(content)
                    .contains("L1")
                    .contains("Opportunistic");
        }

        @Test
        @DisplayName("contains L2 level definition")
        void assemble_owaspScan_hasL2(
                @TempDir Path tempDir)
                throws IOException {
            String content =
                    generateClaudeContent(tempDir);
            assertThat(content)
                    .contains("L2")
                    .contains("Standard");
        }

        @Test
        @DisplayName("contains L3 level definition")
        void assemble_owaspScan_hasL3(
                @TempDir Path tempDir)
                throws IOException {
            String content =
                    generateClaudeContent(tempDir);
            assertThat(content)
                    .contains("L3")
                    .contains("Advanced");
        }

        @Test
        @DisplayName("L3 checks include more items"
                + " than L1")
        void assemble_owaspScan_l3HasMoreChecks(
                @TempDir Path tempDir)
                throws IOException {
            String content =
                    generateClaudeContent(tempDir);
            assertThat(content)
                    .contains("L3 Checks (includes L1+L2)");
        }
    }

    @Nested
    @DisplayName("Claude SKILL.md -- A06 Delegation")
    class A06Delegation {

        @Test
        @DisplayName("A06 is DELEGATED to"
                + " x-dependency-audit")
        void assemble_owaspScan_a06Delegated(
                @TempDir Path tempDir)
                throws IOException {
            String content =
                    generateClaudeContent(tempDir);
            assertThat(content)
                    .contains("DELEGATED")
                    .contains("x-dependency-audit");
        }

        @Test
        @DisplayName("references RULE-011 for"
                + " delegation")
        void assemble_owaspScan_refsRule011(
                @TempDir Path tempDir)
                throws IOException {
            String content =
                    generateClaudeContent(tempDir);
            assertThat(content)
                    .contains("RULE-011");
        }

        @Test
        @DisplayName("delegation result has"
                + " delegatedTo field")
        void assemble_owaspScan_hasDelegatedToField(
                @TempDir Path tempDir)
                throws IOException {
            String content =
                    generateClaudeContent(tempDir);
            assertThat(content)
                    .contains("delegatedTo");
        }
    }

    @Nested
    @DisplayName("Claude SKILL.md -- Scoring")
    class Scoring {

        @Test
        @DisplayName("contains grade mapping A-F")
        void assemble_owaspScan_hasGradeMapping(
                @TempDir Path tempDir)
                throws IOException {
            String content =
                    generateClaudeContent(tempDir);
            assertThat(content)
                    .contains("90-100")
                    .contains("Grade");
        }

        @Test
        @DisplayName("contains ASVS coverage percentage")
        void assemble_owaspScan_hasAsvsCoverage(
                @TempDir Path tempDir)
                throws IOException {
            String content =
                    generateClaudeContent(tempDir);
            assertThat(content)
                    .contains("ASVS Coverage");
        }

        @Test
        @DisplayName("contains per-category score"
                + " formula")
        void assemble_owaspScan_hasScoreFormula(
                @TempDir Path tempDir)
                throws IOException {
            String content =
                    generateClaudeContent(tempDir);
            assertThat(content)
                    .contains("passedChecks")
                    .contains("totalChecks");
        }
    }

    @Nested
    @DisplayName("Claude SKILL.md -- SARIF Output")
    class SarifOutput {

        @Test
        @DisplayName("contains SARIF 2.1.0 reference")
        void assemble_owaspScan_hasSarif(
                @TempDir Path tempDir)
                throws IOException {
            String content =
                    generateClaudeContent(tempDir);
            assertThat(content)
                    .contains("SARIF")
                    .contains("2.1.0");
        }

        @Test
        @DisplayName("SARIF schema references OASIS")
        void assemble_owaspScan_sarifSchemaOasis(
                @TempDir Path tempDir)
                throws IOException {
            String content =
                    generateClaudeContent(tempDir);
            assertThat(content)
                    .contains("sarif-schema-2.1.0");
        }

        @Test
        @DisplayName("SARIF output includes OWASP"
                + " rule IDs")
        void assemble_owaspScan_sarifHasRuleIds(
                @TempDir Path tempDir)
                throws IOException {
            String content =
                    generateClaudeContent(tempDir);
            assertThat(content)
                    .contains("OWASP-A01");
        }
    }

    @Nested
    @DisplayName("Claude SKILL.md -- CI Integration")
    class CiIntegration {

        @Test
        @DisplayName("contains CI integration section")
        void assemble_owaspScan_hasCiSection(
                @TempDir Path tempDir)
                throws IOException {
            String content =
                    generateClaudeContent(tempDir);
            assertThat(content)
                    .contains("CI Integration");
        }

        @Test
        @DisplayName("mentions exit code for CI")
        void assemble_owaspScan_hasExitCode(
                @TempDir Path tempDir)
                throws IOException {
            String content =
                    generateClaudeContent(tempDir);
            assertThat(content)
                    .contains("exit code");
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
                        "skills/x-owasp-scan/SKILL.md"),
                StandardCharsets.UTF_8);
    }

}
