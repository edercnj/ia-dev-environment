package dev.iadev.domain.stack;

import dev.iadev.model.InterfaceConfig;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("StackValidator")
class StackValidatorTest {

    @Nested
    @DisplayName("validateStack()")
    class ValidateStackTests {

        @Test
        @DisplayName("valid java-quarkus config has no errors")
        void validateStack_validJavaQuarkus_noErrors() {
            var config = new TestProjectConfigBuilder()
                    .language("java", "21")
                    .framework("quarkus", "3.17")
                    .buildTool("maven")
                    .architectureStyle("microservice")
                    .build();

            var errors = StackValidator.validateStack(config);

            assertThat(errors).isEmpty();
        }

        @Test
        @DisplayName("multiple validation failures accumulate")
        void validateStack_multipleFailures_allReported() {
            var config = new TestProjectConfigBuilder()
                    .language("python", "3.8")
                    .framework("spring-boot", "3.4")
                    .buildTool("pip")
                    .architectureStyle("invalid-style")
                    .clearInterfaces()
                    .addInterface("invalid-type", "", "")
                    .build();

            var errors = StackValidator.validateStack(config);

            assertThat(errors).hasSizeGreaterThanOrEqualTo(3);
        }
    }

    @Nested
    @DisplayName("validateLanguageFramework()")
    class LanguageFrameworkTests {

        @Test
        @DisplayName("spring-boot with python fails")
        void validateLF_springBootPython_error() {
            var config = new TestProjectConfigBuilder()
                    .language("python", "3.12")
                    .framework("spring-boot", "3.4")
                    .build();

            var errors = StackValidator.validateLanguageFramework(config);

            assertThat(errors).isNotEmpty();
            assertThat(errors.get(0))
                    .contains("spring-boot")
                    .contains("python");
        }

        @Test
        @DisplayName("quarkus with java passes")
        void validateLF_quarkusJava_noError() {
            var config = new TestProjectConfigBuilder()
                    .language("java", "21")
                    .framework("quarkus", "3.17")
                    .build();

            var errors = StackValidator.validateLanguageFramework(config);

            assertThat(errors).isEmpty();
        }

        @Test
        @DisplayName("quarkus with kotlin passes")
        void validateLF_quarkusKotlin_noError() {
            var config = new TestProjectConfigBuilder()
                    .language("kotlin", "2.1")
                    .framework("quarkus", "3.17")
                    .buildTool("gradle")
                    .build();

            var errors = StackValidator.validateLanguageFramework(config);

            assertThat(errors).isEmpty();
        }

        @Test
        @DisplayName("nestjs with java fails")
        void validateLF_nestjsJava_error() {
            var config = new TestProjectConfigBuilder()
                    .language("java", "21")
                    .framework("nestjs", "10")
                    .build();

            var errors = StackValidator.validateLanguageFramework(config);

            assertThat(errors).isNotEmpty();
            assertThat(errors.get(0)).contains("nestjs");
        }

        @Test
        @DisplayName("unknown framework passes (no rules)")
        void validateLF_unknownFramework_noError() {
            var config = new TestProjectConfigBuilder()
                    .framework("unknown-fw", "1.0")
                    .build();

            var errors = StackValidator.validateLanguageFramework(config);

            assertThat(errors).isEmpty();
        }

        @Test
        @DisplayName("fastapi with python passes")
        void validateLF_fastapiPython_noError() {
            var config = new TestProjectConfigBuilder()
                    .language("python", "3.12")
                    .framework("fastapi", "0.115")
                    .buildTool("pip")
                    .build();

            var errors = StackValidator.validateLanguageFramework(config);

            assertThat(errors).isEmpty();
        }

        @Test
        @DisplayName("gin with java fails")
        void validateLF_ginJava_error() {
            var config = new TestProjectConfigBuilder()
                    .language("java", "21")
                    .framework("gin", "1.10")
                    .build();

            var errors = StackValidator.validateLanguageFramework(config);

            assertThat(errors).isNotEmpty();
        }
    }

    @Nested
    @DisplayName("checkJavaFrameworkVersion()")
    class JavaVersionTests {

        @Test
        @DisplayName("Quarkus 3 with Java 11 fails")
        void java17_quarkus3Java11_error() {
            var config = new TestProjectConfigBuilder()
                    .language("java", "11")
                    .framework("quarkus", "3.0")
                    .build();

            var errors = StackValidator.checkJavaFrameworkVersion(config);

            assertThat(errors).isNotEmpty();
            assertThat(errors.get(0))
                    .contains("Java 17+")
                    .contains("Java 11");
        }

