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
class SkillsSelectionTest {

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
                + " x-obs-instrument")
        void select_observability_includesOtel() {
            ProjectConfig config = TestConfigBuilder.builder()
                    .observabilityTool("prometheus")
                    .build();

            List<String> skills =
                    SkillsSelection.selectInfraSkills(config);

            assertThat(skills)
                    .contains("x-obs-instrument");
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
                + " x-security-pentest")
        void select_pentestTrue_includesPentest() {
            ProjectConfig config =
                    TestConfigBuilder.builder()
                            .pentestReadiness(true)
                            .build();

            List<String> skills =
                    SkillsSelection.selectPentestSkills(
                            config);

            assertThat(skills)
                    .containsExactly("x-security-pentest");
        }

        @Test
        @DisplayName("pentestReadiness false excludes"
                + " x-security-pentest")
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
        @DisplayName("default config excludes x-security-pentest")
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

            assertThat(skills).contains("x-security-pentest");
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
                    .doesNotContain("x-security-pentest");
        }
    }

    @Nested
    @DisplayName("selectReviewSkills")
    class SelectReviewSkills {

        @Test
        @DisplayName("database configured includes"
                + " x-review-db")
        void select_database_includesDbReview() {
            ProjectConfig config =
                    TestConfigBuilder.builder()
                            .database("postgresql", "16")
                            .build();

            List<String> skills =
                    SkillsSelection.selectReviewSkills(
                            config);

            assertThat(skills).contains("x-review-db");
        }

        @Test
        @DisplayName("database none excludes x-review-db")
        void select_databaseNone_excludesDbReview() {
            ProjectConfig config =
                    TestConfigBuilder.builder()
                            .database("none", "")
                            .build();

            List<String> skills =
                    SkillsSelection.selectReviewSkills(
                            config);

            assertThat(skills)
                    .doesNotContain("x-review-db");
        }

        @Test
        @DisplayName("observability configured includes"
                + " x-review-obs")
        void select_observability_includesObsReview() {
            ProjectConfig config =
                    TestConfigBuilder.builder()
                            .observabilityTool(
                                    "opentelemetry")
                            .build();

            List<String> skills =
                    SkillsSelection.selectReviewSkills(
                            config);

            assertThat(skills).contains("x-review-obs");
        }

        @Test
        @DisplayName("observability none excludes"
                + " x-review-obs")
        void select_observabilityNone_excludesObsReview() {
            ProjectConfig config =
                    TestConfigBuilder.builder()
                            .observabilityTool("none")
                            .build();

            List<String> skills =
                    SkillsSelection.selectReviewSkills(
                            config);

            assertThat(skills)
                    .doesNotContain("x-review-obs");
        }

        @Test
        @DisplayName("container configured includes"
                + " x-review-devops")
        void select_container_includesDevopsReview() {
            ProjectConfig config =
                    TestConfigBuilder.builder()
                            .container("docker")
                            .build();

            List<String> skills =
                    SkillsSelection.selectReviewSkills(
                            config);

            assertThat(skills)
                    .contains("x-review-devops");
        }

        @Test
        @DisplayName("container none excludes"
                + " x-review-devops")
        void select_containerNone_excludesDevopsReview() {
            ProjectConfig config =
                    TestConfigBuilder.builder()
                            .container("none")
                            .build();

            List<String> skills =
                    SkillsSelection.selectReviewSkills(
                            config);

            assertThat(skills)
                    .doesNotContain("x-review-devops");
        }

        @Test
        @DisplayName("database + hexagonal includes"
                + " x-review-data-modeling")
        void select_dbHexagonal_includesDataModeling() {
            ProjectConfig config =
                    TestConfigBuilder.builder()
                            .database("postgresql", "16")
                            .archStyle("hexagonal")
                            .build();

            List<String> skills =
                    SkillsSelection.selectReviewSkills(
                            config);

            assertThat(skills)
                    .contains("x-review-data-modeling");
        }

        @Test
        @DisplayName("database + ddd includes"
                + " x-review-data-modeling")
        void select_dbDdd_includesDataModeling() {
            ProjectConfig config =
                    TestConfigBuilder.builder()
                            .database("postgresql", "16")
                            .archStyle("ddd")
                            .build();

            List<String> skills =
                    SkillsSelection.selectReviewSkills(
                            config);

            assertThat(skills)
                    .contains("x-review-data-modeling");
        }

        @Test
        @DisplayName("database + cqrs includes"
                + " x-review-data-modeling")
        void select_dbCqrs_includesDataModeling() {
            ProjectConfig config =
                    TestConfigBuilder.builder()
                            .database("postgresql", "16")
                            .archStyle("cqrs")
                            .build();

            List<String> skills =
                    SkillsSelection.selectReviewSkills(
                            config);

            assertThat(skills)
                    .contains("x-review-data-modeling");
        }

        @Test
        @DisplayName("database + clean includes"
                + " x-review-data-modeling")
        void select_dbClean_includesDataModeling() {
            ProjectConfig config =
                    TestConfigBuilder.builder()
                            .database("postgresql", "16")
                            .archStyle("clean")
                            .build();

            List<String> skills =
                    SkillsSelection.selectReviewSkills(
                            config);

            assertThat(skills)
                    .contains("x-review-data-modeling");
        }

        @Test
        @DisplayName("database + monolith excludes"
                + " x-review-data-modeling")
        void select_dbMonolith_excludesDataModeling() {
            ProjectConfig config =
                    TestConfigBuilder.builder()
                            .database("postgresql", "16")
                            .archStyle("monolith")
                            .build();

            List<String> skills =
                    SkillsSelection.selectReviewSkills(
                            config);

            assertThat(skills)
                    .doesNotContain(
                            "x-review-data-modeling");
        }

        @Test
        @DisplayName("no database excludes"
                + " x-review-data-modeling even if"
                + " hexagonal")
        void select_noDbHexagonal_excludesDataModeling() {
            ProjectConfig config =
                    TestConfigBuilder.builder()
                            .database("none", "")
                            .archStyle("hexagonal")
                            .build();

            List<String> skills =
                    SkillsSelection.selectReviewSkills(
                            config);

            assertThat(skills)
                    .doesNotContain(
                            "x-review-data-modeling");
        }

        @Test
        @DisplayName("all gates inactive returns"
                + " empty list")
        void select_allNone_returnsEmpty() {
            ProjectConfig config =
                    TestConfigBuilder.builder()
                            .database("none", "")
                            .observabilityTool("none")
                            .container("none")
                            .archStyle("library")
                            .build();

            List<String> skills =
                    SkillsSelection.selectReviewSkills(
                            config);

            assertThat(skills).isEmpty();
        }

        @Test
        @DisplayName("review skills appear in"
                + " selectConditionalSkills")
        void select_reviewSkills_inConditional() {
            ProjectConfig config =
                    TestConfigBuilder.builder()
                            .database("postgresql", "16")
                            .container("docker")
                            .build();

            List<String> skills =
                    SkillsSelection
                            .selectConditionalSkills(config);

            assertThat(skills)
                    .contains("x-review-db",
                            "x-review-devops");
        }

        @Test
        @DisplayName("review skills absent from"
                + " selectConditionalSkills when"
                + " gates inactive")
        void select_noGates_notInConditional() {
            ProjectConfig config =
                    TestConfigBuilder.builder()
                            .database("none", "")
                            .observabilityTool("none")
                            .container("none")
                            .build();

            List<String> skills =
                    SkillsSelection
                            .selectConditionalSkills(config);

            assertThat(skills)
                    .doesNotContain(
                            "x-review-db",
                            "x-review-obs",
                            "x-review-devops",
                            "x-review-data-modeling");
        }
    }

}
