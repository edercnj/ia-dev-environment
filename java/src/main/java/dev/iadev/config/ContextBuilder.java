package dev.iadev.config;

import dev.iadev.application.assembler.PlatformContextBuilder;
import dev.iadev.domain.model.Platform;
import dev.iadev.domain.stack.ProtocolMapping;
import dev.iadev.domain.model.ProjectConfig;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Builds a template context map from a
 * {@link ProjectConfig}.
 *
 * <p>Produces exactly 46 fields (was 44 before branching model) matching the TypeScript
 * {@code buildDefaultContext()} function (RULE-010).
 * Architecture and review checklist sections are
 * delegated to {@link ContextArchitectureBuilder}.</p>
 *
 * @see ProjectConfig
 * @see ContextArchitectureBuilder
 */
public final class ContextBuilder {

    private static final int INITIAL_CONTEXT_CAPACITY = 64;

    private static final String PYTHON_TRUE = "True";
    private static final String PYTHON_FALSE = "False";

    private static final Set<String>
            CONTRACT_INTERFACE_TYPES = Set.of(
                    "rest", "grpc",
                    "event-consumer", "event-producer",
                    "websocket");

    private ContextBuilder() {
        // utility class
    }

    /**
     * Builds a context map with exactly 46 template fields.
     *
     * @param config the project configuration
     * @return an ordered map with 44 template context entries
     */
    public static Map<String, Object> buildContext(
            ProjectConfig config) {
        Map<String, Object> ctx =
                new LinkedHashMap<>(
                        INITIAL_CONTEXT_CAPACITY);

        buildIdentity(config, ctx);
        buildLanguage(config, ctx);
        buildFramework(config, ctx);
        ContextArchitectureBuilder
                .buildArchitecture(config, ctx);
        buildInfrastructure(config, ctx);
        buildData(config, ctx);
        buildTesting(config, ctx);
        buildInterfaces(config, ctx);
        ContextArchitectureBuilder
                .buildReviewChecklist(config, ctx);

        return ctx;
    }

    private static void buildIdentity(
            ProjectConfig config,
            Map<String, Object> ctx) {
        ctx.put("project_name",
                config.project().name());
        ctx.put("project_purpose",
                config.project().purpose());
        ctx.put("compliance",
                config.compliance());
        ctx.put("branching_model",
                config.branchingModel().configValue());
        ctx.put("base_branch",
                config.baseBranch());
    }

    private static void buildLanguage(
            ProjectConfig config,
            Map<String, Object> ctx) {
        ctx.put("language_name",
                config.language().name());
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
        ctx.put("build_tool",
                config.framework().buildTool());
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
        ctx.put("database_name",
                config.databaseName());
        ctx.put("migration_name",
                config.migrationName());
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
                toPythonBool(
                        config.testing().smokeTests()));
        ctx.put("contract_tests",
                toPythonBool(
                        config.testing().contractTests()));
        ctx.put("performance_tests",
                toPythonBool(
                        config.testing()
                                .performanceTests()));
        ctx.put("coverage_line",
                config.testing().coverageLine());
        ctx.put("coverage_branch",
                config.testing().coverageBranch());
    }

    private static void buildInterfaces(
            ProjectConfig config,
            Map<String, Object> ctx) {
        String interfacesList =
                config.interfaces().stream()
                        .map(i -> i.type())
                        .collect(Collectors.joining(", "));
        ctx.put("interfaces_list",
                interfacesList.isEmpty()
                        ? "none" : interfacesList);
        ctx.put("has_contract_interfaces",
                toPythonBool(
                        hasContractInterfaces(config)));
    }

    private static boolean hasContractInterfaces(
            ProjectConfig config) {
        return config.interfaces().stream()
                .anyMatch(i ->
                        CONTRACT_INTERFACE_TYPES
                                .contains(i.type()));
    }

    /**
     * Converts a boolean to Python-style string.
     *
     * @param value the boolean value
     * @return "True" or "False"
     */
    static String toPythonBool(boolean value) {
        return value ? PYTHON_TRUE : PYTHON_FALSE;
    }

    /**
     * Builds platform-aware context variables.
     *
     * <p>Produces flags (hasClaude, hasCopilot, hasCodex,
     * isMultiPlatform) and a platforms list with CLI
     * names. Delegates to
     * {@link PlatformContextBuilder}.</p>
     *
     * @param platforms the active platforms (empty = all)
     * @return map with platform flags and list
     */
    public static Map<String, Object> buildPlatformContext(
            Set<Platform> platforms) {
        return PlatformContextBuilder.buildPlatformFlags(
                platforms);
    }
}
