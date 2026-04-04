package dev.iadev.assembler;

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
 * Tests for SloSliTemplateAssembler -- copies SLO/SLI
 * definition template to docs/templates/.
 */
@DisplayName("SloSliTemplateAssembler")
class SloSliTemplateAssemblerTest {

    @Nested
    @DisplayName("implements Assembler interface")
    class ImplementsAssembler {

        @Test
        @DisplayName("is instance of Assembler")
        void instanceOf_whenCreated_implementsAssembler() {
            SloSliTemplateAssembler assembler =
                    new SloSliTemplateAssembler();

            assertThat(assembler)
                    .isInstanceOf(Assembler.class);
        }
    }

    @Nested
    @DisplayName("assemble -- generates template")
    class AssembleTemplate {

        @Test
        @DisplayName("generates SLO/SLI definition in"
                + " docs/templates/")
        void assemble_whenCalled_generatesFile(
                @TempDir Path tempDir) {
            Path outputDir = tempDir.resolve("output");

            SloSliTemplateAssembler assembler =
                    new SloSliTemplateAssembler();
            ProjectConfig config =
                    TestConfigBuilder.minimal();
            TemplateEngine engine = new TemplateEngine();

            List<String> files = assembler.assemble(
                    config, engine, outputDir);

            assertThat(files).hasSize(1);
            Path sloPath = outputDir.resolve(
                    "docs/templates/"
                            + "_TEMPLATE-SLO-SLI-DEFINITION"
                            + ".md");
            assertThat(sloPath).exists();
        }

        @Test
        @DisplayName("creates docs/templates/ subdirectory")
        void assemble_whenCalled_createsSubdir(
                @TempDir Path tempDir) {
            Path outputDir = tempDir.resolve("output");

            SloSliTemplateAssembler assembler =
                    new SloSliTemplateAssembler();
            ProjectConfig config =
                    TestConfigBuilder.minimal();
            TemplateEngine engine = new TemplateEngine();

            assembler.assemble(config, engine, outputDir);

            assertThat(
                    outputDir.resolve("docs/templates"))
                    .exists()
                    .isDirectory();
        }

        @Test
        @DisplayName("returns file path in result list")
        void assemble_whenCalled_returnsFilePath(
                @TempDir Path tempDir) {
            Path outputDir = tempDir.resolve("output");

            SloSliTemplateAssembler assembler =
                    new SloSliTemplateAssembler();
            ProjectConfig config =
                    TestConfigBuilder.minimal();
            TemplateEngine engine = new TemplateEngine();

            List<String> files = assembler.assemble(
                    config, engine, outputDir);

            assertThat(files).hasSize(1);
            assertThat(files.get(0)).endsWith(
                    "_TEMPLATE-SLO-SLI-DEFINITION.md");
        }
    }

    @Nested
    @DisplayName("template has all 7 mandatory sections")
    class MandatorySections {

        @Test
        @DisplayName("contains Service Overview section")
        void assemble_template_containsServiceOverview(
                @TempDir Path tempDir) {
            String content = generateAndRead(tempDir);
            assertThat(content)
                    .contains("## Service Overview");
        }

        @Test
        @DisplayName("contains SLI Definitions section"
                + " with 4 standard SLIs")
        void assemble_template_containsSliDefinitions(
                @TempDir Path tempDir) {
            String content = generateAndRead(tempDir);
            assertThat(content)
                    .contains("## SLI Definitions");
            assertThat(content)
                    .contains("Availability");
            assertThat(content)
                    .contains("Latency");
            assertThat(content)
                    .contains("Throughput");
            assertThat(content)
                    .contains("Error Rate");
        }

        @Test
        @DisplayName("SLI Definitions table has required"
                + " columns")
        void assemble_template_sliTableHasColumns(
                @TempDir Path tempDir) {
            String content = generateAndRead(tempDir);
            assertThat(content).contains("| SLI |");
            assertThat(content).contains("Metric");
            assertThat(content).contains("Method");
            assertThat(content).contains("Source");
        }

