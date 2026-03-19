package dev.iadev.exception;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("ConfigValidationException")
class ConfigValidationExceptionTest {

    @Test
    @DisplayName("missing field constructor includes field and model")
    void missingField_messageContainsFieldAndModel() {
        var ex = new ConfigValidationException("name", "ProjectIdentity");

        assertThat(ex.getMessage())
                .contains("name")
                .contains("ProjectIdentity")
                .contains("Missing required field");
    }

    @Test
    @DisplayName("invalid type constructor includes field, type, and model")
    void invalidType_messageContainsFieldTypeAndModel() {
        var ex = new ConfigValidationException(
                "name", "String", "ProjectIdentity");

        assertThat(ex.getMessage())
                .contains("name")
                .contains("String")
                .contains("ProjectIdentity")
                .contains("Invalid type");
    }

    @Test
    @DisplayName("cause constructor preserves message and cause")
    void causeConstructor_preservesMessageAndCause() {
        var cause = new RuntimeException("root cause");
        var ex = new ConfigValidationException(
                "Custom error message", cause);

        assertThat(ex.getMessage())
                .isEqualTo("Custom error message");
        assertThat(ex.getCause()).isSameAs(cause);
    }

    @Test
    @DisplayName("extends RuntimeException")
    void extendsRuntimeException() {
        var ex = new ConfigValidationException("field", "Model");

        assertThat(ex).isInstanceOf(RuntimeException.class);
    }
}
