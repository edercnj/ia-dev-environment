package dev.iadev.application.assembler;

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
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for AntiPatternsRuleWriter — conditionally
 * generates 10-anti-patterns.md based on language and
 * framework configuration.
 *
 * <p>Covers Gherkin scenarios @GK-1 through @GK-6 from
 * story-0017-0001.</p>
 */
@DisplayName("AntiPatternsRuleWriter")
class AntiPatternsRuleWriterTest {

    @Nested
    @DisplayName("GK-1: config without language skips")
    class NoLanguageConfig {

        @Test
        @DisplayName("config without language does not"
                + " generate anti-patterns")
        void write_noLanguage_returnsEmpty(
                @TempDir Path tempDir)
                throws IOException {
            Path resourceDir = createResources(tempDir);
            Path rulesDir = tempDir.resolve("rules");
            Files.createDirectories(rulesDir);

            AntiPatternsRuleWriter writer =
                    new AntiPatternsRuleWriter(resourceDir);
            ProjectConfig config = TestConfigBuilder
                    .builder()
                    .language("", "")
                    .framework("", "")
                    .build();

            List<String> files =
                    writer.copyConditionalAntiPatternsRule(
                            config, rulesDir,
                            new TemplateEngine(), Map.of());

            assertThat(files).isEmpty();
            assertThat(rulesDir.resolve(
                    "10-anti-patterns.md"))
                    .doesNotExist();
        }

        @Test
        @DisplayName("config with null-like language"
                + " does not generate anti-patterns")
        void write_blankLanguage_returnsEmpty(
                @TempDir Path tempDir)
                throws IOException {
            Path resourceDir = createResources(tempDir);
            Path rulesDir = tempDir.resolve("rules");
            Files.createDirectories(rulesDir);

            AntiPatternsRuleWriter writer =
                    new AntiPatternsRuleWriter(resourceDir);
            ProjectConfig config = TestConfigBuilder
                    .builder()
                    .language("  ", "")
                    .framework("spring-boot", "3.4")
                    .build();

            List<String> files =
                    writer.copyConditionalAntiPatternsRule(
                            config, rulesDir,
                            new TemplateEngine(), Map.of());

            assertThat(files).isEmpty();
        }
    }

    @Nested
    @DisplayName("GK-2: java-spring generates 10+ entries")
    class JavaSpringAntiPatterns {

        @Test
        @DisplayName("java-spring generates"
                + " 10-anti-patterns.md with 10+ entries")
        void write_javaSpring_generatesTenPlus(
                @TempDir Path tempDir)
                throws IOException {
            Path resourceDir =
                    createResourcesWithTemplate(
                            tempDir, "java", "spring-boot");
            Path rulesDir = tempDir.resolve("rules");
            Files.createDirectories(rulesDir);

            AntiPatternsRuleWriter writer =
                    new AntiPatternsRuleWriter(resourceDir);
            ProjectConfig config = TestConfigBuilder
                    .builder()
                    .language("java", "21")
                    .framework("spring-boot", "3.4")
                    .build();

            List<String> files =
                    writer.copyConditionalAntiPatternsRule(
                            config, rulesDir,
                            new TemplateEngine(), Map.of());

            assertThat(files).hasSize(1);
            assertThat(files.get(0))
                    .contains("10-anti-patterns.md");

            String content = Files.readString(
                    rulesDir.resolve(
                            "10-anti-patterns.md"),
                    StandardCharsets.UTF_8);
            assertThat(content)
                    .contains("Anti-Patterns");

            long antiPatternCount = content.lines()
                    .filter(l -> l.startsWith("### ANTI-"))
                    .count();
            assertThat(antiPatternCount)
                    .as("Must have 10+ anti-patterns")
                    .isGreaterThanOrEqualTo(10);
        }

