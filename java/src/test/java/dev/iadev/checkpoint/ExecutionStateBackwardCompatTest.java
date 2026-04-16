package dev.iadev.checkpoint;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Backward-compatibility guard for the EPIC-0040 addition of
 * {@code telemetryPath} to {@link ExecutionState}.
 *
 * <p>Every scenario here protects a legacy invariant: existing
 * {@code execution-state.json} files on disk (v1 and v2) MUST continue
 * to load without error, and the serialized JSON of a state without a
 * telemetry path MUST not contain the new field.</p>
 */
class ExecutionStateBackwardCompatTest {

    @TempDir
    Path tmp;

    private static final Instant STARTED_AT =
            Instant.parse("2026-04-16T12:34:56Z");

    @Test
    void loadsLegacyV1_withoutTelemetryPathField()
            throws Exception {
        String legacyJson = """
                {
                  "version": "1.0",
                  "epicId": "EPIC-0040",
                  "branch": "feat/epic-0040",
                  "startedAt": "2026-04-16T12:34:56Z",
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
        Path file = tmp.resolve("execution-state.json");
        Files.writeString(file, legacyJson);

        ExecutionState state =
                new JacksonCheckpointPersistence().load(file);

        assertThat(state.version())
                .isEqualTo(ExecutionState.VERSION_1_0);
        assertThat(state.epicId()).isEqualTo("EPIC-0040");
        assertThat(state.telemetryPath()).isNull();
        assertThat(state.telemetryPathOptional()).isEmpty();
    }

    @Test
    void loadsV2_withTelemetryPathField()
            throws Exception {
        String v2Json = """
                {
                  "version": "2.0",
                  "epicId": "EPIC-0040",
                  "branch": "feat/epic-0040",
                  "startedAt": "2026-04-16T12:34:56Z",
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
                  },
                  "telemetryPath": "telemetry/events.ndjson"
                }
                """;
        Path file = tmp.resolve("execution-state.json");
        Files.writeString(file, v2Json);

        ExecutionState state =
                new JacksonCheckpointPersistence().load(file);

        assertThat(state.telemetryPath())
                .isEqualTo("telemetry/events.ndjson");
        assertThat(state.telemetryPathOptional())
                .contains("telemetry/events.ndjson");
    }

    @Test
    void serializing_withoutTelemetryPath_omitsField()
            throws Exception {
        ExecutionState state = new ExecutionState(
                "EPIC-0040", "feat/epic-0040", STARTED_AT,
                0, ExecutionMode.FULL,
                Map.of(), Map.of(),
                emptyMetrics());

        String json = configuredMapper()
                .writeValueAsString(state);

        assertThat(json).doesNotContain("telemetryPath");
    }

    @Test
    void serializing_withTelemetryPath_includesField()
            throws Exception {
        ExecutionState state = new ExecutionState(
                ExecutionState.VERSION_2_0,
                "EPIC-0040", "feat/epic-0040", STARTED_AT,
                0, ExecutionMode.FULL,
                Map.of(), Map.of(),
                emptyMetrics(),
                "telemetry/events.ndjson");

        String json = configuredMapper()
                .writeValueAsString(state);

        assertThat(json).contains(
                "\"telemetryPath\" : \"telemetry/events.ndjson\"");
    }

    @Test
    void withTelemetryPath_createsCopy() {
        ExecutionState original = new ExecutionState(
                "EPIC-0040", "feat/epic-0040", STARTED_AT,
                0, ExecutionMode.FULL,
                Map.of(), Map.of(),
                emptyMetrics());

        ExecutionState updated = original
                .withTelemetryPath("telemetry/events.ndjson");

        assertThat(original.telemetryPathOptional())
                .isEmpty();
        assertThat(updated.telemetryPathOptional())
                .contains("telemetry/events.ndjson");
        // Other fields preserved.
        assertThat(updated.epicId()).isEqualTo("EPIC-0040");
        assertThat(updated.branch())
                .isEqualTo("feat/epic-0040");
    }

    @Test
    void withTelemetryPath_nullClearsValue() {
        ExecutionState original = new ExecutionState(
                ExecutionState.VERSION_2_0,
                "EPIC-0040", "feat/epic-0040", STARTED_AT,
                0, ExecutionMode.FULL,
                Map.of(), Map.of(),
                emptyMetrics(),
                "telemetry/events.ndjson");

        ExecutionState cleared = original
                .withTelemetryPath(null);

        assertThat(cleared.telemetryPath()).isNull();
        assertThat(cleared.telemetryPathOptional()).isEmpty();
    }

    @Test
    void noArgVersionConstructor_defaultsVersionTo1_0() {
        ExecutionState state = new ExecutionState(
                "EPIC-0040", "feat/epic-0040", STARTED_AT,
                0, ExecutionMode.FULL,
                Map.of(), Map.of(),
                emptyMetrics());

        assertThat(state.version())
                .isEqualTo(ExecutionState.VERSION_1_0);
        assertThat(state.telemetryPath()).isNull();
    }

    @Test
    void telemetryPathOptional_neverNull() {
        ExecutionState state = new ExecutionState(
                "EPIC-0040", "feat/epic-0040", STARTED_AT,
                0, ExecutionMode.FULL,
                Map.of(), Map.of(),
                emptyMetrics());

        Optional<String> opt = state.telemetryPathOptional();

        assertThat(opt).isNotNull();
        assertThat(opt).isEmpty();
    }

    private static ExecutionMetrics emptyMetrics() {
        return ExecutionMetrics.initial(0);
    }

    private static ObjectMapper configuredMapper() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        mapper.enable(SerializationFeature.INDENT_OUTPUT);
        mapper.disable(SerializationFeature
                .WRITE_DATES_AS_TIMESTAMPS);
        return mapper;
    }
}
