package dev.iadev.checkpoint;

import java.util.List;
import java.util.Map;

/**
 * Represents the state of an individual story during execution.
 *
 * <p>Tracks status, commit information, timing, retries, dependencies,
 * review findings, per-task state, and parent branch. All fields are
 * immutable; create a new instance to update state.</p>
 *
 * <p>The {@code tasks} field is optional for backward compatibility
 * (RULE-010). Epics created before v2.0 will not have this field in
 * their JSON; deserialization defaults to an empty map.</p>
 *
 * @param status        current lifecycle status
 * @param commitSha     SHA of the commit if completed (nullable)
 * @param phase         execution phase (0-based)
 * @param duration      duration in milliseconds
 * @param retries       number of retry attempts
 * @param blockedBy     IDs of stories blocking this one
 * @param summary       execution summary (nullable)
 * @param findingsCount number of issues found during review
 * @param tasks         per-task state keyed by task ID (default empty)
 * @param parentBranch  parent branch for auto-approve-pr mode (nullable)
 */
public record StoryEntry(
        StoryStatus status,
        String commitSha,
        int phase,
        long duration,
        int retries,
        List<String> blockedBy,
        String summary,
        int findingsCount,
        Map<String, TaskEntry> tasks,
        String parentBranch
) {

    /**
     * Compact constructor for backward compatibility.
     *
     * <p>Ensures {@code tasks} is never null (defaults to empty map)
     * and {@code blockedBy} is never null (defaults to empty list).</p>
     */
    public StoryEntry {
        tasks = tasks != null ? Map.copyOf(tasks) : Map.of();
        blockedBy = blockedBy != null
                ? List.copyOf(blockedBy) : List.of();
    }

    /**
     * Backward-compatible constructor without tasks and parentBranch.
     *
     * <p>Used by existing code that does not yet track per-task state.
     * Defaults tasks to empty map and parentBranch to null.</p>
     *
     * @param status        current lifecycle status
     * @param commitSha     SHA of the commit if completed
     * @param phase         execution phase (0-based)
     * @param duration      duration in milliseconds
     * @param retries       number of retry attempts
     * @param blockedBy     IDs of stories blocking this one
     * @param summary       execution summary
     * @param findingsCount number of issues found during review
     */
    public StoryEntry(
            StoryStatus status,
            String commitSha,
            int phase,
            long duration,
            int retries,
            List<String> blockedBy,
            String summary,
            int findingsCount) {
        this(status, commitSha, phase, duration, retries,
                blockedBy, summary, findingsCount,
                Map.of(), null);
    }

    /**
     * Creates a StoryEntry with default values for optional fields.
     *
     * @param phase the execution phase
     * @return a new StoryEntry in initial state
     */
    public static StoryEntry pending(int phase) {
        return new StoryEntry(
                StoryStatus.PENDING, null, phase, 0L, 0,
                List.of(), null, 0, Map.of(), null
        );
    }

    /**
     * Returns a copy with the given status.
     *
     * @param newStatus the new status
     * @return a new StoryEntry with updated status
     */
    public StoryEntry withStatus(StoryStatus newStatus) {
        return new StoryEntry(
                newStatus, commitSha, phase, duration, retries,
                blockedBy, summary, findingsCount,
                tasks, parentBranch
        );
    }

    /**
     * Returns a copy with the given commit SHA.
     *
     * @param newCommitSha the commit SHA
     * @return a new StoryEntry with updated commitSha
     */
    public StoryEntry withCommitSha(String newCommitSha) {
        return new StoryEntry(
                status, newCommitSha, phase, duration, retries,
                blockedBy, summary, findingsCount,
                tasks, parentBranch
        );
    }

    /**
     * Returns a copy with the given retry count.
     *
     * @param newRetries the retry count
     * @return a new StoryEntry with updated retries
     */
    public StoryEntry withRetries(int newRetries) {
        return new StoryEntry(
                status, commitSha, phase, duration, newRetries,
                blockedBy, summary, findingsCount,
                tasks, parentBranch
        );
    }

    /**
     * Returns a copy with the given duration.
     *
     * @param newDuration the duration in milliseconds
     * @return a new StoryEntry with updated duration
     */
    public StoryEntry withDuration(long newDuration) {
        return new StoryEntry(
                status, commitSha, phase, newDuration, retries,
                blockedBy, summary, findingsCount,
                tasks, parentBranch
        );
    }

    /**
     * Returns a copy with the given blocked-by list.
     *
     * @param newBlockedBy the blocking story IDs
     * @return a new StoryEntry with updated blockedBy
     */
    public StoryEntry withBlockedBy(List<String> newBlockedBy) {
        return new StoryEntry(
                status, commitSha, phase, duration, retries,
                newBlockedBy, summary, findingsCount,
                tasks, parentBranch
        );
    }

    /**
     * Returns a copy with the given summary.
     *
     * @param newSummary the execution summary
     * @return a new StoryEntry with updated summary
     */
    public StoryEntry withSummary(String newSummary) {
        return new StoryEntry(
                status, commitSha, phase, duration, retries,
                blockedBy, newSummary, findingsCount,
                tasks, parentBranch
        );
    }

    /**
     * Returns a copy with the given findings count.
     *
     * @param newFindingsCount the number of issues found
     * @return a new StoryEntry with updated findingsCount
     */
    public StoryEntry withFindingsCount(int newFindingsCount) {
        return new StoryEntry(
                status, commitSha, phase, duration, retries,
                blockedBy, summary, newFindingsCount,
                tasks, parentBranch
        );
    }

    /**
     * Returns a copy with the given tasks map.
     *
     * @param newTasks the per-task state map
     * @return a new StoryEntry with updated tasks
     */
    public StoryEntry withTasks(Map<String, TaskEntry> newTasks) {
        return new StoryEntry(
                status, commitSha, phase, duration, retries,
                blockedBy, summary, findingsCount,
                newTasks, parentBranch
        );
    }

    /**
     * Returns a copy with a single task updated.
     *
     * @param taskId the task ID to update
     * @param entry  the new task entry
     * @return a new StoryEntry with the task updated
     */
    public StoryEntry withTask(
            String taskId, TaskEntry entry) {
        var updated =
                new java.util.LinkedHashMap<>(tasks);
        updated.put(taskId, entry);
        return new StoryEntry(
                status, commitSha, phase, duration, retries,
                blockedBy, summary, findingsCount,
                updated, parentBranch
        );
    }

    /**
     * Returns a copy with the given parent branch.
     *
     * @param newParentBranch the parent branch name
     * @return a new StoryEntry with updated parentBranch
     */
    public StoryEntry withParentBranch(String newParentBranch) {
        return new StoryEntry(
                status, commitSha, phase, duration, retries,
                blockedBy, summary, findingsCount,
                tasks, newParentBranch
        );
    }
}
