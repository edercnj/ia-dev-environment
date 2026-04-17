package dev.iadev.telemetry.trend;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.util.Objects;

/**
 * Serializes a {@link TrendReport} to the JSON shape documented in story
 * §5.2. ISO-8601 timestamps, pretty-printed output, non-null fields only.
 */
public final class TrendJsonRenderer {

    private final ObjectMapper mapper;

    /** Creates the renderer with the shared telemetry-style mapper. */
    public TrendJsonRenderer() {
        ObjectMapper m = new ObjectMapper();
        m.registerModule(new JavaTimeModule());
        m.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        m.setSerializationInclusion(JsonInclude.Include.NON_NULL);
        m.enable(SerializationFeature.INDENT_OUTPUT);
        this.mapper = m;
    }

    /**
     * Renders the report as JSON.
     *
     * @param report the report to render
     * @return the JSON text
     */
    public String render(TrendReport report) {
        Objects.requireNonNull(report, "report is required");
        try {
            return mapper.writeValueAsString(report);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException(
                    "failed to serialize trend report", e);
        }
    }
}
