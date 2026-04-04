package dev.iadev.application.assembler;

import dev.iadev.domain.model.ProjectConfig;
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
 * Tests for PrIssueTemplateAssembler -- generates
 * PR and Issue templates to .github/ output directory.
 */
@DisplayName("PrIssueTemplateAssembler")
class PrIssueTemplateAssemblerTest {

    @Nested
    @DisplayName("implements Assembler interface")
    class ImplementsAssembler {

        @Test
        @DisplayName("is instance of Assembler")
        void instanceOf_whenCreated_implementsAssemblerInterface() {
            PrIssueTemplateAssembler assembler =
                    new PrIssueTemplateAssembler();

            assertThat(assembler)
                    .isInstanceOf(Assembler.class);
        }
    }

    @Nested
    @DisplayName("assemble — generates 4 files")
    class AssembleFiles {

        @Test
        @DisplayName("generates exactly 4 files")
        void assemble_whenCalled_generatesFourFiles(
                @TempDir Path tempDir) {
            Path outputDir = tempDir.resolve("output");

            PrIssueTemplateAssembler assembler =
                    new PrIssueTemplateAssembler();
            ProjectConfig config =
                    TestConfigBuilder.minimal();
            TemplateEngine engine = new TemplateEngine();

            List<String> files = assembler.assemble(
                    config, engine, outputDir);

            assertThat(files).hasSize(4);
        }

        @Test
        @DisplayName("creates ISSUE_TEMPLATE subdirectory")
        void assemble_whenCalled_createsIssueTemplateDir(
                @TempDir Path tempDir) {
            Path outputDir = tempDir.resolve("output");

            PrIssueTemplateAssembler assembler =
                    new PrIssueTemplateAssembler();
            ProjectConfig config =
                    TestConfigBuilder.minimal();
            TemplateEngine engine = new TemplateEngine();

            assembler.assemble(config, engine, outputDir);

            assertThat(outputDir.resolve("ISSUE_TEMPLATE"))
                    .exists()
                    .isDirectory();
        }

        @Test
        @DisplayName("generates pull_request_template.md")
        void assemble_whenCalled_generatesPrTemplate(
                @TempDir Path tempDir) {
            Path outputDir = tempDir.resolve("output");

            PrIssueTemplateAssembler assembler =
                    new PrIssueTemplateAssembler();
            ProjectConfig config =
                    TestConfigBuilder.minimal();
            TemplateEngine engine = new TemplateEngine();

            assembler.assemble(config, engine, outputDir);

            Path prTemplate = outputDir.resolve(
                    "pull_request_template.md");
            assertThat(prTemplate).exists();
        }

        @Test
        @DisplayName("generates bug_report.md")
        void assemble_whenCalled_generatesBugReport(
                @TempDir Path tempDir) {
            Path outputDir = tempDir.resolve("output");

            PrIssueTemplateAssembler assembler =
                    new PrIssueTemplateAssembler();
            ProjectConfig config =
                    TestConfigBuilder.minimal();
            TemplateEngine engine = new TemplateEngine();

            assembler.assemble(config, engine, outputDir);

            Path bugReport = outputDir.resolve(
                    "ISSUE_TEMPLATE/bug_report.md");
            assertThat(bugReport).exists();
        }

        @Test
        @DisplayName("generates feature_request.md")
        void assemble_whenCalled_generatesFeatureRequest(
                @TempDir Path tempDir) {
            Path outputDir = tempDir.resolve("output");

            PrIssueTemplateAssembler assembler =
                    new PrIssueTemplateAssembler();
            ProjectConfig config =
                    TestConfigBuilder.minimal();
            TemplateEngine engine = new TemplateEngine();

            assembler.assemble(config, engine, outputDir);

            Path featureRequest = outputDir.resolve(
                    "ISSUE_TEMPLATE/feature_request.md");
            assertThat(featureRequest).exists();
        }

        @Test
        @DisplayName("generates config.yml")
        void assemble_whenCalled_generatesConfigYml(
                @TempDir Path tempDir) {
            Path outputDir = tempDir.resolve("output");

            PrIssueTemplateAssembler assembler =
                    new PrIssueTemplateAssembler();
            ProjectConfig config =
                    TestConfigBuilder.minimal();
            TemplateEngine engine = new TemplateEngine();

            assembler.assemble(config, engine, outputDir);

            Path configYml = outputDir.resolve(
                    "ISSUE_TEMPLATE/config.yml");
            assertThat(configYml).exists();
        }
    }

