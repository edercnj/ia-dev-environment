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
 * Tests for GrpcDocsAssembler — generates
 * docs/api/grpc-reference.md conditionally when the project
 * has a gRPC interface configured.
 */
@DisplayName("GrpcDocsAssembler")
class GrpcDocsAssemblerTest {

    @Nested
    @DisplayName("implements Assembler interface")
    class ImplementsAssembler {

        @Test
        @DisplayName("is instance of Assembler")
        void instanceOf_whenCreated_implementsAssemblerInterface() {
            GrpcDocsAssembler assembler =
                    new GrpcDocsAssembler();

            assertThat(assembler)
                    .isInstanceOf(Assembler.class);
        }
    }

    @Nested
    @DisplayName("assemble — grpc interface present")
    class GrpcPresent {

        @Test
        @DisplayName("generates grpc-reference.md in"
                + " api/ subdirectory")
        void assemble_whenCalled_generatesGrpcReferenceFile(
                @TempDir Path tempDir) {
            Path outputDir = tempDir.resolve("output");

            GrpcDocsAssembler assembler =
                    new GrpcDocsAssembler();
            ProjectConfig config =
                    TestConfigBuilder.builder()
                            .addInterface("grpc")
                            .build();
            TemplateEngine engine = new TemplateEngine();

            List<String> files = assembler.assemble(
                    config, engine, outputDir);

            assertThat(files).hasSize(1);
            Path expected = outputDir.resolve(
                    "api/grpc-reference.md");
            assertThat(expected).exists();
        }

        @Test
        @DisplayName("creates api/ subdirectory")
        void assemble_whenCalled_createsApiSubdir(
                @TempDir Path tempDir) {
            Path outputDir = tempDir.resolve("output");

            GrpcDocsAssembler assembler =
                    new GrpcDocsAssembler();
            ProjectConfig config =
                    TestConfigBuilder.builder()
                            .addInterface("grpc")
                            .build();
            TemplateEngine engine = new TemplateEngine();

            assembler.assemble(config, engine, outputDir);

            assertThat(outputDir.resolve("api"))
                    .exists()
                    .isDirectory();
        }

        @Test
        @DisplayName("resolves project_name variable")
        void assemble_whenCalled_resolvesProjectName(
                @TempDir Path tempDir) {
            Path outputDir = tempDir.resolve("output");

            GrpcDocsAssembler assembler =
                    new GrpcDocsAssembler();
            ProjectConfig config =
                    TestConfigBuilder.builder()
                            .projectName("api-pagamentos")
                            .addInterface("grpc")
                            .build();
            TemplateEngine engine = new TemplateEngine();

            assembler.assemble(config, engine, outputDir);

            Path file = outputDir.resolve(
                    "api/grpc-reference.md");
            String content = readFile(file);
            assertThat(content)
                    .contains("api-pagamentos");
            assertThat(content)
                    .doesNotContain("{{ project_name }}");
        }

        @Test
        @DisplayName("resolves framework_name variable")
        void assemble_whenCalled_resolvesFrameworkName(
                @TempDir Path tempDir) {
            Path outputDir = tempDir.resolve("output");

            GrpcDocsAssembler assembler =
                    new GrpcDocsAssembler();
            ProjectConfig config =
                    TestConfigBuilder.builder()
                            .framework("quarkus", "3.17")
                            .addInterface("grpc")
                            .build();
            TemplateEngine engine = new TemplateEngine();

            assembler.assemble(config, engine, outputDir);

            Path file = outputDir.resolve(
                    "api/grpc-reference.md");
            String content = readFile(file);
            assertThat(content).contains("quarkus");
        }

        @Test
        @DisplayName("no unresolved Pebble variables")
        void assemble_noUnresolvedVariables_succeeds(
                @TempDir Path tempDir) {
            Path outputDir = tempDir.resolve("output");

            GrpcDocsAssembler assembler =
                    new GrpcDocsAssembler();
            ProjectConfig config =
                    TestConfigBuilder.builder()
                            .projectName("my-grpc-svc")
                            .language("java", "21")
                            .framework("quarkus", "3.17")
                            .addInterface("grpc")
                            .build();
            TemplateEngine engine = new TemplateEngine();

            assembler.assemble(config, engine, outputDir);

            Path file = outputDir.resolve(
                    "api/grpc-reference.md");
            String content = readFile(file);
            assertThat(content)
                    .doesNotContain("{{ ");
        }

