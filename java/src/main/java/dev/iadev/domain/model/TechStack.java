package dev.iadev.domain.model;

import java.util.Map;

/**
 * Optional technical stack of a {@link ProjectConfig}.
 *
 * <p>Bundles the five optional configuration sections — data,
 * infrastructure, security, testing, and mcp — that may be
 * absent from the YAML input. Each field falls back to the
 * corresponding sub-config's {@code fromMap(Map.of())} default
 * when the section is missing.</p>
 *
 * <p>Extracted by EPIC-0044 (audit finding M-003) to keep the
 * root {@link ProjectConfig} aggregate within the 4-parameter
 * guideline of Rule 03.</p>
 *
 * @param data the data layer config (optional, defaults to empty)
 * @param infrastructure the infrastructure config (optional,
 *     defaults)
 * @param security the security config (optional, defaults
 *     to empty)
 * @param testing the testing config (optional, defaults: 95/90)
 * @param mcp the MCP config (optional, defaults to empty)
 */
public record TechStack(
        DataConfig data,
        InfraConfig infrastructure,
        SecurityConfig security,
        TestingConfig testing,
        McpConfig mcp) {

    /**
     * Creates a TechStack from a YAML-parsed root map.
     *
     * <p>Each optional section falls back to the corresponding
     * sub-config's empty-map default when absent.</p>
     *
     * @param root the root map from YAML deserialization
     * @return a new TechStack instance with defaults for any
     *     missing section
     */
    public static TechStack fromMap(Map<String, Object> root) {
        return new TechStack(
                DataConfig.fromMap(MapHelper
                        .optionalMap(root, "data")),
                InfraConfig.fromMap(MapHelper
                        .optionalMap(root, "infrastructure")),
                SecurityConfig.fromMap(MapHelper
                        .optionalMap(root, "security")),
                TestingConfig.fromMap(MapHelper
                        .optionalMap(root, "testing")),
                McpConfig.fromMap(MapHelper
                        .optionalMap(root, "mcp")));
    }
}