        @Test
        @DisplayName("contains SLO Targets section")
        void assemble_template_containsSloTargets(
                @TempDir Path tempDir) {
            String content = generateAndRead(tempDir);
            assertThat(content)
                    .contains("## SLO Targets");
            assertThat(content)
                    .contains("99.9%");
            assertThat(content)
                    .contains("Rolling 30 days");
        }

        @Test
        @DisplayName("contains Error Budget Policy section"
                + " with consumption levels")
        void assemble_template_containsErrorBudget(
                @TempDir Path tempDir) {
            String content = generateAndRead(tempDir);
            assertThat(content)
                    .contains("## Error Budget Policy");
            assertThat(content)
                    .contains("50% consumed");
            assertThat(content)
                    .contains("75% consumed");
            assertThat(content)
                    .contains("100% consumed");
        }

        @Test
        @DisplayName("contains Burn Rate Alerting"
                + " Configuration with fast and slow burn")
        void assemble_template_containsBurnRate(
                @TempDir Path tempDir) {
            String content = generateAndRead(tempDir);
            assertThat(content)
                    .contains("## Burn Rate Alerting"
                            + " Configuration");
            assertThat(content)
                    .contains("14.4x");
            assertThat(content).contains("6x");
            assertThat(content)
                    .contains("PagerDuty");
        }

        @Test
        @DisplayName("contains Dashboard Requirements"
                + " section")
        void assemble_template_containsDashboard(
                @TempDir Path tempDir) {
            String content = generateAndRead(tempDir);
            assertThat(content)
                    .contains("## Dashboard Requirements");
            assertThat(content)
                    .contains("Remaining Error Budget");
            assertThat(content)
                    .contains("Burn Rate");
            assertThat(content)
                    .contains("Budget Exhaustion Forecast");
        }

        @Test
        @DisplayName("contains Review Cadence section")
        void assemble_template_containsReviewCadence(
                @TempDir Path tempDir) {
            String content = generateAndRead(tempDir);
            assertThat(content)
                    .contains("## Review Cadence");
            assertThat(content)
                    .contains("Monthly");
            assertThat(content)
                    .contains("Quarterly");
        }

        @Test
        @DisplayName("has all 7 mandatory sections")
        void assemble_template_hasAllMandatorySections(
                @TempDir Path tempDir) {
            String content = generateAndRead(tempDir);
            assertThat(content)
                    .contains("# SLO/SLI Definitions");
            for (String section
                    : SloSliTemplateAssembler
                    .MANDATORY_SECTIONS) {
                assertThat(content)
                        .as("Missing section: %s", section)
                        .contains(section);
            }
        }
    }

    @Nested
    @DisplayName("assemble -- graceful no-op")
    class GracefulNoOp {

        @Test
        @DisplayName("returns empty list when template"
                + " file absent")
        void assemble_whenCalled_returnsEmptyWhenAbsent(
                @TempDir Path tempDir) {
            Path resourcesDir =
                    tempDir.resolve("nonexistent");
            Path outputDir = tempDir.resolve("output");

            SloSliTemplateAssembler assembler =
                    new SloSliTemplateAssembler(
                            resourcesDir);
            ProjectConfig config =
                    TestConfigBuilder.minimal();
            TemplateEngine engine = new TemplateEngine();

            List<String> files = assembler.assemble(
                    config, engine, outputDir);

            assertThat(files).isEmpty();
        }

        @Test
        @DisplayName("does not create output directory"
                + " when template absent")
        void assemble_whenCalled_doesNotCreateOutput(
                @TempDir Path tempDir) {
            Path resourcesDir =
                    tempDir.resolve("nonexistent");
            Path outputDir = tempDir.resolve("output");

            SloSliTemplateAssembler assembler =
                    new SloSliTemplateAssembler(
                            resourcesDir);
            ProjectConfig config =
                    TestConfigBuilder.minimal();
            TemplateEngine engine = new TemplateEngine();

            assembler.assemble(config, engine, outputDir);

            assertThat(outputDir).doesNotExist();
        }
    }

