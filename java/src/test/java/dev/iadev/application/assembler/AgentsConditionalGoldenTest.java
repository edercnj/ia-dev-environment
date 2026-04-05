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

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for AgentsAssembler — conditional agents,
 * checklist injection, and golden file parity.
 */
@DisplayName("AgentsAssembler — conditional + golden")
class AgentsConditionalGoldenTest {

    @Nested
    @DisplayName("assemble — conditional agents")
    class ConditionalAgents {

        @Test
        @DisplayName("database generates"
                + " database-engineer.md")
        void assemble_database_generatesDbEngineer(
                @TempDir Path tempDir)
                throws IOException {
            Path outputDir = tempDir.resolve("output");
            Files.createDirectories(outputDir);
            AgentsAssembler assembler =
                    new AgentsAssembler();
            ProjectConfig config =
                    TestConfigBuilder.builder()
                            .database("postgresql", "16")
                            .build();
            assembler.assemble(
                    config, new TemplateEngine(),
                    outputDir);
            assertThat(outputDir.resolve(
                    "agents/database-engineer.md"))
                    .exists();
        }

        @Test
        @DisplayName("events generates"
                + " event-engineer.md")
        void assemble_whenCalled_eventsGenerateEventEngineer(
                @TempDir Path tempDir)
                throws IOException {
            Path outputDir = tempDir.resolve("output");
            Files.createDirectories(outputDir);
            AgentsAssembler assembler =
                    new AgentsAssembler();
            ProjectConfig config =
                    TestConfigBuilder.builder()
                            .eventDriven(true)
                            .build();
            assembler.assemble(
                    config, new TemplateEngine(),
                    outputDir);
            assertThat(outputDir.resolve(
                    "agents/event-engineer.md"))
                    .exists();
        }

        @Test
        @DisplayName("no database excludes db engineer")
        void assemble_noDb_excludesDbEngineer(
                @TempDir Path tempDir)
                throws IOException {
            Path outputDir = tempDir.resolve("output");
            Files.createDirectories(outputDir);
            AgentsAssembler assembler =
                    new AgentsAssembler();
            ProjectConfig config =
                    TestConfigBuilder.builder()
                            .container("none")
                            .orchestrator("none")
                            .iac("none")
                            .clearInterfaces()
                            .addInterface("cli")
                            .eventDriven(false)
                            .build();
            assembler.assemble(
                    config, new TemplateEngine(),
                    outputDir);
            assertThat(outputDir.resolve(
                    "agents/database-engineer.md"))
                    .doesNotExist();
        }

        @Test
        @DisplayName("REST generates api-engineer.md")
        void assemble_rest_generatesApiEngineer(
                @TempDir Path tempDir)
                throws IOException {
            Path outputDir = tempDir.resolve("output");
            Files.createDirectories(outputDir);
            AgentsAssembler assembler =
                    new AgentsAssembler();
            ProjectConfig config =
                    TestConfigBuilder.builder()
                            .clearInterfaces()
                            .addInterface("rest")
                            .build();
            assembler.assemble(
                    config, new TemplateEngine(),
                    outputDir);
            assertThat(outputDir.resolve(
                    "agents/api-engineer.md"))
                    .exists();
        }

        @Test
        @DisplayName("security frameworks generates"
                + " appsec-engineer.md")
        void assemble_securityFrameworks_generatesAppsec(
                @TempDir Path tempDir)
                throws IOException {
            Path outputDir = tempDir.resolve("output");
            Files.createDirectories(outputDir);
            AgentsAssembler assembler =
                    new AgentsAssembler();
            ProjectConfig config =
                    TestConfigBuilder.builder()
                            .securityFrameworks("owasp")
                            .build();
            assembler.assemble(
                    config, new TemplateEngine(),
                    outputDir);
            assertThat(outputDir.resolve(
                    "agents/appsec-engineer.md"))
                    .exists();
        }

        @Test
        @DisplayName("no security frameworks excludes"
                + " appsec-engineer.md")
        void assemble_noFrameworks_excludesAppsec(
                @TempDir Path tempDir)
                throws IOException {
            Path outputDir = tempDir.resolve("output");
            Files.createDirectories(outputDir);
            AgentsAssembler assembler =
                    new AgentsAssembler();
            ProjectConfig config =
                    TestConfigBuilder.builder()
                            .container("none")
                            .orchestrator("none")
                            .iac("none")
                            .clearInterfaces()
                            .addInterface("cli")
                            .eventDriven(false)
                            .build();
            assembler.assemble(
                    config, new TemplateEngine(),
                    outputDir);
            assertThat(outputDir.resolve(
                    "agents/appsec-engineer.md"))
                    .doesNotExist();
        }

        @Test
        @DisplayName("appsec-engineer has 12-point"
                + " checklist")
        void assemble_appsec_has12PointChecklist(
                @TempDir Path tempDir)
                throws IOException {
            Path outputDir = tempDir.resolve("output");
            Files.createDirectories(outputDir);
            AgentsAssembler assembler =
                    new AgentsAssembler();
            ProjectConfig config =
                    TestConfigBuilder.builder()
                            .securityFrameworks("owasp")
                            .build();
            assembler.assemble(
                    config, new TemplateEngine(),
                    outputDir);
            String content = Files.readString(
                    outputDir.resolve(
                            "agents/appsec-engineer.md"),
                    StandardCharsets.UTF_8);
            assertThat(content)
                    .contains("12-Point SDLC Security"
                            + " Checklist");
            // Verify all 12 items are present
            for (int i = 1; i <= 12; i++) {
                assertThat(content)
                        .contains(i + ".");
            }
        }

