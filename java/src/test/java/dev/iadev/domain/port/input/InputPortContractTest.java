package dev.iadev.domain.port.input;

import dev.iadev.domain.model.GenerationContext;
import dev.iadev.domain.model.GenerationResult;
import dev.iadev.domain.model.ProjectConfig;
import dev.iadev.domain.model.ValidationResult;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;

import static org.assertj.core.api.Assertions.assertThat;

class InputPortContractTest {

    @Nested
    @DisplayName("GenerateEnvironmentUseCase")
    class GenerateEnvironmentUseCaseContract {

        @Test
        void shouldBeInterface() {
            assertThat(GenerateEnvironmentUseCase
                    .class.isInterface()).isTrue();
        }

        @Test
        void generate_acceptsContext_returnsResult()
                throws NoSuchMethodException {
            Method method = GenerateEnvironmentUseCase
                    .class.getDeclaredMethod("generate",
                            GenerationContext.class);

            assertThat(method.getReturnType())
                    .isEqualTo(GenerationResult.class);
            assertThat(method.getParameterCount())
                    .isEqualTo(1);
        }
    }

    @Nested
    @DisplayName("ValidateConfigUseCase")
    class ValidateConfigUseCaseContract {

        @Test
        void shouldBeInterface() {
            assertThat(ValidateConfigUseCase
                    .class.isInterface()).isTrue();
        }

        @Test
        void validate_acceptsConfig_returnsResult()
                throws NoSuchMethodException {
            Method method = ValidateConfigUseCase
                    .class.getDeclaredMethod("validate",
                            ProjectConfig.class);

            assertThat(method.getReturnType())
                    .isEqualTo(ValidationResult.class);
            assertThat(method.getParameterCount())
                    .isEqualTo(1);
        }
    }

    @Nested
    @DisplayName("Domain Purity")
    class DomainPurity {

        @Test
        void inputPorts_samePackage() {
            String generatePkg =
                    GenerateEnvironmentUseCase.class
                            .getPackageName();
            String validatePkg =
                    ValidateConfigUseCase.class
                            .getPackageName();

            assertThat(generatePkg)
                    .isEqualTo(validatePkg)
                    .isEqualTo(
                            "dev.iadev.domain.port.input");
        }
    }
}
