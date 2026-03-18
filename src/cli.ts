/**
 * CLI command definitions — generate and validate subcommands.
 *
 * Migrated from Python `__main__.py` using commander (replacing click).
 *
 * @module
 */
import { existsSync } from "node:fs";
import { Command } from "commander";
import { loadConfig } from "./config.js";
import { runPipeline } from "./assembler/pipeline.js";
import { runInteractive } from "./interactive.js";
import { findResourcesDir, setupLogging } from "./utils.js";
import { validateStack } from "./domain/validator.js";
import {
  CliError,
  ConfigParseError,
  ConfigValidationError,
  PipelineError,
} from "./exceptions.js";
import {
  checkExistingArtifacts,
  formatConflictMessage,
} from "./overwrite-detector.js";
import type { ProjectConfig, PipelineResult } from "./models.js";
import { DEFAULT_FOUNDATION } from "./models.js";
import { displayResult } from "./cli-display.js";

const PROGRAM_NAME = "ia-dev-env";
const PROGRAM_DESCRIPTION =
  "Claude Setup - Project scaffolding tool.";

const MUTUAL_EXCLUSIVE_MSG =
  "Options --config and --interactive are mutually exclusive.";
const MISSING_INPUT_MSG =
  "Either --config or --interactive is required.";
const CONFIG_NOT_FOUND_PREFIX = "Config file not found:";
const RESOURCES_NOT_FOUND_PREFIX = "Resources directory not found:";
const GENERIC_CLI_MSG =
  "Command failed. Run with --help for usage.";

/** Validate that --config and --interactive are mutually exclusive. */
function validateGenerateOptions(
  configPath: string | undefined,
  interactive: boolean | undefined,
): void {
  if (configPath && interactive) {
    throw new CliError(
      MUTUAL_EXCLUSIVE_MSG,
      "MUTUAL_EXCLUSIVE",
    );
  }
  if (!configPath && !interactive) {
    throw new CliError(MISSING_INPUT_MSG, "MISSING_INPUT");
  }
}

/** Load project config from file path or interactive mode. */
async function loadProjectConfig(
  configPath: string | undefined,
  interactive: boolean | undefined,
): Promise<ProjectConfig> {
  if (configPath) {
    if (!existsSync(configPath)) {
      throw new CliError(
        `${CONFIG_NOT_FOUND_PREFIX} ${configPath}`,
        "CONFIG_NOT_FOUND",
      );
    }
    return loadConfig(configPath);
  }
  return runInteractive();
}

/** Resolve resources directory from explicit path or auto-detection. */
function resolveResourcesDir(
  resourcesDir: string | undefined,
): string {
  if (resourcesDir !== undefined) {
    if (!existsSync(resourcesDir)) {
      throw new CliError(
        `${RESOURCES_NOT_FOUND_PREFIX} ${resourcesDir}`,
        "RESOURCES_NOT_FOUND",
      );
    }
    return resourcesDir;
  }
  return findResourcesDir();
}

/** Run the generate pipeline and display results. */
async function executeGenerate(
  config: ProjectConfig,
  resourcesDir: string,
  outputDir: string,
  dryRun: boolean,
): Promise<PipelineResult> {
  const result = await runPipeline(
    config, resourcesDir, outputDir, dryRun,
  );
  displayResult(result);
  return result;
}

/** Handle known errors with friendly messages. */
function handleKnownError(
  error: unknown,
  verbose: boolean | undefined,
): never {
  if (
    error instanceof CliError
    || error instanceof ConfigValidationError
    || error instanceof ConfigParseError
    || error instanceof PipelineError
  ) {
    console.error(error.message);
    process.exit(1);
  }
  if (verbose && error instanceof Error) {
    console.error(error.stack ?? error.message);
  } else {
    console.error(GENERIC_CLI_MSG);
  }
  process.exit(1);
}

interface GenerateOptions {
  readonly config?: string;
  readonly interactive?: boolean;
  readonly outputDir: string;
  readonly resourcesDir?: string;
  readonly verbose?: boolean;
  readonly dryRun?: boolean;
  readonly force?: boolean;
}

/** Action handler for the generate subcommand. */
async function handleGenerate(
  options: GenerateOptions,
): Promise<void> {
  try {
    if (options.verbose) {
      setupLogging(true);
    }
    validateGenerateOptions(options.config, options.interactive);
    const config = await loadProjectConfig(
      options.config, options.interactive,
    );
    const resourcesDir = resolveResourcesDir(
      options.resourcesDir,
    );
    if (!options.dryRun && !options.force) {
      const check = checkExistingArtifacts(options.outputDir);
      if (check.hasConflicts) {
        throw new CliError(
          formatConflictMessage(check.conflictDirs),
          "OVERWRITE_CONFLICT",
        );
      }
    }
    await executeGenerate(
      config,
      resourcesDir,
      options.outputDir,
      options.dryRun ?? false,
    );
  } catch (error: unknown) {
    handleKnownError(error, options.verbose);
  }
}

interface ValidateOptions {
  readonly config: string;
  readonly verbose?: boolean;
}

/** Action handler for the validate subcommand. */
async function handleValidate(
  options: ValidateOptions,
): Promise<void> {
  try {
    if (options.verbose) {
      setupLogging(true);
    }
    if (!existsSync(options.config)) {
      throw new CliError(
        `${CONFIG_NOT_FOUND_PREFIX} ${options.config}`,
        "CONFIG_NOT_FOUND",
      );
    }
    const config = loadConfig(options.config);
    const errors = validateStack(config);
    if (errors.length > 0) {
      console.error(errors.join("\n"));
      process.exit(1);
    }
    console.log("Config is valid.");
  } catch (error: unknown) {
    handleKnownError(error, options.verbose);
  }
}

/** Register the generate subcommand on a program. */
function registerGenerateCommand(program: Command): void {
  program
    .command("generate")
    .description(
      "Generate project scaffolding from config or interactive mode.",
    )
    .option("-c, --config <path>", "Path to YAML config file.")
    .option("-i, --interactive", "Run in interactive mode.")
    .option("-o, --output-dir <path>", "Output directory.", ".")
    .option("-s, --resources-dir <path>", "Resources templates directory.")
    .option("-v, --verbose", "Enable verbose logging.")
    .option("--dry-run", "Show what would be generated without writing.")
    .option("-f, --force", "Overwrite existing generated artifacts.", false)
    .action(handleGenerate);
}

/** Register the validate subcommand on a program. */
function registerValidateCommand(program: Command): void {
  program
    .command("validate")
    .description("Validate a config file without generating output.")
    .requiredOption("-c, --config <path>", "Path to YAML config file.")
    .option("-v, --verbose", "Enable verbose logging.")
    .action(handleValidate);
}

/**
 * Create the CLI program with generate and validate subcommands.
 *
 * @returns A configured commander Command ready for parsing.
 */
export function createCli(): Command {
  const program = new Command()
    .name(PROGRAM_NAME)
    .description(PROGRAM_DESCRIPTION)
    .version(DEFAULT_FOUNDATION.version);

  registerGenerateCommand(program);
  registerValidateCommand(program);
  return program;
}

/**
 * Parse argv and execute the matched command.
 *
 * @param argv - Process arguments (typically process.argv).
 * @param cli - Injectable command for testing.
 */
export async function runCli(
  argv: readonly string[],
  cli: Pick<Command, "parseAsync"> = createCli(),
): Promise<void> {
  await cli.parseAsync([...argv]);
}
