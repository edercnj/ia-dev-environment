package dev.iadev.telemetry.analyze;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Renders an {@link AnalysisReport} as a JSON document matching the schema
 * declared in story-0040-0010 §5.1.
 *
 * <p>Fields are serialized in the order the schema declares so the output
 * diffs cleanly against fixtures. Empty lists are emitted as {@code []}
 * rather than being elided — downstream consumers (CSV post-processors,
 * dashboards) may rely on the stable field presence.</p>
 *
 * <p>Not to be confused with {@code TelemetryJson} (used by
 * {@code TelemetryEvent} for NDJSON (de)serialization) — this renderer has
 * its own mapper configured with {@link SerializationFeature#INDENT_OUTPUT}
 * so the exported JSON is human-readable on disk.</p>
 */
public final class JsonReportRenderer {

    private static final ObjectMapper MAPPER = buildMapper();

    /**
     * Renders the report as a pretty-printed JSON string.
     *
     * @param report the analysis report
     * @return the JSON document
     */
    public String render(AnalysisReport report) {
        Map<String, Object> root = new LinkedHashMap<>();
        root.put("generatedAt", report.generatedAt().toString());
        root.put("epics", report.epics());
        Map<String, Object> totals = new LinkedHashMap<>();
        totals.put("events", report.totalEvents());
        totals.put("durationMs", report.totalDurationMs());
        root.put("totals", totals);
        root.put("skills", toStatNodes(report.skills()));
        root.put("phases", toStatNodes(report.phases()));
        root.put("tools", toStatNodes(report.tools()));
        try {
            return MAPPER.writeValueAsString(root);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException(
                    "failed to serialize telemetry report", e);
        }
    }

    private static List<Map<String, Object>> toStatNodes(
            List<Stat> stats) {
        List<Map<String, Object>> out = new ArrayList<>(stats.size());
        for (Stat s : stats) {
            Map<String, Object> node = new LinkedHashMap<>();
            node.put("name", s.name());
            node.put("invocations", s.invocations());
            node.put("totalMs", s.totalMs());
            node.put("avgMs", s.avgMs());
            node.put("p50Ms", s.p50Ms());
            node.put("p95Ms", s.p95Ms());
            node.put("epicIds", s.epicIds());
            out.add(node);
        }
        return out;
    }

    private static ObjectMapper buildMapper() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        mapper.enable(SerializationFeature.INDENT_OUTPUT);
        mapper.disable(
                SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        return mapper;
    }
}