    @Nested
    @DisplayName("assemble — PR template content")
    class PrTemplateContent {

        @Test
        @DisplayName("PR template with default coverage"
                + " values (95/90)")
        void assemble_defaultCoverage_usesDefaults(
                @TempDir Path tempDir) {
            Path outputDir = tempDir.resolve("output");

            PrIssueTemplateAssembler assembler =
                    new PrIssueTemplateAssembler();
            ProjectConfig config =
                    TestConfigBuilder.minimal();
            TemplateEngine engine = new TemplateEngine();

            assembler.assemble(config, engine, outputDir);

            String content = readFile(
                    outputDir.resolve(
                            "pull_request_template.md"));
            assertThat(content).contains("95%");
            assertThat(content).contains("90%");
        }

        @Test
        @DisplayName("PR template resolves project name")
        void assemble_withProjectName_resolvesVariable(
                @TempDir Path tempDir) {
            Path outputDir = tempDir.resolve("output");

            PrIssueTemplateAssembler assembler =
                    new PrIssueTemplateAssembler();
            ProjectConfig config =
                    TestConfigBuilder.builder()
                            .projectName("my-service")
                            .build();
            TemplateEngine engine = new TemplateEngine();

            assembler.assemble(config, engine, outputDir);

            String content = readFile(
                    outputDir.resolve(
                            "pull_request_template.md"));
            assertThat(content).contains("my-service");
            assertThat(content)
                    .doesNotContain("{{ project_name }}");
        }

        @Test
        @DisplayName("PR template contains Summary section")
        void assemble_whenCalled_containsSummarySection(
                @TempDir Path tempDir) {
            Path outputDir = tempDir.resolve("output");

            PrIssueTemplateAssembler assembler =
                    new PrIssueTemplateAssembler();
            ProjectConfig config =
                    TestConfigBuilder.minimal();
            TemplateEngine engine = new TemplateEngine();

            assembler.assemble(config, engine, outputDir);

            String content = readFile(
                    outputDir.resolve(
                            "pull_request_template.md"));
            assertThat(content).contains("## Summary");
        }

        @Test
        @DisplayName("PR template contains Type of Change"
                + " checklist")
        void assemble_whenCalled_containsTypeOfChange(
                @TempDir Path tempDir) {
            Path outputDir = tempDir.resolve("output");

            PrIssueTemplateAssembler assembler =
                    new PrIssueTemplateAssembler();
            ProjectConfig config =
                    TestConfigBuilder.minimal();
            TemplateEngine engine = new TemplateEngine();

            assembler.assemble(config, engine, outputDir);

            String content = readFile(
                    outputDir.resolve(
                            "pull_request_template.md"));
            assertThat(content)
                    .contains("## Type of Change");
        }

        @Test
        @DisplayName("PR template contains Testing section")
        void assemble_whenCalled_containsTestingSection(
                @TempDir Path tempDir) {
            Path outputDir = tempDir.resolve("output");

            PrIssueTemplateAssembler assembler =
                    new PrIssueTemplateAssembler();
            ProjectConfig config =
                    TestConfigBuilder.minimal();
            TemplateEngine engine = new TemplateEngine();

            assembler.assemble(config, engine, outputDir);

            String content = readFile(
                    outputDir.resolve(
                            "pull_request_template.md"));
            assertThat(content).contains("## Testing");
        }

        @Test
        @DisplayName("PR template contains Checklist section")
        void assemble_whenCalled_containsChecklist(
                @TempDir Path tempDir) {
            Path outputDir = tempDir.resolve("output");

            PrIssueTemplateAssembler assembler =
                    new PrIssueTemplateAssembler();
            ProjectConfig config =
                    TestConfigBuilder.minimal();
            TemplateEngine engine = new TemplateEngine();

            assembler.assemble(config, engine, outputDir);

            String content = readFile(
                    outputDir.resolve(
                            "pull_request_template.md"));
            assertThat(content).contains("## Checklist");
        }

        @Test
        @DisplayName("PR template resolves architecture"
                + " style")
        void assemble_withArchStyle_resolvesVariable(
                @TempDir Path tempDir) {
            Path outputDir = tempDir.resolve("output");

            PrIssueTemplateAssembler assembler =
                    new PrIssueTemplateAssembler();
            ProjectConfig config =
                    TestConfigBuilder.builder()
                            .archStyle("hexagonal")
                            .build();
            TemplateEngine engine = new TemplateEngine();

            assembler.assemble(config, engine, outputDir);

            String content = readFile(
                    outputDir.resolve(
                            "pull_request_template.md"));
            assertThat(content).contains("hexagonal");
        }

