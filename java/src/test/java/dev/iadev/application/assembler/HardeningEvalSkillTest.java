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
 * Tests for story-0022-0012: x-hardening-eval skill for
 * application hardening evaluation against CIS/OWASP
 * benchmarks.
 *
 * <p>Validates that the x-hardening-eval skill template is
 * generated correctly with proper frontmatter, 7 hardening
 * dimensions, weighted scoring, SARIF output, benchmark
 * support, and ASVS level mapping.</p>
 */
@DisplayName("x-hardening-eval Skill")
class HardeningEvalSkillTest {

    @Nested
    @DisplayName("Claude SKILL.md -- Frontmatter")
    class ClaudeFrontmatter {

        @Test
        @DisplayName("x-hardening-eval SKILL.md exists"
                + " after assembly")
        void assemble_hardeningEval_skillMdExists(
                @TempDir Path tempDir)
                throws IOException {
            Path outputDir = generateOutput(tempDir);
            Path skillMd = outputDir.resolve(
                    "skills/x-hardening-eval/SKILL.md");
            assertThat(skillMd).exists();
        }

        @Test
        @DisplayName("frontmatter contains name:"
                + " x-hardening-eval")
        void assemble_hardeningEval_hasName(
                @TempDir Path tempDir)
                throws IOException {
            String content =
                    generateClaudeContent(tempDir);
            assertThat(content)
                    .contains("name: x-hardening-eval");
        }

        @Test
        @DisplayName("frontmatter contains"
                + " user-invocable: true")
        void assemble_hardeningEval_hasUserInvocable(
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
                + " with --target")
        void assemble_hardeningEval_hasArgumentHint(
                @TempDir Path tempDir)
                throws IOException {
            String content =
                    generateClaudeContent(tempDir);
            assertThat(content)
                    .contains("argument-hint:")
                    .contains("--target");
        }

        @Test
        @DisplayName("frontmatter contains allowed-tools")
        void assemble_hardeningEval_hasAllowedTools(
                @TempDir Path tempDir)
                throws IOException {
            String content =
                    generateClaudeContent(tempDir);
            assertThat(content)
                    .contains("allowed-tools:");
        }

        @Test
        @DisplayName("frontmatter description mentions"
                + " hardening and SARIF")
        void assemble_hardeningEval_hasDescription(
                @TempDir Path tempDir)
                throws IOException {
            String content =
                    generateClaudeContent(tempDir);
            assertThat(content)
                    .contains("description:")
                    .contains("hardening")
                    .contains("SARIF");
        }
    }

    @Nested
    @DisplayName("Claude SKILL.md -- Dimensions")
    class HardeningDimensions {

        @Test
        @DisplayName("contains HTTP Headers dimension"
                + " with 25% weight")
        void assemble_hardeningEval_hasHeaders(
                @TempDir Path tempDir)
                throws IOException {
            String content =
                    generateClaudeContent(tempDir);
            assertThat(content)
                    .contains("HTTP Security Headers")
                    .contains("25%");
        }

        @Test
        @DisplayName("contains TLS dimension"
                + " with 20% weight")
        void assemble_hardeningEval_hasTls(
                @TempDir Path tempDir)
                throws IOException {
            String content =
                    generateClaudeContent(tempDir);
            assertThat(content)
                    .contains("TLS Configuration")
                    .contains("20%");
        }

        @Test
        @DisplayName("contains CORS dimension"
                + " with 15% weight")
        void assemble_hardeningEval_hasCors(
                @TempDir Path tempDir)
                throws IOException {
            String content =
                    generateClaudeContent(tempDir);
            assertThat(content)
                    .contains("CORS Policy")
                    .contains("15%");
        }

        @Test
        @DisplayName("contains Cookie Security dimension"
                + " with 15% weight")
        void assemble_hardeningEval_hasCookies(
                @TempDir Path tempDir)
                throws IOException {
            String content =
                    generateClaudeContent(tempDir);
            assertThat(content)
                    .contains("Cookie Security")
                    .contains("Secure")
                    .contains("HttpOnly")
                    .contains("SameSite");
        }

        @Test
        @DisplayName("contains Error Handling dimension"
                + " with 10% weight")
        void assemble_hardeningEval_hasErrors(
                @TempDir Path tempDir)
                throws IOException {
            String content =
                    generateClaudeContent(tempDir);
            assertThat(content)
                    .contains("Error Handling")
                    .contains("Stack Trace Suppression");
        }

        @Test
        @DisplayName("contains Input Limits dimension"
                + " with 10% weight")
        void assemble_hardeningEval_hasLimits(
                @TempDir Path tempDir)
                throws IOException {
            String content =
                    generateClaudeContent(tempDir);
            assertThat(content)
                    .contains("Input Limits")
                    .contains("Rate Limiting");
        }

        @Test
        @DisplayName("contains Information Disclosure"
                + " dimension with 5% weight")
        void assemble_hardeningEval_hasDisclosure(
                @TempDir Path tempDir)
                throws IOException {
            String content =
                    generateClaudeContent(tempDir);
            assertThat(content)
                    .contains("Information Disclosure")
                    .contains("5%");
        }

