package dev.iadev.application.assembler;

import dev.iadev.config.ContextBuilder;
import dev.iadev.domain.model.ProjectConfig;
import dev.iadev.template.TemplateEngine;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

/**
 * Assembles
 * {@code docs/templates/_TEMPLATE-OPERATIONAL-RUNBOOK.md}
 * from the {@code _TEMPLATE-OPERATIONAL-RUNBOOK.md} Pebble
 * template.
 *
 * <p>This assembler renders the operational runbook template
 * using full Pebble rendering via
 * {@link TemplateEngine#render(String, Map)} to resolve
 * project context variables and conditional sections for
 * database maintenance, cache operations, and message
 * broker operations.</p>
 *
 * <p>Unconditional sections (Scaling Procedures,
 * Certificate Rotation, Dependency Failure Handling,
 * Backup and Restore) are always included. Conditional
 * sections are included based on {@code database_name},
 * {@code cache_name}, and {@code message_broker} context
 * variables.</p>
 *
 * <p>Graceful no-op: if the source template does not exist
 * in the resources directory, returns an empty list
 * (backward compatibility).</p>
 *
 * @see Assembler
 * @see TemplateEngine#render(String, Map)
 */
public final class OperationalRunbookAssembler
        implements Assembler {

    private static final String TEMPLATE_PATH =
            "templates/_TEMPLATE-OPERATIONAL-RUNBOOK.md";
    private static final String OUTPUT_SUBDIR =
            "docs/templates";
    private static final String OUTPUT_FILENAME =
            "_TEMPLATE-OPERATIONAL-RUNBOOK.md";

    private final Path resourcesDir;

    /**
     * Creates an OperationalRunbookAssembler using
     * classpath resources.
     */
    public OperationalRunbookAssembler() {
        this(resolveClasspathResources());
    }

    /**
     * Creates an OperationalRunbookAssembler with an
     * explicit resources directory.
     *
     * @param resourcesDir the base resources directory
     */
    public OperationalRunbookAssembler(
            Path resourcesDir) {
        this.resourcesDir = resourcesDir;
    }

    /**
     * {@inheritDoc}
     *
     * <p>Renders the operational runbook template with
     * Pebble and writes it to
     * {@code docs/templates/_TEMPLATE-OPERATIONAL-RUNBOOK.md}.
     * </p>
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
