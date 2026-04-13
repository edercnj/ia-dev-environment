package dev.iadev.application.assembler;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Resolves skill directories under a hierarchical
 * source-of-truth layout while preserving a flat output.
 *
 * <p>Skills physically live under <strong>single-level</strong>
 * category subfolders (e.g. {@code core/plan/x-story-epic/})
 * but their emitted output path is always
 * {@code .claude/skills/{name}/}. Nested categories beyond one
 * level are not scanned. The {@code lib/} subtree is the one
 * exception and retains its {@code lib/} prefix in the emitted
 * name. A directory qualifies as a skill directory iff it
 * contains a {@code SKILL.md} file.</p>
 *
 * @see SkillsAssembler
 */
final class SkillsHierarchyResolver {

    private static final String SKILL_MD = "SKILL.md";
    private static final String LIB_DIR = "lib";

    private SkillsHierarchyResolver() {
        // utility class
    }

    /**
     * Checks whether a directory is a skill directory.
     */
    static boolean isSkillDirectory(Path dir) {
        return Files.isRegularFile(dir.resolve(SKILL_MD));
    }

    /**
     * Lists skill names under a root directory, supporting
     * flat layout and a single category level of nesting.
     *
     * <p>Exactly two layouts are recognized:
     * <ul>
     *   <li>{@code rootDir/{skill}/SKILL.md} (flat)</li>
     *   <li>{@code rootDir/{category}/{skill}/SKILL.md}
     *       (one category level)</li>
     * </ul>
     * Deeper nesting (e.g.
     * {@code rootDir/{category}/{sub}/{skill}/SKILL.md}) is
     * NOT discovered. The {@code lib/} subfolder is
     * special-cased: its immediate children that contain
     * {@code SKILL.md} are emitted with the {@code lib/}
     * prefix preserved.</p>
     *
     * @param rootDir the core or conditional root directory
     * @return list of skill names, in scan order (top-level
     *         directories are sorted by name; within each
     *         category, skills are sorted by name; skills
     *         are NOT globally sorted across categories)
     */
    static List<String> listSkillNames(Path rootDir) {
        List<String> skills = new ArrayList<>();
        if (!Files.isDirectory(rootDir)) {
            return skills;
        }
        for (Path entry :
                SkillsCopyHelper.listDirsSorted(rootDir)) {
            String name = entry.getFileName().toString();
            if (LIB_DIR.equals(name)) {
                addLibSkills(entry, skills);
            } else if (isSkillDirectory(entry)) {
                skills.add(name);
            } else {
                addCategorySkills(entry, skills);
            }
        }
        return skills;
    }

    private static void addLibSkills(
            Path libDir, List<String> skills) {
        for (Path sub :
                SkillsCopyHelper.listDirsSorted(libDir)) {
            if (!isSkillDirectory(sub)) {
                continue;
            }
            skills.add(LIB_DIR + "/"
                    + sub.getFileName().toString());
        }
    }

    /**
     * Adds direct child skills of a category directory.
     * Only one level deep — nested category subfolders are
     * intentionally not traversed.
     */
    private static void addCategorySkills(
            Path categoryDir, List<String> skills) {
        for (Path sub :
                SkillsCopyHelper.listDirsSorted(
                        categoryDir)) {
            if (isSkillDirectory(sub)) {
                skills.add(sub.getFileName().toString());
            }
        }
    }

    /**
     * Resolves the on-disk path of a named skill within
     * a root directory. Supports flat layout, hierarchical
     * layout, and the {@code lib/} prefix.
     *
     * @param rootDir   the core or conditional root
     * @param skillName the skill name (may carry lib/ prefix)
     * @return the resolved skill directory; may not exist
     *         when the skill cannot be located
     */
    static Path resolveSkillPath(
            Path rootDir, String skillName) {
        if (skillName.startsWith(LIB_DIR + "/")) {
            return rootDir.resolve(skillName);
        }
        Path flat = rootDir.resolve(skillName);
        if (isSkillDirectory(flat)) {
            return flat;
        }
        if (!Files.isDirectory(rootDir)) {
            return flat;
        }
        for (Path category :
                SkillsCopyHelper.listDirsSorted(rootDir)) {
            if (LIB_DIR.equals(
                    category.getFileName().toString())) {
                continue;
            }
            Path candidate = category.resolve(skillName);
            if (isSkillDirectory(candidate)) {
                return candidate;
            }
        }
        return flat;
    }
}
