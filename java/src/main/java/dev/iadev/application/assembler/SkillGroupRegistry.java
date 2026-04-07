package dev.iadev.application.assembler;

import dev.iadev.domain.model.ProjectConfig;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
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
                "x-mcp-recommend",
                "x-perf-profile",
                "x-setup-dev-environment",
                "x-ci-cd-generate",
                "x-security-pipeline"));
        SKILL_GROUPS.put("review", List.of(
                "x-review", "x-review-api", "x-review-pr",
                "x-review-grpc", "x-review-events",
                "x-review-gateway",
                "x-codebase-audit",
                "x-dependency-audit",
                "x-supply-chain-audit",
                "x-owasp-scan",
                "x-spec-drift-check",
                "x-threat-model",
                "x-hardening-eval",
                "x-runtime-protection",
                "x-security-dashboard",
                "x-contract-lint"));
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
                "feature-flags", "disaster-recovery",
                "finops", "patterns-outbox",
                "pci-dss-requirements"));
        SKILL_GROUPS.put("git-troubleshooting", List.of(
                "x-git-push", "x-ops-troubleshoot",
                "x-ops-incident",
                "x-fix-pr-comments",
                "x-fix-epic-pr-comments",
                "x-changelog",
                "x-release"));
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

    /**
     * Knowledge-pack skill conditions mapping skill name
     * to a predicate on {@link ProjectConfig}.
     *
     * <p>Only knowledge-packs group skills listed here are
     * filtered; unlisted packs are always included.</p>
     */
    public static final
            Map<String, Predicate<ProjectConfig>>
                    KP_SKILL_CONDITIONS;

    private static final String KP_GROUP =
            "knowledge-packs";

    static {
        KP_SKILL_CONDITIONS = new LinkedHashMap<>();
        KP_SKILL_CONDITIONS.put(
                "disaster-recovery",
                c -> {
                    String ct = c.infrastructure()
                            .container();
                    return ct != null && !ct.isBlank()
                            && !"none".equals(ct);
                });
        KP_SKILL_CONDITIONS.put(
                "finops",
                c -> {
                    String p = c.infrastructure()
                            .cloudProvider();
                    return p != null && !p.isBlank()
                            && !"none".equals(p);
                });
        KP_SKILL_CONDITIONS.put(
                "patterns-outbox",
                c -> c.architecture().outboxPattern());
        KP_SKILL_CONDITIONS.put(
                "pci-dss-requirements",
                c -> c.security().frameworks()
                        .contains("pci-dss"));
    }

    private static final Set<String> CONTRACT_TYPES =
            Set.of("rest", "grpc",
                    "event-consumer", "event-producer",
                    "websocket");

    /**
     * Review skill conditions mapping skill name to a
     * predicate on {@link ProjectConfig}.
     *
     * <p>Only review group skills listed here are filtered;
     * unlisted skills are always included.</p>
     */
    public static final
            Map<String, Predicate<ProjectConfig>>
                    REVIEW_SKILL_CONDITIONS;

    private static final String REVIEW_GROUP = "review";

    static {
        REVIEW_SKILL_CONDITIONS = new LinkedHashMap<>();
        REVIEW_SKILL_CONDITIONS.put(
                "x-contract-lint",
                c -> c.interfaces().stream()
                        .anyMatch(i ->
                                CONTRACT_TYPES.contains(
                                        i.type())));
    }

    /**
     * Returns the set of groups that have conditions.
     *
     * @return set of group names with conditions
     */
    public static Map<String, Predicate<ProjectConfig>>
            conditionsForGroup(String group) {
        if (INFRA_GROUP.equals(group)) {
            return INFRA_SKILL_CONDITIONS;
        }
        if (KP_GROUP.equals(group)) {
            return KP_SKILL_CONDITIONS;
        }
        if (REVIEW_GROUP.equals(group)) {
            return REVIEW_SKILL_CONDITIONS;
        }
        return Map.of();
    }

    private static final String INFRA_GROUP =
            "infrastructure";
}
