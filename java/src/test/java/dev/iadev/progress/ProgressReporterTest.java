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

/**
 * Tests for {@link ProgressReporter}.
 */
class ProgressReporterTest {

    private final ProgressReporter reporter =
            new ProgressReporter();

    @Nested
    class CompleteReport {

        @Test
        void generateReport_mixedStatuses_containsAllSections() {
            var stories = buildMixedStories();
            var state = stateWith(stories, 2);

            var report = reporter.generateReport(state, 2, 5);

            assertThat(report)
                    .contains("=== Epic Execution Progress ===")
                    .contains("5/10 (50%)")
                    .contains("SUCCESS: 5 | FAILED: 1"
                            + " | BLOCKED: 2 | PENDING: 2")
                    .contains("Phase 2/5 in progress")
                    .contains("Estimated remaining:")
                    .contains("Average:")
                    .contains("Stories by status:")
                    .contains("SUCCESS: s1, s2, s3, s4, s5")
                    .contains("FAILED: s6")
                    .contains("BLOCKED: s7, s8")
                    .contains("PENDING: s10, s9");
        }

        @Test
        void generateReport_reportHasCorrectFormat() {
            var stories = buildMixedStories();
            var state = stateWith(stories, 2);

            var report = reporter.generateReport(state, 2, 5);
            var lines = report.split("\n");

            assertThat(lines[0]).isEqualTo(
                    "=== Epic Execution Progress ===");
            // Line 1: progress bar
            assertThat(lines[1]).startsWith("[");
            // Line 2: status summary
            assertThat(lines[2]).startsWith("SUCCESS:");
            // Line 3: phase progress
            assertThat(lines[3]).startsWith("Phase ");
            // Line 4: ETA
            assertThat(lines[4]).startsWith(
                    "Estimated remaining:");
            // Line 5: throughput
            assertThat(lines[5]).startsWith("Average:");
        }
    }

    @Nested
    class EmptyState {

        @Test
        void generateReport_emptyState_metricsZeroed() {
            var state = emptyState();

            var report = reporter.generateReport(state, 0, 0);

            assertThat(report)
                    .contains("=== Epic Execution Progress ===")
                    .contains("0/0 (0%)")
                    .contains("SUCCESS: 0 | FAILED: 0"
                            + " | BLOCKED: 0 | PENDING: 0")
                    .contains("Estimated remaining: unknown")
                    .contains("Average: 0.0 min/story");
        }
    }

    @Nested
    class AllCompleted {

        @Test
        void generateReport_allCompleted_fullBar() {
            var stories = new LinkedHashMap<String, StoryEntry>();
            stories.put("s1", successEntry(0, 60_000L));
            stories.put("s2", successEntry(0, 120_000L));

            var state = stateWith(stories, 0);

            var report = reporter.generateReport(state, 1, 1);

            assertThat(report)
                    .contains("2/2 (100%)")
                    .contains("SUCCESS: 2 | FAILED: 0"
                            + " | BLOCKED: 0 | PENDING: 0")
                    .contains("Estimated remaining: 0.0 min")
                    .contains("  SUCCESS: s1, s2")
                    .doesNotContain("  FAILED:")
                    .doesNotContain("  BLOCKED:")
                    .doesNotContain("  PENDING:");
        }
    }

    @Nested
    class OnlyFailed {

        @Test
        void generateReport_allFailed_etaUnknown() {
            var stories = new LinkedHashMap<String, StoryEntry>();
            stories.put("s1", failedEntry(0, 50_000L));
            stories.put("s2", failedEntry(0, 30_000L));

            var state = stateWith(stories, 0);

            var report = reporter.generateReport(state, 1, 1);

            assertThat(report)
                    .contains("0/2 (0%)")
                    .contains("Estimated remaining: unknown")
                    .contains("FAILED: s1, s2");
        }
    }

    @Nested
    class StoriesByStatusSection {

        @Test
        void generateReport_storiesByStatus_omitsEmptyGroups() {
            var stories = new LinkedHashMap<String, StoryEntry>();
            stories.put("s1", successEntry(0, 60_000L));
            stories.put("s2", pendingEntry(1));

            var state = stateWith(stories, 0);
            var report = reporter.generateReport(state, 1, 2);

            assertThat(report)
                    .contains("  SUCCESS: s1")
                    .contains("  PENDING: s2")
                    .doesNotContain("  FAILED:")
                    .doesNotContain("  BLOCKED:");
        }

        @Test
        void generateReport_storiesByStatus_sortsIds() {
            var stories = new LinkedHashMap<String, StoryEntry>();
            stories.put("s3", successEntry(0, 30_000L));
            stories.put("s1", successEntry(0, 10_000L));
            stories.put("s2", successEntry(0, 20_000L));

            var state = stateWith(stories, 0);
            var report = reporter.generateReport(state, 1, 1);

            assertThat(report).contains("SUCCESS: s1, s2, s3");
        }
    }

    private static LinkedHashMap<String, StoryEntry>
            buildMixedStories() {
        var stories = new LinkedHashMap<String, StoryEntry>();
        stories.put("s1", successEntry(0, 60_000L));
        stories.put("s2", successEntry(0, 120_000L));
        stories.put("s3", successEntry(0, 90_000L));
        stories.put("s4", successEntry(0, 130_000L));
        stories.put("s5", successEntry(1, 100_000L));
        stories.put("s6", failedEntry(1, 40_000L));
        stories.put("s7", blockedEntry(2));
        stories.put("s8", blockedEntry(2));
        stories.put("s9", pendingEntry(2));
        stories.put("s10", pendingEntry(2));
        return stories;
    }

    private static ExecutionState emptyState() {
        return new ExecutionState(
                "EPIC-001", "main", Instant.now(), 0,
                ExecutionMode.FULL, Map.of(),
                Map.of(), ExecutionMetrics.initial(0)
        );
    }

    private static ExecutionState stateWith(
            Map<String, StoryEntry> stories, int phase) {
        return new ExecutionState(
                "EPIC-001", "main", Instant.now(), phase,
                ExecutionMode.FULL, stories,
                Map.of(), ExecutionMetrics.initial(stories.size())
        );
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
