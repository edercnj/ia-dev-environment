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
 * Tests for story-0022-0020: x-security-pipeline skill
 * for CI/CD pipeline generation with conditional security
 * stages.
 *
 * <p>Validates that the x-security-pipeline skill template
 * is generated correctly with proper frontmatter, stage
 * definitions, platform support, composability, and
 * conditional stage evaluation.</p>
 */
@DisplayName("x-security-pipeline Skill")
class SecurityPipelineSkillTest {

    @Nested
    @DisplayName("Claude SKILL.md -- Frontmatter")
    class ClaudeFrontmatter {

        @Test
        @DisplayName("x-security-pipeline SKILL.md exists"
                + " after assembly")
        void assemble_securityPipeline_skillMdExists(
                @TempDir Path tempDir)
                throws IOException {
            Path outputDir = generateOutput(tempDir);
            Path skillMd = outputDir.resolve(
                    "skills/x-security-pipeline/SKILL.md");
            assertThat(skillMd).exists();
        }

        @Test
        @DisplayName("frontmatter contains name:"
                + " x-security-pipeline")
        void assemble_securityPipeline_hasName(
                @TempDir Path tempDir)
                throws IOException {
            String content =
                    generateClaudeContent(tempDir);
            assertThat(content)
                    .contains(
                            "name: x-security-pipeline");
        }

        @Test
        @DisplayName("frontmatter contains"
                + " user-invocable: true")
        void assemble_securityPipeline_hasUserInvocable(
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
                + " with pipeline options")
        void assemble_securityPipeline_hasArgumentHint(
                @TempDir Path tempDir)
                throws IOException {
            String content =
                    generateClaudeContent(tempDir);
            assertThat(content)
                    .contains("argument-hint:")
                    .contains("--ci")
                    .contains("--stages")
                    .contains("--trigger")
                    .contains("--fail-on-findings")
                    .contains("--severity-threshold");
        }

        @Test
        @DisplayName("frontmatter contains allowed-tools")
        void assemble_securityPipeline_hasAllowedTools(
                @TempDir Path tempDir)
                throws IOException {
            String content =
                    generateClaudeContent(tempDir);
            assertThat(content)
                    .contains("allowed-tools:");
        }

        @Test
        @DisplayName("frontmatter contains description"
                + " with security pipeline keywords")
        void assemble_securityPipeline_hasDescription(
                @TempDir Path tempDir)
                throws IOException {
            String content =
                    generateClaudeContent(tempDir);
            assertThat(content)
                    .contains("description:")
                    .contains("security stages");
        }
    }

    @Nested
    @DisplayName("Claude SKILL.md -- Security Stages")
    class SecurityStages {

        @Test
        @DisplayName("contains all 9 security stages"
                + " in order")
        void assemble_securityPipeline_hasAllStages(
                @TempDir Path tempDir)
                throws IOException {
            String content =
                    generateClaudeContent(tempDir);
            assertThat(content)
                    .contains("Secret Scan")
                    .contains("SAST")
                    .contains("Dependency Audit")
                    .contains("SonarQube")
                    .contains("Container Scan")
                    .contains("DAST Passive")
                    .contains("OWASP Scan")
                    .contains("Hardening Eval")
                    .contains("Quality Gate");
        }

        @Test
        @DisplayName("contains stage condition mappings")
        void assemble_securityPipeline_hasConditions(
                @TempDir Path tempDir)
                throws IOException {
            String content =
                    generateClaudeContent(tempDir);
            assertThat(content)
                    .contains(
                            "security.scanning.secrets")
                    .contains(
                            "security.scanning.sast")
                    .contains(
                            "security.scanning.sonar")
                    .contains(
                            "security.scanning.dast")
                    .contains(
                            "security.scanning.hardening")
                    .contains(
                            "infrastructure.container");
        }

        @Test
        @DisplayName("contains phase definitions"
                + " (pre-commit, build, deploy-staging,"
                + " gate)")
        void assemble_securityPipeline_hasPhases(
                @TempDir Path tempDir)
                throws IOException {
            String content =
                    generateClaudeContent(tempDir);
            assertThat(content)
                    .contains("pre-commit")
                    .contains("build")
                    .contains("deploy-staging")
                    .contains("gate");
        }

