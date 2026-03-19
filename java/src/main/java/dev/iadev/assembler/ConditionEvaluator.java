package dev.iadev.assembler;

import dev.iadev.model.ProjectConfig;

import java.util.List;
import java.util.Set;

/**
 * Evaluates feature gates to determine which assemblers and
 * artifacts should be included in the generation pipeline.
 *
 * <p>Provides convenience methods for checking interfaces,
 * database, cache, and other feature flags from the project
 * configuration.</p>
 *
 * <p>Example usage:
 * <pre>{@code
 * if (ConditionEvaluator.hasInterface(config, "grpc")) {
 *     // include gRPC-specific artifacts
 * }
 * if (ConditionEvaluator.hasDatabase(config)) {
 *     // include database-related rules
 * }
 * }</pre>
 * </p>
 */
public final class ConditionEvaluator {

    private ConditionEvaluator() {
        // Utility class — no instantiation
    }

    /**
     * Extracts the list of interface type strings from config.
     *
     * @param config the project configuration
     * @return list of interface type names
     */
    public static List<String> extractInterfaceTypes(
            ProjectConfig config) {
        return config.interfaces().stream()
                .map(iface -> iface.type())
                .toList();
    }

    /**
     * Checks if a specific interface type exists in config.
     *
     * @param config    the project configuration
     * @param ifaceType the interface type to check
     * @return true if the interface type is present
     */
    public static boolean hasInterface(
            ProjectConfig config, String ifaceType) {
        return config.interfaces().stream()
                .anyMatch(iface ->
                        iface.type().equals(ifaceType));
    }

    /**
     * Checks if any of the specified interface types exist.
     *
     * @param config the project configuration
     * @param types  the interface types to check
     * @return true if at least one type is present
     */
    public static boolean hasAnyInterface(
            ProjectConfig config, String... types) {
        Set<String> typeSet = Set.of(types);
        return config.interfaces().stream()
                .anyMatch(iface ->
                        typeSet.contains(iface.type()));
    }

    /**
     * Checks if the project has a configured database.
     *
     * @param config the project configuration
     * @return true if database name is not "none" or empty
     */
    public static boolean hasDatabase(ProjectConfig config) {
        String dbName = config.data().database().name();
        return isPresent(dbName);
    }

    /**
     * Checks if the project has a configured cache.
     *
     * @param config the project configuration
     * @return true if cache name is not "none" or empty
     */
    public static boolean hasCache(ProjectConfig config) {
        String cacheName = config.data().cache().name();
        return isPresent(cacheName);
    }

    /**
     * Checks if the project has a specific feature enabled.
     *
     * <p>Supported feature names:
     * <ul>
     *   <li>{@code domain_driven} — DDD patterns</li>
     *   <li>{@code event_driven} — Event-driven patterns</li>
     *   <li>{@code native_build} — GraalVM native build</li>
     *   <li>{@code database} — Database configured</li>
     *   <li>{@code cache} — Cache configured</li>
     * </ul>
     *
     * @param config      the project configuration
     * @param featureName the feature name to check
     * @return true if the feature is enabled/present
     */
    public static boolean hasFeature(
            ProjectConfig config, String featureName) {
        return switch (featureName) {
            case "domain_driven" ->
                    config.architecture().domainDriven();
            case "event_driven" ->
                    config.architecture().eventDriven();
            case "native_build" ->
                    config.framework().nativeBuild();
            case "database" -> hasDatabase(config);
            case "cache" -> hasCache(config);
            default -> false;
        };
    }

    /**
     * Evaluates a generic condition string against the config.
     *
     * <p>Condition formats:
     * <ul>
     *   <li>{@code interface:<type>} — checks interface</li>
     *   <li>{@code feature:<name>} — checks feature flag</li>
     *   <li>{@code <name>} — checks as feature (fallback)</li>
     * </ul>
     *
     * @param config    the project configuration
     * @param condition the condition string to evaluate
     * @return true if the condition is satisfied
     */
    public static boolean evaluate(
            ProjectConfig config, String condition) {
        if (condition.startsWith("interface:")) {
            String type = condition.substring(
                    "interface:".length());
            return hasInterface(config, type);
        }
        if (condition.startsWith("feature:")) {
            String feature = condition.substring(
                    "feature:".length());
            return hasFeature(config, feature);
        }
        return hasFeature(config, condition);
    }

    private static boolean isPresent(String value) {
        return value != null
                && !value.isEmpty()
                && !"none".equals(value);
    }
}
