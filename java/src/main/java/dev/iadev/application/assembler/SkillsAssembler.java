package dev.iadev.application.assembler;

import dev.iadev.config.ContextBuilder;
import dev.iadev.domain.model.ProjectConfig;
import dev.iadev.template.TemplateEngine;

import dev.iadev.domain.model.ContextBudget;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
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

    /**
     * Top-level entries under {@code skills/} that are owned
     * by other assemblers and MUST never be pruned by this
     * class.
     *
     * <p>These directories are written by classes earlier in
     * the pipeline (RulesAssembler / CoreRulesWriter):</p>
     * <ul>
     *   <li>{@code knowledge-packs/} — from
     *       {@code RulesInfraConditionals} (cloud/k8s/container
     *       reference files).</li>
     *   <li>{@code database-patterns/} — from
     *       {@code RulesConditionals.copyDatabaseRefs} and
     *       {@code CoreKpRouting} (database KP is not in the
     *       SkillRegistry 17-pack set; its content is sourced
     *       from {@code knowledge/databases/} and
     *       {@code knowledge/core/11-database-principles.md}).
     *       </li>
     * </ul>
     *
     * <p>Other directories written by {@code CoreKpRouting}
     * (e.g., {@code architecture/}, {@code security/},
     * {@code testing/}) are already part of
     * {@link dev.iadev.domain.stack.SkillRegistry}
     * {@code .CORE_KNOWLEDGE_PACKS}, so they appear in the
     * generated set and do not need explicit protection.</p>
     */
    private static final Set<String> PROTECTED_NAMES =
            Set.of("knowledge-packs", "database-patterns");

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
        pruneStaleSkills(outputDir, generated);
        return generated;
    }

    /**
     * Removes top-level directories under
     * {@code outputDir/skills/} that are not present in
     * {@code generatedPaths} and are not in
     * {@link #PROTECTED_NAMES}.
     *
     * <p>Without this pass the output is additive-only:
     * skills renamed or removed in the source of truth
     * persist indefinitely as stale copies. Stray files
     * at the skills root are never touched — only
     * directories are candidates for removal.</p>
     *
     * @param outputDir       the root output directory
     * @param generatedPaths  absolute paths of every skill
     *                        written in this run
     */
    void pruneStaleSkills(
            Path outputDir, List<String> generatedPaths) {
        Path skillsDir = outputDir.resolve(SKILLS_OUTPUT);
        if (!Files.isDirectory(skillsDir)) {
            return;
        }
        Set<String> expected = expectedTopLevelNames(
                generatedPaths, skillsDir);
        try (Stream<Path> stream = Files.list(skillsDir)) {
            stream
                    .filter(Files::isDirectory)
                    .filter(p -> !isRetained(p, expected))
                    .forEach(SkillsAssembler::deleteStrictly);
        } catch (IOException e) {
            throw new UncheckedIOException(
                    "Failed to prune stale skills in: %s"
                            .formatted(skillsDir), e);
        }
    }

    /**
     * Attempts to delete {@code path} recursively and raises
     * an {@link UncheckedIOException} if the directory is still
     * present afterwards.
     *
     * <p>Wraps {@code CopyHelpers.deleteQuietly} so that silent
     * failures (insufficient permissions, open file handles on
     * Windows, EBUSY, etc.) do not leave a half-pruned output.
     * The stale directory surviving is a contract violation of
     * the destructive prune and must surface to the operator.</p>
     *
     * @param path the directory to delete
     * @throws UncheckedIOException if {@code path} exists after
     *                              the delete attempt
     */
    private static void deleteStrictly(Path path) {
        try {
            Files.walkFileTree(path, new SimpleFileVisitor<>() {
                @Override
                public FileVisitResult visitFile(
                        Path file, BasicFileAttributes attrs)
                        throws IOException {
                    Files.delete(file);
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult postVisitDirectory(
                        Path dir, IOException exc)
                        throws IOException {
                    if (exc != null) {
                        throw exc;
                    }
                    Files.delete(dir);
                    return FileVisitResult.CONTINUE;
                }
            });
        } catch (IOException e) {
            throw new UncheckedIOException(
                    "Failed to prune stale skill directory: "
                            + path,
                    e);
        }
    }

    private static boolean isRetained(
            Path dir, Set<String> expected) {
        String name = dir.getFileName().toString();
        return PROTECTED_NAMES.contains(name)
                || expected.contains(name);
    }

    private static Set<String> expectedTopLevelNames(
            List<String> generatedPaths, Path skillsDir) {
        Path root = skillsDir.toAbsolutePath().normalize();
        return generatedPaths.stream()
                .map(Path::of)
                .map(Path::toAbsolutePath)
                .map(Path::normalize)
                .filter(p -> p.startsWith(root)
                        && !p.equals(root))
                .map(p -> root.relativize(p)
                        .getName(0).toString())
                .collect(Collectors.toSet());
    }

    /**
     * Scans core skills directory and returns skill names.
     *
     * <p><b>Source-of-truth is hierarchical (10 category
     * subfolders), output is flat.</b> Skills physically live
     * at {@code core/{category}/{name}/SKILL.md} (e.g.
     * {@code core/plan/x-epic-create/SKILL.md}) but the
     * generated output is always {@code .claude/skills/{name}/
     * SKILL.md} — category subfolders are stripped.
     * The {@code lib/} subtree is the one exception and keeps
     * its {@code lib/} prefix in the emitted name. Traversal
     * is delegated to {@link SkillsHierarchyResolver}, which
     * uses the {@code SKILL.md} marker rule to transparently
     * support legacy flat layouts (backward-compatible) plus
     * exactly one category level of nesting. Deeper nesting
     * (e.g. {@code core/{cat}/{sub}/{name}/SKILL.md}) is NOT
     * discovered.</p>
     *
     * @return list of core skill names in scan order
     *         (top-level dirs sorted, then per-category
     *         sorted; NOT globally sorted across categories)
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
