package dev.iadev.telemetry;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/**
 * Terminal outcome flag for {@code *.end} and {@code tool.result} events.
 *
 * <p>Matches the closed enum in {@code _TEMPLATE-TELEMETRY-EVENT.json}
 * ({@code properties.status.enum}). JSON wire values are lowercase tokens
 * (e.g. {@code "ok"}).</p>
 */
public enum EventStatus {

    OK("ok"),
    FAILED("failed"),
    SKIPPED("skipped");

    private final String wireValue;

    EventStatus(String wireValue) {
        this.wireValue = wireValue;
    }

    /**
     * Returns the canonical wire value for this status.
     *
     * @return the lowercase token (e.g. {@code "ok"})
     */
    @JsonValue
    public String wireValue() {
        return wireValue;
    }

    /**
     * Resolves an enum constant from its canonical wire value.
     *
     * @param wireValue the lowercase token
     * @return the matching {@code EventStatus}
     * @throws IllegalArgumentException if the value is unknown
     */
    @JsonCreator
    public static EventStatus fromWireValue(String wireValue) {
        if (wireValue == null) {
            throw new IllegalArgumentException(
                    "event status wire value is required");
        }
        for (EventStatus value : values()) {
            if (value.wireValue.equals(wireValue)) {
                return value;
            }
        }
        throw new IllegalArgumentException(
                "unknown event status: " + wireValue);
    }
}
