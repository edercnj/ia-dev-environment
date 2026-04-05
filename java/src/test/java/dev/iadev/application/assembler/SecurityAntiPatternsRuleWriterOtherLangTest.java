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
 * Tests for SecurityAntiPatternsRuleWriter — non-Java
 * profiles (Python, Go, TypeScript, Rust) and
 * cross-language isolation.
 */
@DisplayName("SecurityAntiPatternsRuleWriter — other"
        + " languages")
class SecurityAntiPatternsRuleWriterOtherLangTest {

    @Nested
    @DisplayName("Python generates 5 security"
            + " anti-patterns (P1-P5)")
    class PythonSecurityAntiPatterns {

        @Test
        @DisplayName("python generates 5 entries")
        void write_python_generatesFiveEntries(
                @TempDir Path tempDir)
                throws IOException {
            Path resourceDir =
                    SecurityAntiPatternsTestHelper
                            .createResourcesWithTemplate(
                                    tempDir, "python");
            Path rulesDir = tempDir.resolve("rules");
            Files.createDirectories(rulesDir);

            SecurityAntiPatternsRuleWriter writer =
                    new SecurityAntiPatternsRuleWriter(
                            resourceDir);
            ProjectConfig config = TestConfigBuilder
                    .builder()
                    .language("python", "3.12")
                    .framework("fastapi", "0.115")
                    .build();

            List<String> files =
                    writer
                            .copyConditionalSecurityAntiPatternsRule(
                                    config, rulesDir,
                                    new TemplateEngine(),
                                    Map.of());

            assertThat(files).hasSize(1);

            String content = Files.readString(
                    rulesDir.resolve(
                            "12-security-anti-patterns.md"),
                    StandardCharsets.UTF_8);

            long count = content.lines()
                    .filter(l -> l.startsWith("### P"))
                    .count();
            assertThat(count)
                    .as("Must have 5 Python security"
                            + " anti-patterns (P1-P5)")
                    .isEqualTo(5);
        }

        @Test
        @DisplayName("python contains pickle and eval"
                + " anti-patterns")
        void write_python_containsPickleAndEval(
                @TempDir Path tempDir)
                throws IOException {
            Path resourceDir =
                    SecurityAntiPatternsTestHelper
                            .createResourcesWithTemplate(
                                    tempDir, "python");
            Path rulesDir = tempDir.resolve("rules");
            Files.createDirectories(rulesDir);

            SecurityAntiPatternsRuleWriter writer =
                    new SecurityAntiPatternsRuleWriter(
                            resourceDir);
            ProjectConfig config = TestConfigBuilder
                    .builder()
                    .language("python", "3.12")
                    .framework("fastapi", "0.115")
                    .build();

            writer.copyConditionalSecurityAntiPatternsRule(
                    config, rulesDir,
                    new TemplateEngine(), Map.of());

            String content = Files.readString(
                    rulesDir.resolve(
                            "12-security-anti-patterns.md"),
                    StandardCharsets.UTF_8);

            assertThat(content)
                    .contains("pickle.loads")
                    .contains("eval()")
                    .contains("subprocess");
        }

        @Test
        @DisplayName("python does not contain Java or Go"
                + " patterns")
        void write_python_noJavaOrGoPatterns(
                @TempDir Path tempDir)
                throws IOException {
            Path resourceDir =
                    SecurityAntiPatternsTestHelper
                            .createResourcesWithTemplate(
                                    tempDir, "python");
            Path rulesDir = tempDir.resolve("rules");
            Files.createDirectories(rulesDir);

            SecurityAntiPatternsRuleWriter writer =
                    new SecurityAntiPatternsRuleWriter(
                            resourceDir);
            ProjectConfig config = TestConfigBuilder
                    .builder()
                    .language("python", "3.12")
                    .framework("fastapi", "0.115")
                    .build();

            writer.copyConditionalSecurityAntiPatternsRule(
                    config, rulesDir,
                    new TemplateEngine(), Map.of());

            String content = Files.readString(
                    rulesDir.resolve(
                            "12-security-anti-patterns.md"),
                    StandardCharsets.UTF_8);

            assertThat(content)
                    .doesNotContain("ObjectInputStream")
                    .doesNotContain("Math.random()")
                    .doesNotContain("X509TrustManager")
                    .doesNotContain("template.HTML");
        }
    }

    @Nested
    @DisplayName("Go generates 4 security anti-patterns"
            + " (G1-G4)")
    class GoSecurityAntiPatterns {

