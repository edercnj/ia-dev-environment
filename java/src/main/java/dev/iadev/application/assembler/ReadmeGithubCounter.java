package dev.iadev.application.assembler;

import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Counting utilities for Codex directory artifacts used
 * in README generation.
 *
 * <p>Originally extracted from {@link ReadmeUtils} to
 * keep both classes under 250 lines per RULE-004.</p>
 *
 * @see ReadmeUtils
 * @see ReadmeAssembler
 */
public final class ReadmeGithubCounter {

    private ReadmeGithubCounter() {
        // utility class
    }

    /**
     * Counts files in a {@code .codex/} directory.
     *
     * @param codexDir the .codex/ directory
     * @return count of files, 0 if dir missing
     */
    public static int countCodexFiles(Path codexDir) {
        if (!Files.exists(codexDir)) {
            return 0;
        }
        return ReadmeUtils.countFilesRecursive(codexDir);
    }

    /**
     * Recursively counts all files in a {@code .agents/}
     * directory.
     *
     * @param agentsDir the .agents/ directory
     * @return total file count, 0 if dir missing
     */
    public static int countCodexAgentsFiles(
            Path agentsDir) {
        if (!Files.exists(agentsDir)) {
            return 0;
        }
        return ReadmeUtils.countFilesRecursive(agentsDir);
    }
}
