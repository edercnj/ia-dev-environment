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
 * Tests for DocsContributingAssembler — generates
 * docs/templates/_TEMPLATE-CONTRIBUTING.md from a Pebble
 * template.
 */
@DisplayName("DocsContributingAssembler")
class DocsContributingAssemblerTest {

    @Nested
    @DisplayName("implements Assembler interface")
    class ImplementsAssembler {

        @Test
        @DisplayName("is instance of Assembler")
        void instanceOf_whenCreated_implementsAssembler() {
            DocsContributingAssembler assembler =
                    new DocsContributingAssembler();

            assertThat(assembler)
                    .isInstanceOf(Assembler.class);
        }
    }

    @Nested
    @DisplayName("assemble — generates contributing template")
    class AssembleContributing {

        @Test
        @DisplayName("generates _TEMPLATE-CONTRIBUTING.md"
                + " in docs/templates/ subdirectory")
        void assemble_whenCalled_generatesContributingFile(
                @TempDir Path tempDir) {
            Path outputDir = tempDir.resolve("output");

            DocsContributingAssembler assembler =
                    new DocsContributingAssembler();
            ProjectConfig config =
                    TestConfigBuilder.minimal();
            TemplateEngine engine = new TemplateEngine();

            List<String> files = assembler.assemble(
                    config, engine, outputDir);

            assertThat(files).hasSize(1);
            Path expected = outputDir.resolve(
                    "docs/templates/"
                            + "_TEMPLATE-CONTRIBUTING.md");
            assertThat(expected).exists();
        }

        @Test
        @DisplayName("creates docs/templates/ subdirectory")
        void assemble_whenCalled_createsDocsTemplatesSubdir(
                @TempDir Path tempDir) {
            Path outputDir = tempDir.resolve("output");

            DocsContributingAssembler assembler =
                    new DocsContributingAssembler();
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
        @DisplayName("resolves project_name variable")
        void assemble_whenCalled_resolvesProjectName(
                @TempDir Path tempDir) {
            Path outputDir = tempDir.resolve("output");

            DocsContributingAssembler assembler =
                    new DocsContributingAssembler();
            ProjectConfig config =
                    TestConfigBuilder.builder()
                            .projectName("payment-service")
                            .build();
            TemplateEngine engine = new TemplateEngine();

            assembler.assemble(config, engine, outputDir);

            Path file = outputDir.resolve(
                    "docs/templates/"
                            + "_TEMPLATE-CONTRIBUTING.md");
            String content = readFile(file);
            assertThat(content)
                    .contains("payment-service");
            assertThat(content)
                    .contains(
                            "# Contributing to payment-service");
        }

        @Test
        @DisplayName("resolves language and framework"
                + " variables")
        void assemble_whenCalled_resolvesLangAndFramework(
                @TempDir Path tempDir) {
            Path outputDir = tempDir.resolve("output");

            DocsContributingAssembler assembler =
                    new DocsContributingAssembler();
            ProjectConfig config =
                    TestConfigBuilder.builder()
                            .language("java", "21")
                            .framework("quarkus", "3.17")
                            .build();
            TemplateEngine engine = new TemplateEngine();

            assembler.assemble(config, engine, outputDir);

            Path file = outputDir.resolve(
                    "docs/templates/"
                            + "_TEMPLATE-CONTRIBUTING.md");
            String content = readFile(file);
            assertThat(content).contains("Java JDK");
            assertThat(content).contains("mvn test");
        }

        @Test
        @DisplayName("resolves build_tool variable for maven")
        void assemble_whenMaven_containsMavenCommands(
                @TempDir Path tempDir) {
            Path outputDir = tempDir.resolve("output");

            DocsContributingAssembler assembler =
                    new DocsContributingAssembler();
            ProjectConfig config =
                    TestConfigBuilder.builder()
                            .language("java", "21")
                            .buildTool("maven")
                            .build();
            TemplateEngine engine = new TemplateEngine();

            assembler.assemble(config, engine, outputDir);

            Path file = outputDir.resolve(
                    "docs/templates/"
                            + "_TEMPLATE-CONTRIBUTING.md");
            String content = readFile(file);
            assertThat(content)
                    .contains("mvn clean install");
            assertThat(content)
                    .contains("Apache Maven");
        }

        @Test
        @DisplayName("contains all 8 required sections")
        void assemble_whenCalled_containsAllSections(
                @TempDir Path tempDir) {
            Path outputDir = tempDir.resolve("output");

            DocsContributingAssembler assembler =
                    new DocsContributingAssembler();
            ProjectConfig config =
                    TestConfigBuilder.minimal();
            TemplateEngine engine = new TemplateEngine();

            assembler.assemble(config, engine, outputDir);

            Path file = outputDir.resolve(
                    "docs/templates/"
                            + "_TEMPLATE-CONTRIBUTING.md");
            String content = readFile(file);
            assertThat(content)
                    .contains("## Prerequisites");
            assertThat(content)
                    .contains("## Getting Started");
            assertThat(content)
                    .contains("## Development Workflow");
            assertThat(content)
                    .contains("## Code Standards");
            assertThat(content)
                    .contains("## Testing");
            assertThat(content)
                    .contains("## Pull Request Process");
            assertThat(content)
                    .contains("## Architecture Overview");
            assertThat(content)
                    .contains("## Code of Conduct");
        }

        @Test
        @DisplayName("returns file path in result list")
        void assemble_whenCalled_returnsFilePath(
                @TempDir Path tempDir) {
            Path outputDir = tempDir.resolve("output");

            DocsContributingAssembler assembler =
                    new DocsContributingAssembler();
            ProjectConfig config =
                    TestConfigBuilder.minimal();
            TemplateEngine engine = new TemplateEngine();

            List<String> files = assembler.assemble(
                    config, engine, outputDir);

            assertThat(files).hasSize(1);
            assertThat(files.get(0))
                    .endsWith(
                            "_TEMPLATE-CONTRIBUTING.md");
        }

        @Test
        @DisplayName("resolves coverage thresholds")
        void assemble_whenCalled_resolvesCoverageVars(
                @TempDir Path tempDir) {
            Path outputDir = tempDir.resolve("output");

            DocsContributingAssembler assembler =
                    new DocsContributingAssembler();
            ProjectConfig config =
                    TestConfigBuilder.minimal();
            TemplateEngine engine = new TemplateEngine();

            assembler.assemble(config, engine, outputDir);

            Path file = outputDir.resolve(
                    "docs/templates/"
                            + "_TEMPLATE-CONTRIBUTING.md");
            String content = readFile(file);
            assertThat(content).contains(">= 95%");
            assertThat(content).contains(">= 90%");
        }
    }

