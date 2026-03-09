import { Command } from "commander";

const PROGRAM_NAME = "ia-dev-env";
const PROGRAM_DESCRIPTION = "ia-dev-environment CLI foundation (STORY-001).";
const PROGRAM_VERSION = "0.1.0";

export function createCli(): Command {
  return new Command().name(PROGRAM_NAME).description(PROGRAM_DESCRIPTION).version(PROGRAM_VERSION);
}

export async function runCli(
  argv: readonly string[],
  cli: Pick<Command, "parseAsync"> = createCli(),
): Promise<void> {
  await cli.parseAsync([...argv]);
}
