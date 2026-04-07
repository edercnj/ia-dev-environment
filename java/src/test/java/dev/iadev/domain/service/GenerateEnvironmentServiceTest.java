package dev.iadev.domain.service;

import dev.iadev.domain.model.ArchitectureConfig;
import dev.iadev.domain.model.CheckpointState;
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
import dev.iadev.domain.port.output.CheckpointStore;
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
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link GenerateEnvironmentService}.
 *
 * <p>Tests use mocked Output Ports to validate business logic
 * in isolation from infrastructure.</p>
 */
@ExtendWith(MockitoExtension.class)
class GenerateEnvironmentServiceTest {

    @Mock
    private StackProfileRepository profileRepository;

    @Mock
    private TemplateRenderer templateRenderer;

    @Mock
    private FileSystemWriter fileSystemWriter;

    @Mock
    private CheckpointStore checkpointStore;

    @Mock
    private ProgressReporter progressReporter;

    private GenerateEnvironmentService service;

    @BeforeEach
    void setUp() {
        service = new GenerateEnvironmentService(
                profileRepository,
                templateRenderer,
                fileSystemWriter,
                checkpointStore,
                progressReporter);
    }

    @Nested
    @DisplayName("Constructor validation")
    class ConstructorValidation {

        @Test
        @DisplayName("constructor_nullProfileRepository"
                + "_throwsNullPointerException")
        void constructor_nullProfileRepository_throwsNpe() {
            assertThatThrownBy(
                    () -> new GenerateEnvironmentService(
                            null, templateRenderer,
                            fileSystemWriter,
                            checkpointStore,
                            progressReporter))
                    .isInstanceOf(NullPointerException.class)
                    .hasMessageContaining(
                            "profileRepository");
        }

        @Test
        @DisplayName("constructor_nullTemplateRenderer"
                + "_throwsNullPointerException")
        void constructor_nullTemplateRenderer_throwsNpe() {
            assertThatThrownBy(
                    () -> new GenerateEnvironmentService(
                            profileRepository, null,
                            fileSystemWriter,
                            checkpointStore,
                            progressReporter))
                    .isInstanceOf(NullPointerException.class)
                    .hasMessageContaining(
                            "templateRenderer");
        }

        @Test
        @DisplayName("constructor_nullFileSystemWriter"
                + "_throwsNullPointerException")
        void constructor_nullFileSystemWriter_throwsNpe() {
            assertThatThrownBy(
                    () -> new GenerateEnvironmentService(
                            profileRepository,
                            templateRenderer, null,
                            checkpointStore,
                            progressReporter))
                    .isInstanceOf(NullPointerException.class)
                    .hasMessageContaining(
                            "fileSystemWriter");
        }

        @Test
        @DisplayName("constructor_nullCheckpointStore"
                + "_throwsNullPointerException")
        void constructor_nullCheckpointStore_throwsNpe() {
            assertThatThrownBy(
                    () -> new GenerateEnvironmentService(
                            profileRepository,
                            templateRenderer,
                            fileSystemWriter, null,
                            progressReporter))
                    .isInstanceOf(NullPointerException.class)
                    .hasMessageContaining(
                            "checkpointStore");
        }

        @Test
        @DisplayName("constructor_nullProgressReporter"
                + "_throwsNullPointerException")
        void constructor_nullProgressReporter_throwsNpe() {
            assertThatThrownBy(
                    () -> new GenerateEnvironmentService(
                            profileRepository,
                            templateRenderer,
                            fileSystemWriter,
                            checkpointStore, null))
                    .isInstanceOf(NullPointerException.class)
                    .hasMessageContaining(
                            "progressReporter");
        }
    }

    @Nested
    @DisplayName("generate — happy path")
    class GenerateHappyPath {

        @Test
        @DisplayName("generate_validContext"
                + "_returnsSuccessResult")
        void generate_validContext_returnsSuccessResult() {
            GenerationContext context = buildValidContext();
            when(profileRepository.findByName(anyString()))
                    .thenReturn(Optional.of(buildProfile()));
            when(checkpointStore.load(anyString()))
                    .thenReturn(Optional.empty());

            GenerationResult result =
                    service.generate(context);

            assertThat(result.success()).isTrue();
            assertThat(result.warnings()).isEmpty();
        }

        @Test
        @DisplayName("generate_validContext"
                + "_reportsProgressComplete")
        void generate_validContext_reportsProgressComplete() {
            GenerationContext context = buildValidContext();
            when(profileRepository.findByName(anyString()))
                    .thenReturn(Optional.of(buildProfile()));
            when(checkpointStore.load(anyString()))
                    .thenReturn(Optional.empty());

            service.generate(context);

            verify(progressReporter)
                    .reportStart(anyString(), anyInt());
            verify(progressReporter)
                    .reportComplete(anyString());
        }

        @Test
        @DisplayName("generate_validContext"
                + "_savesCheckpoint")
        void generate_validContext_savesCheckpoint() {
            GenerationContext context = buildValidContext();
            when(profileRepository.findByName(anyString()))
                    .thenReturn(Optional.of(buildProfile()));
            when(checkpointStore.load(anyString()))
                    .thenReturn(Optional.empty());

            service.generate(context);

            verify(checkpointStore)
                    .save(any(CheckpointState.class));
        }
    }

    @Nested
    @DisplayName("generate — error paths")
    class GenerateErrorPaths {

        @Test
        @DisplayName("generate_nullContext"
                + "_throwsNullPointerException")
        void generate_nullContext_throwsNpe() {
            assertThatThrownBy(
                    () -> service.generate(null))
                    .isInstanceOf(NullPointerException.class)
                    .hasMessageContaining("context");
        }

        @Test
        @DisplayName("generate_profileNotFound"
                + "_returnsErrorResult")
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
        @DisplayName("generate_profileNotFound"
                + "_doesNotWriteFiles")
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
        @DisplayName("generate_profileNotFound"
                + "_reportsError")
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
    @DisplayName("generate — checkpoint resume")
    class GenerateCheckpointResume {

        @Test
        @DisplayName("generate_existingCheckpoint"
                + "_loadsCheckpoint")
        void generate_existingCheckpoint_loadsCheckpoint() {
            GenerationContext context = buildValidContext();
            when(profileRepository.findByName(anyString()))
                    .thenReturn(Optional.of(buildProfile()));
            CheckpointState existing = new CheckpointState(
                    "test-exec", Instant.now(), Instant.now(),
                    Map.of("step1", "done"), Map.of());
            when(checkpointStore.load(anyString()))
                    .thenReturn(Optional.of(existing));

            GenerationResult result =
                    service.generate(context);

            assertThat(result.success()).isTrue();
            verify(checkpointStore).load(anyString());
        }
    }

    @Nested
    @DisplayName("Interface implementation")
    class InterfaceImplementation {

        @Test
        @DisplayName("service_implementsGenerateEnvironment"
                + "UseCase")
        void service_implementsGenerateEnvironmentUseCase() {
            assertThat(service)
                    .isInstanceOf(
                            dev.iadev.domain.port.input
                                    .GenerateEnvironmentUseCase
                                    .class);
        }
    }

    // --- Test fixture builders ---

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
