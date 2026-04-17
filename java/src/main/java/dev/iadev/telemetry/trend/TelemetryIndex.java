package dev.iadev.telemetry.trend;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Serializable cache entry written to {@code .claude/telemetry/index.json} by
 * {@link TelemetryIndexBuilder}.
 *
 * <p>The index collapses each epic's NDJSON log into a compact per-skill P95
 * series so the {@code /x-telemetry-trend} skill can answer questions in
 * O(epics * skills) without re-reading the raw events. Cache invalidation is
 * driven by the {@link #epicMtimesEpochMs} map: when any tracked epic's
 * {@code events.ndjson} mtime is newer than the recorded value, the index is
 * rebuilt.</p>
 *
 * @param generatedAt         when the index was built (ISO-8601 UTC)
 * @param schemaVersion       index schema version (for forward compat)
 * @param epicMtimesEpochMs   per-epic NDJSON mtime in epoch ms, used for
 *                            invalidation (key = epic ID)
 * @param series              flat list of per-skill per-epic P95 rows
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({
        "schemaVersion",
        "generatedAt",
        "epicMtimesEpochMs",
        "series"
})
public record TelemetryIndex(
        String schemaVersion,
        Instant generatedAt,
        Map<String, Long> epicMtimesEpochMs,
        List<EpicSkillP95> series) {

    /** Current index schema version. */
    public static final String CURRENT_SCHEMA_VERSION = "1.0.0";

    /**
     * Canonical constructor. Defensively copies {@code epicMtimesEpochMs} and
     * {@code series}.
     *
     * @throws IllegalArgumentException when {@code schemaVersion} is blank or
     *                                  {@code generatedAt} is null
     */
    public TelemetryIndex {
        Objects.requireNonNull(schemaVersion,
                "schemaVersion is required");
        if (schemaVersion.isBlank()) {
            throw new IllegalArgumentException(
                    "schemaVersion must not be blank");
        }
        Objects.requireNonNull(generatedAt,
                "generatedAt is required");
        epicMtimesEpochMs = epicMtimesEpochMs == null
                ? Map.of()
                : Map.copyOf(epicMtimesEpochMs);
        series = series == null
                ? List.of()
                : List.copyOf(series);
    }
}
