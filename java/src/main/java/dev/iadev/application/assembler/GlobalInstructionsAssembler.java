package dev.iadev.application.assembler;

import dev.iadev.domain.model.InterfaceConfig;
import dev.iadev.domain.model.ProjectConfig;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Generates the global {@code copilot-instructions.md}
 * file content.
 *
 * <p>Builds the file programmatically using string
 * concatenation for sections: Identity, Stack,
 * Constraints, and Contextual References.</p>
 *
 * <p>Originally extracted per story-0008-0014 to satisfy
 * the 250-line SRP constraint.</p>
 *
 * @see ContextualInstructionsAssembler
 */
public final class GlobalInstructionsAssembler {

    GlobalInstructionsAssembler() {
        // Package-private — instantiated by coordinator
    }

    /**
     * Generates copilot-instructions.md at the given
     * output directory.
     *
     * @param config    the project configuration
     * @param githubDir the .github output directory
     * @return the generated file path
     */
    String generate(ProjectConfig config, Path githubDir) {
        String content = buildCopilotInstructions(config);
        Path dest =
                githubDir.resolve("copilot-instructions.md");
        CopyHelpers.writeFile(dest, content);
        return dest.toString();
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
        List<String> lines = new ArrayList<>();
        lines.add("## Technology Stack");
        lines.add("");
        lines.add("| Layer | Technology |");
        lines.add("|-------|-----------|");
        lines.addAll(buildCoreStackRows(config, fwVer));
        lines.addAll(buildInfraStackRows(config));
        lines.addAll(buildQualityStackRows(config));
        lines.add("");
        return lines;
    }

    private static List<String> buildCoreStackRows(
            ProjectConfig config, String fwVer) {
        return List.of(
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
                        + " |");
    }

    private static List<String> buildInfraStackRows(
            ProjectConfig config) {
        return List.of(
                "| Container | "
                        + capitalize(
                        config.infrastructure()
                                .container()) + " |",
                "| Orchestrator | "
                        + capitalize(
                        config.infrastructure()
                                .orchestrator()) + " |",
                "| Resilience | Mandatory"
                        + " (always enabled) |");
    }

    private static List<String> buildQualityStackRows(
            ProjectConfig config) {
        return List.of(
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
                        + " |");
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
        List<String> lines = new ArrayList<>();
        lines.add("## Contextual Instructions");
        lines.add("");
        lines.add("The following instruction files provide"
                + " domain-specific context:");
        lines.add("");
        lines.addAll(contextualRefItems());
        lines.add("");
        lines.add("For deep-dive references, see the"
                + " knowledge packs in"
                + " `.claude/skills/` (generated"
                + " alongside this structure).");
        return lines;
    }

    private static List<String> contextualRefItems() {
        return List.of(
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
                        + " categories, merge checklist");
    }

    static String capitalize(String s) {
        if (s == null || s.isEmpty()) {
            return s;
        }
        return Character.toUpperCase(s.charAt(0))
                + s.substring(1);
    }
}
