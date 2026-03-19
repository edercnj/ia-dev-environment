package dev.iadev.checkpoint;

import java.util.List;

/**
 * Represents the state of an individual story during execution.
 *
 * <p>Tracks status, commit information, timing, retries, dependencies,
 * and review findings. All fields are immutable; create a new instance
 * to update state.</p>
 *
 * @param status        current lifecycle status
 * @param commitSha     SHA of the commit if completed (nullable)
 * @param phase         execution phase (0-based)
 * @param duration      duration in milliseconds
 * @param retries       number of retry attempts
 * @param blockedBy     IDs of stories blocking this one
 * @param summary       execution summary (nullable)
 * @param findingsCount number of issues found during review
 */
public record StoryEntry(
        StoryStatus status,
        String commitSha,
        int phase,
        long duration,
        int retries,
        List<String> blockedBy,
        String summary,
        int findingsCount
) {

    /**
     * Creates a StoryEntry with default values for optional fields.
     *
     * @param status the initial status
     * @param phase  the execution phase
     * @return a new StoryEntry in initial state
     */
    public static StoryEntry pending(int phase) {
        return new StoryEntry(
                StoryStatus.PENDING, null, phase, 0L, 0,
                List.of(), null, 0
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
                blockedBy, summary, findingsCount
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
                blockedBy, summary, findingsCount
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
                blockedBy, summary, findingsCount
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
                blockedBy, summary, findingsCount
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
                List.copyOf(newBlockedBy), summary, findingsCount
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
                blockedBy, newSummary, findingsCount
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
                blockedBy, summary, newFindingsCount
        );
    }
}
