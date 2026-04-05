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
 * Tests for story-0022-0014: Enhanced Supply Chain Audit
 * (x-supply-chain-audit) skill generation.
 *
 * <p>Validates that the x-supply-chain-audit SKILL.md is
 * generated as a core skill, contains all 6 advanced
 * capabilities, risk scoring formula, SARIF output format,
 * and does not duplicate x-dependency-audit content.</p>
 */
@DisplayName("Supply Chain Audit Skill (x-supply-chain-audit)")
class SupplyChainAuditSkillTest {

    @Nested
    @DisplayName("Core Skill Generation")
    class CoreSkillGeneration {

        @Test
        @DisplayName("x-supply-chain-audit SKILL.md exists"
                + " after assembly")
        void assemble_minimal_generatesSkillMd(
                @TempDir Path tempDir)
                throws IOException {
            generateOutput(tempDir);
            Path skillMd = tempDir.resolve(
                    "output/skills/x-supply-chain-audit"
                            + "/SKILL.md");
            assertThat(skillMd).exists();
        }

        @Test
        @DisplayName("SKILL.md contains correct frontmatter"
                + " name")
        void assemble_minimal_hasFrontmatterName(
                @TempDir Path tempDir)
                throws IOException {
            String content = generateContent(tempDir);
            assertThat(content)
                    .contains("name: x-supply-chain-audit");
        }

        @Test
        @DisplayName("SKILL.md contains allowed-tools")
        void assemble_minimal_hasAllowedTools(
                @TempDir Path tempDir)
                throws IOException {
            String content = generateContent(tempDir);
            assertThat(content)
                    .contains("allowed-tools:");
        }

        @Test
        @DisplayName("SKILL.md contains Purpose section")
        void assemble_minimal_hasPurposeSection(
                @TempDir Path tempDir)
                throws IOException {
            String content = generateContent(tempDir);
            assertThat(content)
                    .contains("## Purpose")
                    .contains("supply chain security");
        }
    }

    @Nested
    @DisplayName("Six Advanced Capabilities")
    class AdvancedCapabilities {

        @Test
        @DisplayName("contains maintainer risk analysis")
        void assemble_minimal_hasMaintainerRisk(
                @TempDir Path tempDir)
                throws IOException {
            String content = generateContent(tempDir);
            assertThat(content)
                    .contains("Maintainer Risk Analysis")
                    .contains("bus factor");
        }

        @Test
        @DisplayName("contains typosquatting detection")
        void assemble_minimal_hasTyposquatting(
                @TempDir Path tempDir)
                throws IOException {
            String content = generateContent(tempDir);
            assertThat(content)
                    .contains("Typosquatting Detection")
                    .contains("Levenshtein");
        }

        @Test
        @DisplayName("contains phantom dependency detection")
        void assemble_minimal_hasPhantomDeps(
                @TempDir Path tempDir)
                throws IOException {
            String content = generateContent(tempDir);
            assertThat(content)
                    .contains("Phantom Dependency Detection")
                    .contains("AST");
        }

        @Test
        @DisplayName("contains dependency age analysis")
        void assemble_minimal_hasDependencyAge(
                @TempDir Path tempDir)
                throws IOException {
            String content = generateContent(tempDir);
            assertThat(content)
                    .contains("Dependency Age Analysis")
                    .contains("last release");
        }

        @Test
        @DisplayName("contains EPSS scoring")
        void assemble_minimal_hasEpssScoring(
                @TempDir Path tempDir)
                throws IOException {
            String content = generateContent(tempDir);
            assertThat(content)
                    .contains("EPSS Scoring")
                    .contains("FIRST.org");
        }

        @Test
        @DisplayName("contains SLSA assessment")
        void assemble_minimal_hasSlsaAssessment(
                @TempDir Path tempDir)
                throws IOException {
            String content = generateContent(tempDir);
            assertThat(content)
                    .contains("SLSA Assessment")
                    .contains("SLSA 0")
                    .contains("SLSA 3");
        }
    }

    @Nested
    @DisplayName("Risk Scoring Formula")
    class RiskScoringFormula {

        @Test
        @DisplayName("contains weighted risk score formula")
        void assemble_minimal_hasRiskFormula(
                @TempDir Path tempDir)
                throws IOException {
            String content = generateContent(tempDir);
            assertThat(content)
                    .contains("cve_severity * 0.40")
                    .contains("depth_score * 0.20")
                    .contains("maintainer_risk * 0.15")
                    .contains("license_risk * 0.15")
                    .contains("popularity_inverse * 0.10");
        }

        @Test
        @DisplayName("contains severity classification"
                + " thresholds")
        void assemble_minimal_hasSeverityClassification(
                @TempDir Path tempDir)
                throws IOException {
            String content = generateContent(tempDir);
            assertThat(content)
                    .contains("CRITICAL")
                    .contains("HIGH")
                    .contains("MEDIUM")
                    .contains("LOW")
                    .contains("INFO");
        }

        @Test
        @DisplayName("contains grade scale A through F")
        void assemble_minimal_hasGradeScale(
                @TempDir Path tempDir)
                throws IOException {
            String content = generateContent(tempDir);
            assertThat(content)
                    .contains("| A |")
                    .contains("| B |")
                    .contains("| C |")
                    .contains("| D |")
                    .contains("| F |");
        }
    }

    @Nested
    @DisplayName("CLI Parameters")
    class CliParameters {

        @Test
        @DisplayName("contains --depth parameter")
        void assemble_minimal_hasDepthParam(
                @TempDir Path tempDir)
                throws IOException {
            String content = generateContent(tempDir);
            assertThat(content)
                    .contains("--depth")
                    .contains("shallow")
                    .contains("deep");
        }

