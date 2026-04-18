package dev.iadev.telemetry.analyze;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.Test;

class ReportRendererTest {

    private final ReportRenderer renderer = new ReportRenderer();

    @Test
    void render_markdownFormat_producesMdHeader() {
        String md = renderer.render(emptyReport(), "md");

        assertThat(md)
                .startsWith("# Telemetry Report")
                .contains("## Resumo geral");
    }

    @Test
    void render_jsonFormat_producesValidJson() throws Exception {
        String json = renderer.render(emptyReport(), "json");

        JsonNode node = new ObjectMapper().readTree(json);
        assertThat(node.isObject()).isTrue();
        assertThat(node.get("epics").isArray()).isTrue();
    }

    @Test
    void render_csvFormat_producesCsvHeader() {
        String csv = renderer.render(emptyReport(), "csv");

        assertThat(csv)
                .startsWith("type,name,invocations,totalMs,avgMs,"
                        + "p50Ms,p95Ms,epicIds");
    }

    @Test
    void render_unknownFormat_throwsIllegalArgumentException() {
        assertThatThrownBy(() -> renderer.render(emptyReport(), "xml"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("xml");
    }

    @Test
    void render_nullFormat_defaultsToMarkdown() {
        String out = renderer.render(emptyReport(), null);

        assertThat(out).startsWith("# Telemetry Report");
    }

    @Test
    void render_blankFormat_defaultsToMarkdown() {
        String out = renderer.render(emptyReport(), "   ");

        assertThat(out).startsWith("# Telemetry Report");
    }

    @Test
    void render_caseInsensitive_acceptsUppercaseTokens() {
        String csv = renderer.render(emptyReport(), "CSV");
        String json = renderer.render(emptyReport(), "JSON");

        assertThat(csv).startsWith("type,name,");
        assertThat(json).startsWith("{");
    }

    @Test
    void render_singleEpicMarkdownReport_showsSkillRow() {
        Stat skill = new Stat(
                "x-story-implement", 3, 3000L, 1000L, 950L,
                1500L, List.of("EPIC-0040"));
        AnalysisReport report = new AnalysisReport(
                Instant.parse("2026-04-17T12:00:00Z"),
                List.of("EPIC-0040"),
                3L, 3000L,
                List.of(skill), List.of(), List.of(),
                List.of(), List.of());

        String md = renderer.render(report, "md");

        assertThat(md).contains("`x-story-implement`");
    }

    @Test
    void renderMarkdown_directMethod_matchesRenderDispatch() {
        AnalysisReport report = emptyReport();

        assertThat(renderer.renderMarkdown(report))
                .isEqualTo(renderer.render(report, "md"));
    }

    @Test
    void renderJson_directMethod_matchesRenderDispatch() {
        AnalysisReport report = emptyReport();

        assertThat(renderer.renderJson(report))
                .isEqualTo(renderer.render(report, "json"));
    }

    @Test
    void renderCsv_directMethod_matchesRenderDispatch() {
        AnalysisReport report = emptyReport();

        assertThat(renderer.renderCsv(report))
                .isEqualTo(renderer.render(report, "csv"));
    }

    private AnalysisReport emptyReport() {
        return new AnalysisReport(
                Instant.parse("2026-04-17T12:00:00Z"),
                List.of(),
                0L, 0L,
                List.of(), List.of(), List.of(),
                List.of(), List.of());
    }
}
