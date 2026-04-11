package dev.iadev.application.assembler;

import dev.iadev.domain.model.Platform;

import java.nio.file.Path;
import java.util.Set;

/**
 * Cross-platform mapping table builder.
 *
 * <p>Historically rendered a table documenting how
 * configuration maps between {@code .claude/} and
 * {@code .codex/}. With Codex target support removed,
 * the mapping is vacuous and this builder always returns
 * an empty string. Retained for README builder
 * compatibility until cross-platform support is
 * reintroduced.</p>
 *
 * @see ReadmeTables
 * @see ReadmeUtils
 */
public final class MappingTableBuilder {

    MappingTableBuilder() {
        // package-private constructor
    }

    /**
     * Builds the mapping table (all platforms).
     *
     * @param outputDir the .claude/ output directory
     * @return empty string — single-platform mapping is
     *         always empty
     */
    String build(Path outputDir) {
        return build(outputDir, Set.of());
    }

    /**
     * Builds a platform-filtered mapping table.
     *
     * <p>Returns an empty string in all cases since only a
     * single user-selectable platform exists; a
     * cross-platform mapping is not meaningful.</p>
     *
     * @param outputDir the .claude/ output directory
     * @param platforms the active platforms (ignored)
     * @return empty string
     */
    String build(Path outputDir, Set<Platform> platforms) {
        return "";
    }
}
