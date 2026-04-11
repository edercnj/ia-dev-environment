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
 * Tests for story-0022-0013: x-runtime-protection skill
 * for runtime protection evaluation.
 *
 * <p>Validates that the x-runtime-protection skill template
 * is generated correctly with proper frontmatter, 7
 * evaluation dimensions, 3 intensity levels, ASVS mapping,
 * SARIF output, and scoring model.</p>
 */
@DisplayName("x-runtime-protection Skill")
class RuntimeProtectionSkillTest {

    @Nested
    @DisplayName("Claude SKILL.md -- Frontmatter")
    class ClaudeFrontmatter {

        @Test
        @DisplayName("x-runtime-protection SKILL.md exists"
                + " after assembly")
        void assemble_runtimeProtection_skillMdExists(
                @TempDir Path tempDir)
                throws IOException {
            Path outputDir = generateOutput(tempDir);
            Path skillMd = outputDir.resolve(
                    "skills/x-runtime-protection/SKILL.md");
            assertThat(skillMd).exists();
        }

        @Test
        @DisplayName("frontmatter contains name:"
                + " x-runtime-protection")
        void assemble_runtimeProtection_hasName(
                @TempDir Path tempDir)
                throws IOException {
            String content =
                    generateClaudeContent(tempDir);
            assertThat(content).contains(
                    "name: x-runtime-protection");
        }

