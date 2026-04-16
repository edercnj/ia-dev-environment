package dev.iadev.release.prompt;

import java.time.Instant;

/**
 * Port providing the current time. Injected into
 * {@link PromptEngine} to enable deterministic testing.
 */
@FunctionalInterface
public interface ClockPort {

    /**
     * Returns the current instant (UTC).
     *
     * @return current instant
     */
    Instant now();
}
