package dev.iadev.assembler;

import dev.iadev.domain.model.ProjectConfig;
import dev.iadev.template.TemplateEngine;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * Assembles {@code .github/copilot-instructions.md} and
 * contextual instruction files for GitHub Copilot.
 *
 * <p>This is the eighth assembler in the pipeline (position
 * 8 of 23 per RULE-005). It delegates global instructions
 * generation to {@link GlobalInstructionsAssembler} and
 * contextual instructions to
 * {@link ContextualInstructionsAssembler}.</p>
 *
 * <p>Example usage:
 * <pre>{@code
 * Assembler instructions =
 *     new GithubInstructionsAssembler();
 * List<String> files = instructions.assemble(
 *     config, engine, outputDir);
 * }</pre>
 * </p>
 *
 * @see Assembler
 * @see GlobalInstructionsAssembler
 * @see ContextualInstructionsAssembler
 */
public final class GithubInstructionsAssembler
        implements Assembler {

    /** Contextual instruction template names. */
    static final List<String> CONTEXTUAL_INSTRUCTIONS =
            ContextualInstructionsAssembler
                    .CONTEXTUAL_INSTRUCTIONS;

    /**
     * Pattern for single-brace placeholder replacement.
     * Matches {@code {key}} but not {@code {{key}}}.
     */
    static final Pattern SINGLE_BRACE_PATTERN =
            ContextualInstructionsAssembler
                    .SINGLE_BRACE_PATTERN;

    private final GlobalInstructionsAssembler
            globalAssembler;
    private final ContextualInstructionsAssembler
            contextualAssembler;

    /**
     * Creates a GithubInstructionsAssembler using classpath
     * resources.
     */
    public GithubInstructionsAssembler() {
        this(resolveClasspathResources());
    }

    /**
     * Creates a GithubInstructionsAssembler with an explicit
     * resources directory.
     *
     * @param resourcesDir the base resources directory
     */
    public GithubInstructionsAssembler(Path resourcesDir) {
        this.globalAssembler =
                new GlobalInstructionsAssembler();
        this.contextualAssembler =
                new ContextualInstructionsAssembler(
                        resourcesDir);
    }

    /**
     * {@inheritDoc}
     *
     * <p>Generates the global copilot-instructions.md and
     * four contextual instruction files. Returns the list
     * of generated file paths.</p>
     */
    @Override
    public List<String> assemble(
            ProjectConfig config,
            TemplateEngine engine,
            Path outputDir) {
        CopyHelpers.ensureDirectory(outputDir);

        List<String> results = new ArrayList<>();
        results.add(
                globalAssembler.generate(config, outputDir));

        Path instructionsDir =
                outputDir.resolve("instructions");
        CopyHelpers.ensureDirectory(instructionsDir);
        results.addAll(
                contextualAssembler.generate(
                        config, instructionsDir));

        return results;
    }

    /**
     * Builds the complete copilot-instructions.md content
     * programmatically.
     *
     * @param config the project configuration
     * @return the full markdown content with trailing newline
     */
    static String buildCopilotInstructions(
            ProjectConfig config) {
        return GlobalInstructionsAssembler
                .buildCopilotInstructions(config);
    }

    /**
     * Formats interface types for display.
     *
     * @param config the project configuration
     * @return formatted interface string
     */
    static String formatInterfaces(ProjectConfig config) {
        return GlobalInstructionsAssembler
                .formatInterfaces(config);
    }

    /**
     * Formats the framework version with a leading space.
     *
     * @param config the project configuration
     * @return formatted framework version
     */
    static String formatFrameworkVersion(
            ProjectConfig config) {
        return GlobalInstructionsAssembler
                .formatFrameworkVersion(config);
    }

    /**
     * Builds a context map for single-brace placeholder
     * replacement.
     *
     * @param config the project configuration
     * @return the placeholder context map
     */
    static Map<String, String> buildPlaceholderContext(
            ProjectConfig config) {
        return ContextualInstructionsAssembler
                .buildPlaceholderContext(config);
    }

    /**
     * Replaces single-brace placeholders with context
     * values.
     *
     * @param content the content with placeholders
     * @param context the key-value map for replacement
     * @return the content with known placeholders replaced
     */
    static String replaceSingleBracePlaceholders(
            String content,
            Map<String, String> context) {
        return ContextualInstructionsAssembler
                .replaceSingleBracePlaceholders(
                        content, context);
    }

    private static Path resolveClasspathResources() {
        return dev.iadev.util.ResourceResolver
                .resolveResourcesRoot(
                        "github-instructions-templates");
    }
}
