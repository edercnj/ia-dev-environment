package dev.iadev.release.validate;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/**
 * Dispatches independent validation checks in parallel.
 *
 * <p>Core executor for the VALIDATE-DEEP parallelization
 * (story-0039-0004). Given a list of {@link CheckSpec}, runs
 * each on a worker thread bounded by the configured
 * {@code maxParallel} (default {@code min(CPU,4)}), captures
 * exit code, duration and diagnostic detail per check, and
 * returns an {@link AggregatedResult} sorted by severity.</p>
 *
 * <p>Preserves every {@code VALIDATE_*} error code (RULE-005).
 * Thread-safe: each invocation of {@link #execute(List)}
 * provisions its own {@link ExecutorService} and shuts it down
 * before returning, so instances are reusable.</p>
 */
public final class ParallelCheckExecutor {

    private static final int MIN_PARALLEL = 1;
    private static final int MAX_PARALLEL = 16;

    private final int maxParallel;

    /**
     * @param maxParallel bound on worker thread count, 1..16
     * @throws IllegalArgumentException if out of bounds
     */
    public ParallelCheckExecutor(int maxParallel) {
        if (maxParallel < MIN_PARALLEL || maxParallel > MAX_PARALLEL) {
            throw new IllegalArgumentException(
                    "max-parallel must be between "
                            + MIN_PARALLEL + " and "
                            + MAX_PARALLEL + ", got " + maxParallel);
        }
        this.maxParallel = maxParallel;
    }

    /**
     * Dispatches the given checks in parallel and returns the
     * aggregated result. Returns an empty result for an empty
     * input.
     *
     * @param specs list of checks to run (non-null, possibly empty)
     * @return aggregated result; never {@code null}
     */
    public AggregatedResult execute(List<CheckSpec> specs) {
        if (specs.isEmpty()) {
            return new AggregatedResult(List.of());
        }
        int pool = Math.min(maxParallel, specs.size());
        ExecutorService executor = Executors.newFixedThreadPool(pool);
        try {
            return runAndAggregate(executor, specs);
        } finally {
            executor.shutdownNow();
        }
    }

    private AggregatedResult runAndAggregate(
            ExecutorService executor,
            List<CheckSpec> specs) {
        List<Future<CheckResult>> futures = new ArrayList<>(specs.size());
        for (CheckSpec spec : specs) {
            futures.add(executor.submit(() -> runOne(spec)));
        }
        List<CheckResult> results = new ArrayList<>(specs.size());
        for (Future<CheckResult> future : futures) {
            results.add(awaitResult(future));
        }
        return new AggregatedResult(results);
    }

    private static CheckResult runOne(CheckSpec spec) {
        long start = System.nanoTime();
        try {
            CheckOutcome outcome = spec.runner().call();
            Duration elapsed = Duration.ofNanos(System.nanoTime() - start);
            Severity severity = outcome.failed()
                    ? Severity.FAIL
                    : Severity.PASS;
            return new CheckResult(
                    spec.name(),
                    spec.failCode(),
                    severity,
                    elapsed,
                    outcome.detail());
        } catch (Exception e) {
            Duration elapsed = Duration.ofNanos(System.nanoTime() - start);
            String message = e.getMessage();
            String detail = message != null
                    ? message
                    : e.getClass().getSimpleName();
            return new CheckResult(
                    spec.name(),
                    spec.failCode(),
                    Severity.FAIL,
                    elapsed,
                    detail);
        }
    }

    private static CheckResult awaitResult(Future<CheckResult> future) {
        try {
            // runOne catches every Exception so ExecutionException is
            // unexpected here, but we still distinguish the two cases:
            // InterruptedException restores the interrupt flag; an
            // ExecutionException (defensive branch) propagates without
            // marking this thread as interrupted.
            return future.get();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(
                    "Interrupted while awaiting check result", e);
        } catch (ExecutionException e) {
            Throwable cause = e.getCause() != null ? e.getCause() : e;
            throw new IllegalStateException(
                    "Check execution failed unexpectedly: "
                            + cause.getMessage(), cause);
        }
    }
}
