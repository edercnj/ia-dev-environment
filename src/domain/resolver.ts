/**
 * Stack resolution functions.
 *
 * Migrated from Python `domain/resolver.py`.
 * Derives commands, Docker image, health path, port, project type,
 * and protocols from a ProjectConfig.
 */
import type {
  FrameworkConfig,
  LanguageConfig,
  ProjectConfig,
} from "../models.js";
import type { ResolvedStack } from "./resolved-stack.js";
import {
  DEFAULT_DOCKER_IMAGE,
  DEFAULT_HEALTH_PATH,
  DEFAULT_PORT_FALLBACK,
  DOCKER_BASE_IMAGES,
  FRAMEWORK_HEALTH_PATHS,
  FRAMEWORK_PORTS,
  INTERFACE_SPEC_PROTOCOL_MAP,
  LANGUAGE_COMMANDS,
  NATIVE_SUPPORTED_FRAMEWORKS,
} from "./stack-mapping.js";

const EMPTY_COMMAND = "";
const CLI_INTERFACE = "cli";
const EVENT_CONSUMER_INTERFACE = "event-consumer";
const REST_INTERFACE = "rest";

function resolveCommands(
  language: LanguageConfig,
  framework: FrameworkConfig,
): Record<string, string> {
  const key = `${language.name}-${framework.buildTool}`;
  const cmds = LANGUAGE_COMMANDS[key];
  if (cmds === undefined) return {};
  return {
    buildCmd: cmds.buildCmd,
    testCmd: cmds.testCmd,
    compileCmd: cmds.compileCmd,
    coverageCmd: cmds.coverageCmd,
    fileExtension: cmds.fileExtension,
    buildFile: cmds.buildFile,
    packageManager: cmds.packageManager,
  };
}

function resolveDockerImage(language: LanguageConfig): string {
  const template = DOCKER_BASE_IMAGES[language.name];
  if (template === undefined) return DEFAULT_DOCKER_IMAGE;
  try {
    return template.replace("{version}", language.version);
  } catch {
    return DEFAULT_DOCKER_IMAGE;
  }
}

function resolveHealthPath(framework: FrameworkConfig): string {
  return FRAMEWORK_HEALTH_PATHS[framework.name] ?? DEFAULT_HEALTH_PATH;
}

function resolveDefaultPort(framework: FrameworkConfig): number {
  return FRAMEWORK_PORTS[framework.name] ?? DEFAULT_PORT_FALLBACK;
}

function inferNativeBuild(config: ProjectConfig): boolean {
  if (!config.framework.nativeBuild) return false;
  return NATIVE_SUPPORTED_FRAMEWORKS.includes(config.framework.name);
}

function extractInterfaceTypes(config: ProjectConfig): string[] {
  return config.interfaces.map((iface) => iface.type);
}

function microserviceType(interfaceTypes: string[]): string {
  const hasEvent = interfaceTypes.includes(EVENT_CONSUMER_INTERFACE);
  const hasRest = interfaceTypes.includes(REST_INTERFACE);
  if (hasEvent && !hasRest) return "worker";
  return "api";
}

function libraryType(interfaceTypes: string[]): string {
  if (interfaceTypes.includes(CLI_INTERFACE)) return "cli";
  return "library";
}

function deriveProjectType(config: ProjectConfig): string {
  const style = config.architecture.style;
  const interfaceTypes = extractInterfaceTypes(config);
  const dispatch: Record<string, () => string> = {
    "microservice": () => microserviceType(interfaceTypes),
    "modular-monolith": () => "api",
    "monolith": () => "api",
    "library": () => libraryType(interfaceTypes),
    "serverless": () => "api",
  };
  const handler = dispatch[style] ?? (() => "api");
  return handler();
}

function deriveProtocols(config: ProjectConfig): string[] {
  const interfaceTypes = extractInterfaceTypes(config);
  const protocols: string[] = [];
  for (const itype of interfaceTypes) {
    const protocol = INTERFACE_SPEC_PROTOCOL_MAP[itype];
    if (protocol !== undefined) {
      protocols.push(protocol);
    }
  }
  return protocols;
}

/** Resolve all derived stack values from project config. */
export function resolveStack(
  config: ProjectConfig,
): Readonly<ResolvedStack> {
  const commands = resolveCommands(config.language, config.framework);
  const protocols = deriveProtocols(config);
  const stack: ResolvedStack = {
    buildCmd: commands["buildCmd"] ?? EMPTY_COMMAND,
    testCmd: commands["testCmd"] ?? EMPTY_COMMAND,
    compileCmd: commands["compileCmd"] ?? EMPTY_COMMAND,
    coverageCmd: commands["coverageCmd"] ?? EMPTY_COMMAND,
    dockerBaseImage: resolveDockerImage(config.language),
    healthPath: resolveHealthPath(config.framework),
    packageManager: commands["packageManager"] ?? EMPTY_COMMAND,
    defaultPort: resolveDefaultPort(config.framework),
    fileExtension: commands["fileExtension"] ?? EMPTY_COMMAND,
    buildFile: commands["buildFile"] ?? EMPTY_COMMAND,
    nativeSupported: inferNativeBuild(config),
    projectType: deriveProjectType(config),
    protocols,
  };
  return Object.freeze(stack);
}