        @Test
        @DisplayName("dependency audit is always"
                + " enabled (baseline)")
        void assemble_securityPipeline_depAuditAlways(
                @TempDir Path tempDir)
                throws IOException {
            String content =
                    generateClaudeContent(tempDir);
            assertThat(content)
                    .contains("Always enabled");
        }

        @Test
        @DisplayName("contains minimal mode definition"
                + " with 3 stages")
        void assemble_securityPipeline_hasMinimalMode(
                @TempDir Path tempDir)
                throws IOException {
            String content =
                    generateClaudeContent(tempDir);
            assertThat(content)
                    .contains("minimal")
                    .contains("stages 1-3");
        }
    }

    @Nested
    @DisplayName("Claude SKILL.md -- CI Platforms")
    class CiPlatforms {

        @Test
        @DisplayName("contains GitHub Actions pipeline"
                + " template")
        void assemble_securityPipeline_hasGithubActions(
                @TempDir Path tempDir)
                throws IOException {
            String content =
                    generateClaudeContent(tempDir);
            assertThat(content)
                    .contains("GitHub Actions")
                    .contains(
                            ".github/workflows/security.yml");
        }

        @Test
        @DisplayName("contains GitLab CI pipeline"
                + " template")
        void assemble_securityPipeline_hasGitlabCi(
                @TempDir Path tempDir)
                throws IOException {
            String content =
                    generateClaudeContent(tempDir);
            assertThat(content)
                    .contains("GitLab CI")
                    .contains(
                            ".gitlab-ci-security.yml");
        }

        @Test
        @DisplayName("contains Azure DevOps pipeline"
                + " template")
        void assemble_securityPipeline_hasAzureDevOps(
                @TempDir Path tempDir)
                throws IOException {
            String content =
                    generateClaudeContent(tempDir);
            assertThat(content)
                    .contains("Azure DevOps")
                    .contains(
                            "azure-pipelines-security.yml");
        }

        @Test
        @DisplayName("GitHub Actions template uses"
                + " correct YAML syntax")
        void assemble_securityPipeline_githubYamlSyntax(
                @TempDir Path tempDir)
                throws IOException {
            String content =
                    generateClaudeContent(tempDir);
            assertThat(content)
                    .contains("runs-on:")
                    .contains("uses: actions/checkout@v4")
                    .contains("jobs:");
        }

        @Test
        @DisplayName("GitLab CI template uses"
                + " correct YAML syntax")
        void assemble_securityPipeline_gitlabYamlSyntax(
                @TempDir Path tempDir)
                throws IOException {
            String content =
                    generateClaudeContent(tempDir);
            assertThat(content)
                    .contains("stages:")
                    .contains("stage: pre-commit")
                    .contains("script:")
                    .contains("artifacts:");
        }

        @Test
        @DisplayName("Azure DevOps template uses"
                + " correct YAML syntax")
        void assemble_securityPipeline_azureYamlSyntax(
                @TempDir Path tempDir)
                throws IOException {
            String content =
                    generateClaudeContent(tempDir);
            assertThat(content)
                    .contains("vmImage:")
                    .contains("displayName:")
                    .contains("dependsOn:");
        }
    }

    @Nested
    @DisplayName("Claude SKILL.md -- Composability")
    class Composability {

        @Test
        @DisplayName("references atomic scanning skills"
                + " (RULE-011)")
        void assemble_securityPipeline_refsAtomicSkills(
                @TempDir Path tempDir)
                throws IOException {
            String content =
                    generateClaudeContent(tempDir);
            assertThat(content)
                    .contains("x-secret-scan")
                    .contains("x-sast-scan")
                    .contains("x-dependency-audit")
                    .contains("x-sonar-gate")
                    .contains("x-container-scan")
                    .contains("x-dast-scan")
                    .contains("x-owasp-scan")
                    .contains("x-hardening-eval");
        }

        @Test
        @DisplayName("contains RULE-011 composability"
                + " reference")
        void assemble_securityPipeline_hasRule011(
                @TempDir Path tempDir)
                throws IOException {
            String content =
                    generateClaudeContent(tempDir);
            assertThat(content)
                    .contains("RULE-011");
        }

        @Test
        @DisplayName("never duplicates scan logic")
        void assemble_securityPipeline_noDuplication(
                @TempDir Path tempDir)
                throws IOException {
            String content =
                    generateClaudeContent(tempDir);
            assertThat(content)
                    .contains(
                            "never duplicates their"
                                    + " scan logic");
        }
    }

