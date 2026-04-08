package dev.iadev.checkpoint;

import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Pattern;

/**
 * Represents the state of an individual task during story execution.
 *
 * <p>Tracks status, PR information, branch, timing, retries,
 * and failure details. All fields are immutable; use {@code with*}
 * methods to create updated copies.</p>
 *
 * @param taskId        task identifier in TASK-XXXX-YYYY-NNN format
 * @param status        current lifecycle status
 * @param prUrl         URL of the pull request (nullable)
 * @param prNumber      number of the pull request (nullable)
 * @param branch        Git branch name (nullable)
 * @param startedAt     timestamp of task start (nullable)
 * @param completedAt   timestamp of task completion (nullable)
 * @param attempts      number of execution attempts
 * @param failureReason reason for last failure (nullable)
 */
public record TaskEntry(
        String taskId,
        TaskStatus status,
        String prUrl,
        Integer prNumber,
        String branch,
        Instant startedAt,
        Instant completedAt,
        int attempts,
        String failureReason
) {

    private static final Pattern TASK_ID_PATTERN =
            Pattern.compile("^TASK-\\d{4}-\\d{4}-\\d{3}$");

    /**
     * Creates a pending TaskEntry for the given task ID.
     *
     * @param taskId the task identifier
     * @return a new TaskEntry in PENDING state
     */
    public static TaskEntry pending(String taskId) {
        return new TaskEntry(
                taskId, TaskStatus.PENDING,
                null, null, null,
                null, null, 0, null
        );
    }

    /**
     * Validates whether the given task ID matches the expected format.
     *
     * @param taskId the task ID to validate
     * @return true if format is TASK-XXXX-YYYY-NNN
     */
    public static boolean isValidTaskId(String taskId) {
        return taskId != null
                && TASK_ID_PATTERN.matcher(taskId).matches();
    }

    /**
     * Validates the task ID format and throws if invalid.
     *
     * @param taskId the task ID to validate
     * @throws IllegalArgumentException if format is invalid
     */
    public static void validateTaskId(String taskId) {
        if (!isValidTaskId(taskId)) {
            throw new IllegalArgumentException(
                    "Invalid task ID '%s': expected format TASK-XXXX-YYYY-NNN"
                            .formatted(taskId)
            );
        }
    }

    /**
     * Extracts components from a valid task ID.
     *
     * @param taskId the task ID to parse
     * @return map with keys: epicId, storyId, sequential
     * @throws IllegalArgumentException if format is invalid
     */
    public static Map<String, String> parseTaskId(String taskId) {
        validateTaskId(taskId);
        var parts = taskId.split("-");
        return Map.of(
                "epicId", parts[1],
                "storyId", parts[2],
                "sequential", parts[3]
        );
    }

    /** @return the PR URL as Optional */
    public Optional<String> optionalPrUrl() {
        return Optional.ofNullable(prUrl);
    }

    /** @return the PR number as Optional */
    public Optional<Integer> optionalPrNumber() {
        return Optional.ofNullable(prNumber);
    }

    /** @return the branch as Optional */
    public Optional<String> optionalBranch() {
        return Optional.ofNullable(branch);
    }

    /** @return the start timestamp as Optional */
    public Optional<Instant> optionalStartedAt() {
        return Optional.ofNullable(startedAt);
    }

    /** @return the completion timestamp as Optional */
    public Optional<Instant> optionalCompletedAt() {
        return Optional.ofNullable(completedAt);
    }

    /** @return the failure reason as Optional */
    public Optional<String> optionalFailureReason() {
        return Optional.ofNullable(failureReason);
    }

    /**
     * Returns a copy with the given status.
     *
     * @param newStatus the new status
     * @return a new TaskEntry with updated status
     */
    public TaskEntry withStatus(TaskStatus newStatus) {
        return new TaskEntry(
                taskId, newStatus, prUrl, prNumber, branch,
                startedAt, completedAt, attempts, failureReason
        );
    }

    /**
     * Returns a copy with the given PR URL.
     *
     * @param newPrUrl the PR URL
     * @return a new TaskEntry with updated prUrl
     */
    public TaskEntry withPrUrl(String newPrUrl) {
        return new TaskEntry(
                taskId, status, newPrUrl, prNumber, branch,
                startedAt, completedAt, attempts, failureReason
        );
    }

    /**
     * Returns a copy with the given PR number.
     *
     * @param newPrNumber the PR number
     * @return a new TaskEntry with updated prNumber
     */
    public TaskEntry withPrNumber(Integer newPrNumber) {
        return new TaskEntry(
                taskId, status, prUrl, newPrNumber, branch,
                startedAt, completedAt, attempts, failureReason
        );
    }

    /**
     * Returns a copy with the given branch.
     *
     * @param newBranch the branch name
     * @return a new TaskEntry with updated branch
     */
    public TaskEntry withBranch(String newBranch) {
        return new TaskEntry(
                taskId, status, prUrl, prNumber, newBranch,
                startedAt, completedAt, attempts, failureReason
        );
    }

    /**
     * Returns a copy with the given start timestamp.
     *
     * @param newStartedAt the start timestamp
     * @return a new TaskEntry with updated startedAt
     */
    public TaskEntry withStartedAt(Instant newStartedAt) {
        return new TaskEntry(
                taskId, status, prUrl, prNumber, branch,
                newStartedAt, completedAt, attempts, failureReason
        );
    }

    /**
     * Returns a copy with the given completion timestamp.
     *
     * @param newCompletedAt the completion timestamp
     * @return a new TaskEntry with updated completedAt
     */
    public TaskEntry withCompletedAt(Instant newCompletedAt) {
        return new TaskEntry(
                taskId, status, prUrl, prNumber, branch,
                startedAt, newCompletedAt, attempts, failureReason
        );
    }

    /**
     * Returns a copy with the given attempt count.
     *
     * @param newAttempts the attempt count
     * @return a new TaskEntry with updated attempts
     */
    public TaskEntry withAttempts(int newAttempts) {
        return new TaskEntry(
                taskId, status, prUrl, prNumber, branch,
                startedAt, completedAt, newAttempts, failureReason
        );
    }

    /**
     * Returns a copy with the given failure reason.
     *
     * @param newFailureReason the failure reason
     * @return a new TaskEntry with updated failureReason
     */
    public TaskEntry withFailureReason(String newFailureReason) {
        return new TaskEntry(
                taskId, status, prUrl, prNumber, branch,
                startedAt, completedAt, attempts, newFailureReason
        );
    }
}