        @Test
        @DisplayName("no unresolved Pebble variables in"
                + " PR template")
        void assemble_whenCalled_noUnresolvedVariables(
                @TempDir Path tempDir) {
            Path outputDir = tempDir.resolve("output");

            PrIssueTemplateAssembler assembler =
                    new PrIssueTemplateAssembler();
            ProjectConfig config =
                    TestConfigBuilder.builder()
                            .projectName("api-test")
                            .language("java", "21")
                            .framework("quarkus", "3.17")
                            .build();
            TemplateEngine engine = new TemplateEngine();

            assembler.assemble(config, engine, outputDir);

            String content = readFile(
                    outputDir.resolve(
                            "pull_request_template.md"));
            assertThat(content)
                    .doesNotContain("{{ ");
            assertThat(content)
                    .doesNotContain("{%");
        }
    }

    @Nested
    @DisplayName("assemble — bug report content")
    class BugReportContent {

        @Test
        @DisplayName("bug report has frontmatter with"
                + " name field")
        void assemble_bugReport_hasFrontmatterName(
                @TempDir Path tempDir) {
            Path outputDir = tempDir.resolve("output");

            PrIssueTemplateAssembler assembler =
                    new PrIssueTemplateAssembler();
            ProjectConfig config =
                    TestConfigBuilder.minimal();
            TemplateEngine engine = new TemplateEngine();

            assembler.assemble(config, engine, outputDir);

            String content = readFile(
                    outputDir.resolve(
                            "ISSUE_TEMPLATE/bug_report.md"));
            assertThat(content)
                    .contains("name: Bug Report");
        }

        @Test
        @DisplayName("bug report has labels with bug")
        void assemble_bugReport_hasLabelsBug(
                @TempDir Path tempDir) {
            Path outputDir = tempDir.resolve("output");

            PrIssueTemplateAssembler assembler =
                    new PrIssueTemplateAssembler();
            ProjectConfig config =
                    TestConfigBuilder.minimal();
            TemplateEngine engine = new TemplateEngine();

            assembler.assemble(config, engine, outputDir);

            String content = readFile(
                    outputDir.resolve(
                            "ISSUE_TEMPLATE/bug_report.md"));
            assertThat(content).contains("bug");
        }

        @Test
        @DisplayName("bug report contains Describe the Bug"
                + " section")
        void assemble_bugReport_containsDescribeBug(
                @TempDir Path tempDir) {
            Path outputDir = tempDir.resolve("output");

            PrIssueTemplateAssembler assembler =
                    new PrIssueTemplateAssembler();
            ProjectConfig config =
                    TestConfigBuilder.minimal();
            TemplateEngine engine = new TemplateEngine();

            assembler.assemble(config, engine, outputDir);

            String content = readFile(
                    outputDir.resolve(
                            "ISSUE_TEMPLATE/bug_report.md"));
            assertThat(content)
                    .contains("Describe the Bug");
        }

        @Test
        @DisplayName("bug report contains Steps to Reproduce")
        void assemble_bugReport_containsStepsToReproduce(
                @TempDir Path tempDir) {
            Path outputDir = tempDir.resolve("output");

            PrIssueTemplateAssembler assembler =
                    new PrIssueTemplateAssembler();
            ProjectConfig config =
                    TestConfigBuilder.minimal();
            TemplateEngine engine = new TemplateEngine();

            assembler.assemble(config, engine, outputDir);

            String content = readFile(
                    outputDir.resolve(
                            "ISSUE_TEMPLATE/bug_report.md"));
            assertThat(content)
                    .contains("Steps to Reproduce");
        }

        @Test
        @DisplayName("bug report contains Expected Behavior")
        void assemble_bugReport_containsExpectedBehavior(
                @TempDir Path tempDir) {
            Path outputDir = tempDir.resolve("output");

            PrIssueTemplateAssembler assembler =
                    new PrIssueTemplateAssembler();
            ProjectConfig config =
                    TestConfigBuilder.minimal();
            TemplateEngine engine = new TemplateEngine();

            assembler.assemble(config, engine, outputDir);

            String content = readFile(
                    outputDir.resolve(
                            "ISSUE_TEMPLATE/bug_report.md"));
            assertThat(content)
                    .contains("Expected Behavior");
        }

