package dev.iadev.cli;

import dev.iadev.exception.GenerationCancelledException;
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
import java.util.regex.Pattern;

/**
 * Orchestrates interactive CLI prompts to build a
 * {@link ProjectConfig}.
 *
 * <p>Guides the user through prompts collecting project
 * name, purpose, architecture style, language, framework,
 * build tool, interfaces, database, cache, architecture
 * pattern (java/kotlin only), ArchUnit validation
 * (hexagonal/clean only), and compliance frameworks.</p>
 *
 * <p>Uses {@link TerminalProvider} abstraction for
 * testability. Production code injects
 * {@link JLineTerminalProvider}; tests inject a mock.</p>
 */
public class InteractivePrompter {

    private static final Pattern KEBAB_CASE =
            Pattern.compile("^[a-z][a-z0-9-]*[a-z0-9]$");
    private static final int MIN_NAME_LENGTH = 3;
    private static final int MIN_PURPOSE_LENGTH = 10;
    private static final Set<String> ARCHUNIT_STYLES =
            Set.of("hexagonal", "clean");

    static final String KEBAB_ERROR =
            "Project name must be kebab-case "
                    + "(e.g., my-project)";
    static final String NAME_TOO_SHORT_ERROR =
            "Project name must be at least 3 characters";
    static final String PURPOSE_ERROR =
            "Purpose must be at least 10 characters";
    static final String CANCELLED_BY_USER =
            "Generation cancelled by user";
    static final String CANCELLED =
            "Generation cancelled";
    static final String COMPLIANCE_NONE_ERROR =
            "Cannot select 'none' with other "
                    + "compliance options";

    private final TerminalProvider terminal;

    /**
     * Creates a prompter backed by the given terminal.
     *
     * @param terminal the terminal provider for interaction
     */
    public InteractivePrompter(TerminalProvider terminal) {
        this.terminal = terminal;
    }

    /**
     * Executes the full interactive prompt flow.
     *
     * @return a fully populated {@link ProjectConfig}
     * @throws GenerationCancelledException if cancelled
     */
    public ProjectConfig prompt() {
        String name = promptProjectName();
        String purpose = promptPurpose();
        String archStyle = promptArchitectureStyle();
        String language = promptLanguage();
        String framework = promptFramework(language);
        String buildTool = promptBuildTool(language);
        List<String> interfaces = promptInterfaces();
        String database =
                promptOptionalField("Database (optional):");
        String cache =
                promptOptionalField("Cache (optional):");

        String archPattern =
                promptArchPatternStyle(language);
        boolean validateArchUnit =
                promptValidateArchUnit(archPattern);
        List<String> compliance = promptCompliance();

        ProjectSummary summary = new ProjectSummary(
                name, purpose, archStyle, language,
                framework, buildTool, interfaces,
                database, cache, archPattern,
                validateArchUnit, compliance);
        displaySummary(summary);

        boolean confirmed = terminal.confirm(
                "Proceed with generation?",
                ConfirmDefault.DEFAULT_YES);
        if (!confirmed) {
            throw new GenerationCancelledException(
                    CANCELLED);
        }

        return buildConfig(summary);
    }

    String promptArchPatternStyle(String language) {
        if (!isArchPatternLanguage(language)) {
            return "";
        }
        return terminal.selectFromList(
                "Architecture style:",
                LanguageFrameworkMapping
                        .ARCH_PATTERN_STYLES,
                0);
    }

    boolean promptValidateArchUnit(String archPattern) {
        if (!ARCHUNIT_STYLES.contains(archPattern)) {
            return false;
        }
        return terminal.confirm(
                "Validate with ArchUnit?",
                ConfirmDefault.DEFAULT_NO);
    }

    List<String> promptCompliance() {
        List<String> selected = terminal.selectMultiple(
                "Compliance requirements:",
                LanguageFrameworkMapping
                        .COMPLIANCE_OPTIONS,
                List.of("none"));
        return resolveCompliance(selected);
    }

    List<String> resolveCompliance(List<String> selected) {
        boolean hasNone = selected.contains("none");
        boolean hasOthers = selected.stream()
                .anyMatch(s -> !"none".equals(s));
        if (hasNone && hasOthers) {
            terminal.display(COMPLIANCE_NONE_ERROR);
            return promptCompliance();
        }
        if (hasNone) {
            return List.of();
        }
        return List.copyOf(selected);
    }