        @Test
        @DisplayName("go generates 4 entries")
        void write_go_generatesFourEntries(
                @TempDir Path tempDir)
                throws IOException {
            Path resourceDir =
                    SecurityAntiPatternsTestHelper
                            .createResourcesWithTemplate(
                                    tempDir, "go");
            Path rulesDir = tempDir.resolve("rules");
            Files.createDirectories(rulesDir);

            SecurityAntiPatternsRuleWriter writer =
                    new SecurityAntiPatternsRuleWriter(
                            resourceDir);
            ProjectConfig config = TestConfigBuilder
                    .builder()
                    .language("go", "1.22")
                    .framework("gin", "1.10")
                    .build();

            List<String> files =
                    writer
                            .copyConditionalSecurityAntiPatternsRule(
                                    config, rulesDir,
                                    new TemplateEngine(),
                                    Map.of());

            assertThat(files).hasSize(1);

            String content = Files.readString(
                    rulesDir.resolve(
                            "12-security-anti-patterns.md"),
                    StandardCharsets.UTF_8);

            long count = content.lines()
                    .filter(l -> l.startsWith("### G"))
                    .count();
            assertThat(count)
                    .as("Must have 4 Go security"
                            + " anti-patterns (G1-G4)")
                    .isEqualTo(4);
        }

        @Test
        @DisplayName("go contains template.HTML and TLS"
                + " anti-patterns")
        void write_go_containsExpectedPatterns(
                @TempDir Path tempDir)
                throws IOException {
            Path resourceDir =
                    SecurityAntiPatternsTestHelper
                            .createResourcesWithTemplate(
                                    tempDir, "go");
            Path rulesDir = tempDir.resolve("rules");
            Files.createDirectories(rulesDir);

            SecurityAntiPatternsRuleWriter writer =
                    new SecurityAntiPatternsRuleWriter(
                            resourceDir);
            ProjectConfig config = TestConfigBuilder
                    .builder()
                    .language("go", "1.22")
                    .framework("gin", "1.10")
                    .build();

            writer.copyConditionalSecurityAntiPatternsRule(
                    config, rulesDir,
                    new TemplateEngine(), Map.of());

            String content = Files.readString(
                    rulesDir.resolve(
                            "12-security-anti-patterns.md"),
                    StandardCharsets.UTF_8);

            assertThat(content)
                    .contains("template.HTML")
                    .contains("ListenAndServe")
                    .contains("CWE-319")
                    .contains("CWE-79");
        }

        @Test
        @DisplayName("go does not contain Java or Python"
                + " patterns")
        void write_go_noJavaOrPythonPatterns(
                @TempDir Path tempDir)
                throws IOException {
            Path resourceDir =
                    SecurityAntiPatternsTestHelper
                            .createResourcesWithTemplate(
                                    tempDir, "go");
            Path rulesDir = tempDir.resolve("rules");
            Files.createDirectories(rulesDir);

            SecurityAntiPatternsRuleWriter writer =
                    new SecurityAntiPatternsRuleWriter(
                            resourceDir);
            ProjectConfig config = TestConfigBuilder
                    .builder()
                    .language("go", "1.22")
                    .framework("gin", "1.10")
                    .build();

            writer.copyConditionalSecurityAntiPatternsRule(
                    config, rulesDir,
                    new TemplateEngine(), Map.of());

            String content = Files.readString(
                    rulesDir.resolve(
                            "12-security-anti-patterns.md"),
                    StandardCharsets.UTF_8);

            assertThat(content)
                    .doesNotContain("ObjectInputStream")
                    .doesNotContain("pickle.loads")
                    .doesNotContain("innerHTML")
                    .doesNotContain("Math.random()");
        }
    }

    @Nested
    @DisplayName("TypeScript generates 5 security"
            + " anti-patterns (T1-T5)")
    class TypeScriptSecurityAntiPatterns {

        @Test
        @DisplayName("typescript generates 5 entries")
        void write_typescript_generatesFiveEntries(
                @TempDir Path tempDir)
                throws IOException {
            Path resourceDir =
                    SecurityAntiPatternsTestHelper
                            .createResourcesWithTemplate(
                                    tempDir, "typescript");
            Path rulesDir = tempDir.resolve("rules");
            Files.createDirectories(rulesDir);

            SecurityAntiPatternsRuleWriter writer =
                    new SecurityAntiPatternsRuleWriter(
                            resourceDir);
            ProjectConfig config = TestConfigBuilder
                    .builder()
                    .language("typescript", "5.4")
                    .framework("nestjs", "10")
                    .build();

            List<String> files =
                    writer
                            .copyConditionalSecurityAntiPatternsRule(
                                    config, rulesDir,
                                    new TemplateEngine(),
                                    Map.of());

            assertThat(files).hasSize(1);

            String content = Files.readString(
                    rulesDir.resolve(
                            "12-security-anti-patterns.md"),
                    StandardCharsets.UTF_8);

            long count = content.lines()
                    .filter(l -> l.startsWith("### T"))
                    .count();
            assertThat(count)
                    .as("Must have 5 TypeScript security"
                            + " anti-patterns (T1-T5)")
                    .isEqualTo(5);
        }

