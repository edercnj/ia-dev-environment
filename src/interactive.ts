import inquirer from "inquirer";
import { CliError } from "./exceptions.js";
import {
  ArchitectureConfig,
  FrameworkConfig,
  InterfaceConfig,
  LanguageConfig,
  ProjectConfig,
  ProjectIdentity,
} from "./models.js";

interface ConfirmationPrompt {
  readonly confirmed: boolean;
}

const CONFIRMATION_PROMPT_NAME = "confirmed";
const DEFAULT_PROMPT_TIMEOUT_MS = 30_000;

export const ARCHITECTURE_CHOICES = [
  "library",
  "microservice",
  "monolith",
] as const;

export const LANGUAGE_CHOICES = [
  "python",
  "java",
  "go",
  "kotlin",
  "typescript",
  "rust",
] as const;

export const INTERFACE_CHOICES = [
  "rest",
  "grpc",
  "cli",
  "event-consumer",
  "event-producer",
] as const;

export const BUILD_TOOL_CHOICES = [
  "pip",
  "maven",
  "gradle",
  "go",
  "cargo",
  "npm",
] as const;

export const FRAMEWORK_CHOICES: Readonly<Record<string, readonly string[]>> = {
  python: ["fastapi", "click", "django", "flask"],
  java: ["quarkus", "spring-boot"],
  go: ["gin"],
  kotlin: ["ktor"],
  typescript: ["nestjs"],
  rust: ["axum"],
};

export async function promptConfirmation(
  message: string,
  defaultValue = true,
  timeoutMs = DEFAULT_PROMPT_TIMEOUT_MS,
): Promise<boolean> {
  if (timeoutMs <= 0) {
    throw new CliError("Prompt timeout must be greater than zero.", "E_INVALID_TIMEOUT");
  }

  let timeoutId: NodeJS.Timeout | undefined;
  const abortController = new AbortController();
  const promptOptions: Record<string, unknown> = { signal: abortController.signal };
  const promptPromise = inquirer.prompt<ConfirmationPrompt>([
    {
      type: "confirm",
      name: CONFIRMATION_PROMPT_NAME,
      message,
      default: defaultValue,
    },
  ], promptOptions);

  const timeoutPromise = new Promise<never>((_resolve, reject) => {
    timeoutId = setTimeout(() => {
      abortController.abort();
      process.stdin.pause();
      reject(new CliError("Prompt timed out while waiting for user input.", "E_PROMPT_TIMEOUT"));
    }, timeoutMs);
  });

  try {
    const result = await Promise.race<ConfirmationPrompt>([promptPromise, timeoutPromise]);
    return result.confirmed;
  } finally {
    if (timeoutId) {
      clearTimeout(timeoutId);
    }
  }
}

interface InteractiveAnswers {
  readonly projectName: string;
  readonly projectPurpose: string;
  readonly architectureStyle: string;
  readonly language: string;
  readonly framework: string;
  readonly languageVersion: string;
  readonly frameworkVersion: string;
  readonly buildTool: string;
  readonly interfaceType: string;
  readonly domainDriven: boolean;
  readonly eventDriven: boolean;
}

const MAX_NAME_LENGTH = 100;
const MAX_PURPOSE_LENGTH = 500;
const MAX_VERSION_LENGTH = 20;

export function validateProjectName(value: string): boolean | string {
  const trimmed = value.trim();
  if (trimmed.length === 0) {
    return "Project name cannot be empty.";
  }
  if (trimmed.length > MAX_NAME_LENGTH) {
    return `Project name must be at most ${MAX_NAME_LENGTH} characters.`;
  }
  return true;
}

export function validateProjectPurpose(value: string): boolean | string {
  const trimmed = value.trim();
  if (trimmed.length === 0) {
    return "Project purpose cannot be empty.";
  }
  if (trimmed.length > MAX_PURPOSE_LENGTH) {
    return `Project purpose must be at most ${MAX_PURPOSE_LENGTH} characters.`;
  }
  return true;
}

export function validateVersion(value: string): boolean | string {
  const trimmed = value.trim();
  if (trimmed.length === 0) {
    return "Version cannot be empty.";
  }
  if (trimmed.length > MAX_VERSION_LENGTH) {
    return `Version must be at most ${MAX_VERSION_LENGTH} characters.`;
  }
  return true;
}

export function getFrameworkChoices(language: string): readonly string[] {
  return FRAMEWORK_CHOICES[language] ?? ["other"];
}

async function collectAnswers(): Promise<InteractiveAnswers> {
  const identity = await inquirer.prompt<{
    projectName: string;
    projectPurpose: string;
  }>([
    {
      type: "input",
      name: "projectName",
      message: "Project name:",
      validate: validateProjectName,
    },
    {
      type: "input",
      name: "projectPurpose",
      message: "Project purpose:",
      validate: validateProjectPurpose,
    },
  ]);

  const archAndLang = await inquirer.prompt<{
    architectureStyle: string;
    language: string;
  }>([
    {
      type: "list",
      name: "architectureStyle",
      message: "Architecture style:",
      choices: [...ARCHITECTURE_CHOICES],
    },
    {
      type: "list",
      name: "language",
      message: "Language:",
      choices: [...LANGUAGE_CHOICES],
    },
  ]);

  const frameworkChoices = getFrameworkChoices(archAndLang.language);

  const frameworkAndVersions = await inquirer.prompt<{
    framework: string;
    languageVersion: string;
    frameworkVersion: string;
    buildTool: string;
  }>([
    {
      type: "list",
      name: "framework",
      message: "Framework:",
      choices: [...frameworkChoices],
    },
    {
      type: "input",
      name: "languageVersion",
      message: "Language version:",
      validate: validateVersion,
    },
    {
      type: "input",
      name: "frameworkVersion",
      message: "Framework version:",
      validate: validateVersion,
    },
    {
      type: "list",
      name: "buildTool",
      message: "Build tool:",
      choices: [...BUILD_TOOL_CHOICES],
    },
  ]);

  const interfaceAndFlags = await inquirer.prompt<{
    interfaceType: string;
    domainDriven: boolean;
    eventDriven: boolean;
  }>([
    {
      type: "list",
      name: "interfaceType",
      message: "Interface type:",
      choices: [...INTERFACE_CHOICES],
    },
    {
      type: "confirm",
      name: "domainDriven",
      message: "Domain-driven design?",
      default: false,
    },
    {
      type: "confirm",
      name: "eventDriven",
      message: "Event-driven?",
      default: false,
    },
  ]);

  return {
    ...identity,
    ...archAndLang,
    ...frameworkAndVersions,
    ...interfaceAndFlags,
  };
}

function buildProjectConfig(answers: InteractiveAnswers): ProjectConfig {
  const project = new ProjectIdentity(
    answers.projectName,
    answers.projectPurpose,
  );
  const architecture = new ArchitectureConfig(
    answers.architectureStyle,
    answers.domainDriven,
    answers.eventDriven,
  );
  const interfaces = [new InterfaceConfig(answers.interfaceType)];
  const language = new LanguageConfig(
    answers.language,
    answers.languageVersion,
  );
  const framework = new FrameworkConfig(
    answers.framework,
    answers.frameworkVersion,
    answers.buildTool,
  );

  return new ProjectConfig(
    project,
    architecture,
    interfaces,
    language,
    framework,
  );
}

/** Prompts the user for all config fields interactively and returns a ProjectConfig. */
export async function runInteractive(): Promise<ProjectConfig> {
  const answers = await collectAnswers();
  return buildProjectConfig(answers);
}
