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
 * Tests for GithubHooksAssembler -- the twelfth
 * assembler in the pipeline, copying hook JSON files
 * verbatim to .github/hooks/.
 */
@DisplayName("GithubHooksAssembler")
class GithubHooksAssemblerTest {

    @Nested
    @DisplayName("implements Assembler interface")
    class ImplementsAssembler {

        @Test
        @DisplayName("is instance of Assembler")
        void instanceOf_whenCreated_implementsAssemblerInterface() {
            GithubHooksAssembler assembler =
                    new GithubHooksAssembler();

            assertThat(assembler)
                    .isInstanceOf(Assembler.class);
        }
    }

    @Nested
    @DisplayName("GITHUB_HOOK_TEMPLATES constant")
    class HookTemplatesConstant {

        @Test
        @DisplayName("contains exactly 3 templates")
        void assemble_whenCalled_containsThreeTemplates() {
            assertThat(
                    GithubHooksAssembler
                            .GITHUB_HOOK_TEMPLATES)
                    .hasSize(3);
        }

        @Test
        @DisplayName("contains expected template names")
        void assemble_whenCalled_containsExpectedNames() {
            assertThat(
                    GithubHooksAssembler
                            .GITHUB_HOOK_TEMPLATES)
                    .containsExactly(
                            "post-compile-check.json",
                            "pre-commit-lint.json",
                            "session-context-loader.json");
        }
    }

    @Nested
    @DisplayName("assemble — copies hook JSON verbatim")
    class AssembleHooks {

        @Test
        @DisplayName("copies 3 hook JSON files to"
                + " hooks/ directory")
        void assemble_whenCalled_copiesThreeHookFiles(
                @TempDir Path tempDir)
                throws IOException {
            Path resourcesDir = createHooksResources(
                    tempDir);
            Path outputDir = tempDir.resolve("output");
            Files.createDirectories(outputDir);

            GithubHooksAssembler assembler =
                    new GithubHooksAssembler(resourcesDir);
            ProjectConfig config =
                    TestConfigBuilder.minimal();
            TemplateEngine engine = new TemplateEngine();

            List<String> files = assembler.assemble(
                    config, engine, outputDir);

            assertThat(files).hasSize(3);
        }

        @Test
        @DisplayName("creates hooks/ subdirectory")
        void assemble_whenCalled_createsHooksSubdirectory(
                @TempDir Path tempDir)
                throws IOException {
            Path resourcesDir = createHooksResources(
                    tempDir);
            Path outputDir = tempDir.resolve("output");
            Files.createDirectories(outputDir);

            GithubHooksAssembler assembler =
                    new GithubHooksAssembler(resourcesDir);
            ProjectConfig config =
                    TestConfigBuilder.minimal();
            TemplateEngine engine = new TemplateEngine();

            assembler.assemble(config, engine, outputDir);

            assertThat(outputDir.resolve("hooks"))
                    .exists()
                    .isDirectory();
        }

        @Test
        @DisplayName("output files are verbatim copies"
                + " of templates")
        void assemble_output_isVerbatimCopy(
                @TempDir Path tempDir)
                throws IOException {
            Path resourcesDir = createHooksResources(
                    tempDir);
            Path outputDir = tempDir.resolve("output");
            Files.createDirectories(outputDir);

            GithubHooksAssembler assembler =
                    new GithubHooksAssembler(resourcesDir);
            ProjectConfig config =
                    TestConfigBuilder.minimal();
            TemplateEngine engine = new TemplateEngine();

            assembler.assemble(config, engine, outputDir);

            for (String name
                    : GithubHooksAssembler
                            .GITHUB_HOOK_TEMPLATES) {
                Path src = resourcesDir.resolve(
                        "github-hooks-templates/" + name);
                Path dest = outputDir.resolve(
                        "hooks/" + name);
                String srcContent = Files.readString(
                        src, StandardCharsets.UTF_8);
                String destContent = Files.readString(
                        dest, StandardCharsets.UTF_8);
                assertThat(destContent)
                        .as("content of %s", name)
                        .isEqualTo(srcContent);
            }
        }

