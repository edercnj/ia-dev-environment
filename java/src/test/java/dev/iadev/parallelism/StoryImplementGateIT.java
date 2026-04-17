package dev.iadev.parallelism;

import static org.assertj.core.api.Assertions.assertThat;

import dev.iadev.checkpoint.ExecutionMetrics;
import dev.iadev.checkpoint.ExecutionMode;
import dev.iadev.checkpoint.ExecutionState;
import dev.iadev.checkpoint.JacksonCheckpointPersistence;
import dev.iadev.checkpoint.ParallelismDowngrade;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Story-0041-0006 integration validation for the
 * {@code x-story-implement} parallelism gate (Phase 1.5).
 *
 * <p>Mirrors {@link EpicImplementGateIT} but at story + task scope. The
 * gate runs inside an LLM-driven skill so the IT validates (1) that the
 * skill source of truth documents the gate contract and (2) that the
 * per-story {@code execution-state.json} round-trip records task-level
 * downgrades with RULE-006 backward compatibility guarantees.
 */
@DisplayName("x-story-implement Phase 1.5 parallelism gate")
class StoryImplementGateIT {

    @TempDir
    Path tmp;

    private static final Path STORY_IMPLEMENT_SKILL = Path.of(
            "src", "main", "resources", "targets", "claude",
            "skills", "core", "dev", "x-story-implement",
            "SKILL.md");

    // ------------------------------------------------------------
    // Structural contracts on SKILL.md
    // ------------------------------------------------------------

    @Test
    @DisplayName("SKILL.md documents Phase 1.5 gate invocation")
    void skill_documentsGateInvocation() throws IOException {
        String content = Files.readString(STORY_IMPLEMENT_SKILL);

        assertThat(content)
                .contains("## Phase 1.5")
                .contains("x-parallel-eval")
                .contains("--scope=task");
    }

    @Test
    @DisplayName("SKILL.md documents exit code → action table")
    void skill_documentsExitCodeTable() throws IOException {
        String content = Files.readString(STORY_IMPLEMENT_SKILL);

        assertThat(content)
                .contains("Exit Code")
                .contains("`0`")
                .contains("`1`")
                .contains("`2`")
                .contains("Downgrade the affected wave to serial");
    }

    @Test
    @DisplayName("SKILL.md documents fail-open contract")
    void skill_documentsFailOpen() throws IOException {
        String content = Files.readString(STORY_IMPLEMENT_SKILL);

        assertThat(content)
                .contains("Fail-open")
                .contains("indispon")
                .contains("RULE-005");
    }

    @Test
    @DisplayName("SKILL.md documents single-task skip")
    void skill_documentsSingleTaskSkip() throws IOException {
        String content = Files.readString(STORY_IMPLEMENT_SKILL);

        assertThat(content)
                .contains("sem paralelismo de tasks nesta story");
    }

    // ------------------------------------------------------------
    // End-to-end persistence round-trip on ExecutionState
    // ------------------------------------------------------------

    @Test
    @DisplayName(
            "task-level hard conflict persists in execution-state.json")
    void hardConflict_betweenTasks_persists() throws IOException {
        ParallelismDowngrade downgrade = new ParallelismDowngrade(
                1,
                List.of("TASK-0041-0006-003",
                        "TASK-0041-0006-004"),
                List.of(List.of("TASK-0041-0006-003"),
                        List.of("TASK-0041-0006-004")),
                "hard conflict on ExecutionState.java",
                Instant.parse("2026-04-17T10:00:00Z"));
        ExecutionState state = baseState()
                .withParallelismDowngrades(List.of(downgrade));

        Path file = tmp.resolve("execution-state.json");
        new JacksonCheckpointPersistence().save(state, file);

        String json = Files.readString(file);
        assertThat(json)
                .contains("TASK-0041-0006-003")
                .contains("TASK-0041-0006-004")
                .contains("hard conflict on ExecutionState.java");

        ExecutionState restored =
                new JacksonCheckpointPersistence().load(file);
        assertThat(restored.parallelismDowngrades())
                .containsExactly(downgrade);
    }

    @Test
    @DisplayName("fail-open at story scope leaves state unchanged")
    void failOpen_atStoryScope_noDowngradeRecorded()
            throws IOException {
        ExecutionState state = baseState();

        Path file = tmp.resolve("execution-state.json");
        new JacksonCheckpointPersistence().save(state, file);

        String json = Files.readString(file);
        assertThat(json).doesNotContain("parallelismDowngrades");

        ExecutionState restored =
                new JacksonCheckpointPersistence().load(file);
        assertThat(restored.parallelismDowngrades()).isNull();
    }

    @Test
    @DisplayName(
            "accumulates multiple waves' downgrades in the same array")
    void multipleWaves_accumulateDowngrades()
            throws IOException {
        ParallelismDowngrade wave1 = new ParallelismDowngrade(
                1,
                List.of("TASK-A", "TASK-B"),
                List.of(List.of("TASK-A"), List.of("TASK-B")),
                "regen conflict",
                Instant.parse("2026-04-17T10:00:00Z"));
        ParallelismDowngrade wave2 = new ParallelismDowngrade(
                2,
                List.of("TASK-C", "TASK-D"),
                List.of(List.of("TASK-C"), List.of("TASK-D")),
                "hard conflict on Foo.java",
                Instant.parse("2026-04-17T10:05:00Z"));
        ExecutionState state = baseState()
                .withParallelismDowngrades(List.of(wave1, wave2));

        Path file = tmp.resolve("execution-state.json");
        new JacksonCheckpointPersistence().save(state, file);
        ExecutionState restored =
                new JacksonCheckpointPersistence().load(file);

        assertThat(restored.parallelismDowngrades())
                .containsExactly(wave1, wave2);
    }

    // ------------------------------------------------------------
    // Helpers
    // ------------------------------------------------------------

    private static ExecutionState baseState() {
        return new ExecutionState(
                "EPIC-0041",
                "feat/epic-0041",
                Instant.parse("2026-04-17T09:00:00Z"),
                0,
                ExecutionMode.FULL,
                Map.of(),
                Map.of(),
                ExecutionMetrics.initial(0));
    }
}
