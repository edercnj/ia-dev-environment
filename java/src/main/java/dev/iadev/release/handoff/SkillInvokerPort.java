package dev.iadev.release.handoff;

/**
 * Port for invoking a sibling Claude Code skill via the
 * {@code Skill} tool. Abstracted so that tests can inject
 * stubs without triggering a real skill execution.
 *
 * <p>The adapter implementation is expected to wrap the
 * Claude Code {@code Skill(skill, args)} tool call and
 * translate any runtime failure into a
 * {@link SkillInvocationException}.
 *
 * <p>Introduced by story-0039-0011.
 */
@FunctionalInterface
public interface SkillInvokerPort {

    /**
     * Invokes the named skill with the given argument string.
     *
     * @param skill skill name (e.g. {@code "x-pr-fix"})
     * @param args  argument string passed to the skill
     * @throws SkillInvocationException when the skill fails
     *         (wraps any underlying error; carries context)
     */
    void invoke(String skill, String args);
}