        @Test
        @DisplayName("returns file path in result list")
        void assemble_whenCalled_returnsFilePath(
                @TempDir Path tempDir) {
            Path outputDir = tempDir.resolve("output");

            GrpcDocsAssembler assembler =
                    new GrpcDocsAssembler();
            ProjectConfig config =
                    TestConfigBuilder.builder()
                            .addInterface("grpc")
                            .build();
            TemplateEngine engine = new TemplateEngine();

            List<String> files = assembler.assemble(
                    config, engine, outputDir);

            assertThat(files).hasSize(1);
            assertThat(files.get(0))
                    .endsWith("grpc-reference.md");
        }
    }

    @Nested
    @DisplayName("assemble — grpc interface absent")
    class GrpcAbsent {

        @Test
        @DisplayName("returns empty list when no grpc"
                + " interface")
        void assemble_whenCalled_returnsEmptyWhenNoGrpc(
                @TempDir Path tempDir) {
            Path outputDir = tempDir.resolve("output");

            GrpcDocsAssembler assembler =
                    new GrpcDocsAssembler();
            ProjectConfig config =
                    TestConfigBuilder.minimal();
            TemplateEngine engine = new TemplateEngine();

            List<String> files = assembler.assemble(
                    config, engine, outputDir);

            assertThat(files).isEmpty();
        }

        @Test
        @DisplayName("returns empty list when only rest"
                + " interface")
        void assemble_whenCalled_returnsEmptyWhenOnlyRest(
                @TempDir Path tempDir) {
            Path outputDir = tempDir.resolve("output");

            GrpcDocsAssembler assembler =
                    new GrpcDocsAssembler();
            ProjectConfig config =
                    TestConfigBuilder.builder()
                            .clearInterfaces()
                            .addInterface("rest")
                            .build();
            TemplateEngine engine = new TemplateEngine();

            List<String> files = assembler.assemble(
                    config, engine, outputDir);

            assertThat(files).isEmpty();
        }

        @Test
        @DisplayName("does not create api/ directory"
                + " when grpc absent")
        void assemble_whenCalled_doesNotCreateOutputDir(
                @TempDir Path tempDir) {
            Path outputDir = tempDir.resolve("output");

            GrpcDocsAssembler assembler =
                    new GrpcDocsAssembler();
            ProjectConfig config =
                    TestConfigBuilder.minimal();
            TemplateEngine engine = new TemplateEngine();

            assembler.assemble(config, engine, outputDir);

            assertThat(outputDir).doesNotExist();
        }
    }

    @Nested
    @DisplayName("assemble — double graceful no-op")
    class DoubleGracefulNoOp {

        @Test
        @DisplayName("returns empty list when grpc present"
                + " but template absent")
        void assemble_whenCalled_returnsEmptyWhenTemplateAbsent(
                @TempDir Path tempDir) {
            Path resourcesDir =
                    tempDir.resolve("nonexistent");
            Path outputDir = tempDir.resolve("output");

            GrpcDocsAssembler assembler =
                    new GrpcDocsAssembler(resourcesDir);
            ProjectConfig config =
                    TestConfigBuilder.builder()
                            .addInterface("grpc")
                            .build();
            TemplateEngine engine = new TemplateEngine();

            List<String> files = assembler.assemble(
                    config, engine, outputDir);

            assertThat(files).isEmpty();
        }

        @Test
        @DisplayName("does not create output dir when"
                + " template absent")
        void assemble_whenCalled_doesNotCreateOutputDirForMissingTemplate(
                @TempDir Path tempDir) {
            Path resourcesDir =
                    tempDir.resolve("nonexistent");
            Path outputDir = tempDir.resolve("output");

            GrpcDocsAssembler assembler =
                    new GrpcDocsAssembler(resourcesDir);
            ProjectConfig config =
                    TestConfigBuilder.builder()
                            .addInterface("grpc")
                            .build();
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
            String template = "{% if language_name"
                    + " %}lang={{ language_name }}"
                    + "{% endif %}";
            Files.writeString(
                    templatesDir.resolve(
                            "_TEMPLATE-GRPC-REFERENCE.md"),
                    template, StandardCharsets.UTF_8);

            Path outputDir = tempDir.resolve("output");

            GrpcDocsAssembler assembler =
                    new GrpcDocsAssembler(tempDir);
            ProjectConfig config =
                    TestConfigBuilder.builder()
                            .language("java", "21")
                            .addInterface("grpc")
                            .build();
            TemplateEngine engine =
                    new TemplateEngine(tempDir);

            assembler.assemble(config, engine, outputDir);

            Path dest = outputDir.resolve(
                    "api/grpc-reference.md");
            assertThat(dest).exists();
            String content = readFile(dest);
            assertThat(content).contains("lang=java");
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
