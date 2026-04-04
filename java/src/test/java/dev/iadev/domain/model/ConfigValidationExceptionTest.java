package dev.iadev.domain.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Tests for the domain-level {@link ConfigValidationException}.
 *
 * <p>Verifies all four constructors, immutability of
 * missingSections, toString output, and RuntimeException
 * inheritance.</p>
 */
@DisplayName("domain.model.ConfigValidationException")
class ConfigValidationExceptionTest {

    @Nested
    @DisplayName("missing field constructor (field, model)")
    class MissingFieldConstructor {

        @Test
        @DisplayName("includes field and model in message")
        void missingField_message_containsFieldAndModel() {
            var ex = new ConfigValidationException(
                    "name", "ProjectIdentity");

            assertThat(ex.getMessage())
                    .contains("name")
                    .contains("ProjectIdentity")
                    .contains("Missing required field");
            assertThat(ex.getMissingSections()).isEmpty();
        }
    }

    @Nested
    @DisplayName("invalid type constructor (field, type, model)")
    class InvalidTypeConstructor {

        @Test
        @DisplayName("includes field, type, and model in message")
        void invalidType_message_containsAllFields() {
            var ex = new ConfigValidationException(
                    "name", "String", "ProjectIdentity");

            assertThat(ex.getMessage())
                    .contains("name")
                    .contains("String")
                    .contains("ProjectIdentity")
                    .contains("Invalid type");
            assertThat(ex.getMissingSections()).isEmpty();
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
                    "Custom error", cause);

            assertThat(ex.getMessage())
                    .isEqualTo("Custom error");
            assertThat(ex.getCause()).isSameAs(cause);
            assertThat(ex.getMissingSections()).isEmpty();
        }
    }

    @Nested
    @DisplayName("missing sections constructor")
    class MissingSectionsConstructor {

        @Test
        @DisplayName("carries message and sections list")
        void constructor_carriesMessageAndSections() {
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
            var ex = new ConfigValidationException(
                    "Missing sections",
                    List.of("language", "framework"));

            assertThatThrownBy(
                    () -> ex.getMissingSections().add("another"))
                    .isInstanceOf(
                            UnsupportedOperationException.class);
        }

        @Test
        @DisplayName("defensive copy prevents external mutation")
        void missingSections_defensiveCopy() {
            var mutable = new ArrayList<>(
                    List.of("language", "framework"));
            var ex = new ConfigValidationException(
                    "Missing sections", mutable);

            mutable.add("database");

            assertThat(ex.getMissingSections())
                    .containsExactly("language", "framework");
        }
    }

    @Test
    @DisplayName("extends RuntimeException")
    void extendsRuntimeException() {
        var ex = new ConfigValidationException(
                "field", "Model");

        assertThat(ex)
                .isInstanceOf(RuntimeException.class);
    }

    @Test
    @DisplayName("toString with missing sections includes them")
    void toString_withSections_includesContext() {
        var ex = new ConfigValidationException(
                "Validation failed",
                List.of("language", "framework"));

        assertThat(ex.toString())
                .contains("ConfigValidationException")
                .contains("language")
                .contains("framework");
    }

    @Test
    @DisplayName("toString without missing sections uses short format")
    void toString_withoutSections_shortFormat() {
        var ex = new ConfigValidationException(
                "name", "ProjectIdentity");

        assertThat(ex.toString())
                .contains("ConfigValidationException")
                .contains("Missing required field")
                .doesNotContain("missingSections");
    }

    @Test
    @DisplayName("toString with empty sections uses short format")
    void toString_emptyList_shortFormat() {
        var ex = new ConfigValidationException(
                "Some error", List.of());

        assertThat(ex.toString())
                .contains("ConfigValidationException")
                .contains("Some error")
                .doesNotContain("missingSections");
    }
}