    @Nested
    @DisplayName("Claude SKILL.md -- Workflow")
    class Workflow {

        @Test
        @DisplayName("contains 7-step workflow")
        void assemble_securityPipeline_hasWorkflowSteps(
                @TempDir Path tempDir)
                throws IOException {
            String content =
                    generateClaudeContent(tempDir);
            assertThat(content)
                    .contains("Read Configuration")
                    .contains("Evaluate Stage Conditions")
                    .contains("Select Stages")
                    .contains("Render Platform-Specific")
                    .contains("Validate Generated YAML")
                    .contains("Write Pipeline File")
                    .contains("Report");
        }

        @Test
        @DisplayName("references ci-cd-patterns KP")
        void assemble_securityPipeline_refsCiCdPatterns(
                @TempDir Path tempDir)
                throws IOException {
            String content =
                    generateClaudeContent(tempDir);
            assertThat(content)
                    .contains("ci-cd-patterns");
        }

        @Test
        @DisplayName("references ci-cd-generate skill")
        void assemble_securityPipeline_refsDevopsAgent(
                @TempDir Path tempDir)
                throws IOException {
            String content =
                    generateClaudeContent(tempDir);
            assertThat(content)
                    .contains("x-ci-cd-generate");
        }
    }

    @Nested
    @DisplayName("Claude SKILL.md -- Triggers")
    class Triggers {

        @Test
        @DisplayName("supports push trigger")
        void assemble_securityPipeline_hasPushTrigger(
                @TempDir Path tempDir)
                throws IOException {
            String content =
                    generateClaudeContent(tempDir);
            assertThat(content)
                    .contains("push");
        }

        @Test
        @DisplayName("supports pr trigger")
        void assemble_securityPipeline_hasPrTrigger(
                @TempDir Path tempDir)
                throws IOException {
            String content =
                    generateClaudeContent(tempDir);
            assertThat(content)
                    .contains("pull_request");
        }

        @Test
        @DisplayName("supports schedule trigger")
        void assemble_securityPipeline_hasScheduleTrigger(
                @TempDir Path tempDir)
                throws IOException {
            String content =
                    generateClaudeContent(tempDir);
            assertThat(content)
                    .contains("schedule")
                    .contains("cron:");
        }
    }

    @Nested
    @DisplayName(
            "Claude SKILL.md -- Severity and Findings")
    class SeverityAndFindings {

        @Test
        @DisplayName("contains fail-on-findings"
                + " configuration")
        void assemble_securityPipeline_hasFailOnFindings(
                @TempDir Path tempDir)
                throws IOException {
            String content =
                    generateClaudeContent(tempDir);
            assertThat(content)
                    .contains("fail-on-findings");
        }

        @Test
        @DisplayName("contains severity-threshold"
                + " configuration")
        void assemble_securityPipeline_hasSeverity(
                @TempDir Path tempDir)
                throws IOException {
            String content =
                    generateClaudeContent(tempDir);
            assertThat(content)
                    .contains("SEVERITY_THRESHOLD")
                    .contains("CRITICAL")
                    .contains("HIGH")
                    .contains("MEDIUM");
        }
    }

    @Nested
    @DisplayName("Claude SKILL.md -- Placeholders")
    class Placeholders {

        @Test
        @DisplayName("retains runtime placeholder"
                + " {{LANGUAGE}} for AI interpretation")
        void assemble_securityPipeline_retainsLanguage(
                @TempDir Path tempDir)
                throws IOException {
            String content =
                    generateClaudeContent(tempDir);
            assertThat(content)
                    .contains("{{LANGUAGE}}");
        }

        @Test
        @DisplayName("retains runtime placeholder"
                + " {{BUILD_TOOL}} for AI interpretation")
        void assemble_securityPipeline_retainsBuildTool(
                @TempDir Path tempDir)
                throws IOException {
            String content =
                    generateClaudeContent(tempDir);
            assertThat(content)
                    .contains("{{BUILD_TOOL}}");
        }

        @Test
        @DisplayName("retains runtime placeholder"
                + " {{PROJECT_NAME}} for AI"
                + " interpretation")
        void assemble_securityPipeline_retainsProject(
                @TempDir Path tempDir)
                throws IOException {
            String content =
                    generateClaudeContent(tempDir);
            assertThat(content)
                    .contains("{{PROJECT_NAME}}");
        }

