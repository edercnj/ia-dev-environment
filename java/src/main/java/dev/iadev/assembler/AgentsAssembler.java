package dev.iadev.assembler;

import dev.iadev.config.ContextBuilder;
import dev.iadev.model.ProjectConfig;
import dev.iadev.template.TemplateEngine;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

/**
 * Assembles {@code .claude/agents/} from templates based on
 * project configuration.
 *
 * <p>This is the third assembler in the pipeline (position 3
 * of 23 per RULE-005). It generates three categories of
 * agents:
 * <ol>
 *   <li>Core agents — always included regardless of
 *       profile (scanned from core/ directory)</li>
 *   <li>Developer agent — language-specific agent
 *       (e.g. java-developer.md, go-developer.md)</li>
 *   <li>Conditional agents — included based on feature
 *       gates evaluated by {@link AgentsSelection}</li>
 * </ol>
 *
 * <p>After copying all agent files, the assembler injects
 * checklist sections into agents based on
 * {@link AgentsSelection#buildChecklistRules}.</p>
 *
 * <p>Assembly flow:
 * <ol>
 *   <li>Scan core agents directory for .md files</li>
 *   <li>Copy each core agent with placeholder
 *       replacement</li>
 *   <li>Evaluate feature gates via
 *       {@link AgentsSelection}</li>
 *   <li>Copy matching conditional agent templates</li>
 *   <li>Copy developer agent template</li>
 *   <li>Inject checklists into agent files</li>
 * </ol>
 *
 * <p>Example usage:
 * <pre>{@code
 * Assembler agents = new AgentsAssembler();
 * List<String> files = agents.assemble(
 *     config, engine, outputDir);
 * }</pre>
 * </p>
 *
 * @see Assembler
 * @see AgentsSelection
 */
public final class AgentsAssembler implements Assembler {

    private static final String AGENTS_TEMPLATES_DIR =
            "agents-templates";
    private static final String CORE_DIR = "core";
    private static final String CONDITIONAL_DIR =
            "conditional";
    private static final String DEVELOPERS_DIR = "developers";
    private static final String CHECKLISTS_DIR = "checklists";
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

    /**
     * {@inheritDoc}
     *
     * <p>Orchestrates all assembly layers: core agents,
     * conditional agents, developer agent, and checklist
     * injection. Returns the list of generated file
     * paths.</p>
     */
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

        String dev = copyDeveloperAgent(
                config, outputDir, engine, context);
        if (dev != null) {
            files.add(dev);
        }

        injectChecklists(config, outputDir);
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
            String result = copyConditionalAgent(
                    agent, outputDir, engine, context);
            if (result != null) {
                generated.add(result);
            }
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

    private String copyConditionalAgent(
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

    private String copyDeveloperAgent(
            ProjectConfig config,
            Path outputDir,
            TemplateEngine engine,
            Map<String, Object> context) {
        String agentFile =
                AgentsSelection.selectDeveloperAgent(config);
        Path src = resourcesDir.resolve(
                AGENTS_TEMPLATES_DIR + "/"
                        + DEVELOPERS_DIR + "/"
                        + agentFile);
        Path dest = outputDir.resolve(
                AGENTS_OUTPUT + "/" + agentFile);
        return CopyHelpers.copyTemplateFileIfExists(
                src, dest, engine, context);
    }

    private void injectChecklists(
            ProjectConfig config,
            Path outputDir) {
        for (var rule : AgentsSelection
                .buildChecklistRules(config)) {
            if (!rule.active()) {
                continue;
            }
            injectSingleChecklist(
                    rule.agent(),
                    rule.checklist(),
                    outputDir);
        }
    }

    private void injectSingleChecklist(
            String agentFile,
            String checklistFile,
            Path outputDir) {
        Path agentPath = outputDir.resolve(
                AGENTS_OUTPUT + "/" + agentFile);
        if (!Files.exists(agentPath)) {
            return;
        }
        Path checklistSrc = resourcesDir.resolve(
                AGENTS_TEMPLATES_DIR + "/"
                        + CHECKLISTS_DIR + "/"
                        + checklistFile);
        if (!Files.exists(checklistSrc)) {
            return;
        }
        try {
            String marker = AgentsSelection.checklistMarker(
                    checklistFile);
            String section = Files.readString(
                    checklistSrc, StandardCharsets.UTF_8);
            String base = Files.readString(
                    agentPath, StandardCharsets.UTF_8);
            String result = TemplateEngine.injectSection(
                    base, section, marker);
            Files.writeString(
                    agentPath, result,
                    StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException(
                    "Failed to inject checklist: "
                            + checklistFile, e);
        }
    }

    private static Path resolveClasspathResources() {
        return dev.iadev.util.ResourceResolver
                .resolveResourcesRoot(AGENTS_TEMPLATES_DIR);
    }
}
