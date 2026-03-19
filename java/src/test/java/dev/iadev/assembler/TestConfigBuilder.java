package dev.iadev.assembler;

import dev.iadev.model.ArchitectureConfig;
import dev.iadev.model.DataConfig;
import dev.iadev.model.FrameworkConfig;
import dev.iadev.model.InfraConfig;
import dev.iadev.model.InterfaceConfig;
import dev.iadev.model.LanguageConfig;
import dev.iadev.model.McpConfig;
import dev.iadev.model.ObservabilityConfig;
import dev.iadev.model.ProjectConfig;
import dev.iadev.model.ProjectIdentity;
import dev.iadev.model.SecurityConfig;
import dev.iadev.model.TechComponent;
import dev.iadev.model.TestingConfig;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Test helper to build ProjectConfig instances with sensible
 * defaults for assembler tests.
 */
final class TestConfigBuilder {

    private String projectName = "test-project";
    private String projectPurpose = "Test purpose";
    private String archStyle = "microservice";
    private boolean domainDriven = false;
    private boolean eventDriven = false;
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
    private String purpose = "Test purpose";
    private final List<InterfaceConfig> interfaces =
            new ArrayList<>();

    private TestConfigBuilder() {
        interfaces.add(
                new InterfaceConfig("rest", "", ""));
    }

    static ProjectConfig minimal() {
        return new TestConfigBuilder().build();
    }

    static TestConfigBuilder builder() {
        return new TestConfigBuilder();
    }

    TestConfigBuilder projectName(String name) {
        this.projectName = name;
        return this;
    }

    TestConfigBuilder archStyle(String style) {
        this.archStyle = style;
        return this;
    }

    TestConfigBuilder domainDriven(boolean enabled) {
        this.domainDriven = enabled;
        return this;
    }

    TestConfigBuilder eventDriven(boolean enabled) {
        this.eventDriven = enabled;
        return this;
    }

    TestConfigBuilder language(String name, String version) {
        this.langName = name;
        this.langVersion = version;
        return this;
    }

    TestConfigBuilder framework(String name, String version) {
        this.fwName = name;
        this.fwVersion = version;
        return this;
    }

    TestConfigBuilder buildTool(String tool) {
        this.buildTool = tool;
        return this;
    }

    TestConfigBuilder nativeBuild(boolean enabled) {
        this.nativeBuild = enabled;
        return this;
    }

    TestConfigBuilder database(String name, String version) {
        this.dbName = name;
        this.dbVersion = version;
        return this;
    }

    TestConfigBuilder cache(String name, String version) {
        this.cacheName = name;
        this.cacheVersion = version;
        return this;
    }

    TestConfigBuilder clearInterfaces() {
        this.interfaces.clear();
        return this;
    }

    TestConfigBuilder addInterface(String type) {
        this.interfaces.add(
                new InterfaceConfig(type, "", ""));
        return this;
    }

    TestConfigBuilder addInterface(
            String type, String spec, String broker) {
        this.interfaces.add(
                new InterfaceConfig(type, spec, broker));
        return this;
    }

    TestConfigBuilder purpose(String purpose) {
        this.purpose = purpose;
        return this;
    }

    TestConfigBuilder migration(
            String name, String version) {
        this.migrationName = name;
        this.migrationVersion = version;
        return this;
    }

    TestConfigBuilder container(String container) {
        this.container = container;
        return this;
    }

    TestConfigBuilder orchestrator(String orchestrator) {
        this.orchestrator = orchestrator;
        return this;
    }

    TestConfigBuilder cloudProvider(String provider) {
        this.cloudProvider = provider;
        return this;
    }

    TestConfigBuilder iac(String iac) {
        this.iac = iac;
        return this;
    }

    TestConfigBuilder smokeTests(boolean enabled) {
        this.smokeTests = enabled;
        return this;
    }

    TestConfigBuilder contractTests(boolean enabled) {
        this.contractTests = enabled;
        return this;
    }

    TestConfigBuilder performanceTests(boolean enabled) {
        this.performanceTests = enabled;
        return this;
    }

    ProjectConfig build() {
        return new ProjectConfig(
                new ProjectIdentity(
                        projectName, purpose),
                new ArchitectureConfig(
                        archStyle, domainDriven,
                        eventDriven),
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
                        "kustomize", iac,
                        "none", "none", "none",
                        cloudProvider,
                        new ObservabilityConfig(
                                "none", "none", "none")),
                SecurityConfig.fromMap(Map.of()),
                new TestingConfig(
                        smokeTests, contractTests,
                        performanceTests, 95, 90),
                McpConfig.fromMap(Map.of()));
    }
}
