package dev.iadev.telemetry.analyze;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.Test;

class CsvReportRendererTest {

    private final CsvReportRenderer renderer =
            new CsvReportRenderer();

    @Test
    void render_emptyReport_producesHeaderOnly() {
        AnalysisReport report = new AnalysisReport(
                Instant.parse("2026-04-16T12:00:00Z"),
                List.of("EPIC-0040"),
                0L, 0L,
                List.of(), List.of(), List.of(),
                List.of(), List.of());

        String csv = renderer.render(report);

        assertThat(csv).isEqualTo(
                "type,name,invocations,totalMs,avgMs,p50Ms,p95Ms,"
                        + "epicIds\n");
    }

    @Test
    void render_skillRow_eightColumns() {
        Stat stat = new Stat(
                "x-story-implement", 3, 300L, 100L, 80L,
                280L, List.of("EPIC-0040"));
        AnalysisReport report = new AnalysisReport(
                Instant.parse("2026-04-16T12:00:00Z"),
                List.of("EPIC-0040"),
                3L, 300L,
                List.of(stat), List.of(), List.of(),
                List.of(), List.of());

        String csv = renderer.render(report);

        String[] lines = csv.split("\n");
        assertThat(lines).hasSize(2);
        assertThat(lines[1])
                .isEqualTo("skill,x-story-implement,3,300,100,"
                        + "80,280,EPIC-0040");
        assertThat(lines[1].split(",")).hasSize(8);
    }

    @Test
    void render_multiEpicCell_isQuotedWithEmbeddedComma() {
        Stat stat = new Stat(
                "x-story-implement", 1, 100L, 100L, 100L,
                100L, List.of("EPIC-0040", "EPIC-0041"));
        AnalysisReport report = new AnalysisReport(
                Instant.parse("2026-04-16T12:00:00Z"),
                List.of("EPIC-0040", "EPIC-0041"),
                1L, 100L,
                List.of(stat), List.of(), List.of(),
                List.of(), List.of());

        String csv = renderer.render(report);

        assertThat(csv)
                .contains("\"EPIC-0040,EPIC-0041\"");
    }

    @Test
    void render_phaseAndToolRows_tagTypeColumnCorrectly() {
        Stat phase = new Stat(
                "x-story-implement/Phase-2", 1, 200L, 200L, 200L,
                200L, List.of("EPIC-0040"));
        Stat tool = new Stat(
                "Bash", 5, 500L, 100L, 80L, 150L,
                List.of("EPIC-0040"));
        AnalysisReport report = new AnalysisReport(
                Instant.parse("2026-04-16T12:00:00Z"),
                List.of("EPIC-0040"),
                6L, 700L,
                List.of(), List.of(phase), List.of(tool),
                List.of(), List.of());

        String csv = renderer.render(report);

        assertThat(csv)
                .contains(
                        "phase,x-story-implement/Phase-2,1,200,"
                                + "200,200,200,EPIC-0040")
                .contains("tool,Bash,5,500,100,80,150,EPIC-0040");
    }
}
