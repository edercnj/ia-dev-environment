package dev.iadev.telemetry.analyze;

import java.util.Locale;

/**
 * Dispatches an {@link AnalysisReport} to the appropriate format-specific
 * renderer ({@link MarkdownReportRenderer}, {@link JsonReportRenderer},
 * or {@link CsvReportRenderer}).
 *
 * <p>Extracted from {@link TelemetryAnalyzeCli} to isolate the format
 * selection concern from the CLI concerns (argument parsing, event
 * aggregation, output destination). Matches the naming symmetry used by
 * the trend package siblings ({@code TrendJsonRenderer},
 * {@code TrendMarkdownRenderer}).</p>
 *
 * <p>Supported {@code format} tokens (case-insensitive):
 * <ul>
 *   <li>{@code md} — Markdown report (default when {@code --export} is
 *       absent)</li>
 *   <li>{@code json} — JSON document matching story-0040-0010 §5.1</li>
 *   <li>{@code csv} — CSV document matching story-0040-0010 §5.2</li>
 * </ul></p>
 */
public final class ReportRenderer {

    private final MarkdownReportRenderer markdown;
    private final JsonReportRenderer json;
    private final CsvReportRenderer csv;

    /** Default constructor using the standard format-specific renderers. */
    public ReportRenderer() {
        this(new MarkdownReportRenderer(),
                new JsonReportRenderer(),
                new CsvReportRenderer());
    }

    /** Constructor for dependency injection (tests). */
    ReportRenderer(
            MarkdownReportRenderer markdown,
            JsonReportRenderer json,
            CsvReportRenderer csv) {
        this.markdown = markdown;
        this.json = json;
        this.csv = csv;
    }

    /**
     * Renders {@code report} in the requested {@code format}.
     *
     * @param report  analysis payload to render (non-null)
     * @param format  one of {@code md}, {@code json}, {@code csv}; when
     *                {@code null} or blank, defaults to {@code md}
     * @return the rendered report
     * @throws IllegalArgumentException when {@code format} is not one of
     *                                  the supported tokens
     */
    public String render(AnalysisReport report, String format) {
        String token = normalizeFormat(format);
        return switch (token) {
            case "md" -> renderMarkdown(report);
            case "json" -> renderJson(report);
            case "csv" -> renderCsv(report);
            default -> throw new IllegalArgumentException(
                    "Unknown report format '" + format
                            + "' (expected md, json, or csv)");
        };
    }

    /** Renders {@code report} as Markdown. */
    public String renderMarkdown(AnalysisReport report) {
        return markdown.render(report);
    }

    /** Renders {@code report} as JSON. */
    public String renderJson(AnalysisReport report) {
        return json.render(report);
    }

    /** Renders {@code report} as CSV. */
    public String renderCsv(AnalysisReport report) {
        return csv.render(report);
    }

    private static String normalizeFormat(String format) {
        if (format == null || format.isBlank()) {
            return "md";
        }
        return format.trim().toLowerCase(Locale.ROOT);
    }
}
