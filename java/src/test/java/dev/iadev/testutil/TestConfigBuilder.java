package dev.iadev.testutil;

import dev.iadev.domain.model.ArchitectureConfig;
import dev.iadev.domain.model.DataConfig;
import dev.iadev.domain.model.FrameworkConfig;
import dev.iadev.domain.model.InfraConfig;
import dev.iadev.domain.model.InterfaceConfig;
import dev.iadev.domain.model.LanguageConfig;
import dev.iadev.domain.model.McpConfig;
import dev.iadev.domain.model.McpServerConfig;
import dev.iadev.domain.model.ObservabilityConfig;
import dev.iadev.domain.model.ProjectConfig;
import dev.iadev.domain.model.ProjectIdentity;
import dev.iadev.domain.model.SecurityConfig;
import dev.iadev.domain.model.TechComponent;
import dev.iadev.domain.model.TestingConfig;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Shared test helper to build ProjectConfig instances
 * with sensible defaults.
 *
 * <p>Consolidates the former package-private builders from
 * application.assembler and domain.stack packages.</p>
 */
public final class TestConfigBuilder {

    private String projectName = "test-project";
    private String purpose = "Test purpose";
    private String archStyle = "microservice";
    private boolean domainDriven = false;
    private boolean eventDriven = false;
    private boolean validateWithArchUnit = false;
    private String basePackage = "";
    private String eventStore = "eventstoredb";
    private String schemaRegistry = "";
    private boolean outboxPattern = false;
    private String deadLetterStrategy = "";
    private int eventsPerSnapshot =
            ArchitectureConfig.DEFAULT_EVENTS_PER_SNAPSHOT;
    private boolean dddEnabled = false;
    private String langName = "java";
    private String langVersion = "21";
    private String fwName = "quarkus";
    private String fwVersion = "3.17";
    private String buildTool = "maven";
    private boolean nativeBuild = false;
    private String dbName = "none";
    private String dbVersion = "";
    private String cacheName = "none";
    private String cacheVersion = "";
    private String migrationName = "none";
    private String migrationVersion = "";
    private String container = "docker";
    private String orchestrator = "none";
    private String cloudProvider = "none";
    private String iac = "none";
    private boolean smokeTests = true;
    private boolean contractTests = false;
    private boolean performanceTests = true;
    private String apiGateway = "none";
    private String registry = "none";
    private String observabilityTool = "none";
    private String serviceMesh = "none";
    private String templating = "kustomize";
    private List<String> securityFrameworksList =
            Collections.emptyList();
    private final List<InterfaceConfig> interfaces =
            new ArrayList<>();
    private final List<McpServerConfig> mcpServers =
            new ArrayList<>();

    private TestConfigBuilder() {
        interfaces.add(
                new InterfaceConfig("rest", "", ""));
    }

    public static ProjectConfig minimal() {
        return new TestConfigBuilder().build();
    }

    public static TestConfigBuilder builder() {
        return new TestConfigBuilder();
    }

    public TestConfigBuilder projectName(String name) {
        this.projectName = name;
        return this;
    }

    public TestConfigBuilder purpose(String value) {
        this.purpose = value;
        return this;
    }

    public TestConfigBuilder archStyle(String style) {
        this.archStyle = style;
        return this;
    }

    /** Alias for {@link #archStyle(String)}. */
    public TestConfigBuilder architectureStyle(String style) {
        return archStyle(style);
    }

    public TestConfigBuilder domainDriven(boolean enabled) {
        this.domainDriven = enabled;
        return this;
    }

    public TestConfigBuilder eventDriven(boolean enabled) {
        this.eventDriven = enabled;
        return this;
    }

    public TestConfigBuilder validateWithArchUnit(
            boolean enabled) {
        this.validateWithArchUnit = enabled;
        return this;
    }

    public TestConfigBuilder basePackage(String pkg) {
        this.basePackage = pkg;
        return this;
    }

    public TestConfigBuilder eventStore(String store) {
        this.eventStore = store;
        return this;
    }

    public TestConfigBuilder schemaRegistry(
            String value) {
        this.schemaRegistry = value;
        return this;
    }

    public TestConfigBuilder outboxPattern(boolean enabled) {
        this.outboxPattern = enabled;
        return this;
    }

    public TestConfigBuilder deadLetterStrategy(
            String strategy) {
        this.deadLetterStrategy = strategy;
        return this;
    }

    public TestConfigBuilder eventsPerSnapshot(int count) {
        this.eventsPerSnapshot = count;
        return this;
    }

