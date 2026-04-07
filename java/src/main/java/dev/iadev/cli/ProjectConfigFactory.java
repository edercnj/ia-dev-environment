package dev.iadev.cli;

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
import dev.iadev.domain.model.TechComponent;
import dev.iadev.domain.model.TestingConfig;

import java.util.List;
import java.util.Set;

/**
 * Builds a {@link ProjectConfig} from a
 * {@link ProjectSummary}.
 *
 * <p>Extracted from {@link InteractivePrompter} to keep
 * both classes under 250 lines per RULE-004.</p>
 *
 * @see InteractivePrompter
 */
final class ProjectConfigFactory {

    private static final Set<String> ARCHUNIT_STYLES =
            Set.of("hexagonal", "clean");

    private ProjectConfigFactory() {
        // utility class
    }

    /**
     * Converts a prompt summary into a fully populated
     * project configuration.
     *
     * @param ps the project summary from prompts
     * @return a new ProjectConfig
     */
    static ProjectConfig buildConfig(ProjectSummary ps) {
        var project = new ProjectIdentity(
                ps.name(), ps.purpose());
        String effectiveStyle =
                resolveEffectiveStyle(ps);
        boolean archUnit =
                ps.validateArchUnit()
                        && ARCHUNIT_STYLES.contains(
                        ps.archPatternStyle());
        var architecture = new ArchitectureConfig(
                effectiveStyle, false, false,
                archUnit, "",
                new ArchitectureConfig.CqrsConfig(
                        "eventstoredb",
                        ArchitectureConfig
                                .DEFAULT_EVENTS_PER_SNAPSHOT,
                        "", false, ""),
                false);
        var interfaceList = ps.interfaces().stream()
                .map(type ->
                        new InterfaceConfig(type, "", ""))
                .toList();
        String langVersion =
                LanguageFrameworkMapping
                        .defaultVersionFor(ps.language());
        var lang = new LanguageConfig(
                ps.language(), langVersion);
        String fwVersion =
                LanguageFrameworkMapping
                        .frameworkVersionFor(
                                ps.framework());
        var fw = new FrameworkConfig(
                ps.framework(), fwVersion,
                ps.buildTool(), false);
        var data = new DataConfig(
                buildTechComponent(ps.database()),
                new TechComponent("none", ""),
                buildTechComponent(ps.cache()));
        var infra = new InfraConfig(
                "docker", "none", "kustomize", "none",
                "none", "none", "none", "none",
                new ObservabilityConfig(
                        "none", "none", "none"));
        var security = new SecurityConfig(
                ps.compliance());
        var testing = new TestingConfig(
                true, false, true, 95, 90);
        var mcp = new McpConfig(List.of());

        return new ProjectConfig(
                project, architecture, interfaceList,
                lang, fw, data, infra, security,
                testing, mcp, "none",
                java.util.Set.of(), null);
    }

    private static String resolveEffectiveStyle(
            ProjectSummary ps) {
        if (!ps.archPatternStyle().isEmpty()) {
            return ps.archPatternStyle();
        }
        return ps.archStyle();
    }

    private static TechComponent buildTechComponent(
            String value) {
        if (value == null || value.isBlank()) {
            return new TechComponent("none", "");
        }
        return new TechComponent(value, "");
    }
}
