import * as fs from "node:fs";
import * as path from "node:path";
import type { VerificationResult } from "../../src/models.js";

export const CONFIG_PROFILES = [
  "go-gin",
  "java-quarkus",
  "java-spring",
  "kotlin-ktor",
  "python-click-cli",
  "python-fastapi",
  "rust-axum",
  "typescript-nestjs",
] as const;

export const PROJECT_ROOT = path.resolve(__dirname, "../..");
export const CONFIG_TEMPLATES_DIR = path.join(
  PROJECT_ROOT, "resources", "config-templates",
);
export const GOLDEN_DIR = path.join(PROJECT_ROOT, "tests", "golden");
export const RESOURCES_DIR = path.join(PROJECT_ROOT, "resources");

const DIFF_PREVIEW_CHARS = 500;

export function formatVerificationFailures(
  result: VerificationResult,
): string {
  const lines: string[] = [];
  for (const m of result.mismatches) {
    lines.push(
      `MISMATCH: ${m.path} (actual=${m.pythonSize}B, ref=${m.referenceSize}B)`,
    );
    lines.push(m.diff.slice(0, DIFF_PREVIEW_CHARS));
  }
  for (const p of result.missingFiles) {
    lines.push(`MISSING: ${p}`);
  }
  for (const p of result.extraFiles) {
    lines.push(`EXTRA: ${p}`);
  }
  return lines.join("\n");
}

export function goldenExists(profileName: string): boolean {
  return fs.existsSync(path.join(GOLDEN_DIR, profileName));
}
