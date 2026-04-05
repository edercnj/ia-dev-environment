package dev.iadev.config;

import dev.iadev.exception.ConfigParseException;
import dev.iadev.exception.ConfigValidationException;
import dev.iadev.domain.model.ProjectConfig;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("ConfigLoader")
class ConfigLoaderTest {

    @TempDir
    Path tempDir;

    private Path writeYaml(String content) throws IOException {
        Path file = tempDir.resolve("config.yaml");
        Files.writeString(file, content);
        return file;
    }

    private static final String FULL_CONFIG = """
            project:
              name: "my-app"
              purpose: "A microservice"
            architecture:
              style: microservice
              domain_driven: true
              event_driven: false
            interfaces:
              - type: rest
                spec: openapi
            language:
              name: java
              version: "21"
            framework:
              name: spring-boot
              version: "3.4"
              build_tool: maven
            data:
              database:
                name: postgresql
                version: "16"
              cache:
                name: redis
                version: "7"
            testing:
              coverage_line: 95
              coverage_branch: 90
              smoke_tests: true
              contract_tests: false
            """;

    private static final String MINIMAL_CONFIG = """
            project:
              name: "minimal"
              purpose: "Minimal test"
            architecture:
              style: library
            interfaces:
              - type: cli
            language:
              name: python
              version: "3.10"
            framework:
              name: click
              version: "8.1"
            """;

    @Nested
    @DisplayName("loadConfig() with valid full config")
    class ValidFullConfig {

        @Test
        @DisplayName("returns ProjectConfig with all sections")
        void loadConfig_fullConfig_returnsPopulatedConfig()
                throws IOException {
            Path file = writeYaml(FULL_CONFIG);

            ProjectConfig config = ConfigLoader.loadConfig(
                    file.toString());

            assertThat(config.project().name())
                    .isEqualTo("my-app");
            assertThat(config.project().purpose())
                    .isEqualTo("A microservice");
            assertThat(config.architecture().style())
                    .isEqualTo("microservice");
            assertThat(config.architecture().domainDriven()).isTrue();
            assertThat(config.architecture().eventDriven()).isFalse();
            assertThat(config.interfaces()).hasSize(1);
            assertThat(config.interfaces().get(0).type())
                    .isEqualTo("rest");
            assertThat(config.language().name()).isEqualTo("java");
            assertThat(config.language().version()).isEqualTo("21");
            assertThat(config.framework().name())
                    .isEqualTo("spring-boot");
            assertThat(config.framework().buildTool())
                    .isEqualTo("maven");
            assertThat(config.data().database().name())
                    .isEqualTo("postgresql");
            assertThat(config.data().cache().name())
                    .isEqualTo("redis");
            assertThat(config.testing().coverageLine())
                    .isEqualTo(95);
        }
    }

    @Nested
    @DisplayName("loadConfig() with minimal config")
    class ValidMinimalConfig {

        @Test
        @DisplayName("uses defaults for optional sections")
        void loadConfig_minimalConfig_defaultsApplied()
                throws IOException {
            Path file = writeYaml(MINIMAL_CONFIG);

            ProjectConfig config = ConfigLoader.loadConfig(
                    file.toString());

            assertThat(config.project().name())
                    .isEqualTo("minimal");
            assertThat(config.data().database().name())
                    .isEqualTo("none");
            assertThat(config.data().cache().name())
                    .isEqualTo("none");
            assertThat(config.testing().coverageLine())
                    .isEqualTo(95);
            assertThat(config.testing().coverageBranch())
                    .isEqualTo(90);
            assertThat(config.infrastructure().container())
                    .isEqualTo("docker");
        }
    }

    @Nested
    @DisplayName("loadConfig() with missing required sections")
    class MissingRequiredSections {

        @Test
        @DisplayName("throws when project section is missing")
        void loadConfig_missingProject_throwsValidation()
                throws IOException {
            String yaml = """
                    architecture:
                      style: microservice
                    interfaces:
                      - type: rest
                    language:
                      name: java
                      version: "21"
                    framework:
                      name: spring-boot
                      version: "3.4"
                    """;
            Path file = writeYaml(yaml);

            assertThatThrownBy(() ->
                    ConfigLoader.loadConfig(file.toString()))
                    .isInstanceOf(ConfigValidationException.class)
                    .satisfies(e -> {
                        var cve = (ConfigValidationException) e;
                        assertThat(cve.getMissingSections())
                                .contains("project");
                    });
        }

        @Test
        @DisplayName("throws when language section is missing")
        void loadConfig_missingLanguage_throwsValidation()
                throws IOException {
            String yaml = """
                    project:
                      name: "test"
                      purpose: "test"
                    architecture:
                      style: microservice
                    interfaces:
                      - type: rest
                    framework:
                      name: spring-boot
                      version: "3.4"
                    """;
            Path file = writeYaml(yaml);

            assertThatThrownBy(() ->
                    ConfigLoader.loadConfig(file.toString()))
                    .isInstanceOf(ConfigValidationException.class)
                    .satisfies(e -> {
                        var cve = (ConfigValidationException) e;
                        assertThat(cve.getMissingSections())
                                .contains("language");
                    });
        }

