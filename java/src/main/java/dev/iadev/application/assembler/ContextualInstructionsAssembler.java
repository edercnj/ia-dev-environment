package dev.iadev.application.assembler;

import dev.iadev.domain.model.ProjectConfig;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Generates contextual instruction files under
 * {@code instructions/*.instructions.md} for GitHub Copilot.
 *
 * <p>Loads templates from
 * {@code github-instructions-templates/} and renders them
 * with single-brace placeholder replacement matching the
 * TypeScript implementation.</p>
 *
 * <p>Extracted from {@link GithubInstructionsAssembler}
 * per story-0008-0014 to satisfy the 250-line SRP
 * constraint.</p>
 *
 * @see GithubInstructionsAssembler
 * @see GlobalInstructionsAssembler
 */
public final class ContextualInstructionsAssembler {

    private static final String TEMPLATES_DIR =
            "github-instructions-templates";

    /** Contextual instruction template names. */
    static final List<String> CONTEXTUAL_INSTRUCTIONS =
            List.of(
                    "domain",
                    "coding-standards",
                    "architecture",
                    "quality-gates");

    /**
     * Pattern for single-brace placeholder replacement.
     * Matches {@code {key}} but not {@code {{key}}}.
     */
    static final Pattern SINGLE_BRACE_PATTERN =
            Pattern.compile("(?<!\\{)\\{(\\w+)\\}(?!\\})");

    private final Path resourcesDir;

    /**
     * Creates a ContextualInstructionsAssembler with an
     * explicit resources directory.
     *
     * @param resourcesDir the base resources directory
     */
    ContextualInstructionsAssembler(Path resourcesDir) {
        this.resourcesDir = resourcesDir;
    }

    /**
     * Generates all contextual instruction files.
     *
     * @param config          the project configuration
     * @param instructionsDir the instructions output
     *                        directory
     * @return list of generated file paths
     */
    List<String> generate(
            ProjectConfig config,
            Path instructionsDir) {
        Path srcDir = resourcesDir.resolve(TEMPLATES_DIR);
        if (!Files.exists(srcDir)
                || !Files.isDirectory(srcDir)) {
            return List.of();
        }

        Map<String, String> context =
                buildPlaceholderContext(config);
        List<String> results = new ArrayList<>();

        for (String name : CONTEXTUAL_INSTRUCTIONS) {
            Path src = srcDir.resolve(name + ".md");
            if (!Files.exists(src)) {
                continue;
            }
            String content = CopyHelpers.readFile(src);
            String rendered =
                    replaceSingleBracePlaceholders(
                            content, context);
            Path dest = instructionsDir.resolve(
                    name + ".instructions.md");
            CopyHelpers.writeFile(dest, rendered);
            results.add(dest.toString());
        }

        return results;
    }

    /**
     * Builds a context map for single-brace placeholder
     * replacement, matching the TypeScript
     * {@code buildDefaultContext} function.
     *
     * @param config the project configuration
     * @return the placeholder context map
     */
    static Map<String, String> buildPlaceholderContext(
            ProjectConfig config) {
        return Map.ofEntries(
                Map.entry("project_name",
                        config.project().name()),
                Map.entry("project_purpose",
                        config.project().purpose()),
                Map.entry("language_name",
                        config.language().name()),
                Map.entry("language_version",
                        config.language().version()),
                Map.entry("framework_name",
                        config.framework().name()),
                Map.entry("framework_version",
                        config.framework().version()),
                Map.entry("build_tool",
                        config.framework().buildTool()),
                Map.entry("architecture_style",
                        config.architecture().style()),
                Map.entry("coverage_line",
                        String.valueOf(
                                config.testing()
                                        .coverageLine())),
                Map.entry("coverage_branch",
                        String.valueOf(
                                config.testing()
                                        .coverageBranch())));
    }

    /**
     * Replaces single-brace {@code {key}} placeholders
     * with values from the context map.
     *
     * <p>Known keys are replaced; unknown keys are
     * preserved verbatim. Does not match double-brace
     * patterns.</p>
     *
     * @param content the content with placeholders
     * @param context the key-value map for replacement
     * @return the content with known placeholders replaced
     */
    static String replaceSingleBracePlaceholders(
            String content,
            Map<String, String> context) {
        Matcher matcher =
                SINGLE_BRACE_PATTERN.matcher(content);
        StringBuilder sb = new StringBuilder();
        while (matcher.find()) {
            String key = matcher.group(1);
            String value = context.get(key);
            if (value != null) {
                matcher.appendReplacement(
                        sb, Matcher.quoteReplacement(value));
            } else {
                matcher.appendReplacement(
                        sb, Matcher.quoteReplacement(
                                matcher.group(0)));
            }
        }
        matcher.appendTail(sb);
        return sb.toString();
    }
}
