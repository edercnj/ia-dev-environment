package dev.iadev.checkpoint;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Coverage tests for {@link CheckpointEngine} —
 * targeting the uncovered mapper() accessor.
 */
@DisplayName("CheckpointEngine — coverage")
class CheckpointEngineCoverageTest {

    @Test
    @DisplayName("mapper returns configured ObjectMapper")
    void mapper_returnsNonNull() {
        ObjectMapper mapper = CheckpointEngine.mapper();

        assertThat(mapper).isNotNull();
    }
}
