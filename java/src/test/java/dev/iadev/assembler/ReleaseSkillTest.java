package dev.iadev.assembler;

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
 * Tests for story-0013-0014: x-release skill for
 * orchestrated release automation.
 *
 * <p>Validates that the x-release skill template is
 * generated correctly with proper frontmatter, workflow
 * steps, version detection logic, version file update
 * patterns, and integration references.</p>
 */
@DisplayName("x-release Skill")
class ReleaseSkillTest {

    @Nested
    @DisplayName("Claude SKILL.md -- Frontmatter")
    class ClaudeFrontmatter {

        @Test
        @DisplayName("x-release SKILL.md exists after"
                + " assembly")
        void assemble_release_skillMdExists(
                @TempDir Path tempDir)
                throws IOException {
            Path outputDir = generateOutput(tempDir);
            Path skillMd = outputDir.resolve(
                    "skills/x-release/SKILL.md");
            assertThat(skillMd).exists();
        }

        @Test
        @DisplayName("frontmatter contains name:"
                + " x-release")
        void assemble_release_hasName(
                @TempDir Path tempDir)
                throws IOException {
            String content =
                    generateClaudeContent(tempDir);
            assertThat(content)
                    .contains("name: x-release");
        }

        @Test
        @DisplayName("frontmatter contains"
                + " user-invocable: true")
        void assemble_release_hasUserInvocable(
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
                + " with version options")
        void assemble_release_hasArgumentHint(
                @TempDir Path tempDir)
                throws IOException {
            String content =
                    generateClaudeContent(tempDir);
            assertThat(content)
                    .contains("argument-hint:")
                    .contains("major")
                    .contains("minor")
                    .contains("patch")
                    .contains("--dry-run");
        }

        @Test
        @DisplayName("frontmatter contains allowed-tools")
        void assemble_release_hasAllowedTools(
                @TempDir Path tempDir)
                throws IOException {
            String content =
                    generateClaudeContent(tempDir);
            assertThat(content)
                    .contains("allowed-tools:");
        }

        @Test
        @DisplayName("allowed-tools includes Read, Write,"
                + " Edit, Bash, Glob, Grep, Agent")
        void assemble_release_hasExpectedTools(
                @TempDir Path tempDir)
                throws IOException {
            String content =
                    generateClaudeContent(tempDir);
            assertThat(content)
                    .contains("Read")
                    .contains("Write")
                    .contains("Edit")
                    .contains("Bash")
                    .contains("Glob")
                    .contains("Grep")
                    .contains("Agent");
        }

        @Test
        @DisplayName("frontmatter contains description"
                + " with release keywords")
        void assemble_release_hasDescription(
                @TempDir Path tempDir)
                throws IOException {
            String content =
                    generateClaudeContent(tempDir);
            assertThat(content)
                    .contains("description:")
                    .contains("release");
        }
    }

    @Nested
    @DisplayName("Claude SKILL.md -- Workflow")
    class WorkflowSteps {

        @Test
        @DisplayName("contains 8-step workflow")
        void assemble_release_hasEightWorkflowSteps(
                @TempDir Path tempDir)
                throws IOException {
            String content =
                    generateClaudeContent(tempDir);
            assertThat(content)
                    .contains("DETERMINE")
                    .contains("VALIDATE")
                    .contains("UPDATE")
                    .contains("CHANGELOG")
                    .contains("COMMIT")
                    .contains("TAG")
                    .contains("DRY-RUN")
                    .contains("PUBLISH");
        }

        @Test
        @DisplayName("references x-changelog for"
                + " changelog generation")
        void assemble_release_refsChangelog(
                @TempDir Path tempDir)
                throws IOException {
            String content =
                    generateClaudeContent(tempDir);
            assertThat(content)
                    .contains("x-changelog");
        }

        @Test
        @DisplayName("references x-git-push for"
                + " commit and tag patterns")
        void assemble_release_refsGitPush(
                @TempDir Path tempDir)
                throws IOException {
            String content =
                    generateClaudeContent(tempDir);
            assertThat(content)
                    .contains("x-git-push");
        }
    }

