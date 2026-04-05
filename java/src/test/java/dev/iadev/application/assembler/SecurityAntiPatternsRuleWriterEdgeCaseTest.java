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
 * Tests for SecurityAntiPatternsRuleWriter — edge cases,
 * no-match scenarios, and backward compatibility.
 */
@DisplayName("SecurityAntiPatternsRuleWriter — edge cases")
class SecurityAntiPatternsRuleWriterEdgeCaseTest {

    @Nested
    @DisplayName("config without language skips generation")
    class NoLanguageConfig {

        @Test
        @DisplayName("config without language does not"
                + " generate security anti-patterns")
        void write_noLanguage_returnsEmpty(
                @TempDir Path tempDir)
                throws IOException {
            Path resourceDir =
                    SecurityAntiPatternsTestHelper
                            .createResources(tempDir);
            Path rulesDir = tempDir.resolve("rules");
            Files.createDirectories(rulesDir);

            SecurityAntiPatternsRuleWriter writer =
                    new SecurityAntiPatternsRuleWriter(
                            resourceDir);
            ProjectConfig config = TestConfigBuilder
                    .builder()
                    .language("", "")
                    .framework("", "")
                    .build();

            List<String> files =
                    writer
                            .copyConditionalSecurityAntiPatternsRule(
                                    config, rulesDir,
                                    new TemplateEngine(),
                                    Map.of());

            assertThat(files).isEmpty();
            assertThat(rulesDir.resolve(
                    "12-security-anti-patterns.md"))
                    .doesNotExist();
        }

        @Test
        @DisplayName("config with blank language"
                + " does not generate")
        void write_blankLanguage_returnsEmpty(
                @TempDir Path tempDir)
                throws IOException {
            Path resourceDir =
                    SecurityAntiPatternsTestHelper
                            .createResources(tempDir);
            Path rulesDir = tempDir.resolve("rules");
            Files.createDirectories(rulesDir);

            SecurityAntiPatternsRuleWriter writer =
                    new SecurityAntiPatternsRuleWriter(
                            resourceDir);
            ProjectConfig config = TestConfigBuilder
                    .builder()
                    .language("  ", "")
                    .framework("spring-boot", "3.4")
                    .build();

            List<String> files =
                    writer
                            .copyConditionalSecurityAntiPatternsRule(
                                    config, rulesDir,
                                    new TemplateEngine(),
                                    Map.of());

            assertThat(files).isEmpty();
        }
    }

    @Nested
    @DisplayName("template missing — silent skip")
    class TemplateMissing {

        @Test
        @DisplayName("unknown language returns empty list")
        void write_unknownLanguage_returnsEmpty(
                @TempDir Path tempDir)
                throws IOException {
            Path resourceDir =
                    SecurityAntiPatternsTestHelper
                            .createResources(tempDir);
            Path rulesDir = tempDir.resolve("rules");
            Files.createDirectories(rulesDir);

            SecurityAntiPatternsRuleWriter writer =
                    new SecurityAntiPatternsRuleWriter(
                            resourceDir);
            ProjectConfig config = TestConfigBuilder
                    .builder()
                    .language("cobol", "85")
                    .framework("unknown-fw", "1.0")
                    .build();

            List<String> files =
                    writer
                            .copyConditionalSecurityAntiPatternsRule(
                                    config, rulesDir,
                                    new TemplateEngine(),
                                    Map.of());

            assertThat(files).isEmpty();
            assertThat(rulesDir.resolve(
                    "12-security-anti-patterns.md"))
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

            SecurityAntiPatternsRuleWriter writer =
                    new SecurityAntiPatternsRuleWriter(
                            resourceDir);
            ProjectConfig config = TestConfigBuilder
                    .builder()
                    .language("java", "21")
                    .framework("spring-boot", "3.4")
                    .build();

            List<String> files =
                    writer
                            .copyConditionalSecurityAntiPatternsRule(
                                    config, rulesDir,
                                    new TemplateEngine(),
                                    Map.of());

            assertThat(files).isEmpty();
        }
    }

