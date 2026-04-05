package dev.iadev.domain.qualitygate;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Parses data contract fields and dependency references
 * from story markdown tables.
 */
final class MarkdownTableParser {

    private static final Set<String> GENERIC_TYPES = Set.of(
            "data", "object", "any", "dynamic", "var");
    static final Set<String> EMPTY_MARKERS =
            Set.of("-", "--", "\u2014", "");
    private static final int MIN_TABLE_CELLS = 3;
    private static final String DATA_CONTRACT_SECTION =
            "contratos de dados";
    private static final String DEPENDENCY_SECTION =
            "dependencias";

    private MarkdownTableParser() {
    }

    static List<DataContractField> extractDataContract(
            String content) {
        if (content == null || content.isBlank()) {
            return List.of();
        }
        var lines = content.split("\n");
        int i = SectionNavigator.skipToSection(
                lines, 0, DATA_CONTRACT_SECTION);
        if (i >= lines.length) {
            return List.of();
        }

        var fields = new ArrayList<DataContractField>();
        boolean inTable = false;
        boolean headerSkipped = false;

        while (i < lines.length) {
            var line = lines[i].trim();
            if (SectionNavigator.isNextSection(line)
                    && !SectionNavigator.sectionMatches(
                    line, DATA_CONTRACT_SECTION)) {
                break;
            }
            if (line.startsWith("|")) {
                if (!inTable) {
                    inTable = true;
                    i++;
                    continue;
                }
                if (!headerSkipped
                        && line.contains("---")) {
                    headerSkipped = true;
                    i++;
                    continue;
                }
                parseContractRow(line)
                        .ifPresent(fields::add);
            } else if (inTable && headerSkipped
                    && !line.isBlank()) {
                inTable = false;
                headerSkipped = false;
            }
            i++;
        }
        return List.copyOf(fields);
    }

    static List<String> extractDependencies(
            String content) {
        if (content == null || content.isBlank()) {
            return List.of();
        }
        var lines = content.split("\n");
        int i = SectionNavigator.skipToSection(
                lines, 0, DEPENDENCY_SECTION);
        if (i >= lines.length) {
            return List.of();
        }

        while (i < lines.length) {
            var line = lines[i].trim();
            if (line.startsWith("|")
                    && !line.contains("---")
                    && !line.toLowerCase(Locale.ROOT)
                    .contains("blocked")) {
                var cells = splitTableRow(line);
                if (!cells.isEmpty()) {
                    var first = cells.getFirst().trim();
                    if (!EMPTY_MARKERS.contains(first)
                            && !first.isBlank()) {
                        return parseDependencyIds(first);
                    }
                }
            }
            i++;
        }
        return List.of();
    }

    static boolean isGenericType(String type) {
        if (type == null || type.isBlank()) {
            return true;
        }
        return GENERIC_TYPES.contains(
                type.toLowerCase(Locale.ROOT).trim());
    }

    private static java.util.Optional<DataContractField>
    parseContractRow(String line) {
        var cells = splitTableRow(line);
        if (cells.size() < MIN_TABLE_CELLS) {
            return java.util.Optional.empty();
        }
        var name = stripMarkdown(cells.get(0).trim());
        var type = stripMarkdown(cells.get(1).trim());
        var mCell = cells.get(2).trim();
        boolean mandatory =
                "M".equalsIgnoreCase(mCell);

        if (name.isBlank()
                || EMPTY_MARKERS.contains(name)) {
            return java.util.Optional.empty();
        }

        return java.util.Optional.of(
                new DataContractField(
                        name, type, mandatory));
    }

    private static String stripMarkdown(String text) {
        return text.replaceAll("[`*_]", "")
                .replaceAll("<[^>]+>", "")
                .replaceAll("&lt;", "<")
                .replaceAll("&gt;", ">")
                .trim();
    }

    static List<String> splitTableRow(String line) {
        var parts = line.split("\\|");
        var cells = new ArrayList<String>();
        for (int i = 1; i < parts.length; i++) {
            var trimmed = parts[i].trim();
            if (!trimmed.isEmpty()
                    || i < parts.length - 1) {
                cells.add(trimmed);
            }
        }
        return cells;
    }

    private static List<String> parseDependencyIds(
            String cell) {
        var ids = new ArrayList<String>();
        for (var id : cell.split(",")) {
            var stripped = id.trim();
            if (!stripped.isEmpty()
                    && !EMPTY_MARKERS.contains(stripped)) {
                ids.add(stripped);
            }
        }
        return List.copyOf(ids);
    }
}
