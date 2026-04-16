package dev.iadev.application.assembler;

import dev.iadev.testutil.TestConfigBuilder;

import dev.iadev.domain.model.ProjectConfig;
import dev.iadev.template.TemplateEngine;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for DockerfileAssembler — generates Dockerfile
 * conditionally.
 */
@DisplayName("DockerfileAssembler")
class DockerfileAssemblerTest {

    @Nested
    @DisplayName("assemble — container=docker")
    class ContainerDocker {

        @Test
        @DisplayName("generates Dockerfile for"
                + " java-maven")
        void assemble_whenCalled_generatesDockerfile(@TempDir Path tempDir) {
            DockerfileAssembler assembler =
                    new DockerfileAssembler();
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
                    .endsWith("Dockerfile");
            assertThat(result.warnings()).isEmpty();
        }

        @Test
        @DisplayName("Dockerfile exists on disk")
        void assemble_whenCalled_dockerfileExistsOnDisk(
                @TempDir Path tempDir) {
            DockerfileAssembler assembler =
                    new DockerfileAssembler();
            ProjectConfig config = TestConfigBuilder
                    .builder()
                    .container("docker")
                    .language("java", "21")
                    .buildTool("maven")
                    .build();
            CicdContext cicdCtx = buildContext(
                    config, tempDir);

            assembler.assemble(cicdCtx);

            assertThat(tempDir.resolve("Dockerfile"))
                    .exists();
        }
    }

    @Nested
    @DisplayName("assemble — container=none")
    class ContainerNone {

        @Test
        @DisplayName("skips Dockerfile when"
                + " container=none")
        void assemble_whenCalled_skipsDockerfile(@TempDir Path tempDir) {
            DockerfileAssembler assembler =
                    new DockerfileAssembler();
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
                            "Dockerfile skipped"));
        }
    }

    @Nested
    @DisplayName("assemble — template not found")
    class TemplateNotFound {

        @Test
        @DisplayName("warns when template not found"
                + " for unknown stack")
        void assemble_whenCalled_warnsForUnknownStack(@TempDir Path tempDir)
                throws IOException {
            DockerfileAssembler assembler =
                    new DockerfileAssembler();
            Path resDir = tempDir.resolve("res");
            Files.createDirectories(
                    resDir.resolve("shared/cicd-templates"));
            Path outputDir = tempDir.resolve("output");

            ProjectConfig config = TestConfigBuilder
                    .builder()
                    .container("docker")
                    .language("cobol", "85")
                    .buildTool("none")
                    .build();
            Map<String, Object> ctx =
                    CicdAssembler.buildStackContext(config);
            CicdContext cicdCtx = new CicdContext(
                    config, outputDir, resDir,
                    new TemplateEngine(), ctx);

            CicdResult result = assembler.assemble(cicdCtx);

            assertThat(result.files()).isEmpty();
            assertThat(result.warnings())
                    .anyMatch(w -> w.contains(
                            "template not found"));
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
                .resolveResourceDir("shared")
                .getParent();
    }
}
