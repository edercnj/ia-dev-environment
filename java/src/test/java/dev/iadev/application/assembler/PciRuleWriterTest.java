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
 * Tests for PciRuleWriter — conditional generation of
 * 11-security-pci.md rule based on pci-dss compliance.
 */
@DisplayName("PciRuleWriter")
class PciRuleWriterTest {

    @Nested
    @DisplayName("copyConditionalPciRule")
    class CopyConditionalPciRule {

        @Test
        @DisplayName("config with pci-dss generates"
                + " 11-security-pci.md")
        void copy_pciDss_generatesRule(
                @TempDir Path tempDir) throws IOException {
            Path resDir = setupResources(tempDir);
            Path rulesDir = tempDir.resolve("rules");
            Files.createDirectories(rulesDir);

            PciRuleWriter writer = new PciRuleWriter(resDir);
            ProjectConfig config =
                    TestConfigBuilder.builder()
                            .compliance("pci-dss")
                            .build();

            List<String> result =
                    writer.copyConditionalPciRule(
                            config, rulesDir,
                            new TemplateEngine(),
                            Map.of());

            assertThat(result).hasSize(1);
            assertThat(result.get(0))
                    .contains("11-security-pci.md");
            assertThat(Files.exists(
                    rulesDir.resolve(
                            "11-security-pci.md")))
                    .isTrue();
        }

        @Test
        @DisplayName("config without pci-dss returns"
                + " empty")
        void copy_noPciDss_returnsEmpty(
                @TempDir Path tempDir) throws IOException {
            Path resDir = setupResources(tempDir);
            Path rulesDir = tempDir.resolve("rules");
            Files.createDirectories(rulesDir);

            PciRuleWriter writer = new PciRuleWriter(resDir);
            ProjectConfig config =
                    TestConfigBuilder.builder()
                            .securityFrameworks("lgpd")
                            .build();

            List<String> result =
                    writer.copyConditionalPciRule(
                            config, rulesDir,
                            new TemplateEngine(),
                            Map.of());

            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("config with no frameworks returns"
                + " empty")
        void copy_noFrameworks_returnsEmpty(
                @TempDir Path tempDir) throws IOException {
            Path resDir = setupResources(tempDir);
            Path rulesDir = tempDir.resolve("rules");
            Files.createDirectories(rulesDir);

            PciRuleWriter writer = new PciRuleWriter(resDir);
            ProjectConfig config =
                    TestConfigBuilder.builder()
                            .build();

            List<String> result =
                    writer.copyConditionalPciRule(
                            config, rulesDir,
                            new TemplateEngine(),
                            Map.of());

            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("template file missing throws"
                + " IllegalStateException")
        void copy_templateMissing_throwsException(
                @TempDir Path tempDir) throws IOException {
            Path resDir = tempDir.resolve("resources");
            Files.createDirectories(resDir);
            Path rulesDir = tempDir.resolve("rules");
            Files.createDirectories(rulesDir);

            PciRuleWriter writer = new PciRuleWriter(resDir);
            ProjectConfig config =
                    TestConfigBuilder.builder()
                            .compliance("pci-dss")
                            .build();

            org.assertj.core.api.Assertions
                    .assertThatThrownBy(() ->
                            writer.copyConditionalPciRule(
                                    config, rulesDir,
                                    new TemplateEngine(),
                                    Map.of()))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining(
                            "PCI-DSS is enabled");
        }

        private Path setupResources(Path tempDir)
                throws IOException {
            Path resDir = tempDir.resolve("resources");
            Path conditionalDir = resDir.resolve(
                    "targets/claude/rules/conditional");
            Files.createDirectories(conditionalDir);
            Files.writeString(
                    conditionalDir.resolve(
                            "11-security-pci.md"),
                    "# PCI Security Rule\nTest content",
                    StandardCharsets.UTF_8);
            return resDir;
        }
    }
}
