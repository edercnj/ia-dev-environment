package dev.iadev.application.assembler;

import dev.iadev.domain.model.ProjectConfig;
import dev.iadev.template.TemplateEngine;
import dev.iadev.testutil.TestConfigBuilder;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for ConstitutionAssembler -- generates
 * CONSTITUTION.md conditionally when compliance != "none".
 */
@DisplayName("ConstitutionAssembler")
class ConstitutionAssemblerTest {

    @Nested
    @DisplayName("implements Assembler interface")
    class ImplementsAssembler {

        @Test
        @DisplayName("is instance of Assembler")
        void instanceOf_whenCreated_implementsAssembler() {
            ConstitutionAssembler assembler =
                    new ConstitutionAssembler();

            assertThat(assembler)
                    .isInstanceOf(Assembler.class);
        }
    }

    @Nested
    @DisplayName("assemble -- compliance is none")
    class ComplianceNone {

        @Test
        @DisplayName("returns empty list when compliance"
                + " is empty")
        void assemble_complianceEmpty_returnsEmptyList(
                @TempDir Path tempDir) {
            Path outputDir = tempDir.resolve("output");
            ConstitutionAssembler assembler =
                    new ConstitutionAssembler();
            ProjectConfig config =
                    TestConfigBuilder.minimal();
            TemplateEngine engine = new TemplateEngine();

            List<String> files = assembler.assemble(
                    config, engine, outputDir);

            assertThat(files).isEmpty();
        }

        @Test
        @DisplayName("does not create CONSTITUTION.md"
                + " when compliance is empty")
        void assemble_complianceEmpty_noFileCreated(
                @TempDir Path tempDir) {
            Path outputDir = tempDir.resolve("output");
            ConstitutionAssembler assembler =
                    new ConstitutionAssembler();
            ProjectConfig config =
                    TestConfigBuilder.minimal();
            TemplateEngine engine = new TemplateEngine();

            assembler.assemble(
                    config, engine, outputDir);

            Path constitutionFile =
                    outputDir.resolve("CONSTITUTION.md");
            assertThat(constitutionFile).doesNotExist();
        }

        @Test
        @DisplayName("does not create output directory"
                + " when compliance is empty")
        void assemble_complianceEmpty_noOutputDir(
                @TempDir Path tempDir) {
            Path outputDir = tempDir.resolve("output");
            ConstitutionAssembler assembler =
                    new ConstitutionAssembler();
            ProjectConfig config =
                    TestConfigBuilder.minimal();
            TemplateEngine engine = new TemplateEngine();

            assembler.assemble(
                    config, engine, outputDir);

            assertThat(outputDir).doesNotExist();
        }
    }

    @Nested
    @DisplayName("assemble -- compliance is pci-dss")
    class CompliancePciDss {

        @Test
        @DisplayName("generates CONSTITUTION.md when"
                + " compliance is pci-dss")
        void assemble_pciDss_generatesFile(
                @TempDir Path tempDir) {
            Path outputDir = tempDir.resolve("output");
            ConstitutionAssembler assembler =
                    new ConstitutionAssembler();
            ProjectConfig config = buildPciDssConfig();
            TemplateEngine engine = new TemplateEngine();

            List<String> files = assembler.assemble(
                    config, engine, outputDir);

            assertThat(files).hasSize(1);
            Path expected =
                    outputDir.resolve("CONSTITUTION.md");
            assertThat(expected).exists();
        }

        @Test
        @DisplayName("returns file path ending in"
                + " CONSTITUTION.md")
        void assemble_pciDss_returnsFilePath(
                @TempDir Path tempDir) {
            Path outputDir = tempDir.resolve("output");
            ConstitutionAssembler assembler =
                    new ConstitutionAssembler();
            ProjectConfig config = buildPciDssConfig();
            TemplateEngine engine = new TemplateEngine();

            List<String> files = assembler.assemble(
                    config, engine, outputDir);

            assertThat(files.get(0))
                    .endsWith("CONSTITUTION.md");
        }

