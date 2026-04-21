package dev.iadev.infrastructure.config;

import dev.iadev.domain.port.input.GenerateEnvironmentUseCase;
import dev.iadev.domain.port.input.ValidateConfigUseCase;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("ApplicationFactory")
class ApplicationFactoryTest {

    @Test
    void constructor_assembliesCompleteDependencyGraph() {
        var factory = new ApplicationFactory();

        assertThat(factory.generateUseCase())
                .isInstanceOf(
                        GenerateEnvironmentUseCase.class);
        assertThat(factory.validateUseCase())
                .isInstanceOf(
                        ValidateConfigUseCase.class);
    }

    @Test
    void sameFactory_returnsSameInstances() {
        var factory = new ApplicationFactory();

        assertThat(factory.generateUseCase())
                .isSameAs(factory.generateUseCase());
        assertThat(factory.validateUseCase())
                .isSameAs(factory.validateUseCase());
    }

    @Test
    void create_knownClass_returnsInstance()
            throws Exception {
        var factory = new ApplicationFactory();

        assertThat(factory.create(ApplicationFactory.class))
                .isInstanceOf(ApplicationFactory.class);
    }
}
