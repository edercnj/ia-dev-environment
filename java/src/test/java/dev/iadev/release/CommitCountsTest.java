package dev.iadev.release;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("CommitCounts — value object invariants")
class CommitCountsTest {

    @Test
    @DisplayName("zero_allFieldsZero")
    void zero_allFieldsZero() {
        assertThat(CommitCounts.ZERO.feat()).isZero();
        assertThat(CommitCounts.ZERO.fix()).isZero();
        assertThat(CommitCounts.ZERO.perf()).isZero();
        assertThat(CommitCounts.ZERO.breaking()).isZero();
        assertThat(CommitCounts.ZERO.ignored()).isZero();
    }

    @Test
    @DisplayName("total_sumsFeatFixPerfIgnored_excludingBreaking")
    void total_sumsFeatFixPerfIgnored_excludingBreaking() {
        CommitCounts counts = new CommitCounts(3, 2, 1, 4, 5);

        // breaking is orthogonal (a feat! contributes both feat and breaking)
        assertThat(counts.total()).isEqualTo(3 + 2 + 1 + 5);
    }

    @Test
    @DisplayName("new_negativeFeat_rejected")
    void new_negativeFeat_rejected() {
        assertThatThrownBy(() -> new CommitCounts(-1, 0, 0, 0, 0))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("non-negative");
    }

    @Test
    @DisplayName("new_negativeFix_rejected")
    void new_negativeFix_rejected() {
        assertThatThrownBy(() -> new CommitCounts(0, -1, 0, 0, 0))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("new_negativePerf_rejected")
    void new_negativePerf_rejected() {
        assertThatThrownBy(() -> new CommitCounts(0, 0, -1, 0, 0))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("new_negativeBreaking_rejected")
    void new_negativeBreaking_rejected() {
        assertThatThrownBy(() -> new CommitCounts(0, 0, 0, -1, 0))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("new_negativeIgnored_rejected")
    void new_negativeIgnored_rejected() {
        assertThatThrownBy(() -> new CommitCounts(0, 0, 0, 0, -1))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
