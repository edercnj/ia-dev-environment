package dev.iadev.application.assembler;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;

/**
 * Counting utilities for README generation.
 *
 * <p>GitHub/Codex counting: {@link ReadmeGithubCounter}.
 * Metadata extraction: {@link ReadmeMetadata}.</p>
 *
 * @see ReadmeAssembler
 * @see ReadmeGithubCounter
 * @see ReadmeMetadata
 */
public final class ReadmeUtils {

    private ReadmeUtils() {
        // utility class
    }

    /** Counts .md files in outputDir/rules/. */
    public static int countRules(Path outputDir) {
        Path rulesDir = outputDir.resolve("rules");
        if (!Files.exists(rulesDir)) {
            return 0;
        }
        return countMdFiles(rulesDir);
    }

    /** Counts SKILL.md files in subdirs of skills/. */
    public static int countSkills(Path outputDir) {
        Path skillsDir = outputDir.resolve("skills");
        if (!Files.exists(skillsDir)) {
            return 0;
        }
        return countSkillMdFiles(skillsDir);
    }

    /** Counts .md files in outputDir/agents/. */
    public static int countAgents(Path outputDir) {
        Path agentsDir = outputDir.resolve("agents");
        if (!Files.exists(agentsDir)) {
            return 0;
        }
        return countMdFiles(agentsDir);
    }

    /** Counts knowledge packs in skills dir. */
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

    /** Counts entries in outputDir/hooks/. */
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

    /** Counts settings files (0, 1, or 2). */
    public static int countSettings(Path outputDir) {
        int count = 0;
        if (Files.exists(
                outputDir.resolve("settings.json"))) {
            count++;
        }
        if (Files.exists(
                outputDir.resolve(
                        "settings.local.json"))) {
            count++;
        }
        return count;
    }

    /** Delegates to {@link ReadmeGithubCounter}. */
    public static int countGithubFiles(Path githubDir) {
        return ReadmeGithubCounter
                .countGithubFiles(githubDir);
    }

    /** Delegates to {@link ReadmeGithubCounter}. */
    public static int countGithubComponent(
            Path githubDir, String component) {
        return ReadmeGithubCounter
                .countGithubComponent(
                        githubDir, component);
    }

    /** Delegates to {@link ReadmeGithubCounter}. */
    public static int countGithubSkills(Path githubDir) {
        return ReadmeGithubCounter
                .countGithubSkills(githubDir);
    }

    /** Delegates to {@link ReadmeGithubCounter}. */
    public static int countCodexFiles(Path codexDir) {
        return ReadmeGithubCounter
                .countCodexFiles(codexDir);
    }

    /** Delegates to {@link ReadmeGithubCounter}. */
    public static int countCodexAgentsFiles(
            Path agentsDir) {
        return ReadmeGithubCounter
                .countCodexAgentsFiles(agentsDir);
    }

    /** Delegates to {@link ReadmeMetadata}. */
    public static boolean isKnowledgePack(
            Path skillMdPath) {
        return ReadmeMetadata.isKnowledgePack(skillMdPath);
    }

    /** Delegates to {@link ReadmeMetadata}. */
    public static String extractRuleNumber(
            String filename) {
        return ReadmeMetadata.extractRuleNumber(filename);
    }

    /** Delegates to {@link ReadmeMetadata}. */
    public static String extractRuleScope(
            String filename) {
        return ReadmeMetadata.extractRuleScope(filename);
    }

    /** Delegates to {@link ReadmeMetadata}. */
    public static String extractSkillDescription(
            Path skillMdPath) {
        return ReadmeMetadata.extractSkillDescription(
                skillMdPath);
    }

    /** Recursively counts all files in a directory. */
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

    private static int countSkillMdFiles(
            Path skillsDir) {
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
