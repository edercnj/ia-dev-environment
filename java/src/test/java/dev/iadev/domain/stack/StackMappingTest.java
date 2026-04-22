package dev.iadev.domain.stack;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("StackMapping (Java-only since EPIC-0048 / v4.0.0)")
class StackMappingTest {

    @Nested
    @DisplayName("LANGUAGE_COMMANDS")
    class LanguageCommandsTests {

        @Test
        @DisplayName("contains exactly 2 Java entries")
        void languageCommands_size_two() {
            assertThat(StackMapping.LANGUAGE_COMMANDS).hasSize(2);
        }

        @ParameterizedTest
        @ValueSource(strings = {"java-maven", "java-gradle"})
        @DisplayName("contains key: {0}")
        void languageCommands_containsKey(String key) {
            assertThat(StackMapping.LANGUAGE_COMMANDS).containsKey(key);
        }

        @Test
        @DisplayName("java-maven has correct commands")
        void languageCommands_javaMaven_correctCommands() {
            var cmds = StackMapping.LANGUAGE_COMMANDS.get("java-maven");
            assertThat(cmds.compileCmd()).isEqualTo("./mvnw compile -q");
            assertThat(cmds.buildCmd())
                    .isEqualTo("./mvnw package -DskipTests");
            assertThat(cmds.testCmd()).isEqualTo("./mvnw verify");
            assertThat(cmds.coverageCmd())
                    .isEqualTo("./mvnw verify jacoco:report");
            assertThat(cmds.fileExtension()).isEqualTo(".java");
            assertThat(cmds.buildFile()).isEqualTo("pom.xml");
            assertThat(cmds.packageManager()).isEqualTo("maven");
        }

        @Test
        @DisplayName("java-gradle has correct commands")
        void languageCommands_javaGradle_correctCommands() {
            var cmds = StackMapping.LANGUAGE_COMMANDS.get("java-gradle");
            assertThat(cmds.compileCmd())
                    .isEqualTo("./gradlew compileJava -q");
            assertThat(cmds.fileExtension()).isEqualTo(".java");
            assertThat(cmds.buildFile()).isEqualTo("build.gradle");
            assertThat(cmds.packageManager()).isEqualTo("gradle");
        }

        @ParameterizedTest
        @ValueSource(strings = {
                "kotlin-gradle", "typescript-npm", "python-pip",
                "go-go", "rust-cargo", "csharp-dotnet"})
        @DisplayName("non-Java key {0} is absent")
        void languageCommands_nonJavaKey_absent(String key) {
            assertThat(StackMapping.LANGUAGE_COMMANDS)
                    .doesNotContainKey(key);
        }
    }

    @Nested
    @DisplayName("FRAMEWORK_PORTS")
    class FrameworkPortsTests {

        @Test
        @DisplayName("contains exactly 2 Java entries")
        void frameworkPorts_size_two() {
            assertThat(StackMapping.FRAMEWORK_PORTS).hasSize(2);
        }

        @Test
        @DisplayName("quarkus port is 8080")
        void frameworkPorts_quarkus() {
            assertThat(StackMapping.FRAMEWORK_PORTS.get("quarkus"))
                    .isEqualTo(8080);
        }

        @Test
        @DisplayName("spring-boot port is 8080")
        void frameworkPorts_springBoot() {
            assertThat(StackMapping.FRAMEWORK_PORTS.get("spring-boot"))
                    .isEqualTo(8080);
        }

        @ParameterizedTest
        @ValueSource(strings = {
                "nestjs", "express", "fastapi", "django",
                "gin", "ktor", "axum", "actix-web", "aspnet"})
        @DisplayName("non-Java framework {0} is absent")
        void frameworkPorts_nonJava_absent(String fw) {
            assertThat(StackMapping.FRAMEWORK_PORTS)
                    .doesNotContainKey(fw);
        }
    }

    @Nested
    @DisplayName("FRAMEWORK_HEALTH_PATHS")
    class FrameworkHealthPathsTests {

        @Test
        @DisplayName("contains exactly 2 Java entries")
        void frameworkHealthPaths_size_two() {
            assertThat(StackMapping.FRAMEWORK_HEALTH_PATHS).hasSize(2);
        }

        @Test
        @DisplayName("quarkus has /q/health")
        void frameworkHealthPaths_whenCalled_quarkus() {
            assertThat(StackMapping.FRAMEWORK_HEALTH_PATHS.get("quarkus"))
                    .isEqualTo("/q/health");
        }

        @Test
        @DisplayName("spring-boot has /actuator/health")
        void frameworkHealthPaths_whenCalled_springBoot() {
            assertThat(StackMapping.FRAMEWORK_HEALTH_PATHS.get("spring-boot"))
                    .isEqualTo("/actuator/health");
        }
    }

    @Nested
    @DisplayName("FRAMEWORK_LANGUAGE_RULES")
    class FrameworkLanguageRulesTests {

        @Test
        @DisplayName("contains 2 Java entries")
        void frameworkLanguageRules_size_two() {
            assertThat(StackMapping.FRAMEWORK_LANGUAGE_RULES).hasSize(2);
        }

        @Test
        @DisplayName("quarkus accepts java")
        void frameworkLanguageRules_whenCalled_quarkus() {
            assertThat(StackMapping.FRAMEWORK_LANGUAGE_RULES.get("quarkus"))
                    .containsExactly("java");
        }

