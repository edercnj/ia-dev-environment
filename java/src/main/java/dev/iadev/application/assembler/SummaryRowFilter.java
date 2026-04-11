package dev.iadev.application.assembler;

import dev.iadev.domain.model.Platform;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Filters generation summary rows by platform.
 *
 * <p>Extracted from {@link SummaryTableBuilder} to keep
 * both classes under 250 lines per RULE-004.</p>
 *
 * @see SummaryTableBuilder
 * @see Platform
 */
final class SummaryRowFilter {

    private SummaryRowFilter() {
        // utility class
    }

    /**
     * Filters summary rows to include only those matching
     * the requested platforms.
     *
     * <p>When platforms is empty or contains all
     * user-selectable platforms, returns the original
     * rows unchanged.</p>
     *
     * @param rows      the full set of summary rows
     * @param platforms the active platforms (empty = all)
     * @return filtered rows
     */
    static Object[][] filter(
            Object[][] rows, Set<Platform> platforms) {
        if (shouldShowAll(platforms)) {
            return rows;
        }
        List<Object[]> filtered = new ArrayList<>();
        for (Object[] row : rows) {
            String label = (String) row[0];
            if (matchesPlatform(label, platforms)) {
                filtered.add(row);
            }
        }
        return filtered.toArray(new Object[0][]);
    }

    private static boolean shouldShowAll(
            Set<Platform> platforms) {
        return platforms.isEmpty()
                || platforms.containsAll(
                        Platform.allUserSelectable());
    }

    private static boolean matchesPlatform(
            String label, Set<Platform> platforms) {
        if (label.contains("(.claude)")) {
            return platforms.contains(
                    Platform.CLAUDE_CODE);
        }
        if (label.contains("(.codex)")
                || label.contains("(.agents)")
                || label.contains("AGENTS.md")
                || label.contains("AGENTS.override.md")) {
            return platforms.contains(Platform.CODEX);
        }
        return true;
    }
}
