package dev.iadev.application.assembler;

import dev.iadev.domain.model.ProjectConfig;
import dev.iadev.template.TemplateEngine;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

/**
 * Assembles {@code adr/} with an ADR index README.md and
 * the ADR template file.
 *
 * <p>This is the twenty-second assembler in the pipeline
 * (position 22 of 25 per RULE-005). It generates two files:
 * <ol>
 *   <li>{@code adr/README.md} — programmatically built
 *       index with project name, empty ADR table, and
 *       creation instructions</li>
 *   <li>{@code adr/_TEMPLATE-ADR.md} — copied verbatim
 *       from resources after validating mandatory sections</li>
 * </ol>
 *
 * <p>Double graceful no-op:
 * <ol>
 *   <li>If the ADR template does not exist in resources,
 *       returns an empty list.</li>
 *   <li>If the template exists but lacks mandatory sections
 *       (Status, Context, Decision, Consequences), returns
 *       an empty list.</li>
 * </ol>
 *
 * <p>Also provides static utility methods for downstream
 * ADR management:
 * <ul>
 *   <li>{@link #getNextAdrNumber(Path)} — scans existing ADRs
 *       and returns next sequential number</li>
 *   <li>{@link #formatAdrFilename(int, String)} — formats
 *       an ADR filename with zero-padded number and kebab-case
 *       title</li>
 * </ul>
 *
 * <p>Example usage:
 * <pre>{@code
 * Assembler adr = new DocsAdrAssembler();
 * List<String> files = adr.assemble(
 *     config, engine, outputDir);
 * int next = DocsAdrAssembler.getNextAdrNumber(adrDir);
 * String name = DocsAdrAssembler.formatAdrFilename(
 *     1, "My Decision");
 * }</pre>
 * </p>
 *
 * @see Assembler
 */
public final class DocsAdrAssembler implements Assembler {

    private static final String TEMPLATE_FILENAME =
            "_TEMPLATE-ADR.md";
    private static final String TEMPLATES_SUBDIR =
            "shared/templates";
    private static final String ADR_OUTPUT_SUBDIR =
            "adr";
    private static final String README_FILENAME =
            "README.md";
    private static final String ADR_TITLE_HEADING =
            "# Architecture Decision Records";

    private static final Pattern ADR_FILE_PATTERN =
            Pattern.compile("^ADR-(\\d{4,})-.*\\.md$");
    private static final int ADR_NUMBER_PAD_WIDTH = 4;

    private static final List<String> MANDATORY_SECTIONS =
            List.of(
                    "Status",
                    "Context",
                    "Decision",
                    "Consequences");

    private final Path resourcesDir;

    /**
     * Creates a DocsAdrAssembler using classpath resources.
     */
    public DocsAdrAssembler() {
        this(resolveClasspathResources());
    }

    /**
     * Creates a DocsAdrAssembler with an explicit resources
     * directory.
     *
     * @param resourcesDir the base resources directory
     */
    public DocsAdrAssembler(Path resourcesDir) {
        this.resourcesDir = resourcesDir;
    }

    /**
     * {@inheritDoc}
     *
     * <p>Validates the ADR template, builds the README.md
     * index, copies the template verbatim, and writes both
     * to {@code adr/}.</p>
     */
    @Override
    public List<String> assemble(
            ProjectConfig config,
            TemplateEngine engine,
            Path outputDir) {
        String templateContent =
                loadValidatedTemplate();
        if (templateContent == null) {
            return List.of();
        }
        return writeAdrFiles(
                config, outputDir, templateContent);
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

    private List<String> writeAdrFiles(
            ProjectConfig config, Path outputDir,
            String templateContent) {
        Path adrDir =
                outputDir.resolve(ADR_OUTPUT_SUBDIR);
        CopyHelpers.ensureDirectory(adrDir);

        String readmeContent =
                buildReadmeContent(
                        config.project().name());
        Path readmeDest =
                adrDir.resolve(README_FILENAME);
        CopyHelpers.writeFile(readmeDest, readmeContent);

        Path templateDest =
                adrDir.resolve(TEMPLATE_FILENAME);
        CopyHelpers.writeFile(
                templateDest, templateContent);

        return List.of(
                readmeDest.toString(),
                templateDest.toString());
    }

    /**
     * Builds the README.md content for the ADR index.
     *
     * <p>Produces a markdown document with a title heading,
     * project-name blockquote, empty ADR table, and
     * instructions for creating new ADRs.</p>
     *
     * @param projectName the project name for the heading
     * @return the complete README.md content
     */
    static String buildReadmeContent(
            String projectName) {
        return (ADR_TITLE_HEADING + "\n"
                + "\n"
                + "> Architecture Decision Records for"
                + " **%s**.\n"
                + "\n"
                + "| ID | Title | Status | Date |\n"
                + "|----|-------|--------|------|\n"
                + "\n"
                + "## Creating a New ADR\n"
                + "\n"
                + "Copy `_TEMPLATE-ADR.md` and follow the"
                + " naming convention:\n"
                + "`ADR-NNNN-title-in-kebab-case.md`\n")
                        .formatted(projectName);
    }

    /**
     * Checks that the ADR template contains all mandatory
     * sections.
     *
     * @param templateContent the template file content
     * @return true if all four sections are present
     */
    static boolean hasAllMandatorySections(
            String templateContent) {
        return CopyHelpers.hasAllMandatorySections(
                templateContent, MANDATORY_SECTIONS);
    }

    /**
     * Scans existing ADR files and returns the next
     * sequential number.
     *
     * <p>Looks for files matching the pattern
     * {@code ADR-NNNN-*.md} in the given directory. Returns
     * the maximum number found plus one, or 1 if the
     * directory is empty or does not exist.</p>
     *
     * @param adrDir the ADR directory to scan
     * @return the next ADR number (1 if empty or missing)
     */
    public static int getNextAdrNumber(Path adrDir) {
        if (!Files.exists(adrDir)) {
            return 1;
        }
        try (Stream<Path> stream = Files.list(adrDir)) {
            int max = stream
                    .map(p -> p.getFileName().toString())
                    .map(ADR_FILE_PATTERN::matcher)
                    .filter(Matcher::matches)
                    .mapToInt(m ->
                            Integer.parseInt(m.group(1)))
                    .max()
                    .orElse(0);
            return max + 1;
        } catch (IOException e) {
            return 1;
        }
    }

    /**
     * Formats an ADR filename from a number and title.
     *
     * <p>Produces a filename like
     * {@code ADR-0001-title-in-kebab-case.md} with
     * zero-padded number and sanitized kebab-case title.</p>
     *
     * @param num   the ADR sequential number
     * @param title the ADR title in plain text
     * @return formatted filename
     */
    public static String formatAdrFilename(
            int num, String title) {
        String padded = "%04d".formatted(num);
        String sanitized = title.toLowerCase()
                .replaceAll("[^a-z0-9-]+", "-")
                .replaceAll("-{2,}", "-")
                .replaceAll("^-|-$", "");
        String slug = sanitized.isEmpty()
                ? "untitled" : sanitized;
        return "ADR-%s-%s.md".formatted(padded, slug);
    }

    private static Path resolveClasspathResources() {
        return dev.iadev.util.ResourceResolver
                .resolveResourcesRoot(
                        TEMPLATES_SUBDIR + "/"
                                + TEMPLATE_FILENAME,
                        3);
    }
}
