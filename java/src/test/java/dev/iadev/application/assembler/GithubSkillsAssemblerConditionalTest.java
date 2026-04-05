package dev.iadev.application.assembler;

import dev.iadev.testutil.TestConfigBuilder;
import dev.iadev.domain.model.ProjectConfig;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for GithubSkillsAssembler — conditional skills,
 * feature gates (infra, knowledge-packs), and full
 * pipeline integration.
 */
@DisplayName("GithubSkillsAssembler — conditional")
class GithubSkillsAssemblerConditionalTest {

    @Nested
    @DisplayName("filterSkills — non-infrastructure")
    class FilterSkillsNonInfra {

        @Test
        @DisplayName("returns all skills for"
                + " non-infrastructure group")
        void filterSkills_whenCalled_returnsAllForNonInfra() {
            GithubSkillsAssembler assembler =
                    new GithubSkillsAssembler();
            ProjectConfig config =
                    TestConfigBuilder.minimal();
            List<String> skills = List.of(
                    "x-story-epic", "x-story-create");

            List<String> filtered =
                    assembler.filterSkills(
                            config, "story", skills);

            assertThat(filtered)
                    .containsExactly(
                            "x-story-epic",
                            "x-story-create");
        }
    }

    @Nested
    @DisplayName("filterSkills — infrastructure"
            + " feature gates")
    class FilterSkillsInfra {

        @Test
        @DisplayName("dockerfile included when"
                + " container is docker")
        void filterSkills_whenCalled_dockerfileIncludedWhenDocker() {
            GithubSkillsAssembler assembler =
                    new GithubSkillsAssembler();
            ProjectConfig config =
                    TestConfigBuilder.builder()
                            .container("docker")
                            .orchestrator("none")
                            .build();
            List<String> skills = List.of(
                    "setup-environment",
                    "k8s-deployment",
                    "k8s-kustomize",
                    "dockerfile",
                    "iac-terraform");

            List<String> filtered =
                    assembler.filterSkills(
                            config, "infrastructure",
                            skills);

            assertThat(filtered)
                    .contains("dockerfile")
                    .doesNotContain("setup-environment")
                    .doesNotContain("k8s-deployment");
        }

        @Test
        @DisplayName("k8s skills included when"
                + " orchestrator is kubernetes")
        void filterSkills_whenCalled_k8sSkillsIncludedWhenKubernetes() {
            GithubSkillsAssembler assembler =
                    new GithubSkillsAssembler();
            ProjectConfig config =
                    TestConfigBuilder.builder()
                            .container("docker")
                            .orchestrator("kubernetes")
                            .build();
            List<String> skills = List.of(
                    "setup-environment",
                    "k8s-deployment",
                    "k8s-kustomize",
                    "dockerfile",
                    "iac-terraform");

            List<String> filtered =
                    assembler.filterSkills(
                            config, "infrastructure",
                            skills);

            assertThat(filtered)
                    .contains("setup-environment")
                    .contains("k8s-deployment")
                    .contains("k8s-kustomize")
                    .contains("dockerfile");
        }

        @Test
        @DisplayName("no infra skills when all none")
        void filterSkills_noInfraSkillsWhenAllNone_succeeds() {
            GithubSkillsAssembler assembler =
                    new GithubSkillsAssembler();
            ProjectConfig config =
                    TestConfigBuilder.builder()
                            .container("none")
                            .orchestrator("none")
                            .iac("none")
                            .templating("none")
                            .build();
            List<String> skills = List.of(
                    "setup-environment",
                    "k8s-deployment",
                    "k8s-kustomize",
                    "dockerfile",
                    "iac-terraform");

            List<String> filtered =
                    assembler.filterSkills(
                            config, "infrastructure",
                            skills);

            assertThat(filtered).isEmpty();
        }

        @Test
        @DisplayName("iac-terraform included when"
                + " iac is terraform")
        void filterSkills_whenCalled_iacTerraformIncluded() {
            GithubSkillsAssembler assembler =
                    new GithubSkillsAssembler();
            ProjectConfig config =
                    TestConfigBuilder.builder()
                            .container("none")
                            .orchestrator("none")
                            .iac("terraform")
                            .templating("none")
                            .build();
            List<String> skills = List.of(
                    "setup-environment",
                    "k8s-deployment",
                    "k8s-kustomize",
                    "dockerfile",
                    "iac-terraform");

            List<String> filtered =
                    assembler.filterSkills(
                            config, "infrastructure",
                            skills);

            assertThat(filtered)
                    .containsExactly("iac-terraform");
        }
    }

    @Nested
    @DisplayName("filterSkills — knowledge-packs"
            + " feature gates")
    class FilterSkillsKnowledgePacks {

        @Test
        @DisplayName("disaster-recovery included when"
                + " container is docker")
        void filterSkills_kp_drIncludedWhenDocker() {
            GithubSkillsAssembler assembler =
                    new GithubSkillsAssembler();
            ProjectConfig config =
                    TestConfigBuilder.builder()
                            .container("docker")
                            .build();
            List<String> skills = List.of(
                    "architecture", "coding-standards",
                    "disaster-recovery");

            List<String> filtered =
                    assembler.filterSkills(
                            config,
                            "knowledge-packs",
                            skills);

            assertThat(filtered)
                    .contains("disaster-recovery")
                    .contains("architecture")
                    .contains("coding-standards");
        }

        @Test
        @DisplayName("disaster-recovery excluded when"
                + " container is none")
        void filterSkills_kp_drExcludedWhenNone() {
            GithubSkillsAssembler assembler =
                    new GithubSkillsAssembler();
            ProjectConfig config =
                    TestConfigBuilder.builder()
                            .container("none")
                            .build();
            List<String> skills = List.of(
                    "architecture", "coding-standards",
                    "disaster-recovery");

            List<String> filtered =
                    assembler.filterSkills(
                            config,
                            "knowledge-packs",
                            skills);

            assertThat(filtered)
                    .doesNotContain("disaster-recovery")
                    .contains("architecture")
                    .contains("coding-standards");
        }

        @Test
        @DisplayName("unconditioned kp skills always"
                + " included")
        void filterSkills_kp_unconditionedAlwaysIncluded() {
            GithubSkillsAssembler assembler =
                    new GithubSkillsAssembler();
            ProjectConfig config =
                    TestConfigBuilder.builder()
                            .container("none")
                            .build();
            List<String> skills = List.of(
                    "architecture", "resilience",
                    "security");

            List<String> filtered =
                    assembler.filterSkills(
                            config,
                            "knowledge-packs",
                            skills);

            assertThat(filtered)
                    .containsExactly("architecture",
                            "resilience", "security");
        }
    }

}