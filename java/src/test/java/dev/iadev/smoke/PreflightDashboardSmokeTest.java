package dev.iadev.smoke;

import dev.iadev.release.BumpType;
import dev.iadev.release.CommitCounts;
import dev.iadev.release.SemVer;
import dev.iadev.release.integrity.CheckResult;
import dev.iadev.release.integrity.IntegrityReport;
import dev.iadev.release.preflight.DashboardData;
import dev.iadev.release.preflight.PreflightDecision;
import dev.iadev.release.preflight.PreflightOrchestrator;
import dev.iadev.release.preflight.PreflightResult;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Smoke tests for the pre-flight dashboard (story-0039-0009 TASK-010/011).
 *
 * <p>Validates the 5 end-to-end scenarios: proceed, edit version,
 * abort, integrity FAIL, and evaluate awaiting decision.</p>
 */
class PreflightDashboardSmokeTest {

    // ----- helpers -----

    private static DashboardData passingData() {
        return new DashboardData(
                new SemVer(3, 2, 0, null),
                Optional.of(new SemVer(3, 1, 0, null)),
                12,
                new CommitCounts(7, 2, 0, 0, 5),
                BumpType.MINOR,
                List.of("### Added", "- EPIC-0036: skill taxonomy",
                        "### Fixed", "- fix: null check"),
                IntegrityReport.aggregate(List.of(
                        CheckResult.pass("changelog_unreleased_non_empty"),
                        CheckResult.pass("version_alignment"),
                        CheckResult.pass("no_new_todos"))),
                "develop");
    }

    private static DashboardData failingData() {
        return new DashboardData(
                new SemVer(3, 2, 0, null),
                Optional.of(new SemVer(3, 1, 0, null)),
                12,
                new CommitCounts(7, 2, 0, 0, 5),
                BumpType.MINOR,
                List.of(),
                IntegrityReport.aggregate(List.of(
                        CheckResult.fail("changelog_unreleased_non_empty",
                                List.of("CHANGELOG.md")),
                        CheckResult.pass("version_alignment"),
                        CheckResult.pass("no_new_todos"))),
                "develop");
    }

    // ----- Scenario 1: Proceed (happy path) -----

    @Test
    @DisplayName("smoke_proceed_exitCode0AndDecisionProceed")
    void smoke_proceed_exitCode0AndDecisionProceed() {
        PreflightResult eval = PreflightOrchestrator.evaluate(passingData());
        PreflightResult result = PreflightOrchestrator.resolve(
                PreflightDecision.PROCEED, eval.dashboard());

        assertThat(result.exitCode()).isZero();
        assertThat(result.errorCode()).isEmpty();
        assertThat(result.decision())
                .hasValue(PreflightDecision.PROCEED);
        assertThat(result.dashboard()).contains("PRE-FLIGHT");
        assertThat(result.dashboard())
                .contains("Integrity checks: PASS");
    }

    // ----- Scenario 2: Edit version (exit 1) -----

    @Test
    @DisplayName("smoke_editVersion_exitCode1WithPreflightEditVersion")
    void smoke_editVersion_exitCode1WithPreflightEditVersion() {
        PreflightResult eval = PreflightOrchestrator.evaluate(passingData());
        PreflightResult result = PreflightOrchestrator.resolve(
                PreflightDecision.EDIT_VERSION, eval.dashboard());

        assertThat(result.exitCode()).isEqualTo(1);
        assertThat(result.errorCode())
                .hasValue(PreflightResult.ERROR_EDIT_VERSION);
        assertThat(result.decision())
                .hasValue(PreflightDecision.EDIT_VERSION);
    }

    // ----- Scenario 3: Abort (exit 0) -----

    @Test
    @DisplayName("smoke_abort_exitCode0NoBranchMutation")
    void smoke_abort_exitCode0NoBranchMutation() {
        PreflightResult eval = PreflightOrchestrator.evaluate(passingData());
        PreflightResult result = PreflightOrchestrator.resolve(
                PreflightDecision.ABORT, eval.dashboard());

        assertThat(result.exitCode()).isZero();
        assertThat(result.errorCode()).isEmpty();
        assertThat(result.decision())
                .hasValue(PreflightDecision.ABORT);
    }

    // ----- Scenario 4: Integrity FAIL (no prompt) -----

    @Test
    @DisplayName("smoke_integrityFail_exitCode1NoPrompt")
    void smoke_integrityFail_exitCode1NoPrompt() {
        PreflightResult result = PreflightOrchestrator.evaluate(
                failingData());

        assertThat(result.exitCode()).isEqualTo(1);
        assertThat(result.errorCode())
                .hasValue(PreflightResult.ERROR_INTEGRITY_FAIL);
        assertThat(result.decision()).isEmpty();
        assertThat(result.dashboard())
                .contains("Integrity checks: FAIL");
    }

    // ----- Scenario 5: evaluate returns empty decision (awaiting) -----

    @Test
    @DisplayName("smoke_evaluate_returnsEmptyDecisionAwaitingOperator")
    void smoke_evaluate_returnsEmptyDecisionAwaitingOperator() {
        PreflightResult eval = PreflightOrchestrator.evaluate(passingData());

        assertThat(eval.exitCode()).isZero();
        assertThat(eval.errorCode()).isEmpty();
        assertThat(eval.decision()).isEmpty();
        assertThat(eval.dashboard()).contains("PRE-FLIGHT");
    }

    // ----- Cross-scenario: dashboard content validation -----

    @Test
    @DisplayName("smoke_dashboardLinesAreReasonableWidth")
    void smoke_dashboardLinesAreReasonableWidth() {
        PreflightResult result = PreflightOrchestrator.evaluate(
                passingData());

        for (String line : result.dashboard().split("\n")) {
            assertThat(line.length())
                    .as("line exceeds 120 cols: '%s'", line)
                    .isLessThanOrEqualTo(120);
        }
    }

    @Test
    @DisplayName("smoke_truncationIndicatorFormat")
    void smoke_truncationIndicatorFormat() {
        DashboardData data = new DashboardData(
                new SemVer(3, 2, 0, null),
                Optional.of(new SemVer(3, 1, 0, null)),
                12,
                new CommitCounts(7, 2, 0, 0, 5),
                BumpType.MINOR,
                java.util.stream.IntStream.rangeClosed(1, 50)
                        .mapToObj(i -> "- line " + i)
                        .toList(),
                IntegrityReport.aggregate(List.of(
                        CheckResult.pass("changelog_unreleased_non_empty"))),
                "develop");

        PreflightResult result = PreflightOrchestrator.evaluate(data);

        assertThat(result.dashboard()).contains("(40 linhas omitidas)");
    }
}
