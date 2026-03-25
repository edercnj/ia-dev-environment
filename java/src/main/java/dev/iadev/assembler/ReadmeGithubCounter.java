package dev.iadev.assembler;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;

/**
 * Counting utilities for GitHub and Codex directory
 * artifacts used in README generation.
 *
 * <p>Extracted from {@link ReadmeUtils} to keep both
 * classes under 250 lines per RULE-004.</p>
 *
 * @see ReadmeUtils
 * @see ReadmeAssembler
 */
public final class ReadmeGithubCounter {

    private ReadmeGithubCounter() {
        // utility class
    }

    /**
     * Recursively counts all files under a .github/
     * directory.
     *
     * @param githubDir the .github/ directory
     * @return total file count, 0 if dir missing
     */
    public static int countGithubFiles(Path githubDir) {
        if (!Files.exists(githubDir)) {
            return 0;
        }
        return ReadmeUtils.countFilesRecursive(githubDir);
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
