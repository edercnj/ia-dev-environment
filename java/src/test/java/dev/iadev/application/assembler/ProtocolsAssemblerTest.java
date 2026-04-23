package dev.iadev.application.assembler;

import dev.iadev.testutil.TestConfigBuilder;

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
 * Tests for ProtocolsAssembler — the fifth assembler in
 * the pipeline, generating .claude/knowledge/protocols/
 * references with concatenated protocol convention files.
 */
@DisplayName("ProtocolsAssembler")
class ProtocolsAssemblerTest {

    @Nested
    @DisplayName("assemble — implements Assembler interface")
    class ImplementsAssembler {

        @Test
        @DisplayName("is instance of Assembler")
        void instanceOf_whenCreated_implementsAssemblerInterface() {
            ProtocolsAssembler assembler =
                    new ProtocolsAssembler();

            assertThat(assembler)
                    .isInstanceOf(Assembler.class);
        }
    }

    @Nested
    @DisplayName("assemble — REST interface generates"
            + " REST/OpenAPI conventions")
    class RestProtocol {

        @Test
        @DisplayName("interfaces=[rest] generates"
                + " rest-conventions.md")
        void assemble_rest_generatesConventions(
                @TempDir Path tempDir)
                throws IOException {
            Path outputDir = tempDir.resolve("output");
            Files.createDirectories(outputDir);

            ProtocolsAssembler assembler =
                    new ProtocolsAssembler();
            ProjectConfig config =
                    TestConfigBuilder.builder()
                            .clearInterfaces()
                            .addInterface("rest")
                            .build();

            List<String> files = assembler.assemble(
                    config, new TemplateEngine(), outputDir);

            assertThat(files).isNotEmpty();
            Path refsDir = outputDir.resolve(
                    "knowledge/protocols");
            assertThat(refsDir.resolve(
                    "rest-conventions.md"))
                    .exists();
        }

        @Test
        @DisplayName("rest-conventions.md contains"
                + " OpenAPI content")
        void assemble_whenCalled_restConventionsContainOpenapi(
                @TempDir Path tempDir)
                throws IOException {
            Path outputDir = tempDir.resolve("output");
            Files.createDirectories(outputDir);

            ProtocolsAssembler assembler =
                    new ProtocolsAssembler();
            ProjectConfig config =
                    TestConfigBuilder.builder()
                            .clearInterfaces()
                            .addInterface("rest")
                            .build();

            assembler.assemble(
                    config, new TemplateEngine(), outputDir);

            String content = Files.readString(
                    outputDir.resolve(
                            "knowledge/protocols/"
                                    + "rest-conventions.md"),
                    StandardCharsets.UTF_8);
            assertThat(content)
                    .contains("OpenAPI");
        }
    }

    @Nested
    @DisplayName("assemble — gRPC interface generates"
            + " gRPC/Proto3 conventions")
    class GrpcProtocol {

        @Test
        @DisplayName("interfaces=[grpc] generates"
                + " grpc-conventions.md")
        void assemble_grpc_generatesConventions(
                @TempDir Path tempDir)
                throws IOException {
            Path outputDir = tempDir.resolve("output");
            Files.createDirectories(outputDir);

            ProtocolsAssembler assembler =
                    new ProtocolsAssembler();
            ProjectConfig config =
                    TestConfigBuilder.builder()
                            .clearInterfaces()
                            .addInterface("grpc")
                            .build();

            List<String> files = assembler.assemble(
                    config, new TemplateEngine(), outputDir);

            assertThat(files).isNotEmpty();
            Path refsDir = outputDir.resolve(
                    "knowledge/protocols");
            assertThat(refsDir.resolve(
                    "grpc-conventions.md"))
                    .exists();
        }

