package dev.iadev.application.assembler;

import dev.iadev.testutil.TestConfigBuilder;

import dev.iadev.domain.model.ProjectConfig;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for GithubAgentsAssembler —
 * selectGithubConditionalAgents and interface contract.
 */
@DisplayName("GithubAgentsAssembler — conditional")
class GithubAgentsConditionalTest {

    @Nested
    @DisplayName("implements Assembler interface")
    class ImplementsAssembler {

        @Test
        @DisplayName("is instance of Assembler")
        void instanceOf_whenCreated_implementsAssemblerInterface() {
            GithubAgentsAssembler assembler =
                    new GithubAgentsAssembler();

            assertThat(assembler)
                    .isInstanceOf(Assembler.class);
        }
    }

    @Nested
    @DisplayName("selectGithubConditionalAgents")
    class SelectConditionalAgents {

        @Test
        @DisplayName("devops when docker")
        void assemble_whenDocker_devops() {
            ProjectConfig config =
                    TestConfigBuilder.builder()
                            .container("docker")
                            .orchestrator("none")
                            .iac("none")
                            .build();

            List<String> agents =
                    GithubAgentsAssembler
                            .selectGithubConditionalAgents(
                                    config);

            assertThat(agents)
                    .contains("devops-engineer.md");
        }

        @Test
        @DisplayName("devops when kubernetes")
        void assemble_whenKubernetes_devops() {
            ProjectConfig config =
                    TestConfigBuilder.builder()
                            .container("none")
                            .orchestrator("kubernetes")
                            .iac("none")
                            .build();

            List<String> agents =
                    GithubAgentsAssembler
                            .selectGithubConditionalAgents(
                                    config);

            assertThat(agents)
                    .contains("devops-engineer.md");
        }

        @Test
        @DisplayName("devops when iac")
        void assemble_whenIac_devops() {
            ProjectConfig config =
                    TestConfigBuilder.builder()
                            .container("none")
                            .orchestrator("none")
                            .iac("terraform")
                            .build();

            List<String> agents =
                    GithubAgentsAssembler
                            .selectGithubConditionalAgents(
                                    config);

            assertThat(agents)
                    .contains("devops-engineer.md");
        }

        @Test
        @DisplayName("devops when service mesh")
        void assemble_whenServiceMesh_devops() {
            ProjectConfig config =
                    TestConfigBuilder.builder()
                            .container("none")
                            .orchestrator("none")
                            .iac("none")
                            .serviceMesh("istio")
                            .build();

            List<String> agents =
                    GithubAgentsAssembler
                            .selectGithubConditionalAgents(
                                    config);

            assertThat(agents)
                    .contains("devops-engineer.md");
        }

        @Test
        @DisplayName("devsecops when docker")
        void assemble_whenDocker_devsecops() {
            ProjectConfig config =
                    TestConfigBuilder.builder()
                            .container("docker")
                            .orchestrator("none")
                            .iac("none")
                            .build();

            List<String> agents =
                    GithubAgentsAssembler
                            .selectGithubConditionalAgents(
                                    config);

            assertThat(agents)
                    .contains(
                            "devsecops-engineer.md");
        }

        @Test
        @DisplayName("devsecops when kubernetes")
        void assemble_whenKubernetes_devsecops() {
            ProjectConfig config =
                    TestConfigBuilder.builder()
                            .container("none")
                            .orchestrator("kubernetes")
                            .iac("none")
                            .build();

            List<String> agents =
                    GithubAgentsAssembler
                            .selectGithubConditionalAgents(
                                    config);

            assertThat(agents)
                    .contains(
                            "devsecops-engineer.md");
        }

        @Test
        @DisplayName("no devsecops when only iac")
        void assemble_whenOnlyIac_noDevsecops() {
            ProjectConfig config =
                    TestConfigBuilder.builder()
                            .container("none")
                            .orchestrator("none")
                            .iac("terraform")
                            .build();

            List<String> agents =
                    GithubAgentsAssembler
                            .selectGithubConditionalAgents(
                                    config);

            assertThat(agents)
                    .doesNotContain(
                            "devsecops-engineer.md");
        }