        @Test
        @DisplayName("Quarkus 3 with Java 21 passes")
        void java17_quarkus3Java21_noError() {
            var config = new TestProjectConfigBuilder()
                    .language("java", "21")
                    .framework("quarkus", "3.17")
                    .build();

            var errors = StackValidator.checkJavaFrameworkVersion(config);

            assertThat(errors).isEmpty();
        }

        @Test
        @DisplayName("Spring Boot 3 with Java 11 fails")
        void java17_springBoot3Java11_error() {
            var config = new TestProjectConfigBuilder()
                    .language("java", "11")
                    .framework("spring-boot", "3.4")
                    .build();

            var errors = StackValidator.checkJavaFrameworkVersion(config);

            assertThat(errors).isNotEmpty();
            assertThat(errors.get(0))
                    .contains("Spring-boot")
                    .contains("Java 17+");
        }

        @Test
        @DisplayName("Quarkus 2 with Java 11 passes")
        void java17_quarkus2Java11_noError() {
            var config = new TestProjectConfigBuilder()
                    .language("java", "11")
                    .framework("quarkus", "2.16")
                    .build();

            var errors = StackValidator.checkJavaFrameworkVersion(config);

            assertThat(errors).isEmpty();
        }

        @Test
        @DisplayName("non-java language skips check")
        void java17_python_skipped() {
            var config = new TestProjectConfigBuilder()
                    .language("python", "3.12")
                    .framework("fastapi", "0.115")
                    .buildTool("pip")
                    .build();

            var errors = StackValidator.checkJavaFrameworkVersion(config);

            assertThat(errors).isEmpty();
        }

        @Test
        @DisplayName("Java with unparseable version skips check")
        void java17_unparseableVersion_skipped() {
            var config = new TestProjectConfigBuilder()
                    .language("java", "latest")
                    .framework("quarkus", "3.0")
                    .build();

            var errors = StackValidator.checkJavaFrameworkVersion(config);

            assertThat(errors).isEmpty();
        }

        @Test
        @DisplayName("Java with non-quarkus non-springboot framework skips")
        void java17_javaWithKtor_skipped() {
            var config = new TestProjectConfigBuilder()
                    .language("java", "11")
                    .framework("ktor", "3.0")
                    .buildTool("gradle")
                    .build();

            var errors = StackValidator.checkJavaFrameworkVersion(config);

            assertThat(errors).isEmpty();
        }

        @Test
        @DisplayName("Quarkus with unparseable fw version skips check")
        void java17_unparseableFwVersion_noError() {
            var config = new TestProjectConfigBuilder()
                    .language("java", "11")
                    .framework("quarkus", "latest")
                    .build();

            var errors = StackValidator.checkJavaFrameworkVersion(config);

            assertThat(errors).isEmpty();
        }

        @Test
        @DisplayName("Spring Boot 3 with Java 17 passes")
        void java17_springBoot3Java17_noError() {
            var config = new TestProjectConfigBuilder()
                    .language("java", "17")
                    .framework("spring-boot", "3.4")
                    .build();

            var errors = StackValidator.checkJavaFrameworkVersion(config);

            assertThat(errors).isEmpty();
        }
    }

    @Nested
    @DisplayName("checkDjangoPythonVersion()")
    class DjangoPythonVersionTests {

        @Test
        @DisplayName("Django 5 with Python 3.8 fails")
        void django5_python38_error() {
            var config = new TestProjectConfigBuilder()
                    .language("python", "3.8")
                    .framework("django", "5.1")
                    .buildTool("pip")
                    .build();

            var errors = StackValidator.checkDjangoPythonVersion(config);

            assertThat(errors).isNotEmpty();
            assertThat(errors.get(0))
                    .contains("Django 5.x")
                    .contains("Python 3.10+")
                    .contains("Python 3.8");
        }

        @Test
        @DisplayName("Django 5 with Python 3.10 passes")
        void django5_python310_noError() {
            var config = new TestProjectConfigBuilder()
                    .language("python", "3.10")
                    .framework("django", "5.1")
                    .buildTool("pip")
                    .build();

            var errors = StackValidator.checkDjangoPythonVersion(config);

            assertThat(errors).isEmpty();
        }

        @Test
        @DisplayName("Django 4 skips version check")
        void django4_python38_noError() {
            var config = new TestProjectConfigBuilder()
                    .language("python", "3.8")
                    .framework("django", "4.2")
                    .buildTool("pip")
                    .build();

            var errors = StackValidator.checkDjangoPythonVersion(config);

            assertThat(errors).isEmpty();
        }

