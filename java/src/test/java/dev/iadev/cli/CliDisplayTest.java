package dev.iadev.cli;

import dev.iadev.domain.model.PipelineResult;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link CliDisplay}.
 *
 * <p>Tests follow TPP ordering:
 * classifyFiles (empty, single, multiple) ->
 * formatSummaryTable (empty, single, multiple) ->
 * formatResult (success, dry-run, warnings).
 */
@DisplayName("CliDisplay")
class CliDisplayTest {

    @Nested
    @DisplayName("classifyFiles")
    class ClassifyFiles {

        @Test
        void emptyList_whenCalled_returnsEmptyMap() {
            Map<String, List<String>> result =
                    CliDisplay.classifyFiles(List.of());

            assertThat(result).isEmpty();
        }

        @Test
        void rulesPath_whenCalled_classifiedAsRules() {
            Map<String, List<String>> result =
                    CliDisplay.classifyFiles(
                            List.of(".claude/rules/01-identity.md"));

            assertThat(result).containsKey("Rules");
            assertThat(result.get("Rules"))
                    .containsExactly(
                            ".claude/rules/01-identity.md");
        }

        @Test
        void skillsPath_whenCalled_classifiedAsSkills() {
            Map<String, List<String>> result =
                    CliDisplay.classifyFiles(
                            List.of(".claude/skills/x-dev/SKILL.md"));

            assertThat(result).containsKey("Skills");
            assertThat(result.get("Skills"))
                    .containsExactly(
                            ".claude/skills/x-dev/SKILL.md");
        }

        @Test
        void agentsPath_whenCalled_classifiedAsAgents() {
            Map<String, List<String>> result =
                    CliDisplay.classifyFiles(
                            List.of(".claude/agents/architect.md"));

            assertThat(result).containsKey("Agents");
            assertThat(result.get("Agents"))
                    .containsExactly(
                            ".claude/agents/architect.md");
        }

        @Test
        void hooksPath_whenCalled_classifiedAsHooks() {
            Map<String, List<String>> result =
                    CliDisplay.classifyFiles(
                            List.of(".claude/hooks/post-compile.sh"));

            assertThat(result).containsKey("Hooks");
            assertThat(result.get("Hooks"))
                    .containsExactly(
                            ".claude/hooks/post-compile.sh");
        }

        @Test
        void settingsPath_whenCalled_classifiedAsSettings() {
            Map<String, List<String>> result =
                    CliDisplay.classifyFiles(
                            List.of(".claude/settings.json"));

            assertThat(result).containsKey("Settings");
            assertThat(result.get("Settings"))
                    .containsExactly(".claude/settings.json");
        }

        @Test
        void githubInstructionsPath_whenCalled_classifiedCorrectly() {
            Map<String, List<String>> result =
                    CliDisplay.classifyFiles(List.of(
                            ".github/instructions/coding.instructions.md"));

            assertThat(result)
                    .containsKey("GitHub Instructions");
        }

        @Test
        void githubSkillsPath_whenCalled_classifiedCorrectly() {
            Map<String, List<String>> result =
                    CliDisplay.classifyFiles(List.of(
                            ".github/skills/dev/SKILL.md"));

            assertThat(result).containsKey("GitHub Skills");
        }

        @Test
        void githubAgentsPath_whenCalled_classifiedCorrectly() {
            Map<String, List<String>> result =
                    CliDisplay.classifyFiles(List.of(
                            ".github/agents/architect.agent.md"));

            assertThat(result).containsKey("GitHub Agents");
        }

        @Test
        void githubHooksPath_whenCalled_classifiedCorrectly() {
            Map<String, List<String>> result =
                    CliDisplay.classifyFiles(List.of(
                            ".github/hooks/post-push.json"));

            assertThat(result).containsKey("GitHub Hooks");
        }

        @Test
        void githubPromptsPath_whenCalled_classifiedCorrectly() {
            Map<String, List<String>> result =
                    CliDisplay.classifyFiles(List.of(
                            ".github/prompts/review.prompt.md"));

            assertThat(result).containsKey("GitHub Prompts");
        }

        @Test
        void githubCopilotConfig_whenCalled_classifiedCorrectly() {
            Map<String, List<String>> result =
                    CliDisplay.classifyFiles(List.of(
                            ".github/copilot-instructions.md"));

            assertThat(result).containsKey("GitHub Config");
        }

        @Test
        void codexPath_whenCalled_classifiedCorrectly() {
            Map<String, List<String>> result =
                    CliDisplay.classifyFiles(
                            List.of(".codex/config.toml"));

            assertThat(result).containsKey("Codex");
        }

        @Test
        void agentsMdPath_whenCalled_classifiedCorrectly() {
            Map<String, List<String>> result =
                    CliDisplay.classifyFiles(
                            List.of(".agents/skills/dev/SKILL.md"));

            assertThat(result).containsKey("Agents MD");
        }

        @Test
        void adrPath_whenCalled_classifiedCorrectly() {
            Map<String, List<String>> result =
                    CliDisplay.classifyFiles(
                            List.of("adr/001.md"));

            assertThat(result).containsKey("ADR");
        }

        @Test
        void plansPath_whenCalled_classifiedCorrectly() {
            Map<String, List<String>> result =
                    CliDisplay.classifyFiles(
                            List.of("plans/epic-0001/EPIC-0001.md"));

            assertThat(result).containsKey("Plans");
        }

        @Test
        void steeringPath_whenCalled_classifiedCorrectly() {
            Map<String, List<String>> result =
                    CliDisplay.classifyFiles(
                            List.of("steering/product.md"));

            assertThat(result).containsKey("Steering");
        }

