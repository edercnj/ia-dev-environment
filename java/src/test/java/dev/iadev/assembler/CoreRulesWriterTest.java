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
 * Tests for CoreRulesWriter — writes core rules (01-05),
 * routes core KPs, generates identity and domain files,
 * and copies conditional resources.
 */
@DisplayName("CoreRulesWriter")
class CoreRulesWriterTest {

    @Nested
    @DisplayName("copyCoreRules")
    class CopyCoreRules {

        @Test
        @DisplayName("copies core rule files with"
                + " placeholder replacement")
        void copiesWithPlaceholders(
                @TempDir Path tempDir) throws IOException {
            Path resourceDir = tempDir.resolve("res");
            Path coreRules =
                    resourceDir.resolve("core-rules");
            Files.createDirectories(coreRules);
            Files.writeString(
                    coreRules.resolve("03-test.md"),
                    "Project: {PROJECT_NAME}\n",
                    StandardCharsets.UTF_8);

            Path rulesDir = tempDir.resolve("rules");
            Files.createDirectories(rulesDir);

            CoreRulesWriter writer =
                    new CoreRulesWriter(resourceDir);
            Map<String, Object> context =
                    Map.of("project_name", "my-project");

            List<String> files = writer.copyCoreRules(
                    rulesDir, new TemplateEngine(), context);

            assertThat(files).hasSize(1);
            String content = Files.readString(
                    rulesDir.resolve("03-test.md"),
                    StandardCharsets.UTF_8);
            assertThat(content).contains("my-project");
        }

        @Test
        @DisplayName("missing core-rules dir returns empty")
        void missingDirReturnsEmpty(
                @TempDir Path tempDir) throws IOException {
            Path resourceDir = tempDir.resolve("res");
            Files.createDirectories(resourceDir);

            Path rulesDir = tempDir.resolve("rules");
            Files.createDirectories(rulesDir);

            CoreRulesWriter writer =
                    new CoreRulesWriter(resourceDir);

            List<String> files = writer.copyCoreRules(
                    rulesDir, new TemplateEngine(), Map.of());

            assertThat(files).isEmpty();
        }
    }

    @Nested
    @DisplayName("routeCoreToKps")
    class RouteCoreToKps {

        @Test
        @DisplayName("missing core dir returns empty")
        void missingCoreDirReturnsEmpty(
                @TempDir Path tempDir) throws IOException {
            Path resourceDir = tempDir.resolve("res");
            Files.createDirectories(resourceDir);
            Path skillsDir = tempDir.resolve("skills");
            Files.createDirectories(skillsDir);

            CoreRulesWriter writer =
                    new CoreRulesWriter(resourceDir);
            ProjectConfig config =
                    TestConfigBuilder.minimal();

            List<String> files =
                    writer.routeCoreToKps(
                            config, skillsDir);

            assertThat(files).isEmpty();
        }

        @Test
        @DisplayName("core dir present but no route source"
                + " files skips")
        void missingRouteSourceSkips(
                @TempDir Path tempDir) throws IOException {
            Path resourceDir = tempDir.resolve("res");
            Path coreDir = resourceDir.resolve("core");
            Files.createDirectories(coreDir);
            Path skillsDir = tempDir.resolve("skills");
            Files.createDirectories(skillsDir);

            CoreRulesWriter writer =
                    new CoreRulesWriter(resourceDir);
            ProjectConfig config =
                    TestConfigBuilder.builder()
                            .clearInterfaces()
                            .addInterface("rest")
                            .build();

            List<String> files =
                    writer.routeCoreToKps(
                            config, skillsDir);

            assertThat(files).isEmpty();
        }

        @Test
        @DisplayName("route source is a directory not file"
                + " triggers continue")
        void routeSourceIsDirectorySkips(
                @TempDir Path tempDir) throws IOException {
            Path resourceDir = tempDir.resolve("res");
            Path coreDir = resourceDir.resolve("core");
            Files.createDirectories(coreDir);
            // Create a directory where a file is expected
            Files.createDirectories(
                    coreDir.resolve("api-design.md"));
            Path skillsDir = tempDir.resolve("skills");
            Files.createDirectories(skillsDir);

            CoreRulesWriter writer =
                    new CoreRulesWriter(resourceDir);
            ProjectConfig config =
                    TestConfigBuilder.builder()
                            .clearInterfaces()
                            .addInterface("rest")
                            .build();

            List<String> files =
                    writer.routeCoreToKps(
                            config, skillsDir);

            assertThat(files).isEmpty();
        }
    }

