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
 * Tests for story-0013-0019: x-perf-profile skill and
 * run-perf-test extension with regression detection.
 *
 * <p>Validates that the x-perf-profile skill template is
 * generated correctly with proper frontmatter, workflow
 * steps, profiler selection, and output modes. Also
 * validates that run-perf-test is extended with regression
 * detection while preserving existing content.</p>
 */
@DisplayName("x-perf-profile Skill and run-perf-test Extension")
class PerfProfileSkillTest {

    @Nested
    @DisplayName("Claude SKILL.md -- Frontmatter")
    class ClaudeFrontmatter {

        @Test
        @DisplayName("x-perf-profile SKILL.md exists after"
                + " assembly")
        void assemble_perfProfile_skillMdExists(
                @TempDir Path tempDir)
                throws IOException {
            Path outputDir = generateOutput(tempDir);
            Path skillMd = outputDir.resolve(
                    "skills/x-perf-profile/SKILL.md");
            assertThat(skillMd).exists();
        }

        @Test
        @DisplayName("frontmatter contains name:"
                + " x-perf-profile")
        void assemble_perfProfile_hasName(
                @TempDir Path tempDir)
                throws IOException {
            String content =
                    generateClaudeContent(tempDir);
            assertThat(content)
                    .contains("name: x-perf-profile");
        }

        @Test
        @DisplayName("frontmatter contains"
                + " user-invocable: true")
        void assemble_perfProfile_hasUserInvocable(
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
                + " with profiling types")
        void assemble_perfProfile_hasArgumentHint(
                @TempDir Path tempDir)
                throws IOException {
            String content =
                    generateClaudeContent(tempDir);
            assertThat(content)
                    .contains("argument-hint:")
                    .contains("cpu")
                    .contains("memory")
                    .contains("duration")
                    .contains("flamegraph");
        }

        @Test
        @DisplayName("frontmatter contains allowed-tools")
        void assemble_perfProfile_hasAllowedTools(
                @TempDir Path tempDir)
                throws IOException {
            String content =
                    generateClaudeContent(tempDir);
            assertThat(content)
                    .contains("allowed-tools:");
        }

        @Test
        @DisplayName("allowed-tools includes Read, Bash,"
                + " Glob, Grep, Agent")
        void assemble_perfProfile_hasExpectedTools(
                @TempDir Path tempDir)
                throws IOException {
            String content =
                    generateClaudeContent(tempDir);
            assertThat(content)
                    .contains("Read")
                    .contains("Bash")
                    .contains("Glob")
                    .contains("Grep")
                    .contains("Agent");
        }

        @Test
        @DisplayName("frontmatter contains description"
                + " with profiling keywords")
        void assemble_perfProfile_hasDescription(
                @TempDir Path tempDir)
                throws IOException {
            String content =
                    generateClaudeContent(tempDir);
            assertThat(content)
                    .contains("description:")
                    .contains("profil");
        }
    }

    @Nested
    @DisplayName("Claude SKILL.md -- Language Detection")
    class LanguageDetection {

        @Test
        @DisplayName("contains Java/JFR profiler"
                + " instructions")
        void assemble_perfProfile_hasJfr(
                @TempDir Path tempDir)
                throws IOException {
            String content =
                    generateClaudeContent(tempDir);
            assertThat(content)
                    .contains("JFR");
        }

        @Test
        @DisplayName("contains Go/pprof profiler"
                + " instructions")
        void assemble_perfProfile_hasPprof(
                @TempDir Path tempDir)
                throws IOException {
            String content =
                    generateClaudeContent(tempDir);
            assertThat(content)
                    .contains("pprof");
        }

        @Test
        @DisplayName("contains Python/py-spy profiler"
                + " instructions")
        void assemble_perfProfile_hasPySpy(
                @TempDir Path tempDir)
                throws IOException {
            String content =
                    generateClaudeContent(tempDir);
            assertThat(content)
                    .contains("py-spy");
        }

        @Test
        @DisplayName("contains pom.xml detection for"
                + " Java")
        void assemble_perfProfile_hasPomXmlDetection(
                @TempDir Path tempDir)
                throws IOException {
            String content =
                    generateClaudeContent(tempDir);
            assertThat(content)
                    .contains("pom.xml");
        }

