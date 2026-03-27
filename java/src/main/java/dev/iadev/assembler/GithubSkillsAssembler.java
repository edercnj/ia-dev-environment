package dev.iadev.assembler;

import dev.iadev.config.ContextBuilder;
import dev.iadev.model.ProjectConfig;
import dev.iadev.template.TemplateEngine;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;

/**
 * Assembles {@code .github/skills/} from templates for
 * GitHub Copilot, mirroring the Claude skills structure.
 *
 * <p>Generates skills in 8 groups with conditional
 * filtering for infrastructure and knowledge-packs.</p>
 *
 * @see Assembler
 */
public final class GithubSkillsAssembler
        implements Assembler {

    private static final String TEMPLATES_DIR =
            "github-skills-templates";
    private static final String SKILL_MD = "SKILL.md";
    private static final String SKILLS_OUTPUT = "skills";
    private static final String INFRA_GROUP =
            "infrastructure";
    private static final String KP_GROUP =
            "knowledge-packs";

    /**
     * Groups whose skills are nested under a subdirectory
     * in the output (e.g., {@code skills/lib/{name}/}).
     */
    static final Set<String> NESTED_GROUPS =
            Set.of("lib");

    /** @see SkillGroupRegistry#SKILL_GROUPS */
    static final Map<String, List<String>> SKILL_GROUPS =
            SkillGroupRegistry.SKILL_GROUPS;

    /** @see SkillGroupRegistry#INFRA_SKILL_CONDITIONS */
    static final Map<String, Predicate<ProjectConfig>>
            INFRA_SKILL_CONDITIONS =
            SkillGroupRegistry.INFRA_SKILL_CONDITIONS;

    /** @see SkillGroupRegistry#KP_SKILL_CONDITIONS */
    static final Map<String, Predicate<ProjectConfig>>
            KP_SKILL_CONDITIONS =
            SkillGroupRegistry.KP_SKILL_CONDITIONS;

    private final Path resourcesDir;

    /**
     * Creates a GithubSkillsAssembler using classpath
     * resources.
     */
    public GithubSkillsAssembler() {
        this(resolveClasspathResources());
    }

    /**
     * Creates a GithubSkillsAssembler with an explicit
     * resources directory.
     *
     * @param resourcesDir the base resources directory
     */
    public GithubSkillsAssembler(Path resourcesDir) {
        this.resourcesDir = resourcesDir;
    }

    /**
     * {@inheritDoc}
     *
     * <p>Generates skill files from all 8 registered
     * groups, applying feature gates for the
     * infrastructure group and nesting for the lib
     * group.</p>
     */
    @Override
    public List<String> assemble(
            ProjectConfig config,
            TemplateEngine engine,
            Path outputDir) {
        Map<String, Object> context =
                ContextBuilder.buildContext(config);
        List<String> results = new ArrayList<>();
        for (Map.Entry<String, List<String>> entry
                : SKILL_GROUPS.entrySet()) {
            String group = entry.getKey();
            List<String> skillNames = entry.getValue();
            List<String> filtered =
                    filterSkills(config, group, skillNames);
            Path srcDir = resourcesDir.resolve(
                    TEMPLATES_DIR + "/" + group);
            String subDir = NESTED_GROUPS.contains(group)
                    ? group : null;
            SkillRenderContext ctx =
                    new SkillRenderContext(
                            srcDir, outputDir,
                            subDir, context);
            results.addAll(generateGroup(
                    engine, ctx, filtered));
        }
        return results;
    }

    /**
     * Filters skills based on config conditions.
     *
     * <p>Only the infrastructure group is filtered; all
     * other groups return the full list unchanged.</p>
     *
     * @param config     the project configuration
     * @param group      the skill group name
     * @param skillNames the skill names in the group
     * @return the filtered list of skill names
     */
    List<String> filterSkills(
            ProjectConfig config,
            String group,
            List<String> skillNames) {
        Map<String, Predicate<ProjectConfig>> conditions =
                SkillGroupRegistry.conditionsForGroup(group);
        if (conditions.isEmpty()) {
            return new ArrayList<>(skillNames);
        }
        List<String> filtered = new ArrayList<>();
        for (String name : skillNames) {
            Predicate<ProjectConfig> condition =
                    conditions.get(name);
            if (condition == null
                    || condition.test(config)) {
                filtered.add(name);
            }
        }
        return filtered;
    }

    private List<String> generateGroup(
            TemplateEngine engine,
            SkillRenderContext ctx,
            List<String> skillNames) {
        if (!Files.exists(ctx.srcDir())) {
            return List.of();
        }
        List<String> results = new ArrayList<>();
        for (String name : skillNames) {
            renderSkill(engine, ctx, name)
                    .ifPresent(results::add);
        }
        return results;
    }

    /**
     * Renders a single skill template to output.
     *
     * <p>Reads the template, applies placeholder
     * replacement, creates the output directory, writes
     * SKILL.md, and copies any references.</p>
     *
     * @param engine the template engine
     * @param ctx    the skill render context
     * @param name   the skill name
     * @return Optional containing the destination path,
     *         or empty if template is missing
     */
    Optional<String> renderSkill(
            TemplateEngine engine,
            SkillRenderContext ctx,
            String name) {
        Path src = ctx.srcDir().resolve(name + ".md");
        if (!Files.exists(src)) {
            return Optional.empty();
        }
        String content = CopyHelpers.readFile(src);
        String rendered = engine.replacePlaceholders(
                content, ctx.context());

        Path skillDir;
        if (ctx.subDir() != null) {
            skillDir = ctx.outputDir().resolve(
                    SKILLS_OUTPUT + "/" + ctx.subDir()
                            + "/" + name);
        } else {
            skillDir = ctx.outputDir().resolve(
                    SKILLS_OUTPUT + "/" + name);
        }
        CopyHelpers.ensureDirectory(skillDir);

        Path dest = skillDir.resolve(SKILL_MD);
        CopyHelpers.writeFile(dest, rendered);
        copyReferences(ctx, engine, skillDir, name);
        return Optional.of(dest.toString());
    }

    /**
     * Copies a references directory for a skill if it
     * exists, applying placeholder replacement to all
     * markdown files.
     *
     * @param ctx      the skill render context
     * @param engine   the template engine
     * @param skillDir the destination skill directory
     * @param name     the skill name
     */
    void copyReferences(
            SkillRenderContext ctx,
            TemplateEngine engine,
            Path skillDir,
            String name) {
        Path refsDir = ctx.srcDir().resolve(
                "references/" + name);
        if (!Files.exists(refsDir)) {
            return;
        }
        Path destRefs = skillDir.resolve("references");
        CopyHelpers.copyDirectory(refsDir, destRefs);
        CopyHelpers.replacePlaceholdersInDir(
                destRefs, engine, ctx.context());
    }

    private static Path resolveClasspathResources() {
        return dev.iadev.util.ResourceResolver
                .resolveResourcesRoot(TEMPLATES_DIR);
    }
}
