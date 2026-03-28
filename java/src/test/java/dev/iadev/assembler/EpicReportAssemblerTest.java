package dev.iadev.assembler;

import dev.iadev.model.ProjectConfig;
import dev.iadev.template.TemplateEngine;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for EpicReportAssembler — copies epic execution
 * report template to .claude/templates/ and
 * .github/templates/.
 */
@DisplayName("EpicReportAssembler")
class EpicReportAssemblerTest {

    private static final String TEMPLATE_CONTENT =
            "# Epic Execution Report -- {{EPIC_ID}}\n\n"
                    + "> Branch: `{{BRANCH}}`\n"
                    + "> Started: {{STARTED_AT}}"
                    + " | Finished: {{FINISHED_AT}}\n\n"
                    + "## Sumário Executivo\n\n"
                    + "| Metric | Value |\n"
                    + "|--------|-------|\n"
                    + "| Stories Completed"
                    + " | {{STORIES_COMPLETED}} |\n"
                    + "| Stories Failed"
                    + " | {{STORIES_FAILED}} |\n"
                    + "| Stories Blocked"
                    + " | {{STORIES_BLOCKED}} |\n"
                    + "| Stories Total"
                    + " | {{STORIES_TOTAL}} |\n"
                    + "| Completion"
                    + " | {{COMPLETION_PERCENTAGE}} |\n\n"
                    + "## Timeline de Execução\n\n"
                    + "{{PHASE_TIMELINE_TABLE}}\n\n"
                    + "## Status Final por Story\n\n"
                    + "{{STORY_STATUS_TABLE}}\n\n"
                    + "## Findings Consolidados\n\n"
                    + "{{FINDINGS_SUMMARY}}\n\n"
                    + "## Coverage Delta\n\n"
                    + "| Metric | Before | After"
                    + " | Delta |\n"
                    + "|--------|--------|-------"
                    + "|-------|\n"
                    + "| Line Coverage"
                    + " | {{COVERAGE_BEFORE}}"
                    + " | {{COVERAGE_AFTER}}"
                    + " | {{COVERAGE_DELTA}} |\n\n"
                    + "## TDD Compliance\n\n"
                    + "### Per-Story TDD Metrics\n\n"
                    + "| Story | TDD Commits"
                    + " | Total Commits | TDD %"
                    + " | TPP Progression"
                    + " | Status |\n"
                    + "|-------|-------------|"
                    + "---------------|-------|"
                    + "-----------------|--------|\n"
                    + "{{TDD_COMPLIANCE_TABLE}}\n\n"
                    + "### Summary\n\n"
                    + "{{TDD_SUMMARY}}\n\n"
                    + "## Commits e SHAs\n\n"
                    + "{{COMMIT_LOG}}\n\n"
                    + "## Issues Não Resolvidos\n\n"
                    + "{{UNRESOLVED_ISSUES}}\n\n"
                    + "## PR Link\n\n"
                    + "{{PR_LINK}}\n";

    @Nested
    @DisplayName("implements Assembler interface")
    class ImplementsAssembler {

        @Test
        @DisplayName("is instance of Assembler")
        void implements_whenCalled_isAssemblerInstance() {
            assertThat(new EpicReportAssembler())
                    .isInstanceOf(Assembler.class);
        }
    }

    @Nested
    @DisplayName("hasAllMandatorySections")
    class HasAllMandatorySections {

        @Test
        @DisplayName("returns true for complete template")
        void assemble_forComplete_returnsTrue() {
            assertThat(EpicReportAssembler
                    .hasAllMandatorySections(
                            TEMPLATE_CONTENT))
                    .isTrue();
        }

        @Test
        @DisplayName("returns false when section missing")
        void assemble_whenMissing_returnsFalse() {
            String incomplete = TEMPLATE_CONTENT
                    .replace(
                            "## PR Link",
                            "## Some Other");
            assertThat(EpicReportAssembler
                    .hasAllMandatorySections(incomplete))
                    .isFalse();
        }

        @Test
        @DisplayName("returns false for empty content")
        void assemble_forEmpty_returnsFalse() {
            assertThat(EpicReportAssembler
                    .hasAllMandatorySections(""))
                    .isFalse();
        }
    }

