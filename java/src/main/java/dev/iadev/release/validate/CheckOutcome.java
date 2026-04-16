package dev.iadev.release.validate;

import java.util.Objects;

/**
 * Raw outcome of running a single check.
 *
 * <p>Immutable carrier of the exit code (0 = success) and a
 * human-readable detail string (typically the error message
 * or diagnostic output). Used as the return type of
 * {@link CheckSpec#runner()}.</p>
 *
 * @param exitCode shell-style exit code (0 pass, non-zero fail)
 * @param detail   diagnostic detail, may be empty
 */
public record CheckOutcome(int exitCode, String detail) {

    public CheckOutcome {
        Objects.requireNonNull(detail, "detail");
    }

    public boolean failed() {
        return exitCode != 0;
    }
}
