package dev.iadev.domain.stack;

import dev.iadev.testutil.TestConfigBuilder;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for StackValidator — architecture style validation,
 * event store, schema registry, dead letter strategy,
 * extractMajor, and extractMinor.
 */
@DisplayName("StackValidator — edge cases")
class StackValidatorVersionEdgeCaseTest {

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
            var config = TestConfigBuilder.builder()
                    .architectureStyle(s).build();
            assertThat(StackValidator
                    .validateArchitectureStyle(config))
                    .isEmpty();
        }

        @Test
        void architectureStyle_invalid_error() {
            var config = TestConfigBuilder.builder()
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
            var config = TestConfigBuilder.builder()
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
            var config = TestConfigBuilder.builder()
                    .eventStore(es).build();
            assertThat(StackValidator
                    .validateEventStore(config))
                    .isEmpty();
        }

        @Test
        @DisplayName("invalid eventStore returns error")
        void validateEventStore_mongodb_error() {
            var config = TestConfigBuilder.builder()
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
            var config = TestConfigBuilder.builder()
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
            var config = TestConfigBuilder.builder()
                    .schemaRegistry(sr).build();
            assertThat(StackValidator
                    .validateSchemaRegistry(config))
                    .isEmpty();
        }

        @Test
        @DisplayName("invalid schemaRegistry returns error")
        void validateSchemaRegistry_invalid_error() {
            var config = TestConfigBuilder.builder()
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
            var config = TestConfigBuilder.builder()
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
            var config = TestConfigBuilder.builder()
                    .deadLetterStrategy(dls).build();
            assertThat(StackValidator
                    .validateDeadLetterStrategy(config))
                    .isEmpty();
        }

        @Test
        @DisplayName("invalid deadLetterStrategy errors")
        void validateDeadLetterStrategy_invalid_error() {
            var config = TestConfigBuilder.builder()
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