        @Test
        @DisplayName("each anti-pattern has incorrect"
                + " and correct code blocks")
        void write_javaSpring_hasCodeBlocks(
                @TempDir Path tempDir)
                throws IOException {
            Path resourceDir =
                    createResourcesWithTemplate(
                            tempDir, "java", "spring-boot");
            Path rulesDir = tempDir.resolve("rules");
            Files.createDirectories(rulesDir);

            AntiPatternsRuleWriter writer =
                    new AntiPatternsRuleWriter(resourceDir);
            ProjectConfig config = TestConfigBuilder
                    .builder()
                    .language("java", "21")
                    .framework("spring-boot", "3.4")
                    .build();

            writer.copyConditionalAntiPatternsRule(
                    config, rulesDir,
                    new TemplateEngine(), Map.of());

            String content = Files.readString(
                    rulesDir.resolve(
                            "10-anti-patterns.md"),
                    StandardCharsets.UTF_8);

            assertThat(content)
                    .contains("**Incorrect code:**")
                    .contains("**Correct code:**");
        }

        @Test
        @DisplayName("each anti-pattern has a rule"
                + " reference")
        void write_javaSpring_hasRuleReference(
                @TempDir Path tempDir)
                throws IOException {
            Path resourceDir =
                    createResourcesWithTemplate(
                            tempDir, "java", "spring-boot");
            Path rulesDir = tempDir.resolve("rules");
            Files.createDirectories(rulesDir);

            AntiPatternsRuleWriter writer =
                    new AntiPatternsRuleWriter(resourceDir);
            ProjectConfig config = TestConfigBuilder
                    .builder()
                    .language("java", "21")
                    .framework("spring-boot", "3.4")
                    .build();

            writer.copyConditionalAntiPatternsRule(
                    config, rulesDir,
                    new TemplateEngine(), Map.of());

            String content = Files.readString(
                    rulesDir.resolve(
                            "10-anti-patterns.md"),
                    StandardCharsets.UTF_8);

            assertThat(content)
                    .contains("**Rule violated:**");
        }
    }

    @Nested
    @DisplayName("GK-3: go-gin generates Go-specific"
            + " anti-patterns")
    class GoGinAntiPatterns {

        @Test
        @DisplayName("go-gin generates 4+ anti-patterns")
        void write_goGin_generatesFourPlus(
                @TempDir Path tempDir)
                throws IOException {
            Path resourceDir =
                    createResourcesWithTemplate(
                            tempDir, "go", "gin");
            Path rulesDir = tempDir.resolve("rules");
            Files.createDirectories(rulesDir);

            AntiPatternsRuleWriter writer =
                    new AntiPatternsRuleWriter(resourceDir);
            ProjectConfig config = TestConfigBuilder
                    .builder()
                    .language("go", "1.22")
                    .framework("gin", "1.10")
                    .build();

            List<String> files =
                    writer.copyConditionalAntiPatternsRule(
                            config, rulesDir,
                            new TemplateEngine(), Map.of());

            assertThat(files).hasSize(1);

            String content = Files.readString(
                    rulesDir.resolve(
                            "10-anti-patterns.md"),
                    StandardCharsets.UTF_8);

            long count = content.lines()
                    .filter(l -> l.startsWith("### ANTI-"))
                    .count();
            assertThat(count)
                    .as("Must have 4+ anti-patterns")
                    .isGreaterThanOrEqualTo(4);
        }

        @Test
        @DisplayName("go-gin contains goroutine leak"
                + " anti-pattern")
        void write_goGin_containsGoroutineLeak(
                @TempDir Path tempDir)
                throws IOException {
            Path resourceDir =
                    createResourcesWithTemplate(
                            tempDir, "go", "gin");
            Path rulesDir = tempDir.resolve("rules");
            Files.createDirectories(rulesDir);

            AntiPatternsRuleWriter writer =
                    new AntiPatternsRuleWriter(resourceDir);
            ProjectConfig config = TestConfigBuilder
                    .builder()
                    .language("go", "1.22")
                    .framework("gin", "1.10")
                    .build();

            writer.copyConditionalAntiPatternsRule(
                    config, rulesDir,
                    new TemplateEngine(), Map.of());

            String content = Files.readString(
                    rulesDir.resolve(
                            "10-anti-patterns.md"),
                    StandardCharsets.UTF_8);

            assertThat(content)
                    .containsIgnoringCase(
                            "goroutine leak");
        }

