package dev.iadev.assembler;

import dev.iadev.model.ProjectConfig;
import dev.iadev.template.TemplateEngine;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for CicdAssembler — generates CI/CD pipeline
 * artifacts conditionally based on project configuration.
 */
@DisplayName("CicdAssembler")
class CicdAssemblerTest {

    @Nested
    @DisplayName("implements Assembler interface")
    class ImplementsAssembler {

        @Test
        @DisplayName("is instance of Assembler")
        void instanceOf_whenCreated_implementsAssemblerInterface() {
            CicdAssembler assembler =
                    new CicdAssembler();

            assertThat(assembler)
                    .isInstanceOf(Assembler.class);
        }
    }

    @Nested
    @DisplayName("buildStackContext — java-maven")
    class BuildStackContextJavaMaven {

        @Test
        @DisplayName("resolves compile_cmd for"
                + " java-maven")
        void buildStackContext_whenCalled_resolvesCompileCmd() {
            ProjectConfig config = TestConfigBuilder
                    .builder()
                    .language("java", "21")
                    .buildTool("maven")
                    .build();

            Map<String, Object> ctx =
                    CicdAssembler
                            .buildStackContext(config);

            assertThat(ctx.get("compile_cmd"))
                    .isEqualTo("./mvnw compile -q");
        }

        @Test
        @DisplayName("resolves test_cmd for java-maven")
        void buildStackContext_whenCalled_resolvesTestCmd() {
            ProjectConfig config = TestConfigBuilder
                    .builder()
                    .language("java", "21")
                    .buildTool("maven")
                    .build();

            Map<String, Object> ctx =
                    CicdAssembler
                            .buildStackContext(config);

            assertThat(ctx.get("test_cmd"))
                    .isEqualTo("./mvnw verify");
        }

        @Test
        @DisplayName("resolves lint_cmd for java-maven")
        void buildStackContext_whenCalled_resolvesLintCmd() {
            ProjectConfig config = TestConfigBuilder
                    .builder()
                    .language("java", "21")
                    .buildTool("maven")
                    .build();

            Map<String, Object> ctx =
                    CicdAssembler
                            .buildStackContext(config);

            assertThat(ctx.get("lint_cmd"))
                    .isEqualTo("./mvnw checkstyle:check");
        }

        @Test
        @DisplayName("resolves framework_port for"
                + " spring-boot")
        void buildStackContext_whenCalled_resolvesFrameworkPort() {
            ProjectConfig config = TestConfigBuilder
                    .builder()
                    .framework("spring-boot", "3.2")
                    .language("java", "21")
                    .buildTool("maven")
                    .build();

            Map<String, Object> ctx =
                    CicdAssembler
                            .buildStackContext(config);

            assertThat(ctx.get("framework_port"))
                    .isEqualTo(8080);
        }

        @Test
        @DisplayName("resolves health_path for"
                + " spring-boot")
        void buildStackContext_whenCalled_resolvesHealthPath() {
            ProjectConfig config = TestConfigBuilder
                    .builder()
                    .framework("spring-boot", "3.2")
                    .language("java", "21")
                    .buildTool("maven")
                    .build();

            Map<String, Object> ctx =
                    CicdAssembler
                            .buildStackContext(config);

            assertThat(ctx.get("health_path"))
                    .isEqualTo("/actuator/health");
        }

        @Test
        @DisplayName("resolves docker_base_image with"
                + " version")
        void buildStackContext_whenCalled_resolvesDockerBaseImage() {
            ProjectConfig config = TestConfigBuilder
                    .builder()
                    .language("java", "21")
                    .buildTool("maven")
                    .build();

            Map<String, Object> ctx =
                    CicdAssembler
                            .buildStackContext(config);

            assertThat(ctx.get("docker_base_image"))
                    .isEqualTo(
                            "eclipse-temurin:"
                                    + "21-jre-alpine");
        }
    }

    @Nested
    @DisplayName("buildStackContext — typescript-npm")
    class BuildStackContextTypescriptNpm {

        @Test
        @DisplayName("resolves lint_cmd for"
                + " typescript-npm")
        void buildStackContext_whenCalled_resolvesLintCmd() {
            ProjectConfig config = TestConfigBuilder
                    .builder()
                    .language("typescript", "5.0")
                    .buildTool("npm")
                    .framework("nestjs", "10.0")
                    .build();

            Map<String, Object> ctx =
                    CicdAssembler
                            .buildStackContext(config);

            assertThat(ctx.get("lint_cmd"))
                    .isEqualTo("npm run lint");
        }

