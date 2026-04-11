package dev.iadev.application.assembler;

import dev.iadev.domain.model.ProjectConfig;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;

/**
 * Conditional predicates applied to the
 * filesystem-derived skill groups consumed by
 * {@link GithubSkillsAssembler}.
 *
 * <p>Post EPIC-0036 / ADR-0003: the skill group <b>membership</b>
 * is discovered from the filesystem under
 * {@code targets/github-copilot/skills/}, removing the
 * hardcoded map that previously lived in
 * {@code SkillGroupRegistry}. The per-skill activation
 * predicates kept here are not filesystem data — they
 * depend on {@link ProjectConfig} feature gates and are
 * intentionally declared as code.</p>
 *
 * @see GithubSkillsAssembler
 */
final class SkillGroupConditions {

    private static final String INFRA_GROUP =
            "infrastructure";
    private static final String KP_GROUP =
            "knowledge-packs";
    private static final String REVIEW_GROUP = "review";

    private static final Set<String> CONTRACT_TYPES =
            Set.of("rest", "grpc",
                    "event-consumer", "event-producer",
                    "websocket");

    /**
     * Infrastructure skill conditions mapping skill name
     * to a predicate on {@link ProjectConfig}.
     */
    static final Map<String, Predicate<ProjectConfig>>
            INFRA_SKILL_CONDITIONS;

    static {
        INFRA_SKILL_CONDITIONS = new LinkedHashMap<>();
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
     */
    static final Map<String, Predicate<ProjectConfig>>
            KP_SKILL_CONDITIONS;

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
                "pci-dss-requirements",
                c -> c.security().frameworks()
                        .contains("pci-dss"));
    }

    /**
     * Review skill conditions mapping skill name to a
     * predicate on {@link ProjectConfig}.
     */
    static final Map<String, Predicate<ProjectConfig>>
            REVIEW_SKILL_CONDITIONS;

    static {
        REVIEW_SKILL_CONDITIONS = new LinkedHashMap<>();
        REVIEW_SKILL_CONDITIONS.put(
                "x-test-contract-lint",
                c -> c.interfaces().stream()
                        .anyMatch(i ->
                                CONTRACT_TYPES.contains(
                                        i.type())));
    }

    private SkillGroupConditions() {
        // Utility class — no instantiation
    }

    /**
     * Returns the predicate map for a given group.
     *
     * @param group the skill group name
     * @return predicates keyed by skill name, or an empty
     *         map when no conditional filtering applies
     */
    static Map<String, Predicate<ProjectConfig>>
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
}
