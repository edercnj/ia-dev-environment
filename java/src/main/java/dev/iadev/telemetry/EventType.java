package dev.iadev.telemetry;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/**
 * Canonical telemetry event kinds, per EPIC-0040 story-0040-0001 schema.
 *
 * <p>Each constant maps 1:1 to the closed enum in
 * {@code _TEMPLATE-TELEMETRY-EVENT.json} ({@code properties.type.enum}). JSON
 * wire values are lowercase dotted (e.g. {@code "skill.start"}) and are the
 * canonical form persisted on disk.</p>
 *
 * <p>Adding or removing a value MUST be accompanied by a MINOR bump of the
 * telemetry event schema version (see RULE-001 of EPIC-0040).</p>
 */
public enum EventType {

    SKILL_START("skill.start"),
    SKILL_END("skill.end"),
    PHASE_START("phase.start"),
    PHASE_END("phase.end"),
    TOOL_CALL("tool.call"),
    TOOL_RESULT("tool.result"),
    SESSION_START("session.start"),
    SESSION_END("session.end"),
    SUBAGENT_START("subagent.start"),
    SUBAGENT_END("subagent.end"),
    ERROR("error");

    private final String wireValue;

    EventType(String wireValue) {
        this.wireValue = wireValue;
    }

    /**
     * Returns the canonical wire value for this event kind.
     *
     * @return the lowercase dotted identifier (e.g. {@code "skill.start"})
     */
    @JsonValue
    public String wireValue() {
        return wireValue;
    }

    /**
     * Resolves an enum constant from its canonical wire value.
     *
     * @param wireValue the lowercase dotted identifier
     * @return the matching {@code EventType}
     * @throws IllegalArgumentException if the value is null or unknown
     */
    @JsonCreator
    public static EventType fromWireValue(String wireValue) {
        if (wireValue == null) {
            throw new IllegalArgumentException(
                    "event type wire value is required");
        }
        for (EventType value : values()) {
            if (value.wireValue.equals(wireValue)) {
                return value;
            }
        }
        throw new IllegalArgumentException(
                "unknown event type: " + wireValue);
    }
}
