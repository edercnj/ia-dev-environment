package dev.iadev.domain.model;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Root aggregate containing the complete project configuration.
 *
 * <p>Assembled from three cohesive sub-records: {@link CoreStack}
 * (required sections), {@link TechStack} (optional sections) and
 * {@link Governance} (policy / meta fields). Every legacy
 * accessor ({@link #project()}, {@link #architecture()}, ...,
 * {@link #telemetryEnabled()}) is preserved as a one-line
 * delegator so no caller needed to change when EPIC-0044 /
 * audit finding M-003 split the aggregate.</p>
 *
 * <p>Parsing is driven by {@link #fromMap(Map)}, which delegates
 * to each sub-record's {@code fromMap()} factory. Required
 * sections (project, architecture, interfaces, language,
 * framework) throw {@link ConfigValidationException} if missing;
 * optional sections use the sub-record defaults.</p>
 *
 * <p>Example fromMap usage:
 * <pre>{@code
 * Map<String, Object> yamlRoot = snakeYaml.load(configFile);
 * ProjectConfig config = ProjectConfig.fromMap(yamlRoot);
 * }</pre>
 * </p>
 *
 * @param core the required core stack (project, architecture,
 *     interfaces, language, framework)
 * @param tech the optional technical stack (data,
 *     infrastructure, security, testing, mcp)
 * @param governance the policy / meta block (compliance,
 *     platforms, branching model, telemetry flag)
 */
public record ProjectConfig(
        CoreStack core,
        TechStack tech,
        Governance governance) {

    private static final String DEFAULT_COMPLIANCE = "none";

    private static final Set<String> SUPPORTED_COMPLIANCE =
            Set.of("none", "pci-dss");

    /**
     * Backward-compatible 14-argument constructor. Delegates to
     * the canonical 3-component form by grouping fields into the
     * three sub-records.
     */
    public ProjectConfig(
            ProjectIdentity project,
            ArchitectureConfig architecture,
            List<InterfaceConfig> interfaces,
            LanguageConfig language,
            FrameworkConfig framework,
            DataConfig data,
            InfraConfig infrastructure,
            SecurityConfig security,
            TestingConfig testing,
            McpConfig mcp,
            String compliance,
            Set<Platform> platforms,
            BranchingModel branchingModel,
            boolean telemetryEnabled) {
        this(new CoreStack(project, architecture, interfaces,
                        language, framework),
                new TechStack(data, infrastructure, security,
                        testing, mcp),
                new Governance(compliance, platforms,
                        branchingModel, telemetryEnabled));
    }

    /**
     * Backward-compatible convenience constructor for
     * pre-EPIC-0040 call sites that did not specify
     * {@code telemetryEnabled}. Defaults the flag to
     * {@code true}.
     */
    public ProjectConfig(
            ProjectIdentity project,
            ArchitectureConfig architecture,
            List<InterfaceConfig> interfaces,
            LanguageConfig language,
            FrameworkConfig framework,
            DataConfig data,
            InfraConfig infrastructure,
            SecurityConfig security,
            TestingConfig testing,
            McpConfig mcp,
            String compliance,
            Set<Platform> platforms,
            BranchingModel branchingModel) {
        this(project, architecture, interfaces, language,
                framework, data, infrastructure, security,
                testing, mcp, compliance, platforms,
                branchingModel, true);
    }

    // --- Delegating accessors (zero caller impact) ---

    public ProjectIdentity project() { return core.project(); }
    public ArchitectureConfig architecture() { return core.architecture(); }
    public List<InterfaceConfig> interfaces() { return core.interfaces(); }
    public LanguageConfig language() { return core.language(); }
    public FrameworkConfig framework() { return core.framework(); }
    public DataConfig data() { return tech.data(); }
    public InfraConfig infrastructure() { return tech.infrastructure(); }
    public SecurityConfig security() { return tech.security(); }
    public TestingConfig testing() { return tech.testing(); }
    public McpConfig mcp() { return tech.mcp(); }
    public String compliance() { return governance.compliance(); }
    public Set<Platform> platforms() { return governance.platforms(); }
    public BranchingModel branchingModel() { return governance.branchingModel(); }
    public boolean telemetryEnabled() { return governance.telemetryEnabled(); }

    // --- Convenience accessors (Law of Demeter) ---

    /**
     * Returns the observability tool name, breaking the
     * 3-level chain {@code infrastructure().observability().tool()}.
     *
     * @return the observability tool (e.g. "prometheus", "none")
     */
    public String observabilityTool() {
        return infrastructure().observability().tool();
    }

    /**
     * Returns the observability tracing backend, breaking the
     * 3-level chain {@code infrastructure().observability().tracing()}.
     *
     * @return the tracing backend (e.g. "jaeger", "none")
     */
    public String observabilityTracing() {
        return infrastructure().observability().tracing();
    }

    /**
     * Returns the observability metrics backend, breaking the
     * 3-level chain {@code infrastructure().observability().metrics()}.
     *
     * @return the metrics backend (e.g. "micrometer", "none")
     */
    public String observabilityMetrics() {
        return infrastructure().observability().metrics();
    }

    /**
     * Returns the database name, breaking the
     * 3-level chain {@code data().database().name()}.
     *
     * @return the database name (e.g. "postgresql", "none")
     */
    public String databaseName() {
        return data().database().name();
    }

    /**
     * Returns the migration tool name, breaking the
     * 3-level chain {@code data().migration().name()}.
     *
     * @return the migration tool name (e.g. "flyway", "none")
     */
    public String migrationName() {
        return data().migration().name();
    }

    /**
     * Returns the cache name, breaking the
     * 3-level chain {@code data().cache().name()}.
     *
     * @return the cache name (e.g. "redis", "none")
     */
    public String cacheName() {
        return data().cache().name();
    }

    /**
     * Returns the base branch name for the configured
     * branching model.
     *
     * @return "develop" for GITFLOW, "main" for TRUNK
     */
    public String baseBranch() {
        return branchingModel().baseBranch();
    }

    // --- End convenience accessors ---

    /**
     * Creates a ProjectConfig from a YAML-parsed map.
     *
     * <p>Delegates to each sub-record's {@code fromMap()} factory.
     * Required sections throw if absent; optional sections use
     * defaults.</p>
     *
     * @param map the root map from YAML deserialization
     * @return a new ProjectConfig instance
     * @throws ConfigValidationException if required sections are
     *     missing
     */
    public static ProjectConfig fromMap(
            Map<String, Object> map) {
        List<InterfaceConfig> interfaceList =
                parseInterfaces(map);
        return new ProjectConfig(
                CoreStack.fromMap(map, interfaceList),
                TechStack.fromMap(map),
                Governance.fromMap(map));
    }

    @SuppressWarnings("unchecked")
    private static List<InterfaceConfig> parseInterfaces(
            Map<String, Object> map) {
        var raw = MapHelper.requireField(
                map, "interfaces", "ProjectConfig");
        if (raw instanceof List<?> list) {
            return list.stream()
                    .map(item -> InterfaceConfig.fromMap(
                            (Map<String, Object>) item))
                    .toList();
        }
        throw new ConfigValidationException(
                "interfaces", "List", "ProjectConfig");
    }

    /**
     * Parses the optional {@code telemetry.enabled} field.
     *
     * <p>Defaults to {@code true} when the {@code telemetry}
     * section is absent or when the {@code enabled} key inside
     * the section is absent. Introduced by story-0040-0004 to
     * gate the injection of telemetry hooks.</p>
     *
     * @param map the root config map
     * @return {@code true} unless {@code telemetry.enabled}
     *     is explicitly set to {@code false}
     */
    static boolean parseTelemetryEnabled(
            Map<String, Object> map) {
        Map<String, Object> telemetry = MapHelper
                .optionalMap(map, "telemetry");
        return MapHelper.optionalBoolean(
                telemetry, "enabled", true);
    }

    static String parseCompliance(Map<String, Object> map) {
        String value = MapHelper.optionalString(
                map, "compliance", DEFAULT_COMPLIANCE);
        if (!SUPPORTED_COMPLIANCE.contains(value)) {
            throw new ConfigValidationException(
                    ("Unsupported compliance value: '%s'."
                            + " Supported: none, pci-dss")
                            .formatted(value));
        }
        return value;
    }

    /**
     * Parses the optional {@code branching-model} field.
     *
     * <p>Defaults to {@link BranchingModel#GITFLOW} when
     * the field is absent. Case-insensitive matching.</p>
     */
    static BranchingModel parseBranchingModel(
            Map<String, Object> map) {
        String value = MapHelper.optionalString(
                map, "branching-model", null);
        if (value == null) {
            return BranchingModel.GITFLOW;
        }
        return BranchingModel.fromConfigValue(value)
                .orElseThrow(() ->
                        new ConfigValidationException(
                                ("Invalid branching-model:"
                                        + " '%s'. Accepted"
                                        + " values: gitflow,"
                                        + " trunk")
                                        .formatted(value)));
    }

    /**
     * Delegates to {@link PlatformParser} for parsing
     * the optional {@code platform} YAML field.
     */
    static Set<Platform> parsePlatforms(
            Map<String, Object> map) {
        return PlatformParser.parse(map);
    }
}
