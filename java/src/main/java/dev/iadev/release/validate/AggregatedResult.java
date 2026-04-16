package dev.iadev.release.validate;

import java.util.Comparator;
import java.util.List;
import java.util.Objects;

/**
 * Aggregated outcome of a parallel check execution.
 *
 * <p>Holds the list of {@link CheckResult} in dispatch order
 * and exposes a {@link #sorted()} view ordered by severity
 * (FAIL &lt; WARN &lt; PASS) with alphabetic fail-code as
 * secondary key. The first failure code is the canonical
 * abort signal reported by the orchestrating skill.</p>
 */
public record AggregatedResult(List<CheckResult> results) {

    private static final Comparator<CheckResult> BY_SEVERITY_THEN_CODE =
            Comparator.comparing(CheckResult::severity)
                    .thenComparing(CheckResult::failCode);

    public AggregatedResult {
        Objects.requireNonNull(results, "results");
        results = List.copyOf(results);
    }

    public boolean hasFailures() {
        return results.stream().anyMatch(CheckResult::isFailure);
    }

    public String firstFailureCode() {
        return sorted().stream()
                .filter(CheckResult::isFailure)
                .map(CheckResult::failCode)
                .findFirst()
                .orElse(null);
    }

    public List<CheckResult> sorted() {
        return results.stream()
                .sorted(BY_SEVERITY_THEN_CODE)
                .toList();
    }
}