        @Test
        @DisplayName("spring-boot accepts java")
        void frameworkLanguageRules_whenCalled_springBoot() {
            assertThat(
                    StackMapping.FRAMEWORK_LANGUAGE_RULES.get("spring-boot"))
                    .containsExactly("java");
        }

        @ParameterizedTest
        @ValueSource(strings = {
                "nestjs", "fastapi", "gin", "ktor", "axum",
                "aspnet", "django", "express", "fastify",
                "commander", "flask", "stdlib", "fiber", "actix-web"})
        @DisplayName("non-Java framework {0} is absent")
        void frameworkLanguageRules_nonJava_absent(String fw) {
            assertThat(StackMapping.FRAMEWORK_LANGUAGE_RULES)
                    .doesNotContainKey(fw);
        }
    }

    @Nested
    @DisplayName("Constants")
    class ConstantsTests {

        @Test
        @DisplayName("NATIVE_SUPPORTED_FRAMEWORKS has quarkus and spring-boot")
        void nativeSupportedFrameworks_whenCalled_twoEntries() {
            assertThat(StackMapping.NATIVE_SUPPORTED_FRAMEWORKS)
                    .containsExactly("quarkus", "spring-boot");
        }

        @Test
        @DisplayName("VALID_INTERFACE_TYPES has 9 entries")
        void validInterfaceTypes_whenCalled_nine() {
            assertThat(StackMapping.VALID_INTERFACE_TYPES).hasSize(9);
            assertThat(StackMapping.VALID_INTERFACE_TYPES)
                    .contains("rest", "grpc", "graphql", "websocket",
                            "tcp-custom", "cli", "event-consumer",
                            "event-producer", "scheduled");
        }

        @Test
        @DisplayName("VALID_ARCHITECTURE_STYLES has 10 entries")
        void validArchitectureStyles_whenCalled_ten() {
            assertThat(StackMapping.VALID_ARCHITECTURE_STYLES)
                    .hasSize(10);
        }

        @Test
        @DisplayName("DEFAULT_PORT_FALLBACK is 8080")
        void map_whenCalled_defaultPortFallback() {
            assertThat(StackMapping.DEFAULT_PORT_FALLBACK).isEqualTo(8080);
        }

        @Test
        @DisplayName("DEFAULT_HEALTH_PATH is /health")
        void map_whenCalled_defaultHealthPath() {
            assertThat(StackMapping.DEFAULT_HEALTH_PATH)
                    .isEqualTo("/health");
        }

        @Test
        @DisplayName("DEFAULT_DOCKER_IMAGE is alpine:latest")
        void map_whenCalled_defaultDockerImage() {
            assertThat(StackMapping.DEFAULT_DOCKER_IMAGE)
                    .isEqualTo("alpine:latest");
        }

        @Test
        @DisplayName("DOCKER_BASE_IMAGES has 1 Java entry")
        void dockerBaseImages_whenCalled_one() {
            assertThat(StackMapping.DOCKER_BASE_IMAGES).hasSize(1);
            assertThat(StackMapping.DOCKER_BASE_IMAGES)
                    .containsKey("java");
        }

        @Test
        @DisplayName("INTERFACE_SPEC_PROTOCOL_MAP has 7 entries")
        void interfaceSpecProtocolMap_whenCalled_seven() {
            assertThat(StackMapping.INTERFACE_SPEC_PROTOCOL_MAP).hasSize(7);
        }

        @Test
        @DisplayName("HOOK_TEMPLATE_MAP has 2 Java entries")
        void hookTemplateMap_whenCalled_two() {
            assertThat(StackMapping.HOOK_TEMPLATE_MAP).hasSize(2);
        }

        @Test
        @DisplayName("SETTINGS_LANG_MAP has 2 Java entries")
        void settingsLangMap_whenCalled_two() {
            assertThat(StackMapping.SETTINGS_LANG_MAP).hasSize(2);
        }
    }

    @Nested
    @DisplayName("Utility Methods")
    class UtilityMethodsTests {

        @Test
        @DisplayName("getHookTemplateKey returns correct key")
        void getHookTemplateKey_javaGradle_correct() {
            assertThat(StackMapping.getHookTemplateKey("java", "gradle"))
                    .isEqualTo("java-gradle");
        }

        @Test
        @DisplayName("getHookTemplateKey returns empty for python")
        void getHookTemplateKey_pythonPip_empty() {
            assertThat(StackMapping.getHookTemplateKey("python", "pip"))
                    .isEmpty();
        }

        @Test
        @DisplayName("getHookTemplateKey returns empty for unknown")
        void getHookTemplateKey_unknown_empty() {
            assertThat(StackMapping.getHookTemplateKey("unknown", "x"))
                    .isEmpty();
        }

        @Test
        @DisplayName("getSettingsLangKey returns correct key")
        void getSettingsLangKey_javaMaven_correct() {
            assertThat(StackMapping.getSettingsLangKey("java", "maven"))
                    .isEqualTo("java-maven");
        }

        @Test
        @DisplayName("getSettingsLangKey returns empty for unknown")
        void getSettingsLangKey_unknown_empty() {
            assertThat(StackMapping.getSettingsLangKey("unknown", "x"))
                    .isEmpty();
        }
    }
}
