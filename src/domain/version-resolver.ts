/**
 * Version directory lookup with fallback.
 *
 * Migrated from Python `domain/version_resolver.py`.
 */

import { statSync } from "node:fs";
import { join } from "node:path";

/**
 * Find a version-specific directory with fallback.
 *
 * Resolution order:
 * 1. Exact match: `{baseDir}/{name}-{version}`
 * 2. Major version fallback: `{baseDir}/{name}-{major}.x`
 *
 * Returns `undefined` if neither directory exists.
 */
export function findVersionDir(
  baseDir: string,
  name: string,
  version: string,
): string | undefined {
  const exact = join(baseDir, `${name}-${version}`);
  if (isDirectory(exact)) {
    return exact;
  }
  const major = version.split(".")[0] ?? version;
  const fallback = join(baseDir, `${name}-${major}.x`);
  if (isDirectory(fallback)) {
    return fallback;
  }
  return undefined;
}

function isDirectory(path: string): boolean {
  try {
    return statSync(path).isDirectory();
  } catch {
    return false;
  }
}