        @Test
        @DisplayName("contains Invariants section")
        void assemble_pciDss_containsInvariants(
                @TempDir Path tempDir) {
            Path outputDir = tempDir.resolve("output");
            ConstitutionAssembler assembler =
                    new ConstitutionAssembler();
            ProjectConfig config = buildPciDssConfig();
            TemplateEngine engine = new TemplateEngine();

            assembler.assemble(
                    config, engine, outputDir);

            String content = readFile(
                    outputDir.resolve("CONSTITUTION.md"));
            assertThat(content)
                    .contains("## Invariants");
        }

        @Test
        @DisplayName("contains Security Constraints section")
        void assemble_pciDss_containsSecurityConstraints(
                @TempDir Path tempDir) {
            Path outputDir = tempDir.resolve("output");
            ConstitutionAssembler assembler =
                    new ConstitutionAssembler();
            ProjectConfig config = buildPciDssConfig();
            TemplateEngine engine = new TemplateEngine();

            assembler.assemble(
                    config, engine, outputDir);

            String content = readFile(
                    outputDir.resolve("CONSTITUTION.md"));
            assertThat(content)
                    .contains("## Security Constraints");
        }

        @Test
        @DisplayName("contains Architecture Boundaries"
                + " section")
        void assemble_pciDss_containsArchBoundaries(
                @TempDir Path tempDir) {
            Path outputDir = tempDir.resolve("output");
            ConstitutionAssembler assembler =
                    new ConstitutionAssembler();
            ProjectConfig config = buildPciDssConfig();
            TemplateEngine engine = new TemplateEngine();

            assembler.assemble(
                    config, engine, outputDir);

            String content = readFile(
                    outputDir.resolve("CONSTITUTION.md"));
            assertThat(content)
                    .contains("## Architecture Boundaries");
        }

        @Test
        @DisplayName("contains Naming Conventions section")
        void assemble_pciDss_containsNamingConventions(
                @TempDir Path tempDir) {
            Path outputDir = tempDir.resolve("output");
            ConstitutionAssembler assembler =
                    new ConstitutionAssembler();
            ProjectConfig config = buildPciDssConfig();
            TemplateEngine engine = new TemplateEngine();

            assembler.assemble(
                    config, engine, outputDir);

            String content = readFile(
                    outputDir.resolve("CONSTITUTION.md"));
            assertThat(content)
                    .contains("## Naming Conventions");
        }

        @Test
        @DisplayName("contains Compliance Requirements"
                + " section")
        void assemble_pciDss_containsComplianceReqs(
                @TempDir Path tempDir) {
            Path outputDir = tempDir.resolve("output");
            ConstitutionAssembler assembler =
                    new ConstitutionAssembler();
            ProjectConfig config = buildPciDssConfig();
            TemplateEngine engine = new TemplateEngine();

            assembler.assemble(
                    config, engine, outputDir);

            String content = readFile(
                    outputDir.resolve("CONSTITUTION.md"));
            assertThat(content)
                    .contains("## Compliance Requirements");
        }
    }

    @Nested
    @DisplayName("assemble -- CWE mappings")
    class CweMappings {

        @Test
        @DisplayName("contains CWE-89 SQL Injection"
                + " mapping")
        void assemble_pciDss_containsCwe89(
                @TempDir Path tempDir) {
            Path outputDir = tempDir.resolve("output");
            ConstitutionAssembler assembler =
                    new ConstitutionAssembler();
            ProjectConfig config = buildPciDssConfig();
            TemplateEngine engine = new TemplateEngine();

            assembler.assemble(
                    config, engine, outputDir);

            String content = readFile(
                    outputDir.resolve("CONSTITUTION.md"));
            assertThat(content).contains("CWE-89");
        }

        @Test
        @DisplayName("contains CWE-312 Cleartext Storage"
                + " mapping")
        void assemble_pciDss_containsCwe312(
                @TempDir Path tempDir) {
            Path outputDir = tempDir.resolve("output");
            ConstitutionAssembler assembler =
                    new ConstitutionAssembler();
            ProjectConfig config = buildPciDssConfig();
            TemplateEngine engine = new TemplateEngine();

            assembler.assemble(
                    config, engine, outputDir);

            String content = readFile(
                    outputDir.resolve("CONSTITUTION.md"));
            assertThat(content).contains("CWE-312");
        }

