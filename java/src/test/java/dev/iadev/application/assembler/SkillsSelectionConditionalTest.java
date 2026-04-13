package dev.iadev.application.assembler;

import dev.iadev.testutil.TestConfigBuilder;

import dev.iadev.domain.model.ProjectConfig;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for SkillsSelection — conditional skill inclusion:
 * aggregation, outbox, DDD, container, and event packs.
 */
@DisplayName("SkillsSelection — conditional")
class SkillsSelectionConditionalTest {

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
                    .contains("x-test-perf")
                    .contains("x-review-security");
        }
    }

    @Nested
    @DisplayName("selectKnowledgePacks — conditional packs")
    class ConditionalKnowledgePacks {

        @Test
        @DisplayName("config with outboxPattern true"
                + " includes patterns-outbox")
        void select_outboxTrue_includesOutboxPack() {
            ProjectConfig config =
                    TestConfigBuilder.builder()
                            .outboxPattern(true)
                            .build();

            List<String> packs =
                    SkillsSelection.selectKnowledgePacks(
                            config);

            assertThat(packs)
                    .contains("patterns-outbox");
        }

        @Test
        @DisplayName("config without outboxPattern excludes"
                + " patterns-outbox")
        void select_outboxFalse_excludesOutboxPack() {
            ProjectConfig config =
                    TestConfigBuilder.builder()
                            .outboxPattern(false)
                            .build();

            List<String> packs =
                    SkillsSelection.selectKnowledgePacks(
                            config);

            assertThat(packs)
                    .doesNotContain("patterns-outbox");
        }

        @Test
        @DisplayName("config with eventDriven and"
                + " outboxPattern includes patterns-outbox")
        void select_eventDrivenAndOutbox_includesOutbox() {
            ProjectConfig config =
                    TestConfigBuilder.builder()
                            .eventDriven(true)
                            .outboxPattern(true)
                            .build();

            List<String> packs =
                    SkillsSelection.selectKnowledgePacks(
                            config);

            assertThat(packs)
                    .contains("patterns-outbox");
        }

        @Test
        @DisplayName("config with container includes"
                + " disaster-recovery")
        void select_container_includesDrPack() {
            ProjectConfig config =
                    TestConfigBuilder.builder()
                            .container("docker")
                            .build();

            List<String> packs =
                    SkillsSelection.selectKnowledgePacks(
                            config);

            assertThat(packs)
                    .contains("disaster-recovery");
        }

        @Test
        @DisplayName("config without container excludes"
                + " disaster-recovery")
        void select_noContainer_excludesDrPack() {
            ProjectConfig config =
                    TestConfigBuilder.builder()
                            .container("none")
                            .build();

            List<String> packs =
                    SkillsSelection.selectKnowledgePacks(
                            config);

            assertThat(packs)
                    .doesNotContain("disaster-recovery");
        }

        @Test
        @DisplayName("config with ddd.enabled true includes"
                + " ddd-strategic")
        void select_dddEnabled_includesDddStrategic() {
            ProjectConfig config =
                    TestConfigBuilder.builder()
                            .dddEnabled(true)
                            .build();

            List<String> packs =
                    SkillsSelection.selectKnowledgePacks(
                            config);

            assertThat(packs)
                    .contains("ddd-strategic");
        }

        @Test
        @DisplayName("config with architecture.style ddd"
                + " includes ddd-strategic")
        void select_styleDdd_includesDddStrategic() {
            ProjectConfig config =
                    TestConfigBuilder.builder()
                            .archStyle("ddd")
                            .build();

            List<String> packs =
                    SkillsSelection.selectKnowledgePacks(
                            config);

            assertThat(packs)
                    .contains("ddd-strategic");
        }

        @Test
        @DisplayName("config with architecture.style hexagonal"
                + " includes ddd-strategic")
        void select_styleHexagonal_includesDddStrategic() {
            ProjectConfig config =
                    TestConfigBuilder.builder()
                            .archStyle("hexagonal")
                            .build();

            List<String> packs =
                    SkillsSelection.selectKnowledgePacks(
                            config);

            assertThat(packs)
                    .contains("ddd-strategic");
        }

        @Test
        @DisplayName("config without ddd activation excludes"
                + " ddd-strategic")
        void select_noDddActivation_excludesDddStrategic() {
            ProjectConfig config =
                    TestConfigBuilder.builder()
                            .archStyle("layered")
                            .dddEnabled(false)
                            .build();

            List<String> packs =
                    SkillsSelection.selectKnowledgePacks(
                            config);

            assertThat(packs)
                    .doesNotContain("ddd-strategic");
        }
    }
}