    @Nested
    @DisplayName("edge cases — branch coverage")
    class EdgeCases {

        @Test
        @DisplayName("security-anti-patterns dir is a"
                + " file returns empty")
        void write_secAntiDirIsFile_returnsEmpty(
                @TempDir Path tempDir)
                throws IOException {
            Path resourceDir = tempDir.resolve("res");
            Path condDir = resourceDir.resolve(
                    "targets/claude/rules/conditional");
            Files.createDirectories(condDir);
            Files.writeString(
                    condDir.resolve(
                            "security-anti-patterns"),
                    "not a directory");

            Path rulesDir = tempDir.resolve("rules");
            Files.createDirectories(rulesDir);

            SecurityAntiPatternsRuleWriter writer =
                    new SecurityAntiPatternsRuleWriter(
                            resourceDir);
            ProjectConfig config = TestConfigBuilder
                    .builder()
                    .language("java", "21")
                    .framework("spring-boot", "3.4")
                    .build();

            List<String> files =
                    writer
                            .copyConditionalSecurityAntiPatternsRule(
                                    config, rulesDir,
                                    new TemplateEngine(),
                                    Map.of());

            assertThat(files).isEmpty();
        }

        @Test
        @DisplayName("template name is a directory"
                + " returns empty")
        void write_templateIsDirectory_returnsEmpty(
                @TempDir Path tempDir)
                throws IOException {
            Path resourceDir = tempDir.resolve("res");
            Path secAntiDir = resourceDir.resolve(
                    "targets/claude/rules/conditional/"
                            + "security-anti-patterns");
            Files.createDirectories(secAntiDir);
            Files.createDirectories(
                    secAntiDir.resolve(
                            "12-security-anti-patterns"
                                    + ".java.md"));

            Path rulesDir = tempDir.resolve("rules");
            Files.createDirectories(rulesDir);

            SecurityAntiPatternsRuleWriter writer =
                    new SecurityAntiPatternsRuleWriter(
                            resourceDir);
            ProjectConfig config = TestConfigBuilder
                    .builder()
                    .language("java", "21")
                    .framework("spring-boot", "3.4")
                    .build();

            List<String> files =
                    writer
                            .copyConditionalSecurityAntiPatternsRule(
                                    config, rulesDir,
                                    new TemplateEngine(),
                                    Map.of());

            assertThat(files).isEmpty();
        }
    }

    @Nested
    @DisplayName("backward compatibility")
    class BackwardCompatibility {

        @Test
        @DisplayName("language-only lookup does not"
                + " require framework in template name")
        void write_languageOnly_frameworkNotInTemplateName(
                @TempDir Path tempDir)
                throws IOException {
            Path resourceDir =
                    SecurityAntiPatternsTestHelper
                            .createResourcesWithTemplate(
                                    tempDir, "java");
            Path rulesDir = tempDir.resolve("rules");
            Files.createDirectories(rulesDir);

            SecurityAntiPatternsRuleWriter writer =
                    new SecurityAntiPatternsRuleWriter(
                            resourceDir);

            ProjectConfig springConfig = TestConfigBuilder
                    .builder()
                    .language("java", "21")
                    .framework("spring-boot", "3.4")
                    .build();

            ProjectConfig quarkusConfig = TestConfigBuilder
                    .builder()
                    .language("java", "21")
                    .framework("quarkus", "3.17")
                    .build();

            List<String> springFiles =
                    writer
                            .copyConditionalSecurityAntiPatternsRule(
                                    springConfig, rulesDir,
                                    new TemplateEngine(),
                                    Map.of());

            Path rulesDir2 = tempDir.resolve("rules2");
            Files.createDirectories(rulesDir2);

            List<String> quarkusFiles =
                    writer
                            .copyConditionalSecurityAntiPatternsRule(
                                    quarkusConfig, rulesDir2,
                                    new TemplateEngine(),
                                    Map.of());

            assertThat(springFiles).hasSize(1);
            assertThat(quarkusFiles).hasSize(1);
        }
    }
}
