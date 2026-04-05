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
 * {@code specs/_templates/_TEMPLATE-SLO-SLI-DEFINITION.md}
 * from the {@code _TEMPLATE-SLO-SLI-DEFINITION.md} Pebble
 * template.
 *
 * <p>This assembler renders the SLO/SLI definition template
 * using full Pebble rendering via
 * {@link TemplateEngine#render(String, Map)} to resolve
 * the {@code PROJECT_NAME} variable.</p>
 *
 * <p>Unconditional: always generated regardless of profile,
 * since SLO/SLI definitions are a universal SRE practice.</p>
 *
 * <p>Graceful no-op: if the source template does not exist
 * in the resources directory, returns an empty list (backward
 * compatibility).</p>
 *
 * @see Assembler
 * @see TemplateEngine#render(String, Map)
 */
public final class SloSliTemplateAssembler
        implements Assembler {

    private static final String TEMPLATE_PATH =
            "shared/templates/_TEMPLATE-SLO-SLI-DEFINITION.md";
    private static final String OUTPUT_SUBDIR =
            "specs/_templates";
    private static final String OUTPUT_FILENAME =
            "_TEMPLATE-SLO-SLI-DEFINITION.md";

    /** The 7 mandatory SLO/SLI template sections. */
    static final List<String> MANDATORY_SECTIONS =
            List.of(
                    "Service Overview",
                    "SLI Definitions",
                    "SLO Targets",
                    "Error Budget Policy",
                    "Burn Rate Alerting Configuration",
                    "Dashboard Requirements",
                    "Review Cadence");

    private final Path resourcesDir;

    /**
     * Creates a SloSliTemplateAssembler using classpath
     * resources.
     */
    public SloSliTemplateAssembler() {
        this(resolveClasspathResources());
    }

    /**
     * Creates a SloSliTemplateAssembler with an explicit
     * resources directory.
     *
     * @param resourcesDir the base resources directory
     */
    public SloSliTemplateAssembler(
            Path resourcesDir) {
        this.resourcesDir = resourcesDir;
    }

    /**
     * {@inheritDoc}
     *
     * <p>Renders the SLO/SLI definition template with
     * Pebble and writes it to
     * {@code specs/_templates/_TEMPLATE-SLO-SLI-DEFINITION.md}.
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
        if (!hasAllSections(rendered)) {
            return List.of();
        }
        Path destDir =
                outputDir.resolve(OUTPUT_SUBDIR);
        CopyHelpers.ensureDirectory(destDir);
        Path destFile =
                destDir.resolve(OUTPUT_FILENAME);
        CopyHelpers.writeFile(destFile, rendered);
        return List.of(destFile.toString());
    }

    private static boolean hasAllSections(String content) {
        return CopyHelpers.hasAllMandatorySections(
                content, MANDATORY_SECTIONS);
    }

    private static Path resolveClasspathResources() {
        return dev.iadev.util.ResourceResolver
                .resolveResourcesRoot(TEMPLATE_PATH, 3);
    }
}