    @Nested
    @DisplayName("generateProjectIdentity")
    class GenerateProjectIdentity {

        @Test
        @DisplayName("generates 01-project-identity.md")
        void generatesFile(
                @TempDir Path tempDir) throws IOException {
            Path resourceDir = tempDir.resolve("res");
            Files.createDirectories(resourceDir);
            Path rulesDir = tempDir.resolve("rules");
            Files.createDirectories(rulesDir);

            CoreRulesWriter writer =
                    new CoreRulesWriter(resourceDir);
            ProjectConfig config =
                    TestConfigBuilder.builder()
                            .projectName("test-api")
                            .build();

            String path = writer.generateProjectIdentity(
                    config, rulesDir);

            assertThat(path).contains(
                    "01-project-identity.md");
            String content = Files.readString(
                    rulesDir.resolve(
                            "01-project-identity.md"),
                    StandardCharsets.UTF_8);
            assertThat(content).contains("test-api");
        }
    }

    @Nested
    @DisplayName("copyDomainTemplate")
    class CopyDomainTemplate {

        @Test
        @DisplayName("copies from template when present")
        void copiesFromTemplate(
                @TempDir Path tempDir) throws IOException {
            Path resourceDir = tempDir.resolve("res");
            Path templates =
                    resourceDir.resolve("templates");
            Files.createDirectories(templates);
            Files.writeString(
                    templates.resolve("domain-template.md"),
                    "# Rule — {DOMAIN_NAME} Domain\n");

            Path rulesDir = tempDir.resolve("rules");
            Files.createDirectories(rulesDir);

            CoreRulesWriter writer =
                    new CoreRulesWriter(resourceDir);
            ProjectConfig config =
                    TestConfigBuilder.minimal();

            String path = writer.copyDomainTemplate(
                    config, rulesDir,
                    new TemplateEngine(), Map.of());

            assertThat(path).contains("02-domain.md");
            String content = Files.readString(
                    rulesDir.resolve("02-domain.md"),
                    StandardCharsets.UTF_8);
            assertThat(content).contains("{DOMAIN_NAME}");
        }

        @Test
        @DisplayName("uses fallback when template missing")
        void usesFallback(
                @TempDir Path tempDir) throws IOException {
            Path resourceDir = tempDir.resolve("res");
            Files.createDirectories(resourceDir);
            Path rulesDir = tempDir.resolve("rules");
            Files.createDirectories(rulesDir);

            CoreRulesWriter writer =
                    new CoreRulesWriter(resourceDir);
            ProjectConfig config =
                    TestConfigBuilder.builder()
                            .projectName("fallback-proj")
                            .build();

            String path = writer.copyDomainTemplate(
                    config, rulesDir,
                    new TemplateEngine(), Map.of());

            assertThat(path).contains("02-domain.md");
            String content = Files.readString(
                    rulesDir.resolve("02-domain.md"),
                    StandardCharsets.UTF_8);
            assertThat(content).contains("fallback-proj");
        }
    }

    @Nested
    @DisplayName("copyConditionals")
    class CopyConditionals {

        @Test
        @DisplayName("minimal config returns empty"
                + " conditionals")
        void minimalConfigReturnsEmpty(
                @TempDir Path tempDir) throws IOException {
            Path resourceDir = tempDir.resolve("res");
            Files.createDirectories(resourceDir);
            Path skillsDir = tempDir.resolve("skills");
            Files.createDirectories(skillsDir);

            CoreRulesWriter writer =
                    new CoreRulesWriter(resourceDir);
            ProjectConfig config =
                    TestConfigBuilder.minimal();

            List<String> files = writer.copyConditionals(
                    config, skillsDir,
                    new TemplateEngine(), Map.of());

            assertThat(files).isEmpty();
        }
    }
}
