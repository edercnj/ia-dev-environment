package dev.iadev.assembler;

import dev.iadev.model.InfraConfig;
import dev.iadev.model.ProjectConfig;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Pure agent selection functions based on project config
 * feature gates.
 *
 * <p>These functions evaluate config conditions and return
 * agent filenames. No file I/O — consumed by
 * {@link AgentsAssembler} for assembly decisions.</p>
 *
 * <p>Selection categories:
 * <ul>
 *   <li>Data agents — database-engineer when database
 *       is configured</li>
 *   <li>Infrastructure agents — observability-engineer,
 *       devops-engineer based on infra config</li>
 *   <li>Interface agents — api-engineer when REST, gRPC,
 *       or GraphQL interfaces present</li>
 *   <li>Event agents — event-engineer when event-driven
 *       or event interfaces present</li>
 * </ul>
 *
 * <p>Example usage:
 * <pre>{@code
 * List<String> conditionals =
 *     AgentsSelection.selectConditionalAgents(config);
 * String devAgent =
 *     AgentsSelection.selectDeveloperAgent(config);
 * }</pre>
 * </p>
 *
 * @see AgentsAssembler
 * @see ConditionEvaluator
 */
public final class AgentsSelection {

    private static final String MD_EXTENSION = ".md";

    private AgentsSelection() {
        // utility class
    }

    /**
     * Evaluates all feature gates and returns conditional
     * agent filenames.
     *
     * @param config the project configuration
     * @return aggregated list of conditional agent filenames
     */
    public static List<String> selectConditionalAgents(
            ProjectConfig config) {
        List<String> agents = new ArrayList<>();
        agents.addAll(selectDataAgents(config));
        agents.addAll(selectInfraAgents(config));
        agents.addAll(selectInterfaceAgents(config));
        agents.addAll(selectEventAgents(config));
        return agents;
    }

    /**
     * Returns the developer agent filename for the project
     * language.
     *
     * @param config the project configuration
     * @return developer agent filename (e.g. "java-developer.md")
     */
    public static String selectDeveloperAgent(
            ProjectConfig config) {
        String langName = config.language().name();
        return langName + "-developer" + MD_EXTENSION;
    }

    /**
     * Builds the complete list of checklist injection rules.
     *
     * @param config the project configuration
     * @return list of checklist rules with active flags
     */
    public static List<ChecklistRule> buildChecklistRules(
            ProjectConfig config) {
        List<ChecklistRule> rules = new ArrayList<>();
        rules.addAll(securityChecklistRules(
                config.security().frameworks()));
        rules.addAll(apiChecklistRules(config));
        rules.addAll(devopsChecklistRules(
                config.infrastructure()));
        return rules;
    }

    /**
     * Derives the marker string from a checklist filename.
     *
     * <p>Example: "pci-dss-security.md" becomes
     * "&lt;!-- PCI_DSS_SECURITY --&gt;"</p>
     *
     * @param checklistFile the checklist filename
     * @return the HTML comment marker string
     */
    public static String checklistMarker(
            String checklistFile) {
        String name = checklistFile
                .replace(MD_EXTENSION, "")
                .toUpperCase()
                .replace("-", "_");
        return "<!-- " + name + " -->";
    }

    private static List<String> selectDataAgents(
            ProjectConfig config) {
        if (!"none".equals(
                config.data().database().name())) {
            return List.of("database-engineer.md");
        }
        return List.of();
    }

    private static List<String> selectInfraAgents(
            ProjectConfig config) {
        List<String> agents = new ArrayList<>();
        InfraConfig infra = config.infrastructure();

        if (!"none".equals(
                infra.observability().tool())) {
            agents.add("observability-engineer.md");
        }

        boolean hasDevops =
                !"none".equals(infra.container())
                        || !"none".equals(
                                infra.orchestrator())
                        || !"none".equals(infra.iac())
                        || !"none".equals(
                                infra.serviceMesh());
        if (hasDevops) {
            agents.add("devops-engineer.md");
        }
        return agents;
    }

    private static List<String> selectInterfaceAgents(
            ProjectConfig config) {
        if (hasAnyInterface(config,
                "rest", "grpc", "graphql")) {
            return List.of("api-engineer.md");
        }
        return List.of();
    }

    private static List<String> selectEventAgents(
            ProjectConfig config) {
        boolean hasEvents =
                config.architecture().eventDriven()
                        || hasAnyInterface(config,
                                "event-consumer",
                                "event-producer");
        if (hasEvents) {
            return List.of("event-engineer.md");
        }
        return List.of();
    }

    private static List<ChecklistRule> securityChecklistRules(
            List<String> frameworks) {
        boolean hasPrivacy = frameworks.contains("lgpd")
                || frameworks.contains("gdpr");
        return List.of(
                new ChecklistRule(
                        "security-engineer.md",
                        "pci-dss-security.md",
                        frameworks.contains("pci-dss")),
                new ChecklistRule(
                        "security-engineer.md",
                        "privacy-security.md",
                        hasPrivacy),
                new ChecklistRule(
                        "security-engineer.md",
                        "hipaa-security.md",
                        frameworks.contains("hipaa")),
                new ChecklistRule(
                        "security-engineer.md",
                        "sox-security.md",
                        frameworks.contains("sox")));
    }

    private static List<ChecklistRule> apiChecklistRules(
            ProjectConfig config) {
        return List.of(
                new ChecklistRule(
                        "api-engineer.md",
                        "grpc-api.md",
                        hasInterface(config, "grpc")),
                new ChecklistRule(
                        "api-engineer.md",
                        "graphql-api.md",
                        hasInterface(config, "graphql")),
                new ChecklistRule(
                        "api-engineer.md",
                        "websocket-api.md",
                        hasInterface(config, "websocket")));
    }

    private static List<ChecklistRule> devopsChecklistRules(
            InfraConfig infra) {
        return List.of(
                new ChecklistRule(
                        "devops-engineer.md",
                        "helm-devops.md",
                        "helm".equals(infra.templating())),
                new ChecklistRule(
                        "devops-engineer.md",
                        "iac-devops.md",
                        !"none".equals(infra.iac())),
                new ChecklistRule(
                        "devops-engineer.md",
                        "mesh-devops.md",
                        !"none".equals(infra.serviceMesh())),
                new ChecklistRule(
                        "devops-engineer.md",
                        "registry-devops.md",
                        !"none".equals(infra.registry())));
    }

    private static boolean hasInterface(
            ProjectConfig config, String type) {
        return config.interfaces().stream()
                .anyMatch(i -> type.equals(i.type()));
    }

    private static boolean hasAnyInterface(
            ProjectConfig config, String... types) {
        Set<String> typeSet = Set.of(types);
        return config.interfaces().stream()
                .anyMatch(i -> typeSet.contains(i.type()));
    }

    /**
     * A (targetAgent, checklistFile, condition) tuple for
     * checklist injection.
     *
     * @param agent     the agent file to inject into
     * @param checklist the checklist file to inject
     * @param active    whether this rule is active
     */
    public record ChecklistRule(
            String agent,
            String checklist,
            boolean active) {
    }
}
