package dev.iadev.checkpoint;

import java.time.Instant;

/**
 * Represents the result of an integrity gate verification.
 *
 * <p>Gates validate compilation, test passing, code coverage, and other
 * quality criteria at phase boundaries.</p>
 *
 * @param gateName  name of the gate (e.g., "compilation", "tests", "coverage")
 * @param passed    whether the gate passed
 * @param message   detail message (nullable)
 * @param timestamp moment of verification
 */
public record IntegrityGateEntry(
        String gateName,
        boolean passed,
        String message,
        Instant timestamp
) {

    /**
     * Creates a passing gate entry with the current timestamp.
     *
     * @param gateName the gate name
     * @return a new passing IntegrityGateEntry
     */
    public static IntegrityGateEntry pass(String gateName) {
        return new IntegrityGateEntry(
                gateName, true, null, Instant.now()
        );
    }

    /**
     * Creates a failing gate entry with a message and current timestamp.
     *
     * @param gateName the gate name
     * @param message  the failure detail
     * @return a new failing IntegrityGateEntry
     */
    public static IntegrityGateEntry fail(
            String gateName, String message) {
        return new IntegrityGateEntry(
                gateName, false, message, Instant.now()
        );
    }
}
