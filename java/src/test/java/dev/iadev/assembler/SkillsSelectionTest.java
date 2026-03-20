package dev.iadev.assembler;

import dev.iadev.model.ProjectConfig;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for SkillsSelection — pure selection logic that
 * evaluates feature gates and returns skill/pack names.
 */
@DisplayName("SkillsSelection")
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
    @DisplayName("selectTestingSkills")
    class SelectTestingSkills {

        @Test
        @DisplayName("always includes run-e2e")
        void select_always_includesRunE2e() {
            ProjectConfig config = TestConfigBuilder.builder()
                    .build();

            List<String> skills =
                    SkillsSelection.selectTestingSkills(
                            config);

            assertThat(skills).contains("run-e2e");
        }

        @Test
        @DisplayName("smoke tests with REST includes"
                + " run-smoke-api")
        void select_smokeRest_includesSmokeApi() {
            ProjectConfig config = TestConfigBuilder.builder()
                    .smokeTests(true)
                    .clearInterfaces()
                    .addInterface("rest")
                    .build();

            List<String> skills =
                    SkillsSelection.selectTestingSkills(
                            config);

            assertThat(skills).contains("run-smoke-api");
        }

        @Test
        @DisplayName("smoke tests with tcp-custom includes"
                + " run-smoke-socket")
        void select_smokeTcp_includesSmokeSocket() {
            ProjectConfig config = TestConfigBuilder.builder()
                    .smokeTests(true)
                    .clearInterfaces()
                    .addInterface("tcp-custom")
                    .build();

            List<String> skills =
                    SkillsSelection.selectTestingSkills(
                            config);

            assertThat(skills)
                    .contains("run-smoke-socket");
        }

        @Test
        @DisplayName("performance tests includes"
                + " run-perf-test")
        void select_performance_includesPerfTest() {
            ProjectConfig config = TestConfigBuilder.builder()
                    .performanceTests(true)
                    .build();

            List<String> skills =
                    SkillsSelection.selectTestingSkills(
                            config);

            assertThat(skills).contains("run-perf-test");
        }

        @Test
        @DisplayName("contract tests includes"
                + " run-contract-tests")
        void select_contract_includesContractTests() {
            ProjectConfig config = TestConfigBuilder.builder()
                    .contractTests(true)
                    .build();

            List<String> skills =
                    SkillsSelection.selectTestingSkills(
                            config);

            assertThat(skills)
                    .contains("run-contract-tests");
        }
    }

    @Nested
    @DisplayName("selectSecuritySkills")
    class SelectSecuritySkills {

        @Test
        @DisplayName("config with security frameworks"
                + " includes x-review-security")
        void select_securityFrameworks_includesReview() {
            ProjectConfig config = TestConfigBuilder.builder()
                    .securityFrameworks("spring-security")
                    .build();

            List<String> skills =
                    SkillsSelection.selectSecuritySkills(
                            config);

            assertThat(skills)
                    .contains("x-review-security");
        }

        @Test
        @DisplayName("config without security frameworks"
                + " returns empty")
        void select_noSecurity_returnsEmpty() {
            ProjectConfig config = TestConfigBuilder.builder()
                    .build();

            List<String> skills =
                    SkillsSelection.selectSecuritySkills(
                            config);

            assertThat(skills).isEmpty();
        }
    }

    @Nested
    @DisplayName("selectConditionalSkills")
    class SelectConditionalSkills {

        @Test
        @DisplayName("aggregates all conditional skills")
        void select_whenCalled_aggregatesAll() {
            ProjectConfig config = TestConfigBuilder.builder()
                    .clearInterfaces()
                    .addInterface("rest")
                    .addInterface("grpc")
                    .orchestrator("kubernetes")
                    .performanceTests(true)
                    .securityFrameworks("spring-security")
                    .build();

            List<String> skills =
                    SkillsSelection.selectConditionalSkills(
                            config);

            assertThat(skills)
                    .contains("x-review-api")
                    .contains("x-review-grpc")
                    .contains("setup-environment")
                    .contains("run-perf-test")
                    .contains("x-review-security");
        }
    }

    @Nested
    @DisplayName("selectKnowledgePacks")
    class SelectKnowledgePacks {

        @Test
        @DisplayName("always returns core knowledge packs"
                + " plus layer-templates")
        void select_always_returnsCoreAndLayerTemplates() {
            ProjectConfig config = TestConfigBuilder.builder()
                    .build();

            List<String> packs =
                    SkillsSelection.selectKnowledgePacks(
                            config);

            assertThat(packs)
                    .contains("coding-standards")
                    .contains("architecture")
                    .contains("testing")
                    .contains("security")
                    .contains("compliance")
                    .contains("api-design")
                    .contains("observability")
                    .contains("resilience")
                    .contains("infrastructure")
                    .contains("protocols")
                    .contains("story-planning")
                    .contains("layer-templates");
        }

        @Test
        @DisplayName("config with database includes"
                + " database-patterns")
        void select_database_includesDbPatterns() {
            ProjectConfig config = TestConfigBuilder.builder()
                    .database("postgresql", "16")
                    .build();

            List<String> packs =
                    SkillsSelection.selectKnowledgePacks(
                            config);

            assertThat(packs)
                    .contains("database-patterns");
        }

        @Test
        @DisplayName("config with cache includes"
                + " database-patterns")
        void select_cache_includesDbPatterns() {
            ProjectConfig config = TestConfigBuilder.builder()
                    .cache("redis", "7")
                    .build();

            List<String> packs =
                    SkillsSelection.selectKnowledgePacks(
                            config);

            assertThat(packs)
                    .contains("database-patterns");
        }

        @Test
        @DisplayName("config without database or cache"
                + " excludes database-patterns")
        void select_noDbNoCache_excludesDbPatterns() {
            ProjectConfig config = TestConfigBuilder.builder()
                    .build();

            List<String> packs =
                    SkillsSelection.selectKnowledgePacks(
                            config);

            assertThat(packs)
                    .doesNotContain("database-patterns");
        }

        @Test
        @DisplayName("returns at least 12 packs for"
                + " minimal config")
        void select_minimalConfig_returnsAtLeast12Packs() {
            ProjectConfig config =
                    TestConfigBuilder.minimal();

            List<String> packs =
                    SkillsSelection.selectKnowledgePacks(
                            config);

            assertThat(packs).hasSizeGreaterThanOrEqualTo(12);
        }
    }
}
