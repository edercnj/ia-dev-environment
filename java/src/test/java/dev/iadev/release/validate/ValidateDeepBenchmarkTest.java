package dev.iadev.release.validate;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Benchmark asserting the parallel VALIDATE-DEEP flow delivers
 * at least the required wall-clock reduction vs a serialized
 * baseline on the same fixture.
 *
 * <p>Story-0039-0004 DoD: <em>benchmark documentado mostrando
 * &gt;=40% redução</em>. This test enforces the assertion
 * programmatically so regressions are caught in CI.</p>
 *
 * <p>Uses a 7-check fixture mirroring the VALIDATE-DEEP
 * parallel wave (coverage_line, coverage_branch, golden,
 * hardcoded_version, version_match, generation_drift, plus a
 * synthetic 7th) with stubbed 120 ms latency per check. To
 * blunt CI jitter the benchmark runs 5 rounds and takes the
 * median wall-clock per mode.</p>
 */
final class ValidateDeepBenchmarkTest {

    private static final int CHECK_LATENCY_MILLIS = 120;
    private static final int NUM_CHECKS = 7;
    private static final int ROUNDS = 5;
    private static final double REDUCTION_THRESHOLD = 0.40;

    @Test
    void parallel_isAtLeast40PercentFasterThanSerial() {
        List<CheckSpec> checks = buildFixture();

        long medianSerial = medianWallClock(
                checks, new ParallelCheckExecutor(1));
        long medianParallel = medianWallClock(
                checks, new ParallelCheckExecutor(4));

        double reduction = 1.0
                - ((double) medianParallel / (double) medianSerial);

        assertThat(reduction)
                .as("parallel wall-clock (median=%d ms) vs serial"
                        + " (median=%d ms) must be >=40%% faster; "
                        + "observed reduction=%.3f",
                        medianParallel, medianSerial, reduction)
                .isGreaterThanOrEqualTo(REDUCTION_THRESHOLD);
    }

    @Test
    void parallel_wallClock_lessThanSumOfCheckDurations() {
        List<CheckSpec> checks = buildFixture();
        ParallelCheckExecutor executor = new ParallelCheckExecutor(4);

        long start = System.nanoTime();
        AggregatedResult result = executor.execute(checks);
        Duration wallClock = Duration.ofNanos(
                System.nanoTime() - start);

        long sumDurations = result.results().stream()
                .mapToLong(r -> r.duration().toMillis())
                .sum();
        assertThat(wallClock.toMillis()).isLessThan(sumDurations);
    }

    private static long medianWallClock(
            List<CheckSpec> checks,
            ParallelCheckExecutor executor) {
        long[] samples = new long[ROUNDS];
        for (int i = 0; i < ROUNDS; i++) {
            long start = System.nanoTime();
            executor.execute(checks);
            samples[i] = (System.nanoTime() - start) / 1_000_000L;
        }
        java.util.Arrays.sort(samples);
        return samples[ROUNDS / 2];
    }

    private static List<CheckSpec> buildFixture() {
        List<CheckSpec> checks = new ArrayList<>(NUM_CHECKS);
        String[] names = {
                "coverage_line",
                "coverage_branch",
                "golden_files",
                "hardcoded_version",
                "version_match",
                "generation_drift",
                "cross_file_consistency"
        };
        String[] codes = {
                "VALIDATE_COVERAGE_LINE",
                "VALIDATE_COVERAGE_BRANCH",
                "VALIDATE_GOLDEN_DRIFT",
                "VALIDATE_HARDCODED_VERSION",
                "VALIDATE_VERSION_MISMATCH",
                "VALIDATE_GENERATION_DRIFT",
                "VALIDATE_CROSS_FILE_CONSISTENCY"
        };
        for (int i = 0; i < NUM_CHECKS; i++) {
            checks.add(new CheckSpec(
                    names[i],
                    codes[i],
                    sleepingRunner(CHECK_LATENCY_MILLIS)));
        }
        return checks;
    }

    // intentional workload simulation for timing test; not a sync
    // primitive. Each check's `Thread.sleep(millis)` models a fixed
    // per-check latency so the benchmark can measure parallel-vs-
    // serial wall-clock reduction (≥ 40% threshold). Replacing the
    // sleep with Awaitility would defeat the purpose — we WANT the
    // thread to block for a known duration, not wait for a signal.
    private static Callable<CheckOutcome> sleepingRunner(long millis) {
        return () -> {
            try {
                Thread.sleep(millis);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new RuntimeException(e);
            }
            return new CheckOutcome(0, "");
        };
    }
}
