package dev.iadev.application.assembler;

import dev.iadev.domain.model.Platform;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Builds the {@code .claude/ <-> .codex/} mapping table
 * for README.md.
 *
 * <p>Shows how configuration files map across the
 * platform directories.</p>
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
        lines.add("| .claude/ | .codex/ | Notes |");
        lines.add("|----------|---------|-------|");
        for (String[] row : rows) {
            lines.add("| %s | %s | %s |"
                    .formatted(row[0], row[1], row[2]));
        }
        lines.add("");
        lines.add("> Generated only when the "
                + "corresponding platform is selected "
                + "via `--platform`.");
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
        return new String[][]{
                {"Rules (`rules/*.md`)",
                        "Sections in `AGENTS.md`",
                        "Rules \u2192 consolidated"
                                + " sections"},
                {"Skills (`skills/*/SKILL.md`)",
                        "Skills (`.agents/skills/` + "
                                + "`.codex/skills/`)",
                        "Dual output with identical content"},
                {"Agents (`agents/*.md`)",
                        "Sections (`[agents.*]`) in"
                                + " `config.toml`",
                        "Agents represented as TOML"
                                + " sections"},
                {"Hooks (`hooks/`)",
                        "Reference in `AGENTS.md`",
                        "Hooks influence"
                                + " approval_policy"},
                {"Settings (`settings*.json`)",
                        "`.codex/config.toml` +"
                                + " `.codex/requirements.toml`",
                        "Runtime and enforced policies"},
                {"N/A",
                        "`AGENTS.md` +"
                                + " `AGENTS.override.md` (root)",
                        "Base instructions + local override"},
        };
    }
}
