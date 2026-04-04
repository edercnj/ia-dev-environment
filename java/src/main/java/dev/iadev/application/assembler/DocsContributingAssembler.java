package dev.iadev.assembler;

import dev.iadev.config.ContextBuilder;
import dev.iadev.domain.model.ProjectConfig;
import dev.iadev.template.TemplateEngine;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

/**
 * Assembles {@code docs/templates/_TEMPLATE-CONTRIBUTING.md}
 * from the {@code _TEMPLATE-CONTRIBUTING.md} Pebble template.
 *
 * <p>This assembler renders the contributing guide template
 * using full Pebble rendering via
 * {@link TemplateEngine#render(String, Map)} to resolve
 * project context variables (name, language, framework,
 * build tool) and conditional sections per stack.</p>
 *
 * <p>Unconditional: always generated for all profiles.
 * Content varies based on Pebble variables.</p>
 *
 * <p>Graceful no-op: if the source template does not exist
 * in the resources directory, returns an empty list (backward
 * compatibility).</p>
 *
 * <p>Example usage:
 * <pre>{@code
 * Assembler contributing = new DocsContributingAssembler();
 * List<String> files = contributing.assemble(
 *     config, engine, outputDir);
 * }</pre>
 * </p>
 *
 * @see Assembler
 * @see TemplateEngine#render(String, Map)
 */
public final class DocsContributingAssembler
        implements Assembler {

    private static final String TEMPLATE_PATH =
            "templates/_TEMPLATE-CONTRIBUTING.md";
    private static final String OUTPUT_SUBDIR =
            "docs/templates";
    private static final String OUTPUT_FILENAME =
            "_TEMPLATE-CONTRIBUTING.md";

    private final Path resourcesDir;

    /**
     * Creates a DocsContributingAssembler using classpath
     * resources.
     */
    public DocsContributingAssembler() {
        this(resolveClasspathResources());
    }

    /**
     * Creates a DocsContributingAssembler with an explicit
     * resources directory.
     *
     * @param resourcesDir the base resources directory
     */
    public DocsContributingAssembler(Path resourcesDir) {
        this.resourcesDir = resourcesDir;
    }

    /**
     * {@inheritDoc}
     *
     * <p>Renders the contributing guide template with Pebble
     * and writes it to
     * {@code docs/templates/_TEMPLATE-CONTRIBUTING.md}.</p>
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
                .resolveResourcesRoot(TEMPLATE_PATH, 2);
    }
}
