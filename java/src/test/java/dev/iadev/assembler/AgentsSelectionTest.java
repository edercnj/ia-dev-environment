package dev.iadev.assembler;

import dev.iadev.model.ProjectConfig;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for AgentsSelection — pure selection logic that
 * evaluates feature gates and returns agent filenames for
 * conditional agents, developer agents, and checklist rules.
 */
@DisplayName("AgentsSelection")
class AgentsSelectionTest {

    @Nested
    @DisplayName("selectConditionalAgents")
    class SelectConditionalAgents {

        @Test
        @DisplayName("config with database includes"
                + " database-engineer.md")
        void databaseIncludesDbEngineer() {
            ProjectConfig config = TestConfigBuilder.builder()
                    .database("postgresql", "16")
                    .build();

            List<String> agents =
                    AgentsSelection
                            .selectConditionalAgents(config);

            assertThat(agents)
                    .contains("database-engineer.md");
        }

        @Test
        @DisplayName("config without database excludes"
                + " database-engineer.md")
        void noDatabaseExcludesDbEngineer() {
            ProjectConfig config = TestConfigBuilder.builder()
                    .build();

            List<String> agents =
                    AgentsSelection
                            .selectConditionalAgents(config);

            assertThat(agents)
                    .doesNotContain("database-engineer.md");
        }

        @Test
        @DisplayName("config with observability includes"
                + " observability-engineer.md")
        void observabilityIncludesObsEngineer() {
            ProjectConfig config = TestConfigBuilder.builder()
                    .observabilityTool("prometheus")
                    .build();

            List<String> agents =
                    AgentsSelection
                            .selectConditionalAgents(config);

            assertThat(agents)
                    .contains("observability-engineer.md");
        }

        @Test
        @DisplayName("config without observability excludes"
                + " observability-engineer.md")
        void noObservabilityExcludesObsEngineer() {
            ProjectConfig config = TestConfigBuilder.builder()
                    .build();

            List<String> agents =
                    AgentsSelection
                            .selectConditionalAgents(config);

            assertThat(agents)
                    .doesNotContain(
                            "observability-engineer.md");
        }

        @Test
        @DisplayName("config with container includes"
                + " devops-engineer.md")
        void containerIncludesDevops() {
            ProjectConfig config = TestConfigBuilder.builder()
                    .container("docker")
                    .orchestrator("none")
                    .iac("none")
                    .build();

            List<String> agents =
                    AgentsSelection
                            .selectConditionalAgents(config);

            assertThat(agents)
                    .contains("devops-engineer.md");
        }

        @Test
        @DisplayName("config with orchestrator includes"
                + " devops-engineer.md")
        void orchestratorIncludesDevops() {
            ProjectConfig config = TestConfigBuilder.builder()
                    .container("none")
                    .orchestrator("kubernetes")
                    .build();

            List<String> agents =
                    AgentsSelection
                            .selectConditionalAgents(config);

            assertThat(agents)
                    .contains("devops-engineer.md");
        }

        @Test
        @DisplayName("config with iac includes"
                + " devops-engineer.md")
        void iacIncludesDevops() {
            ProjectConfig config = TestConfigBuilder.builder()
                    .container("none")
                    .orchestrator("none")
                    .iac("terraform")
                    .build();

            List<String> agents =
                    AgentsSelection
                            .selectConditionalAgents(config);

            assertThat(agents)
                    .contains("devops-engineer.md");
        }

        @Test
        @DisplayName("config with no infra excludes"
                + " devops-engineer.md")
        void noInfraExcludesDevops() {
            ProjectConfig config = TestConfigBuilder.builder()
                    .container("none")
                    .orchestrator("none")
                    .iac("none")
                    .build();

            List<String> agents =
                    AgentsSelection
                            .selectConditionalAgents(config);

            assertThat(agents)
                    .doesNotContain("devops-engineer.md");
        }

        @Test
        @DisplayName("config with REST includes"
                + " api-engineer.md")
        void restIncludesApiEngineer() {
            ProjectConfig config = TestConfigBuilder.builder()
                    .clearInterfaces()
                    .addInterface("rest")
                    .build();

            List<String> agents =
                    AgentsSelection
                            .selectConditionalAgents(config);

            assertThat(agents)
                    .contains("api-engineer.md");
        }

        @Test
        @DisplayName("config with gRPC includes"
                + " api-engineer.md")
        void grpcIncludesApiEngineer() {
            ProjectConfig config = TestConfigBuilder.builder()
                    .clearInterfaces()
                    .addInterface("grpc")
                    .build();

            List<String> agents =
                    AgentsSelection
                            .selectConditionalAgents(config);

            assertThat(agents)
                    .contains("api-engineer.md");
        }

        @Test
        @DisplayName("config with GraphQL includes"
                + " api-engineer.md")
        void graphqlIncludesApiEngineer() {
            ProjectConfig config = TestConfigBuilder.builder()
                    .clearInterfaces()
                    .addInterface("graphql")
                    .build();

            List<String> agents =
                    AgentsSelection
                            .selectConditionalAgents(config);

            assertThat(agents)
                    .contains("api-engineer.md");
        }