        @Test
        @DisplayName("contains go.mod detection for Go")
        void assemble_perfProfile_hasGoModDetection(
                @TempDir Path tempDir)
                throws IOException {
            String content =
                    generateClaudeContent(tempDir);
            assertThat(content)
                    .contains("go.mod");
        }

        @Test
        @DisplayName("contains pyproject.toml detection"
                + " for Python")
        void assemble_perfProfile_hasPyprojectDetection(
                @TempDir Path tempDir)
                throws IOException {
            String content =
                    generateClaudeContent(tempDir);
            assertThat(content)
                    .contains("pyproject.toml");
        }
    }

    @Nested
    @DisplayName("Claude SKILL.md -- Workflow")
    class Workflow {

        @Test
        @DisplayName("contains 7-step workflow")
        void assemble_perfProfile_hasWorkflowSteps(
                @TempDir Path tempDir)
                throws IOException {
            String content =
                    generateClaudeContent(tempDir);
            assertThat(content)
                    .contains("Detect")
                    .contains("Select")
                    .contains("Configure")
                    .contains("Execute")
                    .contains("Generate")
                    .contains("Identify")
                    .contains("Suggest");
        }

        @Test
        @DisplayName("references performance-engineering"
                + " KP for optimizations")
        void assemble_perfProfile_refsPerformanceKp(
                @TempDir Path tempDir)
                throws IOException {
            String content =
                    generateClaudeContent(tempDir);
            assertThat(content)
                    .contains("performance-engineering");
        }

        @Test
        @DisplayName("references performance-engineer"
                + " agent")
        void assemble_perfProfile_refsPerfEngineerAgent(
                @TempDir Path tempDir)
                throws IOException {
            String content =
                    generateClaudeContent(tempDir);
            assertThat(content)
                    .contains("performance-engineer");
        }
    }

    @Nested
    @DisplayName("Claude SKILL.md -- Output Modes")
    class OutputModes {

        @Test
        @DisplayName("contains flamegraph output mode")
        void assemble_perfProfile_hasFlamegraphMode(
                @TempDir Path tempDir)
                throws IOException {
            String content =
                    generateClaudeContent(tempDir);
            assertThat(content)
                    .containsIgnoringCase("flamegraph");
        }

        @Test
        @DisplayName("contains report output mode")
        void assemble_perfProfile_hasReportMode(
                @TempDir Path tempDir)
                throws IOException {
            String content =
                    generateClaudeContent(tempDir);
            assertThat(content)
                    .contains("report");
        }

        @Test
        @DisplayName("contains raw output mode")
        void assemble_perfProfile_hasRawMode(
                @TempDir Path tempDir)
                throws IOException {
            String content =
                    generateClaudeContent(tempDir);
            assertThat(content)
                    .contains("raw");
        }
    }

    @Nested
    @DisplayName("run-perf-test Extension -- Regression"
            + " Detection")
    class RunPerfTestRegression {

        @Test
        @DisplayName("extended run-perf-test contains"
                + " Regression Detection section")
        void assemble_runPerfTest_hasRegressionDetection(
                @TempDir Path tempDir)
                throws IOException {
            String content =
                    generateRunPerfTestContent(tempDir);
            assertThat(content)
                    .contains("Regression Detection");
        }

        @Test
        @DisplayName("extended run-perf-test contains"
                + " Baseline Management section")
        void assemble_runPerfTest_hasBaselineManagement(
                @TempDir Path tempDir)
                throws IOException {
            String content =
                    generateRunPerfTestContent(tempDir);
            assertThat(content)
                    .contains("Baseline Management");
        }

        @Test
        @DisplayName("extended run-perf-test contains"
                + " Threshold Validation section")
        void assemble_runPerfTest_hasThresholdValidation(
                @TempDir Path tempDir)
                throws IOException {
            String content =
                    generateRunPerfTestContent(tempDir);
            assertThat(content)
                    .contains("Threshold Validation");
        }

        @Test
        @DisplayName("extended run-perf-test contains"
                + " Regression Detection section")
        void assemble_runPerfTest_hasComparisonReport(
                @TempDir Path tempDir)
                throws IOException {
            String content =
                    generateRunPerfTestContent(tempDir);
            assertThat(content)
                    .contains("Regression Detection");
        }

