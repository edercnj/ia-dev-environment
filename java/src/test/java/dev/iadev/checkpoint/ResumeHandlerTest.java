package dev.iadev.checkpoint;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link ResumeHandler}.
 */
class ResumeHandlerTest {

    private ExecutionState createStateWith(
            Map<String, StoryEntry> stories) {
        return new ExecutionState(
                "EPIC-001", "main",
                Instant.parse("2026-03-19T10:00:00Z"),
                0, ExecutionMode.FULL,
                stories, Map.of(),
                ExecutionMetrics.initial(stories.size())
        );
    }

    @Test
    void prepareResume_failedWithRetriesLeft_becomesInProgress() {
        var stories = Map.of(
                "story-002",
                StoryEntry.pending(0)
                        .withStatus(StoryStatus.FAILED)
                        .withRetries(1)
        );
        var state = createStateWith(stories);

        var resumed = ResumeHandler.prepareResume(state, 3);

        assertThat(resumed.stories().get("story-002").status())
                .isEqualTo(StoryStatus.IN_PROGRESS);
        assertThat(resumed.stories().get("story-002").retries())
                .isEqualTo(1);
    }

    @Test
    void prepareResume_failedAtMaxRetries_remainsFailed() {
        var stories = Map.of(
                "story-002",
                StoryEntry.pending(0)
                        .withStatus(StoryStatus.FAILED)
                        .withRetries(3)
        );
        var state = createStateWith(stories);

        var resumed = ResumeHandler.prepareResume(state, 3);

        assertThat(resumed.stories().get("story-002").status())
                .isEqualTo(StoryStatus.FAILED);
    }

    @Test
    void prepareResume_partial_becomesInProgress() {
        var stories = Map.of(
                "story-002",
                StoryEntry.pending(0)
                        .withStatus(StoryStatus.PARTIAL)
        );
        var state = createStateWith(stories);

        var resumed = ResumeHandler.prepareResume(state, 3);

        assertThat(resumed.stories().get("story-002").status())
                .isEqualTo(StoryStatus.IN_PROGRESS);
    }

    @Test
    void prepareResume_staleInProgress_becomesPending() {
        var stories = Map.of(
                "story-002",
                StoryEntry.pending(0)
                        .withStatus(StoryStatus.IN_PROGRESS)
        );
        var state = createStateWith(stories);

        var resumed = ResumeHandler.prepareResume(state, 3);

        assertThat(resumed.stories().get("story-002").status())
                .isEqualTo(StoryStatus.PENDING);
    }

    @Test
    void prepareResume_success_remainsSuccess() {
        var stories = Map.of(
                "story-001",
                StoryEntry.pending(0)
                        .withStatus(StoryStatus.SUCCESS)
                        .withCommitSha("abc123")
                        .withDuration(60_000L)
        );
        var state = createStateWith(stories);

        var resumed = ResumeHandler.prepareResume(state, 3);

        assertThat(resumed.stories().get("story-001").status())
                .isEqualTo(StoryStatus.SUCCESS);
    }

    @Test
    void prepareResume_pending_remainsPending() {
        var stories = Map.of(
                "story-004", StoryEntry.pending(0)
        );
        var state = createStateWith(stories);

        var resumed = ResumeHandler.prepareResume(state, 3);

        assertThat(resumed.stories().get("story-004").status())
                .isEqualTo(StoryStatus.PENDING);
    }

    @Test
    void prepareResume_blockedWithSuccessfulDep_becomesPending() {
        var stories = new LinkedHashMap<String, StoryEntry>();
        stories.put(
                "story-001",
                StoryEntry.pending(0)
                        .withStatus(StoryStatus.SUCCESS)
                        .withDuration(60_000L)
        );
        stories.put(
                "story-003",
                StoryEntry.pending(1)
                        .withStatus(StoryStatus.BLOCKED)
                        .withBlockedBy(List.of("story-001"))
        );
        var state = createStateWith(stories);

        var resumed = ResumeHandler.prepareResume(state, 3);

        assertThat(resumed.stories().get("story-003").status())
                .isEqualTo(StoryStatus.PENDING);
    }

    @Test
    void prepareResume_blockedWithFailedDep_remainsBlocked() {
        var stories = new LinkedHashMap<String, StoryEntry>();
        stories.put(
                "story-002",
                StoryEntry.pending(0)
                        .withStatus(StoryStatus.FAILED)
                        .withRetries(3)
        );
        stories.put(
                "story-003",
                StoryEntry.pending(1)
                        .withStatus(StoryStatus.BLOCKED)
                        .withBlockedBy(List.of("story-002"))
        );
        var state = createStateWith(stories);

        var resumed = ResumeHandler.prepareResume(state, 3);

        assertThat(resumed.stories().get("story-003").status())
                .isEqualTo(StoryStatus.BLOCKED);
    }

