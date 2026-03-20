package dev.iadev.assembler;

import dev.iadev.config.ContextBuilder;
import dev.iadev.model.ProjectConfig;
import dev.iadev.template.TemplateEngine;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Assembles {@code .github/prompts/} with prompt templates
 * for GitHub Copilot in the {@code .prompt.md} format.
 *
 * <p>This is the thirteenth assembler in the pipeline
 * (position 13 of 23 per RULE-005). It renders four
 * Pebble templates using full template rendering via
 * {@link TemplateEngine#render(String, Map)} — not just
 * placeholder replacement.</p>
 *
 * <p>Template filenames have a {@code .j2} suffix that is
 * stripped in the output (e.g., {@code new-feature.prompt
 * .md.j2} becomes {@code new-feature.prompt.md}).</p>
 *
 * <p>If the source templates directory does not exist,
 * the assembler returns an empty list (graceful no-op).
 * Individual missing templates are skipped.</p>
 *
 * <p>Example usage:
 * <pre>{@code
 * Assembler prompts = new GithubPromptsAssembler();
 * List<String> files = prompts.assemble(
 *     config, engine, outputDir);
 * }</pre>
 * </p>
 *
 * @see Assembler
 * @see TemplateEngine#render(String, Map)
 */
public final class GithubPromptsAssembler
        implements Assembler {

    private static final String TEMPLATES_DIR =
            "github-prompts-templates";
    private static final String PROMPTS_OUTPUT = "prompts";
    private static final String J2_SUFFIX = ".j2";

    /** The 4 Pebble prompt template filenames. */
    static final List<String> GITHUB_PROMPT_TEMPLATES =
            List.of(
                    "new-feature.prompt.md.j2",
                    "decompose-spec.prompt.md.j2",
                    "code-review.prompt.md.j2",
                    "troubleshoot.prompt.md.j2");

    private final Path resourcesDir;

    /**
     * Creates a GithubPromptsAssembler using classpath
     * resources.
     */
    public GithubPromptsAssembler() {
        this(resolveClasspathResources());
    }

    /**
     * Creates a GithubPromptsAssembler with an explicit
     * resources directory.
     *
     * @param resourcesDir the base resources directory
     */
    public GithubPromptsAssembler(Path resourcesDir) {
        this.resourcesDir = resourcesDir;
    }

    /**
     * {@inheritDoc}
     *
     * <p>Renders prompt templates with Pebble and writes
     * them to the output directory. The {@code .j2} suffix
     * is removed from each output filename.</p>
     */
    @Override
    public List<String> assemble(
            ProjectConfig config,
            TemplateEngine engine,
            Path outputDir) {
        Path srcDir = resourcesDir.resolve(TEMPLATES_DIR);
        if (!Files.exists(srcDir)) {
            return List.of();
        }
        Path promptsDir =
                outputDir.resolve(PROMPTS_OUTPUT);
        CopyHelpers.ensureDirectory(promptsDir);
        Map<String, Object> context =
                ContextBuilder.buildContext(config);

        List<String> results = new ArrayList<>();
        for (String templateName
                : GITHUB_PROMPT_TEMPLATES) {
            renderPromptTemplate(
                    srcDir, promptsDir, templateName,
                    engine, context, results);
        }
        return results;
    }

    private void renderPromptTemplate(
            Path srcDir, Path promptsDir,
            String templateName,
            TemplateEngine engine,
            Map<String, Object> context,
            List<String> results) {
        Path src = srcDir.resolve(templateName);
        if (!Files.exists(src)) {
            return;
        }
        String outputName = stripJ2Suffix(templateName);
        String content = engine.render(
                TEMPLATES_DIR + "/" + templateName,
                context);
        Path dest = promptsDir.resolve(outputName);
        CopyHelpers.writeFile(dest, content);
        results.add(dest.toString());
    }

    private static String stripJ2Suffix(String name) {
        return name.endsWith(J2_SUFFIX)
                ? name.substring(0,
                name.length() - J2_SUFFIX.length())
                : name;
    }

    private static Path resolveClasspathResources() {
        return dev.iadev.util.ResourceResolver
                .resolveResourcesRoot(TEMPLATES_DIR);
    }
}
