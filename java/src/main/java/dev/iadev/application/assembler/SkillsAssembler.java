package dev.iadev.application.assembler;

import dev.iadev.config.ContextBuilder;
import dev.iadev.domain.model.ProjectConfig;
import dev.iadev.template.TemplateEngine;

import dev.iadev.domain.model.ContextBudget;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;

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
 * <p><b>Source-of-truth taxonomy (EPIC-0036 / ADR-0003):</b>
 * Skills under {@code core/} and {@code conditional/} live in
 * category subfolders ({@code plan/}, {@code dev/},
 * {@code test/}, {@code review/}, {@code security/},
 * {@code code/}, {@code git/}, {@code pr/}, {@code ops/},
 * {@code jira/}). The generated output remains <b>flat</b>:
 * {@code .claude/skills/{name}/SKILL.md} with no category
 * prefix. {@code core/lib/} retains its historical nested
 * layout as {@code skills/lib/{name}/}. See
 * {@code adr/ADR-0003-skill-taxonomy-and-naming.md} for the
 * rationale behind the SoT-hierarchical / output-flat
 * asymmetry.</p>
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
        Map<String, Object> context =
                ContextBuilder.buildContext(config);
        Map<String, Path> coreSources =
                discoverSkillSources(CORE_DIR);
        Map<String, Path> conditionalSources =
                discoverSkillSources(CONDITIONAL_DIR);
        List<String> generated = new ArrayList<>();
        generated.addAll(
                assembleCore(
                        coreSources, outputDir,
                        engine, context));
        generated.addAll(
                assembleConditional(
                        config, conditionalSources,
                        outputDir, engine, context));
        generated.addAll(
                assembleKnowledge(
                        config, outputDir, engine, context));
        return generated;
    }

    /**
     * Scans core skills directory and returns skill names.
     *
     * <p>Post EPIC-0036: walks two layers
     * ({@code core/{category}/{skill-name}}) and returns a
     * flat list of skill names. {@code lib/} retains the
     * legacy pseudo-category behavior producing
     * {@code lib/{name}} entries.</p>
     *
     * @return sorted list of core skill names
     */
    List<String> selectCoreSkills() {
        return discoverSkillSources(CORE_DIR)
                .keySet().stream().sorted().toList();
    }

    /**
     * Walks {@code targets/claude/skills/{subdir}} two layers
     * deep and returns a name→source-path map. The first
     * layer is the category ({@code plan/}, {@code dev/},
     * ...); the second layer is the skill directory. For
     * {@code lib/}, entries are keyed as
     * {@code lib/{skill-name}} to preserve the legacy nested
     * output layout.
     *
     * @param subdir either {@code core} or {@code conditional}
     * @return sorted map of skill-name → source path
     */
    private Map<String, Path> discoverSkillSources(
            String subdir) {
        Map<String, Path> result = new TreeMap<>();
        Path base = resourcesDir.resolve(
                SKILLS_TEMPLATES_DIR + "/" + subdir);
        if (!Files.exists(base)
                || !Files.isDirectory(base)) {
            return result;
        }
        for (Path category
                : SkillsCopyHelper.listDirsSorted(base)) {
            String catName =
                    category.getFileName().toString();
            if (LIB_DIR.equals(catName)) {
                for (Path sub
                        : SkillsCopyHelper.listDirsSorted(
                                category)) {
                    String subName =
                            sub.getFileName().toString();
                    result.put(
                            LIB_DIR + "/" + subName, sub);
                }
            } else {
                for (Path skill
                        : SkillsCopyHelper.listDirsSorted(
                                category)) {
                    result.put(
                            skill.getFileName().toString(),
                            skill);
                }
            }
        }
        return result;
    }

    private List<String> assembleCore(
            Map<String, Path> coreSources,
            Path outputDir,
            TemplateEngine engine,
            Map<String, Object> context) {
        List<String> generated = new ArrayList<>();
        for (Map.Entry<String, Path> entry
                : coreSources.entrySet()) {
            String result = copyCoreSkill(
                    entry.getKey(), entry.getValue(),
                    outputDir, engine, context);
            generated.add(result);
        }
        return generated;
    }

    private List<String> assembleConditional(
            ProjectConfig config,
            Map<String, Path> conditionalSources,
            Path outputDir,
            TemplateEngine engine,
            Map<String, Object> context) {
        List<String> generated = new ArrayList<>();
        List<String> conditional =
                SkillsSelection.selectConditionalSkills(
                        config);
        for (String skill : conditional) {
            Path src = conditionalSources.get(skill);
            if (src == null) {
                continue;
            }
            copyConditionalSkill(
                    skill, src, outputDir, engine, context)
                    .ifPresent(generated::add);
        }
        return generated;
    }

    private List<String> assembleKnowledge(
            ProjectConfig config,
            Path outputDir,
            TemplateEngine engine,
            Map<String, Object> context) {
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
            Path src,
            Path outputDir,
            TemplateEngine engine,
            Map<String, Object> context) {
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
            Path src,
            Path outputDir,
            TemplateEngine engine,
            Map<String, Object> context) {
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
