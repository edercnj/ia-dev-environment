package dev.iadev.telemetry.trend;

import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.Callable;

import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Spec;
import picocli.CommandLine.Model.CommandSpec;

/**
 * Picocli entry point for {@code /x-telemetry-trend}.
 *
 * <p>Consumes the cached telemetry index produced by
 * {@link TelemetryIndexBuilder}, detects P95 regressions across the most
 * recent N epics, and renders a report in Markdown (default) or JSON.</p>
 *
 * <p>Exit codes (story §5.3):
 * <ul>
 *   <li>{@code 0} — success</li>
 *   <li>{@code 1} — validation error (bad argument format)</li>
 *   <li>{@code 5} — fewer than 2 epics with data</li>
 *   <li>{@code 6} — {@code --threshold-pct} is negative</li>
 * </ul>
 * </p>
 */
@Command(
        name = "telemetry-trend",
        mixinStandardHelpOptions = true,
        description = "Detect cross-epic P95 regressions and rank "
                + "slowest skills."
)
public class TelemetryTrendCli implements Callable<Integer> {

    /** Exit code when fewer than 2 epics have telemetry data. */
    public static final int EXIT_INSUFFICIENT_EPICS = 5;

    /** Exit code when {@code --threshold-pct} is negative. */
    public static final int EXIT_THRESHOLD_NEGATIVE = 6;

    /** Exit code for generic argument validation errors. */
    public static final int EXIT_VALIDATION = 1;

    @Option(names = "--last",
            description = "Number of most-recent epics to analyze "
                    + "(default 5).")
    int last = 5;

    @Option(names = "--threshold-pct",
            description = "Minimum delta %% qualifying as a regression "
                    + "(default 20).")
    double thresholdPct = 20.0;

    @Option(names = "--baseline",
            description = "Baseline strategy: mean or median "
                    + "(default median).")
    String baseline = "median";

    @Option(names = "--format",
            description = "Output format: md (default) or json.")
    String format = "md";

    @Option(names = "--out",
            description = "Destination path. When omitted, the report "
                    + "is written to stdout (RULE-007).")
    Path out;

    @Option(names = "--base-dir",
            description = "Override the base plans directory "
                    + "(test-only).")
    Path baseDir;

    @Option(names = "--index-path",
            description = "Override the cache index path "
                    + "(test-only).")
    Path indexPath;

    @Option(names = "--rebuild-index",
            description = "Ignore the cache and rebuild from NDJSON.")
    boolean rebuildIndex;

    @Spec
    CommandSpec spec;

    /** Required public no-arg constructor for Picocli reflection. */
    public TelemetryTrendCli() {
    }

    @Override
    public Integer call() {
        Integer validationCode = validateArgs();
        if (validationCode != null) {
            return validationCode;
        }
        BaselineStrategy strategy = BaselineStrategy.parse(baseline);
        String fmt = normalizedFormat();

        TelemetryIndex index = loadIndex();
        TelemetryTrendAnalyzer analyzer = new TelemetryTrendAnalyzer();
        int epicCount = analyzer.epicCount(index);
        if (epicCount < 2) {
            spec.commandLine().getErr().println(
                    "Need at least 2 epics for trend analysis, found "
                            + epicCount);
            return EXIT_INSUFFICIENT_EPICS;
        }

        TrendReport report = analyzer.analyze(
                index, last, thresholdPct, strategy);
        String rendered = "json".equals(fmt)
                ? new TrendJsonRenderer().render(report)
                : new TrendMarkdownRenderer().render(report);
        return emitReport(rendered);
    }

    /**
     * Pure argument validation. Returns a non-null exit code when an
     * argument is invalid, null otherwise.
     */
    private Integer validateArgs() {
        if (thresholdPct < 0) {
            spec.commandLine().getErr().println(
                    "--threshold-pct must be >= 0");
            return EXIT_THRESHOLD_NEGATIVE;
        }
        if (last < 1) {
            spec.commandLine().getErr().println(
                    "--last must be >= 1");
            return EXIT_VALIDATION;
        }
        try {
            BaselineStrategy.parse(baseline);
        } catch (IllegalArgumentException e) {
            spec.commandLine().getErr().println(e.getMessage());
            return EXIT_VALIDATION;
        }
        String fmt = normalizedFormat();
        if (!"md".equals(fmt) && !"json".equals(fmt)) {
            spec.commandLine().getErr().println(
                    "--format must be 'md' or 'json', got '"
                            + format + "'");
            return EXIT_VALIDATION;
        }
        return null;
    }

    private String normalizedFormat() {
        return format == null ? "md" : format.trim().toLowerCase();
    }

    private TelemetryIndex loadIndex() {
        Path base = baseDir != null ? baseDir : Path.of("plans");
        TelemetryIndexBuilder builder = indexPath != null
                ? new TelemetryIndexBuilder(base, indexPath)
                : new TelemetryIndexBuilder(base);
        return rebuildIndex
                ? builder.rebuild()
                : builder.buildOrRefresh();
    }

    private Integer emitReport(String rendered) {
        if (out == null) {
            PrintWriter writer = spec.commandLine().getOut();
            writer.println(rendered);
            writer.flush();
            return 0;
        }
        try {
            Path parent = out.getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
            Files.writeString(out, rendered,
                    StandardCharsets.UTF_8);
        } catch (java.io.IOException e) {
            spec.commandLine().getErr().println(
                    "Failed to write report: " + e.getMessage());
            return EXIT_VALIDATION;
        }
        spec.commandLine().getOut().println(
                "Report written to " + out);
        return 0;
    }

    /**
     * Standalone entry point.
     *
     * @param args command-line arguments
     */
    public static void main(String[] args) {
        int code = new CommandLine(new TelemetryTrendCli())
                .execute(args);
        System.exit(code);
    }
}
