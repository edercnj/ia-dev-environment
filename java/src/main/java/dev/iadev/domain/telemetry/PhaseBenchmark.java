package dev.iadev.domain.telemetry;

import java.util.Objects;

/**
 * A single entry of the top-3 slowest phases, including
 * this release's duration, the historical mean, and the
 * delta percentage vs. that mean (story-0039-0012 §5.2).
 *
 * <p>{@code deltaPercent} is a signed integer: positive
 * means this release is slower than the mean, negative
 * means faster. The mean is computed over at least
 * {@link BenchmarkAnalyzer#MIN_HISTORY} historical
 * releases.
 */
public record PhaseBenchmark(
        String phase,
        long durationSec,
        long meanSec,
        int deltaPercent) {

    public PhaseBenchmark {
        Objects.requireNonNull(phase, "phase");
        if (phase.isBlank()) {
            throw new IllegalArgumentException(
                    "phase must not be blank");
        }
        if (durationSec < 0) {
            throw new IllegalArgumentException(
                    "durationSec must be >= 0");
        }
        if (meanSec < 0) {
            throw new IllegalArgumentException(
                    "meanSec must be >= 0");
        }
    }
}
