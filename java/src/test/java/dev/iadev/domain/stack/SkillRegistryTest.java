package dev.iadev.domain.stack;

import dev.iadev.model.InfraConfig;
import dev.iadev.model.ObservabilityConfig;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("SkillRegistry")
class SkillRegistryTest {

    @Nested
    @DisplayName("CORE_KNOWLEDGE_PACKS")
    class CoreKnowledgePacksTests {

        @Test
        @DisplayName("contains exactly 14 entries")
        void coreKnowledgePacks_size_fourteen() {
            assertThat(SkillRegistry.CORE_KNOWLEDGE_PACKS)
                    .hasSize(14);
        }

        @Test
        @DisplayName("contains all expected packs")
        void coreKnowledgePacks_whenCalled_allExpected() {
            assertThat(SkillRegistry.CORE_KNOWLEDGE_PACKS)
                    .containsExactly(
                            "coding-standards",
                            "architecture",
                            "testing",
                            "security",
                            "compliance",
                            "api-design",
                            "observability",
                            "resilience",
                            "infrastructure",
                            "protocols",
                            "story-planning",
                            "ci-cd-patterns",
                            "sre-practices",
                            "release-management"
                    );
        }

        @Test
        @DisplayName("list is immutable")
        void coreKnowledgePacks_whenCalled_immutable() {
            assertThat(SkillRegistry.CORE_KNOWLEDGE_PACKS)
                    .isUnmodifiable();
        }
    }

    @Nested
    @DisplayName("buildInfraPackRules()")
    class BuildInfraPackRulesTests {

        @Test
        @DisplayName("kubernetes orchestrator enables k8s-deployment")
        void infraPackRules_kubernetes_k8sDeployment() {
            var infra = buildInfra("kubernetes", "kustomize",
                    "docker", "none", "none");

            var rules = SkillRegistry.buildInfraPackRules(infra);

            var k8sDeployment = rules.stream()
                    .filter(r -> "k8s-deployment".equals(r.packName()))
                    .findFirst();
            assertThat(k8sDeployment).isPresent();
            assertThat(k8sDeployment.get().included()).isTrue();
        }

        @Test
        @DisplayName("non-kubernetes orchestrator disables k8s-deployment")
        void infraPackRules_noK8s_k8sDeploymentDisabled() {
            var infra = buildInfra("none", "kustomize",
                    "docker", "none", "none");

            var rules = SkillRegistry.buildInfraPackRules(infra);

            var k8sDeployment = rules.stream()
                    .filter(r -> "k8s-deployment".equals(r.packName()))
                    .findFirst();
            assertThat(k8sDeployment).isPresent();
            assertThat(k8sDeployment.get().included()).isFalse();
        }

        @Test
        @DisplayName("helm templating enables k8s-helm")
        void infraPackRules_helm_k8sHelm() {
            var infra = buildInfra("kubernetes", "helm",
                    "docker", "none", "none");

            var rules = SkillRegistry.buildInfraPackRules(infra);

            var k8sHelm = rules.stream()
                    .filter(r -> "k8s-helm".equals(r.packName()))
                    .findFirst();
            assertThat(k8sHelm).isPresent();
            assertThat(k8sHelm.get().included()).isTrue();
        }

        @Test
        @DisplayName("docker container enables dockerfile")
        void infraPackRules_docker_dockerfileEnabled() {
            var infra = buildInfra("none", "kustomize",
                    "docker", "none", "none");

            var rules = SkillRegistry.buildInfraPackRules(infra);

            var dockerfile = rules.stream()
                    .filter(r -> "dockerfile".equals(r.packName()))
                    .findFirst();
            assertThat(dockerfile).isPresent();
            assertThat(dockerfile.get().included()).isTrue();
        }

        @Test
        @DisplayName("no container disables dockerfile")
        void infraPackRules_noContainer_dockerfileDisabled() {
            var infra = buildInfra("none", "kustomize",
                    "none", "none", "none");

            var rules = SkillRegistry.buildInfraPackRules(infra);

            var dockerfile = rules.stream()
                    .filter(r -> "dockerfile".equals(r.packName()))
                    .findFirst();
            assertThat(dockerfile).isPresent();
            assertThat(dockerfile.get().included()).isFalse();
        }

        @Test
        @DisplayName("terraform iac enables iac-terraform")
        void infraPackRules_terraform_iacTerraform() {
            var infra = buildInfra("none", "kustomize",
                    "docker", "terraform", "none");

            var rules = SkillRegistry.buildInfraPackRules(infra);

            var iacTerraform = rules.stream()
                    .filter(r -> "iac-terraform".equals(r.packName()))
                    .findFirst();
            assertThat(iacTerraform).isPresent();
            assertThat(iacTerraform.get().included()).isTrue();
        }

        @Test
        @DisplayName("crossplane iac enables iac-crossplane")
        void infraPackRules_crossplane_iacCrossplane() {
            var infra = buildInfra("none", "kustomize",
                    "docker", "crossplane", "none");

            var rules = SkillRegistry.buildInfraPackRules(infra);

            var iacCrossplane = rules.stream()
                    .filter(r -> "iac-crossplane".equals(r.packName()))
                    .findFirst();
            assertThat(iacCrossplane).isPresent();
            assertThat(iacCrossplane.get().included()).isTrue();
        }

        @Test
        @DisplayName("registry not none enables container-registry")
        void infraPackRules_registry_containerRegistryEnabled() {
            var infra = buildInfra("none", "kustomize",
                    "docker", "none", "ghcr.io");

            var rules = SkillRegistry.buildInfraPackRules(infra);

            var containerReg = rules.stream()
                    .filter(r -> "container-registry".equals(r.packName()))
                    .findFirst();
            assertThat(containerReg).isPresent();
            assertThat(containerReg.get().included()).isTrue();
        }

        @Test
        @DisplayName("returns exactly 7 rules")
        void infraPackRules_size_seven() {
            var infra = buildInfra("none", "kustomize",
                    "docker", "none", "none");

            var rules = SkillRegistry.buildInfraPackRules(infra);

            assertThat(rules).hasSize(7);
        }
    }

    private InfraConfig buildInfra(
            String orchestrator, String templating,
            String container, String iac, String registry) {
        return new InfraConfig(
                container, orchestrator, templating, iac, registry,
                "none", "none", "none",
                ObservabilityConfig.fromMap(Map.of()));
    }
}
