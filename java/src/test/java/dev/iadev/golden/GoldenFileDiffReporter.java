package dev.iadev.golden;

import java.util.ArrayList;
import java.util.List;

/**
 * Generates unified diff reports comparing expected (golden)
 * and actual (generated) file content.
 *
 * <p>Produces human-readable diff output with context lines,
 * line numbers, and invisible whitespace highlighting. Output
 * is limited to {@link #MAX_DIFF_LINES} lines to prevent
 * overwhelming test logs.</p>
 *
 * <p>Example usage:
 * <pre>{@code
 * String diff = GoldenFileDiffReporter.generateDiff(
 *     "path/to/file.md", expectedContent, actualContent);
 * if (!diff.isEmpty()) {
 *     fail("Golden file mismatch:\n" + diff);
 * }
 * }</pre>
 * </p>
 *
 * @see GoldenFileTest
 */
public final class GoldenFileDiffReporter {

    /** Maximum number of diff lines to output per file. */
    public static final int MAX_DIFF_LINES = 50;

    private static final int CONTEXT_LINES = 3;

    private GoldenFileDiffReporter() {
        // utility class
    }

    /**
     * Generates a unified diff between expected and actual
     * content for the given file path.
     *
     * <p>Returns an empty string if the contents are
     * identical. Otherwise, returns a diff report with
     * context lines and invisible whitespace markers.</p>
     *
     * @param filePath the relative path of the file being
     *                 compared (used in diff header)
     * @param expected the expected (golden) file content
     * @param actual   the actual (generated) file content
     * @return the diff report, or empty string if identical
     */
    public static String generateDiff(
            String filePath,
            String expected,
            String actual) {
        if (expected.equals(actual)) {
            return "";
        }

        List<String> expectedLines = splitLines(expected);
        List<String> actualLines = splitLines(actual);

        List<String> diffLines = new ArrayList<>();
        diffLines.add("--- golden/%s".formatted(filePath));
        diffLines.add("+++ generated/%s".formatted(filePath));

        appendLineDiffs(
                diffLines, expectedLines, actualLines);

        return String.join("\n", diffLines);
    }

    private static void appendLineDiffs(
            List<String> diffLines,
            List<String> expectedLines,
            List<String> actualLines) {
        int maxLen = Math.max(
                expectedLines.size(),
                actualLines.size());
        boolean[] firstDiff = {true};

        for (int i = 0; i < maxLen; i++) {
            if (isTruncated(diffLines)) {
                break;
            }
            processLine(diffLines, expectedLines,
                    actualLines, i, firstDiff);
        }
    }

    private static boolean isTruncated(
            List<String> diffLines) {
        if (diffLines.size() >= MAX_DIFF_LINES) {
            diffLines.add(
                    "... (truncated, >%d lines)"
                            .formatted(MAX_DIFF_LINES));
            return true;
        }
        return false;
    }

    private static void processLine(
            List<String> diffLines,
            List<String> expectedLines,
            List<String> actualLines,
            int i, boolean[] firstDiff) {
        String expLine = i < expectedLines.size()
                ? expectedLines.get(i) : null;
        String actLine = i < actualLines.size()
                ? actualLines.get(i) : null;

        if (linesMatch(expLine, actLine)) {
            return;
        }
        if (firstDiff[0]) {
            addContextBefore(
                    diffLines, expectedLines, i);
            firstDiff[0] = false;
        }
        appendDiffEntry(
                diffLines, expLine, actLine, i);
    }

    private static boolean linesMatch(
            String expLine, String actLine) {
        return expLine != null && actLine != null
                && expLine.equals(actLine);
    }

    private static void appendDiffEntry(
            List<String> diffLines,
            String expLine,
            String actLine,
            int index) {
        if (expLine != null && actLine != null) {
            diffLines.add(
                    "@@ line %d @@".formatted(index + 1));
            diffLines.add(
                    "- %s".formatted(visualize(expLine)));
            diffLines.add(
                    "+ %s".formatted(visualize(actLine)));
        } else if (expLine == null) {
            diffLines.add(
                    "@@ line %d (added) @@"
                            .formatted(index + 1));
            diffLines.add(
                    "+ %s".formatted(visualize(actLine)));
        } else {
            diffLines.add(
                    "@@ line %d (removed) @@"
                            .formatted(index + 1));
            diffLines.add(
                    "- %s".formatted(visualize(expLine)));
        }
    }

    private static List<String> splitLines(String content) {
        if (content.isEmpty()) {
            return List.of();
        }
        List<String> result = new ArrayList<>();
        int start = 0;
        for (int i = 0; i < content.length(); i++) {
            if (content.charAt(i) == '\n') {
                result.add(content.substring(start, i));
                start = i + 1;
            }
        }
        if (start < content.length()) {
            result.add(content.substring(start));
        }
        return result;
    }

    private static void addContextBefore(
            List<String> diffLines,
            List<String> expectedLines,
            int currentIndex) {
        int contextStart = Math.max(
                0, currentIndex - CONTEXT_LINES);
        for (int c = contextStart; c < currentIndex; c++) {
            if (c < expectedLines.size()) {
                diffLines.add(
                        "  " + expectedLines.get(c));
            }
        }
    }

    /**
     * Makes invisible whitespace characters visible in the
     * diff output.
     *
     * @param line the line to visualize
     * @return the line with invisible characters replaced by
     *         visible markers
     */
    static String visualize(String line) {
        return line
                .replace("\r", "\\r\\n")
                .replace("\t", "\\t");
    }
}
