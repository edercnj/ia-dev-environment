#!/usr/bin/env node

import { existsSync, realpathSync } from "node:fs";
import { resolve } from "node:path";
import { fileURLToPath } from "node:url";
import { runCli } from "./cli.js";
import {
  CliError,
  ConfigParseError,
  ConfigValidationError,
  PipelineError,
} from "./exceptions.js";

const GENERIC_ERROR_MESSAGE = "Command failed. Run with --help for usage.";

export async function bootstrap(argv: readonly string[] = process.argv): Promise<void> {
  await runCli([...argv]);
}

async function main(): Promise<void> {
  try {
    await bootstrap();
  } catch (error: unknown) {
    const isKnownError =
      error instanceof CliError
      || error instanceof ConfigValidationError
      || error instanceof ConfigParseError
      || error instanceof PipelineError;
    const safeMessage = isKnownError
      ? (error as Error).message
      : GENERIC_ERROR_MESSAGE;
    console.error(safeMessage);
    process.exitCode = 1;
  }
}

export function shouldRunAsCli(entryUrl = import.meta.url, argv = process.argv): boolean {
  const entryArg = argv[1];
  if (!entryArg) {
    return false;
  }

  const entryPath = fileURLToPath(entryUrl);
  const argvPath = resolve(entryArg);

  if (existsSync(entryPath) && existsSync(argvPath)) {
    return realpathSync(entryPath) === realpathSync(argvPath);
  }

  return entryPath === argvPath;
}

if (shouldRunAsCli()) {
  void main();
}
