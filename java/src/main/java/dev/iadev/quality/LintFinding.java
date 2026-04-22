package dev.iadev.quality;

import java.nio.file.Path;

/**
 * A single finding produced by {@link SkillSizeLinter}.
 *
 * <p>Immutable value object per story-0047-0003 §5.1. Fields:
 * <ul>
 *   <li>{@code path}: relative path of the offending SKILL.md
 *       under {@code java/src/main/resources/targets/claude/skills/}.</li>
 *   <li>{@code lineCount}: current line count of the file.</li>
 *   <li>{@code severity}: tier classification (see {@link Severity}).</li>
 *   <li>{@code hasReferencesDir}: whether a {@code references/}
 *       sibling directory exists next to the SKILL.md.</li>
 *   <li>{@code referencesNonEmpty}: whether that sibling contains
 *       at least one {@code .md} file other than {@code README.md}.</li>
 *   <li>{@code message}: human-readable actionable message
 *       per story §3.2 format.</li>
 * </ul>
 *
 * @param path SKILL.md relative path
 * @param lineCount file line count
 * @param severity classification tier
 * @param hasReferencesDir references/ sibling exists
 * @param referencesNonEmpty references/ contains &ge; 1 non-README
 *        markdown file
 * @param message actionable human-readable message
 */
public record LintFinding(
        Path path,
        int lineCount,
        Severity severity,
        boolean hasReferencesDir,
        boolean referencesNonEmpty,
        String message) {
}
