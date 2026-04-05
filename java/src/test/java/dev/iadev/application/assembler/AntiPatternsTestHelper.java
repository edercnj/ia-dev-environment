package dev.iadev.application.assembler;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Shared test helpers for AntiPatternsRuleWriter tests.
 */
final class AntiPatternsTestHelper {

    private AntiPatternsTestHelper() {
    }

    static Path createResources(Path tempDir)
            throws IOException {
        Path resourceDir = tempDir.resolve("res");
        Path antiDir = resourceDir.resolve(
                "targets/claude/rules/conditional/anti-patterns");
        Files.createDirectories(antiDir);
        return resourceDir;
    }

    static Path createResourcesWithTemplate(
            Path tempDir,
            String language,
            String framework)
            throws IOException {
        Path resourceDir = createResources(tempDir);
        Path antiDir = resourceDir.resolve(
                "targets/claude/rules/conditional/anti-patterns");

        String templateName =
                "10-anti-patterns.%s-%s.md".formatted(
                        language, framework);

        String content = loadAntiPatternTemplate(
                language, framework);
        Files.writeString(
                antiDir.resolve(templateName),
                content,
                StandardCharsets.UTF_8);
        return resourceDir;
    }

    private static String loadAntiPatternTemplate(
            String language, String framework) {
        var url = AntiPatternsTestHelper.class
                .getClassLoader()
                .getResource(
                        "targets/claude/rules/conditional/"
                                + "anti-patterns/"
                                + "10-anti-patterns."
                                + language + "-"
                                + framework + ".md");
        if (url == null) {
            return buildFallbackTemplate(
                    language, framework);
        }
        try {
            return Files.readString(
                    Path.of(url.getPath()),
                    StandardCharsets.UTF_8);
        } catch (IOException e) {
            return buildFallbackTemplate(
                    language, framework);
        }
    }

    private static String buildFallbackTemplate(
            String language, String framework) {
        return "# Fallback template for "
                + language + "-" + framework;
    }
}
