package dev.iadev.application.assembler;

import dev.iadev.testutil.TestConfigBuilder;

import dev.iadev.domain.model.ProjectConfig;
import dev.iadev.template.TemplateEngine;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for AntiPatternsRuleWriter — edge cases,
 * no-match scenarios, and placeholder replacement.
 */
@DisplayName("AntiPatternsRuleWriter — edge cases")
class AntiPatternsRuleWriterEdgeCaseTest {

    @Nested
    @DisplayName("GK-1: config without language skips")
    class NoLanguageConfig {

        @Test
        @DisplayName("config without language does not"
                + " generate anti-patterns")
        void write_noLanguage_returnsEmpty(
                @TempDir Path tempDir)
                throws IOException {
            Path resourceDir =
                    AntiPatternsTestHelper
                            .createResources(tempDir);
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
            Path resourceDir =
                    AntiPatternsTestHelper
                            .createResources(tempDir);
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
    @DisplayName("template missing — silent skip")
    class TemplateMissing {

        @Test
        @DisplayName("unknown stack returns empty list")
        void write_unknownStack_returnsEmpty(
                @TempDir Path tempDir)
                throws IOException {
            Path resourceDir =
                    AntiPatternsTestHelper
                            .createResources(tempDir);
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
    @DisplayName("edge cases — branch coverage")
    class EdgeCases {

        @Test
        @DisplayName("blank framework returns empty")
        void write_blankFramework_returnsEmpty(
                @TempDir Path tempDir)
                throws IOException {
            Path resourceDir =
                    AntiPatternsTestHelper
                            .createResources(tempDir);
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
}