        @Test
        @DisplayName("grpc-conventions.md contains"
                + " Proto3 content")
        void assemble_whenCalled_grpcConventionsContainProto3(
                @TempDir Path tempDir)
                throws IOException {
            Path outputDir = tempDir.resolve("output");
            Files.createDirectories(outputDir);

            ProtocolsAssembler assembler =
                    new ProtocolsAssembler();
            ProjectConfig config =
                    TestConfigBuilder.builder()
                            .clearInterfaces()
                            .addInterface("grpc")
                            .build();

            assembler.assemble(
                    config, new TemplateEngine(), outputDir);

            String content = Files.readString(
                    outputDir.resolve(
                            "knowledge/protocols/"
                                    + "grpc-conventions.md"),
                    StandardCharsets.UTF_8);
            assertThat(content)
                    .contains("Proto3");
        }
    }

    @Nested
    @DisplayName("assemble — no interfaces generates"
            + " no files")
    class NoInterfaces {

        @Test
        @DisplayName("empty interfaces returns empty list")
        void assemble_emptyInterfaces_returnsEmpty(
                @TempDir Path tempDir)
                throws IOException {
            Path outputDir = tempDir.resolve("output");
            Files.createDirectories(outputDir);

            ProtocolsAssembler assembler =
                    new ProtocolsAssembler();
            ProjectConfig config =
                    TestConfigBuilder.builder()
                            .clearInterfaces()
                            .addInterface("cli")
                            .build();

            List<String> files = assembler.assemble(
                    config, new TemplateEngine(), outputDir);

            assertThat(files).isEmpty();
        }

        @Test
        @DisplayName("cli-only config creates no"
                + " protocols directory")
        void assemble_cliOnly_createsNoDir(
                @TempDir Path tempDir)
                throws IOException {
            Path outputDir = tempDir.resolve("output");
            Files.createDirectories(outputDir);

            ProtocolsAssembler assembler =
                    new ProtocolsAssembler();
            ProjectConfig config =
                    TestConfigBuilder.builder()
                            .clearInterfaces()
                            .addInterface("cli")
                            .build();

            assembler.assemble(
                    config, new TemplateEngine(), outputDir);

            Path refsDir = outputDir.resolve(
                    "knowledge/protocols");
            assertThat(refsDir).doesNotExist();
        }
    }

    @Nested
    @DisplayName("assemble — event interfaces with broker")
    class EventWithBroker {

        @Test
        @DisplayName("event-consumer with kafka generates"
                + " messaging-conventions.md with kafka"
                + " content")
        void assemble_whenCalled_kafkaBrokerSelectsKafka(
                @TempDir Path tempDir)
                throws IOException {
            Path outputDir = tempDir.resolve("output");
            Files.createDirectories(outputDir);

            ProtocolsAssembler assembler =
                    new ProtocolsAssembler();
            ProjectConfig config =
                    TestConfigBuilder.builder()
                            .clearInterfaces()
                            .addInterface(
                                    "event-consumer",
                                    "", "kafka")
                            .build();

            assembler.assemble(
                    config, new TemplateEngine(), outputDir);

            Path refsDir = outputDir.resolve(
                    "knowledge/protocols");
            assertThat(refsDir.resolve(
                    "messaging-conventions.md"))
                    .exists();
            assertThat(refsDir.resolve(
                    "event-driven-conventions.md"))
                    .exists();
        }
    }

    @Nested
    @DisplayName("assemble — golden file parity")
    class GoldenFile {

