/**
 * Synthetic 5-story, 3-phase implementation map for E2E tests.
 *
 * DAG structure:
 *   Phase 0: story-e2e-0001 (root, no deps)
 *   Phase 1: story-e2e-0002 (blocked by 0001)
 *             story-e2e-0003 (blocked by 0001)
 *   Phase 2: story-e2e-0004 (blocked by 0002, 0003)
 *             story-e2e-0005 (blocked by 0003)
 *
 * Exercises: fan-out, fan-in, transitive blocking, critical path.
 */

export const STORY_IDS = [
  "story-e2e-0001",
  "story-e2e-0002",
  "story-e2e-0003",
  "story-e2e-0004",
  "story-e2e-0005",
] as const;

export const EPIC_ID = "e2e-test";
export const BRANCH = "feat/e2e-test";
export const TOTAL_PHASES = 3;
export const TOTAL_STORIES = 5;

export const PHASE_STORIES: ReadonlyMap<number, readonly string[]> = new Map([
  [0, ["story-e2e-0001"]],
  [1, ["story-e2e-0002", "story-e2e-0003"]],
  [2, ["story-e2e-0004", "story-e2e-0005"]],
]);

/**
 * Build a valid IMPLEMENTATION-MAP.md markdown string.
 * Only Section 1 (Dependency Matrix) is needed by `parseImplementationMap`.
 */
export function buildMiniImplementationMap(): string {
  return [
    "# Implementation Map \u2014 E2E Test Epic",
    "",
    "## 1. Matriz de Depend\u00eancias",
    "",
    "| Story | T\u00edtulo | Blocked By | Blocks | Status |",
    "| :--- | :--- | :--- | :--- | :--- |",
    "| story-e2e-0001 | Foundation | \u2014 | story-e2e-0002, story-e2e-0003 | Pendente |",
    "| story-e2e-0002 | Service A | story-e2e-0001 | story-e2e-0004 | Pendente |",
    "| story-e2e-0003 | Service B | story-e2e-0001 | story-e2e-0004, story-e2e-0005 | Pendente |",
    "| story-e2e-0004 | Integration | story-e2e-0002, story-e2e-0003 | \u2014 | Pendente |",
    "| story-e2e-0005 | Final | story-e2e-0003 | \u2014 | Pendente |",
    "",
    "## 2. Fases de Implementa\u00e7\u00e3o",
    "",
  ].join("\n");
}
