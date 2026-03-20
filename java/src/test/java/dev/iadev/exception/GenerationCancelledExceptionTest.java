package dev.iadev.exception;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("GenerationCancelledException")
class GenerationCancelledExceptionTest {

    @Test
    @DisplayName("constructor_withMessage_preservesMessage")
    void constructor_withMessage_preservesMessage() {
        var exception = new GenerationCancelledException(
                "Generation cancelled by user");

        assertThat(exception.getMessage())
                .isEqualTo("Generation cancelled by user");
    }

    @Test
    @DisplayName("exception_isRuntimeException")
    void exception_whenCalled_isRuntimeException() {
        var exception = new GenerationCancelledException("test");

        assertThat(exception)
                .isInstanceOf(RuntimeException.class);
    }

    @Test
    @DisplayName("exception_canBeThrown")
    void exception_whenCalled_canBeThrown() {
        assertThatThrownBy(() -> {
            throw new GenerationCancelledException("cancelled");
        })
                .isInstanceOf(GenerationCancelledException.class)
                .hasMessage("cancelled");
    }
}
