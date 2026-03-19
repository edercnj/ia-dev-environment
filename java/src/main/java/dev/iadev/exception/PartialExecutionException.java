package dev.iadev.exception;

/**
 * Thrown when a story executes partially (neither full success nor complete failure).
 *
 * <p>Carries the story ID to identify which story had partial execution.</p>
 *
 * <p>Example usage:
 * <pre>{@code
 * throw new PartialExecutionException(
 *     "Story completed 3 of 5 tasks", "story-0006-0010");
 * }</pre>
 */
public class PartialExecutionException extends RuntimeException {

    private final String storyId;

    /**
     * Creates a partial execution exception for a specific story.
     *
     * @param message description of the partial execution
     * @param storyId the ID of the story with partial execution
     */
    public PartialExecutionException(
            String message, String storyId) {
        super(message);
        this.storyId = storyId;
    }

    /**
     * Returns the ID of the story that had partial execution.
     *
     * @return the story ID
     */
    public String getStoryId() {
        return storyId;
    }

    @Override
    public String toString() {
        return "PartialExecutionException{message='%s', storyId='%s'}"
                .formatted(getMessage(), storyId);
    }
}
