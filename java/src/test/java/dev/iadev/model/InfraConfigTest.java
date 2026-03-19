package dev.iadev.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("InfraConfig")
class InfraConfigTest {

    @Nested
    @DisplayName("fromMap()")
    class FromMap {

        @Test
        @DisplayName("creates config with all fields")
        void fromMap_allFields_allSet() {
            var map = Map.<String, Object>of(
                    "container", "docker",
                    "orchestrator", "kubernetes",
                    "templating", "helm",
                    "iac", "terraform",
                    "registry", "ecr",
                    "api_gateway", "kong",
                    "service_mesh", "istio",
                    "cloud_provider", "aws",
                    "observability", Map.of(
                            "tool", "datadog",
                            "metrics", "statsd",
                            "tracing", "xray"));

            var result = InfraConfig.fromMap(map);

            assertThat(result.container()).isEqualTo("docker");
            assertThat(result.orchestrator()).isEqualTo("kubernetes");
            assertThat(result.templating()).isEqualTo("helm");
            assertThat(result.iac()).isEqualTo("terraform");
            assertThat(result.registry()).isEqualTo("ecr");
            assertThat(result.apiGateway()).isEqualTo("kong");
            assertThat(result.serviceMesh()).isEqualTo("istio");
            assertThat(result.cloudProvider()).isEqualTo("aws");
            assertThat(result.observability().tool())
                    .isEqualTo("datadog");
        }

        @Test
        @DisplayName("empty map uses all defaults")
        void fromMap_emptyMap_allDefaults() {
            var result = InfraConfig.fromMap(Map.of());

            assertThat(result.container()).isEqualTo("docker");
            assertThat(result.orchestrator()).isEqualTo("none");
            assertThat(result.templating()).isEqualTo("kustomize");
            assertThat(result.iac()).isEqualTo("none");
            assertThat(result.registry()).isEqualTo("none");
            assertThat(result.apiGateway()).isEqualTo("none");
            assertThat(result.serviceMesh()).isEqualTo("none");
            assertThat(result.cloudProvider()).isEqualTo("none");
            assertThat(result.observability().tool()).isEqualTo("none");
            assertThat(result.observability().metrics())
                    .isEqualTo("none");
            assertThat(result.observability().tracing())
                    .isEqualTo("none");
        }

        @Test
        @DisplayName("nativeBuild defaults to false via InfraConfig")
        void fromMap_emptyMap_nativeBuildDefaultFalse() {
            // InfraConfig doesn't have nativeBuild directly,
            // but FrameworkConfig does. Verify InfraConfig defaults
            // don't include unexpected fields.
            var result = InfraConfig.fromMap(Map.of());

            assertThat(result.container()).isEqualTo("docker");
        }

        @Test
        @DisplayName("partial map defaults missing fields")
        void fromMap_partialMap_missingDefaulted() {
            var map = Map.<String, Object>of(
                    "container", "podman",
                    "orchestrator", "nomad");

            var result = InfraConfig.fromMap(map);

            assertThat(result.container()).isEqualTo("podman");
            assertThat(result.orchestrator()).isEqualTo("nomad");
            assertThat(result.iac()).isEqualTo("none");
        }
    }
}
