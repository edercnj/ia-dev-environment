package dev.iadev.telemetry.analyze;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

class MarkdownReportRendererTest {

    private final MarkdownReportRenderer renderer =
            new MarkdownReportRenderer();

    @Test
    void render_emptyReport_containsAllRequiredSections() {
        AnalysisReport report = emptyReport();

        String md = renderer.render(report);

        assertThat(md)
                .contains("# Telemetry Report")
                .contains("## Resumo geral")
                .contains("## Por skill")
                .contains("## Por fase")
                .contains("## Por tool")
                .contains("## Gantt")
                .contains("## Observacoes");
    }

    @Test
    void render_populatedReport_showsSkillTableRow() {
        Stat skill = new Stat(
                "x-story-implement", 3, 3000L, 1000L, 950L,
                1500L, List.of("EPIC-0040"));
        AnalysisReport report = new AnalysisReport(
                Instant.parse("2026-04-16T12:00:00Z"),
                List.of("EPIC-0040"),
                3L, 3000L,
                List.of(skill), List.of(), List.of(),
                List.of(), List.of());

        String md = renderer.render(report);

        assertThat(md)
                .contains("| `x-story-implement` | 3 | 3000 "
                        + "| 1000 | 950 | 1500 | EPIC-0040 |");
    }

    @Test
    void render_timeline_producesMermaidGanttBlock() {
        PhaseTimeline row = new PhaseTimeline(
                "x-story-implement", "Phase-1",
                Instant.parse("2026-04-16T12:00:00Z"),
                Instant.parse("2026-04-16T12:00:05Z"),
                5000L);
        AnalysisReport report = new AnalysisReport(
                Instant.parse("2026-04-16T12:10:00Z"),
                List.of("EPIC-0040"),
                1L, 5000L,
                List.of(), List.of(), List.of(),
                List.of(row), List.of());

        String md = renderer.render(report);

        assertThat(md)
                .contains("```mermaid")
                .contains("gantt")
                .contains("section x-story-implement")
                .contains("Phase-1 :");
    }

    @Test
    void render_timelineExceeds50Rows_truncatesAndNotes() {
        List<PhaseTimeline> timeline = new ArrayList<>();
        for (int i = 0; i < 60; i++) {
            timeline.add(new PhaseTimeline(
                    "skill", "Phase-" + i,
                    Instant.parse("2026-04-16T12:00:00Z")
                            .plusSeconds(i),
                    Instant.parse("2026-04-16T12:00:01Z")
                            .plusSeconds(i),
                    100L));
        }
        AnalysisReport report = new AnalysisReport(
                Instant.parse("2026-04-16T13:00:00Z"),
                List.of("EPIC-0040"),
                60L, 6000L,
                List.of(), List.of(), List.of(),
                timeline, List.of());

        String md = renderer.render(report);

        assertThat(md).contains("Gantt truncated");
    }

    @Test
    void render_summarySection_topFiveSkills() {
        List<Stat> skills = new ArrayList<>();
        for (int i = 0; i < 7; i++) {
            skills.add(new Stat(
                    "skill-" + i, 1, (long) (1000 - i * 100),
                    1L, 1L, 1L, List.of("EPIC-0040")));
        }
        AnalysisReport report = new AnalysisReport(
                Instant.parse("2026-04-16T12:00:00Z"),
                List.of("EPIC-0040"),
                7L, 4200L,
                skills, List.of(), List.of(),
                List.of(), List.of());

        String md = renderer.render(report);

        assertThat(md)
                .contains("Top-5 skills by total time")
                .contains("`skill-0`")
                .contains("`skill-4`");
    }

    private AnalysisReport emptyReport() {
        return new AnalysisReport(
                Instant.parse("2026-04-16T12:00:00Z"),
                List.of("EPIC-0040"),
                0L, 0L,
                List.of(), List.of(), List.of(),
                List.of(), List.of());
    }
}
