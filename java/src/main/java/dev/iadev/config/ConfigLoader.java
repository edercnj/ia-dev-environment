package dev.iadev.config;

import dev.iadev.exception.ConfigParseException;
import dev.iadev.exception.ConfigValidationException;
import dev.iadev.model.ProjectConfig;
import org.yaml.snakeyaml.Yaml;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Loads and validates YAML configuration files using SnakeYAML.
 *
 * <p>Reads a YAML file from the filesystem, validates that all
 * required sections are present, applies shorthand type mappings
 * (e.g., "api" to microservice+rest), and returns a fully
 * populated {@link ProjectConfig} via {@code fromMap()}.
 *
 * <p>SnakeYAML is used exclusively in this class. All downstream
 * consumers receive typed {@link ProjectConfig} instances
 * (RULE-007: zero framework dependency in domain).
 *
 * <p>Example usage:
 * <pre>{@code
 * ProjectConfig config = ConfigLoader.loadConfig("config.yaml");
 * }</pre>
 *
 * @see ProjectConfig
 * @see ConfigValidationException
 * @see ConfigParseException
 */
public final class ConfigLoader {

    /**
     * Required top-level sections in the YAML configuration.
     */
    static final List<String> REQUIRED_SECTIONS = List.of(
            "project",
            "architecture",
            "interfaces",
            "language",
            "framework"
    );

    /**
     * Shorthand type mappings that resolve a project type to
     * architecture style and interfaces.
     */
    private static final Map<String, ShorthandMapping>
            TYPE_MAPPING = Map.of(
            "api", new ShorthandMapping(
                    "microservice",
                    List.of(Map.of("type", "rest"))),
            "cli", new ShorthandMapping(
                    "library",
                    List.of(Map.of("type", "cli"))),
            "library", new ShorthandMapping(
                    "library",
                    List.of()),
            "worker", new ShorthandMapping(
                    "microservice",
                    List.of(Map.of("type", "event-consumer"))),
            "fullstack", new ShorthandMapping(
                    "monolith",
                    List.of(Map.of("type", "rest")))
    );

    private ConfigLoader() {
        // utility class
    }

    /**
     * Loads a YAML configuration file and returns a validated
     * {@link ProjectConfig}.
     *
     * <p>Processing steps:
     * <ol>
     *   <li>Read file content as UTF-8</li>
     *   <li>Parse with SnakeYAML</li>
     *   <li>Validate root is a map (not null, scalar, or array)</li>
     *   <li>Apply shorthand type mapping if "type" is present
     *       in the "project" section</li>
     *   <li>Validate required sections</li>
     *   <li>Delegate to {@code ProjectConfig.fromMap()}</li>
     * </ol>
     *
     * @param filePath path to the YAML configuration file
     * @return a validated ProjectConfig instance
     * @throws ConfigParseException if the file cannot be read or
     *     contains invalid YAML syntax
     * @throws ConfigValidationException if required sections are
     *     missing or fields have invalid types
     */
    @SuppressWarnings("unchecked")
    public static ProjectConfig loadConfig(String filePath) {
        String content = readFile(filePath);
        Object parsed = parseYaml(content, filePath);
        Map<String, Object> data = validateRoot(parsed);
        applyShorthandMapping(data);
        validateRequiredSections(data);
        return ProjectConfig.fromMap(data);
    }

    private static String readFile(String filePath) {
        try {
            return Files.readString(
                    Path.of(filePath), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new ConfigParseException(
                    "Failed to read config file: " + filePath,
                    filePath, e);
        }
    }

    private static Object parseYaml(
            String content, String filePath) {
        try {
            return new Yaml().load(content);
        } catch (Exception e) {
            throw new ConfigParseException(
                    "Failed to parse YAML: " + e.getMessage(),
                    filePath, e);
        }
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> validateRoot(Object parsed) {
        if (parsed == null
                || !(parsed instanceof Map<?, ?>)) {
            throw new ConfigValidationException(
                    "Config must be a YAML mapping with required "
                            + "sections: " + REQUIRED_SECTIONS,
                    new ArrayList<>(REQUIRED_SECTIONS));
        }
        return (Map<String, Object>) parsed;
    }

    @SuppressWarnings("unchecked")
    private static void applyShorthandMapping(
            Map<String, Object> data) {
        Object projectObj = data.get("project");
        if (!(projectObj instanceof Map<?, ?> projectMap)) {
            return;
        }

        Object typeObj = ((Map<String, Object>) projectMap)
                .get("type");
        if (!(typeObj instanceof String typeKey)) {
            return;
        }

        ShorthandMapping mapping = TYPE_MAPPING.get(typeKey);
        if (mapping == null) {
            return;
        }

        if (!data.containsKey("architecture")) {
            data.put("architecture",
                    Map.of("style", mapping.style()));
        }
        if (!data.containsKey("interfaces")) {
            data.put("interfaces", mapping.interfaces());
        }
    }

    private static void validateRequiredSections(
            Map<String, Object> data) {
        List<String> missing = new ArrayList<>();
        for (String section : REQUIRED_SECTIONS) {
            if (!data.containsKey(section)
                    || data.get(section) == null) {
                missing.add(section);
            }
        }
        if (!missing.isEmpty()) {
            throw new ConfigValidationException(
                    "Missing required config sections: " + missing,
                    missing);
        }
    }

    private record ShorthandMapping(
            String style,
            List<Map<String, String>> interfaces) {
    }
}
