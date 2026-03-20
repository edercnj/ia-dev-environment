package dev.iadev.exception;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("ConfigParseException")
class ConfigParseExceptionTest {

    @Test
    @DisplayName("carries message, filePath, and cause")
    void constructor_whenCalled_carriesAllFields() {
        var cause = new IOException("malformed YAML");
        var ex = new ConfigParseException(
                "Failed to parse config", "config.yaml", cause);

        assertThat(ex.getMessage()).isEqualTo("Failed to parse config");
        assertThat(ex.getFilePath()).isEqualTo("config.yaml");
        assertThat(ex.getCause()).isSameAs(cause);
    }

    @Test
    @DisplayName("extends RuntimeException")
    void load_whenCalled_extendsRuntimeException() {
        var ex = new ConfigParseException(
                "error", "file.yaml", new RuntimeException());

        assertThat(ex).isInstanceOf(RuntimeException.class);
    }

    @Test
    @DisplayName("preserves cause chain for stack trace")
    void load_whenCalled_preservesCauseChain() {
        var rootCause = new IllegalStateException("scanner error");
        var ex = new ConfigParseException(
                "Parse failed", "/path/to/config.yaml", rootCause);

        assertThat(ex.getCause()).isSameAs(rootCause);
        assertThat(ex.getCause().getMessage()).isEqualTo("scanner error");
    }

    @Test
    @DisplayName("toString includes filePath")
    void toString_whenCalled_includesFilePath() {
        var ex = new ConfigParseException(
                "Parse failed", "config.yaml", new RuntimeException());

        assertThat(ex.toString())
                .contains("ConfigParseException")
                .contains("config.yaml");
    }
}