        @Test
        @DisplayName("go-gin contains panic in handler"
                + " anti-pattern")
        void write_goGin_containsPanicHandler(
                @TempDir Path tempDir)
                throws IOException {
            Path resourceDir =
                    createResourcesWithTemplate(
                            tempDir, "go", "gin");
            Path rulesDir = tempDir.resolve("rules");
            Files.createDirectories(rulesDir);

            AntiPatternsRuleWriter writer =
                    new AntiPatternsRuleWriter(resourceDir);
            ProjectConfig config = TestConfigBuilder
                    .builder()
                    .language("go", "1.22")
                    .framework("gin", "1.10")
                    .build();

            writer.copyConditionalAntiPatternsRule(
                    config, rulesDir,
                    new TemplateEngine(), Map.of());

            String content = Files.readString(
                    rulesDir.resolve(
                            "10-anti-patterns.md"),
                    StandardCharsets.UTF_8);

            assertThat(content)
                    .containsIgnoringCase("panic");
        }
    }

    @Nested
    @DisplayName("GK-5: go-gin does not contain Java"
            + " anti-patterns")
    class CrossLanguageIsolation {

        @Test
        @DisplayName("go-gin output does not contain"
                + " @Transactional")
        void write_goGin_noTransactional(
                @TempDir Path tempDir)
                throws IOException {
            Path resourceDir =
                    createResourcesWithTemplate(
                            tempDir, "go", "gin");
            Path rulesDir = tempDir.resolve("rules");
            Files.createDirectories(rulesDir);

            AntiPatternsRuleWriter writer =
                    new AntiPatternsRuleWriter(resourceDir);
            ProjectConfig config = TestConfigBuilder
                    .builder()
                    .language("go", "1.22")
                    .framework("gin", "1.10")
                    .build();

            writer.copyConditionalAntiPatternsRule(
                    config, rulesDir,
                    new TemplateEngine(), Map.of());

            String content = Files.readString(
                    rulesDir.resolve(
                            "10-anti-patterns.md"),
                    StandardCharsets.UTF_8);

            assertThat(content)
                    .doesNotContain("@Transactional")
                    .doesNotContain("@Autowired")
                    .doesNotContain("Spring")
                    .doesNotContain("JPA");
        }
    }

    @Nested
    @DisplayName("GK-6: java-quarkus contains"
            + " Quarkus-specific anti-patterns")
    class JavaQuarkusAntiPatterns {

        @Test
        @DisplayName("java-quarkus contains static"
                + " @Inject anti-pattern")
        void write_javaQuarkus_containsStaticInject(
                @TempDir Path tempDir)
                throws IOException {
            Path resourceDir =
                    createResourcesWithTemplate(
                            tempDir, "java", "quarkus");
            Path rulesDir = tempDir.resolve("rules");
            Files.createDirectories(rulesDir);

            AntiPatternsRuleWriter writer =
                    new AntiPatternsRuleWriter(resourceDir);
            ProjectConfig config = TestConfigBuilder
                    .builder()
                    .language("java", "21")
                    .framework("quarkus", "3.17")
                    .build();

            writer.copyConditionalAntiPatternsRule(
                    config, rulesDir,
                    new TemplateEngine(), Map.of());

            String content = Files.readString(
                    rulesDir.resolve(
                            "10-anti-patterns.md"),
                    StandardCharsets.UTF_8);

            assertThat(content)
                    .contains("static")
                    .contains("@Inject");
        }

