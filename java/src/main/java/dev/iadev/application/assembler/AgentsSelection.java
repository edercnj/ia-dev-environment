package dev.iadev.application.assembler;

import dev.iadev.domain.model.InfraConfig;
import dev.iadev.domain.model.ProjectConfig;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Pure agent selection functions based on project config
 * feature gates.
 *
 * <p>Checklist rule building is delegated to
 * {@link ChecklistRulesBuilder}.</p>
 *
 * @see AgentsAssembler
 * @see ChecklistRulesBuilder
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
        agents.addAll(selectSecurityAgents(config));
        return agents;
    }

    /**
     * Returns the developer agent filename for the project
     * language.
     *
     * @param config the project configuration
     * @return developer agent filename
     */
    public static String selectDeveloperAgent(
            ProjectConfig config) {
        String langName = config.language().name();
        return langName + "-developer" + MD_EXTENSION;
    }

    /**
     * Delegates to {@link ChecklistRulesBuilder}.
     *
     * @param config the project configuration
     * @return list of checklist rules with active flags
     */
    public static List<ChecklistRule> buildChecklistRules(
            ProjectConfig config) {
        return ChecklistRulesBuilder
                .buildChecklistRules(config);
    }

    /**
     * Derives the marker string from a checklist filename.
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

        boolean hasDevsecops =
                !"none".equals(infra.container())
                        || !"none".equals(
                                infra.orchestrator());
        if (hasDevsecops) {
            agents.add("devsecops-engineer.md");
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

    private static List<String> selectSecurityAgents(
            ProjectConfig config) {
        List<String> agents = new ArrayList<>();
        if (!config.security().frameworks().isEmpty()) {
            agents.add("appsec-engineer.md");
            agents.add("compliance-auditor.md");
        }
        if (config.security().pentest()) {
            agents.add("pentest-engineer.md");
        }
        return agents;
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
