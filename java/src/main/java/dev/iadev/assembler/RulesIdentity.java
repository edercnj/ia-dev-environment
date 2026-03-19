package dev.iadev.assembler;

import dev.iadev.model.ProjectConfig;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Builds the content of {@code 01-project-identity.md} from
 * project configuration data.
 *
 * <p>Generates three sections: identity header, technology
 * stack table, and footer (source-of-truth hierarchy, language
 * policy, constraints). Output must match the TypeScript
 * implementation byte-for-byte (RULE-001).</p>
 *
 * <p>Example usage:
 * <pre>{@code
 * String content = RulesIdentity.buildContent(config);
 * Files.writeString(dest, content);
 * }</pre>
 * </p>
 *
 * @see RulesAssembler
 */
public final class RulesIdentity {

    private static final String NONE_VALUE = "none";

    private RulesIdentity() {
        // Utility class — no instantiation
    }

    /**
     * Builds the full 01-project-identity.md content.
     *
     * @param config the project configuration
     * @return the complete identity rule content
     */
    public static String buildContent(ProjectConfig config) {
        String ifaces = extractInterfaces(config);
        String fwVer = formatFrameworkVersion(config);

        List<String> lines = new ArrayList<>();
        lines.addAll(buildHeader(config, ifaces, fwVer));
        lines.addAll(buildTechStack(config, fwVer));
        lines.addAll(buildFooter());

        return String.join("\n", lines) + "\n";
    }

    /**
     * Generates fallback domain content when template is
     * missing.
     *
     * @param config the project configuration
     * @return minimal domain rule content
     */
    public static String fallbackDomainContent(
            ProjectConfig config) {
        return "# Rule — {DOMAIN_NAME} Domain\n\n"
                + config.project().name() + "\n";
    }

    static String extractInterfaces(ProjectConfig config) {
        String result = config.interfaces().stream()
                .map(i -> i.type())
                .collect(Collectors.joining(", "));
        return result.isEmpty() ? NONE_VALUE : result;
    }

    static String formatFrameworkVersion(
            ProjectConfig config) {
        String version = config.framework().version();
        if (version == null || version.isEmpty()) {
            return "";
        }
        return " " + version;
    }

    private static List<String> buildHeader(
            ProjectConfig config,
            String ifaces,
            String fwVer) {
        return List.of(
                "# Global Behavior & Language Policy",
                "- **Output Language**: English ONLY."
                        + " (Mandatory for all responses"
                        + " and internal reasoning).",
                "- **Token Optimization**: Eliminate all"
                        + " greetings, apologies, and"
                        + " conversational fluff. Start"
                        + " responses directly with"
                        + " technical information.",
                "- **Priority**: Maintain 100% fidelity"
                        + " to the technical constraints"
                        + " defined in the original"
                        + " rules below.",
                "",
                "# Project Identity — "
                        + config.project().name(),
                "",
                "## Identity",
                "- **Name:** " + config.project().name(),
                "- **Purpose:** "
                        + config.project().purpose(),
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
                        + config.framework().name()
                        + fwVer);
    }

    private static List<String> buildTechStack(
            ProjectConfig config, String fwVer) {
        var obs = config.infrastructure().observability();
        return List.of(
                "",
                "## Technology Stack",
                "| Layer | Technology |",
                "|-------|-----------|",
                "| Architecture | "
                        + config.architecture().style()
                        + " |",
                "| Language | "
                        + config.language().name() + " "
                        + config.language().version()
                        + " |",
                "| Framework | "
                        + config.framework().name()
                        + fwVer + " |",
                "| Build Tool | "
                        + config.framework().buildTool()
                        + " |",
                "| Database | "
                        + config.data().database().name()
                        + " |",
                "| Migration | "
                        + config.data().migration().name()
                        + " |",
                "| Cache | "
                        + config.data().cache().name()
                        + " |",
                "| Message Broker | none |",
                "| Container | "
                        + config.infrastructure().container()
                        + " |",
                "| Orchestrator | "
                        + config.infrastructure()
                        .orchestrator() + " |",
                "| Observability | "
                        + obs.tool() + " (" + obs.tracing()
                        + ") |",
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
                        + " |");
    }

    private static List<String> buildFooter() {
        return List.of(
                "",
                "## Source of Truth (Hierarchy)",
                "1. Epics / PRDs (vision and global rules)",
                "2. ADRs (architectural decisions)",
                "3. Stories / tickets"
                        + " (detailed requirements)",
                "4. Rules (.claude/rules/)",
                "5. Source code",
                "",
                "## Language",
                "- Code: English"
                        + " (classes, methods, variables)",
                "- Commits: English"
                        + " (Conventional Commits)",
                "- Documentation: English"
                        + " (customize as needed)",
                "- Application logs: English",
                "",
                "## Constraints",
                "<!-- Customize constraints"
                        + " for your project -->",
                "- Cloud-Agnostic: ZERO dependencies"
                        + " on cloud-specific services",
                "- Horizontal scalability: Application"
                        + " must be stateless",
                "- Externalized configuration: All"
                        + " configuration via environment"
                        + " variables or ConfigMaps");
    }
}
