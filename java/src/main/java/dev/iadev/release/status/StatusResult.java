package dev.iadev.release.status;

import java.util.Objects;

/**
 * Result of a {@code --status} command invocation.
 *
 * @param exitCode process exit code (0 = success, 1 = error)
 * @param output   human-readable output to display
 * @param errorCode optional error code (null when successful)
 */
public record StatusResult(
        int exitCode,
        String output,
        String errorCode) {

    public StatusResult {
        Objects.requireNonNull(output, "output");
    }

    /** Convenience factory for successful results. */
    public static StatusResult success(String output) {
        return new StatusResult(0, output, null);
    }

    /** Convenience factory for error results. */
    public static StatusResult error(
            String output, String errorCode) {
        return new StatusResult(1, output, errorCode);
    }
}