    @Nested
    @DisplayName("assemble — copies to two destinations")
    class Assemble {

        @Test
        @DisplayName("copies template to .claude/templates/"
                + " and .github/templates/")
        void assemble_whenCalled_copiesToBothDestinations(
                @TempDir Path tempDir) throws IOException {
            Path resourcesDir = setupResources(tempDir);
            Path outputDir = tempDir.resolve("output");
            Files.createDirectories(outputDir);

            EpicReportAssembler assembler =
                    new EpicReportAssembler(resourcesDir);
            ProjectConfig config =
                    TestConfigBuilder.minimal();

            List<String> files = assembler.assemble(
                    config, new TemplateEngine(), outputDir);

            assertThat(files).hasSize(2);
            assertThat(outputDir.resolve(
                    ".claude/templates/"
                            + "_TEMPLATE-EPIC-EXECUTION"
                            + "-REPORT.md"))
                    .exists();
            assertThat(outputDir.resolve(
                    ".github/templates/"
                            + "_TEMPLATE-EPIC-EXECUTION"
                            + "-REPORT.md"))
                    .exists();
        }

        @Test
        @DisplayName("content is verbatim copy"
                + " (no rendering)")
        void assemble_content_isVerbatim(
                @TempDir Path tempDir) throws IOException {
            Path resourcesDir = setupResources(tempDir);
            Path outputDir = tempDir.resolve("output");
            Files.createDirectories(outputDir);

            EpicReportAssembler assembler =
                    new EpicReportAssembler(resourcesDir);
            ProjectConfig config =
                    TestConfigBuilder.minimal();

            assembler.assemble(
                    config, new TemplateEngine(), outputDir);

            String claudeContent = Files.readString(
                    outputDir.resolve(
                            ".claude/templates/"
                                    + "_TEMPLATE-EPIC"
                                    + "-EXECUTION"
                                    + "-REPORT.md"),
                    StandardCharsets.UTF_8);
            assertThat(claudeContent)
                    .isEqualTo(TEMPLATE_CONTENT);
            assertThat(claudeContent)
                    .contains("{{EPIC_ID}}");
            assertThat(claudeContent)
                    .contains("{{BRANCH}}");
        }

        @Test
        @DisplayName("returns empty when template missing")
        void assemble_whenCalled_returnsEmptyWhenMissing(
                @TempDir Path tempDir) throws IOException {
            Path resourcesDir = tempDir.resolve("empty");
            Files.createDirectories(resourcesDir);
            Path outputDir = tempDir.resolve("output");
            Files.createDirectories(outputDir);

            EpicReportAssembler assembler =
                    new EpicReportAssembler(resourcesDir);
            ProjectConfig config =
                    TestConfigBuilder.minimal();

            List<String> files = assembler.assemble(
                    config, new TemplateEngine(), outputDir);

            assertThat(files).isEmpty();
        }

        @Test
        @DisplayName("returns empty when sections"
                + " incomplete")
        void assemble_whenCalled_returnsEmptyWhenIncomplete(
                @TempDir Path tempDir) throws IOException {
            Path resourcesDir = tempDir.resolve("res");
            Path templateDir =
                    resourcesDir.resolve("templates");
            Files.createDirectories(templateDir);
            Files.writeString(
                    templateDir.resolve(
                            "_TEMPLATE-EPIC-EXECUTION"
                                    + "-REPORT.md"),
                    "# Incomplete\n\n## Sumário Executivo",
                    StandardCharsets.UTF_8);

            Path outputDir = tempDir.resolve("output");
            Files.createDirectories(outputDir);

            EpicReportAssembler assembler =
                    new EpicReportAssembler(resourcesDir);
            ProjectConfig config =
                    TestConfigBuilder.minimal();

            List<String> files = assembler.assemble(
                    config, new TemplateEngine(), outputDir);

            assertThat(files).isEmpty();
        }

