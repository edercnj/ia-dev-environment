package dev.iadev.assembler;
import dev.iadev.model.ProjectConfig;
import dev.iadev.template.TemplateEngine;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.Path;
import java.util.List;
import static org.assertj.core.api.Assertions.assertThat;
/**
 * Tests for CicdAssembler — assemble method
 * conditional generation.
 */
@DisplayName("CicdAssembler — conditional gen")
class CicdAssembleConditionalTest {

    @Nested
    @DisplayName("CI workflow always generated")
    class CiWorkflow {

        @Test
        @DisplayName("generates ci.yml")
        void assemble_generatesCi(@TempDir Path tempDir) {
            Path out = tempDir.resolve("output");
            CicdAssembler assembler = new CicdAssembler();
            ProjectConfig config = TestConfigBuilder
                    .builder()
                    .language("java", "21")
                    .buildTool("maven")
                    .container("none")
                    .orchestrator("none")
                    .smokeTests(false)
                    .build();
            List<String> files = assembler.assemble(
                    config, new TemplateEngine(), out);
            assertThat(files).isNotEmpty();
            assertThat(files.stream()
                    .anyMatch(f -> f.contains("ci.yml")))
                    .isTrue();
            assertThat(out.resolve(
                    ".github/workflows/ci.yml"))
                    .exists();
        }
    }

    @Nested
    @DisplayName("Dockerfile conditional")
    class DockerfileConditional {

        @Test
        @DisplayName("generates Dockerfile when docker")
        void assemble_generatesDockerfile(
                @TempDir Path tempDir) {
            Path out = tempDir.resolve("output");
            List<String> files = new CicdAssembler()
                    .assemble(
                            TestConfigBuilder.builder()
                                    .container("docker")
                                    .language("java", "21")
                                    .buildTool("maven")
                                    .build(),
                            new TemplateEngine(), out);
            assertThat(files.stream()
                    .anyMatch(f ->
                            f.endsWith("Dockerfile")))
                    .isTrue();
        }

        @Test
        @DisplayName("skips Dockerfile when none")
        void assemble_skipsDockerfile(
                @TempDir Path tempDir) {
            Path out = tempDir.resolve("output");
            List<String> files = new CicdAssembler()
                    .assemble(
                            TestConfigBuilder.builder()
                                    .container("none")
                                    .orchestrator("none")
                                    .smokeTests(false)
                                    .build(),
                            new TemplateEngine(), out);
            assertThat(files.stream()
                    .noneMatch(f ->
                            f.endsWith("Dockerfile")))
                    .isTrue();
        }
    }

    @Nested
    @DisplayName("Docker Compose conditional")
    class DockerComposeConditional {

        @Test
        @DisplayName("generates when docker")
        void assemble_generatesCompose(
                @TempDir Path tempDir) {
            Path out = tempDir.resolve("output");
            List<String> files = new CicdAssembler()
                    .assemble(
                            TestConfigBuilder.builder()
                                    .container("docker")
                                    .language("java", "21")
                                    .buildTool("maven")
                                    .build(),
                            new TemplateEngine(), out);
            assertThat(files.stream()
                    .anyMatch(f -> f.contains(
                            "docker-compose.yml")))
                    .isTrue();
        }

        @Test
        @DisplayName("skips when none")
        void assemble_skipsCompose(
                @TempDir Path tempDir) {
            Path out = tempDir.resolve("output");
            List<String> files = new CicdAssembler()
                    .assemble(
                            TestConfigBuilder.builder()
                                    .container("none")
                                    .orchestrator("none")
                                    .smokeTests(false)
                                    .build(),
                            new TemplateEngine(), out);
            assertThat(files.stream()
                    .noneMatch(f -> f.contains(
                            "docker-compose.yml")))
                    .isTrue();
        }
    }

    @Nested
    @DisplayName("K8s manifests conditional")
    class K8sManifests {

        @Test
        @DisplayName("generates when kubernetes")
        void assemble_generatesK8s(@TempDir Path tempDir) {
            Path out = tempDir.resolve("output");
            List<String> files = new CicdAssembler()
                    .assemble(
                            TestConfigBuilder.builder()
                                    .container("docker")
                                    .orchestrator(
                                            "kubernetes")
                                    .language("java", "21")
                                    .buildTool("maven")
                                    .build(),
                            new TemplateEngine(), out);
            assertThat(files.stream()
                    .anyMatch(f -> f.contains(
                            "deployment.yaml")))
                    .isTrue();
        }

        @Test
        @DisplayName("skips when none")
        void assemble_skipsK8s(@TempDir Path tempDir) {
            Path out = tempDir.resolve("output");
            List<String> files = new CicdAssembler()
                    .assemble(
                            TestConfigBuilder.builder()
                                    .container("docker")
                                    .orchestrator("none")
                                    .language("java", "21")
                                    .buildTool("maven")
                                    .build(),
                            new TemplateEngine(), out);
            assertThat(files.stream()
                    .noneMatch(f -> f.contains(
                            "deployment.yaml")))
                    .isTrue();
        }
    }

    @Nested
    @DisplayName("Smoke test conditional")
    class SmokeTest {

        @Test
        @DisplayName("generates when true")
        void assemble_generatesSmoke(
                @TempDir Path tempDir) {
            Path out = tempDir.resolve("output");
            List<String> files = new CicdAssembler()
                    .assemble(
                            TestConfigBuilder.builder()
                                    .smokeTests(true)
                                    .container("none")
                                    .orchestrator("none")
                                    .build(),
                            new TemplateEngine(), out);
            assertThat(files.stream()
                    .anyMatch(f -> f.contains(
                            "smoke-config.md")))
                    .isTrue();
        }

        @Test
        @DisplayName("skips when false")
        void assemble_skipsSmoke(
                @TempDir Path tempDir) {
            Path out = tempDir.resolve("output");
            List<String> files = new CicdAssembler()
                    .assemble(
                            TestConfigBuilder.builder()
                                    .smokeTests(false)
                                    .container("none")
                                    .orchestrator("none")
                                    .build(),
                            new TemplateEngine(), out);
            assertThat(files.stream()
                    .noneMatch(f -> f.contains(
                            "smoke-config.md")))
                    .isTrue();
        }
    }

    @Nested
    @DisplayName("Full generation")
    class FullGeneration {

        @Test
        @DisplayName("generates all 11 artifacts")
        void assemble_generatesAll(@TempDir Path tempDir) {
            Path out = tempDir.resolve("output");
            List<String> files = new CicdAssembler()
                    .assemble(
                            TestConfigBuilder.builder()
                                    .language("java", "21")
                                    .buildTool("maven")
                                    .framework(
                                            "spring-boot",
                                            "3.2")
                                    .container("docker")
                                    .orchestrator(
                                            "kubernetes")
                                    .smokeTests(true)
                                    .build(),
                            new TemplateEngine(), out);
            assertThat(files).hasSize(11);
        }

        @Test
        @DisplayName("generates minimum artifacts")
        void assemble_generatesMinimum(
                @TempDir Path tempDir) {
            Path out = tempDir.resolve("output");
            List<String> files = new CicdAssembler()
                    .assemble(
                            TestConfigBuilder.builder()
                                    .container("none")
                                    .orchestrator("none")
                                    .smokeTests(false)
                                    .build(),
                            new TemplateEngine(), out);
            assertThat(files).hasSize(2);
            assertThat(files).anyMatch(
                    f -> f.contains("ci.yml"));
            assertThat(files).anyMatch(
                    f -> f.contains("release.yml"));
        }
    }
}
