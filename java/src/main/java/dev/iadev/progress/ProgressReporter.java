package dev.iadev.progress;

import dev.iadev.checkpoint.ExecutionMetrics;
import dev.iadev.checkpoint.ExecutionState;
import dev.iadev.checkpoint.StoryEntry;
import dev.iadev.checkpoint.StoryStatus;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * Generates consolidated progress reports combining
 * {@link MetricsCalculator} and {@link ProgressFormatter}.
 *
 * <p>The report includes a progress bar, status summary, ETA,
 * phase progress, throughput, and stories grouped by status.</p>
 */
public final class ProgressReporter {

    private static final String HEADER =
            "=== Epic Execution Progress ===";

    private static final StoryStatus[] DISPLAY_ORDER = {
            StoryStatus.SUCCESS,
            StoryStatus.FAILED,
            StoryStatus.BLOCKED,
            StoryStatus.PENDING
    };

    /**
     * Generates a complete progress report from execution state.
     *
     * @param state        the current execution state
     * @param currentPhase current phase number (1-based)
     * @param totalPhases  total number of phases
     * @return formatted progress report
     */
    public String generateReport(
            ExecutionState state,
            int currentPhase,
            int totalPhases) {
        var metrics = MetricsCalculator.calculate(state);
        var formatted = ProgressFormatter.format(
                metrics, currentPhase, totalPhases);
        var storiesByStatus = buildStoriesByStatus(
                state.stories());

        var report = new StringBuilder();
        report.append(HEADER).append('\n');
        report.append(formatted).append('\n');
        appendStoriesByStatus(report, storiesByStatus);
        return report.toString();
    }

    private Map<StoryStatus, List<String>> buildStoriesByStatus(
            Map<String, StoryEntry> stories) {
        var grouped = new TreeMap<StoryStatus, List<String>>();
        for (var entry : stories.entrySet()) {
            grouped
                    .computeIfAbsent(
                            entry.getValue().status(),
                            k -> new ArrayList<>())
                    .add(entry.getKey());
        }
        for (var ids : grouped.values()) {
            ids.sort(String::compareTo);
        }
        return grouped;
    }

    private void appendStoriesByStatus(
            StringBuilder report,
            Map<StoryStatus, List<String>> grouped) {
        if (grouped.isEmpty()) {
            return;
        }
        report.append('\n');
        report.append("Stories by status:\n");
        for (var status : DISPLAY_ORDER) {
            var ids = grouped.get(status);
            if (ids != null && !ids.isEmpty()) {
                report.append("  ")
                        .append(status.name())
                        .append(": ")
                        .append(String.join(", ", ids))
                        .append('\n');
            }
        }
    }
}