    @Nested
    @DisplayName("Claude SKILL.md -- Version Detection")
    class VersionDetection {

        @Test
        @DisplayName("contains Conventional Commits"
                + " auto-detection logic")
        void assemble_release_hasAutoDetection(
                @TempDir Path tempDir)
                throws IOException {
            String content =
                    generateClaudeContent(tempDir);
            assertThat(content)
                    .contains("Conventional Commits");
        }

        @Test
        @DisplayName("contains BREAKING CHANGE detection"
                + " for major bumps")
        void assemble_release_hasBreakingChangeDetection(
                @TempDir Path tempDir)
                throws IOException {
            String content =
                    generateClaudeContent(tempDir);
            assertThat(content)
                    .contains("BREAKING CHANGE");
        }

        @Test
        @DisplayName("contains feat detection for"
                + " minor bumps")
        void assemble_release_hasFeatDetection(
                @TempDir Path tempDir)
                throws IOException {
            String content =
                    generateClaudeContent(tempDir);
            assertThat(content)
                    .contains("feat");
        }

        @Test
        @DisplayName("contains SemVer reference")
        void assemble_release_hasSemVerReference(
                @TempDir Path tempDir)
                throws IOException {
            String content =
                    generateClaudeContent(tempDir);
            assertThat(content)
                    .contains("Semantic Versioning");
        }
    }

    @Nested
    @DisplayName("Claude SKILL.md -- Version File Patterns")
    class VersionFilePatterns {

        @Test
        @DisplayName("contains pom.xml pattern for Maven")
        void assemble_release_hasPomXml(
                @TempDir Path tempDir)
                throws IOException {
            String content =
                    generateClaudeContent(tempDir);
            assertThat(content)
                    .contains("pom.xml");
        }

        @Test
        @DisplayName("contains package.json pattern"
                + " for npm")
        void assemble_release_hasPackageJson(
                @TempDir Path tempDir)
                throws IOException {
            String content =
                    generateClaudeContent(tempDir);
            assertThat(content)
                    .contains("package.json");
        }

        @Test
        @DisplayName("contains Cargo.toml pattern"
                + " for Rust")
        void assemble_release_hasCargoToml(
                @TempDir Path tempDir)
                throws IOException {
            String content =
                    generateClaudeContent(tempDir);
            assertThat(content)
                    .contains("Cargo.toml");
        }

        @Test
        @DisplayName("contains pyproject.toml pattern"
                + " for Python")
        void assemble_release_hasPyprojectToml(
                @TempDir Path tempDir)
                throws IOException {
            String content =
                    generateClaudeContent(tempDir);
            assertThat(content)
                    .contains("pyproject.toml");
        }

        @Test
        @DisplayName("contains build.gradle pattern"
                + " for Gradle")
        void assemble_release_hasBuildGradle(
                @TempDir Path tempDir)
                throws IOException {
            String content =
                    generateClaudeContent(tempDir);
            assertThat(content)
                    .contains("build.gradle");
        }

        @Test
        @DisplayName("contains go module note for Go")
        void assemble_release_hasGoModule(
                @TempDir Path tempDir)
                throws IOException {
            String content =
                    generateClaudeContent(tempDir);
            assertThat(content)
                    .contains("Go");
        }
    }

    @Nested
    @DisplayName("Claude SKILL.md -- Dry-Run Mode")
    class DryRunMode {

        @Test
        @DisplayName("contains dry-run mode"
                + " documentation")
        void assemble_release_hasDryRunMode(
                @TempDir Path tempDir)
                throws IOException {
            String content =
                    generateClaudeContent(tempDir);
            assertThat(content)
                    .contains("dry-run")
                    .contains("--dry-run");
        }

        @Test
        @DisplayName("dry-run describes expected output")
        void assemble_release_dryRunDescribesOutput(
                @TempDir Path tempDir)
                throws IOException {
            String content =
                    generateClaudeContent(tempDir);
            assertThat(content)
                    .contains("plan");
        }
    }

    @Nested
    @DisplayName("Claude SKILL.md -- Pre-conditions")
    class PreConditions {

