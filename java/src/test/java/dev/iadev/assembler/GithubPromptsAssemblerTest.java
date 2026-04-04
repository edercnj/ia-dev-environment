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
 * Tests for GithubPromptsAssembler -- the thirteenth
 * assembler in the pipeline, rendering Pebble prompt
 * templates to .github/prompts/*.prompt.md.
 */
@DisplayName("GithubPromptsAssembler")
class GithubPromptsAssemblerTest {

    @Nested
    @DisplayName("implements Assembler interface")
    class ImplementsAssembler {

        @Test
        @DisplayName("is instance of Assembler")
        void instanceOf_whenCreated_implementsAssemblerInterface() {
            GithubPromptsAssembler assembler =
                    new GithubPromptsAssembler();

            assertThat(assembler)
                    .isInstanceOf(Assembler.class);
        }
    }

    @Nested
    @DisplayName("GITHUB_PROMPT_TEMPLATES constant")
    class PromptTemplatesConstant {

        @Test
        @DisplayName("contains exactly 4 templates")
        void assemble_whenCalled_containsFourTemplates() {
            assertThat(
                    GithubPromptsAssembler
                            .GITHUB_PROMPT_TEMPLATES)
                    .hasSize(4);
        }

        @Test
        @DisplayName("contains expected template names")
        void assemble_whenCalled_containsExpectedNames() {
            assertThat(
                    GithubPromptsAssembler
                            .GITHUB_PROMPT_TEMPLATES)
                    .containsExactly(
                            "new-feature.prompt.md.j2",
                            "decompose-spec.prompt.md.j2",
                            "code-review.prompt.md.j2",
                            "troubleshoot.prompt.md.j2");
        }

        @Test
        @DisplayName("all templates end with .j2 suffix")
        void assemble_withJ2Suffix_allEnd() {
            assertThat(
                    GithubPromptsAssembler
                            .GITHUB_PROMPT_TEMPLATES)
                    .allMatch(t -> t.endsWith(".j2"));
        }
    }

    @Nested
    @DisplayName("assemble — renders prompts with Pebble")
    class AssemblePrompts {

        @Test
        @DisplayName("renders 4 prompt files to"
                + " prompts/ directory")
        void assemble_whenCalled_rendersFourPromptFiles(
                @TempDir Path tempDir) {
            Path outputDir = tempDir.resolve("output");

            GithubPromptsAssembler assembler =
                    new GithubPromptsAssembler();
            ProjectConfig config =
                    TestConfigBuilder.minimal();
            TemplateEngine engine = new TemplateEngine();

            List<String> files = assembler.assemble(
                    config, engine, outputDir);

            assertThat(files).hasSize(4);
        }

        @Test
        @DisplayName("creates prompts/ subdirectory")
        void assemble_whenCalled_createsPromptsSubdirectory(
                @TempDir Path tempDir) {
            Path outputDir = tempDir.resolve("output");

            GithubPromptsAssembler assembler =
                    new GithubPromptsAssembler();
            ProjectConfig config =
                    TestConfigBuilder.minimal();
            TemplateEngine engine = new TemplateEngine();

            assembler.assemble(config, engine, outputDir);

            assertThat(outputDir.resolve("prompts"))
                    .exists()
                    .isDirectory();
        }

        @Test
        @DisplayName("removes .j2 suffix from output"
                + " filenames")
        void assemble_whenCalled_removesJ2Suffix(
                @TempDir Path tempDir) {
            Path outputDir = tempDir.resolve("output");

            GithubPromptsAssembler assembler =
                    new GithubPromptsAssembler();
            ProjectConfig config =
                    TestConfigBuilder.minimal();
            TemplateEngine engine = new TemplateEngine();

            List<String> files = assembler.assemble(
                    config, engine, outputDir);

            assertThat(files)
                    .allMatch(f ->
                            f.endsWith(".prompt.md"));
            assertThat(files)
                    .noneMatch(f -> f.endsWith(".j2"));
        }

        @Test
        @DisplayName("output contains expected filenames")
        void assemble_output_containsExpectedFilenames(
                @TempDir Path tempDir) {
            Path outputDir = tempDir.resolve("output");

            GithubPromptsAssembler assembler =
                    new GithubPromptsAssembler();
            ProjectConfig config =
                    TestConfigBuilder.minimal();
            TemplateEngine engine = new TemplateEngine();

            List<String> files = assembler.assemble(
                    config, engine, outputDir);

            List<String> fileNames = files.stream()
                    .map(f -> Path.of(f).getFileName()
                            .toString())
                    .toList();
            assertThat(fileNames)
                    .containsExactly(
                            "new-feature.prompt.md",
                            "decompose-spec.prompt.md",
                            "code-review.prompt.md",
                            "troubleshoot.prompt.md");
        }

        @Test
        @DisplayName("resolves project variables in"
                + " output")
        void assemble_whenCalled_resolvesProjectVariables(
                @TempDir Path tempDir) {
            Path outputDir = tempDir.resolve("output");

            GithubPromptsAssembler assembler =
                    new GithubPromptsAssembler();
            ProjectConfig config =
                    TestConfigBuilder.builder()
                            .projectName("api-pagamentos")
                            .language("java", "21")
                            .build();
            TemplateEngine engine = new TemplateEngine();

            assembler.assemble(config, engine, outputDir);

            Path prompt = outputDir.resolve(
                    "prompts/new-feature.prompt.md");
            assertThat(prompt).exists();
            String content = readFile(prompt);
            assertThat(content)
                    .contains("api-pagamentos");
            assertThat(content)
                    .doesNotContain(
                            "{{ project_name }}");
        }

