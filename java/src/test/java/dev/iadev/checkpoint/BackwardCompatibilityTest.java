package dev.iadev.checkpoint;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests backward compatibility for execution-state.json schema
 * evolution from v1.0 to v2.0 (RULE-010).
 */
class BackwardCompatibilityTest {

    @TempDir
    Path tempDir;

    private JacksonCheckpointPersistence persistence;

    @BeforeEach
    void setUp() {
        persistence = new JacksonCheckpointPersistence();
    }

    @Nested
    class V1JsonDeserialization {

        @Test
        void load_v1JsonWithoutTasks_defaultsToEmptyTasks()
                throws Exception {
            var json = """
                    {
                      "epicId": "EPIC-001",
                      "branch": "main",
                      "startedAt": "2026-03-19T10:00:00Z",
                      "currentPhase": 0,
                      "mode": "FULL",
                      "stories": {
                        "s1": {
                          "status": "SUCCESS",
                          "commitSha": "abc123",
                          "phase": 0,
                          "duration": 120000,
                          "retries": 0,
                          "blockedBy": [],
                          "summary": "Done",
                          "findingsCount": 0
                        }
                      },
                      "integrityGates": {},
                      "metrics": {
                        "storiesCompleted": 1,
                        "storiesTotal": 1,
                        "storiesFailed": 0,
                        "storiesBlocked": 0,
                        "estimatedRemainingMinutes": 0.0,
                        "elapsedMs": 120000,
                        "averageStoryDurationMs": 120000.0,
                        "storyDurations": {},
                        "phaseDurations": {}
                      }
                    }
                    """;
            var path = tempDir.resolve("v1-state.json");
            Files.writeString(path, json);

            var loaded = persistence.load(path);

            assertThat(loaded.version())
                    .isEqualTo(ExecutionState.VERSION_1_0);
            assertThat(loaded.epicId()).isEqualTo("EPIC-001");
            var story = loaded.stories().get("s1");
            assertThat(story.tasks()).isEmpty();
            assertThat(story.parentBranch()).isNull();
            assertThat(story.status())
                    .isEqualTo(StoryStatus.SUCCESS);
            assertThat(story.commitSha()).isEqualTo("abc123");
            assertThat(story.duration()).isEqualTo(120_000L);
        }

