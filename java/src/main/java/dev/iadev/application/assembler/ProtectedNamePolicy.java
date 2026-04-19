package dev.iadev.application.assembler;

import java.util.Set;

/**
 * Policy for skill output directory names that are owned by
 * other assemblers and MUST never be pruned by
 * {@link SkillsAssembler}.
 *
 * <p>Extracted from {@code SkillsAssembler} (audit M-002) so
 * the "which top-level names are reserved" guardrail is a
 * single-responsibility data class with its own test surface.
 * Guarded directories are written by classes earlier in the
 * pipeline (RulesAssembler / CoreRulesWriter / RulesConditionals)
 * and their survival through a regeneration pass is a hard
 * invariant validated by {@link SkillsAssemblerPruneTest}.</p>
 *
 * <p>The canonical reserved set is:</p>
 * <ul>
 *   <li>{@code knowledge-packs/} — from
 *       {@code RulesInfraConditionals} (cloud / k8s / container
 *       reference files).</li>
 *   <li>{@code database-patterns/} — from
 *       {@code RulesConditionals.copyDatabaseRefs} and
 *       {@code CoreKpRouting}. The database KP is not in the
 *       {@code SkillRegistry} 17-pack set; its content is
 *       sourced from {@code knowledge/databases/} and
 *       {@code knowledge/core/11-database-principles.md}.</li>
 * </ul>
 *
 * <p>Other directories written by {@code CoreKpRouting}
 * (e.g., {@code architecture/}, {@code security/},
 * {@code testing/}) are already part of
 * {@link dev.iadev.domain.stack.SkillRegistry}
 * {@code .CORE_KNOWLEDGE_PACKS}, so they appear in the
 * generated set and do not need explicit protection.</p>
 *
 * @see SkillsAssembler
 */
final class ProtectedNamePolicy {

    /**
     * Canonical set of reserved top-level skill directory
     * names. Order is irrelevant — the contract is membership,
     * not iteration.
     */
    static final Set<String> PROTECTED_NAMES =
            Set.of("knowledge-packs", "database-patterns");

    private ProtectedNamePolicy() {
        // utility class
    }

    /**
     * Returns {@code true} when {@code name} identifies a
     * top-level skills directory that is owned by another
     * assembler and MUST survive a prune pass.
     *
     * @param name the directory name (no slashes, single
     *             component)
     * @return {@code true} iff {@code name} is reserved
     */
    static boolean isProtected(String name) {
        return PROTECTED_NAMES.contains(name);
    }
}
