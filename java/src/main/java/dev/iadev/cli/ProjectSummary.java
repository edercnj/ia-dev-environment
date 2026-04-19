package dev.iadev.cli;

import java.util.List;
import java.util.Objects;

/**
 * Immutable parameter object capturing the results of
 * interactive project prompts.
 *
 * <p>Internally organised into four cohesive clusters
 * ({@link Identity}, {@link Stack}, {@link Platform},
 * {@link Architecture}) to stay below the RULE-003 4-param
 * threshold while preserving every legacy accessor. Each
 * legacy accessor is delegated to the corresponding nested
 * record so existing callers compile unchanged.</p>
 *
 * <p>This record is in-memory only (never serialized) so no
 * Jackson compatibility concern applies.</p>
 *
 * @param identity     the project identity cluster
 * @param stack        the language / framework / build tool
 * @param platform     the interface + data-layer cluster
 * @param architecture the architecture-style cluster
 */
public record ProjectSummary(
        Identity identity,
        Stack stack,
        Platform platform,
        Architecture architecture) {

    /**
     * Canonical constructor validating non-null clusters.
     */
    public ProjectSummary {
        Objects.requireNonNull(identity,
                "identity must not be null");
        Objects.requireNonNull(stack,
                "stack must not be null");
        Objects.requireNonNull(platform,
                "platform must not be null");
        Objects.requireNonNull(architecture,
                "architecture must not be null");
    }

    /**
     * Flat 12-argument constructor kept for backward
     * compatibility with every existing caller. Groups the
     * flat parameters into the four sub-records internally.
     */
    public ProjectSummary(
            String name,
            String purpose,
            String archStyle,
            String language,
            String framework,
            String buildTool,
            List<String> interfaces,
            String database,
            String cache,
            String archPatternStyle,
            boolean validateArchUnit,
            List<String> compliance) {
        this(
                new Identity(name, purpose),
                new Stack(language, framework, buildTool),
                new Platform(interfaces, database, cache),
                new Architecture(
                        archStyle, archPatternStyle,
                        validateArchUnit, compliance));
    }

    /**
     * Flat 9-argument constructor kept for backward
     * compatibility with legacy callers (pre-archPattern).
     */
    public ProjectSummary(
            String name, String purpose,
            String archStyle, String language,
            String framework, String buildTool,
            List<String> interfaces,
            String database, String cache) {
        this(name, purpose, archStyle, language,
                framework, buildTool, interfaces,
                database, cache, "", false, List.of());
    }

    /** @return the project name. */
    public String name() {
        return identity.name();
    }

    /** @return the project purpose description. */
    public String purpose() {
        return identity.purpose();
    }

    /** @return the architecture style. */
    public String archStyle() {
        return architecture.archStyle();
    }

    /** @return the programming language. */
    public String language() {
        return stack.language();
    }

    /** @return the framework name. */
    public String framework() {
        return stack.framework();
    }

    /** @return the build tool name. */
    public String buildTool() {
        return stack.buildTool();
    }

    /** @return the selected interface types. */
    public List<String> interfaces() {
        return platform.interfaces();
    }

    /** @return the database name (may be empty). */
    public String database() {
        return platform.database();
    }

    /** @return the cache name (may be empty). */
    public String cache() {
        return platform.cache();
    }

    /** @return the architecture pattern style. */
    public String archPatternStyle() {
        return architecture.archPatternStyle();
    }

    /** @return whether to validate with ArchUnit. */
    public boolean validateArchUnit() {
        return architecture.validateArchUnit();
    }

    /** @return the selected compliance frameworks. */
    public List<String> compliance() {
        return architecture.compliance();
    }

    /**
     * Project identity sub-record.
     *
     * @param name    the project name in kebab-case
     * @param purpose the project purpose description
     */
    public record Identity(String name, String purpose) {
        public Identity {
            Objects.requireNonNull(name,
                    "name must not be null");
            Objects.requireNonNull(purpose,
                    "purpose must not be null");
        }
    }

    /**
     * Technology stack sub-record.
     *
     * @param language  the programming language
     * @param framework the framework name
     * @param buildTool the build tool name
     */
    public record Stack(
            String language,
            String framework,
            String buildTool) {
        public Stack {
            Objects.requireNonNull(language,
                    "language must not be null");
            Objects.requireNonNull(framework,
                    "framework must not be null");
            Objects.requireNonNull(buildTool,
                    "buildTool must not be null");
        }
    }

    /**
     * Platform sub-record grouping interface types and the
     * data-layer choices.
     *
     * @param interfaces the selected interface types
     * @param database   the database name or empty string
     * @param cache      the cache name or empty string
     */
    public record Platform(
            List<String> interfaces,
            String database,
            String cache) {
        public Platform {
            Objects.requireNonNull(interfaces,
                    "interfaces must not be null");
            Objects.requireNonNull(database,
                    "database must not be null");
            Objects.requireNonNull(cache,
                    "cache must not be null");
            interfaces = List.copyOf(interfaces);
        }
    }

    /**
     * Architecture-style sub-record.
     *
     * @param archStyle        the architecture style
     * @param archPatternStyle the architecture pattern
     *                         (empty if not applicable)
     * @param validateArchUnit whether to validate via ArchUnit
     * @param compliance       the selected compliance frameworks
     */
    public record Architecture(
            String archStyle,
            String archPatternStyle,
            boolean validateArchUnit,
            List<String> compliance) {
        public Architecture {
            Objects.requireNonNull(archStyle,
                    "archStyle must not be null");
            Objects.requireNonNull(archPatternStyle,
                    "archPatternStyle must not be null");
            Objects.requireNonNull(compliance,
                    "compliance must not be null");
            compliance = List.copyOf(compliance);
        }
    }
}
