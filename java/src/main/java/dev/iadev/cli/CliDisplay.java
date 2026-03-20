package dev.iadev.cli;

import dev.iadev.model.PipelineResult;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Formats pipeline results for terminal display.
 *
 * <p>Classifies generated file paths into categories based
 * on path prefixes, formats a summary table with counts,
 * and produces a complete result display.</p>
 *
 * @see PipelineResult
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
     * Classifies file paths into categories based on path
     * prefixes.
     *
     * <p>Each path is matched against known prefix patterns
     * and grouped into a category. Unknown paths are classified
     * as "Other".</p>
     *
     * @param paths the list of file paths to classify
     * @return ordered map of category name to list of paths
     */
    public static Map<String, List<String>> classifyFiles(
            List<String> paths) {
        Map<String, List<String>> classified =
                new LinkedHashMap<>();

        for (String path : paths) {
            String normalized = normalizePath(path);
            String category = categorize(normalized);
            classified.computeIfAbsent(
                    category, k -> new ArrayList<>())
                    .add(path);
        }

        return classified;
    }

    /**
     * Formats a summary table from the classified file map.
     *
     * <p>Produces a table with category names, counts, and a
     * total row. Uses U+2500 box-drawing horizontal character
     * for separator lines.</p>
     *
     * @param classified the classified file map
     * @return formatted table string
     */
    public static String formatSummaryTable(
            Map<String, List<String>> classified) {
        int total = classified.values().stream()
                .mapToInt(List::size)
                .sum();

        int labelWidth = computeLabelWidth(classified);
        int countWidth = HEADER_COUNT.length();

        String sep = SEPARATOR_CHAR.repeat(labelWidth);
        String cSep = SEPARATOR_CHAR.repeat(countWidth);

        StringBuilder sb = new StringBuilder();
        sb.append("  ")
                .append(pad(HEADER_LABEL, labelWidth))
                .append("  ")
                .append(HEADER_COUNT)
                .append('\n');
        sb.append("  ").append(sep)
                .append("  ").append(cSep)
                .append('\n');

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

        sb.append("  ").append(sep)
                .append("  ").append(cSep)
                .append('\n');
        sb.append("  ")
                .append(pad(TOTAL_LABEL, labelWidth))
                .append("  ")
                .append(padLeft(
                        String.valueOf(total), countWidth));

        return sb.toString();
    }

    /**
     * Formats the complete pipeline result for display.
     *
     * <p>Includes success status with duration, summary table,
     * output directory, and any warnings. In dry-run mode,
     * prefixes with "[DRY RUN]" and lists all files.</p>
     *
     * @param result      the pipeline result
     * @param displayMode the display mode (LIVE or DRY_RUN)
     * @return formatted result string
     */
    public static String formatResult(
            PipelineResult result,
            DisplayMode displayMode) {
        StringBuilder sb = new StringBuilder();

        if (displayMode.isDryRun()) {
            sb.append("[DRY RUN] ");
        }
        sb.append("Pipeline: Success (")
                .append(result.durationMs())
                .append("ms)\n\n");

        Map<String, List<String>> classified =
                classifyFiles(result.filesGenerated());
        sb.append(formatSummaryTable(classified));
        sb.append("\n\n");
        sb.append("Output: ").append(result.outputDir());

        if (displayMode.isDryRun()
                && !result.filesGenerated().isEmpty()) {
            sb.append("\n\nFiles that would be generated:\n");
            for (String file : result.filesGenerated()) {
                sb.append("  ").append(file).append('\n');
            }
        }

        for (String warning : result.warnings()) {
            sb.append("\nWarning: ").append(warning);
        }

        return sb.toString();
    }

    /**
     * Formats verbose output for a single assembler
     * execution.
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

    private static String normalizePath(String path) {
        return path.replace('\\', '/');
    }

    private static String categorize(String path) {
        if (path.startsWith(".claude/rules/")) {
            return "Rules";
        }
        if (path.startsWith(".claude/skills/")) {
            return "Skills";
        }
        if (path.startsWith(".claude/agents/")) {
            return "Agents";
        }
        if (path.startsWith(".claude/hooks/")) {
            return "Hooks";
        }
        if (path.startsWith(".claude/settings")) {
            return "Settings";
        }
        if (path.startsWith(".github/instructions/")) {
            return "GitHub Instructions";
        }
        if (path.startsWith(".github/skills/")) {
            return "GitHub Skills";
        }
        if (path.startsWith(".github/agents/")) {
            return "GitHub Agents";
        }
        if (path.startsWith(".github/hooks/")) {
            return "GitHub Hooks";
        }
        if (path.startsWith(".github/prompts/")) {
            return "GitHub Prompts";
        }
        if (path.startsWith(".github/copilot-")
                || path.startsWith(".github/copilot_")) {
            return "GitHub Config";
        }
        if (path.startsWith(".codex/")) {
            return "Codex";
        }
        if (path.startsWith(".agents/")) {
            return "Agents MD";
        }
        if (path.startsWith("docs/")) {
            return "Documentation";
        }
        if ("CLAUDE.md".equals(path)
                || "README.md".equals(path)
                || "AGENTS.md".equals(path)) {
            return "Root Files";
        }
        return "Other";
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
