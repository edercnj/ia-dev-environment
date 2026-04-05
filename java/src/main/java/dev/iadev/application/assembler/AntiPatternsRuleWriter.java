package dev.iadev.application.assembler;

import dev.iadev.domain.model.ProjectConfig;
import dev.iadev.template.TemplateEngine;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

/**
 * Conditionally generates {@code 10-anti-patterns.md}
 * based on language and framework configuration.
 *
 * <p>The writer looks for a template file at
 * {@code targets/claude/rules/conditional/anti-patterns/
 * 10-anti-patterns.{lang}-{fw}.md}. When a matching
 * template exists, it copies it with placeholder
 * replacement. When no template matches, no file is
 * generated (silent skip, not an error).</p>
 *
 * <p>Extracted per SRP — the {@link CoreRulesWriter}
 * delegates anti-pattern generation to this class.</p>
 *
 * @see CoreRulesWriter
 * @see RulesAssembler
 */
public final class AntiPatternsRuleWriter {

    private static final String ANTI_PATTERNS_DIR =
            "targets/claude/rules/conditional/anti-patterns";
    private static final String OUTPUT_FILENAME =
            "10-anti-patterns.md";
    private static final String TEMPLATE_PREFIX =
            "10-anti-patterns.";
    private static final String TEMPLATE_SUFFIX = ".md";

    private final Path resourcesDir;

    /**
     * Creates an AntiPatternsRuleWriter with an explicit
     * resources directory.
     *
     * @param resourcesDir the base resources directory
     */
    AntiPatternsRuleWriter(Path resourcesDir) {
        this.resourcesDir = resourcesDir;
    }

    /**
     * Conditionally generates the anti-patterns rule
     * file when a matching template exists for the
     * project's language+framework combination.
     *
     * @param config   the project configuration
     * @param rulesDir the rules output directory
     * @param engine   the template engine
     * @param context  the placeholder context
     * @return list of generated file paths (0 or 1)
     */
    List<String> copyConditionalAntiPatternsRule(
            ProjectConfig config,
            Path rulesDir,
            TemplateEngine engine,
            Map<String, Object> context) {
        String langName = config.language().name();
        if (langName == null || langName.isBlank()) {
            return List.of();
        }

        String fwName = config.framework().name();
        if (fwName == null || fwName.isBlank()) {
            return List.of();
        }

        Path antiDir = resourcesDir.resolve(
                ANTI_PATTERNS_DIR);
        if (!Files.exists(antiDir)
                || !Files.isDirectory(antiDir)) {
            return List.of();
        }

        String templateName = TEMPLATE_PREFIX
                + langName + "-" + fwName
                + TEMPLATE_SUFFIX;
        Path template = antiDir.resolve(templateName);

        if (!Files.exists(template)
                || !Files.isRegularFile(template)) {
            return List.of();
        }

        Path dest = rulesDir.resolve(OUTPUT_FILENAME);
        String path = CopyHelpers.copyTemplateFile(
                template, dest, engine, context);
        return List.of(path);
    }
}
