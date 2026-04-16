package dev.iadev.release.abort;

import java.util.List;

/**
 * Result of a {@code --abort} command invocation.
 *
 * @param exitCode  process exit code (0 = success, 1 = error,
 *                  2 = user cancelled)
 * @param output    human-readable output to display
 * @param errorCode optional error code (null when successful)
 * @param warnings  list of non-fatal warnings
 *                  (warn-only cleanup failures)
 */
public record AbortResult(
        int exitCode,
        String output,
        String errorCode,
        List<String> warnings) {

    /** Convenience factory for successful abort. */
    public static AbortResult success(
            String output, List<String> warnings) {
        return new AbortResult(0, output, null, warnings);
    }

    /** Convenience factory for user cancellation. */
    public static AbortResult cancelled(String output) {
        return new AbortResult(
                2, output, "ABORT_USER_CANCELLED",
                List.of());
    }

    /** Convenience factory for error results. */
    public static AbortResult error(
            String output, String errorCode) {
        return new AbortResult(
                1, output, errorCode, List.of());
    }
}
