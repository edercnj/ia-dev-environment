package dev.iadev.application.assembler;

import dev.iadev.config.ContextBuilder;
import dev.iadev.domain.model.ProjectConfig;
import dev.iadev.template.TemplateEngine;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Assembles {@code CONSTITUTION.md} at the output root
 * when compliance is active (non-empty frameworks list).
 *
 * <p>This assembler is conditional: it produces no output
 * when {@code security.frameworks} is empty (compliance
 * is "none"). When active, it renders the constitution
 * template containing invariants, CWE mappings,
 * architecture boundaries, naming conventions, and
 * compliance-specific requirements.</p>
 *
 * <p>Registered as the first assembler in the pipeline
 * per story-0016-0002, since the constitution defines
 * constraints that other assemblers must respect.</p>
 *
 * @see Assembler
 * @see AssemblerFactory
 */
public final class ConstitutionAssembler
        implements Assembler {

    private static final String TEMPLATE_PATH =
            "templates/constitution/CONSTITUTION.md";
    private static final String OUTPUT_FILENAME =
            "CONSTITUTION.md";

    private final Path resourcesDir;

    /**
     * Creates a ConstitutionAssembler using classpath
     * resources.
     */
    public ConstitutionAssembler() {
        this(resolveClasspathResources());
    }

    /**
     * Creates a ConstitutionAssembler with an explicit
     * resources directory.
     *
     * @param resourcesDir the base resources directory
     */
    public ConstitutionAssembler(Path resourcesDir) {
        this.resourcesDir = resourcesDir;
    }

    /**
     * {@inheritDoc}
     *
     * <p>No-op when compliance frameworks list is empty.
     * Renders CONSTITUTION.md at the output root when
     * compliance is active.</p>
     */
    @Override
    public List<String> assemble(
            ProjectConfig config,
            TemplateEngine engine,
            Path outputDir) {
        if (!hasActiveCompliance(config)) {
            return List.of();
        }
        Path templateFile =
                resourcesDir.resolve(TEMPLATE_PATH);
        if (!Files.exists(templateFile)) {
            return List.of();
        }
        return renderConstitution(
                config, engine, outputDir);
    }

    private List<String> renderConstitution(
            ProjectConfig config,
            TemplateEngine engine,
            Path outputDir) {
        Map<String, Object> context =
                buildConstitutionContext(config);
        String rendered = engine.render(
                TEMPLATE_PATH, context);
        CopyHelpers.ensureDirectory(outputDir);
        Path destFile =
                outputDir.resolve(OUTPUT_FILENAME);
        CopyHelpers.writeFile(destFile, rendered);
        return List.of(destFile.toString());
    }

    private Map<String, Object> buildConstitutionContext(
            ProjectConfig config) {
        Map<String, Object> context =
                new LinkedHashMap<>(
                        ContextBuilder.buildContext(config));
        List<String> frameworks =
                config.security().frameworks();
        context.put("compliance_frameworks", frameworks);
        context.put("compliance_primary",
                frameworks.isEmpty()
                        ? "none" : frameworks.get(0));
        return context;
    }

    private static boolean hasActiveCompliance(
            ProjectConfig config) {
        return !config.security().frameworks().isEmpty();
    }

    private static Path resolveClasspathResources() {
        return dev.iadev.util.ResourceResolver
                .resolveResourcesRoot(TEMPLATE_PATH, 3);
    }
}
