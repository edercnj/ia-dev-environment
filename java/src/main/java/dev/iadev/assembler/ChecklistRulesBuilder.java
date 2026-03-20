package dev.iadev.assembler;

import dev.iadev.model.InfraConfig;
import dev.iadev.model.ProjectConfig;

import java.util.ArrayList;
import java.util.List;

/**
 * Builds checklist injection rules from project
 * configuration.
 *
 * <p>Extracted from {@link AgentsSelection} to keep both
 * classes under 250 lines per RULE-004.</p>
 *
 * @see AgentsSelection
 * @see AgentsAssembler
 */
final class ChecklistRulesBuilder {

    private ChecklistRulesBuilder() {
        // utility class
    }

    /**
     * Builds the complete list of checklist injection
     * rules.
     *
     * @param config the project configuration
     * @return list of checklist rules with active flags
     */
    static List<AgentsSelection.ChecklistRule>
            buildChecklistRules(ProjectConfig config) {
        List<AgentsSelection.ChecklistRule> rules =
                new ArrayList<>();
        rules.addAll(securityChecklistRules(
                config.security().frameworks()));
        rules.addAll(apiChecklistRules(config));
        rules.addAll(devopsChecklistRules(
                config.infrastructure()));
        return rules;
    }

    private static List<AgentsSelection.ChecklistRule>
            securityChecklistRules(
                    List<String> frameworks) {
        boolean hasPrivacy =
                frameworks.contains("lgpd")
                        || frameworks.contains("gdpr");
        return List.of(
                new AgentsSelection.ChecklistRule(
                        "security-engineer.md",
                        "pci-dss-security.md",
                        frameworks.contains("pci-dss")),
                new AgentsSelection.ChecklistRule(
                        "security-engineer.md",
                        "privacy-security.md",
                        hasPrivacy),
                new AgentsSelection.ChecklistRule(
                        "security-engineer.md",
                        "hipaa-security.md",
                        frameworks.contains("hipaa")),
                new AgentsSelection.ChecklistRule(
                        "security-engineer.md",
                        "sox-security.md",
                        frameworks.contains("sox")));
    }

    private static List<AgentsSelection.ChecklistRule>
            apiChecklistRules(ProjectConfig config) {
        return List.of(
                new AgentsSelection.ChecklistRule(
                        "api-engineer.md",
                        "grpc-api.md",
                        hasInterface(config, "grpc")),
                new AgentsSelection.ChecklistRule(
                        "api-engineer.md",
                        "graphql-api.md",
                        hasInterface(config, "graphql")),
                new AgentsSelection.ChecklistRule(
                        "api-engineer.md",
                        "websocket-api.md",
                        hasInterface(config, "websocket")));
    }

    private static List<AgentsSelection.ChecklistRule>
            devopsChecklistRules(InfraConfig infra) {
        return List.of(
                new AgentsSelection.ChecklistRule(
                        "devops-engineer.md",
                        "helm-devops.md",
                        "helm".equals(infra.templating())),
                new AgentsSelection.ChecklistRule(
                        "devops-engineer.md",
                        "iac-devops.md",
                        !"none".equals(infra.iac())),
                new AgentsSelection.ChecklistRule(
                        "devops-engineer.md",
                        "mesh-devops.md",
                        !"none".equals(
                                infra.serviceMesh())),
                new AgentsSelection.ChecklistRule(
                        "devops-engineer.md",
                        "registry-devops.md",
                        !"none".equals(infra.registry())));
    }

    private static boolean hasInterface(
            ProjectConfig config, String type) {
        return config.interfaces().stream()
                .anyMatch(i -> type.equals(i.type()));
    }
}