        @Test
        void specsPath_whenCalled_classifiedCorrectly() {
            Map<String, List<String>> result =
                    CliDisplay.classifyFiles(
                            List.of("specs/SPEC-001.md"));

            assertThat(result).containsKey("Specs");
        }

        @Test
        void resultsPath_whenCalled_classifiedCorrectly() {
            Map<String, List<String>> result =
                    CliDisplay.classifyFiles(
                            List.of("results/audits/audit.md"));

            assertThat(result).containsKey("Results");
        }

        @Test
        void contractsPath_whenCalled_classifiedCorrectly() {
            Map<String, List<String>> result =
                    CliDisplay.classifyFiles(
                            List.of("contracts/api/grpc.md"));

            assertThat(result).containsKey("Contracts");
        }

        @Test
        void claudeMd_whenCalled_classifiedAsRootFiles() {
            Map<String, List<String>> result =
                    CliDisplay.classifyFiles(
                            List.of("CLAUDE.md"));

            assertThat(result).containsKey("Root Files");
        }

        @Test
        void readmeMd_whenCalled_classifiedAsRootFiles() {
            Map<String, List<String>> result =
                    CliDisplay.classifyFiles(
                            List.of("README.md"));

            assertThat(result).containsKey("Root Files");
        }

        @Test
        void unknownPath_whenCalled_classifiedAsOther() {
            Map<String, List<String>> result =
                    CliDisplay.classifyFiles(
                            List.of("random/file.txt"));

            assertThat(result).containsKey("Other");
        }

        @Test
        void multiplePaths_whenCalled_groupedCorrectly() {
            Map<String, List<String>> result =
                    CliDisplay.classifyFiles(List.of(
                            ".claude/rules/01-identity.md",
                            ".claude/rules/02-domain.md",
                            ".claude/skills/dev/SKILL.md",
                            ".github/agents/arch.agent.md",
                            "CLAUDE.md"));

            assertThat(result.get("Rules")).hasSize(2);
            assertThat(result.get("Skills")).hasSize(1);
            assertThat(result.get("GitHub Agents")).hasSize(1);
            assertThat(result.get("Root Files")).hasSize(1);
        }
    }

    @Nested
    @DisplayName("formatSummaryTable")
    class FormatSummaryTable {

        @Test
        void emptyMap_whenCalled_returnsHeaderAndTotalZero() {
            String table =
                    CliDisplay.formatSummaryTable(Map.of());

            assertThat(table).contains("Category");
            assertThat(table).contains("Count");
            assertThat(table).contains("Total");
        }

        @Test
        void singleCategory_whenCalled_showsCountAndTotal() {
            Map<String, List<String>> classified = Map.of(
                    "Rules", List.of("a.md", "b.md", "c.md"));

            String table =
                    CliDisplay.formatSummaryTable(classified);

            assertThat(table).contains("Rules");
            assertThat(table).contains("3");
            assertThat(table).contains("Total");
        }

        @Test
        void multipleCategories_whenCalled_showsAllCountsAndTotal() {
            Map<String, List<String>> classified = Map.of(
                    "Rules", List.of("a", "b", "c", "d", "e"),
                    "Skills", List.of("s1", "s2"),
                    "Agents", List.of("ag1"));

            String table =
                    CliDisplay.formatSummaryTable(classified);

            assertThat(table).contains("Rules");
            assertThat(table).contains("5");
            assertThat(table).contains("Skills");
            assertThat(table).contains("2");
            assertThat(table).contains("Agents");
            assertThat(table).contains("1");
            assertThat(table).contains("8");
        }

        @Test
        void table_whenCalled_containsSeparatorLines() {
            Map<String, List<String>> classified = Map.of(
                    "Rules", List.of("a"));

            String table =
                    CliDisplay.formatSummaryTable(classified);

            assertThat(table).contains("\u2500");
        }
    }

    @Nested
    @DisplayName("formatResult")
    class FormatResult {

        @Test
        void successResult_whenCalled_containsPipelineSuccess() {
            PipelineResult result = new PipelineResult(
                    true, "/tmp/output",
                    List.of(".claude/rules/01.md"),
                    List.of(), 150);

            String output =
                    CliDisplay.formatResult(result, DisplayMode.LIVE);

            assertThat(output).contains("Success");
            assertThat(output).contains("150ms");
            assertThat(output).contains("/tmp/output");
        }

        @Test
        void dryRunResult_whenCalled_containsDryRunHeader() {
            PipelineResult result = new PipelineResult(
                    true, "/tmp/output",
                    List.of(".claude/rules/01.md"),
                    List.of(), 100);

            String output =
                    CliDisplay.formatResult(result, DisplayMode.DRY_RUN);

            assertThat(output).contains("[DRY RUN]");
        }

        @Test
        void resultWithWarnings_whenCalled_showsWarnings() {
            PipelineResult result = new PipelineResult(
                    true, "/tmp/output",
                    List.of(".claude/rules/01.md"),
                    List.of("Some warning"), 100);

            String output =
                    CliDisplay.formatResult(result, DisplayMode.LIVE);

            assertThat(output).contains("Warning:");
            assertThat(output).contains("Some warning");
        }

        @Test
        void dryRunResult_whenCalled_listsAllFiles() {
            PipelineResult result = new PipelineResult(
                    true, "/tmp/output",
                    List.of(".claude/rules/01.md",
                            ".github/agents/a.md"),
                    List.of(), 100);

            String output =
                    CliDisplay.formatResult(result, DisplayMode.DRY_RUN);

            assertThat(output)
                    .contains(".claude/rules/01.md");
            assertThat(output)
                    .contains(".github/agents/a.md");
        }
    }
}