        @Test
        @DisplayName("java-quarkus contains blocking"
                + " I/O in event loop anti-pattern")
        void write_javaQuarkus_containsBlockingIO(
                @TempDir Path tempDir)
                throws IOException {
            Path resourceDir =
                    createResourcesWithTemplate(
                            tempDir, "java", "quarkus");
            Path rulesDir = tempDir.resolve("rules");
            Files.createDirectories(rulesDir);

            AntiPatternsRuleWriter writer =
                    new AntiPatternsRuleWriter(resourceDir);
            ProjectConfig config = TestConfigBuilder
                    .builder()
                    .language("java", "21")
                    .framework("quarkus", "3.17")
                    .build();

            writer.copyConditionalAntiPatternsRule(
                    config, rulesDir,
                    new TemplateEngine(), Map.of());

            String content = Files.readString(
                    rulesDir.resolve(
                            "10-anti-patterns.md"),
                    StandardCharsets.UTF_8);

            assertThat(content)
                    .containsIgnoringCase(
                            "blocking");
        }
    }

    @Nested
    @DisplayName("template missing — silent skip")
    class TemplateMissing {

        @Test
        @DisplayName("unknown stack returns empty list")
        void write_unknownStack_returnsEmpty(
                @TempDir Path tempDir)
                throws IOException {
            Path resourceDir = createResources(tempDir);
            Path rulesDir = tempDir.resolve("rules");
            Files.createDirectories(rulesDir);

            AntiPatternsRuleWriter writer =
                    new AntiPatternsRuleWriter(resourceDir);
            ProjectConfig config = TestConfigBuilder
                    .builder()
                    .language("cobol", "85")
                    .framework("unknown-fw", "1.0")
                    .build();

            List<String> files =
                    writer.copyConditionalAntiPatternsRule(
                            config, rulesDir,
                            new TemplateEngine(), Map.of());

            assertThat(files).isEmpty();
            assertThat(rulesDir.resolve(
                    "10-anti-patterns.md"))
                    .doesNotExist();
        }

        @Test
        @DisplayName("template dir missing returns"
                + " empty list")
        void write_noTemplateDir_returnsEmpty(
                @TempDir Path tempDir)
                throws IOException {
            Path resourceDir = tempDir.resolve("res");
            Files.createDirectories(resourceDir);
            Path rulesDir = tempDir.resolve("rules");
            Files.createDirectories(rulesDir);

            AntiPatternsRuleWriter writer =
                    new AntiPatternsRuleWriter(resourceDir);
            ProjectConfig config = TestConfigBuilder
                    .builder()
                    .language("java", "21")
                    .framework("spring-boot", "3.4")
                    .build();

            List<String> files =
                    writer.copyConditionalAntiPatternsRule(
                            config, rulesDir,
                            new TemplateEngine(), Map.of());

            assertThat(files).isEmpty();
        }
    }

    @Nested
    @DisplayName("python-fastapi anti-patterns")
    class PythonFastapiAntiPatterns {

        @Test
        @DisplayName("python-fastapi generates 3+"
                + " anti-patterns")
        void write_pythonFastapi_generatesThreePlus(
                @TempDir Path tempDir)
                throws IOException {
            Path resourceDir =
                    createResourcesWithTemplate(
                            tempDir, "python", "fastapi");
            Path rulesDir = tempDir.resolve("rules");
            Files.createDirectories(rulesDir);

            AntiPatternsRuleWriter writer =
                    new AntiPatternsRuleWriter(resourceDir);
            ProjectConfig config = TestConfigBuilder
                    .builder()
                    .language("python", "3.12")
                    .framework("fastapi", "0.115")
                    .build();

            writer.copyConditionalAntiPatternsRule(
                    config, rulesDir,
                    new TemplateEngine(), Map.of());

            String content = Files.readString(
                    rulesDir.resolve(
                            "10-anti-patterns.md"),
                    StandardCharsets.UTF_8);

            long count = content.lines()
                    .filter(l -> l.startsWith("### ANTI-"))
                    .count();
            assertThat(count)
                    .as("Must have 3+ anti-patterns")
                    .isGreaterThanOrEqualTo(3);
        }
    }