        @Test
        @DisplayName("does not use template engine"
                + " for rendering")
        void assemble_whenCalled_doesNotUseTemplateEngine(
                @TempDir Path tempDir)
                throws IOException {
            Path resourcesDir = createHooksResources(
                    tempDir);
            // Write a template with Pebble syntax
            Path src = resourcesDir.resolve(
                    "github-hooks-templates/"
                            + "post-compile-check.json");
            String contentWithPebble =
                    "{\"name\": \"{{ project_name }}\"}";
            Files.writeString(src, contentWithPebble,
                    StandardCharsets.UTF_8);

            Path outputDir = tempDir.resolve("output");
            Files.createDirectories(outputDir);

            GithubHooksAssembler assembler =
                    new GithubHooksAssembler(resourcesDir);
            ProjectConfig config =
                    TestConfigBuilder.minimal();
            TemplateEngine engine = new TemplateEngine();

            assembler.assemble(config, engine, outputDir);

            Path dest = outputDir.resolve(
                    "hooks/post-compile-check.json");
            String result = Files.readString(
                    dest, StandardCharsets.UTF_8);
            // Pebble syntax preserved (not rendered)
            assertThat(result)
                    .isEqualTo(contentWithPebble);
        }

        @Test
        @DisplayName("result paths end with .json")
        void assemble_whenCalled_resultPathsEndWithJson(
                @TempDir Path tempDir)
                throws IOException {
            Path resourcesDir = createHooksResources(
                    tempDir);
            Path outputDir = tempDir.resolve("output");
            Files.createDirectories(outputDir);

            GithubHooksAssembler assembler =
                    new GithubHooksAssembler(resourcesDir);
            ProjectConfig config =
                    TestConfigBuilder.minimal();
            TemplateEngine engine = new TemplateEngine();

            List<String> files = assembler.assemble(
                    config, engine, outputDir);

            assertThat(files)
                    .allMatch(f -> f.endsWith(".json"));
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

            GithubHooksAssembler assembler =
                    new GithubHooksAssembler(resourcesDir);
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
            Path hooksDir = tempDir.resolve(
                    "github-hooks-templates");
            Files.createDirectories(hooksDir);
            // Only create 2 of 3 templates
            Files.writeString(
                    hooksDir.resolve(
                            "post-compile-check.json"),
                    "{}",
                    StandardCharsets.UTF_8);
            Files.writeString(
                    hooksDir.resolve(
                            "pre-commit-lint.json"),
                    "{}",
                    StandardCharsets.UTF_8);
            // session-context-loader.json missing

            Path outputDir = tempDir.resolve("output");
            Files.createDirectories(outputDir);

            GithubHooksAssembler assembler =
                    new GithubHooksAssembler(tempDir);
            ProjectConfig config =
                    TestConfigBuilder.minimal();
            TemplateEngine engine = new TemplateEngine();

            List<String> files = assembler.assemble(
                    config, engine, outputDir);

            assertThat(files).hasSize(2);
        }
    }

    @Nested
    @DisplayName("assemble — classpath resources")
    class ClasspathResources {

        @Test
        @DisplayName("generates hooks from classpath"
                + " templates")
        void assemble_whenCalled_generatesFromClasspath(
                @TempDir Path tempDir) {
            Path outputDir = tempDir.resolve("output");

            GithubHooksAssembler assembler =
                    new GithubHooksAssembler();
            ProjectConfig config =
                    TestConfigBuilder.minimal();
            TemplateEngine engine = new TemplateEngine();

            List<String> files = assembler.assemble(
                    config, engine, outputDir);

            assertThat(files).hasSize(3);
            assertThat(files)
                    .allMatch(f -> f.endsWith(".json"));
        }
    }

    /**
     * Creates a temporary resources directory with 3
     * hook template JSON files.
     */
    private static Path createHooksResources(Path tempDir)
            throws IOException {
        Path hooksDir = tempDir.resolve(
                "github-hooks-templates");
        Files.createDirectories(hooksDir);
        for (String name
                : GithubHooksAssembler
                        .GITHUB_HOOK_TEMPLATES) {
            Files.writeString(
                    hooksDir.resolve(name),
                    "{\"hook\": \"" + name + "\"}",
                    StandardCharsets.UTF_8);
        }
        return tempDir;
    }
}
