package dev.iadev.domain.telemetry;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Tests for {@link BenchmarkResult} value types — covers
 * validation branches not exercised by
 * {@code BenchmarkAnalyzerTest}.
 */
@DisplayName("BenchmarkResult")
class BenchmarkResultTest {

    @Test
    @DisplayName("InsufficientHistory_negative_"
            + "throwsIllegalArgumentException")
    void insufficientHistory_negativeCount_throws() {
        assertThatThrownBy(
                () -> new BenchmarkResult
                        .InsufficientHistory(-1))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("releasesObserved");
    }

    @Test
    @DisplayName("InsufficientHistory_zero_accepted")
    void insufficientHistory_zero_accepted() {
        BenchmarkResult.InsufficientHistory ih =
                new BenchmarkResult.InsufficientHistory(0);

        assertThat(ih.releasesObserved()).isZero();
    }

    @Test
    @DisplayName("TopPhases_copiesEntriesDefensively")
    void topPhases_copiesEntriesDefensively() {
        BenchmarkResult.TopPhases tp =
                new BenchmarkResult.TopPhases(List.of());

        assertThat(tp.entries()).isEmpty();
    }
}
