package dev.iadev.application.assembler;

/**
 * Metadata extracted from a generated skill's
 * {@code SKILL.md} frontmatter.
 *
 * @param name          the skill name (from frontmatter or dir)
 * @param description   the skill description
 * @param userInvocable whether the skill appears in the
 *                      {@code /} menu
 */
public record SkillInfo(
        String name,
        String description,
        boolean userInvocable) {
}
