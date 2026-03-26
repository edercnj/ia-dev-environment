package dev.iadev.assembler;

import dev.iadev.model.ProjectConfig;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for SkillGroupRegistry — extracted skill group
 * data and infrastructure conditions.
 */
@DisplayName("SkillGroupRegistry")
class SkillGroupRegistryTest {

    @Nested
    @DisplayName("SKILL_GROUPS")
    class SkillGroups {

        @Test
        @DisplayName("contains exactly 8 groups")
        void register_whenCalled_containsEightGroups() {
            assertThat(SkillGroupRegistry.SKILL_GROUPS)
                    .hasSize(8);
        }

        @Test
        @DisplayName("contains all expected group names")
        void register_whenCalled_containsExpectedGroupNames() {
            assertThat(SkillGroupRegistry.SKILL_GROUPS
                    .keySet())
                    .containsExactly(
                            "story", "dev", "review",
                            "testing", "infrastructure",
                            "knowledge-packs",
                            "git-troubleshooting", "lib");
        }

        @Test
        @DisplayName("story group has 7 skills")
        void register_whenCalled_storyGroupSize() {
            assertThat(SkillGroupRegistry.SKILL_GROUPS
                    .get("story")).hasSize(7);
        }

        @Test
        @DisplayName("dev group has 8 skills")
        void register_whenCalled_devGroupSize() {
            assertThat(SkillGroupRegistry.SKILL_GROUPS
                    .get("dev")).hasSize(8);
        }

        @Test
        @DisplayName("review group has 8 skills")
        void register_whenCalled_reviewGroupSize() {
            assertThat(SkillGroupRegistry.SKILL_GROUPS
                    .get("review")).hasSize(8);
        }

        @Test
        @DisplayName("testing group has 6 skills")
        void register_whenCalled_testingGroupSize() {
            assertThat(SkillGroupRegistry.SKILL_GROUPS
                    .get("testing")).hasSize(6);
        }

        @Test
        @DisplayName("infrastructure group has 5 skills")
        void register_whenCalled_infrastructureGroupSize() {
            assertThat(SkillGroupRegistry.SKILL_GROUPS
                    .get("infrastructure")).hasSize(5);
        }

        @Test
        @DisplayName("knowledge-packs group has 13 skills")
        void register_whenCalled_knowledgePacksGroupSize() {
            assertThat(SkillGroupRegistry.SKILL_GROUPS
                    .get("knowledge-packs")).hasSize(13);
        }

        @Test
        @DisplayName("git-troubleshooting has 4 skills")
        void register_whenCalled_gitTroubleshootingGroupSize() {
            assertThat(SkillGroupRegistry.SKILL_GROUPS
                    .get("git-troubleshooting")).hasSize(4);
        }

        @Test
        @DisplayName("lib group has 3 skills")
        void register_whenCalled_libGroupSize() {
            assertThat(SkillGroupRegistry.SKILL_GROUPS
                    .get("lib")).hasSize(3);
        }

        @Test
        @DisplayName("data matches GithubSkillsAssembler"
                + " delegate")
        void register_data_matchesDelegate() {
            assertThat(GithubSkillsAssembler.SKILL_GROUPS)
                    .isSameAs(
                            SkillGroupRegistry.SKILL_GROUPS);
        }
    }

    @Nested
    @DisplayName("INFRA_SKILL_CONDITIONS")
    class InfraSkillConditions {

        @Test
        @DisplayName("contains exactly 5 conditions")
        void register_whenCalled_containsFiveConditions() {
            assertThat(SkillGroupRegistry
                    .INFRA_SKILL_CONDITIONS)
                    .hasSize(5);
        }

        @Test
        @DisplayName("contains expected skill names")
        void register_whenCalled_containsExpectedSkillNames() {
            assertThat(SkillGroupRegistry
                    .INFRA_SKILL_CONDITIONS.keySet())
                    .containsExactly(
                            "setup-environment",
                            "k8s-deployment",
                            "k8s-kustomize",
                            "dockerfile",
                            "iac-terraform");
        }

        @Test
        @DisplayName("setup-environment requires"
                + " non-none orchestrator")
        void register_whenCalled_setupEnvironmentCondition() {
            Predicate<ProjectConfig> cond =
                    SkillGroupRegistry
                            .INFRA_SKILL_CONDITIONS
                            .get("setup-environment");
            ProjectConfig k8s = TestConfigBuilder.builder()
                    .orchestrator("kubernetes").build();
            ProjectConfig none = TestConfigBuilder.builder()
                    .orchestrator("none").build();
            assertThat(cond.test(k8s)).isTrue();
            assertThat(cond.test(none)).isFalse();
        }

        @Test
        @DisplayName("k8s-deployment requires kubernetes")
        void register_whenCalled_k8sDeploymentCondition() {
            Predicate<ProjectConfig> cond =
                    SkillGroupRegistry
                            .INFRA_SKILL_CONDITIONS
                            .get("k8s-deployment");
            ProjectConfig k8s = TestConfigBuilder.builder()
                    .orchestrator("kubernetes").build();
            ProjectConfig other = TestConfigBuilder.builder()
                    .orchestrator("nomad").build();
            assertThat(cond.test(k8s)).isTrue();
            assertThat(cond.test(other)).isFalse();
        }

        @Test
        @DisplayName("dockerfile requires non-none"
                + " container")
        void register_whenCalled_dockerfileCondition() {
            Predicate<ProjectConfig> cond =
                    SkillGroupRegistry
                            .INFRA_SKILL_CONDITIONS
                            .get("dockerfile");
            ProjectConfig docker = TestConfigBuilder
                    .builder()
                    .container("docker").build();
            ProjectConfig none = TestConfigBuilder
                    .builder()
                    .container("none").build();
            assertThat(cond.test(docker)).isTrue();
            assertThat(cond.test(none)).isFalse();
        }

        @Test
        @DisplayName("iac-terraform requires terraform")
        void register_whenCalled_iacTerraformCondition() {
            Predicate<ProjectConfig> cond =
                    SkillGroupRegistry
                            .INFRA_SKILL_CONDITIONS
                            .get("iac-terraform");
            ProjectConfig tf = TestConfigBuilder.builder()
                    .iac("terraform").build();
            ProjectConfig none = TestConfigBuilder.builder()
                    .iac("none").build();
            assertThat(cond.test(tf)).isTrue();
            assertThat(cond.test(none)).isFalse();
        }

        @Test
        @DisplayName("data matches GithubSkillsAssembler"
                + " delegate")
        void register_data_matchesDelegate() {
            assertThat(GithubSkillsAssembler
                    .INFRA_SKILL_CONDITIONS)
                    .isSameAs(SkillGroupRegistry
                            .INFRA_SKILL_CONDITIONS);
        }
    }
}
