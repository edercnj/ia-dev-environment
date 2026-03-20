package dev.iadev.exception;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("CheckpointIOException")
class CheckpointIOExceptionTest {

    @Test
    @DisplayName("carries message, filePath, and cause")
    void constructor_whenCalled_carriesAllFields() {
        var cause = new IOException("file not found");
        var ex = new CheckpointIOException(
                "Failed to read checkpoint",
                "/tmp/checkpoint/execution-state.json", cause);

        assertThat(ex.getMessage())
                .isEqualTo("Failed to read checkpoint");
        assertThat(ex.getFilePath())
                .isEqualTo("/tmp/checkpoint/execution-state.json");
        assertThat(ex.getCause()).isSameAs(cause);
    }

    @Test
    @DisplayName("extends RuntimeException")
    void create_whenCalled_extendsRuntimeException() {
        var ex = new CheckpointIOException(
                "error", "/path", new RuntimeException());

        assertThat(ex).isInstanceOf(RuntimeException.class);
    }

    @Test
    @DisplayName("preserves original IOException as cause")
    void create_whenCalled_preservesCauseChain() {
        var ioException = new IOException("permission denied");
        var ex = new CheckpointIOException(
                "Write failed",
                "/var/checkpoint/state.json", ioException);

        assertThat(ex.getCause())
                .isSameAs(ioException)
                .hasMessage("permission denied");
    }

    @Test
    @DisplayName("toString includes filePath")
    void toString_whenCalled_includesFilePath() {
        var ex = new CheckpointIOException(
                "Read failed",
                "/tmp/checkpoint/execution-state.json",
                new IOException());

        assertThat(ex.toString())
                .contains("CheckpointIOException")
                .contains("/tmp/checkpoint/execution-state.json");
    }
}
