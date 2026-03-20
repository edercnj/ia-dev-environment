package dev.iadev.checkpoint;

import dev.iadev.exception.CheckpointIOException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;

/**
 * Tests for {@link CheckpointEngine} save/load/update operations.
 */
class CheckpointEngineTest {

    @TempDir
    Path tempDir;

    private CheckpointEngine engine;

    @BeforeEach
    void setUp() {
        engine = new CheckpointEngine(
                new JacksonCheckpointPersistence()
        );
    }

    private ExecutionState createSampleState() {
        var stories = new LinkedHashMap<String, StoryEntry>();
        stories.put(
                "story-0006-0001",
                new StoryEntry(
                        StoryStatus.SUCCESS, "abc123", 0, 120_000L,
                        0, List.of(), "Projeto Maven criado", 0
                )
        );
        stories.put("story-0006-0002", StoryEntry.pending(0));
        stories.put("story-0006-0003", StoryEntry.pending(1));

        var gates = new LinkedHashMap<String, IntegrityGateEntry>();
        gates.put(
                "compilation",
                new IntegrityGateEntry(
                        "compilation", true, null,
                        Instant.parse("2026-03-19T10:05:00Z")
                )
        );

        return new ExecutionState(
                "EPIC-0006",
                "feat/epic-0006",
                Instant.parse("2026-03-19T10:00:00Z"),
                0,
                ExecutionMode.FULL,
                Map.copyOf(stories),
                Map.copyOf(gates),
                ExecutionMetrics.initial(3)
        );
    }

    @Test
    void saveAndLoad_roundTrip_preservesState() {
        var original = createSampleState();
        var path = tempDir.resolve("execution-state.json");

        engine.save(original, path);
        var loaded = engine.load(path);

        assertThat(loaded.epicId()).isEqualTo(original.epicId());
        assertThat(loaded.branch()).isEqualTo(original.branch());
        assertThat(loaded.startedAt())
                .isEqualTo(original.startedAt());
        assertThat(loaded.currentPhase())
                .isEqualTo(original.currentPhase());
        assertThat(loaded.mode()).isEqualTo(original.mode());
        assertThat(loaded.stories()).hasSize(3);
        assertThat(loaded.integrityGates()).hasSize(1);
    }

    @Test
    void saveAndLoad_preservesStoryFields() {
        var original = createSampleState();
        var path = tempDir.resolve("execution-state.json");

        engine.save(original, path);
        var loaded = engine.load(path);

        var story = loaded.stories().get("story-0006-0001");
        assertThat(story.status()).isEqualTo(StoryStatus.SUCCESS);
        assertThat(story.commitSha()).isEqualTo("abc123");
        assertThat(story.phase()).isZero();
        assertThat(story.duration()).isEqualTo(120_000L);
        assertThat(story.retries()).isZero();
        assertThat(story.blockedBy()).isEmpty();
        assertThat(story.summary()).isEqualTo("Projeto Maven criado");
        assertThat(story.findingsCount()).isZero();
    }

    @Test
    void saveAndLoad_preservesIntegrityGates() {
        var original = createSampleState();
        var path = tempDir.resolve("execution-state.json");

        engine.save(original, path);
        var loaded = engine.load(path);

        var gate = loaded.integrityGates().get("compilation");
        assertThat(gate.gateName()).isEqualTo("compilation");
        assertThat(gate.passed()).isTrue();
        assertThat(gate.message()).isNull();
        assertThat(gate.timestamp())
                .isEqualTo(
                        Instant.parse("2026-03-19T10:05:00Z")
                );
    }

    @Test
    void saveAndLoad_preservesInstantPrecision() {
        var instant = Instant.parse("2026-03-19T10:00:00.123Z");
        var stories = Map.of(
                "s1", StoryEntry.pending(0)
        );
        var state = new ExecutionState(
                "EPIC-001", "main", instant, 0,
                ExecutionMode.FULL, stories, Map.of(),
                ExecutionMetrics.initial(1)
        );
        var path = tempDir.resolve("instant-test.json");

        engine.save(state, path);
        var loaded = engine.load(path);

        assertThat(loaded.startedAt()).isEqualTo(instant);
    }

    @Test
    void save_producesIndentedJson() throws Exception {
        var state = createSampleState();
        var path = tempDir.resolve("formatted.json");

        engine.save(state, path);
        var json = Files.readString(path);

        assertThat(json).contains("\n");
        assertThat(json).contains("  ");
        assertThat(json).contains("\"epicId\"");
    }

