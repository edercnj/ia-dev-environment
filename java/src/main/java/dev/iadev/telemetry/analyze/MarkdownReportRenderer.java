package dev.iadev.telemetry.analyze;

import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Renders an {@link AnalysisReport} to the Markdown format defined in
 * {@code _TEMPLATE-TELEMETRY-REPORT.md}.
 *
 * <p>Output contains the seven canonical sections required by
 * story-0040-0010 §3.2: Header, Resumo geral, Por skill, Por fase, Por
 * tool, Gantt, Observacoes. Numeric values are integer-formatted (no
 * decimals — durations are already integer milliseconds).</p>
 *
 * <p>The Mermaid Gantt is capped at 50 rows (per story §3.2 item 6) so the
 * generated document stays readable in GitHub / VS Code preview. When the
 * cap is hit, an observation row is appended stating how many phases were
 * truncated.</p>
 */
public final class MarkdownReportRenderer {

    private static final int MAX_GANTT_ROWS = 50;

    /**
     * Renders the report as a Markdown string.
     *
     * @param report the analysis report to render
     * @return the Markdown document (ends with a trailing newline)
     */
    public String render(AnalysisReport report) {
        StringBuilder sb = new StringBuilder();
        appendHeader(sb, report);
        appendSummary(sb, report);
        appendSkillTable(sb, report.skills());
        appendPhaseTable(sb, report.phases());
        appendToolTable(sb, report.tools());
        appendGantt(sb, report.timeline());
        appendObservations(sb, report);
        return sb.toString();
    }

    private void appendHeader(StringBuilder sb, AnalysisReport report) {
        sb.append("# Telemetry Report\n\n");
        sb.append("- **Epics:** ")
                .append(String.join(", ", report.epics()))
                .append("\n");
        sb.append("- **Generated At:** ")
                .append(DateTimeFormatter.ISO_INSTANT
                        .format(report.generatedAt()))
                .append("\n");
        sb.append("- **Total Events:** ")
                .append(report.totalEvents())
                .append("\n\n");
    }

    private void appendSummary(StringBuilder sb, AnalysisReport report) {
        sb.append("## Resumo geral\n\n");
        sb.append("- **Total Duration (ms):** ")
                .append(report.totalDurationMs())
                .append("\n");
        sb.append("- **Skills analysed:** ")
                .append(report.skills().size())
                .append("\n");
        sb.append("- **Phases analysed:** ")
                .append(report.phases().size())
                .append("\n");
        sb.append("- **Tools analysed:** ")
                .append(report.tools().size())
                .append("\n\n");

        sb.append("### Top-5 skills by total time\n\n");
        appendTopN(sb, report.skills(), 5);
        sb.append("\n");

        sb.append("### Top-5 phases by total time\n\n");
        appendTopN(sb, report.phases(), 5);
        sb.append("\n");
    }

    private void appendTopN(
            StringBuilder sb, List<Stat> stats, int n) {
        if (stats.isEmpty()) {
            sb.append("_(no data)_\n");
            return;
        }
        int limit = Math.min(n, stats.size());
        for (int i = 0; i < limit; i++) {
            Stat s = stats.get(i);
            sb.append(i + 1).append(". `").append(s.name())
                    .append("` — ").append(s.totalMs())
                    .append(" ms (").append(s.invocations())
                    .append(" invocations)\n");
        }
    }

    private void appendSkillTable(
            StringBuilder sb, List<Stat> stats) {
        sb.append("## Por skill\n\n");
        appendStatTable(sb, stats, "Skill");
    }

    private void appendPhaseTable(
            StringBuilder sb, List<Stat> stats) {
        sb.append("## Por fase\n\n");
        appendStatTable(sb, stats, "Phase");
    }

    private void appendToolTable(
            StringBuilder sb, List<Stat> stats) {
        sb.append("## Por tool\n\n");
        appendStatTable(sb, stats, "Tool");
    }

    private void appendStatTable(
            StringBuilder sb, List<Stat> stats, String label) {
        if (stats.isEmpty()) {
            sb.append("_(no data)_\n\n");
            return;
        }
        sb.append("| ").append(label)
                .append(" | Invocations | Total (ms) | Avg (ms)"
                        + " | P50 (ms) | P95 (ms) | Epics |\n");
        sb.append("| --- | ---: | ---: | ---: | ---: | ---:"
                + " | --- |\n");
        for (Stat s : stats) {
            sb.append("| `").append(s.name()).append("` | ")
                    .append(s.invocations()).append(" | ")
                    .append(s.totalMs()).append(" | ")
                    .append(s.avgMs()).append(" | ")
                    .append(s.p50Ms()).append(" | ")
                    .append(s.p95Ms()).append(" | ")
                    .append(String.join(",", s.epicIds()))
                    .append(" |\n");
        }
        sb.append("\n");
    }

    private void appendGantt(
            StringBuilder sb, List<PhaseTimeline> timeline) {
        sb.append("## Gantt\n\n");
        if (timeline.isEmpty()) {
            sb.append("_(no phase timeline available)_\n\n");
            return;
        }
        sb.append("```mermaid\n");
        sb.append("gantt\n");
        sb.append("    title Phase timeline\n");
        sb.append("    dateFormat YYYY-MM-DDTHH:mm:ss.SSSZ\n");
        sb.append("    axisFormat %H:%M:%S\n");
        int limit = Math.min(MAX_GANTT_ROWS, timeline.size());
        String currentSection = null;
        for (int i = 0; i < limit; i++) {
            PhaseTimeline row = timeline.get(i);
            if (!row.skill().equals(currentSection)) {
                currentSection = row.skill();
                sb.append("    section ").append(currentSection)
                        .append("\n");
            }
            // Mermaid requires a positive duration; synthesize 1ms floor.
            long durationMs = Math.max(row.durationMs(), 1L);
            sb.append("    ").append(row.phase())
                    .append(" : ")
                    .append(DateTimeFormatter.ISO_INSTANT
                            .format(row.startInstant()))
                    .append(", ").append(durationMs).append("ms\n");
        }
        sb.append("```\n\n");
    }

    private void appendObservations(
            StringBuilder sb, AnalysisReport report) {
        sb.append("## Observacoes\n\n");
        int truncated = report.timeline().size() - MAX_GANTT_ROWS;
        if (truncated > 0) {
            sb.append("- Gantt truncated: ")
                    .append(truncated)
                    .append(" additional phase rows were omitted to keep"
                            + " the chart readable.\n");
        }
        if (report.observations().isEmpty() && truncated <= 0) {
            sb.append("_(no observations)_\n");
        } else {
            for (String obs : report.observations()) {
                sb.append("- ").append(obs).append("\n");
            }
        }
        sb.append("\n");
    }
}
