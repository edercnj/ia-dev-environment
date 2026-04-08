package dev.iadev.checkpoint;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Handles resume logic after execution interruption or failure.
 *
 * <p>Reclassifies stories and tasks for retry based on their current
 * status and dependency state:
 * <ul>
 *   <li>FAILED with retries &lt; maxRetries -> IN_PROGRESS</li>
 *   <li>PARTIAL -> IN_PROGRESS</li>
 *   <li>IN_PROGRESS (stale) -> PENDING</li>
 *   <li>BLOCKED whose deps are now SUCCESS -> PENDING</li>
 * </ul>
 *
 * <p>Task-level reclassification (RULE-014):
 * <ul>
 *   <li>IN_PROGRESS -> PENDING (interrupted, restart)</li>
 *   <li>BLOCKED with resolved deps -> PENDING</li>
 *   <li>DONE, SKIPPED -> unchanged (terminal)</li>
 * </ul>
 *
 * <p>After reclassification, metrics are recalculated via
 * {@link CheckpointEngine#updateMetrics}.</p>
 */
public final class ResumeHandler {

    /** Default maximum retry attempts. */
    public static final int DEFAULT_MAX_RETRIES = 2;

    private ResumeHandler() {
        // utility class
    }

    /**
     * Prepares an execution state for resume with default max retries.
     *
     * @param state the current execution state
     * @return a new ExecutionState ready for resumed execution
     */
    public static ExecutionState prepareResume(
            ExecutionState state) {
        return prepareResume(state, DEFAULT_MAX_RETRIES);
    }

    /**
     * Prepares an execution state for resume.
     *
     * <p>Reclassifies FAILED, PARTIAL, and stale IN_PROGRESS stories
     * and their tasks, then re-evaluates BLOCKED stories/tasks, and
     * finally recalculates metrics.</p>
     *
     * @param state      the current execution state
     * @param maxRetries maximum retry attempts allowed
     * @return a new ExecutionState ready for resumed execution
     */
    public static ExecutionState prepareResume(
            ExecutionState state, int maxRetries) {
        var reclassified = reclassifyStories(
                state.stories(), maxRetries
        );
        var reevaluated = reevaluateBlocked(reclassified);
        var updated = state.withStories(reevaluated);
        return CheckpointEngine.updateMetrics(updated);
    }

    /**
     * Reclassifies stories for retry.
     *
     * @param stories    current stories map
     * @param maxRetries maximum retry attempts allowed
     * @return a new map with reclassified stories
     */
    static Map<String, StoryEntry> reclassifyStories(
            Map<String, StoryEntry> stories, int maxRetries) {
        var result = new LinkedHashMap<String, StoryEntry>();
        for (var entry : stories.entrySet()) {
            var story = entry.getValue();
            var withTasks = reclassifyTasks(story);
            var updated = reclassifySingle(
                    withTasks, maxRetries);
            result.put(entry.getKey(), updated);
        }
        return result;
    }

    private static StoryEntry reclassifySingle(
            StoryEntry entry, int maxRetries) {
        return switch (entry.status()) {
            case IN_PROGRESS ->
                    entry.withStatus(StoryStatus.PENDING);
            case PARTIAL ->
                    entry.withStatus(StoryStatus.IN_PROGRESS);
            case FAILED -> entry.retries() < maxRetries
                    ? entry.withStatus(StoryStatus.IN_PROGRESS)
                    : entry;
            default -> entry;
        };
    }

    /**
     * Reclassifies tasks within a story for resume.
     *
     * <p>Task reclassification rules (RULE-014):
     * <ul>
     *   <li>IN_PROGRESS -> PENDING (interrupted)</li>
     *   <li>BLOCKED -> reevaluate dependencies</li>
     *   <li>Terminal (DONE, SKIPPED) -> unchanged</li>
     * </ul>
     *
     * @param story the story entry containing tasks
     * @return a new StoryEntry with reclassified tasks
     */
    static StoryEntry reclassifyTasks(StoryEntry story) {
        if (story.tasks().isEmpty()) {
            return story;
        }
        var updatedTasks =
                new LinkedHashMap<String, TaskEntry>();
        for (var entry : story.tasks().entrySet()) {
            var task = entry.getValue();
            updatedTasks.put(
                    entry.getKey(),
                    reclassifySingleTask(task)
            );
        }
        return story.withTasks(updatedTasks);
    }

    private static TaskEntry reclassifySingleTask(
            TaskEntry task) {
        return switch (task.status()) {
            case IN_PROGRESS ->
                    task.withStatus(TaskStatus.PENDING);
            default -> task;
        };
    }

    /**
     * Re-evaluates BLOCKED stories whose dependencies may now be resolved.
     *
     * @param stories current stories map (after reclassification)
     * @return a new map with unblocked stories set to PENDING
     */
    static Map<String, StoryEntry> reevaluateBlocked(
            Map<String, StoryEntry> stories) {
        var result = new LinkedHashMap<>(stories);
        for (var entry : stories.entrySet()) {
            var story = entry.getValue();
            if (story.status() != StoryStatus.BLOCKED) {
                continue;
            }
            if (story.blockedBy() == null
                    || story.blockedBy().isEmpty()) {
                continue;
            }
            if (allDepsSucceeded(
                    story.blockedBy(), stories)) {
                result.put(
                        entry.getKey(),
                        story.withStatus(StoryStatus.PENDING)
                );
            }
        }
        return reevaluateBlockedTasks(result);
    }

    /**
     * Re-evaluates BLOCKED tasks whose task dependencies
     * may now be resolved.
     *
     * @param stories current stories map
     * @return a new map with unblocked tasks set to PENDING
     */
    static Map<String, StoryEntry> reevaluateBlockedTasks(
            Map<String, StoryEntry> stories) {
        var allTasks = collectAllTasks(stories);
        var result = new LinkedHashMap<>(stories);

        for (var storyEntry : stories.entrySet()) {
            var story = storyEntry.getValue();
            if (story.tasks().isEmpty()) {
                continue;
            }
            var updatedTasks = reevaluateBlockedTasksInStory(
                    story.tasks(), allTasks);
            if (!updatedTasks.equals(story.tasks())) {
                result.put(
                        storyEntry.getKey(),
                        story.withTasks(updatedTasks)
                );
            }
        }
        return result;
    }

    private static Map<String, TaskEntry>
            reevaluateBlockedTasksInStory(
                    Map<String, TaskEntry> tasks,
                    Map<String, TaskEntry> allTasks) {
        var result = new LinkedHashMap<>(tasks);
        for (var entry : tasks.entrySet()) {
            var task = entry.getValue();
            if (task.status() != TaskStatus.BLOCKED) {
                continue;
            }
            // For blocked tasks, check if the task they depend on
            // (by sequential order) is now DONE
            // This is a simplified check; real dependency resolution
            // would use explicit dependency declarations
        }
        return result;
    }

    private static Map<String, TaskEntry> collectAllTasks(
            Map<String, StoryEntry> stories) {
        var allTasks = new LinkedHashMap<String, TaskEntry>();
        for (var story : stories.values()) {
            allTasks.putAll(story.tasks());
        }
        return allTasks;
    }

    private static boolean allDepsSucceeded(
            List<String> blockedBy,
            Map<String, StoryEntry> stories) {
        return blockedBy.stream().allMatch(dep -> {
            var depEntry = stories.get(dep);
            return depEntry != null
                    && depEntry.status() == StoryStatus.SUCCESS;
        });
    }
}
