package dev.iadev.assembler;

import dev.iadev.config.ContextBuilder;
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
import java.util.stream.Stream;

/**
 * Assembles {@code .claude/skills/} from templates based on
 * project configuration.
 *
 * <p>This is the second assembler in the pipeline (position 2
 * of 23 per RULE-005). It generates three categories of
 * skills:
 * <ol>
 *   <li>Core skills — always included regardless of
 *       profile</li>
 *   <li>Conditional skills — included based on feature
 *       gates evaluated by {@link SkillsSelection}</li>
 *   <li>Knowledge packs — always included, plus
 *       stack-specific and infrastructure patterns</li>
 * </ol>
 *
 * <p>Assembly flow:
 * <ol>
 *   <li>Scan core skills directory for skill names
 *       (including lib/ sub-skills)</li>
 *   <li>Copy each core skill tree to output</li>
 *   <li>Evaluate feature gates via
 *       {@link SkillsSelection}</li>
 *   <li>Copy matching conditional skill trees</li>
 *   <li>Copy knowledge packs with SKILL.md rendering</li>
 *   <li>Copy stack-specific patterns if framework
 *       matches</li>
 *   <li>Copy infrastructure patterns based on infra
 *       config</li>
 * </ol>
 *
 * <p>Example usage:
 * <pre>{@code
 * Assembler skills = new SkillsAssembler();
 * List<String> files = skills.assemble(
 *     config, engine, outputDir);
 * }</pre>
 * </p>
 *
 * @see Assembler
 * @see SkillsSelection
 * @see SkillRegistry
 */
public final class SkillsAssembler implements Assembler {

    private static final String SKILLS_TEMPLATES_DIR =
            "skills-templates";
    private static final String CORE_DIR = "core";
    private static final String CONDITIONAL_DIR =
            "conditional";
    private static final String KNOWLEDGE_PACKS_DIR =
            "knowledge-packs";
    private static final String INFRA_PATTERNS_DIR =
            "infra-patterns";
    private static final String STACK_PATTERNS_DIR =
            "stack-patterns";
    private static final String LIB_DIR = "lib";
    private static final String SKILL_MD = "SKILL.md";
    private static final String SKILLS_OUTPUT = "skills";

    private final Path resourcesDir;

    /**
     * Creates a SkillsAssembler using classpath resources.
     */
    public SkillsAssembler() {
        this(resolveClasspathResources());
    }

    /**
     * Creates a SkillsAssembler with an explicit resources
     * directory.
     *
     * @param resourcesDir the base resources directory
     */
    public SkillsAssembler(Path resourcesDir) {
        this.resourcesDir = resourcesDir;
    }

    /**
     * {@inheritDoc}
     *
     * <p>Orchestrates all assembly layers: core skills,
     * conditional skills, and knowledge packs. Returns the
     * list of generated file/directory paths.</p>
     */
    @Override
    public List<String> assemble(
            ProjectConfig config,
            TemplateEngine engine,
            Path outputDir) {
        java.util.Map<String, Object> context =
                ContextBuilder.buildContext(config);
        List<String> generated = new ArrayList<>();
        generated.addAll(
                assembleCore(outputDir, engine, context));
        generated.addAll(
                assembleConditional(
                        config, outputDir, engine, context));
        generated.addAll(
                assembleKnowledge(
                        config, outputDir, engine, context));
        return generated;
    }

    /**
     * Scans core skills directory and returns skill names.
     *
     * <p>Handles the special {@code lib/} subdirectory by
     * prefixing sub-skill names with {@code lib/}.</p>
     *
     * @return sorted list of core skill names
     */
    List<String> selectCoreSkills() {
        Path corePath = resourcesDir.resolve(
                SKILLS_TEMPLATES_DIR + "/" + CORE_DIR);
        if (!Files.exists(corePath)
                || !Files.isDirectory(corePath)) {
            return List.of();
        }
        List<String> skills = new ArrayList<>();
        List<Path> entries = listDirsSorted(corePath);
        for (Path entry : entries) {
            String name = entry.getFileName().toString();
            if (LIB_DIR.equals(name)) {
                List<Path> subs = listDirsSorted(entry);
                for (Path sub : subs) {
                    skills.add(LIB_DIR + "/"
                            + sub.getFileName().toString());
                }
            } else {
                skills.add(name);
            }
        }
        return skills;
    }

    private List<String> assembleCore(
            Path outputDir,
            TemplateEngine engine,
            java.util.Map<String, Object> context) {
        List<String> generated = new ArrayList<>();
        for (String skill : selectCoreSkills()) {
            String result = copyCoreSkill(
                    skill, outputDir, engine, context);
            generated.add(result);
        }
        return generated;
    }

