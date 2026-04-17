package dev.iadev.telemetry.analyze;

import java.util.List;

/**
 * Renders an {@link AnalysisReport} as a CSV document matching the schema
 * declared in story-0040-0010 §5.2.
 *
 * <p>Column layout is exactly: {@code type,name,invocations,totalMs,avgMs,
 * p50Ms,p95Ms,epicIds}. {@code epicIds} is quoted and comma-delimited so a
 * downstream tool can split on the outer CSV comma first and then on the
 * inner list separator.</p>
 *
 * <p>Quoting rules (RFC 4180 §2):
 * <ul>
 *   <li>A field that contains {@code ,}, {@code "}, or {@code \n} is
 *       enclosed in double quotes.</li>
 *   <li>Literal {@code "} inside a quoted field is doubled ({@code ""}).
 *       </li>
 * </ul>
 * Skill / phase / tool names already match kebab-case + {@code /} so
 * quoting is rarely required, but the escape routine is applied
 * universally for safety.</p>
 */
public final class CsvReportRenderer {

    private static final String HEADER =
            "type,name,invocations,totalMs,avgMs,p50Ms,p95Ms,epicIds\n";

    /**
     * Renders the report as a CSV string (header + one row per stat).
     *
     * @param report the analysis report
     * @return the CSV document (ends with a trailing newline)
     */
    public String render(AnalysisReport report) {
        StringBuilder sb = new StringBuilder();
        sb.append(HEADER);
        appendRows(sb, "skill", report.skills());
        appendRows(sb, "phase", report.phases());
        appendRows(sb, "tool", report.tools());
        return sb.toString();
    }

    private static void appendRows(
            StringBuilder sb, String type, List<Stat> stats) {
        for (Stat s : stats) {
            sb.append(type).append(',')
                    .append(escape(s.name())).append(',')
                    .append(s.invocations()).append(',')
                    .append(s.totalMs()).append(',')
                    .append(s.avgMs()).append(',')
                    .append(s.p50Ms()).append(',')
                    .append(s.p95Ms()).append(',')
                    .append(escape(String.join(",", s.epicIds())))
                    .append('\n');
        }
    }

    private static String escape(String value) {
        if (value == null) {
            return "";
        }
        boolean needsQuote = value.indexOf(',') >= 0
                || value.indexOf('"') >= 0
                || value.indexOf('\n') >= 0;
        if (!needsQuote) {
            return value;
        }
        return '"' + value.replace("\"", "\"\"") + '"';
    }
}