    @Test
    void prepareResume_recalculatesMetrics() {
        var stories = new LinkedHashMap<String, StoryEntry>();
        stories.put(
                "story-001",
                StoryEntry.pending(0)
                        .withStatus(StoryStatus.SUCCESS)
                        .withDuration(60_000L)
        );
        stories.put(
                "story-002",
                StoryEntry.pending(0)
                        .withStatus(StoryStatus.FAILED)
                        .withRetries(0)
        );
        stories.put(
                "story-003",
                StoryEntry.pending(1)
                        .withStatus(StoryStatus.BLOCKED)
                        .withBlockedBy(List.of("story-002"))
        );
        var state = createStateWith(stories);

        var resumed = ResumeHandler.prepareResume(state, 3);

        assertThat(resumed.metrics().storiesCompleted())
                .isEqualTo(1);
    }

    @Test
    void prepareResume_defaultMaxRetries_usesTwo() {
        var stories = Map.of(
                "s1",
                StoryEntry.pending(0)
                        .withStatus(StoryStatus.FAILED)
                        .withRetries(1)
        );
        var state = createStateWith(stories);

        var resumed = ResumeHandler.prepareResume(state);

        assertThat(resumed.stories().get("s1").status())
                .isEqualTo(StoryStatus.IN_PROGRESS);
    }

    @Test
    void prepareResume_failedAtDefaultMax_remainsFailed() {
        var stories = Map.of(
                "s1",
                StoryEntry.pending(0)
                        .withStatus(StoryStatus.FAILED)
                        .withRetries(2)
        );
        var state = createStateWith(stories);

        var resumed = ResumeHandler.prepareResume(state);

        assertThat(resumed.stories().get("s1").status())
                .isEqualTo(StoryStatus.FAILED);
    }

    @Test
    void prepareResume_complexScenario_correctReclassification() {
        var stories = new LinkedHashMap<String, StoryEntry>();
        // story-001: SUCCESS (should stay)
        stories.put(
                "story-001",
                StoryEntry.pending(0)
                        .withStatus(StoryStatus.SUCCESS)
                        .withDuration(60_000L)
                        .withCommitSha("abc")
        );
        // story-002: FAILED retries=1 < max=3 (should become IN_PROGRESS)
        stories.put(
                "story-002",
                StoryEntry.pending(0)
                        .withStatus(StoryStatus.FAILED)
                        .withRetries(1)
        );
        // story-003: BLOCKED by story-002 (still failed -> stay BLOCKED)
        stories.put(
                "story-003",
                StoryEntry.pending(1)
                        .withStatus(StoryStatus.BLOCKED)
                        .withBlockedBy(List.of("story-002"))
        );
        // story-004: PARTIAL (should become IN_PROGRESS)
        stories.put(
                "story-004",
                StoryEntry.pending(1)
                        .withStatus(StoryStatus.PARTIAL)
        );
        // story-005: PENDING (should stay)
        stories.put("story-005", StoryEntry.pending(2));

        var state = createStateWith(stories);
        var resumed = ResumeHandler.prepareResume(state, 3);

        assertThat(resumed.stories().get("story-001").status())
                .isEqualTo(StoryStatus.SUCCESS);
        assertThat(resumed.stories().get("story-002").status())
                .isEqualTo(StoryStatus.IN_PROGRESS);
        assertThat(resumed.stories().get("story-003").status())
                .isEqualTo(StoryStatus.BLOCKED);
        assertThat(resumed.stories().get("story-004").status())
                .isEqualTo(StoryStatus.IN_PROGRESS);
        assertThat(resumed.stories().get("story-005").status())
                .isEqualTo(StoryStatus.PENDING);
    }

    @Test
    void reevaluateBlocked_emptyBlockedBy_remainsBlocked() {
        var stories = Map.of(
                "s1",
                StoryEntry.pending(0)
                        .withStatus(StoryStatus.BLOCKED)
        );

        var result = ResumeHandler.reevaluateBlocked(stories);

        assertThat(result.get("s1").status())
                .isEqualTo(StoryStatus.BLOCKED);
    }

    @Test
    void reclassifyStories_preservesNonRetriableStatuses() {
        var stories = Map.of(
                "s1",
                StoryEntry.pending(0)
                        .withStatus(StoryStatus.SUCCESS)
                        .withDuration(60_000L),
                "s2",
                StoryEntry.pending(0)
                        .withStatus(StoryStatus.BLOCKED)
        );

        var result = ResumeHandler.reclassifyStories(stories, 3);

        assertThat(result.get("s1").status())
                .isEqualTo(StoryStatus.SUCCESS);
        assertThat(result.get("s2").status())
                .isEqualTo(StoryStatus.BLOCKED);
    }
}