        @Test
        @DisplayName("resolves test_cmd for"
                + " typescript-npm")
        void buildStackContext_whenCalled_resolvesTestCmd() {
            ProjectConfig config = TestConfigBuilder
                    .builder()
                    .language("typescript", "5.0")
                    .buildTool("npm")
                    .framework("nestjs", "10.0")
                    .build();

            Map<String, Object> ctx =
                    CicdAssembler
                            .buildStackContext(config);

            assertThat(ctx.get("test_cmd"))
                    .isEqualTo("npm test");
        }
    }

    @Nested
    @DisplayName("buildStackContext — unknown stack")
    class BuildStackContextUnknown {

        @Test
        @DisplayName("uses default lint cmd for"
                + " unknown stack")
        void buildStackContext_defaultLintCmd_succeeds() {
            ProjectConfig config = TestConfigBuilder
                    .builder()
                    .language("cobol", "85")
                    .buildTool("none")
                    .build();

            Map<String, Object> ctx =
                    CicdAssembler
                            .buildStackContext(config);

            assertThat(ctx.get("lint_cmd"))
                    .isEqualTo(
                            "echo 'No linter"
                                    + " configured'");
        }

        @Test
        @DisplayName("uses empty strings for unknown"
                + " commands")
        void buildStackContext_emptyCommandsForUnknown_succeeds() {
            ProjectConfig config = TestConfigBuilder
                    .builder()
                    .language("cobol", "85")
                    .buildTool("none")
                    .build();

            Map<String, Object> ctx =
                    CicdAssembler
                            .buildStackContext(config);

            assertThat(ctx.get("compile_cmd"))
                    .isEqualTo("");
            assertThat(ctx.get("build_cmd"))
                    .isEqualTo("");
        }
    }

    @Nested
    @DisplayName("LINT_COMMANDS map")
    class LintCommandsMap {

        @Test
        @DisplayName("contains java-maven entry")
        void assemble_whenCalled_containsJavaMaven() {
            assertThat(CicdAssembler.LINT_COMMANDS)
                    .containsEntry("java-maven",
                            "./mvnw checkstyle:check");
        }

        @Test
        @DisplayName("contains typescript-npm entry")
        void assemble_whenCalled_containsTypescriptNpm() {
            assertThat(CicdAssembler.LINT_COMMANDS)
                    .containsEntry("typescript-npm",
                            "npm run lint");
        }

        @Test
        @DisplayName("contains python-pip entry")
        void assemble_whenCalled_containsPythonPip() {
            assertThat(CicdAssembler.LINT_COMMANDS)
                    .containsEntry("python-pip",
                            "ruff check .");
        }

        @Test
        @DisplayName("contains go-go entry")
        void assemble_whenCalled_containsGoGo() {
            assertThat(CicdAssembler.LINT_COMMANDS)
                    .containsEntry("go-go",
                            "golangci-lint run");
        }

        @Test
        @DisplayName("contains rust-cargo entry")
        void assemble_whenCalled_containsRustCargo() {
            assertThat(CicdAssembler.LINT_COMMANDS)
                    .containsEntry("rust-cargo",
                            "cargo clippy -- -D warnings");
        }

        @Test
        @DisplayName("contains kotlin-gradle entry")
        void assemble_whenCalled_containsKotlinGradle() {
            assertThat(CicdAssembler.LINT_COMMANDS)
                    .containsEntry("kotlin-gradle",
                            "./gradlew ktlintCheck");
        }

        @Test
        @DisplayName("contains java-gradle entry")
        void assemble_whenCalled_containsJavaGradle() {
            assertThat(CicdAssembler.LINT_COMMANDS)
                    .containsEntry("java-gradle",
                            "./gradlew spotlessCheck");
        }

        @Test
        @DisplayName("has exactly 8 entries")
        void assemble_whenCalled_hasEightEntries() {
            assertThat(CicdAssembler.LINT_COMMANDS)
                    .hasSize(8);
        }
    }

    @Nested
    @DisplayName("assemble — CI workflow always generated")
    class CiWorkflowAlwaysGenerated {

