import { Command } from "commander";
import { DEFAULT_FOUNDATION } from "./models.js";

const PROGRAM_NAME = "ia-dev-env";
const PROGRAM_DESCRIPTION = "ia-dev-environment CLI foundation (STORY-001).";

export function createCli(): Command {
  return new Command().name(PROGRAM_NAME).description(PROGRAM_DESCRIPTION).version(DEFAULT_FOUNDATION.version);
}

export async function runCli(
  argv: readonly string[],
  cli: Pick<Command, "parseAsync"> = createCli(),
): Promise<void> {
  await cli.parseAsync([...argv]);
}
