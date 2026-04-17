package dev.iadev.telemetry.trend;

import java.util.Objects;

/**
 * Renders a {@link TrendReport} as the canonical Markdown layout described
 * in story §3.3.
 *
 * <p>Output sections:</p>
 * <ol>
 *   <li>Header with scope (epics analyzed, threshold, baseline).</li>
 *   <li>Top-10 regressions table (skill, baseline P95, current P95, delta%).</li>
 *   <li>Top-10 slowest skills table (skill, avg P95, invocations).</li>
 *   <li>Observations — an automatic interpretive note ("N skills regressed ...").</li>
 * </ol>
 */
public final class TrendMarkdownRenderer {

    /**
     * Renders the report.
     *
     * @param report the report to render
     * @return the Markdown body
     */
    public String render(TrendReport report) {
        Objects.requireNonNull(report, "report is required");
        StringBuilder out = new StringBuilder();
        renderHeader(out, report);
        renderRegressions(out, report);
        renderSlowest(out, report);
        renderObservations(out, report);
        return out.toString();
    }

    private static void renderHeader(
            StringBuilder out, TrendReport r) {
        out.append("# Telemetry Trend Report\n\n");
        out.append("**Generated at:** ")
                .append(r.generatedAt()).append('\n');
        out.append("**Epics analyzed:** ")
                .append(String.join(", ", r.epicsAnalyzed()))
                .append('\n');
        out.append("**Threshold:** ")
                .append(r.thresholdPct()).append("%\n");
        out.append("**Baseline:** ")
                .append(r.baseline().toLowerCase()).append("\n\n");
    }

    private static void renderRegressions(
            StringBuilder out, TrendReport r) {
        out.append("## Top-10 regressions\n\n");
        if (r.regressions().isEmpty()) {
            out.append("_Nenhuma regressão detectada._\n\n");
            return;
        }
        out.append("| Skill | Baseline P95 (ms) | Current P95 (ms) | "
                + "Delta % | Epics analyzed |\n");
        out.append("| :--- | ---: | ---: | ---: | :--- |\n");
        for (Regression reg : r.regressions()) {
            out.append("| ").append(reg.skill())
                    .append(" | ").append(reg.baselineP95Ms())
                    .append(" | ").append(reg.currentP95Ms())
                    .append(" | ").append(reg.deltaPct())
                    .append(" | ").append(String.join(", ",
                            reg.epicsAnalyzed()))
                    .append(" |\n");
        }
        out.append('\n');
    }

    private static void renderSlowest(
            StringBuilder out, TrendReport r) {
        out.append("## Top-10 slowest skills\n\n");
        if (r.slowest().isEmpty()) {
            out.append("_No skill data in analyzed window._\n\n");
            return;
        }
        out.append("| Skill | Avg P95 (ms) | Invocations |\n");
        out.append("| :--- | ---: | ---: |\n");
        for (SlowSkill s : r.slowest()) {
            out.append("| ").append(s.skill())
                    .append(" | ").append(s.avgP95Ms())
                    .append(" | ").append(s.invocations())
                    .append(" |\n");
        }
        out.append('\n');
    }

    private static void renderObservations(
            StringBuilder out, TrendReport r) {
        out.append("## Observations\n\n");
        int n = r.regressions().size();
        if (n == 0) {
            out.append("- Nenhuma regressão detectada acima de ")
                    .append(r.thresholdPct()).append("%.\n");
        } else {
            String lastEpic = r.epicsAnalyzed().isEmpty()
                    ? "(unknown)"
                    : r.epicsAnalyzed().get(
                            r.epicsAnalyzed().size() - 1);
            out.append("- ").append(n)
                    .append(" skill(s) regressed >= ")
                    .append(r.thresholdPct())
                    .append("% in ").append(lastEpic)
                    .append(" — investigar.\n");
        }
        if (!r.slowest().isEmpty()) {
            SlowSkill top = r.slowest().get(0);
            out.append("- Skill mais lenta (avg P95): ")
                    .append(top.skill())
                    .append(" (").append(top.avgP95Ms())
                    .append("ms).\n");
        }
    }
}
