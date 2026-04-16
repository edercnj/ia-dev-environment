package dev.iadev.release.preflight;

import dev.iadev.release.BumpType;
import dev.iadev.release.CommitCounts;
import dev.iadev.release.SemVer;
import dev.iadev.release.integrity.CheckResult;
import dev.iadev.release.integrity.IntegrityReport;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PreflightOrchestratorTest {

    private static DashboardData passingData() {
        return new DashboardData(
                new SemVer(3, 2, 0, null),
                Optional.of(new SemVer(3, 1, 0, null)),
                12,
                new CommitCounts(7, 2, 0, 0, 5),
                BumpType.MINOR,
                List.of("### Added", "- feature"),
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
                        CheckResult.pass("version_alignment"))),
                "develop");
    }

    @Nested
    @DisplayName("evaluate — degenerate")
    class EvaluateDegenerate {

        @Test
        @DisplayName("evaluate_nullData_throwsNPE")
        void evaluate_nullData_throwsNPE() {
            assertThatThrownBy(() -> PreflightOrchestrator.evaluate(null))
                    .isInstanceOf(NullPointerException.class);
        }
    }

    @Nested
    @DisplayName("evaluate — integrity FAIL path")
    class IntegrityFailPath {

        @Test
        @DisplayName("evaluate_integrityFail_returnsExitCode1")
        void evaluate_integrityFail_returnsExitCode1() {
            PreflightResult result = PreflightOrchestrator.evaluate(
                    failingData());

            assertThat(result.exitCode()).isEqualTo(1);
            assertThat(result.errorCode())
                    .hasValue(PreflightResult.ERROR_INTEGRITY_FAIL);
        }

        @Test
        @DisplayName("evaluate_integrityFail_noDecision")
        void evaluate_integrityFail_noDecision() {
            PreflightResult result = PreflightOrchestrator.evaluate(
                    failingData());

            assertThat(result.decision()).isEmpty();
        }

        @Test
        @DisplayName("evaluate_integrityFail_dashboardRendered")
        void evaluate_integrityFail_dashboardRendered() {
            PreflightResult result = PreflightOrchestrator.evaluate(
                    failingData());

            assertThat(result.dashboard()).contains("PRE-FLIGHT");
            assertThat(result.dashboard()).contains("Integrity checks: FAIL");
        }
    }

    @Nested
    @DisplayName("evaluate — integrity PASS path")
    class IntegrityPassPath {

        @Test
        @DisplayName("evaluate_integrityPass_returnsProceed")
        void evaluate_integrityPass_returnsProceed() {
            PreflightResult result = PreflightOrchestrator.evaluate(
                    passingData());

            assertThat(result.exitCode()).isZero();
            assertThat(result.decision())
                    .hasValue(PreflightDecision.PROCEED);
        }

        @Test
        @DisplayName("evaluate_integrityPass_dashboardRendered")
        void evaluate_integrityPass_dashboardRendered() {
            PreflightResult result = PreflightOrchestrator.evaluate(
                    passingData());

            assertThat(result.dashboard()).contains("PRE-FLIGHT");
            assertThat(result.dashboard())
                    .contains("Integrity checks: PASS");
        }
    }

    @Nested
    @DisplayName("resolve — operator decisions")
    class Resolve {

        @Test
        @DisplayName("resolve_proceed_exitCode0")
        void resolve_proceed_exitCode0() {
            PreflightResult r = PreflightOrchestrator.resolve(
                    PreflightDecision.PROCEED, "dashboard");

            assertThat(r.exitCode()).isZero();
            assertThat(r.errorCode()).isEmpty();
            assertThat(r.decision()).hasValue(PreflightDecision.PROCEED);
        }

        @Test
        @DisplayName("resolve_editVersion_exitCode1WithErrorCode")
        void resolve_editVersion_exitCode1WithErrorCode() {
            PreflightResult r = PreflightOrchestrator.resolve(
                    PreflightDecision.EDIT_VERSION, "dashboard");

            assertThat(r.exitCode()).isEqualTo(1);
            assertThat(r.errorCode())
                    .hasValue(PreflightResult.ERROR_EDIT_VERSION);
            assertThat(r.decision())
                    .hasValue(PreflightDecision.EDIT_VERSION);
        }

        @Test
        @DisplayName("resolve_abort_exitCode0NullErrorCode")
        void resolve_abort_exitCode0NullErrorCode() {
            PreflightResult r = PreflightOrchestrator.resolve(
                    PreflightDecision.ABORT, "dashboard");

            assertThat(r.exitCode()).isZero();
            assertThat(r.errorCode()).isEmpty();
            assertThat(r.decision()).hasValue(PreflightDecision.ABORT);
        }

        @Test
        @DisplayName("resolve_nullDecision_throwsNPE")
        void resolve_nullDecision_throwsNPE() {
            assertThatThrownBy(
                    () -> PreflightOrchestrator.resolve(null, "dashboard"))
                    .isInstanceOf(NullPointerException.class);
        }
    }
}