        @Test
        @DisplayName("config without REST/gRPC/GraphQL"
                + " excludes api-engineer.md")
        void noApiExcludesApiEngineer() {
            ProjectConfig config = TestConfigBuilder.builder()
                    .clearInterfaces()
                    .addInterface("cli")
                    .build();

            List<String> agents =
                    AgentsSelection
                            .selectConditionalAgents(config);

            assertThat(agents)
                    .doesNotContain("api-engineer.md");
        }

        @Test
        @DisplayName("config with eventDriven includes"
                + " event-engineer.md")
        void eventDrivenIncludesEventEngineer() {
            ProjectConfig config = TestConfigBuilder.builder()
                    .eventDriven(true)
                    .clearInterfaces()
                    .addInterface("cli")
                    .build();

            List<String> agents =
                    AgentsSelection
                            .selectConditionalAgents(config);

            assertThat(agents)
                    .contains("event-engineer.md");
        }

        @Test
        @DisplayName("config with event-consumer includes"
                + " event-engineer.md")
        void eventConsumerIncludesEventEngineer() {
            ProjectConfig config = TestConfigBuilder.builder()
                    .eventDriven(false)
                    .clearInterfaces()
                    .addInterface("event-consumer")
                    .build();

            List<String> agents =
                    AgentsSelection
                            .selectConditionalAgents(config);

            assertThat(agents)
                    .contains("event-engineer.md");
        }

        @Test
        @DisplayName("config with event-producer includes"
                + " event-engineer.md")
        void eventProducerIncludesEventEngineer() {
            ProjectConfig config = TestConfigBuilder.builder()
                    .eventDriven(false)
                    .clearInterfaces()
                    .addInterface("event-producer")
                    .build();

            List<String> agents =
                    AgentsSelection
                            .selectConditionalAgents(config);

            assertThat(agents)
                    .contains("event-engineer.md");
        }

        @Test
        @DisplayName("config without events excludes"
                + " event-engineer.md")
        void noEventsExcludesEventEngineer() {
            ProjectConfig config = TestConfigBuilder.builder()
                    .eventDriven(false)
                    .clearInterfaces()
                    .addInterface("cli")
                    .build();

            List<String> agents =
                    AgentsSelection
                            .selectConditionalAgents(config);

            assertThat(agents)
                    .doesNotContain("event-engineer.md");
        }

        @Test
        @DisplayName("aggregates all conditional agents"
                + " for full-featured config")
        void aggregatesAllConditionals() {
            ProjectConfig config = TestConfigBuilder.builder()
                    .database("postgresql", "16")
                    .observabilityTool("prometheus")
                    .container("docker")
                    .orchestrator("kubernetes")
                    .eventDriven(true)
                    .clearInterfaces()
                    .addInterface("rest")
                    .addInterface("event-consumer")
                    .build();

            List<String> agents =
                    AgentsSelection
                            .selectConditionalAgents(config);

            assertThat(agents)
                    .contains("database-engineer.md")
                    .contains("observability-engineer.md")
                    .contains("devops-engineer.md")
                    .contains("api-engineer.md")
                    .contains("event-engineer.md");
        }
    }

    @Nested
    @DisplayName("selectDeveloperAgent")
    class SelectDeveloperAgent {

        @Test
        @DisplayName("language=java returns"
                + " java-developer.md")
        void javaReturnsJavaDeveloper() {
            ProjectConfig config = TestConfigBuilder.builder()
                    .language("java", "21")
                    .build();

            String agent =
                    AgentsSelection
                            .selectDeveloperAgent(config);

            assertThat(agent)
                    .isEqualTo("java-developer.md");
        }

        @Test
        @DisplayName("language=typescript returns"
                + " typescript-developer.md")
        void tsReturnsTsDeveloper() {
            ProjectConfig config = TestConfigBuilder.builder()
                    .language("typescript", "5")
                    .build();

            String agent =
                    AgentsSelection
                            .selectDeveloperAgent(config);

            assertThat(agent)
                    .isEqualTo("typescript-developer.md");
        }

        @Test
        @DisplayName("language=go returns"
                + " go-developer.md")
        void goReturnsGoDeveloper() {
            ProjectConfig config = TestConfigBuilder.builder()
                    .language("go", "1.22")
                    .build();

            String agent =
                    AgentsSelection
                            .selectDeveloperAgent(config);

            assertThat(agent)
                    .isEqualTo("go-developer.md");
        }

        @Test
        @DisplayName("language=python returns"
                + " python-developer.md")
        void pythonReturnsPythonDeveloper() {
            ProjectConfig config = TestConfigBuilder.builder()
                    .language("python", "3.12")
                    .build();

            String agent =
                    AgentsSelection
                            .selectDeveloperAgent(config);

            assertThat(agent)
                    .isEqualTo("python-developer.md");
        }

        @Test
        @DisplayName("language=kotlin returns"
                + " kotlin-developer.md")
        void kotlinReturnsKotlinDeveloper() {
            ProjectConfig config = TestConfigBuilder.builder()
                    .language("kotlin", "2.0")
                    .build();

            String agent =
                    AgentsSelection
                            .selectDeveloperAgent(config);

            assertThat(agent)
                    .isEqualTo("kotlin-developer.md");
        }

