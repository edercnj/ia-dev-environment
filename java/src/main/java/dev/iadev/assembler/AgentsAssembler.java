package dev.iadev.assembler;

import dev.iadev.config.ContextBuilder;
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
import java.util.stream.Stream;

/**
 * Assembles {@code .claude/agents/} from templates based on
 * project configuration.
 *
 * <p>Checklist injection is delegated to
 * {@link ChecklistInjector}.</p>
 *
 * @see Assembler
 * @see AgentsSelection
 * @see ChecklistInjector
 */
public final class AgentsAssembler implements Assembler {

    private static final String AGENTS_TEMPLATES_DIR =
            "agents-templates";
    private static final String CORE_DIR = "core";
    private static final String CONDITIONAL_DIR =
            "conditional";
    private static final String DEVELOPERS_DIR =
            "developers";
    private static final String AGENTS_OUTPUT = "agents";
    private static final String MD_EXTENSION = ".md";

    private final Path resourcesDir;

    /**
     * Creates an AgentsAssembler using classpath resources.
     */
    public AgentsAssembler() {
        this(resolveClasspathResources());
    }

    /**
     * Creates an AgentsAssembler with an explicit resources
     * directory.
     *
     * @param resourcesDir the base resources directory
     */
    public AgentsAssembler(Path resourcesDir) {
        this.resourcesDir = resourcesDir;
    }

    /** {@inheritDoc} */
    @Override
    public List<String> assemble(
            ProjectConfig config,
            TemplateEngine engine,
            Path outputDir) {
        Map<String, Object> context =
                ContextBuilder.buildContext(config);
        List<String> files = new ArrayList<>();

        files.addAll(assembleCore(
                outputDir, engine, context));
        files.addAll(assembleConditional(
                config, outputDir, engine, context));

        copyDeveloperAgent(
                config, outputDir, engine, context)
                .ifPresent(files::add);

        ChecklistInjector.injectChecklists(
                config, resourcesDir, outputDir);
        return files;
    }

    /**
     * Scans core agents directory for .md files, sorted
     * alphabetically.
     *
     * @return sorted list of core agent filenames
     */
    List<String> selectCoreAgents() {
        Path corePath = resourcesDir.resolve(
                AGENTS_TEMPLATES_DIR + "/" + CORE_DIR);
        if (!Files.exists(corePath)
                || !Files.isDirectory(corePath)) {
            return List.of();
        }
        try (Stream<Path> stream = Files.list(corePath)) {
            return stream
                    .filter(Files::isRegularFile)
                    .map(p -> p.getFileName().toString())
                    .filter(name -> name.endsWith(
                            MD_EXTENSION))
                    .sorted()
                    .toList();
        } catch (IOException e) {
            throw new UncheckedIOException(
                    "Failed to list core agents: "
                            + corePath, e);
        }
    }

    private List<String> assembleCore(
            Path outputDir,
            TemplateEngine engine,
            Map<String, Object> context) {
        List<String> generated = new ArrayList<>();
        for (String agent : selectCoreAgents()) {
            String result = copyCoreAgent(
                    agent, outputDir, engine, context);
            generated.add(result);
        }
        return generated;
    }

    private List<String> assembleConditional(
            ProjectConfig config,
            Path outputDir,
            TemplateEngine engine,
            Map<String, Object> context) {
        List<String> generated = new ArrayList<>();
        List<String> conditional =
                AgentsSelection.selectConditionalAgents(
                        config);
        for (String agent : conditional) {
            copyConditionalAgent(
                    agent, outputDir, engine, context)
                    .ifPresent(generated::add);
        }
        return generated;
    }

    private String copyCoreAgent(
            String agentFile,
            Path outputDir,
            TemplateEngine engine,
            Map<String, Object> context) {
        Path src = resourcesDir.resolve(
                AGENTS_TEMPLATES_DIR + "/"
                        + CORE_DIR + "/" + agentFile);
        Path dest = outputDir.resolve(
                AGENTS_OUTPUT + "/" + agentFile);
        return CopyHelpers.copyTemplateFile(
                src, dest, engine, context);
    }

    private Optional<String> copyConditionalAgent(
            String agentFile,
            Path outputDir,
            TemplateEngine engine,
            Map<String, Object> context) {
        Path src = resourcesDir.resolve(
                AGENTS_TEMPLATES_DIR + "/"
                        + CONDITIONAL_DIR + "/"
                        + agentFile);
        Path dest = outputDir.resolve(
                AGENTS_OUTPUT + "/" + agentFile);
        return CopyHelpers.copyTemplateFileIfExists(
                src, dest, engine, context);
    }

    private Optional<String> copyDeveloperAgent(
            ProjectConfig config,
            Path outputDir,
            TemplateEngine engine,
            Map<String, Object> context) {
        String agentFile =
                AgentsSelection.selectDeveloperAgent(
                        config);
        Path src = resourcesDir.resolve(
                AGENTS_TEMPLATES_DIR + "/"
                        + DEVELOPERS_DIR + "/"
                        + agentFile);
        Path dest = outputDir.resolve(
                AGENTS_OUTPUT + "/" + agentFile);
        return CopyHelpers.copyTemplateFileIfExists(
                src, dest, engine, context);
    }

    private static Path resolveClasspathResources() {
        return dev.iadev.util.ResourceResolver
                .resolveResourcesRoot(AGENTS_TEMPLATES_DIR);
    }
}
