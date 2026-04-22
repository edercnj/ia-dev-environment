package dev.iadev.domain.stack;

import dev.iadev.testutil.TestConfigBuilder;

import dev.iadev.domain.model.InterfaceConfig;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.List;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("StackResolver")
class StackResolverTest {

    @Nested
    @DisplayName("resolve() for all 8 stacks")
    class ResolveAllStacks {

        @Test
        @DisplayName("java-quarkus returns all fields correctly")
        void resolve_javaQuarkus_allFieldsCorrect() {
            var config = TestConfigBuilder.builder()
                    .language("java", "21")
                    .framework("quarkus", "3.17")
                    .buildTool("maven")
                    .build();

            var result = StackResolver.resolve(config);

            assertThat(result.compileCmd())
                    .isEqualTo("./mvnw compile -q");
            assertThat(result.buildCmd())
                    .isEqualTo("./mvnw package -DskipTests");
            assertThat(result.testCmd()).isEqualTo("./mvnw verify");
            assertThat(result.coverageCmd())
                    .contains("jacoco");
            assertThat(result.fileExtension()).isEqualTo(".java");
            assertThat(result.buildFile()).isEqualTo("pom.xml");
            assertThat(result.packageManager()).isEqualTo("maven");
            assertThat(result.defaultPort()).isEqualTo(8080);
            assertThat(result.healthPath()).isEqualTo("/q/health");
            assertThat(result.dockerBaseImage())
                    .contains("eclipse-temurin");
            assertThat(result.dockerBaseImage()).contains("21");
        }

        @Test
        @DisplayName("java-spring-boot with gradle returns correct values")
        void resolve_javaSpringBootGradle_allFieldsCorrect() {
            var config = TestConfigBuilder.builder()
                    .language("java", "21")
                    .framework("spring-boot", "3.4")
                    .buildTool("gradle")
                    .build();

            var result = StackResolver.resolve(config);

            assertThat(result.compileCmd())
                    .isEqualTo("./gradlew compileJava -q");
            assertThat(result.buildCmd())
                    .isEqualTo("./gradlew build -x test");
            assertThat(result.testCmd()).isEqualTo("./gradlew test");
            assertThat(result.coverageCmd()).contains("jacocoTestReport");
            assertThat(result.fileExtension()).isEqualTo(".java");
            assertThat(result.buildFile()).isEqualTo("build.gradle");
            assertThat(result.packageManager()).isEqualTo("gradle");
            assertThat(result.defaultPort()).isEqualTo(8080);
            assertThat(result.healthPath())
                    .isEqualTo("/actuator/health");
        }

        // Non-Java resolve tests (typescript-nestjs, python-fastapi,
        // go-gin, kotlin-ktor, rust-axum, csharp-aspnet) removed in
        // EPIC-0048 full cleanup — the frameworks/languages are no
        // longer supported.
    }

    @Nested
    @DisplayName("Docker image resolution")
    class DockerImageTests {

        @Test
        @DisplayName("java language produces eclipse-temurin image")
        void resolveDockerImage_java_eclipseTemurin() {
            var config = TestConfigBuilder.builder()
                    .language("java", "21")
                    .build();

            var result = StackResolver.resolve(config);

            assertThat(result.dockerBaseImage())
                    .isEqualTo("eclipse-temurin:21-jre-alpine");
        }

        @Test
        @DisplayName("unknown language falls back to alpine:latest")
        void resolveDockerImage_unknown_alpine() {
            var config = TestConfigBuilder.builder()
                    .language("haskell", "9")
                    .framework("unknown", "1.0")
                    .buildTool("cabal")
                    .build();

            var result = StackResolver.resolve(config);

            assertThat(result.dockerBaseImage())
                    .isEqualTo("alpine:latest");
        }
    }

    @Nested
    @DisplayName("Health path resolution")
    class HealthPathTests {

