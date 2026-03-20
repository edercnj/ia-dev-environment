package dev.iadev.exception;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("CheckpointValidationException")
class CheckpointValidationExceptionTest {

    @Test
    @DisplayName("carries message and field")
    void constructor_whenCalled_carriesMessageAndField() {
        var ex = new CheckpointValidationException(
                "Invalid checkpoint state", "status");

        assertThat(ex.getMessage())
                .isEqualTo("Invalid checkpoint state");
        assertThat(ex.getField()).isEqualTo("status");
    }

    @Test
    @DisplayName("extends RuntimeException")
    void create_whenCalled_extendsRuntimeException() {
        var ex = new CheckpointValidationException(
                "error", "field");

        assertThat(ex).isInstanceOf(RuntimeException.class);
    }

    @Test
    @DisplayName("toString includes field name")
    void toString_whenCalled_includesField() {
        var ex = new CheckpointValidationException(
                "Missing required field", "storyId");

        assertThat(ex.toString())
                .contains("CheckpointValidationException")
                .contains("storyId");
    }
}
