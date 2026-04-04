package dev.iadev.application.assembler;

import dev.iadev.config.ContextBuilder;
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
 * Tests for DocsAssembler — generates
 * docs/architecture/service-architecture.md from a Pebble
 * template.
 */
@DisplayName("DocsAssembler")
class DocsAssemblerTest {

    @Nested
    @DisplayName("implements Assembler interface")
    class ImplementsAssembler {

        @Test
        @DisplayName("is instance of Assembler")
        void instanceOf_whenCreated_implementsAssemblerInterface() {
            DocsAssembler assembler =
                    new DocsAssembler();

            assertThat(assembler)
                    .isInstanceOf(Assembler.class);
        }
    }

    @Nested
    @DisplayName("assemble — generates service-architecture.md")
    class AssembleDocs {

        @Test
        @DisplayName("generates service-architecture.md in"
                + " architecture/ subdirectory")
        void assemble_whenCalled_generatesServiceArchFile(
                @TempDir Path tempDir) {
            Path outputDir = tempDir.resolve("output");

            DocsAssembler assembler =
                    new DocsAssembler();
            ProjectConfig config =
                    TestConfigBuilder.minimal();
            TemplateEngine engine = new TemplateEngine();

            List<String> files = assembler.assemble(
                    config, engine, outputDir);

            assertThat(files).hasSize(1);
            Path expected = outputDir.resolve(
                    "architecture/service-architecture.md");
            assertThat(expected).exists();
        }

        @Test
        @DisplayName("creates architecture/ subdirectory")
        void assemble_whenCalled_createsArchitectureSubdir(
                @TempDir Path tempDir) {
            Path outputDir = tempDir.resolve("output");

            DocsAssembler assembler =
                    new DocsAssembler();
            ProjectConfig config =
                    TestConfigBuilder.minimal();
            TemplateEngine engine = new TemplateEngine();

            assembler.assemble(config, engine, outputDir);

            assertThat(
                    outputDir.resolve("architecture"))
                    .exists()
                    .isDirectory();
        }

        @Test
        @DisplayName("resolves project_name variable")
        void assemble_whenCalled_resolvesProjectName(
                @TempDir Path tempDir) {
            Path outputDir = tempDir.resolve("output");

            DocsAssembler assembler =
                    new DocsAssembler();
            ProjectConfig config =
                    TestConfigBuilder.builder()
                            .projectName("api-pagamentos")
                            .build();
            TemplateEngine engine = new TemplateEngine();

            assembler.assemble(config, engine, outputDir);

            Path file = outputDir.resolve(
                    "architecture/service-architecture.md");
            String content = readFile(file);
            assertThat(content)
                    .contains("api-pagamentos");
            assertThat(content)
                    .doesNotContain("{{ project_name }}");
        }

        @Test
        @DisplayName("resolves language and framework"
                + " variables")
        void assemble_whenCalled_resolvesLanguageAndFramework(
                @TempDir Path tempDir) {
            Path outputDir = tempDir.resolve("output");

            DocsAssembler assembler =
                    new DocsAssembler();
            ProjectConfig config =
                    TestConfigBuilder.builder()
                            .language("java", "21")
                            .framework("quarkus", "3.17")
                            .build();
            TemplateEngine engine = new TemplateEngine();

            assembler.assemble(config, engine, outputDir);

            Path file = outputDir.resolve(
                    "architecture/service-architecture.md");
            String content = readFile(file);
            assertThat(content).contains("java");
            assertThat(content).contains("quarkus");
        }

        @Test
        @DisplayName("resolves architecture_style variable")
        void assemble_whenCalled_resolvesArchStyle(
                @TempDir Path tempDir) {
            Path outputDir = tempDir.resolve("output");

            DocsAssembler assembler =
                    new DocsAssembler();
            ProjectConfig config =
                    TestConfigBuilder.builder()
                            .archStyle("hexagonal")
                            .build();
            TemplateEngine engine = new TemplateEngine();

            assembler.assemble(config, engine, outputDir);

            Path file = outputDir.resolve(
                    "architecture/service-architecture.md");
            String content = readFile(file);
            assertThat(content).contains("hexagonal");
        }

        @Test
        @DisplayName("no unresolved Pebble variables in"
                + " output")
        void assemble_noUnresolvedVariables_succeeds(
                @TempDir Path tempDir) {
            Path outputDir = tempDir.resolve("output");

            DocsAssembler assembler =
                    new DocsAssembler();
            ProjectConfig config =
                    TestConfigBuilder.builder()
                            .projectName("my-service")
                            .language("kotlin", "2.0")
                            .framework("ktor", "2.3")
                            .archStyle("clean")
                            .build();
            TemplateEngine engine = new TemplateEngine();

            assembler.assemble(config, engine, outputDir);

            Path file = outputDir.resolve(
                    "architecture/service-architecture.md");
            String content = readFile(file);
            assertThat(content)
                    .doesNotContain("{{ ");
        }

        @Test
        @DisplayName("returns file path in result list")
        void assemble_whenCalled_returnsFilePath(
                @TempDir Path tempDir) {
            Path outputDir = tempDir.resolve("output");

            DocsAssembler assembler =
                    new DocsAssembler();
            ProjectConfig config =
                    TestConfigBuilder.minimal();
            TemplateEngine engine = new TemplateEngine();

            List<String> files = assembler.assemble(
                    config, engine, outputDir);

            assertThat(files).hasSize(1);
            assertThat(files.get(0))
                    .endsWith("service-architecture.md");
        }
    }

    @Nested
    @DisplayName("assemble — graceful no-op")
    class GracefulNoOp {

        @Test
        @DisplayName("returns empty list when template"
                + " file absent")
        void assemble_whenCalled_returnsEmptyWhenTemplateAbsent(
                @TempDir Path tempDir) {
            Path resourcesDir =
                    tempDir.resolve("nonexistent");
            Path outputDir = tempDir.resolve("output");

            DocsAssembler assembler =
                    new DocsAssembler(resourcesDir);
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
        void assemble_whenCalled_doesNotCreateOutputDir(
                @TempDir Path tempDir) {
            Path resourcesDir =
                    tempDir.resolve("nonexistent");
            Path outputDir = tempDir.resolve("output");

            DocsAssembler assembler =
                    new DocsAssembler(resourcesDir);
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
        @DisplayName("uses engine.render not"
                + " replacePlaceholders")
        void assemble_whenCalled_usesRenderNotReplace(
                @TempDir Path tempDir)
                throws IOException {
            Path templatesDir =
                    tempDir.resolve("templates");
            Files.createDirectories(templatesDir);
            String template = "{% if framework_name"
                    + " %}fw={{ framework_name }}"
                    + "{% endif %}";
            Files.writeString(
                    templatesDir.resolve(
                            "_TEMPLATE-SERVICE-ARCHITECTURE"
                                    + ".md"),
                    template, StandardCharsets.UTF_8);

            Path outputDir = tempDir.resolve("output");

            DocsAssembler assembler =
                    new DocsAssembler(tempDir);
            ProjectConfig config =
                    TestConfigBuilder.builder()
                            .framework("quarkus", "3.17")
                            .build();
            TemplateEngine engine =
                    new TemplateEngine(tempDir);

            assembler.assemble(config, engine, outputDir);

            Path dest = outputDir.resolve(
                    "architecture/"
                            + "service-architecture.md");
            assertThat(dest).exists();
            String content = readFile(dest);
            assertThat(content).contains("fw=quarkus");
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
