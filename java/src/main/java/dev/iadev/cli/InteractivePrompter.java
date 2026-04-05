package dev.iadev.cli;

import dev.iadev.exception.GenerationCancelledException;
import dev.iadev.domain.model.ProjectConfig;

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
 * <p>Summary formatting is delegated to
 * {@link ProjectSummaryFormatter}. Config construction
 * is delegated to {@link ProjectConfigFactory}.</p>
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
        ProjectSummaryFormatter.displaySummary(
                terminal, summary);

        boolean confirmed = terminal.confirm(
                "Proceed with generation?",
                ConfirmDefault.DEFAULT_YES);
        if (!confirmed) {
            throw new GenerationCancelledException(
                    CANCELLED);
        }

        return ProjectConfigFactory.buildConfig(summary);
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

    /**
     * Delegates to {@link ProjectConfigFactory}.
     *
     * @param ps the project summary
     * @return a new ProjectConfig
     */
    ProjectConfig buildConfig(ProjectSummary ps) {
        return ProjectConfigFactory.buildConfig(ps);
    }
}