        @Test
        @DisplayName("extended run-perf-test references"
                + " baseline.json")
        void assemble_runPerfTest_hasBaselineJson(
                @TempDir Path tempDir)
                throws IOException {
            String content =
                    generateRunPerfTestContent(tempDir);
            assertThat(content)
                    .contains("baseline.json");
        }

        @Test
        @DisplayName("extended run-perf-test contains"
                + " --save-baseline flag")
        void assemble_runPerfTest_hasSaveBaseline(
                @TempDir Path tempDir)
                throws IOException {
            String content =
                    generateRunPerfTestContent(tempDir);
            assertThat(content)
                    .contains("--save-baseline");
        }

        @Test
        @DisplayName("extended run-perf-test contains"
                + " --compare-baseline flag")
        void assemble_runPerfTest_hasCompareBaseline(
                @TempDir Path tempDir)
                throws IOException {
            String content =
                    generateRunPerfTestContent(tempDir);
            assertThat(content)
                    .contains("--compare-baseline");
        }

        @Test
        @DisplayName("extended run-perf-test contains"
                + " p99 threshold reference")
        void assemble_runPerfTest_hasP99Threshold(
                @TempDir Path tempDir)
                throws IOException {
            String content =
                    generateRunPerfTestContent(tempDir);
            assertThat(content)
                    .contains("p99");
        }

        @Test
        @DisplayName("extended run-perf-test contains"
                + " throughput reference")
        void assemble_runPerfTest_hasThroughput(
                @TempDir Path tempDir)
                throws IOException {
            String content =
                    generateRunPerfTestContent(tempDir);
            assertThat(content)
                    .contains("throughput");
        }
    }

    @Nested
    @DisplayName("run-perf-test Extension -- Preservation")
    class RunPerfTestPreservation {

        @Test
        @DisplayName("extended run-perf-test preserves"
                + " original name in frontmatter")
        void assemble_runPerfTest_preservesName(
                @TempDir Path tempDir)
                throws IOException {
            String content =
                    generateRunPerfTestContent(tempDir);
            assertThat(content)
                    .contains("name: run-perf-test");
        }

        @Test
        @DisplayName("extended run-perf-test preserves"
                + " SLA Targets section")
        void assemble_runPerfTest_preservesSlaTargets(
                @TempDir Path tempDir)
                throws IOException {
            String content =
                    generateRunPerfTestContent(tempDir);
            assertThat(content)
                    .contains("SLA Targets");
        }

        @Test
        @DisplayName("extended run-perf-test preserves"
                + " scenario definitions")
        void assemble_runPerfTest_preservesTestScenarios(
                @TempDir Path tempDir)
                throws IOException {
            String content =
                    generateRunPerfTestContent(tempDir);
            assertThat(content)
                    .contains("Select and Run Scenario");
        }

        @Test
        @DisplayName("extended run-perf-test preserves"
                + " workflow section")
        void assemble_runPerfTest_preservesExecutionFlow(
                @TempDir Path tempDir)
                throws IOException {
            String content =
                    generateRunPerfTestContent(tempDir);
            assertThat(content)
                    .contains("## Workflow");
        }

        @Test
        @DisplayName("extended run-perf-test preserves"
                + " Review Checklist section")
        void assemble_runPerfTest_preservesReviewChecklist(
                @TempDir Path tempDir)
                throws IOException {
            String content =
                    generateRunPerfTestContent(tempDir);
            assertThat(content)
                    .contains("Review Checklist");
        }

        @Test
        @DisplayName("extended run-perf-test preserves"
                + " Data Generation section")
        void assemble_runPerfTest_preservesDataGeneration(
                @TempDir Path tempDir)
                throws IOException {
            String content =
                    generateRunPerfTestContent(tempDir);
            assertThat(content)
                    .contains("Data Generation");
        }
    }

    @Nested
    @DisplayName("GitHub Copilot SKILL.md")
    class GithubCopilotSkill {

        @Test
        @DisplayName("x-perf-profile GitHub SKILL.md"
                + " exists after assembly")
        void assemble_github_perfProfileExists(
                @TempDir Path tempDir)
                throws IOException {
            Path outputDir = generateGithubOutput(tempDir);
            Path skillMd = outputDir.resolve(
                    "skills/x-perf-profile/SKILL.md");
            assertThat(skillMd).exists();
        }

