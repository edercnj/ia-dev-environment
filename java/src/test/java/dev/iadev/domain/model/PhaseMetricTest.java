package dev.iadev.domain.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PhaseMetricTest {

    private static final Instant T0 = Instant.parse(
            "2026-04-13T08:00:00Z");
    private static final Instant T1 = Instant.parse(
            "2026-04-13T08:02:22Z");

    @Nested
    @DisplayName("construction — degenerate cases")
    class Degenerate {

        @Test
        @DisplayName("constructor_nullReleaseVersion"
                + "_throwsIllegalArgumentException")
        void constructor_nullReleaseVersion_throws() {
            assertThatThrownBy(() -> new PhaseMetric(
                    null, ReleaseType.RELEASE,
                    "VALIDATED", T0, T1, 142L,
                    PhaseOutcome.SUCCESS))
                    .isInstanceOf(
                            IllegalArgumentException.class)
                    .hasMessageContaining("releaseVersion");
        }

        @Test
        @DisplayName("constructor_blankReleaseVersion"
                + "_throwsIllegalArgumentException")
        void constructor_blankReleaseVersion_throws() {
            assertThatThrownBy(() -> new PhaseMetric(
                    "  ", ReleaseType.RELEASE,
                    "VALIDATED", T0, T1, 142L,
                    PhaseOutcome.SUCCESS))
                    .isInstanceOf(
                            IllegalArgumentException.class);
        }

        @Test
        @DisplayName("constructor_nullReleaseType_throws")
        void constructor_nullReleaseType_throws() {
            assertThatThrownBy(() -> new PhaseMetric(
                    "3.2.0", null,
                    "VALIDATED", T0, T1, 142L,
                    PhaseOutcome.SUCCESS))
                    .isInstanceOf(
                            NullPointerException.class);
        }

        @Test
        @DisplayName("constructor_blankPhase_throws")
        void constructor_blankPhase_throws() {
            assertThatThrownBy(() -> new PhaseMetric(
                    "3.2.0", ReleaseType.RELEASE, "",
                    T0, T1, 142L,
                    PhaseOutcome.SUCCESS))
                    .isInstanceOf(
                            IllegalArgumentException.class);
        }

        @Test
        @DisplayName("constructor_negativeDuration_throws")
        void constructor_negativeDuration_throws() {
            assertThatThrownBy(() -> new PhaseMetric(
                    "3.2.0", ReleaseType.RELEASE,
                    "VALIDATED", T0, T1, -1L,
                    PhaseOutcome.SUCCESS))
                    .isInstanceOf(
                            IllegalArgumentException.class)
                    .hasMessageContaining("durationSec");
        }

        @Test
        @DisplayName("constructor_nullOutcome_throws")
        void constructor_nullOutcome_throws() {
            assertThatThrownBy(() -> new PhaseMetric(
                    "3.2.0", ReleaseType.RELEASE,
                    "VALIDATED", T0, T1, 0L, null))
                    .isInstanceOf(
                            NullPointerException.class);
        }
    }

    @Nested
    @DisplayName("construction — happy path")
    class HappyPath {

        @Test
        @DisplayName("constructor_allFieldsValid_buildsRecord")
        void constructor_allFieldsValid_buildsRecord() {
            PhaseMetric metric = new PhaseMetric(
                    "3.2.0",
                    ReleaseType.RELEASE,
                    "VALIDATED",
                    T0, T1, 142L,
                    PhaseOutcome.SUCCESS);

            assertThat(metric.releaseVersion())
                    .isEqualTo("3.2.0");
            assertThat(metric.releaseType())
                    .isEqualTo(ReleaseType.RELEASE);
            assertThat(metric.phase())
                    .isEqualTo("VALIDATED");
            assertThat(metric.durationSec()).isEqualTo(142L);
            assertThat(metric.outcome())
                    .isEqualTo(PhaseOutcome.SUCCESS);
        }

        @Test
        @DisplayName("constructor_zeroDuration_accepted")
        void constructor_zeroDuration_accepted() {
            PhaseMetric metric = new PhaseMetric(
                    "3.2.0",
                    ReleaseType.RELEASE,
                    "SKIP",
                    T0, T0, 0L,
                    PhaseOutcome.SKIPPED);

            assertThat(metric.durationSec()).isZero();
        }
    }
}
