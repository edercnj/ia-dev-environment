package dev.iadev.config;

import dev.iadev.model.ProjectConfig;
import org.yaml.snakeyaml.Yaml;

import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Provides pre-defined configuration profiles for the 8 bundled
 * technology stacks.
 *
 * <p>Each profile is loaded from a YAML config template on the
 * classpath (e.g., {@code config-templates/setup-config.java-spring.yaml})
 * and converted to a {@link ProjectConfig} via {@code fromMap()}.
 * Profiles are loaded lazily and cached for subsequent access.
 *
 * <p>Supported stacks:
 * <ul>
 *   <li>java-quarkus</li>
 *   <li>java-spring</li>
 *   <li>python-fastapi</li>
 *   <li>python-click-cli</li>
 *   <li>go-gin</li>
 *   <li>kotlin-ktor</li>
 *   <li>typescript-nestjs</li>
 *   <li>rust-axum</li>
 * </ul>
 *
 * <p>Example usage:
 * <pre>{@code
 * ProjectConfig config = ConfigProfiles.getStack("java-spring");
 * List<String> stacks = ConfigProfiles.getAvailableStacks();
 * }</pre>
 *
 * @see ProjectConfig
 * @see ConfigLoader
 */
public final class ConfigProfiles {

    private static final List<String> STACK_KEYS = List.of(
            "java-quarkus",
            "java-spring",
            "python-fastapi",
            "python-click-cli",
            "go-gin",
            "kotlin-ktor",
            "typescript-nestjs",
            "rust-axum"
    );

    private static final String TEMPLATE_PATH_PREFIX =
            "config-templates/setup-config.";

    private static final String TEMPLATE_PATH_SUFFIX = ".yaml";

    private static final Map<String, ProjectConfig> CACHE =
            new ConcurrentHashMap<>();

    private ConfigProfiles() {
        // utility class
    }

    /**
     * Returns the pre-defined {@link ProjectConfig} for the given
     * stack key.
     *
     * <p>Loads the config from the classpath YAML template on first
     * access and caches for subsequent calls.</p>
     *
     * @param stackKey the stack identifier (e.g., "java-spring")
     * @return the pre-defined ProjectConfig for that stack
     * @throws IllegalArgumentException if the stack key is unknown
     */
    public static ProjectConfig getStack(String stackKey) {
        if (!isValidStack(stackKey)) {
            throw new IllegalArgumentException(
                    "Unknown stack: '%s'. Valid stacks: %s"
                            .formatted(stackKey, STACK_KEYS));
        }
        return CACHE.computeIfAbsent(stackKey,
                ConfigProfiles::loadFromClasspath);
    }

    /**
     * Returns the list of all available stack keys.
     *
     * @return unmodifiable list of 8 stack key strings
     */
    public static List<String> getAvailableStacks() {
        return STACK_KEYS;
    }

    /**
     * Checks whether the given stack key is a known profile.
     *
     * @param stackKey the stack key to check
     * @return true if the key is valid, false otherwise
     */
    public static boolean isValidStack(String stackKey) {
        return stackKey != null && STACK_KEYS.contains(stackKey);
    }

    @SuppressWarnings("unchecked")
    private static ProjectConfig loadFromClasspath(String stackKey) {
        String resourcePath = TEMPLATE_PATH_PREFIX
                + stackKey + TEMPLATE_PATH_SUFFIX;

        try (InputStream is = ConfigProfiles.class
                .getClassLoader()
                .getResourceAsStream(resourcePath)) {

            if (is == null) {
                throw new IllegalStateException(
                        "Config template not found on classpath: "
                                + resourcePath);
            }

            Object parsed = new Yaml().load(is);
            if (!(parsed instanceof Map<?, ?> map)) {
                throw new IllegalStateException(
                        "Config template is not a valid YAML map: "
                                + resourcePath);
            }

            return ProjectConfig.fromMap(
                    (Map<String, Object>) map);

        } catch (java.io.IOException e) {
            throw new IllegalStateException(
                    "Failed to read config template: "
                            + resourcePath, e);
        }
    }
}
