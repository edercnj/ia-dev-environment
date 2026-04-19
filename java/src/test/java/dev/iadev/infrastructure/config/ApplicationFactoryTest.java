package dev.iadev.infrastructure.config;

import dev.iadev.domain.port.input.GenerateEnvironmentUseCase;
import dev.iadev.domain.port.input.ListStackProfilesUseCase;
import dev.iadev.domain.port.input.ValidateConfigUseCase;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions
        .assertThatThrownBy;

/**
 * Unit tests for {@link ApplicationFactory}.
 *
 * <p>Tests follow TPP ordering: degenerate (null checkpoint
 * dir) -> happy path (full graph) -> boundary cases.</p>
 *
 * <p>Verifies GK-1 (degenerate), GK-2 (happy path),
 * and factory getter contracts.</p>
 */
@DisplayName("ApplicationFactory")
class ApplicationFactoryTest {

    @TempDir
    Path tempDir;

    @Nested
    @DisplayName("GK-2: Happy path — full graph assembly")
    class HappyPath {

        @Test
        @DisplayName("constructor_defaultCheckpointDir"
                + "_assembliesCompleteDependencyGraph")
        void constructor_default_assembliesCompleteGraph() {
            var factory = new ApplicationFactory();

            assertThat(factory.generateUseCase())
                    .isInstanceOf(
                            GenerateEnvironmentUseCase.class);
            assertThat(factory.validateUseCase())
                    .isInstanceOf(
                            ValidateConfigUseCase.class);
            assertThat(factory.listProfilesUseCase())
                    .isInstanceOf(
                            ListStackProfilesUseCase.class);
        }

        @Test
        @DisplayName("generateUseCase_returnsInstance"
                + "_ofGenerateEnvironmentUseCase")
        void generateUseCase_returnsCorrectType() {
            var factory = new ApplicationFactory();

            assertThat(factory.generateUseCase())
                    .isInstanceOf(
                            GenerateEnvironmentUseCase.class);
        }

        @Test
        @DisplayName("validateUseCase_returnsInstance"
                + "_ofValidateConfigUseCase")
        void validateUseCase_returnsCorrectType() {
            var factory = new ApplicationFactory();

            assertThat(factory.validateUseCase())
                    .isInstanceOf(
                            ValidateConfigUseCase.class);
        }

        @Test
        @DisplayName("listProfilesUseCase_returnsInstance"
                + "_ofListStackProfilesUseCase")
        void listProfilesUseCase_returnsCorrectType() {
            var factory = new ApplicationFactory();

            assertThat(factory.listProfilesUseCase())
                    .isInstanceOf(
                            ListStackProfilesUseCase.class);
        }

        @Test
        @DisplayName("constructor_calledMultipleTimes"
                + "_returnsSameUseCaseInstances")
        void constructor_sameFactory_returnsSameInstances() {
            var factory = new ApplicationFactory();

            assertThat(factory.generateUseCase())
                    .isSameAs(factory.generateUseCase());
            assertThat(factory.validateUseCase())
                    .isSameAs(factory.validateUseCase());
            assertThat(factory.listProfilesUseCase())
                    .isSameAs(
                            factory.listProfilesUseCase());
        }
    }

    @Nested
    @DisplayName("Custom checkpoint directory")
    class CustomCheckpointDir {

        @Test
        @DisplayName("constructor_customCheckpointDir"
                + "_assembliesCompleteDependencyGraph")
        void constructor_customDir_assembliesGraph() {
            Path checkpointDir =
                    tempDir.resolve("checkpoints");
            var factory =
                    new ApplicationFactory(checkpointDir);

            assertThat(factory.generateUseCase())
                    .isInstanceOf(
                            GenerateEnvironmentUseCase.class);
            assertThat(factory.validateUseCase())
                    .isInstanceOf(
                            ValidateConfigUseCase.class);
            assertThat(factory.listProfilesUseCase())
                    .isInstanceOf(
                            ListStackProfilesUseCase.class);
        }

        @Test
        @DisplayName("constructor_nullCheckpointDir"
                + "_throwsNullPointerException")
        void constructor_nullDir_throwsNpe() {
            assertThatThrownBy(
                    () -> new ApplicationFactory(null))
                    .isInstanceOf(
                            NullPointerException.class)
                    .hasMessageContaining(
                            "checkpointDir");
        }
    }

    @Nested
    @DisplayName("Picocli IFactory integration")
    class PicocliIntegration {

        @Test
        @DisplayName("create_knownCommandClass"
                + "_returnsWiredInstance")
        void create_knownClass_returnsInstance()
                throws Exception {
            var factory = new ApplicationFactory();
            Object instance = factory.create(
                    ApplicationFactory.class);

            assertThat(instance)
                    .isInstanceOf(ApplicationFactory.class);
        }
    }
}