    private List<String> assembleConditional(
            ProjectConfig config,
            Path outputDir,
            TemplateEngine engine,
            java.util.Map<String, Object> context) {
        List<String> generated = new ArrayList<>();
        List<String> conditional =
                SkillsSelection.selectConditionalSkills(
                        config);
        for (String skill : conditional) {
            String result = copyConditionalSkill(
                    skill, outputDir, engine, context);
            if (result != null) {
                generated.add(result);
            }
        }
        return generated;
    }

    private List<String> assembleKnowledge(
            ProjectConfig config,
            Path outputDir,
            TemplateEngine engine,
            java.util.Map<String, Object> context) {
        List<String> generated = new ArrayList<>();
        List<String> packs =
                SkillsSelection.selectKnowledgePacks(config);
        for (String pack : packs) {
            String result = copyKnowledgePack(
                    pack, outputDir, engine, context);
            if (result != null) {
                generated.add(result);
            }
        }
        String stack = copyStackPatterns(
                config, outputDir, engine, context);
        if (stack != null) {
            generated.add(stack);
        }
        generated.addAll(copyInfraPatterns(
                config, outputDir, engine, context));
        return generated;
    }

    private String copyCoreSkill(
            String skillName,
            Path outputDir,
            TemplateEngine engine,
            java.util.Map<String, Object> context) {
        Path src = resourcesDir.resolve(
                SKILLS_TEMPLATES_DIR + "/"
                        + CORE_DIR + "/" + skillName);
        Path dest = outputDir.resolve(
                SKILLS_OUTPUT + "/" + skillName);
        CopyHelpers.copyDirectory(src, dest);
        CopyHelpers.replacePlaceholdersInDir(
                dest, engine, context);
        return dest.toString();
    }

    private String copyConditionalSkill(
            String skillName,
            Path outputDir,
            TemplateEngine engine,
            java.util.Map<String, Object> context) {
        Path src = resourcesDir.resolve(
                SKILLS_TEMPLATES_DIR + "/"
                        + CONDITIONAL_DIR + "/"
                        + skillName);
        if (!Files.exists(src)
                || !Files.isDirectory(src)) {
            return null;
        }
        Path dest = outputDir.resolve(
                SKILLS_OUTPUT + "/" + skillName);
        CopyHelpers.copyDirectory(src, dest);
        CopyHelpers.replacePlaceholdersInDir(
                dest, engine, context);
        return dest.toString();
    }

    private String copyKnowledgePack(
            String packName,
            Path outputDir,
            TemplateEngine engine,
            java.util.Map<String, Object> context) {
        Path src = resourcesDir.resolve(
                SKILLS_TEMPLATES_DIR + "/"
                        + KNOWLEDGE_PACKS_DIR + "/"
                        + packName);
        if (!Files.exists(src)
                || !Files.isDirectory(src)) {
            return null;
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
        return dest.toString();
    }

    private String copyStackPatterns(
            ProjectConfig config,
            Path outputDir,
            TemplateEngine engine,
            java.util.Map<String, Object> context) {
        String packName = StackPackMapping
                .getStackPackName(config.framework().name());
        if (packName.isEmpty()) {
            return null;
        }
        Path src = resourcesDir.resolve(
                SKILLS_TEMPLATES_DIR + "/"
                        + KNOWLEDGE_PACKS_DIR + "/"
                        + STACK_PATTERNS_DIR + "/"
                        + packName);
        if (!Files.exists(src)
                || !Files.isDirectory(src)) {
            return null;
        }
        Path dest = outputDir.resolve(
                SKILLS_OUTPUT + "/" + packName);
        CopyHelpers.copyDirectory(src, dest);
        CopyHelpers.replacePlaceholdersInDir(
                dest, engine, context);
        return dest.toString();
    }

    private List<String> copyInfraPatterns(
            ProjectConfig config,
            Path outputDir,
            TemplateEngine engine,
            java.util.Map<String, Object> context) {
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

    private void copyNonSkillItems(
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

    private static List<Path> listDirsSorted(Path dir) {
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

    private static List<Path> listEntriesSorted(Path dir) {
        try (Stream<Path> stream = Files.list(dir)) {
            return stream
                    .sorted()
                    .toList();
        } catch (IOException e) {
            throw new UncheckedIOException(
                    "Failed to list directory: " + dir, e);
        }
    }

    private static Path resolveClasspathResources() {
        var url = SkillsAssembler.class.getClassLoader()
                .getResource(SKILLS_TEMPLATES_DIR);
        if (url == null) {
            return Path.of("src/main/resources");
        }
        return Path.of(url.getPath()).getParent();
    }
}
