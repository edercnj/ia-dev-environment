package dev.iadev.progress;

import dev.iadev.checkpoint.ExecutionMetrics;
import dev.iadev.checkpoint.ExecutionMode;
import dev.iadev.checkpoint.ExecutionState;
import dev.iadev.checkpoint.StoryEntry;
import dev.iadev.checkpoint.StoryStatus;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

/**
 * Tests for {@link MetricsCalculator}.
 */
class MetricsCalculatorTest {

    private static final Instant NOW = Instant.now();

    private static ExecutionState emptyState() {
        return new ExecutionState(
                "EPIC-001", "main", NOW, 0,
                ExecutionMode.FULL, Map.of(),
                Map.of(), ExecutionMetrics.initial(0)
        );
    }

    private static ExecutionState stateWith(
            Map<String, StoryEntry> stories) {
        return new ExecutionState(
                "EPIC-001", "main", NOW, 0,
                ExecutionMode.FULL, stories,
                Map.of(), ExecutionMetrics.initial(stories.size())
        );
    }

    @Nested
    class EmptyState {

        @Test
        void calculate_emptyState_returnsZeroMetrics() {
            var metrics = MetricsCalculator.calculate(emptyState());

            assertThat(metrics.storiesCompleted()).isZero();
            assertThat(metrics.storiesTotal()).isZero();
            assertThat(metrics.storiesFailed()).isZero();
            assertThat(metrics.storiesBlocked()).isZero();
            assertThat(metrics.averageStoryDurationMs()).isZero();
            assertThat(metrics.estimatedRemainingMinutes())
                    .isEqualTo(-1.0);
            assertThat(metrics.storyDurations()).isEmpty();
            assertThat(metrics.phaseDurations()).isEmpty();
        }
    }

    @Nested
    class SingleStorySuccess {

        @Test
        void calculate_oneSuccess_computesAvgAndEta() {
            var stories = Map.of(
                    "story-001",
                    successEntry(0, 60_000L)
            );
            var state = stateWith(stories);

            var metrics = MetricsCalculator.calculate(state);

            assertThat(metrics.storiesCompleted()).isEqualTo(1);
            assertThat(metrics.storiesTotal()).isEqualTo(1);
            assertThat(metrics.averageStoryDurationMs())
                    .isEqualTo(60_000.0);
            assertThat(metrics.estimatedRemainingMinutes())
                    .isEqualTo(0.0);
        }
    }

    @Nested
    class MultipleStoriesSuccess {

        @Test
        void calculate_fourSuccess_computesCorrectAvgAndEta() {
            var stories = new LinkedHashMap<String, StoryEntry>();
            stories.put("story-001", successEntry(0, 60_000L));
            stories.put("story-002", successEntry(0, 120_000L));
            stories.put("story-003", successEntry(0, 90_000L));
            stories.put("story-004", successEntry(0, 130_000L));
            stories.put("story-005", pendingEntry(1));
            stories.put("story-006", pendingEntry(1));
            stories.put("story-007", pendingEntry(1));
            stories.put("story-008", pendingEntry(1));
            stories.put("story-009", pendingEntry(2));
            stories.put("story-010", pendingEntry(2));

            var state = stateWith(stories);

            var metrics = MetricsCalculator.calculate(state);

            assertThat(metrics.storiesCompleted()).isEqualTo(4);
            assertThat(metrics.storiesTotal()).isEqualTo(10);
            assertThat(metrics.averageStoryDurationMs())
                    .isCloseTo(100_000.0, within(0.01));
            // (10 - 4) * 100000 / 60000 = 10.0
            assertThat(metrics.estimatedRemainingMinutes())
                    .isCloseTo(10.0, within(0.01));
        }
    }

    @Nested
    class NoCompletedStories {

        @Test
        void calculate_noneCompleted_etaIsNegativeOne() {
            var stories = Map.of(
                    "story-001", pendingEntry(0),
                    "story-002", pendingEntry(0)
            );
            var state = stateWith(stories);

            var metrics = MetricsCalculator.calculate(state);

            assertThat(metrics.storiesCompleted()).isZero();
            assertThat(metrics.storiesTotal()).isEqualTo(2);
            assertThat(metrics.averageStoryDurationMs()).isZero();
            assertThat(metrics.estimatedRemainingMinutes())
                    .isEqualTo(-1.0);
        }

        @Test
        void calculate_allFailed_etaIsNegativeOne() {
            var stories = Map.of(
                    "story-001", failedEntry(0, 50_000L),
                    "story-002", failedEntry(0, 30_000L)
            );
            var state = stateWith(stories);

            var metrics = MetricsCalculator.calculate(state);

            assertThat(metrics.storiesCompleted()).isZero();
            assertThat(metrics.storiesFailed()).isEqualTo(2);
            assertThat(metrics.estimatedRemainingMinutes())
                    .isEqualTo(-1.0);
        }
    }

