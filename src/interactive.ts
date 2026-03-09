import inquirer from "inquirer";
import { CliError } from "./exceptions.js";

interface ConfirmationPrompt {
  readonly confirmed: boolean;
}

const CONFIRMATION_PROMPT_NAME = "confirmed";
const DEFAULT_PROMPT_TIMEOUT_MS = 30_000;

export async function promptConfirmation(
  message: string,
  defaultValue = true,
  timeoutMs = DEFAULT_PROMPT_TIMEOUT_MS,
): Promise<boolean> {
  if (timeoutMs <= 0) {
    throw new CliError("Prompt timeout must be greater than zero.", "E_INVALID_TIMEOUT");
  }

  let timeoutId: NodeJS.Timeout | undefined;
  const promptPromise = inquirer.prompt<ConfirmationPrompt>([
    {
      type: "confirm",
      name: CONFIRMATION_PROMPT_NAME,
      message,
      default: defaultValue,
    },
  ]);

  const timeoutPromise = new Promise<never>((_resolve, reject) => {
    timeoutId = setTimeout(() => {
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
