package dev.iadev.assembler;

import dev.iadev.domain.stack.LanguageCommandSet;
import dev.iadev.domain.stack.StackMapping;
import dev.iadev.model.ProjectConfig;
import dev.iadev.model.ProjectFoundation;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

/**
 * Builds markdown tables and sections for README.md.
 *
 * <p>Extracted from ReadmeAssembler to respect the 250-line
 * limit per class. Provides static methods for building
 * rules table, skills table, agents table, knowledge packs
 * table, hooks section, settings section, mapping table,
 * and generation summary.</p>
 *
 * @see ReadmeAssembler
 * @see ReadmeUtils
 */
public final class ReadmeTables {

    private ReadmeTables() {
        // utility class
    }

    /**
     * Builds markdown table of rules with number, file,
     * and scope columns.
     *
     * @param outputDir the .claude/ output directory
     * @return formatted markdown table
     */
    public static String buildRulesTable(Path outputDir) {
        Path rulesDir = outputDir.resolve("rules");
        if (!Files.exists(rulesDir)) {
            return "No rules configured.";
        }
        List<String> files = listMdFilesSorted(rulesDir);
        if (files.isEmpty()) {
            return "No rules configured.";
        }
        List<String> lines = new ArrayList<>();
        lines.add("| # | File | Scope |");
        lines.add("|---|------|-------|");
        for (String fname : files) {
            String num =
                    ReadmeUtils.extractRuleNumber(fname);
            String scope =
                    ReadmeUtils.extractRuleScope(fname);
            lines.add("| " + num + " | `" + fname
                    + "` | " + scope + " |");
        }
        return String.join("\n", lines);
    }

    /**
     * Builds markdown table of skills, excluding knowledge
     * packs.
     *
     * @param outputDir the .claude/ output directory
     * @return formatted markdown table
     */
    public static String buildSkillsTable(Path outputDir) {
        Path skillsDir = outputDir.resolve("skills");
        if (!Files.exists(skillsDir)) {
            return "No skills configured.";
        }
        List<String> dirs = listDirsSorted(skillsDir);
        List<String> rows = new ArrayList<>();
        for (String sname : dirs) {
            Path skillMd = skillsDir.resolve(sname)
                    .resolve("SKILL.md");
            if (!Files.exists(skillMd)) {
                continue;
            }
            if (ReadmeUtils.isKnowledgePack(skillMd)) {
                continue;
            }
            String desc = ReadmeUtils
                    .extractSkillDescription(skillMd);
            rows.add("| **" + sname + "** | `/" + sname
                    + "` | " + desc + " |");
        }
        if (rows.isEmpty()) {
            return "No skills configured.";
        }
        List<String> lines = new ArrayList<>();
        lines.add("| Skill | Path | Description |");
        lines.add("|-------|------|-------------|");
        lines.addAll(rows);
        return String.join("\n", lines);
    }

    /**
     * Builds markdown table of agents with name and file.
     *
     * @param outputDir the .claude/ output directory
     * @return formatted markdown table
     */
    public static String buildAgentsTable(Path outputDir) {
        Path agentsDir = outputDir.resolve("agents");
        if (!Files.exists(agentsDir)) {
            return "No agents configured.";
        }
        List<String> files = listMdFilesSorted(agentsDir);
        if (files.isEmpty()) {
            return "No agents configured.";
        }
        List<String> lines = new ArrayList<>();
        lines.add("| Agent | File |");
        lines.add("|-------|------|");
        for (String fname : files) {
            String aname = fname.replaceFirst("\\.md$", "");
            lines.add("| **" + aname + "** | `"
                    + fname + "` |");
        }
        return String.join("\n", lines);
    }

