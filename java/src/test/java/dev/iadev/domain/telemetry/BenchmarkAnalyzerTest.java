package dev.iadev.domain.telemetry;

import dev.iadev.domain.model.PhaseMetric;
import dev.iadev.domain.model.PhaseOutcome;
import dev.iadev.domain.model.ReleaseType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Pure-unit tests for {@link BenchmarkAnalyzer} covering
 * the TPP ladder declared in story-0039-0012 task plan:
 * TASK-008 (nil), TASK-009 (happy), TASK-010 (&lt;5
 * boundary), TASK-011 (insufficient-history).
 */
class BenchmarkAnalyzerTest {

    private static final Instant ANCHOR = Instant.parse(
            "2026-04-13T08:00:00Z");

    private final BenchmarkAnalyzer analyzer =
            new BenchmarkAnalyzer();

    @Nested
    @DisplayName("degenerate cases (TASK-008)")
    class Degenerate {

        @Test
        @DisplayName("analyze_nullStream_throws")
        void analyze_nullStream_throws() {
            assertThatThrownBy(() -> analyzer.analyze(
                    null, "3.2.0"))
                    .isInstanceOf(
                            IllegalArgumentException.class);
        }

        @Test
        @DisplayName("analyze_blankVersion_throws")
        void analyze_blankVersion_throws() {
            assertThatThrownBy(() -> analyzer.analyze(
                    Stream.empty(), " "))
                    .isInstanceOf(
                            IllegalArgumentException.class);
        }

        @Test
        @DisplayName("analyze_emptyStream_returns"
                + "InsufficientHistory")
        void analyze_emptyStream_insufficient() {
            BenchmarkResult result = analyzer.analyze(
                    Stream.empty(), "3.2.0");

            assertThat(result).isInstanceOf(
                    BenchmarkResult.InsufficientHistory.class);
            assertThat(((BenchmarkResult.InsufficientHistory)
                    result).releasesObserved()).isZero();
        }
    }

    @Nested
    @DisplayName("boundary — fewer than MIN_HISTORY (TASK-010/011)")
    class InsufficientBoundary {

        @Test
        @DisplayName("analyze_fourHistoricalReleases"
                + "_returnsInsufficientHistory")
        void analyze_fourHistoricalReleases() {
            List<PhaseMetric> metrics = new ArrayList<>();
            for (int i = 0; i < 4; i++) {
                metrics.add(historical(
                        "3.1." + i, "VALIDATED", 100L));
            }
            metrics.add(current(
                    "3.2.0", "VALIDATED", 142L));

            BenchmarkResult result = analyzer.analyze(
                    metrics.stream(), "3.2.0");

            assertThat(result).isInstanceOf(
                    BenchmarkResult.InsufficientHistory.class);
            BenchmarkResult.InsufficientHistory insuf =
                    (BenchmarkResult.InsufficientHistory)
                            result;
            assertThat(insuf.releasesObserved()).isEqualTo(4);
        }

        @Test
        @DisplayName("MIN_HISTORY_isFive")
        void minHistoryIsFive() {
            assertThat(BenchmarkAnalyzer.MIN_HISTORY)
                    .isEqualTo(5);
        }
    }

    @Nested
    @DisplayName("happy path — 5 historical releases (TASK-009)")
    class HappyPath {

        @Test
        @DisplayName("analyze_fiveReleases_returnsTop3")
        void analyze_fiveReleases_top3() {
            List<PhaseMetric> metrics = new ArrayList<>();
            // Historical: 5 releases each with 3 phases
            for (int i = 0; i < 5; i++) {
                String v = "3.1." + i;
                metrics.add(historical(
                        v, "VALIDATED", 100L));
                metrics.add(historical(
                        v, "PR_OPENED", 16L));
                metrics.add(historical(
                        v, "CHANGELOG", 8L));
            }
            // Current release with slower VALIDATED
            metrics.add(current("3.2.0", "VALIDATED", 130L));
            metrics.add(current("3.2.0", "PR_OPENED", 12L));
            metrics.add(current("3.2.0", "CHANGELOG", 8L));

            BenchmarkResult result = analyzer.analyze(
                    metrics.stream(), "3.2.0");

            assertThat(result).isInstanceOf(
                    BenchmarkResult.TopPhases.class);
            List<PhaseBenchmark> top =
                    ((BenchmarkResult.TopPhases) result)
                            .entries();
            assertThat(top).hasSize(3);
            // Ranked by current durationSec desc
            assertThat(top.get(0).phase())
                    .isEqualTo("VALIDATED");
            assertThat(top.get(0).durationSec())
                    .isEqualTo(130L);
            assertThat(top.get(0).meanSec()).isEqualTo(100L);
            // (130-100)/100 * 100 = +30
            assertThat(top.get(0).deltaPercent())
                    .isEqualTo(30);
            assertThat(top.get(1).phase())
                    .isEqualTo("PR_OPENED");
            assertThat(top.get(1).deltaPercent())
                    .isEqualTo(-25);
            assertThat(top.get(2).phase())
                    .isEqualTo("CHANGELOG");
            assertThat(top.get(2).deltaPercent()).isZero();
        }

        @Test
        @DisplayName("analyze_skippedOutcome_excludedFromMean")
        void analyze_skippedOutcome_excludedFromMean() {
            List<PhaseMetric> metrics = new ArrayList<>();
            for (int i = 0; i < 5; i++) {
                metrics.add(historical(
                        "3.1." + i, "VALIDATED", 100L));
            }
            // Current with SKIPPED outcome — excluded
            metrics.add(new PhaseMetric(
                    "3.2.0", ReleaseType.RELEASE,
                    "VALIDATED", ANCHOR,
                    ANCHOR.plusSeconds(0), 0L,
                    PhaseOutcome.SKIPPED));

            BenchmarkResult result = analyzer.analyze(
                    metrics.stream(), "3.2.0");

            assertThat(result).isInstanceOf(
                    BenchmarkResult.TopPhases.class);
            List<PhaseBenchmark> top =
                    ((BenchmarkResult.TopPhases) result)
                            .entries();
            assertThat(top).isEmpty();
        }
    }

    private static PhaseMetric historical(
            String version, String phase, long duration) {
        return new PhaseMetric(
                version,
                ReleaseType.RELEASE,
                phase,
                ANCHOR,
                ANCHOR.plusSeconds(duration),
                duration,
                PhaseOutcome.SUCCESS);
    }

    private static PhaseMetric current(
            String version, String phase, long duration) {
        return historical(version, phase, duration);
    }
}