    @Nested
    class StatusCounts {

        @Test
        void calculate_mixedStatuses_countsCorrectly() {
            var stories = new LinkedHashMap<String, StoryEntry>();
            stories.put("s1", successEntry(0, 10_000L));
            stories.put("s2", successEntry(0, 20_000L));
            stories.put("s3", successEntry(0, 30_000L));
            stories.put("s4", failedEntry(0, 5_000L));
            stories.put("s5", blockedEntry(1));
            stories.put("s6", blockedEntry(1));
            stories.put("s7", pendingEntry(2));
            stories.put("s8", pendingEntry(2));
            stories.put("s9", pendingEntry(2));
            stories.put("s10", pendingEntry(2));

            var state = stateWith(stories);
            var metrics = MetricsCalculator.calculate(state);

            assertThat(metrics.storiesCompleted()).isEqualTo(3);
            assertThat(metrics.storiesFailed()).isEqualTo(1);
            assertThat(metrics.storiesBlocked()).isEqualTo(2);
            assertThat(metrics.storiesTotal()).isEqualTo(10);
        }
    }

    @Nested
    class StoryDurations {

        @Test
        void calculate_storyDurations_onlyPositive() {
            var stories = new LinkedHashMap<String, StoryEntry>();
            stories.put("s1", successEntry(0, 60_000L));
            stories.put("s2", successEntry(0, 0L));
            stories.put("s3", pendingEntry(1));

            var state = stateWith(stories);
            var metrics = MetricsCalculator.calculate(state);

            assertThat(metrics.storyDurations())
                    .containsEntry("s1", 60_000L)
                    .doesNotContainKey("s2")
                    .doesNotContainKey("s3")
                    .hasSize(1);
        }
    }

    @Nested
    class PhaseDurations {

        @Test
        void calculate_phaseDurations_aggregatesByPhase() {
            var stories = new LinkedHashMap<String, StoryEntry>();
            stories.put("story-001", successEntry(0, 60_000L));
            stories.put("story-002", successEntry(0, 80_000L));
            stories.put("story-003", successEntry(1, 120_000L));
            stories.put("story-004", successEntry(2, 90_000L));
            stories.put("story-005", successEntry(2, 110_000L));

            var state = stateWith(stories);
            var metrics = MetricsCalculator.calculate(state);

            assertThat(metrics.phaseDurations())
                    .containsEntry(0, 140_000L)
                    .containsEntry(1, 120_000L)
                    .containsEntry(2, 200_000L)
                    .hasSize(3);
        }

        @Test
        void calculate_phaseDurations_includesAllStatusTypes() {
            var stories = new LinkedHashMap<String, StoryEntry>();
            stories.put("s1", successEntry(0, 60_000L));
            stories.put("s2", failedEntry(0, 40_000L));

            var state = stateWith(stories);
            var metrics = MetricsCalculator.calculate(state);

            assertThat(metrics.phaseDurations())
                    .containsEntry(0, 100_000L);
        }
    }

    @Nested
    class ElapsedMs {

        @Test
        void calculate_elapsedMs_computesDifference() {
            var pastInstant = Instant.now().minusMillis(5_000L);
            var state = new ExecutionState(
                    "EPIC-001", "main", pastInstant, 0,
                    ExecutionMode.FULL, Map.of(),
                    Map.of(), ExecutionMetrics.initial(0)
            );

            var metrics = MetricsCalculator.calculate(state);

            assertThat(metrics.elapsedMs())
                    .isGreaterThanOrEqualTo(4_900L);
        }
    }

    @Nested
    class AllCompleted {

        @Test
        void calculate_allCompleted_etaIsZero() {
            var stories = Map.of(
                    "s1", successEntry(0, 30_000L),
                    "s2", successEntry(0, 60_000L)
            );
            var state = stateWith(stories);

            var metrics = MetricsCalculator.calculate(state);

            assertThat(metrics.storiesCompleted()).isEqualTo(2);
            assertThat(metrics.storiesTotal()).isEqualTo(2);
            assertThat(metrics.estimatedRemainingMinutes())
                    .isEqualTo(0.0);
        }
    }

    private static StoryEntry successEntry(
            int phase, long duration) {
        return StoryEntry.pending(phase)
                .withStatus(StoryStatus.SUCCESS)
                .withDuration(duration);
    }

    private static StoryEntry pendingEntry(int phase) {
        return StoryEntry.pending(phase);
    }

    private static StoryEntry failedEntry(
            int phase, long duration) {
        return StoryEntry.pending(phase)
                .withStatus(StoryStatus.FAILED)
                .withDuration(duration);
    }

    private static StoryEntry blockedEntry(int phase) {
        return StoryEntry.pending(phase)
                .withStatus(StoryStatus.BLOCKED);
    }
}