    @Test
    void save_writesAtomically_tmpFileRemoved() {
        var state = createSampleState();
        var path = tempDir.resolve("atomic.json");
        var tmpPath = tempDir.resolve(".atomic.json.tmp");

        engine.save(state, path);

        assertThat(path).exists();
        assertThat(tmpPath).doesNotExist();
    }

    @Test
    void load_nonExistentFile_throwsCheckpointIOException() {
        var path = tempDir.resolve("missing.json");

        assertThatThrownBy(() -> engine.load(path))
                .isInstanceOf(CheckpointIOException.class)
                .hasMessageContaining("Failed to load checkpoint");
    }

    @Test
    void load_invalidJson_throwsCheckpointIOException()
            throws Exception {
        var path = tempDir.resolve("bad.json");
        Files.writeString(path, "not valid json {{{");

        assertThatThrownBy(() -> engine.load(path))
                .isInstanceOf(CheckpointIOException.class);
    }

    @Test
    void updateStory_returnsNewState_originalUnchanged() {
        var state = createSampleState();
        var updated = CheckpointEngine.updateStory(
                state,
                "story-0006-0002",
                StoryEntry.pending(0)
                        .withStatus(StoryStatus.IN_PROGRESS)
        );

        assertThat(updated.stories()
                .get("story-0006-0002").status())
                .isEqualTo(StoryStatus.IN_PROGRESS);
        assertThat(state.stories()
                .get("story-0006-0002").status())
                .isEqualTo(StoryStatus.PENDING);
    }

    @Test
    void updateStory_pendingToInProgressToSuccess() {
        var state = createSampleState();

        var inProgress = CheckpointEngine.updateStory(
                state, "story-0006-0002",
                StoryEntry.pending(0)
                        .withStatus(StoryStatus.IN_PROGRESS)
        );
        assertThat(inProgress.stories()
                .get("story-0006-0002").status())
                .isEqualTo(StoryStatus.IN_PROGRESS);

        var success = CheckpointEngine.updateStory(
                inProgress, "story-0006-0002",
                inProgress.stories().get("story-0006-0002")
                        .withStatus(StoryStatus.SUCCESS)
                        .withCommitSha("abc123")
        );
        assertThat(success.stories()
                .get("story-0006-0002").status())
                .isEqualTo(StoryStatus.SUCCESS);
        assertThat(success.stories()
                .get("story-0006-0002").commitSha())
                .isEqualTo("abc123");
    }

    @Test
    void updateStory_failedIncrementsRetries() {
        var state = createSampleState();

        var failed = CheckpointEngine.updateStory(
                state, "story-0006-0002",
                StoryEntry.pending(0)
                        .withStatus(StoryStatus.FAILED)
                        .withRetries(1)
        );

        assertThat(failed.stories()
                .get("story-0006-0002").status())
                .isEqualTo(StoryStatus.FAILED);
        assertThat(failed.stories()
                .get("story-0006-0002").retries())
                .isEqualTo(1);
    }

    @Test
    void updateMetrics_calculatesFromStoryStates() {
        var stories = new LinkedHashMap<String, StoryEntry>();
        stories.put(
                "s1",
                StoryEntry.pending(0)
                        .withStatus(StoryStatus.SUCCESS)
                        .withDuration(60_000L)
        );
        stories.put(
                "s2",
                StoryEntry.pending(0)
                        .withStatus(StoryStatus.SUCCESS)
                        .withDuration(120_000L)
        );
        stories.put(
                "s3",
                StoryEntry.pending(0)
                        .withStatus(StoryStatus.SUCCESS)
                        .withDuration(90_000L)
        );
        stories.put(
                "s4",
                StoryEntry.pending(0)
                        .withStatus(StoryStatus.SUCCESS)
                        .withDuration(130_000L)
        );
        for (int i = 5; i <= 10; i++) {
            stories.put("s" + i, StoryEntry.pending(1));
        }

        var state = new ExecutionState(
                "EPIC-001", "main",
                Instant.parse("2026-03-19T10:00:00Z"),
                0, ExecutionMode.FULL,
                Map.copyOf(stories), Map.of(),
                ExecutionMetrics.initial(10)
        );

        var updated = CheckpointEngine.updateMetrics(state);

        assertThat(updated.metrics().storiesCompleted())
                .isEqualTo(4);
        assertThat(updated.metrics().storiesTotal()).isEqualTo(10);
        assertThat(updated.metrics().averageStoryDurationMs())
                .isCloseTo(100_000.0, within(0.1));
        assertThat(updated.metrics().estimatedRemainingMinutes())
                .isCloseTo(10.0, within(0.1));
    }