        @Test
        @DisplayName("frontmatter contains"
                + " user-invocable: true")
        void assemble_runtimeProtection_hasUserInvocable(
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
        void assemble_runtimeProtection_hasArgumentHint(
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
        void assemble_runtimeProtection_hasAllowedTools(
                @TempDir Path tempDir)
                throws IOException {
            String content =
                    generateClaudeContent(tempDir);
            assertThat(content)
                    .contains("allowed-tools:");
        }

        @Test
        @DisplayName("allowed-tools includes Read, Write,"
                + " Bash, Glob, Grep")
        void assemble_runtimeProtection_hasExpectedTools(
                @TempDir Path tempDir)
                throws IOException {
            String content =
                    generateClaudeContent(tempDir);
            assertThat(content)
                    .contains("Read")
                    .contains("Write")
                    .contains("Bash")
                    .contains("Glob")
                    .contains("Grep");
        }

        @Test
        @DisplayName("frontmatter contains description"
                + " with runtime protection")
        void assemble_runtimeProtection_hasDescription(
                @TempDir Path tempDir)
                throws IOException {
            String content =
                    generateClaudeContent(tempDir);
            assertThat(content)
                    .contains("description:")
                    .contains("runtime protection");
        }
    }

    @Nested
    @DisplayName("Claude SKILL.md -- Evaluation Dimensions")
    class EvaluationDimensions {

        @Test
        @DisplayName("contains Rate Limiting dimension")
        void assemble_runtimeProtection_hasRateLimiting(
                @TempDir Path tempDir)
                throws IOException {
            String content =
                    generateClaudeContent(tempDir);
            assertThat(content)
                    .contains("Rate Limiting");
        }

        @Test
        @DisplayName("contains WAF Rules dimension")
        void assemble_runtimeProtection_hasWafRules(
                @TempDir Path tempDir)
                throws IOException {
            String content =
                    generateClaudeContent(tempDir);
            assertThat(content)
                    .contains("WAF Rules");
        }

        @Test
        @DisplayName("contains Bot Protection dimension")
        void assemble_runtimeProtection_hasBotProtection(
                @TempDir Path tempDir)
                throws IOException {
            String content =
                    generateClaudeContent(tempDir);
            assertThat(content)
                    .contains("Bot Protection");
        }

        @Test
        @DisplayName("contains Account Lockout dimension")
        void assemble_runtimeProtection_hasAccountLockout(
                @TempDir Path tempDir)
                throws IOException {
            String content =
                    generateClaudeContent(tempDir);
            assertThat(content)
                    .contains("Account Lockout");
        }

        @Test
        @DisplayName("contains Brute Force dimension")
        void assemble_runtimeProtection_hasBruteForce(
                @TempDir Path tempDir)
                throws IOException {
            String content =
                    generateClaudeContent(tempDir);
            assertThat(content)
                    .contains("Brute Force");
        }

        @Test
        @DisplayName("contains CSP Enforcement dimension")
        void assemble_runtimeProtection_hasCspEnforcement(
                @TempDir Path tempDir)
                throws IOException {
            String content =
                    generateClaudeContent(tempDir);
            assertThat(content)
                    .contains("CSP Enforcement");
        }

        @Test
        @DisplayName("contains Permissions Policy dimension")
        void assemble_runtimeProtection_hasPermissionsPolicy(
                @TempDir Path tempDir)
                throws IOException {
            String content =
                    generateClaudeContent(tempDir);
            assertThat(content)
                    .contains("Permissions Policy");
        }
    }

    @Nested
    @DisplayName("Claude SKILL.md -- Intensity Levels")
    class IntensityLevels {

        @Test
        @DisplayName("contains passive intensity level")
        void assemble_runtimeProtection_hasPassive(
                @TempDir Path tempDir)
                throws IOException {
            String content =
                    generateClaudeContent(tempDir);
            assertThat(content)
                    .contains("passive");
        }

        @Test
        @DisplayName("contains moderate intensity level")
        void assemble_runtimeProtection_hasModerate(
                @TempDir Path tempDir)
                throws IOException {
            String content =
                    generateClaudeContent(tempDir);
            assertThat(content)
                    .contains("moderate");
        }

        @Test
        @DisplayName("contains aggressive intensity level")
        void assemble_runtimeProtection_hasAggressive(
                @TempDir Path tempDir)
                throws IOException {
            String content =
                    generateClaudeContent(tempDir);
            assertThat(content)
                    .contains("aggressive");
        }

        @Test
        @DisplayName("aggressive intensity restricted to"
                + " local/dev environments")
        void assemble_runtimeProtection_aggressiveRestricted(
                @TempDir Path tempDir)
                throws IOException {
            String content =
                    generateClaudeContent(tempDir);
            assertThat(content).contains(
                    "Aggressive intensity not allowed"
                            + " in production");
        }
    }

    @Nested
    @DisplayName("Claude SKILL.md -- ASVS Mapping")
    class AsvsMapping {

        @Test
        @DisplayName("rate limiting maps to ASVS V4.3")
        void assemble_runtimeProtection_rateLimitAsvs(
                @TempDir Path tempDir)
                throws IOException {
            String content =
                    generateClaudeContent(tempDir);
            assertThat(content).contains("V4.3");
        }

        @Test
        @DisplayName("WAF maps to ASVS V5.1")
        void assemble_runtimeProtection_wafAsvs(
                @TempDir Path tempDir)
                throws IOException {
            String content =
                    generateClaudeContent(tempDir);
            assertThat(content).contains("V5.1");
        }

        @Test
        @DisplayName("account lockout maps to ASVS V2.2")
        void assemble_runtimeProtection_lockoutAsvs(
                @TempDir Path tempDir)
                throws IOException {
            String content =
                    generateClaudeContent(tempDir);
            assertThat(content).contains("V2.2");
        }

        @Test
        @DisplayName("brute force maps to ASVS V11.1")
        void assemble_runtimeProtection_bruteForceAsvs(
                @TempDir Path tempDir)
                throws IOException {
            String content =
                    generateClaudeContent(tempDir);
            assertThat(content).contains("V11.1");
        }

        @Test
        @DisplayName("CSP and permissions map to ASVS V14.4")
        void assemble_runtimeProtection_cspAsvs(
                @TempDir Path tempDir)
                throws IOException {
            String content =
                    generateClaudeContent(tempDir);
            assertThat(content).contains("V14.4");
        }

        @Test
        @DisplayName("contains ASVS Mapping Summary table")
        void assemble_runtimeProtection_hasAsvsTable(
                @TempDir Path tempDir)
                throws IOException {
            String content =
                    generateClaudeContent(tempDir);
            assertThat(content)
                    .contains("ASVS Mapping Summary");
        }
    }

    @Nested
    @DisplayName("Claude SKILL.md -- SARIF Output")
    class SarifOutput {

        @Test
        @DisplayName("references SARIF 2.1.0 schema")
        void assemble_runtimeProtection_hasSarifSchema(
                @TempDir Path tempDir)
                throws IOException {
            String content =
                    generateClaudeContent(tempDir);
            assertThat(content).contains("sarif-schema-2.1.0");
        }

        @Test
        @DisplayName("SARIF output path follows convention")
        void assemble_runtimeProtection_hasSarifPath(
                @TempDir Path tempDir)
                throws IOException {
            String content =
                    generateClaudeContent(tempDir);
            assertThat(content).contains(
                    "results/security/"
                            + "runtime-protection-");
        }

        @Test
        @DisplayName("SARIF contains tool driver name")
        void assemble_runtimeProtection_hasSarifToolName(
                @TempDir Path tempDir)
                throws IOException {
            String content =
                    generateClaudeContent(tempDir);
            assertThat(content).contains(
                    "\"name\": \"x-runtime-protection\"");
        }

        @Test
        @DisplayName("SARIF contains RTPROT rule IDs")
        void assemble_runtimeProtection_hasSarifRuleIds(
                @TempDir Path tempDir)
                throws IOException {
            String content =
                    generateClaudeContent(tempDir);
            assertThat(content).contains("RTPROT-001");
        }

        @Test
        @DisplayName("SARIF contains fix-recommendation"
                + " property")
        void assemble_runtimeProtection_hasFixRec(
                @TempDir Path tempDir)
                throws IOException {
            String content =
                    generateClaudeContent(tempDir);
            assertThat(content)
                    .contains("fix-recommendation");
        }
    }

    @Nested
    @DisplayName("Claude SKILL.md -- Scoring Model")
    class ScoringModel {

        @Test
        @DisplayName("contains PROTECTED status")
        void assemble_runtimeProtection_hasProtected(
                @TempDir Path tempDir)
                throws IOException {
            String content =
                    generateClaudeContent(tempDir);
            assertThat(content).contains("PROTECTED");
        }

        @Test
        @DisplayName("contains PARTIAL status")
        void assemble_runtimeProtection_hasPartial(
                @TempDir Path tempDir)
                throws IOException {
            String content =
                    generateClaudeContent(tempDir);
            assertThat(content).contains("PARTIAL");
        }

        @Test
        @DisplayName("contains UNPROTECTED status")
        void assemble_runtimeProtection_hasUnprotected(
                @TempDir Path tempDir)
                throws IOException {
            String content =
                    generateClaudeContent(tempDir);
            assertThat(content).contains("UNPROTECTED");
        }

        @Test
        @DisplayName("contains SKIPPED status")
        void assemble_runtimeProtection_hasSkipped(
                @TempDir Path tempDir)
                throws IOException {
            String content =
                    generateClaudeContent(tempDir);
            assertThat(content).contains("SKIPPED");
        }

        @Test
        @DisplayName("contains grade A through F")
        void assemble_runtimeProtection_hasGrades(
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
        @DisplayName("contains severity weights")
        void assemble_runtimeProtection_hasSeverityWeights(
                @TempDir Path tempDir)
                throws IOException {
            String content =
                    generateClaudeContent(tempDir);
            assertThat(content)
                    .contains("CRITICAL=10")
                    .contains("HIGH=5")
                    .contains("MEDIUM=2")
                    .contains("LOW=1");
        }
    }

    @Nested
    @DisplayName("Claude SKILL.md -- Workflow")
    class WorkflowSteps {

        @Test
        @DisplayName("contains 6-step workflow")
        void assemble_runtimeProtection_hasWorkflow(
                @TempDir Path tempDir)
                throws IOException {
            String content =
                    generateClaudeContent(tempDir);
            assertThat(content)
                    .contains("Validate Parameters")
                    .contains("Configure Intensity")
                    .contains("Discover Baseline")
                    .contains("Evaluate Dimensions")
                    .contains("Score Results")
                    .contains("Generate Reports");
        }

        @Test
        @DisplayName("references security KP")
        void assemble_runtimeProtection_refsSecurityKp(
                @TempDir Path tempDir)
                throws IOException {
            String content =
                    generateClaudeContent(tempDir);
            assertThat(content)
                    .contains("| security |");
        }

        @Test
        @DisplayName("references OWASP ASVS knowledge pack")
        void assemble_runtimeProtection_refsAsvsKp(
                @TempDir Path tempDir)
                throws IOException {
            String content =
                    generateClaudeContent(tempDir);
            assertThat(content).contains("ASVS");
        }
    }

    @Nested
    @DisplayName("Claude SKILL.md -- Error Handling")
    class ErrorHandling {

        @Test
        @DisplayName("contains target unreachable error")
        void assemble_runtimeProtection_hasUnreachable(
                @TempDir Path tempDir)
                throws IOException {
            String content =
                    generateClaudeContent(tempDir);
            assertThat(content)
                    .contains("Target unreachable");
        }

        @Test
        @DisplayName("contains aggressive-in-production"
                + " downgrade")
        void assemble_runtimeProtection_hasEnvRestriction(
                @TempDir Path tempDir)
                throws IOException {
            String content =
                    generateClaudeContent(tempDir);
            assertThat(content).contains(
                    "Downgrade to passive");
        }

        @Test
        @DisplayName("contains login endpoint missing"
                + " handling")
        void assemble_runtimeProtection_hasLoginMissing(
                @TempDir Path tempDir)
                throws IOException {
            String content =
                    generateClaudeContent(tempDir);
            assertThat(content).contains(
                    "Login endpoint");
        }
    }

    @Nested
    @DisplayName("SkillGroupRegistry -- Review Group")
    class RegistryReviewGroup {

        @Test
        @DisplayName("review group contains"
                + " x-runtime-protection")
        void register_reviewGroup_containsRuntimeProtection() {
            assertThat(SkillGroupRegistry.SKILL_GROUPS
                    .get("review"))
                    .contains("x-runtime-protection");
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
                        "skills/x-runtime-protection"
                                + "/SKILL.md"),
                StandardCharsets.UTF_8);
    }

}
