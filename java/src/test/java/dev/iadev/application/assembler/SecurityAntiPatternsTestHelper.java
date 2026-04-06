package dev.iadev.application.assembler;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Shared test helpers for SecurityAntiPatternsRuleWriter
 * tests.
 */
final class SecurityAntiPatternsTestHelper {

    private SecurityAntiPatternsTestHelper() {
    }

    static Path createResources(Path tempDir)
            throws IOException {
        Path resourceDir = tempDir.resolve("res");
        Path secAntiDir = resourceDir.resolve(
                "targets/claude/rules/conditional/"
                        + "security-anti-patterns");
        Files.createDirectories(secAntiDir);
        return resourceDir;
    }

    static Path createResourcesWithTemplate(
            Path tempDir,
            String language)
            throws IOException {
        Path resourceDir = createResources(tempDir);
        Path secAntiDir = resourceDir.resolve(
                "targets/claude/rules/conditional/"
                        + "security-anti-patterns");

        String templateName =
                "12-security-anti-patterns.%s.md"
                        .formatted(language);

        String content = loadSecurityAntiPatternTemplate(
                language);
        Files.writeString(
                secAntiDir.resolve(templateName),
                content,
                StandardCharsets.UTF_8);
        return resourceDir;
    }

    private static String loadSecurityAntiPatternTemplate(
            String language) {
        var url = SecurityAntiPatternsTestHelper.class
                .getClassLoader()
                .getResource(
                        "targets/claude/rules/conditional/"
                                + "security-anti-patterns/"
                                + "12-security-anti-patterns."
                                + language + ".md");
        if (url == null) {
            return buildFallbackTemplate(language);
        }
        try {
            return Files.readString(
                    Path.of(url.getPath()),
                    StandardCharsets.UTF_8);
        } catch (IOException e) {
            return buildFallbackTemplate(language);
        }
    }

    private static String buildFallbackTemplate(
            String language) {
        return "# Fallback template for " + language;
    }
}