    /**
     * Builds markdown table of knowledge packs.
     *
     * @param outputDir the .claude/ output directory
     * @return formatted markdown table
     */
    public static String buildKnowledgePacksTable(
            Path outputDir) {
        Path skillsDir = outputDir.resolve("skills");
        if (!Files.exists(skillsDir)) {
            return "No knowledge packs configured.";
        }
        List<String> dirs = listDirsSorted(skillsDir);
        List<String> rows = new ArrayList<>();
        for (String sname : dirs) {
            Path skillMd = skillsDir.resolve(sname)
                    .resolve("SKILL.md");
            if (!Files.exists(skillMd)) {
                continue;
            }
            if (!ReadmeUtils.isKnowledgePack(skillMd)) {
                continue;
            }
            rows.add("| `" + sname
                    + "` | Referenced internally"
                    + " by agents |");
        }
        if (rows.isEmpty()) {
            return "No knowledge packs configured.";
        }
        String header = "| Pack | Usage |\n|------|-------|";
        return header + "\n" + String.join("\n", rows);
    }

    /**
     * Builds hooks documentation section.
     *
     * @param config the project configuration
     * @return formatted hooks section
     */
    public static String buildReadmeHooksSection(
            ProjectConfig config) {
        String key = StackMapping.getHookTemplateKey(
                config.language().name(),
                config.framework().buildTool());
        if (key.isEmpty()) {
            return "No hooks configured.";
        }
        String langKey = config.language().name()
                + "-" + config.framework().buildTool();
        LanguageCommandSet commands =
                StackMapping.LANGUAGE_COMMANDS.get(langKey);
        String ext = commands != null
                ? commands.fileExtension() : "";
        String compileCmd = commands != null
                ? commands.compileCmd() : "";
        return "### Post-Compile Check\n"
                + "\n"
                + "- **Event:** `PostToolUse`"
                + " (after `Write` or `Edit`)\n"
                + "- **Script:** `.claude/hooks/"
                + "post-compile-check.sh`\n"
                + "- **Behavior:** When a `" + ext
                + "` file is modified,"
                + " runs `" + compileCmd
                + "` automatically\n"
                + "- **Purpose:** Catch compilation errors"
                + " immediately after file changes";
    }

    /**
     * Builds static settings section content.
     *
     * @return formatted settings section
     */
    public static String buildSettingsSection() {
        return "### settings.json\n"
                + "\n"
                + "Permissions are configured in"
                + " `settings.json`"
                + " under `permissions.allow`.\n"
                + "This controls which Bash commands"
                + " Claude Code"
                + " can run without asking.\n"
                + "\n"
                + "### settings.local.json\n"
                + "\n"
                + "Local overrides (gitignored)."
                + " Use for personal"
                + " preferences or team-specific tools.\n"
                + "\n"
                + "See the files directly for current"
                + " configuration.";
    }

    /**
     * Builds the {@code .claude/ <-> .github/ <-> .codex/}
     * mapping table.
     *
     * @param outputDir the .claude/ output directory
     * @return formatted mapping table with totals
     */
    public static String buildMappingTable(Path outputDir) {
        String[][] rows = buildMappingRows();
        List<String> lines = new ArrayList<>();
        lines.add("| .claude/ | .github/ | .codex/"
                + " | Notes |");
        lines.add("|----------|----------|---------|"
                + "-------|");
        for (String[] row : rows) {
            lines.add("| " + row[0] + " | " + row[1]
                    + " | " + row[2] + " | "
                    + row[3] + " |");
        }
        Path githubDir = resolveGithubDir(outputDir);
        int ghTotal = Files.exists(githubDir)
                ? ReadmeUtils.countGithubFiles(githubDir) : 0;
        if (ghTotal > 0) {
            lines.add("");
            lines.add("**Total .github/ artifacts: "
                    + ghTotal + "**");
        }
        return String.join("\n", lines);
    }

