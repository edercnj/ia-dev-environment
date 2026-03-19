package dev.iadev.domain.stack;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("StackMapping")
class StackMappingTest {

    @Nested
    @DisplayName("LANGUAGE_COMMANDS")
    class LanguageCommandsTests {

        @Test
        @DisplayName("contains exactly 8 entries")
        void languageCommands_size_eight() {
            assertThat(StackMapping.LANGUAGE_COMMANDS).hasSize(8);
        }

        @ParameterizedTest
        @ValueSource(strings = {
                "java-maven", "java-gradle", "kotlin-gradle",
                "typescript-npm", "python-pip", "go-go",
                "rust-cargo", "csharp-dotnet"
        })
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
        @DisplayName("typescript-npm has correct commands")
        void languageCommands_typescriptNpm_correctCommands() {
            var cmds = StackMapping.LANGUAGE_COMMANDS.get("typescript-npm");
            assertThat(cmds.compileCmd())
                    .isEqualTo("npx --no-install tsc --noEmit");
            assertThat(cmds.buildCmd()).isEqualTo("npm run build");
            assertThat(cmds.testCmd()).isEqualTo("npm test");
            assertThat(cmds.coverageCmd())
                    .isEqualTo("npm test -- --coverage");
            assertThat(cmds.fileExtension()).isEqualTo(".ts");
            assertThat(cmds.buildFile()).isEqualTo("package.json");
            assertThat(cmds.packageManager()).isEqualTo("npm");
        }

        @Test
        @DisplayName("go-go has correct commands")
        void languageCommands_goGo_correctCommands() {
            var cmds = StackMapping.LANGUAGE_COMMANDS.get("go-go");
            assertThat(cmds.compileCmd()).isEqualTo("go build ./...");
            assertThat(cmds.buildCmd()).isEqualTo("go build ./...");
            assertThat(cmds.testCmd()).isEqualTo("go test ./...");
            assertThat(cmds.coverageCmd()).contains("coverprofile");
            assertThat(cmds.fileExtension()).isEqualTo(".go");
            assertThat(cmds.buildFile()).isEqualTo("go.mod");
            assertThat(cmds.packageManager()).isEqualTo("go");
        }

        @Test
        @DisplayName("rust-cargo has correct commands")
        void languageCommands_rustCargo_correctCommands() {
            var cmds = StackMapping.LANGUAGE_COMMANDS.get("rust-cargo");
            assertThat(cmds.compileCmd()).isEqualTo("cargo check");
            assertThat(cmds.buildCmd()).isEqualTo("cargo build");
            assertThat(cmds.testCmd()).isEqualTo("cargo test");
            assertThat(cmds.coverageCmd()).isEqualTo("cargo tarpaulin");
            assertThat(cmds.fileExtension()).isEqualTo(".rs");
            assertThat(cmds.buildFile()).isEqualTo("Cargo.toml");
            assertThat(cmds.packageManager()).isEqualTo("cargo");
        }

        @Test
        @DisplayName("kotlin-gradle has correct commands")
        void languageCommands_kotlinGradle_correctCommands() {
            var cmds = StackMapping.LANGUAGE_COMMANDS.get("kotlin-gradle");
            assertThat(cmds.compileCmd())
                    .isEqualTo("./gradlew compileKotlin -q");
            assertThat(cmds.buildCmd())
                    .isEqualTo("./gradlew build -x test");
            assertThat(cmds.testCmd()).isEqualTo("./gradlew test");
            assertThat(cmds.coverageCmd()).contains("jacocoTestReport");
            assertThat(cmds.fileExtension()).isEqualTo(".kt");
            assertThat(cmds.buildFile()).isEqualTo("build.gradle.kts");
            assertThat(cmds.packageManager()).isEqualTo("gradle");
        }

