package dev.iadev.checkpoint;

import dev.iadev.exception.CheckpointIOException;
import dev.iadev.exception.CheckpointValidationException;
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

/**
 * Tests for {@link JacksonCheckpointPersistence}.
 */
class JacksonCheckpointPersistenceTest {

    @TempDir
    Path tempDir;

    private JacksonCheckpointPersistence persistence;

    @BeforeEach
    void setUp() {
        persistence = new JacksonCheckpointPersistence();
    }

    private ExecutionState createSampleState() {
        var stories = new LinkedHashMap<String, StoryEntry>();
        stories.put(
                "story-0006-0001",
                new StoryEntry(
                        StoryStatus.SUCCESS, "abc123", 0,
                        120_000L, 0, List.of(),
                        "Projeto Maven criado", 0
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
    void save_whenCalled_producesIndentedJson() throws Exception {
        var state = createSampleState();
        var path = tempDir.resolve("formatted.json");

        persistence.save(state, path);
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

        persistence.save(state, path);

        assertThat(path).exists();
        assertThat(tmpPath).doesNotExist();
    }

    @Test
    void saveAndLoad_roundTrip_preservesState() {
        var original = createSampleState();
        var path = tempDir.resolve("execution-state.json");

        persistence.save(original, path);
        var loaded = persistence.load(path);

        assertThat(loaded.epicId())
                .isEqualTo(original.epicId());
        assertThat(loaded.branch())
                .isEqualTo(original.branch());
        assertThat(loaded.startedAt())
                .isEqualTo(original.startedAt());
        assertThat(loaded.currentPhase())
                .isEqualTo(original.currentPhase());
        assertThat(loaded.mode())
                .isEqualTo(original.mode());
        assertThat(loaded.stories()).hasSize(3);
        assertThat(loaded.integrityGates()).hasSize(1);
    }

    @Test
    void saveAndLoad_whenCalled_preservesStoryFields() {
        var original = createSampleState();
        var path = tempDir.resolve("execution-state.json");

        persistence.save(original, path);
        var loaded = persistence.load(path);

        var story = loaded.stories().get("story-0006-0001");
        assertThat(story.status())
                .isEqualTo(StoryStatus.SUCCESS);
        assertThat(story.commitSha()).isEqualTo("abc123");
        assertThat(story.phase()).isZero();
        assertThat(story.duration()).isEqualTo(120_000L);
        assertThat(story.retries()).isZero();
        assertThat(story.blockedBy()).isEmpty();
        assertThat(story.summary())
                .isEqualTo("Projeto Maven criado");
        assertThat(story.findingsCount()).isZero();
    }

    @Test
    void saveAndLoad_whenCalled_preservesIntegrityGates() {
        var original = createSampleState();
        var path = tempDir.resolve("execution-state.json");

        persistence.save(original, path);
        var loaded = persistence.load(path);

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
    void saveAndLoad_whenCalled_preservesInstantPrecision() {
        var instant = Instant.parse(
                "2026-03-19T10:00:00.123Z"
        );
        var stories = Map.of("s1", StoryEntry.pending(0));
        var state = new ExecutionState(
                "EPIC-001", "main", instant, 0,
                ExecutionMode.FULL, stories, Map.of(),
                ExecutionMetrics.initial(1)
        );
        var path = tempDir.resolve("instant-test.json");

        persistence.save(state, path);
        var loaded = persistence.load(path);

        assertThat(loaded.startedAt()).isEqualTo(instant);
    }

    @Test
    void load_nonExistentFile_throwsCheckpointIOException() {
        var path = tempDir.resolve("missing.json");

        assertThatThrownBy(() -> persistence.load(path))
                .isInstanceOf(CheckpointIOException.class)
                .hasMessageContaining(
                        "Failed to load checkpoint"
                );
    }

    @Test
    void load_invalidJson_throwsCheckpointIOException()
            throws Exception {
        var path = tempDir.resolve("bad.json");
        Files.writeString(path, "not valid json {{{");

        assertThatThrownBy(() -> persistence.load(path))
                .isInstanceOf(CheckpointIOException.class);
    }

    @Test
    void load_invalidState_throwsValidationException()
            throws Exception {
        var json = """
                {
                  "epicId": null,
                  "branch": "main",
                  "startedAt": "2026-03-19T10:00:00Z",
                  "currentPhase": 0,
                  "mode": "FULL",
                  "stories": {},
                  "integrityGates": {},
                  "metrics": {
                    "storiesCompleted": 0,
                    "storiesTotal": 0,
                    "storiesFailed": 0,
                    "storiesBlocked": 0,
                    "estimatedRemainingMinutes": 0.0,
                    "elapsedMs": 0,
                    "averageStoryDurationMs": 0.0,
                    "storyDurations": {},
                    "phaseDurations": {}
                  }
                }
                """;
        var path = tempDir.resolve("invalid.json");
        Files.writeString(path, json);

        assertThatThrownBy(() -> persistence.load(path))
                .isInstanceOf(CheckpointValidationException.class)
                .hasMessageContaining("epicId is required");
    }

    @Test
    void load_unknownProperties_ignored() throws Exception {
        var json = """
                {
                  "epicId": "EPIC-001",
                  "branch": "main",
                  "startedAt": "2026-03-19T10:00:00Z",
                  "currentPhase": 0,
                  "mode": "FULL",
                  "unknownField": "should be ignored",
                  "stories": {
                    "s1": {
                      "status": "PENDING",
                      "commitSha": null,
                      "phase": 0,
                      "duration": 0,
                      "retries": 0,
                      "blockedBy": [],
                      "summary": null,
                      "findingsCount": 0,
                      "extraField": true
                    }
                  },
                  "integrityGates": {},
                  "metrics": {
                    "storiesCompleted": 0,
                    "storiesTotal": 1,
                    "storiesFailed": 0,
                    "storiesBlocked": 0,
                    "estimatedRemainingMinutes": 0.0,
                    "elapsedMs": 0,
                    "averageStoryDurationMs": 0.0,
                    "storyDurations": {},
                    "phaseDurations": {}
                  }
                }
                """;
        var path = tempDir.resolve("extra-fields.json");
        Files.writeString(path, json);

        var loaded = persistence.load(path);

        assertThat(loaded.epicId()).isEqualTo("EPIC-001");
        assertThat(loaded.stories()).hasSize(1);
    }

    @Test
    void create_whenCalled_implementsCheckpointPersistenceInterface() {
        assertThat(persistence)
                .isInstanceOf(CheckpointPersistence.class);
    }
}
