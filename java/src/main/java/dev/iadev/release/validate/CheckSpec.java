package dev.iadev.release.validate;

import java.util.Objects;
import java.util.concurrent.Callable;

/**
 * Specification of a single validation check to run.
 *
 * <p>Carries the display name, the preserved {@code VALIDATE_*}
 * error code (RULE-005), and the runner callable that produces
 * a {@link CheckOutcome} when invoked. The runner is deliberately
 * a {@link Callable} so the executor can dispatch it on a worker
 * thread without coupling to the check's implementation.</p>
 *
 * @param name     short human-readable identifier (e.g. {@code coverage_line})
 * @param failCode error code emitted when this check fails
 * @param runner   callable that produces the outcome
 */
public record CheckSpec(
        String name,
        String failCode,
        Callable<CheckOutcome> runner) {

    public CheckSpec {
        Objects.requireNonNull(name, "name");
        Objects.requireNonNull(failCode, "failCode");
        Objects.requireNonNull(runner, "runner");
    }
}