        @Test
        @DisplayName("bug report contains Environment")
        void assemble_bugReport_containsEnvironment(
                @TempDir Path tempDir) {
            Path outputDir = tempDir.resolve("output");

            PrIssueTemplateAssembler assembler =
                    new PrIssueTemplateAssembler();
            ProjectConfig config =
                    TestConfigBuilder.minimal();
            TemplateEngine engine = new TemplateEngine();

            assembler.assemble(config, engine, outputDir);

            String content = readFile(
                    outputDir.resolve(
                            "ISSUE_TEMPLATE/bug_report.md"));
            assertThat(content)
                    .contains("Environment");
        }

        @Test
        @DisplayName("bug report starts with YAML"
                + " frontmatter")
        void assemble_bugReport_startsWithFrontmatter(
                @TempDir Path tempDir) {
            Path outputDir = tempDir.resolve("output");

            PrIssueTemplateAssembler assembler =
                    new PrIssueTemplateAssembler();
            ProjectConfig config =
                    TestConfigBuilder.minimal();
            TemplateEngine engine = new TemplateEngine();

            assembler.assemble(config, engine, outputDir);

            String content = readFile(
                    outputDir.resolve(
                            "ISSUE_TEMPLATE/bug_report.md"));
            assertThat(content).startsWith("---");
        }
    }

    @Nested
    @DisplayName("assemble — feature request content")
    class FeatureRequestContent {

        @Test
        @DisplayName("feature request has frontmatter"
                + " with name")
        void assemble_featureRequest_hasFrontmatterName(
                @TempDir Path tempDir) {
            Path outputDir = tempDir.resolve("output");

            PrIssueTemplateAssembler assembler =
                    new PrIssueTemplateAssembler();
            ProjectConfig config =
                    TestConfigBuilder.minimal();
            TemplateEngine engine = new TemplateEngine();

            assembler.assemble(config, engine, outputDir);

            String content = readFile(
                    outputDir.resolve(
                            "ISSUE_TEMPLATE/"
                                    + "feature_request.md"));
            assertThat(content)
                    .contains("name: Feature Request");
        }

        @Test
        @DisplayName("feature request contains Proposed"
                + " Solution")
        void assemble_featureRequest_containsProposedSolution(
                @TempDir Path tempDir) {
            Path outputDir = tempDir.resolve("output");

            PrIssueTemplateAssembler assembler =
                    new PrIssueTemplateAssembler();
            ProjectConfig config =
                    TestConfigBuilder.minimal();
            TemplateEngine engine = new TemplateEngine();

            assembler.assemble(config, engine, outputDir);

            String content = readFile(
                    outputDir.resolve(
                            "ISSUE_TEMPLATE/"
                                    + "feature_request.md"));
            assertThat(content)
                    .contains("Proposed Solution");
        }

        @Test
        @DisplayName("feature request contains Acceptance"
                + " Criteria")
        void assemble_featureRequest_containsAcceptanceCriteria(
                @TempDir Path tempDir) {
            Path outputDir = tempDir.resolve("output");

            PrIssueTemplateAssembler assembler =
                    new PrIssueTemplateAssembler();
            ProjectConfig config =
                    TestConfigBuilder.minimal();
            TemplateEngine engine = new TemplateEngine();

            assembler.assemble(config, engine, outputDir);

            String content = readFile(
                    outputDir.resolve(
                            "ISSUE_TEMPLATE/"
                                    + "feature_request.md"));
            assertThat(content)
                    .contains("Acceptance Criteria");
        }

        @Test
        @DisplayName("feature request starts with YAML"
                + " frontmatter")
        void assemble_featureRequest_startsWithFrontmatter(
                @TempDir Path tempDir) {
            Path outputDir = tempDir.resolve("output");

            PrIssueTemplateAssembler assembler =
                    new PrIssueTemplateAssembler();
            ProjectConfig config =
                    TestConfigBuilder.minimal();
            TemplateEngine engine = new TemplateEngine();

            assembler.assemble(config, engine, outputDir);

            String content = readFile(
                    outputDir.resolve(
                            "ISSUE_TEMPLATE/"
                                    + "feature_request.md"));
            assertThat(content).startsWith("---");
        }
    }

