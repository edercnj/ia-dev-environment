package dev.iadev.release;

import java.util.Objects;

/**
 * Domain exception raised when a version-detection operation violates the
 * auto-version contract documented in story-0039-0001 §5.3.
 *
 * <p>Each instance carries an {@link Code} so the CLI can map it to the
 * documented exit code / error banner without inspecting the message.</p>
 */
public final class InvalidBumpException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    /** Error codes matching the CLI contract in story-0039-0001 §5.3. */
    public enum Code {
        /** {@code --version} argument does not match the SemVer grammar. */
        VERSION_INVALID_FORMAT,
        /**
         * No commits since the last tag classify as feat/fix/perf/breaking
         * (only ignored types — docs, chore, etc.).
         */
        VERSION_NO_BUMP_SIGNAL,
        /** {@code git log <tag>..HEAD} returned zero commits. */
        VERSION_NO_COMMITS,
        /**
         * Attempted operation does not form a valid bump on the source version
         * (e.g., pre-release downgrade, currently unused but reserved for
         * richer rules).
         */
        INVALID_BUMP_COMBINATION
    }

    private final Code code;

    public InvalidBumpException(Code code, String message) {
        super(message);
        this.code = Objects.requireNonNull(code, "code");
    }

    public Code code() {
        return code;
    }
}
