package dev.iadev.application.assembler;

import dev.iadev.config.ContextBuilder;
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
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Additional coverage tests for RulesAssembler —
 * targeting uncovered branches and edge cases.
 */
@DisplayName("RulesAssembler — coverage")
class RulesAssemblerCoverageTest {

    @Nested
    @DisplayName("copyCoreRules — edge cases")
    class CopyCoreRulesEdgeCases {

        @Test
        @DisplayName("core-rules dir missing returns"
                + " only identity and domain")
        void assemble_noCoreRulesDir_succeeds(@TempDir Path tempDir)
                throws IOException {
            Path resourceDir = tempDir.resolve("res");
            Files.createDirectories(resourceDir);
            Path templates = resourceDir.resolve("templates");
            Files.createDirectories(templates);
            Files.writeString(
                    templates.resolve("domain-template.md"),
                    "Domain template\n");

            Path outputDir = tempDir.resolve("output");
            Files.createDirectories(outputDir);

            RulesAssembler assembler =
                    new RulesAssembler(resourceDir);
            ProjectConfig config =
                    TestConfigBuilder.minimal();

            List<String> files = assembler.assemble(
                    config, new TemplateEngine(), outputDir);

            assertThat(files).anyMatch(
                    f -> f.contains("01-project-identity.md"));
            assertThat(files).anyMatch(
                    f -> f.contains("02-domain.md"));
        }

        @Test
        @DisplayName("core-rules is a file not directory"
                + " returns only identity and domain")
        void assemble_coreRules_isFile(@TempDir Path tempDir)
                throws IOException {
            Path resourceDir = tempDir.resolve("res");
            Files.createDirectories(resourceDir);
            Files.writeString(
                    resourceDir.resolve("core-rules"),
                    "not a directory");
            Path templates = resourceDir.resolve("templates");
            Files.createDirectories(templates);
            Files.writeString(
                    templates.resolve("domain-template.md"),
                    "Domain\n");

            Path outputDir = tempDir.resolve("output");
            Files.createDirectories(outputDir);

            RulesAssembler assembler =
                    new RulesAssembler(resourceDir);
            ProjectConfig config =
                    TestConfigBuilder.minimal();

            List<String> files = assembler.assemble(
                    config, new TemplateEngine(), outputDir);

            assertThat(files).anyMatch(
                    f -> f.contains("01-project-identity.md"));
        }
    }

    @Nested
    @DisplayName("routeCoreToKps — edge cases")
    class RouteCoreToKps {

        @Test
        @DisplayName("core dir missing returns empty"
                + " kp list")
        void routeCoreToKps_noCoreDir_succeeds(@TempDir Path tempDir)
                throws IOException {
            Path resourceDir = tempDir.resolve("res");
            Path coreRules = resourceDir.resolve("core-rules");
            Files.createDirectories(coreRules);
            Path templates = resourceDir.resolve("templates");
            Files.createDirectories(templates);
            Files.writeString(
                    templates.resolve("domain-template.md"),
                    "Domain\n");

            Path outputDir = tempDir.resolve("output");
            Files.createDirectories(outputDir);

            RulesAssembler assembler =
                    new RulesAssembler(resourceDir);
            ProjectConfig config =
                    TestConfigBuilder.minimal();

            List<String> files = assembler.assemble(
                    config, new TemplateEngine(), outputDir);

            assertThat(files)
                    .noneMatch(f -> f.contains("references"));
        }

        @Test
        @DisplayName("core dir is a file returns empty")
        void routeCoreToKps_coreDir_isFile(@TempDir Path tempDir)
                throws IOException {
            Path resourceDir = tempDir.resolve("res");
            Path coreRules = resourceDir.resolve("core-rules");
            Files.createDirectories(coreRules);
            Files.writeString(
                    resourceDir.resolve("core"),
                    "not a dir");
            Path templates = resourceDir.resolve("templates");
            Files.createDirectories(templates);
            Files.writeString(
                    templates.resolve("domain-template.md"),
                    "Domain\n");

            Path outputDir = tempDir.resolve("output");
            Files.createDirectories(outputDir);

            RulesAssembler assembler =
                    new RulesAssembler(resourceDir);
            ProjectConfig config =
                    TestConfigBuilder.minimal();

            List<String> files = assembler.assemble(
                    config, new TemplateEngine(), outputDir);

            assertThat(files).isNotEmpty();
        }

