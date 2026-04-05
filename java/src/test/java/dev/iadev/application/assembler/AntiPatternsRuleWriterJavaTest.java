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
 * Tests for AntiPatternsRuleWriter — Java profiles
 * (java-spring, java-quarkus).
 */
@DisplayName("AntiPatternsRuleWriter — Java profiles")
class AntiPatternsRuleWriterJavaTest {

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
                    AntiPatternsTestHelper
                            .createResourcesWithTemplate(
                                    tempDir, "java",
                                    "spring-boot");
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
                    AntiPatternsTestHelper
                            .createResourcesWithTemplate(
                                    tempDir, "java",
                                    "spring-boot");
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
                    AntiPatternsTestHelper
                            .createResourcesWithTemplate(
                                    tempDir, "java",
                                    "spring-boot");
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
                    AntiPatternsTestHelper
                            .createResourcesWithTemplate(
                                    tempDir, "java",
                                    "quarkus");
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
                    AntiPatternsTestHelper
                            .createResourcesWithTemplate(
                                    tempDir, "java",
                                    "quarkus");
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
}