        @Test
        @DisplayName("unknown framework defaults to /health")
        void resolveHealthPath_unknown_defaultHealth() {
            var config = TestConfigBuilder.builder()
                    .framework("unknown", "1.0")
                    .build();

            var result = StackResolver.resolve(config);

            assertThat(result.healthPath()).isEqualTo("/health");
        }
    }

    @Nested
    @DisplayName("Port resolution")
    class PortTests {

        @Test
        @DisplayName("unknown framework defaults to 8080")
        void resolvePort_unknown_default8080() {
            var config = TestConfigBuilder.builder()
                    .framework("unknown", "1.0")
                    .build();

            var result = StackResolver.resolve(config);

            assertThat(result.defaultPort()).isEqualTo(8080);
        }
    }

    @Nested
    @DisplayName("Native build inference")
    class NativeBuildTests {

        @Test
        @DisplayName("quarkus with nativeBuild=true returns true")
        void nativeBuild_quarkusEnabled_true() {
            var config = TestConfigBuilder.builder()
                    .framework("quarkus", "3.17")
                    .nativeBuild(true)
                    .build();

            var result = StackResolver.resolve(config);

            assertThat(result.nativeSupported()).isTrue();
        }

        @Test
        @DisplayName("quarkus with nativeBuild=false returns false")
        void nativeBuild_quarkusDisabled_false() {
            var config = TestConfigBuilder.builder()
                    .framework("quarkus", "3.17")
                    .nativeBuild(false)
                    .build();

            var result = StackResolver.resolve(config);

            assertThat(result.nativeSupported()).isFalse();
        }

        @Test
        @DisplayName("nestjs with nativeBuild=true returns false")
        void nativeBuild_nestjsEnabled_false() {
            var config = TestConfigBuilder.builder()
                    .language("typescript", "5")
                    .framework("nestjs", "10")
                    .buildTool("npm")
                    .nativeBuild(true)
                    .build();

            var result = StackResolver.resolve(config);

            assertThat(result.nativeSupported()).isFalse();
        }
    }

    @Nested
    @DisplayName("Project type derivation")
    class ProjectTypeTests {

        @Test
        @DisplayName("microservice with rest is api")
        void projectType_microserviceRest_api() {
            var config = TestConfigBuilder.builder()
                    .architectureStyle("microservice")
                    .clearInterfaces()
                    .addInterface("rest", "", "")
                    .build();

            var result = StackResolver.resolve(config);

            assertThat(result.projectType()).isEqualTo("api");
        }

        @Test
        @DisplayName("microservice with only event-consumer is worker")
        void projectType_microserviceEventOnly_worker() {
            var config = TestConfigBuilder.builder()
                    .architectureStyle("microservice")
                    .clearInterfaces()
                    .addInterface("event-consumer", "", "kafka")
                    .build();

            var result = StackResolver.resolve(config);

            assertThat(result.projectType()).isEqualTo("worker");
        }

        @Test
        @DisplayName("microservice with both rest and event is api")
        void projectType_microserviceBoth_api() {
            var config = TestConfigBuilder.builder()
                    .architectureStyle("microservice")
                    .clearInterfaces()
                    .addInterface("rest", "", "")
                    .addInterface("event-consumer", "", "kafka")
                    .build();

            var result = StackResolver.resolve(config);

            assertThat(result.projectType()).isEqualTo("api");
        }

        @Test
        @DisplayName("library with cli is cli")
        void projectType_libraryCli_cli() {
            var config = TestConfigBuilder.builder()
                    .architectureStyle("library")
                    .clearInterfaces()
                    .addInterface("cli", "", "")
                    .build();

            var result = StackResolver.resolve(config);

            assertThat(result.projectType()).isEqualTo("cli");
        }

        @Test
        @DisplayName("library without cli is library")
        void projectType_libraryNoCli_library() {
            var config = TestConfigBuilder.builder()
                    .architectureStyle("library")
                    .clearInterfaces()
                    .addInterface("rest", "", "")
                    .build();

            var result = StackResolver.resolve(config);

            assertThat(result.projectType()).isEqualTo("library");
        }