    /**
     * Builds the generation summary table with component
     * counts.
     *
     * @param outputDir the .claude/ output directory
     * @param config    the project configuration
     * @return formatted generation summary
     */
    public static String buildGenerationSummary(
            Path outputDir, ProjectConfig config) {
        Path githubDir = resolveGithubDir(outputDir);
        Object[][] rows =
                buildSummaryRows(outputDir, githubDir);
        List<String> lines = new ArrayList<>();
        lines.add("| Component | Count |");
        lines.add("|-----------|-------|");
        for (Object[] row : rows) {
            lines.add("| " + row[0] + " | " + row[1]
                    + " |");
        }
        lines.add("");
        String ver = ProjectFoundation.DEFAULT.version();
        lines.add("Generated by `ia-dev-env v"
                + ver + "`.");
        return String.join("\n", lines);
    }

    private static Path resolveGithubDir(Path outputDir) {
        return outputDir.getParent().resolve(".github");
    }

    private static Path resolveCodexDir(Path outputDir) {
        return outputDir.getParent().resolve(".codex");
    }

    private static Path resolveAgentsDir(Path outputDir) {
        return outputDir.getParent().resolve(".agents");
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

    private static Object[][] buildSummaryRows(
            Path outputDir, Path githubDir) {
        int kps = ReadmeUtils
                .countKnowledgePacks(outputDir);
        int ghGlobal = Files.exists(
                githubDir.resolve(
                        "copilot-instructions.md")) ? 1 : 0;
        int ghMcp = Files.exists(
                githubDir.resolve(
                        "copilot-mcp.json")) ? 1 : 0;
        Path codexDir = resolveCodexDir(outputDir);
        int codexCount =
                ReadmeUtils.countCodexFiles(codexDir);
        Path agentsDir = resolveAgentsDir(outputDir);
        int agentsCount =
                ReadmeUtils.countCodexAgentsFiles(agentsDir);
        Path rootDir = outputDir.getParent();
        int agentsMdCount = Files.exists(
                rootDir.resolve("AGENTS.md")) ? 1 : 0;

        return new Object[][]{
                {"Rules (.claude)",
                        ReadmeUtils.countRules(outputDir)},
                {"Skills (.claude)",
                        ReadmeUtils.countSkills(outputDir)
                                - kps},
                {"Knowledge Packs (.claude)", kps},
                {"Agents (.claude)",
                        ReadmeUtils.countAgents(outputDir)},
                {"Hooks (.claude)",
                        ReadmeUtils.countHooks(outputDir)},
                {"Settings (.claude)",
                        ReadmeUtils
                                .countSettings(outputDir)},
                {"Instructions (.github)",
                        ReadmeUtils.countGithubComponent(
                                githubDir, "instructions")
                                + ghGlobal},
                {"Skills (.github)",
                        ReadmeUtils.countGithubSkills(
                                githubDir)},
                {"Agents (.github)",
                        ReadmeUtils.countGithubComponent(
                                githubDir, "agents")},
                {"Prompts (.github)",
                        ReadmeUtils.countGithubComponent(
                                githubDir, "prompts")},
                {"Hooks (.github)",
                        ReadmeUtils.countGithubComponent(
                                githubDir, "hooks")},
                {"MCP (.github)", ghMcp},
                {"AGENTS.md (root)", agentsMdCount},
                {"Codex (.codex)", codexCount},
                {"Skills (.agents)", agentsCount},
        };
    }

    private static List<String> listMdFilesSorted(Path dir) {
        try (Stream<Path> entries = Files.list(dir)) {
            return entries
                    .filter(p -> p.getFileName().toString()
                            .endsWith(".md"))
                    .map(p -> p.getFileName().toString())
                    .sorted()
                    .toList();
        } catch (IOException e) {
            throw new UncheckedIOException(
                    "Failed to list directory: " + dir, e);
        }
    }

    private static List<String> listDirsSorted(Path dir) {
        try (Stream<Path> entries = Files.list(dir)) {
            return entries
                    .filter(Files::isDirectory)
                    .map(p -> p.getFileName().toString())
                    .sorted()
                    .toList();
        } catch (IOException e) {
            throw new UncheckedIOException(
                    "Failed to list directory: " + dir, e);
        }
    }
}