        @Test
        @DisplayName("generates ci.yml in .github/workflows")
        void assemble_whenCalled_generatesCiWorkflow(
                @TempDir Path tempDir) {
            Path outputDir = tempDir.resolve("output");

            CicdAssembler assembler =
                    new CicdAssembler();
            ProjectConfig config = TestConfigBuilder
                    .builder()
                    .language("java", "21")
                    .buildTool("maven")
                    .container("none")
                    .orchestrator("none")
                    .smokeTests(false)
                    .build();
            TemplateEngine engine = new TemplateEngine();

            List<String> files = assembler.assemble(
                    config, engine, outputDir);

            assertThat(files).isNotEmpty();
            assertThat(files.stream()
                    .anyMatch(f -> f.contains("ci.yml")))
                    .isTrue();
            assertThat(
                    outputDir.resolve(
                            ".github/workflows/ci.yml"))
                    .exists();
        }
    }

    @Nested
    @DisplayName("assemble — Dockerfile conditional")
    class DockerfileConditional {

        @Test
        @DisplayName("generates Dockerfile when"
                + " container=docker")
        void assemble_whenCalled_generatesDockerfile(
                @TempDir Path tempDir) {
            Path outputDir = tempDir.resolve("output");

            CicdAssembler assembler =
                    new CicdAssembler();
            ProjectConfig config = TestConfigBuilder
                    .builder()
                    .container("docker")
                    .language("java", "21")
                    .buildTool("maven")
                    .build();
            TemplateEngine engine = new TemplateEngine();

            List<String> files = assembler.assemble(
                    config, engine, outputDir);

            assertThat(files.stream()
                    .anyMatch(f ->
                            f.endsWith("Dockerfile")))
                    .isTrue();
        }

        @Test
        @DisplayName("skips Dockerfile when"
                + " container=none")
        void assemble_whenCalled_skipsDockerfileWhenNone(
                @TempDir Path tempDir) {
            Path outputDir = tempDir.resolve("output");

            CicdAssembler assembler =
                    new CicdAssembler();
            ProjectConfig config = TestConfigBuilder
                    .builder()
                    .container("none")
                    .orchestrator("none")
                    .smokeTests(false)
                    .build();
            TemplateEngine engine = new TemplateEngine();

            List<String> files = assembler.assemble(
                    config, engine, outputDir);

            assertThat(files.stream()
                    .noneMatch(f ->
                            f.endsWith("Dockerfile")))
                    .isTrue();
        }
    }

    @Nested
    @DisplayName("assemble — Docker Compose conditional")
    class DockerComposeConditional {

        @Test
        @DisplayName("generates docker-compose.yml when"
                + " container=docker")
        void assemble_whenCalled_generatesDockerCompose(
                @TempDir Path tempDir) {
            Path outputDir = tempDir.resolve("output");

            CicdAssembler assembler =
                    new CicdAssembler();
            ProjectConfig config = TestConfigBuilder
                    .builder()
                    .container("docker")
                    .language("java", "21")
                    .buildTool("maven")
                    .build();
            TemplateEngine engine = new TemplateEngine();

            List<String> files = assembler.assemble(
                    config, engine, outputDir);

            assertThat(files.stream()
                    .anyMatch(f ->
                            f.contains(
                                    "docker-compose.yml")))
                    .isTrue();
        }

        @Test
        @DisplayName("skips docker-compose.yml when"
                + " container=none")
        void assemble_whenCalled_skipsDockerComposeWhenNone(
                @TempDir Path tempDir) {
            Path outputDir = tempDir.resolve("output");

            CicdAssembler assembler =
                    new CicdAssembler();
            ProjectConfig config = TestConfigBuilder
                    .builder()
                    .container("none")
                    .orchestrator("none")
                    .smokeTests(false)
                    .build();
            TemplateEngine engine = new TemplateEngine();

            List<String> files = assembler.assemble(
                    config, engine, outputDir);

            assertThat(files.stream()
                    .noneMatch(f ->
                            f.contains(
                                    "docker-compose.yml")))
                    .isTrue();
        }
    }

    @Nested
    @DisplayName("assemble — K8s manifests conditional")
    class K8sManifestsConditional {

        @Test
        @DisplayName("generates K8s manifests when"
                + " orchestrator=kubernetes")
        void assemble_whenCalled_generatesK8sManifests(
                @TempDir Path tempDir) {
            Path outputDir = tempDir.resolve("output");

            CicdAssembler assembler =
                    new CicdAssembler();
            ProjectConfig config = TestConfigBuilder
                    .builder()
                    .container("docker")
                    .orchestrator("kubernetes")
                    .language("java", "21")
                    .buildTool("maven")
                    .build();
            TemplateEngine engine = new TemplateEngine();

            List<String> files = assembler.assemble(
                    config, engine, outputDir);

            assertThat(files.stream()
                    .anyMatch(f ->
                            f.contains(
                                    "deployment.yaml")))
                    .isTrue();
            assertThat(files.stream()
                    .anyMatch(f ->
                            f.contains(
                                    "service.yaml")))
                    .isTrue();
            assertThat(files.stream()
                    .anyMatch(f ->
                            f.contains(
                                    "configmap.yaml")))
                    .isTrue();
        }

