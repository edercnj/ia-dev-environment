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
 * <p>Preservation logic (story-0016-0003): when an
 * existing CONSTITUTION.md is detected in the output
 * directory, the assembler skips regeneration by default
 * to preserve user customizations. The
 * {@code overwriteConstitution} flag forces
 * regeneration.</p>
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
            "shared/templates/constitution/CONSTITUTION.md";
    static final String OUTPUT_FILENAME =
            "CONSTITUTION.md";
    static final String SKIP_MESSAGE =
            "CONSTITUTION.md exists — skipping "
                    + "(use --overwrite-constitution "
                    + "to regenerate)";
    static final String OVERWRITE_MESSAGE =
            "CONSTITUTION.md overwritten "
                    + "(--overwrite-constitution active)";

    private final Path resourcesDir;
    private final boolean overwriteConstitution;

    /**
     * Creates a ConstitutionAssembler using classpath
     * resources with default preservation behavior.
     */
    public ConstitutionAssembler() {
        this(resolveClasspathResources(), false);
    }

    /**
     * Creates a ConstitutionAssembler with an explicit
     * resources directory and default preservation.
     *
     * @param resourcesDir the base resources directory
     */
    public ConstitutionAssembler(Path resourcesDir) {
        this(resourcesDir, false);
    }

    /**
     * Creates a ConstitutionAssembler with explicit
     * resources directory and overwrite control.
     *
     * @param resourcesDir           the base resources dir
     * @param overwriteConstitution  if true, overwrites
     *     existing CONSTITUTION.md instead of preserving it
     */
    public ConstitutionAssembler(
            Path resourcesDir,
            boolean overwriteConstitution) {
        this.resourcesDir = resourcesDir;
        this.overwriteConstitution = overwriteConstitution;
    }

    /**
     * {@inheritDoc}
     *
     * <p>No-op when compliance frameworks list is empty.
     * Skips when CONSTITUTION.md already exists and
     * overwriteConstitution is false. Renders at the
     * output root otherwise.</p>
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
        Path destFile =
                outputDir.resolve(OUTPUT_FILENAME);
        if (shouldSkipExisting(destFile)) {
            return List.of();
        }
        return renderConstitution(
                config, engine, outputDir, destFile);
    }

    /**
     * {@inheritDoc}
     *
     * <p>Returns warnings when skipping or overwriting
     * an existing CONSTITUTION.md.</p>
     */
    @Override
    public AssemblerResult assembleWithResult(
            ProjectConfig config,
            TemplateEngine engine,
            Path outputDir) {
        if (!hasActiveCompliance(config)) {
            return AssemblerResult.empty();
        }
        Path templateFile =
                resourcesDir.resolve(TEMPLATE_PATH);
        if (!Files.exists(templateFile)) {
            return AssemblerResult.empty();
        }
        Path destFile =
                outputDir.resolve(OUTPUT_FILENAME);
        if (shouldSkipExisting(destFile)) {
            return AssemblerResult.of(
                    List.of(), List.of(SKIP_MESSAGE));
        }
        boolean existed = Files.exists(destFile);
        List<String> files = renderConstitution(
                config, engine, outputDir, destFile);
        List<String> warnings = existed
                ? List.of(OVERWRITE_MESSAGE) : List.of();
        return AssemblerResult.of(files, warnings);
    }

    private boolean shouldSkipExisting(Path destFile) {
        return Files.exists(destFile)
                && !overwriteConstitution;
    }

    private List<String> renderConstitution(
            ProjectConfig config,
            TemplateEngine engine,
            Path outputDir,
            Path destFile) {
        Map<String, Object> context =
                buildConstitutionContext(config);
        String rendered = engine.render(
                TEMPLATE_PATH, context);
        CopyHelpers.ensureDirectory(outputDir);
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
                .resolveResourcesRoot(TEMPLATE_PATH, 4);
    }
}
