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
 * <p>Policy and path-resolution responsibilities are
 * delegated to cohesive helpers (audit M-002):</p>
 * <ul>
 *   <li>{@link ProtectedNamePolicy} — owns the reserved
 *       top-level names set used by the prune pass.</li>
 *   <li>{@link SkillCategoryResolver} — owns skill-name to
 *       source-path resolution across the EPIC-0036
 *       category-aware layout (flat output, hierarchical
 *       source).</li>
 * </ul>
 *
 * @see Assembler
 * @see SkillsSelection
 * @see SkillsCopyHelper
 * @see ProtectedNamePolicy
 * @see SkillCategoryResolver
 */
public final class SkillsAssembler implements Assembler {

    private static final String SKILLS_TEMPLATES_DIR =
            "targets/claude/skills";
    private static final String CORE_DIR = "core";
    private static final String CONDITIONAL_DIR =
            "conditional";
    private static final String SHARED_DIR = "_shared";
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
        // EPIC-0051 (ADR-0013): knowledge packs moved to
        // .claude/knowledge/ under KnowledgeAssembler; no
        // longer emitted by SkillsAssembler.
        assembleShared(outputDir)
                .ifPresent(generated::add);
        pruneStaleSkills(outputDir, generated);
        return generated;
    }

    /**
     * Copies the source-of-truth {@code _shared/} directory
     * (peer of {@code core/} / {@code conditional/} /
     * {@code knowledge-packs/}) to
     * {@code outputDir/skills/_shared/} verbatim.
     *
     * <p>Story-0047-0001 introduces {@code _shared/} for
     * cross-cutting Markdown snippets referenced by
     * consumer skills via Markdown relative links
     * (per ADR-0011 — shared-snippets inclusion strategy).
     * For links like {@code ../_shared/error-handling.md}
     * in a generated {@code skills/x-git-commit/SKILL.md}
     * to resolve at runtime, the target directory must ship
     * in the same output tree.</p>
     *
     * <p>When the source {@code _shared/} directory does not
     * exist, this method is a no-op and returns
     * {@link Optional#empty()}.</p>
     *
     * <p>Returning the output path (when present) lets
     * {@link #pruneStaleSkills(Path, List)} keep the
     * directory across reruns.</p>
     *
     * @param outputDir the output root directory
     * @return the absolute output path of the copied
     *         {@code _shared/} directory, or empty when the
     *         source directory is absent
     */
    private Optional<String> assembleShared(Path outputDir) {
        Path sharedSrc = sharedPath();
        if (!Files.exists(sharedSrc)
                || !Files.isDirectory(sharedSrc)) {
            return Optional.empty();
        }
        Path dest = outputDir.resolve(
                SKILLS_OUTPUT + "/" + SHARED_DIR);
        CopyHelpers.copyDirectory(sharedSrc, dest);
        return Optional.of(dest.toString());
    }

    private Path sharedPath() {
        return resourcesDir.resolve(
                SKILLS_TEMPLATES_DIR + "/" + SHARED_DIR);
    }

    /**
     * Removes top-level directories under
     * {@code outputDir/skills/} that are not present in
     * {@code generatedPaths} and are not reserved by
     * {@link ProtectedNamePolicy}.
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
        return ProtectedNamePolicy.isProtected(name)
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
     * Scans the core skills directory and returns skill names.
     *
     * <p>Delegates to {@link SkillCategoryResolver} so the
     * hierarchical-source / flat-output mapping is owned by a
     * single helper. See
     * {@link SkillCategoryResolver#listSkills(Path)} for scan
     * order and sort contract.</p>
     *
     * @return list of core skill names in scan order
     */
    List<String> selectCoreSkills() {
        return SkillCategoryResolver.listSkills(corePath());
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

    private String copyCoreSkill(
            String skillName,
            Path outputDir,
            TemplateEngine engine,
            java.util.Map<String, Object> context) {
        Path src = SkillCategoryResolver.resolveSourcePath(
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
        Path src = SkillCategoryResolver.resolveSourcePath(
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
                .resolveResourceDir("shared")
                .getParent();
    }
}
