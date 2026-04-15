package dev.iadev.release.validate;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Tests for {@link ParallelCheckExecutor}.
 *
 * <p>TPP-ordered: degenerate (empty) -> scalar (single) ->
 * collection (multi) -> boundary (max-parallel validation) ->
 * ordering (severity sort).</p>
 */
final class ParallelCheckExecutorTest {

    @Test
    void execute_emptyList_returnsEmptyAggregatedResult() {
        ParallelCheckExecutor executor = new ParallelCheckExecutor(4);

        AggregatedResult result = executor.execute(List.of());

        assertThat(result.results()).isEmpty();
        assertThat(result.hasFailures()).isFalse();
        assertThat(result.firstFailureCode()).isNull();
    }

    @Test
    void execute_singlePassingCheck_returnsPassResult() {
        ParallelCheckExecutor executor = new ParallelCheckExecutor(4);
        CheckSpec spec = new CheckSpec(
                "coverage_line",
                "VALIDATE_COVERAGE_LINE",
                () -> new CheckOutcome(0, ""));

        AggregatedResult result = executor.execute(List.of(spec));

        assertThat(result.results()).hasSize(1);
        CheckResult single = result.results().get(0);
        assertThat(single.name()).isEqualTo("coverage_line");
        assertThat(single.severity()).isEqualTo(Severity.PASS);
        assertThat(single.duration()).isNotNull();
        assertThat(result.hasFailures()).isFalse();
    }

    @Test
    void execute_singleFailingCheck_returnsFailWithCode() {
        ParallelCheckExecutor executor = new ParallelCheckExecutor(4);
        CheckSpec spec = new CheckSpec(
                "golden_files",
                "VALIDATE_GOLDEN_DRIFT",
                () -> new CheckOutcome(1, "drift detected"));

        AggregatedResult result = executor.execute(List.of(spec));

        assertThat(result.hasFailures()).isTrue();
        assertThat(result.firstFailureCode())
                .isEqualTo("VALIDATE_GOLDEN_DRIFT");
        assertThat(result.results().get(0).severity())
                .isEqualTo(Severity.FAIL);
    }

    @Test
    void execute_multipleChecks_allReportCapturedIndependently() {
        ParallelCheckExecutor executor = new ParallelCheckExecutor(4);
        CheckSpec pass1 = new CheckSpec(
                "coverage_line",
                "VALIDATE_COVERAGE_LINE",
                () -> new CheckOutcome(0, ""));
        CheckSpec pass2 = new CheckSpec(
                "hardcoded_version",
                "VALIDATE_HARDCODED_VERSION",
                () -> new CheckOutcome(0, ""));
        CheckSpec fail1 = new CheckSpec(
                "golden_files",
                "VALIDATE_GOLDEN_DRIFT",
                () -> new CheckOutcome(1, "drift"));

        AggregatedResult result = executor.execute(
                List.of(pass1, pass2, fail1));

        assertThat(result.results()).hasSize(3);
        assertThat(result.hasFailures()).isTrue();
    }

    @Test
    void execute_multipleFailures_sortsAlphabeticallyByCode() {
        ParallelCheckExecutor executor = new ParallelCheckExecutor(4);
        CheckSpec failZ = new CheckSpec(
                "hardcoded_version",
                "VALIDATE_HARDCODED_VERSION",
                () -> new CheckOutcome(1, "zzz"));
        CheckSpec failA = new CheckSpec(
                "golden_files",
                "VALIDATE_GOLDEN_DRIFT",
                () -> new CheckOutcome(1, "aaa"));

        AggregatedResult result = executor.execute(
                List.of(failZ, failA));

        // FAIL severity, alphabetic secondary sort
        assertThat(result.firstFailureCode())
                .isEqualTo("VALIDATE_GOLDEN_DRIFT");
    }

    @Test
    void execute_mixedSeverities_sortsFailBeforePass() {
        ParallelCheckExecutor executor = new ParallelCheckExecutor(4);
        CheckSpec passCheck = new CheckSpec(
                "coverage_line",
                "VALIDATE_COVERAGE_LINE",
                () -> new CheckOutcome(0, ""));
        CheckSpec failCheck = new CheckSpec(
                "golden_files",
                "VALIDATE_GOLDEN_DRIFT",
                () -> new CheckOutcome(1, "drift"));

        AggregatedResult result = executor.execute(
                List.of(passCheck, failCheck));

        List<CheckResult> sorted = result.sorted();
        assertThat(sorted.get(0).severity()).isEqualTo(Severity.FAIL);
        assertThat(sorted.get(1).severity()).isEqualTo(Severity.PASS);
    }

    @Test
    void construct_maxParallelBelowMinimum_throws() {
        assertThatThrownBy(() -> new ParallelCheckExecutor(0))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("max-parallel");
    }

    @Test
    void construct_maxParallelAboveMaximum_throws() {
        assertThatThrownBy(() -> new ParallelCheckExecutor(17))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("max-parallel");
    }

    @Test
    void construct_maxParallelAtBounds_accepted() {
        new ParallelCheckExecutor(1);
        new ParallelCheckExecutor(16);
    }

    @Test
    void execute_wallClockLowerThanSumOfDurations_whenParallel() {
        ParallelCheckExecutor executor = new ParallelCheckExecutor(4);
        CheckSpec slow1 = new CheckSpec(
                "slow1",
                "VALIDATE_SLOW_1",
                () -> {
                    sleepMillis(120);
                    return new CheckOutcome(0, "");
                });
        CheckSpec slow2 = new CheckSpec(
                "slow2",
                "VALIDATE_SLOW_2",
                () -> {
                    sleepMillis(120);
                    return new CheckOutcome(0, "");
                });
        CheckSpec slow3 = new CheckSpec(
                "slow3",
                "VALIDATE_SLOW_3",
                () -> {
                    sleepMillis(120);
                    return new CheckOutcome(0, "");
                });

        long start = System.nanoTime();
        AggregatedResult result = executor.execute(
                List.of(slow1, slow2, slow3));
        Duration wallClock = Duration.ofNanos(
                System.nanoTime() - start);

        long sumDurations = result.results().stream()
                .mapToLong(r -> r.duration().toMillis())
                .sum();
        assertThat(wallClock.toMillis())
                .isLessThan(sumDurations);
    }

    @Test
    void execute_exceptionDuringCheck_capturedAsFailure() {
        ParallelCheckExecutor executor = new ParallelCheckExecutor(4);
        CheckSpec throwing = new CheckSpec(
                "broken",
                "VALIDATE_BROKEN",
                () -> {
                    throw new RuntimeException("boom");
                });

        AggregatedResult result = executor.execute(List.of(throwing));

        assertThat(result.hasFailures()).isTrue();
        assertThat(result.results().get(0).severity())
                .isEqualTo(Severity.FAIL);
        assertThat(result.firstFailureCode())
                .isEqualTo("VALIDATE_BROKEN");
        assertThat(result.results().get(0).detail()).isEqualTo("boom");
    }

    @Test
    void execute_exceptionWithoutMessage_detailIsClassName() {
        ParallelCheckExecutor executor = new ParallelCheckExecutor(4);
        CheckSpec throwing = new CheckSpec(
                "broken",
                "VALIDATE_BROKEN",
                () -> {
                    throw new IllegalStateException();
                });

        AggregatedResult result = executor.execute(List.of(throwing));

        assertThat(result.results().get(0).detail())
                .isEqualTo("IllegalStateException");
    }

    private static void sleepMillis(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException(e);
        }
    }
}
