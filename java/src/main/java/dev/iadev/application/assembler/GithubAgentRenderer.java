package dev.iadev.application.assembler;

import dev.iadev.domain.model.ProjectConfig;
import dev.iadev.template.TemplateEngine;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

/**
 * Renders GitHub agent templates and selects conditional
 * agents based on project configuration.
 *
 * <p>Extracted from {@link GithubAgentsAssembler} to keep
 * both classes under 250 lines per RULE-004.</p>
 *
 * @see GithubAgentsAssembler
 */
final class GithubAgentRenderer {

    private static final String AGENT_MD_EXTENSION =
            ".agent.md";
    private static final String MD_EXTENSION = ".md";

    private GithubAgentRenderer() {
        // utility class
    }

    /**
     * Renders a single agent template to the output
     * directory with {@code .agent.md} extension.
     *
     * @param srcPath   the source template file path
     * @param agentsDir the agents output directory
     * @param engine    the template engine
     * @param context   the context map for replacement
     * @return the destination file path
     */
    static String renderAgent(
            Path srcPath,
            Path agentsDir,
            TemplateEngine engine,
            Map<String, Object> context) {
        String content = CopyHelpers.readFile(srcPath);
        String rendered = engine.replacePlaceholders(
                content, context);
        String fileName =
                srcPath.getFileName().toString();
        String stem = fileName.endsWith(MD_EXTENSION)
                ? fileName.substring(0,
                        fileName.length()
                                - MD_EXTENSION.length())
                : fileName;
        String outputName = stem + AGENT_MD_EXTENSION;
        Path dest = agentsDir.resolve(outputName);
        CopyHelpers.writeFile(dest, rendered);
        return dest.toString();
    }

    /**
     * Assembles core agents from the core/ directory.
     *
     * @param coreDir   the core templates directory
     * @param agentsDir the agents output directory
     * @param engine    the template engine
     * @param context   the context map
     * @return list of generated file paths
     */
    static List<String> assembleCore(
            Path coreDir,
            Path agentsDir,
            TemplateEngine engine,
            Map<String, Object> context) {
        if (!Files.isDirectory(coreDir)) {
            return List.of();
        }
        List<Path> entries;
        try (Stream<Path> stream = Files.list(coreDir)) {
            entries = stream
                    .filter(Files::isRegularFile)
                    .sorted()
                    .toList();
        } catch (IOException e) {
            throw new UncheckedIOException(
                    "Failed to list core agents: %s"
                            .formatted(coreDir), e);
        }
        List<String> results = new ArrayList<>();
        for (Path entry : entries) {
            results.add(renderAgent(
                    entry, agentsDir, engine, context));
        }
        return results;
    }

    /**
     * Assembles conditional agents.
     *
     * @param condDir   the conditional templates directory
     * @param config    the project configuration
     * @param agentsDir the agents output directory
     * @param engine    the template engine
     * @param warnings  list to collect warnings
     * @param context   the context map
     * @return list of generated file paths
     */
    static List<String> assembleConditional(
            Path condDir,
            ProjectConfig config,
            Path agentsDir,
            TemplateEngine engine,
            List<String> warnings,
            Map<String, Object> context) {
        if (!Files.exists(condDir)) {
            return List.of();
        }
        List<String> results = new ArrayList<>();
        for (String name : selectGithubConditionalAgents(
                config)) {
            Path src = condDir.resolve(name);
            if (!Files.exists(src)) {
                warnings.add(
                        "Conditional agent template"
                                + " missing: " + name);
                continue;
            }
            results.add(renderAgent(
                    src, agentsDir, engine, context));
        }
        return results;
    }

    /**
     * Assembles the developer agent for the project
     * language.
     *
     * @param devDir    the developers templates directory
     * @param config    the project configuration
     * @param agentsDir the agents output directory
     * @param engine    the template engine
     * @param context   the context map
     * @return Optional containing the generated file path
     */
    static Optional<String> assembleDeveloper(
            Path devDir,
            ProjectConfig config,
            Path agentsDir,
            TemplateEngine engine,
            Map<String, Object> context) {
        if (!Files.exists(devDir)) {
            return Optional.empty();
        }
        String safeName = config.language().name();
        Path template = devDir.resolve(
                safeName + "-developer.md");
        if (!Files.exists(template)) {
            return Optional.empty();
        }
        return Optional.of(renderAgent(
                template, agentsDir, engine, context));
    }

    /**
     * Selects conditional agent filenames based on
     * project configuration feature gates.
     *
     * @param config the project configuration
     * @return list of conditional agent template filenames
     */
    static List<String> selectGithubConditionalAgents(
            ProjectConfig config) {
        List<String> agents = new ArrayList<>();
        var infra = config.infrastructure();

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

        if (hasAnyInterface(config,
                "rest", "grpc", "graphql")) {
            agents.add("api-engineer.md");
        }

        boolean hasEvents =
                config.architecture().eventDriven()
                        || hasAnyInterface(config,
                                "event-consumer",
                                "event-producer");
        if (hasEvents) {
            agents.add("event-engineer.md");
        }

        if (!config.security().frameworks().isEmpty()) {
            agents.add("appsec-engineer.md");
        }

        return agents;
    }

    private static boolean hasAnyInterface(
            ProjectConfig config, String... types) {
        Set<String> typeSet = Set.of(types);
        return config.interfaces().stream()
                .anyMatch(i -> typeSet.contains(i.type()));
    }
}