        @Test
        @DisplayName("core dir present but route source"
                + " files missing triggers continue")
        void routeCoreToKps_whenCalled_routeSourceFileMissing(
                @TempDir Path tempDir) throws IOException {
            Path resourceDir = setupMinimalRes(tempDir);
            Path coreDir = resourceDir.resolve("core");
            Files.createDirectories(coreDir);

            Path outputDir = tempDir.resolve("output");
            Files.createDirectories(outputDir);

            RulesAssembler assembler =
                    new RulesAssembler(resourceDir);
            ProjectConfig config =
                    TestConfigBuilder.builder()
                            .clearInterfaces()
                            .addInterface("rest")
                            .build();

            List<String> files = assembler.assemble(
                    config, new TemplateEngine(), outputDir);

            assertThat(files).isNotEmpty();
        }
    }

    @Nested
    @DisplayName("copyLanguageKps — edge cases")
    class CopyLanguageKps {

        @Test
        @DisplayName("language dir missing returns empty")
        void copyLanguageKps_noLanguageDir_succeeds(@TempDir Path tempDir)
                throws IOException {
            Path resourceDir = setupMinimalRes(tempDir);
            Path outputDir = tempDir.resolve("output");
            Files.createDirectories(outputDir);

            RulesAssembler assembler =
                    new RulesAssembler(resourceDir);
            ProjectConfig config =
                    TestConfigBuilder.builder()
                            .language("unknown-lang", "1.0")
                            .build();

            List<String> files = assembler.assemble(
                    config, new TemplateEngine(), outputDir);

            assertThat(files).noneMatch(
                    f -> f.contains("coding-standards"));
        }

        @Test
        @DisplayName("language dir is a file returns empty")
        void copyLanguageKps_languageDir_isFile(@TempDir Path tempDir)
                throws IOException {
            Path resourceDir = setupMinimalRes(tempDir);
            Path langParent = resourceDir.resolve(
                    "languages");
            Files.createDirectories(langParent);
            Files.writeString(
                    langParent.resolve("java"),
                    "not a dir");
            Path outputDir = tempDir.resolve("output");
            Files.createDirectories(outputDir);

            RulesAssembler assembler =
                    new RulesAssembler(resourceDir);
            ProjectConfig config =
                    TestConfigBuilder.builder()
                            .language("java", "21")
                            .build();

            List<String> files = assembler.assemble(
                    config, new TemplateEngine(), outputDir);

            assertThat(files).isNotEmpty();
        }

        @Test
        @DisplayName("common subdir missing still works")
        void copyLanguageKps_noCommonSubdir_succeeds(@TempDir Path tempDir)
                throws IOException {
            Path resourceDir = setupMinimalRes(tempDir);
            Path langDir = resourceDir.resolve(
                    "languages/java");
            Files.createDirectories(langDir);

            Path outputDir = tempDir.resolve("output");
            Files.createDirectories(outputDir);

            RulesAssembler assembler =
                    new RulesAssembler(resourceDir);
            ProjectConfig config =
                    TestConfigBuilder.builder()
                            .language("java", "21")
                            .build();

            List<String> files = assembler.assemble(
                    config, new TemplateEngine(), outputDir);

            assertThat(files).isNotEmpty();
        }

        @Test
        @DisplayName("common with testing file routes"
                + " to testing refs")
        void copyLanguageKps_whenCalled_testingFileRoutesToTestingRefs(
                @TempDir Path tempDir) throws IOException {
            Path resourceDir = setupMinimalRes(tempDir);
            Path common = resourceDir.resolve(
                    "languages/java/common");
            Files.createDirectories(common);
            Files.writeString(
                    common.resolve("testing-patterns.md"),
                    "testing content");
            Files.writeString(
                    common.resolve("coding-style.md"),
                    "coding content");

            Path outputDir = tempDir.resolve("output");
            Files.createDirectories(outputDir);

            RulesAssembler assembler =
                    new RulesAssembler(resourceDir);
            ProjectConfig config =
                    TestConfigBuilder.builder()
                            .language("java", "21")
                            .build();

            List<String> files = assembler.assemble(
                    config, new TemplateEngine(), outputDir);

            assertThat(files).anyMatch(
                    f -> f.contains("testing/references/"
                            + "testing-patterns.md"));
            assertThat(files).anyMatch(
                    f -> f.contains("coding-standards/"
                            + "references/"
                            + "coding-style.md"));
        }

