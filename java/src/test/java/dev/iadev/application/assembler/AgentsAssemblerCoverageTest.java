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

/**
 * Additional coverage tests for AgentsAssembler —
 * targeting uncovered branches and edge cases.
 */
@DisplayName("AgentsAssembler — coverage")
class AgentsAssemblerCoverageTest {

    @Nested
    @DisplayName("selectCoreAgents — edge cases")
    class SelectCoreAgents {

        @Test
        @DisplayName("core dir missing returns empty")
        void selectCoreAgents_whenCalled_coreDirMissing(@TempDir Path tempDir) {
            AgentsAssembler assembler =
                    new AgentsAssembler(tempDir);

            List<String> agents =
                    assembler.selectCoreAgents();

            assertThat(agents).isEmpty();
        }

        @Test
        @DisplayName("core dir is file returns empty")
        void selectCoreAgents_coreDir_isFile(@TempDir Path tempDir)
                throws IOException {
            Files.createDirectories(
                    tempDir.resolve("agents-templates"));
            Files.writeString(
                    tempDir.resolve(
                            "agents-templates/core"),
                    "not a dir");

            AgentsAssembler assembler =
                    new AgentsAssembler(tempDir);

            List<String> agents =
                    assembler.selectCoreAgents();

            assertThat(agents).isEmpty();
        }

        @Test
        @DisplayName("core dir with non-md files"
                + " filters them out")
        void selectCoreAgents_nonMdFilesFiltered_succeeds(@TempDir Path tempDir)
                throws IOException {
            Path core = tempDir.resolve(
                    "agents-templates/core");
            Files.createDirectories(core);
            Files.writeString(
                    core.resolve("architect.md"),
                    "Agent content");
            Files.writeString(
                    core.resolve("readme.txt"),
                    "Not an agent");

            AgentsAssembler assembler =
                    new AgentsAssembler(tempDir);

            List<String> agents =
                    assembler.selectCoreAgents();

            assertThat(agents)
                    .containsExactly("architect.md");
        }
    }

    @Nested
    @DisplayName("assembleConditional — edge cases")
    class ConditionalEdgeCases {

        @Test
        @DisplayName("conditional agent source missing"
                + " returns null (filtered out)")
        void assembleConditional_whenCalled_conditionalMissing(@TempDir Path tempDir)
                throws IOException {
            Path core = tempDir.resolve(
                    "agents-templates/core");
            Files.createDirectories(core);

            Path outputDir = tempDir.resolve("output");
            Files.createDirectories(outputDir);

            AgentsAssembler assembler =
                    new AgentsAssembler(tempDir);
            ProjectConfig config =
                    TestConfigBuilder.builder()
                            .database("postgresql", "16")
                            .build();

            List<String> files = assembler.assemble(
                    config, new TemplateEngine(), outputDir);

            assertThat(files).isEmpty();
        }
    }

    @Nested
    @DisplayName("copyDeveloperAgent — edge cases")
    class DeveloperAgent {

        @Test
        @DisplayName("developer agent source missing"
                + " returns null (filtered out)")
        void copyDeveloperAgent_whenCalled_developerMissing(@TempDir Path tempDir)
                throws IOException {
            Path core = tempDir.resolve(
                    "agents-templates/core");
            Files.createDirectories(core);

            Path outputDir = tempDir.resolve("output");
            Files.createDirectories(outputDir);

            AgentsAssembler assembler =
                    new AgentsAssembler(tempDir);
            ProjectConfig config =
                    TestConfigBuilder.minimal();

            List<String> files = assembler.assemble(
                    config, new TemplateEngine(), outputDir);

            assertThat(files).isEmpty();
        }
    }

    @Nested
    @DisplayName("injectChecklists — edge cases")
    class InjectChecklists {

        @Test
        @DisplayName("checklist injection when agent"
                + " file missing is skipped")
        void injectChecklists_whenCalled_agentFileMissing(@TempDir Path tempDir)
                throws IOException {
            Path core = tempDir.resolve(
                    "agents-templates/core");
            Files.createDirectories(core);

            Path outputDir = tempDir.resolve("output");
            Files.createDirectories(outputDir);

            AgentsAssembler assembler =
                    new AgentsAssembler(tempDir);
            ProjectConfig config =
                    TestConfigBuilder.builder()
                            .database("postgresql", "16")
                            .build();

            List<String> files = assembler.assemble(
                    config, new TemplateEngine(), outputDir);

            assertThat(files).isEmpty();
        }

        @Test
        @DisplayName("checklist source file missing"
                + " is skipped")
        void injectChecklists_whenCalled_checklistSourceMissing(@TempDir Path tempDir)
                throws IOException {
            Path core = tempDir.resolve(
                    "agents-templates/core");
            Files.createDirectories(core);
            Files.writeString(
                    core.resolve("tech-lead.md"),
                    "Tech lead content\n"
                            + "{{CHECKLIST_DATABASE}}");

            Path outputDir = tempDir.resolve("output");
            Files.createDirectories(outputDir);

            AgentsAssembler assembler =
                    new AgentsAssembler(tempDir);
            ProjectConfig config =
                    TestConfigBuilder.builder()
                            .database("postgresql", "16")
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
        @DisplayName("context has all 42 entries")
        void buildContext_withAllFields_returnsExpectedEntries() {
            ProjectConfig config = TestConfigBuilder
                    .builder()
                    .projectName("agent-test")
                    .purpose("Testing agents")
                    .language("go", "1.22")
                    .framework("gin", "")
                    .buildTool("go-mod")
                    .archStyle("hexagonal")
                    .domainDriven(true)
                    .eventDriven(false)
                    .container("docker")
                    .orchestrator("kubernetes")
                    .database("mongodb", "7")
                    .cache("redis", "7.4")
                    .build();

            Map<String, Object> context =
                    ContextBuilder.buildContext(config);

            assertThat(context).hasSize(42);
            assertThat(context)
                    .containsEntry("project_name",
                            "agent-test")
                    .containsEntry("language_name", "go")
                    .containsEntry("framework_name", "gin")
                    .containsEntry("domain_driven", "True")
                    .containsEntry("event_driven", "False")
                    .containsEntry("database_name",
                            "mongodb")
                    .containsEntry("cache_name", "redis")
                    .containsEntry("message_broker",
                            "none");
        }
    }

    @Nested
    @DisplayName("default constructor")
    class DefaultConstructor {

        @Test
        @DisplayName("default constructor resolves")
        void constructor_withDefaults_resolvesCorrectly() {
            AgentsAssembler assembler =
                    new AgentsAssembler();

            assertThat(assembler)
                    .isInstanceOf(Assembler.class);
        }
    }
}
