package dev.iadev.domain.port.input;

import dev.iadev.domain.model.GenerationContext;
import dev.iadev.domain.model.GenerationResult;
import dev.iadev.domain.model.ProjectConfig;
import dev.iadev.domain.model.StackProfile;
import dev.iadev.domain.model.ValidationResult;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Contract tests verifying that input port interfaces define
 * the correct method signatures with domain model types only.
 */
class InputPortContractTest {

    @Nested
    @DisplayName("GenerateEnvironmentUseCase")
    class GenerateEnvironmentUseCaseContract {

        @Test
        @DisplayName("should be an interface")
        void shouldBeInterface() {
            assertThat(GenerateEnvironmentUseCase.class.isInterface())
                    .isTrue();
        }

        @Test
        @DisplayName("generate method should accept "
                + "GenerationContext and return GenerationResult")
        void generate_withGenerationContext_returnsGenerationResult()
                throws NoSuchMethodException {
            Method method = GenerateEnvironmentUseCase.class
                    .getDeclaredMethod("generate",
                            GenerationContext.class);

            assertThat(method.getReturnType())
                    .isEqualTo(GenerationResult.class);
            assertThat(method.getParameterCount())
                    .isEqualTo(1);
            assertThat(method.getParameterTypes()[0])
                    .isEqualTo(GenerationContext.class);
        }
    }

    @Nested
    @DisplayName("ValidateConfigUseCase")
    class ValidateConfigUseCaseContract {

        @Test
        @DisplayName("should be an interface")
        void shouldBeInterface() {
            assertThat(ValidateConfigUseCase.class.isInterface())
                    .isTrue();
        }

        @Test
        @DisplayName("validate method should accept "
                + "ProjectConfig and return ValidationResult")
        void validate_withProjectConfig_returnsValidationResult()
                throws NoSuchMethodException {
            Method method = ValidateConfigUseCase.class
                    .getDeclaredMethod("validate",
                            ProjectConfig.class);

            assertThat(method.getReturnType())
                    .isEqualTo(ValidationResult.class);
            assertThat(method.getParameterCount())
                    .isEqualTo(1);
            assertThat(method.getParameterTypes()[0])
                    .isEqualTo(ProjectConfig.class);
        }
    }

    @Nested
    @DisplayName("ListStackProfilesUseCase")
    class ListStackProfilesUseCaseContract {

        @Test
        @DisplayName("should be an interface")
        void shouldBeInterface() {
            assertThat(ListStackProfilesUseCase.class.isInterface())
                    .isTrue();
        }

        @Test
        @DisplayName("listProfiles method should return "
                + "List of StackProfile")
        void listProfiles_noArgs_returnsStackProfileList()
                throws NoSuchMethodException {
            Method method = ListStackProfilesUseCase.class
                    .getDeclaredMethod("listProfiles");

            assertThat(method.getReturnType())
                    .isEqualTo(List.class);
            assertThat(method.getParameterCount())
                    .isZero();
        }
    }

    @Nested
    @DisplayName("Domain Purity")
    class DomainPurity {

        @Test
        @DisplayName("input ports should not import "
                + "framework classes")
        void inputPorts_noFrameworkImports() {
            Package pkg = GenerateEnvironmentUseCase.class
                    .getPackage();

            assertThat(pkg.getName())
                    .isEqualTo("dev.iadev.domain.port.input");
        }

        @Test
        @DisplayName("all three input ports should exist "
                + "in the same package")
        void allInputPorts_samePackage() {
            String generatePkg = GenerateEnvironmentUseCase.class
                    .getPackageName();
            String validatePkg = ValidateConfigUseCase.class
                    .getPackageName();
            String listPkg = ListStackProfilesUseCase.class
                    .getPackageName();

            assertThat(generatePkg)
                    .isEqualTo(validatePkg)
                    .isEqualTo(listPkg)
                    .isEqualTo("dev.iadev.domain.port.input");
        }
    }
}
