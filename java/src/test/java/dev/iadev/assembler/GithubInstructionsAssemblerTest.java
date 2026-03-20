package dev.iadev.assembler;

import dev.iadev.model.ProjectConfig;
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
 * Tests for GithubInstructionsAssembler — the eighth
 * assembler in the pipeline, generating
 * .github/copilot-instructions.md and contextual
 * instruction files for GitHub Copilot.
 */
@DisplayName("GithubInstructionsAssembler")
class GithubInstructionsAssemblerTest {

    @Nested
    @DisplayName("assemble — implements Assembler")
    class ImplementsAssembler {

        @Test
        @DisplayName("is instance of Assembler")
        void instanceOf_whenCreated_implementsAssemblerInterface() {
            GithubInstructionsAssembler assembler =
                    new GithubInstructionsAssembler();

            assertThat(assembler)
                    .isInstanceOf(Assembler.class);
        }
    }

    @Nested
    @DisplayName("buildCopilotInstructions"
            + " — copilot-instructions.md generation")
    class BuildCopilotInstructions {

        @Test
        @DisplayName("contains project identity header")
        void buildCopilotInstructions_whenCalled_containsIdentityHeader() {
            ProjectConfig config =
                    TestConfigBuilder.builder()
                            .projectName("my-project")
                            .build();

            String result =
                    GithubInstructionsAssembler
                            .buildCopilotInstructions(
                                    config);

            assertThat(result).contains(
                    "# Project Identity \u2014 my-project");
        }

        @Test
        @DisplayName("contains Identity section"
                + " with all fields")
        void buildCopilotInstructions_whenCalled_containsIdentitySection() {
            ProjectConfig config =
                    TestConfigBuilder.builder()
                            .projectName("api-pagamentos")
                            .archStyle("hexagonal")
                            .domainDriven(true)
                            .eventDriven(false)
                            .language("java", "21")
                            .framework("quarkus", "3.17")
                            .build();

            String result =
                    GithubInstructionsAssembler
                            .buildCopilotInstructions(
                                    config);

            assertThat(result)
                    .contains(
                            "- **Name:** api-pagamentos")
                    .contains(
                            "- **Architecture Style:**"
                                    + " hexagonal")
                    .contains(
                            "- **Domain-Driven Design:**"
                                    + " true")
                    .contains(
                            "- **Event-Driven:** false")
                    .contains(
                            "- **Language:** java 21")
                    .contains(
                            "- **Framework:** quarkus"
                                    + " 3.17");
        }

        @Test
        @DisplayName("contains Technology Stack table")
        void buildCopilotInstructions_whenCalled_containsStackTable() {
            ProjectConfig config =
                    TestConfigBuilder.builder()
                            .archStyle("microservice")
                            .language("rust", "2024")
                            .framework("axum", "")
                            .buildTool("cargo")
                            .container("docker")
                            .orchestrator("kubernetes")
                            .build();

            String result =
                    GithubInstructionsAssembler
                            .buildCopilotInstructions(
                                    config);

            assertThat(result)
                    .contains("## Technology Stack")
                    .contains(
                            "| Architecture |"
                                    + " Microservice |")
                    .contains(
                            "| Language | Rust 2024 |")
                    .contains("| Framework | Axum |")
                    .contains(
                            "| Build Tool | Cargo |")
                    .contains(
                            "| Container | Docker |")
                    .contains(
                            "| Orchestrator |"
                                    + " Kubernetes |")
                    .contains(
                            "| Resilience | Mandatory"
                                    + " (always enabled) |");
        }

        @Test
        @DisplayName("contains Constraints section")
        void buildCopilotInstructions_whenCalled_containsConstraints() {
            ProjectConfig config =
                    TestConfigBuilder.minimal();

            String result =
                    GithubInstructionsAssembler
                            .buildCopilotInstructions(
                                    config);

            assertThat(result)
                    .contains("## Constraints")
                    .contains("Cloud-Agnostic")
                    .contains("Horizontal scalability")
                    .contains(
                            "Externalized configuration");
        }

        @Test
        @DisplayName("contains Contextual Instructions"
                + " references")
        void buildCopilotInstructions_whenCalled_containsContextualRefs() {
            ProjectConfig config =
                    TestConfigBuilder.minimal();

            String result =
                    GithubInstructionsAssembler
                            .buildCopilotInstructions(
                                    config);

            assertThat(result)
                    .contains(
                            "## Contextual Instructions")
                    .contains(
                            "domain.instructions.md")
                    .contains(
                            "coding-standards"
                                    + ".instructions.md")
                    .contains(
                            "architecture"
                                    + ".instructions.md")
                    .contains(
                            "quality-gates"
                                    + ".instructions.md");
        }

