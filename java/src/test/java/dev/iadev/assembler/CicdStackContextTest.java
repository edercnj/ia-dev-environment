package dev.iadev.assembler;
import dev.iadev.model.ProjectConfig;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import java.util.Map;
import static org.assertj.core.api.Assertions.assertThat;
/**
 * Tests for CicdAssembler — interface contract,
 * buildStackContext, and LINT_COMMANDS map.
 */
@DisplayName("CicdAssembler — stack context")
class CicdStackContextTest {

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
        @DisplayName("resolves compile_cmd")
        void buildStackContext_resolvesCompileCmd() {
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
        @DisplayName("resolves test_cmd")
        void buildStackContext_resolvesTestCmd() {
            ProjectConfig config = TestConfigBuilder
                    .builder()
                    .language("java", "21")
                    .buildTool("maven")
                    .build();
            assertThat(CicdAssembler
                    .buildStackContext(config)
                    .get("test_cmd"))
                    .isEqualTo("./mvnw verify");
        }

        @Test
        @DisplayName("resolves lint_cmd")
        void buildStackContext_resolvesLintCmd() {
            ProjectConfig config = TestConfigBuilder
                    .builder()
                    .language("java", "21")
                    .buildTool("maven")
                    .build();
            assertThat(CicdAssembler
                    .buildStackContext(config)
                    .get("lint_cmd"))
                    .isEqualTo("./mvnw checkstyle:check");
        }

        @Test
        @DisplayName("resolves framework_port")
        void buildStackContext_resolvesPort() {
            ProjectConfig config = TestConfigBuilder
                    .builder()
                    .framework("spring-boot", "3.2")
                    .language("java", "21")
                    .buildTool("maven")
                    .build();
            assertThat(CicdAssembler
                    .buildStackContext(config)
                    .get("framework_port"))
                    .isEqualTo(8080);
        }

        @Test
        @DisplayName("resolves health_path")
        void buildStackContext_resolvesHealthPath() {
            ProjectConfig config = TestConfigBuilder
                    .builder()
                    .framework("spring-boot", "3.2")
                    .language("java", "21")
                    .buildTool("maven")
                    .build();
            assertThat(CicdAssembler
                    .buildStackContext(config)
                    .get("health_path"))
                    .isEqualTo("/actuator/health");
        }

        @Test
        @DisplayName("resolves docker_base_image")
        void buildStackContext_resolvesDockerImage() {
            ProjectConfig config = TestConfigBuilder
                    .builder()
                    .language("java", "21")
                    .buildTool("maven")
                    .build();
            assertThat(CicdAssembler
                    .buildStackContext(config)
                    .get("docker_base_image"))
                    .isEqualTo(
                            "eclipse-temurin:"
                                    + "21-jre-alpine");
        }
    }

    @Nested
    @DisplayName("buildStackContext — typescript-npm")
    class BuildStackContextTypescriptNpm {

        @Test
        @DisplayName("resolves lint_cmd")
        void buildStackContext_resolvesLintCmd() {
            ProjectConfig config = TestConfigBuilder
                    .builder()
                    .language("typescript", "5.0")
                    .buildTool("npm")
                    .framework("nestjs", "10.0")
                    .build();
            assertThat(CicdAssembler
                    .buildStackContext(config)
                    .get("lint_cmd"))
                    .isEqualTo("npm run lint");
        }

        @Test
        @DisplayName("resolves test_cmd")
        void buildStackContext_resolvesTestCmd() {
            ProjectConfig config = TestConfigBuilder
                    .builder()
                    .language("typescript", "5.0")
                    .buildTool("npm")
                    .framework("nestjs", "10.0")
                    .build();
            assertThat(CicdAssembler
                    .buildStackContext(config)
                    .get("test_cmd"))
                    .isEqualTo("npm test");
        }
    }

    @Nested
    @DisplayName("buildStackContext — unknown stack")
    class BuildStackContextUnknown {

        @Test
        @DisplayName("default lint cmd for unknown")
        void buildStackContext_defaultLintCmd() {
            ProjectConfig config = TestConfigBuilder
                    .builder()
                    .language("cobol", "85")
                    .buildTool("none")
                    .build();
            assertThat(CicdAssembler
                    .buildStackContext(config)
                    .get("lint_cmd"))
                    .isEqualTo(
                            "echo 'No linter configured'");
        }

        @Test
        @DisplayName("empty commands for unknown")
        void buildStackContext_emptyCommands() {
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
        void containsJavaMaven() {
            assertThat(CicdAssembler.LINT_COMMANDS)
                    .containsEntry("java-maven",
                            "./mvnw checkstyle:check");
        }

        @Test
        void containsTypescriptNpm() {
            assertThat(CicdAssembler.LINT_COMMANDS)
                    .containsEntry("typescript-npm",
                            "npm run lint");
        }

        @Test
        void containsPythonPip() {
            assertThat(CicdAssembler.LINT_COMMANDS)
                    .containsEntry("python-pip",
                            "ruff check .");
        }

        @Test
        void containsGoGo() {
            assertThat(CicdAssembler.LINT_COMMANDS)
                    .containsEntry("go-go",
                            "golangci-lint run");
        }

        @Test
        void containsRustCargo() {
            assertThat(CicdAssembler.LINT_COMMANDS)
                    .containsEntry("rust-cargo",
                            "cargo clippy -- -D warnings");
        }

        @Test
        void containsKotlinGradle() {
            assertThat(CicdAssembler.LINT_COMMANDS)
                    .containsEntry("kotlin-gradle",
                            "./gradlew ktlintCheck");
        }

        @Test
        void containsJavaGradle() {
            assertThat(CicdAssembler.LINT_COMMANDS)
                    .containsEntry("java-gradle",
                            "./gradlew spotlessCheck");
        }

        @Test
        void hasEightEntries() {
            assertThat(CicdAssembler.LINT_COMMANDS)
                    .hasSize(8);
        }
    }
}
