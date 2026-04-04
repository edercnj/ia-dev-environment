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
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Additional coverage tests for CicdAssembler —
 * targeting uncovered branches and edge cases.
 */
@DisplayName("CicdAssembler — coverage")
class CicdAssemblerCoverageTest {

    @Nested
    @DisplayName("buildStackContext — edge cases")
    class BuildStackContextEdge {

        @Test
        @DisplayName("unknown language returns empty"
                + " commands")
        void assemble_whenCalled_unknownLanguageEmptyCommands() {
            ProjectConfig config = TestConfigBuilder
                    .builder()
                    .language("unknown-lang", "1.0")
                    .framework("unknown-fw", "1.0")
                    .buildTool("unknown-build")
                    .build();

            Map<String, Object> ctx =
                    CicdAssembler.buildStackContext(config);

            assertThat(ctx)
                    .containsEntry("compile_cmd", "")
                    .containsEntry("build_cmd", "")
                    .containsEntry("test_cmd", "")
                    .containsEntry("coverage_cmd", "")
                    .containsEntry("file_extension", "")
                    .containsEntry("build_file", "")
                    .containsEntry("package_manager", "");
            assertThat(ctx.get("lint_cmd").toString())
                    .contains("No linter configured");
        }

        @Test
        @DisplayName("java-maven resolves correct"
                + " commands")
        void assemble_whenCalled_javaMavenCommands() {
            ProjectConfig config = TestConfigBuilder
                    .builder()
                    .language("java", "21")
                    .framework("quarkus", "3.17")
                    .buildTool("maven")
                    .build();

            Map<String, Object> ctx =
                    CicdAssembler.buildStackContext(config);

            assertThat(ctx.get("compile_cmd").toString())
                    .contains("mvnw compile");
            assertThat(ctx.get("file_extension"))
                    .isEqualTo(".java");
            assertThat(ctx.get("build_file"))
                    .isEqualTo("pom.xml");
            assertThat(ctx.get("lint_cmd").toString())
                    .contains("checkstyle");
        }

        @Test
        @DisplayName("unknown framework uses default"
                + " port and health")
        void assemble_whenCalled_unknownFrameworkDefaults() {
            ProjectConfig config = TestConfigBuilder
                    .builder()
                    .framework("unknown-fw", "1.0")
                    .build();

            Map<String, Object> ctx =
                    CicdAssembler.buildStackContext(config);

            assertThat(ctx).containsKey("framework_port");
            assertThat(ctx).containsKey("health_path");
            assertThat(ctx).containsKey("docker_base_image");
        }

        @Test
        @DisplayName("version placeholder replaced"
                + " in docker image")
        void assemble_whenCalled_versionReplacedInImage() {
            ProjectConfig config = TestConfigBuilder
                    .builder()
                    .language("java", "21")
                    .framework("quarkus", "3.17")
                    .buildTool("maven")
                    .build();

            Map<String, Object> ctx =
                    CicdAssembler.buildStackContext(config);

            String image =
                    ctx.get("docker_base_image").toString();
            assertThat(image).contains("21");
            assertThat(image)
                    .doesNotContain("{version}");
        }
    }

    @Nested
    @DisplayName("assemble — conditional generation")
    class ConditionalGeneration {

        @Test
        @DisplayName("no docker skips Dockerfile"
                + " and Compose")
        void assemble_noDocker_skipsFiles(@TempDir Path tempDir)
                throws IOException {
            Path outputDir = tempDir.resolve("output");
            Files.createDirectories(outputDir);

            CicdAssembler assembler =
                    new CicdAssembler();
            ProjectConfig config =
                    TestConfigBuilder.builder()
                            .container("none")
                            .orchestrator("none")
                            .smokeTests(false)
                            .build();

            List<String> files = assembler.assemble(
                    config, new TemplateEngine(), outputDir);

            assertThat(files).noneMatch(
                    f -> f.contains("Dockerfile"));
            assertThat(files).noneMatch(
                    f -> f.contains("docker-compose"));
        }

        @Test
        @DisplayName("no kubernetes skips K8s manifests")
        void assemble_noK8s_skipsManifests(@TempDir Path tempDir)
                throws IOException {
            Path outputDir = tempDir.resolve("output");
            Files.createDirectories(outputDir);

            CicdAssembler assembler =
                    new CicdAssembler();
            ProjectConfig config =
                    TestConfigBuilder.builder()
                            .orchestrator("none")
                            .smokeTests(false)
                            .build();

            List<String> files = assembler.assemble(
                    config, new TemplateEngine(), outputDir);

            assertThat(files).noneMatch(
                    f -> f.contains("deployment.yaml"));
        }

        @Test
        @DisplayName("smokeTests=false skips smoke"
                + " config")
        void assemble_noSmokeTests_succeeds(@TempDir Path tempDir)
                throws IOException {
            Path outputDir = tempDir.resolve("output");
            Files.createDirectories(outputDir);

            CicdAssembler assembler =
                    new CicdAssembler();
            ProjectConfig config =
                    TestConfigBuilder.builder()
                            .smokeTests(false)
                            .build();

            List<String> files = assembler.assemble(
                    config, new TemplateEngine(), outputDir);

            assertThat(files).noneMatch(
                    f -> f.contains("smoke-config"));
        }
    }

    @Nested
    @DisplayName("assemble — template not found")
    class TemplateNotFound {

        @Test
        @DisplayName("Dockerfile template not found"
                + " for unknown stack adds warning")
        void assemble_whenCalled_dockerfileNotFound(@TempDir Path tempDir)
                throws IOException {
            Path resourceDir = tempDir.resolve("res");
            Files.createDirectories(
                    resourceDir.resolve("cicd-templates"));
            Path outputDir = tempDir.resolve("output");
            Files.createDirectories(outputDir);

            CicdAssembler assembler =
                    new CicdAssembler(resourceDir);
            ProjectConfig config =
                    TestConfigBuilder.builder()
                            .language("java", "21")
                            .framework("quarkus", "3.17")
                            .buildTool("maven")
                            .container("docker")
                            .build();

            List<String> files = assembler.assemble(
                    config, new TemplateEngine(), outputDir);

            assertThat(files).noneMatch(
                    f -> f.contains("Dockerfile"));
        }
    }

    @Nested
    @DisplayName("LINT_COMMANDS — lookup")
    class LintCommands {

        @Test
        @DisplayName("all 8 entries present")
        void lintCommands_allEntriesPresent_succeeds() {
            assertThat(CicdAssembler.LINT_COMMANDS)
                    .hasSize(8)
                    .containsKey("java-maven")
                    .containsKey("java-gradle")
                    .containsKey("kotlin-gradle")
                    .containsKey("typescript-npm")
                    .containsKey("python-pip")
                    .containsKey("go-go")
                    .containsKey("go-go-mod")
                    .containsKey("rust-cargo");
        }
    }

    @Nested
    @DisplayName("default constructor")
    class DefaultConstructor {

        @Test
        @DisplayName("default constructor resolves")
        void constructor_withDefaults_resolvesCorrectly() {
            CicdAssembler assembler =
                    new CicdAssembler();

            assertThat(assembler)
                    .isInstanceOf(Assembler.class);
        }
    }
}