        @Test
        @DisplayName("language=rust returns"
                + " rust-developer.md")
        void rustReturnsRustDeveloper() {
            ProjectConfig config = TestConfigBuilder.builder()
                    .language("rust", "1.77")
                    .build();

            String agent =
                    AgentsSelection
                            .selectDeveloperAgent(config);

            assertThat(agent)
                    .isEqualTo("rust-developer.md");
        }

        @Test
        @DisplayName("language=csharp returns"
                + " csharp-developer.md")
        void csharpReturnsCsharpDeveloper() {
            ProjectConfig config = TestConfigBuilder.builder()
                    .language("csharp", "12")
                    .build();

            String agent =
                    AgentsSelection
                            .selectDeveloperAgent(config);

            assertThat(agent)
                    .isEqualTo("csharp-developer.md");
        }
    }

    @Nested
    @DisplayName("buildChecklistRules")
    class BuildChecklistRules {

        @Test
        @DisplayName("pci-dss security framework"
                + " activates pci-dss-security checklist")
        void pciDssActivatesChecklist() {
            ProjectConfig config = TestConfigBuilder.builder()
                    .securityFrameworks("pci-dss")
                    .build();

            List<AgentsSelection.ChecklistRule> rules =
                    AgentsSelection.buildChecklistRules(
                            config);

            assertThat(rules)
                    .anyMatch(r ->
                            "pci-dss-security.md"
                                    .equals(r.checklist())
                                    && r.active());
        }

        @Test
        @DisplayName("lgpd security framework"
                + " activates privacy-security checklist")
        void lgpdActivatesPrivacyChecklist() {
            ProjectConfig config = TestConfigBuilder.builder()
                    .securityFrameworks("lgpd")
                    .build();

            List<AgentsSelection.ChecklistRule> rules =
                    AgentsSelection.buildChecklistRules(
                            config);

            assertThat(rules)
                    .anyMatch(r ->
                            "privacy-security.md"
                                    .equals(r.checklist())
                                    && r.active());
        }

        @Test
        @DisplayName("gdpr security framework"
                + " activates privacy-security checklist")
        void gdprActivatesPrivacyChecklist() {
            ProjectConfig config = TestConfigBuilder.builder()
                    .securityFrameworks("gdpr")
                    .build();

            List<AgentsSelection.ChecklistRule> rules =
                    AgentsSelection.buildChecklistRules(
                            config);

            assertThat(rules)
                    .anyMatch(r ->
                            "privacy-security.md"
                                    .equals(r.checklist())
                                    && r.active());
        }

        @Test
        @DisplayName("grpc interface activates"
                + " grpc-api checklist")
        void grpcActivatesGrpcChecklist() {
            ProjectConfig config = TestConfigBuilder.builder()
                    .clearInterfaces()
                    .addInterface("grpc")
                    .build();

            List<AgentsSelection.ChecklistRule> rules =
                    AgentsSelection.buildChecklistRules(
                            config);

            assertThat(rules)
                    .anyMatch(r ->
                            "grpc-api.md"
                                    .equals(r.checklist())
                                    && r.active());
        }

        @Test
        @DisplayName("helm templating activates"
                + " helm-devops checklist")
        void helmActivatesHelmChecklist() {
            ProjectConfig config = TestConfigBuilder.builder()
                    .build();

            // Default templating is kustomize, not helm
            List<AgentsSelection.ChecklistRule> rules =
                    AgentsSelection.buildChecklistRules(
                            config);

            assertThat(rules)
                    .anyMatch(r ->
                            "helm-devops.md"
                                    .equals(r.checklist())
                                    && !r.active());
        }

        @Test
        @DisplayName("no security frameworks means"
                + " all security checklists inactive")
        void noFrameworksMeansInactive() {
            ProjectConfig config = TestConfigBuilder.builder()
                    .build();

            List<AgentsSelection.ChecklistRule> rules =
                    AgentsSelection.buildChecklistRules(
                            config);

            List<AgentsSelection.ChecklistRule> secRules =
                    rules.stream()
                            .filter(r -> r.agent()
                                    .equals("security-engineer.md"))
                            .toList();

            assertThat(secRules)
                    .allMatch(r -> !r.active());
        }
    }

    @Nested
    @DisplayName("checklistMarker")
    class ChecklistMarkerTests {

        @Test
        @DisplayName("derives marker from checklist"
                + " filename")
        void derivesMarkerFromFilename() {
            String marker = AgentsSelection.checklistMarker(
                    "pci-dss-security.md");

            assertThat(marker)
                    .isEqualTo(
                            "<!-- PCI_DSS_SECURITY -->");
        }

        @Test
        @DisplayName("handles single-word checklist"
                + " filename")
        void handlesSingleWord() {
            String marker = AgentsSelection.checklistMarker(
                    "simple.md");

            assertThat(marker)
                    .isEqualTo("<!-- SIMPLE -->");
        }
    }
}
