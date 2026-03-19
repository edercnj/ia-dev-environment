package dev.iadev.checkpoint;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link ExecutionMetrics} record.
 */
class ExecutionMetricsTest {

    @Test
    void initial_createsZeroMetrics() {
        var metrics = ExecutionMetrics.initial(10);

        assertThat(metrics.storiesCompleted()).isZero();
        assertThat(metrics.storiesTotal()).isEqualTo(10);
        assertThat(metrics.storiesFailed()).isZero();
        assertThat(metrics.storiesBlocked()).isZero();
        assertThat(metrics.estimatedRemainingMinutes()).isZero();
        assertThat(metrics.elapsedMs()).isZero();
        assertThat(metrics.averageStoryDurationMs()).isZero();
        assertThat(metrics.storyDurations()).isEmpty();
        assertThat(metrics.phaseDurations()).isEmpty();
    }

    @Test
    void constructor_allFields_preservesValues() {
        var storyDurations = Map.of("s1", 60_000L, "s2", 120_000L);
        var phaseDurations = Map.of(0, 180_000L);

        var metrics = new ExecutionMetrics(
                5, 10, 1, 2, 45.5, 600_000L, 120_000.0,
                storyDurations, phaseDurations
        );

        assertThat(metrics.storiesCompleted()).isEqualTo(5);
        assertThat(metrics.storiesTotal()).isEqualTo(10);
        assertThat(metrics.storiesFailed()).isEqualTo(1);
        assertThat(metrics.storiesBlocked()).isEqualTo(2);
        assertThat(metrics.estimatedRemainingMinutes())
                .isEqualTo(45.5);
        assertThat(metrics.elapsedMs()).isEqualTo(600_000L);
        assertThat(metrics.averageStoryDurationMs())
                .isEqualTo(120_000.0);
        assertThat(metrics.storyDurations())
                .containsEntry("s1", 60_000L)
                .containsEntry("s2", 120_000L);
        assertThat(metrics.phaseDurations())
                .containsEntry(0, 180_000L);
    }
}
