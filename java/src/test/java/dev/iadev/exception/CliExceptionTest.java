package dev.iadev.exception;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

@DisplayName("CliException")
class CliExceptionTest {

    @Test
    @DisplayName("carries message and errorCode")
    void constructor_carriesMessageAndErrorCode() {
        var ex = new CliException("Invalid argument", 1);

        assertThat(ex.getMessage()).isEqualTo("Invalid argument");
        assertThat(ex.getErrorCode()).isEqualTo(1);
    }

    @Test
    @DisplayName("extends RuntimeException")
    void extendsRuntimeException() {
        var ex = new CliException("error", 1);

        assertThat(ex).isInstanceOf(RuntimeException.class);
    }

    @Test
    @DisplayName("errorCode 1 for usage errors")
    void errorCode1_usageError() {
        var ex = new CliException("Missing required flag", 1);

        assertThat(ex.getErrorCode()).isEqualTo(1);
    }

    @Test
    @DisplayName("errorCode 2 for execution errors")
    void errorCode2_executionError() {
        var ex = new CliException("Generation failed", 2);

        assertThat(ex.getErrorCode()).isEqualTo(2);
    }

    @Test
    @DisplayName("toString includes errorCode")
    void toString_includesErrorCode() {
        var ex = new CliException("Invalid argument", 1);

        assertThat(ex.toString())
                .contains("CliException")
                .contains("Invalid argument")
                .contains("1");
    }

    @Test
    @DisplayName("is catchable as RuntimeException")
    void catchableAsRuntimeException() {
        Throwable thrown = catchThrowable(() -> {
            throw new CliException("test", 1);
        });

        assertThat(thrown)
                .isInstanceOf(RuntimeException.class)
                .isInstanceOf(CliException.class);
    }
}
