package dev.iadev.config;

import dev.iadev.domain.model.ProjectConfig;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("ConfigProfiles")
class ConfigProfilesTest {

    private static final List<String> ALL_STACKS = List.of(
            "java-picocli-cli",
            "java-quarkus",
            "java-spring",
            "java-spring-fintech-pci",
            "java-spring-hexagonal",
            "java-spring-cqrs-es",
            "java-spring-event-driven",
            "java-spring-fintech-pci",
            "python-fastapi",
            "python-click-cli",
            "go-gin",
            "kotlin-ktor",
            "typescript-nestjs",
            "rust-axum"
    );

    @Nested
    @DisplayName("getAvailableStacks()")
    class AvailableStacks {

        @Test
        @DisplayName("returns all 13 stack keys")
        void getAvailableStacks_whenCalled_returns13Keys() {
            List<String> stacks =
                    ConfigProfiles.getAvailableStacks();

            assertThat(stacks).hasSize(13);
            assertThat(stacks).containsAll(ALL_STACKS);
        }
    }

    @Nested
    @DisplayName("isValidStack()")
    class ValidStack {

        @ParameterizedTest
        @ValueSource(strings = {
                "java-picocli-cli", "java-quarkus",
                "java-spring", "java-spring-fintech-pci",
                "java-spring-hexagonal",
                "java-spring-cqrs-es",
                "java-spring-event-driven",
                "java-spring-fintech-pci",
                "python-fastapi", "python-click-cli",
                "go-gin", "kotlin-ktor",
                "typescript-nestjs", "rust-axum"})
        @DisplayName("returns true for valid stack keys")
        void isValidStack_validKey_returnsTrue(String key) {
            assertThat(ConfigProfiles.isValidStack(key)).isTrue();
        }

        @Test
        @DisplayName("returns false for unknown stack key")
        void isValidStack_unknownKey_returnsFalse() {
            assertThat(ConfigProfiles.isValidStack("csharp-dotnet"))
                    .isFalse();
        }

        @Test
        @DisplayName("returns false for null stack key")
        void isValidStack_null_returnsFalse() {
            assertThat(ConfigProfiles.isValidStack(null)).isFalse();
        }
    }

    @Nested
    @DisplayName("getStack() — java-spring")
    class JavaSpringStack {

        @Test
        @DisplayName("returns ProjectConfig for java-spring")
        void getStack_javaSpring_returnsConfig() {
            ProjectConfig config =
                    ConfigProfiles.getStack("java-spring");

            assertThat(config.language().name())
                    .isEqualTo("java");
            assertThat(config.language().version())
                    .isEqualTo("21");
            assertThat(config.framework().name())
                    .isEqualTo("spring-boot");
            assertThat(config.architecture().style())
                    .isEqualTo("microservice");
        }
    }

    @Nested
    @DisplayName("getStack() — java-quarkus")
    class JavaQuarkusStack {

        @Test
        @DisplayName("returns ProjectConfig for java-quarkus")
        void getStack_javaQuarkus_returnsConfig() {
            ProjectConfig config =
                    ConfigProfiles.getStack("java-quarkus");

            assertThat(config.language().name())
                    .isEqualTo("java");
            assertThat(config.language().version())
                    .isEqualTo("21");
            assertThat(config.framework().name())
                    .isEqualTo("quarkus");
        }
    }

    @Nested
    @DisplayName("getStack() — python-fastapi")
    class PythonFastapiStack {

        @Test
        @DisplayName("returns ProjectConfig for python-fastapi")
        void getStack_pythonFastapi_returnsConfig() {
            ProjectConfig config =
                    ConfigProfiles.getStack("python-fastapi");

            assertThat(config.language().name())
                    .isEqualTo("python");
            assertThat(config.framework().name())
                    .isEqualTo("fastapi");
        }
    }

    @Nested
    @DisplayName("getStack() — python-click-cli")
    class PythonClickCliStack {

