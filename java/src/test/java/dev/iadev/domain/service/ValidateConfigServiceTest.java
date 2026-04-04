package dev.iadev.domain.service;

import dev.iadev.domain.model.ArchitectureConfig;
import dev.iadev.domain.model.DataConfig;
import dev.iadev.domain.model.FrameworkConfig;
import dev.iadev.domain.model.InfraConfig;
import dev.iadev.domain.model.InterfaceConfig;
import dev.iadev.domain.model.LanguageConfig;
import dev.iadev.domain.model.McpConfig;
import dev.iadev.domain.model.ObservabilityConfig;
import dev.iadev.domain.model.ProjectConfig;
import dev.iadev.domain.model.ProjectIdentity;
import dev.iadev.domain.model.SecurityConfig;
import dev.iadev.domain.model.StackProfile;
import dev.iadev.domain.model.TestingConfig;
import dev.iadev.domain.model.ValidationResult;
import dev.iadev.domain.port.output.StackProfileRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link ValidateConfigService}.
 *
 * <p>Tests use mocked Output Ports to validate business logic
 * in isolation from infrastructure.</p>
 */
@ExtendWith(MockitoExtension.class)
class ValidateConfigServiceTest {

    @Mock
    private StackProfileRepository profileRepository;

    private ValidateConfigService service;

    @BeforeEach
    void setUp() {
        service = new ValidateConfigService(
                profileRepository);
    }

    @Nested
    @DisplayName("Constructor validation")
    class ConstructorValidation {

        @Test
        @DisplayName("constructor_nullRepository"
                + "_throwsNullPointerException")
        void constructor_nullRepository_throwsNpe() {
            assertThatThrownBy(
                    () -> new ValidateConfigService(null))
                    .isInstanceOf(NullPointerException.class)
                    .hasMessageContaining(
                            "profileRepository");
        }
    }

    @Nested
    @DisplayName("validate — happy path")
    class ValidateHappyPath {

        @Test
        @DisplayName("validate_validConfig"
                + "_returnsSuccessWithZeroErrors")
        void validate_validConfig_returnsSuccessWithZeroErrors() {
            ProjectConfig config = buildValidConfig();

            ValidationResult result =
                    service.validate(config);

            assertThat(result.valid()).isTrue();
            assertThat(result.errors()).isEmpty();
        }
    }

    @Nested
    @DisplayName("validate — error paths")
    class ValidateErrorPaths {

        @Test
        @DisplayName("validate_nullConfig"
                + "_throwsNullPointerException")
        void validate_nullConfig_throwsNpe() {
            assertThatThrownBy(
                    () -> service.validate(null))
                    .isInstanceOf(NullPointerException.class)
                    .hasMessageContaining("config");
        }

        @Test
        @DisplayName("validate_nullProjectIdentity"
                + "_returnsErrorResult")
        void validate_nullProjectIdentity_returnsErrorResult() {
            ProjectConfig config = buildConfigWithNullIdentity();

            ValidationResult result =
                    service.validate(config);

            assertThat(result.valid()).isFalse();
            assertThat(result.errors())
                    .isNotEmpty()
                    .anyMatch(e -> e.toLowerCase()
                            .contains("project"));
        }

        @Test
        @DisplayName("validate_nullLanguage"
                + "_returnsErrorResult")
        void validate_nullLanguage_returnsErrorResult() {
            ProjectConfig config = buildConfigWithNullLanguage();

            ValidationResult result =
                    service.validate(config);

            assertThat(result.valid()).isFalse();
            assertThat(result.errors())
                    .isNotEmpty()
                    .anyMatch(e -> e.toLowerCase()
                            .contains("language"));
        }

        @Test
        @DisplayName("validate_nullFramework"
                + "_returnsErrorResult")
        void validate_nullFramework_returnsErrorResult() {
            ProjectConfig config =
                    buildConfigWithNullFramework();

            ValidationResult result =
                    service.validate(config);

            assertThat(result.valid()).isFalse();
            assertThat(result.errors())
                    .isNotEmpty()
                    .anyMatch(e -> e.toLowerCase()
                            .contains("framework"));
        }

        @Test
        @DisplayName("validate_emptyInterfaces"
                + "_returnsErrorResult")
        void validate_emptyInterfaces_returnsErrorResult() {
            ProjectConfig config =
                    buildConfigWithEmptyInterfaces();

            ValidationResult result =
                    service.validate(config);

            assertThat(result.valid()).isFalse();
            assertThat(result.errors())
                    .isNotEmpty()
                    .anyMatch(e -> e.toLowerCase()
                            .contains("interface"));
        }
    }

    @Nested
    @DisplayName("validate — architecture cross-field")
    class ArchitectureCrossField {

        @Test
        @DisplayName("validate_archUnitWithoutBasePackage"
                + "_returnsErrorResult")
        void validate_archUnitNoBase_returnsError() {
            ProjectConfig config =
                    buildConfigWithArchUnit(true, "");

            ValidationResult result =
                    service.validate(config);

            assertThat(result.valid()).isFalse();
            assertThat(result.errors())
                    .isNotEmpty()
                    .anyMatch(e -> e.toLowerCase()
                            .contains("basepackage"));
        }

        @Test
        @DisplayName("validate_archUnitWithBasePackage"
                + "_returnsValidResult")
        void validate_archUnitWithBase_returnsValid() {
            ProjectConfig config =
                    buildConfigWithArchUnit(
                            true, "com.example.myapp");

            ValidationResult result =
                    service.validate(config);

            assertThat(result.valid()).isTrue();
            assertThat(result.errors()).isEmpty();
        }