        @Test
        @DisplayName("all protocol convention files match"
                + " golden files for go-gin profile")
        void assemble_allProtocolsMatchGolden_succeeds(
                @TempDir Path tempDir)
                throws IOException {
            Path outputDir = tempDir.resolve("output");
            Files.createDirectories(outputDir);

            ProtocolsAssembler assembler =
                    new ProtocolsAssembler();
            ProjectConfig config = buildGoGinConfig();

            assembler.assemble(
                    config, new TemplateEngine(), outputDir);

            String[] conventionFiles = {
                    "event-driven-conventions.md",
                    "grpc-conventions.md",
                    "messaging-conventions.md",
                    "rest-conventions.md"
            };

            for (String file : conventionFiles) {
                String goldenPath =
                        "golden/go-gin/.claude/skills/"
                                + "protocols/references/"
                                + file;
                String expected =
                        loadResource(goldenPath);
                if (expected != null) {
                    String actual = Files.readString(
                            outputDir.resolve(
                                    "knowledge/protocols/"
                                            + "references/"
                                            + file),
                            StandardCharsets.UTF_8);
                    assertThat(actual)
                            .as("Protocol: " + file)
                            .isEqualTo(expected);
                }
            }
        }

        @Test
        @DisplayName("go-gin generates exactly 4"
                + " convention files")
        void assemble_goGin_generatesExactlyFour(
                @TempDir Path tempDir)
                throws IOException {
            Path outputDir = tempDir.resolve("output");
            Files.createDirectories(outputDir);

            ProtocolsAssembler assembler =
                    new ProtocolsAssembler();
            ProjectConfig config = buildGoGinConfig();

            List<String> files = assembler.assemble(
                    config, new TemplateEngine(), outputDir);

            assertThat(files).hasSize(4);
        }

        private String loadResource(String path) {
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

    @Nested
    @DisplayName("assemble — edge cases")
    class EdgeCases {

        @Test
        @DisplayName("custom resourcesDir with no"
                + " protocols dir returns empty")
        void assemble_emptyResources_returnsEmpty(
                @TempDir Path tempDir)
                throws IOException {
            Path resourceDir = tempDir.resolve("res");
            Files.createDirectories(resourceDir);
            Path outputDir = tempDir.resolve("output");
            Files.createDirectories(outputDir);

            ProtocolsAssembler assembler =
                    new ProtocolsAssembler(resourceDir);
            ProjectConfig config =
                    TestConfigBuilder.builder()
                            .clearInterfaces()
                            .addInterface("rest")
                            .build();

            List<String> files = assembler.assemble(
                    config, new TemplateEngine(), outputDir);

            assertThat(files).isEmpty();
        }

        @Test
        @DisplayName("multiple event interfaces produce"
                + " deduplicated protocols")
        void assemble_whenCalled_deduplicatedEventProtocols(
                @TempDir Path tempDir)
                throws IOException {
            Path outputDir = tempDir.resolve("output");
            Files.createDirectories(outputDir);

            ProtocolsAssembler assembler =
                    new ProtocolsAssembler();
            ProjectConfig config =
                    TestConfigBuilder.builder()
                            .clearInterfaces()
                            .addInterface(
                                    "event-consumer",
                                    "", "kafka")
                            .addInterface(
                                    "event-producer",
                                    "", "kafka")
                            .build();

            List<String> files = assembler.assemble(
                    config, new TemplateEngine(), outputDir);

            // event-driven and messaging (deduplicated)
            assertThat(files).hasSize(2);
        }
    }

    private static ProjectConfig buildGoGinConfig() {
        var builder = TestConfigBuilder.builder()
                .projectName("my-go-service")
                .purpose(
                        "Describe your service purpose here")
                .archStyle("microservice")
                .domainDriven(false)
                .eventDriven(true)
                .language("go", "1.22")
                .framework("gin", "")
                .buildTool("go-mod")
                .nativeBuild(false);
        return configureGoInfra(builder).build();
    }

    private static TestConfigBuilder configureGoInfra(
            TestConfigBuilder builder) {
        return builder
                .container("docker")
                .orchestrator("kubernetes")
                .iac("terraform")
                .apiGateway("kong")
                .smokeTests(true)
                .contractTests(false)
                .performanceTests(true)
                .clearInterfaces()
                .addInterface("rest")
                .addInterface("grpc")
                .addInterface("event-consumer",
                        "", "kafka")
                .addInterface("event-producer",
                        "", "kafka");
    }
}
