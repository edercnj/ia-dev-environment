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
 * Tests for story-0013-0005: x-ci-generate skill for
 * interactive CI/CD pipeline generation.
 *
 * <p>Validates that the x-ci-generate skill template is
 * generated correctly with proper frontmatter, workflow
 * steps, stack detection, capabilities, and integration
 * notes referencing the ci-cd-patterns KP.</p>
 */
@DisplayName("x-ci-generate Skill")
class CiCdGenerateSkillTest {

    @Nested
    @DisplayName("Claude SKILL.md -- Frontmatter")
    class ClaudeFrontmatter {

        @Test
        @DisplayName("x-ci-generate SKILL.md exists"
                + " after assembly")
        void assemble_ciCdGenerate_skillMdExists(
                @TempDir Path tempDir)
                throws IOException {
            Path outputDir = generateOutput(tempDir);
            Path skillMd = outputDir.resolve(
                    "skills/x-ci-generate/SKILL.md");
            assertThat(skillMd).exists();
        }

        @Test
        @DisplayName("frontmatter contains name:"
                + " x-ci-generate")
        void assemble_ciCdGenerate_hasName(
                @TempDir Path tempDir)
                throws IOException {
            String content =
                    generateClaudeContent(tempDir);
            assertThat(content)
                    .contains("name: x-ci-generate");
        }

        @Test
        @DisplayName("frontmatter contains"
                + " user-invocable: true")
        void assemble_ciCdGenerate_hasUserInvocable(
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
                + " with pipeline types")
        void assemble_ciCdGenerate_hasArgumentHint(
                @TempDir Path tempDir)
                throws IOException {
            String content =
                    generateClaudeContent(tempDir);
            assertThat(content)
                    .contains("argument-hint:")
                    .contains("ci")
                    .contains("cd")
                    .contains("--monorepo")
                    .contains("--force");
        }

        @Test
        @DisplayName("frontmatter contains allowed-tools")
        void assemble_ciCdGenerate_hasAllowedTools(
                @TempDir Path tempDir)
                throws IOException {
            String content =
                    generateClaudeContent(tempDir);
            assertThat(content)
                    .contains("allowed-tools:");
        }

        @Test
        @DisplayName("allowed-tools includes Read, Write,"
                + " Edit, Glob, Grep, Bash, Agent")
        void assemble_ciCdGenerate_hasExpectedTools(
                @TempDir Path tempDir)
                throws IOException {
            String content =
                    generateClaudeContent(tempDir);
            assertThat(content)
                    .contains("Read")
                    .contains("Write")
                    .contains("Edit")
                    .contains("Glob")
                    .contains("Grep")
                    .contains("Bash")
                    .contains("Agent");
        }

        @Test
        @DisplayName("frontmatter contains description"
                + " with CI/CD keywords")
        void assemble_ciCdGenerate_hasDescription(
                @TempDir Path tempDir)
                throws IOException {
            String content =
                    generateClaudeContent(tempDir);
            assertThat(content)
                    .contains("description:")
                    .contains("CI/CD");
        }
    }

    @Nested
    @DisplayName("Claude SKILL.md -- Stack Detection")
    class StackDetection {

        @Test
        @DisplayName("contains pom.xml detection for"
                + " Java/Maven")
        void assemble_ciCdGenerate_hasPomXml(
                @TempDir Path tempDir)
                throws IOException {
            String content =
                    generateClaudeContent(tempDir);
            assertThat(content)
                    .contains("pom.xml");
        }

        @Test
        @DisplayName("contains package.json detection for"
                + " Node.js")
        void assemble_ciCdGenerate_hasPackageJson(
                @TempDir Path tempDir)
                throws IOException {
            String content =
                    generateClaudeContent(tempDir);
            assertThat(content)
                    .contains("package.json");
        }

        @Test
        @DisplayName("contains go.mod detection for Go")
        void assemble_ciCdGenerate_hasGoMod(
                @TempDir Path tempDir)
                throws IOException {
            String content =
                    generateClaudeContent(tempDir);
            assertThat(content)
                    .contains("go.mod");
        }

        @Test
        @DisplayName("contains Cargo.toml detection for"
                + " Rust")
        void assemble_ciCdGenerate_hasCargoToml(
                @TempDir Path tempDir)
                throws IOException {
            String content =
                    generateClaudeContent(tempDir);
            assertThat(content)
                    .contains("Cargo.toml");
        }

        @Test
        @DisplayName("contains pyproject.toml detection"
                + " for Python")
        void assemble_ciCdGenerate_hasPyprojectToml(
                @TempDir Path tempDir)
                throws IOException {
            String content =
                    generateClaudeContent(tempDir);
            assertThat(content)
                    .contains("pyproject.toml");
        }

        @Test
        @DisplayName("contains build.gradle detection for"
                + " Gradle")
        void assemble_ciCdGenerate_hasBuildGradle(
                @TempDir Path tempDir)
                throws IOException {
            String content =
                    generateClaudeContent(tempDir);
            assertThat(content)
                    .contains("build.gradle");
        }
    }

    @Nested
    @DisplayName("Claude SKILL.md -- Workflow")
    class Workflow {

        @Test
        @DisplayName("contains 5-step workflow")
        void assemble_ciCdGenerate_hasWorkflowSteps(
                @TempDir Path tempDir)
                throws IOException {
            String content =
                    generateClaudeContent(tempDir);
            assertThat(content)
                    .contains("DETECT")
                    .contains("ANALYZE")
                    .contains("GENERATE")
                    .contains("VALIDATE")
                    .contains("REPORT");
        }

