package dev.iadev.assembler;

import dev.iadev.config.ContextBuilder;
import dev.iadev.model.ProjectConfig;
import dev.iadev.template.TemplateEngine;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Assembles {@code .github/agents/} with agent files in
 * the {@code .agent.md} format for GitHub Copilot.
 *
 * <p>Agent rendering and selection are delegated to
 * {@link GithubAgentRenderer}.</p>
 *
 * @see Assembler
 * @see GithubAgentRenderer
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

    /** {@inheritDoc} */
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

        Path coreDir = resourcesDir.resolve(
                TEMPLATES_DIR + "/" + CORE_DIR);
        files.addAll(GithubAgentRenderer.assembleCore(
                coreDir, agentsDir, engine, context));

        Path condDir = resourcesDir.resolve(
                TEMPLATES_DIR + "/" + CONDITIONAL_DIR);
        files.addAll(
                GithubAgentRenderer.assembleConditional(
                        condDir, config, agentsDir,
                        engine, warnings, context));

        Path devDir = resourcesDir.resolve(
                TEMPLATES_DIR + "/" + DEVELOPERS_DIR);
        Optional<String> dev =
                GithubAgentRenderer.assembleDeveloper(
                        devDir, config, agentsDir,
                        engine, context);
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
     * Delegates to
     * {@link GithubAgentRenderer#selectGithubConditionalAgents}.
     *
     * @param config the project configuration
     * @return list of conditional agent template filenames
     */
    static List<String> selectGithubConditionalAgents(
            ProjectConfig config) {
        return GithubAgentRenderer
                .selectGithubConditionalAgents(config);
    }

    /**
     * Delegates to
     * {@link GithubAgentRenderer#assembleCore}.
     */
    List<String> assembleCore(
            Path agentsDir,
            TemplateEngine engine,
            Map<String, Object> context) {
        Path coreDir = resourcesDir.resolve(
                TEMPLATES_DIR + "/" + CORE_DIR);
        return GithubAgentRenderer.assembleCore(
                coreDir, agentsDir, engine, context);
    }

    /**
     * Delegates to
     * {@link GithubAgentRenderer#assembleDeveloper}.
     */
    Optional<String> assembleDeveloper(
            ProjectConfig config,
            Path agentsDir,
            TemplateEngine engine,
            Map<String, Object> context) {
        Path devDir = resourcesDir.resolve(
                TEMPLATES_DIR + "/" + DEVELOPERS_DIR);
        return GithubAgentRenderer.assembleDeveloper(
                devDir, config, agentsDir,
                engine, context);
    }

    /**
     * Delegates to
     * {@link GithubAgentRenderer#renderAgent}.
     */
    String renderAgent(
            Path srcPath,
            Path agentsDir,
            TemplateEngine engine,
            Map<String, Object> context) {
        return GithubAgentRenderer.renderAgent(
                srcPath, agentsDir, engine, context);
    }

    private static Path resolveClasspathResources() {
        return dev.iadev.util.ResourceResolver
                .resolveResourcesRoot(TEMPLATES_DIR);
    }
}