        @ParameterizedTest
        @MethodSource(
                "dev.iadev.domain.stack.StackResolverTest"
                        + "#apiArchitectureStyles")
        @DisplayName("architecture style {0} derives to api")
        void projectType_variousStyles_api(String style) {
            var config = TestConfigBuilder.builder()
                    .architectureStyle(style)
                    .build();

            var result = StackResolver.resolve(config);

            assertThat(result.projectType()).isEqualTo("api");
        }
    }

    static Stream<Arguments> apiArchitectureStyles() {
        return Stream.of(
                Arguments.of("monolith"),
                Arguments.of("modular-monolith"),
                Arguments.of("serverless")
        );
    }

    @Nested
    @DisplayName("Project type - unknown style")
    class UnknownStyleTests {

        @Test
        @DisplayName("unknown architecture style defaults to api")
        void projectType_unknownStyle_api() {
            var config = TestConfigBuilder.builder()
                    .architectureStyle("unknown-style")
                    .build();

            var result = StackResolver.resolve(config);

            assertThat(result.projectType()).isEqualTo("api");
        }

        @Test
        @DisplayName("event-consumer without rest in microservice is worker")
        void projectType_eventOnlyMicroservice_worker() {
            var config = TestConfigBuilder.builder()
                    .architectureStyle("microservice")
                    .clearInterfaces()
                    .addInterface("event-consumer", "", "kafka")
                    .addInterface("grpc", "", "")
                    .build();

            var result = StackResolver.resolve(config);

            assertThat(result.projectType()).isEqualTo("worker");
        }
    }

    @Nested
    @DisplayName("Protocol derivation")
    class ProtocolTests {

        @Test
        @DisplayName("rest interface produces openapi protocol")
        void protocols_rest_openapi() {
            var config = TestConfigBuilder.builder()
                    .clearInterfaces()
                    .addInterface("rest", "", "")
                    .build();

            var result = StackResolver.resolve(config);

            assertThat(result.protocols()).containsExactly("openapi");
        }

        @Test
        @DisplayName("grpc interface produces proto3 protocol")
        void protocols_grpc_proto3() {
            var config = TestConfigBuilder.builder()
                    .clearInterfaces()
                    .addInterface("grpc", "", "")
                    .build();

            var result = StackResolver.resolve(config);

            assertThat(result.protocols()).containsExactly("proto3");
        }

        @Test
        @DisplayName("multiple interfaces produce multiple protocols")
        void protocols_multiple_correctList() {
            var config = TestConfigBuilder.builder()
                    .clearInterfaces()
                    .addInterface("rest", "", "")
                    .addInterface("grpc", "", "")
                    .build();

            var result = StackResolver.resolve(config);

            assertThat(result.protocols())
                    .containsExactly("openapi", "proto3");
        }

        @Test
        @DisplayName("unknown interface type produces no protocol")
        void protocols_unknownInterface_empty() {
            var config = TestConfigBuilder.builder()
                    .clearInterfaces()
                    .addInterface("scheduled", "", "")
                    .build();

            var result = StackResolver.resolve(config);

            assertThat(result.protocols()).isEmpty();
        }
    }

    @Nested
    @DisplayName("Unknown stack key")
    class UnknownStackTests {

        @Test
        @DisplayName("unknown language/buildTool returns empty commands")
        void resolve_unknownStack_emptyCommands() {
            var config = TestConfigBuilder.builder()
                    .language("haskell", "9")
                    .framework("unknown", "1.0")
                    .buildTool("cabal")
                    .build();

            var result = StackResolver.resolve(config);

            assertThat(result.compileCmd()).isEmpty();
            assertThat(result.buildCmd()).isEmpty();
            assertThat(result.testCmd()).isEmpty();
            assertThat(result.coverageCmd()).isEmpty();
            assertThat(result.fileExtension()).isEmpty();
            assertThat(result.buildFile()).isEmpty();
            assertThat(result.packageManager()).isEmpty();
        }
    }
}
