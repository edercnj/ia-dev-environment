package dev.iadev.exception;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("PipelineException")
class PipelineExceptionTest {

    @Test
    @DisplayName("carries message, assemblerName, and cause")
    void constructor_whenCalled_carriesAllFields() {
        var cause = new IOException("disk full");
        var ex = new PipelineException(
                "Pipeline failed at RulesAssembler",
                "RulesAssembler", cause);

        assertThat(ex.getMessage())
                .isEqualTo("Pipeline failed at RulesAssembler");
        assertThat(ex.getAssemblerName()).isEqualTo("RulesAssembler");
        assertThat(ex.getCause()).isSameAs(cause);
    }

    @Test
    @DisplayName("extends RuntimeException")
    void create_whenCalled_extendsRuntimeException() {
        var ex = new PipelineException(
                "error", "assembler", new RuntimeException());

        assertThat(ex).isInstanceOf(RuntimeException.class);
    }

    @Test
    @DisplayName("toString includes assemblerName")
    void toString_whenCalled_includesAssemblerName() {
        var ex = new PipelineException(
                "Pipeline failed", "SkillsAssembler",
                new RuntimeException());

        assertThat(ex.toString())
                .contains("PipelineException")
                .contains("SkillsAssembler");
    }

    @Test
    @DisplayName("preserves original IOException as cause")
    void create_whenCalled_preservesOriginalCause() {
        var ioException = new IOException("permission denied");
        var ex = new PipelineException(
                "Write failed", "AgentsAssembler", ioException);

        assertThat(ex.getCause())
                .isSameAs(ioException)
                .hasMessage("permission denied");
    }
}