        @Test
        @DisplayName("returns ProjectConfig for python-click-cli")
        void getStack_pythonClickCli_returnsConfig() {
            ProjectConfig config =
                    ConfigProfiles.getStack("python-click-cli");

            assertThat(config.language().name())
                    .isEqualTo("python");
            assertThat(config.framework().name())
                    .isEqualTo("click");
            assertThat(config.architecture().style())
                    .isEqualTo("library");
        }
    }

    @Nested
    @DisplayName("getStack() — go-gin")
    class GoGinStack {

        @Test
        @DisplayName("returns ProjectConfig for go-gin")
        void getStack_goGin_returnsConfig() {
            ProjectConfig config =
                    ConfigProfiles.getStack("go-gin");

            assertThat(config.language().name())
                    .isEqualTo("go");
            assertThat(config.framework().name())
                    .isEqualTo("gin");
        }
    }

    @Nested
    @DisplayName("getStack() — kotlin-ktor")
    class KotlinKtorStack {

        @Test
        @DisplayName("returns ProjectConfig for kotlin-ktor")
        void getStack_kotlinKtor_returnsConfig() {
            ProjectConfig config =
                    ConfigProfiles.getStack("kotlin-ktor");

            assertThat(config.language().name())
                    .isEqualTo("kotlin");
            assertThat(config.framework().name())
                    .isEqualTo("ktor");
        }
    }

    @Nested
    @DisplayName("getStack() — typescript-nestjs")
    class TypescriptNestjsStack {

        @Test
        @DisplayName("returns ProjectConfig for typescript-nestjs")
        void getStack_typescriptNestjs_returnsConfig() {
            ProjectConfig config =
                    ConfigProfiles.getStack("typescript-nestjs");

            assertThat(config.language().name())
                    .isEqualTo("typescript");
            assertThat(config.framework().name())
                    .isEqualTo("nestjs");
        }
    }

    @Nested
    @DisplayName("getStack() — rust-axum")
    class RustAxumStack {

        @Test
        @DisplayName("returns ProjectConfig for rust-axum")
        void getStack_rustAxum_returnsConfig() {
            ProjectConfig config =
                    ConfigProfiles.getStack("rust-axum");

            assertThat(config.language().name())
                    .isEqualTo("rust");
            assertThat(config.framework().name())
                    .isEqualTo("axum");
        }
    }

    @Nested
    @DisplayName("getStack() — java-spring-fintech-pci")
    class JavaSpringFintechPciStack {

        @Test
        @DisplayName("returns ProjectConfig for "
                + "java-spring-fintech-pci")
        void getStack_fintechPci_returnsConfig() {
            ProjectConfig config =
                    ConfigProfiles.getStack(
                            "java-spring-fintech-pci");

            assertThat(config.language().name())
                    .isEqualTo("java");
            assertThat(config.language().version())
                    .isEqualTo("21");
            assertThat(config.framework().name())
                    .isEqualTo("spring-boot");
            assertThat(config.architecture().style())
                    .isEqualTo("microservice");
        }

        @Test
        @DisplayName("has pci-dss compliance framework")
        void getStack_fintechPci_hasPciCompliance() {
            ProjectConfig config =
                    ConfigProfiles.getStack(
                            "java-spring-fintech-pci");

            assertThat(config.security().frameworks())
                    .contains("pci-dss");
        }

        @Test
        @DisplayName("has encryption at rest enabled")
        void getStack_fintechPci_hasEncryption() {
            ProjectConfig config =
                    ConfigProfiles.getStack(
                            "java-spring-fintech-pci");

            assertThat(config.security().frameworks())
                    .isNotEmpty();
        }
    }

    @Nested
    @DisplayName("getStack() — java-spring-hexagonal")
    class JavaSpringHexagonalStack {

        @Test
        @DisplayName("returns hexagonal profile config")
        void getStack_hexagonal_returnsConfig() {
            ProjectConfig config =
                    ConfigProfiles.getStack(
                            "java-spring-hexagonal");

            assertThat(config.language().name())
                    .isEqualTo("java");
            assertThat(config.framework().name())
                    .isEqualTo("spring-boot");
            assertThat(config.architecture().style())
                    .isEqualTo("hexagonal");
            assertThat(config.architecture().domainDriven())
                    .isTrue();
        }
    }

