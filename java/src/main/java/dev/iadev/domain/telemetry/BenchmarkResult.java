package dev.iadev.domain.telemetry;

import java.util.List;
import java.util.Objects;

/**
 * Sealed hierarchy representing the outcome of
 * {@link BenchmarkAnalyzer#analyze} (story-0039-0012 §3.3).
 *
 * <p>Either the analyzer had enough history to compute a
 * {@link TopPhases} list, or the input contained fewer
 * than {@link BenchmarkAnalyzer#MIN_HISTORY} releases and
 * the analyzer returns {@link InsufficientHistory}.
 */
public sealed interface BenchmarkResult
        permits BenchmarkResult.TopPhases,
                BenchmarkResult.InsufficientHistory {

    /** Successful computation of the top-3 slowest phases. */
    record TopPhases(List<PhaseBenchmark> entries)
            implements BenchmarkResult {
        public TopPhases {
            Objects.requireNonNull(entries, "entries");
            entries = List.copyOf(entries);
        }
    }

    /**
     * Sentinel returned when the JSONL contains fewer
     * releases than {@link BenchmarkAnalyzer#MIN_HISTORY}.
     */
    record InsufficientHistory(int releasesObserved)
            implements BenchmarkResult {
        public InsufficientHistory {
            if (releasesObserved < 0) {
                throw new IllegalArgumentException(
                        "releasesObserved must be >= 0");
            }
        }
    }
}
