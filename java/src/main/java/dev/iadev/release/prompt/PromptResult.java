package dev.iadev.release.prompt;

/**
 * Result of a {@link PromptEngine#resolve} call, carrying
 * the resolved action plus optional exit metadata.
 *
 * @param action    resolved action
 * @param exitCode  process exit code (0 for normal, 2 for abort)
 * @param errorCode error code string (null when no error)
 */
public record PromptResult(
        PromptAction action,
        int exitCode,
        String errorCode) {

    /** Convenience factory for non-error results. */
    public static PromptResult of(PromptAction action) {
        return new PromptResult(action, 0, null);
    }

    /** Factory for abort with error code. */
    public static PromptResult abort(
            int exitCode, String errorCode) {
        return new PromptResult(
                PromptAction.ABORT, exitCode, errorCode);
    }
}
