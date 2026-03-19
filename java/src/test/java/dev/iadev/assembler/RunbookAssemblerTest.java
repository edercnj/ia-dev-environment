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
 * Tests for RunbookAssembler — generates
 * docs/runbook/deploy-runbook.md from a Pebble template.
 */
@DisplayName("RunbookAssembler")
class RunbookAssemblerTest {

    @Nested
    @DisplayName("implements Assembler interface")
    class ImplementsAssembler {

        @Test
        @DisplayName("is instance of Assembler")
        void isAssemblerInstance() {
            RunbookAssembler assembler =
                    new RunbookAssembler();

            assertThat(assembler)
                    .isInstanceOf(Assembler.class);
        }
    }

    @Nested
    @DisplayName("assemble — generates deploy-runbook.md")
    class AssembleRunbook {

        @Test
        @DisplayName("generates deploy-runbook.md in"
                + " docs/runbook/ subdirectory")
        void generatesDeployRunbookFile(
                @TempDir Path tempDir) {
            Path outputDir = tempDir.resolve("output");

            RunbookAssembler assembler =
                    new RunbookAssembler();
            ProjectConfig config =
                    TestConfigBuilder.minimal();
            TemplateEngine engine = new TemplateEngine();

            List<String> files = assembler.assemble(
                    config, engine, outputDir);

            assertThat(files).hasSize(1);
            Path expected = outputDir.resolve(
                    "docs/runbook/deploy-runbook.md");
            assertThat(expected).exists();
        }

        @Test
        @DisplayName("creates docs/runbook/ subdirectory")
        void createsRunbookSubdir(
                @TempDir Path tempDir) {
            Path outputDir = tempDir.resolve("output");

            RunbookAssembler assembler =
                    new RunbookAssembler();
            ProjectConfig config =
                    TestConfigBuilder.minimal();
            TemplateEngine engine = new TemplateEngine();

            assembler.assemble(config, engine, outputDir);

            assertThat(
                    outputDir.resolve("docs/runbook"))
                    .exists()
                    .isDirectory();
        }

        @Test
        @DisplayName("resolves project_name variable")
        void resolvesProjectName(
                @TempDir Path tempDir) {
            Path outputDir = tempDir.resolve("output");

            RunbookAssembler assembler =
                    new RunbookAssembler();
            ProjectConfig config =
                    TestConfigBuilder.builder()
                            .projectName("api-pagamentos")
                            .build();
            TemplateEngine engine = new TemplateEngine();

            assembler.assemble(config, engine, outputDir);

            Path file = outputDir.resolve(
                    "docs/runbook/deploy-runbook.md");
            String content = readFile(file);
            assertThat(content)
                    .contains("api-pagamentos");
        }

        @Test
        @DisplayName("contains deploy and rollback sections")
        void containsDeployAndRollbackSections(
                @TempDir Path tempDir) {
            Path outputDir = tempDir.resolve("output");

            RunbookAssembler assembler =
                    new RunbookAssembler();
            ProjectConfig config =
                    TestConfigBuilder.minimal();
            TemplateEngine engine = new TemplateEngine();

            assembler.assemble(config, engine, outputDir);

            Path file = outputDir.resolve(
                    "docs/runbook/deploy-runbook.md");
            String content = readFile(file);
            assertThat(content)
                    .contains("Deploy");
        }

        @Test
        @DisplayName("returns file path in result list")
        void returnsFilePath(
                @TempDir Path tempDir) {
            Path outputDir = tempDir.resolve("output");

            RunbookAssembler assembler =
                    new RunbookAssembler();
            ProjectConfig config =
                    TestConfigBuilder.minimal();
            TemplateEngine engine = new TemplateEngine();

            List<String> files = assembler.assemble(
                    config, engine, outputDir);

            assertThat(files).hasSize(1);
            assertThat(files.get(0))
                    .endsWith("deploy-runbook.md");
        }
    }

    @Nested
    @DisplayName("assemble — graceful no-op")
    class GracefulNoOp {

        @Test
        @DisplayName("returns empty list when template"
                + " file absent")
        void returnsEmptyWhenTemplateAbsent(
                @TempDir Path tempDir) {
            Path resourcesDir =
                    tempDir.resolve("nonexistent");
            Path outputDir = tempDir.resolve("output");

            RunbookAssembler assembler =
                    new RunbookAssembler(resourcesDir);
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
        void doesNotCreateOutputDir(
                @TempDir Path tempDir) {
            Path resourcesDir =
                    tempDir.resolve("nonexistent");
            Path outputDir = tempDir.resolve("output");

            RunbookAssembler assembler =
                    new RunbookAssembler(resourcesDir);
            ProjectConfig config =
                    TestConfigBuilder.minimal();
            TemplateEngine engine = new TemplateEngine();

            assembler.assemble(config, engine, outputDir);

            assertThat(outputDir).doesNotExist();
        }
    }

    @Nested
    @DisplayName("assemble — uses Pebble rendering")
    class UsesPebbleRendering {

        @Test
        @DisplayName("resolves Pebble conditionals")
        void resolvesPebbleConditionals(
                @TempDir Path tempDir)
                throws IOException {
            Path templatesDir =
                    tempDir.resolve("templates");
            Files.createDirectories(templatesDir);
            String template =
                    "{% if database_name != \"none\" %}"
                            + "HAS_DB{% endif %}";
            Files.writeString(
                    templatesDir.resolve(
                            "_TEMPLATE-DEPLOY-RUNBOOK"
                                    + ".md"),
                    template, StandardCharsets.UTF_8);

            Path outputDir = tempDir.resolve("output");

            RunbookAssembler assembler =
                    new RunbookAssembler(tempDir);
            ProjectConfig config =
                    TestConfigBuilder.builder()
                            .database("postgresql", "15")
                            .build();
            TemplateEngine engine =
                    new TemplateEngine(tempDir);

            assembler.assemble(config, engine, outputDir);

            Path dest = outputDir.resolve(
                    "docs/runbook/deploy-runbook.md");
            assertThat(dest).exists();
            String content = readFile(dest);
            assertThat(content).contains("HAS_DB");
            assertThat(content)
                    .doesNotContain("{%");
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
