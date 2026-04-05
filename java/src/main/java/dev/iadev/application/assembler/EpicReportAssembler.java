package dev.iadev.application.assembler;

import dev.iadev.domain.model.ProjectConfig;
import dev.iadev.template.TemplateEngine;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Copies the epic execution report template to two output
 * locations for runtime resolution.
 *
 * <p>The template contains {@code {{PLACEHOLDER}}} tokens
 * intended for runtime resolution by the consolidation
 * subagent, NOT for build-time rendering. Content is copied
 * verbatim.</p>
 *
 * <p>This is the twenty-fourth assembler in the pipeline
 * (position 22 of 23 per RULE-005). Its target is
 * {@link AssemblerTarget#ROOT}.</p>
 *
 * @see Assembler
 */
public final class EpicReportAssembler
        implements Assembler {

    private static final String TEMPLATE_FILENAME =
            "_TEMPLATE-EPIC-EXECUTION-REPORT.md";
    private static final String TEMPLATES_SUBDIR =
            "shared/templates";
    private static final String CLAUDE_OUTPUT_SUBDIR =
            ".claude/templates";
    private static final String GITHUB_OUTPUT_SUBDIR =
            ".github/templates";

    /** The 9 mandatory sections that must be present. */
    static final List<String> MANDATORY_SECTIONS = List.of(
            "Sumário Executivo",
            "Timeline de Execução",
            "Status Final por Story",
            "Findings Consolidados",
            "Coverage Delta",
            "TDD Compliance",
            "Commits e SHAs",
            "Issues Não Resolvidos",
            "PR Link"
    );

    private final Path resourcesDir;

    /**
     * Creates an EpicReportAssembler using classpath
     * resources.
     */
    public EpicReportAssembler() {
        this(resolveClasspathResources());
    }

    /**
     * Creates an EpicReportAssembler with an explicit
     * resources directory.
     *
     * @param resourcesDir the base resources directory
     */
    public EpicReportAssembler(Path resourcesDir) {
        this.resourcesDir = resourcesDir;
    }

    /**
     * {@inheritDoc}
     *
     * <p>Copies the epic report template verbatim to
     * {@code .claude/templates/} and
     * {@code .github/templates/}. Returns empty list if
     * the template is missing or does not contain all 9
     * mandatory sections.</p>
     */
    @Override
    public List<String> assemble(
            ProjectConfig config,
            TemplateEngine engine,
            Path outputDir) {
        String content = loadValidatedTemplate();
        if (content == null) {
            return List.of();
        }
        return copyToOutputDirs(content, outputDir);
    }

    private String loadValidatedTemplate() {
        Path templatePath = resourcesDir
                .resolve(TEMPLATES_SUBDIR)
                .resolve(TEMPLATE_FILENAME);

        if (!Files.exists(templatePath)) {
            return null;
        }
        String content =
                CopyHelpers.readFile(templatePath);
        return hasAllMandatorySections(content)
                ? content : null;
    }

    private List<String> copyToOutputDirs(
            String content, Path outputDir) {
        List<String> results = new ArrayList<>();
        List<String> outputSubdirs = List.of(
                CLAUDE_OUTPUT_SUBDIR,
                GITHUB_OUTPUT_SUBDIR);

        for (String subdir : outputSubdirs) {
            Path destDir = outputDir.resolve(subdir);
            CopyHelpers.ensureDirectory(destDir);
            Path destPath =
                    destDir.resolve(TEMPLATE_FILENAME);
            CopyHelpers.writeFile(destPath, content);
            results.add(destPath.toString());
        }
        return results;
    }

    /**
     * Checks that the content contains all 9 mandatory
     * sections.
     *
     * @param content the template content
     * @return true if all mandatory sections are present
     */
    static boolean hasAllMandatorySections(String content) {
        return CopyHelpers.hasAllMandatorySections(
                content, MANDATORY_SECTIONS);
    }

    private static Path resolveClasspathResources() {
        return dev.iadev.util.ResourceResolver
                .resolveResourcesRoot(
                        TEMPLATES_SUBDIR + "/"
                                + TEMPLATE_FILENAME,
                        3);
    }
}
