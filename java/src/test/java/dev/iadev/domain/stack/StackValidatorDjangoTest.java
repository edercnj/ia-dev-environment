package dev.iadev.domain.stack;

import dev.iadev.testutil.TestConfigBuilder;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for StackValidator — Django/Python version checks,
 * native build, and interface type validation.
 */
@DisplayName("StackValidator — Django + native + interfaces")
class StackValidatorDjangoTest {

    @Nested
    @DisplayName("checkDjangoPythonVersion()")
    class DjangoPythonVersionTests {

        @Test
        void django5_python38_error() {
            var config = TestConfigBuilder.builder()
                    .language("python", "3.8")
                    .framework("django", "5.1")
                    .buildTool("pip").build();
            var errors = StackValidator
                    .checkDjangoPythonVersion(config);
            assertThat(errors).isNotEmpty();
            assertThat(errors.get(0))
                    .contains("Django 5.x");
        }

        @Test
        void django5_python310_noError() {
            var config = TestConfigBuilder.builder()
                    .language("python", "3.10")
                    .framework("django", "5.1")
                    .buildTool("pip").build();
            assertThat(StackValidator
                    .checkDjangoPythonVersion(config))
                    .isEmpty();
        }

        @Test
        void django4_python38_noError() {
            var config = TestConfigBuilder.builder()
                    .language("python", "3.8")
                    .framework("django", "4.2")
                    .buildTool("pip").build();
            assertThat(StackValidator
                    .checkDjangoPythonVersion(config))
                    .isEmpty();
        }

        @Test
        void nonDjango_skipped() {
            var config = TestConfigBuilder.builder()
                    .language("python", "3.8")
                    .framework("fastapi", "0.115")
                    .buildTool("pip").build();
            assertThat(StackValidator
                    .checkDjangoPythonVersion(config))
                    .isEmpty();
        }

        @Test
        void django5_python27_error() {
            var config = TestConfigBuilder.builder()
                    .language("python", "2.7")
                    .framework("django", "5.1")
                    .buildTool("pip").build();
            assertThat(StackValidator
                    .checkDjangoPythonVersion(config))
                    .isNotEmpty();
        }

        @Test
        void django5_python312_noError() {
            var config = TestConfigBuilder.builder()
                    .language("python", "3.12")
                    .framework("django", "5.1")
                    .buildTool("pip").build();
            assertThat(StackValidator
                    .checkDjangoPythonVersion(config))
                    .isEmpty();
        }

        @Test
        void django_unparseableFwVersion_noError() {
            var config = TestConfigBuilder.builder()
                    .language("python", "3.8")
                    .framework("django", "latest")
                    .buildTool("pip").build();
            assertThat(StackValidator
                    .checkDjangoPythonVersion(config))
                    .isEmpty();
        }

        @Test
        void django5_unparseablePyVersion_noError() {
            var config = TestConfigBuilder.builder()
                    .language("python", "latest")
                    .framework("django", "5.1")
                    .buildTool("pip").build();
            assertThat(StackValidator
                    .checkDjangoPythonVersion(config))
                    .isEmpty();
        }

        @Test
        void django5_majorOnlyPyVersion_noError() {
            var config = TestConfigBuilder.builder()
                    .language("python", "3")
                    .framework("django", "5.1")
                    .buildTool("pip").build();
            assertThat(StackValidator
                    .checkDjangoPythonVersion(config))
                    .isEmpty();
        }

        @Test
        void django5_python40_noError() {
            var config = TestConfigBuilder.builder()
                    .language("python", "4.0")
                    .framework("django", "5.1")
                    .buildTool("pip").build();
            assertThat(StackValidator
                    .checkDjangoPythonVersion(config))
                    .isEmpty();
        }
    }

    @Nested
    @DisplayName("validateNativeBuild()")
    class NativeBuildTests {

        @Test
        void nativeBuild_nestjs_error() {
            var config = TestConfigBuilder.builder()
                    .language("typescript", "5")
                    .framework("nestjs", "10")
                    .buildTool("npm")
                    .nativeBuild(true).build();
            assertThat(StackValidator
                    .validateNativeBuild(config))
                    .isNotEmpty();
        }

        @Test
        void nativeBuild_quarkus_noError() {
            var config = TestConfigBuilder.builder()
                    .nativeBuild(true)
                    .framework("quarkus", "3.17").build();
            assertThat(StackValidator
                    .validateNativeBuild(config))
                    .isEmpty();
        }

        @Test
        void nativeBuild_disabled_noError() {
            var config = TestConfigBuilder.builder()
                    .nativeBuild(false).build();
            assertThat(StackValidator
                    .validateNativeBuild(config))
                    .isEmpty();
        }
    }

    @Nested
    @DisplayName("validateInterfaceTypes()")
    class InterfaceTypesTests {

        @Test
        void interfaceTypes_rest_noError() {
            var config = TestConfigBuilder.builder()
                    .clearInterfaces()
                    .addInterface("rest", "", "").build();
            assertThat(StackValidator
                    .validateInterfaceTypes(config))
                    .isEmpty();
        }

        @Test
        void interfaceTypes_invalid_error() {
            var config = TestConfigBuilder.builder()
                    .clearInterfaces()
                    .addInterface("soap", "", "").build();
            assertThat(StackValidator
                    .validateInterfaceTypes(config))
                    .isNotEmpty();
        }

        @Test
        void interfaceTypes_multipleInvalid() {
            var config = TestConfigBuilder.builder()
                    .clearInterfaces()
                    .addInterface("soap", "", "")
                    .addInterface("rmi", "", "").build();
            assertThat(StackValidator
                    .validateInterfaceTypes(config))
                    .hasSize(2);
        }

        @ParameterizedTest
        @ValueSource(strings = {
                "rest", "grpc", "graphql", "websocket",
                "tcp-custom", "cli", "event-consumer",
                "event-producer", "scheduled"
        })
        void interfaceTypes_allValid_noError(String type) {
            var config = TestConfigBuilder.builder()
                    .clearInterfaces()
                    .addInterface(type, "", "").build();
            assertThat(StackValidator
                    .validateInterfaceTypes(config))
                    .isEmpty();
        }
    }
}
