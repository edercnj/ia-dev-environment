package dev.iadev.model;

import java.util.List;
import java.util.Map;

/**
 * Represents a single MCP (Model Context Protocol) server configuration.
 *
 * <p>The {@code id} and {@code url} are required. The {@code capabilities}
 * list and {@code env} map are optional.</p>
 *
 * <p>The env map may contain sensitive values (API keys, tokens).
 * Use caution when serializing.</p>
 *
 * <p>Example fromMap usage:
 * <pre>{@code
 * var map = Map.of("id", "server1", "url", "https://mcp.example.com",
 *     "capabilities", List.of("read", "write"));
 * McpServerConfig cfg = McpServerConfig.fromMap(map);
 * }</pre>
 * </p>
 *
 * @param id the server identifier (required)
 * @param url the server URL (required)
 * @param capabilities the list of capabilities (default: empty, immutable)
 * @param env the environment variables map (default: empty, immutable)
 */
public record McpServerConfig(
        String id,
        String url,
        List<String> capabilities,
        Map<String, String> env) {

    /**
     * Compact constructor enforcing immutability of collections.
     */
    public McpServerConfig {
        capabilities = List.copyOf(capabilities);
        env = Map.copyOf(env);
    }

    /**
     * Creates a McpServerConfig from a YAML-parsed map.
     *
     * @param map the map from YAML deserialization
     * @return a new McpServerConfig instance
     * @throws dev.iadev.exception.ConfigValidationException if id or url is missing
     */
    public static McpServerConfig fromMap(Map<String, Object> map) {
        return new McpServerConfig(
                MapHelper.requireString(map, "id", "McpServerConfig"),
                MapHelper.requireString(map, "url", "McpServerConfig"),
                MapHelper.optionalStringList(map, "capabilities"),
                MapHelper.optionalStringMap(map, "env")
        );
    }
}
