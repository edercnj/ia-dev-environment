package dev.iadev.checkpoint;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link StoryEntry} record.
 */
class StoryEntryTest {

    @Test
    void pending_createsEntryWithDefaults() {
        var entry = StoryEntry.pending(0);

        assertThat(entry.status()).isEqualTo(StoryStatus.PENDING);
        assertThat(entry.commitSha()).isNull();
        assertThat(entry.phase()).isZero();
        assertThat(entry.duration()).isZero();
        assertThat(entry.retries()).isZero();
        assertThat(entry.blockedBy()).isEmpty();
        assertThat(entry.summary()).isNull();
        assertThat(entry.findingsCount()).isZero();
    }

    @Test
    void pending_preservesPhase() {
        var entry = StoryEntry.pending(3);
        assertThat(entry.phase()).isEqualTo(3);
    }

    @Test
    void constructor_allFields_preservesValues() {
        var entry = new StoryEntry(
                StoryStatus.SUCCESS, "abc123", 2, 120_000L, 1,
                List.of("s1", "s2"), "Done", 3
        );

        assertThat(entry.status()).isEqualTo(StoryStatus.SUCCESS);
        assertThat(entry.commitSha()).isEqualTo("abc123");
        assertThat(entry.phase()).isEqualTo(2);
        assertThat(entry.duration()).isEqualTo(120_000L);
        assertThat(entry.retries()).isEqualTo(1);
        assertThat(entry.blockedBy()).containsExactly("s1", "s2");
        assertThat(entry.summary()).isEqualTo("Done");
        assertThat(entry.findingsCount()).isEqualTo(3);
    }

    @Test
    void withStatus_returnsNewEntryWithUpdatedStatus() {
        var original = StoryEntry.pending(0);
        var updated = original.withStatus(StoryStatus.IN_PROGRESS);

        assertThat(updated.status())
                .isEqualTo(StoryStatus.IN_PROGRESS);
        assertThat(original.status())
                .isEqualTo(StoryStatus.PENDING);
    }

    @Test
    void withCommitSha_returnsNewEntryWithUpdatedSha() {
        var original = StoryEntry.pending(0);
        var updated = original.withCommitSha("def456");

        assertThat(updated.commitSha()).isEqualTo("def456");
        assertThat(original.commitSha()).isNull();
    }

    @Test
    void withRetries_returnsNewEntryWithUpdatedRetries() {
        var original = StoryEntry.pending(0);
        var updated = original.withRetries(2);

        assertThat(updated.retries()).isEqualTo(2);
        assertThat(original.retries()).isZero();
    }

    @Test
    void withDuration_returnsNewEntryWithUpdatedDuration() {
        var original = StoryEntry.pending(0);
        var updated = original.withDuration(5000L);

        assertThat(updated.duration()).isEqualTo(5000L);
        assertThat(original.duration()).isZero();
    }

    @Test
    void withBlockedBy_returnsNewEntryWithUpdatedBlockedBy() {
        var original = StoryEntry.pending(0);
        var updated = original.withBlockedBy(
                List.of("story-001", "story-002")
        );

        assertThat(updated.blockedBy())
                .containsExactly("story-001", "story-002");
        assertThat(original.blockedBy()).isEmpty();
    }

    @Test
    void withSummary_returnsNewEntryWithUpdatedSummary() {
        var original = StoryEntry.pending(0);
        var updated = original.withSummary("Completed");

        assertThat(updated.summary()).isEqualTo("Completed");
        assertThat(original.summary()).isNull();
    }

    @Test
    void withFindingsCount_returnsNewEntryWithUpdatedFindings() {
        var original = StoryEntry.pending(0);
        var updated = original.withFindingsCount(5);

        assertThat(updated.findingsCount()).isEqualTo(5);
        assertThat(original.findingsCount()).isZero();
    }

    @Test
    void withStatus_chainsCorrectly() {
        var entry = StoryEntry.pending(1)
                .withStatus(StoryStatus.IN_PROGRESS)
                .withStatus(StoryStatus.SUCCESS)
                .withCommitSha("abc")
                .withDuration(100_000L)
                .withSummary("All good");

        assertThat(entry.status()).isEqualTo(StoryStatus.SUCCESS);
        assertThat(entry.commitSha()).isEqualTo("abc");
        assertThat(entry.duration()).isEqualTo(100_000L);
        assertThat(entry.summary()).isEqualTo("All good");
        assertThat(entry.phase()).isEqualTo(1);
    }
}