    @Test
    void updateMetrics_countsFailedAndBlocked() {
        var stories = new LinkedHashMap<String, StoryEntry>();
        stories.put(
                "s1",
                StoryEntry.pending(0)
                        .withStatus(StoryStatus.SUCCESS)
                        .withDuration(60_000L)
        );
        stories.put(
                "s2",
                StoryEntry.pending(0)
                        .withStatus(StoryStatus.FAILED)
        );
        stories.put(
                "s3",
                StoryEntry.pending(0)
                        .withStatus(StoryStatus.BLOCKED)
        );

        var state = new ExecutionState(
                "EPIC-001", "main",
                Instant.now(), 0, ExecutionMode.FULL,
                Map.copyOf(stories), Map.of(),
                ExecutionMetrics.initial(3)
        );

        var updated = CheckpointEngine.updateMetrics(state);

        assertThat(updated.metrics().storiesFailed()).isEqualTo(1);
        assertThat(updated.metrics().storiesBlocked()).isEqualTo(1);
        assertThat(updated.metrics().storiesCompleted()).isEqualTo(1);
    }

    @Test
    void updateMetrics_zeroCompleted_zeroAverage() {
        var stories = Map.of(
                "s1", StoryEntry.pending(0),
                "s2", StoryEntry.pending(0)
        );
        var state = new ExecutionState(
                "EPIC-001", "main",
                Instant.now(), 0, ExecutionMode.FULL,
                stories, Map.of(),
                ExecutionMetrics.initial(2)
        );

        var updated = CheckpointEngine.updateMetrics(state);

        assertThat(updated.metrics().averageStoryDurationMs())
                .isZero();
        assertThat(updated.metrics().estimatedRemainingMinutes())
                .isZero();
    }

    @Test
    void updateMetrics_tracksPhaseDurations() {
        var stories = new LinkedHashMap<String, StoryEntry>();
        stories.put(
                "s1",
                StoryEntry.pending(0)
                        .withStatus(StoryStatus.SUCCESS)
                        .withDuration(60_000L)
        );
        stories.put(
                "s2",
                StoryEntry.pending(0)
                        .withStatus(StoryStatus.SUCCESS)
                        .withDuration(40_000L)
        );
        stories.put(
                "s3",
                StoryEntry.pending(1)
                        .withStatus(StoryStatus.SUCCESS)
                        .withDuration(80_000L)
        );

        var state = new ExecutionState(
                "EPIC-001", "main",
                Instant.now(), 0, ExecutionMode.FULL,
                Map.copyOf(stories), Map.of(),
                ExecutionMetrics.initial(3)
        );

        var updated = CheckpointEngine.updateMetrics(state);

        assertThat(updated.metrics().phaseDurations())
                .containsEntry(0, 100_000L)
                .containsEntry(1, 80_000L);
    }

    @Test
    void updateMetrics_tracksStoryDurations() {
        var stories = new LinkedHashMap<String, StoryEntry>();
        stories.put(
                "s1",
                StoryEntry.pending(0)
                        .withStatus(StoryStatus.SUCCESS)
                        .withDuration(60_000L)
        );
        stories.put(
                "s2",
                StoryEntry.pending(0)
                        .withStatus(StoryStatus.FAILED)
                        .withDuration(30_000L)
        );

        var state = new ExecutionState(
                "EPIC-001", "main",
                Instant.now(), 0, ExecutionMode.FULL,
                Map.copyOf(stories), Map.of(),
                ExecutionMetrics.initial(2)
        );

        var updated = CheckpointEngine.updateMetrics(state);

        assertThat(updated.metrics().storyDurations())
                .containsEntry("s1", 60_000L)
                .containsEntry("s2", 30_000L);
    }

    @Test
    void fullRoundTrip_saveLoadUpdateSaveLoad() {
        var state = createSampleState();
        var path = tempDir.resolve("roundtrip.json");

        engine.save(state, path);
        var loaded1 = engine.load(path);

        var updated = CheckpointEngine.updateStory(
                loaded1, "story-0006-0002",
                loaded1.stories().get("story-0006-0002")
                        .withStatus(StoryStatus.SUCCESS)
                        .withCommitSha("def456")
                        .withDuration(90_000L)
        );
        updated = CheckpointEngine.updateMetrics(updated);

        engine.save(updated, path);
        var loaded2 = engine.load(path);

        assertThat(loaded2.stories()
                .get("story-0006-0002").status())
                .isEqualTo(StoryStatus.SUCCESS);
        assertThat(loaded2.stories()
                .get("story-0006-0002").commitSha())
                .isEqualTo("def456");
        assertThat(loaded2.metrics().storiesCompleted())
                .isEqualTo(2);
    }
}
