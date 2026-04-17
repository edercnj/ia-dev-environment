package dev.iadev.telemetry.analyze;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.Test;

class JsonReportRendererTest {

    private final JsonReportRenderer renderer =
            new JsonReportRenderer();

    @Test
    void render_emptyReport_producesValidJsonWithSchemaFields()
            throws Exception {
        AnalysisReport report = new AnalysisReport(
                Instant.parse("2026-04-16T12:00:00Z"),
                List.of("EPIC-0040"),
                0L, 0L,
                List.of(), List.of(), List.of(),
                List.of(), List.of());

        String json = renderer.render(report);
        JsonNode node = new ObjectMapper().readTree(json);

        assertThat(node.get("generatedAt").asText())
                .isEqualTo("2026-04-16T12:00:00Z");
        assertThat(node.get("epics").isArray()).isTrue();
        assertThat(node.get("totals").get("events").asLong())
                .isZero();
        assertThat(node.get("totals").get("durationMs").asLong())
                .isZero();
        assertThat(node.get("skills").isArray()).isTrue();
        assertThat(node.get("phases").isArray()).isTrue();
        assertThat(node.get("tools").isArray()).isTrue();
    }

    @Test
    void render_populatedReport_serializesStatFields()
            throws Exception {
        Stat stat = new Stat(
                "x-story-implement", 3, 300L, 100L, 80L,
                280L, List.of("EPIC-0040"));
        AnalysisReport report = new AnalysisReport(
                Instant.parse("2026-04-16T12:00:00Z"),
                List.of("EPIC-0040"),
                3L, 300L,
                List.of(stat), List.of(), List.of(),
                List.of(), List.of());

        String json = renderer.render(report);
        JsonNode root = new ObjectMapper().readTree(json);
        JsonNode skill = root.get("skills").get(0);

        assertThat(skill.get("name").asText())
                .isEqualTo("x-story-implement");
        assertThat(skill.get("invocations").asInt()).isEqualTo(3);
        assertThat(skill.get("totalMs").asLong()).isEqualTo(300L);
        assertThat(skill.get("avgMs").asLong()).isEqualTo(100L);
        assertThat(skill.get("p50Ms").asLong()).isEqualTo(80L);
        assertThat(skill.get("p95Ms").asLong()).isEqualTo(280L);
        assertThat(skill.get("epicIds").isArray()).isTrue();
        assertThat(skill.get("epicIds").get(0).asText())
                .isEqualTo("EPIC-0040");
    }
}
