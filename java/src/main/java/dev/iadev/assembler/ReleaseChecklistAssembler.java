package dev.iadev.assembler;

import dev.iadev.config.ContextBuilder;
import dev.iadev.model.ProjectConfig;
import dev.iadev.template.TemplateEngine;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Assembles {@code docs/templates/_TEMPLATE-RELEASE-CHECKLIST.md}
 * from the {@code _TEMPLATE-RELEASE-CHECKLIST.md} Pebble template.
 *
 * <p>Renders a release checklist with conditional sections
 * for container, native build, and contract testing based on
 * the project configuration.</p>
 *
 * <p>Graceful no-op: if the source template does not exist
 * in the resources directory, returns an empty list (backward
 * compatibility).</p>
 *
 * @see Assembler
 * @see TemplateEngine#render(String, Map)
 */
public final class ReleaseChecklistAssembler
        implements Assembler {

    private static final String TEMPLATE_PATH =
            "templates/_TEMPLATE-RELEASE-CHECKLIST.md";
    private static final String OUTPUT_SUBDIR =
            "docs/templates";
    private static final String OUTPUT_FILENAME =
            "_TEMPLATE-RELEASE-CHECKLIST.md";

    /** The 6 mandatory release checklist sections. */
    static final List<String> MANDATORY_SECTIONS =
            List.of(
                    "Pre-Release Validation",
                    "Version & Changelog",
                    "Artifact Build",
                    "Quality Gate",
                    "Publish",
                    "Post-Release");

    private final Path resourcesDir;

    /**
     * Creates a ReleaseChecklistAssembler using classpath
     * resources.
     */
    public ReleaseChecklistAssembler() {
        this(resolveClasspathResources());
    }

    /**
     * Creates a ReleaseChecklistAssembler with an explicit
     * resources directory.
     *
     * @param resourcesDir the base resources directory
     */
    public ReleaseChecklistAssembler(
            Path resourcesDir) {
        this.resourcesDir = resourcesDir;
    }

    /**
     * {@inheritDoc}
     *
     * <p>Renders the release checklist template with Pebble
     * conditionals and writes it to
     * {@code docs/templates/_TEMPLATE-RELEASE-CHECKLIST.md}.
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
                buildContext(config);
        String rendered = engine.render(
                TEMPLATE_PATH, context);
        boolean hasSections = MANDATORY_SECTIONS.stream()
                .allMatch(rendered::contains);
        if (!hasSections) {
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

    private static Map<String, Object> buildContext(
            ProjectConfig config) {
        Map<String, Object> ctx =
                new LinkedHashMap<>(
                        ContextBuilder.buildContext(config));
        ctx.put("native_build",
                config.framework().nativeBuild());
        return ctx;
    }

    private static Path resolveClasspathResources() {
        return dev.iadev.util.ResourceResolver
                .resolveResourcesRoot(TEMPLATE_PATH, 2);
    }
}
