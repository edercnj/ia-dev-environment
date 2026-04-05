package dev.iadev.application.assembler;

import dev.iadev.testutil.TestConfigBuilder;

import dev.iadev.domain.model.ProjectConfig;
import dev.iadev.template.TemplateEngine;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for AntiPatternsRuleWriter — placeholder
 * replacement in templates.
 */
@DisplayName("AntiPatternsRuleWriter — placeholders")
class AntiPatternsPlaceholderTest {

    @Nested
    @DisplayName("placeholder replacement in template")
    class PlaceholderReplacement {

        @Test
        @DisplayName("template placeholders are replaced"
                + " with context values")
        void write_whenCalled_placeholdersReplaced(
                @TempDir Path tempDir)
                throws IOException {
            Path resourceDir = tempDir.resolve("res");
            Path antiDir = resourceDir.resolve(
                    "targets/claude/rules/conditional/anti-patterns");
            Files.createDirectories(antiDir);
            Files.writeString(
                    antiDir.resolve(
                            "10-anti-patterns.java"
                                    + "-spring-boot.md"),
                    "# Anti-Patterns for"
                            + " {LANGUAGE_NAME}\n"
                            + "### ANTI-001: Test (HIGH)\n"
                            + "**Incorrect code:**\n"
                            + "```java\n// bad\n```\n"
                            + "**Correct code:**\n"
                            + "```java\n// good\n```\n"
                            + "**Rule violated:** 03\n",
                    StandardCharsets.UTF_8);

            Path rulesDir = tempDir.resolve("rules");
            Files.createDirectories(rulesDir);

            AntiPatternsRuleWriter writer =
                    new AntiPatternsRuleWriter(resourceDir);
            ProjectConfig config = TestConfigBuilder
                    .builder()
                    .language("java", "21")
                    .framework("spring-boot", "3.4")
                    .build();
            Map<String, Object> context =
                    Map.of("language_name", "java");

            writer.copyConditionalAntiPatternsRule(
                    config, rulesDir,
                    new TemplateEngine(), context);

            String content = Files.readString(
                    rulesDir.resolve(
                            "10-anti-patterns.md"),
                    StandardCharsets.UTF_8);

            assertThat(content)
                    .contains("Anti-Patterns for java")
                    .doesNotContain("{LANGUAGE_NAME}");
        }
    }
}
