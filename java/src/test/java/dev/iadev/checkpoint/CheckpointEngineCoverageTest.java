package dev.iadev.checkpoint;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.nio.file.Path;
import java.time.Instant;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests for {@link CheckpointEngine} with mock persistence —
 * verifies the engine delegates to the port correctly
 * without requiring Jackson on the test classpath.
 */
@DisplayName("CheckpointEngine — mock persistence")
@ExtendWith(MockitoExtension.class)
class CheckpointEngineCoverageTest {

    @Mock
    private CheckpointPersistence persistence;

    @Test
    @DisplayName("save delegates to persistence port")
    void save_delegatesToPersistence() {
        var engine = new CheckpointEngine(persistence);
        var state = createMinimalState();
        var path = Path.of("/tmp/test-checkpoint.json");

        engine.save(state, path);

        verify(persistence).save(state, path);
    }

    @Test
    @DisplayName("load delegates to persistence port")
    void load_delegatesToPersistence() {
        var engine = new CheckpointEngine(persistence);
        var path = Path.of("/tmp/test-checkpoint.json");
        var expected = createMinimalState();
        when(persistence.load(path)).thenReturn(expected);

        var result = engine.load(path);

        verify(persistence).load(path);
        assertThat(result).isEqualTo(expected);
    }

    private ExecutionState createMinimalState() {
        return new ExecutionState(
                "EPIC-001", "main",
                Instant.parse("2026-03-19T10:00:00Z"),
                0, ExecutionMode.FULL,
                Map.of("s1", StoryEntry.pending(0)),
                Map.of(),
                ExecutionMetrics.initial(1)
        );
    }
}