        @Test
        @DisplayName("references ci-cd-patterns KP")
        void assemble_ciCdGenerate_refsCiCdPatternsKp(
                @TempDir Path tempDir)
                throws IOException {
            String content =
                    generateClaudeContent(tempDir);
            assertThat(content)
                    .contains("ci-cd-patterns");
        }

        @Test
        @DisplayName("references devops-engineer agent")
        void assemble_ciCdGenerate_refsDevopsAgent(
                @TempDir Path tempDir)
                throws IOException {
            String content =
                    generateClaudeContent(tempDir);
            assertThat(content)
                    .contains("devops-engineer");
        }
    }

    @Nested
    @DisplayName("Claude SKILL.md -- Capabilities")
    class Capabilities {

        @Test
        @DisplayName("contains CI pipeline generation")
        void assemble_ciCdGenerate_hasCiPipeline(
                @TempDir Path tempDir)
                throws IOException {
            String content =
                    generateClaudeContent(tempDir);
            assertThat(content)
                    .contains("ci.yml");
        }

        @Test
        @DisplayName("contains CD pipeline generation")
        void assemble_ciCdGenerate_hasCdPipeline(
                @TempDir Path tempDir)
                throws IOException {
            String content =
                    generateClaudeContent(tempDir);
            assertThat(content)
                    .contains("deploy-staging.yml")
                    .contains("deploy-production.yml")
                    .contains("rollback.yml");
        }

        @Test
        @DisplayName("contains release pipeline generation")
        void assemble_ciCdGenerate_hasReleasePipeline(
                @TempDir Path tempDir)
                throws IOException {
            String content =
                    generateClaudeContent(tempDir);
            assertThat(content)
                    .contains("release.yml");
        }

        @Test
        @DisplayName("contains security scan pipeline"
                + " generation")
        void assemble_ciCdGenerate_hasSecurityPipeline(
                @TempDir Path tempDir)
                throws IOException {
            String content =
                    generateClaudeContent(tempDir);
            assertThat(content)
                    .contains("security-scan.yml");
        }

        @Test
        @DisplayName("contains dependency audit pipeline"
                + " generation")
        void assemble_ciCdGenerate_hasDependencyAudit(
                @TempDir Path tempDir)
                throws IOException {
            String content =
                    generateClaudeContent(tempDir);
            assertThat(content)
                    .contains("dependency-audit.yml");
        }

        @Test
        @DisplayName("contains monorepo path-based"
                + " triggers")
        void assemble_ciCdGenerate_hasMonorepoTriggers(
                @TempDir Path tempDir)
                throws IOException {
            String content =
                    generateClaudeContent(tempDir);
            assertThat(content)
                    .contains("--monorepo")
                    .contains("paths:");
        }
    }

    @Nested
    @DisplayName("Claude SKILL.md -- actionlint Validation")
    class ActionlintValidation {

        @Test
        @DisplayName("contains actionlint validation"
                + " instructions")
        void assemble_ciCdGenerate_hasActionlint(
                @TempDir Path tempDir)
                throws IOException {
            String content =
                    generateClaudeContent(tempDir);
            assertThat(content)
                    .contains("actionlint");
        }

        @Test
        @DisplayName("contains actionlint install"
                + " instructions when not available")
        void assemble_ciCdGenerate_hasActionlintInstall(
                @TempDir Path tempDir)
                throws IOException {
            String content =
                    generateClaudeContent(tempDir);
            assertThat(content)
                    .contains("brew install actionlint");
        }
    }

    @Nested
    @DisplayName("Claude SKILL.md -- Error Handling")
    class ErrorHandling {

        @Test
        @DisplayName("contains --force conflict"
                + " resolution")
        void assemble_ciCdGenerate_hasForceConflict(
                @TempDir Path tempDir)
                throws IOException {
            String content =
                    generateClaudeContent(tempDir);
            assertThat(content)
                    .contains("--force");
        }

        @Test
        @DisplayName("contains error handling table")
        void assemble_ciCdGenerate_hasErrorHandling(
                @TempDir Path tempDir)
                throws IOException {
            String content =
                    generateClaudeContent(tempDir);
            assertThat(content)
                    .contains("Error Handling");
        }
    }

    @Nested
    @DisplayName("Claude SKILL.md -- Integration Notes")
    class IntegrationNotes {

        @Test
        @DisplayName("contains Integration Notes section")
        void assemble_ciCdGenerate_hasIntegrationNotes(
                @TempDir Path tempDir)
                throws IOException {
            String content =
                    generateClaudeContent(tempDir);
            assertThat(content)
                    .contains("Integration Notes");
        }

        @Test
        @DisplayName("references GitHub Actions workflow"
                + " syntax")
        void assemble_ciCdGenerate_hasGithubActions(
                @TempDir Path tempDir)
                throws IOException {
            String content =
                    generateClaudeContent(tempDir);
            assertThat(content)
                    .contains("GitHub Actions");
        }
    }

    @Nested
    @DisplayName("SkillGroupRegistry -- Dev Group")
    class RegistryDevGroup {

        @Test
        @DisplayName("dev group contains"
                + " x-ci-generate")
        void register_devGroup_containsCiCdGenerate() {
            assertThat(SkillGroupRegistry.SKILL_GROUPS
                    .get("dev"))
                    .contains("x-ci-generate");
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
                        "skills/x-ci-generate/SKILL.md"),
                StandardCharsets.UTF_8);
    }

}
