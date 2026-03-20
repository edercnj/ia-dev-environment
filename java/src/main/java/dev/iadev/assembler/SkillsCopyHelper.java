package dev.iadev.assembler;

import dev.iadev.domain.stack.SkillRegistry;
import dev.iadev.domain.stack.StackPackMapping;
import dev.iadev.model.ProjectConfig;
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

    private static final String SKILLS_TEMPLATES_DIR =
            "skills-templates";
    private static final String KNOWLEDGE_PACKS_DIR =
            "knowledge-packs";
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
        Path src = resourcesDir.resolve(
                SKILLS_TEMPLATES_DIR + "/"
                        + KNOWLEDGE_PACKS_DIR + "/"
                        + packName);
        if (!Files.exists(src)
                || !Files.isDirectory(src)) {
            return Optional.empty();
        }
        Path dest = outputDir.resolve(
                SKILLS_OUTPUT + "/" + packName);
        CopyHelpers.ensureDirectory(dest);

        Path skillMdSrc = src.resolve(SKILL_MD);
        if (Files.exists(skillMdSrc)) {
            CopyHelpers.copyTemplateFile(
                    skillMdSrc,
                    dest.resolve(SKILL_MD),
                    engine,
                    context);
        }
        copyNonSkillItems(src, dest);
        CopyHelpers.replacePlaceholdersInDir(
                dest, engine, context);
        return Optional.of(dest.toString());
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
        String packName = StackPackMapping
                .getStackPackName(config.framework().name());
        if (packName.isEmpty()) {
            return Optional.empty();
        }
        Path src = resourcesDir.resolve(
                SKILLS_TEMPLATES_DIR + "/"
                        + KNOWLEDGE_PACKS_DIR + "/"
                        + STACK_PATTERNS_DIR + "/"
                        + packName);
        if (!Files.exists(src)
                || !Files.isDirectory(src)) {
            return Optional.empty();
        }
        Path dest = outputDir.resolve(
                SKILLS_OUTPUT + "/" + packName);
        CopyHelpers.copyDirectory(src, dest);
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
            Path src = resourcesDir.resolve(
                    SKILLS_TEMPLATES_DIR + "/"
                            + KNOWLEDGE_PACKS_DIR + "/"
                            + INFRA_PATTERNS_DIR + "/"
                            + rule.packName());
            if (!Files.exists(src)
                    || !Files.isDirectory(src)) {
                continue;
            }
            Path dest = outputDir.resolve(
                    SKILLS_OUTPUT + "/" + rule.packName());
            CopyHelpers.copyDirectory(src, dest);
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
            if (Files.exists(target)) {
                continue;
            }
            if (Files.isDirectory(entry)) {
                CopyHelpers.copyDirectory(entry, target);
            } else {
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
