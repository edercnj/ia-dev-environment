package dev.iadev.parallelism;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.TreeSet;

/**
 * Aggregates per-task {@link FileFootprint} instances into a single
 * story-level footprint with deterministic ordering and legacy-safe
 * warning reporting.
 *
 * <p>Consumed by {@code x-story-plan} Phase 6 (story-0041-0003) to emit
 * the {@code ## Story File Footprint} block appended to a story's
 * planning report. Downstream consumers ({@code /x-parallel-eval
 * --scope=story}) use the aggregated block to detect write-write
 * conflicts between pairs of stories WITHOUT parsing N task plans.</p>
 *
 * <p>Semantics:</p>
 * <ul>
 *   <li>Union-set per sub-section — {@code writes = ⋃ write(task)},
 *       {@code reads = ⋃ read(task)}, {@code regens = ⋃ regen(task)}.</li>
 *   <li>Duplicated paths across tasks are deduplicated.</li>
 *   <li>Each sub-section is alphabetically ordered (TreeSet) — RULE-008
 *       (Output Determinístico).</li>
 *   <li>A task that yields {@link FileFootprint#EMPTY} (legacy plan with
 *       no structured footprint block) is skipped from the union and
 *       recorded as a warning (RULE-006 — Backward Compatibility).</li>
 *   <li>Warnings are emitted in task-ID lexicographic order (NOT the
 *       discovery order of the input list) so re-runs are byte-identical.</li>
 * </ul>
 */
public final class StoryFootprintAggregator {

    private StoryFootprintAggregator() {
        // utility
    }

    /**
     * A single task's footprint contribution identified by its task ID.
     *
     * @param taskId    the canonical task identifier
     *                  (e.g. {@code "TASK-0041-0003-001"}); non-null
     * @param footprint the parsed footprint of the task
     *                  (may be {@link FileFootprint#EMPTY}); non-null
     */
    public record TaskFootprintSource(String taskId, FileFootprint footprint) {
        public TaskFootprintSource {
            Objects.requireNonNull(taskId, "taskId");
            Objects.requireNonNull(footprint, "footprint");
        }
    }

    /**
     * Immutable aggregation result returned to the SKILL renderer.
     *
     * @param footprint the union-set across all non-empty task footprints
     * @param warnings  human-readable warning messages (one per legacy
     *                  task with empty footprint, plus the synthetic
     *                  {@code "no task plans found"} warning for the
     *                  zero-task degenerate case)
     * @param taskCount total number of task sources processed (INCLUDING
     *                  legacy ones that produced warnings) — matches the
     *                  {@code "Aggregated from N task footprints"}
     *                  header figure
     */
    public record Result(
            FileFootprint footprint,
            List<String> warnings,
            int taskCount) {
        public Result {
            Objects.requireNonNull(footprint, "footprint");
            warnings = (warnings == null)
                    ? List.of()
                    : List.copyOf(warnings);
        }
    }

    /**
     * Aggregate the given list of per-task footprints into a single
     * story-level footprint.
     *
     * @param sources ordered list of {@link TaskFootprintSource} (the
     *                list MAY be empty — that is the documented
     *                degenerate case for stories with 0 generated task
     *                plans)
     * @return the aggregation result; never null
     * @throws NullPointerException if {@code sources} is null or any
     *                              element is null
     */
    public static Result aggregate(List<TaskFootprintSource> sources) {
        Objects.requireNonNull(sources, "sources");

        if (sources.isEmpty()) {
            return new Result(
                    FileFootprint.EMPTY,
                    List.of("no task plans found"),
                    0);
        }

        TreeSet<String> writes = new TreeSet<>();
        TreeSet<String> reads = new TreeSet<>();
        TreeSet<String> regens = new TreeSet<>();
        TreeSet<String> legacyTaskIds = new TreeSet<>();

        for (TaskFootprintSource src : sources) {
            Objects.requireNonNull(src, "source");
            FileFootprint fp = src.footprint();
            if (fp.isEmpty()) {
                legacyTaskIds.add(src.taskId());
                continue;
            }
            writes.addAll(fp.writes());
            reads.addAll(fp.reads());
            regens.addAll(fp.regens());
        }

        List<String> warnings = new ArrayList<>(legacyTaskIds.size());
        for (String taskId : legacyTaskIds) {
            warnings.add(taskId + " sem footprint (legacy)");
        }

        FileFootprint aggregated = new FileFootprint(writes, reads, regens);
        return new Result(
                aggregated,
                Collections.unmodifiableList(warnings),
                sources.size());
    }
}
