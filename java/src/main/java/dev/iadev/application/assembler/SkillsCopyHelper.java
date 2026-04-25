package dev.iadev.application.assembler;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;

/**
 * Helper methods for skill directory traversal during
 * skills assembly.
 *
 * <p>EPIC-0051 (ADR-0013): the knowledge-pack copy methods
 * ({@code copyKnowledgePack}, {@code copyStackPatterns},
 * {@code copyInfraPatterns}) were removed. Knowledge packs
 * are now assembled by {@link KnowledgeAssembler} into
 * {@code .claude/knowledge/} — {@code SkillsAssembler}
 * emits only invocable skills.</p>
 *
 * @see SkillsAssembler
 * @see KnowledgeAssembler
 */
final class SkillsCopyHelper {

    private static final String SKILL_MD = "SKILL.md";

    private SkillsCopyHelper() {
        // utility class
    }

    /**
     * Copies non-SKILL.md items from source to dest.
     *
     * @param src  the source directory
     * @param dest the destination directory
     */
    static void copyNonSkillItems(
            Path src, Path dest) {
        List<Path> entries = listEntriesSorted(src);
        for (Path entry : entries) {
            String name = entry.getFileName().toString();
            if (SKILL_MD.equals(name)) {
                continue;
            }
            Path target = dest.resolve(name);
            if (Files.isDirectory(entry)) {
                if (Files.exists(target)
                        && Files.isDirectory(target)) {
                    mergeDirectory(entry, target);
                } else if (!Files.exists(target)) {
                    CopyHelpers.copyDirectory(
                            entry, target);
                }
            } else if (!Files.exists(target)) {
                CopyHelpers.copyStaticFile(entry, target);
            }
        }
    }

    /** Merges src into existing dest, skipping existing files. */
    private static void mergeDirectory(
            Path src, Path dest) {
        for (Path entry : listEntriesSorted(src)) {
            Path target = dest.resolve(
                    entry.getFileName().toString());
            if (Files.isDirectory(entry)) {
                if (Files.exists(target)) {
                    mergeDirectory(entry, target);
                } else {
                    CopyHelpers.copyDirectory(
                            entry, target);
                }
            } else if (!Files.exists(target)) {
                CopyHelpers.copyStaticFile(entry, target);
            }
        }
    }

    /**
     * Lists directories sorted alphabetically.
     *
     * @param dir the directory to list
     * @return sorted list of subdirectory paths
     */
    static List<Path> listDirsSorted(Path dir) {
        try (Stream<Path> stream = Files.list(dir)) {
            return stream
                    .filter(Files::isDirectory)
                    .sorted()
                    .toList();
        } catch (IOException e) {
            throw new UncheckedIOException(
                    "Failed to list directory: " + dir, e);
        }
    }

    /**
     * Lists all entries sorted alphabetically.
     *
     * @param dir the directory to list
     * @return sorted list of entry paths
     */
    static List<Path> listEntriesSorted(Path dir) {
        try (Stream<Path> stream = Files.list(dir)) {
            return stream
                    .sorted()
                    .toList();
        } catch (IOException e) {
            throw new UncheckedIOException(
                    "Failed to list directory: " + dir, e);
        }
    }
}
