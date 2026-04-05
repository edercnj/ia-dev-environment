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
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for AntiPatternsRuleWriter — non-Java profiles
 * (go, python, kotlin, rust, typescript) and cross-language
 * isolation.
 */
@DisplayName("AntiPatternsRuleWriter — other languages")
class AntiPatternsRuleWriterOtherLangTest {

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
                    AntiPatternsTestHelper
                            .createResourcesWithTemplate(
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
                    AntiPatternsTestHelper
                            .createResourcesWithTemplate(
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
                    AntiPatternsTestHelper
                            .createResourcesWithTemplate(
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
                    AntiPatternsTestHelper
                            .createResourcesWithTemplate(
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
    @DisplayName("python-fastapi anti-patterns")
    class PythonFastapiAntiPatterns {

        @Test
        @DisplayName("python-fastapi generates 3+"
                + " anti-patterns")
        void write_pythonFastapi_generatesThreePlus(
                @TempDir Path tempDir)
                throws IOException {
            Path resourceDir =
                    AntiPatternsTestHelper
                            .createResourcesWithTemplate(
                                    tempDir, "python",
                                    "fastapi");
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
}
