package dev.iadev.checkpoint;

import dev.iadev.exception.CheckpointValidationException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Tests for {@link CheckpointValidation}.
 */
class CheckpointValidationTest {

    @TempDir
    Path tempDir;

    @Test
    void validate_validState_returnsEmptyList() {
        var state = new ExecutionState(
                "EPIC-0006", "main",
                Instant.now(), 0, ExecutionMode.FULL,
                Map.of("s1", StoryEntry.pending(0)),
                Map.of(), ExecutionMetrics.initial(1)
        );

        assertThat(CheckpointValidation.validate(state)).isEmpty();
    }

    @Test
    void validate_nullState_returnsError() {
        var errors = CheckpointValidation.validate(null);
        assertThat(errors)
                .containsExactly("ExecutionState is null");
    }

    @Test
    void validate_nullEpicId_returnsError() {
        var state = new ExecutionState(
                null, "main", Instant.now(), 0,
                ExecutionMode.FULL, Map.of(), Map.of(),
                ExecutionMetrics.initial(0)
        );

        var errors = CheckpointValidation.validate(state);
        assertThat(errors).contains("epicId is required");
    }

    @Test
    void validate_emptyEpicId_returnsError() {
        var state = new ExecutionState(
                "  ", "main", Instant.now(), 0,
                ExecutionMode.FULL, Map.of(), Map.of(),
                ExecutionMetrics.initial(0)
        );

        var errors = CheckpointValidation.validate(state);
        assertThat(errors).contains("epicId is required");
    }

    @Test
    void validate_nullBranch_returnsError() {
        var state = new ExecutionState(
                "EPIC-001", null, Instant.now(), 0,
                ExecutionMode.FULL, Map.of(), Map.of(),
                ExecutionMetrics.initial(0)
        );

        var errors = CheckpointValidation.validate(state);
        assertThat(errors).contains("branch is required");
    }

    @Test
    void validate_emptyBranch_returnsError() {
        var state = new ExecutionState(
                "EPIC-001", "", Instant.now(), 0,
                ExecutionMode.FULL, Map.of(), Map.of(),
                ExecutionMetrics.initial(0)
        );

        var errors = CheckpointValidation.validate(state);
        assertThat(errors).contains("branch is required");
    }

    @Test
    void validate_nullStartedAt_returnsError() {
        var state = new ExecutionState(
                "EPIC-001", "main", null, 0,
                ExecutionMode.FULL, Map.of(), Map.of(),
                ExecutionMetrics.initial(0)
        );

        var errors = CheckpointValidation.validate(state);
        assertThat(errors).contains("startedAt is required");
    }

    @Test
    void validate_nullMode_returnsError() {
        var state = new ExecutionState(
                "EPIC-001", "main", Instant.now(), 0,
                null, Map.of(), Map.of(),
                ExecutionMetrics.initial(0)
        );

        var errors = CheckpointValidation.validate(state);
        assertThat(errors).contains("mode is required");
    }

    @Test
    void validate_nullStories_returnsError() {
        var state = new ExecutionState(
                "EPIC-001", "main", Instant.now(), 0,
                ExecutionMode.FULL, null, Map.of(),
                ExecutionMetrics.initial(0)
        );

        var errors = CheckpointValidation.validate(state);
        assertThat(errors).contains("stories is required");
    }

    @Test
    void validate_nullIntegrityGates_returnsError() {
        var state = new ExecutionState(
                "EPIC-001", "main", Instant.now(), 0,
                ExecutionMode.FULL, Map.of(), null,
                ExecutionMetrics.initial(0)
        );

        var errors = CheckpointValidation.validate(state);
        assertThat(errors)
                .contains("integrityGates is required");
    }

    @Test
    void validate_nullMetrics_returnsError() {
        var state = new ExecutionState(
                "EPIC-001", "main", Instant.now(), 0,
                ExecutionMode.FULL, Map.of(), Map.of(),
                null
        );

        var errors = CheckpointValidation.validate(state);
        assertThat(errors).contains("metrics is required");
    }

    @Test
    void validate_multipleErrors_reportsAll() {
        var state = new ExecutionState(
                null, null, null, 0,
                null, null, null, null
        );

        var errors = CheckpointValidation.validate(state);
        assertThat(errors).hasSizeGreaterThanOrEqualTo(7);
    }

    @Test
    void validate_storyWithNullStatus_returnsError() {
        var stories = Map.of(
                "s1", new StoryEntry(
                        null, null, 0, 0L, 0,
                        java.util.List.of(), null, 0
                )
        );
        var state = new ExecutionState(
                "EPIC-001", "main", Instant.now(), 0,
                ExecutionMode.FULL, stories, Map.of(),
                ExecutionMetrics.initial(1)
        );

        var errors = CheckpointValidation.validate(state);
        assertThat(errors)
                .contains("story 's1': status is required");
    }

    @Test
    void load_invalidState_throwsCheckpointValidationException() throws Exception {
        var json = """
                {
                  "epicId": null,
                  "branch": "main",
                  "startedAt": "2026-03-19T10:00:00Z",
                  "currentPhase": 0,
                  "mode": "FULL",
                  "stories": {},
                  "integrityGates": {},
                  "metrics": {
                    "storiesCompleted": 0,
                    "storiesTotal": 0,
                    "storiesFailed": 0,
                    "storiesBlocked": 0,
                    "estimatedRemainingMinutes": 0.0,
                    "elapsedMs": 0,
                    "averageStoryDurationMs": 0.0,
                    "storyDurations": {},
                    "phaseDurations": {}
                  }
                }
                """;
        var path = tempDir.resolve("invalid.json");
        Files.writeString(path, json);

        var engine = new CheckpointEngine(
                new JacksonCheckpointPersistence()
        );
        assertThatThrownBy(() -> engine.load(path))
                .isInstanceOf(CheckpointValidationException.class)
                .hasMessageContaining("epicId is required");
    }
}
