package dev.iadev.domain.model;

import java.util.List;
import java.util.Map;

/**
 * Required core stack of a {@link ProjectConfig}.
 *
 * <p>Bundles the five required configuration sections — project
 * identity, architecture, interfaces, language, and framework —
 * that must be present in every YAML input. Optional sections
 * (data, infrastructure, security, testing, mcp) live in
 * {@link TechStack}; policy / meta sections in {@link Governance}.</p>
 *
 * <p>Extracted by EPIC-0044 (audit finding M-003) to keep the
 * root {@link ProjectConfig} aggregate within the 4-parameter
 * guideline of Rule 03.</p>
 *
 * @param project the project identity (required)
 * @param architecture the architecture config (required)
 * @param interfaces the list of interface configs
 *     (required, immutable)
 * @param language the language config (required)
 * @param framework the framework config (required)
 */
public record CoreStack(
        ProjectIdentity project,
        ArchitectureConfig architecture,
        List<InterfaceConfig> interfaces,
        LanguageConfig language,
        FrameworkConfig framework) {

    /**
     * Compact constructor enforcing immutability of the
     * {@code interfaces} list.
     */
    public CoreStack {
        interfaces = List.copyOf(interfaces);
    }

    /**
     * Creates a CoreStack from a YAML-parsed root map using the
     * pre-parsed interface list.
     *
     * <p>The interface list is passed in separately because YAML
     * input stores it as a top-level list of maps, which requires
     * an unchecked cast performed by the caller.</p>
     *
     * @param root the root map from YAML deserialization
     * @param interfaces the pre-parsed interface list
     * @return a new CoreStack instance
     * @throws ConfigValidationException if any required section
     *     is missing
     */
    public static CoreStack fromMap(
            Map<String, Object> root,
            List<InterfaceConfig> interfaces) {
        return new CoreStack(
                ProjectIdentity.fromMap(MapHelper
                        .requireMap(root, "project",
                                "ProjectConfig")),
                ArchitectureConfig.fromMap(MapHelper
                        .requireMap(root, "architecture",
                                "ProjectConfig")),
                interfaces,
                LanguageConfig.fromMap(MapHelper
                        .requireMap(root, "language",
                                "ProjectConfig")),
                FrameworkConfig.fromMap(MapHelper
                        .requireMap(root, "framework",
                                "ProjectConfig")));
    }
}