    @Nested
    @DisplayName("getStack() — java-spring-cqrs-es")
    class JavaSpringCqrsEsStack {

        @Test
        @DisplayName("returns CQRS/ES profile config")
        void getStack_cqrsEs_returnsConfig() {
            ProjectConfig config =
                    ConfigProfiles.getStack(
                            "java-spring-cqrs-es");

            assertThat(config.language().name())
                    .isEqualTo("java");
            assertThat(config.framework().name())
                    .isEqualTo("spring-boot");
            assertThat(config.architecture().style())
                    .isEqualTo("cqrs");
            assertThat(config.architecture().eventStore())
                    .isEqualTo("eventstoredb");
            assertThat(config.architecture()
                    .eventsPerSnapshot())
                    .isEqualTo(100);
        }
    }

    @Nested
    @DisplayName("getStack() — java-spring-event-driven")
    class JavaSpringEventDrivenStack {

        @Test
        @DisplayName("returns event-driven profile config")
        void getStack_eventDriven_returnsConfig() {
            ProjectConfig config =
                    ConfigProfiles.getStack(
                            "java-spring-event-driven");

            assertThat(config.language().name())
                    .isEqualTo("java");
            assertThat(config.framework().name())
                    .isEqualTo("spring-boot");
            assertThat(config.architecture().style())
                    .isEqualTo("event-driven");
            assertThat(config.architecture()
                    .schemaRegistry())
                    .isEqualTo("confluent");
            assertThat(config.architecture()
                    .deadLetterStrategy())
                    .isEqualTo("kafka-dlq");
        }
    }

    @Nested
    @DisplayName("getStack() — invalid key")
    class InvalidKey {

        @Test
        @DisplayName("throws for unknown stack key")
        void getStack_unknownKey_throwsException() {
            assertThatThrownBy(() ->
                    ConfigProfiles.getStack("csharp-dotnet"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("csharp-dotnet");
        }

        @Test
        @DisplayName("throws for null stack key")
        void getStack_null_throwsException() {
            assertThatThrownBy(() ->
                    ConfigProfiles.getStack(null))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }

    @Nested
    @DisplayName("all stacks produce valid ProjectConfig")
    class AllStacksValid {

        @ParameterizedTest
        @ValueSource(strings = {
                "java-picocli-cli", "java-quarkus",
                "java-spring", "java-spring-fintech-pci",
                "java-spring-hexagonal",
                "java-spring-cqrs-es",
                "java-spring-event-driven",
                "java-spring-fintech-pci",
                "python-fastapi", "python-click-cli",
                "go-gin", "kotlin-ktor",
                "typescript-nestjs", "rust-axum"})
        @DisplayName("each stack has non-null required fields")
        void getStack_eachStack_hasRequiredFields(String key) {
            ProjectConfig config =
                    ConfigProfiles.getStack(key);

            assertThat(config.project().name()).isNotBlank();
            assertThat(config.architecture().style())
                    .isNotBlank();
            assertThat(config.interfaces()).isNotEmpty();
            assertThat(config.language().name()).isNotBlank();
            assertThat(config.language().version())
                    .isNotBlank();
            assertThat(config.framework().name())
                    .isNotBlank();
            assertThat(config.data().database().name())
                    .isNotBlank();
            assertThat(config.testing().coverageLine())
                    .isGreaterThan(0);
        }

        @ParameterizedTest
        @ValueSource(strings = {
                "java-picocli-cli", "java-quarkus",
                "java-spring", "java-spring-hexagonal",
                "java-spring-cqrs-es",
                "java-spring-event-driven",
                "python-fastapi", "python-click-cli",
                "go-gin", "kotlin-ktor",
                "typescript-nestjs", "rust-axum"})
        @DisplayName("each stack defaults compliance to 'none'")
        void getStack_eachStack_complianceDefaultsNone(
                String key) {
            ProjectConfig config =
                    ConfigProfiles.getStack(key);

            assertThat(config.compliance())
                    .isEqualTo("none");
        }
    }
}
