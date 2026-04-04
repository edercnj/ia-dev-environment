package dev.iadev.cli;

/**
 * Categorizes file paths into display categories based on
 * path prefixes.
 *
 * <p>Extracted from {@link CliDisplay} to keep both
 * classes under 250 lines per RULE-004.</p>
 *
 * @see CliDisplay
 */
final class FileCategorizer {

    private FileCategorizer() {
        // utility class
    }

    /**
     * Normalizes path separators to forward slashes.
     *
     * @param path the file path to normalize
     * @return the normalized path
     */
    static String normalizePath(String path) {
        return path.replace('\\', '/');
    }

    /**
     * Categorizes a normalized file path into a display
     * category.
     *
     * @param path the normalized file path
     * @return the category name
     */
    static String categorize(String path) {
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
        if (path.startsWith("steering/")) {
            return "Steering";
        }
        if (path.startsWith("specs/")) {
            return "Specs";
        }
        if (path.startsWith("results/")) {
            return "Results";
        }
        if (path.startsWith("contracts/")) {
            return "Contracts";
        }
        if (path.startsWith("adr/")) {
            return "ADR";
        }
        if (path.startsWith("plans/")) {
            return "Plans";
        }
        if ("CLAUDE.md".equals(path)
                || "README.md".equals(path)
                || "AGENTS.md".equals(path)) {
            return "Root Files";
        }
        return "Other";
    }
}
