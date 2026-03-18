/**
 * Overwrite detection for generated artifact directories.
 *
 * Checks whether the output directory already contains directories
 * that the pipeline would generate, enabling the CLI to warn users
 * before unintentional overwrites.
 *
 * @module
 */
import { existsSync } from "node:fs";
import { join } from "node:path";

/** Directories that the pipeline generates as top-level output. */
const ARTIFACT_DIRS = [".claude", ".github", "docs"] as const;

/** Result of checking for existing artifacts in the output directory. */
export interface OverwriteCheckResult {
  readonly hasConflicts: boolean;
  readonly conflictDirs: readonly string[];
}

/**
 * Checks whether outputDir already contains generated artifact directories.
 *
 * @param outputDir - The directory to inspect.
 * @returns List of conflicting directories with trailing slash.
 */
export function checkExistingArtifacts(
  outputDir: string,
): OverwriteCheckResult {
  if (!existsSync(outputDir)) {
    return { hasConflicts: false, conflictDirs: [] };
  }
  const conflictDirs: string[] = [];
  for (const dir of ARTIFACT_DIRS) {
    if (existsSync(join(outputDir, dir))) {
      conflictDirs.push(`${dir}/`);
    }
  }
  return {
    hasConflicts: conflictDirs.length > 0,
    conflictDirs,
  };
}

/**
 * Formats a user-facing error message listing conflicting directories.
 *
 * @param conflictDirs - Directories that already exist.
 * @returns Multi-line error message with --force hint.
 */
export function formatConflictMessage(
  conflictDirs: readonly string[],
): string {
  const dirList = conflictDirs
    .map((d) => `  - ${d} (exists)`)
    .join("\n");
  return [
    "Output directory contains existing generated artifacts:",
    dirList,
    "",
    "Use --force to overwrite existing files,",
    "or specify a different --output-dir.",
  ].join("\n");
}
