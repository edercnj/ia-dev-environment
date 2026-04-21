package dev.iadev.infrastructure.config;

import dev.iadev.domain.port.input.GenerateEnvironmentUseCase;
import dev.iadev.domain.port.input.ListStackProfilesUseCase;
import dev.iadev.domain.port.input.ValidateConfigUseCase;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("ApplicationFactory")
class ApplicationFactoryTest {

    @Nested
    @DisplayName("Happy path — full graph assembly")
    class HappyPath {

        @Test
        void constructor_assembliesCompleteDependencyGraph() {
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
        void generateUseCase_returnsCorrectType() {
            var factory = new ApplicationFactory();

            assertThat(factory.generateUseCase())
                    .isInstanceOf(
                            GenerateEnvironmentUseCase.class);
        }

        @Test
        void validateUseCase_returnsCorrectType() {
            var factory = new ApplicationFactory();

            assertThat(factory.validateUseCase())
                    .isInstanceOf(
                            ValidateConfigUseCase.class);
        }

        @Test
        void listProfilesUseCase_returnsCorrectType() {
            var factory = new ApplicationFactory();

            assertThat(factory.listProfilesUseCase())
                    .isInstanceOf(
                            ListStackProfilesUseCase.class);
        }

        @Test
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
    @DisplayName("Picocli IFactory integration")
    class PicocliIntegration {

        @Test
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