        @Test
        @DisplayName("contains --include-dev-deps"
                + " parameter")
        void assemble_minimal_hasIncludeDevDeps(
                @TempDir Path tempDir)
                throws IOException {
            String content = generateContent(tempDir);
            assertThat(content)
                    .contains("--include-dev-deps");
        }

        @Test
        @DisplayName("contains --risk-threshold parameter")
        void assemble_minimal_hasRiskThreshold(
                @TempDir Path tempDir)
                throws IOException {
            String content = generateContent(tempDir);
            assertThat(content)
                    .contains("--risk-threshold");
        }

        @Test
        @DisplayName("contains --focus parameter with all"
                + " categories")
        void assemble_minimal_hasFocusParam(
                @TempDir Path tempDir)
                throws IOException {
            String content = generateContent(tempDir);
            assertThat(content)
                    .contains("--focus")
                    .contains("maintainer")
                    .contains("typosquatting")
                    .contains("phantom")
                    .contains("age")
                    .contains("epss")
                    .contains("slsa");
        }
    }

    @Nested
    @DisplayName("SARIF Output Format")
    class SarifOutput {

        @Test
        @DisplayName("references SARIF 2.1.0")
        void assemble_minimal_referencesSarif(
                @TempDir Path tempDir)
                throws IOException {
            String content = generateContent(tempDir);
            assertThat(content)
                    .contains("SARIF 2.1.0");
        }

        @Test
        @DisplayName("contains SARIF rule IDs")
        void assemble_minimal_hasSarifRuleIds(
                @TempDir Path tempDir)
                throws IOException {
            String content = generateContent(tempDir);
            assertThat(content)
                    .contains("SCA-MAINT-001")
                    .contains("SCA-TYPO-001")
                    .contains("SCA-PHANTOM-001")
                    .contains("SCA-AGE-001")
                    .contains("SCA-EPSS-001")
                    .contains("SCA-SLSA-001");
        }

        @Test
        @DisplayName("references sarif-template.md"
                + " knowledge pack")
        void assemble_minimal_referencesSarifTemplate(
                @TempDir Path tempDir)
                throws IOException {
            String content = generateContent(tempDir);
            assertThat(content)
                    .contains("sarif-template.md");
        }
    }

    @Nested
    @DisplayName("Relationship with x-dependency-audit")
    class DependencyAuditRelation {

        @Test
        @DisplayName("documents relationship with"
                + " x-dependency-audit")
        void assemble_minimal_hasRelationTable(
                @TempDir Path tempDir)
                throws IOException {
            String content = generateContent(tempDir);
            assertThat(content)
                    .contains("x-dependency-audit")
                    .contains("x-supply-chain-audit");
        }

        @Test
        @DisplayName("states it complements not replaces"
                + " x-dependency-audit")
        void assemble_minimal_complementsNotReplaces(
                @TempDir Path tempDir)
                throws IOException {
            String content = generateContent(tempDir);
            assertThat(content)
                    .contains("complements")
                    .contains("does NOT replace");
        }

        @Test
        @DisplayName("x-dependency-audit skill remains"
                + " unchanged")
        void assemble_minimal_depAuditUnchanged(
                @TempDir Path tempDir)
                throws IOException {
            Path outputDir = tempDir.resolve("output");
            Files.createDirectories(outputDir);
            SkillsAssembler assembler =
                    new SkillsAssembler();
            assembler.assemble(
                    TestConfigBuilder.minimal(),
                    new TemplateEngine(), outputDir);
            String depAudit = Files.readString(
                    outputDir.resolve(
                            "skills/x-dependency-audit"
                                    + "/SKILL.md"),
                    StandardCharsets.UTF_8);
            assertThat(depAudit)
                    .contains("name: x-dependency-audit")
                    .contains("## Workflow")
                    .contains("DETECT")
                    .contains("AUDIT")
                    .contains("## Error Handling");
        }
    }

    @Nested
    @DisplayName("Template Variable Preservation")
    class TemplateVariables {

        @Test
        @DisplayName("preserves PROJECT_NAME placeholder"
                + " for runtime substitution")
        void assemble_minimal_preservesProjectName(
                @TempDir Path tempDir)
                throws IOException {
            String content = generateContent(tempDir);
            assertThat(content)
                    .contains("{{PROJECT_NAME}}");
        }

        @Test
        @DisplayName("preserves BUILD_TOOL placeholder"
                + " for runtime substitution")
        void assemble_minimal_preservesBuildTool(
                @TempDir Path tempDir)
                throws IOException {
            String content = generateContent(tempDir);
            assertThat(content)
                    .contains("{{BUILD_TOOL}}");
        }
    }

    @Nested
    @DisplayName("SkillGroupRegistry Integration")
    class RegistryIntegration {

        @Test
        @DisplayName("review group contains"
                + " x-supply-chain-audit")
        void registry_reviewGroup_containsSupplyChainAudit() {
            assertThat(SkillGroupRegistry.SKILL_GROUPS
                    .get("review"))
                    .contains("x-supply-chain-audit");
        }

        @Test
        @DisplayName("review group contains both"
                + " dependency-audit and supply-chain-audit")
        void registry_reviewGroup_containsBothAuditSkills() {
            assertThat(SkillGroupRegistry.SKILL_GROUPS
                    .get("review"))
                    .contains("x-dependency-audit")
                    .contains("x-supply-chain-audit");
        }
    }

    private String generateContent(Path tempDir)
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
                        "skills/x-supply-chain-audit"
                                + "/SKILL.md"),
                StandardCharsets.UTF_8);
    }

    private void generateOutput(Path tempDir)
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
