package dev.iadev.release.validate;

import java.time.Duration;
import java.util.Objects;

/**
 * Result of a single executed check.
 *
 * <p>Immutable aggregate of the check's name, preserved
 * error code, resolved severity, wall-clock duration, and
 * diagnostic detail. Produced by
 * {@link ParallelCheckExecutor} for every dispatched
 * {@link CheckSpec}.</p>
 */
public record CheckResult(
        String name,
        String failCode,
        Severity severity,
        Duration duration,
        String detail) {

    public CheckResult {
        Objects.requireNonNull(name, "name");
        Objects.requireNonNull(failCode, "failCode");
        Objects.requireNonNull(severity, "severity");
        Objects.requireNonNull(duration, "duration");
        Objects.requireNonNull(detail, "detail");
    }

    public boolean isFailure() {
        return severity == Severity.FAIL;
    }
}