        @Test
        @DisplayName("version dir empty returns no"
                + " version files")
        void copyLanguageKps_noVersionDir_succeeds(@TempDir Path tempDir)
                throws IOException {
            Path resourceDir = setupMinimalRes(tempDir);
            Path langDir = resourceDir.resolve(
                    "languages/java");
            Files.createDirectories(langDir);

            Path outputDir = tempDir.resolve("output");
            Files.createDirectories(outputDir);

            RulesAssembler assembler =
                    new RulesAssembler(resourceDir);
            ProjectConfig config =
                    TestConfigBuilder.builder()
                            .language("java", "99")
                            .build();

            List<String> files = assembler.assemble(
                    config, new TemplateEngine(), outputDir);

            assertThat(files).isNotEmpty();
        }
    }

    @Nested
    @DisplayName("copyFrameworkKps — edge cases")
    class CopyFrameworkKps {

        @Test
        @DisplayName("unknown framework returns empty")
        void copyFrameworkKps_whenCalled_unknownFramework(@TempDir Path tempDir)
                throws IOException {
            Path resourceDir = setupMinimalRes(tempDir);
            Path outputDir = tempDir.resolve("output");
            Files.createDirectories(outputDir);

            RulesAssembler assembler =
                    new RulesAssembler(resourceDir);
            ProjectConfig config =
                    TestConfigBuilder.builder()
                            .framework("unknown-fw", "1.0")
                            .build();

            List<String> files = assembler.assemble(
                    config, new TemplateEngine(), outputDir);

            assertThat(files).isNotEmpty();
        }

        @Test
        @DisplayName("framework dir missing returns empty")
        void copyFrameworkKps_whenCalled_frameworkDirMissing(@TempDir Path tempDir)
                throws IOException {
            Path resourceDir = setupMinimalRes(tempDir);
            Path outputDir = tempDir.resolve("output");
            Files.createDirectories(outputDir);

            RulesAssembler assembler =
                    new RulesAssembler(resourceDir);
            ProjectConfig config =
                    TestConfigBuilder.builder()
                            .framework("quarkus", "3.17")
                            .build();

            List<String> files = assembler.assemble(
                    config, new TemplateEngine(), outputDir);

            assertThat(files).isNotEmpty();
        }

        @Test
        @DisplayName("framework dir is a file returns"
                + " empty fw files")
        void copyFrameworkKps_frameworkDir_isFile(@TempDir Path tempDir)
                throws IOException {
            Path resourceDir = setupMinimalRes(tempDir);
            Path frameworks = resourceDir.resolve(
                    "frameworks");
            Files.createDirectories(frameworks);
            Files.writeString(
                    frameworks.resolve("quarkus"),
                    "not a dir");

            Path outputDir = tempDir.resolve("output");
            Files.createDirectories(outputDir);

            RulesAssembler assembler =
                    new RulesAssembler(resourceDir);
            ProjectConfig config =
                    TestConfigBuilder.builder()
                            .framework("quarkus", "3.17")
                            .build();

            List<String> files = assembler.assemble(
                    config, new TemplateEngine(), outputDir);

            assertThat(files).isNotEmpty();
        }

        @Test
        @DisplayName("framework with common and version"
                + " copies both")
        void copyFrameworkKps_whenCalled_frameworkCommonAndVersion(
                @TempDir Path tempDir) throws IOException {
            Path resourceDir = setupMinimalRes(tempDir);
            Path fwDir = resourceDir.resolve(
                    "frameworks/quarkus");
            Path common = fwDir.resolve("common");
            Files.createDirectories(common);
            Files.writeString(
                    common.resolve("patterns.md"),
                    "common patterns");

            Path outputDir = tempDir.resolve("output");
            Files.createDirectories(outputDir);

            RulesAssembler assembler =
                    new RulesAssembler(resourceDir);
            ProjectConfig config =
                    TestConfigBuilder.builder()
                            .framework("quarkus", "3.17")
                            .build();

            List<String> files = assembler.assemble(
                    config, new TemplateEngine(), outputDir);

            assertThat(files).anyMatch(
                    f -> f.contains("patterns.md"));
        }

