package dev.iadev.release.integrity;

import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class IntegrityReportTest {

    @Test
    @DisplayName("aggregate_allPass_overallPassNoErrorCode")
    void aggregate_allPass_overallPassNoErrorCode() {
        IntegrityReport r = IntegrityReport.aggregate(List.of(
                CheckResult.pass("a"), CheckResult.pass("b")));

        assertThat(r.overallStatus()).isEqualTo(CheckStatus.PASS);
        assertThat(r.errorCode()).isEmpty();
    }

    @Test
    @DisplayName("aggregate_anyWarn_overallWarnNoErrorCode")
    void aggregate_anyWarn_overallWarnNoErrorCode() {
        IntegrityReport r = IntegrityReport.aggregate(List.of(
                CheckResult.pass("a"), CheckResult.warn("b", List.of("x"))));

        assertThat(r.overallStatus()).isEqualTo(CheckStatus.WARN);
        assertThat(r.errorCode()).isEmpty();
    }

    @Test
    @DisplayName("aggregate_anyFail_overallFailWithValidateIntegrityDrift")
    void aggregate_anyFail_overallFailWithValidateIntegrityDrift() {
        IntegrityReport r = IntegrityReport.aggregate(List.of(
                CheckResult.warn("a", List.of("x")),
                CheckResult.fail("b", List.of("y"))));

        assertThat(r.overallStatus()).isEqualTo(CheckStatus.FAIL);
        assertThat(r.errorCode()).contains("VALIDATE_INTEGRITY_DRIFT");
    }

    @Test
    @DisplayName("aggregate_empty_overallPass")
    void aggregate_empty_overallPass() {
        IntegrityReport r = IntegrityReport.aggregate(List.of());

        assertThat(r.overallStatus()).isEqualTo(CheckStatus.PASS);
        assertThat(r.checks()).isEmpty();
    }

    @Test
    @DisplayName("aggregate_nullResults_throws")
    void aggregate_nullResults_throws() {
        assertThatThrownBy(() -> IntegrityReport.aggregate(null))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    @DisplayName("checksList_isImmutable")
    void checksList_isImmutable() {
        IntegrityReport r = IntegrityReport.aggregate(List.of(CheckResult.pass("a")));
        assertThatThrownBy(() -> r.checks().add(CheckResult.pass("b")))
                .isInstanceOf(UnsupportedOperationException.class);
    }
}