        @Test
        @DisplayName("python-pip has correct commands")
        void languageCommands_pythonPip_correctCommands() {
            var cmds = StackMapping.LANGUAGE_COMMANDS.get("python-pip");
            assertThat(cmds.compileCmd())
                    .isEqualTo("python3 -m py_compile");
            assertThat(cmds.buildCmd()).isEqualTo("pip install -e .");
            assertThat(cmds.testCmd()).isEqualTo("pytest");
            assertThat(cmds.coverageCmd()).isEqualTo("pytest --cov");
            assertThat(cmds.fileExtension()).isEqualTo(".py");
            assertThat(cmds.buildFile()).isEqualTo("pyproject.toml");
            assertThat(cmds.packageManager()).isEqualTo("pip");
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

        @Test
        @DisplayName("csharp-dotnet has correct commands")
        void languageCommands_csharpDotnet_correctCommands() {
            var cmds = StackMapping.LANGUAGE_COMMANDS.get("csharp-dotnet");
            assertThat(cmds.compileCmd()).contains("dotnet build");
            assertThat(cmds.buildCmd()).isEqualTo("dotnet build");
            assertThat(cmds.testCmd()).isEqualTo("dotnet test");
            assertThat(cmds.coverageCmd()).contains("XPlat Code Coverage");
            assertThat(cmds.fileExtension()).isEqualTo(".cs");
            assertThat(cmds.buildFile()).isEqualTo("*.csproj");
            assertThat(cmds.packageManager()).isEqualTo("dotnet");
        }
    }

    @Nested
    @DisplayName("FRAMEWORK_PORTS")
    class FrameworkPortsTests {

        @Test
        @DisplayName("contains exactly 11 entries")
        void frameworkPorts_size_eleven() {
            assertThat(StackMapping.FRAMEWORK_PORTS).hasSize(11);
        }

        @ParameterizedTest
        @CsvSource({
                "quarkus, 8080",
                "spring-boot, 8080",
                "nestjs, 3000",
                "express, 3000",
                "fastapi, 8000",
                "django, 8000",
                "gin, 8080",
                "ktor, 8080",
                "axum, 3000",
                "actix-web, 8080",
                "aspnet, 5000"
        })
        @DisplayName("port for {0} is {1}")
        void frameworkPorts_correctPort(String fw, int port) {
            assertThat(StackMapping.FRAMEWORK_PORTS.get(fw))
                    .isEqualTo(port);
        }
    }

    @Nested
    @DisplayName("FRAMEWORK_HEALTH_PATHS")
    class FrameworkHealthPathsTests {

        @Test
        @DisplayName("contains exactly 11 entries")
        void frameworkHealthPaths_size_eleven() {
            assertThat(StackMapping.FRAMEWORK_HEALTH_PATHS).hasSize(11);
        }

        @Test
        @DisplayName("quarkus has /q/health")
        void frameworkHealthPaths_quarkus() {
            assertThat(StackMapping.FRAMEWORK_HEALTH_PATHS.get("quarkus"))
                    .isEqualTo("/q/health");
        }

        @Test
        @DisplayName("spring-boot has /actuator/health")
        void frameworkHealthPaths_springBoot() {
            assertThat(StackMapping.FRAMEWORK_HEALTH_PATHS.get("spring-boot"))
                    .isEqualTo("/actuator/health");
        }

        @ParameterizedTest
        @ValueSource(strings = {
                "nestjs", "express", "fastapi", "django",
                "gin", "ktor", "axum", "actix-web", "aspnet"
        })
        @DisplayName("{0} has /health")
        void frameworkHealthPaths_defaultHealth(String fw) {
            assertThat(StackMapping.FRAMEWORK_HEALTH_PATHS.get(fw))
                    .isEqualTo("/health");
        }
    }

    @Nested
    @DisplayName("FRAMEWORK_LANGUAGE_RULES")
    class FrameworkLanguageRulesTests {

        @Test
        @DisplayName("contains 15 entries")
        void frameworkLanguageRules_size_fifteen() {
            assertThat(StackMapping.FRAMEWORK_LANGUAGE_RULES).hasSize(15);
        }

        @Test
        @DisplayName("quarkus accepts java and kotlin")
        void frameworkLanguageRules_quarkus() {
            assertThat(StackMapping.FRAMEWORK_LANGUAGE_RULES.get("quarkus"))
                    .containsExactly("java", "kotlin");
        }

        @Test
        @DisplayName("nestjs requires typescript")
        void frameworkLanguageRules_nestjs() {
            assertThat(StackMapping.FRAMEWORK_LANGUAGE_RULES.get("nestjs"))
                    .containsExactly("typescript");
        }

        @Test
        @DisplayName("fastapi requires python")
        void frameworkLanguageRules_fastapi() {
            assertThat(StackMapping.FRAMEWORK_LANGUAGE_RULES.get("fastapi"))
                    .containsExactly("python");
        }

