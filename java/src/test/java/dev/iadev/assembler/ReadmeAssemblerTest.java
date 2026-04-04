package dev.iadev.assembler;

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

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for ReadmeAssembler -- the last assembler in the
 * pipeline, generating README.md with summary tables.
 */
@DisplayName("ReadmeAssembler")
class ReadmeAssemblerTest {

    @Nested
    @DisplayName("assemble — implements Assembler")
    class ImplementsAssembler {

        @Test
        @DisplayName("is instance of Assembler")
        void instanceOf_whenCreated_implementsAssemblerInterface() {
            ReadmeAssembler assembler =
                    new ReadmeAssembler();

            assertThat(assembler)
                    .isInstanceOf(Assembler.class);
        }
    }

    @Nested
    @DisplayName("assemble — template mode")
    class TemplateMode {

        @Test
        @DisplayName("generates README.md from template")
        void assemble_whenCalled_generatesFromTemplate(@TempDir Path tempDir)
                throws IOException {
            Path resourcesDir = setupResources(tempDir);
            Path outputDir = setupOutput(tempDir);

            ReadmeAssembler assembler =
                    new ReadmeAssembler(resourcesDir);
            ProjectConfig config =
                    TestConfigBuilder.builder()
                            .projectName("my-project")
                            .build();

            List<String> files = assembler.assemble(
                    config, new TemplateEngine(), outputDir);

            assertThat(files).hasSize(1);
            assertThat(files.get(0))
                    .endsWith("README.md");
            assertThat(outputDir.resolve("README.md"))
                    .exists();
        }

        @Test
        @DisplayName("replaces PROJECT_NAME placeholder")
        void assemble_whenCalled_replacesProjectName(@TempDir Path tempDir)
                throws IOException {
            Path resourcesDir = setupResources(tempDir);
            Path outputDir = setupOutput(tempDir);

            ReadmeAssembler assembler =
                    new ReadmeAssembler(resourcesDir);
            ProjectConfig config =
                    TestConfigBuilder.builder()
                            .projectName("my-project")
                            .build();

            assembler.assemble(
                    config, new TemplateEngine(), outputDir);

            String content = Files.readString(
                    outputDir.resolve("README.md"),
                    StandardCharsets.UTF_8);
            assertThat(content)
                    .contains("**my-project**")
                    .doesNotContain("{{PROJECT_NAME}}");
        }

        @Test
        @DisplayName("replaces RULES_COUNT placeholder")
        void assemble_whenCalled_replacesRulesCount(@TempDir Path tempDir)
                throws IOException {
            Path resourcesDir = setupResources(tempDir);
            Path outputDir = setupOutput(tempDir);
            // Create 2 rules
            Path rulesDir = Files.createDirectories(
                    outputDir.resolve("rules"));
            Files.writeString(
                    rulesDir.resolve("01-identity.md"),
                    "c", StandardCharsets.UTF_8);
            Files.writeString(
                    rulesDir.resolve("02-domain.md"),
                    "c", StandardCharsets.UTF_8);

            ReadmeAssembler assembler =
                    new ReadmeAssembler(resourcesDir);
            ProjectConfig config =
                    TestConfigBuilder.minimal();

            assembler.assemble(
                    config, new TemplateEngine(), outputDir);

            String content = Files.readString(
                    outputDir.resolve("README.md"),
                    StandardCharsets.UTF_8);
            assertThat(content)
                    .doesNotContain("{{RULES_COUNT}}")
                    .contains("**Total: 2 rules**");
        }

