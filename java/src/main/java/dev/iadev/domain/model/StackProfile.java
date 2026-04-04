package dev.iadev.domain.model;

import java.util.List;
import java.util.Map;

/**
 * Represents a technology stack profile configuration.
 *
 * <p>A stack profile defines the complete set of technology choices
 * for a project: language, framework, build tool, database, and
 * other infrastructure components. Profiles are immutable value
 * objects used by the generation pipeline.</p>
 *
 * <p>Domain purity: this record contains zero external library
 * imports. Only standard library types are used.</p>
 *
 * @param name        unique profile identifier (e.g., "java-spring")
 * @param language    programming language (e.g., "java")
 * @param framework   framework name (e.g., "spring")
 * @param buildTool   build tool (e.g., "maven", "gradle")
 * @param properties  additional profile properties as key-value pairs
 */
public record StackProfile(
        String name,
        String language,
        String framework,
        String buildTool,
        Map<String, Object> properties
) {

    /**
     * Creates a StackProfile with defensive copies of mutable inputs.
     */
    public StackProfile {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException(
                    "StackProfile name must not be null or blank");
        }
        properties = properties == null
                ? Map.of()
                : Map.copyOf(properties);
    }
}
