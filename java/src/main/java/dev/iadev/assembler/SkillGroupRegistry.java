package dev.iadev.assembler;

import dev.iadev.model.ProjectConfig;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

/**
 * Registry of skill groups and infrastructure skill
 * conditions.
 *
 * <p>Extracted from {@link GithubSkillsAssembler} to reduce
 * class size and centralize skill group configuration in a
 * single dedicated location.</p>
 *
 * <p>All maps are unmodifiable after initialization. The
 * insertion order of {@link #SKILL_GROUPS} is preserved
 * to match TypeScript output byte-for-byte
 * (RULE-001).</p>
 *
 * @see GithubSkillsAssembler
 */
public final class SkillGroupRegistry {

    private SkillGroupRegistry() {
        // Utility class — no instantiation
    }

    /**
     * Skill groups mapping group name to template names.
     *
     * <p>Ordered by insertion to match TypeScript output.
     * Each group contains a list of skill template names
     * (without the .md extension).</p>
     */
    public static final Map<String, List<String>>
            SKILL_GROUPS;

    static {
        SKILL_GROUPS = new LinkedHashMap<>();
        SKILL_GROUPS.put("story", List.of(
                "x-story-epic", "x-story-create",
                "x-story-map", "x-story-epic-full",
                "story-planning",
                "x-jira-create-epic",
                "x-jira-create-stories"));
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
                "x-dependency-audit",
                "x-threat-model"));
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
                "api-design", "sre-practices",
                "release-management", "data-management",
                "performance-engineering",
                "feature-flags"));
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
     *
     * <p>Only infrastructure group skills are filtered;
     * other groups return the full list unchanged.</p>
     */
    public static final
            Map<String, Predicate<ProjectConfig>>
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
}
