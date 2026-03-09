#!/usr/bin/env node

import { runCli } from "./cli.js";
import { CliError } from "./exceptions.js";

const GENERIC_ERROR_MESSAGE = "Command failed. Run with --help for usage.";

export async function bootstrap(argv: readonly string[] = process.argv): Promise<void> {
  await runCli([...argv]);
}

async function main(): Promise<void> {
  try {
    await bootstrap();
  } catch (error: unknown) {
    const safeMessage = error instanceof CliError ? error.message : GENERIC_ERROR_MESSAGE;
    console.error(safeMessage);
    process.exitCode = 1;
  }
}

void main();
