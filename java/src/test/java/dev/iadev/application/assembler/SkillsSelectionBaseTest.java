package dev.iadev.application.assembler;

import dev.iadev.testutil.TestConfigBuilder;

import dev.iadev.domain.model.ProjectConfig;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for SkillsSelection — core skill selection:
 * interface, infra, testing, and security skills.
 */
@DisplayName("SkillsSelection — base")
class SkillsSelectionBaseTest {

    @Nested
    @DisplayName("selectInterfaceSkills")
    class SelectInterfaceSkills {

        @Test
        @DisplayName("config with REST includes x-review-api")
        void select_rest_includesApiReview() {
            ProjectConfig config = TestConfigBuilder.builder()
                    .clearInterfaces()
                    .addInterface("rest")
                    .build();

            List<String> skills =
                    SkillsSelection.selectInterfaceSkills(
                            config);

            assertThat(skills).contains("x-review-api");
        }

        @Test
        @DisplayName("config with gRPC includes"
                + " x-review-grpc")
        void select_grpc_includesGrpcReview() {
            ProjectConfig config = TestConfigBuilder.builder()
                    .clearInterfaces()
                    .addInterface("grpc")
                    .build();

            List<String> skills =
                    SkillsSelection.selectInterfaceSkills(
                            config);

            assertThat(skills).contains("x-review-grpc");
        }

        @Test
        @DisplayName("config with GraphQL includes"
                + " x-review-graphql")
        void select_graphql_includesGraphqlReview() {
            ProjectConfig config = TestConfigBuilder.builder()
                    .clearInterfaces()
                    .addInterface("graphql")
                    .build();

            List<String> skills =
                    SkillsSelection.selectInterfaceSkills(
                            config);

            assertThat(skills).contains("x-review-graphql");
        }

        @Test
        @DisplayName("config with event-consumer includes"
                + " x-review-events")
        void select_eventConsumer_includesEventsReview() {
            ProjectConfig config = TestConfigBuilder.builder()
                    .clearInterfaces()
                    .addInterface("event-consumer")
                    .build();

            List<String> skills =
                    SkillsSelection.selectInterfaceSkills(
                            config);

            assertThat(skills).contains("x-review-events");
        }

        @Test
        @DisplayName("config with event-producer includes"
                + " x-review-events")
        void select_eventProducer_includesEventsReview() {
            ProjectConfig config = TestConfigBuilder.builder()
                    .clearInterfaces()
                    .addInterface("event-producer")
                    .build();

            List<String> skills =
                    SkillsSelection.selectInterfaceSkills(
                            config);

            assertThat(skills).contains("x-review-events");
        }

        @Test
        @DisplayName("config without interfaces returns"
                + " empty list")
        void select_noInterfaces_returnsEmpty() {
            ProjectConfig config = TestConfigBuilder.builder()
                    .clearInterfaces()
                    .addInterface("cli")
                    .build();

            List<String> skills =
                    SkillsSelection.selectInterfaceSkills(
                            config);

            assertThat(skills).isEmpty();
        }
    }

    @Nested
    @DisplayName("selectInfraSkills")
    class SelectInfraSkills {

        @Test
        @DisplayName("observability tool not none includes"
                + " instrument-otel")
        void select_observability_includesOtel() {
            ProjectConfig config = TestConfigBuilder.builder()
                    .observabilityTool("prometheus")
                    .build();

            List<String> skills =
                    SkillsSelection.selectInfraSkills(config);

            assertThat(skills)
                    .contains("instrument-otel");
        }

        @Test
        @DisplayName("orchestrator not none includes"
                + " setup-environment")
        void select_orchestrator_includesSetup() {
            ProjectConfig config = TestConfigBuilder.builder()
                    .orchestrator("kubernetes")
                    .build();

            List<String> skills =
                    SkillsSelection.selectInfraSkills(config);

            assertThat(skills)
                    .contains("setup-environment");
        }

        @Test
        @DisplayName("api gateway not none includes"
                + " x-review-gateway")
        void select_apiGateway_includesGatewayReview() {
            ProjectConfig config = TestConfigBuilder.builder()
                    .apiGateway("kong")
                    .build();

            List<String> skills =
                    SkillsSelection.selectInfraSkills(config);

            assertThat(skills)
                    .contains("x-review-gateway");
        }

        @Test
        @DisplayName("all infra none returns empty list")
        void select_allNone_returnsEmpty() {
            ProjectConfig config = TestConfigBuilder.builder()
                    .build();

            List<String> skills =
                    SkillsSelection.selectInfraSkills(config);

            assertThat(skills).isEmpty();
        }
    }

    @Nested
    @DisplayName("selectPentestSkills")
    class SelectPentestSkills {

        @Test
        @DisplayName("pentestReadiness true includes"
                + " x-pentest")
        void select_pentestTrue_includesPentest() {
            ProjectConfig config =
                    TestConfigBuilder.builder()
                            .pentestReadiness(true)
                            .build();

            List<String> skills =
                    SkillsSelection.selectPentestSkills(
                            config);

            assertThat(skills)
                    .containsExactly("x-pentest");
        }

        @Test
        @DisplayName("pentestReadiness false excludes"
                + " x-pentest")
        void select_pentestFalse_returnsEmpty() {
            ProjectConfig config =
                    TestConfigBuilder.builder()
                            .pentestReadiness(false)
                            .build();

            List<String> skills =
                    SkillsSelection.selectPentestSkills(
                            config);

            assertThat(skills).isEmpty();
        }

        @Test
        @DisplayName("default config excludes x-pentest")
        void select_defaultConfig_returnsEmpty() {
            ProjectConfig config =
                    TestConfigBuilder.minimal();

            List<String> skills =
                    SkillsSelection.selectPentestSkills(
                            config);

            assertThat(skills).isEmpty();
        }

        @Test
        @DisplayName("pentestReadiness true appears in"
                + " selectConditionalSkills")
        void select_pentestTrue_inConditional() {
            ProjectConfig config =
                    TestConfigBuilder.builder()
                            .pentestReadiness(true)
                            .build();

            List<String> skills =
                    SkillsSelection
                            .selectConditionalSkills(config);

            assertThat(skills).contains("x-pentest");
        }

        @Test
        @DisplayName("pentestReadiness false not in"
                + " selectConditionalSkills")
        void select_pentestFalse_notInConditional() {
            ProjectConfig config =
                    TestConfigBuilder.builder()
                            .pentestReadiness(false)
                            .build();

            List<String> skills =
                    SkillsSelection
                            .selectConditionalSkills(config);

            assertThat(skills)
                    .doesNotContain("x-pentest");
        }
    }

}