        @Test
        @DisplayName("skips K8s manifests when"
                + " orchestrator=none")
        void assemble_whenCalled_skipsK8sWhenNone(
                @TempDir Path tempDir) {
            Path outputDir = tempDir.resolve("output");

            CicdAssembler assembler =
                    new CicdAssembler();
            ProjectConfig config = TestConfigBuilder
                    .builder()
                    .container("docker")
                    .orchestrator("none")
                    .language("java", "21")
                    .buildTool("maven")
                    .build();
            TemplateEngine engine = new TemplateEngine();

            List<String> files = assembler.assemble(
                    config, engine, outputDir);

            assertThat(files.stream()
                    .noneMatch(f ->
                            f.contains(
                                    "deployment.yaml")))
                    .isTrue();
        }
    }

    @Nested
    @DisplayName("assemble — smoke test conditional")
    class SmokeTestConditional {

        @Test
        @DisplayName("generates smoke-config.md when"
                + " smokeTests=true")
        void assemble_whenCalled_generatesSmokeConfig(
                @TempDir Path tempDir) {
            Path outputDir = tempDir.resolve("output");

            CicdAssembler assembler =
                    new CicdAssembler();
            ProjectConfig config = TestConfigBuilder
                    .builder()
                    .smokeTests(true)
                    .container("none")
                    .orchestrator("none")
                    .build();
            TemplateEngine engine = new TemplateEngine();

            List<String> files = assembler.assemble(
                    config, engine, outputDir);

            assertThat(files.stream()
                    .anyMatch(f ->
                            f.contains(
                                    "smoke-config.md")))
                    .isTrue();
        }

        @Test
        @DisplayName("skips smoke-config.md when"
                + " smokeTests=false")
        void assemble_whenCalled_skipsSmokeWhenFalse(
                @TempDir Path tempDir) {
            Path outputDir = tempDir.resolve("output");

            CicdAssembler assembler =
                    new CicdAssembler();
            ProjectConfig config = TestConfigBuilder
                    .builder()
                    .smokeTests(false)
                    .container("none")
                    .orchestrator("none")
                    .build();
            TemplateEngine engine = new TemplateEngine();

            List<String> files = assembler.assemble(
                    config, engine, outputDir);

            assertThat(files.stream()
                    .noneMatch(f ->
                            f.contains(
                                    "smoke-config.md")))
                    .isTrue();
        }
    }

    @Nested
    @DisplayName("assemble — full docker+k8s+smoke")
    class FullGeneration {

        @Test
        @DisplayName("generates all artifacts for"
                + " java-maven with docker+kubernetes"
                + "+smoke")
        void assemble_whenCalled_generatesAllArtifacts(
                @TempDir Path tempDir) {
            Path outputDir = tempDir.resolve("output");

            CicdAssembler assembler =
                    new CicdAssembler();
            ProjectConfig config = TestConfigBuilder
                    .builder()
                    .language("java", "21")
                    .buildTool("maven")
                    .framework("spring-boot", "3.2")
                    .container("docker")
                    .orchestrator("kubernetes")
                    .smokeTests(true)
                    .build();
            TemplateEngine engine = new TemplateEngine();

            List<String> files = assembler.assemble(
                    config, engine, outputDir);

            // ci.yml + Dockerfile + docker-compose.yml
            // + 3 k8s manifests + smoke-config.md = 7
            assertThat(files).hasSize(7);
        }

        @Test
        @DisplayName("generates minimum artifacts when"
                + " no docker, no k8s, no smoke")
        void assemble_whenCalled_generatesMinimumArtifacts(
                @TempDir Path tempDir) {
            Path outputDir = tempDir.resolve("output");

            CicdAssembler assembler =
                    new CicdAssembler();
            ProjectConfig config = TestConfigBuilder
                    .builder()
                    .container("none")
                    .orchestrator("none")
                    .smokeTests(false)
                    .build();
            TemplateEngine engine = new TemplateEngine();

            List<String> files = assembler.assemble(
                    config, engine, outputDir);

            // Only ci.yml
            assertThat(files).hasSize(1);
            assertThat(files.get(0))
                    .contains("ci.yml");
        }
    }
}
