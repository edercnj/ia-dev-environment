package dev.iadev.application.assembler;

import dev.iadev.domain.model.ProjectConfig;
import dev.iadev.template.TemplateEngine;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Copies the 20 planning and review templates to
 * {@code .claude/templates/} in the output directory.
 *
 * <p>Templates contain {@code {{PLACEHOLDER}}} tokens
 * intended for runtime resolution by the LLM, NOT for
 * build-time rendering. Content is copied verbatim
 * (RULE-003).</p>
 *
 * <p>Each template is validated for mandatory sections
 * before copying (RULE-010). Templates with missing
 * sections are skipped with a warning. Templates not
 * found on the classpath produce a warning without
 * throwing an exception. The authoritative list of
 * templates and their mandatory section headings lives in
 * {@link PlanTemplateDefinitions}.</p>
 *
 * @see Assembler
 * @see PlanTemplateDefinitions
 * @see EpicReportAssembler
 */
public final class PlanTemplatesAssembler
        implements Assembler {

    private static final String TEMPLATES_SUBDIR =
            "shared/templates";
    private static final String CLAUDE_OUTPUT_SUBDIR =
            ".claude/templates";

    /** Number of templates this assembler manages. */
    static final int TEMPLATE_COUNT =
            PlanTemplateDefinitions.TEMPLATE_COUNT;

    /**
     * Template definitions: filename to mandatory
     * sections mapping. Insertion-ordered for
     * deterministic processing.
     */
    static final Map<String, List<String>>
            TEMPLATE_SECTIONS =
                    PlanTemplateDefinitions.TEMPLATE_SECTIONS;

    private final Path resourcesDir;

    /**
     * Creates a PlanTemplatesAssembler using classpath
     * resources.
     */
    public PlanTemplatesAssembler() {
        this(resolveClasspathResources());
    }

    /**
     * Creates a PlanTemplatesAssembler with an explicit
     * resources directory.
     *
     * @param resourcesDir the base resources directory
     */
    public PlanTemplatesAssembler(Path resourcesDir) {
        this.resourcesDir = resourcesDir;
    }

    /**
     * {@inheritDoc}
     *
     * <p>Copies validated templates verbatim to
     * {@code .claude/templates/}. Returns only paths
     * of successfully copied files.</p>
     */
    @Override
    public List<String> assemble(
            ProjectConfig config,
            TemplateEngine engine,
            Path outputDir) {
        return assembleWithResult(
                config, engine, outputDir).files();
    }

    /**
     * {@inheritDoc}
     *
     * <p>Returns a structured result with both copied
     * file paths and validation warnings.</p>
     */
    @Override
    public AssemblerResult assembleWithResult(
            ProjectConfig config,
            TemplateEngine engine,
            Path outputDir) {
        Path templatesDir = resourcesDir
                .resolve(TEMPLATES_SUBDIR);
        if (!Files.isDirectory(templatesDir)) {
            return AssemblerResult.empty();
        }

        List<String> files = new ArrayList<>();
        List<String> warnings = new ArrayList<>();

        for (var entry
                : TEMPLATE_SECTIONS.entrySet()) {
            processTemplate(
                    entry.getKey(),
                    entry.getValue(),
                    outputDir,
                    files,
                    warnings);
        }

        return AssemblerResult.of(files, warnings);
    }

    private void processTemplate(
            String filename,
            List<String> mandatorySections,
            Path outputDir,
            List<String> files,
            List<String> warnings) {
        Path sourcePath = resourcesDir
                .resolve(TEMPLATES_SUBDIR)
                .resolve(filename);

        if (!Files.exists(sourcePath)) {
            warnings.add(
                    "Template not found: " + filename);
            return;
        }

        String content =
                CopyHelpers.readFile(sourcePath);

        if (!CopyHelpers.hasAllMandatorySections(
                content, mandatorySections)) {
            warnings.add(
                    "Missing mandatory section in "
                            + filename);
            return;
        }

        copyToClaudeTemplates(
                filename, content, outputDir, files);
    }

    private void copyToClaudeTemplates(
            String filename,
            String content,
            Path outputDir,
            List<String> files) {
        Path destDir = outputDir.resolve(CLAUDE_OUTPUT_SUBDIR);
        CopyHelpers.ensureDirectory(destDir);
        Path destPath = destDir.resolve(filename);
        CopyHelpers.writeFile(destPath, content);
        files.add(destPath.toString());
    }

    private static Path resolveClasspathResources() {
        return dev.iadev.util.ResourceResolver
                .resolveResourcesRoot(
                        TEMPLATES_SUBDIR + "/"
                                + "_TEMPLATE-IMPLEMENTATION"
                                + "-PLAN.md",
                        3);
    }
}
