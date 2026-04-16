package dev.iadev.telemetry;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.core.JsonProcessingException;
import java.time.Instant;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.regex.Pattern;

/**
 * Immutable value carrier for a single telemetry event.
 *
 * <p>Faithfully mirrors the canonical schema published by story-0040-0001
 * ({@code _TEMPLATE-TELEMETRY-EVENT.json}). All instances are constructed via
 * the canonical constructor which fail-fast validates required fields and
 * enforces the schema's format constraints (SemVer {@link #schemaVersion},
 * bounded {@link #sessionId}, ID pattern for {@link #epicId},
 * {@link #storyId}, {@link #taskId}, kebab-case {@link #skill},
 * non-negative {@link #durationMs}, bounded {@link #failureReason}).</p>
 *
 * <p>Produced by {@link #toJsonLine()} in NDJSON form (one JSON object
 * terminated by {@code \n}) and consumed by {@link #fromJsonLine(String)}.
 * {@link Instant} is serialized as ISO-8601 UTC (e.g.
 * {@code 2026-04-16T12:34:56.789Z}). {@code null} optional fields are elided
 * from the JSON output — this matches fixture expectations under
 * {@code src/test/resources/fixtures/telemetry/}.</p>
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({
        "schemaVersion",
        "eventId",
        "timestamp",
        "sessionId",
        "epicId",
        "storyId",
        "taskId",
        "type",
        "skill",
        "phase",
        "tool",
        "durationMs",
        "status",
        "failureReason",
        "metadata"
})
public record TelemetryEvent(
        String schemaVersion,
        UUID eventId,
        Instant timestamp,
        String sessionId,
        String epicId,
        String storyId,
        String taskId,
        EventType type,
        String skill,
        String phase,
        String tool,
        Long durationMs,
        EventStatus status,
        String failureReason,
        Map<String, Object> metadata
) {

    /** SemVer regex (no pre-release suffix, matching schema pattern). */
    private static final Pattern SEMVER =
            Pattern.compile("^\\d+\\.\\d+\\.\\d+$");

    private static final Pattern EPIC_ID =
            Pattern.compile("^(EPIC-\\d{4}|unknown)$");

    private static final Pattern STORY_ID =
            Pattern.compile("^story-\\d{4}-\\d{4}$");

    private static final Pattern TASK_ID =
            Pattern.compile("^TASK-\\d{4}-\\d{4}-\\d{3}$");

    private static final Pattern SKILL_KEBAB =
            Pattern.compile("^[a-z0-9]+(?:-[a-z0-9]+)*$");

    private static final int SESSION_ID_MAX = 128;
    private static final int PHASE_MAX = 64;
    private static final int TOOL_MAX = 64;
    private static final int SKILL_MAX = 64;
    private static final int FAILURE_REASON_MAX = 256;

    /**
     * Canonical constructor applying fail-fast validation and defensive
     * copying of {@code metadata}.
     *
     * @throws IllegalArgumentException when required fields are null or
     *                                  when any format constraint is violated
     */
    public TelemetryEvent {
        requireNonNull(schemaVersion, "schemaVersion is required");
        requireNonNull(eventId, "eventId is required");
        requireNonNull(timestamp, "timestamp is required");
        requireNonNull(sessionId, "sessionId is required");
        requireNonNull(type, "type is required");

        requireMatches(schemaVersion, SEMVER,
                "schemaVersion must be SemVer (x.y.z): "
                        + schemaVersion);
        requireNonBlankBounded(sessionId, SESSION_ID_MAX,
                "sessionId must be 1.." + SESSION_ID_MAX
                        + " chars: " + sessionId);

        if (epicId != null) {
            requireMatches(epicId, EPIC_ID,
                    "epicId must match EPIC-NNNN or 'unknown': "
                            + epicId);
        }
        if (storyId != null) {
            requireMatches(storyId, STORY_ID,
                    "storyId must match story-XXXX-YYYY: "
                            + storyId);
        }
        if (taskId != null) {
            requireMatches(taskId, TASK_ID,
                    "taskId must match TASK-XXXX-YYYY-NNN: "
                            + taskId);
        }
        if (skill != null) {
            requireBounded(skill, SKILL_MAX,
                    "skill exceeds " + SKILL_MAX + " chars");
            requireMatches(skill, SKILL_KEBAB,
                    "skill must be kebab-case: " + skill);
        }
        if (phase != null) {
            requireBounded(phase, PHASE_MAX,
                    "phase exceeds " + PHASE_MAX + " chars");
        }
        if (tool != null) {
            requireBounded(tool, TOOL_MAX,
                    "tool exceeds " + TOOL_MAX + " chars");
        }
        if (durationMs != null && durationMs < 0) {
            throw new IllegalArgumentException(
                    "durationMs must be >= 0: " + durationMs);
        }
        if (failureReason != null) {
            requireBounded(failureReason, FAILURE_REASON_MAX,
                    "failureReason exceeds "
                            + FAILURE_REASON_MAX + " chars");
        }
        // Use LinkedHashMap + unmodifiableMap instead of Map.copyOf because
        // the schema allows null JSON values inside metadata, and Map.copyOf
        // rejects null values with NullPointerException.
        metadata = metadata == null
                ? null
                : Collections.unmodifiableMap(
                        new LinkedHashMap<>(metadata));
    }

    /**
     * Serializes this event as a single NDJSON line terminated by
     * {@code "\n"}. Null optional fields are omitted.
     *
     * @return the NDJSON line (guaranteed to end with a single newline)
     */
    public String toJsonLine() {
        try {
            String body = TelemetryJson.mapper()
                    .writeValueAsString(this);
            return body + "\n";
        } catch (JsonProcessingException e) {
            throw new IllegalStateException(
                    "failed to serialize TelemetryEvent", e);
        }
    }

    /**
     * Deserializes a single NDJSON line (with or without trailing newline)
     * into a {@link TelemetryEvent}.
     *
     * @param line the JSON payload (single line, no embedded newline
     *             except optional trailer)
     * @return the parsed event
     * @throws IllegalArgumentException when the payload is null, blank,
     *                                  or not valid JSON matching the schema
     */
    public static TelemetryEvent fromJsonLine(String line) {
        if (line == null) {
            throw new IllegalArgumentException(
                    "line is required");
        }
        String trimmed = line.strip();
        if (trimmed.isEmpty()) {
            throw new IllegalArgumentException(
                    "line is required");
        }
        try {
            return TelemetryJson.mapper()
                    .readValue(trimmed, TelemetryEvent.class);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException(
                    "invalid NDJSON line: "
                            + e.getOriginalMessage(), e);
        }
    }

    /** @return the optional duration, if present. */
    public Optional<Long> durationMillis() {
        return Optional.ofNullable(durationMs);
    }

    /** @return the optional failure reason, if present. */
    public Optional<String> failureReasonOptional() {
        return Optional.ofNullable(failureReason);
    }

    private static void requireNonNull(
            Object value, String message) {
        if (value == null) {
            throw new IllegalArgumentException(message);
        }
    }

    private static void requireMatches(
            String value, Pattern pattern, String message) {
        if (!pattern.matcher(value).matches()) {
            throw new IllegalArgumentException(message);
        }
    }

    private static void requireNonBlankBounded(
            String value, int max, String message) {
        if (value.isBlank() || value.length() > max) {
            throw new IllegalArgumentException(message);
        }
    }

    private static void requireBounded(
            String value, int max, String message) {
        if (value.length() > max) {
            throw new IllegalArgumentException(message);
        }
    }
}
