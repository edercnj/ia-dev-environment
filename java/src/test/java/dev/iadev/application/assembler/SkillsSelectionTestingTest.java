package dev.iadev.application.assembler;

import dev.iadev.testutil.TestConfigBuilder;

import dev.iadev.domain.model.ProjectConfig;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for SkillsSelection — testing skill selection.
 */
@DisplayName("SkillsSelection — testing skills")
class SkillsSelectionTestingTest {

    @Nested
    @DisplayName("selectTestingSkills")
    class SelectTestingSkills {

        @Test
        @DisplayName("always includes x-test-e2e")
        void select_always_includesRunE2e() {
            ProjectConfig config =
                    TestConfigBuilder.builder().build();

            List<String> skills =
                    SkillsSelection.selectTestingSkills(
                            config);

            assertThat(skills).contains("x-test-e2e");
        }

        @Test
        @DisplayName("smoke tests with REST includes"
                + " x-test-smoke-api")
        void select_smokeRest_includesSmokeApi() {
            ProjectConfig config =
                    TestConfigBuilder.builder()
                            .smokeTests(true)
                            .clearInterfaces()
                            .addInterface("rest")
                            .build();

            List<String> skills =
                    SkillsSelection.selectTestingSkills(
                            config);

            assertThat(skills).contains("x-test-smoke-api");
        }

        @Test
        @DisplayName("smoke tests with tcp-custom"
                + " includes x-test-smoke-socket")
        void select_smokeTcp_includesSmokeSocket() {
            ProjectConfig config =
                    TestConfigBuilder.builder()
                            .smokeTests(true)
                            .clearInterfaces()
                            .addInterface("tcp-custom")
                            .build();

            List<String> skills =
                    SkillsSelection.selectTestingSkills(
                            config);

            assertThat(skills)
                    .contains("x-test-smoke-socket");
        }

        @Test
        @DisplayName("performance tests includes"
                + " x-test-perf")
        void select_performance_includesPerfTest() {
            ProjectConfig config =
                    TestConfigBuilder.builder()
                            .performanceTests(true)
                            .build();

            List<String> skills =
                    SkillsSelection.selectTestingSkills(
                            config);

            assertThat(skills).contains("x-test-perf");
        }

        @Test
        @DisplayName("contract tests includes"
                + " x-test-contract")
        void select_contract_includesContractTests() {
            ProjectConfig config =
                    TestConfigBuilder.builder()
                            .contractTests(true)
                            .build();

            List<String> skills =
                    SkillsSelection.selectTestingSkills(
                            config);

            assertThat(skills)
                    .contains("x-test-contract");
        }
    }
}