        @Test
        @DisplayName("throws when multiple sections are missing")
        void loadConfig_multipleMissing_listsAll()
                throws IOException {
            String yaml = """
                    project:
                      name: "test"
                      purpose: "test"
                    """;
            Path file = writeYaml(yaml);

            assertThatThrownBy(() ->
                    ConfigLoader.loadConfig(file.toString()))
                    .isInstanceOf(ConfigValidationException.class)
                    .satisfies(e -> {
                        var cve = (ConfigValidationException) e;
                        assertThat(cve.getMissingSections())
                                .contains("architecture",
                                        "interfaces",
                                        "language",
                                        "framework");
                    });
        }
    }

    @Nested
    @DisplayName("loadConfig() with invalid YAML")
    class InvalidYaml {

        @Test
        @DisplayName("throws ConfigParseException for syntax error")
        void loadConfig_syntaxError_throwsParseException()
                throws IOException {
            String invalid = """
                    project:
                      name: "test
                    invalid: [
                    """;
            Path file = writeYaml(invalid);

            assertThatThrownBy(() ->
                    ConfigLoader.loadConfig(file.toString()))
                    .isInstanceOf(ConfigParseException.class)
                    .satisfies(e -> {
                        var cpe = (ConfigParseException) e;
                        assertThat(cpe.getFilePath())
                                .isEqualTo(file.toString());
                        assertThat(cpe.getCause())
                                .isInstanceOf(
                                        Exception.class);
                    });
        }

        @Test
        @DisplayName("throws for non-existent file")
        void loadConfig_nonExistentFile_throwsParseException() {
            String path = "/nonexistent/config.yaml";

            assertThatThrownBy(() ->
                    ConfigLoader.loadConfig(path))
                    .isInstanceOf(ConfigParseException.class)
                    .satisfies(e -> {
                        var cpe = (ConfigParseException) e;
                        assertThat(cpe.getFilePath())
                                .isEqualTo(path);
                    });
        }
    }

    @Nested
    @DisplayName("loadConfig() with null/empty YAML content")
    class NullContent {

        @Test
        @DisplayName("throws for empty file")
        void loadConfig_emptyFile_throwsValidation()
                throws IOException {
            Path file = writeYaml("");

            assertThatThrownBy(() ->
                    ConfigLoader.loadConfig(file.toString()))
                    .isInstanceOf(ConfigValidationException.class);
        }

        @Test
        @DisplayName("throws for YAML with only scalar")
        void loadConfig_scalarContent_throwsValidation()
                throws IOException {
            Path file = writeYaml("just a string");

            assertThatThrownBy(() ->
                    ConfigLoader.loadConfig(file.toString()))
                    .isInstanceOf(ConfigValidationException.class);
        }

        @Test
        @DisplayName("throws for YAML array at root")
        void loadConfig_arrayRoot_throwsValidation()
                throws IOException {
            Path file = writeYaml("- item1\n- item2\n");

            assertThatThrownBy(() ->
                    ConfigLoader.loadConfig(file.toString()))
                    .isInstanceOf(ConfigValidationException.class);
        }
    }

    @Nested
    @DisplayName("shorthand mapping")
    class ShorthandMapping {

        @Test
        @DisplayName("api shorthand resolves to microservice+rest")
        void loadConfig_typeApi_resolvesToMicroserviceRest()
                throws IOException {
            String yaml = """
                    project:
                      name: "api-app"
                      purpose: "API app"
                      type: api
                    language:
                      name: java
                      version: "21"
                    framework:
                      name: spring-boot
                      version: "3.4"
                    """;
            Path file = writeYaml(yaml);

            ProjectConfig config = ConfigLoader.loadConfig(
                    file.toString());

            assertThat(config.architecture().style())
                    .isEqualTo("microservice");
            assertThat(config.interfaces()).hasSize(1);
            assertThat(config.interfaces().get(0).type())
                    .isEqualTo("rest");
        }

        @Test
        @DisplayName("cli shorthand resolves to library+cli")
        void loadConfig_typeCli_resolvesToLibraryCli()
                throws IOException {
            String yaml = """
                    project:
                      name: "cli-app"
                      purpose: "CLI app"
                      type: cli
                    language:
                      name: python
                      version: "3.10"
                    framework:
                      name: click
                      version: "8.1"
                    """;
            Path file = writeYaml(yaml);

            ProjectConfig config = ConfigLoader.loadConfig(
                    file.toString());

            assertThat(config.architecture().style())
                    .isEqualTo("library");
            assertThat(config.interfaces()).hasSize(1);
            assertThat(config.interfaces().get(0).type())
                    .isEqualTo("cli");
        }

