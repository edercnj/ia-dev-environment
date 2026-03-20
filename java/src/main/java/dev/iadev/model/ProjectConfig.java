package dev.iadev.model;

import java.util.List;
import java.util.Map;

/**
 * Root aggregate containing the complete project configuration.
 *
 * <p>This is the primary configuration object parsed from YAML. It delegates
 * to sub-config {@code fromMap()} methods for each section. Required sections
 * (project, architecture, interfaces, language, framework) throw
 * {@link dev.iadev.exception.ConfigValidationException} if missing. Optional sections
 * (data, security, observability, infrastructure, testing, mcp) use defaults.</p>
 *
 * <p>Example fromMap usage:
 * <pre>{@code
 * Map<String, Object> yamlRoot = snakeYaml.load(configFile);
 * ProjectConfig config = ProjectConfig.fromMap(yamlRoot);
 * }</pre>
 * </p>
 *
 * @param project the project identity (required)
 * @param architecture the architecture config (required)
 * @param interfaces the list of interface configs (required, immutable)
 * @param language the language config (required)
 * @param framework the framework config (required)
 * @param data the data layer config (optional, defaults to empty)
 * @param infrastructure the infrastructure config (optional, defaults)
 * @param security the security config (optional, defaults to empty)
 * @param testing the testing config (optional, defaults: 95/90)
 * @param mcp the MCP config (optional, defaults to empty)
 */
public record ProjectConfig(
        ProjectIdentity project,
        ArchitectureConfig architecture,
        List<InterfaceConfig> interfaces,
        LanguageConfig language,
        FrameworkConfig framework,
        DataConfig data,
        InfraConfig infrastructure,
        SecurityConfig security,
        TestingConfig testing,
        McpConfig mcp) {

    /**
     * Compact constructor enforcing immutability of the interfaces list.
     */
    public ProjectConfig {
        interfaces = List.copyOf(interfaces);
    }

    // --- Convenience accessors (Law of Demeter) ---

    /**
     * Returns the observability tool name, breaking the
     * 3-level chain {@code infrastructure().observability().tool()}.
     *
     * @return the observability tool (e.g. "prometheus", "none")
     */
    public String observabilityTool() {
        return infrastructure().observability().tool();
    }

    /**
     * Returns the observability tracing backend, breaking the
     * 3-level chain {@code infrastructure().observability().tracing()}.
     *
     * @return the tracing backend (e.g. "jaeger", "none")
     */
    public String observabilityTracing() {
        return infrastructure().observability().tracing();
    }

    /**
     * Returns the observability metrics backend, breaking the
     * 3-level chain {@code infrastructure().observability().metrics()}.
     *
     * @return the metrics backend (e.g. "micrometer", "none")
     */
    public String observabilityMetrics() {
        return infrastructure().observability().metrics();
    }

    /**
     * Returns the database name, breaking the
     * 3-level chain {@code data().database().name()}.
     *
     * @return the database name (e.g. "postgresql", "none")
     */
    public String databaseName() {
        return data().database().name();
    }

    /**
     * Returns the migration tool name, breaking the
     * 3-level chain {@code data().migration().name()}.
     *
     * @return the migration tool name (e.g. "flyway", "none")
     */
    public String migrationName() {
        return data().migration().name();
    }

    /**
     * Returns the cache name, breaking the
     * 3-level chain {@code data().cache().name()}.
     *
     * @return the cache name (e.g. "redis", "none")
     */
    public String cacheName() {
        return data().cache().name();
    }

    // --- End convenience accessors ---

    /**
     * Creates a ProjectConfig from a YAML-parsed map.
     *
     * <p>Delegates to each sub-config's {@code fromMap()} method.
     * Required sections throw if absent; optional sections use defaults.</p>
     *
     * @param map the root map from YAML deserialization
     * @return a new ProjectConfig instance
     * @throws dev.iadev.exception.ConfigValidationException if required sections are missing
     */
    @SuppressWarnings("unchecked")
    public static ProjectConfig fromMap(
            Map<String, Object> map) {
        List<InterfaceConfig> interfaceList =
                parseInterfaces(map);
        return buildFromMap(map, interfaceList);
    }

    @SuppressWarnings("unchecked")
    private static List<InterfaceConfig> parseInterfaces(
            Map<String, Object> map) {
        var raw = MapHelper.requireField(
                map, "interfaces", "ProjectConfig");
        if (raw instanceof List<?> list) {
            return list.stream()
                    .map(item -> InterfaceConfig.fromMap(
                            (Map<String, Object>) item))
                    .toList();
        }
        throw new dev.iadev.exception
                .ConfigValidationException(
                "interfaces", "List", "ProjectConfig");
    }

    private static ProjectConfig buildFromMap(
            Map<String, Object> map,
            List<InterfaceConfig> interfaceList) {
        return new ProjectConfig(
                ProjectIdentity.fromMap(MapHelper
                        .requireMap(map, "project",
                                "ProjectConfig")),
                ArchitectureConfig.fromMap(MapHelper
                        .requireMap(map, "architecture",
                                "ProjectConfig")),
                interfaceList,
                LanguageConfig.fromMap(MapHelper
                        .requireMap(map, "language",
                                "ProjectConfig")),
                FrameworkConfig.fromMap(MapHelper
                        .requireMap(map, "framework",
                                "ProjectConfig")),
                DataConfig.fromMap(MapHelper
                        .optionalMap(map, "data")),
                InfraConfig.fromMap(MapHelper
                        .optionalMap(map,
                                "infrastructure")),
                SecurityConfig.fromMap(MapHelper
                        .optionalMap(map, "security")),
                TestingConfig.fromMap(MapHelper
                        .optionalMap(map, "testing")),
                McpConfig.fromMap(MapHelper
                        .optionalMap(map, "mcp")));
    }
}
