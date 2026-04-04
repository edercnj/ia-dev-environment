package dev.iadev.domain.stack;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import static org.assertj.core.api.Assertions.assertThat;
/**
 * Tests for StackValidator — Java version checks,
 * Django version checks, native build, interface types,
 * architecture style, extractMajor, extractMinor.
 */
@DisplayName("StackValidator — version + validation")
class StackValidatorVersionTest {

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
            var errors = StackValidator
                    .checkJavaFrameworkVersion(config);
            assertThat(errors).isNotEmpty();
            assertThat(errors.get(0))
                    .contains("Java 17+");
        }

        @Test
        @DisplayName("Quarkus 3 with Java 21 passes")
        void java17_quarkus3Java21_noError() {
            var config = new TestProjectConfigBuilder()
                    .language("java", "21")
                    .framework("quarkus", "3.17")
                    .build();
            assertThat(StackValidator
                    .checkJavaFrameworkVersion(config))
                    .isEmpty();
        }

        @Test
        @DisplayName("Spring Boot 3 with Java 11 fails")
        void java17_springBoot3Java11_error() {
            var config = new TestProjectConfigBuilder()
                    .language("java", "11")
                    .framework("spring-boot", "3.4")
                    .build();
            assertThat(StackValidator
                    .checkJavaFrameworkVersion(config))
                    .isNotEmpty();
        }

        @Test
        @DisplayName("Quarkus 2 with Java 11 passes")
        void java17_quarkus2Java11_noError() {
            var config = new TestProjectConfigBuilder()
                    .language("java", "11")
                    .framework("quarkus", "2.16")
                    .build();
            assertThat(StackValidator
                    .checkJavaFrameworkVersion(config))
                    .isEmpty();
        }

        @Test
        @DisplayName("non-java skips check")
        void java17_python_skipped() {
            var config = new TestProjectConfigBuilder()
                    .language("python", "3.12")
                    .framework("fastapi", "0.115")
                    .buildTool("pip")
                    .build();
            assertThat(StackValidator
                    .checkJavaFrameworkVersion(config))
                    .isEmpty();
        }

        @Test
        @DisplayName("unparseable version skips")
        void java17_unparseableVersion_skipped() {
            var config = new TestProjectConfigBuilder()
                    .language("java", "latest")
                    .framework("quarkus", "3.0")
                    .build();
            assertThat(StackValidator
                    .checkJavaFrameworkVersion(config))
                    .isEmpty();
        }

        @Test
        @DisplayName("non-quarkus non-spring skips")
        void java17_javaWithKtor_skipped() {
            var config = new TestProjectConfigBuilder()
                    .language("java", "11")
                    .framework("ktor", "3.0")
                    .buildTool("gradle")
                    .build();
            assertThat(StackValidator
                    .checkJavaFrameworkVersion(config))
                    .isEmpty();
        }

        @Test
        @DisplayName("unparseable fw version skips")
        void java17_unparseableFwVersion_noError() {
            var config = new TestProjectConfigBuilder()
                    .language("java", "11")
                    .framework("quarkus", "latest")
                    .build();
            assertThat(StackValidator
                    .checkJavaFrameworkVersion(config))
                    .isEmpty();
        }

        @Test
        @DisplayName("Spring Boot 3 with Java 17 passes")
        void java17_springBoot3Java17_noError() {
            var config = new TestProjectConfigBuilder()
                    .language("java", "17")
                    .framework("spring-boot", "3.4")
                    .build();
            assertThat(StackValidator
                    .checkJavaFrameworkVersion(config))
                    .isEmpty();
        }
    }

    @Nested
    @DisplayName("checkDjangoPythonVersion()")
    class DjangoPythonVersionTests {

        @Test
        void django5_python38_error() {
            var config = new TestProjectConfigBuilder()
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
            var config = new TestProjectConfigBuilder()
                    .language("python", "3.10")
                    .framework("django", "5.1")
                    .buildTool("pip").build();
            assertThat(StackValidator
                    .checkDjangoPythonVersion(config))
                    .isEmpty();
        }

        @Test
        void django4_python38_noError() {
            var config = new TestProjectConfigBuilder()
                    .language("python", "3.8")
                    .framework("django", "4.2")
                    .buildTool("pip").build();
            assertThat(StackValidator
                    .checkDjangoPythonVersion(config))
                    .isEmpty();
        }

        @Test
        void nonDjango_skipped() {
            var config = new TestProjectConfigBuilder()
                    .language("python", "3.8")
                    .framework("fastapi", "0.115")
                    .buildTool("pip").build();
            assertThat(StackValidator
                    .checkDjangoPythonVersion(config))
                    .isEmpty();
        }

        @Test
        void django5_python27_error() {
            var config = new TestProjectConfigBuilder()
                    .language("python", "2.7")
                    .framework("django", "5.1")
                    .buildTool("pip").build();
            assertThat(StackValidator
                    .checkDjangoPythonVersion(config))
                    .isNotEmpty();
        }

        @Test
        void django5_python312_noError() {
            var config = new TestProjectConfigBuilder()
                    .language("python", "3.12")
                    .framework("django", "5.1")
                    .buildTool("pip").build();
            assertThat(StackValidator
                    .checkDjangoPythonVersion(config))
                    .isEmpty();
        }

        @Test
        void django_unparseableFwVersion_noError() {
            var config = new TestProjectConfigBuilder()
                    .language("python", "3.8")
                    .framework("django", "latest")
                    .buildTool("pip").build();
            assertThat(StackValidator
                    .checkDjangoPythonVersion(config))
                    .isEmpty();
        }

        @Test
        void django5_unparseablePyVersion_noError() {
            var config = new TestProjectConfigBuilder()
                    .language("python", "latest")
                    .framework("django", "5.1")
                    .buildTool("pip").build();
            assertThat(StackValidator
                    .checkDjangoPythonVersion(config))
                    .isEmpty();
        }

        @Test
        void django5_majorOnlyPyVersion_noError() {
            var config = new TestProjectConfigBuilder()
                    .language("python", "3")
                    .framework("django", "5.1")
                    .buildTool("pip").build();
            assertThat(StackValidator
                    .checkDjangoPythonVersion(config))
                    .isEmpty();
        }

        @Test
        void django5_python40_noError() {
            var config = new TestProjectConfigBuilder()
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
            var config = new TestProjectConfigBuilder()
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
            var config = new TestProjectConfigBuilder()
                    .nativeBuild(true)
                    .framework("quarkus", "3.17").build();
            assertThat(StackValidator
                    .validateNativeBuild(config))
                    .isEmpty();
        }

        @Test
        void nativeBuild_disabled_noError() {
            var config = new TestProjectConfigBuilder()
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
            var config = new TestProjectConfigBuilder()
                    .clearInterfaces()
                    .addInterface("rest", "", "").build();
            assertThat(StackValidator
                    .validateInterfaceTypes(config))
                    .isEmpty();
        }

        @Test
        void interfaceTypes_invalid_error() {
            var config = new TestProjectConfigBuilder()
                    .clearInterfaces()
                    .addInterface("soap", "", "").build();
            assertThat(StackValidator
                    .validateInterfaceTypes(config))
                    .isNotEmpty();
        }

        @Test
        void interfaceTypes_multipleInvalid() {
            var config = new TestProjectConfigBuilder()
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
            var config = new TestProjectConfigBuilder()
                    .clearInterfaces()
                    .addInterface(type, "", "").build();
            assertThat(StackValidator
                    .validateInterfaceTypes(config))
                    .isEmpty();
        }
    }

    @Nested
    @DisplayName("validateArchitectureStyle()")
    class ArchitectureStyleTests {

        @ParameterizedTest
        @ValueSource(strings = {
                "microservice", "modular-monolith",
                "monolith", "library", "serverless",
                "hexagonal", "cqrs", "event-driven",
                "clean"
        })
        void architectureStyle_valid_noError(String s) {
            var config = new TestProjectConfigBuilder()
                    .architectureStyle(s).build();
            assertThat(StackValidator
                    .validateArchitectureStyle(config))
                    .isEmpty();
        }

        @Test
        void architectureStyle_invalid_error() {
            var config = new TestProjectConfigBuilder()
                    .architectureStyle("microkernel")
                    .build();
            assertThat(StackValidator
                    .validateArchitectureStyle(config))
                    .isNotEmpty();
        }
    }

    @Nested
    @DisplayName("validateEventStore()")
    class EventStoreValidationTests {

        @Test
        @DisplayName("empty eventStore is valid")
        void validateEventStore_empty_noError() {
            var config = new TestProjectConfigBuilder()
                    .build();
            assertThat(StackValidator
                    .validateEventStore(config))
                    .isEmpty();
        }

        @ParameterizedTest
        @ValueSource(strings = {
                "eventstoredb", "axon", "custom"})
        @DisplayName("valid eventStore values pass")
        void validateEventStore_valid_noError(String es) {
            var config = new TestProjectConfigBuilder()
                    .eventStore(es).build();
            assertThat(StackValidator
                    .validateEventStore(config))
                    .isEmpty();
        }

        @Test
        @DisplayName("invalid eventStore returns error")
        void validateEventStore_mongodb_error() {
            var config = new TestProjectConfigBuilder()
                    .eventStore("mongodb").build();
            var errors = StackValidator
                    .validateEventStore(config);
            assertThat(errors).hasSize(1);
            assertThat(errors.get(0))
                    .contains("mongodb")
                    .contains("eventstoredb");
        }
    }

    @Nested
    @DisplayName("validateSchemaRegistry()")
    class SchemaRegistryValidationTests {

        @Test
        @DisplayName("empty schemaRegistry is valid")
        void validateSchemaRegistry_empty_noError() {
            var config = new TestProjectConfigBuilder()
                    .build();
            assertThat(StackValidator
                    .validateSchemaRegistry(config))
                    .isEmpty();
        }

        @ParameterizedTest
        @ValueSource(strings = {
                "confluent", "apicurio", "glue"})
        @DisplayName("valid schemaRegistry values pass")
        void validateSchemaRegistry_valid_noError(
                String sr) {
            var config = new TestProjectConfigBuilder()
                    .schemaRegistry(sr).build();
            assertThat(StackValidator
                    .validateSchemaRegistry(config))
                    .isEmpty();
        }

        @Test
        @DisplayName("invalid schemaRegistry returns error")
        void validateSchemaRegistry_invalid_error() {
            var config = new TestProjectConfigBuilder()
                    .schemaRegistry("avro-only").build();
            var errors = StackValidator
                    .validateSchemaRegistry(config);
            assertThat(errors).hasSize(1);
            assertThat(errors.get(0))
                    .contains("avro-only")
                    .contains("confluent");
        }
    }

    @Nested
    @DisplayName("validateDeadLetterStrategy()")
    class DeadLetterStrategyValidationTests {

        @Test
        @DisplayName("empty deadLetterStrategy is valid")
        void validateDeadLetterStrategy_empty_noError() {
            var config = new TestProjectConfigBuilder()
                    .build();
            assertThat(StackValidator
                    .validateDeadLetterStrategy(config))
                    .isEmpty();
        }

        @ParameterizedTest
        @ValueSource(strings = {
                "kafka-dlq", "sqs-dlq", "database"})
        @DisplayName("valid deadLetterStrategy values pass")
        void validateDeadLetterStrategy_valid_noError(
                String dls) {
            var config = new TestProjectConfigBuilder()
                    .deadLetterStrategy(dls).build();
            assertThat(StackValidator
                    .validateDeadLetterStrategy(config))
                    .isEmpty();
        }

        @Test
        @DisplayName("invalid deadLetterStrategy errors")
        void validateDeadLetterStrategy_invalid_error() {
            var config = new TestProjectConfigBuilder()
                    .deadLetterStrategy("rabbitmq")
                    .build();
            var errors = StackValidator
                    .validateDeadLetterStrategy(config);
            assertThat(errors).hasSize(1);
            assertThat(errors.get(0))
                    .contains("rabbitmq")
                    .contains("kafka-dlq");
        }
    }

    @Nested
    @DisplayName("extractMajor()")
    class ExtractMajorTests {

        @Test
        void extractMajor_simple() {
            assertThat(
                    StackValidator.extractMajor("21"))
                    .contains(21);
        }

        @Test
        void extractMajor_dotted() {
            assertThat(
                    StackValidator.extractMajor("3.17.1"))
                    .contains(3);
        }

        @Test
        void extractMajor_unparseable() {
            assertThat(
                    StackValidator.extractMajor("latest"))
                    .isEmpty();
        }

        @Test
        void extractMajor_null() {
            assertThat(
                    StackValidator.extractMajor(null))
                    .isEmpty();
        }

        @Test
        void extractMajor_emptyString() {
            assertThat(
                    StackValidator.extractMajor(""))
                    .isEmpty();
        }
    }

    @Nested
    @DisplayName("extractMinor()")
    class ExtractMinorTests {

        @Test
        void extractMinor_dotted() {
            assertThat(
                    StackValidator.extractMinor("3.10.1"))
                    .contains(10);
        }

        @Test
        void extractMinor_noDot() {
            assertThat(
                    StackValidator.extractMinor("21"))
                    .isEmpty();
        }

        @Test
        void extractMinor_null() {
            assertThat(
                    StackValidator.extractMinor(null))
                    .isEmpty();
        }

        @Test
        void extractMinor_emptyString() {
            assertThat(
                    StackValidator.extractMinor(""))
                    .isEmpty();
        }

        @Test
        void extractMinor_unparseable() {
            assertThat(
                    StackValidator.extractMinor("3.abc"))
                    .isEmpty();
        }
    }
}