    static boolean isArchPatternLanguage(String language) {
        return LanguageFrameworkMapping
                .ARCH_PATTERN_LANGUAGES
                .contains(language);
    }

    private String promptProjectName() {
        return terminal.readLineWithValidation(
                "Project name:",
                this::isValidProjectName,
                KEBAB_ERROR);
    }

    private String promptPurpose() {
        return terminal.readLineWithValidation(
                "Purpose:",
                input -> input != null
                        && input.trim().length()
                        >= MIN_PURPOSE_LENGTH,
                PURPOSE_ERROR);
    }

    private String promptArchitectureStyle() {
        return terminal.selectFromList(
                "Architecture:",
                LanguageFrameworkMapping
                        .ARCHITECTURE_STYLES,
                0);
    }

    private String promptLanguage() {
        return terminal.selectFromList(
                "Language:",
                LanguageFrameworkMapping.LANGUAGES,
                0);
    }

    private String promptFramework(String language) {
        List<String> frameworks =
                LanguageFrameworkMapping
                        .frameworksFor(language);
        if (frameworks.size() == 1) {
            return frameworks.getFirst();
        }
        return terminal.selectFromList(
                "Framework:", frameworks, 0);
    }

    private String promptBuildTool(String language) {
        List<String> tools =
                LanguageFrameworkMapping
                        .buildToolsFor(language);
        if (tools.size() == 1) {
            return tools.getFirst();
        }
        return terminal.selectFromList(
                "Build Tool:", tools, 0);
    }

    private List<String> promptInterfaces() {
        return terminal.selectMultiple(
                "Interfaces:",
                LanguageFrameworkMapping.INTERFACE_TYPES,
                List.of("rest"));
    }

    private String promptOptionalField(String prompt) {
        return terminal.readLine(prompt);
    }

    boolean isValidProjectName(String input) {
        if (input == null
                || input.trim().length() < MIN_NAME_LENGTH) {
            return false;
        }
        return KEBAB_CASE.matcher(input.trim()).matches();
    }

    private void displaySummary(ProjectSummary ps) {
        String langDisplay =
                formatLanguageDisplay(ps.language());
        String text =
                formatSummaryText(ps, langDisplay);
        terminal.display(text);
    }

    private String formatLanguageDisplay(String language) {
        String langVersion =
                LanguageFrameworkMapping
                        .defaultVersionFor(language);
        return langVersion.isEmpty()
                ? language
                : language + " " + langVersion;
    }

    private String formatSummaryText(
            ProjectSummary ps, String langDisplay) {
        String db = ps.database().isBlank()
                ? "none" : ps.database();
        String ch = ps.cache().isBlank()
                ? "none" : ps.cache();
        String complianceDisplay =
                ps.compliance().isEmpty()
                        ? "none"
                        : String.join(", ",
                        ps.compliance());
        var sb = new StringBuilder();
        sb.append("""

                Project Configuration Summary:
                  Name:          %s
                  Purpose:       %s
                  Architecture:  %s
                  Language:       %s
                  Framework:      %s
                  Build Tool:     %s
                  Interfaces:     %s
                  Database:       %s
                  Cache:          %s
                """.formatted(
                ps.name(), ps.purpose(), ps.archStyle(),
                langDisplay, ps.framework(),
                ps.buildTool(),
                String.join(", ", ps.interfaces()),
                db, ch));
        appendArchPatternSummary(sb, ps);
        sb.append("  Compliance:    %s%n"
                .formatted(complianceDisplay));
        return sb.toString();
    }

    private void appendArchPatternSummary(
            StringBuilder sb, ProjectSummary ps) {
        if (!ps.archPatternStyle().isEmpty()) {
            sb.append("  Arch Pattern:  %s%n"
                    .formatted(ps.archPatternStyle()));
            if (ps.validateArchUnit()) {
                sb.append("  ArchUnit:      yes%n");
            }
        }
    }

    ProjectConfig buildConfig(ProjectSummary ps) {
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
                "eventstoredb", "", false, "",
                ArchitectureConfig
                        .DEFAULT_EVENTS_PER_SNAPSHOT,
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
                testing, mcp);
    }

    private String resolveEffectiveStyle(
            ProjectSummary ps) {
        if (!ps.archPatternStyle().isEmpty()) {
            return ps.archPatternStyle();
        }
        return ps.archStyle();
    }

    private TechComponent buildTechComponent(
            String value) {
        if (value == null || value.isBlank()) {
            return new TechComponent("none", "");
        }
        return new TechComponent(value, "");
    }
}
