package dev.iadev.parallelism;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
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
 * {@code x-epic-implement} parallelism gate (Phase 0.5.0).
 *
 * <p>The gate itself runs inside an LLM-driven skill so the IT is
 * structural + persistence: it validates (1) that the skill source of
 * truth documents the gate contract, and (2) that the
 * {@code execution-state.json} round-trip correctly records the
 * downgrade decisions with RULE-006 backward compatibility guarantees.
 */
@DisplayName("x-epic-implement Phase 0.5.0 parallelism gate")
class EpicImplementGateIT {

    @TempDir
    Path tmp;

    private static final Path EPIC_IMPLEMENT_SKILL = Path.of(
            "src", "main", "resources", "targets", "claude",
            "skills", "core", "dev", "x-epic-implement",
            "SKILL.md");

    // ------------------------------------------------------------
    // Structural contracts on SKILL.md
    // ------------------------------------------------------------

    @Test
    @DisplayName("SKILL.md documents Phase 0.5.0 gate invocation")
    void skill_documentsGateInvocation() throws IOException {
        String content = Files.readString(EPIC_IMPLEMENT_SKILL);

        assertThat(content)
                .contains("### 0.5.0 Parallelism Gate")
                .contains("x-parallel-eval")
                .contains("--scope=epic");
    }

    @Test
    @DisplayName("SKILL.md documents exit code → action table")
    void skill_documentsExitCodeTable() throws IOException {
        String content = Files.readString(EPIC_IMPLEMENT_SKILL);

        assertThat(content)
                .contains("Exit Code")
                .contains("`0`")
                .contains("`1`")
                .contains("`2`")
                .contains("Downgrade the affected phase to serial");
    }

    @Test
    @DisplayName("SKILL.md documents fail-open contract")
    void skill_documentsFailOpen() throws IOException {
        String content = Files.readString(EPIC_IMPLEMENT_SKILL);

        assertThat(content)
                .contains("Fail-open")
                .contains("indispon")
                .contains("RULE-005");
    }

    @Test
    @DisplayName("SKILL.md documents parallelismDowngrades shape")
    void skill_documentsPersistenceShape() throws IOException {
        String content = Files.readString(EPIC_IMPLEMENT_SKILL);

        assertThat(content)
                .contains("parallelismDowngrades")
                .contains("originalGroup")
                .contains("adjustedSequence")
                .contains("evaluatedAt");
    }

    // ------------------------------------------------------------
    // End-to-end persistence round-trip on ExecutionState
    // ------------------------------------------------------------

    @Test
    @DisplayName(
            "hard-conflict downgrade persists in execution-state.json")
    void hardConflict_persistsDowngrade() throws IOException {
        ParallelismDowngrade downgrade = new ParallelismDowngrade(
                3,
                List.of("story-0041-0006", "story-0041-0007"),
                List.of(List.of("story-0041-0006"),
                        List.of("story-0041-0007")),
                "hard conflict on SettingsAssembler.java",
                Instant.parse("2026-04-17T10:00:00Z"));
        ExecutionState state = baseState()
                .withParallelismDowngrades(List.of(downgrade));

        Path file = tmp.resolve("execution-state.json");
        new JacksonCheckpointPersistence().save(state, file);

        String json = Files.readString(file);
        assertThat(json)
                .contains("parallelismDowngrades")
                .contains("story-0041-0006")
                .contains("story-0041-0007")
                .contains("hard conflict on SettingsAssembler.java");

        ExecutionState restored =
                new JacksonCheckpointPersistence().load(file);
        assertThat(restored.parallelismDowngrades())
                .containsExactly(downgrade);
    }

    @Test
    @DisplayName("fail-open leaves parallelismDowngrades unset")
    void failOpen_noDowngradeRecorded() throws IOException {
        // Gate missing -> state is written without downgrades.
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
    @DisplayName("zero conflicts leave parallelismDowngrades unset")
    void zeroConflicts_noDowngradeRecorded() throws IOException {
        // Gate exit=0 -> orchestrator writes no downgrade entries.
        ExecutionState state = baseState()
                .withParallelismDowngrades(List.of());

        Path file = tmp.resolve("execution-state.json");
        new JacksonCheckpointPersistence().save(state, file);

        String json = Files.readString(file);
        assertThat(json).doesNotContain("parallelismDowngrades");

        ExecutionState restored =
                new JacksonCheckpointPersistence().load(file);
        assertThat(restored.parallelismDowngradesOptional())
                .isEmpty();
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

    @SuppressWarnings("unused")
    private static ObjectMapper configuredMapper() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        mapper.enable(SerializationFeature.INDENT_OUTPUT);
        mapper.disable(SerializationFeature
                .WRITE_DATES_AS_TIMESTAMPS);
        return mapper;
    }
}
