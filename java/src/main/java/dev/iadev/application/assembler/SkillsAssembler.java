package dev.iadev.application.assembler;

import dev.iadev.config.ContextBuilder;
import dev.iadev.domain.model.ProjectConfig;
import dev.iadev.template.TemplateEngine;

import dev.iadev.domain.model.ContextBudget;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

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
 * @see Assembler
 * @see SkillsSelection
 * @see SkillsCopyHelper
 */
public final class SkillsAssembler implements Assembler {

    private static final String SKILLS_TEMPLATES_DIR =
            "targets/claude/skills";
    private static final String CORE_DIR = "core";
    private static final String CONDITIONAL_DIR =
            "conditional";
    private static final String LIB_DIR = "lib";
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

    /** {@inheritDoc} */
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
     * <p><b>Source-of-truth is hierarchical (10 category
     * subfolders), output is flat.</b> Skills physically live
     * at {@code core/{category}/{name}/SKILL.md} (e.g.
     * {@code core/plan/x-story-epic/SKILL.md}) but the
     * generated output is always {@code .claude/skills/{name}/
     * SKILL.md} — category subfolders are stripped.
     * The {@code lib/} subtree is the one exception and keeps
     * its {@code lib/} prefix in the emitted name. Traversal
     * is delegated to {@link SkillsHierarchyResolver}, which
     * uses the {@code SKILL.md} marker rule to transparently
     * support legacy flat layouts (backward-compatible) and a
     * single level of category nesting.</p>
     *
     * @return list of core skill names in scan order
     *         (categories sorted alphabetically, then skills
     *         within each category sorted alphabetically)
     */
    List<String> selectCoreSkills() {
        return SkillsHierarchyResolver.listSkillNames(
                corePath());
    }

    private Path corePath() {
        return resourcesDir.resolve(
                SKILLS_TEMPLATES_DIR + "/" + CORE_DIR);
    }

    private Path conditionalPath() {
        return resourcesDir.resolve(
                SKILLS_TEMPLATES_DIR + "/"
                        + CONDITIONAL_DIR);
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
            copyConditionalSkill(
                    skill, outputDir, engine, context)
                    .ifPresent(generated::add);
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
                SkillsSelection.selectKnowledgePacks(
                        config);
        for (String pack : packs) {
            SkillsCopyHelper.copyKnowledgePack(
                    pack, resourcesDir, outputDir,
                    engine, context)
                    .ifPresent(generated::add);
        }
        SkillsCopyHelper.copyStackPatterns(
                config, resourcesDir, outputDir,
                engine, context)
                .ifPresent(generated::add);
        generated.addAll(
                SkillsCopyHelper.copyInfraPatterns(
                        config, resourcesDir, outputDir,
                        engine, context));
        return generated;
    }

    private String copyCoreSkill(
            String skillName,
            Path outputDir,
            TemplateEngine engine,
            java.util.Map<String, Object> context) {
        Path src = SkillsHierarchyResolver.resolveSkillPath(
                corePath(), skillName);
        Path dest = outputDir.resolve(
                SKILLS_OUTPUT + "/" + skillName);
        CopyHelpers.copyDirectory(src, dest);
        CopyHelpers.replacePlaceholdersInDir(
                dest, engine, context);
        injectBudgetField(src, dest);
        return dest.toString();
    }

    private void injectBudgetField(
            Path srcDir, Path destDir) {
        Path destSkill = destDir.resolve("SKILL.md");
        if (!Files.exists(destSkill)) {
            return;
        }
        String content = CopyHelpers.readFile(destSkill);
        int lineCount =
                (int) content.lines().count();
        ContextBudget budget =
                ContextBudget.fromLineCount(lineCount);
        String injected =
                FrontmatterInjector.injectContextBudget(
                        content, budget);
        CopyHelpers.writeFile(destSkill, injected);
    }


    private Optional<String> copyConditionalSkill(
            String skillName,
            Path outputDir,
            TemplateEngine engine,
            java.util.Map<String, Object> context) {
        Path src = SkillsHierarchyResolver.resolveSkillPath(
                conditionalPath(), skillName);
        if (!Files.exists(src)
                || !Files.isDirectory(src)) {
            return Optional.empty();
        }
        Path dest = outputDir.resolve(
                SKILLS_OUTPUT + "/" + skillName);
        CopyHelpers.copyDirectory(src, dest);
        CopyHelpers.replacePlaceholdersInDir(
                dest, engine, context);
        injectBudgetField(src, dest);
        return Optional.of(dest.toString());
    }

    private static Path resolveClasspathResources() {
        return dev.iadev.util.ResourceResolver
                .resolveResourcesRoot(
                        SKILLS_TEMPLATES_DIR, 3);
    }
}
