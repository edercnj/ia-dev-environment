package dev.iadev.exception;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("PartialExecutionException")
class PartialExecutionExceptionTest {

    @Test
    @DisplayName("carries message and storyId")
    void constructor_whenCalled_carriesMessageAndStoryId() {
        var ex = new PartialExecutionException(
                "Story executed partially", "story-0006-0003");

        assertThat(ex.getMessage())
                .isEqualTo("Story executed partially");
        assertThat(ex.getStoryId()).isEqualTo("story-0006-0003");
    }

    @Test
    @DisplayName("extends RuntimeException")
    void create_whenCalled_extendsRuntimeException() {
        var ex = new PartialExecutionException(
                "error", "story-id");

        assertThat(ex).isInstanceOf(RuntimeException.class);
    }

    @Test
    @DisplayName("toString includes storyId")
    void toString_whenCalled_includesStoryId() {
        var ex = new PartialExecutionException(
                "Partial execution", "story-0006-0010");

        assertThat(ex.toString())
                .contains("PartialExecutionException")
                .contains("story-0006-0010");
    }
}
