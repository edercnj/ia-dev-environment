/**
 * Configurable mock for subagent dispatch.
 *
 * Replaces the real subagent dispatch boundary in E2E tests,
 * returning pre-configured results per story ID.
 */
import type { SubagentResult } from "../../../../src/checkpoint/types.js";

export interface MockSubagentConfig {
  readonly results: Readonly<
    Record<string, SubagentResult | readonly SubagentResult[]>
  >;
  readonly defaultResult?: SubagentResult;
}

export interface CallLogEntry {
  readonly storyId: string;
  readonly attempt: number;
}

export interface MockSubagentDispatch {
  readonly dispatch: (storyId: string) => SubagentResult;
  readonly callLog: CallLogEntry[];
}

export function createMockDispatch(
  config: MockSubagentConfig,
): MockSubagentDispatch {
  const callLog: CallLogEntry[] = [];
  const attemptCounters = new Map<string, number>();

  function dispatch(storyId: string): SubagentResult {
    const attempt = (attemptCounters.get(storyId) ?? 0) + 1;
    attemptCounters.set(storyId, attempt);
    callLog.push({ storyId, attempt });

    const configured = config.results[storyId];
    if (configured === undefined) {
      if (config.defaultResult) {
        return config.defaultResult;
      }
      throw new Error(
        `No mock result configured for story "${storyId}" and no default provided`,
      );
    }

    if (Array.isArray(configured)) {
      const index = Math.min(attempt - 1, configured.length - 1);
      return configured[index];
    }

    return configured;
  }

  return { dispatch, callLog };
}

export const SUCCESS_RESULT: SubagentResult = {
  status: "SUCCESS",
  commitSha: "sha-e2e-test",
  findingsCount: 0,
  summary: "Mock success",
};

export const FAILED_RESULT: SubagentResult = {
  status: "FAILED",
  findingsCount: 1,
  summary: "Mock failure",
};