        @Test
        @DisplayName("library shorthand resolves to library+empty")
        void loadConfig_typeLibrary_resolvesToLibraryEmpty()
                throws IOException {
            String yaml = """
                    project:
                      name: "lib-app"
                      purpose: "Library"
                      type: library
                    language:
                      name: java
                      version: "21"
                    framework:
                      name: spring-boot
                      version: "3.4"
                    """;
            Path file = writeYaml(yaml);

            ProjectConfig config = ConfigLoader.loadConfig(
                    file.toString());

            assertThat(config.architecture().style())
                    .isEqualTo("library");
            assertThat(config.interfaces()).isEmpty();
        }

        @Test
        @DisplayName("worker shorthand resolves to "
                + "microservice+event-consumer")
        void loadConfig_typeWorker_resolvesToMicroserviceEvent()
                throws IOException {
            String yaml = """
                    project:
                      name: "worker-app"
                      purpose: "Worker"
                      type: worker
                    language:
                      name: java
                      version: "21"
                    framework:
                      name: spring-boot
                      version: "3.4"
                    """;
            Path file = writeYaml(yaml);

            ProjectConfig config = ConfigLoader.loadConfig(
                    file.toString());

            assertThat(config.architecture().style())
                    .isEqualTo("microservice");
            assertThat(config.interfaces()).hasSize(1);
            assertThat(config.interfaces().get(0).type())
                    .isEqualTo("event-consumer");
        }

        @Test
        @DisplayName("fullstack shorthand resolves to monolith+rest")
        void loadConfig_typeFullstack_resolvesToMonolithRest()
                throws IOException {
            String yaml = """
                    project:
                      name: "full-app"
                      purpose: "Fullstack"
                      type: fullstack
                    language:
                      name: java
                      version: "21"
                    framework:
                      name: spring-boot
                      version: "3.4"
                    """;
            Path file = writeYaml(yaml);

            ProjectConfig config = ConfigLoader.loadConfig(
                    file.toString());

            assertThat(config.architecture().style())
                    .isEqualTo("monolith");
            assertThat(config.interfaces()).hasSize(1);
            assertThat(config.interfaces().get(0).type())
                    .isEqualTo("rest");
        }

        @Test
        @DisplayName("explicit architecture/interfaces override "
                + "shorthand when both present")
        void loadConfig_typeWithExplicitSections_explicitWins()
                throws IOException {
            String yaml = """
                    project:
                      name: "override-app"
                      purpose: "Override test"
                      type: api
                    architecture:
                      style: monolith
                    interfaces:
                      - type: grpc
                    language:
                      name: java
                      version: "21"
                    framework:
                      name: spring-boot
                      version: "3.4"
                    """;
            Path file = writeYaml(yaml);

            ProjectConfig config = ConfigLoader.loadConfig(
                    file.toString());

            assertThat(config.architecture().style())
                    .isEqualTo("monolith");
            assertThat(config.interfaces().get(0).type())
                    .isEqualTo("grpc");
        }
    }

    @Nested
    @DisplayName("REQUIRED_SECTIONS constant")
    class RequiredSectionsConstant {

        @Test
        @DisplayName("contains the five required section names")
        void requiredSections_whenCalled_containsFiveSections() {
            assertThat(ConfigLoader.REQUIRED_SECTIONS)
                    .containsExactly("project", "architecture",
                            "interfaces", "language", "framework");
        }
    }

    @Nested
    @DisplayName("compliance field parsing")
    class ComplianceField {

        @Test
        @DisplayName("defaults to 'none' when absent")
        void loadConfig_noCompliance_defaultsToNone()
                throws IOException {
            Path file = writeYaml(MINIMAL_CONFIG);

            ProjectConfig config = ConfigLoader.loadConfig(
                    file.toString());

            assertThat(config.compliance())
                    .isEqualTo("none");
        }

        @Test
        @DisplayName("parses 'pci-dss' correctly")
        void loadConfig_compliancePciDss_parsed()
                throws IOException {
            String yaml = MINIMAL_CONFIG
                    + "compliance: pci-dss\n";
            Path file = writeYaml(yaml);

            ProjectConfig config = ConfigLoader.loadConfig(
                    file.toString());

            assertThat(config.compliance())
                    .isEqualTo("pci-dss");
        }

        @Test
        @DisplayName("parses 'none' correctly")
        void loadConfig_complianceNone_parsed()
                throws IOException {
            String yaml = MINIMAL_CONFIG
                    + "compliance: none\n";
            Path file = writeYaml(yaml);

            ProjectConfig config = ConfigLoader.loadConfig(
                    file.toString());

            assertThat(config.compliance())
                    .isEqualTo("none");
        }

        @Test
        @DisplayName("rejects unsupported value with clear error")
        void loadConfig_complianceInvalid_throws()
                throws IOException {
            String yaml = MINIMAL_CONFIG
                    + "compliance: sox\n";
            Path file = writeYaml(yaml);

            assertThatThrownBy(() ->
                    ConfigLoader.loadConfig(file.toString()))
                    .isInstanceOf(
                            dev.iadev.domain.model
                                    .ConfigValidationException
                                    .class)
                    .hasMessageContaining(
                            "Unsupported compliance value:"
                                    + " 'sox'")
                    .hasMessageContaining(
                            "Supported: none, pci-dss");
        }
    }
}
