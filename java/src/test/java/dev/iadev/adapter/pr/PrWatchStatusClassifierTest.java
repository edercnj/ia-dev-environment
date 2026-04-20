package dev.iadev.adapter.pr;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * Unit tests for {@link PrWatchStatusClassifier}.
 *
 * <p>Covers all 8 exit codes from RULE-045-05 via {@code @ParameterizedTest}.
 * Test-first: this test class precedes the classifier implementation (TDD Rule 05).</p>
 */
@DisplayName("PrWatchStatusClassifier — classify() covers all 8 exit codes")
class PrWatchStatusClassifierTest {

    private final PrWatchStatusClassifier classifier =
            new PrWatchStatusClassifier();

    // ── Scenario factory (TPP order: degenerate → happy path → error paths
    //    → conditionals → edge cases) ──────────────────────────────────────

    static Stream<Arguments> classifyScenarios() {
        return Stream.of(

            // Row 1 — Degenerate: PR not found
            Arguments.of(
                "PR not found → PR_NOT_FOUND",
                scenario()
                    .prState("NOT_FOUND")
                    .build(),
                PrWatchExitCode.PR_NOT_FOUND
            ),

            // Row 2 — Happy path: checks green + Copilot review present
            Arguments.of(
                "checks green + copilot present → SUCCESS",
                scenario()
                    .checks(List.of(
                        new PrWatchStatusClassifier.CheckResult(
                                "build", "success"),
                        new PrWatchStatusClassifier.CheckResult(
                                "test", "success")))
                    .copilotPresent(true)
                    .prState("OPEN")
                    .build(),
                PrWatchExitCode.SUCCESS
            ),

            // Row 3 — Error path: CI failed
            Arguments.of(
                "check conclusion=failure → CI_FAILED",
                scenario()
                    .checks(List.of(
                        new PrWatchStatusClassifier.CheckResult(
                                "build", "failure")))
                    .prState("OPEN")
                    .build(),
                PrWatchExitCode.CI_FAILED
            ),

            // Row 4 — CI timed_out maps to CI_FAILED
            Arguments.of(
                "check conclusion=timed_out → CI_FAILED",
                scenario()
                    .checks(List.of(
                        new PrWatchStatusClassifier.CheckResult(
                                "ci", "timed_out")))
                    .prState("OPEN")
                    .build(),
                PrWatchExitCode.CI_FAILED
            ),

            // Row 5 — Copilot timeout: checks green, Copilot absent, copilot timeout elapsed
            Arguments.of(
                "checks green + copilot timeout elapsed → CI_PENDING_PROCEED",
                scenario()
                    .checks(List.of(
                        new PrWatchStatusClassifier.CheckResult(
                                "build", "success")))
                    .copilotPresent(false)
                    .copilotTimeoutElapsed(true)
                    .prState("OPEN")
                    .requireCopilotReview(true)
                    .build(),
                PrWatchExitCode.CI_PENDING_PROCEED
            ),

            // Row 6 — PR already merged
            Arguments.of(
                "mergedAt != null → PR_ALREADY_MERGED",
                scenario()
                    .prState("MERGED")
                    .merged(true)
                    .build(),
                PrWatchExitCode.PR_ALREADY_MERGED
            ),

            // Row 7 — Global timeout (checks still pending)
            Arguments.of(
                "global timeout elapsed, checks pending → TIMEOUT",
                scenario()
                    .checks(List.of(
                        new PrWatchStatusClassifier.CheckResult(
                                "build", "pending")))
                    .prState("OPEN")
                    .globalTimeoutElapsed(true)
                    .build(),
                PrWatchExitCode.TIMEOUT
            ),

            // Row 8 — PR closed without merge
            Arguments.of(
                "PR closed without merge → PR_CLOSED",
                scenario()
                    .prState("CLOSED")
                    .merged(false)
                    .build(),
                PrWatchExitCode.PR_CLOSED
            )
        );
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("classifyScenarios")
    @DisplayName("classify_scenario_returnsExpectedExitCode")
    void classify_scenario_returnsExpectedExitCode(
            String description,
            PrWatchStatusClassifier.ClassifyInput input,
            PrWatchExitCode expected) {

        PrWatchExitCode result = classifier.classify(input);

        assertThat(result)
                .as("Scenario: %s", description)
                .isEqualTo(expected);
    }

    // ── Additional targeted tests for coverage completeness ───────────────

    @ParameterizedTest(name = "conclusion={0} → CI_FAILED")
    @org.junit.jupiter.params.provider.ValueSource(
            strings = {"failure", "timed_out", "cancelled", "action_required"})
    @DisplayName("classify_failingConclusion_returnsCiFailed")
    void classify_failingConclusion_returnsCiFailed(String conclusion) {
        var input = scenario()
                .checks(List.of(
                    new PrWatchStatusClassifier.CheckResult("ci", conclusion)))
                .prState("OPEN")
                .build();

        assertThat(classifier.classify(input))
                .isEqualTo(PrWatchExitCode.CI_FAILED);
    }

    @org.junit.jupiter.api.Test
    @DisplayName("classify_checksGreenCopilotNotRequired_returnsSuccess")
    void classify_checksGreenCopilotNotRequired_returnsSuccess() {
        var input = scenario()
                .checks(List.of(
                    new PrWatchStatusClassifier.CheckResult(
                            "build", "success")))
                .copilotPresent(false)
                .requireCopilotReview(false)
                .prState("OPEN")
                .build();

        assertThat(classifier.classify(input))
                .isEqualTo(PrWatchExitCode.SUCCESS);
    }

    @org.junit.jupiter.api.Test
    @DisplayName("classify_emptyChecks_globalTimeoutElapsed_returnsTimeout")
    void classify_emptyChecks_globalTimeoutElapsed_returnsTimeout() {
        var input = scenario()
                .checks(List.of())
                .prState("OPEN")
                .globalTimeoutElapsed(true)
                .build();

        assertThat(classifier.classify(input))
                .isEqualTo(PrWatchExitCode.TIMEOUT);
    }

    @org.junit.jupiter.api.Test
    @DisplayName("classify_emptyChecks_noTimeout_returnsTimeout")
    void classify_emptyChecks_noTimeout_returnsTimeout() {
        // empty checks = no checks green = falls through to TIMEOUT
        var input = scenario()
                .checks(List.of())
                .prState("OPEN")
                .globalTimeoutElapsed(false)
                .build();

        assertThat(classifier.classify(input))
                .isEqualTo(PrWatchExitCode.TIMEOUT);
    }

    @org.junit.jupiter.api.Test
    @DisplayName("classify_checksNeutral_copilotPresent_returnsSuccess")
    void classify_checksNeutral_copilotPresent_returnsSuccess() {
        var input = scenario()
                .checks(List.of(
                    new PrWatchStatusClassifier.CheckResult(
                            "lint", "neutral"),
                    new PrWatchStatusClassifier.CheckResult(
                            "build", "success")))
                .copilotPresent(true)
                .prState("OPEN")
                .build();

        assertThat(classifier.classify(input))
                .isEqualTo(PrWatchExitCode.SUCCESS);
    }

    @org.junit.jupiter.api.Test
    @DisplayName("classify_checksSkipped_copilotPresent_returnsSuccess")
    void classify_checksSkipped_copilotPresent_returnsSuccess() {
        var input = scenario()
                .checks(List.of(
                    new PrWatchStatusClassifier.CheckResult(
                            "optional", "skipped")))
                .copilotPresent(true)
                .prState("OPEN")
                .build();

        assertThat(classifier.classify(input))
                .isEqualTo(PrWatchExitCode.SUCCESS);
    }

    @org.junit.jupiter.api.Test
    @DisplayName("classify_closedMerged_isMerged_returnsMerged")
    void classify_closedMerged_isMerged_returnsMerged() {
        // CLOSED state but merged=true → PR_ALREADY_MERGED takes precedence
        var input = scenario()
                .prState("CLOSED")
                .merged(true)
                .build();

        assertThat(classifier.classify(input))
                .isEqualTo(PrWatchExitCode.PR_ALREADY_MERGED);
    }

    @org.junit.jupiter.api.Test
    @DisplayName("classify_checksGreenNoCopilotNoCopilotTimeout_returnsTimeout")
    void classify_checksGreenNoCopilotNoCopilotTimeout_returnsTimeout() {
        // Checks green, Copilot required but not present, copilot timeout NOT elapsed
        // → waiting for Copilot, but we can't wait more → TIMEOUT (classify falls through)
        var input = scenario()
                .checks(List.of(
                    new PrWatchStatusClassifier.CheckResult(
                            "build", "success")))
                .copilotPresent(false)
                .copilotTimeoutElapsed(false)
                .requireCopilotReview(true)
                .globalTimeoutElapsed(false)
                .prState("OPEN")
                .build();

        // The classify loop logic falls through to TIMEOUT when copilot not present
        // and copilot-timeout not elapsed — the polling loop hasn't finished waiting.
        assertThat(classifier.classify(input))
                .isEqualTo(PrWatchExitCode.TIMEOUT);
    }

    // ── Builder helper ────────────────────────────────────────────────────

    static ScenarioBuilder scenario() {
        return new ScenarioBuilder();
    }

    static final class ScenarioBuilder {

        private List<PrWatchStatusClassifier.CheckResult> checks =
                List.of();
        private boolean copilotPresent = false;
        private boolean copilotTimeoutElapsed = false;
        private String prState = "OPEN";
        private boolean merged = false;
        private boolean globalTimeoutElapsed = false;
        private boolean requireCopilotReview = true;

        ScenarioBuilder checks(
                List<PrWatchStatusClassifier.CheckResult> checks) {
            this.checks = checks;
            return this;
        }

        ScenarioBuilder copilotPresent(boolean copilotPresent) {
            this.copilotPresent = copilotPresent;
            return this;
        }

        ScenarioBuilder copilotTimeoutElapsed(
                boolean copilotTimeoutElapsed) {
            this.copilotTimeoutElapsed = copilotTimeoutElapsed;
            return this;
        }

        ScenarioBuilder prState(String prState) {
            this.prState = prState;
            return this;
        }

        ScenarioBuilder merged(boolean merged) {
            this.merged = merged;
            return this;
        }

        ScenarioBuilder globalTimeoutElapsed(
                boolean globalTimeoutElapsed) {
            this.globalTimeoutElapsed = globalTimeoutElapsed;
            return this;
        }

        ScenarioBuilder requireCopilotReview(boolean requireCopilotReview) {
            this.requireCopilotReview = requireCopilotReview;
            return this;
        }

        PrWatchStatusClassifier.ClassifyInput build() {
            return new PrWatchStatusClassifier.ClassifyInput(
                    checks,
                    copilotPresent,
                    copilotTimeoutElapsed,
                    prState,
                    merged,
                    globalTimeoutElapsed,
                    requireCopilotReview);
        }
    }
}