        @Test
        @DisplayName("non-django framework skips check")
        void nonDjango_skipped() {
            var config = new TestProjectConfigBuilder()
                    .language("python", "3.8")
                    .framework("fastapi", "0.115")
                    .buildTool("pip")
                    .build();

            var errors = StackValidator.checkDjangoPythonVersion(config);

            assertThat(errors).isEmpty();
        }

        @Test
        @DisplayName("Django 5 with Python 2.7 fails (pyMajor < 3)")
        void django5_python27_error() {
            var config = new TestProjectConfigBuilder()
                    .language("python", "2.7")
                    .framework("django", "5.1")
                    .buildTool("pip")
                    .build();

            var errors = StackValidator.checkDjangoPythonVersion(config);

            assertThat(errors).isNotEmpty();
            assertThat(errors.get(0)).contains("Django 5.x");
        }

        @Test
        @DisplayName("Django 5 with Python 3.12 passes")
        void django5_python312_noError() {
            var config = new TestProjectConfigBuilder()
                    .language("python", "3.12")
                    .framework("django", "5.1")
                    .buildTool("pip")
                    .build();

            var errors = StackValidator.checkDjangoPythonVersion(config);

            assertThat(errors).isEmpty();
        }

        @Test
        @DisplayName("Django with unparseable fw version skips check")
        void django_unparseableFwVersion_noError() {
            var config = new TestProjectConfigBuilder()
                    .language("python", "3.8")
                    .framework("django", "latest")
                    .buildTool("pip")
                    .build();

            var errors = StackValidator.checkDjangoPythonVersion(config);

            assertThat(errors).isEmpty();
        }

        @Test
        @DisplayName("Django 5 with unparseable Python version skips")
        void django5_unparseablePyVersion_noError() {
            var config = new TestProjectConfigBuilder()
                    .language("python", "latest")
                    .framework("django", "5.1")
                    .buildTool("pip")
                    .build();

            var errors = StackValidator.checkDjangoPythonVersion(config);

            assertThat(errors).isEmpty();
        }

        @Test
        @DisplayName("Django 5 with Python major-only version skips")
        void django5_majorOnlyPyVersion_noError() {
            var config = new TestProjectConfigBuilder()
                    .language("python", "3")
                    .framework("django", "5.1")
                    .buildTool("pip")
                    .build();

            var errors = StackValidator.checkDjangoPythonVersion(config);

            assertThat(errors).isEmpty();
        }

        @Test
        @DisplayName("Django 5 with Python 4.0 passes (pyMajor > 3)")
        void django5_python40_noError() {
            var config = new TestProjectConfigBuilder()
                    .language("python", "4.0")
                    .framework("django", "5.1")
                    .buildTool("pip")
                    .build();

            var errors = StackValidator.checkDjangoPythonVersion(config);

            assertThat(errors).isEmpty();
        }
    }

    @Nested
    @DisplayName("validateNativeBuild()")
    class NativeBuildTests {

        @Test
        @DisplayName("unsupported framework with nativeBuild fails")
        void nativeBuild_nestjs_error() {
            var config = new TestProjectConfigBuilder()
                    .language("typescript", "5")
                    .framework("nestjs", "10")
                    .buildTool("npm")
                    .nativeBuild(true)
                    .build();

            var errors = StackValidator.validateNativeBuild(config);

            assertThat(errors).isNotEmpty();
            assertThat(errors.get(0)).contains("nestjs");
        }

        @Test
        @DisplayName("supported framework with nativeBuild passes")
        void nativeBuild_quarkus_noError() {
            var config = new TestProjectConfigBuilder()
                    .nativeBuild(true)
                    .framework("quarkus", "3.17")
                    .build();

            var errors = StackValidator.validateNativeBuild(config);

            assertThat(errors).isEmpty();
        }

        @Test
        @DisplayName("nativeBuild disabled passes for any framework")
        void nativeBuild_disabled_noError() {
            var config = new TestProjectConfigBuilder()
                    .nativeBuild(false)
                    .build();

            var errors = StackValidator.validateNativeBuild(config);

            assertThat(errors).isEmpty();
        }
    }

    @Nested
    @DisplayName("validateInterfaceTypes()")
    class InterfaceTypesTests {

        @Test
        @DisplayName("valid interface type passes")
        void interfaceTypes_rest_noError() {
            var config = new TestProjectConfigBuilder()
                    .clearInterfaces()
                    .addInterface("rest", "", "")
                    .build();

            var errors = StackValidator.validateInterfaceTypes(config);

            assertThat(errors).isEmpty();
        }

