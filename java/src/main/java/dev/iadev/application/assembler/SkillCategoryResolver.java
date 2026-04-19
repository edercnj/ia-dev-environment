package dev.iadev.application.assembler;

import java.nio.file.Path;
import java.util.List;

/**
 * Resolves skill names and source paths across the EPIC-0036
 * category-aware source-of-truth layout.
 *
 * <p>Extracted from {@code SkillsAssembler} (audit M-002) so
 * the "skill lives in a category subfolder, output is flat"
 * mapping is a single-responsibility helper with its own test
 * surface. Under EPIC-0036, skills physically live under one
 * level of category subfolders (e.g.
 * {@code core/plan/x-epic-create/SKILL.md}) but emit to the
 * flat output path {@code .claude/skills/{name}/}. The
 * {@code lib/} subtree is the one exception — it keeps its
 * {@code lib/} prefix in the emitted name.</p>
 *
 * <p>This class is a thin adapter over
 * {@link SkillsHierarchyResolver}: it provides the root-aware
 * operations callers need (list skills under a root, resolve
 * the on-disk path of a named skill) while keeping the
 * low-level traversal logic single-sourced.</p>
 *
 * @see SkillsHierarchyResolver
 * @see SkillsAssembler
 */
final class SkillCategoryResolver {

    private SkillCategoryResolver() {
        // utility class
    }

    /**
     * Lists all skill names under a root directory, honouring
     * one level of category nesting and the {@code lib/}
     * prefix exception.
     *
     * <p>Delegates traversal to
     * {@link SkillsHierarchyResolver#listSkillNames(Path)};
     * scan order and sort contract are preserved verbatim.</p>
     *
     * @param rootDir the core or conditional root directory
     * @return list of skill names in discovery order
     */
    static List<String> listSkills(Path rootDir) {
        return SkillsHierarchyResolver.listSkillNames(rootDir);
    }

    /**
     * Resolves the on-disk source path of a named skill
     * within a root directory.
     *
     * <p>Delegates to
     * {@link SkillsHierarchyResolver#resolveSkillPath(Path,
     * String)}; supports flat layout, one level of category
     * nesting, and the {@code lib/} prefix.</p>
     *
     * @param rootDir   the core or conditional root
     * @param skillName the skill name (may carry lib/ prefix)
     * @return the resolved skill directory; may not exist
     *         when the skill cannot be located
     */
    static Path resolveSourcePath(
            Path rootDir, String skillName) {
        return SkillsHierarchyResolver.resolveSkillPath(
                rootDir, skillName);
    }
}
