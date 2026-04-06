package dev.iadev.application.assembler;

import dev.iadev.domain.model.Platform;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

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
     * Builds the mapping table (all platforms).
     *
     * @param outputDir the .claude/ output directory
     * @return formatted mapping table with totals
     */
    String build(Path outputDir) {
        return build(outputDir, Set.of());
    }

    /**
     * Builds a platform-filtered mapping table.
     *
     * <p>Returns an empty string when only one platform
     * is active (cross-platform mapping is irrelevant).
     * When platforms is empty or contains all
     * user-selectable platforms, the full table is
     * shown.</p>
     *
     * @param outputDir the .claude/ output directory
     * @param platforms the active platforms (empty = all)
     * @return formatted mapping table, or empty string
     */
    String build(Path outputDir, Set<Platform> platforms) {
        if (isSinglePlatform(platforms)) {
            return "";
        }
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
                ? ReadmeUtils.countGithubFiles(githubDir)
                : 0;
        if (ghTotal > 0) {
            lines.add("");
            lines.add(
                    "**Total .github/ artifacts: %d**"
                            .formatted(ghTotal));
        }
        return String.join("\n", lines);
    }

    private static boolean isSinglePlatform(
            Set<Platform> platforms) {
        if (platforms.isEmpty()) {
            return false;
        }
        if (platforms.containsAll(
                Platform.allUserSelectable())) {
            return false;
        }
        Set<Platform> userOnly = new java.util.HashSet<>(
                platforms);
        userOnly.remove(Platform.SHARED);
        return userOnly.size() <= 1;
    }

    private static String[][] buildMappingRows() {
        var core = coreArtifactRows();
        var extra = additionalArtifactRows();
        var result = new String[core.length
                + extra.length][];
        System.arraycopy(core, 0, result, 0,
                core.length);
        System.arraycopy(extra, 0, result,
                core.length, extra.length);
        return result;
    }

    private static String[][] coreArtifactRows() {
        return new String[][]{
                {"Rules (`rules/*.md`)",
                        "Instructions (`instructions/"
                                + "*.instructions.md`)",
                        "Sections in `AGENTS.md`",
                        "Rules \u2192 consolidated"
                                + " sections"},
                {"Skills (`skills/*/SKILL.md`)",
                        "Skills (`skills/*/SKILL.md`)",
                        "Skills (`.agents/skills/` + "
                                + "`.codex/skills/`)",
                        "Dual output with identical content"},
                {"Agents (`agents/*.md`)",
                        "Agents (`agents/*.agent.md`)",
                        "Sections (`[agents.*]`) in"
                                + " `config.toml`",
                        "Agents represented as TOML"
                                + " sections"},
                {"Hooks (`hooks/`)",
                        "Hooks (`hooks/*.json`)",
                        "Reference in `AGENTS.md`",
                        "Hooks influence"
                                + " approval_policy"},
                {"Settings (`settings*.json`)",
                        "N/A",
                        "`.codex/config.toml` +"
                                + " `.codex/requirements.toml`",
                        "Runtime and enforced policies"},
        };
    }

    private static String[][] additionalArtifactRows() {
        return new String[][]{
                {"N/A", "N/A",
                        "`AGENTS.md` +"
                                + " `AGENTS.override.md` (root)",
                        "Base instructions + local override"},
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
