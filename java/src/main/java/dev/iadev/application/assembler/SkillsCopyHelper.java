package dev.iadev.application.assembler;

import dev.iadev.domain.stack.SkillRegistry;
import dev.iadev.domain.stack.StackPackMapping;
import dev.iadev.domain.model.ProjectConfig;
import dev.iadev.template.TemplateEngine;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

/**
 * Helper methods for copying skills, knowledge packs,
 * and infrastructure patterns during skills assembly.
 *
 * <p>Extracted from {@link SkillsAssembler} to keep both
 * classes under 250 lines per RULE-004.</p>
 *
 * @see SkillsAssembler
 */
final class SkillsCopyHelper {

    private static final String KNOWLEDGE_DIR =
            "targets/claude/knowledge";
    private static final String INFRA_PATTERNS_DIR =
            "infra-patterns";
    private static final String STACK_PATTERNS_DIR =
            "stack-patterns";
    private static final String SKILL_MD = "SKILL.md";
    private static final String SKILLS_OUTPUT = "skills";

    private SkillsCopyHelper() {
        // utility class
    }

    /**
     * Copies a knowledge pack with SKILL.md rendering.
     *
     * @param packName     the knowledge pack name
     * @param resourcesDir the resources directory
     * @param outputDir    the output directory
     * @param engine       the template engine
     * @param context      the context map
     * @return Optional containing the generated path
     */
    static Optional<String> copyKnowledgePack(
            String packName,
            Path resourcesDir,
            Path outputDir,
            TemplateEngine engine,
            Map<String, Object> context) {
        // STORY-0051-0002: KPs moved to targets/claude/knowledge/.
        // During transition (stories 0003/0004/0005 retrofit consumers),
        // SkillsAssembler continues to emit .claude/skills/{pack}/ from
        // the NEW source layout so existing tests and consumers keep
        // working. STORY-0051-0006 removes this dual-output entirely.
        Path newSrc = resourcesDir.resolve(
                KNOWLEDGE_DIR + "/" + packName);
        Path newSrcFile = resourcesDir.resolve(
                KNOWLEDGE_DIR + "/" + packName + ".md");
        boolean isComplexKp = Files.isDirectory(newSrc);
        boolean isSimpleKp = Files.isRegularFile(newSrcFile);
        if (!isComplexKp && !isSimpleKp) {
            return Optional.empty();
        }
        Path dest = outputDir.resolve(
                SKILLS_OUTPUT + "/" + packName);
        CopyHelpers.ensureDirectory(dest);

        if (isSimpleKp) {
            // knowledge/{pack}.md → skills/{pack}/SKILL.md
            CopyHelpers.copyTemplateFile(
                    newSrcFile,
                    dest.resolve(SKILL_MD),
                    engine,
                    context);
        } else {
            // knowledge/{pack}/index.md → skills/{pack}/SKILL.md;
            // other files copied as-is into skills/{pack}/
            Path indexMd = newSrc.resolve("index.md");
            if (Files.exists(indexMd)) {
                CopyHelpers.copyTemplateFile(
                        indexMd,
                        dest.resolve(SKILL_MD),
                        engine,
                        context);
            }
            copyNonIndexItems(newSrc, dest);
        }
        CopyHelpers.replacePlaceholdersInDir(
                dest, engine, context);
        return Optional.of(dest.toString());
    }

    private static void copyNonIndexItems(
            Path src, Path dest) {
        // Copy all files except index.md. Plain .md files
        // (previously in references/) are placed back under
        // dest/references/ to preserve the legacy skill output
        // shape expected by existing tests and consumers.
        // Subdirectories are copied as-is (nested KP support).
        try (Stream<Path> stream = Files.list(src)) {
            stream
                    .filter(p -> !"index.md".equals(
                            p.getFileName().toString()))
                    .forEach(p -> copyNonIndexEntry(p, dest));
        } catch (IOException e) {
            throw new UncheckedIOException(
                    "Failed to copy knowledge items: %s"
                            .formatted(src), e);
        }
    }