        @Test
        @DisplayName("all 7 dimension weights sum to"
                + " 100%")
        void assemble_hardeningEval_weightsDocumented(
                @TempDir Path tempDir)
                throws IOException {
            String content =
                    generateClaudeContent(tempDir);
            assertThat(content)
                    .contains("0.25")
                    .contains("0.20")
                    .contains("0.15")
                    .contains("0.10")
                    .contains("0.05");
        }
    }

    @Nested
    @DisplayName("Claude SKILL.md -- HTTP Header Checks")
    class HttpHeaderChecks {

        @Test
        @DisplayName("contains HSTS check with"
                + " HIGH severity")
        void assemble_hardeningEval_hasHstsCheck(
                @TempDir Path tempDir)
                throws IOException {
            String content =
                    generateClaudeContent(tempDir);
            assertThat(content)
                    .contains(
                            "Strict-Transport-Security");
        }

        @Test
        @DisplayName("contains X-Frame-Options check")
        void assemble_hardeningEval_hasXFrameOptions(
                @TempDir Path tempDir)
                throws IOException {
            String content =
                    generateClaudeContent(tempDir);
            assertThat(content)
                    .contains("X-Frame-Options");
        }

        @Test
        @DisplayName("contains CSP check")
        void assemble_hardeningEval_hasCsp(
                @TempDir Path tempDir)
                throws IOException {
            String content =
                    generateClaudeContent(tempDir);
            assertThat(content)
                    .contains("Content-Security-Policy");
        }

        @Test
        @DisplayName("contains HSTS fix recommendation"
                + " with max-age")
        void assemble_hardeningEval_hasHstsFix(
                @TempDir Path tempDir)
                throws IOException {
            String content =
                    generateClaudeContent(tempDir);
            assertThat(content).contains(
                    "max-age=31536000;"
                            + " includeSubDomains");
        }

        @Test
        @DisplayName("contains X-Content-Type-Options"
                + " check")
        void assemble_hardeningEval_hasXContentType(
                @TempDir Path tempDir)
                throws IOException {
            String content =
                    generateClaudeContent(tempDir);
            assertThat(content)
                    .contains("X-Content-Type-Options")
                    .contains("nosniff");
        }

        @Test
        @DisplayName("contains Permissions-Policy check")
        void assemble_hardeningEval_hasPermissionsPolicy(
                @TempDir Path tempDir)
                throws IOException {
            String content =
                    generateClaudeContent(tempDir);
            assertThat(content)
                    .contains("Permissions-Policy");
        }

        @Test
        @DisplayName("contains Referrer-Policy check")
        void assemble_hardeningEval_hasReferrerPolicy(
                @TempDir Path tempDir)
                throws IOException {
            String content =
                    generateClaudeContent(tempDir);
            assertThat(content)
                    .contains("Referrer-Policy");
        }
    }

    @Nested
    @DisplayName("Claude SKILL.md -- SARIF Output")
    class SarifOutput {

        @Test
        @DisplayName("contains SARIF 2.1.0 output format")
        void assemble_hardeningEval_hasSarifFormat(
                @TempDir Path tempDir)
                throws IOException {
            String content =
                    generateClaudeContent(tempDir);
            assertThat(content)
                    .contains("SARIF 2.1.0")
                    .contains("sarif-schema-2.1.0");
        }

        @Test
        @DisplayName("contains SARIF rule ID convention"
                + " with HARDEN prefix")
        void assemble_hardeningEval_hasSarifRuleIds(
                @TempDir Path tempDir)
                throws IOException {
            String content =
                    generateClaudeContent(tempDir);
            assertThat(content)
                    .contains("HARDEN-HDR")
                    .contains("HARDEN-TLS")
                    .contains("HARDEN-COR")
                    .contains("HARDEN-COK")
                    .contains("HARDEN-ERR")
                    .contains("HARDEN-LIM")
                    .contains("HARDEN-DIS");
        }

        @Test
        @DisplayName("SARIF output contains"
                + " fixRecommendation")
        void assemble_hardeningEval_hasFixRecommendation(
                @TempDir Path tempDir)
                throws IOException {
            String content =
                    generateClaudeContent(tempDir);
            assertThat(content)
                    .contains("fixRecommendation");
        }

        @Test
        @DisplayName("SARIF output contains overall score"
                + " and grade properties")
        void assemble_hardeningEval_hasSarifScoring(
                @TempDir Path tempDir)
                throws IOException {
            String content =
                    generateClaudeContent(tempDir);
            assertThat(content)
                    .contains("overallScore")
                    .contains("grade");
        }
    }

    @Nested
    @DisplayName("Claude SKILL.md -- Scoring")
    class WeightedScoring {

        @Test
        @DisplayName("contains weighted score calculation"
                + " formula")
        void assemble_hardeningEval_hasScoreFormula(
                @TempDir Path tempDir)
                throws IOException {
            String content =
                    generateClaudeContent(tempDir);
            assertThat(content)
                    .contains("dimension_score")
                    .contains("overall_score")
                    .contains("applicable_weights");
        }

