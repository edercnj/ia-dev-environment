package dev.iadev.assembler;

import dev.iadev.model.ProjectConfig;
import dev.iadev.template.TemplateEngine;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Assembles {@code .github/hooks/} with hook JSON files
 * for GitHub Copilot event-driven automations.
 *
 * <p>This is the twelfth assembler in the pipeline
 * (position 12 of 23 per RULE-005). It copies three
 * pre-defined hook JSON templates verbatim — no template
 * rendering or placeholder replacement is applied.</p>
 *
 * <p>The {@code engine} parameter is accepted for API
 * uniformity per RULE-004 but is not used. If the source
 * templates directory does not exist, the assembler
 * returns an empty list (graceful no-op).</p>
 *
 * <p>Example usage:
 * <pre>{@code
 * Assembler hooks = new GithubHooksAssembler();
 * List<String> files = hooks.assemble(
 *     config, engine, outputDir);
 * }</pre>
 * </p>
 *
 * @see Assembler
 */
public final class GithubHooksAssembler
        implements Assembler {

    private static final String TEMPLATES_DIR =
            "github-hooks-templates";
    private static final String HOOKS_OUTPUT = "hooks";

    /** The 3 GitHub Copilot hook template filenames. */
    static final List<String> GITHUB_HOOK_TEMPLATES =
            List.of(
                    "post-compile-check.json",
                    "pre-commit-lint.json",
                    "session-context-loader.json");

    private final Path resourcesDir;

    /**
     * Creates a GithubHooksAssembler using classpath
     * resources.
     */
    public GithubHooksAssembler() {
        this(resolveClasspathResources());
    }

    /**
     * Creates a GithubHooksAssembler with an explicit
     * resources directory.
     *
     * @param resourcesDir the base resources directory
     */
    public GithubHooksAssembler(Path resourcesDir) {
        this.resourcesDir = resourcesDir;
    }

    /**
     * {@inheritDoc}
     *
     * <p>Copies hook JSON templates verbatim to the
     * output directory. Returns an empty list if the
     * templates directory does not exist.</p>
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
        Path hooksDir = outputDir.resolve(HOOKS_OUTPUT);
        CopyHelpers.ensureDirectory(hooksDir);
        List<String> results = new ArrayList<>();
        for (String template : GITHUB_HOOK_TEMPLATES) {
            Path src = srcDir.resolve(template);
            if (!Files.exists(src)) {
                continue;
            }
            Path dest = hooksDir.resolve(template);
            results.add(
                    CopyHelpers.copyStaticFile(src, dest));
        }
        return results;
    }

    private static Path resolveClasspathResources() {
        return dev.iadev.util.ResourceResolver
                .resolveResourcesRoot(TEMPLATES_DIR);
    }
}