    public TestConfigBuilder dddEnabled(boolean enabled) {
        this.dddEnabled = enabled;
        return this;
    }

    public TestConfigBuilder language(
            String name, String version) {
        this.langName = name;
        this.langVersion = version;
        return this;
    }

    public TestConfigBuilder framework(
            String name, String version) {
        this.fwName = name;
        this.fwVersion = version;
        return this;
    }

    public TestConfigBuilder buildTool(String tool) {
        this.buildTool = tool;
        return this;
    }

    public TestConfigBuilder nativeBuild(boolean enabled) {
        this.nativeBuild = enabled;
        return this;
    }

    public TestConfigBuilder database(
            String name, String version) {
        this.dbName = name;
        this.dbVersion = version;
        return this;
    }

    public TestConfigBuilder cache(
            String name, String version) {
        this.cacheName = name;
        this.cacheVersion = version;
        return this;
    }

    public TestConfigBuilder clearInterfaces() {
        this.interfaces.clear();
        return this;
    }

    public TestConfigBuilder addInterface(String type) {
        this.interfaces.add(
                new InterfaceConfig(type, "", ""));
        return this;
    }

    public TestConfigBuilder addInterface(
            String type, String spec, String broker) {
        this.interfaces.add(
                new InterfaceConfig(type, spec, broker));
        return this;
    }

    public TestConfigBuilder migration(
            String name, String version) {
        this.migrationName = name;
        this.migrationVersion = version;
        return this;
    }

    public TestConfigBuilder container(String value) {
        this.container = value;
        return this;
    }

    public TestConfigBuilder orchestrator(String value) {
        this.orchestrator = value;
        return this;
    }

    public TestConfigBuilder cloudProvider(String provider) {
        this.cloudProvider = provider;
        return this;
    }

    public TestConfigBuilder iac(String value) {
        this.iac = value;
        return this;
    }

    public TestConfigBuilder smokeTests(boolean enabled) {
        this.smokeTests = enabled;
        return this;
    }

    public TestConfigBuilder contractTests(boolean enabled) {
        this.contractTests = enabled;
        return this;
    }

    public TestConfigBuilder performanceTests(
            boolean enabled) {
        this.performanceTests = enabled;
        return this;
    }

    public TestConfigBuilder apiGateway(String gateway) {
        this.apiGateway = gateway;
        return this;
    }

    public TestConfigBuilder registry(String value) {
        this.registry = value;
        return this;
    }

    public TestConfigBuilder observabilityTool(String tool) {
        this.observabilityTool = tool;
        return this;
    }

    public TestConfigBuilder serviceMesh(String mesh) {
        this.serviceMesh = mesh;
        return this;
    }

    public TestConfigBuilder templating(String value) {
        this.templating = value;
        return this;
    }

    public TestConfigBuilder securityFrameworks(
            String... frameworks) {
        this.securityFrameworksList = List.of(frameworks);
        return this;
    }

    public TestConfigBuilder addMcpServer(
            McpServerConfig server) {
        this.mcpServers.add(server);
        return this;
    }

    public TestConfigBuilder clearMcpServers() {
        this.mcpServers.clear();
        return this;
    }

    public ProjectConfig build() {
        return new ProjectConfig(
                new ProjectIdentity(
                        projectName, purpose),
                new ArchitectureConfig(
                        archStyle, domainDriven,
                        eventDriven, validateWithArchUnit,
                        basePackage,
                        new ArchitectureConfig.CqrsConfig(
                                eventStore,
                                eventsPerSnapshot,
                                schemaRegistry,
                                outboxPattern,
                                deadLetterStrategy),
                        dddEnabled),
                interfaces,
                new LanguageConfig(langName, langVersion),
                new FrameworkConfig(
                        fwName, fwVersion,
                        buildTool, nativeBuild),
                new DataConfig(
                        new TechComponent(dbName, dbVersion),
                        new TechComponent(
                                migrationName,
                                migrationVersion),
                        new TechComponent(
                                cacheName, cacheVersion)),
                new InfraConfig(
                        container, orchestrator,
                        templating, iac,
                        registry, apiGateway, serviceMesh,
                        cloudProvider,
                        new ObservabilityConfig(
                                observabilityTool,
                                "none", "none")),
                new SecurityConfig(securityFrameworksList),
                new TestingConfig(
                        smokeTests, contractTests,
                        performanceTests, 95, 90),
                new McpConfig(mcpServers));
    }
}
