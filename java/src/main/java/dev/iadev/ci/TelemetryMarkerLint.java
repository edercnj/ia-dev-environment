package dev.iadev.ci;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * CI-grade validator for telemetry phase markers inserted by
 * story-0040-0006.
 *
 * <p>Scans a {@code SKILL.md} body for calls to
 * {@code telemetry-phase.sh start|end SKILL PHASE [STATUS]} and rejects:
 * <ul>
 *   <li><b>DUPLICATE_START</b> — two consecutive {@code phase.start} for
 *       the same {@code (skill, phase)} with no matching {@code end} in
 *       between.</li>
 *   <li><b>DUPLICATE_END</b> — two consecutive {@code phase.end} for the
 *       same {@code (skill, phase)} with no matching {@code start} in
 *       between.</li>
 *   <li><b>DANGLING_END</b> — a {@code phase.end} that has no preceding
 *       {@code phase.start} with the same {@code (skill, phase)}.</li>
 *   <li><b>UNCLOSED_START</b> — a {@code phase.start} with no matching
 *       {@code phase.end} before the file ends.</li>
 * </ul>
 *
 * <p>The linter is used by a CI step and by acceptance tests that need to
 * prove a SKILL.md is correctly instrumented. See RULE-013 and
 * story-0040-0006 §5.3.
 */
public final class TelemetryMarkerLint {

    private static final Pattern MARKER_PATTERN = Pattern.compile(
            "telemetry-phase\\.sh\\s+(start|end)\\s+"
                    + "([A-Za-z0-9_-]+)\\s+"
                    + "([A-Za-z0-9_-]+)(?:\\s+([A-Za-z0-9_-]+))?");

    private TelemetryMarkerLint() {
        // Static utility class.
    }

    /**
     * Lint a single SKILL.md file.
     *
     * @param skillFile absolute path to the file
     * @return list of findings; empty when the file is clean
     */
    public static List<Finding> lint(Path skillFile) {
        try {
            List<String> lines = Files.readAllLines(
                    skillFile, StandardCharsets.UTF_8);
            return lintLines(skillFile, lines);
        } catch (IOException e) {
            throw new UncheckedIOException(
                    "Failed to read SKILL.md: " + skillFile, e);
        }
    }

    /**
     * Lint a pre-read SKILL.md body (exposed for tests that build input in
     * memory).
     *
     * @param skillFile the logical file path reported in findings
     * @param lines the lines of the file in order
     * @return list of findings; empty when the body is clean
     */
    public static List<Finding> lintLines(
            Path skillFile, List<String> lines) {
        List<Finding> findings = new ArrayList<>();
        List<Marker> markers = extractMarkers(skillFile, lines);
        validateBalance(markers, findings);
        return findings;
    }

    private static List<Marker> extractMarkers(
            Path file, List<String> lines) {
        List<Marker> markers = new ArrayList<>();
        for (int i = 0; i < lines.size(); i++) {
            String line = lines.get(i);
            Matcher m = MARKER_PATTERN.matcher(line);
            while (m.find()) {
                String kind = m.group(1);
                String skill = m.group(2);
                String phase = m.group(3);
                markers.add(new Marker(
                        file, i + 1, kind, skill, phase));
            }
        }
        return markers;
    }

    private static void validateBalance(
            List<Marker> markers, List<Finding> findings) {
        // Track currently "open" phases keyed by skill+phase. The value is
        // the line number of the last `start` seen.
        java.util.Map<String, Integer> openStart =
                new java.util.LinkedHashMap<>();
        // Track the last terminal kind per phase so we can catch
        // DUPLICATE_END without an intervening start.
        java.util.Map<String, String> lastKind =
                new java.util.HashMap<>();

        Path file = null;
        for (Marker marker : markers) {
            file = marker.file();
            String key = marker.skill() + "::" + marker.phase();
            String kind = marker.kind();

            if ("start".equals(kind)) {
                if (openStart.containsKey(key)) {
                    findings.add(new Finding(
                            marker.file(),
                            marker.line(),
                            FindingType.DUPLICATE_START,
                            marker.skill(),
                            marker.phase(),
                            "Consecutive phase.start without"
                                    + " a matching phase.end"));
                } else {
                    openStart.put(key, marker.line());
                }
                lastKind.put(key, kind);
                continue;
            }

            // kind == "end"
            if (!openStart.containsKey(key)) {
                if ("end".equals(lastKind.get(key))) {
                    findings.add(new Finding(
                            marker.file(),
                            marker.line(),
                            FindingType.DUPLICATE_END,
                            marker.skill(),
                            marker.phase(),
                            "Consecutive phase.end without"
                                    + " a matching phase.start"));
                } else {
                    findings.add(new Finding(
                            marker.file(),
                            marker.line(),
                            FindingType.DANGLING_END,
                            marker.skill(),
                            marker.phase(),
                            "phase.end with no preceding"
                                    + " phase.start"));
                }
            } else {
                openStart.remove(key);
            }
            lastKind.put(key, kind);
        }

        // Any remaining open starts are UNCLOSED.
        for (var entry : openStart.entrySet()) {
            String[] parts = entry.getKey().split("::", 2);
            findings.add(new Finding(
                    file,
                    entry.getValue(),
                    FindingType.UNCLOSED_START,
                    parts[0],
                    parts.length > 1 ? parts[1] : "",
                    "phase.start with no matching phase.end"
                            + " before end of file"));
        }
    }

    /** Kind of violation reported by the linter. */
    public enum FindingType {
        DUPLICATE_START,
        DUPLICATE_END,
        DANGLING_END,
        UNCLOSED_START
    }

    /** One parsed marker occurrence. */
    private record Marker(
            Path file,
            int line,
            String kind,
            String skill,
            String phase) {
    }

    /**
     * A linter finding. Immutable.
     *
     * @param file the source file
     * @param line line number (1-based) where the violation was detected
     * @param type kind of violation
     * @param skill skill identifier extracted from the marker
     * @param phase phase identifier extracted from the marker
     * @param message human-readable explanation
     */
    public record Finding(
            Path file,
            int line,
            FindingType type,
            String skill,
            String phase,
            String message) {
    }
}
