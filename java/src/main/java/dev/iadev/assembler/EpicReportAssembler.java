package dev.iadev.assembler;

import dev.iadev.model.ProjectConfig;
import dev.iadev.template.TemplateEngine;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
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
 * <p>This is the twenty-second assembler in the pipeline
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
            "templates";
    private static final String CLAUDE_OUTPUT_SUBDIR =
            ".claude/templates";
    private static final String GITHUB_OUTPUT_SUBDIR =
            ".github/templates";

    /** The 8 mandatory sections that must be present. */
    static final List<String> MANDATORY_SECTIONS = List.of(
            "## Sumario Executivo",
            "## Timeline de Execucao",
            "## Status Final por Story",
            "## Findings Consolidados",
            "## Coverage Delta",
            "## Commits e SHAs",
            "## Issues Nao Resolvidos",
            "## PR Link"
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
     * the template is missing or does not contain all 8
     * mandatory sections.</p>
     */
    @Override
    public List<String> assemble(
            ProjectConfig config,
            TemplateEngine engine,
            Path outputDir) {
        Path templatePath = resourcesDir
                .resolve(TEMPLATES_SUBDIR)
                .resolve(TEMPLATE_FILENAME);

        if (!Files.exists(templatePath)) {
            return List.of();
        }

        String content = readFile(templatePath);
        if (!hasAllMandatorySections(content)) {
            return List.of();
        }

        List<String> results = new ArrayList<>();
        List<String> outputSubdirs = List.of(
                CLAUDE_OUTPUT_SUBDIR,
                GITHUB_OUTPUT_SUBDIR);

        for (String subdir : outputSubdirs) {
            Path destDir = outputDir.resolve(subdir);
            CopyHelpers.ensureDirectory(destDir);
            Path destPath =
                    destDir.resolve(TEMPLATE_FILENAME);
            writeFile(destPath, content);
            results.add(destPath.toString());
        }

        return results;
    }

    /**
     * Checks that the content contains all 8 mandatory
     * sections.
     *
     * @param content the template content
     * @return true if all mandatory sections are present
     */
    static boolean hasAllMandatorySections(String content) {
        for (String section : MANDATORY_SECTIONS) {
            if (!content.contains(section)) {
                return false;
            }
        }
        return true;
    }

    private static String readFile(Path path) {
        try {
            return Files.readString(
                    path, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException(
                    "Failed to read file: " + path, e);
        }
    }

    private static void writeFile(
            Path dest, String content) {
        try {
            Files.writeString(
                    dest, content, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException(
                    "Failed to write file: " + dest, e);
        }
    }

    private static Path resolveClasspathResources() {
        var url = EpicReportAssembler.class.getClassLoader()
                .getResource(
                        TEMPLATES_SUBDIR + "/"
                                + TEMPLATE_FILENAME);
        if (url == null) {
            return Path.of("src/main/resources");
        }
        return Path.of(url.getPath())
                .getParent().getParent();
    }
}
