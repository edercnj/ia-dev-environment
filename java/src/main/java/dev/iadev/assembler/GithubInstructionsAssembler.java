package dev.iadev.assembler;

import dev.iadev.model.InterfaceConfig;
import dev.iadev.model.ProjectConfig;
import dev.iadev.template.TemplateEngine;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Assembles {@code .github/copilot-instructions.md} and
 * contextual instruction files for GitHub Copilot.
 *
 * <p>This is the eighth assembler in the pipeline (position
 * 8 of 23 per RULE-005). It generates a global instructions
 * file programmatically and four contextual instruction files
 * from templates with placeholder replacement.</p>
 *
 * <p>The global file is built without a template engine,
 * using string concatenation for sections: Identity, Stack,
 * Constraints, and Contextual References.</p>
 *
 * <p>Contextual files are loaded from
 * {@code github-instructions-templates/} and rendered with
 * single-brace placeholder replacement matching the
 * TypeScript implementation.</p>
 *
 * <p>Example usage:
 * <pre>{@code
 * Assembler instructions =
 *     new GithubInstructionsAssembler();
 * List<String> files = instructions.assemble(
 *     config, engine, outputDir);
 * }</pre>
 * </p>
 *
 * @see Assembler
 */
public final class GithubInstructionsAssembler
        implements Assembler {

    private static final String TEMPLATES_DIR =
            "github-instructions-templates";

    /** Contextual instruction template names. */
    static final List<String> CONTEXTUAL_INSTRUCTIONS =
            List.of(
                    "domain",
                    "coding-standards",
                    "architecture",
                    "quality-gates");

    /**
     * Pattern for single-brace placeholder replacement.
     * Matches {@code {key}} but not {@code {{key}}}.
     */
    static final Pattern SINGLE_BRACE_PATTERN =
            Pattern.compile("(?<!\\{)\\{(\\w+)\\}(?!\\})");

    private final Path resourcesDir;

    /**
     * Creates a GithubInstructionsAssembler using classpath
     * resources.
     */
    public GithubInstructionsAssembler() {
        this(resolveClasspathResources());
    }

    /**
     * Creates a GithubInstructionsAssembler with an explicit
     * resources directory.
     *
     * @param resourcesDir the base resources directory
     */
    public GithubInstructionsAssembler(Path resourcesDir) {
        this.resourcesDir = resourcesDir;
    }

    /**
     * {@inheritDoc}
     *
     * <p>Generates the global copilot-instructions.md and
     * four contextual instruction files. Returns the list
     * of generated file paths.</p>
     */
    @Override
    public List<String> assemble(
            ProjectConfig config,
            TemplateEngine engine,
            Path outputDir) {
        CopyHelpers.ensureDirectory(outputDir);

        List<String> results = new ArrayList<>();
        results.add(generateGlobal(config, outputDir));

        Path instructionsDir =
                outputDir.resolve("instructions");
        CopyHelpers.ensureDirectory(instructionsDir);
        results.addAll(generateContextual(
                config, instructionsDir));

        return results;
    }

    /**
     * Builds the complete copilot-instructions.md content
     * programmatically.
     *
     * @param config the project configuration
     * @return the full markdown content with trailing newline
     */
    static String buildCopilotInstructions(
            ProjectConfig config) {
        String ifaces = formatInterfaces(config);
        String fwVer = formatFrameworkVersion(config);

        List<String> lines = new ArrayList<>();
        lines.addAll(
                buildIdentitySection(config, ifaces, fwVer));
        lines.addAll(
                buildStackSection(config, fwVer));
        lines.addAll(buildConstraintsSection());
        lines.addAll(buildContextualRefsSection());

        return String.join("\n", lines) + "\n";
    }

    /**
     * Formats interface types for display. REST and GRPC
     * are uppercased; other types are left as-is.
     *
     * @param config the project configuration
     * @return formatted interface string
     */
    static String formatInterfaces(ProjectConfig config) {
        if (config.interfaces().isEmpty()) {
            return "none";
        }
        return config.interfaces().stream()
                .map(InterfaceConfig::type)
                .map(type -> "rest".equals(type)
                        || "grpc".equals(type)
                        ? type.toUpperCase()
                        : type)
                .reduce((a, b) -> a + ", " + b)
                .orElse("none");
    }

    /**
     * Formats the framework version with a leading space,
     * or returns empty string if no version is set.
     *
     * @param config the project configuration
     * @return formatted framework version
     */
    static String formatFrameworkVersion(
            ProjectConfig config) {
        String version = config.framework().version();
        if (version == null || version.isEmpty()) {
            return "";
        }
        return " " + version;
    }

    private static List<String> buildIdentitySection(
            ProjectConfig config,
            String ifaces,
            String fwVer) {
        return List.of(
                "# Project Identity \u2014 "
                        + config.project().name(),
                "",
                "## Identity",
                "",
                "- **Name:** " + config.project().name(),
                "- **Architecture Style:** "
                        + config.architecture().style(),
                "- **Domain-Driven Design:** "
                        + String.valueOf(
                        config.architecture()
                                .domainDriven()),
                "- **Event-Driven:** "
                        + String.valueOf(
                        config.architecture()
                                .eventDriven()),
                "- **Interfaces:** " + ifaces,
                "- **Language:** "
                        + config.language().name() + " "
                        + config.language().version(),
                "- **Framework:** "
                        + config.framework().name() + fwVer,
                "");
    }

    private static List<String> buildStackSection(
            ProjectConfig config,
            String fwVer) {
        return List.of(
                "## Technology Stack",
                "",
                "| Layer | Technology |",
                "|-------|-----------|",
                "| Architecture | "
                        + capitalize(
                        config.architecture().style())
                        + " |",
                "| Language | "
                        + capitalize(
                        config.language().name()) + " "
                        + config.language().version()
                        + " |",
                "| Framework | "
                        + capitalize(
                        config.framework().name())
                        + fwVer + " |",
                "| Build Tool | "
                        + capitalize(
                        config.framework().buildTool())
                        + " |",
                "| Container | "
                        + capitalize(
                        config.infrastructure()
                                .container()) + " |",
                "| Orchestrator | "
                        + capitalize(
                        config.infrastructure()
                                .orchestrator()) + " |",
                "| Resilience | Mandatory"
                        + " (always enabled) |",
                "| Native Build | "
                        + String.valueOf(
                        config.framework().nativeBuild())
                        + " |",
                "| Smoke Tests | "
                        + String.valueOf(
                        config.testing().smokeTests())
                        + " |",
                "| Contract Tests | "
                        + String.valueOf(
                        config.testing().contractTests())
                        + " |",
                "");
    }

    private static List<String> buildConstraintsSection() {
        return List.of(
                "## Constraints",
                "",
                "- Cloud-Agnostic: ZERO dependencies on"
                        + " cloud-specific services",
                "- Horizontal scalability: Application"
                        + " must be stateless",
                "- Externalized configuration: All"
                        + " configuration via environment"
                        + " variables or ConfigMaps",
                "");
    }

    private static List<String> buildContextualRefsSection() {
        return List.of(
                "## Contextual Instructions",
                "",
                "The following instruction files provide"
                        + " domain-specific context:",
                "",
                "- `instructions/domain.instructions.md`"
                        + " \u2014 Domain model, business"
                        + " rules, sensitive data",
                "- `instructions/coding-standards"
                        + ".instructions.md` \u2014 Clean"
                        + " Code, SOLID, naming, error"
                        + " handling",
                "- `instructions/architecture"
                        + ".instructions.md` \u2014"
                        + " Hexagonal architecture, layer"
                        + " rules, package structure",
                "- `instructions/quality-gates"
                        + ".instructions.md` \u2014"
                        + " Coverage thresholds, test"
                        + " categories, merge checklist",
                "",
                "For deep-dive references, see the"
                        + " knowledge packs in"
                        + " `.claude/skills/` (generated"
                        + " alongside this structure).");
    }

    private static String capitalize(String s) {
        if (s == null || s.isEmpty()) {
            return s;
        }
        return Character.toUpperCase(s.charAt(0))
                + s.substring(1);
    }

    private String generateGlobal(
            ProjectConfig config,
            Path githubDir) {
        String content =
                buildCopilotInstructions(config);
        Path dest =
                githubDir.resolve("copilot-instructions.md");
        CopyHelpers.writeFile(dest, content);
        return dest.toString();
    }

    private List<String> generateContextual(
            ProjectConfig config,
            Path instructionsDir) {
        Path srcDir = resourcesDir.resolve(TEMPLATES_DIR);
        if (!Files.exists(srcDir)
                || !Files.isDirectory(srcDir)) {
            return List.of();
        }

        Map<String, String> context =
                buildPlaceholderContext(config);
        List<String> results = new ArrayList<>();

        for (String name : CONTEXTUAL_INSTRUCTIONS) {
            Path src = srcDir.resolve(name + ".md");
            if (!Files.exists(src)) {
                continue;
            }
            String content = CopyHelpers.readFile(src);
            String rendered =
                    replaceSingleBracePlaceholders(
                            content, context);
            Path dest = instructionsDir.resolve(
                    name + ".instructions.md");
            CopyHelpers.writeFile(dest, rendered);
            results.add(dest.toString());
        }

        return results;
    }

    /**
     * Builds a context map for single-brace placeholder
     * replacement, matching the TypeScript
     * {@code buildDefaultContext} function.
     *
     * @param config the project configuration
     * @return the placeholder context map
     */
    static Map<String, String> buildPlaceholderContext(
            ProjectConfig config) {
        return Map.ofEntries(
                Map.entry("project_name",
                        config.project().name()),
                Map.entry("project_purpose",
                        config.project().purpose()),
                Map.entry("language_name",
                        config.language().name()),
                Map.entry("language_version",
                        config.language().version()),
                Map.entry("framework_name",
                        config.framework().name()),
                Map.entry("framework_version",
                        config.framework().version()),
                Map.entry("build_tool",
                        config.framework().buildTool()),
                Map.entry("architecture_style",
                        config.architecture().style()),
                Map.entry("coverage_line",
                        String.valueOf(
                                config.testing()
                                        .coverageLine())),
                Map.entry("coverage_branch",
                        String.valueOf(
                                config.testing()
                                        .coverageBranch())));
    }

    /**
     * Replaces single-brace {@code {key}} placeholders
     * with values from the context map.
     *
     * <p>Known keys are replaced; unknown keys are
     * preserved verbatim. Does not match double-brace
     * patterns.</p>
     *
     * @param content the content with placeholders
     * @param context the key-value map for replacement
     * @return the content with known placeholders replaced
     */
    static String replaceSingleBracePlaceholders(
            String content,
            Map<String, String> context) {
        Matcher matcher =
                SINGLE_BRACE_PATTERN.matcher(content);
        StringBuilder sb = new StringBuilder();
        while (matcher.find()) {
            String key = matcher.group(1);
            String value = context.get(key);
            if (value != null) {
                matcher.appendReplacement(
                        sb, Matcher.quoteReplacement(value));
            } else {
                matcher.appendReplacement(
                        sb, Matcher.quoteReplacement(
                                matcher.group(0)));
            }
        }
        matcher.appendTail(sb);
        return sb.toString();
    }

    private static Path resolveClasspathResources() {
        return dev.iadev.util.ResourceResolver
                .resolveResourcesRoot(TEMPLATES_DIR);
    }
}
