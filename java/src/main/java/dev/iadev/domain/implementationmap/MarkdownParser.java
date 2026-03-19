package dev.iadev.domain.implementationmap;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Parses markdown tables from IMPLEMENTATION-MAP.md to extract
 * dependency matrix rows.
 *
 * <p>Recognizes Section 1 (dependency matrix) tables with the
 * format:
 * <pre>
 * | ID | Title | Blocked By |
 * | :--- | :--- | :--- |
 * | story-0006-0001 | Project Maven | - |
 * | story-0006-0005 | YAML Loader | story-0006-0002, story-0006-0003 |
 * </pre>
 */
public final class MarkdownParser {

    private static final Pattern SECTION_PATTERN =
            Pattern.compile("^##\\s+\\d+\\.\\s");
    private static final Pattern SEPARATOR_ROW =
            Pattern.compile("^\\|\\s*:?-+");
    private static final Pattern TABLE_ROW =
            Pattern.compile("^\\|");
    private static final Set<String> EMPTY_MARKERS =
            Set.of("-", "\u2014", "");
    private static final int MIN_CELLS = 3;

    private MarkdownParser() {
    }

    /**
     * Parses a markdown string to extract dependency matrix rows.
     *
     * @param markdown the full markdown content
     * @return list of parsed rows (empty if no table found)
     */
    public static List<DependencyMatrixRow> parse(String markdown) {
        if (markdown == null || markdown.isBlank()) {
            return List.of();
        }

        var lines = markdown.split("\n");
        var dataRows = extractTableDataRows(lines);
        var rows = new ArrayList<DependencyMatrixRow>();

        for (var line : dataRows) {
            var row = parseRow(line);
            if (row != null) {
                rows.add(row);
            }
        }

        return List.copyOf(rows);
    }

    private static List<String> extractTableDataRows(String[] lines) {
        var tableLines = new ArrayList<String>();
        boolean headerFound = false;

        for (var line : lines) {
            if (!TABLE_ROW.matcher(line).find()) {
                if (headerFound && !line.isBlank()) {
                    break;
                }
                continue;
            }
            if (!headerFound) {
                headerFound = true;
                continue;
            }
            if (SEPARATOR_ROW.matcher(line).find()) {
                continue;
            }
            tableLines.add(line);
        }

        return tableLines;
    }

    private static DependencyMatrixRow parseRow(String line) {
        var cells = splitTableRow(line);
        if (cells.size() < MIN_CELLS) {
            return null;
        }

        var storyId = cells.get(0).trim();
        var title = cells.get(1).trim();
        var blockedBy = parseStoryList(cells.get(2));

        return new DependencyMatrixRow(storyId, title, blockedBy);
    }

    private static List<String> splitTableRow(String line) {
        var parts = line.split("\\|");
        var cells = new ArrayList<String>();
        for (int i = 1; i < parts.length - 1; i++) {
            cells.add(parts[i].trim());
        }
        if (parts.length > 1
                && parts[parts.length - 1].isBlank()) {
            // trailing pipe present, already handled
        } else if (parts.length > 1) {
            cells.add(parts[parts.length - 1].trim());
        }
        return cells;
    }

    private static List<String> parseStoryList(String cell) {
        var trimmed = cell.trim();
        if (EMPTY_MARKERS.contains(trimmed)) {
            return List.of();
        }
        var ids = new ArrayList<String>();
        for (var id : trimmed.split(",")) {
            var stripped = id.trim();
            if (!stripped.isEmpty()) {
                ids.add(stripped);
            }
        }
        return List.copyOf(ids);
    }
}
