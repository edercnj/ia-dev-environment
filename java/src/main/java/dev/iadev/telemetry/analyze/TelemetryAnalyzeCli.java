package dev.iadev.telemetry.analyze;

import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.Callable;

import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Spec;
import picocli.CommandLine.Model.CommandSpec;

/**
 * Picocli subcommand for {@code /x-telemetry-analyze}. Parses arguments,
 * delegates aggregation to {@link EpicTelemetryAggregator}, delegates
 * rendering to {@link ReportRenderer}, and writes the result to stdout or
 * a file. Exit codes match story §5.3: {@code 0} success, {@code 2} no
 * telemetry, {@code 3} NDJSON parse error, {@code 4} {@code --export}
 * without {@code --out}.
 */
@Command(
        name = "telemetry-analyze",
        mixinStandardHelpOptions = true,
        description = "Analyze telemetry NDJSON and produce a "
                + "report (Markdown / JSON / CSV)."
)
public class TelemetryAnalyzeCli implements Callable<Integer> {

    /** Exit code when {@code --export} is missing its {@code --out} pair. */
    public static final int EXIT_EXPORT_MISSING_OUT = 4;

    /** Exit code when NDJSON cannot be parsed. */
    public static final int EXIT_CORRUPT_NDJSON = 3;

    /** Exit code when the epic has no telemetry data. */
    public static final int EXIT_NO_TELEMETRY = 2;

    /** Exit code for validation / argument errors. */
    public static final int EXIT_VALIDATION = 1;

    @Option(names = "--epic",
            description = "Single epic to analyze (EPIC-XXXX).")
    String epic;

    @Option(names = "--epics", split = ",",
            description = "Multiple epics to analyze "
                    + "(comma-separated).")
    List<String> epics;

    @Option(names = "--base-dir",
            description = "Override the base plans directory.")
    Path baseDir;

    @Option(names = "--out",
            description = "Output path for the rendered report "
                    + "(required when --export is set).")
    Path out;

    @Option(names = "--export",
            description = "Export format: json or csv. Markdown is "
                    + "the default when this flag is absent.")
    String exportFormat;

    @Option(names = "--since",
            description = "Filter events with timestamp >= "
                    + "YYYY-MM-DD (ISO-8601 date).")
    String since;

    @Option(names = "--by-tool",
            description = "Emphasize the tool breakdown in the "
                    + "Markdown output.")
    boolean byTool;

    @Spec
    CommandSpec spec;

    /** Public no-arg constructor required by Picocli reflection. */
    public TelemetryAnalyzeCli() {
    }

    @Override
    public Integer call() {
        List<String> epicIds = resolveEpics();
        if (epicIds.isEmpty()) {
            spec.commandLine().getErr().println(
                    "Usage: --epic EPIC-XXXX | --epics A,B,...");
            return EXIT_VALIDATION;
        }
        if (exportFormat != null && out == null) {
            spec.commandLine().getErr().println(
                    "--export requires --out <path>");
            return EXIT_EXPORT_MISSING_OUT;
        }
        Optional<Instant> sinceInstant;
        try {
            sinceInstant = parseSince(since);
        } catch (DateTimeParseException e) {
            spec.commandLine().getErr().println(
                    "Invalid --since value '" + since
                            + "' (expected YYYY-MM-DD)");
            return EXIT_VALIDATION;
        }
        Path base = baseDir != null ? baseDir : Path.of("plans");

        AnalysisReport report;
        try {
            report = new EpicTelemetryAggregator()
                    .aggregate(epicIds, base, sinceInstant);
        } catch (EpicTelemetryAggregator.NoTelemetryException e) {
            spec.commandLine().getErr().println(e.getMessage());
            return EXIT_NO_TELEMETRY;
        } catch (IllegalArgumentException e) {
            spec.commandLine().getErr().println(
                    "Could not parse telemetry: " + e.getMessage());
            return EXIT_CORRUPT_NDJSON;
        }

        return writeOutput(report, epicIds, base);
    }

    private List<String> resolveEpics() {
        Set<String> collected = new LinkedHashSet<>();
        if (epic != null && !epic.isBlank()) {
            collected.add(epic.trim());
        }
        if (epics != null) {
            for (String e : epics) {
                if (e != null && !e.isBlank()) {
                    collected.add(e.trim());
                }
            }
        }
        return new ArrayList<>(collected);
    }

    private Optional<Instant> parseSince(String value) {
        if (value == null || value.isBlank()) {
            return Optional.empty();
        }
        // Accept pure date (YYYY-MM-DD) -> midnight UTC.
        if (value.length() == 10 && value.charAt(4) == '-'
                && value.charAt(7) == '-') {
            return Optional.of(Instant.parse(value + "T00:00:00Z"));
        }
        return Optional.of(Instant.parse(value));
    }

    private Integer writeOutput(
            AnalysisReport report,
            List<String> epicIds,
            Path base) {
        String rendered = new ReportRenderer()
                .render(report, exportFormat);
        Path target = out != null
                ? out
                : defaultReportPath(epicIds, base);
        try {
            Path parent = target.getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
            Files.writeString(target, rendered, StandardCharsets.UTF_8);
        } catch (java.io.IOException e) {
            spec.commandLine().getErr().println(
                    "Failed to write report: " + e.getMessage());
            return EXIT_VALIDATION;
        }
        PrintWriter writer = spec.commandLine().getOut();
        writer.println("Report written to " + target);
        writer.flush();
        return 0;
    }

    private static Path defaultReportPath(
            List<String> epicIds, Path base) {
        String first = epicIds.get(0);
        String suffix = EpicTelemetryAggregator.extractEpicSuffix(first);
        return base.resolve("epic-" + suffix)
                .resolve("reports")
                .resolve("telemetry-report-" + first + ".md");
    }

    /**
     * Standalone entry point.
     *
     * @param args command-line arguments
     */
    public static void main(String[] args) {
        int code = new CommandLine(new TelemetryAnalyzeCli())
                .execute(args);
        System.exit(code);
    }
}
