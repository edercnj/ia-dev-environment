package dev.iadev.assembler;

import dev.iadev.config.ContextBuilder;
import dev.iadev.model.ProjectConfig;
import dev.iadev.template.TemplateEngine;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;

/**
 * Assembles {@code .github/skills/} from templates for
 * GitHub Copilot, mirroring the Claude skills structure.
 *
 * <p>This is the tenth assembler in the pipeline (position
 * 10 of 23 per RULE-005). It generates skills organized
 * in 8 groups: story, dev, review, testing,
 * infrastructure, knowledge-packs, git-troubleshooting,
 * and lib.</p>
 *
 * <p>The infrastructure group applies conditional filtering
 * based on project configuration feature gates. The lib
 * group generates skills in a nested subdirectory
 * ({@code skills/lib/{name}/SKILL.md}).</p>
 *
 * <p>Each skill template is read from
 * {@code github-skills-templates/{group}/{name}.md},
 * rendered with placeholder replacement, and written to
 * {@code skills/{name}/SKILL.md}. Skills with a
 * {@code references/} subdirectory also copy that
 * structure with placeholder replacement.</p>
 *
 * <p>Example usage:
 * <pre>{@code
 * Assembler skills = new GithubSkillsAssembler();
 * List<String> files = skills.assemble(
 *     config, engine, outputDir);
 * }</pre>
 * </p>
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

    /**
     * Groups whose skills are nested under a subdirectory
     * in the output (e.g., {@code skills/lib/{name}/}).
     */
    static final Set<String> NESTED_GROUPS =
            Set.of("lib");

    /**
     * Skill groups mapping group name to template names.
     *
     * <p>Ordered by insertion to match TypeScript output.
     * Each group contains a list of skill template names
     * (without the .md extension).</p>
     */
    static final Map<String, List<String>> SKILL_GROUPS;

    static {
        SKILL_GROUPS = new LinkedHashMap<>();
        SKILL_GROUPS.put("story", List.of(
                "x-story-epic", "x-story-create",
                "x-story-map", "x-story-epic-full",
                "story-planning"));
        SKILL_GROUPS.put("dev", List.of(
                "x-dev-implement", "x-dev-lifecycle",
                "x-dev-epic-implement",
                "x-dev-architecture-plan",
                "x-dev-arch-update",
                "layer-templates",
                "x-dev-adr-automation",
                "x-mcp-recommend"));
        SKILL_GROUPS.put("review", List.of(
                "x-review", "x-review-api", "x-review-pr",
                "x-review-grpc", "x-review-events",
                "x-review-gateway",
                "x-codebase-audit",
                "x-dependency-audit"));
        SKILL_GROUPS.put("testing", List.of(
                "x-test-plan", "x-test-run", "run-e2e",
                "run-smoke-api", "run-contract-tests",
                "run-perf-test"));
        SKILL_GROUPS.put("infrastructure", List.of(
                "setup-environment", "k8s-deployment",
                "k8s-kustomize", "dockerfile",
                "iac-terraform"));
        SKILL_GROUPS.put("knowledge-packs", List.of(
                "architecture", "coding-standards",
                "patterns", "protocols", "observability",
                "resilience", "security", "compliance",
                "api-design"));
        SKILL_GROUPS.put("git-troubleshooting", List.of(
                "x-git-push", "x-ops-troubleshoot",
                "x-fix-pr-comments", "x-changelog"));
        SKILL_GROUPS.put("lib", List.of(
                "x-lib-task-decomposer",
                "x-lib-audit-rules",
                "x-lib-group-verifier"));
    }

    /**
     * Infrastructure skill conditions mapping skill name
     * to a predicate on {@link ProjectConfig}.
     */
    static final Map<String, Predicate<ProjectConfig>>
            INFRA_SKILL_CONDITIONS;

    static {
        INFRA_SKILL_CONDITIONS = new LinkedHashMap<>();
        INFRA_SKILL_CONDITIONS.put(
                "setup-environment",
                c -> !"none".equals(
                        c.infrastructure().orchestrator()));
        INFRA_SKILL_CONDITIONS.put(
                "k8s-deployment",
                c -> "kubernetes".equals(
                        c.infrastructure().orchestrator()));
        INFRA_SKILL_CONDITIONS.put(
                "k8s-kustomize",
                c -> "kustomize".equals(
                        c.infrastructure().templating()));
        INFRA_SKILL_CONDITIONS.put(
                "dockerfile",
                c -> !"none".equals(
                        c.infrastructure().container()));
        INFRA_SKILL_CONDITIONS.put(
                "iac-terraform",
                c -> "terraform".equals(
                        c.infrastructure().iac()));
    }

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
            results.addAll(generateGroup(
                    engine, srcDir, outputDir,
                    filtered, subDir, context));
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
        if (!INFRA_GROUP.equals(group)) {
            return new ArrayList<>(skillNames);
        }
        List<String> filtered = new ArrayList<>();
        for (String name : skillNames) {
            Predicate<ProjectConfig> condition =
                    INFRA_SKILL_CONDITIONS.get(name);
            if (condition == null || condition.test(config)) {
                filtered.add(name);
            }
        }
        return filtered;
    }

    private List<String> generateGroup(
            TemplateEngine engine,
            Path srcDir,
            Path outputDir,
            List<String> skillNames,
            String subDir,
            Map<String, Object> context) {
        if (!Files.exists(srcDir)) {
            return List.of();
        }
        List<String> results = new ArrayList<>();
        for (String name : skillNames) {
            String dest = renderSkill(
                    engine, srcDir, outputDir,
                    name, subDir, context);
            if (dest != null) {
                results.add(dest);
            }
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
     * @param engine    the template engine
     * @param srcDir    the source template directory
     * @param outputDir the output directory
     * @param name      the skill name
     * @param subDir    optional subdirectory for nesting
     * @return the destination path, or null if missing
     */
    String renderSkill(
            TemplateEngine engine,
            Path srcDir,
            Path outputDir,
            String name,
            String subDir,
            Map<String, Object> context) {
        Path src = srcDir.resolve(name + ".md");
        if (!Files.exists(src)) {
            return null;
        }
        String content = readFile(src);
        String rendered = engine.replacePlaceholders(
                content, context);

        Path skillDir;
        if (subDir != null) {
            skillDir = outputDir.resolve(
                    SKILLS_OUTPUT + "/" + subDir
                            + "/" + name);
        } else {
            skillDir = outputDir.resolve(
                    SKILLS_OUTPUT + "/" + name);
        }
        CopyHelpers.ensureDirectory(skillDir);

        Path dest = skillDir.resolve(SKILL_MD);
        writeFile(dest, rendered);
        copyReferences(
                srcDir, skillDir, name, engine, context);
        return dest.toString();
    }

    /**
     * Copies a references directory for a skill if it
     * exists, applying placeholder replacement to all
     * markdown files.
     *
     * @param srcDir   the source template directory
     * @param skillDir the destination skill directory
     * @param name     the skill name
     * @param engine   the template engine
     */
    void copyReferences(
            Path srcDir,
            Path skillDir,
            String name,
            TemplateEngine engine,
            Map<String, Object> context) {
        Path refsDir = srcDir.resolve(
                "references/" + name);
        if (!Files.exists(refsDir)) {
            return;
        }
        Path destRefs = skillDir.resolve("references");
        CopyHelpers.copyDirectory(refsDir, destRefs);
        CopyHelpers.replacePlaceholdersInDir(
                destRefs, engine, context);
    }

    private static String readFile(Path path) {
        try {
            return Files.readString(
                    path, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException(
                    "Failed to read template: " + path, e);
        }
    }

    private static void writeFile(
            Path path, String content) {
        try {
            Files.writeString(
                    path, content, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException(
                    "Failed to write file: " + path, e);
        }
    }

    private static Path resolveClasspathResources() {
        return dev.iadev.util.ResourceResolver
                .resolveResourcesRoot(TEMPLATES_DIR);
    }
}