        @Test
        @DisplayName("retains runtime placeholder"
                + " {{FRAMEWORK}} for AI interpretation")
        void assemble_securityPipeline_retainsFramework(
                @TempDir Path tempDir)
                throws IOException {
            String content =
                    generateClaudeContent(tempDir);
            assertThat(content)
                    .contains("{{FRAMEWORK}}");
        }
    }

    @Nested
    @DisplayName("Claude SKILL.md -- Error Handling")
    class ErrorHandling {

        @Test
        @DisplayName("contains error handling table")
        void assemble_securityPipeline_hasErrorHandling(
                @TempDir Path tempDir)
                throws IOException {
            String content =
                    generateClaudeContent(tempDir);
            assertThat(content)
                    .contains("Error Handling");
        }

        @Test
        @DisplayName("handles unknown CI platform")
        void assemble_securityPipeline_unknownPlatform(
                @TempDir Path tempDir)
                throws IOException {
            String content =
                    generateClaudeContent(tempDir);
            assertThat(content)
                    .contains("Unknown CI platform");
        }

        @Test
        @DisplayName("handles no SecurityConfig flags")
        void assemble_securityPipeline_noFlags(
                @TempDir Path tempDir)
                throws IOException {
            String content =
                    generateClaudeContent(tempDir);
            assertThat(content)
                    .contains("No SecurityConfig flags");
        }
    }

    @Nested
    @DisplayName("GitHub Copilot SKILL.md")
    class GithubCopilotSkill {

        @Test
        @DisplayName("x-security-pipeline GitHub"
                + " SKILL.md exists after assembly")
        void assemble_github_securityPipelineExists(
                @TempDir Path tempDir)
                throws IOException {
            Path outputDir =
                    generateGithubOutput(tempDir);
            Path skillMd = outputDir.resolve(
                    "skills/x-security-pipeline/SKILL.md");
            assertThat(skillMd).exists();
        }

        @Test
        @DisplayName("GitHub skill contains security"
                + " pipeline reference")
        void assemble_github_hasSecurityPipeline(
                @TempDir Path tempDir)
                throws IOException {
            String content =
                    generateGithubContent(tempDir);
            assertThat(content)
                    .contains("Security CI Pipeline");
        }

        @Test
        @DisplayName("GitHub skill contains name:"
                + " x-security-pipeline")
        void assemble_github_hasName(
                @TempDir Path tempDir)
                throws IOException {
            String content =
                    generateGithubContent(tempDir);
            assertThat(content)
                    .contains(
                            "name: x-security-pipeline");
        }

        @Test
        @DisplayName("GitHub skill references all"
                + " 3 CI platforms")
        void assemble_github_hasAllPlatforms(
                @TempDir Path tempDir)
                throws IOException {
            String content =
                    generateGithubContent(tempDir);
            assertThat(content)
                    .contains("GitHub Actions")
                    .contains("GitLab CI")
                    .contains("Azure DevOps");
        }

        @Test
        @DisplayName("GitHub skill references"
                + " RULE-011 composability")
        void assemble_github_hasRule011(
                @TempDir Path tempDir)
                throws IOException {
            String content =
                    generateGithubContent(tempDir);
            assertThat(content)
                    .contains("RULE-011");
        }
    }

    @Nested
    @DisplayName("SkillGroupRegistry -- Dev Group")
    class RegistryDevGroup {

        @Test
        @DisplayName("dev group contains"
                + " x-security-pipeline")
        void register_devGroup_containsSecPipeline() {
            assertThat(SkillGroupRegistry.SKILL_GROUPS
                    .get("dev"))
                    .contains("x-security-pipeline");
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
                        "skills/x-security-pipeline"
                                + "/SKILL.md"),
                StandardCharsets.UTF_8);
    }

    private Path generateGithubOutput(Path tempDir)
            throws IOException {
        Path outputDir = tempDir.resolve("output");
        Files.createDirectories(outputDir);
        GithubSkillsAssembler assembler =
                new GithubSkillsAssembler();
        assembler.assemble(
                TestConfigBuilder.minimal(),
                new TemplateEngine(), outputDir);
        return outputDir;
    }

    private String generateGithubContent(Path tempDir)
            throws IOException {
        Path outputDir = generateGithubOutput(tempDir);
        return Files.readString(
                outputDir.resolve(
                        "skills/x-security-pipeline"
                                + "/SKILL.md"),
                StandardCharsets.UTF_8);
    }
}
