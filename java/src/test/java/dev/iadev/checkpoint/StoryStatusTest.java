package dev.iadev.checkpoint;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link StoryStatus} enum values and parsing.
 */
class StoryStatusTest {

    @Test
    void values_containsSixStatuses() {
        assertThat(StoryStatus.values()).hasSize(6);
    }

    @Test
    void valueOf_pending_returnsPending() {
        assertThat(StoryStatus.valueOf("PENDING"))
                .isEqualTo(StoryStatus.PENDING);
    }

    @Test
    void valueOf_inProgress_returnsInProgress() {
        assertThat(StoryStatus.valueOf("IN_PROGRESS"))
                .isEqualTo(StoryStatus.IN_PROGRESS);
    }

    @Test
    void valueOf_success_returnsSuccess() {
        assertThat(StoryStatus.valueOf("SUCCESS"))
                .isEqualTo(StoryStatus.SUCCESS);
    }

    @Test
    void valueOf_failed_returnsFailed() {
        assertThat(StoryStatus.valueOf("FAILED"))
                .isEqualTo(StoryStatus.FAILED);
    }

    @Test
    void valueOf_blocked_returnsBlocked() {
        assertThat(StoryStatus.valueOf("BLOCKED"))
                .isEqualTo(StoryStatus.BLOCKED);
    }

    @Test
    void valueOf_partial_returnsPartial() {
        assertThat(StoryStatus.valueOf("PARTIAL"))
                .isEqualTo(StoryStatus.PARTIAL);
    }

    @Test
    void name_returnsExpectedStrings() {
        assertThat(StoryStatus.PENDING.name()).isEqualTo("PENDING");
        assertThat(StoryStatus.IN_PROGRESS.name())
                .isEqualTo("IN_PROGRESS");
        assertThat(StoryStatus.SUCCESS.name()).isEqualTo("SUCCESS");
        assertThat(StoryStatus.FAILED.name()).isEqualTo("FAILED");
        assertThat(StoryStatus.BLOCKED.name()).isEqualTo("BLOCKED");
        assertThat(StoryStatus.PARTIAL.name()).isEqualTo("PARTIAL");
    }
}
