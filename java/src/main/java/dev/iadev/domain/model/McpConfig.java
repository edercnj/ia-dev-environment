package dev.iadev.domain.model;

import java.util.List;
import java.util.Map;

/**
 * Represents the MCP (Model Context Protocol) configuration section.
 *
 * <p>Contains a list of MCP server configurations. Defaults to an empty list.</p>
 *
 * <p>Example fromMap usage:
 * <pre>{@code
 * var map = Map.of("servers", List.of(
 *     Map.of("id", "s1", "url", "https://mcp1.example.com")));
 * McpConfig cfg = McpConfig.fromMap(map);
 * }</pre>
 * </p>
 *
 * @param servers the list of MCP server configurations (default: empty, immutable)
 */
public record McpConfig(List<McpServerConfig> servers) {

    /**
     * Compact constructor enforcing immutability.
     */
    public McpConfig {
        servers = List.copyOf(servers);
    }

    /**
     * Creates a McpConfig from a YAML-parsed map.
     *
     * @param map the map from YAML deserialization
     * @return a new McpConfig instance
     */
    @SuppressWarnings("unchecked")
    public static McpConfig fromMap(Map<String, Object> map) {
        var raw = map.get("servers");
        if (raw instanceof List<?> list) {
            var servers = list.stream()
                    .map(item -> McpServerConfig.fromMap(
                            (Map<String, Object>) item))
                    .toList();
            return new McpConfig(servers);
        }
        return new McpConfig(List.of());
    }
}
