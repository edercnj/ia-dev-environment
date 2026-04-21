package dev.iadev.domain.service;

import dev.iadev.domain.model.ArchitectureConfig;
import dev.iadev.domain.model.DataConfig;
import dev.iadev.domain.model.FrameworkConfig;
import dev.iadev.domain.model.GenerationContext;
import dev.iadev.domain.model.GenerationResult;
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
import dev.iadev.domain.port.output.FileSystemWriter;
import dev.iadev.domain.port.output.ProgressReporter;
import dev.iadev.domain.port.output.StackProfileRepository;
import dev.iadev.domain.port.output.TemplateRenderer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions
        .assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GenerateEnvironmentServiceTest {

    @Mock
    private StackProfileRepository profileRepository;

    @Mock
    private TemplateRenderer templateRenderer;

    @Mock
    private FileSystemWriter fileSystemWriter;

    @Mock
    private ProgressReporter progressReporter;

    private GenerateEnvironmentService service;

    @BeforeEach
    void setUp() {
        service = new GenerateEnvironmentService(
                profileRepository,
                templateRenderer,
                fileSystemWriter,
                progressReporter);
    }

    @Nested
    @DisplayName("Constructor validation")
    class ConstructorValidation {

        @Test
        void constructor_nullProfileRepository_throwsNpe() {
            assertThatThrownBy(
                    () -> new GenerateEnvironmentService(
                            null, templateRenderer,
                            fileSystemWriter,
                            progressReporter))
                    .isInstanceOf(NullPointerException.class)
                    .hasMessageContaining(
                            "profileRepository");
        }

        @Test
        void constructor_nullTemplateRenderer_throwsNpe() {
            assertThatThrownBy(
                    () -> new GenerateEnvironmentService(
                            profileRepository, null,
                            fileSystemWriter,
                            progressReporter))
                    .isInstanceOf(NullPointerException.class)
                    .hasMessageContaining(
                            "templateRenderer");
        }

        @Test
        void constructor_nullFileSystemWriter_throwsNpe() {
            assertThatThrownBy(
                    () -> new GenerateEnvironmentService(
                            profileRepository,
                            templateRenderer, null,
                            progressReporter))
                    .isInstanceOf(NullPointerException.class)
                    .hasMessageContaining(
                            "fileSystemWriter");
        }

        @Test
        void constructor_nullProgressReporter_throwsNpe() {
            assertThatThrownBy(
                    () -> new GenerateEnvironmentService(
                            profileRepository,
                            templateRenderer,
                            fileSystemWriter, null))
                    .isInstanceOf(NullPointerException.class)
                    .hasMessageContaining(
                            "progressReporter");
        }
    }

    @Nested
    @DisplayName("generate — happy path")
    class GenerateHappyPath {

        @Test
        void generate_validContext_returnsSuccessResult() {
            GenerationContext context = buildValidContext();
            when(profileRepository.findByName(anyString()))
                    .thenReturn(Optional.of(buildProfile()));

            GenerationResult result =
                    service.generate(context);

            assertThat(result.success()).isTrue();
            assertThat(result.warnings()).isEmpty();
        }

        @Test
        void generate_validContext_reportsProgressComplete() {
            GenerationContext context = buildValidContext();
            when(profileRepository.findByName(anyString()))
                    .thenReturn(Optional.of(buildProfile()));

            service.generate(context);

            verify(progressReporter)
                    .reportStart(anyString(), anyInt());
            verify(progressReporter)
                    .reportComplete(anyString());
        }
    }

    @Nested
    @DisplayName("generate — error paths")
    class GenerateErrorPaths {

        @Test
        void generate_nullContext_throwsNpe() {
            assertThatThrownBy(
                    () -> service.generate(null))
                    .isInstanceOf(NullPointerException.class)
                    .hasMessageContaining("context");
        }

        @Test
        void generate_profileNotFound_returnsErrorResult() {
            GenerationContext context =
                    buildContextWithProfile("nonexistent");
            when(profileRepository.findByName("nonexistent"))
                    .thenReturn(Optional.empty());

            GenerationResult result =
                    service.generate(context);

            assertThat(result.success()).isFalse();
            assertThat(result.warnings())
                    .anyMatch(w -> w.contains("nonexistent"));
        }

        @Test
        void generate_profileNotFound_doesNotWriteFiles() {
            GenerationContext context =
                    buildContextWithProfile("nonexistent");
            when(profileRepository.findByName("nonexistent"))
                    .thenReturn(Optional.empty());

            service.generate(context);

            verify(fileSystemWriter, never())
                    .writeFile(any(Path.class), anyString());
        }

        @Test
        void generate_profileNotFound_reportsError() {
            GenerationContext context =
                    buildContextWithProfile("nonexistent");
            when(profileRepository.findByName("nonexistent"))
                    .thenReturn(Optional.empty());

            service.generate(context);

            verify(progressReporter)
                    .reportError(anyString(), anyString());
        }
    }

    @Nested
    @DisplayName("Interface implementation")
    class InterfaceImplementation {

        @Test
        void service_implementsGenerateEnvironmentUseCase() {
            assertThat(service)
                    .isInstanceOf(
                            dev.iadev.domain.port.input
                                    .GenerateEnvironmentUseCase
                                    .class);
        }
    }

    private GenerationContext buildValidContext() {
        return new GenerationContext(
                buildValidConfig(),
                Path.of("/tmp/output"),
                false);
    }

    private GenerationContext buildContextWithProfile(
            String profileName) {
        ProjectConfig config = new ProjectConfig(
                new ProjectIdentity(profileName,
                        "Test purpose"),
                new ArchitectureConfig("microservice",
                        false, false, false, "",
                        new ArchitectureConfig.CqrsConfig(
                                "eventstoredb", 100,
                                "", false, ""),
                        false),
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
                McpConfig.fromMap(Map.of()),
                "none",
                java.util.Set.of(),
                null);
        return new GenerationContext(
                config, Path.of("/tmp/output"), false);
    }

    private ProjectConfig buildValidConfig() {
        return new ProjectConfig(
                new ProjectIdentity("test-project",
                        "Test purpose"),
                new ArchitectureConfig("microservice",
                        false, false, false, "",
                        new ArchitectureConfig.CqrsConfig(
                                "eventstoredb", 100,
                                "", false, ""),
                        false),
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
                McpConfig.fromMap(Map.of()),
                "none",
                java.util.Set.of(),
                null);
    }

    private StackProfile buildProfile() {
        return new StackProfile(
                "test-project", "java", "quarkus",
                "maven", Map.of());
    }
}