        @Test
        @DisplayName("validates uncommitted changes")
        void assemble_release_validatesUncommitted(
                @TempDir Path tempDir)
                throws IOException {
            String content =
                    generateClaudeContent(tempDir);
            assertThat(content)
                    .contains("uncommitted");
        }

        @Test
        @DisplayName("validates tests pass")
        void assemble_release_validatesTests(
                @TempDir Path tempDir)
                throws IOException {
            String content =
                    generateClaudeContent(tempDir);
            assertThat(content)
                    .contains("--skip-tests");
        }

        @Test
        @DisplayName("validates current branch")
        void assemble_release_validatesBranch(
                @TempDir Path tempDir)
                throws IOException {
            String content =
                    generateClaudeContent(tempDir);
            assertThat(content)
                    .contains("main");
        }
    }

    @Nested
    @DisplayName("Claude SKILL.md -- Integration")
    class Integration {

        @Test
        @DisplayName("references release-management KP")
        void assemble_release_refsReleaseManagementKp(
                @TempDir Path tempDir)
                throws IOException {
            String content =
                    generateClaudeContent(tempDir);
            assertThat(content)
                    .contains("release-management");
        }

        @Test
        @DisplayName("references release checklist"
                + " template")
        void assemble_release_refsReleaseChecklist(
                @TempDir Path tempDir)
                throws IOException {
            String content =
                    generateClaudeContent(tempDir);
            assertThat(content).satisfiesAnyOf(
                    c -> assertThat(c).contains(
                            "release-checklist"),
                    c -> assertThat(c).contains(
                            "RELEASE-CHECKLIST"));
        }

        @Test
        @DisplayName("contains --no-publish flag")
        void assemble_release_hasNoPublishFlag(
                @TempDir Path tempDir)
                throws IOException {
            String content =
                    generateClaudeContent(tempDir);
            assertThat(content)
                    .contains("--no-publish");
        }

        @Test
        @DisplayName("release commit follows"
                + " Conventional Commits")
        void assemble_release_hasReleaseCommitFormat(
                @TempDir Path tempDir)
                throws IOException {
            String content =
                    generateClaudeContent(tempDir);
            assertThat(content)
                    .contains("release: v");
        }

        @Test
        @DisplayName("annotated tag with v prefix")
        void assemble_release_hasAnnotatedTag(
                @TempDir Path tempDir)
                throws IOException {
            String content =
                    generateClaudeContent(tempDir);
            assertThat(content)
                    .contains("git tag -a");
        }
    }

    @Nested
    @DisplayName("GitHub Copilot SKILL.md")
    class GithubCopilotSkill {

        @Test
        @DisplayName("x-release GitHub SKILL.md"
                + " exists after assembly")
        void assemble_github_releaseExists(
                @TempDir Path tempDir)
                throws IOException {
            Path outputDir = generateGithubOutput(tempDir);
            Path skillMd = outputDir.resolve(
                    "skills/x-release/SKILL.md");
            assertThat(skillMd).exists();
        }

        @Test
        @DisplayName("GitHub skill contains release"
                + " reference")
        void assemble_github_releaseHasRelease(
                @TempDir Path tempDir)
                throws IOException {
            String content =
                    generateGithubContent(tempDir);
            assertThat(content)
                    .containsIgnoringCase("release");
        }

        @Test
        @DisplayName("GitHub skill contains name:"
                + " x-release")
        void assemble_github_releaseHasName(
                @TempDir Path tempDir)
                throws IOException {
            String content =
                    generateGithubContent(tempDir);
            assertThat(content)
                    .contains("name: x-release");
        }
    }

    @Nested
    @DisplayName("SkillGroupRegistry --"
            + " git-troubleshooting Group")
    class RegistryGitTroubleshootingGroup {

        @Test
        @DisplayName("git-troubleshooting group"
                + " contains x-release")
        void register_gitGroup_containsRelease() {
            assertThat(SkillGroupRegistry.SKILL_GROUPS
                    .get("git-troubleshooting"))
                    .contains("x-release");
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
                        "skills/x-release/SKILL.md"),
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
                        "skills/x-release/SKILL.md"),
                StandardCharsets.UTF_8);
    }
}
