package dev.iadev.application.assembler;

import dev.iadev.domain.model.Platform;

import java.util.Set;

/**
 * Filters generation summary rows by platform.
 *
 * <p>Extracted from {@link SummaryTableBuilder} to keep
 * both classes under 250 lines per RULE-004.</p>
 *
 * <p>After the Codex removal (EPIC-0034) only a single
 * user-selectable platform ({@link Platform#CLAUDE_CODE})
 * remains, so any non-empty platform set is equivalent to
 * "show all rows" and the filter becomes a passthrough.
 * The class is retained as a seam for future multi-platform
 * support and to keep {@link SummaryTableBuilder} unchanged.</p>
 *
 * @see SummaryTableBuilder
 * @see Platform
 */
final class SummaryRowFilter {

    private SummaryRowFilter() {
        // utility class
    }

    /**
     * Returns the input rows unchanged.
     *
     * @param rows      the full set of summary rows
     * @param platforms the active platforms (ignored)
     * @return the original rows
     */
    static Object[][] filter(
            Object[][] rows, Set<Platform> platforms) {
        return rows;
    }
}
