package dev.iadev.application.assembler;

import dev.iadev.domain.model.ProjectConfig;
import dev.iadev.template.TemplateEngine;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

/**
 * Conditionally generates {@code 12-security-anti-patterns.md}
 * based on the project language configuration.
 *
 * <p>Unlike {@link AntiPatternsRuleWriter} which uses
 * language+framework as the key, this writer uses
 * language-only. A Java project gets Java security
 * anti-patterns regardless of whether it uses Spring
 * or Quarkus. Kotlin projects reuse the Java/Kotlin
 * adapted patterns.</p>
 *
 * <p>The writer looks for a template file at
 * {@code targets/claude/rules/conditional/
 * security-anti-patterns/
 * 12-security-anti-patterns.{lang}.md}. When a
 * matching template exists, it copies it with
 * placeholder replacement. When no template matches,
 * no file is generated (silent skip).</p>
 *
 * @see CoreRulesWriter
 * @see RulesAssembler
 */
public final class SecurityAntiPatternsRuleWriter {

    private static final String SEC_ANTI_PATTERNS_DIR =
            "targets/claude/rules/conditional/"
                    + "security-anti-patterns";
    private static final String OUTPUT_FILENAME =
            "12-security-anti-patterns.md";
    private static final String TEMPLATE_PREFIX =
            "12-security-anti-patterns.";
    private static final String TEMPLATE_SUFFIX = ".md";

    private final Path resourcesDir;

    /**
     * Creates a SecurityAntiPatternsRuleWriter with
     * an explicit resources directory.
     *
     * @param resourcesDir the base resources directory
     */
    SecurityAntiPatternsRuleWriter(Path resourcesDir) {
        this.resourcesDir = resourcesDir;
    }

    /**
     * Conditionally generates the security anti-patterns
     * rule file when a matching template exists for the
     * project's language.
     *
     * @param config   the project configuration
     * @param rulesDir the rules output directory
     * @param engine   the template engine
     * @param context  the placeholder context
     * @return list of generated file paths (0 or 1)
     */
    List<String> copyConditionalSecurityAntiPatternsRule(
            ProjectConfig config,
            Path rulesDir,
            TemplateEngine engine,
            Map<String, Object> context) {
        String langName = config.language().name();
        if (langName == null || langName.isBlank()) {
            return List.of();
        }

        Path secAntiDir = resourcesDir.resolve(
                SEC_ANTI_PATTERNS_DIR);
        if (!Files.exists(secAntiDir)
                || !Files.isDirectory(secAntiDir)) {
            return List.of();
        }

        String templateName = TEMPLATE_PREFIX
                + langName + TEMPLATE_SUFFIX;
        Path template = secAntiDir.resolve(templateName);

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
