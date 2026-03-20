package dev.iadev.assembler;

import dev.iadev.config.ContextBuilder;
import dev.iadev.model.ProjectConfig;
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
 * Assembles {@code .github/agents/} with agent files in
 * the {@code .agent.md} format for GitHub Copilot.
 *
 * <p>This is the eleventh assembler in the pipeline
 * (position 11 of 23 per RULE-005). It generates three
 * categories of agents:
 * <ol>
 *   <li>Core agents -- always included, read from
 *       {@code github-agents-templates/core/}</li>
 *   <li>Conditional agents -- included based on feature
 *       gates (devops, api, event)</li>
 *   <li>Developer agent -- language-specific, read from
 *       {@code github-agents-templates/developers/}</li>
 * </ol>
 *
 * <p>Each agent template has YAML frontmatter with
 * {@code tools} and {@code disallowed-tools} fields.
 * Output files use {@code .agent.md} extension (GitHub
 * convention) instead of {@code .md} (Claude
 * convention).</p>
 *
 * <p>Example usage:
 * <pre>{@code
 * GithubAgentsAssembler agents =
 *     new GithubAgentsAssembler();
 * AssemblerResult result = agents.assembleWithWarnings(
 *     config, engine, outputDir);
 * }</pre>
 * </p>
 *
 * @see Assembler
 */
public final class GithubAgentsAssembler
        implements Assembler {

    private static final String TEMPLATES_DIR =
            "github-agents-templates";
    private static final String CORE_DIR = "core";
    private static final String CONDITIONAL_DIR =
            "conditional";
    private static final String DEVELOPERS_DIR =
            "developers";
    private static final String AGENTS_OUTPUT = "agents";
    private static final String AGENT_MD_EXTENSION =
            ".agent.md";
    private static final String MD_EXTENSION = ".md";

    private final Path resourcesDir;

    /**
     * Creates a GithubAgentsAssembler using classpath
     * resources.
     */
    public GithubAgentsAssembler() {
        this(resolveClasspathResources());
    }

    /**
     * Creates a GithubAgentsAssembler with an explicit
     * resources directory.
     *
     * @param resourcesDir the base resources directory
     */
    public GithubAgentsAssembler(Path resourcesDir) {
        this.resourcesDir = resourcesDir;
    }

    /**
     * {@inheritDoc}
     *
     * <p>Generates all GitHub agent files and returns
     * the list of file paths. Warnings for missing
     * templates are logged to stderr.</p>
     */
    @Override
    public List<String> assemble(
            ProjectConfig config,
            TemplateEngine engine,
            Path outputDir) {
        AssemblerResult result =
                assembleWithWarnings(
                        config, engine, outputDir);
        for (String warning : result.warnings()) {
            System.err.println("WARNING: " + warning);
        }
        return result.files();
    }

    /**
     * Generates all GitHub agent files with full result
     * including warnings for missing templates.
     *
     * @param config    the project configuration
     * @param engine    the template engine
     * @param outputDir the target output directory
     * @return result with generated files and warnings
     */
    public AssemblerResult assembleWithWarnings(
            ProjectConfig config,
            TemplateEngine engine,
            Path outputDir) {
        Map<String, Object> context =
                ContextBuilder.buildContext(config);
        Path agentsDir =
                outputDir.resolve(AGENTS_OUTPUT);
        CopyHelpers.ensureDirectory(agentsDir);

        List<String> files = new ArrayList<>();
        List<String> warnings = new ArrayList<>();

        files.addAll(assembleCore(
                agentsDir, engine, context));
        files.addAll(assembleConditional(
                config, agentsDir, engine,
                warnings, context));

        Optional<String> dev = assembleDeveloper(
                config, agentsDir, engine, context);
        if (dev.isPresent()) {
            files.add(dev.orElseThrow());
        } else {
            String expected = config.language().name()
                    + "-developer.md";
            warnings.add(
                    "Developer agent template missing: %s"
                            .formatted(expected));
        }

        return AssemblerResult.of(files, warnings);
    }

    /**
     * Selects conditional agent filenames based on
     * project configuration feature gates.
     *
     * <p>Evaluates three conditions:
     * <ul>
     *   <li>devops-engineer -- infrastructure present</li>
     *   <li>api-engineer -- REST, gRPC, or GraphQL</li>
     *   <li>event-engineer -- event-driven or event
     *       interfaces</li>
     * </ul>
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

        return agents;
    }

    /**
     * Assembles core agents from the core/ directory,
     * sorted alphabetically.
     *
     * @param agentsDir the agents output directory
     * @param engine    the template engine
     * @return list of generated file paths
     */
    List<String> assembleCore(
            Path agentsDir,
            TemplateEngine engine,
            Map<String, Object> context) {
        Path coreDir = resourcesDir.resolve(
                TEMPLATES_DIR + "/" + CORE_DIR);
        if (!Files.exists(coreDir)) {
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

    private List<String> assembleConditional(
            ProjectConfig config,
            Path agentsDir,
            TemplateEngine engine,
            List<String> warnings,
            Map<String, Object> context) {
        Path condDir = resourcesDir.resolve(
                TEMPLATES_DIR + "/" + CONDITIONAL_DIR);
        if (!Files.exists(condDir)) {
            return List.of();
        }
        List<String> results = new ArrayList<>();
        for (String name
                : selectGithubConditionalAgents(config)) {
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
     * @param config    the project configuration
     * @param agentsDir the agents output directory
     * @param engine    the template engine
     * @param context   the context map for replacement
     * @return Optional containing the generated file path,
     *         or empty if the template is missing
     */
    Optional<String> assembleDeveloper(
            ProjectConfig config,
            Path agentsDir,
            TemplateEngine engine,
            Map<String, Object> context) {
        Path devDir = resourcesDir.resolve(
                TEMPLATES_DIR + "/" + DEVELOPERS_DIR);
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
     * Renders a single agent template to the output
     * directory with {@code .agent.md} extension and
     * context-aware placeholder replacement.
     *
     * @param srcPath   the source template file path
     * @param agentsDir the agents output directory
     * @param engine    the template engine
     * @param context   the context map for replacement
     * @return the destination file path
     */
    String renderAgent(
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

    private static boolean hasAnyInterface(
            ProjectConfig config, String... types) {
        Set<String> typeSet = Set.of(types);
        return config.interfaces().stream()
                .anyMatch(i -> typeSet.contains(i.type()));
    }

    private static Path resolveClasspathResources() {
        return dev.iadev.util.ResourceResolver
                .resolveResourcesRoot(TEMPLATES_DIR);
    }

}