        @Test
        @DisplayName("contains RULE-SEC invariant IDs")
        void assemble_pciDss_containsRuleSecIds(
                @TempDir Path tempDir) {
            Path outputDir = tempDir.resolve("output");
            ConstitutionAssembler assembler =
                    new ConstitutionAssembler();
            ProjectConfig config = buildPciDssConfig();
            TemplateEngine engine = new TemplateEngine();

            assembler.assemble(
                    config, engine, outputDir);

            String content = readFile(
                    outputDir.resolve("CONSTITUTION.md"));
            assertThat(content).contains("RULE-SEC-001");
            assertThat(content).contains("RULE-SEC-002");
        }

        @Test
        @DisplayName("contains RULE-ARCH invariant IDs")
        void assemble_pciDss_containsRuleArchIds(
                @TempDir Path tempDir) {
            Path outputDir = tempDir.resolve("output");
            ConstitutionAssembler assembler =
                    new ConstitutionAssembler();
            ProjectConfig config = buildPciDssConfig();
            TemplateEngine engine = new TemplateEngine();

            assembler.assemble(
                    config, engine, outputDir);

            String content = readFile(
                    outputDir.resolve("CONSTITUTION.md"));
            assertThat(content).contains("RULE-ARCH-001");
        }
    }

    @Nested
    @DisplayName("assemble -- project context variables")
    class ProjectContext {

        @Test
        @DisplayName("resolves project name in output")
        void assemble_pciDss_resolvesProjectName(
                @TempDir Path tempDir) {
            Path outputDir = tempDir.resolve("output");
            ConstitutionAssembler assembler =
                    new ConstitutionAssembler();
            ProjectConfig config =
                    TestConfigBuilder.builder()
                            .projectName("payments-api")
                            .securityFrameworks("pci-dss")
                            .build();
            TemplateEngine engine = new TemplateEngine();

            assembler.assemble(
                    config, engine, outputDir);

            String content = readFile(
                    outputDir.resolve("CONSTITUTION.md"));
            assertThat(content)
                    .contains("payments-api");
        }
    }

    @Nested
    @DisplayName("pipeline registration")
    class PipelineRegistration {

        @Test
        @DisplayName("ConstitutionAssembler is registered"
                + " in AssemblerFactory")
        void buildAssemblers_always_containsConstitution() {
            List<AssemblerDescriptor> descriptors =
                    AssemblerFactory.buildAssemblers();

            assertThat(descriptors)
                    .extracting(AssemblerDescriptor::name)
                    .contains("ConstitutionAssembler");
        }

        @Test
        @DisplayName("ConstitutionAssembler executes"
                + " before RulesAssembler")
        void buildAssemblers_always_constitutionBeforeRules() {
            List<AssemblerDescriptor> descriptors =
                    AssemblerFactory.buildAssemblers();

            List<String> names = descriptors.stream()
                    .map(AssemblerDescriptor::name)
                    .toList();

            int constitutionIdx =
                    names.indexOf("ConstitutionAssembler");
            int rulesIdx =
                    names.indexOf("RulesAssembler");

            assertThat(constitutionIdx)
                    .isGreaterThanOrEqualTo(0);
            assertThat(constitutionIdx)
                    .isLessThan(rulesIdx);
        }
    }

    @Nested
    @DisplayName("assemble -- graceful no-op")
    class GracefulNoOp {

        @Test
        @DisplayName("returns empty list when template"
                + " is absent")
        void assemble_templateAbsent_returnsEmpty(
                @TempDir Path tempDir) {
            Path resourcesDir =
                    tempDir.resolve("nonexistent");
            Path outputDir = tempDir.resolve("output");
            ConstitutionAssembler assembler =
                    new ConstitutionAssembler(resourcesDir);
            ProjectConfig config = buildPciDssConfig();
            TemplateEngine engine = new TemplateEngine();

            List<String> files = assembler.assemble(
                    config, engine, outputDir);

            assertThat(files).isEmpty();
        }
    }

    private static ProjectConfig buildPciDssConfig() {
        return TestConfigBuilder.builder()
                .projectName("test-project")
                .language("java", "21")
                .framework("spring-boot", "3.2")
                .archStyle("hexagonal")
                .securityFrameworks("pci-dss")
                .build();
    }

    private static String readFile(Path path) {
        try {
            return Files.readString(
                    path, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new RuntimeException(
                    "Failed to read: " + path, e);
        }
    }
}
