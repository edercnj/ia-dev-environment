package dev.iadev.assembler;

import dev.iadev.model.ProjectConfig;
import dev.iadev.template.TemplateEngine;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
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
        void assemble_whenCalled_generatesDockerCompose(
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

            CicdResult result = assembler.assemble(cicdCtx);

            assertThat(result.files()).hasSize(1);
            assertThat(result.files().get(0))
                    .contains("docker-compose.yml");
            assertThat(result.warnings()).isEmpty();
        }

        @Test
        @DisplayName("docker-compose.yml exists on disk")
        void assemble_whenCalled_fileExistsOnDisk(@TempDir Path tempDir) {
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
        void assemble_whenCalled_skipsDockerCompose(@TempDir Path tempDir) {
            DockerComposeAssembler assembler =
                    new DockerComposeAssembler();
            ProjectConfig config = TestConfigBuilder
                    .builder()
                    .container("none")
                    .build();
            CicdContext cicdCtx = buildContext(
                    config, tempDir);

            CicdResult result = assembler.assemble(cicdCtx);

            assertThat(result.files()).isEmpty();
            assertThat(result.warnings())
                    .anyMatch(w -> w.contains(
                            "Docker Compose skipped"));
        }
    }

    private static CicdContext buildContext(
            ProjectConfig config, Path outputDir) {
        Map<String, Object> ctx =
                CicdAssembler.buildStackContext(config);
        return new CicdContext(
                config, outputDir, resolveResources(),
                new TemplateEngine(), ctx);
    }

    private static Path resolveResources() {
        return dev.iadev.util.ResourceResolver
                .resolveResourcesRoot("cicd-templates");
    }
}
