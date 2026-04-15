package dev.iadev.smoke;

import dev.iadev.release.validate.AggregatedResult;
import dev.iadev.release.validate.CheckOutcome;
import dev.iadev.release.validate.CheckResult;
import dev.iadev.release.validate.CheckSpec;
import dev.iadev.release.validate.ParallelCheckExecutor;
import dev.iadev.release.validate.Severity;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.Callable;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Smoke test for VALIDATE-DEEP parallel execution.
 *
 * <p>Exercises the end-to-end orchestration contract: with a
 * forced failure on the golden check (mirroring a real
 * hand-edited golden file drifting out of sync), the aggregator
 * MUST report the abort code {@code VALIDATE_GOLDEN_DRIFT} as
 * the first failure while still capturing the output of every
 * other parallel check that ran concurrently.</p>
 *
 * <p>Also covers the multi-failure Gherkin scenario from the
 * story: when golden and hardcoded both fail, the
 * alphabetically-first code ({@code VALIDATE_GOLDEN_DRIFT})
 * wins as the abort code.</p>
 */
final class ValidateDeepParallelSmokeTest {

    @Test
    void forcedGoldenFailure_abortsWithGoldenDriftCode() {
        ParallelCheckExecutor executor = new ParallelCheckExecutor(4);
        List<CheckSpec> checks = List.of(
                passing("coverage_line", "VALIDATE_COVERAGE_LINE"),
                passing("coverage_branch", "VALIDATE_COVERAGE_BRANCH"),
                failing(
                        "golden_files",
                        "VALIDATE_GOLDEN_DRIFT",
                        "regenerate goldens"),
                passing("hardcoded_version",
                        "VALIDATE_HARDCODED_VERSION"),
                passing("version_match",
                        "VALIDATE_VERSION_MISMATCH"));

        AggregatedResult result = executor.execute(checks);

        assertThat(result.hasFailures()).isTrue();
        assertThat(result.firstFailureCode())
                .isEqualTo("VALIDATE_GOLDEN_DRIFT");
        // Other checks still executed (captured)
        assertThat(result.results()).hasSize(5);
        long passes = result.results().stream()
                .filter(r -> r.severity() == Severity.PASS)
                .count();
        assertThat(passes).isEqualTo(4);
    }

    @Test
    void multipleFailures_firstAlphabeticCodeWins() {
        ParallelCheckExecutor executor = new ParallelCheckExecutor(4);
        List<CheckSpec> checks = List.of(
                failing("golden_files",
                        "VALIDATE_GOLDEN_DRIFT",
                        "drift"),
                failing("hardcoded_version",
                        "VALIDATE_HARDCODED_VERSION",
                        "stale ref"),
                passing("coverage_line",
                        "VALIDATE_COVERAGE_LINE"));

        AggregatedResult result = executor.execute(checks);

        assertThat(result.firstFailureCode())
                .isEqualTo("VALIDATE_GOLDEN_DRIFT");
        List<CheckResult> sorted = result.sorted();
        assertThat(sorted.get(0).severity()).isEqualTo(Severity.FAIL);
        assertThat(sorted.get(1).severity()).isEqualTo(Severity.FAIL);
        assertThat(sorted.get(2).severity()).isEqualTo(Severity.PASS);
    }

    @Test
    void maxParallelOne_stillReportsSameAbortCode() {
        ParallelCheckExecutor serial = new ParallelCheckExecutor(1);
        List<CheckSpec> checks = List.of(
                passing("coverage_line", "VALIDATE_COVERAGE_LINE"),
                failing("golden_files",
                        "VALIDATE_GOLDEN_DRIFT",
                        "drift"));

        AggregatedResult result = serial.execute(checks);

        assertThat(result.firstFailureCode())
                .isEqualTo("VALIDATE_GOLDEN_DRIFT");
    }

    private static CheckSpec passing(String name, String code) {
        return new CheckSpec(name, code, okRunner());
    }

    private static CheckSpec failing(
            String name, String code, String detail) {
        return new CheckSpec(name, code, failRunner(detail));
    }

    private static Callable<CheckOutcome> okRunner() {
        return () -> new CheckOutcome(0, "");
    }

    private static Callable<CheckOutcome> failRunner(String detail) {
        return () -> new CheckOutcome(1, detail);
    }
}
