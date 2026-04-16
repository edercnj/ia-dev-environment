package dev.iadev.telemetry;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.networknt.schema.JsonSchema;
import com.networknt.schema.JsonSchemaFactory;
import com.networknt.schema.SpecVersion;
import com.networknt.schema.ValidationMessage;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class TelemetryEventTest {

    private static final UUID EVENT_ID =
            UUID.fromString(
                    "11111111-1111-4111-8111-111111111111");
    private static final Instant TS =
            Instant.parse("2026-04-16T12:34:56.789Z");
    private static final String SESSION =
            "claude-sess-abc123";

    @Test
    void constructor_nullTimestamp_throwsIllegalArgument() {
        assertThatThrownBy(() -> new TelemetryEvent(
                "1.0.0", EVENT_ID, null, SESSION,
                null, null, null, EventType.SESSION_START,
                null, null, null, null, null, null, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("timestamp is required");
    }

    @Test
    void constructor_nullSchemaVersion_throwsIllegalArgument() {
        assertThatThrownBy(() -> new TelemetryEvent(
                null, EVENT_ID, TS, SESSION,
                null, null, null, EventType.SESSION_START,
                null, null, null, null, null, null, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining(
                        "schemaVersion is required");
    }

    @Test
    void constructor_nullType_throwsIllegalArgument() {
        assertThatThrownBy(() -> new TelemetryEvent(
                "1.0.0", EVENT_ID, TS, SESSION,
                null, null, null, null,
                null, null, null, null, null, null, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("type is required");
    }

    @Test
    void constructor_invalidSchemaVersion_throws() {
        assertThatThrownBy(() -> new TelemetryEvent(
                "1.0", EVENT_ID, TS, SESSION,
                null, null, null, EventType.SESSION_START,
                null, null, null, null, null, null, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("SemVer");
    }

    @Test
    void constructor_sessionIdTooLong_throws() {
        String oversized = "a".repeat(129);
        assertThatThrownBy(() -> new TelemetryEvent(
                "1.0.0", EVENT_ID, TS, oversized,
                null, null, null, EventType.SESSION_START,
                null, null, null, null, null, null, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("sessionId");
    }

    @Test
    void constructor_blankSessionId_throws() {
        assertThatThrownBy(() -> new TelemetryEvent(
                "1.0.0", EVENT_ID, TS, "   ",
                null, null, null, EventType.SESSION_START,
                null, null, null, null, null, null, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("sessionId");
    }

    @Test
    void constructor_invalidEpicId_throws() {
        assertThatThrownBy(() -> new TelemetryEvent(
                "1.0.0", EVENT_ID, TS, SESSION,
                "EPIC-40", null, null,
                EventType.SESSION_START,
                null, null, null, null, null, null, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("epicId");
    }

    @Test
    void constructor_invalidStoryId_throws() {
        assertThatThrownBy(() -> new TelemetryEvent(
                "1.0.0", EVENT_ID, TS, SESSION,
                null, "STORY-0040-0001", null,
                EventType.SESSION_START,
                null, null, null, null, null, null, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("storyId");
    }

    @Test
    void constructor_invalidTaskId_throws() {
        assertThatThrownBy(() -> new TelemetryEvent(
                "1.0.0", EVENT_ID, TS, SESSION,
                null, null, "TASK-40-0001-001",
                EventType.SESSION_START,
                null, null, null, null, null, null, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("taskId");
    }

    @Test
    void constructor_invalidSkillCasing_throws() {
        assertThatThrownBy(() -> new TelemetryEvent(
                "1.0.0", EVENT_ID, TS, SESSION,
                null, null, null, EventType.SKILL_START,
                "XStoryImplement", null, null, null,
                null, null, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("kebab-case");
    }

    @Test
    void constructor_negativeDuration_throws() {
        assertThatThrownBy(() -> new TelemetryEvent(
                "1.0.0", EVENT_ID, TS, SESSION,
                null, null, null, EventType.SKILL_END,
                null, null, null, -1L,
                EventStatus.OK, null, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("durationMs");
    }

    @Test
    void constructor_oversizedFailureReason_throws() {
        String oversized = "x".repeat(257);
        assertThatThrownBy(() -> new TelemetryEvent(
                "1.0.0", EVENT_ID, TS, SESSION,
                null, null, null, EventType.TOOL_CALL,
                null, null, "Bash", 1L,
                EventStatus.FAILED, oversized, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("failureReason");
    }

    @Test
    void toJsonLine_minimalSessionStart_matchesFixture()
            throws Exception {
        TelemetryEvent event = new TelemetryEvent(
                "1.0.0", EVENT_ID, TS, SESSION,
                null, null, null, EventType.SESSION_START,
                null, null, null, null, null, null, null);

        String line = event.toJsonLine();

        assertThat(line).endsWith("\n");
        assertThat(line).doesNotContain("null");
        assertThat(line).contains(
                "\"schemaVersion\":\"1.0.0\"");
        assertThat(line).contains(
                "\"type\":\"session.start\"");
        validateAgainstSchema(line);
    }

    @Test
    void toJsonLine_fullSkillEnd_roundTripsEquivalent() {
        TelemetryEvent event = new TelemetryEvent(
                "1.0.0", EVENT_ID, TS, SESSION,
                "EPIC-0040", "story-0040-0001",
                "TASK-0040-0001-001",
                EventType.SKILL_END, "x-story-implement",
                "Phase-2-Implementation", null, 12345L,
                EventStatus.OK, null,
                Map.of("retryCount", 0));

        String line = event.toJsonLine();
        TelemetryEvent parsed =
                TelemetryEvent.fromJsonLine(line);

        assertThat(parsed).isEqualTo(event);
    }

    @Test
    void fromJsonLine_sessionStartFixture_parses()
            throws Exception {
        String line = Files.readString(Path.of(
                "src/test/resources/fixtures/telemetry/"
                        + "session-start-minimal.json"));

        TelemetryEvent event =
                TelemetryEvent.fromJsonLine(line);

        assertThat(event.schemaVersion()).isEqualTo("1.0.0");
        assertThat(event.eventId()).isEqualTo(EVENT_ID);
        assertThat(event.timestamp()).isEqualTo(TS);
        assertThat(event.sessionId()).isEqualTo(SESSION);
        assertThat(event.type())
                .isEqualTo(EventType.SESSION_START);
        assertThat(event.epicId()).isNull();
        assertThat(event.metadata()).isNull();
    }

    @Test
    void fromJsonLine_toolCallFailedFixture_parses()
            throws Exception {
        String line = Files.readString(Path.of(
                "src/test/resources/fixtures/telemetry/"
                        + "tool-call-failed.json"));

        TelemetryEvent event =
                TelemetryEvent.fromJsonLine(line);

        assertThat(event.type())
                .isEqualTo(EventType.TOOL_CALL);
        assertThat(event.tool()).isEqualTo("Bash");
        assertThat(event.status())
                .isEqualTo(EventStatus.FAILED);
        assertThat(event.durationMillis())
                .contains(678L);
        assertThat(event.failureReasonOptional())
                .contains("timeout");
        assertThat(event.metadata())
                .containsEntry("retryCount", 2);
    }

    @Test
    void fromJsonLine_nullLine_throws() {
        assertThatThrownBy(() -> TelemetryEvent.fromJsonLine(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("line is required");
    }

    @Test
    void fromJsonLine_blankLine_throws() {
        assertThatThrownBy(
                () -> TelemetryEvent.fromJsonLine("   \n"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("line is required");
    }

    @Test
    void fromJsonLine_malformed_throws() {
        assertThatThrownBy(
                () -> TelemetryEvent.fromJsonLine(
                        "{\"schemaVersion\":"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("invalid NDJSON line");
    }

    @Test
    void metadata_isDefensivelyCopied() {
        java.util.HashMap<String, Object> mutable =
                new java.util.HashMap<>();
        mutable.put("retryCount", 0);

        TelemetryEvent event = new TelemetryEvent(
                "1.0.0", EVENT_ID, TS, SESSION,
                null, null, null, EventType.SESSION_START,
                null, null, null, null, null, null, mutable);

        mutable.put("retryCount", 42);

        assertThat(event.metadata())
                .containsEntry("retryCount", 0);
        assertThatThrownBy(
                () -> event.metadata().put("x", "y"))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    private static void validateAgainstSchema(String json)
            throws Exception {
        String schemaJson = Files.readString(Path.of(
                "src/main/resources/shared/templates/"
                        + "_TEMPLATE-TELEMETRY-EVENT.json"));
        JsonSchemaFactory factory = JsonSchemaFactory.getInstance(
                SpecVersion.VersionFlag.V202012);
        JsonSchema schema = factory.getSchema(schemaJson);
        Set<ValidationMessage> messages = schema.validate(
                new com.fasterxml.jackson.databind.ObjectMapper()
                        .readTree(json.trim()));
        assertThat(messages).isEmpty();
    }
}
