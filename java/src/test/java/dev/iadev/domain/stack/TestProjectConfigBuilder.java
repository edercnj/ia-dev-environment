package dev.iadev.domain.stack;

import dev.iadev.domain.model.ArchitectureConfig;
import dev.iadev.domain.model.DataConfig;
import dev.iadev.domain.model.FrameworkConfig;
import dev.iadev.domain.model.InfraConfig;
import dev.iadev.domain.model.InterfaceConfig;
import dev.iadev.domain.model.LanguageConfig;
import dev.iadev.domain.model.McpConfig;
import dev.iadev.domain.model.ObservabilityConfig;
import dev.iadev.domain.model.ProjectConfig;
import dev.iadev.domain.model.ProjectIdentity;
import dev.iadev.domain.model.SecurityConfig;
import dev.iadev.domain.model.TestingConfig;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Test helper to build ProjectConfig instances with sensible defaults.
 */
final class TestProjectConfigBuilder {

    private String langName = "java";
    private String langVersion = "21";
    private String fwName = "quarkus";
    private String fwVersion = "3.17";
    private String buildTool = "maven";
    private boolean nativeBuild = false;
    private String archStyle = "microservice";
    private boolean domainDriven = false;
    private boolean eventDriven = false;
    private final List<InterfaceConfig> interfaces = new ArrayList<>();
    private String orchestrator = "none";
    private String templating = "kustomize";
    private String container = "docker";
    private String iac = "none";
    private String registry = "none";

    TestProjectConfigBuilder() {
        interfaces.add(new InterfaceConfig("rest", "", ""));
    }

    TestProjectConfigBuilder language(String name, String version) {
        this.langName = name;
        this.langVersion = version;
        return this;
    }

    TestProjectConfigBuilder framework(String name, String version) {
        this.fwName = name;
        this.fwVersion = version;
        return this;
    }

    TestProjectConfigBuilder buildTool(String tool) {
        this.buildTool = tool;
        return this;
    }

    TestProjectConfigBuilder nativeBuild(boolean enabled) {
        this.nativeBuild = enabled;
        return this;
    }

    TestProjectConfigBuilder architectureStyle(String style) {
        this.archStyle = style;
        return this;
    }

    TestProjectConfigBuilder domainDriven(boolean enabled) {
        this.domainDriven = enabled;
        return this;
    }

    TestProjectConfigBuilder eventDriven(boolean enabled) {
        this.eventDriven = enabled;
        return this;
    }

    TestProjectConfigBuilder interfaces(List<InterfaceConfig> ifaces) {
        this.interfaces.clear();
        this.interfaces.addAll(ifaces);
        return this;
    }

    TestProjectConfigBuilder addInterface(
            String type, String spec, String broker) {
        this.interfaces.add(new InterfaceConfig(type, spec, broker));
        return this;
    }

    TestProjectConfigBuilder clearInterfaces() {
        this.interfaces.clear();
        return this;
    }

    TestProjectConfigBuilder orchestrator(String value) {
        this.orchestrator = value;
        return this;
    }

    TestProjectConfigBuilder templating(String value) {
        this.templating = value;
        return this;
    }

    TestProjectConfigBuilder container(String value) {
        this.container = value;
        return this;
    }

    TestProjectConfigBuilder iac(String value) {
        this.iac = value;
        return this;
    }

    TestProjectConfigBuilder registry(String value) {
        this.registry = value;
        return this;
    }

    ProjectConfig build() {
        return new ProjectConfig(
                new ProjectIdentity("test-project", "Test purpose"),
                new ArchitectureConfig(archStyle, domainDriven, eventDriven,
                        false, "", "eventstoredb", 100),
                interfaces,
                new LanguageConfig(langName, langVersion),
                new FrameworkConfig(fwName, fwVersion, buildTool, nativeBuild),
                DataConfig.fromMap(Map.of()),
                new InfraConfig(
                        container, orchestrator, templating, iac, registry,
                        "none", "none", "none",
                        ObservabilityConfig.fromMap(Map.of())),
                SecurityConfig.fromMap(Map.of()),
                TestingConfig.fromMap(Map.of()),
                McpConfig.fromMap(Map.of())
        );
    }
}