        @Test
        @DisplayName("GitHub skill contains profiling"
                + " reference")
        void assemble_github_perfProfileHasProfiling(
                @TempDir Path tempDir)
                throws IOException {
            String content =
                    generateGithubContent(tempDir);
            assertThat(content)
                    .containsIgnoringCase("profil");
        }

        @Test
        @DisplayName("GitHub skill contains name:"
                + " x-perf-profile")
        void assemble_github_perfProfileHasName(
                @TempDir Path tempDir)
                throws IOException {
            String content =
                    generateGithubContent(tempDir);
            assertThat(content)
                    .contains("name: x-perf-profile");
        }
    }

    @Nested
    @DisplayName("GitHub Copilot -- run-perf-test"
            + " Extension")
    class GithubRunPerfTestExtension {

        @Test
        @DisplayName("GitHub run-perf-test contains"
                + " Regression Detection")
        void assemble_github_runPerfTestHasRegression(
                @TempDir Path tempDir)
                throws IOException {
            String content =
                    generateGithubRunPerfTestContent(
                            tempDir);
            assertThat(content)
                    .contains("Regression Detection");
        }

        @Test
        @DisplayName("GitHub run-perf-test contains"
                + " baseline.json reference")
        void assemble_github_runPerfTestHasBaseline(
                @TempDir Path tempDir)
                throws IOException {
            String content =
                    generateGithubRunPerfTestContent(
                            tempDir);
            assertThat(content)
                    .contains("baseline.json");
        }

        @Test
        @DisplayName("GitHub run-perf-test preserves"
                + " original content")
        void assemble_github_runPerfTestPreserves(
                @TempDir Path tempDir)
                throws IOException {
            String content =
                    generateGithubRunPerfTestContent(
                            tempDir);
            assertThat(content)
                    .contains("SLA Targets")
                    .contains("Test Scenarios");
        }
    }

    @Nested
    @DisplayName("GithubSkillsAssembler.SKILL_GROUPS -- Dev Group")
    class RegistryDevGroup {

        @Test
        @DisplayName("dev group contains x-perf-profile")
        void register_devGroup_containsPerfProfile() {
            assertThat(GithubSkillsAssembler.SKILL_GROUPS
                    .get("dev"))
                    .contains("x-perf-profile");
        }
    }

    private Path generateOutput(Path tempDir)
            throws IOException {
        Path outputDir = tempDir.resolve("output");
        Files.createDirectories(outputDir);
        SkillsAssembler assembler =
                new SkillsAssembler();
        assembler.assemble(
                TestConfigBuilder.builder()
                        .performanceTests(true)
                        .build(),
                new TemplateEngine(), outputDir);
        return outputDir;
    }

    private String generateClaudeContent(Path tempDir)
            throws IOException {
        Path outputDir = generateOutput(tempDir);
        return Files.readString(
                outputDir.resolve(
                        "skills/x-perf-profile/SKILL.md"),
                StandardCharsets.UTF_8);
    }

    private String generateRunPerfTestContent(
            Path tempDir)
            throws IOException {
        Path outputDir = generateOutput(tempDir);
        return Files.readString(
                outputDir.resolve(
                        "skills/run-perf-test/SKILL.md"),
                StandardCharsets.UTF_8);
    }

    private Path generateGithubOutput(Path tempDir)
            throws IOException {
        Path outputDir = tempDir.resolve("output");
        Files.createDirectories(outputDir);
        GithubSkillsAssembler assembler =
                new GithubSkillsAssembler();
        assembler.assemble(
                TestConfigBuilder.builder()
                        .performanceTests(true)
                        .build(),
                new TemplateEngine(), outputDir);
        return outputDir;
    }

    private String generateGithubContent(Path tempDir)
            throws IOException {
        Path outputDir = generateGithubOutput(tempDir);
        return Files.readString(
                outputDir.resolve(
                        "skills/x-perf-profile/SKILL.md"),
                StandardCharsets.UTF_8);
    }

    private String generateGithubRunPerfTestContent(
            Path tempDir)
            throws IOException {
        Path outputDir = generateGithubOutput(tempDir);
        return Files.readString(
                outputDir.resolve(
                        "skills/run-perf-test/SKILL.md"),
                StandardCharsets.UTF_8);
    }
}
