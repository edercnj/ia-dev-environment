package dev.iadev.domain.schemaversion;

import java.util.Objects;
import java.util.Optional;

/**
 * Result of {@link SchemaVersionResolver}: the resolved version plus a fallback reason
 * enumeration so callers can emit the authoritative warning log required by story-0038-0008
 * §5.2.
 *
 * @param version  the resolved {@link PlanningSchemaVersion}
 * @param reason   a {@link FallbackReason} when resolution fell back to V1, or empty when
 *                 the wire value was explicitly recognised
 */
public record SchemaVersionResolution(
        PlanningSchemaVersion version,
        Optional<FallbackReason> reason) {

    public SchemaVersionResolution {
        Objects.requireNonNull(version, "version");
        Objects.requireNonNull(reason, "reason");
    }

    public boolean isFallback() {
        return reason.isPresent();
    }

    public enum FallbackReason {

        /** {@code execution-state.json} is absent entirely. */
        NO_FILE,

        /** File exists but {@code planningSchemaVersion} field is missing. */
        MISSING_FIELD,

        /** Field is present but holds an unknown / malformed value. */
        INVALID_VALUE
    }
}