        @Test
        @DisplayName("ends with trailing newline")
        void buildCopilotInstructions_whenCalled_endsWithTrailingNewline() {
            ProjectConfig config =
                    TestConfigBuilder.minimal();

            String result =
                    GithubInstructionsAssembler
                            .buildCopilotInstructions(
                                    config);

            assertThat(result).endsWith("\n");
            assertThat(result).doesNotEndWith("\n\n");
        }

        @Test
        @DisplayName("framework version appended"
                + " when present")
        void buildCopilotInstructions_whenCalled_frameworkVersionAppended() {
            ProjectConfig config =
                    TestConfigBuilder.builder()
                            .framework("quarkus", "3.17")
                            .build();

            String result =
                    GithubInstructionsAssembler
                            .buildCopilotInstructions(
                                    config);

            assertThat(result).contains(
                    "| Framework | Quarkus 3.17 |");
        }

        @Test
        @DisplayName("framework version omitted"
                + " when empty")
        void buildCopilotInstructions_whenCalled_frameworkVersionOmitted() {
            ProjectConfig config =
                    TestConfigBuilder.builder()
                            .framework("axum", "")
                            .build();

            String result =
                    GithubInstructionsAssembler
                            .buildCopilotInstructions(
                                    config);

            assertThat(result).contains(
                    "| Framework | Axum |");
        }
    }

    @Nested
    @DisplayName("formatInterfaces — interface formatting")
    class FormatInterfaces {

        @Test
        @DisplayName("REST uppercased")
        void formatInterfaces_whenCalled_restUppercased() {
            ProjectConfig config =
                    TestConfigBuilder.builder()
                            .clearInterfaces()
                            .addInterface("rest")
                            .build();

            String result =
                    GithubInstructionsAssembler
                            .formatInterfaces(config);

            assertThat(result).isEqualTo("REST");
        }

        @Test
        @DisplayName("GRPC uppercased")
        void formatInterfaces_whenCalled_grpcUppercased() {
            ProjectConfig config =
                    TestConfigBuilder.builder()
                            .clearInterfaces()
                            .addInterface("grpc")
                            .build();

            String result =
                    GithubInstructionsAssembler
                            .formatInterfaces(config);

            assertThat(result).isEqualTo("GRPC");
        }

        @Test
        @DisplayName("mixed interfaces formatted"
                + " correctly")
        void formatInterfaces_whenCalled_mixedInterfaces() {
            ProjectConfig config =
                    TestConfigBuilder.builder()
                            .clearInterfaces()
                            .addInterface("rest")
                            .addInterface("grpc")
                            .addInterface("event-consumer")
                            .build();

            String result =
                    GithubInstructionsAssembler
                            .formatInterfaces(config);

            assertThat(result)
                    .isEqualTo(
                            "REST, GRPC, event-consumer");
        }

        @Test
        @DisplayName("empty interfaces returns none")
        void formatInterfaces_emptyInterfaces_returnsNone() {
            ProjectConfig config =
                    TestConfigBuilder.builder()
                            .clearInterfaces()
                            .build();

            String result =
                    GithubInstructionsAssembler
                            .formatInterfaces(config);

            assertThat(result).isEqualTo("none");
        }
    }

    @Nested
    @DisplayName("formatFrameworkVersion")
    class FormatFrameworkVersion {

        @Test
        @DisplayName("returns space-prefixed version"
                + " when present")
        void assemble_withSpace_returnsVersion() {
            ProjectConfig config =
                    TestConfigBuilder.builder()
                            .framework("spring", "3.4")
                            .build();

            String result =
                    GithubInstructionsAssembler
                            .formatFrameworkVersion(
                                    config);

            assertThat(result).isEqualTo(" 3.4");
        }

