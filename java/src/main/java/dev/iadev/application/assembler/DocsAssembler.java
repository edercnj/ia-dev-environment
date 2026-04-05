package dev.iadev.application.assembler;

import dev.iadev.config.ContextBuilder;
import dev.iadev.domain.model.ProjectConfig;
import dev.iadev.template.TemplateEngine;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

/**
 * Assembles {@code steering/service-architecture.md}
 * from the {@code _TEMPLATE-SERVICE-ARCHITECTURE.md} Pebble
 * template.
 *
 * <p>This is the fourteenth assembler in the pipeline
 * (position 14 of 23 per RULE-005). It renders the template
 * using full Pebble rendering via
 * {@link TemplateEngine#render(String, Map)} to resolve
 * project context variables (name, language, framework,
 * architecture).</p>
 *
 * <p>Graceful no-op: if the source template does not exist
 * in the resources directory, returns an empty list (backward
 * compatibility).</p>
 *
 * <p>Example usage:
 * <pre>{@code
 * Assembler docs = new DocsAssembler();
 * List<String> files = docs.assemble(
 *     config, engine, outputDir);
 * }</pre>
 * </p>
 *
 * @see Assembler
 * @see TemplateEngine#render(String, Map)
 */
public final class DocsAssembler implements Assembler {

    private static final String TEMPLATE_PATH =
            "shared/templates/_TEMPLATE-SERVICE-ARCHITECTURE.md";
    private static final String OUTPUT_SUBDIR =
            "steering";
    private static final String OUTPUT_FILENAME =
            "service-architecture.md";

    private final Path resourcesDir;

    /**
     * Creates a DocsAssembler using classpath resources.
     */
    public DocsAssembler() {
        this(resolveClasspathResources());
    }

    /**
     * Creates a DocsAssembler with an explicit resources
     * directory.
     *
     * @param resourcesDir the base resources directory
     */
    public DocsAssembler(Path resourcesDir) {
        this.resourcesDir = resourcesDir;
    }

    /**
     * {@inheritDoc}
     *
     * <p>Renders the service architecture template with
     * Pebble and writes it to
     * {@code architecture/service-architecture.md}.</p>
     */
    @Override
    public List<String> assemble(
            ProjectConfig config,
            TemplateEngine engine,
            Path outputDir) {
        Path templateFile =
                resourcesDir.resolve(TEMPLATE_PATH);
        if (!Files.exists(templateFile)) {
            return List.of();
        }
        Map<String, Object> context =
                ContextBuilder.buildContext(config);
        String rendered = engine.render(
                TEMPLATE_PATH, context);
        Path destDir =
                outputDir.resolve(OUTPUT_SUBDIR);
        CopyHelpers.ensureDirectory(destDir);
        Path destFile =
                destDir.resolve(OUTPUT_FILENAME);
        CopyHelpers.writeFile(destFile, rendered);
        return List.of(destFile.toString());
    }

    private static Path resolveClasspathResources() {
        return dev.iadev.util.ResourceResolver
                .resolveResourcesRoot(TEMPLATE_PATH, 3);
    }
}