    @Nested
    @DisplayName("assemble -- unconditional generation")
    class UnconditionalGeneration {

        @Test
        @DisplayName("generates for minimal config")
        void assemble_minimalConfig_generatesTemplate(
                @TempDir Path tempDir) {
            Path outputDir = tempDir.resolve("output");

            SloSliTemplateAssembler assembler =
                    new SloSliTemplateAssembler();
            ProjectConfig config =
                    TestConfigBuilder.minimal();
            TemplateEngine engine = new TemplateEngine();

            List<String> files = assembler.assemble(
                    config, engine, outputDir);

            assertThat(files).hasSize(1);
        }

        @Test
        @DisplayName("generates for java-spring profile")
        void assemble_javaSpring_generatesTemplate(
                @TempDir Path tempDir) {
            Path outputDir = tempDir.resolve("output");

            SloSliTemplateAssembler assembler =
                    new SloSliTemplateAssembler();
            ProjectConfig config =
                    TestConfigBuilder.builder()
                            .language("java", "21")
                            .framework("spring-boot", "3.2")
                            .build();
            TemplateEngine engine = new TemplateEngine();

            List<String> files = assembler.assemble(
                    config, engine, outputDir);

            assertThat(files).hasSize(1);
        }

        @Test
        @DisplayName("generates for python-fastapi profile")
        void assemble_pythonFastapi_generatesTemplate(
                @TempDir Path tempDir) {
            Path outputDir = tempDir.resolve("output");

            SloSliTemplateAssembler assembler =
                    new SloSliTemplateAssembler();
            ProjectConfig config =
                    TestConfigBuilder.builder()
                            .language("python", "3.12")
                            .framework("fastapi", "0.110")
                            .build();
            TemplateEngine engine = new TemplateEngine();

            List<String> files = assembler.assemble(
                    config, engine, outputDir);

            assertThat(files).hasSize(1);
        }

        @Test
        @DisplayName("generates for go-gin profile")
        void assemble_goGin_generatesTemplate(
                @TempDir Path tempDir) {
            Path outputDir = tempDir.resolve("output");

            SloSliTemplateAssembler assembler =
                    new SloSliTemplateAssembler();
            ProjectConfig config =
                    TestConfigBuilder.builder()
                            .language("go", "1.22")
                            .framework("gin", "1.9")
                            .build();
            TemplateEngine engine = new TemplateEngine();

            List<String> files = assembler.assemble(
                    config, engine, outputDir);

            assertThat(files).hasSize(1);
        }
    }

    @Nested
    @DisplayName("template renders project name")
    class TemplateRendering {

        @Test
        @DisplayName("renders PROJECT_NAME in title")
        void assemble_template_rendersProjectName(
                @TempDir Path tempDir) {
            Path outputDir = tempDir.resolve("output");

            SloSliTemplateAssembler assembler =
                    new SloSliTemplateAssembler();
            ProjectConfig config =
                    TestConfigBuilder.builder()
                            .projectName("my-awesome-service")
                            .build();
            TemplateEngine engine = new TemplateEngine();

            assembler.assemble(config, engine, outputDir);

            Path sloPath = outputDir.resolve(
                    "docs/templates/"
                            + "_TEMPLATE-SLO-SLI-DEFINITION"
                            + ".md");
            String content = readFile(sloPath);
            assertThat(content)
                    .contains("my-awesome-service");
        }
    }

    private static String generateAndRead(Path tempDir) {
        Path outputDir = tempDir.resolve("output");

        SloSliTemplateAssembler assembler =
                new SloSliTemplateAssembler();
        ProjectConfig config =
                TestConfigBuilder.minimal();
        TemplateEngine engine = new TemplateEngine();

        assembler.assemble(config, engine, outputDir);

        Path sloPath = outputDir.resolve(
                "docs/templates/"
                        + "_TEMPLATE-SLO-SLI-DEFINITION.md");
        return readFile(sloPath);
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