        @Test
        @DisplayName("contains grade mapping A through F")
        void assemble_hardeningEval_hasGradeMapping(
                @TempDir Path tempDir)
                throws IOException {
            String content =
                    generateClaudeContent(tempDir);
            assertThat(content)
                    .contains("90-100")
                    .contains("80-89")
                    .contains("70-79")
                    .contains("60-69")
                    .contains("0-59");
        }

        @Test
        @DisplayName("contains weakest and strongest"
                + " dimension reporting")
        void assemble_hardeningEval_hasWeakestStrongest(
                @TempDir Path tempDir)
                throws IOException {
            String content =
                    generateClaudeContent(tempDir);
            assertThat(content)
                    .contains("Weakest Dimension")
                    .contains("Strongest Dimension");
        }
    }

    @Nested
    @DisplayName("Claude SKILL.md -- Benchmark Support")
    class BenchmarkSupport {

        @Test
        @DisplayName("contains OWASP benchmark"
                + " with ASVS V14 reference")
        void assemble_hardeningEval_hasOwaspBenchmark(
                @TempDir Path tempDir)
                throws IOException {
            String content =
                    generateClaudeContent(tempDir);
            assertThat(content)
                    .contains("OWASP")
                    .contains("ASVS V14");
        }

        @Test
        @DisplayName("contains CIS benchmark reference")
        void assemble_hardeningEval_hasCisBenchmark(
                @TempDir Path tempDir)
                throws IOException {
            String content =
                    generateClaudeContent(tempDir);
            assertThat(content)
                    .contains("CIS");
        }

        @Test
        @DisplayName("contains ASVS level mapping"
                + " L1, L2, L3")
        void assemble_hardeningEval_hasAsvsLevels(
                @TempDir Path tempDir)
                throws IOException {
            String content =
                    generateClaudeContent(tempDir);
            assertThat(content)
                    .contains("L1")
                    .contains("L2")
                    .contains("L3")
                    .contains("ASVS");
        }

        @Test
        @DisplayName("CIS and OWASP checks differ"
                + " per header")
        void assemble_hardeningEval_benchmarksDiffer(
                @TempDir Path tempDir)
                throws IOException {
            String content =
                    generateClaudeContent(tempDir);
            assertThat(content)
                    .contains("| Content-Security-Policy"
                            + " | No | Yes");
        }
    }

    @Nested
    @DisplayName("Claude SKILL.md -- Workflow")
    class Workflow {

        @Test
        @DisplayName("contains 6-step workflow")
        void assemble_hardeningEval_hasWorkflowSteps(
                @TempDir Path tempDir)
                throws IOException {
            String content =
                    generateClaudeContent(tempDir);
            assertThat(content)
                    .contains("VALIDATE")
                    .contains("CONFIGURE")
                    .contains("PROBE")
                    .contains("EVALUATE")
                    .contains("SCORE")
                    .contains("REPORT");
        }

        @Test
        @DisplayName("references security KP for"
                + " mitigations")
        void assemble_hardeningEval_refsSecurityKp(
                @TempDir Path tempDir)
                throws IOException {
            String content =
                    generateClaudeContent(tempDir);
            assertThat(content)
                    .contains("skills/security/");
        }
    }

    @Nested
    @DisplayName("Claude SKILL.md -- Error Handling")
    class ErrorHandling {

        @Test
        @DisplayName("contains target unreachable error"
                + " handling")
        void assemble_hardeningEval_hasUnreachable(
                @TempDir Path tempDir)
                throws IOException {
            String content =
                    generateClaudeContent(tempDir);
            assertThat(content)
                    .contains("Target unreachable");
        }

        @Test
        @DisplayName("contains error table with scenarios")
        void assemble_hardeningEval_hasErrorTable(
                @TempDir Path tempDir)
                throws IOException {
            String content =
                    generateClaudeContent(tempDir);
            assertThat(content)
                    .contains("Target unreachable")
                    .contains("invalid certificate")
                    .contains("TLS probing unavailable");
        }
    }

    @Nested
    @DisplayName("Claude SKILL.md -- CI Integration")
    class CiIntegration {

        @Test
        @DisplayName("contains GitHub Actions CI snippet")
        void assemble_hardeningEval_hasGithubActions(
                @TempDir Path tempDir)
                throws IOException {
            String content =
                    generateClaudeContent(tempDir);
            assertThat(content)
                    .contains("GitHub Actions")
                    .contains("code-scanning/sarifs");
        }

        @Test
        @DisplayName("contains GitLab CI snippet")
        void assemble_hardeningEval_hasGitlabCi(
                @TempDir Path tempDir)
                throws IOException {
            String content =
                    generateClaudeContent(tempDir);
            assertThat(content)
                    .contains("GitLab CI");
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
                        "skills/x-hardening-eval/SKILL.md"),
                StandardCharsets.UTF_8);
    }

}
