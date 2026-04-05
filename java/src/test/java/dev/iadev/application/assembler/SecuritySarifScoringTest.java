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
 * Integration tests for story-0022-0002: Security Report
 * Infrastructure (SARIF + Scoring) reference files and
 * SKILL.md registration.
 */
@DisplayName("Security SARIF + Scoring References")
class SecuritySarifScoringTest {

    @Nested
    @DisplayName("SARIF Template Reference")
    class SarifTemplate {

        @Test
        @DisplayName("sarif-template.md exists after"
                + " assembly")
        void assemble_sarifTemplate_fileExists(
                @TempDir Path tempDir)
                throws IOException {
            generateSecurityOutput(tempDir);
            Path sarifTemplate = tempDir.resolve(
                    "output/skills/security/references/"
                            + "sarif-template.md");
            assertThat(sarifTemplate).exists();
        }

        @Test
        @DisplayName("sarif-template contains SARIF"
                + " 2.1.0 schema reference")
        void assemble_sarifTemplate_hasSchemaRef(
                @TempDir Path tempDir)
                throws IOException {
            String content =
                    readSarifTemplate(tempDir);
            assertThat(content)
                    .contains("sarif-schema-2.1.0.json");
        }

        @Test
        @DisplayName("sarif-template contains version"
                + " 2.1.0")
        void assemble_sarifTemplate_hasVersion(
                @TempDir Path tempDir)
                throws IOException {
            String content =
                    readSarifTemplate(tempDir);
            assertThat(content)
                    .contains("\"version\": \"2.1.0\"");
        }

        @Test
        @DisplayName("sarif-template contains required"
                + " fields: tool, results")
        void assemble_sarifTemplate_hasRequiredFields(
                @TempDir Path tempDir)
                throws IOException {
            String content =
                    readSarifTemplate(tempDir);
            assertThat(content)
                    .contains("\"tool\"")
                    .contains("\"results\"")
                    .contains("\"runs\"");
        }

        @Test
        @DisplayName("sarif-template contains custom"
                + " properties")
        void assemble_sarifTemplate_hasCustomProperties(
                @TempDir Path tempDir)
                throws IOException {
            String content =
                    readSarifTemplate(tempDir);
            assertThat(content)
                    .contains("owasp-category")
                    .contains("cvss-score")
                    .contains("cwe-id")
                    .contains("fix-recommendation");
        }

        @Test
        @DisplayName("sarif-template has examples for"
                + " all severity levels")
        void assemble_sarifTemplate_hasExamplesPerLevel(
                @TempDir Path tempDir)
                throws IOException {
            String content =
                    readSarifTemplate(tempDir);
            assertThat(content)
                    .contains("### CRITICAL")
                    .contains("### HIGH")
                    .contains("### MEDIUM")
                    .contains("### LOW")
                    .contains("### INFO");
        }
    }

    @Nested
    @DisplayName("Security Scoring Reference")
    class ScoringReference {

        @Test
        @DisplayName("security-scoring.md exists after"
                + " assembly")
        void assemble_scoring_fileExists(
                @TempDir Path tempDir)
                throws IOException {
            generateSecurityOutput(tempDir);
            Path scoring = tempDir.resolve(
                    "output/skills/security/references/"
                            + "security-scoring.md");
            assertThat(scoring).exists();
        }

        @Test
        @DisplayName("security-scoring contains formula")
        void assemble_scoring_hasFormula(
                @TempDir Path tempDir)
                throws IOException {
            String content =
                    readScoringReference(tempDir);
            assertThat(content).contains(
                    "score = max(0, 100 - sum("
                            + "severity_weight * "
                            + "count_per_severity))");
        }

        @Test
        @DisplayName("security-scoring contains grade"
                + " scale A-F")
        void assemble_scoring_hasGradeScale(
                @TempDir Path tempDir)
                throws IOException {
            String content =
                    readScoringReference(tempDir);
            assertThat(content)
                    .contains("| A | 90 - 100")
                    .contains("| B | 80 - 89")
                    .contains("| C | 70 - 79")
                    .contains("| D | 60 - 69")
                    .contains("| F | 0 - 59");
        }

        @Test
        @DisplayName("security-scoring contains severity"
                + " weights")
        void assemble_scoring_hasSeverityWeights(
                @TempDir Path tempDir)
                throws IOException {
            String content =
                    readScoringReference(tempDir);
            assertThat(content)
                    .contains("| CRITICAL | 10")
                    .contains("| HIGH | 5")
                    .contains("| MEDIUM | 2")
                    .contains("| LOW | 1")
                    .contains("| INFO | 0");
        }

        @Test
        @DisplayName("security-scoring contains output"
                + " convention")
        void assemble_scoring_hasOutputConvention(
                @TempDir Path tempDir)
                throws IOException {
            String content =
                    readScoringReference(tempDir);
            assertThat(content)
                    .contains("results/security/");
        }
    }

    @Nested
    @DisplayName("SKILL.md Registration")
    class SkillMdRegistration {

        @Test
        @DisplayName("SKILL.md references"
                + " sarif-template.md in table")
        void assemble_skillMd_refsSarifTemplate(
                @TempDir Path tempDir)
                throws IOException {
            String content =
                    generateSecurityKpContent(tempDir);
            assertThat(content)
                    .contains("sarif-template.md");
        }

        @Test
        @DisplayName("SKILL.md references"
                + " security-scoring.md in table")
        void assemble_skillMd_refsScoringModel(
                @TempDir Path tempDir)
                throws IOException {
            String content =
                    generateSecurityKpContent(tempDir);
            assertThat(content)
                    .contains("security-scoring.md");
        }

        @Test
        @DisplayName("existing references preserved")
        void assemble_skillMd_preservesExisting(
                @TempDir Path tempDir)
                throws IOException {
            String content =
                    generateSecurityKpContent(tempDir);
            assertThat(content)
                    .contains("security-principles.md")
                    .contains("application-security.md")
                    .contains("cryptography.md")
                    .contains("pentest-readiness.md")
                    .contains("sbom-generation-guide.md")
                    .contains("supply-chain-hardening.md");
        }
    }

    private String generateSecurityKpContent(
            Path tempDir) throws IOException {
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

    private String readSarifTemplate(Path tempDir)
            throws IOException {
        generateSecurityOutput(tempDir);
        return Files.readString(
                tempDir.resolve(
                        "output/skills/security/"
                                + "references/"
                                + "sarif-template.md"),
                StandardCharsets.UTF_8);
    }

    private String readScoringReference(Path tempDir)
            throws IOException {
        generateSecurityOutput(tempDir);
        return Files.readString(
                tempDir.resolve(
                        "output/skills/security/"
                                + "references/"
                                + "security-scoring.md"),
                StandardCharsets.UTF_8);
    }
}
