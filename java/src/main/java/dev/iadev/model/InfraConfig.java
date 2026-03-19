package dev.iadev.model;

import java.util.Map;

/**
 * Represents the infrastructure configuration section.
 *
 * <p>All fields have sensible defaults matching the TypeScript implementation.
 * Contains a nested {@link ObservabilityConfig} for observability settings.</p>
 *
 * <p>Example fromMap usage:
 * <pre>{@code
 * var map = Map.of("container", "docker", "orchestrator", "kubernetes");
 * InfraConfig cfg = InfraConfig.fromMap(map);
 * }</pre>
 * </p>
 *
 * @param container the container runtime (default: "docker")
 * @param orchestrator the orchestration platform (default: "none")
 * @param templating the template/manifest tool (default: "kustomize")
 * @param iac the infrastructure-as-code tool (default: "none")
 * @param registry the container registry (default: "none")
 * @param apiGateway the API gateway name (default: "none")
 * @param serviceMesh the service mesh name (default: "none")
 * @param cloudProvider the cloud provider (default: "none")
 * @param observability the nested observability config
 */
public record InfraConfig(
        String container,
        String orchestrator,
        String templating,
        String iac,
        String registry,
        String apiGateway,
        String serviceMesh,
        String cloudProvider,
        ObservabilityConfig observability) {

    /**
     * Creates an InfraConfig from a YAML-parsed map.
     *
     * @param map the map from YAML deserialization
     * @return a new InfraConfig instance with defaults for missing values
     */
    public static InfraConfig fromMap(Map<String, Object> map) {
        return new InfraConfig(
                MapHelper.optionalString(map, "container", "docker"),
                MapHelper.optionalString(map, "orchestrator", "none"),
                MapHelper.optionalString(map, "templating", "kustomize"),
                MapHelper.optionalString(map, "iac", "none"),
                MapHelper.optionalString(map, "registry", "none"),
                MapHelper.optionalString(map, "api_gateway", "none"),
                MapHelper.optionalString(map, "service_mesh", "none"),
                MapHelper.optionalString(map, "cloud_provider", "none"),
                ObservabilityConfig.fromMap(
                        MapHelper.optionalMap(map, "observability"))
        );
    }
}