        @Test
        void load_v1JsonWithoutVersion_defaultsToV1()
                throws Exception {
            var json = """
                    {
                      "epicId": "EPIC-001",
                      "branch": "main",
                      "startedAt": "2026-03-19T10:00:00Z",
                      "currentPhase": 0,
                      "mode": "FULL",
                      "stories": {
                        "s1": {
                          "status": "PENDING",
                          "commitSha": null,
                          "phase": 0,
                          "duration": 0,
                          "retries": 0,
                          "blockedBy": [],
                          "summary": null,
                          "findingsCount": 0
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
            var path = tempDir.resolve("no-version.json");
            Files.writeString(path, json);

            var loaded = persistence.load(path);

            assertThat(loaded.version())
                    .isEqualTo(ExecutionState.VERSION_1_0);
        }
    }

    @Nested
    class V2JsonRoundTrip {

        @Test
        void saveAndLoad_v2WithTasks_preservesAllFields()
                throws Exception {
            var start = Instant.parse("2026-04-07T10:00:00Z");
            var end = Instant.parse("2026-04-07T11:30:00Z");

            var task1 = new TaskEntry(
                    "TASK-0029-0001-001", TaskStatus.DONE,
                    "https://github.com/org/repo/pull/42",
                    42, "feat/task-0029-0001-001",
                    start, end, 1, null
            );
            var task2 = TaskEntry.pending("TASK-0029-0001-002")
                    .withStatus(TaskStatus.IN_PROGRESS)
                    .withBranch("feat/task-0029-0001-002")
                    .withStartedAt(end)
                    .withAttempts(1);

            var tasks = new LinkedHashMap<String, TaskEntry>();
            tasks.put("TASK-0029-0001-001", task1);
            tasks.put("TASK-0029-0001-002", task2);

            var story = new StoryEntry(
                    StoryStatus.IN_PROGRESS, null, 0,
                    0L, 1, List.of(), null, 0,
                    tasks, "feat/story-0029-0001"
            );

            var state = new ExecutionState(
                    ExecutionState.VERSION_2_0,
                    "epic-0029", "feat/epic-0029",
                    start, 0, ExecutionMode.FULL,
                    Map.of("story-0029-0001", story),
                    Map.of(),
                    ExecutionMetrics.initial(1)
            );

            var path = tempDir.resolve("v2-state.json");
            persistence.save(state, path);
            var loaded = persistence.load(path);

            assertThat(loaded.version())
                    .isEqualTo(ExecutionState.VERSION_2_0);
            var loadedStory =
                    loaded.stories().get("story-0029-0001");
            assertThat(loadedStory.tasks()).hasSize(2);
            assertThat(loadedStory.parentBranch())
                    .isEqualTo("feat/story-0029-0001");

            var loadedTask1 = loadedStory.tasks()
                    .get("TASK-0029-0001-001");
            assertThat(loadedTask1.taskId())
                    .isEqualTo("TASK-0029-0001-001");
            assertThat(loadedTask1.status())
                    .isEqualTo(TaskStatus.DONE);
            assertThat(loadedTask1.prUrl())
                    .isEqualTo(
                            "https://github.com/org/repo/pull/42"
                    );
            assertThat(loadedTask1.prNumber()).isEqualTo(42);
            assertThat(loadedTask1.branch())
                    .isEqualTo("feat/task-0029-0001-001");
            assertThat(loadedTask1.startedAt()).isEqualTo(start);
            assertThat(loadedTask1.completedAt()).isEqualTo(end);
            assertThat(loadedTask1.attempts()).isEqualTo(1);
            assertThat(loadedTask1.failureReason()).isNull();

            var loadedTask2 = loadedStory.tasks()
                    .get("TASK-0029-0001-002");
            assertThat(loadedTask2.status())
                    .isEqualTo(TaskStatus.IN_PROGRESS);
            assertThat(loadedTask2.prUrl()).isNull();
            assertThat(loadedTask2.prNumber()).isNull();
        }

        @Test
        void saveAndLoad_v2Timestamp_preservesMillisecondPrecision()
                throws Exception {
            var instant = Instant.parse(
                    "2026-04-07T10:00:00.123Z");
            var task = TaskEntry.pending("TASK-0029-0001-001")
                    .withStartedAt(instant)
                    .withCompletedAt(instant);
            var story = StoryEntry.pending(0)
                    .withTasks(Map.of(
                            "TASK-0029-0001-001", task));
            var state = new ExecutionState(
                    ExecutionState.VERSION_2_0,
                    "EPIC-001", "main", instant, 0,
                    ExecutionMode.FULL,
                    Map.of("s1", story), Map.of(),
                    ExecutionMetrics.initial(1)
            );
            var path = tempDir.resolve("timestamp-test.json");

            persistence.save(state, path);
            var loaded = persistence.load(path);

            var loadedTask = loaded.stories().get("s1")
                    .tasks().get("TASK-0029-0001-001");
            assertThat(loadedTask.startedAt())
                    .isEqualTo(instant);
            assertThat(loadedTask.completedAt())
                    .isEqualTo(instant);
        }
    }

    @Nested
    class MixedVersionHandling {

        @Test
        void load_v2JsonWithExtraFields_ignoredGracefully()
                throws Exception {
            var json = """
                    {
                      "version": "2.0",
                      "epicId": "EPIC-001",
                      "branch": "main",
                      "startedAt": "2026-03-19T10:00:00Z",
                      "currentPhase": 0,
                      "mode": "FULL",
                      "futureField": "should be ignored",
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
                          "parentBranch": "feat/story",
                          "tasks": {
                            "TASK-0029-0001-001": {
                              "taskId": "TASK-0029-0001-001",
                              "status": "PENDING",
                              "prUrl": null,
                              "prNumber": null,
                              "branch": null,
                              "startedAt": null,
                              "completedAt": null,
                              "attempts": 0,
                              "failureReason": null,
                              "futureTaskField": true
                            }
                          }
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
            var path = tempDir.resolve("v2-extra.json");
            Files.writeString(path, json);

            var loaded = persistence.load(path);

            assertThat(loaded.version())
                    .isEqualTo(ExecutionState.VERSION_2_0);
            assertThat(loaded.stories().get("s1")
                    .parentBranch())
                    .isEqualTo("feat/story");
            assertThat(loaded.stories().get("s1")
                    .tasks()).hasSize(1);
        }

        @Test
        void constructor_backwardCompatible_8params_setsDefaults() {
            var state = new ExecutionState(
                    "EPIC-001", "main",
                    Instant.now(), 0, ExecutionMode.FULL,
                    Map.of("s1", StoryEntry.pending(0)),
                    Map.of(),
                    ExecutionMetrics.initial(1)
            );

            assertThat(state.version())
                    .isEqualTo(ExecutionState.VERSION_1_0);
        }

        @Test
        void storyEntry_backwardCompatible_8params_setsDefaults() {
            var entry = new StoryEntry(
                    StoryStatus.PENDING, null, 0, 0L, 0,
                    List.of(), null, 0
            );

            assertThat(entry.tasks()).isEmpty();
            assertThat(entry.parentBranch()).isNull();
        }
    }
}
