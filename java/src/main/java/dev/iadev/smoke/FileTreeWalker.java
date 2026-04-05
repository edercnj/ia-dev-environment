package dev.iadev.smoke;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

/**
 * File-system walking utilities for manifest generation.
 *
 * <p>Extracted from {@link ExpectedArtifactsGenerator} to
 * keep both classes under 250 lines per RULE-004.</p>
 *
 * @see ExpectedArtifactsGenerator
 */
final class FileTreeWalker {

    private FileTreeWalker() {
        // utility class
    }

    /**
     * Collects output metrics from a pipeline directory.
     *
     * @param outputDir the pipeline output directory
     * @return map of metric name to metric value
     * @throws IOException if file operations fail
     */
    static Map<String, Object> collectMetrics(
            Path outputDir) throws IOException {
        Map<String, Object> metrics = new LinkedHashMap<>();
        metrics.put("totalFiles", countFiles(outputDir));
        metrics.put("directories",
                listDirectories(outputDir));
        metrics.put("categories",
                categorizeFiles(outputDir));
        return metrics;
    }

    /**
     * Counts all regular files recursively.
     *
     * @param dir the directory to walk
     * @return number of regular files
     * @throws IOException if file operations fail
     */
    static int countFiles(Path dir) throws IOException {
        int[] count = {0};
        Files.walkFileTree(dir, new SimpleFileVisitor<>() {
            @Override
            public FileVisitResult visitFile(
                    Path file,
                    BasicFileAttributes attrs) {
                count[0]++;
                return FileVisitResult.CONTINUE;
            }
        });
        return count[0];
    }

    /**
     * Lists all subdirectories relative to the base.
     *
     * @param dir the base directory
     * @return sorted list of relative directory paths
     * @throws IOException if file operations fail
     */
    static List<String> listDirectories(Path dir)
            throws IOException {
        Set<String> dirs = new TreeSet<>();
        Files.walkFileTree(dir, new SimpleFileVisitor<>() {
            @Override
            public FileVisitResult preVisitDirectory(
                    Path d,
                    BasicFileAttributes attrs) {
                if (!d.equals(dir)) {
                    dirs.add(dir.relativize(d)
                            .toString()
                            .replace('\\', '/'));
                }
                return FileVisitResult.CONTINUE;
            }
        });
        return List.copyOf(dirs);
    }

    /**
     * Categorizes files by directory structure.
     *
     * @param dir the base directory
     * @return map of category name to file count
     * @throws IOException if file operations fail
     */
    static Map<String, Integer> categorizeFiles(Path dir)
            throws IOException {
        Map<String, Integer> categories =
                new LinkedHashMap<>();

        countCategory(categories, dir,
                "codex-skills", ".agents/skills");
        countCategory(categories, dir,
                "claude-rules", ".claude/rules");
        countCategory(categories, dir,
                "claude-skills", ".claude/skills");
        countCategory(categories, dir,
                "claude-agents", ".claude/agents");
        countCategory(categories, dir,
                "claude-hooks", ".claude/hooks");
        countCategory(categories, dir,
                "claude-settings", ".claude", true);
        countCategory(categories, dir,
                "codex-config", ".codex");
        countCategory(categories, dir,
                "github-instructions",
                ".github/instructions");
        countCategory(categories, dir,
                "github-skills", ".github/skills");
        countCategory(categories, dir,
                "github-agents", ".github/agents");
        countCategory(categories, dir,
                "github-prompts", ".github/prompts");
        countCategory(categories, dir,
                "github-hooks", ".github/hooks");
        countCategory(categories, dir,
                "github-issue-templates",
                ".github/ISSUE_TEMPLATE");
        countCategory(categories, dir,
                "github-top", ".github", true);
        countCategory(categories, dir,
                "steering", "steering");
        countCategory(categories, dir,
                "adr", "adr");
        countCategory(categories, dir,
                "contracts", "contracts");
        countCategory(categories, dir,
                "results", "results");
        countCategory(categories, dir,
                "specs", "specs");
        countCategory(categories, dir,
                "plans", "plans");
        countCategory(categories, dir, "k8s", "k8s");
        countCategory(categories, dir,
                "tests", "tests");
        countCategory(categories, dir,
                "root-files", "", true);

        categories.entrySet()
                .removeIf(e -> e.getValue() == 0);

        return categories;
    }

    private static void countCategory(
            Map<String, Integer> categories,
            Path baseDir,
            String categoryName,
            String subPath) throws IOException {
        countCategory(categories, baseDir,
                categoryName, subPath, false);
    }

    private static void countCategory(
            Map<String, Integer> categories,
            Path baseDir,
            String categoryName,
            String subPath,
            boolean topLevelOnly) throws IOException {
        Path targetDir = subPath.isEmpty()
                ? baseDir
                : baseDir.resolve(subPath);
        if (!Files.isDirectory(targetDir)) {
            return;
        }

        int count;
        if (topLevelOnly) {
            count = countTopLevelFiles(targetDir);
        } else {
            count = countFiles(targetDir);
        }
        categories.put(categoryName, count);
    }

    private static int countTopLevelFiles(Path dir)
            throws IOException {
        int count = 0;
        try (var stream = Files.list(dir)) {
            for (Path p : stream.toList()) {
                if (Files.isRegularFile(p)) {
                    count++;
                }
            }
        }
        return count;
    }

    /**
     * Recursively deletes a directory tree.
     *
     * @param dir the directory to delete
     * @throws IOException if file operations fail
     */
    static void deleteTree(Path dir) throws IOException {
        if (!Files.exists(dir)) {
            return;
        }
        Files.walkFileTree(dir, new SimpleFileVisitor<>() {
            @Override
            public FileVisitResult visitFile(
                    Path file,
                    BasicFileAttributes attrs)
                    throws IOException {
                Files.delete(file);
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult postVisitDirectory(
                    Path d, IOException exc)
                    throws IOException {
                Files.delete(d);
                return FileVisitResult.CONTINUE;
            }
        });
    }
}