    @Nested
    @DisplayName("assemble — stack-specific content")
    class StackSpecific {

        @Test
        @DisplayName("TypeScript stack shows npm commands")
        void assemble_whenTypeScript_containsNpmCommands(
                @TempDir Path tempDir) {
            Path outputDir = tempDir.resolve("output");

            DocsContributingAssembler assembler =
                    new DocsContributingAssembler();
            ProjectConfig config =
                    TestConfigBuilder.builder()
                            .language("typescript", "20")
                            .framework("nestjs", "10")
                            .buildTool("npm")
                            .build();
            TemplateEngine engine = new TemplateEngine();

            assembler.assemble(config, engine, outputDir);

            Path file = outputDir.resolve(
                    "docs/templates/"
                            + "_TEMPLATE-CONTRIBUTING.md");
            String content = readFile(file);
            assertThat(content).contains("Node.js");
            assertThat(content).contains("npm install");
            assertThat(content).contains("npm test");
        }

        @Test
        @DisplayName("Go stack shows go commands")
        void assemble_whenGo_containsGoCommands(
                @TempDir Path tempDir) {
            Path outputDir = tempDir.resolve("output");

            DocsContributingAssembler assembler =
                    new DocsContributingAssembler();
            ProjectConfig config =
                    TestConfigBuilder.builder()
                            .language("go", "1.22")
                            .framework("gin", "1.9")
                            .buildTool("go")
                            .build();
            TemplateEngine engine = new TemplateEngine();

            assembler.assemble(config, engine, outputDir);

            Path file = outputDir.resolve(
                    "docs/templates/"
                            + "_TEMPLATE-CONTRIBUTING.md");
            String content = readFile(file);
            assertThat(content).contains("Go");
            assertThat(content)
                    .contains("go mod download");
            assertThat(content)
                    .contains("go test ./...");
        }
    }

    @Nested
    @DisplayName("assemble — graceful no-op")
    class GracefulNoOp {

        @Test
        @DisplayName("returns empty list when template"
                + " file absent")
        void assemble_whenAbsent_returnsEmptyList(
                @TempDir Path tempDir) {
            Path resourcesDir =
                    tempDir.resolve("nonexistent");
            Path outputDir = tempDir.resolve("output");

            DocsContributingAssembler assembler =
                    new DocsContributingAssembler(
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
        void assemble_whenAbsent_doesNotCreateOutputDir(
                @TempDir Path tempDir) {
            Path resourcesDir =
                    tempDir.resolve("nonexistent");
            Path outputDir = tempDir.resolve("output");

            DocsContributingAssembler assembler =
                    new DocsContributingAssembler(
                            resourcesDir);
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
        @DisplayName("uses engine.render for Pebble"
                + " conditionals")
        void assemble_whenCalled_usesRenderNotReplace(
                @TempDir Path tempDir)
                throws IOException {
            Path templatesDir =
                    tempDir.resolve("templates");
            Files.createDirectories(templatesDir);
            String template =
                    "{% if build_tool == \"maven\" %}"
                            + "USE_MAVEN"
                            + "{% endif %}";
            Files.writeString(
                    templatesDir.resolve(
                            "_TEMPLATE-CONTRIBUTING.md"),
                    template, StandardCharsets.UTF_8);

            Path outputDir = tempDir.resolve("output");

            DocsContributingAssembler assembler =
                    new DocsContributingAssembler(tempDir);
            ProjectConfig config =
                    TestConfigBuilder.builder()
                            .buildTool("maven")
                            .build();
            TemplateEngine engine =
                    new TemplateEngine(tempDir);

            assembler.assemble(config, engine, outputDir);

            Path dest = outputDir.resolve(
                    "docs/templates/"
                            + "_TEMPLATE-CONTRIBUTING.md");
            assertThat(dest).exists();
            String content = readFile(dest);
            assertThat(content).contains("USE_MAVEN");
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
