package dev.iadev.application.assembler;

import dev.iadev.config.ContextBuilder;
import dev.iadev.domain.model.ProjectConfig;
import dev.iadev.template.TemplateEngine;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import java.util.function.Predicate;
import java.util.stream.Stream;

/**
 * Assembles {@code .github/skills/} from templates for
 * GitHub Copilot, mirroring the Claude skills structure.
 *
 * <p>Post EPIC-0036 / ADR-0003: skill group membership is
 * discovered by walking
 * {@code targets/github-copilot/skills/{group}/*.md} at
 * assembly time. The previous hardcoded registry
 * ({@code SkillGroupRegistry}) was deleted to eliminate the
 * dual source of truth between the filesystem and a Java
 * map. Per-skill activation predicates live in
 * {@link SkillGroupConditions}.</p>
 *
 * <p>The {@code SKILL_GROUPS} field is initialized eagerly
 * from the classpath on class load and preserved in a
 * {@link LinkedHashMap} so iteration order is stable.
 * Within a group, skill names are sorted alphabetically
 * so the assembly output is deterministic across
 * platforms.</p>
 *
 * @see Assembler
 * @see SkillGroupConditions
 */
public final class GithubSkillsAssembler
        implements Assembler {

    private static final String TEMPLATES_DIR =
            "targets/github-copilot/skills";
    private static final String SKILL_MD = "SKILL.md";
    private static final String SKILLS_OUTPUT = "skills";
    private static final String MD_SUFFIX = ".md";
    private static final String REFERENCES_DIR =
            "references";

    /**
     * Groups whose skills are nested under a subdirectory
     * in the output (e.g., {@code skills/lib/{name}/}).
     */
    static final Set<String> NESTED_GROUPS =
            Set.of("lib");

    /**
     * Explicit ordering used when the filesystem yields a
     * group name. Unknown groups fall back to alphabetical
     * order after the listed entries.
     */
    private static final List<String> GROUP_ORDER =
            List.of(
                    "story",
                    "dev",
                    "review",
                    "testing",
                    "infrastructure",
                    "knowledge-packs",
                    "git-troubleshooting",
                    "lib");

    /**
     * Filesystem-derived skill groups. Keys are group
     * names; values are alphabetically sorted skill names
     * (without the {@code .md} suffix). Loaded once on
     * class load using the classpath resources root.
     */
    static final Map<String, List<String>> SKILL_GROUPS =
            discoverSkillGroups(
                    resolveClasspathResources());

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
     * <p>Walks every discovered group, applying feature
     * gates for infrastructure, knowledge-packs, and
     * review and nesting for the lib group.</p>
     */
    @Override
    public List<String> assemble(
            ProjectConfig config,
            TemplateEngine engine,
            Path outputDir) {
        Map<String, Object> context =
                ContextBuilder.buildContext(config);
        Map<String, List<String>> groups =
                discoverSkillGroups(resourcesDir);
        List<String> results = new ArrayList<>();
        for (Map.Entry<String, List<String>> entry
                : groups.entrySet()) {
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
     * Discovers skill groups by walking
     * {@code targets/github-copilot/skills/*}.
     *
     * <p>Each top-level directory is a group. Each
     * {@code .md} file inside (excluding
     * {@code references/}) is a skill. Groups are emitted
     * in {@link #GROUP_ORDER} order when known, then any
     * remaining groups alphabetically.</p>
     *
     * @param resourcesDir the resources root
     * @return the group → skill names map
     */
    static Map<String, List<String>> discoverSkillGroups(
            Path resourcesDir) {
        Path skillsDir = resourcesDir.resolve(TEMPLATES_DIR);
        if (!Files.exists(skillsDir)
                || !Files.isDirectory(skillsDir)) {
            return Collections.emptyMap();
        }
        Map<String, List<String>> unordered = new TreeMap<>();
        try (Stream<Path> stream = Files.list(skillsDir)) {
            stream.filter(Files::isDirectory)
                    .forEach(groupDir -> {
                        String group = groupDir
                                .getFileName().toString();
                        unordered.put(
                                group,
                                listSkillsIn(groupDir));
                    });
        } catch (IOException e) {
            throw new UncheckedIOException(
                    "Failed to list github skills dir: "
                            + skillsDir, e);
        }
        Map<String, List<String>> ordered =
                new LinkedHashMap<>();
        for (String known : GROUP_ORDER) {
            List<String> skills = unordered.remove(known);
            if (skills != null) {
                ordered.put(known, skills);
            }
        }
        ordered.putAll(unordered);
        return Collections.unmodifiableMap(ordered);
    }

    private static List<String> listSkillsIn(Path groupDir) {
        try (Stream<Path> stream = Files.list(groupDir)) {
            List<String> names = new ArrayList<>();
            stream.filter(Files::isRegularFile)
                    .filter(p -> p.getFileName().toString()
                            .endsWith(MD_SUFFIX))
                    .map(p -> {
                        String file =
                                p.getFileName().toString();
                        return file.substring(
                                0,
                                file.length()
                                        - MD_SUFFIX.length());
                    })
                    .sorted()
                    .forEach(names::add);
            return Collections.unmodifiableList(names);
        } catch (IOException e) {
            throw new UncheckedIOException(
                    "Failed to list group: " + groupDir, e);
        }
    }

    /**
     * Filters skills based on config predicates declared
     * in {@link SkillGroupConditions}.
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
                SkillGroupConditions.conditionsForGroup(
                        group);
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
        Path src = ctx.srcDir().resolve(name + MD_SUFFIX);
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
     * exists, applying placeholder replacement to markdown
     * files.
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
                REFERENCES_DIR + "/" + name);
        if (!Files.exists(refsDir)) {
            return;
        }
        Path destRefs = skillDir.resolve(REFERENCES_DIR);
        CopyHelpers.copyDirectory(refsDir, destRefs);
        CopyHelpers.replacePlaceholdersInDir(
                destRefs, engine, ctx.context());
    }

    private static Path resolveClasspathResources() {
        return dev.iadev.util.ResourceResolver
                .resolveResourcesRoot(TEMPLATES_DIR, 3);
    }
}