        @Test
        @DisplayName("preserves {{PLACEHOLDER}} tokens"
                + " for runtime resolution")
        void assemble_whenCalled_preservesPlaceholders(
                @TempDir Path tempDir) throws IOException {
            Path resourcesDir = setupResources(tempDir);
            Path outputDir = tempDir.resolve("output");
            Files.createDirectories(outputDir);

            EpicReportAssembler assembler =
                    new EpicReportAssembler(resourcesDir);
            ProjectConfig config =
                    TestConfigBuilder.minimal();

            assembler.assemble(
                    config, new TemplateEngine(), outputDir);

            String content = Files.readString(
                    outputDir.resolve(
                            ".github/templates/"
                                    + "_TEMPLATE-EPIC"
                                    + "-EXECUTION"
                                    + "-REPORT.md"),
                    StandardCharsets.UTF_8);
            assertThat(content)
                    .contains("{{EPIC_ID}}")
                    .contains("{{BRANCH}}")
                    .contains("{{STORIES_COMPLETED}}")
                    .contains("{{PR_LINK}}");
        }

        @Test
        @DisplayName("both copies are identical")
        void assemble_both_copiesIdentical(
                @TempDir Path tempDir) throws IOException {
            Path resourcesDir = setupResources(tempDir);
            Path outputDir = tempDir.resolve("output");
            Files.createDirectories(outputDir);

            EpicReportAssembler assembler =
                    new EpicReportAssembler(resourcesDir);
            ProjectConfig config =
                    TestConfigBuilder.minimal();

            assembler.assemble(
                    config, new TemplateEngine(), outputDir);

            String claudeContent = Files.readString(
                    outputDir.resolve(
                            ".claude/templates/"
                                    + "_TEMPLATE-EPIC"
                                    + "-EXECUTION"
                                    + "-REPORT.md"),
                    StandardCharsets.UTF_8);
            String githubContent = Files.readString(
                    outputDir.resolve(
                            ".github/templates/"
                                    + "_TEMPLATE-EPIC"
                                    + "-EXECUTION"
                                    + "-REPORT.md"),
                    StandardCharsets.UTF_8);

            assertThat(claudeContent)
                    .isEqualTo(githubContent);
        }

        private Path setupResources(Path tempDir)
                throws IOException {
            Path resourcesDir = tempDir.resolve("res");
            Path templateDir =
                    resourcesDir.resolve("templates");
            Files.createDirectories(templateDir);
            Files.writeString(
                    templateDir.resolve(
                            "_TEMPLATE-EPIC-EXECUTION"
                                    + "-REPORT.md"),
                    TEMPLATE_CONTENT,
                    StandardCharsets.UTF_8);
            return resourcesDir;
        }
    }

    @Nested
    @DisplayName("assemble — golden file parity")
    class GoldenFile {

        @Test
        @DisplayName("output matches golden file for"
                + " kotlin-ktor")
        void assemble_whenCalled_matchesGoldenFile(
                @TempDir Path tempDir) throws IOException {
            Path outputDir = tempDir.resolve("output");
            Files.createDirectories(outputDir);

            EpicReportAssembler assembler =
                    new EpicReportAssembler();
            ProjectConfig config =
                    TestConfigBuilder.minimal();

            List<String> files = assembler.assemble(
                    config, new TemplateEngine(), outputDir);

            assertThat(files).hasSize(2);

            String golden = loadGolden(
                    "golden/kotlin-ktor/.claude/templates/"
                            + "_TEMPLATE-EPIC-EXECUTION"
                            + "-REPORT.md");
            if (golden != null) {
                String actual = Files.readString(
                        outputDir.resolve(
                                ".claude/templates/"
                                        + "_TEMPLATE-EPIC"
                                        + "-EXECUTION"
                                        + "-REPORT.md"),
                        StandardCharsets.UTF_8);
                assertThat(actual)
                        .as("Must match golden file")
                        .isEqualTo(golden);
            }
        }

        private String loadGolden(String path) {
            var url = getClass().getClassLoader()
                    .getResource(path);
            if (url == null) {
                return null;
            }
            try {
                return Files.readString(
                        Path.of(url.getPath()),
                        StandardCharsets.UTF_8);
            } catch (IOException e) {
                return null;
            }
        }
    }
}
