package dev.iadev.assembler;

import dev.iadev.domain.stack.LanguageCommandSet;
import dev.iadev.domain.stack.StackMapping;
import dev.iadev.domain.model.ProjectConfig;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

/**
 * Builds markdown tables for skills, agents, knowledge
 * packs, rules, and hooks sections in README.md.
 *
 * <p>Each method scans the output directory for the relevant
 * artifacts and formats them into markdown tables with
 * appropriate headers and data rows.</p>
 *
 * @see ReadmeTables
 * @see ReadmeUtils
 */
public final class SkillsTableBuilder {

    SkillsTableBuilder() {
        // package-private constructor
    }

    /**
     * Builds markdown table of rules with number, file,
     * and scope columns.
     *
     * @param outputDir the .claude/ output directory
     * @return formatted markdown table
     */
    String buildRulesTable(Path outputDir) {
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
            lines.add("| %s | `%s` | %s |"
                    .formatted(num, fname, scope));
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
    String buildSkillsTable(Path outputDir) {
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
            rows.add("| **%s** | `/%s` | %s |"
                    .formatted(sname, sname, desc));
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
    String buildAgentsTable(Path outputDir) {
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
            lines.add("| **%s** | `%s` |"
                    .formatted(aname, fname));
        }
        return String.join("\n", lines);
    }

    /**
     * Builds markdown table of knowledge packs.
     *
     * @param outputDir the .claude/ output directory
     * @return formatted markdown table
     */
    String buildKnowledgePacksTable(Path outputDir) {
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
            rows.add(
                    "| `%s` | Referenced internally by agents |"
                            .formatted(sname));
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
    String buildReadmeHooksSection(ProjectConfig config) {
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
                    "Failed to list directory: %s"
                            .formatted(dir), e);
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
                    "Failed to list directory: %s"
                            .formatted(dir), e);
        }
    }
}
