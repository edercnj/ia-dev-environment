package dev.iadev.telemetry.trend;

/**
 * Baseline aggregation strategy for {@link RegressionDetector}. Selected by
 * the {@code --baseline} CLI flag.
 *
 * <ul>
 *   <li>{@link #MEAN} — arithmetic mean of the historical P95 samples. Simple
 *       but sensitive to outliers.</li>
 *   <li>{@link #MEDIAN} — median of the historical P95 samples. Robust
 *       against a single outlier epic (Gherkin scenario
 *       "Baseline median estável contra outlier").</li>
 * </ul>
 */
public enum BaselineStrategy {

    /** Arithmetic mean of the historical P95 samples. */
    MEAN,

    /** Median of the historical P95 samples. Outlier-resistant. */
    MEDIAN;

    /**
     * Parses a CLI-friendly value (case-insensitive) into an enum instance.
     *
     * @param value {@code mean} or {@code median}
     * @return the parsed strategy
     * @throws IllegalArgumentException if {@code value} does not match either
     *                                  enum constant
     */
    public static BaselineStrategy parse(String value) {
        if (value == null) {
            throw new IllegalArgumentException(
                    "--baseline value is required");
        }
        String normalized = value.trim().toLowerCase();
        return switch (normalized) {
            case "mean" -> MEAN;
            case "median" -> MEDIAN;
            default -> throw new IllegalArgumentException(
                    "--baseline must be 'mean' or 'median', got '"
                            + value + "'");
        };
    }
}
