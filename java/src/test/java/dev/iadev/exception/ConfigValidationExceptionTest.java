package dev.iadev.exception;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("ConfigValidationException")
class ConfigValidationExceptionTest {

    @Nested
    @DisplayName("missing field constructor (field, model)")
    class MissingFieldConstructor {

        @Test
        @DisplayName("includes field and model in message")
        void missingField_messageContainsFieldAndModel() {
            var ex = new ConfigValidationException(
                    "name", "ProjectIdentity");

            assertThat(ex.getMessage())
                    .contains("name")
                    .contains("ProjectIdentity")
                    .contains("Missing required field");
        }
    }

    @Nested
    @DisplayName("invalid type constructor (field, type, model)")
    class InvalidTypeConstructor {

        @Test
        @DisplayName("includes field, type, and model in message")
        void invalidType_messageContainsFieldTypeAndModel() {
            var ex = new ConfigValidationException(
                    "name", "String", "ProjectIdentity");

            assertThat(ex.getMessage())
                    .contains("name")
                    .contains("String")
                    .contains("ProjectIdentity")
                    .contains("Invalid type");
        }
    }

    @Nested
    @DisplayName("cause constructor (message, cause)")
    class CauseConstructor {

        @Test
        @DisplayName("preserves message and cause")
        void causeConstructor_preservesMessageAndCause() {
            var cause = new RuntimeException("root cause");
            var ex = new ConfigValidationException(
                    "Custom error message", cause);

            assertThat(ex.getMessage())
                    .isEqualTo("Custom error message");
            assertThat(ex.getCause()).isSameAs(cause);
        }
    }

    @Nested
    @DisplayName("missing sections constructor (message, missingSections)")
    class MissingSectionsConstructor {

        @Test
        @DisplayName("carries message and missingSections list")
        void constructor_carriesMessageAndMissingSections() {
            var sections = List.of("language", "framework");
            var ex = new ConfigValidationException(
                    "Missing required sections", sections);

            assertThat(ex.getMessage())
                    .isEqualTo("Missing required sections");
            assertThat(ex.getMissingSections())
                    .containsExactly("language", "framework");
        }

        @Test
        @DisplayName("returned list is immutable")
        void missingSections_isImmutable() {
            var mutableList = new ArrayList<>(
                    List.of("language", "framework"));
            var ex = new ConfigValidationException(
                    "Missing sections", mutableList);

            assertThatThrownBy(
                    () -> ex.getMissingSections().add("another"))
                    .isInstanceOf(
                            UnsupportedOperationException.class);
        }

        @Test
        @DisplayName("mutations to original list do not affect exception")
        void missingSections_defensiveCopy() {
            var mutableList = new ArrayList<>(
                    List.of("language", "framework"));
            var ex = new ConfigValidationException(
                    "Missing sections", mutableList);

            mutableList.add("database");

            assertThat(ex.getMissingSections())
                    .containsExactly("language", "framework");
        }
    }

    @Test
    @DisplayName("extends RuntimeException")
    void extendsRuntimeException() {
        var ex = new ConfigValidationException("field", "Model");

        assertThat(ex).isInstanceOf(RuntimeException.class);
    }

    @Test
    @DisplayName("toString includes context information")
    void toString_includesContext() {
        var ex = new ConfigValidationException(
                "Validation failed",
                List.of("language", "framework"));

        assertThat(ex.toString())
                .contains("ConfigValidationException")
                .contains("language")
                .contains("framework");
    }

    @Test
    @DisplayName("toString without missingSections"
            + " includes message only")
    void toString_noMissingSections() {
        var ex = new ConfigValidationException(
                "name", "ProjectIdentity");

        assertThat(ex.toString())
                .contains("ConfigValidationException")
                .contains("Missing required field");
    }

    @Test
    @DisplayName("toString with empty missingSections"
            + " uses short format")
    void toString_emptyMissingSections() {
        var ex = new ConfigValidationException(
                "Some error", List.of());

        assertThat(ex.toString())
                .contains("ConfigValidationException")
                .contains("Some error")
                .doesNotContain("missingSections");
    }
}