        @Test
        @DisplayName("replaces all 12 placeholders")
        void assemble_whenCalled_replacesAll12Placeholders(
                @TempDir Path tempDir)
                throws IOException {
            Path resourcesDir = setupResources(tempDir);
            Path outputDir = setupOutput(tempDir);

            ReadmeAssembler assembler =
                    new ReadmeAssembler(resourcesDir);
            ProjectConfig config =
                    TestConfigBuilder.minimal();

            assembler.assemble(
                    config, new TemplateEngine(), outputDir);

            String content = Files.readString(
                    outputDir.resolve("README.md"),
                    StandardCharsets.UTF_8);
            assertThat(content)
                    .doesNotContain("{{PROJECT_NAME}}")
                    .doesNotContain("{{RULES_COUNT}}")
                    .doesNotContain("{{SKILLS_COUNT}}")
                    .doesNotContain("{{AGENTS_COUNT}}")
                    .doesNotContain("{{RULES_TABLE}}")
                    .doesNotContain("{{SKILLS_TABLE}}")
                    .doesNotContain("{{AGENTS_TABLE}}")
                    .doesNotContain("{{HOOKS_SECTION}}")
                    .doesNotContain(
                            "{{KNOWLEDGE_PACKS_TABLE}}")
                    .doesNotContain(
                            "{{SETTINGS_SECTION}}")
                    .doesNotContain("{{MAPPING_TABLE}}")
                    .doesNotContain(
                            "{{GENERATION_SUMMARY}}");
        }
    }

    @Nested
    @DisplayName("assemble — minimal mode")
    class MinimalMode {

        @Test
        @DisplayName("falls back to minimal when template"
                + " missing")
        void assemble_whenCalled_fallsBackToMinimal(@TempDir Path tempDir)
                throws IOException {
            // Resources dir without readme-template.md
            Path resourcesDir =
                    Files.createDirectories(
                            tempDir.resolve("resources"));
            Path outputDir =
                    Files.createDirectories(
                            tempDir.resolve("output"));

            ReadmeAssembler assembler =
                    new ReadmeAssembler(resourcesDir);
            ProjectConfig config =
                    TestConfigBuilder.builder()
                            .projectName("minimal-project")
                            .archStyle("library")
                            .build();

            List<String> files = assembler.assemble(
                    config, new TemplateEngine(), outputDir);

            assertThat(files).hasSize(1);
            String content = Files.readString(
                    outputDir.resolve("README.md"),
                    StandardCharsets.UTF_8);
            assertThat(content)
                    .contains("minimal-project")
                    .contains("## Structure")
                    .contains("## Tips");
        }
    }

    @Nested
    @DisplayName("generateMinimalReadme")
    class GenerateMinimalReadme {

        @Test
        @DisplayName("contains project name in header")
        void assemble_whenCalled_containsProjectNameInHeader() {
            ProjectConfig config =
                    TestConfigBuilder.builder()
                            .projectName("test-project")
                            .build();

            String content = ReadmeAssembler
                    .generateMinimalReadme(config);

            assertThat(content)
                    .startsWith(
                            "# .claude/ \u2014 test-project");
        }

        @Test
        @DisplayName("contains structure block")
        void assemble_whenCalled_containsStructureBlock() {
            ProjectConfig config =
                    TestConfigBuilder.minimal();

            String content = ReadmeAssembler
                    .generateMinimalReadme(config);

            assertThat(content)
                    .contains("## Structure")
                    .contains(".claude/")
                    .contains("rules/")
                    .contains("skills/")
                    .contains("agents/");
        }

        @Test
        @DisplayName("contains tips block with"
                + " architecture")
        void assemble_withArchitecture_containsTips() {
            ProjectConfig config =
                    TestConfigBuilder.builder()
                            .archStyle("microservice")
                            .clearInterfaces()
                            .addInterface("rest")
                            .addInterface("grpc")
                            .build();

            String content = ReadmeAssembler
                    .generateMinimalReadme(config);

            assertThat(content)
                    .contains("(microservice)")
                    .contains("(rest grpc)");
        }

        @Test
        @DisplayName("uses 'none' when no interfaces")
        void assemble_forNoInterfaces_usesNone() {
            ProjectConfig config =
                    TestConfigBuilder.builder()
                            .clearInterfaces()
                            .build();

            String content = ReadmeAssembler
                    .generateMinimalReadme(config);

            assertThat(content)
                    .contains("(none)");
        }
    }

