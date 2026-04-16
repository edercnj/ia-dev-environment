package dev.iadev.domain.model;

/**
 * Release flow classifier consumed by the telemetry
 * schema (story-0039-0012 §5.1) and by story-0039-0014
 * for hotfix vs. release analytics.
 *
 * <p>Wire representation is the lowercase enum name
 * ({@code release}, {@code hotfix}) — the adapter layer
 * is responsible for producing this representation.
 */
public enum ReleaseType {
    RELEASE,
    HOTFIX;

    /**
     * Returns the wire-level string representation
     * (lowercase enum name) required by §5.1.
     *
     * @return {@code "release"} or {@code "hotfix"}
     */
    public String wireValue() {
        return name().toLowerCase();
    }

    /**
     * Parses a wire-level string back into an enum value.
     *
     * @param wire lowercase name; {@code null} falls back
     *             to {@link #RELEASE} per §5.1 default
     * @return corresponding enum value
     * @throws IllegalArgumentException if {@code wire}
     *         is non-null and does not match any value
     */
    public static ReleaseType fromWire(String wire) {
        if (wire == null) {
            return RELEASE;
        }
        for (ReleaseType value : values()) {
            if (value.wireValue().equals(wire)) {
                return value;
            }
        }
        throw new IllegalArgumentException(
                "Unknown releaseType wire value: " + wire);
    }
}