        @Test
        @DisplayName("resolves language variables in"
                + " output")
        void assemble_whenCalled_resolvesLanguageVariables(
                @TempDir Path tempDir) {
            Path outputDir = tempDir.resolve("output");

            GithubPromptsAssembler assembler =
                    new GithubPromptsAssembler();
            ProjectConfig config =
                    TestConfigBuilder.builder()
                            .language("python", "3.12")
                            .build();
            TemplateEngine engine = new TemplateEngine();

            assembler.assemble(config, engine, outputDir);

            Path prompt = outputDir.resolve(
                    "prompts/new-feature.prompt.md");
            String content = readFile(prompt);
            assertThat(content)
                    .contains("python");
        }

        @Test
        @DisplayName("uses renderTemplate not"
                + " replacePlaceholders")
        void assemble_whenCalled_usesRenderTemplateNotReplace(
                @TempDir Path tempDir)
                throws IOException {
            // Create a template with Pebble control flow
            Path promptsDir = tempDir.resolve(
                    "github-prompts-templates");
            Files.createDirectories(promptsDir);
            String template = "{% if framework_version"
                    + " %}v{{ framework_version }}"
                    + "{% endif %}";
            Files.writeString(
                    promptsDir.resolve(
                            "new-feature.prompt.md.j2"),
                    template, StandardCharsets.UTF_8);

            Path outputDir = tempDir.resolve("output");

            GithubPromptsAssembler assembler =
                    new GithubPromptsAssembler(tempDir);
            ProjectConfig config =
                    TestConfigBuilder.builder()
                            .framework("quarkus", "3.17")
                            .build();
            TemplateEngine engine =
                    new TemplateEngine(tempDir);

            assembler.assemble(config, engine, outputDir);

            Path dest = outputDir.resolve(
                    "prompts/new-feature.prompt.md");
            assertThat(dest).exists();
            String content = readFile(dest);
            assertThat(content).contains("v3.17");
            assertThat(content)
                    .doesNotContain("{%");
        }
    }

    @Nested
    @DisplayName("assemble — graceful no-op")
    class GracefulNoOp {

        @Test
        @DisplayName("returns empty list when templates"
                + " directory absent")
        void assemble_whenCalled_returnsEmptyWhenTemplatesDirAbsent(
                @TempDir Path tempDir) {
            Path resourcesDir =
                    tempDir.resolve("nonexistent");
            Path outputDir = tempDir.resolve("output");

            GithubPromptsAssembler assembler =
                    new GithubPromptsAssembler(
                            resourcesDir);
            ProjectConfig config =
                    TestConfigBuilder.minimal();
            TemplateEngine engine = new TemplateEngine();

            List<String> files = assembler.assemble(
                    config, engine, outputDir);

            assertThat(files).isEmpty();
        }

        @Test
        @DisplayName("skips individual missing template")
        void assemble_whenCalled_skipsIndividualMissingTemplate(
                @TempDir Path tempDir)
                throws IOException {
            Path promptsDir = tempDir.resolve(
                    "github-prompts-templates");
            Files.createDirectories(promptsDir);
            // Only create 2 of 4 templates
            Files.writeString(
                    promptsDir.resolve(
                            "new-feature.prompt.md.j2"),
                    "# Feature",
                    StandardCharsets.UTF_8);
            Files.writeString(
                    promptsDir.resolve(
                            "code-review.prompt.md.j2"),
                    "# Review",
                    StandardCharsets.UTF_8);

            Path outputDir = tempDir.resolve("output");

            GithubPromptsAssembler assembler =
                    new GithubPromptsAssembler(tempDir);
            ProjectConfig config =
                    TestConfigBuilder.minimal();
            TemplateEngine engine =
                    new TemplateEngine(tempDir);

            List<String> files = assembler.assemble(
                    config, engine, outputDir);

            assertThat(files).hasSize(2);
        }
    }

    @Nested
    @DisplayName("assemble — classpath resources")
    class ClasspathResources {

        @Test
        @DisplayName("generates prompts from classpath"
                + " templates")
        void assemble_whenCalled_generatesFromClasspath(
                @TempDir Path tempDir) {
            Path outputDir = tempDir.resolve("output");

            GithubPromptsAssembler assembler =
                    new GithubPromptsAssembler();
            ProjectConfig config =
                    TestConfigBuilder.minimal();
            TemplateEngine engine = new TemplateEngine();

            List<String> files = assembler.assemble(
                    config, engine, outputDir);

            assertThat(files).hasSize(4);
            assertThat(files)
                    .allMatch(f ->
                            f.endsWith(".prompt.md"));
        }

        @Test
        @DisplayName("no unresolved variables in"
                + " rendered output")
        void assemble_noUnresolvedVariables_succeeds(
                @TempDir Path tempDir) {
            Path outputDir = tempDir.resolve("output");

            GithubPromptsAssembler assembler =
                    new GithubPromptsAssembler();
            ProjectConfig config =
                    TestConfigBuilder.builder()
                            .projectName("my-app")
                            .language("typescript", "5")
                            .framework("commander", "")
                            .build();
            TemplateEngine engine = new TemplateEngine();

            assembler.assemble(config, engine, outputDir);

            Path promptsDir =
                    outputDir.resolve("prompts");
            assertThat(promptsDir).exists();
            for (String name : List.of(
                    "new-feature.prompt.md",
                    "decompose-spec.prompt.md",
                    "code-review.prompt.md",
                    "troubleshoot.prompt.md")) {
                Path file = promptsDir.resolve(name);
                assertThat(file)
                        .as("file %s exists", name)
                        .exists();
                String content = readFile(file);
                assertThat(content)
                        .as("no {{ in %s", name)
                        .doesNotContain("{{ ");
            }
        }
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