        @Test
        @DisplayName("returns empty string when"
                + " version empty")
        void assemble_whenNoVersion_returnsEmpty() {
            ProjectConfig config =
                    TestConfigBuilder.builder()
                            .framework("axum", "")
                            .build();

            String result =
                    GithubInstructionsAssembler
                            .formatFrameworkVersion(
                                    config);

            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("replaceSingleBracePlaceholders")
    class ReplacePlaceholders {

        @Test
        @DisplayName("replaces known placeholders")
        void assemble_whenCalled_replacesKnownPlaceholders() {
            String content = "Hello {name}, v{version}";
            Map<String, String> context = Map.of(
                    "name", "world",
                    "version", "1.0");

            String result =
                    GithubInstructionsAssembler
                            .replaceSingleBracePlaceholders(
                                    content, context);

            assertThat(result)
                    .isEqualTo("Hello world, v1.0");
        }

        @Test
        @DisplayName("preserves unknown placeholders")
        void assemble_whenCalled_preservesUnknownPlaceholders() {
            String content = "Value: {unknown}";
            Map<String, String> context = Map.of();

            String result =
                    GithubInstructionsAssembler
                            .replaceSingleBracePlaceholders(
                                    content, context);

            assertThat(result)
                    .isEqualTo("Value: {unknown}");
        }

        @Test
        @DisplayName("does not match double braces")
        void assemble_whenCalled_doesNotMatchDoubleBraces() {
            String content = "Keep {{this}} intact";
            Map<String, String> context = Map.of(
                    "this", "replaced");

            String result =
                    GithubInstructionsAssembler
                            .replaceSingleBracePlaceholders(
                                    content, context);

            assertThat(result)
                    .isEqualTo("Keep {{this}} intact");
        }
    }

    @Nested
    @DisplayName("assemble — file generation")
    class FileGeneration {

        @Test
        @DisplayName("generates copilot-instructions.md")
        void assemble_whenCalled_generatesCopilotInstructions(
                @TempDir Path tempDir)
                throws IOException {
            Path outputDir = tempDir.resolve("output");
            Files.createDirectories(outputDir);

            GithubInstructionsAssembler assembler =
                    new GithubInstructionsAssembler();
            ProjectConfig config =
                    TestConfigBuilder.builder()
                            .projectName("test-project")
                            .build();

            List<String> files = assembler.assemble(
                    config, new TemplateEngine(),
                    outputDir);

            assertThat(files).isNotEmpty();
            Path global = outputDir.resolve(
                    "copilot-instructions.md");
            assertThat(global).exists();
            String content = Files.readString(
                    global, StandardCharsets.UTF_8);
            assertThat(content).contains(
                    "# Project Identity");
        }

        @Test
        @DisplayName("generates 4 contextual files")
        void assemble_whenCalled_generates4ContextualFiles(
                @TempDir Path tempDir)
                throws IOException {
            Path outputDir = tempDir.resolve("output");
            Files.createDirectories(outputDir);

            GithubInstructionsAssembler assembler =
                    new GithubInstructionsAssembler();
            ProjectConfig config =
                    TestConfigBuilder.minimal();

            List<String> files = assembler.assemble(
                    config, new TemplateEngine(),
                    outputDir);

            assertThat(files).hasSize(5);

            Path instructions =
                    outputDir.resolve("instructions");
            assertThat(instructions.resolve(
                    "domain.instructions.md")).exists();
            assertThat(instructions.resolve(
                    "coding-standards.instructions.md"))
                    .exists();
            assertThat(instructions.resolve(
                    "architecture.instructions.md"))
                    .exists();
            assertThat(instructions.resolve(
                    "quality-gates.instructions.md"))
                    .exists();
        }

        @Test
        @DisplayName("contextual files have"
                + " placeholders replaced")
        void assemble_whenCalled_contextualFilesHavePlaceholdersReplaced(
                @TempDir Path tempDir)
                throws IOException {
            Path outputDir = tempDir.resolve("output");
            Files.createDirectories(outputDir);

            GithubInstructionsAssembler assembler =
                    new GithubInstructionsAssembler();
            ProjectConfig config =
                    TestConfigBuilder.builder()
                            .projectName("my-service")
                            .purpose("Test service")
                            .build();

            assembler.assemble(
                    config, new TemplateEngine(),
                    outputDir);

            Path domain = outputDir.resolve(
                    "instructions/domain.instructions.md");
            String content = Files.readString(
                    domain, StandardCharsets.UTF_8);
            assertThat(content)
                    .contains("my-service")
                    .contains("Test service")
                    .doesNotContain("{project_name}")
                    .doesNotContain("{project_purpose}");
        }

        @Test
        @DisplayName("quality gates has coverage"
                + " values replaced")
        void assemble_whenCalled_qualityGatesCoverageReplaced(
                @TempDir Path tempDir)
                throws IOException {
            Path outputDir = tempDir.resolve("output");
            Files.createDirectories(outputDir);

            GithubInstructionsAssembler assembler =
                    new GithubInstructionsAssembler();
            ProjectConfig config =
                    TestConfigBuilder.minimal();

            assembler.assemble(
                    config, new TemplateEngine(),
                    outputDir);

            Path qg = outputDir.resolve(
                    "instructions/quality-gates"
                            + ".instructions.md");
            String content = Files.readString(
                    qg, StandardCharsets.UTF_8);
            assertThat(content)
                    .contains("95%")
                    .contains("90%")
                    .doesNotContain("{coverage_line}")
                    .doesNotContain("{coverage_branch}");
        }
    }

    @Nested
    @DisplayName("assemble — golden file parity")
    class GoldenFile {

        @Test
        @DisplayName("copilot-instructions.md matches"
                + " golden file for rust-axum")
        void assemble_copilotInstructions_matchesGolden(
                @TempDir Path tempDir)
                throws IOException {
            Path outputDir = tempDir.resolve("output");
            Files.createDirectories(outputDir);

            GithubInstructionsAssembler assembler =
                    new GithubInstructionsAssembler();
            ProjectConfig config =
                    buildRustAxumConfig();

            assembler.assemble(
                    config, new TemplateEngine(),
                    outputDir);

            String goldenPath =
                    "golden/rust-axum/.github/"
                            + "copilot-instructions.md";
            String expected = loadResource(goldenPath);
            assertThat(expected)
                    .as("Golden file must exist")
                    .isNotNull();

            String actual = Files.readString(
                    outputDir.resolve(
                            "copilot-instructions.md"),
                    StandardCharsets.UTF_8);
            assertThat(actual)
                    .as("copilot-instructions.md must"
                            + " match golden file"
                            + " byte-for-byte")
                    .isEqualTo(expected);
        }

        @Test
        @DisplayName("domain.instructions.md matches"
                + " golden file for rust-axum")
        void assemble_domainInstructions_matchesGolden(
                @TempDir Path tempDir)
                throws IOException {
            Path outputDir = tempDir.resolve("output");
            Files.createDirectories(outputDir);

            GithubInstructionsAssembler assembler =
                    new GithubInstructionsAssembler();
            ProjectConfig config =
                    buildRustAxumConfig();

            assembler.assemble(
                    config, new TemplateEngine(),
                    outputDir);

            String goldenPath =
                    "golden/rust-axum/.github/"
                            + "instructions/"
                            + "domain.instructions.md";
            String expected = loadResource(goldenPath);
            assertThat(expected)
                    .as("Golden file must exist")
                    .isNotNull();

            String actual = Files.readString(
                    outputDir.resolve(
                            "instructions/"
                                    + "domain"
                                    + ".instructions.md"),
                    StandardCharsets.UTF_8);
            assertThat(actual)
                    .as("domain.instructions.md must"
                            + " match golden file")
                    .isEqualTo(expected);
        }

        @Test
        @DisplayName("coding-standards.instructions.md"
                + " matches golden for rust-axum")
        void assemble_codingStandards_matchesGolden(
                @TempDir Path tempDir)
                throws IOException {
            Path outputDir = tempDir.resolve("output");
            Files.createDirectories(outputDir);

            GithubInstructionsAssembler assembler =
                    new GithubInstructionsAssembler();
            ProjectConfig config =
                    buildRustAxumConfig();

            assembler.assemble(
                    config, new TemplateEngine(),
                    outputDir);

            String goldenPath =
                    "golden/rust-axum/.github/"
                            + "instructions/coding"
                            + "-standards"
                            + ".instructions.md";
            String expected = loadResource(goldenPath);
            assertThat(expected)
                    .as("Golden file must exist")
                    .isNotNull();

            String actual = Files.readString(
                    outputDir.resolve(
                            "instructions/coding"
                                    + "-standards"
                                    + ".instructions.md"),
                    StandardCharsets.UTF_8);
            assertThat(actual)
                    .as("coding-standards.instructions"
                            + ".md must match golden")
                    .isEqualTo(expected);
        }

        @Test
        @DisplayName("architecture.instructions.md"
                + " matches golden for rust-axum")
        void assemble_architecture_matchesGolden(
                @TempDir Path tempDir)
                throws IOException {
            Path outputDir = tempDir.resolve("output");
            Files.createDirectories(outputDir);

            GithubInstructionsAssembler assembler =
                    new GithubInstructionsAssembler();
            ProjectConfig config =
                    buildRustAxumConfig();

            assembler.assemble(
                    config, new TemplateEngine(),
                    outputDir);

            String goldenPath =
                    "golden/rust-axum/.github/"
                            + "instructions/"
                            + "architecture"
                            + ".instructions.md";
            String expected = loadResource(goldenPath);
            assertThat(expected)
                    .as("Golden file must exist")
                    .isNotNull();

            String actual = Files.readString(
                    outputDir.resolve(
                            "instructions/"
                                    + "architecture"
                                    + ".instructions.md"),
                    StandardCharsets.UTF_8);
            assertThat(actual)
                    .as("architecture.instructions.md"
                            + " must match golden")
                    .isEqualTo(expected);
        }

        @Test
        @DisplayName("quality-gates.instructions.md"
                + " matches golden for rust-axum")
        void assemble_qualityGates_matchesGolden(
                @TempDir Path tempDir)
                throws IOException {
            Path outputDir = tempDir.resolve("output");
            Files.createDirectories(outputDir);

            GithubInstructionsAssembler assembler =
                    new GithubInstructionsAssembler();
            ProjectConfig config =
                    buildRustAxumConfig();

            assembler.assemble(
                    config, new TemplateEngine(),
                    outputDir);

            String goldenPath =
                    "golden/rust-axum/.github/"
                            + "instructions/"
                            + "quality-gates"
                            + ".instructions.md";
            String expected = loadResource(goldenPath);
            assertThat(expected)
                    .as("Golden file must exist")
                    .isNotNull();

            String actual = Files.readString(
                    outputDir.resolve(
                            "instructions/"
                                    + "quality-gates"
                                    + ".instructions.md"),
                    StandardCharsets.UTF_8);
            assertThat(actual)
                    .as("quality-gates.instructions.md"
                            + " must match golden")
                    .isEqualTo(expected);
        }

        private String loadResource(String path) {
            var url = getClass().getClassLoader()
                    .getResource(path);
            if (url == null) {
                return null;
            }
            try {
                return Files.readString(
                        Path.of(url.getPath()),
                        StandardCharsets.UTF_8);
            } catch (IOException e) {
                return null;
            }
        }
    }

    @Nested
    @DisplayName("assemble — edge cases")
    class EdgeCases {

        @Test
        @DisplayName("missing templates directory"
                + " returns global file only")
        void assemble_missingTemplates_returnsGlobal(
                @TempDir Path tempDir)
                throws IOException {
            Path resourceDir =
                    tempDir.resolve("empty-resources");
            Files.createDirectories(resourceDir);
            Path outputDir = tempDir.resolve("output");
            Files.createDirectories(outputDir);

            GithubInstructionsAssembler assembler =
                    new GithubInstructionsAssembler(
                            resourceDir);
            ProjectConfig config =
                    TestConfigBuilder.minimal();

            List<String> files = assembler.assemble(
                    config, new TemplateEngine(),
                    outputDir);

            assertThat(files).hasSize(1);
            assertThat(files.get(0))
                    .contains("copilot-instructions.md");
        }

        @Test
        @DisplayName("CONTEXTUAL_INSTRUCTIONS constant"
                + " has 4 entries")
        void assemble_contextualInstructions_has4Entries() {
            assertThat(GithubInstructionsAssembler
                    .CONTEXTUAL_INSTRUCTIONS)
                    .hasSize(4)
                    .containsExactly(
                            "domain",
                            "coding-standards",
                            "architecture",
                            "quality-gates");
        }
    }

    static ProjectConfig buildRustAxumConfig() {
        return TestConfigBuilder.builder()
                .projectName("my-rust-service")
                .purpose("Describe your service"
                        + " purpose here")
                .archStyle("microservice")
                .domainDriven(false)
                .eventDriven(true)
                .language("rust", "2024")
                .framework("axum", "")
                .buildTool("cargo")
                .nativeBuild(false)
                .container("docker")
                .orchestrator("kubernetes")
                .smokeTests(true)
                .contractTests(false)
                .clearInterfaces()
                .addInterface("rest")
                .addInterface("grpc")
                .addInterface("event-consumer")
                .addInterface("event-producer")
                .build();
    }
}
