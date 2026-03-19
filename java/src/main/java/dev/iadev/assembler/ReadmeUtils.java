package dev.iadev.assembler;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;

/**
 * Counting and extraction utilities for README generation.
 *
 * <p>Provides static methods for counting artifacts (rules,
 * skills, agents, knowledge packs, hooks, settings) and
 * extracting metadata from file names and SKILL.md content.
 * All methods are null-safe and return 0 or empty strings
 * when directories are missing.</p>
 *
 * <p>Extracted from ReadmeAssembler to break circular
 * dependencies between readme-assembler and readme-tables.
 * Both classes import from here.</p>
 *
 * @see ReadmeAssembler
 * @see ReadmeTables
 */
public final class ReadmeUtils {

    private ReadmeUtils() {
        // utility class
    }

    /**
     * Counts {@code .md} files in {@code outputDir/rules/}.
     *
     * @param outputDir the .claude/ output directory
     * @return count of rule files, 0 if dir missing
     */
    public static int countRules(Path outputDir) {
        Path rulesDir = outputDir.resolve("rules");
        if (!Files.exists(rulesDir)) {
            return 0;
        }
        return countMdFiles(rulesDir);
    }

    /**
     * Counts {@code SKILL.md} files in subdirs of
     * {@code outputDir/skills/}.
     *
     * @param outputDir the .claude/ output directory
     * @return count of skill directories with SKILL.md
     */
    public static int countSkills(Path outputDir) {
        Path skillsDir = outputDir.resolve("skills");
        if (!Files.exists(skillsDir)) {
            return 0;
        }
        return countSkillMdFiles(skillsDir);
    }

    /**
     * Counts {@code .md} files in {@code outputDir/agents/}.
     *
     * @param outputDir the .claude/ output directory
     * @return count of agent files, 0 if dir missing
     */
    public static int countAgents(Path outputDir) {
        Path agentsDir = outputDir.resolve("agents");
        if (!Files.exists(agentsDir)) {
            return 0;
        }
        return countMdFiles(agentsDir);
    }

    /**
     * Counts skills where {@link #isKnowledgePack} is true.
     *
     * @param outputDir the .claude/ output directory
     * @return count of knowledge packs
     */
    public static int countKnowledgePacks(Path outputDir) {
        Path skillsDir = outputDir.resolve("skills");
        if (!Files.exists(skillsDir)) {
            return 0;
        }
        try (Stream<Path> dirs = Files.list(skillsDir)) {
            return (int) dirs
                    .filter(Files::isDirectory)
                    .filter(d -> {
                        Path skillMd =
                                d.resolve("SKILL.md");
                        return Files.exists(skillMd)
                                && isKnowledgePack(skillMd);
                    })
                    .count();
        } catch (IOException e) {
            throw new UncheckedIOException(
                    "Failed to list skills", e);
        }
    }

    /**
     * Counts entries in {@code outputDir/hooks/}.
     *
     * @param outputDir the .claude/ output directory
     * @return count of hook files, 0 if dir missing
     */
    public static int countHooks(Path outputDir) {
        Path hooksDir = outputDir.resolve("hooks");
        if (!Files.exists(hooksDir)) {
            return 0;
        }
        try (Stream<Path> entries = Files.list(hooksDir)) {
            return (int) entries.count();
        } catch (IOException e) {
            throw new UncheckedIOException(
                    "Failed to list hooks", e);
        }
    }

    /**
     * Counts settings files (settings.json and
     * settings.local.json).
     *
     * @param outputDir the .claude/ output directory
     * @return count of settings files (0, 1, or 2)
     */
    public static int countSettings(Path outputDir) {
        int count = 0;
        if (Files.exists(
                outputDir.resolve("settings.json"))) {
            count++;
        }
        if (Files.exists(
                outputDir.resolve("settings.local.json"))) {
            count++;
        }
        return count;
    }

    /**
     * Recursively counts all files under a .github/ directory.
     *
     * @param githubDir the .github/ directory
     * @return total file count, 0 if dir missing
     */
    public static int countGithubFiles(Path githubDir) {
        if (!Files.exists(githubDir)) {
            return 0;
        }
        return countFilesRecursive(githubDir);
    }

    /**
     * Counts files directly under
     * {@code githubDir/{component}/}.
     *
     * @param githubDir the .github/ directory
     * @param component the component subdirectory name
     * @return count of files in the component dir
     */
    public static int countGithubComponent(
            Path githubDir, String component) {
        Path compDir = githubDir.resolve(component);
        if (!Files.exists(compDir)) {
            return 0;
        }
        try (Stream<Path> entries = Files.list(compDir)) {
            return (int) entries
                    .filter(Files::isRegularFile)
                    .count();
        } catch (IOException e) {
            throw new UncheckedIOException(
                    "Failed to list component: "
                            + component, e);
        }
    }

