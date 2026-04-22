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
 * Tests for SecurityAntiPatternsRuleWriter — Java
 * and Kotlin profiles generate 8 security anti-patterns
 * (J1-J8).
 */
@DisplayName("SecurityAntiPatternsRuleWriter — Java")
class SecurityAntiPatternsRuleWriterJavaTest {

    @Nested
    @DisplayName("Java generates 8 security anti-patterns"
            + " (J1-J8)")
    class JavaSecurityAntiPatterns {

        @Test
        @DisplayName("java generates"
                + " 12-security-anti-patterns.md"
                + " with 8 entries")
        void write_java_generatesEightEntries(
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

            assertThat(files).hasSize(1);
            assertThat(files.get(0))
                    .contains(
                            "12-security-anti-patterns.md");

            String content = Files.readString(
                    rulesDir.resolve(
                            "12-security-anti-patterns.md"),
                    StandardCharsets.UTF_8);
            assertThat(content)
                    .contains("Security Anti-Patterns");

            long count = content.lines()
                    .filter(l -> l.startsWith("### J"))
                    .count();
            assertThat(count)
                    .as("Must have 8 security"
                            + " anti-patterns (J1-J8)")
                    .isEqualTo(8);
        }

        @Test
        @DisplayName("each anti-pattern has CWE reference")
        void write_java_hasCweReferences(
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
            ProjectConfig config = TestConfigBuilder
                    .builder()
                    .language("java", "21")
                    .framework("spring-boot", "3.4")
                    .build();

            writer.copyConditionalSecurityAntiPatternsRule(
                    config, rulesDir,
                    new TemplateEngine(), Map.of());

            String content = Files.readString(
                    rulesDir.resolve(
                            "12-security-anti-patterns.md"),
                    StandardCharsets.UTF_8);

            assertThat(content)
                    .contains("CWE-89")
                    .contains("CWE-330")
                    .contains("CWE-502")
                    .contains("CWE-798")
                    .contains("CWE-295")
                    .contains("CWE-22")
                    .contains("CWE-209")
                    .contains("CWE-942");
        }

        @Test
        @DisplayName("each anti-pattern has vulnerable"
                + " and fixed code blocks")
        void write_java_hasCodeBlocks(
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
            ProjectConfig config = TestConfigBuilder
                    .builder()
                    .language("java", "21")
                    .framework("quarkus", "3.17")
                    .build();

            writer.copyConditionalSecurityAntiPatternsRule(
                    config, rulesDir,
                    new TemplateEngine(), Map.of());

            String content = Files.readString(
                    rulesDir.resolve(
                            "12-security-anti-patterns.md"),
                    StandardCharsets.UTF_8);

            assertThat(content)
                    .contains("#### Vulnerable Code")
                    .contains("#### Fixed Code")
                    .contains("#### Why it is dangerous");
        }

        @Test
        @DisplayName("each anti-pattern has severity")
        void write_java_hasSeverity(
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
            ProjectConfig config = TestConfigBuilder
                    .builder()
                    .language("java", "21")
                    .framework("spring-boot", "3.4")
                    .build();

            writer.copyConditionalSecurityAntiPatternsRule(
                    config, rulesDir,
                    new TemplateEngine(), Map.of());

            String content = Files.readString(
                    rulesDir.resolve(
                            "12-security-anti-patterns.md"),
                    StandardCharsets.UTF_8);

            long severityCount = content.lines()
                    .filter(l -> l.startsWith(
                            "**Severity:**"))
                    .count();
            assertThat(severityCount)
                    .as("Each anti-pattern must have"
                            + " severity")
                    .isEqualTo(8);
        }
    }

    @Nested
    @DisplayName("Java does not contain other-language"
            + " anti-patterns")
    class JavaLanguageIsolation {

        @Test
        @DisplayName("java output does not contain Python"
                + " or Go or TypeScript patterns")
        void write_java_noOtherLanguagePatterns(
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
            ProjectConfig config = TestConfigBuilder
                    .builder()
                    .language("java", "21")
                    .framework("spring-boot", "3.4")
                    .build();

            writer.copyConditionalSecurityAntiPatternsRule(
                    config, rulesDir,
                    new TemplateEngine(), Map.of());

            String content = Files.readString(
                    rulesDir.resolve(
                            "12-security-anti-patterns.md"),
                    StandardCharsets.UTF_8);

            assertThat(content)
                    .doesNotContain("pickle.loads")
                    .doesNotContain("subprocess")
                    .doesNotContain("template.HTML")
                    .doesNotContain("innerHTML")
                    .doesNotContain("Prototype Check");
        }
    }

    // Kotlin security-anti-patterns test removed — kotlin template
    // resource deleted in EPIC-0048 full cleanup.
}
