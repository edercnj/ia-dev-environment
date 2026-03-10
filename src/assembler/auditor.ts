/**
 * Rules directory auditor — counts files and bytes, checks thresholds.
 *
 * @remarks
 * Uses synchronous `node:fs` by design. Audit runs once per CLI invocation
 * on a small directory (typically ≤ 10 files). Sync I/O matches the Python
 * predecessor and avoids unnecessary async complexity.
 *
 * @module
 */
import * as fs from "node:fs";
import * as path from "node:path";

/** Maximum recommended number of rule files. */
export const MAX_FILE_COUNT = 10;

/** Maximum recommended total bytes (50 KB). */
export const MAX_TOTAL_BYTES = 51_200;

/** Result of auditing the generated rules directory. */
export interface AuditResult {
  readonly totalFiles: number;
  readonly totalBytes: number;
  readonly fileSizes: ReadonlyArray<readonly [string, number]>;
  readonly warnings: readonly string[];
}

/**
 * Count rule files and total size, check against thresholds.
 *
 * Returns an {@link AuditResult} with warnings if thresholds exceeded.
 * Read-only: never modifies files.
 */
export function auditRulesContext(rulesDir: string): AuditResult {
  if (
    !fs.existsSync(rulesDir) ||
    !fs.statSync(rulesDir).isDirectory()
  ) {
    return {
      totalFiles: 0,
      totalBytes: 0,
      fileSizes: [],
      warnings: [],
    };
  }

  const fileSizes = collectFileSizes(rulesDir);
  const totalFiles = fileSizes.length;
  const totalBytes = fileSizes.reduce(
    (sum, [, size]) => sum + size,
    0,
  );
  const warnings = checkThresholds(totalFiles, totalBytes);

  return { totalFiles, totalBytes, fileSizes, warnings };
}

function collectFileSizes(
  rulesDir: string,
): Array<readonly [string, number]> {
  const entries = fs.readdirSync(rulesDir).sort();
  const sizes: Array<readonly [string, number]> = [];

  for (const entry of entries) {
    if (!entry.endsWith(".md")) continue;
    const fullPath = path.join(rulesDir, entry);
    const stat = fs.statSync(fullPath);
    if (stat.isFile()) {
      sizes.push([entry, stat.size] as const);
    }
  }

  sizes.sort((a, b) => b[1] - a[1]);
  return sizes;
}

function checkThresholds(
  totalFiles: number,
  totalBytes: number,
): string[] {
  const warnings: string[] = [];

  if (totalFiles > MAX_FILE_COUNT) {
    warnings.push(
      `${totalFiles} rule files exceeds recommended maximum of ${MAX_FILE_COUNT}.`,
    );
  }
  if (totalBytes > MAX_TOTAL_BYTES) {
    const totalKb = Math.floor(totalBytes / 1024);
    warnings.push(
      `${totalKb}KB total rules exceeds recommended maximum of 50KB.`,
    );
  }

  return warnings;
}
