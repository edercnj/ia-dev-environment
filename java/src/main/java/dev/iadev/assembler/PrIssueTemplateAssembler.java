package dev.iadev.assembler;

import dev.iadev.config.ContextBuilder;
import dev.iadev.model.ProjectConfig;
import dev.iadev.template.TemplateEngine;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Generates GitHub PR and Issue templates to the
 * {@code .github/} output directory.
 *
 * <p>This assembler is <strong>unconditional</strong>:
 * it always generates for all projects, since PR and
 * issue templates are a universal GitHub practice.</p>
 *
 * <p>Generates 4 files:
 * <ul>
 *   <li>{@code pull_request_template.md}</li>
 *   <li>{@code ISSUE_TEMPLATE/bug_report.md}</li>
 *   <li>{@code ISSUE_TEMPLATE/feature_request.md}</li>
 *   <li>{@code ISSUE_TEMPLATE/config.yml}</li>
 * </ul>
 *
 * <p>The first 3 files are rendered via Pebble templates
 * with project variables. The config.yml is copied
 * statically.</p>
 *
 * @see Assembler
 * @see AssemblerFactory
 */
public final class PrIssueTemplateAssembler
        implements Assembler {

    private static final String TEMPLATES_DIR =
            "github-pr-issue-templates";
    private static final String ISSUE_TEMPLATE_DIR =
            "ISSUE_TEMPLATE";
    private static final String J2_SUFFIX = ".j2";

    /** Pebble template filenames for rendered output. */
    static final List<String> PEBBLE_TEMPLATES = List.of(
            "pull_request_template.md.j2",
            "bug_report.md.j2",
            "feature_request.md.j2");

    /** Static file copied verbatim. */
    static final String CONFIG_FILE = "config.yml";

    private final Path resourcesDir;

    /**
     * Creates a PrIssueTemplateAssembler using classpath
     * resources.
     */
    public PrIssueTemplateAssembler() {
        this(resolveClasspathResources());
    }

    /**
     * Creates a PrIssueTemplateAssembler with an explicit
     * resources directory.
     *
     * @param resourcesDir the base resources directory
     */
    public PrIssueTemplateAssembler(Path resourcesDir) {
        this.resourcesDir = resourcesDir;
    }

    /**
     * {@inheritDoc}
     *
     * <p>Renders PR and Issue templates with Pebble and
     * copies config.yml statically. Returns empty list if
     * the templates source directory does not exist.</p>
     */
    @Override
    public List<String> assemble(
            ProjectConfig config,
            TemplateEngine engine,
            Path outputDir) {
        Path srcDir = resourcesDir.resolve(TEMPLATES_DIR);
        if (!Files.exists(srcDir)) {
            return List.of();
        }

        Map<String, Object> context =
                ContextBuilder.buildContext(config);
        List<String> results = new ArrayList<>();

        renderPrTemplate(
                srcDir, outputDir, engine,
                context, results);
        renderIssueTemplates(
                srcDir, outputDir, engine,
                context, results);
        copyConfigYml(srcDir, outputDir, results);

        return results;
    }

    private void renderPrTemplate(
            Path srcDir, Path outputDir,
            TemplateEngine engine,
            Map<String, Object> context,
            List<String> results) {
        String templateName =
                "pull_request_template.md.j2";
        Path src = srcDir.resolve(templateName);
        if (!Files.exists(src)) {
            return;
        }
        String outputName = stripJ2Suffix(templateName);
        String content = engine.render(
                TEMPLATES_DIR + "/" + templateName,
                context);
        Path dest = outputDir.resolve(outputName);
        CopyHelpers.writeFile(dest, content);
        results.add(dest.toString());
    }

    private void renderIssueTemplates(
            Path srcDir, Path outputDir,
            TemplateEngine engine,
            Map<String, Object> context,
            List<String> results) {
        Path issueDir =
                outputDir.resolve(ISSUE_TEMPLATE_DIR);
        CopyHelpers.ensureDirectory(issueDir);

        for (String templateName : List.of(
                "bug_report.md.j2",
                "feature_request.md.j2")) {
            renderSingleIssueTemplate(
                    srcDir, issueDir, templateName,
                    engine, context, results);
        }
    }

    private void renderSingleIssueTemplate(
            Path srcDir, Path issueDir,
            String templateName,
            TemplateEngine engine,
            Map<String, Object> context,
            List<String> results) {
        Path src = srcDir.resolve(templateName);
        if (!Files.exists(src)) {
            return;
        }
        String outputName = stripJ2Suffix(templateName);
        String content = engine.render(
                TEMPLATES_DIR + "/" + templateName,
                context);
        Path dest = issueDir.resolve(outputName);
        CopyHelpers.writeFile(dest, content);
        results.add(dest.toString());
    }

    private void copyConfigYml(
            Path srcDir, Path outputDir,
            List<String> results) {
        Path src = srcDir.resolve(CONFIG_FILE);
        if (!Files.exists(src)) {
            return;
        }
        Path issueDir =
                outputDir.resolve(ISSUE_TEMPLATE_DIR);
        CopyHelpers.ensureDirectory(issueDir);
        Path dest = issueDir.resolve(CONFIG_FILE);
        CopyHelpers.copyStaticFile(src, dest);
        results.add(dest.toString());
    }

    private static String stripJ2Suffix(String name) {
        return name.endsWith(J2_SUFFIX)
                ? name.substring(0,
                name.length() - J2_SUFFIX.length())
                : name;
    }

    private static Path resolveClasspathResources() {
        return dev.iadev.util.ResourceResolver
                .resolveResourcesRoot(TEMPLATES_DIR);
    }
}
