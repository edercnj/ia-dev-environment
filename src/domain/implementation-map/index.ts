/**
 * Implementation Map Parser — public API.
 *
 * Provides parseImplementationMap() facade and barrel exports
 * for all types, functions, and errors.
 */
export * from "./types.js";
export { extractDependencyMatrix, extractPhaseSummary } from "./markdown-parser.js";
export { buildDag } from "./dag-builder.js";
export { validateSymmetry, detectCycles, validateRoots } from "./dag-validator.js";
export { computePhases } from "./phase-computer.js";
export { findCriticalPath, markCriticalPath } from "./critical-path.js";
export { getExecutableStories } from "./executable-stories.js";

import type { ParsedMap } from "./types.js";
import { extractDependencyMatrix } from "./markdown-parser.js";
import { buildDag } from "./dag-builder.js";
import { validateSymmetry, detectCycles, validateRoots } from "./dag-validator.js";
import { computePhases } from "./phase-computer.js";
import { findCriticalPath, markCriticalPath } from "./critical-path.js";

/** Parse an IMPLEMENTATION-MAP.md content into a structured ParsedMap. */
export function parseImplementationMap(content: string): ParsedMap {
  const rows = extractDependencyMatrix(content);
  const dag = buildDag(rows);
  const warnings = validateSymmetry(dag);
  detectCycles(dag);
  validateRoots(dag);
  const phases = computePhases(dag);
  const criticalPath = findCriticalPath(dag, phases);
  markCriticalPath(dag, criticalPath);

  return {
    stories: dag,
    phases,
    criticalPath,
    totalPhases: phases.size,
    warnings,
  };
}