        @Test
        @DisplayName("invalid interface type fails")
        void interfaceTypes_invalid_error() {
            var config = new TestProjectConfigBuilder()
                    .clearInterfaces()
                    .addInterface("soap", "", "")
                    .build();

            var errors = StackValidator.validateInterfaceTypes(config);

            assertThat(errors).isNotEmpty();
            assertThat(errors.get(0))
                    .contains("soap")
                    .contains("Valid:");
        }

        @Test
        @DisplayName("multiple invalid types produce multiple errors")
        void interfaceTypes_multipleInvalid_multipleErrors() {
            var config = new TestProjectConfigBuilder()
                    .clearInterfaces()
                    .addInterface("soap", "", "")
                    .addInterface("rmi", "", "")
                    .build();

            var errors = StackValidator.validateInterfaceTypes(config);

            assertThat(errors).hasSize(2);
        }

        @ParameterizedTest
        @ValueSource(strings = {
                "rest", "grpc", "graphql", "websocket", "tcp-custom",
                "cli", "event-consumer", "event-producer", "scheduled"
        })
        @DisplayName("valid type: {0}")
        void interfaceTypes_allValid_noError(String type) {
            var config = new TestProjectConfigBuilder()
                    .clearInterfaces()
                    .addInterface(type, "", "")
                    .build();

            var errors = StackValidator.validateInterfaceTypes(config);

            assertThat(errors).isEmpty();
        }
    }

    @Nested
    @DisplayName("validateArchitectureStyle()")
    class ArchitectureStyleTests {

        @ParameterizedTest
        @ValueSource(strings = {
                "microservice", "modular-monolith", "monolith",
                "library", "serverless"
        })
        @DisplayName("valid style: {0}")
        void architectureStyle_valid_noError(String style) {
            var config = new TestProjectConfigBuilder()
                    .architectureStyle(style)
                    .build();

            var errors = StackValidator.validateArchitectureStyle(config);

            assertThat(errors).isEmpty();
        }

        @Test
        @DisplayName("invalid style fails with valid list")
        void architectureStyle_serverlessCustom_error() {
            var config = new TestProjectConfigBuilder()
                    .architectureStyle("hexagonal")
                    .build();

            var errors = StackValidator.validateArchitectureStyle(config);

            assertThat(errors).isNotEmpty();
            assertThat(errors.get(0))
                    .contains("hexagonal")
                    .contains("Valid:")
                    .contains("microservice")
                    .contains("monolith")
                    .contains("library");
        }
    }

    @Nested
    @DisplayName("extractMajor()")
    class ExtractMajorTests {

        @Test
        @DisplayName("extracts major from simple version")
        void extractMajor_simple_correct() {
            assertThat(StackValidator.extractMajor("21"))
                    .contains(21);
        }

        @Test
        @DisplayName("extracts major from dotted version")
        void extractMajor_dotted_correct() {
            assertThat(StackValidator.extractMajor("3.17.1"))
                    .contains(3);
        }

        @Test
        @DisplayName("returns empty for unparseable version")
        void extractMajor_unparseable_empty() {
            assertThat(StackValidator.extractMajor("latest"))
                    .isEmpty();
        }

        @Test
        @DisplayName("returns empty for null")
        void extractMajor_null_empty() {
            assertThat(StackValidator.extractMajor(null))
                    .isEmpty();
        }

        @Test
        @DisplayName("returns empty for empty string")
        void extractMajor_emptyString_empty() {
            assertThat(StackValidator.extractMajor(""))
                    .isEmpty();
        }
    }

    @Nested
    @DisplayName("extractMinor()")
    class ExtractMinorTests {

        @Test
        @DisplayName("extracts minor from dotted version")
        void extractMinor_dotted_correct() {
            assertThat(StackValidator.extractMinor("3.10.1"))
                    .contains(10);
        }

        @Test
        @DisplayName("returns empty for single-part version")
        void extractMinor_noDot_empty() {
            assertThat(StackValidator.extractMinor("21"))
                    .isEmpty();
        }

        @Test
        @DisplayName("returns empty for null")
        void extractMinor_null_empty() {
            assertThat(StackValidator.extractMinor(null))
                    .isEmpty();
        }

        @Test
        @DisplayName("returns empty for empty string")
        void extractMinor_emptyString_empty() {
            assertThat(StackValidator.extractMinor(""))
                    .isEmpty();
        }

        @Test
        @DisplayName("returns empty for unparseable minor")
        void extractMinor_unparseable_empty() {
            assertThat(StackValidator.extractMinor("3.abc"))
                    .isEmpty();
        }
    }
}
