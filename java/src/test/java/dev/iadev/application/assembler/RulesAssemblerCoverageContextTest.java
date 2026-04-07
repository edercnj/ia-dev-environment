package dev.iadev.application.assembler;

import dev.iadev.testutil.TestConfigBuilder;

import dev.iadev.config.ContextBuilder;
import dev.iadev.domain.model.ProjectConfig;
import dev.iadev.template.TemplateEngine;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Coverage tests for RulesAssembler — context builder,
 * default constructor, and full integration.
 */
@DisplayName("RulesAssembler — coverage context")
class RulesAssemblerCoverageContextTest {

    @Nested
    @DisplayName("context via ContextBuilder — all entries")
    class BuildContextFull {

        @Test
        @DisplayName("context contains all 46 expected keys")
        void context_allFortyFourKeys_succeeds() {
            ProjectConfig config =
                    TestConfigBuilder.builder()
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

            assertThat(context).hasSize(46);
            assertThat(context)
                    .containsEntry(
                            "project_name", "ctx-full")
                    .containsEntry(
                            "project_purpose",
                            "full purpose")
                    .containsEntry(
                            "language_name", "typescript")
                    .containsEntry(
                            "language_version", "5")
                    .containsEntry(
                            "framework_name", "nestjs")
                    .containsEntry(
                            "framework_version", "10")
                    .containsEntry("build_tool", "npm")
                    .containsEntry(
                            "architecture_style",
                            "hexagonal")
                    .containsEntry(
                            "domain_driven", "True")
                    .containsEntry(
                            "event_driven", "True")
                    .containsEntry("container", "docker")
                    .containsEntry(
                            "orchestrator", "kubernetes")
                    .containsEntry(
                            "database_name", "postgresql")
                    .containsEntry(
                            "cache_name", "redis");
        }
    }

    @Nested
    @DisplayName("default constructor")
    class DefaultConstructor {

        @Test
        @DisplayName("default constructor resolves"
                + " resources")
        void default_defaultConstructorWorks_succeeds() {
            RulesAssembler assembler =
                    new RulesAssembler();
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
            assertThat(outputDir.resolve("rules"))
                    .exists();
            assertThat(outputDir.resolve("skills"))
                    .exists();
        }
    }
}