    private static void copyNonIndexEntry(
            Path entry, Path dest) {
        String name = entry.getFileName().toString();
        if (Files.isDirectory(entry)) {
            Path target = dest.resolve(name);
            CopyHelpers.copyDirectory(entry, target);
            return;
        }
        if (name.endsWith(".md")) {
            // Place under references/ to preserve legacy
            // skill output layout (skill/{kp}/references/*.md).
            Path refsDir = dest.resolve("references");
            CopyHelpers.ensureDirectory(refsDir);
            CopyHelpers.copyStaticFile(
                    entry, refsDir.resolve(name));
        } else {
            CopyHelpers.copyStaticFile(
                    entry, dest.resolve(name));
        }
    }

    /**
     * Copies stack patterns for the project framework.
     *
     * @param config       the project configuration
     * @param resourcesDir the resources directory
     * @param outputDir    the output directory
     * @param engine       the template engine
     * @param context      the context map
     * @return Optional containing the generated path
     */
    static Optional<String> copyStackPatterns(
            ProjectConfig config,
            Path resourcesDir,
            Path outputDir,
            TemplateEngine engine,
            Map<String, Object> context) {
        // STORY-0051-0002: stack-patterns migrated under knowledge/.
        String packName = StackPackMapping
                .getStackPackName(config.framework().name());
        if (packName.isEmpty()) {
            return Optional.empty();
        }
        Path src = resourcesDir.resolve(
                KNOWLEDGE_DIR + "/"
                        + STACK_PATTERNS_DIR + "/"
                        + packName);
        if (!Files.exists(src)
                || !Files.isDirectory(src)) {
            return Optional.empty();
        }
        Path dest = outputDir.resolve(
                SKILLS_OUTPUT + "/" + packName);
        // Map index.md → SKILL.md to preserve old skill output shape
        Path indexMd = src.resolve("index.md");
        if (Files.exists(indexMd)) {
            CopyHelpers.ensureDirectory(dest);
            CopyHelpers.copyTemplateFile(
                    indexMd, dest.resolve(SKILL_MD),
                    engine, context);
            copyNonIndexItems(src, dest);
        } else {
            CopyHelpers.copyDirectory(src, dest);
        }
        CopyHelpers.replacePlaceholdersInDir(
                dest, engine, context);
        return Optional.of(dest.toString());
    }

    /**
     * Copies infrastructure pattern packs.
     *
     * @param config       the project configuration
     * @param resourcesDir the resources directory
     * @param outputDir    the output directory
     * @param engine       the template engine
     * @param context      the context map
     * @return list of generated file paths
     */
    static List<String> copyInfraPatterns(
            ProjectConfig config,
            Path resourcesDir,
            Path outputDir,
            TemplateEngine engine,
            Map<String, Object> context) {
        List<String> generated = new ArrayList<>();
        var rules = SkillRegistry.buildInfraPackRules(
                config.infrastructure());
        for (var rule : rules) {
            if (!rule.included()) {
                continue;
            }
            // STORY-0051-0002: infra-patterns migrated under knowledge/.
            Path src = resourcesDir.resolve(
                    KNOWLEDGE_DIR + "/"
                            + INFRA_PATTERNS_DIR + "/"
                            + rule.packName());
            if (!Files.exists(src)
                    || !Files.isDirectory(src)) {
                continue;
            }
            Path dest = outputDir.resolve(
                    SKILLS_OUTPUT + "/" + rule.packName());
            // index.md → SKILL.md for output compatibility
            Path indexMd = src.resolve("index.md");
            if (Files.exists(indexMd)) {
                CopyHelpers.ensureDirectory(dest);
                CopyHelpers.copyTemplateFile(
                        indexMd, dest.resolve(SKILL_MD),
                        engine, context);
                copyNonIndexItems(src, dest);
            } else {
                CopyHelpers.copyDirectory(src, dest);
            }
            CopyHelpers.replacePlaceholdersInDir(
                    dest, engine, context);
            generated.add(dest.toString());
        }
        return generated;
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
