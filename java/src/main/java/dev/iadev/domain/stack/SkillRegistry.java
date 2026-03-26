package dev.iadev.domain.stack;

import dev.iadev.model.InfraConfig;

import java.util.ArrayList;
import java.util.List;

/**
 * Skill registry constants and infrastructure pack rule builder.
 *
 * <p>Provides {@link #CORE_KNOWLEDGE_PACKS} — the list of knowledge packs
 * included in every generation, regardless of profile. Also provides
 * conditional infrastructure pack rules based on configuration.</p>
 *
 * <p>Zero external framework dependencies (RULE-007).</p>
 */
public final class SkillRegistry {

    private SkillRegistry() {
        // utility class
    }

    /**
     * Core knowledge packs included in every generation (12 entries).
     *
     * <p>These packs are always included regardless of stack or profile.</p>
     */
    public static final List<String> CORE_KNOWLEDGE_PACKS = List.of(
            "coding-standards",
            "architecture",
            "testing",
            "security",
            "compliance",
            "api-design",
            "observability",
            "resilience",
            "infrastructure",
            "protocols",
            "story-planning",
            "ci-cd-patterns"
    );

    /**
     * A conditional infrastructure pack rule.
     *
     * @param packName the pack name to include if condition is met
     * @param included whether the pack should be included
     */
    public record InfraPackRule(String packName, boolean included) {
    }

    /**
     * Builds conditional infrastructure pack rules.
     *
     * <p>Returns tuples of pack name and boolean indicating whether
     * each infrastructure pack should be included based on the
     * current infrastructure configuration.</p>
     *
     * @param infra the infrastructure configuration
     * @return list of infrastructure pack rules
     */
    public static List<InfraPackRule> buildInfraPackRules(InfraConfig infra) {
        List<InfraPackRule> rules = new ArrayList<>();
        rules.add(new InfraPackRule("k8s-deployment",
                "kubernetes".equals(infra.orchestrator())));
        rules.add(new InfraPackRule("k8s-kustomize",
                "kustomize".equals(infra.templating())));
        rules.add(new InfraPackRule("k8s-helm",
                "helm".equals(infra.templating())));
        rules.add(new InfraPackRule("dockerfile",
                !"none".equals(infra.container())));
        rules.add(new InfraPackRule("container-registry",
                !"none".equals(infra.registry())));
        rules.add(new InfraPackRule("iac-terraform",
                "terraform".equals(infra.iac())));
        rules.add(new InfraPackRule("iac-crossplane",
                "crossplane".equals(infra.iac())));
        return rules;
    }
}