        @Test
        @DisplayName("typescript contains eval and"
                + " prototype pollution patterns")
        void write_typescript_containsExpectedPatterns(
                @TempDir Path tempDir)
                throws IOException {
            Path resourceDir =
                    SecurityAntiPatternsTestHelper
                            .createResourcesWithTemplate(
                                    tempDir, "typescript");
            Path rulesDir = tempDir.resolve("rules");
            Files.createDirectories(rulesDir);

            SecurityAntiPatternsRuleWriter writer =
                    new SecurityAntiPatternsRuleWriter(
                            resourceDir);
            ProjectConfig config = TestConfigBuilder
                    .builder()
                    .language("typescript", "5.4")
                    .framework("nestjs", "10")
                    .build();

            writer.copyConditionalSecurityAntiPatternsRule(
                    config, rulesDir,
                    new TemplateEngine(), Map.of());

            String content = Files.readString(
                    rulesDir.resolve(
                            "12-security-anti-patterns.md"),
                    StandardCharsets.UTF_8);

            assertThat(content)
                    .contains("eval")
                    .contains("Prototype Check")
                    .contains("innerHTML")
                    .contains("jwt.verify")
                    .contains("ReDoS");
        }

        @Test
        @DisplayName("typescript does not contain Java"
                + " or Python patterns")
        void write_typescript_noOtherLangPatterns(
                @TempDir Path tempDir)
                throws IOException {
            Path resourceDir =
                    SecurityAntiPatternsTestHelper
                            .createResourcesWithTemplate(
                                    tempDir, "typescript");
            Path rulesDir = tempDir.resolve("rules");
            Files.createDirectories(rulesDir);

            SecurityAntiPatternsRuleWriter writer =
                    new SecurityAntiPatternsRuleWriter(
                            resourceDir);
            ProjectConfig config = TestConfigBuilder
                    .builder()
                    .language("typescript", "5.4")
                    .framework("nestjs", "10")
                    .build();

            writer.copyConditionalSecurityAntiPatternsRule(
                    config, rulesDir,
                    new TemplateEngine(), Map.of());

            String content = Files.readString(
                    rulesDir.resolve(
                            "12-security-anti-patterns.md"),
                    StandardCharsets.UTF_8);

            assertThat(content)
                    .doesNotContain("ObjectInputStream")
                    .doesNotContain("pickle.loads")
                    .doesNotContain("template.HTML")
                    .doesNotContain("SecureRandom");
        }
    }

    @Nested
    @DisplayName("Rust generates 4 security"
            + " anti-patterns (R1-R4)")
    class RustSecurityAntiPatterns {

        @Test
        @DisplayName("rust generates 4 entries")
        void write_rust_generatesFourEntries(
                @TempDir Path tempDir)
                throws IOException {
            Path resourceDir =
                    SecurityAntiPatternsTestHelper
                            .createResourcesWithTemplate(
                                    tempDir, "rust");
            Path rulesDir = tempDir.resolve("rules");
            Files.createDirectories(rulesDir);

            SecurityAntiPatternsRuleWriter writer =
                    new SecurityAntiPatternsRuleWriter(
                            resourceDir);
            ProjectConfig config = TestConfigBuilder
                    .builder()
                    .language("rust", "1.78")
                    .framework("axum", "0.7")
                    .build();

            List<String> files =
                    writer
                            .copyConditionalSecurityAntiPatternsRule(
                                    config, rulesDir,
                                    new TemplateEngine(),
                                    Map.of());

            assertThat(files).hasSize(1);

            String content = Files.readString(
                    rulesDir.resolve(
                            "12-security-anti-patterns.md"),
                    StandardCharsets.UTF_8);

            long count = content.lines()
                    .filter(l -> l.startsWith("### R"))
                    .count();
            assertThat(count)
                    .as("Must have 4 Rust security"
                            + " anti-patterns (R1-R4)")
                    .isEqualTo(4);
        }
    }
}
