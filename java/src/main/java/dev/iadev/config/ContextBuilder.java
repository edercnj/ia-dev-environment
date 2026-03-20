package dev.iadev.config;

import dev.iadev.model.ProjectConfig;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Builds a template context map from a {@link ProjectConfig}.
 *
 * <p>Produces exactly 25 fields matching the TypeScript
 * {@code buildDefaultContext()} function (RULE-010). Boolean values
 * are converted to Python-style strings ("True"/"False") per
 * RULE-002 for Jinja2/Pebble template rendering parity.
 *
 * <p>The 25 context fields are:
 * <ol>
 *   <li>project_name</li>
 *   <li>project_purpose</li>
 *   <li>language_name</li>
 *   <li>language_version</li>
 *   <li>framework_name</li>
 *   <li>framework_version</li>
 *   <li>build_tool</li>
 *   <li>architecture_style</li>
 *   <li>domain_driven</li>
 *   <li>event_driven</li>
 *   <li>container</li>
 *   <li>orchestrator</li>
 *   <li>templating</li>
 *   <li>iac</li>
 *   <li>registry</li>
 *   <li>api_gateway</li>
 *   <li>service_mesh</li>
 *   <li>database_name</li>
 *   <li>cache_name</li>
 *   <li>smoke_tests</li>
 *   <li>contract_tests</li>
 *   <li>performance_tests</li>
 *   <li>coverage_line</li>
 *   <li>coverage_branch</li>
 *   <li>interfaces_list</li>
 * </ol>
 *
 * <p>Example usage:
 * <pre>{@code
 * Map<String, Object> context =
 *     ContextBuilder.buildContext(projectConfig);
 * }</pre>
 *
 * @see ProjectConfig
 */
public final class ContextBuilder {

    /**
     * Initial capacity for the context map, optimized to avoid
     * rehash with approximately 20 context entries.
     */
    private static final int INITIAL_CONTEXT_CAPACITY = 32;

    private static final String PYTHON_TRUE = "True";
    private static final String PYTHON_FALSE = "False";

    private ContextBuilder() {
        // utility class
    }

    /**
     * Builds a context map with exactly 25 template fields from
     * the given {@link ProjectConfig}.
     *
     * <p>Field values match the TypeScript implementation.
     * Boolean fields use Python-style string conversion
     * (RULE-002).</p>
     *
     * @param config the project configuration
     * @return an ordered map with 25 template context entries
     */
    public static Map<String, Object> buildContext(
            ProjectConfig config) {
        Map<String, Object> ctx =
                new LinkedHashMap<>(INITIAL_CONTEXT_CAPACITY);

        // Project identity (fields 1-2)
        ctx.put("project_name", config.project().name());
        ctx.put("project_purpose", config.project().purpose());

        // Language (fields 3-4)
        ctx.put("language_name", config.language().name());
        ctx.put("language_version", config.language().version());

        // Framework (fields 5-7)
        ctx.put("framework_name", config.framework().name());
        ctx.put("framework_version", config.framework().version());
        ctx.put("build_tool", config.framework().buildTool());

        // Architecture (field 8)
        ctx.put("architecture_style",
                config.architecture().style());

        // Architecture booleans — Python-bool (fields 9-10)
        ctx.put("domain_driven",
                toPythonBool(
                        config.architecture().domainDriven()));
        ctx.put("event_driven",
                toPythonBool(
                        config.architecture().eventDriven()));

        // Infrastructure (fields 11-17)
        ctx.put("container",
                config.infrastructure().container());
        ctx.put("orchestrator",
                config.infrastructure().orchestrator());
        ctx.put("templating",
                config.infrastructure().templating());
        ctx.put("iac",
                config.infrastructure().iac());
        ctx.put("registry",
                config.infrastructure().registry());
        ctx.put("api_gateway",
                config.infrastructure().apiGateway());
        ctx.put("service_mesh",
                config.infrastructure().serviceMesh());

        // Data (fields 18-19)
        ctx.put("database_name",
                config.data().database().name());
        ctx.put("cache_name",
                config.data().cache().name());

        // Testing booleans — Python-bool (fields 20-22)
        ctx.put("smoke_tests",
                toPythonBool(config.testing().smokeTests()));
        ctx.put("contract_tests",
                toPythonBool(config.testing().contractTests()));
        ctx.put("performance_tests",
                toPythonBool(
                        config.testing().performanceTests()));

        // Testing numeric (fields 23-24)
        ctx.put("coverage_line",
                config.testing().coverageLine());
        ctx.put("coverage_branch",
                config.testing().coverageBranch());

        // Interfaces list (field 25)
        String interfacesList = config.interfaces().stream()
                .map(i -> i.type())
                .collect(Collectors.joining(", "));
        ctx.put("interfaces_list",
                interfacesList.isEmpty() ? "none" : interfacesList);

        return ctx;
    }

    /**
     * Converts a boolean to a Python-style string representation.
     *
     * <p>Returns "True" for {@code true} and "False" for
     * {@code false}, matching Python's {@code str(bool)} output
     * for Jinja2/Pebble template rendering parity (RULE-002).</p>
     *
     * @param value the boolean value
     * @return "True" or "False"
     */
    static String toPythonBool(boolean value) {
        return value ? PYTHON_TRUE : PYTHON_FALSE;
    }
}