    @Nested
    @DisplayName("edge cases — branch coverage")
    class EdgeCases {

        @Test
        @DisplayName("blank framework returns empty")
        void write_blankFramework_returnsEmpty(
                @TempDir Path tempDir)
                throws IOException {
            Path resourceDir = createResources(tempDir);
            Path rulesDir = tempDir.resolve("rules");
            Files.createDirectories(rulesDir);

            AntiPatternsRuleWriter writer =
                    new AntiPatternsRuleWriter(resourceDir);
            ProjectConfig config = TestConfigBuilder
                    .builder()
                    .language("java", "21")
                    .framework("  ", "")
                    .build();

            List<String> files =
                    writer.copyConditionalAntiPatternsRule(
                            config, rulesDir,
                            new TemplateEngine(), Map.of());

            assertThat(files).isEmpty();
        }

        @Test
        @DisplayName("anti-patterns dir is a file"
                + " returns empty")
        void write_antiDirIsFile_returnsEmpty(
                @TempDir Path tempDir)
                throws IOException {
            Path resourceDir = tempDir.resolve("res");
            Path condDir = resourceDir.resolve(
                    "core-rules/conditional");
            Files.createDirectories(condDir);
            Files.writeString(
                    condDir.resolve("anti-patterns"),
                    "not a directory");

            Path rulesDir = tempDir.resolve("rules");
            Files.createDirectories(rulesDir);

            AntiPatternsRuleWriter writer =
                    new AntiPatternsRuleWriter(resourceDir);
            ProjectConfig config = TestConfigBuilder
                    .builder()
                    .language("java", "21")
                    .framework("spring-boot", "3.4")
                    .build();

            List<String> files =
                    writer.copyConditionalAntiPatternsRule(
                            config, rulesDir,
                            new TemplateEngine(), Map.of());

            assertThat(files).isEmpty();
        }

        @Test
        @DisplayName("template name is a directory"
                + " returns empty")
        void write_templateIsDirectory_returnsEmpty(
                @TempDir Path tempDir)
                throws IOException {
            Path resourceDir = tempDir.resolve("res");
            Path antiDir = resourceDir.resolve(
                    "core-rules/conditional/anti-patterns");
            Files.createDirectories(antiDir);
            Files.createDirectories(
                    antiDir.resolve(
                            "10-anti-patterns"
                                    + ".java-spring-boot.md"));

            Path rulesDir = tempDir.resolve("rules");
            Files.createDirectories(rulesDir);

            AntiPatternsRuleWriter writer =
                    new AntiPatternsRuleWriter(resourceDir);
            ProjectConfig config = TestConfigBuilder
                    .builder()
                    .language("java", "21")
                    .framework("spring-boot", "3.4")
                    .build();

            List<String> files =
                    writer.copyConditionalAntiPatternsRule(
                            config, rulesDir,
                            new TemplateEngine(), Map.of());

            assertThat(files).isEmpty();
        }
    }

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
                    "core-rules/conditional/anti-patterns");
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

    private static Path createResources(Path tempDir)
            throws IOException {
        Path resourceDir = tempDir.resolve("res");
        Path antiDir = resourceDir.resolve(
                "core-rules/conditional/anti-patterns");
        Files.createDirectories(antiDir);
        return resourceDir;
    }

    private static Path createResourcesWithTemplate(
            Path tempDir,
            String language,
            String framework)
            throws IOException {
        Path resourceDir = createResources(tempDir);
        Path antiDir = resourceDir.resolve(
                "core-rules/conditional/anti-patterns");

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
        var url = AntiPatternsRuleWriterTest.class
                .getClassLoader()
                .getResource(
                        "core-rules/conditional/"
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
