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

    ProjectConfig build() {
        return new ProjectConfig(
                new ProjectIdentity(projectName, projectPurpose),
                new ArchitectureConfig(
                        archStyle, domainDriven, eventDriven),
                interfaces,
                new LanguageConfig(langName, langVersion),
                new FrameworkConfig(
                        fwName, fwVersion, buildTool, nativeBuild),
                new DataConfig(
                        new TechComponent(dbName, dbVersion),
                        TechComponent.fromMap(Map.of()),
                        new TechComponent(cacheName, cacheVersion)),
                InfraConfig.fromMap(Map.of()),
                SecurityConfig.fromMap(Map.of()),
                TestingConfig.fromMap(Map.of()),
                McpConfig.fromMap(Map.of()));
    }
}
