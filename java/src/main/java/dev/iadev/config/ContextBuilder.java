package dev.iadev.config;

import dev.iadev.domain.model.ReviewChecklistScore;
import dev.iadev.domain.model.ReviewChecklistSections;
import dev.iadev.domain.stack.ProtocolMapping;
import dev.iadev.domain.model.ProjectConfig;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Builds a template context map from a {@link ProjectConfig}.
 *
 * <p>Produces exactly 39 fields matching the TypeScript
 * {@code buildDefaultContext()} function (RULE-010). Boolean values
 * are converted to Python-style strings ("True"/"False") per
 * RULE-002 for Jinja2/Pebble template rendering parity.
 *
 * <p>The 39 context fields are:
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
 *   <li>validate_with_archunit</li>
 *   <li>base_package</li>
 *   <li>event_store</li>
 *   <li>events_per_snapshot</li>
 *   <li>ddd_enabled</li>
 *   <li>container</li>
 *   <li>orchestrator</li>
 *   <li>templating</li>
 *   <li>iac</li>
 *   <li>registry</li>
 *   <li>api_gateway</li>
 *   <li>service_mesh</li>
 *   <li>database_name</li>
 *   <li>migration_name</li>
 *   <li>cache_name</li>
 *   <li>message_broker</li>
 *   <li>smoke_tests</li>
 *   <li>contract_tests</li>
 *   <li>performance_tests</li>
 *   <li>coverage_line</li>
 *   <li>coverage_branch</li>
 *   <li>interfaces_list</li>
 *   <li>has_event_interface</li>
 *   <li>has_pci_dss</li>
 *   <li>has_lgpd</li>
 *   <li>review_max_score</li>
 *   <li>review_go_threshold</li>
 *   <li>review_conditional_rubric</li>
 *   <li>review_conditional_criteria</li>
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
     * Builds a context map with exactly 39 template fields from
     * the given {@link ProjectConfig}.
     *
     * <p>Delegates to domain-specific builders for each
     * section of the context map.</p>
     *
     * @param config the project configuration
     * @return an ordered map with 39 template context entries
     */
    public static Map<String, Object> buildContext(
            ProjectConfig config) {
        Map<String, Object> ctx =
                new LinkedHashMap<>(INITIAL_CONTEXT_CAPACITY);

        buildIdentity(config, ctx);
        buildLanguage(config, ctx);
        buildFramework(config, ctx);
        buildArchitecture(config, ctx);
        buildInfrastructure(config, ctx);
        buildData(config, ctx);
        buildTesting(config, ctx);
        buildInterfaces(config, ctx);
        buildReviewChecklist(config, ctx);

        return ctx;
    }

    private static void buildIdentity(
            ProjectConfig config,
            Map<String, Object> ctx) {
        ctx.put("project_name", config.project().name());
        ctx.put("project_purpose",
                config.project().purpose());
    }

    private static void buildLanguage(
            ProjectConfig config,
            Map<String, Object> ctx) {
        ctx.put("language_name", config.language().name());
        ctx.put("language_version",
                config.language().version());
    }

    private static void buildFramework(
            ProjectConfig config,
            Map<String, Object> ctx) {
        ctx.put("framework_name",
                config.framework().name());
        ctx.put("framework_version",
                config.framework().version());
        ctx.put("build_tool", config.framework().buildTool());
    }

    private static void buildArchitecture(
            ProjectConfig config,
            Map<String, Object> ctx) {
        ctx.put("architecture_style",
                config.architecture().style());
        ctx.put("domain_driven",
                toPythonBool(
                        config.architecture().domainDriven()));
        ctx.put("event_driven",
                toPythonBool(
                        config.architecture().eventDriven()));
        ctx.put("validate_with_archunit",
                toPythonBool(
                        config.architecture()
                                .validateWithArchUnit()));
        ctx.put("base_package",
                config.architecture().basePackage());
        ctx.put("event_store",
                config.architecture().eventStore());
        ctx.put("events_per_snapshot",
                config.architecture().eventsPerSnapshot());
        ctx.put("ddd_enabled",
                toPythonBool(
                        config.architecture().dddEnabled()));
    }

    private static void buildInfrastructure(
            ProjectConfig config,
            Map<String, Object> ctx) {
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
    }

    private static void buildData(
            ProjectConfig config,
            Map<String, Object> ctx) {
        ctx.put("database_name", config.databaseName());
        ctx.put("migration_name", config.migrationName());
        ctx.put("cache_name", config.cacheName());
        String broker =
                ProtocolMapping.extractBroker(config);
        ctx.put("message_broker",
                broker.isEmpty() ? "none" : broker);
    }

    private static void buildTesting(
            ProjectConfig config,
            Map<String, Object> ctx) {
        ctx.put("smoke_tests",
                toPythonBool(config.testing().smokeTests()));
        ctx.put("contract_tests",
                toPythonBool(
                        config.testing().contractTests()));
        ctx.put("performance_tests",
                toPythonBool(
                        config.testing().performanceTests()));
        ctx.put("coverage_line",
                config.testing().coverageLine());
        ctx.put("coverage_branch",
                config.testing().coverageBranch());
    }

    private static void buildInterfaces(
            ProjectConfig config,
            Map<String, Object> ctx) {
        String interfacesList = config.interfaces().stream()
                .map(i -> i.type())
                .collect(Collectors.joining(", "));
        ctx.put("interfaces_list",
                interfacesList.isEmpty()
                        ? "none" : interfacesList);
    }

    private static void buildReviewChecklist(
            ProjectConfig config,
            Map<String, Object> ctx) {
        boolean hasEvent = hasEventInterface(config);
        boolean hasPciDss = config.security().frameworks()
                .contains("pci-dss");
        boolean hasLgpd = config.security().frameworks()
                .contains("lgpd");
        ctx.put("has_event_interface",
                toPythonBool(hasEvent));
        ctx.put("has_pci_dss",
                toPythonBool(hasPciDss));
        ctx.put("has_lgpd",
                toPythonBool(hasLgpd));
        ReviewChecklistScore score =
                ReviewChecklistScore.compute(
                        hasEvent, hasPciDss, hasLgpd);
        ctx.put("review_max_score", score.maxScore());
        ctx.put("review_go_threshold",
                score.goThreshold());
        ctx.put("review_conditional_rubric",
                ReviewChecklistSections.buildRubricRows(
                        hasEvent, hasPciDss, hasLgpd));
        ctx.put("review_conditional_criteria",
                ReviewChecklistSections.buildDetailedCriteria(
                        hasEvent, hasPciDss, hasLgpd));
    }

    private static boolean hasEventInterface(
            ProjectConfig config) {
        return config.interfaces().stream()
                .anyMatch(i -> i.type().contains("event"));
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
