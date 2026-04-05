package dev.iadev.domain.stack;

import dev.iadev.testutil.TestConfigBuilder;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for StackValidator — validateStack,
 * validateLanguageFramework.
 */
@DisplayName("StackValidator — stack + lang-fw")
class StackValidatorLangFrameworkTest {

    @Nested
    @DisplayName("validateStack()")
    class ValidateStackTests {

        @Test
        @DisplayName("valid java-quarkus has no errors")
        void validateStack_validJavaQuarkus_noErrors() {
            var config = TestConfigBuilder.builder()
                    .language("java", "21")
                    .framework("quarkus", "3.17")
                    .buildTool("maven")
                    .architectureStyle("microservice")
                    .build();

            var errors =
                    StackValidator.validateStack(config);

            assertThat(errors).isEmpty();
        }

        @Test
        @DisplayName("multiple failures accumulate")
        void validateStack_multiple_allReported() {
            var config = TestConfigBuilder.builder()
                    .language("python", "3.8")
                    .framework("spring-boot", "3.4")
                    .buildTool("pip")
                    .architectureStyle("invalid-style")
                    .clearInterfaces()
                    .addInterface("invalid-type", "", "")
                    .build();

            var errors =
                    StackValidator.validateStack(config);

            assertThat(errors)
                    .hasSizeGreaterThanOrEqualTo(3);
        }
    }

    @Nested
    @DisplayName("validateLanguageFramework()")
    class LanguageFrameworkTests {

        @Test
        @DisplayName("spring-boot with python fails")
        void validateLF_springBootPython_error() {
            var config = TestConfigBuilder.builder()
                    .language("python", "3.12")
                    .framework("spring-boot", "3.4")
                    .build();

            var errors = StackValidator
                    .validateLanguageFramework(config);

            assertThat(errors).isNotEmpty();
            assertThat(errors.get(0))
                    .contains("spring-boot")
                    .contains("python");
        }

        @Test
        @DisplayName("quarkus with java passes")
        void validateLF_quarkusJava_noError() {
            var config = TestConfigBuilder.builder()
                    .language("java", "21")
                    .framework("quarkus", "3.17")
                    .build();

            var errors = StackValidator
                    .validateLanguageFramework(config);

            assertThat(errors).isEmpty();
        }

        @Test
        @DisplayName("quarkus with kotlin passes")
        void validateLF_quarkusKotlin_noError() {
            var config = TestConfigBuilder.builder()
                    .language("kotlin", "2.1")
                    .framework("quarkus", "3.17")
                    .buildTool("gradle")
                    .build();

            var errors = StackValidator
                    .validateLanguageFramework(config);

            assertThat(errors).isEmpty();
        }

        @Test
        @DisplayName("nestjs with java fails")
        void validateLF_nestjsJava_error() {
            var config = TestConfigBuilder.builder()
                    .language("java", "21")
                    .framework("nestjs", "10")
                    .build();

            var errors = StackValidator
                    .validateLanguageFramework(config);

            assertThat(errors).isNotEmpty();
        }

        @Test
        @DisplayName("unknown framework passes")
        void validateLF_unknownFramework_noError() {
            var config = TestConfigBuilder.builder()
                    .framework("unknown-fw", "1.0")
                    .build();

            var errors = StackValidator
                    .validateLanguageFramework(config);

            assertThat(errors).isEmpty();
        }

        @Test
        @DisplayName("fastapi with python passes")
        void validateLF_fastapiPython_noError() {
            var config = TestConfigBuilder.builder()
                    .language("python", "3.12")
                    .framework("fastapi", "0.115")
                    .buildTool("pip")
                    .build();

            var errors = StackValidator
                    .validateLanguageFramework(config);

            assertThat(errors).isEmpty();
        }

        @Test
        @DisplayName("gin with java fails")
        void validateLF_ginJava_error() {
            var config = TestConfigBuilder.builder()
                    .language("java", "21")
                    .framework("gin", "1.10")
                    .build();

            var errors = StackValidator
                    .validateLanguageFramework(config);

            assertThat(errors).isNotEmpty();
        }
    }
}
