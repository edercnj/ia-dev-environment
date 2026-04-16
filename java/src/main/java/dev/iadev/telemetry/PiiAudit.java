package dev.iadev.telemetry;

import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileVisitOption;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.Callable;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.Option;
import picocli.CommandLine.Spec;

/**
 * Audit CLI that scans existing {@code events.ndjson} telemetry
 * files for PII leakage. Intended as a CI gate (non-zero exit
 * when any pattern is found) complementing the emit-time
 * {@link TelemetryScrubber}.
 *
 * <p>For each NDJSON file under {@code --root}, every line is
 * checked against the same rule set used by the scrubber. The
 * audit does NOT modify files; it only reports. Output is a
 * single line per finding formatted as
 * {@code FILE:LINE:CATEGORY:SNIPPET}, suitable for grep-style
 * consumption by CI systems.</p>
 *
 * <p>Exit codes mirror typical grep semantics and the picocli
 * norms used elsewhere in this project:
 * <ul>
 *   <li>{@code 0} — zero findings (clean)</li>
 *   <li>{@code 1} — one or more findings (dirty)</li>
 *   <li>{@code 2} — usage / I/O error</li>
 * </ul>
 */
@Command(
        name = "pii-audit",
        mixinStandardHelpOptions = true,
        description = "Audit events.ndjson for residual PII.")
public final class PiiAudit implements Callable<Integer> {

    /** Grep-clean exit code. */
    public static final int EXIT_CLEAN = 0;
    /** Findings exist exit code. */
    public static final int EXIT_FINDINGS = 1;
    /** I/O or usage error exit code. */
    public static final int EXIT_ERROR = 2;

    @Option(
            names = {"-r", "--root"},
            required = true,
            description = "Root directory to scan recursively"
                    + " for *.ndjson files.")
    Path root;

    @Option(
            names = {"-q", "--quiet"},
            description = "Suppress per-finding output; only"
                    + " the total count is printed.")
    boolean quiet;

    @Option(
            names = {"--follow-links"},
            description = "Follow symbolic links during"
                    + " traversal. Disabled by default to"
                    + " prevent escaping the intended tree"
                    + " or encountering cycles.")
    boolean followLinks;

    @Spec
    CommandSpec spec;

    private final List<ScrubRule> rules;

    /** Creates an audit with the default rule list. */
    public PiiAudit() {
        this(TelemetryScrubber.DEFAULT_RULES);
    }

    /**
     * Creates an audit with a custom rule list (primarily for
     * tests).
     *
     * @param rules the regex rules to apply; must not be null
     */
    public PiiAudit(List<ScrubRule> rules) {
        this.rules = List.copyOf(
                Objects.requireNonNull(rules, "rules"));
    }

    /**
     * Picocli entry-point. Resolves the PrintWriter from the
     * command spec, runs {@link #audit(Path, PrintWriter)}, and
     * returns the grep-style exit code.
     */
    @Override
    public Integer call() {
        PrintWriter out = spec.commandLine().getOut();
        PrintWriter err = spec.commandLine().getErr();
        try {
            List<Finding> findings = audit(root, out);
            if (findings.isEmpty()) {
                out.println("pii-audit: clean (0 findings)");
                return EXIT_CLEAN;
            }
            out.println(
                    "pii-audit: "
                            + findings.size()
                            + " finding(s)");
            return EXIT_FINDINGS;
        } catch (IOException e) {
            err.println(
                    "pii-audit: I/O error: "
                            + e.getMessage());
            return EXIT_ERROR;
        }
    }

    /**
     * Scans every {@code *.ndjson} under {@code root} and
     * returns the list of findings. When {@code reportSink} is
     * non-null, a line per finding is written
     * ({@code FILE:LINE:CATEGORY:SNIPPET}) unless the
     * {@code --quiet} flag is set.
     *
     * @param root       the directory to scan; must not be null
     * @param reportSink PrintWriter used for per-finding output,
     *                   or {@code null} to suppress
     * @return the findings in file-then-line order
     * @throws IOException when the filesystem traversal fails
     */
    public List<Finding> audit(
            Path root, PrintWriter reportSink)
            throws IOException {
        Objects.requireNonNull(root, "root is required");
        if (!Files.exists(root)) {
            throw new IOException(
                    "root does not exist: " + root);
        }
        List<Finding> findings = new ArrayList<>();
        FileVisitOption[] options = followLinks
                ? new FileVisitOption[] {
                        FileVisitOption.FOLLOW_LINKS}
                : new FileVisitOption[0];
        try (Stream<Path> stream = Files.walk(root, options)) {
            List<Path> files = stream
                    .filter(Files::isRegularFile)
                    .filter(p -> p.getFileName()
                            .toString()
                            .endsWith(".ndjson"))
                    .sorted()
                    .toList();
            for (Path file : files) {
                scanFile(file, findings, reportSink);
            }
        }
        return findings;
    }

    private void scanFile(
            Path file,
            List<Finding> findings,
            PrintWriter reportSink)
            throws IOException {
        try (var reader = Files.newBufferedReader(
                file, StandardCharsets.UTF_8)) {
            String line;
            int lineNumber = 0;
            while ((line = reader.readLine()) != null) {
                lineNumber++;
                for (ScrubRule rule : rules) {
                    Pattern pattern = rule.pattern();
                    var matcher = pattern.matcher(line);
                    if (matcher.find()) {
                        Finding f = new Finding(
                                file,
                                lineNumber,
                                rule.category(),
                                snippet(matcher.group()));
                        findings.add(f);
                        if (reportSink != null && !quiet) {
                            reportSink.println(f.format());
                        }
                    }
                }
            }
        }
    }

    private static String snippet(String match) {
        if (match.length() <= 40) {
            return match;
        }
        return match.substring(0, 40) + "...";
    }

    /** One row of the audit report. */
    public record Finding(
            Path file,
            int lineNumber,
            String category,
            String snippet) {
        /**
         * Canonical constructor with validation.
         *
         * @throws NullPointerException     when file, category
         *                                  or snippet is null
         * @throws IllegalArgumentException when lineNumber < 1
         */
        public Finding {
            Objects.requireNonNull(file, "file");
            Objects.requireNonNull(category, "category");
            Objects.requireNonNull(snippet, "snippet");
            if (lineNumber < 1) {
                throw new IllegalArgumentException(
                        "lineNumber must be >= 1: "
                                + lineNumber);
            }
        }

        /** @return grep-compatible string representation. */
        public String format() {
            return file + ":" + lineNumber
                    + ":" + category + ":" + snippet;
        }
    }

    /**
     * Process entry-point. Primarily wired through
     * {@link dev.iadev.cli.IaDevEnvApplication}, but available
     * for direct execution in scripts.
     *
     * @param args CLI arguments
     */
    public static void main(String[] args) {
        int code = new CommandLine(new PiiAudit())
                .execute(args);
        System.exit(code);
    }
}