        @Test
        @DisplayName("gin requires go")
        void frameworkLanguageRules_gin() {
            assertThat(StackMapping.FRAMEWORK_LANGUAGE_RULES.get("gin"))
                    .containsExactly("go");
        }

        @Test
        @DisplayName("axum requires rust")
        void frameworkLanguageRules_axum() {
            assertThat(StackMapping.FRAMEWORK_LANGUAGE_RULES.get("axum"))
                    .containsExactly("rust");
        }

        @Test
        @DisplayName("aspnet requires csharp")
        void frameworkLanguageRules_aspnet() {
            assertThat(StackMapping.FRAMEWORK_LANGUAGE_RULES.get("aspnet"))
                    .containsExactly("csharp");
        }
    }

    @Nested
    @DisplayName("Constants")
    class ConstantsTests {

        @Test
        @DisplayName("NATIVE_SUPPORTED_FRAMEWORKS has quarkus and spring-boot")
        void nativeSupportedFrameworks_twoEntries() {
            assertThat(StackMapping.NATIVE_SUPPORTED_FRAMEWORKS)
                    .containsExactly("quarkus", "spring-boot");
        }

        @Test
        @DisplayName("VALID_INTERFACE_TYPES has 9 entries")
        void validInterfaceTypes_nine() {
            assertThat(StackMapping.VALID_INTERFACE_TYPES).hasSize(9);
            assertThat(StackMapping.VALID_INTERFACE_TYPES)
                    .contains("rest", "grpc", "graphql", "websocket",
                            "tcp-custom", "cli", "event-consumer",
                            "event-producer", "scheduled");
        }

        @Test
        @DisplayName("VALID_ARCHITECTURE_STYLES has 5 entries")
        void validArchitectureStyles_five() {
            assertThat(StackMapping.VALID_ARCHITECTURE_STYLES).hasSize(5);
            assertThat(StackMapping.VALID_ARCHITECTURE_STYLES)
                    .contains("microservice", "modular-monolith",
                            "monolith", "library", "serverless");
        }

        @Test
        @DisplayName("DEFAULT_PORT_FALLBACK is 8080")
        void defaultPortFallback() {
            assertThat(StackMapping.DEFAULT_PORT_FALLBACK).isEqualTo(8080);
        }

        @Test
        @DisplayName("DEFAULT_HEALTH_PATH is /health")
        void defaultHealthPath() {
            assertThat(StackMapping.DEFAULT_HEALTH_PATH)
                    .isEqualTo("/health");
        }

        @Test
        @DisplayName("DEFAULT_DOCKER_IMAGE is alpine:latest")
        void defaultDockerImage() {
            assertThat(StackMapping.DEFAULT_DOCKER_IMAGE)
                    .isEqualTo("alpine:latest");
        }

        @Test
        @DisplayName("DOCKER_BASE_IMAGES has 7 entries")
        void dockerBaseImages_seven() {
            assertThat(StackMapping.DOCKER_BASE_IMAGES).hasSize(7);
        }

        @Test
        @DisplayName("INTERFACE_SPEC_PROTOCOL_MAP has 7 entries")
        void interfaceSpecProtocolMap_seven() {
            assertThat(StackMapping.INTERFACE_SPEC_PROTOCOL_MAP).hasSize(7);
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

        @Test
        @DisplayName("getDatabaseSettingsKey returns correct key")
        void getDatabaseSettingsKey_postgresql_correct() {
            assertThat(StackMapping.getDatabaseSettingsKey("postgresql"))
                    .isEqualTo("database-psql");
        }

        @Test
        @DisplayName("getDatabaseSettingsKey returns empty for unknown")
        void getDatabaseSettingsKey_unknown_empty() {
            assertThat(StackMapping.getDatabaseSettingsKey("unknown"))
                    .isEmpty();
        }

        @Test
        @DisplayName("getCacheSettingsKey returns correct key")
        void getCacheSettingsKey_redis_correct() {
            assertThat(StackMapping.getCacheSettingsKey("redis"))
                    .isEqualTo("cache-redis");
        }

        @Test
        @DisplayName("getCacheSettingsKey returns empty for unknown")
        void getCacheSettingsKey_unknown_empty() {
            assertThat(StackMapping.getCacheSettingsKey("unknown"))
                    .isEmpty();
        }
    }
}