    @Nested
    @DisplayName("assemble — config.yml content")
    class ConfigYmlContent {

        @Test
        @DisplayName("config.yml contains"
                + " blank_issues_enabled: false")
        void assemble_configYml_containsBlankIssuesFalse(
                @TempDir Path tempDir) {
            Path outputDir = tempDir.resolve("output");

            PrIssueTemplateAssembler assembler =
                    new PrIssueTemplateAssembler();
            ProjectConfig config =
                    TestConfigBuilder.minimal();
            TemplateEngine engine = new TemplateEngine();

            assembler.assemble(config, engine, outputDir);

            String content = readFile(
                    outputDir.resolve(
                            "ISSUE_TEMPLATE/config.yml"));
            assertThat(content)
                    .contains("blank_issues_enabled: false");
        }
    }

    @Nested
    @DisplayName("assemble — multiple profiles")
    class MultipleProfiles {

        @Test
        @DisplayName("generates for java-spring profile")
        void assemble_javaSpring_generatesFourFiles(
                @TempDir Path tempDir) {
            Path outputDir = tempDir.resolve("output");

            PrIssueTemplateAssembler assembler =
                    new PrIssueTemplateAssembler();
            ProjectConfig config =
                    TestConfigBuilder.builder()
                            .projectName("api-pagamentos")
                            .language("java", "21")
                            .framework("spring-boot", "3.4")
                            .archStyle("hexagonal")
                            .build();
            TemplateEngine engine = new TemplateEngine();

            List<String> files = assembler.assemble(
                    config, engine, outputDir);

            assertThat(files).hasSize(4);
            String prContent = readFile(
                    outputDir.resolve(
                            "pull_request_template.md"));
            assertThat(prContent)
                    .contains("api-pagamentos");
        }

        @Test
        @DisplayName("generates for python-fastapi profile")
        void assemble_pythonFastapi_generatesFourFiles(
                @TempDir Path tempDir) {
            Path outputDir = tempDir.resolve("output");

            PrIssueTemplateAssembler assembler =
                    new PrIssueTemplateAssembler();
            ProjectConfig config =
                    TestConfigBuilder.builder()
                            .projectName("data-pipeline")
                            .language("python", "3.12")
                            .framework("fastapi", "0.109")
                            .archStyle("microservice")
                            .build();
            TemplateEngine engine = new TemplateEngine();

            List<String> files = assembler.assemble(
                    config, engine, outputDir);

            assertThat(files).hasSize(4);
            String prContent = readFile(
                    outputDir.resolve(
                            "pull_request_template.md"));
            assertThat(prContent)
                    .contains("data-pipeline");
        }

        @Test
        @DisplayName("generates for rust-axum profile")
        void assemble_rustAxum_generatesFourFiles(
                @TempDir Path tempDir) {
            Path outputDir = tempDir.resolve("output");

            PrIssueTemplateAssembler assembler =
                    new PrIssueTemplateAssembler();
            ProjectConfig config =
                    TestConfigBuilder.builder()
                            .projectName("auth-service")
                            .language("rust", "1.75")
                            .framework("axum", "0.7")
                            .archStyle("microservice")
                            .build();
            TemplateEngine engine = new TemplateEngine();

            List<String> files = assembler.assemble(
                    config, engine, outputDir);

            assertThat(files).hasSize(4);
            String prContent = readFile(
                    outputDir.resolve(
                            "pull_request_template.md"));
            assertThat(prContent)
                    .contains("auth-service");
        }
    }

    @Nested
    @DisplayName("assemble — graceful no-op")
    class GracefulNoOp {

        @Test
        @DisplayName("returns empty when templates"
                + " directory absent")
        void assemble_missingDir_returnsEmpty(
                @TempDir Path tempDir) {
            Path resourcesDir =
                    tempDir.resolve("nonexistent");
            Path outputDir = tempDir.resolve("output");

            PrIssueTemplateAssembler assembler =
                    new PrIssueTemplateAssembler(
                            resourcesDir);
            ProjectConfig config =
                    TestConfigBuilder.minimal();
            TemplateEngine engine = new TemplateEngine();

            List<String> files = assembler.assemble(
                    config, engine, outputDir);

            assertThat(files).isEmpty();
        }
    }

    private static String readFile(Path path) {
        try {
            return Files.readString(
                    path, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new RuntimeException(
                    "Failed to read: " + path, e);
        }
    }
}
