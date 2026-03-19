package dev.iadev.domain.stack;

import dev.iadev.model.FrameworkConfig;
import dev.iadev.model.LanguageConfig;
import dev.iadev.model.ProjectConfig;

import java.util.ArrayList;
import java.util.List;

/**
 * Resolves all derived stack values from a ProjectConfig.
 *
 * <p>Derives build commands, Docker image, health path, default port,
 * file extension, project type, and protocols by looking up the
 * language/build-tool combination in {@link StackMapping} constants.</p>
 *
 * <p>This class is stateless and all methods are static. It has zero
 * external framework dependencies (RULE-007).</p>
 */
public final class StackResolver {

    private static final String EMPTY_COMMAND = "";
    private static final String CLI_INTERFACE = "cli";
    private static final String EVENT_CONSUMER_INTERFACE = "event-consumer";
    private static final String REST_INTERFACE = "rest";

    private StackResolver() {
        // utility class
    }

    /**
     * Resolves all derived stack values from a project configuration.
     *
     * @param config the project configuration
     * @return an immutable ResolvedStack containing all resolved values
     */
    public static ResolvedStack resolve(ProjectConfig config) {
        var commands = resolveCommands(
                config.language(), config.framework());
        var protocols = deriveProtocols(config);

        return new ResolvedStack(
                fieldOrEmpty(commands, "compileCmd"),
                fieldOrEmpty(commands, "buildCmd"),
                fieldOrEmpty(commands, "testCmd"),
                fieldOrEmpty(commands, "coverageCmd"),
                fieldOrEmpty(commands, "fileExtension"),
                fieldOrEmpty(commands, "buildFile"),
                fieldOrEmpty(commands, "packageManager"),
                resolveDefaultPort(config.framework()),
                resolveHealthPath(config.framework()),
                resolveDockerImage(config.language()),
                inferNativeBuild(config),
                deriveProjectType(config),
                protocols
        );
    }

    private static LanguageCommandSet resolveCommands(
            LanguageConfig language, FrameworkConfig framework) {
        String key = language.name() + "-" + framework.buildTool();
        return StackMapping.LANGUAGE_COMMANDS.get(key);
    }

    private static String resolveDockerImage(LanguageConfig language) {
        String template = StackMapping.DOCKER_BASE_IMAGES.get(language.name());
        if (template == null) {
            return StackMapping.DEFAULT_DOCKER_IMAGE;
        }
        return template.replace("{version}", language.version());
    }

    private static String resolveHealthPath(FrameworkConfig framework) {
        return StackMapping.FRAMEWORK_HEALTH_PATHS.getOrDefault(
                framework.name(), StackMapping.DEFAULT_HEALTH_PATH);
    }

    private static int resolveDefaultPort(FrameworkConfig framework) {
        return StackMapping.FRAMEWORK_PORTS.getOrDefault(
                framework.name(), StackMapping.DEFAULT_PORT_FALLBACK);
    }

    private static boolean inferNativeBuild(ProjectConfig config) {
        if (!config.framework().nativeBuild()) {
            return false;
        }
        return StackMapping.NATIVE_SUPPORTED_FRAMEWORKS
                .contains(config.framework().name());
    }

    private static List<String> extractInterfaceTypes(ProjectConfig config) {
        return config.interfaces().stream()
                .map(iface -> iface.type())
                .toList();
    }

    private static String deriveProjectType(ProjectConfig config) {
        String style = config.architecture().style();
        List<String> interfaceTypes = extractInterfaceTypes(config);

        return switch (style) {
            case "microservice" -> microserviceType(interfaceTypes);
            case "modular-monolith", "monolith", "serverless" -> "api";
            case "library" -> libraryType(interfaceTypes);
            default -> "api";
        };
    }

    private static String microserviceType(List<String> interfaceTypes) {
        boolean hasEvent =
                interfaceTypes.contains(EVENT_CONSUMER_INTERFACE);
        boolean hasRest = interfaceTypes.contains(REST_INTERFACE);
        if (hasEvent && !hasRest) {
            return "worker";
        }
        return "api";
    }

    private static String libraryType(List<String> interfaceTypes) {
        if (interfaceTypes.contains(CLI_INTERFACE)) {
            return "cli";
        }
        return "library";
    }

    private static List<String> deriveProtocols(ProjectConfig config) {
        List<String> interfaceTypes = extractInterfaceTypes(config);
        List<String> protocols = new ArrayList<>();
        for (String itype : interfaceTypes) {
            String protocol =
                    StackMapping.INTERFACE_SPEC_PROTOCOL_MAP.get(itype);
            if (protocol != null) {
                protocols.add(protocol);
            }
        }
        return protocols;
    }

    private static String fieldOrEmpty(
            LanguageCommandSet commands, String field) {
        if (commands == null) {
            return EMPTY_COMMAND;
        }
        return switch (field) {
            case "compileCmd" -> commands.compileCmd();
            case "buildCmd" -> commands.buildCmd();
            case "testCmd" -> commands.testCmd();
            case "coverageCmd" -> commands.coverageCmd();
            case "fileExtension" -> commands.fileExtension();
            case "buildFile" -> commands.buildFile();
            case "packageManager" -> commands.packageManager();
            default -> EMPTY_COMMAND;
        };
    }
}
