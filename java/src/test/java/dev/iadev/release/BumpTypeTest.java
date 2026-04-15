package dev.iadev.release;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("BumpType.from(CommitCounts) — selection precedence")
class BumpTypeTest {

    @Test
    @DisplayName("from_breakingPresent_returnsMajor")
    void from_breakingPresent_returnsMajor() {
        CommitCounts counts = new CommitCounts(3, 1, 0, 1, 2);

        assertThat(BumpType.from(counts)).isEqualTo(BumpType.MAJOR);
    }

    @Test
    @DisplayName("from_featOnly_returnsMinor")
    void from_featOnly_returnsMinor() {
        CommitCounts counts = new CommitCounts(5, 0, 0, 0, 2);

        assertThat(BumpType.from(counts)).isEqualTo(BumpType.MINOR);
    }

    @Test
    @DisplayName("from_fixOrPerf_returnsPatch")
    void from_fixOrPerf_returnsPatch() {
        CommitCounts fixOnly = new CommitCounts(0, 2, 0, 0, 1);
        CommitCounts perfOnly = new CommitCounts(0, 0, 1, 0, 0);

        assertThat(BumpType.from(fixOnly)).isEqualTo(BumpType.PATCH);
        assertThat(BumpType.from(perfOnly)).isEqualTo(BumpType.PATCH);
    }

    @Test
    @DisplayName("from_onlyIgnored_returnsNull")
    void from_onlyIgnored_returnsNull() {
        CommitCounts counts = new CommitCounts(0, 0, 0, 0, 5);

        assertThat(BumpType.from(counts)).isNull();
    }

    @Test
    @DisplayName("from_zero_returnsNull")
    void from_zero_returnsNull() {
        assertThat(BumpType.from(CommitCounts.ZERO)).isNull();
    }
}