    /**
     * Counts {@code SKILL.md} in {@code githubDir/skills/}
     * subdirs.
     *
     * @param githubDir the .github/ directory
     * @return count of GitHub skill directories
     */
    public static int countGithubSkills(Path githubDir) {
        Path skillsDir = githubDir.resolve("skills");
        if (!Files.exists(skillsDir)) {
            return 0;
        }
        return countSkillMdFiles(skillsDir);
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
        try (Stream<Path> entries = Files.list(codexDir)) {
            return (int) entries
                    .filter(Files::isRegularFile)
                    .count();
        } catch (IOException e) {
            throw new UncheckedIOException(
                    "Failed to list codex dir", e);
        }
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
        return countFilesRecursive(agentsDir);
    }

    /**
     * Returns true if the SKILL.md flags a knowledge pack.
     *
     * <p>Checks for {@code user-invocable: false} anywhere
     * in the content, or a line starting with
     * {@code # Knowledge Pack}.</p>
     *
     * @param skillMdPath path to SKILL.md
     * @return true if it is a knowledge pack
     */
    public static boolean isKnowledgePack(Path skillMdPath) {
        try {
            String text = Files.readString(
                    skillMdPath, StandardCharsets.UTF_8);
            if (text.contains("user-invocable: false")) {
                return true;
            }
            for (String line : text.split("\n")) {
                if (line.startsWith("# Knowledge Pack")) {
                    return true;
                }
            }
            return false;
        } catch (IOException e) {
            throw new UncheckedIOException(
                    "Failed to read SKILL.md: "
                            + skillMdPath, e);
        }
    }

    /**
     * Extracts leading digits from a rule filename.
     *
     * @param filename the rule filename
     * @return the leading digits, or empty string
     */
    public static String extractRuleNumber(String filename) {
        var matcher = java.util.regex.Pattern
                .compile("^(\\d+)")
                .matcher(filename);
        return matcher.find() ? matcher.group(1) : "";
    }

    /**
     * Strips leading number+hyphen and {@code .md} extension,
     * then replaces hyphens with spaces.
     *
     * @param filename the rule filename
     * @return the extracted scope
     */
    public static String extractRuleScope(String filename) {
        String name = filename.replaceFirst("^\\d+-", "");
        name = name.replaceFirst("\\.md$", "");
        return name.replace('-', ' ');
    }

    /**
     * Reads SKILL.md and extracts the {@code description:}
     * value from frontmatter.
     *
     * @param skillMdPath path to SKILL.md
     * @return the description, or empty string
     */
    public static String extractSkillDescription(
            Path skillMdPath) {
        try {
            String text = Files.readString(
                    skillMdPath, StandardCharsets.UTF_8);
            for (String line : text.split("\n")) {
                if (line.startsWith("description:")) {
                    String[] parts = line.split(":", 2);
                    String desc = parts.length > 1
                            ? parts[1].trim() : "";
                    return desc
                            .replaceAll("^[\"']|[\"']$", "");
                }
            }
            return "";
        } catch (IOException e) {
            throw new UncheckedIOException(
                    "Failed to read SKILL.md: "
                            + skillMdPath, e);
        }
    }

    /**
     * Recursively counts all files in a directory tree.
     *
     * @param dir the directory to walk
     * @return total file count
     */
    static int countFilesRecursive(Path dir) {
        try (Stream<Path> walk = Files.walk(dir)) {
            return (int) walk
                    .filter(Files::isRegularFile)
                    .count();
        } catch (IOException e) {
            throw new UncheckedIOException(
                    "Failed to walk directory: " + dir, e);
        }
    }

    private static int countMdFiles(Path dir) {
        try (Stream<Path> entries = Files.list(dir)) {
            return (int) entries
                    .filter(p -> p.toString()
                            .endsWith(".md"))
                    .count();
        } catch (IOException e) {
            throw new UncheckedIOException(
                    "Failed to list directory: " + dir, e);
        }
    }

    private static int countSkillMdFiles(Path skillsDir) {
        try (Stream<Path> dirs = Files.list(skillsDir)) {
            return (int) dirs
                    .filter(Files::isDirectory)
                    .filter(d -> Files.exists(
                            d.resolve("SKILL.md")))
                    .count();
        } catch (IOException e) {
            throw new UncheckedIOException(
                    "Failed to list skills dir", e);
        }
    }
}