        @Test
        @DisplayName("validate_archUnitFalseNoBase"
                + "_returnsValidResult")
        void validate_archUnitFalseNoBase_returnsValid() {
            ProjectConfig config =
                    buildConfigWithArchUnit(false, "");

            ValidationResult result =
                    service.validate(config);

            assertThat(result.valid()).isTrue();
            assertThat(result.errors()).isEmpty();
        }

        private ProjectConfig buildConfigWithArchUnit(
                boolean archUnit, String basePackage) {
            return new ProjectConfig(
                    new ProjectIdentity("test-project",
                            "Test purpose"),
                    new ArchitectureConfig("hexagonal",
                            false, false,
                            archUnit, basePackage,
                            "eventstoredb", "", false, "",
                            100, false),
                    List.of(new InterfaceConfig(
                            "rest", "", "")),
                    new LanguageConfig("java", "21"),
                    new FrameworkConfig("quarkus", "3.17",
                            "maven", false),
                    DataConfig.fromMap(Map.of()),
                    new InfraConfig("docker", "none",
                            "kustomize", "none", "none",
                            "none", "none", "none",
                            ObservabilityConfig.fromMap(
                                    Map.of())),
                    SecurityConfig.fromMap(Map.of()),
                    TestingConfig.fromMap(Map.of()),
                    McpConfig.fromMap(Map.of()));
        }
    }

    @Nested
    @DisplayName("Interface implementation")
    class InterfaceImplementation {

        @Test
        @DisplayName("service_implementsValidateConfigUseCase")
        void service_implementsValidateConfigUseCase() {
            assertThat(service)
                    .isInstanceOf(
                            dev.iadev.domain.port.input
                                    .ValidateConfigUseCase
                                    .class);
        }
    }

    // --- Test fixture builders ---

    private ProjectConfig buildValidConfig() {
        return new ProjectConfig(
                new ProjectIdentity("test-project",
                        "Test purpose"),
                new ArchitectureConfig("microservice",
                        false, false, false, "",
                        "eventstoredb", "", false, "",
                        100, false),
                List.of(new InterfaceConfig(
                        "rest", "", "")),
                new LanguageConfig("java", "21"),
                new FrameworkConfig("quarkus", "3.17",
                        "maven", false),
                DataConfig.fromMap(Map.of()),
                new InfraConfig("docker", "none",
                        "kustomize", "none", "none",
                        "none", "none", "none",
                        ObservabilityConfig.fromMap(
                                Map.of())),
                SecurityConfig.fromMap(Map.of()),
                TestingConfig.fromMap(Map.of()),
                McpConfig.fromMap(Map.of()));
    }

    private ProjectConfig buildConfigWithNullIdentity() {
        return new ProjectConfig(
                null,
                new ArchitectureConfig("microservice",
                        false, false, false, "",
                        "eventstoredb", "", false, "",
                        100, false),
                List.of(new InterfaceConfig(
                        "rest", "", "")),
                new LanguageConfig("java", "21"),
                new FrameworkConfig("quarkus", "3.17",
                        "maven", false),
                DataConfig.fromMap(Map.of()),
                new InfraConfig("docker", "none",
                        "kustomize", "none", "none",
                        "none", "none", "none",
                        ObservabilityConfig.fromMap(
                                Map.of())),
                SecurityConfig.fromMap(Map.of()),
                TestingConfig.fromMap(Map.of()),
                McpConfig.fromMap(Map.of()));
    }

    private ProjectConfig buildConfigWithNullLanguage() {
        return new ProjectConfig(
                new ProjectIdentity("test-project",
                        "Test purpose"),
                new ArchitectureConfig("microservice",
                        false, false, false, "",
                        "eventstoredb", "", false, "",
                        100, false),
                List.of(new InterfaceConfig(
                        "rest", "", "")),
                null,
                new FrameworkConfig("quarkus", "3.17",
                        "maven", false),
                DataConfig.fromMap(Map.of()),
                new InfraConfig("docker", "none",
                        "kustomize", "none", "none",
                        "none", "none", "none",
                        ObservabilityConfig.fromMap(
                                Map.of())),
                SecurityConfig.fromMap(Map.of()),
                TestingConfig.fromMap(Map.of()),
                McpConfig.fromMap(Map.of()));
    }

    private ProjectConfig buildConfigWithNullFramework() {
        return new ProjectConfig(
                new ProjectIdentity("test-project",
                        "Test purpose"),
                new ArchitectureConfig("microservice",
                        false, false, false, "",
                        "eventstoredb", "", false, "",
                        100, false),
                List.of(new InterfaceConfig(
                        "rest", "", "")),
                new LanguageConfig("java", "21"),
                null,
                DataConfig.fromMap(Map.of()),
                new InfraConfig("docker", "none",
                        "kustomize", "none", "none",
                        "none", "none", "none",
                        ObservabilityConfig.fromMap(
                                Map.of())),
                SecurityConfig.fromMap(Map.of()),
                TestingConfig.fromMap(Map.of()),
                McpConfig.fromMap(Map.of()));
    }

    private ProjectConfig buildConfigWithEmptyInterfaces() {
        return new ProjectConfig(
                new ProjectIdentity("test-project",
                        "Test purpose"),
                new ArchitectureConfig("microservice",
                        false, false, false, "",
                        "eventstoredb", "", false, "",
                        100, false),
                List.of(),
                new LanguageConfig("java", "21"),
                new FrameworkConfig("quarkus", "3.17",
                        "maven", false),
                DataConfig.fromMap(Map.of()),
                new InfraConfig("docker", "none",
                        "kustomize", "none", "none",
                        "none", "none", "none",
                        ObservabilityConfig.fromMap(
                                Map.of())),
                SecurityConfig.fromMap(Map.of()),
                TestingConfig.fromMap(Map.of()),
                McpConfig.fromMap(Map.of()));
    }
}
