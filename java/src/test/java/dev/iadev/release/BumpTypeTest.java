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

        assertThat(BumpType.from(counts)).contains(BumpType.MAJOR);
    }

    @Test
    @DisplayName("from_featOnly_returnsMinor")
    void from_featOnly_returnsMinor() {
        CommitCounts counts = new CommitCounts(5, 0, 0, 0, 2);

        assertThat(BumpType.from(counts)).contains(BumpType.MINOR);
    }

    @Test
    @DisplayName("from_fixOrPerf_returnsPatch")
    void from_fixOrPerf_returnsPatch() {
        CommitCounts fixOnly = new CommitCounts(0, 2, 0, 0, 1);
        CommitCounts perfOnly = new CommitCounts(0, 0, 1, 0, 0);

        assertThat(BumpType.from(fixOnly)).contains(BumpType.PATCH);
        assertThat(BumpType.from(perfOnly)).contains(BumpType.PATCH);
    }

    @Test
    @DisplayName("from_onlyIgnored_returnsEmpty")
    void from_onlyIgnored_returnsEmpty() {
        CommitCounts counts = new CommitCounts(0, 0, 0, 0, 5);

        assertThat(BumpType.from(counts)).isEmpty();
    }

    @Test
    @DisplayName("from_zero_returnsEmpty")
    void from_zero_returnsEmpty() {
        assertThat(BumpType.from(CommitCounts.ZERO)).isEmpty();
    }
}
