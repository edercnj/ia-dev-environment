package dev.iadev.application.assembler;

import dev.iadev.testutil.TestConfigBuilder;

import dev.iadev.config.ContextBuilder;
import dev.iadev.domain.model.ProjectConfig;
import dev.iadev.template.TemplateEngine;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for DockerComposeAssembler — generates
 * docker-compose.yml conditionally.
 */
@DisplayName("DockerComposeAssembler")
class DockerComposeAssemblerTest {

    @Nested
    @DisplayName("assemble — container=docker")
    class ContainerDocker {

        @Test
        @DisplayName("generates docker-compose.yml")
        void assemble_containerDocker_generatesFile(
                @TempDir Path tempDir) {
            DockerComposeAssembler assembler =
                    new DockerComposeAssembler();
            ProjectConfig config = TestConfigBuilder
                    .builder()
                    .container("docker")
                    .language("java", "21")
                    .buildTool("maven")
                    .build();
            CicdContext cicdCtx = buildContext(
                    config, tempDir);

            CicdResult result =
                    assembler.assemble(cicdCtx);

            assertThat(result.files()).hasSize(1);
            assertThat(result.files().get(0))
                    .contains("docker-compose.yml");
            assertThat(result.warnings()).isEmpty();
        }

        @Test
        @DisplayName("docker-compose.yml exists on disk")
        void assemble_containerDocker_fileExists(
                @TempDir Path tempDir) {
            DockerComposeAssembler assembler =
                    new DockerComposeAssembler();
            ProjectConfig config = TestConfigBuilder
                    .builder()
                    .container("docker")
                    .language("java", "21")
                    .buildTool("maven")
                    .build();
            CicdContext cicdCtx = buildContext(
                    config, tempDir);

            assembler.assemble(cicdCtx);

            assertThat(tempDir.resolve(
                    "docker-compose.yml"))
                    .exists();
        }
    }

    @Nested
    @DisplayName("assemble — container=none")
    class ContainerNone {

        @Test
        @DisplayName("skips docker-compose.yml when"
                + " container=none")
        void assemble_containerNone_skips(
                @TempDir Path tempDir) {
            DockerComposeAssembler assembler =
                    new DockerComposeAssembler();
            ProjectConfig config = TestConfigBuilder
                    .builder()
                    .container("none")
                    .build();
            CicdContext cicdCtx = buildContext(
                    config, tempDir);

            CicdResult result =
                    assembler.assemble(cicdCtx);

            assertThat(result.files()).isEmpty();
            assertThat(result.warnings())
                    .anyMatch(w -> w.contains(
                            "Docker Compose skipped"));
        }
    }

    @Nested
    @DisplayName("assemble — style=cqrs EventStoreDB")
    class CqrsEventStore {

        @Test
        @DisplayName("includes eventstore service when"
                + " style=cqrs")
        void assemble_styleCqrs_containsEventstore(
                @TempDir Path tempDir) throws Exception {
            DockerComposeAssembler assembler =
                    new DockerComposeAssembler();
            ProjectConfig config = TestConfigBuilder
                    .builder()
                    .container("docker")
                    .archStyle("cqrs")
                    .language("java", "21")
                    .buildTool("maven")
                    .build();
            CicdContext cicdCtx = buildContext(
                    config, tempDir);

            assembler.assemble(cicdCtx);

            String content = Files.readString(
                    tempDir.resolve("docker-compose.yml"),
                    StandardCharsets.UTF_8);
            assertThat(content)
                    .contains("eventstore:");
            assertThat(content)
                    .contains(
                            "eventstore/eventstore");
            assertThat(content)
                    .contains("2113:2113");
            assertThat(content)
                    .contains("EVENTSTORE_INSECURE=true");
            assertThat(content)
                    .contains("eventstore-data:");
        }

        @Test
        @DisplayName("includes health check for"
                + " eventstore")
        void assemble_styleCqrs_hasHealthcheck(
                @TempDir Path tempDir) throws Exception {
            DockerComposeAssembler assembler =
                    new DockerComposeAssembler();
            ProjectConfig config = TestConfigBuilder
                    .builder()
                    .container("docker")
                    .archStyle("cqrs")
                    .language("java", "21")
                    .buildTool("maven")
                    .build();
            CicdContext cicdCtx = buildContext(
                    config, tempDir);

            assembler.assemble(cicdCtx);

            String content = Files.readString(
                    tempDir.resolve("docker-compose.yml"),
                    StandardCharsets.UTF_8);
            assertThat(content)
                    .contains("healthcheck:");
            assertThat(content)
                    .contains("health/live");
        }

        @Test
        @DisplayName("excludes eventstore when style"
                + " is not cqrs")
        void assemble_styleNotCqrs_excludesEventstore(
                @TempDir Path tempDir) throws Exception {
            DockerComposeAssembler assembler =
                    new DockerComposeAssembler();
            ProjectConfig config = TestConfigBuilder
                    .builder()
                    .container("docker")
                    .archStyle("microservice")
                    .language("java", "21")
                    .buildTool("maven")
                    .build();
            CicdContext cicdCtx = buildContext(
                    config, tempDir);

            assembler.assemble(cicdCtx);

            String content = Files.readString(
                    tempDir.resolve("docker-compose.yml"),
                    StandardCharsets.UTF_8);
            assertThat(content)
                    .doesNotContain("eventstore:");
            assertThat(content)
                    .doesNotContain(
                            "eventstore/eventstore");
        }
    }

    private static CicdContext buildContext(
            ProjectConfig config, Path outputDir) {
        Map<String, Object> stackCtx =
                CicdAssembler.buildStackContext(config);
        Map<String, Object> fullCtx =
                new LinkedHashMap<>(
                        ContextBuilder.buildContext(config));
        fullCtx.putAll(stackCtx);
        return new CicdContext(
                config, outputDir, resolveResources(),
                new TemplateEngine(), fullCtx);
    }

    private static Path resolveResources() {
        return dev.iadev.util.ResourceResolver
                .resolveResourcesRoot("cicd-templates");
    }
}
