package dev.iadev.application.assembler;

import java.util.Objects;

/**
 * Measures elapsed time (in milliseconds) around a
 * {@link Runnable} action using {@link System#nanoTime()}.
 *
 * <p>Centralises the timing responsibility previously
 * inlined in {@link AssemblerPipeline}. Contains no
 * filtering, I/O, or pipeline orchestration logic.</p>
 *
 * @see AssemblerPipeline
 */
public final class PipelineTimer {

    private static final long NANOS_PER_MILLI = 1_000_000L;

    /**
     * Creates a new timer.
     */
    public PipelineTimer() {
        // stateless; instance allows future dependency
        // injection of a clock source.
    }

    /**
     * Runs the given action and returns the elapsed time
     * in milliseconds.
     *
     * <p>The action is executed exactly once. If the
     * action throws, the exception propagates unchanged
     * and no duration is returned.</p>
     *
     * @param action the action to time (never null)
     * @return elapsed time in milliseconds (never
     *         negative)
     */
    public long time(Runnable action) {
        Objects.requireNonNull(action, "action");
        long start = start();
        action.run();
        return stop(start);
    }

    /**
     * Captures the current nanosecond clock as a
     * stopwatch start marker.
     *
     * @return the current {@link System#nanoTime()} value
     */
    public long start() {
        return System.nanoTime();
    }

    /**
     * Returns the elapsed time in milliseconds since the
     * given start marker.
     *
     * @param startNanos a value previously returned by
     *                   {@link #start()}
     * @return elapsed time in milliseconds (never
     *         negative)
     */
    public long stop(long startNanos) {
        return toMillis(System.nanoTime() - startNanos);
    }

    /**
     * Converts a nanosecond delta into milliseconds,
     * clamping negatives to zero.
     *
     * @param nanos a non-negative elapsed duration in
     *              nanoseconds
     * @return elapsed duration in milliseconds
     */
    static long toMillis(long nanos) {
        long ms = nanos / NANOS_PER_MILLI;
        return ms < 0 ? 0 : ms;
    }
}