        @Test
        @DisplayName("framework common missing still works")
        void copyFrameworkKps_whenCalled_frameworkNoCommon(@TempDir Path tempDir)
                throws IOException {
            Path resourceDir = setupMinimalRes(tempDir);
            Path fwDir = resourceDir.resolve(
                    "frameworks/quarkus");
            Files.createDirectories(fwDir);

            Path outputDir = tempDir.resolve("output");
            Files.createDirectories(outputDir);

            RulesAssembler assembler =
                    new RulesAssembler(resourceDir);
            ProjectConfig config =
                    TestConfigBuilder.builder()
                            .framework("quarkus", "3.17")
                            .build();

            List<String> files = assembler.assemble(
                    config, new TemplateEngine(), outputDir);

            assertThat(files).isNotEmpty();
        }
    }

    @Nested
    @DisplayName("context via ContextBuilder — all entries")
    class BuildContextFull {

        @Test
        @DisplayName("context contains all 43 expected keys")
        void context_allFortyThreeKeys_succeeds() {
            ProjectConfig config = TestConfigBuilder.builder()
                    .projectName("ctx-full")
                    .purpose("full purpose")
                    .language("typescript", "5")
                    .framework("nestjs", "10")
                    .buildTool("npm")
                    .archStyle("hexagonal")
                    .domainDriven(true)
                    .eventDriven(true)
                    .container("docker")
                    .orchestrator("kubernetes")
                    .database("postgresql", "16")
                    .cache("redis", "7.4")
                    .build();

            Map<String, Object> context =
                    ContextBuilder.buildContext(config);

            assertThat(context).hasSize(43);
            assertThat(context)
                    .containsEntry("project_name", "ctx-full")
                    .containsEntry("project_purpose",
                            "full purpose")
                    .containsEntry("language_name",
                            "typescript")
                    .containsEntry("language_version", "5")
                    .containsEntry("framework_name", "nestjs")
                    .containsEntry("framework_version", "10")
                    .containsEntry("build_tool", "npm")
                    .containsEntry("architecture_style",
                            "hexagonal")
                    .containsEntry("domain_driven", "True")
                    .containsEntry("event_driven", "True")
                    .containsEntry("container", "docker")
                    .containsEntry("orchestrator",
                            "kubernetes")
                    .containsEntry("database_name",
                            "postgresql")
                    .containsEntry("cache_name", "redis");
        }
    }

    @Nested
    @DisplayName("default constructor")
    class DefaultConstructor {

        @Test
        @DisplayName("default constructor resolves resources")
        void default_defaultConstructorWorks_succeeds() {
            RulesAssembler assembler = new RulesAssembler();
            assertThat(assembler)
                    .isInstanceOf(Assembler.class);
        }
    }

    @Nested
    @DisplayName("assemble — full integration")
    class FullIntegration {

        @Test
        @DisplayName("all assembly layers produce files"
                + " for rich config")
        void assemble_allLayersForRichConfig_succeeds(
                @TempDir Path tempDir) throws IOException {
            Path outputDir = tempDir.resolve("output");
            Files.createDirectories(outputDir);

            RulesAssembler assembler =
                    new RulesAssembler();
            ProjectConfig config =
                    TestConfigBuilder.builder()
                            .projectName("full-test")
                            .language("java", "21")
                            .framework("quarkus", "3.17")
                            .buildTool("maven")
                            .database("postgresql", "16")
                            .cache("redis", "7.4")
                            .container("docker")
                            .orchestrator("kubernetes")
                            .iac("terraform")
                            .cloudProvider("aws")
                            .securityFrameworks("owasp")
                            .build();

            List<String> files = assembler.assemble(
                    config, new TemplateEngine(), outputDir);

            assertThat(files).isNotEmpty();
            assertThat(outputDir.resolve("rules")).exists();
            assertThat(outputDir.resolve("skills")).exists();
        }
    }

    private static Path setupMinimalRes(Path tempDir)
            throws IOException {
        Path resourceDir = tempDir.resolve("res");
        Path coreRules = resourceDir.resolve("core-rules");
        Files.createDirectories(coreRules);
        Path templates = resourceDir.resolve("templates");
        Files.createDirectories(templates);
        Files.writeString(
                templates.resolve("domain-template.md"),
                "Domain {DOMAIN_NAME}\n",
                StandardCharsets.UTF_8);
        return resourceDir;
    }
}