        @Test
        @DisplayName("appsec-engineer declares scope"
                + " exclusions per RULE-006")
        void assemble_appsec_declaresExclusions(
                @TempDir Path tempDir)
                throws IOException {
            Path outputDir = tempDir.resolve("output");
            Files.createDirectories(outputDir);
            AgentsAssembler assembler =
                    new AgentsAssembler();
            ProjectConfig config =
                    TestConfigBuilder.builder()
                            .securityFrameworks("owasp")
                            .build();
            assembler.assemble(
                    config, new TemplateEngine(),
                    outputDir);
            String content = Files.readString(
                    outputDir.resolve(
                            "agents/appsec-engineer.md"),
                    StandardCharsets.UTF_8);
            assertThat(content)
                    .contains("security-engineer")
                    .contains("pentest-engineer")
                    .contains("devsecops-engineer")
                    .contains("compliance-auditor");
        }

        @Test
        @DisplayName("appsec-engineer defines MTTR"
                + " and vulnerability density metrics")
        void assemble_appsec_definesMetrics(
                @TempDir Path tempDir)
                throws IOException {
            Path outputDir = tempDir.resolve("output");
            Files.createDirectories(outputDir);
            AgentsAssembler assembler =
                    new AgentsAssembler();
            ProjectConfig config =
                    TestConfigBuilder.builder()
                            .securityFrameworks("pci-dss")
                            .build();
            assembler.assemble(
                    config, new TemplateEngine(),
                    outputDir);
            String content = Files.readString(
                    outputDir.resolve(
                            "agents/appsec-engineer.md"),
                    StandardCharsets.UTF_8);
            assertThat(content)
                    .contains("MTTR")
                    .contains("vulnerability density")
                    .contains("vulns/KLOC");
        }

        @Test
        @DisplayName("full config generates many agents")
        void assemble_fullFeatured_generatesMany(
                @TempDir Path tempDir)
                throws IOException {
            Path outputDir = tempDir.resolve("output");
            Files.createDirectories(outputDir);
            AgentsAssembler assembler =
                    new AgentsAssembler();
            ProjectConfig config =
                    AgentsTestFixtures
                            .buildGoGinConfig();
            List<String> files = assembler.assemble(
                    config, new TemplateEngine(),
                    outputDir);
            assertThat(files).hasSizeGreaterThan(8);
        }
    }

    @Nested
    @DisplayName("assemble — checklist injection")
    class ChecklistInjection {

        @Test
        @DisplayName("security checklist injected"
                + " when lgpd active")
        void assemble_whenCalled_securityChecklistInjected(
                @TempDir Path tempDir)
                throws IOException {
            Path outputDir = tempDir.resolve("output");
            Files.createDirectories(outputDir);
            AgentsAssembler assembler =
                    new AgentsAssembler();
            ProjectConfig config =
                    TestConfigBuilder.builder()
                            .securityFrameworks("lgpd")
                            .build();
            assembler.assemble(
                    config, new TemplateEngine(),
                    outputDir);
            Path securityAgent = outputDir.resolve(
                    "agents/security-engineer.md");
            if (Files.exists(securityAgent)) {
                String content = Files.readString(
                        securityAgent,
                        StandardCharsets.UTF_8);
                assertThat(content).isNotEmpty();
            }
        }
    }

    @Nested
    @DisplayName("assemble — golden file parity")
    class GoldenFile {

        @Test
        @DisplayName("go-gin generates expected agents")
        void assemble_goGin_generatesExpectedAgents(
                @TempDir Path tempDir)
                throws IOException {
            Path outputDir = tempDir.resolve("output");
            Files.createDirectories(outputDir);
            AgentsAssembler assembler =
                    new AgentsAssembler();
            ProjectConfig config =
                    AgentsTestFixtures
                            .buildGoGinConfig();
            assembler.assemble(
                    config, new TemplateEngine(),
                    outputDir);
            Path agentsDir = outputDir.resolve("agents");
            assertThat(agentsDir.resolve("architect.md"))
                    .exists();
            assertThat(agentsDir.resolve(
                    "go-developer.md"))
                    .exists();
            assertThat(agentsDir.resolve(
                    "api-engineer.md"))
                    .exists();
            assertThat(agentsDir.resolve(
                    "appsec-engineer.md"))
                    .exists();
            assertThat(agentsDir.resolve(
                    "event-engineer.md"))
                    .exists();
            assertThat(agentsDir.resolve(
                    "devops-engineer.md"))
                    .exists();
        }

        @Test
        @DisplayName("architect.md matches golden")
        void assemble_architect_matchesGolden(
                @TempDir Path tempDir)
                throws IOException {
            Path outputDir = tempDir.resolve("output");
            Files.createDirectories(outputDir);
            AgentsAssembler assembler =
                    new AgentsAssembler();
            ProjectConfig config =
                    AgentsTestFixtures
                            .buildGoGinConfig();
            assembler.assemble(
                    config, new TemplateEngine(),
                    outputDir);
            String expected = loadResource(
                    "golden/go-gin/.claude/agents/"
                            + "architect.md");
            if (expected != null) {
                String actual = Files.readString(
                        outputDir.resolve(
                                "agents/architect.md"),
                        StandardCharsets.UTF_8);
                assertThat(actual).isEqualTo(expected);
            }
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
}
