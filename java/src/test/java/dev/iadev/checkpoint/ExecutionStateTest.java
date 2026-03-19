package dev.iadev.checkpoint;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link ExecutionState} record and with* methods.
 */
class ExecutionStateTest {

    private ExecutionState createSampleState() {
        var stories = new LinkedHashMap<String, StoryEntry>();
        stories.put("story-001", StoryEntry.pending(0));
        stories.put("story-002", StoryEntry.pending(1));

        return new ExecutionState(
                "EPIC-0006",
                "feat/epic-0006",
                Instant.parse("2026-03-19T10:00:00Z"),
                0,
                ExecutionMode.FULL,
                Map.copyOf(stories),
                Map.of(),
                ExecutionMetrics.initial(2)
        );
    }

    @Test
    void constructor_preservesAllFields() {
        var state = createSampleState();

        assertThat(state.epicId()).isEqualTo("EPIC-0006");
        assertThat(state.branch()).isEqualTo("feat/epic-0006");
        assertThat(state.startedAt())
                .isEqualTo(Instant.parse("2026-03-19T10:00:00Z"));
        assertThat(state.currentPhase()).isZero();
        assertThat(state.mode()).isEqualTo(ExecutionMode.FULL);
        assertThat(state.stories()).hasSize(2);
        assertThat(state.integrityGates()).isEmpty();
        assertThat(state.metrics().storiesTotal()).isEqualTo(2);
    }

    @Test
    void withStory_addsNewStory() {
        var state = createSampleState();
        var newEntry = StoryEntry.pending(2);

        var updated = state.withStory("story-003", newEntry);

        assertThat(updated.stories()).hasSize(3);
        assertThat(updated.stories().get("story-003"))
                .isEqualTo(newEntry);
        assertThat(state.stories()).hasSize(2);
    }

    @Test
    void withStory_updatesExistingStory() {
        var state = createSampleState();
        var updatedEntry = StoryEntry.pending(0)
                .withStatus(StoryStatus.SUCCESS);

        var updated = state.withStory("story-001", updatedEntry);

        assertThat(updated.stories().get("story-001").status())
                .isEqualTo(StoryStatus.SUCCESS);
        assertThat(state.stories().get("story-001").status())
                .isEqualTo(StoryStatus.PENDING);
    }

    @Test
    void withMetrics_returnsNewStateWithUpdatedMetrics() {
        var state = createSampleState();
        var newMetrics = new ExecutionMetrics(
                1, 2, 0, 0, 1.0, 60_000L, 60_000.0,
                Map.of(), Map.of()
        );

        var updated = state.withMetrics(newMetrics);

        assertThat(updated.metrics().storiesCompleted()).isEqualTo(1);
        assertThat(state.metrics().storiesCompleted()).isZero();
    }

    @Test
    void withStories_replacesEntireMap() {
        var state = createSampleState();
        var newStories = Map.of(
                "story-X", StoryEntry.pending(0)
        );

        var updated = state.withStories(newStories);

        assertThat(updated.stories()).hasSize(1);
        assertThat(updated.stories()).containsKey("story-X");
        assertThat(state.stories()).hasSize(2);
    }

    @Test
    void withIntegrityGate_addsGate() {
        var state = createSampleState();
        var gate = IntegrityGateEntry.pass("compilation");

        var updated = state.withIntegrityGate("compilation", gate);

        assertThat(updated.integrityGates()).hasSize(1);
        assertThat(updated.integrityGates().get("compilation"))
                .isEqualTo(gate);
        assertThat(state.integrityGates()).isEmpty();
    }

    @Test
    void withCurrentPhase_updatesPhase() {
        var state = createSampleState();
        var updated = state.withCurrentPhase(3);

        assertThat(updated.currentPhase()).isEqualTo(3);
        assertThat(state.currentPhase()).isZero();
    }

    @Test
    void immutability_withStoryDoesNotMutateOriginal() {
        var state = createSampleState();
        var originalStories = state.stories();

        state.withStory("story-003", StoryEntry.pending(2));

        assertThat(state.stories()).isEqualTo(originalStories);
        assertThat(state.stories()).hasSize(2);
    }
}
