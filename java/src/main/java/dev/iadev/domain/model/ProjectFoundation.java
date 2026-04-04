package dev.iadev.domain.model;

/**
 * Represents the foundational metadata of the ia-dev-env project itself.
 *
 * <p>This is not a user-configurable model but rather the project's own identity
 * used for version display and internal references.</p>
 *
 * <p>Example:
 * <pre>{@code
 * var foundation = ProjectFoundation.DEFAULT;
 * // foundation.name() == "ia-dev-environment"
 * }</pre>
 * </p>
 *
 * @param name the project name
 * @param version the project version
 * @param moduleType the module type (always "module")
 */
public record ProjectFoundation(
        String name,
        String version,
        String moduleType) {

    /** The default project foundation matching the TypeScript implementation. */
    public static final ProjectFoundation DEFAULT = new ProjectFoundation(
            "ia-dev-environment",
            "0.1.0",
            "module"
    );
}
