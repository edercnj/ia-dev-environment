package dev.iadev.domain.implementationmap;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Parses markdown tables from IMPLEMENTATION-MAP.md to extract
 * dependency matrix rows.
 *
 * <p>Supports multiple table formats by detecting column
 * positions from the header row:
 * <ul>
 *   <li><b>6-column (current):</b>
 *   {@code | ID | Title | Chave Jira | Blocked By | ... |}</li>
 *   <li><b>5-column (legacy with Jira at end):</b>
 *   {@code | ID | Title | Blocked By | ... | Chave Jira |}</li>
 *   <li><b>3+ column (legacy no Jira):</b>
 *   {@code | ID | Title | Blocked By | ...}</li>
 * </ul>
 *
 * <p>Column detection uses the header row to find "Jira" and
 * "Blocked" keywords case-insensitively. Falls back to column
 * index 2 for Blocked By when no header match is found.</p>
 */
public final class MarkdownParser {

    private static final Pattern SEPARATOR_ROW =
            Pattern.compile("^\\|\\s*:?-+");
    private static final Pattern TABLE_ROW =
            Pattern.compile("^\\|");
    private static final Set<String> EMPTY_MARKERS =
            Set.of("-", "\u2014", "");
    private static final int MIN_CELLS = 3;
    private static final int DEFAULT_BLOCKED_BY_COL = 2;
    private static final int NO_COLUMN = -1;

    private MarkdownParser() {
    }

    /**
     * Parses a markdown string to extract dependency matrix rows.
     *
     * @param markdown the full markdown content
     * @return list of parsed rows (empty if no table found)
     */
    public static List<DependencyMatrixRow> parse(
            String markdown) {
        if (markdown == null || markdown.isBlank()) {
            return List.of();
        }

        var lines = markdown.split("\n");
        var extraction = extractHeaderAndDataRows(lines);
        if (extraction.dataRows().isEmpty()) {
            return List.of();
        }

        var layout = detectColumnLayout(
                extraction.headerCells());
        var rows = new ArrayList<DependencyMatrixRow>();

        for (var line : extraction.dataRows()) {
            parseRow(line, layout).ifPresent(rows::add);
        }

        return List.copyOf(rows);
    }

    private static TableExtraction extractHeaderAndDataRows(
            String[] lines) {
        var tableLines = new ArrayList<String>();
        List<String> headerCells = List.of();
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
                headerCells = splitTableRow(line);
                continue;
            }
            if (SEPARATOR_ROW.matcher(line).find()) {
                continue;
            }
            tableLines.add(line);
        }

        return new TableExtraction(headerCells, tableLines);
    }

    static ColumnLayout detectColumnLayout(
            List<String> headerCells) {
        int jiraCol = NO_COLUMN;
        int blockedByCol = NO_COLUMN;

        for (int i = 0; i < headerCells.size(); i++) {
            var lower = headerCells.get(i).toLowerCase();
            if (lower.contains("jira")) {
                jiraCol = i;
            }
            if (lower.contains("blocked")) {
                blockedByCol = i;
            }
        }

        if (blockedByCol == NO_COLUMN) {
            blockedByCol = DEFAULT_BLOCKED_BY_COL;
        }

        return new ColumnLayout(jiraCol, blockedByCol);
    }

    private static Optional<DependencyMatrixRow> parseRow(
            String line, ColumnLayout layout) {
        var cells = splitTableRow(line);
        if (cells.size() < MIN_CELLS) {
            return Optional.empty();
        }

        var storyId = cells.get(0).trim();
        var title = cells.get(1).trim();

        Optional<String> jiraKey = Optional.empty();
        if (layout.jiraCol() != NO_COLUMN
                && cells.size() > layout.jiraCol()) {
            jiraKey = parseJiraKey(
                    cells.get(layout.jiraCol()));
        }

        if (layout.blockedByCol() >= cells.size()) {
            return Optional.empty();
        }

        var blockedBy = parseStoryList(
                cells.get(layout.blockedByCol()));

        return Optional.of(
                new DependencyMatrixRow(
                        storyId, title, jiraKey, blockedBy));
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

    static Optional<String> parseJiraKey(String cell) {
        var trimmed = cell.trim();
        if (EMPTY_MARKERS.contains(trimmed)
                || trimmed.startsWith("<")) {
            return Optional.empty();
        }
        return Optional.of(trimmed);
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

    private record TableExtraction(
            List<String> headerCells,
            List<String> dataRows) {
    }

    record ColumnLayout(int jiraCol, int blockedByCol) {
    }
}
