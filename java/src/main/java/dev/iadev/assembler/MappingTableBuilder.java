package dev.iadev.assembler;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Builds the {@code .claude/ <-> .github/ <-> .codex/}
 * mapping table for README.md.
 *
 * <p>Shows how configuration files map across the three
 * platform directories, including a total count of
 * .github/ artifacts when that directory exists.</p>
 *
 * @see ReadmeTables
 * @see ReadmeUtils
 */
public final class MappingTableBuilder {

    MappingTableBuilder() {
        // package-private constructor
    }

    /**
     * Builds the mapping table.
     *
     * @param outputDir the .claude/ output directory
     * @return formatted mapping table with totals
     */
    String build(Path outputDir) {
        String[][] rows = buildMappingRows();
        List<String> lines = new ArrayList<>();
        lines.add("| .claude/ | .github/ | .codex/"
                + " | Notes |");
        lines.add("|----------|----------|---------|"
                + "-------|");
        for (String[] row : rows) {
            lines.add("| %s | %s | %s | %s |"
                    .formatted(row[0], row[1],
                            row[2], row[3]));
        }
        Path githubDir =
                SummaryTableBuilder.resolveGithubDir(
                        outputDir);
        int ghTotal = Files.exists(githubDir)
                ? ReadmeUtils.countGithubFiles(githubDir) : 0;
        if (ghTotal > 0) {
            lines.add("");
            lines.add(
                    "**Total .github/ artifacts: %d**"
                            .formatted(ghTotal));
        }
        return String.join("\n", lines);
    }

    private static String[][] buildMappingRows() {
        return new String[][]{
                {"Rules (`rules/*.md`)",
                        "Instructions (`instructions/"
                                + "*.instructions.md`)",
                        "Sections in `AGENTS.md`",
                        "Rules \u2192 consolidated"
                                + " sections"},
                {"Skills (`skills/*/SKILL.md`)",
                        "Skills (`skills/*/SKILL.md`)",
                        "Skills (`.agents/skills/`)",
                        "Same structure across"
                                + " platforms"},
                {"Agents (`agents/*.md`)",
                        "Agents (`agents/*.agent.md`)",
                        "Agent personas in `AGENTS.md`",
                        "Agents as section"},
                {"Hooks (`hooks/`)",
                        "Hooks (`hooks/*.json`)",
                        "Reference in `AGENTS.md`",
                        "Hooks influence"
                                + " approval_policy"},
                {"Settings (`settings*.json`)",
                        "N/A",
                        "`.codex/config.toml`",
                        "Permissions \u2192 approval"
                                + " policy"},
                {"N/A", "N/A",
                        "`AGENTS.md` (project root)",
                        "Codex project instructions"},
                {"N/A",
                        "Prompts (`prompts/"
                                + "*.prompt.md`)",
                        "N/A",
                        "GitHub Copilot prompt"
                                + " templates"},
                {"N/A",
                        "MCP (`copilot-mcp.json`)",
                        "N/A",
                        "GitHub Copilot MCP server"
                                + " configuration"},
                {"N/A",
                        "Global instructions"
                                + " (`copilot-instructions"
                                + ".md`)",
                        "N/A",
                        "Loaded in every Copilot"
                                + " session"},
        };
    }
}