        @Test
        @DisplayName("no devsecops when all none")
        void assemble_whenAllNone_noDevsecops() {
            ProjectConfig config =
                    TestConfigBuilder.builder()
                            .container("none")
                            .orchestrator("none")
                            .iac("none")
                            .clearInterfaces()
                            .build();

            List<String> agents =
                    GithubAgentsAssembler
                            .selectGithubConditionalAgents(
                                    config);

            assertThat(agents)
                    .doesNotContain(
                            "devsecops-engineer.md");
        }

        @Test
        @DisplayName("no devops when all none")
        void assemble_whenAllNone_noDevops() {
            ProjectConfig config =
                    TestConfigBuilder.builder()
                            .container("none")
                            .orchestrator("none")
                            .iac("none")
                            .clearInterfaces()
                            .build();

            List<String> agents =
                    GithubAgentsAssembler
                            .selectGithubConditionalAgents(
                                    config);

            assertThat(agents)
                    .doesNotContain(
                            "devops-engineer.md");
        }

        @Test
        @DisplayName("api-engineer when REST")
        void assemble_whenRest_apiEngineer() {
            ProjectConfig config =
                    TestConfigBuilder.builder()
                            .container("none")
                            .orchestrator("none")
                            .iac("none")
                            .clearInterfaces()
                            .addInterface("rest")
                            .build();

            List<String> agents =
                    GithubAgentsAssembler
                            .selectGithubConditionalAgents(
                                    config);

            assertThat(agents)
                    .contains("api-engineer.md");
        }

        @Test
        @DisplayName("api-engineer when gRPC")
        void assemble_whenGrpc_apiEngineer() {
            ProjectConfig config =
                    TestConfigBuilder.builder()
                            .container("none")
                            .orchestrator("none")
                            .iac("none")
                            .clearInterfaces()
                            .addInterface("grpc")
                            .build();

            List<String> agents =
                    GithubAgentsAssembler
                            .selectGithubConditionalAgents(
                                    config);

            assertThat(agents)
                    .contains("api-engineer.md");
        }

        @Test
        @DisplayName("api-engineer when GraphQL")
        void assemble_whenGraphql_apiEngineer() {
            ProjectConfig config =
                    TestConfigBuilder.builder()
                            .container("none")
                            .orchestrator("none")
                            .iac("none")
                            .clearInterfaces()
                            .addInterface("graphql")
                            .build();

            List<String> agents =
                    GithubAgentsAssembler
                            .selectGithubConditionalAgents(
                                    config);

            assertThat(agents)
                    .contains("api-engineer.md");
        }

        @Test
        @DisplayName("no api-engineer without API iface")
        void assemble_whenNoApiInterface_noApiEngineer() {
            ProjectConfig config =
                    TestConfigBuilder.builder()
                            .container("none")
                            .orchestrator("none")
                            .iac("none")
                            .clearInterfaces()
                            .addInterface("cli")
                            .build();

            List<String> agents =
                    GithubAgentsAssembler
                            .selectGithubConditionalAgents(
                                    config);

            assertThat(agents)
                    .doesNotContain("api-engineer.md");
        }

        @Test
        @DisplayName("appsec-engineer when security"
                + " frameworks non-empty")
        void assemble_whenSecurityFrameworks_appsec() {
            ProjectConfig config =
                    TestConfigBuilder.builder()
                            .container("none")
                            .orchestrator("none")
                            .iac("none")
                            .securityFrameworks("owasp")
                            .clearInterfaces()
                            .addInterface("cli")
                            .build();

            List<String> agents =
                    GithubAgentsAssembler
                            .selectGithubConditionalAgents(
                                    config);

            assertThat(agents)
                    .contains("appsec-engineer.md");
        }

        @Test
        @DisplayName("no appsec-engineer without"
                + " security frameworks")
        void assemble_whenNoFrameworks_noAppsec() {
            ProjectConfig config =
                    TestConfigBuilder.builder()
                            .container("none")
                            .orchestrator("none")
                            .iac("none")
                            .clearInterfaces()
                            .addInterface("cli")
                            .build();

            List<String> agents =
                    GithubAgentsAssembler
                            .selectGithubConditionalAgents(
                                    config);

            assertThat(agents)
                    .doesNotContain(
                            "appsec-engineer.md");
        }
    }
}
