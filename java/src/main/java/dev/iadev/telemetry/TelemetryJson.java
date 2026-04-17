package dev.iadev.telemetry;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

/**
 * Package-private Jackson {@link ObjectMapper} configured for telemetry
 * (de)serialization.
 *
 * <p>Single shared instance — {@code ObjectMapper} is thread-safe once
 * configured. Writers and readers in {@code dev.iadev.telemetry} MUST use this
 * mapper to guarantee consistent wire format:</p>
 *
 * <ul>
 *   <li>ISO-8601 UTC for {@code Instant} (via
 *       {@code jackson-datatype-jsr310})</li>
 *   <li>{@code null} and absent fields are elided on output</li>
 *   <li>Unknown fields are ignored on input (forward compat per RULE-001 —
 *       readers support N+1 MINOR schema bumps without breaking)</li>
 * </ul>
 */
final class TelemetryJson {

    private static final ObjectMapper MAPPER = buildMapper();

    private TelemetryJson() {
    }

    /**
     * Returns the shared, thread-safe telemetry {@link ObjectMapper}.
     *
     * @return the configured mapper
     */
    static ObjectMapper mapper() {
        return MAPPER;
    }

    private static ObjectMapper buildMapper() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        mapper.disable(
                SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        mapper.disable(
                DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
        mapper.setSerializationInclusion(
                JsonInclude.Include.NON_NULL);
        return mapper;
    }
}