    @Nested
    @DisplayName("buildStructureBlock")
    class BuildStructureBlock {

        @Test
        @DisplayName("contains directory tree elements")
        void assemble_whenCalled_containsDirectoryTree() {
            String block =
                    ReadmeAssembler.buildStructureBlock();

            assertThat(block)
                    .contains("## Structure")
                    .contains("```")
                    .contains("README.md")
                    .contains("settings.json")
                    .contains("settings.local.json")
                    .contains("rules/")
                    .contains("patterns/")
                    .contains("protocols/")
                    .contains("skills/")
                    .contains("agents/")
                    .contains("hooks/");
        }
    }

    @Nested
    @DisplayName("buildTipsBlock")
    class BuildTipsBlock {

        @Test
        @DisplayName("contains all tip lines")
        void assemble_whenCalled_containsAllTips() {
            String block = ReadmeAssembler
                    .buildTipsBlock(
                            "microservice", "rest grpc");

            assertThat(block)
                    .contains("## Tips")
                    .contains("Rules are always active")
                    .contains("Patterns are selected")
                    .contains("(microservice)")
                    .contains("Protocols are selected")
                    .contains("(rest grpc)")
                    .contains("Skills are lazy")
                    .contains("Agents are not invoked")
                    .contains("Hooks run automatically");
        }
    }

    @Nested
    @DisplayName("generateReadme — token replacement")
    class GenerateReadmeTokens {

        @Test
        @DisplayName("replaces PROJECT_NAME token")
        void generateReadme_whenCalled_replacesProjectNameToken(
                @TempDir Path tempDir) throws IOException {
            Path template = createSimpleTemplate(tempDir);
            Path outputDir = setupOutput(tempDir);

            ProjectConfig config =
                    TestConfigBuilder.builder()
                            .projectName("my-svc")
                            .build();

            String content = ReadmeAssembler
                    .generateReadme(
                            config, outputDir, template);

            assertThat(content)
                    .contains("**my-svc**")
                    .doesNotContain("{{PROJECT_NAME}}");
        }

        private Path createSimpleTemplate(Path tempDir)
                throws IOException {
            Path template =
                    tempDir.resolve("readme-template.md");
            Files.writeString(template,
                    "# Project: **{{PROJECT_NAME}}**\n"
                            + "Rules: {{RULES_COUNT}}\n"
                            + "Skills: {{SKILLS_COUNT}}\n"
                            + "Agents: {{AGENTS_COUNT}}\n"
                            + "{{RULES_TABLE}}\n"
                            + "{{SKILLS_TABLE}}\n"
                            + "{{AGENTS_TABLE}}\n"
                            + "{{HOOKS_SECTION}}\n"
                            + "{{KNOWLEDGE_PACKS_TABLE}}\n"
                            + "{{SETTINGS_SECTION}}\n"
                            + "{{MAPPING_TABLE}}\n"
                            + "{{GENERATION_SUMMARY}}\n",
                    StandardCharsets.UTF_8);
            return template;
        }
    }

    private static Path setupResources(Path tempDir)
            throws IOException {
        Path resourcesDir =
                Files.createDirectories(
                        tempDir.resolve("resources"));
        // Copy the actual readme-template.md from
        // classpath resources
        var url = ReadmeAssemblerTest.class
                .getClassLoader()
                .getResource("readme-template.md");
        if (url != null) {
            String templateContent = Files.readString(
                    Path.of(url.getPath()),
                    StandardCharsets.UTF_8);
            Files.writeString(
                    resourcesDir.resolve(
                            "readme-template.md"),
                    templateContent,
                    StandardCharsets.UTF_8);
        }
        return resourcesDir;
    }

    private static Path setupOutput(Path tempDir)
            throws IOException {
        Path outputDir = Files.createDirectories(
                tempDir.resolve("output").resolve(".claude"));
        // Create sibling .github dir
        Files.createDirectories(
                tempDir.resolve("output").resolve(
                        ".github"));
        return outputDir;
    }
}
