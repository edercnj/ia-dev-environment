package dev.iadev.telemetry.analyze;

import dev.iadev.telemetry.TelemetryEvent;
import dev.iadev.telemetry.TelemetryReader;

import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.stream.Stream;

import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Spec;
import picocli.CommandLine.Model.CommandSpec;

/**
 * Picocli subcommand implementing {@code /x-telemetry-analyze}.
 *
 * <p>Reads NDJSON telemetry from {@code plans/epic-XXXX/telemetry/events.
 * ndjson} for one or more epics, aggregates duration statistics via
 * {@link TelemetryAggregator}, and renders a report in Markdown (default),
 * JSON, or CSV.</p>
 *
 * <p>Exit codes match story §5.3:
 * <ul>
 *   <li>{@code 0} — success</li>
 *   <li>{@code 2} — epic has no telemetry data</li>
 *   <li>{@code 3} — NDJSON parse error</li>
 *   <li>{@code 4} — {@code --export} passed without {@code --out}</li>
 * </ul>
 * </p>
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

    /**
     * Public no-arg constructor required by Picocli reflection.
     */
    public TelemetryAnalyzeCli() {
    }

    /**
     * Resolves the argument set into a call, reads telemetry, aggregates
     * and renders the report.
     *
     * @return the process exit code
     */
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
        Instant sinceInstant;
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
            report = aggregateEpics(epicIds, base, sinceInstant);
        } catch (NoTelemetryException e) {
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
        Set<String> out = new LinkedHashSet<>();
        if (epic != null && !epic.isBlank()) {
            out.add(epic.trim());
        }
        if (epics != null) {
            for (String e : epics) {
                if (e != null && !e.isBlank()) {
                    out.add(e.trim());
                }
            }
        }
        return new ArrayList<>(out);
    }

    private Instant parseSince(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        // Accept pure date (YYYY-MM-DD) -> midnight UTC.
        if (value.length() == 10 && value.charAt(4) == '-'
                && value.charAt(7) == '-') {
            return Instant.parse(value + "T00:00:00Z");
        }
        return Instant.parse(value);
    }

    private AnalysisReport aggregateEpics(
            List<String> epicIds, Path base, Instant sinceInstant) {
        TelemetryAggregator aggregator = new TelemetryAggregator();
        // Streaming pass per epic — never buffer the full event set.
        // Each epic's stream is resolved lazily and closed by the
        // aggregator's iterator drain. The running list keeps track of
        // opened streams so we can close them once the aggregation is done
        // (for-each above auto-closes on normal termination; we mirror
        // that guarantee with an explicit finally).
        List<Stream<TelemetryEvent>> opened =
                new ArrayList<>(epicIds.size());
        try {
            Stream<TelemetryEvent> unified = Stream.empty();
            for (String epicId : epicIds) {
                Path events = base
                        .resolve("epic-" + extractEpicSuffix(epicId))
                        .resolve("telemetry")
                        .resolve("events.ndjson");
                if (!Files.exists(events)) {
                    throw new NoTelemetryException(
                            "Epic " + epicId
                                    + " has no telemetry data at "
                                    + events);
                }
                Stream<TelemetryEvent> perEpic =
                        TelemetryReader.open(events)
                                .streamSkippingInvalid()
                                .filter(e -> sinceInstant == null
                                        || !e.timestamp()
                                                .isBefore(
                                                        sinceInstant));
                opened.add(perEpic);
                unified = Stream.concat(unified, perEpic);
            }
            return aggregator.aggregate(unified, epicIds);
        } finally {
            for (Stream<TelemetryEvent> s : opened) {
                try {
                    s.close();
                } catch (RuntimeException ignored) {
                    // Fail-open: closing a telemetry stream must never
                    // abort the caller.
                }
            }
        }
    }

    private static String extractEpicSuffix(String epicId) {
        String prefix = "EPIC-";
        if (epicId.startsWith(prefix)) {
            return epicId.substring(prefix.length());
        }
        return epicId;
    }

    private Integer writeOutput(
            AnalysisReport report,
            List<String> epicIds,
            Path base) {
        String rendered;
        if ("json".equalsIgnoreCase(exportFormat)) {
            rendered = new JsonReportRenderer().render(report);
        } else if ("csv".equalsIgnoreCase(exportFormat)) {
            rendered = new CsvReportRenderer().render(report);
        } else {
            rendered = new MarkdownReportRenderer().render(report);
        }

        Path target = out;
        if (target == null) {
            target = defaultReportPath(epicIds, base);
        }
        try {
            Path parent = target.getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
            Files.writeString(target, rendered,
                    StandardCharsets.UTF_8);
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
        String suffix = extractEpicSuffix(first);
        return base.resolve("epic-" + suffix)
                .resolve("reports")
                .resolve("telemetry-report-" + first + ".md");
    }

    /**
     * Static {@code main} so the class is executable via
     * {@code java -cp ... TelemetryAnalyzeCli}.
     *
     * @param args command-line arguments
     */
    public static void main(String[] args) {
        int code = new CommandLine(new TelemetryAnalyzeCli())
                .execute(args);
        System.exit(code);
    }

    /**
     * Thrown internally when the targeted epic has no NDJSON file on
     * disk. Callers translate this into {@link #EXIT_NO_TELEMETRY}.
     */
    static final class NoTelemetryException
            extends RuntimeException {

        private static final long serialVersionUID = 1L;

        NoTelemetryException(String message) {
            super(message);
        }
    }
}
