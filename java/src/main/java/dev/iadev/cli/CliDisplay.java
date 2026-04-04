package dev.iadev.cli;

import dev.iadev.domain.model.PipelineResult;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Formats pipeline results for terminal display.
 *
 * <p>File path categorization is delegated to
 * {@link FileCategorizer}.</p>
 *
 * @see PipelineResult
 * @see FileCategorizer
 */
public final class CliDisplay {

    private static final String SEPARATOR_CHAR = "\u2500";
    private static final String HEADER_LABEL = "Category";
    private static final String HEADER_COUNT = "Count";
    private static final String TOTAL_LABEL = "Total";
    private static final int MIN_LABEL_WIDTH = 20;

    private CliDisplay() {
        // utility class
    }

    /**
     * Classifies file paths into categories.
     *
     * @param paths the list of file paths to classify
     * @return ordered map of category to paths
     */
    public static Map<String, List<String>> classifyFiles(
            List<String> paths) {
        Map<String, List<String>> classified =
                new LinkedHashMap<>();

        for (String path : paths) {
            String normalized =
                    FileCategorizer.normalizePath(path);
            String category =
                    FileCategorizer.categorize(normalized);
            classified.computeIfAbsent(
                    category, k -> new ArrayList<>())
                    .add(path);
        }

        return classified;
    }

    /**
     * Formats a summary table from the classified file map.
     *
     * @param classified the classified file map
     * @return formatted table string
     */
    public static String formatSummaryTable(
            Map<String, List<String>> classified) {
        int total = classified.values().stream()
                .mapToInt(List::size).sum();
        int labelWidth = computeLabelWidth(classified);
        int countWidth = HEADER_COUNT.length();

        StringBuilder sb = new StringBuilder();
        appendHeader(sb, labelWidth, countWidth);
        appendCategoryRows(
                sb, classified, labelWidth, countWidth);
        appendFooter(
                sb, total, labelWidth, countWidth);

        return sb.toString();
    }

    private static void appendHeader(
            StringBuilder sb,
            int labelWidth, int countWidth) {
        String sep = SEPARATOR_CHAR.repeat(labelWidth);
        String cSep = SEPARATOR_CHAR.repeat(countWidth);
        sb.append("  ")
                .append(pad(HEADER_LABEL, labelWidth))
                .append("  ").append(HEADER_COUNT)
                .append('\n');
        sb.append("  ").append(sep)
                .append("  ").append(cSep).append('\n');
    }

    private static void appendCategoryRows(
            StringBuilder sb,
            Map<String, List<String>> classified,
            int labelWidth, int countWidth) {
        for (Map.Entry<String, List<String>> entry
                : classified.entrySet()) {
            int count = entry.getValue().size();
            if (count > 0) {
                sb.append("  ")
                        .append(pad(entry.getKey(),
                                labelWidth))
                        .append("  ")
                        .append(padLeft(
                                String.valueOf(count),
                                countWidth))
                        .append('\n');
            }
        }
    }

    private static void appendFooter(
            StringBuilder sb, int total,
            int labelWidth, int countWidth) {
        String sep = SEPARATOR_CHAR.repeat(labelWidth);
        String cSep = SEPARATOR_CHAR.repeat(countWidth);
        sb.append("  ").append(sep)
                .append("  ").append(cSep).append('\n');
        sb.append("  ")
                .append(pad(TOTAL_LABEL, labelWidth))
                .append("  ")
                .append(padLeft(
                        String.valueOf(total),
                        countWidth));
    }

    /**
     * Formats the complete pipeline result for display.
     *
     * @param result      the pipeline result
     * @param displayMode the display mode
     * @return formatted result string
     */
    public static String formatResult(
            PipelineResult result,
            DisplayMode displayMode) {
        StringBuilder sb = new StringBuilder();
        appendPipelineHeader(sb, result, displayMode);
        appendSummarySection(sb, result);
        appendDryRunDetails(sb, result, displayMode);
        appendWarnings(sb, result);
        return sb.toString();
    }

    private static void appendPipelineHeader(
            StringBuilder sb, PipelineResult result,
            DisplayMode displayMode) {
        if (displayMode.isDryRun()) {
            sb.append("[DRY RUN] ");
        }
        sb.append("Pipeline: Success (")
                .append(result.durationMs())
                .append("ms)\n\n");
    }

    private static void appendSummarySection(
            StringBuilder sb, PipelineResult result) {
        Map<String, List<String>> classified =
                classifyFiles(result.filesGenerated());
        sb.append(formatSummaryTable(classified));
        sb.append("\n\n");
        sb.append("Output: ").append(result.outputDir());
    }

    private static void appendDryRunDetails(
            StringBuilder sb, PipelineResult result,
            DisplayMode displayMode) {
        if (displayMode.isDryRun()
                && !result.filesGenerated().isEmpty()) {
            sb.append(
                    "\n\nFiles that would be generated:\n");
            for (String file : result.filesGenerated()) {
                sb.append("  ").append(file).append('\n');
            }
        }
    }

    private static void appendWarnings(
            StringBuilder sb, PipelineResult result) {
        for (String warning : result.warnings()) {
            sb.append("\nWarning: ").append(warning);
        }
    }

    /**
     * Formats verbose output for an assembler execution.
     *
     * @param assemblerName the assembler name
     * @param fileCount     number of files generated
     * @param durationMs    execution time in milliseconds
     * @return formatted verbose line
     */
    public static String formatAssemblerVerbose(
            String assemblerName,
            int fileCount,
            long durationMs) {
        return "%s completed in %dms (%d files)"
                .formatted(assemblerName,
                        durationMs, fileCount);
    }

    private static int computeLabelWidth(
            Map<String, List<String>> classified) {
        int maxWidth = Math.max(
                HEADER_LABEL.length(), MIN_LABEL_WIDTH);
        for (String label : classified.keySet()) {
            if (label.length() > maxWidth) {
                maxWidth = label.length();
            }
        }
        if (TOTAL_LABEL.length() > maxWidth) {
            maxWidth = TOTAL_LABEL.length();
        }
        return maxWidth;
    }

    private static String pad(String s, int width) {
        if (s.length() >= width) {
            return s;
        }
        return s + " ".repeat(width - s.length());
    }

    private static String padLeft(String s, int width) {
        if (s.length() >= width) {
            return s;
        }
        return " ".repeat(width - s.length()) + s;
    }
}
