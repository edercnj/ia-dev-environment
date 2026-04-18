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

    private static final String NONE = "none";
    private static final String DEFAULT_CONTAINER = "docker";
    private static final String DEFAULT_MANIFEST_TOOL = "kustomize";
    private static final String DEFAULT_CQRS_EVENT_STORE =
            "eventstoredb";
    private static final String DEFAULT_ROLLOUT_STRATEGY = "none";
    private static final int MIN_LINE_COVERAGE_PCT = 95;
    private static final int MIN_BRANCH_COVERAGE_PCT = 90;

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
        var interfaceList = ps.interfaces().stream()
                .map(type ->
                        new InterfaceConfig(type, "", ""))
                .toList();
        return new ProjectConfig(
                new ProjectIdentity(
                        ps.name(), ps.purpose()),
                buildArchitecture(ps),
                interfaceList,
                buildLanguage(ps),
                buildFramework(ps),
                buildData(ps),
                DefaultInfraConfig.of(),
                new SecurityConfig(ps.compliance()),
                buildTesting(),
                new McpConfig(List.of()),
                DEFAULT_ROLLOUT_STRATEGY,
                java.util.Set.of(),
                null);
    }

    private static DataConfig buildData(ProjectSummary ps) {
        return new DataConfig(
                buildTechComponent(ps.database()),
                new TechComponent(NONE, ""),
                buildTechComponent(ps.cache()));
    }

    private static TestingConfig buildTesting() {
        return new TestingConfig(
                true, false, true,
                MIN_LINE_COVERAGE_PCT,
                MIN_BRANCH_COVERAGE_PCT);
    }

    private static ArchitectureConfig buildArchitecture(
            ProjectSummary ps) {
        String effectiveStyle = resolveEffectiveStyle(ps);
        boolean archUnit =
                ps.validateArchUnit()
                        && ARCHUNIT_STYLES.contains(
                        ps.archPatternStyle());
        return new ArchitectureConfig(
                effectiveStyle, false, false,
                archUnit, "",
                new ArchitectureConfig.CqrsConfig(
                        DEFAULT_CQRS_EVENT_STORE,
                        ArchitectureConfig
                                .DEFAULT_EVENTS_PER_SNAPSHOT,
                        "", false, ""),
                false);
    }

    private static LanguageConfig buildLanguage(
            ProjectSummary ps) {
        String langVersion =
                LanguageFrameworkMapping
                        .defaultVersionFor(ps.language());
        return new LanguageConfig(
                ps.language(), langVersion);
    }

    private static FrameworkConfig buildFramework(
            ProjectSummary ps) {
        String fwVersion =
                LanguageFrameworkMapping
                        .frameworkVersionFor(
                                ps.framework());
        return new FrameworkConfig(
                ps.framework(), fwVersion,
                ps.buildTool(), false);
    }

    /**
     * Hard-coded default infrastructure config as a typed
     * carrier. Values previously appeared as magic literals
     * inside {@link #buildConfig(ProjectSummary)}.
     */
    private static final class DefaultInfraConfig {
        private DefaultInfraConfig() {
            // utility
        }

        static InfraConfig of() {
            return new InfraConfig(
                    DEFAULT_CONTAINER, NONE,
                    DEFAULT_MANIFEST_TOOL, NONE,
                    NONE, NONE, NONE, NONE,
                    new ObservabilityConfig(
                            NONE, NONE, NONE));
        }
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
